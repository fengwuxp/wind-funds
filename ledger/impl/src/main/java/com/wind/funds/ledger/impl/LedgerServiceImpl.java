package com.wind.funds.ledger.impl;

import com.wind.funds.ledger.dal.entities.Ledger;
import com.wind.funds.ledger.dal.entities.table.LedgerNameRefs;
import com.wind.funds.ledger.dal.mapper.LedgerMapper;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.mapstruct.LedgerConverter;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerStateRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.LedgerNormalBalanceGuard;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerState;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.spec.SettlementPolicySpec;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 账户账本服务实现类
 *
 * @author wuxp
 * @since 2026-04-24
 */
@Service
@Slf4j
@AllArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerMapper ledgerMapper;

    private final LedgerProfileCatalog ledgerProfileCatalog;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeRequiredLedgers(@NonNull InitializeSubjectLedgerRequest request) {
        List<CreateLedgerRequest> expectedLedgers = ledgerProfileCatalog.requiredLedgerRequests(request);
        try {
            for (CreateLedgerRequest expected : expectedLedgers) {
                LedgerDTO existingLedger = findExistingLedger(expected);
                if (existingLedger == null) {
                    createLedger(expected);
                } else {
                    ledgerProfileCatalog.assertLedgerMatches(expected, existingLedger);
                }
            }
        } catch (DuplicateKeyException exception) {
            assertConcurrentWinnerMatches(expectedLedgers, exception);
        }
    }

    @Override
    public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
        Ledger entity = LedgerConverter.INSTANCE.convertToLedger(request);
        fillCreateDefaults(entity, request);
        ledgerMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建账户账本失败");
        return entity.getId();
    }

    @Override
    public void updateLedgerState(@NonNull UpdateLedgerStateRequest request) {
        Ledger ledger = findLedger(request.getId());
        LedgerState targetState = request.getState();
        LedgerState.assertTransitionAllowed(ledger.getId(), ledger.getState(), targetState);
        if (ledger.getState() == targetState) {
            return;
        }
        assertCloseableBalance(ledger, targetState);
        Ledger entity = UpdateEntity.of(Ledger.class);
        UpdateWrapper<Ledger> updateWrapper = UpdateWrapper.of(entity);
        updateWrapper.set(LedgerNameRefs.ledger.state, targetState, true);
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.version, 1L);
        AssertUtils.isTrue(ledgerMapper.updateByQuery(entity, QueryWrapper.create()
                        .where(LedgerNameRefs.ledger.id.eq(request.getId()))
                        .and(LedgerNameRefs.ledger.version.eq(ledger.getVersion()))
                        .and(LedgerNameRefs.ledger.state.eq(ledger.getState()))) == 1,
                "账本状态更新失败");
    }

    @Override
    public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
        return LedgerConverter.INSTANCE.convertToLedgerDTO(findLedger(id));
    }

    @Override
    public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return ledgerMapper.selectListByIds(ids).stream()
                .map(LedgerConverter.INSTANCE::convertToLedgerDTO)
                .toList();
    }

    @Override
    public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                           @NonNull WindQuery<? extends QueryOrderField> options) {
        LedgerNameRefs ledger = LedgerNameRefs.ledger;
        QueryWrapper queryWrapper = MybatisQueryHelper.from(options).select()
                .from(ledger)
                .where(ledger.subjectId.eq(query.getSubjectId()))
                .and(ledger.subjectType.eq(query.getSubjectType()))
                .and(ledger.tenantId.eq(query.getTenantId()))
                .and(ledger.ledgerProfileCode.eq(query.getLedgerProfileCode()))
                .and(ledger.ledgerProfileVersion.eq(query.getLedgerProfileVersion()))
                .and(ledger.ledgerSubjectCode.eq(query.getLedgerSubjectCode()))
                .and(ledger.ledgerSubjectCategory.eq(query.getLedgerSubjectCategory()))
                .and(ledger.normalBalanceSide.eq(query.getNormalBalanceSide()))
                .and(ledger.allowNegative.eq(query.getAllowNegative()))
                .and(ledger.debitAmount.eq(query.getDebitAmount()))
                .and(ledger.creditAmount.eq(query.getCreditAmount()))
                .and(ledger.state.eq(query.getState()))
                .and(ledger.currency.eq(query.getCurrency()))
                .and(ledger.settlementPolicy.eq(query.getSettlementPolicy()))
                .and(ledger.cutOffTime.eq(query.getCutOffTime()))
                .and(ledger.periodType.eq(query.getPeriodType()))
                .and(ledger.periodId.eq(query.getPeriodId()))
                .and(ledger.version.eq(query.getVersion()))
                .and(ledger.gmtCreate.ge(query.getGmtCreateMin()))
                .and(ledger.gmtCreate.le(query.getGmtCreateMax()))
                .and(ledger.gmtModified.ge(query.getGmtModifiedMin()))
                .and(ledger.gmtModified.le(query.getGmtModifiedMax()));

        return MybatisQueryHelper.<Ledger, LedgerDTO>query(queryWrapper)
                .counter(ledgerMapper::selectCountByQuery)
                .resultQueryFunc(ledgerMapper::selectListByQuery)
                .converter(LedgerConverter.INSTANCE::convertToLedgerDTO)
                .query(options);
    }


    private Ledger findLedger(Long id) {
        Ledger result = ledgerMapper.selectOneById(id);
        AssertUtils.notNull(result, "账户账本不存在");
        return result;
    }

    private LedgerDTO findExistingLedger(CreateLedgerRequest expected) {
        List<LedgerDTO> records = queryLedgers(new LedgerQuery()
                        .setTenantId(expected.getTenantId())
                        .setSubjectId(expected.getSubjectId())
                        .setSubjectType(expected.getSubjectType())
                        .setCurrency(expected.getCurrency())
                        .setLedgerSubjectCode(expected.getLedgerSubjectCode())
                        .setPeriodType(expected.getPeriodType())
                        .setPeriodId(expected.getPeriodId()),
                com.wind.common.query.supports.DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isTrue(records.size() <= 1,
                "账本唯一桶配置不唯一，subjectId = {}, ledgerSubjectCode = {}, currency = {}, periodType = {}, periodId = {}",
                expected.getSubjectId(), expected.getLedgerSubjectCode(), expected.getCurrency(),
                expected.getPeriodType(), expected.getPeriodId());
        return records.isEmpty() ? null : records.getFirst();
    }

    private void assertConcurrentWinnerMatches(List<CreateLedgerRequest> expectedLedgers,
                                               DuplicateKeyException exception) {
        for (CreateLedgerRequest expected : expectedLedgers) {
            LedgerDTO existingLedger = findExistingLedger(expected);
            if (existingLedger == null) {
                throw exception;
            }
            ledgerProfileCatalog.assertLedgerMatches(expected, existingLedger);
        }
    }

    private void assertCloseableBalance(Ledger ledger, LedgerState targetState) {
        if (targetState != LedgerState.CLOSED) {
            return;
        }
        AssertUtils.isTrue(Objects.equals(0L, ledger.getNormalBalance()),
                "非零余额账本不允许关闭，ledgerId = {}, normalBalance = {}",
                ledger.getId(),
                ledger.getNormalBalance());
    }

    private void fillCreateDefaults(Ledger entity, CreateLedgerRequest request) {
        AssertUtils.hasText(entity.getSubjectId(), "账务主体 ID 不能为空");
        AssertUtils.hasText(entity.getSubjectType(), "账务主体类型不能为空");
        if (entity.getLedgerProfileCode() == null) {
            entity.setLedgerProfileCode(entity.getSubjectType());
        }
        if (entity.getLedgerProfileVersion() == null) {
            entity.setLedgerProfileVersion(1);
        }
        if (entity.getLedgerSubjectCategory() == null) {
            entity.setLedgerSubjectCategory(resolveLedgerSubjectCategory(request));
        }
        if (entity.getNormalBalanceSide() == null) {
            EntrySide normalBalanceSide = resolveNormalBalanceSide(
                    entity.getLedgerSubjectCode(),
                    entity.getLedgerSubjectCategory()
            );
            entity.setNormalBalanceSide(normalBalanceSide);
        }
        LedgerNormalBalanceGuard.assertCategoryNormalBalance(
                "创建",
                entity.getId(),
                entity.getLedgerSubjectCategory(),
                entity.getNormalBalanceSide());
        if (entity.getAllowNegative() == null) {
            entity.setAllowNegative(Boolean.FALSE);
        }
        if (entity.getDebitAmount() == null) {
            entity.setDebitAmount(0L);
        }
        if (entity.getCreditAmount() == null) {
            entity.setCreditAmount(0L);
        }
        if (entity.getState() == null) {
            entity.setState(LedgerState.ACTIVE);
        }
        if (entity.getSettlementPolicy() == null) {
            entity.setSettlementPolicy(SettlementPolicySpec.RT.getRaw());
        }
        if (entity.getCutOffTime() == null) {
            entity.setCutOffTime(LocalTime.MIDNIGHT);
        }
        if (entity.getPeriodType() == null) {
            entity.setPeriodType(AccountBalancePeriodType.LIFETIME);
        }
        if (entity.getPeriodType() == AccountBalancePeriodType.LIFETIME) {
            entity.setPeriodId(AccountBalancePeriodType.LIFETIME.name());
        } else {
            AssertUtils.hasText(entity.getPeriodId(), "非生命周期账本周期 periodId 不能为空");
        }
    }

    private LedgerSubjectCategory resolveLedgerSubjectCategory(CreateLedgerRequest request) {
        LedgerSubjectCategory result = request.getLedgerSubjectCategory();
        if (result != null) {
            return result;
        }
        return LedgerSubjectCategory.MEMO;
    }

    private EntrySide resolveNormalBalanceSide(LedgerSubjectCode ledgerSubjectCode, LedgerSubjectCategory category) {
        if (ledgerSubjectCode == LedgerSubjectCode.LIMIT) {
            return EntrySide.DEBIT;
        }
        EntrySide result = category.getNormalBalance();
        return result == null ? EntrySide.DEBIT : result;
    }

    private void setRawDelta(UpdateWrapper<Ledger> updateWrapper, QueryColumn fieldRef, Long delta) {
        if (delta == null || delta == 0L) {
            return;
        }
        if (delta > 0) {
            updateWrapper.setRaw(fieldRef, fieldRef.add(delta));
            return;
        }
        updateWrapper.setRaw(fieldRef, fieldRef.subtract(Math.abs(delta)));
    }

}

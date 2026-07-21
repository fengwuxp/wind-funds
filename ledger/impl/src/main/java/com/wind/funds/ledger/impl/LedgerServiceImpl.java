package com.wind.funds.ledger.impl;

import com.wind.funds.ledger.dal.entities.Ledger;
import com.wind.funds.ledger.dal.entities.table.LedgerNameRefs;
import com.wind.funds.ledger.dal.mapper.LedgerMapper;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.mapstruct.LedgerConverter;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.request.UpdateLedgerStatusRequest;
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
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerStatus;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.spec.ledger.SettlementPolicySpec;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
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

    @Override
    public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
        Ledger entity = LedgerConverter.INSTANCE.convertToLedger(request);
        fillCreateDefaults(entity, request);
        ledgerMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建账户账本失败");
        return entity.getId();
    }

    @Override
    public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
        Ledger ledger = findLedger(request.getId());
        LedgerPostingAccessType postingAccessType = request.getPostingAccessType() == null
                ? LedgerPostingAccessType.NORMAL
                : request.getPostingAccessType();
        LedgerStatus.assertPostable(ledger.getId(), ledger.getStatus(), postingAccessType);
        validateMinimumNormalBalance(ledger, request);
        Ledger entity = UpdateEntity.of(Ledger.class);
        UpdateWrapper<Ledger> updateWrapper = UpdateWrapper.of(entity);
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.debitAmount, request.getDebitAmountDelta());
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.creditAmount, request.getCreditAmountDelta());
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.version, 1L);
        QueryWrapper where = QueryWrapper.create()
                .where(LedgerNameRefs.ledger.id.eq(request.getId()))
                .and(LedgerNameRefs.ledger.version.eq(ledger.getVersion()))
                .and(LedgerNameRefs.ledger.status.eq(ledger.getStatus()));
        if (request.getMinimumNormalBalance() != null) {
            where.and(normalBalanceAfterDelta(ledger.getNormalBalanceSide(), request)
                    .ge(request.getMinimumNormalBalance()));
        }
        AssertUtils.isTrue(ledgerMapper.updateByQuery(entity, where) > 0, "账本余额更新失败");
    }

    @Override
    public void updateLedgerStatus(@NonNull UpdateLedgerStatusRequest request) {
        Ledger ledger = findLedger(request.getId());
        LedgerStatus targetStatus = request.getStatus();
        LedgerStatus.assertTransitionAllowed(ledger.getId(), ledger.getStatus(), targetStatus);
        if (ledger.getStatus() == targetStatus) {
            return;
        }
        assertCloseableBalance(ledger, targetStatus);
        Ledger entity = UpdateEntity.of(Ledger.class);
        UpdateWrapper<Ledger> updateWrapper = UpdateWrapper.of(entity);
        updateWrapper.set(LedgerNameRefs.ledger.status, targetStatus, true);
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.version, 1L);
        AssertUtils.isTrue(ledgerMapper.updateByQuery(entity, QueryWrapper.create()
                        .where(LedgerNameRefs.ledger.id.eq(request.getId()))
                        .and(LedgerNameRefs.ledger.version.eq(ledger.getVersion()))
                        .and(LedgerNameRefs.ledger.status.eq(ledger.getStatus()))) == 1,
                "账本状态更新失败");
    }

    @Override
    public void deleteLedgerByIds(@NonNull Long... ids) {
        AssertUtils.notEmpty(ids, "argument ids must not empty");
        int total = ledgerMapper.deleteBatchByIds(List.of(ids));
        AssertUtils.isTrue(total == ids.length, "删除账户账本失败");
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
                .and(ledger.status.eq(query.getStatus()))
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

    private void assertCloseableBalance(Ledger ledger, LedgerStatus targetStatus) {
        if (targetStatus != LedgerStatus.CLOSED) {
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
        if (entity.getStatus() == null) {
            entity.setStatus(LedgerStatus.ACTIVE);
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

    private void validateMinimumNormalBalance(Ledger ledger, UpdateLedgerBalanceRequest request) {
        Long minimumNormalBalance = request.getMinimumNormalBalance();
        if (minimumNormalBalance == null) {
            return;
        }
        long normalBalance = computeNormalBalance(
                addDelta(ledger.getDebitAmount(), request.getDebitAmountDelta()),
                addDelta(ledger.getCreditAmount(), request.getCreditAmountDelta()),
                ledger.getNormalBalanceSide()
        );
        AssertUtils.isTrue(normalBalance >= minimumNormalBalance, "账本余额不足");
    }

    private QueryColumn normalBalanceAfterDelta(EntrySide normalBalanceSide, UpdateLedgerBalanceRequest request) {
        QueryColumn debitAmount = amountAfterDelta(LedgerNameRefs.ledger.debitAmount, request.getDebitAmountDelta());
        QueryColumn creditAmount = amountAfterDelta(LedgerNameRefs.ledger.creditAmount, request.getCreditAmountDelta());
        if (normalBalanceSide == EntrySide.DEBIT) {
            return debitAmount.subtract(creditAmount);
        }
        return creditAmount.subtract(debitAmount);
    }

    private QueryColumn amountAfterDelta(QueryColumn fieldRef, Long delta) {
        if (delta == null || delta == 0L) {
            return fieldRef;
        }
        if (delta > 0) {
            return fieldRef.add(delta);
        }
        return fieldRef.subtract(Math.abs(delta));
    }

    private long addDelta(Long value, Long delta) {
        return (value == null ? 0L : value) + (delta == null ? 0L : delta);
    }

    private long computeNormalBalance(long debitAmount, long creditAmount, EntrySide normalBalanceSide) {
        long rawBalance = debitAmount - creditAmount;
        return normalBalanceSide == EntrySide.DEBIT ? rawBalance : -rawBalance;
    }
}

package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.locks.JdkLockFactory;
import com.wind.common.locks.LockFactory;
import com.wind.common.locks.WindLock;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.dal.entities.SpendSubjectFundingRel;
import com.wind.funds.wallet.dal.entities.table.CreditAccountNameRefs;
import com.wind.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.wind.funds.wallet.dal.entities.table.SpendSubjectFundingRelNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendSubjectFundingRelMapper;
import com.wind.funds.wallet.mapstruct.SpendSubjectFundingRelationConverter;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 支出主体资金关系服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class SpendSubjectFundingRelationServiceImpl implements SpendSubjectFundingRelationService {

    private static final String CREATE_LOCK_PREFIX = "funds:spend-subject-funding-relation:";

    private static final LockFactory LOCK_FACTORY = new JdkLockFactory();

    private final SpendSubjectFundingRelMapper spendSubjectFundingRelMapper;

    private final FundingAccountService fundingAccountService;

    private final CreditAccountService creditAccountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createSpendSubjectFundingRelation(
            @NonNull CreateSpendSubjectFundingRelationRequest request) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(request.getContextVariables());
        resolveAndValidateTargetSubject(request);
        assertValidityWindow(request);
        lockActiveRelationScope(request);
        assertNoDuplicateActiveDefaultRelation(request);
        assertNoDuplicateActivePriorityRelation(request);
        SpendSubjectFundingRel entity =
                SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRel(request);
        spendSubjectFundingRelMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支出主体资金关系失败");
        return entity.getId();
    }

    @Override
    public @NonNull SpendSubjectFundingRelationDTO getSpendSubjectFundingRelationById(@NonNull Long id) {
        SpendSubjectFundingRel result = spendSubjectFundingRelMapper.selectOneById(id);
        AssertUtils.notNull(result, "支出主体资金关系不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<SpendSubjectFundingRelationDTO> querySpendSubjectFundingRelations(
            @NonNull SpendSubjectFundingRelationQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.spendSubjectId.eq(query.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(query.getSpendSubjectType()))
                .and(ref.fundingAccountId.eq(query.getFundingAccountId()))
                .and(ref.targetSubjectType.eq(query.getTargetSubjectType()))
                .and(ref.targetSubjectId.eq(query.getTargetSubjectId()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.relationType.eq(query.getRelationType()))
                .and(ref.defaultRelation.eq(query.getDefaultRelation()))
                .and(ref.status.eq(query.getStatus()));
        applyCurrentEffectiveWindow(wrapper, ref, query.getStatus());
        applyActiveTargetSubjectStatus(wrapper, query);
        wrapper.orderBy(ref.priority.asc(), ref.id.asc());
        return MybatisQueryHelper.<SpendSubjectFundingRel, SpendSubjectFundingRelationDTO>query(wrapper)
                .counter(spendSubjectFundingRelMapper::selectCountByQuery)
                .resultQueryFunc(spendSubjectFundingRelMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private SpendSubjectFundingRelationDTO toDTO(SpendSubjectFundingRel entity) {
        return SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRelationDTO(entity);
    }

    private void resolveAndValidateTargetSubject(CreateSpendSubjectFundingRelationRequest request) {
        if (request.getTargetSubjectType() == null && StringUtils.hasText(request.getFundingAccountId())) {
            request.setTargetSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                    .setTargetSubjectId(request.getFundingAccountId());
        }
        AssertUtils.notNull(request.getTargetSubjectType(), "资金责任目标主体类型不能为空");
        AssertUtils.hasText(request.getTargetSubjectId(), "资金责任目标主体 ID 不能为空");
        AssertUtils.isTrue(request.getTargetSubjectType() == FundsSubjectType.FUNDING_ACCOUNT
                        || request.getTargetSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "资金责任目标主体类型不支持，targetSubjectType = {}", request.getTargetSubjectType());
        if (request.getTargetSubjectType() == FundsSubjectType.FUNDING_ACCOUNT) {
            resolveAndValidateFundingAccountTarget(request);
            return;
        }
        resolveAndValidateCreditAccountTarget(request);
    }

    private void resolveAndValidateFundingAccountTarget(CreateSpendSubjectFundingRelationRequest request) {
        if (StringUtils.hasText(request.getFundingAccountId())) {
            AssertUtils.equals(request.getFundingAccountId(), request.getTargetSubjectId(),
                    "资金账户目标主体必须与兼容 fundingAccountId 一致");
        } else {
            request.setFundingAccountId(request.getTargetSubjectId());
        }
        FundingAccountDTO fundingAccount =
                fundingAccountService.getFundingAccount(request.getTenantId(), request.getTargetSubjectId());
        assertFundingAccountCanBind(fundingAccount, request);
    }

    private void resolveAndValidateCreditAccountTarget(CreateSpendSubjectFundingRelationRequest request) {
        AssertUtils.isFalse(StringUtils.hasText(request.getFundingAccountId()),
                "信用账户目标主体不得同时填写 fundingAccountId");
        CreditAccountDTO creditAccount =
                creditAccountService.getCreditAccount(request.getTenantId(), request.getTargetSubjectId());
        AssertUtils.isTrue(creditAccount.getStatus().canDebit(),
                "资金责任目标主体不可用，targetSubjectType = {}, targetSubjectId = {}",
                request.getTargetSubjectType(), request.getTargetSubjectId());
        AssertUtils.equals(creditAccount.getCurrency(), request.getCurrency(),
                "资金责任目标主体币种与资金责任解析关系币种不一致，targetSubjectType = {}, targetSubjectId = {}",
                request.getTargetSubjectType(), request.getTargetSubjectId());
    }

    private void assertFundingAccountCanBind(FundingAccountDTO fundingAccount,
                                             CreateSpendSubjectFundingRelationRequest request) {
        AssertUtils.isTrue(fundingAccount.getStatus().canDebit(),
                "资金账户不可作为资金责任目标主体，fundingAccountId = {}", request.getTargetSubjectId());
        AssertUtils.equals(fundingAccount.getCurrency(), request.getCurrency(),
                "资金账户币种与资金责任解析关系币种不一致，fundingAccountId = {}", request.getTargetSubjectId());
    }

    private void assertValidityWindow(CreateSpendSubjectFundingRelationRequest request) {
        if (request.getValidFrom() == null || request.getValidTo() == null) {
            return;
        }
        AssertUtils.isTrue(request.getValidFrom().isBefore(request.getValidTo()),
                "资金责任解析关系生效时间必须早于失效时间");
    }

    private void lockActiveRelationScope(CreateSpendSubjectFundingRelationRequest request) {
        if (effectiveStatus(request) != FundsAccountStatus.ACTIVE) {
            return;
        }
        AssertUtils.notNull(request.getTenantId(), "资金责任解析关系 tenantId 不能为空");
        AssertUtils.notNull(request.getSpendSubjectType(), "资金责任解析关系 spendSubjectType 不能为空");
        AssertUtils.hasText(request.getSpendSubjectId(), "资金责任解析关系 spendSubjectId 不能为空");
        AssertUtils.notNull(request.getCurrency(), "资金责任解析关系 currency 不能为空");
        AssertUtils.notNull(request.getRelationType(), "资金责任解析关系 relationType 不能为空");
        // 本地串行化覆盖当前单应用写入；多节点同时写同一 scope 时再升级为数据库级排他约束。
        WindLock lock = LOCK_FACTORY.apply(CREATE_LOCK_PREFIX
                + request.getTenantId()
                + ":"
                + request.getSpendSubjectType().name()
                + ":"
                + request.getSpendSubjectId()
                + ":"
                + request.getCurrency().name()
                + ":"
                + request.getRelationType().name());
        lock.lock();
        boolean unlockImmediately = true;
        try {
            unlockImmediately = !registerTransactionCompletionUnlock(lock);
        } finally {
            if (unlockImmediately) {
                lock.unlock();
            }
        }
    }

    private static boolean registerTransactionCompletionUnlock(WindLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
        return true;
    }

    private void assertNoDuplicateActiveDefaultRelation(CreateSpendSubjectFundingRelationRequest request) {
        if (!Boolean.TRUE.equals(request.getDefaultRelation()) || effectiveStatus(request) != FundsAccountStatus.ACTIVE) {
            return;
        }
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(request.getTenantId()))
                .and(ref.spendSubjectId.eq(request.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(request.getSpendSubjectType()))
                .and(ref.currency.eq(request.getCurrency()))
                .and(ref.relationType.eq(request.getRelationType()))
                .and(ref.defaultRelation.eq(Boolean.TRUE))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = spendSubjectFundingRelMapper.selectListByQuery(wrapper).stream()
                .anyMatch(existing -> validityWindowsOverlap(existing.getValidFrom(),
                        existing.getValidTo(),
                        request.getValidFrom(),
                        request.getValidTo()));
        AssertUtils.isFalse(duplicated,
                "默认资金责任解析关系不唯一，spendSubjectId = {}, relationType = {}, currency = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency());
    }

    private void assertNoDuplicateActivePriorityRelation(CreateSpendSubjectFundingRelationRequest request) {
        if (effectiveStatus(request) != FundsAccountStatus.ACTIVE) {
            return;
        }
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        int priority = request.getPriority() == null ? 0 : request.getPriority();
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(request.getTenantId()))
                .and(ref.spendSubjectId.eq(request.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(request.getSpendSubjectType()))
                .and(ref.currency.eq(request.getCurrency()))
                .and(ref.relationType.eq(request.getRelationType()))
                .and(ref.priority.eq(priority))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = spendSubjectFundingRelMapper.selectListByQuery(wrapper).stream()
                .anyMatch(existing -> validityWindowsOverlap(existing.getValidFrom(),
                        existing.getValidTo(),
                        request.getValidFrom(),
                        request.getValidTo()));
        AssertUtils.isFalse(duplicated,
                "资金责任解析关系优先级冲突，spendSubjectId = {}, relationType = {}, currency = {}, priority = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency(),
                priority);
    }

    private FundsAccountStatus effectiveStatus(CreateSpendSubjectFundingRelationRequest request) {
        return request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus();
    }

    private void applyCurrentEffectiveWindow(QueryWrapper wrapper,
                                             SpendSubjectFundingRelNameRefs ref,
                                             FundsAccountStatus status) {
        if (status != FundsAccountStatus.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(ref.validFrom.isNull().or(ref.validFrom.le(now)))
                .and(ref.validTo.isNull().or(ref.validTo.gt(now)));
    }

    private void applyActiveTargetSubjectStatus(QueryWrapper wrapper, SpendSubjectFundingRelationQuery query) {
        if (query.getStatus() != FundsAccountStatus.ACTIVE) {
            return;
        }
        QueryCondition activeFundingAccountTarget = activeFundingAccountTargetCondition(query);
        QueryCondition activeCreditAccountTarget = activeCreditAccountTargetCondition(query);
        if (query.getTargetSubjectType() == FundsSubjectType.FUNDING_ACCOUNT) {
            wrapper.and(activeFundingAccountTarget);
            return;
        }
        if (query.getTargetSubjectType() == FundsSubjectType.CREDIT_ACCOUNT) {
            wrapper.and(activeCreditAccountTarget);
            return;
        }
        wrapper.and(activeFundingAccountTarget.or(activeCreditAccountTarget));
    }

    private QueryCondition activeFundingAccountTargetCondition(SpendSubjectFundingRelationQuery query) {
        FundingAccountNameRefs accountRef = FundingAccountNameRefs.fundingAccount;
        QueryWrapper activeFundingAccount = QueryWrapper.create()
                .select(accountRef.sn)
                .from(accountRef)
                .where(accountRef.tenantId.eq(query.getTenantId()))
                .and(accountRef.status.eq(FundsAccountStatus.ACTIVE));
        SpendSubjectFundingRelNameRefs relationRef = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        return relationRef.targetSubjectType.eq(FundsSubjectType.FUNDING_ACCOUNT)
                .and(relationRef.targetSubjectId.in(activeFundingAccount));
    }

    private QueryCondition activeCreditAccountTargetCondition(SpendSubjectFundingRelationQuery query) {
        CreditAccountNameRefs accountRef = CreditAccountNameRefs.creditAccount;
        QueryWrapper activeCreditAccount = QueryWrapper.create()
                .select(accountRef.sn)
                .from(accountRef)
                .where(accountRef.tenantId.eq(query.getTenantId()))
                .and(accountRef.status.eq(FundsAccountStatus.ACTIVE));
        SpendSubjectFundingRelNameRefs relationRef = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        return relationRef.targetSubjectType.eq(FundsSubjectType.CREDIT_ACCOUNT)
                .and(relationRef.targetSubjectId.in(activeCreditAccount));
    }

    private boolean validityWindowsOverlap(LocalDateTime leftFrom,
                                           LocalDateTime leftTo,
                                           LocalDateTime rightFrom,
                                           LocalDateTime rightTo) {
        boolean leftEndsAfterRightStarts = leftTo == null || rightFrom == null || leftTo.isAfter(rightFrom);
        boolean rightEndsAfterLeftStarts = rightTo == null || leftFrom == null || rightTo.isAfter(leftFrom);
        return leftEndsAfterRightStarts && rightEndsAfterLeftStarts;
    }
}

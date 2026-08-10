package com.wind.funds.ledger;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dto.LedgerTransactionPostResult;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerStatus;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账本交易服务
 *
 * @author wuxp
 * @date 2026-04-14 14:36
 **/
@AllArgsConstructor
@Slf4j
@Component
public class DefaultLedgerTransactionPostingServiceImpl implements LedgerTransactionPostingService {

    private final LedgerTransactionCommandService ledgerTransactionCommandService;

    private final LedgerService ledgerService;

    private final List<LedgerBalanceProjectionService> ledgerBalanceProjectionServices;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public void post(@NonNull LedgerTransactionSpec transaction) {
        assertTransactionPostable(transaction);
        assertAllPostingPlansHaveEntries(transaction);
        assertAllEntriesUsePositiveAmounts(transaction);
        assertAllPostingPlansUseSingleCurrency(transaction);
        assertTransactionCurrencyMatchesPostingPlans(transaction);
        assertAllEntriesUsePostableSubjects(transaction);
        AssertUtils.isTrue(transaction.isBalanced(), "账本交易借记、贷记金额不一致");
        assertAllPostingPlansBalanced(transaction);
        assertAllEntriesBoundToLedgers(transaction);
        Map<Long, LedgerDTO> boundLedgers = assertAllEntriesMatchBoundLedgers(transaction);
        assertAllLedgerBalanceConstraintsSatisfied(transaction, boundLedgers);

        // 按照账务主体分组更新余额
        Map<ProjectionGroupKey, List<LedgerEntrySpec>> groups = groupProjectionEntries(transaction);
        Map<FundsAccountId, LedgerBalanceProjectionService> projectionServices = resolveProjectionServices(groups.keySet()
                .stream()
                .map(ProjectionGroupKey::accountId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        LedgerTransactionPostResult postResult = ledgerTransactionCommandService.postLedgerTransaction(transaction);
        if (!postResult.isNewlyPosted()) {
            log.info("账本交易已存在，跳过重复入账和余额投影，ledgerTransactionSn={}, fundsTransactionSn={}, "
                            + "businessScene={}, businessSn={}",
                    transaction.getSn(), transaction.getFundsTransactionSn(), transaction.getBusinessScene(),
                    transaction.getBusinessSn());
            return;
        }
        for (Map.Entry<ProjectionGroupKey, List<LedgerEntrySpec>> entry : groups.entrySet()) {
            FundsAccountId accountId = entry.getKey().accountId();
            List<LedgerEntrySpec> entries = entry.getValue();
            projectionServices.get(accountId).project(entries, entry.getKey().postingAccessType());
        }
        logAfterCommit(() -> log.info("账本交易入账完成，ledgerTransactionSn={}, fundsTransactionSn={}, eventType={}, "
                        + "businessScene={}, businessSn={}, amount={}, currency={}, postingPlanCount={}, subjectCount={}",
                transaction.getSn(), transaction.getFundsTransactionSn(), transaction.getEventType(),
                transaction.getBusinessScene(), transaction.getBusinessSn(), transaction.getAmount().getAmount(),
                transaction.getCurrency(), transaction.getPostingPlans().size(), projectionServices.size()));
    }

    private void logAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void assertTransactionPostable(LedgerTransactionSpec transaction) {
        AssertUtils.notNull(transaction, "账本交易不能为空");
        AssertUtils.hasText(transaction.getSn(), "账本交易流水号不能为空");
        AssertUtils.notNull(transaction.getAmount(),
                "账本交易金额不能为空，ledgerTransactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(transaction.getAmount().getAmount() > 0,
                "账本交易金额必须大于 0，ledgerTransactionSn = {}, amount = {}",
                transaction.getSn(),
                transaction.getAmount().getAmount());
        AssertUtils.notNull(transaction.getOriginalAmount(),
                "账本交易原始金额不能为空，ledgerTransactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(transaction.getOriginalAmount().getAmount() > 0,
                "账本交易原始金额必须大于 0，ledgerTransactionSn = {}, originalAmount = {}",
                transaction.getSn(),
                transaction.getOriginalAmount().getAmount());
        AssertUtils.notNull(transaction.getExchangeRate(),
                "账本交易汇率不能为空，ledgerTransactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(transaction.getExchangeRate().compareTo(BigDecimal.ZERO) > 0,
                "账本交易汇率必须大于 0，ledgerTransactionSn = {}, exchangeRate = {}",
                transaction.getSn(),
                transaction.getExchangeRate());
        AssertUtils.notEmpty(transaction.getPostingPlans(),
                "账本交易 postingPlans 不能为空，ledgerTransactionSn = {}", transaction.getSn());
    }

    private void assertAllPostingPlansHaveEntries(LedgerTransactionSpec transaction) {
        transaction.getPostingPlans().forEach(plan -> {
            AssertUtils.notNull(plan, "账务计划不能为空，ledgerTransactionSn = {}", transaction.getSn());
            AssertUtils.hasText(plan.getPlanId(),
                    "账务计划流水号不能为空，ledgerTransactionSn = {}", transaction.getSn());
            AssertUtils.hasText(plan.getLedgerTransactionSn(),
                    "账务计划交易流水不能为空，planId = {}, ledgerTransactionSn = {}",
                    plan.getPlanId(),
                    transaction.getSn());
            AssertUtils.isTrue(Objects.equals(transaction.getSn(), plan.getLedgerTransactionSn()),
                    "账务计划交易流水与账本交易流水不一致，planId = {}, ledgerTransactionSn = {}, planLedgerTransactionSn = {}",
                    plan.getPlanId(),
                    transaction.getSn(),
                    plan.getLedgerTransactionSn());
            AssertUtils.notEmpty(plan.getEntries(),
                    "账务计划 entries 不能为空，planId = {}, ledgerTransactionSn = {}",
                    plan.getPlanId(),
                    plan.getLedgerTransactionSn());
            plan.getEntries().forEach(entry -> {
                AssertUtils.notNull(entry,
                        "账本分录不能为空，planId = {}, ledgerTransactionSn = {}",
                        plan.getPlanId(),
                        plan.getLedgerTransactionSn());
                AssertUtils.hasText(entry.getLedgerTransactionSn(),
                        "账本分录交易流水不能为空，planId = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                        plan.getPlanId(),
                        entry.getSubjectId(),
                        entry.getSubjectType(),
                        entry.getLedgerSubjectCode());
                AssertUtils.isTrue(Objects.equals(transaction.getSn(), entry.getLedgerTransactionSn()),
                        "账本分录交易流水与账本交易流水不一致，planId = {}, ledgerTransactionSn = {}, entryLedgerTransactionSn = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                        plan.getPlanId(),
                        transaction.getSn(),
                        entry.getLedgerTransactionSn(),
                        entry.getSubjectId(),
                        entry.getSubjectType(),
                        entry.getLedgerSubjectCode());
            });
        });
    }

    private void assertAllPostingPlansUseSingleCurrency(LedgerTransactionSpec transaction) {
        transaction.getPostingPlans().forEach(plan -> {
            Set<CurrencyIsoCode> currencies = plan.getEntries()
                    .stream()
                    .map(LedgerEntrySpec::getCurrency)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            AssertUtils.isTrue(currencies.size() == 1,
                    "记账计划币种不一致，planId = {}, currencies = {}", plan.getPlanId(), currencies);
        });
    }

    private void assertTransactionCurrencyMatchesPostingPlans(LedgerTransactionSpec transaction) {
        Set<CurrencyIsoCode> currencies = transaction.getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .map(LedgerEntrySpec::getCurrency)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        AssertUtils.isTrue(currencies.size() == 1 && currencies.contains(transaction.getCurrency()),
                "账本交易币种与记账计划币种不一致，ledgerTransactionSn = {}, transactionCurrency = {}, postingPlanCurrencies = {}",
                transaction.getSn(),
                transaction.getCurrency(),
                currencies);
    }

    private void assertAllPostingPlansBalanced(LedgerTransactionSpec transaction) {
        transaction.getPostingPlans().forEach(plan -> AssertUtils.isTrue(
                plan.isBalanced(), "账务计划不平衡，planId = {}", plan.getPlanId()));
    }

    private void assertAllEntriesUsePositiveAmounts(LedgerTransactionSpec transaction) {
        transaction.getPostingPlans().forEach(plan -> plan.getEntries().forEach(entry -> {
            AssertUtils.notNull(entry.getAmount(),
                    "账本分录金额不能为空，planId = {}, ledgerTransactionSn = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                    plan.getPlanId(),
                    entry.getLedgerTransactionSn(),
                    entry.getSubjectId(),
                    entry.getSubjectType(),
                    entry.getLedgerSubjectCode());
            AssertUtils.isTrue(entry.getAmount().getAmount() > 0,
                    "账本分录金额必须大于 0，planId = {}, ledgerTransactionSn = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, amount = {}",
                    plan.getPlanId(),
                    entry.getLedgerTransactionSn(),
                    entry.getSubjectId(),
                    entry.getSubjectType(),
                    entry.getLedgerSubjectCode(),
                    entry.getAmount().getAmount());
        }));
    }

    private void assertAllEntriesUsePostableSubjects(LedgerTransactionSpec transaction) {
        transaction.getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .forEach(entry -> AssertUtils.isTrue(isPostableEntry(entry),
                        "账本分录主体类型不允许入账，ledgerTransactionSn = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                        entry.getLedgerTransactionSn(),
                        entry.getSubjectId(),
                        entry.getSubjectType(),
                        entry.getLedgerSubjectCode()));
    }

    private boolean isPostableEntry(LedgerEntrySpec entry) {
        return FundsSubjectType.isLedgerPostableName(entry.getSubjectType());
    }

    private void assertAllEntriesBoundToLedgers(LedgerTransactionSpec transaction) {
        transaction.getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .forEach(entry -> AssertUtils.notNull(entry.getLedgerId(),
                        "账本分录 ledgerId 不能为空，ledgerTransactionSn = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                        entry.getLedgerTransactionSn(),
                        entry.getSubjectId(),
                        entry.getSubjectType(),
                        entry.getLedgerSubjectCode()));
    }

    private Map<Long, LedgerDTO> assertAllEntriesMatchBoundLedgers(LedgerTransactionSpec transaction) {
        List<LedgerEntrySpec> entries = transaction.getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .toList();
        Set<Long> ledgerIds = entries.stream()
                .map(LedgerEntrySpec::getLedgerId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, LedgerDTO> ledgers = ledgerService.getLedgerByIds(ledgerIds)
                .stream()
                .collect(Collectors.toMap(LedgerDTO::getId, ledger -> ledger));
        transaction.getPostingPlans().forEach(plan -> plan.getEntries().forEach(entry -> {
            LedgerDTO ledger = ledgers.get(entry.getLedgerId());
            AssertUtils.notNull(ledger, "账户账本不存在，ledgerId = {}", entry.getLedgerId());
            LedgerStatus.assertPostable(ledger.getId(), ledger.getStatus(), plan.getPostingAccessType());
            assertEntryMatchesLedger(entry, ledger);
        }));
        return ledgers;
    }

    private void assertEntryMatchesLedger(LedgerEntrySpec entry, LedgerDTO ledger) {
        AssertUtils.isTrue(Objects.equals(ledger.getSubjectId(), entry.getSubjectId())
                        && Objects.equals(ledger.getSubjectType(), entry.getSubjectType()),
                "账本分录主体与账本主体不一致，ledgerId = {}", ledger.getId());
        AssertUtils.isTrue(ledger.getLedgerSubjectCode() == entry.getLedgerSubjectCode()
                        && ledger.getLedgerSubjectCategory() == entry.getLedgerSubjectCategory(),
                "账本分录科目与账本科目不一致，ledgerId = {}", ledger.getId());
        LedgerNormalBalanceGuard.assertCategoryNormalBalance(
                "入账",
                ledger.getId(),
                ledger.getLedgerSubjectCategory(),
                ledger.getNormalBalanceSide());
        AssertUtils.isTrue(ledger.getCurrency() == entry.getCurrency(),
                "账本分录币种与账本币种不一致，ledgerId = {}", ledger.getId());
        AssertUtils.isTrue(ledger.getPeriodType() == entry.getPeriodType()
                        && Objects.equals(ledger.getPeriodId(), entry.getPeriodId()),
                "账本分录周期与账本周期不一致，ledgerId = {}", ledger.getId());
        AssertUtils.isTrue(entry.getBalanceConstraintType() != LedgerBalanceConstraintType.ALLOW_NEGATIVE
                        || Boolean.TRUE.equals(ledger.getAllowNegative()),
                "账本 profile 不允许负余额，ledgerId = {}, ledgerSubjectCode = {}",
                ledger.getId(),
                ledger.getLedgerSubjectCode());
    }

    private void assertAllLedgerBalanceConstraintsSatisfied(LedgerTransactionSpec transaction,
                                                            Map<Long, LedgerDTO> ledgers) {
        transaction.getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(LedgerEntrySpec::getLedgerId, LinkedHashMap::new, Collectors.toList()))
                .forEach((ledgerId, entries) -> {
                    LedgerDTO ledger = ledgers.get(ledgerId);
                    Long minimumNormalBalance = resolveMinimumNormalBalance(ledger, entries);
                    if (minimumNormalBalance == null) {
                        return;
                    }
                    long normalBalanceDelta = computeBalanceDelta(entries, ledger.getNormalBalanceSide());
                    long beforeBalance = ledger.getNormalBalance();
                    AssertUtils.isTrue(beforeBalance >= minimumNormalBalance,
                            "账本余额不允许低于下限，ledgerId = {}, subjectId = {}, subjectType = {}, "
                                    + "ledgerSubjectCode = {}, beforeBalance = {}, minimumNormalBalance = {}",
                            ledger.getId(),
                            ledger.getSubjectId(),
                            ledger.getSubjectType(),
                            ledger.getLedgerSubjectCode(),
                            beforeBalance,
                            minimumNormalBalance);
                    long afterBalance = beforeBalance + normalBalanceDelta;
                    if (afterBalance < minimumNormalBalance) {
                        throw new LedgerPostingRejectedException(
                                transaction.getFundsTransactionSn(),
                                ("账本余额不足，ledgerId = %s, subjectId = %s, subjectType = %s, ledgerSubjectCode = %s, "
                                        + "beforeBalance = %s, balanceDelta = %s, afterBalance = %s, "
                                        + "minimumNormalBalance = %s")
                                        .formatted(ledger.getId(), ledger.getSubjectId(), ledger.getSubjectType(),
                                                ledger.getLedgerSubjectCode(), beforeBalance, normalBalanceDelta,
                                                afterBalance, minimumNormalBalance));
                    }
                });
    }

    private Long resolveMinimumNormalBalance(LedgerDTO ledger, List<LedgerEntrySpec> entries) {
        if (!Boolean.TRUE.equals(ledger.getAllowNegative())) {
            return 0L;
        }
        boolean mustNotBeNegative = entries.stream()
                .map(this::resolveConstraintType)
                .anyMatch(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE::equals);
        return mustNotBeNegative ? 0L : null;
    }

    private LedgerBalanceConstraintType resolveConstraintType(LedgerEntrySpec entry) {
        LedgerBalanceConstraintType value = entry.getBalanceConstraintType();
        if (value == null) {
            return LedgerBalanceConstraintType.PROFILE_DEFAULT;
        }
        return value;
    }

    private long computeBalanceDelta(List<LedgerEntrySpec> entries, EntrySide normalBalanceSide) {
        AssertUtils.notNull(normalBalanceSide, "账本正常余额方向不能为空");
        long debitAmountDelta = 0L;
        long creditAmountDelta = 0L;
        for (LedgerEntrySpec entry : entries) {
            long amount = entry.getAmount().getAmount();
            if (entry.getEntryType() == EntrySide.DEBIT) {
                debitAmountDelta += amount;
            } else {
                creditAmountDelta += amount;
            }
        }
        long rawDelta = debitAmountDelta - creditAmountDelta;
        return normalBalanceSide == EntrySide.DEBIT ? rawDelta : -rawDelta;
    }

    private Map<ProjectionGroupKey, List<LedgerEntrySpec>> groupProjectionEntries(LedgerTransactionSpec transaction) {
        Map<ProjectionGroupKey, List<LedgerEntrySpec>> result = new LinkedHashMap<>();
        for (LedgerPostingPlanSpec plan : transaction.getPostingPlans()) {
            LedgerPostingAccessType postingAccessType = plan.getPostingAccessType();
            AssertUtils.notNull(postingAccessType,
                    "账务计划入账准入类型不能为空，planId = {}", plan.getPlanId());
            for (LedgerEntrySpec entry : plan.getEntries()) {
                FundsAccountId accountId = FundsAccountId.immutable(entry.getSubjectId(), entry.getSubjectType());
                ProjectionGroupKey key = new ProjectionGroupKey(accountId, postingAccessType);
                result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
            }
        }
        return result;
    }

    private Map<FundsAccountId, LedgerBalanceProjectionService> resolveProjectionServices(
            Collection<FundsAccountId> accountIds) {
        Map<FundsAccountId, LedgerBalanceProjectionService> result = new LinkedHashMap<>();
        for (FundsAccountId accountId : accountIds) {
            List<LedgerBalanceProjectionService> supported = ledgerBalanceProjectionServices.stream()
                    .filter(delegate -> delegate.supports(accountId))
                    .toList();
            AssertUtils.notEmpty(supported, "未找到支持的账本余额投影服务，accountId = {}", accountId);
            AssertUtils.isTrue(supported.size() == 1, "账本余额投影服务不唯一，accountId = {}", accountId);
            result.put(accountId, supported.getFirst());
        }
        return result;
    }

    private record ProjectionGroupKey(FundsAccountId accountId, LedgerPostingAccessType postingAccessType) {
    }

}

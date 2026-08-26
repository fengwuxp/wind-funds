package com.wind.funds.ledger.impl;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.wind.common.exception.AssertUtils;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.funds.ledger.LedgerNormalBalanceGuard;
import com.wind.funds.ledger.dal.entities.Ledger;
import com.wind.funds.ledger.dal.entities.table.LedgerNameRefs;
import com.wind.funds.ledger.dal.mapper.LedgerMapper;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerState;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.transaction.support.FundsInstructionContextValidator;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 账本余额投影服务实现
 *
 * @author wuxp
 * @date 2026-04-23 15:39
 **/
@AllArgsConstructor
@Component
@Slf4j
public class LedgerBalanceProjectionServiceImpl {

    private final LedgerService ledgerService;

    private final LedgerMapper ledgerMapper;

    @Transactional(rollbackFor = Exception.class)
    public void project(@NonNull List<LedgerEntrySpec> entries, @NonNull LedgerPostingAccessType postingAccessType) {
        if (entries.isEmpty()) {
            return;
        }
        AssertUtils.notNull(postingAccessType, "账本入账准入类型不能为空");
        assertNoCoreBenefitContextVariables(entries);
        FundsAccountId accountId = requireSingleFundsAccount(entries);
        // 按照 ledger_id 分组，避免同一科目在不同周期账本间串账。
        Map<Long, List<LedgerEntrySpec>> groups = entries.stream()
                .collect(Collectors.groupingBy(this::requireLedgerId, LinkedHashMap::new, Collectors.toList()));
        List<ProjectionCommand> commands = groups.entrySet().stream()
                .map(entry -> prepareProjectionCommand(entry.getKey(), entry.getValue()))
                .toList();
        commands.forEach(command -> {
            LedgerDTO ledger = command.ledger();
            ProjectionDelta delta = command.delta();
            applyProjectionDelta(
                    ledger,
                    delta,
                    resolveMinimumNormalBalance(ledger, command.entries()),
                    postingAccessType);
            publishBalanceChangedEvents(accountId, ledger, command.entries(), command.beforeBalanceAmount(), delta);
        });
    }

    private void applyProjectionDelta(LedgerDTO ledger,
                                      ProjectionDelta delta,
                                      Long minimumNormalBalance,
                                      LedgerPostingAccessType postingAccessType) {
        LedgerState.assertPostable(ledger.getId(), ledger.getState(), postingAccessType);
        validateMinimumNormalBalance(ledger, delta, minimumNormalBalance);
        Ledger entity = UpdateEntity.of(Ledger.class);
        UpdateWrapper<Ledger> updateWrapper = UpdateWrapper.of(entity);
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.debitAmount, delta.debitAmountDelta());
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.creditAmount, delta.creditAmountDelta());
        setRawDelta(updateWrapper, LedgerNameRefs.ledger.version, 1L);
        QueryWrapper where = QueryWrapper.create()
                .where(LedgerNameRefs.ledger.id.eq(ledger.getId()))
                .and(LedgerNameRefs.ledger.version.eq(ledger.getVersion()))
                .and(LedgerNameRefs.ledger.state.eq(ledger.getState()));
        if (minimumNormalBalance != null) {
            where.and(normalBalanceAfterDelta(ledger.getNormalBalanceSide(), delta).ge(minimumNormalBalance));
        }
        AssertUtils.isTrue(ledgerMapper.updateByQuery(entity, where) > 0, "账本余额更新失败");
    }

    private void setRawDelta(UpdateWrapper<Ledger> updateWrapper, QueryColumn fieldRef, long delta) {
        if (delta == 0L) {
            return;
        }
        if (delta > 0) {
            updateWrapper.setRaw(fieldRef, fieldRef.add(delta));
            return;
        }
        updateWrapper.setRaw(fieldRef, fieldRef.subtract(Math.abs(delta)));
    }

    private void validateMinimumNormalBalance(LedgerDTO ledger,
                                              ProjectionDelta delta,
                                              Long minimumNormalBalance) {
        if (minimumNormalBalance == null) {
            return;
        }
        long normalBalance = computeNormalBalance(
                ledger.getDebitAmount() + delta.debitAmountDelta(),
                ledger.getCreditAmount() + delta.creditAmountDelta(),
                ledger.getNormalBalanceSide());
        AssertUtils.isTrue(normalBalance >= minimumNormalBalance, "账本余额不足");
    }

    private QueryColumn normalBalanceAfterDelta(EntrySide normalBalanceSide, ProjectionDelta delta) {
        QueryColumn debitAmount = amountAfterDelta(LedgerNameRefs.ledger.debitAmount, delta.debitAmountDelta());
        QueryColumn creditAmount = amountAfterDelta(LedgerNameRefs.ledger.creditAmount, delta.creditAmountDelta());
        return normalBalanceSide == EntrySide.DEBIT
                ? debitAmount.subtract(creditAmount)
                : creditAmount.subtract(debitAmount);
    }

    private QueryColumn amountAfterDelta(QueryColumn fieldRef, long delta) {
        if (delta == 0L) {
            return fieldRef;
        }
        return delta > 0 ? fieldRef.add(delta) : fieldRef.subtract(Math.abs(delta));
    }

    private long computeNormalBalance(long debitAmount, long creditAmount, EntrySide normalBalanceSide) {
        long rawBalance = debitAmount - creditAmount;
        return normalBalanceSide == EntrySide.DEBIT ? rawBalance : -rawBalance;
    }

    private ProjectionCommand prepareProjectionCommand(Long ledgerId,
                                                       List<LedgerEntrySpec> entries) {
        LedgerDTO ledger = ledgerService.getLedgerById(ledgerId);
        AssertUtils.notNull(ledger, "账本不存在，ledgerId = {}", ledgerId);
        assertEntriesMatchLedger(ledger, entries);
        ProjectionDelta delta = computeProjectionDelta(entries, ledger.getNormalBalanceSide());
        Money beforeBalanceAmount = Money.immutable(ledger.getNormalBalance(), ledger.getCurrency());
        assertMustNotBeNegativeBalance(ledger, entries, beforeBalanceAmount, delta);
        return new ProjectionCommand(ledger, entries, beforeBalanceAmount, delta);
    }

    private void publishBalanceChangedEvents(FundsAccountId accountId,
                                             LedgerDTO ledger,
                                             List<LedgerEntrySpec> entries,
                                             Money beforeBalance,
                                             ProjectionDelta delta) {
        long previousBalance = beforeBalance.getAmount();
        for (LedgerEntrySpec entry : entries) {
            long entryBalanceDelta = balanceDelta(entry, ledger.getNormalBalanceSide());
            long currentBalance = previousBalance + entryBalanceDelta;
            publishBalanceChangedEvent(accountId, ledger, entry, previousBalance, currentBalance, entryBalanceDelta);
            previousBalance = currentBalance;
        }
        AssertUtils.isTrue(previousBalance == beforeBalance.getAmount() + delta.balanceDelta(),
                "余额变更事件累计值与余额投影变更不一致，ledgerId = {}", ledger.getId());
    }

    private void publishBalanceChangedEvent(FundsAccountId accountId,
                                            LedgerDTO ledger,
                                            LedgerEntrySpec entry,
                                            long beforeBalance,
                                            long currentBalance,
                                            long balanceDelta) {
        LedgerBalanceChangedEvent event = LedgerBalanceChangedEvent.builder()
                    .subjectId(accountId.id())
                    .subjectType(accountId.type())
                    .ledgerId(ledger.getId())
                    .ledgerSubjectCode(ledger.getLedgerSubjectCode())
                    .currency(entry.getCurrency())
                    .beforeBalance(beforeBalance)
                    .balance(currentBalance)
                    .balanceDelta(balanceDelta)
                    .ledgerTransactionSn(entry.getLedgerTransactionSn())
                    .ledgerEntrySn(resolveLedgerEntrySn(entry))
                    .ledgerEntryDigest(entry.getSha256())
                    .businessScene(entry.getBusinessScene())
                    .businessSn(entry.getBusinessSn())
                    .transactionTime(entry.getTransactionTime())
                    .contextVariables(entry.getContextVariables())
                    .build();
        publishBalanceChangedEvent(event, entry, ledger, accountId);
    }

    private void assertNoCoreBenefitContextVariables(List<LedgerEntrySpec> entries) {
        entries.forEach(entry -> FundsInstructionContextValidator.immutableInstructionContext(
                entry.getContextVariables(),
                "ledgerBalanceProjection.entry"));
    }

    private void publishBalanceChangedEvent(LedgerBalanceChangedEvent event,
                                            LedgerEntrySpec entry,
                                            LedgerDTO ledger,
                                            FundsAccountId accountId) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()
                    && TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        publishBalanceChangedEventImmediately(event, entry, ledger, accountId);
                    }
                });
                return;
            }
            publishBalanceChangedEventImmediately(event, entry, ledger, accountId);
        } catch (RuntimeException ex) {
            logBalanceChangedEventFailure(entry, ledger, accountId, ex);
        }
    }

    private void publishBalanceChangedEventImmediately(LedgerBalanceChangedEvent event,
                                                       LedgerEntrySpec entry,
                                                       LedgerDTO ledger,
                                                       FundsAccountId accountId) {
        try {
            SpringEventPublishUtils.publishEvent(event);
        } catch (RuntimeException ex) {
            logBalanceChangedEventFailure(entry, ledger, accountId, ex);
        }
    }

    private void logBalanceChangedEventFailure(LedgerEntrySpec entry,
                                               LedgerDTO ledger,
                                               FundsAccountId accountId,
                                               RuntimeException ex) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        log.warn("发布账本余额变更观察事件失败，ledgerTransactionSn = {}, ledgerId = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, cause = {}",
                entry.getLedgerTransactionSn(),
                ledger.getId(),
                accountId.id(),
                accountId.type(),
                ledger.getLedgerSubjectCode(),
                cause.toString());
    }

    private FundsAccountId requireSingleFundsAccount(List<LedgerEntrySpec> entries) {
        LedgerEntrySpec firstEntry = entries.getFirst();
        FundsAccountId accountId = FundsAccountId.immutable(firstEntry.getSubjectId(), firstEntry.getSubjectType());
        boolean allSameAccount = entries.stream()
                .allMatch(entry -> Objects.equals(accountId.id(), entry.getSubjectId())
                        && Objects.equals(accountId.type(), entry.getSubjectType()));
        AssertUtils.isTrue(allSameAccount, "账本余额投影只允许处理同一资金账户的分录");
        return accountId;
    }

    private void assertEntriesMatchLedger(LedgerDTO ledger, List<LedgerEntrySpec> entries) {
        entries.forEach(entry -> {
            AssertUtils.isTrue(Objects.equals(ledger.getSubjectId(), entry.getSubjectId())
                            && Objects.equals(ledger.getSubjectType(), entry.getSubjectType()),
                    "账本分录主体与账本主体不一致，ledgerId = {}", ledger.getId());
            AssertUtils.isTrue(ledger.getLedgerSubjectCode() == entry.getLedgerSubjectCode()
                            && ledger.getLedgerSubjectCategory() == entry.getLedgerSubjectCategory(),
                    "账本分录科目与账本科目不一致，ledgerId = {}", ledger.getId());
            LedgerNormalBalanceGuard.assertCategoryNormalBalance(
                    "余额投影",
                    ledger.getId(),
                    ledger.getLedgerSubjectCategory(),
                    ledger.getNormalBalanceSide());
            AssertUtils.isTrue(ledger.getCurrency() == entry.getCurrency(),
                    "账本分录币种与账本币种不一致，ledgerId = {}", ledger.getId());
            AssertUtils.isTrue(entry.getBalanceConstraintType() != LedgerBalanceConstraintType.ALLOW_NEGATIVE
                            || Boolean.TRUE.equals(ledger.getAllowNegative()),
                    "账本 profile 不允许负余额，ledgerId = {}, ledgerSubjectCode = {}",
                    ledger.getId(),
                    ledger.getLedgerSubjectCode());
        });
    }

    private Long requireLedgerId(LedgerEntrySpec entry) {
        Long ledgerId = entry.getLedgerId();
        AssertUtils.notNull(ledgerId, "账本分录 ledgerId 不能为空");
        return ledgerId;
    }

    private ProjectionDelta computeProjectionDelta(List<LedgerEntrySpec> entries, EntrySide normalBalanceSide) {
        AssertUtils.notNull(normalBalanceSide, "账本正常余额方向不能为空");
        long debitAmountDelta = 0L;
        long creditAmountDelta = 0L;
        for (LedgerEntrySpec entry : entries) {
            EntrySide direction = entry.getEntryType();
            long amount = entry.getAmount().getAmount();
            if (direction == EntrySide.DEBIT) {
                debitAmountDelta += amount;
            } else {
                creditAmountDelta += amount;
            }
        }
        long rawDelta = debitAmountDelta - creditAmountDelta;
        long balanceDelta = normalBalanceSide == EntrySide.DEBIT ? rawDelta : -rawDelta;
        return new ProjectionDelta(balanceDelta, debitAmountDelta, creditAmountDelta);
    }

    private long balanceDelta(LedgerEntrySpec entry, EntrySide normalBalanceSide) {
        long signedAmount = entry.getEntryType() == EntrySide.DEBIT
                ? entry.getAmount().getAmount()
                : -entry.getAmount().getAmount();
        return normalBalanceSide == EntrySide.DEBIT ? signedAmount : -signedAmount;
    }

    private String resolveLedgerEntrySn(LedgerEntrySpec entry) {
        Object value = entry.getContextVariables().get("ledgerEntrySn");
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
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

    private void assertMustNotBeNegativeBalance(LedgerDTO ledger,
                                                List<LedgerEntrySpec> entries,
                                                Money beforeBalanceAmount,
                                                ProjectionDelta delta) {
        if (!containsMustNotBeNegativeConstraint(entries)) {
            return;
        }
        long beforeAmount = beforeBalanceAmount.getAmount();
        AssertUtils.isTrue(beforeAmount >= 0,
                "账本余额不允许为负，ledgerId = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, beforeBalance = {}",
                ledger.getId(),
                ledger.getSubjectId(),
                ledger.getSubjectType(),
                ledger.getLedgerSubjectCode(),
                beforeAmount);
        long afterAmount = beforeAmount + delta.balanceDelta();
        AssertUtils.isTrue(afterAmount >= 0,
                "账本余额不足，ledgerId = {}, subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, beforeBalance = {}, balanceDelta = {}, afterBalance = {}",
                ledger.getId(),
                ledger.getSubjectId(),
                ledger.getSubjectType(),
                ledger.getLedgerSubjectCode(),
                beforeAmount,
                delta.balanceDelta(),
                afterAmount);
    }

    private boolean containsMustNotBeNegativeConstraint(List<LedgerEntrySpec> entries) {
        return entries.stream()
                .map(this::resolveConstraintType)
                .anyMatch(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE::equals);
    }

    private LedgerBalanceConstraintType resolveConstraintType(LedgerEntrySpec entry) {
        LedgerBalanceConstraintType value = entry.getBalanceConstraintType();
        if (value == null) {
            return LedgerBalanceConstraintType.PROFILE_DEFAULT;
        }
        return value;
    }


    private record ProjectionDelta(long balanceDelta, long debitAmountDelta, long creditAmountDelta) {
    }

    private record ProjectionCommand(LedgerDTO ledger,
                                     List<LedgerEntrySpec> entries,
                                     Money beforeBalanceAmount,
                                     ProjectionDelta delta) {
    }

}

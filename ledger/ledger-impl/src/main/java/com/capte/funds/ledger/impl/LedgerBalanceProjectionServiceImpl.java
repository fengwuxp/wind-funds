package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.wind.common.exception.AssertUtils;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.integration.funds.ledger.LedgerBalanceProjectionService;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
public class LedgerBalanceProjectionServiceImpl implements LedgerBalanceProjectionService {

    private final FundsAccountQueryService fundsAccountQueryService;

    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void project(@NonNull List<LedgerEntrySpec> entries) {
        if (entries.isEmpty()) {
            return;
        }
        FundsAccountId accountId = requireSingleFundsAccount(entries);
        // 按照 ledger_id 分组，避免同一科目在不同周期账本间串账。
        Map<Long, List<LedgerEntrySpec>> groups = entries.stream()
                .collect(Collectors.groupingBy(this::requireLedgerId));
        FundsAccountBalanceView beforeBalance = fundsAccountQueryService.getBalance(accountId);
        for (Map.Entry<Long, List<LedgerEntrySpec>> entry : groups.entrySet()) {
            Long ledgerId = entry.getKey();
            LedgerDTO ledger = ledgerService.getLedgerById(ledgerId);
            assertEntriesMatchLedger(ledger, entry.getValue());
            LedgerSubjectCode ledgerCode = ledger.getLedgerSubjectCode();
            ProjectionDelta delta = computeProjectionDelta(entry.getValue(), ledger.getNormalBalanceSide());
            UpdateLedgerBalanceRequest balanceRequest = new UpdateLedgerBalanceRequest()
                    .setId(ledgerId)
                    .setDebitAmountDelta(delta.debitAmountDelta())
                    .setCreditAmountDelta(delta.creditAmountDelta())
                    .setMinimumNormalBalance(resolveMinimumNormalBalance(ledger, entry.getValue()));
            ledgerService.updateLedgerBalance(balanceRequest);
            Money beforeBalanceAmount = beforeBalance.getBalance(ledgerCode);
            // 发送余额变更事件
            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(LedgerBalanceChangedEvent.builder()
                    .subjectId(accountId.id())
                    .subjectType(accountId.type())
                    .ledgerSubjectCode(ledgerCode)
                    .currency(beforeBalance.getCurrency())
                    .beforeBalance(beforeBalanceAmount.getAmount())
                    .balance(beforeBalanceAmount.getAmount() + delta.balanceDelta())
                    .build());
        }
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


    @Override
    public boolean support(@NonNull FundsAccountId accountId) {
        return fundsAccountQueryService.supports(accountId);
    }

    private record ProjectionDelta(long balanceDelta, long debitAmountDelta, long creditAmountDelta) {
    }


}

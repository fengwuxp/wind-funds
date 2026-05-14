package com.capte.funds.transaction;

import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.fx.ExchangeRateType;
import com.wind.integration.funds.fx.FxResult;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 资金交易测试支撑
 *
 * @author wuxp
 * @date 2026-05-06 15:16
 */
public final class FundsTransactionTestSupport {

    public static final CurrencyIsoCode DEFAULT_CURRENCY = CurrencyIsoCode.USD;

    public static final LocalDateTime DEFAULT_TIME = LocalDateTime.of(2026, 5, 6, 12, 0, 0);

    private FundsTransactionTestSupport() {
    }

    public static FundsAccount mockAccount(FundsAccountId accountId, CurrencyIsoCode currency) {
        return ImmutableFundsAccount.builder()
                .id(1L)
                .tenantId(1L)
                .accountId(accountId)
                .owner(FundsAccountOwner.user("user_001"))
                .status(FundsAccountStatus.ACTIVE)
                .currency(currency)
                .accountLedgerIds(Collections.emptyMap())
                .version(1)
                .build();
    }

    public static FundsAccountBalanceView balanceView(FundsAccountId accountId,
                                                      CurrencyIsoCode currency,
                                                      Map<LedgerSubjectCode, Money> balances) {
        Map<LedgerSubjectCode, LedgerBalanceBucket> buckets = new EnumMap<>(LedgerSubjectCode.class);
        balances.forEach((ledgerCode, balance) -> buckets.put(ledgerCode, LedgerBalanceBucket.builder()
                .accountCode(ledgerCode)
                .balance(balance)
                .periodType(AccountBalancePeriodType.LIFETIME)
                .periodId(AccountBalancePeriodType.LIFETIME.name())
                .activeTime(DEFAULT_TIME.minusDays(1))
                .build()));
        return ImmutableFundsBalanceView.builder()
                .id(1L)
                .tenantId(1L)
                .accountId(accountId)
                .currency(currency)
                .balanceBuckets(buckets)
                .build();
    }

    public static FxResult fxResult(Money sourceAmount, Money targetAmount, BigDecimal rate) {
        return FxResult.builder()
                .sourceAmount(sourceAmount)
                .targetAmount(targetAmount)
                .rate(rate)
                .currencyPair(sourceAmount.getCurrency() + "/" + targetAmount.getCurrency())
                .rateType(ExchangeRateType.ASK)
                .build();
    }

    public static MutableLedgerEntrySpec ledgerEntrySpec(String subjectId,
                                                         String subjectType,
                                                         LedgerSubjectCode ledgerSubjectCode,
                                                         LedgerSubjectCategory ledgerSubjectCategory,
                                                         EntrySide entryType,
                                                         String ledgerTransactionSn,
                                                         String businessScene,
                                                         String businessSn,
                                                         long amount,
                                                         CurrencyIsoCode currency,
                                                         LocalDateTime transactionTime) {
        Money money = Money.immutable(amount, currency);
        return new MutableLedgerEntrySpec()
                .setSubjectId(subjectId)
                .setSubjectType(subjectType)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(ledgerSubjectCategory)
                .setEntryType(entryType)
                .setLedgerTransactionSn(ledgerTransactionSn)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setAmount(money)
                .setOriginalAmount(money)
                .setExchangeRate(BigDecimal.ONE)
                .setTransactionTime(transactionTime)
                .setContextVariables(Map.of());
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    public static final class MutableLedgerEntrySpec implements LedgerEntrySpec {

        private String subjectId;

        private String subjectType;

        private LedgerSubjectCode ledgerSubjectCode;

        private LedgerSubjectCategory ledgerSubjectCategory;

        private Long ledgerId;

        private String ledgerTransactionSn;

        private String postingPlanSn;

        private EntrySide entryType;

        private LedgerPhaseCode phaseCode;

        private LedgerPostingIntentType intent;

        private LedgerPostingScope postingScope;

        private LedgerBalanceEffectType balanceEffectType;

        private LedgerBalanceConstraintType balanceConstraintType;

        private String businessScene;

        private String businessSn;

        private Money amount;

        private Money originalAmount;

        private BigDecimal exchangeRate;

        private LocalDateTime transactionTime;

        @Nullable
        private String description;

        private Map<String, Object> contextVariables = Map.of();

        @Nullable
        private String sha256;
    }

}

package com.wind.funds.support;

import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class FundsBalanceAssertionSupportTests {

    private static final FundsAccountId SUBJECT_REF = FundsAccountId.immutable("assert_period_account",
            FundsSubjectType.FUNDING_ACCOUNT);

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    /**
     * 场景：同一主体同一账目存在 LIFETIME 和 MONTHLY 两个余额快照。
     * 输入：两个 FundsSubjectBalanceDTO 分别携带 AVAILABLE / LIFETIME 和 AVAILABLE / MONTHLY。
     * 输出：snapshot 保留两个 bucket，delta 可只断言月度 bucket 变化。
     * 红线：余额断言支撑不得把不同账本周期折叠成同一个余额 key。
     */
    @Test
    void testSnapshotShouldUsePeriodAsBalanceKey() {
        BalanceSnapshot before = snapshot(
                balance(100L, AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name()),
                balance(30L, AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID));
        BalanceSnapshot after = snapshot(
                balance(100L, AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name()),
                balance(45L, AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID));

        assertThat(before.balances()).hasSize(2);
        assertOnlyBalanceDeltas(before, after,
                delta(SUBJECT_REF, LedgerSubjectCode.AVAILABLE, 15L, CURRENCY,
                        AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID),
                delta(SUBJECT_REF, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
    }

    private static FundsSubjectBalanceDTO balance(long amount,
                                                  AccountBalancePeriodType periodType,
                                                  String periodId) {
        return new FundsSubjectBalanceDTO()
                .setTenantId(1L)
                .setSubjectRef(SUBJECT_REF)
                .setCurrency(CURRENCY)
                .setInitialized(Boolean.TRUE)
                .setBalanceBuckets(Map.of(LedgerSubjectCode.AVAILABLE, LedgerBalanceBucket.builder()
                        .accountCode(LedgerSubjectCode.AVAILABLE)
                        .balance(Money.immutable(amount, CURRENCY))
                        .periodType(periodType)
                        .periodId(periodId)
                        .activeTime(LocalDateTime.of(2026, 5, 22, 0, 0))
                        .build()));
    }
}

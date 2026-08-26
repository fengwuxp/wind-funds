package com.wind.funds.wallet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金账户余额公共契约测试。
 *
 * @author wuxp
 * @since 2026-08-04
 */
class FundsAccountBalanceContractTests {

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    /**
     * 场景：检查账户余额公共契约。
     * 预期：保留精确的授权余额和由具体 View 定义的总余额，移除无稳定业务口径的 pending。
     * 红线：总余额不能在查询服务中绕过 View 的 Profile 语义计算。
     */
    @Test
    void testBalanceViewShouldOwnProfileSpecificTotal() throws NoSuchMethodException {
        Method ledgerProfileCode = FundsAccountBalanceView.class.getMethod("getLedgerProfileCode");
        Method authorizationBalance = FundsAccountBalanceView.class.getMethod("getAuthorizationBalance");
        Method totalBalance = FundsAccountBalanceView.class.getMethod("getTotalBalance");

        assertThat(ledgerProfileCode.getAnnotation(Deprecated.class)).isNull();
        assertThat(authorizationBalance.getAnnotation(Deprecated.class)).isNull();
        assertThat(totalBalance.getAnnotation(Deprecated.class)).isNull();
        assertThat(totalBalance.getAnnotation(JsonIgnore.class)).isNotNull();
        assertThatThrownBy(() -> FundsAccountBalanceView.class.getMethod("getPendingBalance"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> FundsAccountQueryService.class
                .getMethod("getOwnedFundsBalance", FundsAccountId.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    /**
     * 场景：账户同时存在授权、清算和结算责任余额。
     * 预期：授权余额与各责任桶保持独立、精确可读。
     * 红线：通用余额视图不能把不同责任桶折叠为含糊聚合。
     */
    @Test
    void testPreciseBalanceBucketsShouldRemainIndependent() {
        FundsAccountBalanceView balance = ImmutableFundsBalanceView.builder()
                .id(1L)
                .tenantId(1L)
                .accountId(FundsAccountId.immutable("ACCOUNT-001", FundsSubjectType.FUNDING_ACCOUNT))
                .currency(CURRENCY)
                .ledgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .balanceBuckets(Map.of(
                        LedgerSubjectCode.AVAILABLE, bucket(LedgerSubjectCode.AVAILABLE, 100L),
                        LedgerSubjectCode.FROZEN, bucket(LedgerSubjectCode.FROZEN, 20L),
                        LedgerSubjectCode.AUTHORIZATION, bucket(LedgerSubjectCode.AUTHORIZATION, 30L),
                        LedgerSubjectCode.CLEARING, bucket(LedgerSubjectCode.CLEARING, 40L),
                        LedgerSubjectCode.SETTLEMENT, bucket(LedgerSubjectCode.SETTLEMENT, 50L)))
                .build();

        assertThat(balance.getAuthorizationBalance()).isEqualTo(Money.immutable(30L, CURRENCY));
        assertThat(balance.getTotalBalance()).isEqualTo(Money.immutable(150L, CURRENCY));
        assertThat(balance.getBalance(LedgerSubjectCode.CLEARING)).isEqualTo(Money.immutable(40L, CURRENCY));
        assertThat(balance.getBalance(LedgerSubjectCode.SETTLEMENT)).isEqualTo(Money.immutable(50L, CURRENCY));
    }

    @Test
    void testProfileSpecificTotalShouldNotBecomeGenericJsonProperty() {
        FundsAccountBalanceView fundingBasic = balance(LedgerProfileCode.FUNDING_BASIC);
        FundsAccountBalanceView fundingPlatform = balance(LedgerProfileCode.FUNDING_PLATFORM);

        assertThat(WindJson.toJsonString(fundingBasic))
                .contains("\"ledgerProfileCode\":\"FUNDING_BASIC\"")
                .doesNotContain("\"totalBalance\"");
        assertThat(WindJson.toJsonString(fundingPlatform))
                .contains("\"ledgerProfileCode\":\"FUNDING_PLATFORM\"")
                .doesNotContain("\"totalBalance\"");
    }

    @Test
    void testBalanceViewShouldRejectMissingLedgerProfile() {
        assertThatThrownBy(() -> ImmutableFundsBalanceView.builder()
                .id(1L)
                .tenantId(1L)
                .accountId(FundsAccountId.immutable("ACCOUNT-001", FundsSubjectType.FUNDING_ACCOUNT))
                .currency(CURRENCY)
                .balanceBuckets(Map.of())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ledgerProfileCode");
    }

    private static FundsAccountBalanceView balance(LedgerProfileCode profileCode) {
        return ImmutableFundsBalanceView.builder()
                .id(1L)
                .tenantId(1L)
                .accountId(FundsAccountId.immutable("ACCOUNT-001", FundsSubjectType.FUNDING_ACCOUNT))
                .currency(CURRENCY)
                .ledgerProfileCode(profileCode)
                .balanceBuckets(Map.of())
                .build();
    }

    private static LedgerBalanceBucket bucket(LedgerSubjectCode subjectCode, long amount) {
        return LedgerBalanceBucket.builder()
                .ledgerSubjectCode(subjectCode)
                .balance(Money.immutable(amount, CURRENCY))
                .periodType(AccountBalancePeriodType.LIFETIME)
                .periodId(AccountBalancePeriodType.LIFETIME.name())
                .activeTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }
}

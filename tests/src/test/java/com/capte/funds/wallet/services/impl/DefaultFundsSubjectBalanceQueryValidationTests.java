package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsSubjectBalanceQueryValidationTests extends DefaultFundsAccountQueryServiceImplTestSupport {

    /**
     * 场景：跨租户查询资金主体余额。
     * 输入：请求租户为 1，资金主体所属租户为 2。
     * 输出：查询失败。
     * 预期：错误信息提示资金主体租户不匹配。
     * 红线：余额查询不得跨租户读取客户资金视图。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectSubjectFromAnotherTenant() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(3L, "funding_other_tenant", 2L, CurrencyIsoCode.USD),
                null,
                null,
                List.of()
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_other_tenant",
                FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资金主体租户不匹配");
    }

    /**
     * 场景：查询币种与资金主体币种不一致。
     * 输入：主体币种 EUR，请求币种 USD。
     * 输出：查询失败。
     * 预期：错误信息提示资金主体币种不匹配。
     * 红线：余额展示不得跨币种汇总或静默换汇。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectSubjectCurrencyMismatch() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(4L, "funding_eur", 1L, CurrencyIsoCode.EUR),
                null,
                null,
                List.of()
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_eur",
                FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资金主体币种不匹配");
    }

    /**
     * 场景：批量余额查询包含重复主体引用。
     * 输入：subjectRefs 两次传入同一个 funding account。
     * 输出：查询失败。
     * 预期：错误信息提示 subjectRefs 不能重复。
     * 红线：不得允许重复主体导致展示层重复计数或汇总口径失真。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectDuplicateSubjectRefs() {
        DefaultFundsAccountQueryServiceImpl service = newService(fundingAccount(), null, null, List.of());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef, fundingRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subjectRefs 不能重复");
    }
}

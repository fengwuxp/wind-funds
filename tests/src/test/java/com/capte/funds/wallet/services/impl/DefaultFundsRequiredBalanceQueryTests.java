package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsRequiredBalanceQueryTests extends DefaultFundsAccountQueryServiceImplTestSupport {

    /**
     * 场景：强一致余额读取要求必需账本存在。
     * 输入：funding account 只有 AVAILABLE 账本，请求必需 FROZEN 账本。
     * 输出：查询失败。
     * 预期：错误信息提示资金主体账本不完整。
     * 红线：强制余额读取不得用空 bucket 掩盖必需账本缺失。
     */
    @Test
    void testGetRequiredCurrentBalanceShouldRejectMissingRequiredLedger() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(),
                null,
                null,
                List.of(ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L))
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.FROZEN))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资金主体账本不完整");
    }

    /**
     * 场景：强一致余额读取按查询契约过滤账本桶。
     * 输入：funding account 有 AVAILABLE 与 FROZEN 账本，请求 FROZEN。
     * 输出：只包含 FROZEN 的余额 DTO。
     * 预期：subjectRef 保持请求值，余额来自 FROZEN bucket。
     * 红线：必需余额读取不得返回未请求科目，也不得忽略 ledgerSubjectCodes 过滤。
     */
    @Test
    void testGetRequiredCurrentBalanceShouldUseQueryContractAndLedgerFilters() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(),
                null,
                null,
                List.of(
                        ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L),
                        ledger(12L, LedgerSubjectCode.FROZEN, EntrySide.CREDIT, 0L, 400L)
                )
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001",
                FundsSubjectType.FUNDING_ACCOUNT.name());

        FundsSubjectBalanceDTO balance = service.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.FROZEN)));

        assertThat(balance.getSubjectRef()).isEqualTo(fundingRef);
        assertThat(balance.getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.FROZEN);
        assertThat(balance.getBalanceBuckets().get(LedgerSubjectCode.FROZEN).balance())
                .isEqualTo(Money.immutable(400L, CurrencyIsoCode.USD));
    }

    /**
     * 场景：强一致余额读取传入多个主体引用。
     * 输入：funding account 与 budget group 两个 subjectRefs。
     * 输出：查询失败。
     * 预期：错误信息提示 subjectRefs 只能包含一个主体。
     * 红线：单主体强制余额读取不得暗中聚合多个主体余额。
     */
    @Test
    void testGetRequiredCurrentBalanceShouldRejectMultipleSubjectRefs() {
        DefaultFundsAccountQueryServiceImpl service = newService(fundingAccount(), null, budgetGroup(), List.of());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001",
                FundsSubjectType.FUNDING_ACCOUNT.name());
        FundsAccountId budgetRef = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP.name());

        assertThatThrownBy(() -> service.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef, budgetRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subjectRefs 只能包含一个主体");
    }
}

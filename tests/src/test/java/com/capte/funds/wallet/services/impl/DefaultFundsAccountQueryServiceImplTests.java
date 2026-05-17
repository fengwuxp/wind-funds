package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsAccountQueryServiceImplTests extends DefaultFundsAccountQueryServiceImplTestSupport {

    /**
     * 场景：按资金账户 ID 查询账户与当前余额视图。
     * 输入：funding account 已存在 AVAILABLE 与 FROZEN 账本，币种 USD。
     * 输出：FundsAccount 与 FundsAccountBalanceView。
     * 预期：账户返回账本 ID 映射，余额视图分别展示可用、冻结和待处理余额。
     * 红线：余额查询只能读取账本投影，不得创建账本、修改余额或绕过主体类型解析。
     */
    @Test
    void testGetAccountAndBalanceShouldResolveFundingAccountFromLedger() {
        FundingAccount fundingAccount = fundingAccount();
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount,
                null,
                null,
                List.of(
                        ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L),
                        ledger(12L, LedgerSubjectCode.FROZEN, EntrySide.CREDIT, 0L, 400L)
                )
        );
        FundsAccountId accountId = FundsAccountId.immutable("funding_001",
                DefaultFundsAccountType.USER_WALLET.name());

        FundsAccount account = service.getAccount(accountId);
        FundsAccountBalanceView balance = service.getBalance(accountId);

        assertThat(account.getAccountId()).isEqualTo(accountId);
        assertThat(account.getTenantId()).isEqualTo(1L);
        assertThat(account.getOwner().ownerId()).isEqualTo("user_001");
        assertThat(account.getAccountLedgerIds())
                .containsEntry(LedgerSubjectCode.AVAILABLE, 11L)
                .containsEntry(LedgerSubjectCode.FROZEN, 12L);
        assertThat(balance.getAvailableBalance()).isEqualTo(Money.immutable(2_500L, CurrencyIsoCode.USD));
        assertThat(balance.getFrozenBalance()).isEqualTo(Money.immutable(400L, CurrencyIsoCode.USD));
        assertThat(balance.getPendingBalance()).isEqualTo(Money.immutable(0L, CurrencyIsoCode.USD));
    }

    /**
     * 场景：查询服务识别预算组主体。
     * 输入：资金账户和信用账户均不存在，预算组主体存在。
     * 输出：supports=true，并返回预算组账户信息。
     * 预期：账户 owner 与币种来自预算组记录。
     * 红线：查询 fallback 只能在只读账户类型解析内完成，不得把预算组误当 funding account。
     */
    @Test
    void testSupportsShouldFallbackToBudgetGroup() {
        BudgetGroup budgetGroup = budgetGroup();
        DefaultFundsAccountQueryServiceImpl service = newService(
                null,
                null,
                budgetGroup,
                List.of(ledger(21L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 800L))
        );
        FundsAccountId accountId = FundsAccountId.immutable("budget_001", "TEAM_BUDGET");

        FundsAccount account = service.getAccount(accountId);

        assertThat(service.supports(accountId)).isTrue();
        assertThat(account.getOwner().ownerType()).isEqualTo(FundsAccountOwnerType.MERCHANT);
        assertThat(account.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
    }

    /**
     * 场景：按 ledger subject type 查询 funding account。
     * 输入：accountId 的 accountType 使用 FUNDING_ACCOUNT 主体类型。
     * 输出：FundsAccount 及 AVAILABLE 账本映射。
     * 预期：服务支持该主体类型，并正确定位 funding account。
     * 红线：主体类型解析不得依赖外部账户类型枚举，也不得丢失账本主体边界。
     */
    @Test
    void testGetAccountShouldResolveFundingAccountByLedgerSubjectType() {
        FundingAccount fundingAccount = fundingAccount();
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount,
                null,
                null,
                List.of(ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L))
        );
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        FundsAccount account = service.getAccount(accountId);

        assertThat(service.supports(accountId)).isTrue();
        assertThat(account.getAccountId()).isEqualTo(accountId);
        assertThat(account.getAccountLedgerIds()).containsEntry(LedgerSubjectCode.AVAILABLE, 11L);
    }

    /**
     * 场景：查询不存在的资金主体。
     * 输入：不存在的 USER_WALLET accountId。
     * 输出：supports=false，getAccount 抛出异常。
     * 预期：未知主体不会被构造为空账户。
     * 红线：不得用空对象掩盖资金主体缺失，避免后续余额查询误判为真实账户。
     */
    @Test
    void testGetAccountShouldRejectUnknownSubject() {
        DefaultFundsAccountQueryServiceImpl service = newService(null, null, null, List.of());
        FundsAccountId accountId = FundsAccountId.immutable("missing_001",
                DefaultFundsAccountType.USER_WALLET.name());

        assertThat(service.supports(accountId)).isFalse();
        assertThatThrownBy(() -> service.getAccount(accountId))
                .isInstanceOf(RuntimeException.class);
    }
}

package com.capte.funds.wallet;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.CreditFundsAccountType;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.ExternalFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundingAccountType;
import com.wind.integration.funds.wallet.enums.UserWalletFundsAccountType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsAccountTypeTests {

    @Test
    void testIsExternalAccountShouldReturnTrueForExternalAccountType() {
        FundsAccountId accountId = FundsAccountId.immutable("external_bank_001",
                DefaultFundsAccountType.EXTERNAL_BANK);

        assertThat(DefaultFundsAccountType.isExternalAccount(accountId)).isTrue();
        assertThat(DefaultFundsAccountType.isExternalAccount(DefaultFundsAccountType.EXTERNAL_MERCHANT.name()))
                .isTrue();
    }

    @Test
    void testIsExternalAccountShouldReturnFalseForFundsSubjectType() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT);

        assertThat(DefaultFundsAccountType.isExternalAccount(accountId)).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(FundsSubjectType.FUNDING_ACCOUNT.name())).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(FundsSubjectType.BUDGET_GROUP.name())).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(RouteNodeType.SUBJECT.name())).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(RouteNodeType.PAYMENT_INSTRUMENT.name())).isFalse();
    }

    @Test
    void testIsExternalAccountShouldExposeUnknownAccountType() {
        assertThatThrownBy(() -> DefaultFundsAccountType.isExternalAccount("UNKNOWN_ACCOUNT_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN_ACCOUNT_TYPE");
    }

    /**
     * 场景：外部账户分类从默认资金账户枚举中拆出。
     * 输入：外部银行账户、外部商户、用户钱包和 route 外部节点类型。
     * 输出：独立外部账户分类命中外部账户，非外部默认账户返回空。
     * 预期：旧 DefaultFundsAccountType 判断入口保持兼容，RouteNodeType.EXTERNAL_ACCOUNT 仍按外部账户识别。
     * 红线：不得把用户钱包、资金主体或支付工具误判为外部账户。
     */
    @Test
    void testExternalAccountClassificationShouldBeSeparatedFromDefaultAccountType() {
        assertThat(ExternalFundsAccountType.fromAccountType(DefaultFundsAccountType.EXTERNAL_BANK.name()))
                .contains(ExternalFundsAccountType.BANK);
        assertThat(ExternalFundsAccountType.fromAccountType(DefaultFundsAccountType.USER_WALLET.name()))
                .isEmpty();
        assertThat(ExternalFundsAccountType.isExternalAccount(DefaultFundsAccountType.EXTERNAL_MERCHANT.name()))
                .isTrue();
        assertThat(ExternalFundsAccountType.isExternalAccount(RouteNodeType.EXTERNAL_ACCOUNT.name()))
                .isTrue();
    }

    /**
     * 场景：用户钱包分类从默认资金账户枚举中拆出。
     * 输入：标准用户钱包、全球账户、返利账户和预付卡账户。
     * 输出：独立用户钱包分类只命中现金型用户余额账户。
     * 预期：旧 DefaultFundsAccountType.isUserWalletType 判断入口保持兼容。
     * 红线：不得把返利、预付卡、外部账户、平台账户或信用额度账户误判为用户钱包。
     */
    @Test
    void testUserWalletClassificationShouldBeSeparatedFromDefaultAccountType() {
        assertThat(UserWalletFundsAccountType.fromAccountType(DefaultFundsAccountType.USER_WALLET.name()))
                .contains(UserWalletFundsAccountType.STANDARD);
        assertThat(UserWalletFundsAccountType.fromAccountType(DefaultFundsAccountType.GLOBAL_ACCOUNT.name()))
                .contains(UserWalletFundsAccountType.GLOBAL);
        assertThat(UserWalletFundsAccountType.fromAccountType(DefaultFundsAccountType.REBATE_ACCOUNT.name()))
                .isEmpty();
        assertThat(UserWalletFundsAccountType.fromAccountType(DefaultFundsAccountType.PREPAID_CARD.name()))
                .isEmpty();
        assertThat(DefaultFundsAccountType.isUserWalletType(DefaultFundsAccountType.USER_WALLET.name()))
                .isTrue();
    }

    /**
     * 场景：资金账户产品分类从默认资金账户枚举中拆出。
     * 输入：标准用户钱包、全球账户、返利账户、预付卡账户、共享卡账户、信用卡和外部银行账户。
     * 输出：独立资金账户分类命中平台对用户形成余额或可用资金事实的账户形态。
     * 预期：返利账户定性为平台对用户的返利负债型资金账户，预付卡账户定性为资金账户。
     * 红线：不得把共享额度、信用卡、外部账户或路由主体误判为资金账户产品。
     */
    @Test
    void testFundingAccountClassificationShouldIncludeRebateAndPrepaidCard() {
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.USER_WALLET.name()))
                .contains(FundingAccountType.USER_WALLET);
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.GLOBAL_ACCOUNT.name()))
                .contains(FundingAccountType.GLOBAL_ACCOUNT);
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.REBATE_ACCOUNT.name()))
                .contains(FundingAccountType.REBATE_ACCOUNT);
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.PREPAID_CARD.name()))
                .contains(FundingAccountType.PREPAID_CARD);
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.SHARED_CARD.name()))
                .isEmpty();
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.CREDIT_CARD.name()))
                .isEmpty();
        assertThat(FundingAccountType.fromAccountType(DefaultFundsAccountType.EXTERNAL_BANK.name()))
                .isEmpty();
        assertThat(FundingAccountType.fromAccountType(FundsSubjectType.FUNDING_ACCOUNT.name()))
                .isEmpty();
        assertThat(FundingAccountType.fromAccountType(RouteNodeType.PAYMENT_INSTRUMENT.name()))
                .isEmpty();
        assertThat(DefaultFundsAccountType.isFundingAccountType(DefaultFundsAccountType.REBATE_ACCOUNT.name()))
                .isTrue();
        assertThat(DefaultFundsAccountType.isFundingAccountType(DefaultFundsAccountType.PREPAID_CARD))
                .isTrue();
        assertThat(DefaultFundsAccountType.isFundingAccountType(DefaultFundsAccountType.SHARED_CARD))
                .isFalse();
    }

    /**
     * 场景：信用与共享卡账户分类从默认资金账户枚举中拆出。
     * 输入：预付卡账户、共享卡账户、信用卡和用户钱包。
     * 输出：独立信用账户分类只命中共享额度和信用账户形态。
     * 预期：旧 DefaultFundsAccountType.isCreditCard 判断入口保持兼容。
     * 红线：不得把预付卡、用户钱包、外部账户或平台账户误判为信用账户。
     */
    @Test
    void testCreditClassificationShouldBeSeparatedFromDefaultAccountType() {
        assertThat(CreditFundsAccountType.fromAccountType(DefaultFundsAccountType.PREPAID_CARD.name()))
                .isEmpty();
        assertThat(CreditFundsAccountType.fromAccountType(DefaultFundsAccountType.SHARED_CARD.name()))
                .contains(CreditFundsAccountType.SHARED_CARD);
        assertThat(CreditFundsAccountType.fromAccountType(DefaultFundsAccountType.CREDIT_CARD.name()))
                .contains(CreditFundsAccountType.CREDIT_CARD);
        assertThat(CreditFundsAccountType.fromAccountType(DefaultFundsAccountType.USER_WALLET.name()))
                .isEmpty();
        assertThat(DefaultFundsAccountType.isCreditCard(DefaultFundsAccountType.PREPAID_CARD.name()))
                .isFalse();
        assertThat(DefaultFundsAccountType.isCreditCard(DefaultFundsAccountType.SHARED_CARD.name()))
                .isTrue();
        assertThat(DefaultFundsAccountType.isCreditCard(DefaultFundsAccountType.CREDIT_CARD.name()))
                .isTrue();
    }
}

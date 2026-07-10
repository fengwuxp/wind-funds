package com.wind.funds.dsl;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.ExternalFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金主体类型契约测试。
 */
class FundsSubjectTypeContractTests {

    /**
     * 场景：调用方枚举资金主体类型。
     * 预期：资金主体只包含资金账户和信用账户。
     * 红线：支出控制范围、Spend Rule 或其他控制对象不得进入资金主体枚举。
     */
    @Test
    void testFundsSubjectTypeShouldExposeLedgerPostableSemantics() {
        assertThat(FundsSubjectType.values())
                .containsExactly(FundsSubjectType.FUNDING_ACCOUNT, FundsSubjectType.CREDIT_ACCOUNT);
        assertThat(FundsSubjectType.FUNDING_ACCOUNT.isLedgerPostable()).isTrue();
        assertThat(FundsSubjectType.CREDIT_ACCOUNT.isLedgerPostable()).isTrue();
        assertThat(FundsSubjectType.isLedgerPostableName("SPEND_CONTROL_SCOPE")).isFalse();
        assertThatThrownBy(() -> DefaultFundsAccountType.valueOf("SPEND_CONTROL_SCOPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 场景：账户分类入口接收到支出控制范围类型。
     * 预期：控制范围不是资金、信用或外部账户类型，分类入口返回空结果或 false。
     * 红线：控制对象不得通过 String 分类入口泄露成运行时异常。
     */
    @Test
    void testSpendControlScopeShouldNotMatchAccountTypeClassifiers() {
        assertThat(FundingAccountType.fromAccountType("SPEND_CONTROL_SCOPE")).isEmpty();
        assertThat(CreditFundsAccountType.fromAccountType("SPEND_CONTROL_SCOPE")).isEmpty();
        assertThat(ExternalFundsAccountType.fromAccountType("SPEND_CONTROL_SCOPE")).isEmpty();
        assertThat(DefaultFundsAccountType.isFundingAccountType("SPEND_CONTROL_SCOPE")).isFalse();
        assertThat(DefaultFundsAccountType.isCreditCard("SPEND_CONTROL_SCOPE")).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount("SPEND_CONTROL_SCOPE")).isFalse();
    }
}

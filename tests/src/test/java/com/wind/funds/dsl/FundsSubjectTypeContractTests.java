package com.wind.funds.dsl;

import com.wind.funds.route.enums.FundsSubjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金主体类型契约测试。
 */
class FundsSubjectTypeContractTests {

    /**
     * 场景：调用方判断资金主体是否允许进入核心账本。
     * 预期：资金账户和信用账户可入账，预算组只作为预算控制视图兼容主体。
     * 红线：预算组、Spend Rule 或其他控制对象不得被误用为真实账务主体。
     */
    @Test
    void testFundsSubjectTypeShouldExposeLedgerPostableSemantics() {
        assertThat(FundsSubjectType.FUNDING_ACCOUNT.isLedgerPostable()).isTrue();
        assertThat(FundsSubjectType.CREDIT_ACCOUNT.isLedgerPostable()).isTrue();
        assertThat(FundsSubjectType.BUDGET_GROUP.isLedgerPostable()).isFalse();
        assertThat(FundsSubjectType.BUDGET_GROUP.isControlScopeOnly()).isTrue();
    }
}

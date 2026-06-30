package com.wind.funds.dsl;

import com.wind.funds.route.enums.FundsSubjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金主体类型契约测试。
 */
class FundsSubjectTypeContractTests {

    /**
     * 场景：调用方枚举资金主体类型。
     * 预期：资金主体只包含资金账户和信用账户。
     * 红线：预算组、Spend Rule 或其他控制对象不得进入资金主体枚举。
     */
    @Test
    void testFundsSubjectTypeShouldExposeLedgerPostableSemantics() {
        assertThat(FundsSubjectType.values())
                .containsExactly(FundsSubjectType.FUNDING_ACCOUNT, FundsSubjectType.CREDIT_ACCOUNT);
        assertThat(FundsSubjectType.FUNDING_ACCOUNT.isLedgerPostable()).isTrue();
        assertThat(FundsSubjectType.CREDIT_ACCOUNT.isLedgerPostable()).isTrue();
        assertThat(FundsSubjectType.isLedgerPostableName("BUDGET_GROUP")).isFalse();
    }
}

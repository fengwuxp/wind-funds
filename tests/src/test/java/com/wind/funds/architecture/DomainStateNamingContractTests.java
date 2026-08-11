package com.wind.funds.architecture;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.wallet.FundsAccount;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 领域生命周期与处理结论的公共命名契约测试。
 */
class DomainStateNamingContractTests {

    @Test
    void testPublicContractsShouldExposeSemanticLifecycleAndResultAccessors() {
        assertAccessors(FundsAccount.class, "getState", "getStatus");
        assertAccessors(LedgerDTO.class, "getState", "getStatus");
        assertAccessors(FundsTransactionDTO.class, "getState", "getStatus");
        assertAccessors(ReconciliationRunResultDTO.class, "getOutcome", "getStatus");
        assertAccessors(PayoutPreflightResultDTO.class, "getDecisionResult", "getFactStatus", "getOperationStatus");
        assertAccessors(PayoutPreflightResultDTO.class, "getAction", "getFactStatus", "getOperationStatus");
    }

    private static void assertAccessors(Class<?> type, String required, String... forbidden) {
        assertThat(Arrays.stream(type.getMethods()).map(Method::getName))
                .contains(required)
                .doesNotContain(forbidden);
    }
}

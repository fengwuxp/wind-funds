package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class BalanceControlFundsInstructionRouteResolverTestSupport {

    protected FundsBalanceControlInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.balanceControlInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    protected static void assertLeg(RouteLegSpec leg,
                                    RouteLegType legType,
                                    LedgerSubjectCode sourceLedgerSubjectCode,
                                    LedgerSubjectCode targetLedgerSubjectCode,
                                    LedgerBalanceEffectType balanceEffectType,
                                    LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(sourceLedgerSubjectCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(targetLedgerSubjectCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(balanceEffectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    protected static void assertMustNotBeNegative(RouteLegSpec leg,
                                                  FundsAccountId accountId,
                                                  LedgerSubjectCode ledgerSubjectCode) {
        assertConstraint(leg, accountId, ledgerSubjectCode, LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    protected static void assertConstraint(RouteLegSpec leg,
                                           FundsAccountId accountId,
                                           LedgerSubjectCode ledgerSubjectCode,
                                           LedgerBalanceConstraintType constraintType) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        constraintType);
    }

    protected static WritableContextVariables context(String name, Object value) {
        return new TestContextVariables().putVariable(name, value);
    }

    protected static WritableContextVariables allowNegativeContext(String policyCode) {
        return new TestContextVariables()
                .putVariable(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE, Boolean.TRUE)
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE, policyCode)
                .putVariable(FundsInstructionContextKeys.APPROVAL_REF, "APPROVAL_0001")
                .putVariable(FundsInstructionContextKeys.ADJUST_REASON, "controlled negative balance test")
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS, "IN_GOVERNANCE")
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT,
                        FundsRouteTestSupport.amount(5_000L))
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT,
                        FundsRouteTestSupport.amount(20_000L))
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_AGING_STARTED_AT,
                        LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    protected static WritableContextVariables budgetAllowNegativeContext() {
        return allowNegativeContext("BUDGET_AVAILABLE_CONTROLLED_NEGATIVE")
                .putVariable(FundsInstructionContextKeys.BUDGET_PERIOD_ID, "BUDGET_2026_M05")
                .putVariable(FundsInstructionContextKeys.BUDGET_GOVERNANCE_POLICY_CODE,
                        "BUDGET_OVERUSE_GOVERNANCE")
                .putVariable(FundsInstructionContextKeys.BUDGET_REPORT_MARKER, "BUDGET_REPORT_2026_M05");
    }

    protected void assertAllowNegativeContextMissingShouldFail(String missingKey,
                                                               String expectedMessage,
                                                               String businessSn) {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_000L, Boolean.FALSE, "ADJUST", businessSn)
                .setContextVariables(allowNegativeContext("FUNDING_AVAILABLE_CONTROLLED_NEGATIVE")
                        .removeVariable(missingKey)),
                WindOperator.system());

        assertThatThrownBy(() -> FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(expectedMessage);
    }

    protected void assertBudgetAllowNegativeContextMissingShouldFail(String missingKey,
                                                                     String expectedMessage,
                                                                     String businessSn) {
        FundsAccountId accountId = FundsRouteTestSupport.budgetGroup("budget_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_500L, Boolean.FALSE, "BUDGET", businessSn)
                .setContextVariables(budgetAllowNegativeContext()
                        .removeVariable(missingKey)),
                WindOperator.system());

        assertThatThrownBy(() -> FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(expectedMessage);
    }

    protected static FundsBalanceAdjustRequest adjustRequest(FundsAccountId accountId,
                                                             long amount,
                                                             Boolean increase,
                                                             String businessScene,
                                                             String businessSn) {
        return new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(amount))
                .setIncrease(increase)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setAdjustReason("adjust reason")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn);
    }

    private static final class TestContextVariables implements WritableContextVariables {

        private final Map<String, Object> variables = new HashMap<>();

        @Override
        public WritableContextVariables putVariable(String name, Object val) {
            variables.put(name, val);
            return this;
        }

        @Override
        public WritableContextVariables removeVariable(String name) {
            variables.remove(name);
            return this;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.copyOf(variables);
        }
    }
}

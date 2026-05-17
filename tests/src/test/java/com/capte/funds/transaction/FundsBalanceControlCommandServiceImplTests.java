package com.capte.funds.transaction;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsBalanceControlCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

    @Test
    void testFreezeShouldBuildHoldRoute() {
        FundsAccountId payer = fundingAccount("funding_001");

        service.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(payer)
                .setAmount(amount(400L))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_00000001")
                .setDescription("freeze"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FREEZE);
        assertLeg(leg, RouteLegType.HOLD, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FROZEN, LedgerBalanceEffectType.HOLD, LedgerPhaseCode.FREEZE);
    }

    @Test
    void testUnfreezeShouldBuildReleaseRouteWithReference() {
        FundsAccountId payer = fundingAccount("funding_001");
        transactionQueryService.freezeOrderSnapshots.put("FREEZE_00000001", originalFreezeSnapshot());

        service.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(payer)
                .setAmount(amount(200L))
                .setReferenceFreezeSn("FREEZE_00000001")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("UNFREEZE_00000001")
                .setDescription("unfreeze"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FREEZE_ORDER);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("FREEZE_00000001");
        assertLeg(leg, RouteLegType.RELEASE, LedgerSubjectCode.FROZEN,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RELEASE, LedgerPhaseCode.UNFREEZE);
    }

    @Test
    void testAdjustFundingAccountIncreaseShouldBuildBalanceAdjustmentRoute() {
        FundsAccountId account = fundingAccount("funding_001");

        service.adjust(adjustRequest(account, 1_000L, Boolean.TRUE, "ADJUST", "ADJUST_00000001")
                .setReconciliationExceptionRef("REC_DIFF_0001")
                .setDescription("increase funding balance"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.BALANCE_ADJUST);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF, "REC_DIFF_0001")
                .containsEntry(FundsInstructionContextKeys.ADJUST_REASON, "adjust reason")
                .containsEntry(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF, "EVIDENCE_ADJUST_00000001")
                .containsEntry(FundsInstructionContextKeys.APPROVAL_REF, "APPROVAL_ADJUST_00000001");
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.ADJUSTMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.ADJUSTMENT);
    }

    @Test
    void testAdjustFundingAccountDecreaseShouldConstrainAvailableBalance() {
        FundsAccountId account = fundingAccount("funding_001");

        service.adjust(adjustRequest(account, 700L, Boolean.FALSE, "ADJUST", "ADJUST_00000002")
                .setDescription("decrease funding balance"), WindOperator.system());

        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction().getEventType()).isEqualTo(FundsTransactionEventType.BALANCE_ADJUST);
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.ADJUSTMENT, LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
        assertThat(leg.getConstraintOverrides())
                .containsEntry(constraintKey(account, LedgerSubjectCode.AVAILABLE),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    @Test
    void testAdjustCreditAccountShouldBuildLimitAdjustmentRoute() {
        FundsAccountId credit = creditAccount("credit_001");

        service.adjust(adjustRequest(credit, 5_000L, Boolean.TRUE, "LIMIT", "LIMIT_00000001")
                .setDescription("increase limit"), WindOperator.system());

        RouteLegSpec leg = route().getLegs().getFirst();
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE,
                LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.ADJUSTMENT);
        assertThat(instruction().getEventType()).isEqualTo(FundsTransactionEventType.LIMIT_ADJUST);
        assertThat(instruction().getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ADJUST_REASON, "adjust reason")
                .containsEntry(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF, "EVIDENCE_LIMIT_00000001")
                .containsEntry(FundsInstructionContextKeys.APPROVAL_REF, "APPROVAL_LIMIT_00000001");
    }

    @Test
    void testAdjustBudgetGroupShouldBuildBudgetLimitAdjustmentRoute() {
        FundsAccountId budgetGroup = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP);

        service.adjust(adjustRequest(budgetGroup, 2_000L, Boolean.FALSE, "BUDGET", "BUDGET_00000001")
                .setDescription("decrease budget"), WindOperator.system());

        RouteLegSpec leg = route().getLegs().getFirst();
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
    }

    @Test
    void testAdjustBudgetGroupShouldPreserveBudgetGovernanceContext() {
        FundsAccountId budgetGroup = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP);

        service.adjust(adjustRequest(budgetGroup, 2_000L, Boolean.FALSE, "BUDGET", "BUDGET_00000002")
                .setContextVariables(budgetGovernanceContext()), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.LIMIT_ADJUST);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.BUDGET_PERIOD_ID, "BUDGET_2026_M05")
                .containsEntry(FundsInstructionContextKeys.BUDGET_GOVERNANCE_POLICY_CODE,
                        "BUDGET_OVERUSE_GOVERNANCE")
                .containsEntry(FundsInstructionContextKeys.BUDGET_REPORT_MARKER, "BUDGET_REPORT_2026_M05");
    }

    @Test
    void testAdjustShouldRejectMissingApprovalEvidenceBeforeOrchestrator() {
        assertThatThrownBy(() -> service.adjust(adjustRequest(creditAccount("credit_001"), 5_000L, Boolean.TRUE,
                "LIMIT", "LIMIT_MISSING_EVIDENCE").setAdjustEvidenceRef(null), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("余额调账缺少调账凭证");
        assertThat(instruction()).isNull();
    }
}

package com.capte.funds.transaction.application;

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

    /**
     * 场景：冻结普通资金账户可用余额。
     * 输入：FundingAccount、冻结金额 400。
     * 输出：BALANCE_CONTROL / FREEZE 指令和 HOLD route。
     * 预期：同一主体 AVAILABLE 转入 FROZEN，冻结不表达消费或跨主体转移。
     */
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

    /**
     * 场景：基于原冻结单释放部分冻结余额。
     * 输入：FundingAccount、解冻金额 200、原冻结单引用。
     * 输出：UNFREEZE 指令带 FREEZE_ORDER 引用，并生成 RELEASE route。
     * 预期：同一主体 FROZEN 回到 AVAILABLE，解冻路径来自原冻结事实。
     */
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

    /**
     * 场景：运营对普通资金账户做余额调增并关联对账差错。
     * 输入：FundingAccount、调增金额 1000、调账原因、凭证、审批和对账差错引用。
     * 输出：BALANCE_ADJUST 指令上下文和 ADJUSTMENT -> AVAILABLE route。
     * 预期：调账审计字段完整保留，调增通过平台 ADJUSTMENT 账户形成平衡路径。
     */
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

    /**
     * 场景：运营对普通资金账户做余额调减。
     * 输入：FundingAccount、调减金额 700、调账原因、凭证和审批。
     * 输出：BALANCE_ADJUST 指令和 AVAILABLE -> ADJUSTMENT route。
     * 预期：调减必须约束 AVAILABLE 不得被打穿为负数。
     */
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

    /**
     * 场景：运营对信用账户做额度调增。
     * 输入：CREDIT_ACCOUNT、调增金额 5000、调额原因、凭证和审批。
     * 输出：LIMIT_ADJUST 指令和 LIMIT -> AVAILABLE route。
     * 预期：信用账户调额只触碰 LIMIT/AVAILABLE，不落入普通资金调账口径。
     */
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

    /**
     * 场景：运营对预算组做预算额度调减。
     * 输入：BUDGET_GROUP、调减金额 2000、调额原因、凭证和审批。
     * 输出：预算组 LIMIT_ADJUST route。
     * 预期：预算组额度调整走控制账户额度口径，不走普通 FundingAccount FX 或余额逻辑。
     */
    @Test
    void testAdjustBudgetGroupShouldBuildBudgetLimitAdjustmentRoute() {
        FundsAccountId budgetGroup = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP);

        service.adjust(adjustRequest(budgetGroup, 2_000L, Boolean.FALSE, "BUDGET", "BUDGET_00000001")
                .setDescription("decrease budget"), WindOperator.system());

        RouteLegSpec leg = route().getLegs().getFirst();
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
    }

    /**
     * 场景：预算组调额请求携带预算治理上下文。
     * 输入：BUDGET_GROUP、调减金额 2000 和预算周期、治理策略、报表标记。
     * 输出：LIMIT_ADJUST 指令上下文。
     * 预期：预算治理元数据透传给交易指令，便于后续报表和治理审计。
     */
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

    /**
     * 场景：调账请求缺少审批或凭证材料。
     * 输入：CREDIT_ACCOUNT 调额请求，但调账凭证为空。
     * 输出：命令服务在进入编排器前抛出校验异常。
     * 预期：资金调账/调额必须有原因、凭证和审批，校验失败时不生成资金指令。
     */
    @Test
    void testAdjustShouldRejectMissingApprovalEvidenceBeforeOrchestrator() {
        assertThatThrownBy(() -> service.adjust(adjustRequest(creditAccount("credit_001"), 5_000L, Boolean.TRUE,
                "LIMIT", "LIMIT_MISSING_EVIDENCE").setAdjustEvidenceRef(null), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("余额调账缺少调账凭证");
        assertThat(instruction()).isNull();
    }
}

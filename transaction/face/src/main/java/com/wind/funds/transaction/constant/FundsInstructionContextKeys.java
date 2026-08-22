package com.wind.funds.transaction.constant;

import com.wind.funds.transaction.support.FundsContextVariables;

/**
 * 资金指令上下文 Key 常量。
 */
public final class FundsInstructionContextKeys {

    public static final String APPROVED = "approved";

    /**
     * 授权拒绝原因，仅用于 approved=false 的授权拒绝，不表示结算后的拒付/争议。
     */
    public static final String DECLINE_REASON = "declineReason";

    public static final String MERCHANT_INFO = "merchantInfo";

    public static final String TRANSACTION_COUNTRY = "transactionCountry";

    public static final String AUTHORIZATION_TRANSACTION_SN = "authorizationTransactionSn";

    public static final String COMPLETION_MODE = "completionMode";

    public static final String FORCE_COMPLETION_POLICY_CODE = "forceCompletionPolicyCode";

    public static final String FORCE_COMPLETION_LIMIT_AMOUNT = "forceCompletionLimitAmount";

    public static final String FORCE_COMPLETION_REASON = "forceCompletionReason";

    public static final String EXTERNAL_ORIGINAL_FACT_REF = "externalOriginalFactRef";

    public static final String FORCE_COMPLETION_VOUCHER_REF = "forceCompletionVoucherRef";

    /**
     * 退款内部归类标签，仅用于资金指令、路由和资金事实上下文，不作为请求字段暴露。
     */
    public static final String REFUND_MODE = "refundMode";

    public static final String REFUND_MODE_NO_AUTH = "NO_AUTH";

    public static final String REFUND_MODE_DISPUTE = "DISPUTE";

    public static final String EXTERNAL_REFERENCE_SN = "externalReferenceSn";

    public static final String REFUND_REASON = "refundReason";

    public static final String DISPUTE_MODE = "disputeMode";

    public static final String DISPUTE_REASON = "disputeReason";

    public static final String DISPUTE_VOUCHER_REF = "disputeVoucherRef";

    public static final String EXTERNAL_DISPUTE_REF = "externalDisputeRef";

    public static final String LINKED_SPEND_CONTROL_SCOPE_ID = "linkedSpendControlScopeId";

    public static final String INCREASE = FundsContextVariables.INCREASE;

    public static final String REFERENCE_FREEZE_SN = "referenceFreezeSn";

    public static final String FREEZE_TYPE = "freezeType";

    public static final String FROZEN_ORDER_EVENT_TYPE = "frozenOrderEventType";

    public static final String ROUTE_SNAPSHOT = "routeSnapshot";

    /**
     * Spend Rule 决策最小快照，用于交易投影解释、审计和对账追踪。
     *
     * <p>该字段只承载已固化的规则、版本、挂载和决策引用，不承载规则 DSL 原文或脚本内容。</p>
     */
    public static final String SPEND_RULE_DECISION = "spendRuleDecision";

    /**
     * 原权益快照 ID 摘要，用于 RouteSnapshot 回放、投影解释和审计追溯。
     *
     * <p>该字段不承载完整权益事实。</p>
     */
    public static final String BENEFIT_SNAPSHOT_ID = "benefitSnapshotId";

    /**
     * 原权益快照稳定摘要，用于判断回放是否引用原路径事实。
     *
     * <p>该字段不承载当前营销计算结果。</p>
     */
    public static final String BENEFIT_SNAPSHOT_STABLE_DIGEST = "stableDigest";

    public static final String ADJUST_REASON = "adjustReason";

    public static final String ADJUST_EVIDENCE_REF = "adjustEvidenceRef";

    public static final String SOURCE_TYPE = "sourceType";

    public static final String SOURCE_SN = "sourceSn";

    public static final String REASON_CODE = "reasonCode";

    public static final String EXTERNAL_INSTITUTION_REF = "externalInstitutionRef";

    public static final String EXTERNAL_ACCOUNT_REF = "externalAccountRef";

    public static final String EXTERNAL_FINAL_EVENT_REF = "externalFinalEventRef";

    public static final String EXTERNAL_BALANCE_SNAPSHOT_REF = "externalBalanceSnapshotRef";

    public static final String RESPONSIBILITY_REF = "responsibilityRef";

    public static final String APPROVAL_REF = "approvalRef";

    public static final String RECONCILIATION_EXCEPTION_REF = "reconciliationExceptionRef";

    public static final String RECONCILIATION_RERUN_REF = "reconciliationRerunRef";

    public static final String BUDGET_PERIOD_ID = "budgetPeriodId";

    public static final String BUDGET_GOVERNANCE_POLICY_CODE = "budgetGovernancePolicyCode";

    public static final String BUDGET_REPORT_MARKER = "budgetReportMarker";

    public static final String CHANNEL_CODE = "channelCode";

    public static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";

    public static final String FEE_TYPE = "feeType";

    public static final String FEE_CHARGE_SPEC = "feeChargeSpec";

    public static final String REPLAY_CONSUMED_LEG_IDS = "replayConsumedLegIds";

    public static final String REPLAY_CONSUMED_LEG_AMOUNTS = "replayConsumedLegAmounts";

    private FundsInstructionContextKeys() {
    }
}

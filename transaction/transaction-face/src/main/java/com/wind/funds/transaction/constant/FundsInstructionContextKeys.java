package com.wind.funds.transaction.constant;

/**
 * 资金指令上下文 Key 常量。
 */
public final class FundsInstructionContextKeys {

    public static final String ACCOUNT_ID = "accountId";

    public static final String PAYER_ACCOUNT_ID = "payerAccountId";

    public static final String PAYEE_ACCOUNT_ID = "payeeAccountId";

    public static final String PAYER_ID = "payerId";

    public static final String PAYEE_ID = "payeeId";

    public static final String PAYER_LEDGER_SUBJECT_CODE = "payerLedgerSubjectCode";

    public static final String PAYEE_LEDGER_SUBJECT_CODE = "payeeLedgerSubjectCode";

    public static final String APPROVED = "approved";

    /**
     * 授权拒绝原因，仅用于 approved=false 的授权拒绝，不表示结算后的拒付/争议。
     */
    public static final String DECLINE_REASON = "declineReason";

    /**
     * 授权过期释放原因，仅用于系统到期释放剩余授权占用，不表示外部撤销或结算后争议。
     */
    public static final String EXPIRE_REASON = "expireReason";

    public static final String MERCHANT_INFO = "merchantInfo";

    public static final String TRANSACTION_COUNTRY = "transactionCountry";

    public static final String AUTHORIZATION_TRANSACTION_SN = "authorizationTransactionSn";

    public static final String SETTLE_MODE = "settleMode";

    public static final String FORCE_SETTLE_POLICY_CODE = "forceSettlePolicyCode";

    public static final String FORCE_SETTLE_LIMIT_AMOUNT = "forceSettleLimitAmount";

    public static final String FORCE_SETTLE_REASON = "forceSettleReason";

    public static final String EXTERNAL_ORIGINAL_FACT_REF = "externalOriginalFactRef";

    public static final String FORCE_SETTLE_VOUCHER_REF = "forceSettleVoucherRef";

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

    public static final String LINKED_FUNDING_ACCOUNT_ID = "linkedFundingAccountId";

    public static final String LINKED_BUDGET_GROUP_ID = "linkedBudgetGroupId";

    public static final String INCREASE = "increase";

    public static final String REFERENCE_FREEZE_SN = "referenceFreezeSn";

    public static final String FREEZE_TYPE = "freezeType";

    public static final String FROZEN_ORDER_EVENT_TYPE = "frozenOrderEventType";

    public static final String ROUTE_SNAPSHOT = "routeSnapshot";

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

    public static final String ALLOW_NEGATIVE_BALANCE = "allowNegativeBalance";

    public static final String NEGATIVE_AVAILABLE_POLICY_CODE = "negativeAvailablePolicyCode";

    public static final String APPROVAL_REF = "approvalRef";

    public static final String RECONCILIATION_EXCEPTION_REF = "reconciliationExceptionRef";

    public static final String RECONCILIATION_RERUN_REF = "reconciliationRerunRef";

    public static final String NEGATIVE_AVAILABLE_RISK_STATUS = "negativeAvailableRiskStatus";

    public static final String NEGATIVE_AVAILABLE_SINGLE_LIMIT = "negativeAvailableSingleLimit";

    public static final String NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT = "negativeAvailableCumulativeLimit";

    public static final String NEGATIVE_AVAILABLE_AGING_STARTED_AT = "negativeAvailableAgingStartedAt";

    public static final String BUDGET_PERIOD_ID = "budgetPeriodId";

    public static final String BUDGET_GOVERNANCE_POLICY_CODE = "budgetGovernancePolicyCode";

    public static final String BUDGET_REPORT_MARKER = "budgetReportMarker";

    public static final String CHANNEL_CODE = "channelCode";

    public static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";

    public static final String FEE_TYPE = "feeType";

    public static final String FEE_SPEC = "feeSpec";

    public static final String REPLAY_CONSUMED_LEG_IDS = "replayConsumedLegIds";

    public static final String REPLAY_CONSUMED_LEG_AMOUNTS = "replayConsumedLegAmounts";

    private FundsInstructionContextKeys() {
    }
}

package com.capte.funds.transaction.constant;

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

    public static final String DECLINE_REASON = "declineReason";

    public static final String MERCHANT_INFO = "merchantInfo";

    public static final String TRANSACTION_COUNTRY = "transactionCountry";

    public static final String AUTHORIZATION_TRANSACTION_SN = "authorizationTransactionSn";

    public static final String LINKED_FUNDING_ACCOUNT_ID = "linkedFundingAccountId";

    public static final String LINKED_BUDGET_GROUP_ID = "linkedBudgetGroupId";

    public static final String INCREASE = "increase";

    public static final String REFERENCE_FREEZE_SN = "referenceFreezeSn";

    public static final String ADJUST_REASON = "adjustReason";

    public static final String CHANNEL_CODE = "channelCode";

    public static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";

    public static final String FEE_TYPE = "feeType";

    public static final String REPLAY_CONSUMED_LEG_IDS = "replayConsumedLegIds";

    private FundsInstructionContextKeys() {
    }
}

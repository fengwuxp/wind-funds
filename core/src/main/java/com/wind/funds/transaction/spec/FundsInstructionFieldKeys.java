package com.wind.funds.transaction.spec;

/**
 * 资金指令一等字段名。
 */
public final class FundsInstructionFieldKeys {

    public static final String ACCOUNT_ID = "accountId";

    public static final String PAYER_ACCOUNT_ID = "payerAccountId";

    public static final String PAYEE_ACCOUNT_ID = "payeeAccountId";

    public static final String PAYER_ID = "payerId";

    public static final String PAYEE_ID = "payeeId";

    public static final String PAYER_LEDGER_SUBJECT_CODE = "payerLedgerSubjectCode";

    public static final String PAYEE_LEDGER_SUBJECT_CODE = "payeeLedgerSubjectCode";

    public static final String LINKED_FUNDING_ACCOUNT_ID = "linkedFundingAccountId";

    public static final String LEDGER_PERIOD_TYPE = "ledgerPeriodType";

    public static final String LEDGER_PERIOD_ID = "ledgerPeriodId";

    private FundsInstructionFieldKeys() {
    }
}

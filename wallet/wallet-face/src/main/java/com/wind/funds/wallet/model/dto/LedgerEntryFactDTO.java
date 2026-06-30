package com.wind.funds.wallet.model.dto;

import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账本分录事实轻量快照。
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class LedgerEntryFactDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6213875719236175840L;

    private String sn;

    private String ledgerTransactionSn;

    private String fundsTransactionSn;

    private String businessScene;

    private String businessSn;

    private String subjectId;

    private String subjectType;

    private LedgerSubjectCode ledgerSubjectCode;

    private LedgerSubjectCategory ledgerSubjectCategory;

    private EntrySide entryType;

    private LedgerBalanceConstraintType balanceConstraintType;

    private LedgerBalanceEffectType balanceEffectType;

    private Long amount;

    private String currency;
}

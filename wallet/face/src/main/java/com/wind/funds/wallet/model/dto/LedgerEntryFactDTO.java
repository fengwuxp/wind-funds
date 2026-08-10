package com.wind.funds.wallet.model.dto;

import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账本分录事实轻量快照。
 *
 * @author wuxp
 * @since 2026-06-30
 */
@Schema(description = "账本分录事实轻量快照")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class LedgerEntryFactDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6213875719236175840L;

    @Schema(description = "账本分录流水号")
    private String sn;

    @Schema(description = "账本交易流水号")
    private String ledgerTransactionSn;

    @Schema(description = "资金交易流水号")
    private String fundsTransactionSn;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号")
    private String businessSn;

    @Schema(description = "账务主体标识")
    private String subjectId;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "账本科目编码")
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "账本科目分类")
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "借贷方向")
    private EntrySide entryType;

    @Schema(description = "余额约束类型")
    private LedgerBalanceConstraintType balanceConstraintType;

    @Schema(description = "余额影响类型")
    private LedgerBalanceEffectType balanceEffectType;

    @Schema(description = "分录金额，最小货币单位")
    private Long amount;

    @Schema(description = "分录币种代码")
    private String currency;
}

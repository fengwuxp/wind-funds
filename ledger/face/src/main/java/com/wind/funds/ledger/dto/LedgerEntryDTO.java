package com.wind.funds.ledger.dto;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.support.FundsInstructionContextValidator;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 账户账本条目
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerEntryDTO {

    @Schema(description = "自增主键")
    @NotNull
    private Long id;

    @Schema(description = "创建时间")
    @NotNull
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    @NotNull
    private LocalDateTime gmtModified;

    @Schema(description = "分录号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "记账计划号")
    private String postingPlanSn;

    @Schema(description = "业务交易号")
    private String fundsTransactionSn;

    @Schema(description = "对应 t_ledger.id")
    private Long ledgerId;

    @Schema(description = "账本周期类型快照")
    private AccountBalancePeriodType periodType;

    @Schema(description = "账本周期 ID 快照")
    private String periodId;

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "账目编码（会计科目编码）\n例如：CASH（现金）、AVAILABLE （余额）、AUTHORIZATION（授权）")
    @NotNull
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "会计科目类别")
    @NotNull
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "账本交易流水号")
    @NotNull
    private String ledgerTransactionSn;

    @Schema(description = "账本分录类型（借贷）")
    @NotNull
    private EntrySide entryType;

    @Schema(description = "多级账户记账角色")
    @NotNull
    private LedgerPostingRole postingRole;

    @Schema(description = "本分录余额约束")
    private LedgerBalanceConstraintType balanceConstraintType;

    @Schema(description = "记账意图")
    private LedgerPostingIntentType intent;

    @Schema(description = "记账范围")
    private LedgerPostingScope postingScope;

    @Schema(description = "余额影响语义")
    private LedgerBalanceEffectType balanceEffectType;

    @Schema(description = "记账阶段")
    private LedgerPhaseCode phaseCode;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "业务 sn（订单号、业务交易流水）")
    private String businessSn;

    @Schema(description = "记账金额，单位：分")
    @NotNull
    private Money amount;

    @Schema(description = "原始金额，单位：分")
    @NotNull
    private Money originalAmount;

    @Schema(description = "汇率")
    @NotNull
    private BigDecimal exchangeRate;

    @Schema(description = "交易时间")
    @NotNull
    private LocalDateTime transactionTime;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "上下文变量")
    private Map<String, Object> contextVariables;

    @Schema(description = "sha256，防止数据篡改")
    @NotNull
    private String sha256;

    public LedgerEntryDTO setContextVariables(Map<String, Object> contextVariables) {
        this.contextVariables = FundsInstructionContextValidator.immutableInstructionContext(
                contextVariables, "ledgerEntryDto");
        return this;
    }

}

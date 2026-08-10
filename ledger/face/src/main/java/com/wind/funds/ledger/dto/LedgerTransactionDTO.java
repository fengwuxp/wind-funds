package com.wind.funds.ledger.dto;

import com.wind.funds.transaction.support.FundsInstructionContextValidator;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 账户账本交易
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerTransactionDTO {

    @Schema(description = "账本交易主键")
    @NotNull
    private Long id;

    @Schema(description = "创建时间")
    @NotNull
    private LocalDateTime gmtCreate;

    @Schema(description = "最后修改时间")
    @NotNull
    private LocalDateTime gmtModified;

    @Schema(description = "账户ID")
    @NotNull
    @Size(min = 12, max = 50)
    private String sn;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "标准业务交易流水")
    private String fundsTransactionSn;

    @Schema(description = "事件类型")
    @NotNull
    private FundsTransactionEventType eventType;

    @Schema(description = "资金指令类型")
    private FundsInstructionType instructionType;

    @Schema(description = "资金交易类型")
    private DefaultFundsTransactionType transactionType;

    @Schema(description = "交易金额，单位：分")
    @NotNull
    private Money amount;

    @Schema(description = "原始交易金额，单位：分")
    @NotNull
    private Money originalAmount;

    @Schema(description = "汇率")
    @NotNull
    private BigDecimal exchangeRate;

    @Schema(description = "借方合计")
    @NotNull
    private Money debitAmount;

    @Schema(description = "贷方合计")
    @NotNull
    private Money creditAmount;

    @Schema(description = "业务单号")
    @Size(min = 10, max = 80)
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "关联账本交易单号")
    private String referenceLedgerTransactionSn;

    @Schema(description = "交易时间")
    @NotNull
    private LocalDateTime transactionTime;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private Map<String, Object> contextVariables;

    @Schema(description = "账本交易事实摘要")
    @NotNull
    private String sha256;

    @Schema(description = "账本交易分录")
    private List<LedgerEntryDTO> entries;

    public LedgerTransactionDTO setContextVariables(Map<String, Object> contextVariables) {
        this.contextVariables = FundsInstructionContextValidator.immutableInstructionContext(
                contextVariables, "ledgerTransactionDto");
        return this;
    }

}

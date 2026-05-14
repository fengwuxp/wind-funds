package com.capte.funds.transaction.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 资金指令生命周期记录结果。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsInstructionLifecycleResult implements Serializable {

    @Serial
    private static final long serialVersionUID = -1725021094911186944L;

    @Schema(description = "标准业务交易流水")
    private String transactionSn;

    @Schema(description = "标准业务交易明细流水列表")
    private List<String> transactionDetailSns;

    @Schema(description = "账本交易流水")
    private String ledgerTransactionSn;

    @Schema(description = "该生命周期明细是否已处理完成")
    private boolean completed;
}

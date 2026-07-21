package com.wind.funds.transaction.model.request;

import com.wind.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 创建资金冻结订单请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateFundsFrozenOrderRequest {

    @Schema(description = "冻结单号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "被冻结主体 ID")
    @NotBlank
    private String subjectId;

    @Schema(description = "被冻结主体类型")
    @NotNull
    private FundsSubjectType subjectType;

    @Schema(description = "冻结类型")
    @NotBlank
    private String freezeType;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "外部业务号")
    @NotBlank
    private String businessSn;

    @Schema(description = "关联资金交易号")
    private String transactionSn;

    @Schema(description = "冻结动作明细号")
    private String freezeDetailSn;

    @Schema(description = "冻结账本交易号")
    private String freezeLedgerTransactionSn;

    @Schema(description = "原冻结金额")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "冻结单状态")
    private FundsFrozenOrderStatus status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}

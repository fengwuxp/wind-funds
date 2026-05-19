package com.capte.funds.transaction.model.dto;

import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标准资金交易 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsTransactionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2186207246028340950L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "交易号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "交易模式")
    private FundsTransactionMode transactionMode;

    @Schema(description = "交易类型")
    private DefaultFundsTransactionType transactionType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务号")
    private String businessSn;

    @Schema(description = "引用交易号")
    private String referenceTransactionSn;

    @Schema(description = "交易状态")
    private FundsTransactionStatus status;

    @Schema(description = "原始交易金额")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "累计授权金额")
    private Long authorizedAmount;

    @Schema(description = "累计撤销金额")
    private Long reversedAmount;

    @Schema(description = "累计结算或完成金额")
    private Long settledAmount;

    @Schema(description = "累计退款金额")
    private Long refundedAmount;

    @Schema(description = "累计拒付/争议退回金额，不包含授权拒绝金额")
    private Long declinedAmount;

    @Schema(description = "累计手续费金额")
    private Long feeAmount;

    @Schema(description = "RouteSnapshot JSON")
    private String routeSnapshot;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;

    @Schema(description = "乐观锁版本")
    private Integer version;
}

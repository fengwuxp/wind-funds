package com.wind.funds.transaction.model.dto;

import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
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
 * 标准资金交易明细 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsTransactionDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8534012669017638089L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "明细号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "业务交易号")
    private String transactionSn;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务动作号")
    private String businessSn;

    @Schema(description = "交易类型")
    private DefaultFundsTransactionType transactionType;

    @Schema(description = "生命周期事件")
    private FundsTransactionEventType eventType;

    @Schema(description = "影响主体 ID")
    private String subjectId;

    @Schema(description = "影响主体类型")
    private String subjectType;

    @Schema(description = "参与方角色")
    private RouteParticipantRole participantRole;

    @Schema(description = "请求参数一致性摘要")
    private String requestHash;

    @Schema(description = "资金业务效果")
    private FundsEffectType fundsEffectType;

    @Schema(description = "账本交易号")
    private String ledgerTransactionSn;

    @Schema(description = "引用明细号")
    private String referenceDetailSn;

    @Schema(description = "引用账本交易号")
    private String referenceLedgerTransactionSn;

    @Schema(description = "本次动作金额")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "明细状态")
    private FundsTransactionDetailStatus status;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误消息")
    private String errorMessage;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}

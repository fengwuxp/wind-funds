package com.capte.funds.transaction.model.query;

import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 标准资金交易明细查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsTransactionDetailQuery {

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

    @Schema(description = "资金业务效果")
    private FundsEffectType fundsEffectType;

    @Schema(description = "账本交易号")
    private String ledgerTransactionSn;

    @Schema(description = "明细状态")
    private FundsTransactionDetailStatus status;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;
}

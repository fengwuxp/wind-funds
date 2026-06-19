package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 清算 / 结算对账准入消费请求。
 *
 * <p>职责：承载清算候选生成、清算确认、结算锁定或结算确认前的对账准入检查对象。</p>
 *
 * <p>边界：请求只用于只读准入检查，不表达清算批次、结算单或账务事实已经创建。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CheckClearingSettlementGateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3173426334905124874L;

    /**
     * 租户 ID。
     */
    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    /**
     * 准入消费对象类型，仅支持 CLEARING 或 SETTLEMENT。
     */
    @Schema(description = "准入消费对象类型，仅支持 CLEARING 或 SETTLEMENT")
    @NotNull
    private ReconciliationGateObjectType gateObjectType;

    /**
     * 准入消费对象流水号，例如清算候选号或结算单号。
     */
    @Schema(description = "准入消费对象流水号，例如清算候选号或结算单号")
    @NotBlank
    private String gateObjectSn;

    /**
     * 准入对象币种，用于调用方审计和后续扩展校验。
     */
    @Schema(description = "准入对象币种")
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 准入对象金额，单位遵循项目金额最小单位约定。
     */
    @Schema(description = "准入对象金额，单位遵循项目金额最小单位约定")
    @NotNull
    private Long amount;

    /**
     * 调用方幂等键，用于调用方追踪准入检查请求。
     */
    @Schema(description = "调用方幂等键，用于追踪准入检查请求")
    @NotBlank
    private String idempotencyKey;
}

package com.capte.funds.wallet.model.query;

import com.wind.funds.wallet.enums.PaymentInstrumentBindingChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支付工具绑定历史查询条件。
 *
 * @author Codex
 * @date 2026-05-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PaymentInstrumentBindingHistoryQuery {

    @Schema(description = "审计号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "绑定号")
    private String bindingSn;

    @Schema(description = "工具号")
    private String instrumentSn;

    @Schema(description = "变更类型")
    private PaymentInstrumentBindingChangeType changeType;

    @Schema(description = "绑定版本")
    private Integer version;

    @Schema(description = "请求号")
    private String requestSn;
}

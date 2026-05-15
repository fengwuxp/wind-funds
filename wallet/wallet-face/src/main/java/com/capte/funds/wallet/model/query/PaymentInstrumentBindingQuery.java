package com.capte.funds.wallet.model.query;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支付工具绑定查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PaymentInstrumentBindingQuery {

    @Schema(description = "绑定号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "工具号")
    private String instrumentSn;

    @Schema(description = "绑定角色")
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "内部主体 ID")
    private String subjectId;

    @Schema(description = "内部主体类型")
    private FundsSubjectType subjectType;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "是否默认绑定")
    private Boolean defaultBinding;

    @Schema(description = "状态")
    private FundsAccountStatus status;
}

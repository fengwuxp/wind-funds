package com.wind.funds.wallet.model.dto;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 支付工具能力准入决策 DTO。
 *
 * @author Codex
 * @date 2026-06-16
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString(exclude = "instrumentNo")
@Accessors(chain = true)
public class PaymentInstrumentCapabilityDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 118855769955563961L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "支付工具主键")
    private Long instrumentId;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "支付工具展示号或稳定识别号")
    private String instrumentNo;

    @Schema(description = "支付工具归属主体 ID")
    private String ownerId;

    @Schema(description = "支付工具归属主体类型")
    private FundsAccountOwnerType ownerType;

    @Schema(description = "支付工具类型")
    private String instrumentType;

    @Schema(description = "支付工具资金流向")
    private PaymentInstrumentFlowDirection flowDirection;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "业务动作")
    private PaymentInstrumentAction action;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "支付工具状态")
    private FundsAccountStatus status;

    @Schema(description = "绑定主键")
    private Long bindingId;

    @Schema(description = "绑定号")
    private String bindingSn;

    @Schema(description = "绑定角色")
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "内部主体 ID")
    private String subjectId;

    @Schema(description = "内部主体类型")
    private FundsSubjectType subjectType;

    @Schema(description = "绑定版本")
    private Integer bindingVersion;

    @Schema(description = "是否默认绑定")
    private Boolean defaultBinding;

    @Schema(description = "支付工具描述")
    private String description;
}

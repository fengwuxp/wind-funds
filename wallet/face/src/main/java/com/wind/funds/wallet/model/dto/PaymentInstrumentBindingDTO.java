package com.wind.funds.wallet.model.dto;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingStatus;
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
 * 支付工具绑定 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PaymentInstrumentBindingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8129489273791104747L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

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

    @Schema(description = "路由优先级")
    private Integer priority;

    @Schema(description = "是否默认绑定")
    private Boolean defaultBinding;

    @Schema(description = "绑定生命周期状态")
    private PaymentInstrumentBindingStatus status;

    @Schema(description = "绑定版本")
    private Integer version;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}

package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.PaymentInstrumentBindingChangeType;
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
 * 支付工具绑定历史 DTO。
 *
 * @author Codex
 * @date 2026-05-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PaymentInstrumentBindingHistoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8606207050338079302L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

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

    @Schema(description = "变更前快照")
    private String beforeSnapshot;

    @Schema(description = "变更后快照")
    private String afterSnapshot;

    @Schema(description = "操作者")
    private String operatorId;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "变更事实生效时间")
    private LocalDateTime effectiveAt;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}

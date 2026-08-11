package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.SettlementReleaseDisposition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 宿主对结算锁定资金释放作出的授权决定。
 *
 * <p>决定必须绑定摘要、有效期、授权主体和证据引用，由 wind-funds 在释放事务内验证后消费。</p>
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算锁定资金释放授权决定")
@Data
@Accessors(chain = true)
public class SettlementReleaseDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1669938878885099354L;

    @Schema(description = "是否允许执行释放")
    private boolean releaseAllowed;

    @Schema(description = "释放资金的目标余额桶")
    private SettlementReleaseDisposition releaseDisposition;

    @Schema(description = "授权决定稳定摘要")
    private String decisionDigest;

    @Schema(description = "支持授权决定的稳定证据引用")
    private List<String> evidenceRefs;

    @Schema(description = "授权决定失效时间")
    private LocalDateTime expiresAt;

    @Schema(description = "授权主体稳定标识")
    private String authorizedBy;

    @Schema(description = "授权时间")
    private LocalDateTime authorizedAt;

    @Nullable
    @Schema(description = "拒绝释放时的阻断原因")
    private String blockingReason;
}

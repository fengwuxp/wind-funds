package com.capte.funds.reconciliation.model.dto;

import com.capte.funds.reconciliation.enums.PayoutPreflightBlockingReasonCode;
import com.capte.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 出款前准入阻断原因 DTO。
 *
 * <p>职责：解释某个准入 guard 为什么阻断、是否可恢复以及需要哪个证据继续处理。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PayoutPreflightBlockingReasonDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3598021989558354323L;

    @Schema(description = "阻断原因编码")
    private PayoutPreflightBlockingReasonCode code;

    @Schema(description = "可展示的阻断原因")
    private String message;

    @Schema(description = "准入 guard 名称")
    private String guardName;

    @Schema(description = "阻断等级")
    private PayoutPreflightBlockingLevel severity;

    @Schema(description = "是否可恢复")
    private boolean recoverable;

    @Schema(description = "关联证据引用")
    private String evidenceRef;

    @Schema(description = "确认责任方")
    private String confirmationOwner;
}

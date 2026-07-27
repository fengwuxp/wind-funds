package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建对账差错请求。
 *
 * <p>职责：指定已固化的逐笔匹配差异，并补充上层确认的责任方和说明。</p>
 *
 * <p>边界：差错号、批次、差异内容、证据、规则版本和 Gate 对象均由服务端从持久化事实派生。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateReconciliationDifferenceRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -737023897466599657L;

    public static final int MAX_MATCH_RESULT_SN_LENGTH = 64;

    public static final int MAX_RESPONSIBLE_PARTY_REF_LENGTH = 128;

    public static final int MAX_DESCRIPTION_LENGTH = 512;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "已固化的对账逐笔匹配结果流水号，用作业务幂等键")
    @NotBlank
    @Size(max = MAX_MATCH_RESULT_SN_LENGTH)
    private String reconciliationMatchResultSn;

    @Schema(description = "责任方引用")
    @NotBlank
    @Size(max = MAX_RESPONSIBLE_PARTY_REF_LENGTH)
    private String responsiblePartyRef;

    @Schema(description = "责任归属或运营说明")
    @Size(max = MAX_DESCRIPTION_LENGTH)
    private String description;
}

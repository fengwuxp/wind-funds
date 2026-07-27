package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对账来源成员输入。
 *
 * <p>内容摘要由可信来源适配器基于规范化、不可变的业务事实生成。对账模块不读取或解释业务原文，
 * 只冻结稳定引用与内容身份，用于识别同一引用背后的事实改写。摘要本身不证明来源真实性；
 * 验签、来源授权、原文归一化和适配器访问控制属于宿主接入边界。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationSourceItemInput implements Serializable {

    @Serial
    private static final long serialVersionUID = 829513469942107135L;

    public static final int MAX_SOURCE_ITEM_REF_LENGTH = 128;

    @Schema(description = "不可变来源事实稳定引用")
    @NotBlank
    @Size(max = MAX_SOURCE_ITEM_REF_LENGTH)
    private String sourceItemRef;

    @Schema(description = "规范化不可变来源事实的 64 位小写 SHA-256")
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentDigest;
}

package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账来源质量。
 *
 * <p>职责：表达外部文件、流水、回单或报表来源是否完成验证、解析和去重。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationSourceQuality implements DescriptiveEnum {

    /**
     * 来源已验证并可用于匹配。
     */
    VERIFIED("已验证"),

    /**
     * 来源未验证。
     */
    UNVERIFIED("未验证"),

    /**
     * 来源解析失败。
     */
    PARSE_FAILED("解析失败"),

    /**
     * 来源重复。
     */
    DUPLICATED("重复来源"),

    /**
     * 缺少主体或账户映射。
     */
    MISSING_SUBJECT_MAPPING("缺少主体映射");

    private final String desc;
}

package com.wind.funds.governance.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 持久投影重放任务状态。
 *
 * @author wuxp
 * @since 2026-08-04
 */
@Getter
@AllArgsConstructor
public enum ProjectionReplayTaskStatus implements DescriptiveEnum {

    /**
     * 已创建，等待执行或继续重放。
     */
    CREATED("已创建"),

    /**
     * 正在执行投影重放。
     */
    RUNNING("执行中"),

    /**
     * 投影重放已经完成。
     */
    COMPLETED("已完成");

    private final String desc;
}

package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账差错生命周期状态。
 *
 * <p>职责：表达差错从发现、阻断、处理、重跑到关闭的服务端事实状态。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationDifferenceStatus implements DescriptiveEnum {

    /**
     * 已阻断相关清分、清算、结算、出款或报表对象。
     */
    BLOCKED("已阻断"),

    /**
     * 已关联调账、冲正、挂账、追偿或核销处理动作。
     */
    ADJUSTING("处理中"),

    /**
     * 处理后重新对账中。
     */
    RECONCILING("重新对账中"),

    /**
     * 依赖的来源、解析或匹配证据已被后继批次确认无效。
     */
    INVALIDATED("证据已失效"),

    /**
     * 重新对账通过并关闭。
     */
    RESOLVED("已解决");

    private final String desc;
}

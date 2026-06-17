package com.wind.funds.spec.ledger;

import org.jspecify.annotations.NonNull;

/**
 * 受控负可用余额策略。
 *
 * @author Codex
 * @date 2026-06-16
 */
public interface NegativeAvailablePolicySpec {

    /**
     * @return 策略编码
     */
    @NonNull
    String getPolicyCode();

    /**
     * @return 策略版本
     */
    @NonNull
    Integer getPolicyVersion();

    /**
     * @return 是否必须关联来源事实
     */
    @NonNull
    Boolean getRequireSourceFact();

    /**
     * @return 是否必须记录负余额原因
     */
    @NonNull
    Boolean getRequireReason();

    /**
     * @return 是否必须有审批或风控规则依据
     */
    @NonNull
    Boolean getRequireApprovalOrRiskRule();

    /**
     * @return 是否必须记录风险状态
     */
    @NonNull
    Boolean getRequireRiskStatus();

    /**
     * @return 是否必须配置单笔上限
     */
    @NonNull
    Boolean getRequireSingleLimit();

    /**
     * @return 是否必须配置累计上限
     */
    @NonNull
    Boolean getRequireCumulativeLimit();

    /**
     * @return 是否必须跟踪账龄
     */
    @NonNull
    Boolean getRequireAgingTracking();

    /**
     * @return 后续交易是否必须重新校验策略
     */
    @NonNull
    Boolean getRecheckFutureTransaction();

    /**
     * @return 后续治理路径
     */
    @NonNull
    String getGovernancePath();
}

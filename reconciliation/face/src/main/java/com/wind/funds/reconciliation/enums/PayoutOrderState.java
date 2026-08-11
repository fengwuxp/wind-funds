package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款单资金事实状态。
 *
 * <p>{@link #SUBMITTED}、{@link #ACCEPTED} 和 {@link #PROCESSING} 均不表示收款方已经到账。</p>
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "出款单资金事实生命周期状态")
@Getter
@AllArgsConstructor
public enum PayoutOrderState implements DescriptiveEnum {

    /** 出款单已创建，尚未提交外部通道。 */
    CREATED("已创建"),
    /** 提交请求已被本系统持久化。 */
    SUBMITTED("已提交"),
    /** 外部通道已经受理。 */
    ACCEPTED("外部已受理"),
    /** 外部通道正在处理。 */
    PROCESSING("外部处理中"),
    /** 外部通道确认出款成功。 */
    SUCCEEDED("出款成功"),
    /** 外部通道确认出款失败。 */
    FAILED("出款失败"),
    /** 出款成功后资金被外部退回。 */
    RETURNED("外部退回"),
    /** 外部回执与本地出款事实不一致。 */
    MISMATCHED("回单不一致"),
    /** 出款单在允许阶段被取消。 */
    CANCELLED("已取消");

    private final String desc;
}

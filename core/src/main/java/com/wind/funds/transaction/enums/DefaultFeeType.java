package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 费用类型（Fee Type）
 * 用于描述资金交易过程中产生的“附加成本结构”，表示一笔交易中为什么会额外产生费用，以及费用的经济学性质。
 * 职责：
 * 1. 描述费用的来源类型（服务费 / 汇率费 / 风控费等）
 * 2. 用于成本拆分与利润核算（收入 vs 成本）
 * 3. 支撑分润、对账、清结算体系
 * 与其他模型关系：
 * - 依附于某个 Transaction（主交易或独立 fee 交易）
 * - 可拆分为多条 fee ledger entry
 *
 * @author wuxp
 * @date 2026-04-16 09:25
 **/
@AllArgsConstructor
@Getter
public enum DefaultFeeType implements DescriptiveEnum {

    /**
     * 无手续费
     */
    NONE("无手续费"),

    /**
     * 平台服务成本
     */
    @Schema(description = "手续费")
    FEE("手续费"),

    /**
     * 惩罚性/附加成本
     */
    @Schema(description = "附加费")
    SURCHARGE_FEE("附加费"),

    /**
     * 退款手续费（退款产生的费用）
     */
    REFUND_FEE("退款手续费"),

    /**
     * 卡组织/通道成本
     */
    NETWORK_FEE("网络手续费（网络费用）"),

    /**
     * 调账类费用（人工/系统修正费用）
     */
    ADJUSTMENT_FEE("调账费用");

    private final String desc;

    public String getCode() {
        return name();
    }
}

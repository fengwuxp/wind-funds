package com.wind.funds.route.spec;

import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Route 原子资金步骤。
 *
 * <p>职责：
 * <ul>
 *   <li>描述一笔路径中的最小资金移动单元</li>
 *   <li>承载来源/去向、金额和回放策略</li>
 *   <li>作为 LedgerPostingAssembler 生成 LedgerPostingPlan 的基础输入</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>RouteLeg 不是会计分录</li>
 *   <li>RouteLeg 不直接决定账本余额，只决定资金移动语义</li>
 * </ul>
 */
public interface RouteLegSpec {

    @NonNull
    String getLegId();

    default int getSequence() {
        return 0;
    }

    @NonNull
    RouteLegType getLegType();

    @NonNull
    RouteNodeSpec getSourceNode();

    @NonNull
    RouteNodeSpec getTargetNode();

    @NonNull
    Money getAmount();

    @NonNull
    default Money getOriginalAmount() {
        return getAmount();
    }

    @NonNull
    default BigDecimal getExchangeRate() {
        return BigDecimal.ONE;
    }

    @NonNull
    default RouteReplayPolicy getReplayPolicy() {
        return RouteReplayPolicy.FULL_ONLY;
    }

    @Nullable
    default String getReplayRefLegId() {
        return null;
    }

    @Nullable
    default String getDescription() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}

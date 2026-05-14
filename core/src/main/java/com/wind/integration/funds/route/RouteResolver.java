package com.wind.integration.funds.route;

import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;

/**
 * 资金路径解析器。
 *
 * <p>职责：
 * <ul>
 *   <li>把资金指令解析为运行态资金路径</li>
 *   <li>根据交易类型、主体关系、外部账户、支付工具等信息选择 Route</li>
 *   <li>输出 LedgerPostingAssembler 可消费的 ResolvedRouteSpec</li>
 * </ul>
 *
 * <p>非职责：
 * <ul>
 *   <li>不生成账本分录</li>
 *   <li>不写入交易流水</li>
 *   <li>不更新余额</li>
 * </ul>
 */
public interface RouteResolver {

    /**
     * 判断当前解析器是否支持该资金指令。
     *
     * <p>能力范围：仅用于组合解析器选择委托实现，不应在该方法内执行路由解析、外部查询或状态变更。</p>
     *
     * @param instruction 资金指令
     * @return true 表示可处理该指令
     */
    boolean support(@NonNull FundsInstructionSpec instruction);

    /**
     * 解析资金路径。
     *
     * <p>能力范围：负责生成 ResolvedRouteSpec，包括参与方、路径步骤、路由决策、外部账户和平台账户快照。
     * 不负责生成 LedgerEntry，也不负责持久化 RouteSnapshot。</p>
     *
     * @param instruction 资金指令
     * @return 已解析资金路径
     */
    @NonNull
    ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction);
}

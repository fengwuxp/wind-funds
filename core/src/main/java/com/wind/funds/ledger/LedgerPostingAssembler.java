package com.wind.funds.ledger;

import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;

/**
 * Route 到 Posting DSL 的翻译器。
 *
 * <p>职责：
 * <ul>
 *   <li>把已解析资金路径翻译为完整 LedgerTransactionSpec</li>
 *   <li>根据 RouteLeg 生成 PostingPlan、PostingPhase 和 LedgerEntry</li>
 *   <li>在组装阶段完成必要的账务平衡校验</li>
 * </ul>
 *
 * <p>非职责：
 * <ul>
 *   <li>不重新路由</li>
 *   <li>不改变业务交易生命周期</li>
 *   <li>不执行账本写入和余额投影</li>
 * </ul>
 */
public interface LedgerPostingAssembler<R extends ResolvedRouteSpec> {

    /**
     * 生成账本交易。
     *
     * <p>能力范围：基于资金指令、资金交易流水号和 ResolvedRoute 生成账本可执行对象。
     * 返回对象必须包含完整 PostingPlan，并满足借贷平衡约束。</p>
     *
     * @param instruction 资金指令
     * @param fundsTransactionSn 资金交易流水号
     * @param resolvedRoute 已解析资金路径
     * @return 账本交易定义
     */
    @NonNull
    LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                   @NonNull String fundsTransactionSn,
                                   @NonNull R resolvedRoute);

    /**
     * 是否支持该已解析路径。
     *
     * <p>能力范围：仅用于 CompositeLedgerPostingAssembler 委托分发，不应执行账本查询、分录生成或状态变更。</p>
     *
     * @param resolvedRoute 已解析资金路径
     * @return true 表示可处理该路径
     */
    boolean supports(@NonNull ResolvedRouteSpec resolvedRoute);

}

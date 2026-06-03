package com.wind.funds.transaction;


import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;

/**
 * 资金指令编排器（Funds Instruction Orchestrator）
 * <p>
 * 职责：
 * <ul>
 *   <li>接收业务侧资金交易意图（{@link FundsInstructionSpec}）</li>
 *   <li>统一编排资金交易流程：路由 → 快照 → 生命周期 → 账本交易 → 入账</li>
 *   <li>协调业务交易生命周期和账本写入结果</li>
 *   <li>生成并提交账本交易（{@link LedgerTransactionSpec}）</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不直接决定 Route，Route 由 RouteResolver 负责</li>
 *   <li>不直接拼装 LedgerEntry，账务翻译由 LedgerPostingAssembler 负责</li>
 *   <li>不承担跨系统重试、清算批处理或业务订单状态机</li>
 * </ul>
 *
 * @author wuxp
 * @date 2026-04-15
 */
public interface FundsInstructionOrchestrator<E extends FundsInstructionSpec> {

    /**
     * 处理资金指令，执行完整交易流程。
     *
     * <p>能力范围：返回标准资金交易流水号，用于业务侧查询交易聚合记录。
     * 具体实现应保证同一事务内处理账本入账成功/失败后的生命周期归纳。</p>
     *
     * @param spec 资金交易描述（业务意图）
     * @return 资金交易流水号（全局唯一）
     */
    @NonNull
    String execute(@NonNull E spec);

    /**
     * 是否支持该指令。
     *
     * <p>能力范围：仅用于编排器选择，不执行交易处理。</p>
     *
     * @param specType 资金交易描述类类型
     * @return true：支持；false：不支持
     */
    boolean supports(@NonNull Class<E> specType);
}

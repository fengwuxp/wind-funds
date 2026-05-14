package com.wind.integration.funds.ledger;

import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 组合 LedgerPostingAssembler。
 *
 * <p>职责：
 * <ul>
 *   <li>在多个 LedgerPostingAssembler 实现之间做委托分发</li>
 *   <li>选择第一个支持当前路径的具体组装器执行组装</li>
 * </ul>
 *
 * <p>非职责：
 * <ul>
 *   <li>不自行组装账本交易</li>
 *   <li>不重新路由</li>
 * </ul>
 */
@AllArgsConstructor
@Component
@Primary
public class CompositeLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec> {

    private final List<LedgerPostingAssembler<? extends ResolvedRouteSpec>> delegates;

    /**
     * 委托具体 LedgerPostingAssembler 生成账本交易。
     *
     * <p>能力范围：按 Spring 注入顺序选择第一个 support=true 的委托实现。
     * 当前组合器不自行组装账本交易，也不会兜底生成空交易。</p>
     *
     * @param instruction 资金指令
     * @param fundsTransactionSn 资金交易流水号
     * @param resolvedRoute 已解析资金路径
     * @return 账本交易定义
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                   @NonNull String fundsTransactionSn,
                                                   @NonNull ResolvedRouteSpec resolvedRoute) {
        for (LedgerPostingAssembler delegate : availableDelegates()) {
            if (delegate.support(resolvedRoute)) {
                return delegate.assemble(instruction, fundsTransactionSn, resolvedRoute);
            }
        }
        throw new IllegalArgumentException("Not found supported LedgerPostingAssembler");
    }

    /**
     * 判断是否存在可处理当前路径的委托实现。
     *
     * <p>能力范围：仅做委托可用性判断，不触发账本查询和交易组装。</p>
     *
     * @param resolvedRoute 已解析资金路径
     * @return true 表示存在可用委托
     */
    @Override
    public boolean support(@NonNull ResolvedRouteSpec resolvedRoute) {
        // support 仅用于组合器判断是否存在可委托实现。
        return availableDelegates().stream().anyMatch(delegate -> delegate.support(resolvedRoute));
    }

    private List<LedgerPostingAssembler<? extends ResolvedRouteSpec>> availableDelegates() {
        return delegates.stream()
                .filter(delegate -> delegate != this)
                .toList();
    }
}

package com.capte.funds.transaction;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.RouteSnapshotFactory;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.FundsInstructionOrchestrator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认资金指令编排器。
 *
 * <p>职责：
 * <ul>
 *   <li>串联资金指令处理主链路：路由、快照、生命周期保存、账本组装、账本入账</li>
 *   <li>在同一本地事务内协调业务交易生命周期和账本写入结果</li>
 *   <li>对无账务影响或已完成的指令进行短路处理</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不自行解析资金路径，路径解析委托 RouteResolver</li>
 *   <li>不自行生成账本交易，账务翻译委托 LedgerPostingAssembler</li>
 *   <li>不直接操作账本表，账本写入委托 LedgerTransactionPostingService</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class DefaultRoutedFundsInstructionOrchestrator implements FundsInstructionOrchestrator<FundsInstructionSpec> {

    private final RouteResolver routeResolver;

    private final RouteSnapshotFactory routeSnapshotFactory;

    private final LedgerPostingAssembler<ResolvedRouteSpec> postingAssembler;

    private final LedgerTransactionPostingService ledgerTransactionPostingService;

    private final FundsInstructionLifecycleRecorder fundsInstructionLifecycleRecorder;

    /**
     * 执行资金指令主流程。
     *
     * <p>能力范围：解析 Route、创建 RouteSnapshot、保存业务生命周期、组装账本交易并提交入账。
     * 返回值是本次生命周期事实流水号，不是账本交易流水号。</p>
     *
     * @param instruction 资金指令
     * @return 资金交易流水号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String execute(@NonNull FundsInstructionSpec instruction) {
        ResolvedRouteSpec resolvedRoute = routeResolver.resolve(instruction);
        RouteSnapshotSpec routeSnapshot = routeSnapshotFactory.createSnapshot(resolvedRoute);
        FundsInstructionLifecycleResult lifecycleResult = fundsInstructionLifecycleRecorder.beforePosting(instruction,
                resolvedRoute, routeSnapshot);
        if (lifecycleResult.isCompleted()) {
            return lifecycleResult.getTransactionSn();
        }
        if (resolvedRoute.getLegs().isEmpty()) {
            fundsInstructionLifecycleRecorder.markSucceeded(instruction, lifecycleResult, null);
            return lifecycleResult.getTransactionSn();
        }
        LedgerTransactionSpec transaction = postingAssembler.assemble(instruction, lifecycleResult.getTransactionSn(),
                resolvedRoute);
        try {
            ledgerTransactionPostingService.post(transaction);
            fundsInstructionLifecycleRecorder.markSucceeded(instruction, lifecycleResult, transaction.getSn());
            return lifecycleResult.getTransactionSn();
        } catch (Throwable throwable) {
            fundsInstructionLifecycleRecorder.markFailed(instruction, lifecycleResult, throwable);
            throw throwable;
        }
    }

    /**
     * 判断当前编排器是否支持指定资金指令类型。
     *
     * <p>能力范围：仅用于上层选择编排器，不执行任何业务逻辑。</p>
     *
     * @param specType 资金指令类型
     * @return true 表示支持 FundsInstructionSpec 及其子类型
     */
    @Override
    public boolean supports(@NonNull Class<FundsInstructionSpec> specType) {
        return FundsInstructionSpec.class.isAssignableFrom(specType);
    }
}

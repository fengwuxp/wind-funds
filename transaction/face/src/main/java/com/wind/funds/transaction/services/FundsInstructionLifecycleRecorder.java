package com.wind.funds.transaction.services;

import com.wind.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 资金指令业务生命周期记录服务。
 *
 * <p>职责：
 * <ul>
 *   <li>在账本入账前后记录资金交易、冻结订单等生命周期事实</li>
 *   <li>保存 RouteSnapshot，用于后续审计和原路径回放</li>
 *   <li>为编排器提供幂等复用和结果归纳能力</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不生成 Route</li>
 *   <li>不生成账本分录</li>
 *   <li>不执行账本写入</li>
 * </ul>
 */
public interface FundsInstructionLifecycleRecorder {

    /**
     * 判断当前记录器是否负责该资金指令的生命周期事实。
     *
     * <p>能力范围：只判断事实载体归属，不查询数据库、不创建事实。</p>
     *
     * @param instruction 资金指令
     * @return true 表示可处理该指令
     */
    boolean supports(@NonNull FundsInstructionSpec instruction);

    /**
     * 账务入账前创建或复用业务交易与生命周期明细。
     *
     * <p>能力范围：根据资金指令、已解析路径和路径快照创建或复用标准资金交易记录。
     * 如果发现相同业务事件已完成，应返回 completed=true 让编排器短路。</p>
     *
     * @param instruction 资金指令
     * @param resolvedRoute 已解析资金路径
     * @param routeSnapshot 路径事实快照
     * @return 生命周期处理结果
     */
    @NonNull
    FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                  @NonNull ResolvedRouteSpec resolvedRoute,
                                                  @NonNull RouteSnapshotSpec routeSnapshot);

    /**
     * 标记生命周期明细处理成功。
     *
     * <p>能力范围：归纳资金交易金额字段和状态，关联账本交易流水。
     * 不负责校验账本交易平衡，也不负责重新投影余额。</p>
     *
     * @param instruction 资金指令
     * @param result 入账前生命周期处理结果
     * @param ledgerTransactionSn 账本交易流水号，无账务影响时允许为空
     */
    void markSucceeded(@NonNull FundsInstructionSpec instruction,
                       @NonNull FundsInstructionLifecycleResult result,
                       @Nullable String ledgerTransactionSn);

    /**
     * 标记生命周期明细处理失败。
     *
     * <p>能力范围：记录失败原因并更新仍处于处理中的明细状态。
     * 已处于稳定状态的聚合交易不应被后续失败事件覆盖。</p>
     *
     * @param instruction 资金指令
     * @param result 入账前生命周期处理结果
     * @param cause 失败原因
     */
    void markFailed(@NonNull FundsInstructionSpec instruction,
                    @NonNull FundsInstructionLifecycleResult result,
                    @NonNull Throwable cause);

}

package com.wind.funds.transaction.services;

import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 资金交易事实查询服务。
 *
 * <p>职责：读取已经落库的资金交易事实，为 replay 和运营查询提供稳定输入。</p>
 */
public interface FundsTransactionQueryService {

    /**
     * 查询主交易事实。
     *
     * @param transactionSn 资金交易流水号
     * @return 已保存主交易；不存在时返回 empty
     */
    @NonNull
    Optional<FundsTransactionDTO> queryFundsTransaction(@NonNull String transactionSn);

    /**
     * 按业务场景和业务流水查询主交易事实。
     *
     * <p>能力范围：用于运营审计、交易解释和幂等追踪等只读场景，查询已经落库的主交易事实。
     * 如果不存在对应交易，返回 empty；不负责生成、修复或重放交易事实。</p>
     *
     * @param tenantId 租户 ID
     * @param businessScene 业务场景
     * @param businessSn 业务流水号
     * @return 已保存主交易；不存在时返回 empty
     */
    @NonNull
    Optional<FundsTransactionDTO> findFundsTransactionByBusiness(@NonNull Long tenantId,
                                                                 @NonNull String businessScene,
                                                                 @NonNull String businessSn);

    /**
     * 查询交易明细事实。
     *
     * <p>按主键升序返回该交易下已有主体明细；不存在明细时返回空列表。</p>
     *
     * @param transactionSn 资金交易流水号
     * @return 交易明细事实列表
     */
    @NonNull
    List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull String transactionSn);

    /**
     * 判断指定 replay leg 是否已被成功消费。
     *
     * <p>能力范围：用于 `REPLAY_ONCE` 幂等边界。实现侧应基于成功的交易明细或账本交易事实判断，
     * 不依赖当前 RouteResolver 重新推导。</p>
     *
     * @param referenceTransactionSn 原资金交易流水号
     * @param eventType 本次 replay 事件语义
     * @param replayRefLegId 原 RouteLeg ID
     * @return true 表示同一原 RouteLeg 已在该 replay 事件下成功消费
     */
    boolean hasConsumedReplayLeg(@NonNull String referenceTransactionSn,
                                 @NonNull FundsTransactionEventType eventType,
                                 @NonNull String replayRefLegId);

    /**
     * 汇总指定 replay leg 已成功消费金额。
     *
     * <p>能力范围：用于后续退款、手续费退回、拒付等 replay 累计上限校验。实现侧应基于成功的交易明细或
     * 账本交易事实判断，并按原 RouteLeg 去重，避免同一 replay 生成的多主体明细重复累计。</p>
     *
     * @param referenceTransactionSn 原资金交易流水号
     * @param eventType 本次 replay 事件语义
     * @param replayRefLegId 原 RouteLeg ID
     * @param currency 原 RouteLeg 币种
     * @return 已成功消费金额；不存在时返回 0
     */
    @NonNull
    Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                     @NonNull FundsTransactionEventType eventType,
                                     @NonNull String replayRefLegId,
                                     @NonNull CurrencyIsoCode currency);

    /**
     * 汇总指定 replay leg 已成功消费金额，并可排除当前幂等业务事件。
     *
     * <p>用于路由解析阶段做来源级剩余额度校验，同时允许同一 `businessScene + businessSn`
     * 的幂等重试继续进入生命周期记录器做请求摘要一致性校验。</p>
     *
     * @param referenceTransactionSn 原资金交易或冻结单流水号
     * @param eventType 本次 replay 或引用消费事件语义
     * @param replayRefLegId 原 RouteLeg ID
     * @param currency 原 RouteLeg 币种
     * @param excludedBusinessScene 需要排除的业务场景；为空时不排除
     * @param excludedBusinessSn 需要排除的业务流水；为空时不排除
     * @return 已成功消费金额；不存在时返回 0
     */
    @NonNull
    default Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                             @NonNull FundsTransactionEventType eventType,
                                             @NonNull String replayRefLegId,
                                             @NonNull CurrencyIsoCode currency,
                                             @Nullable String excludedBusinessScene,
                                             @Nullable String excludedBusinessSn) {
        return sumConsumedReplayLegAmount(referenceTransactionSn, eventType, replayRefLegId, currency);
    }

    /**
     * 查询已保存的 RouteSnapshot。
     *
     * <p>能力范围：按资金交易流水号读取该交易首次解析得到的路径快照，用于后续撤销、结算、
     * 退款或拒付沿原路径回放。不负责生成新路径，也不负责校验可撤销或可退款金额。</p>
     *
     * @param transactionSn 资金交易流水号
     * @return 已保存路径快照；不存在或未保存时返回 empty
     */
    @NonNull
    Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn);

    /**
     * 通过冻结单号查询原冻结动作对应的 RouteSnapshot。
     *
     * <p>能力范围：当后续业务以 `FREEZE_ORDER` 作为引用时，用冻结单号定位原冻结资金交易，
     * 再返回该资金交易保存的路径快照，用于沿原冻结路径释放。若冻结单不存在、未绑定交易号、
     * 或交易未保存 RouteSnapshot，则返回 empty。</p>
     *
     * @param freezeOrderSn 冻结单号
     * @return 原冻结路径快照；不存在时返回 empty
     */
    @NonNull
    Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn);
}

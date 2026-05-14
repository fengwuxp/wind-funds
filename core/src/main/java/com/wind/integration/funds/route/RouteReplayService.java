package com.wind.integration.funds.route;

import com.wind.integration.funds.route.spec.ReplayRequestSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.jspecify.annotations.NonNull;

/**
 * 路径回放服务。
 *
 * <p>职责：基于历史 RouteSnapshot 生成后续生命周期事件需要的反向或部分路径。</p>
 *
 * <p>边界：回放必须沿原路径约束执行，不应重新选择资金来源、平台账户或外部通道。</p>
 */
public interface RouteReplayService {

    /**
     * 回放路径快照。
     *
     * <p>能力范围：根据退款、撤销、结算等回放请求，从原 RouteSnapshot 派生新的 ResolvedRoute。
     * 不负责保存新快照，也不负责账本入账。</p>
     *
     * @param snapshot 原路径快照
     * @param replayRequest 回放请求
     * @return 回放得到的已解析路径
     */
    @NonNull
    ResolvedRouteSpec replay(@NonNull RouteSnapshotSpec snapshot, @NonNull ReplayRequestSpec replayRequest);
}

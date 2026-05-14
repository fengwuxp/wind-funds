package com.wind.integration.funds.route;

import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.jspecify.annotations.NonNull;

/**
 * 路径快照工厂。
 *
 * <p>职责：把运行态 ResolvedRoute 固化为可保存、可审计、可回放的 RouteSnapshot。</p>
 *
 * <p>边界：只负责快照构造，不负责快照落库、不重新路由、不生成账本分录。</p>
 */
public interface RouteSnapshotFactory {

    /**
     * 创建路径事实快照。
     *
     * <p>能力范围：复制 ResolvedRoute 中的路径、参与方、路由决策和外部引用信息。
     * 生成的快照应可用于退款、撤销和结算回放。</p>
     *
     * @param resolvedRoute 已解析资金路径
     * @return 路径事实快照
     */
    @NonNull
    RouteSnapshotSpec createSnapshot(@NonNull ResolvedRouteSpec resolvedRoute);
}

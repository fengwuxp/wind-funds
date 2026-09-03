package com.wind.funds.transaction.projection;

import com.wind.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影正常发布上下文。
 *
 * <p>职责：承载一次资金指令主写链路成功后的交易事实、route snapshot 和生命周期结果，
 * 供交易投影构建用户账单、商户账单、运营时间线或财务视图。</p>
 *
 * <p>边界：该上下文只用于正常只读投影发布，不表达投影重放、余额修复、交易补单或账务补账。</p>
 */
@Builder
public record FundsTransactionProjectionPublishContext(@NonNull FundsInstructionSpec instruction,
                                                       @NonNull ResolvedRouteSpec resolvedRoute,
                                                       @NonNull RouteSnapshotSpec routeSnapshot,
                                                       @NonNull FundsInstructionLifecycleResult lifecycleResult) {
}

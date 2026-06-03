package com.wind.funds.route.spec;

import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 冻结后的路径事实快照。
 *
 * <p>职责：
 * <ul>
 *   <li>固化一次已解析资金路径的事实结果</li>
 *   <li>为退款、撤销、冲正、回放等后续流程提供原路径依据</li>
 *   <li>作为审计、重放和风控复核的稳定输入</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>RouteSnapshot 不参与账本分录生成</li>
 *   <li>RouteSnapshot 不负责重新路由决策</li>
 * </ul>
 */
public interface RouteSnapshotSpec {

    @Nullable
    Long getTenantId();

    @NonNull
    String getSnapshotId();

    @NonNull
    String getSnapshotSchemaVersion();

    @NonNull
    String getRouteCode();

    @NonNull
    String getRouteVersion();

    @NonNull
    String getBusinessScene();

    @NonNull
    String getBusinessSn();

    @NonNull
    FundsInstructionType getInstructionType();

    @NonNull
    FundsTransactionEventType getEventType();

    @NonNull
    DefaultFundsTransactionType getTransactionType();

    @NonNull
    List<RouteParticipantSpec> getParticipants();

    @NonNull
    List<RouteLegSpec> getLegs();

    @Nullable
    default RoutingDecisionSpec getRoutingDecision() {
        return null;
    }

    @Nullable
    default PaymentInstrumentRefSpec getPaymentInstrumentRef() {
        return null;
    }

    @Nullable
    default ExternalAccountRefSpec getExternalAccountRef() {
        return null;
    }

    @Nullable
    default PlatformAccountsSnapshotSpec getPlatformAccounts() {
        return null;
    }

    @NonNull
    LocalDateTime getResolvedAt();

    @Nullable
    default LocalDateTime getExpiresAt() {
        return null;
    }

    @Nullable
    default String getDescription() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}

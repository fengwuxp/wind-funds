package com.wind.integration.funds.route.spec;

import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 运行态已解析资金路径。
 *
 * <p>职责：
 * <ul>
 *   <li>表达一次资金指令经过规则解析后的资金流路径</li>
 *   <li>保留参与主体、路径步骤、外部账户、支付工具和平台账户选择结果</li>
 *   <li>作为 LedgerPostingAssembler 的输入，翻译为可执行账本交易</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>ResolvedRoute 只描述“钱怎么流”，不直接描述借贷分录</li>
 *   <li>ResolvedRoute 是运行态结果，持久化审计应使用 RouteSnapshot</li>
 * </ul>
 */
public interface ResolvedRouteSpec {

    @Nullable
    Long getTenantId();

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

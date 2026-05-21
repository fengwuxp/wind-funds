package com.wind.integration.funds.spec.transaction;

import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 资金指令上的权益结果快照。
 *
 * <p>只承接业务侧已决策的优惠券、代金券、补贴、商户让利等结果，不计算券规则。</p>
 */
public interface FundsBenefitSnapshotSpec {

    @NonNull
    String getBenefitSnapshotId();

    @NonNull
    default String getBenefitSchemaVersion() {
        return "1.0";
    }

    @NonNull
    String getBenefitGroupSn();

    @Nullable
    default String getOrderSn() {
        return null;
    }

    @Nullable
    default String getPricingSnapshotSn() {
        return null;
    }

    @NonNull
    Money getOrderAmount();

    @NonNull
    Money getUserPayAmount();

    @Nullable
    default Money getMerchantReceivableAmount() {
        return null;
    }

    @NonNull
    List<FundsBenefitComponentSpec> getComponents();

    @Nullable
    default FundsBenefitRefundPolicySpec getRefundPolicy() {
        return null;
    }

    @Nullable
    default String getDecisionSource() {
        return null;
    }

    @Nullable
    default String getDecisionTraceId() {
        return null;
    }

    @NonNull
    Map<String, Object> getContextVariables();
}

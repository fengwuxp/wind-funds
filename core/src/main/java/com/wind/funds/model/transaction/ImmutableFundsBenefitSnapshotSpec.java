package com.wind.funds.model.transaction;

import com.wind.funds.spec.transaction.FundsBenefitComponentSpec;
import com.wind.funds.spec.transaction.FundsBenefitRefundPolicySpec;
import com.wind.funds.spec.transaction.FundsBenefitSnapshotSpec;
import com.wind.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 不可变权益结果快照实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundsBenefitSnapshotSpec(String benefitSnapshotId,
                                                String benefitSchemaVersion,
                                                String benefitGroupSn,
                                                @Nullable String orderSn,
                                                @Nullable String pricingSnapshotSn,
                                                Money orderAmount,
                                                Money userPayAmount,
                                                @Nullable Money merchantReceivableAmount,
                                                List<FundsBenefitComponentSpec> components,
                                                @Nullable FundsBenefitRefundPolicySpec refundPolicy,
                                                @Nullable String decisionSource,
                                                @Nullable String decisionTraceId,
                                                Map<String, Object> contextVariables)
        implements FundsBenefitSnapshotSpec {

    public ImmutableFundsBenefitSnapshotSpec {
        FundsBenefitSpecValidators.requireText(benefitSnapshotId,
                "fundsBenefit.benefitSnapshotId must not be blank");
        benefitSchemaVersion = StringUtils.hasText(benefitSchemaVersion)
                ? benefitSchemaVersion
                : "1.0";
        FundsBenefitSpecValidators.requireText(benefitGroupSn, "fundsBenefit.benefitGroupSn must not be blank");
        if (orderAmount == null) {
            throw new IllegalArgumentException("fundsBenefit.orderAmount must not be null");
        }
        if (userPayAmount == null) {
            throw new IllegalArgumentException("fundsBenefit.userPayAmount must not be null");
        }
        if (orderAmount.getAmount() <= 0) {
            throw new IllegalArgumentException("fundsBenefit.orderAmount must be positive");
        }
        if (userPayAmount.getAmount() < 0) {
            throw new IllegalArgumentException("fundsBenefit.userPayAmount must not be negative");
        }
        if (merchantReceivableAmount != null && merchantReceivableAmount.getAmount() < 0) {
            throw new IllegalArgumentException("fundsBenefit.merchantReceivableAmount must not be negative");
        }
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("fundsBenefit.components must not be empty");
        }
        components = List.copyOf(components);
        validateComponents(orderAmount, userPayAmount, components);
        contextVariables = FundsBenefitSpecValidators.immutableContext(contextVariables, "fundsBenefit.snapshot");
    }

    @Override
    public @NonNull String getBenefitSnapshotId() {
        return benefitSnapshotId;
    }

    @Override
    public @NonNull String getBenefitSchemaVersion() {
        return benefitSchemaVersion;
    }

    @Override
    public @NonNull String getBenefitGroupSn() {
        return benefitGroupSn;
    }

    @Override
    public @Nullable String getOrderSn() {
        return orderSn;
    }

    @Override
    public @Nullable String getPricingSnapshotSn() {
        return pricingSnapshotSn;
    }

    @Override
    public @NonNull Money getOrderAmount() {
        return orderAmount;
    }

    @Override
    public @NonNull Money getUserPayAmount() {
        return userPayAmount;
    }

    @Override
    public @Nullable Money getMerchantReceivableAmount() {
        return merchantReceivableAmount;
    }

    @Override
    public @NonNull List<FundsBenefitComponentSpec> getComponents() {
        return components;
    }

    @Override
    public @Nullable FundsBenefitRefundPolicySpec getRefundPolicy() {
        return refundPolicy;
    }

    @Override
    public @Nullable String getDecisionSource() {
        return decisionSource;
    }

    @Override
    public @Nullable String getDecisionTraceId() {
        return decisionTraceId;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    private static void validateComponents(Money orderAmount,
                                           Money userPayAmount,
                                           List<FundsBenefitComponentSpec> components) {
        Set<String> componentSns = new HashSet<>();
        long orderDiscountAmount = 0L;
        for (FundsBenefitComponentSpec component : components) {
            if (component == null) {
                throw new IllegalArgumentException("fundsBenefit.components must not contain null");
            }
            if (!componentSns.add(component.getComponentSn())) {
                throw new IllegalArgumentException("fundsBenefit.componentSn must be unique");
            }
            if (!orderAmount.getCurrency().equals(component.getAmount().getCurrency())) {
                throw new IllegalArgumentException("fundsBenefit.component amount currency must equal orderAmount");
            }
            if (component.getClosureRole() == FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE) {
                orderDiscountAmount = Math.addExact(orderDiscountAmount, component.getAmount().getAmount());
            }
        }
        if (!orderAmount.getCurrency().equals(userPayAmount.getCurrency())) {
            throw new IllegalArgumentException("fundsBenefit.userPayAmount currency must equal orderAmount");
        }
        long closedAmount = Math.addExact(userPayAmount.getAmount(), orderDiscountAmount);
        if (closedAmount != orderAmount.getAmount()) {
            throw new IllegalArgumentException(
                    "fundsBenefit amount must close: userPayAmount + ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
        }
    }
}

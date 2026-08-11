package com.wind.funds.route.model;

import com.wind.funds.fx.FxAppliedRate;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 不可变 RouteLeg 实现。
 */
@Builder
@FieldNameConstants
public record ImmutableRouteLegSpec(String legId,
                                    int sequence,
                                    RouteLegType legType,
                                    RouteNodeSpec sourceNode,
                                    RouteNodeSpec targetNode,
                                    Money amount,
                                    @Nullable Money originalAmount,
                                    @Nullable BigDecimal exchangeRate,
                                    @Nullable RouteReplayPolicy replayPolicy,
                                    @Nullable String replayRefLegId,
                                    @Nullable String description,
                                    Map<String, Object> contextVariables) implements RouteLegSpec {

    public ImmutableRouteLegSpec {
        validateLedgerPostableNode(sourceNode, "sourceNode");
        validateLedgerPostableNode(targetNode, "targetNode");
        originalAmount = originalAmount == null ? amount : originalAmount;
        exchangeRate = exchangeRate == null ? BigDecimal.ONE : exchangeRate;
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("routeLeg.amount must be positive");
        }
        if (originalAmount.getAmount() <= 0) {
            throw new IllegalArgumentException("routeLeg.originalAmount must be positive");
        }
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("routeLeg.exchangeRate must be positive");
        }
        if (amount.getCurrency() == originalAmount.getCurrency()) {
            if (amount.getAmount() != originalAmount.getAmount()) {
                throw new IllegalArgumentException("routeLeg.originalAmount must equal amount for same currency");
            }
            if (exchangeRate.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("routeLeg.exchangeRate must be 1 for same currency");
            }
        }
        FxAppliedRate.validateSupportedPrecision(exchangeRate);
        contextVariables = RouteContextVariablesValidator.immutableContext(contextVariables, "routeLeg");
    }

    private static void validateLedgerPostableNode(RouteNodeSpec node, String fieldName) {
        RouteNodeType nodeType = node.getNodeType();
        if (nodeType == RouteNodeType.PAYMENT_INSTRUMENT || nodeType == RouteNodeType.EXTERNAL_ACCOUNT) {
            throw new IllegalArgumentException("RouteLeg " + fieldName + " must be ledger-postable");
        }
        if (!node.getSubjectRef().getSubjectType().isLedgerPostable()) {
            throw new IllegalArgumentException("RouteLeg " + fieldName + " must be ledger-postable");
        }
    }

    @Override
    public @NonNull String getLegId() {
        return legId;
    }

    @Override
    public @NonNull RouteLegType getLegType() {
        return legType;
    }

    @Override
    public @NonNull RouteNodeSpec getSourceNode() {
        return sourceNode;
    }

    @Override
    public @NonNull RouteNodeSpec getTargetNode() {
        return targetNode;
    }

    @Override
    public @NonNull Money getAmount() {
        return amount;
    }

    @Override
    public @NonNull Money getOriginalAmount() {
        return originalAmount == null ? amount : originalAmount;
    }

    @Override
    public @NonNull BigDecimal getExchangeRate() {
        return exchangeRate == null ? BigDecimal.ONE : exchangeRate;
    }

    @Override
    public @NonNull RouteReplayPolicy getReplayPolicy() {
        return replayPolicy == null ? RouteReplayPolicy.FULL_ONLY : replayPolicy;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }


    @Override
    public int getSequence() {
        return sequence;
    }

    @Override
    public @Nullable String getReplayRefLegId() {
        return replayRefLegId;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

}

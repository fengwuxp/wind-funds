package com.wind.integration.funds.model.route;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 不可变 RouteLeg 实现。
 */
@Builder
public record ImmutableRouteLegSpec(String legId,
                                    int sequence,
                                    RouteLegType legType,
                                    RouteNodeSpec sourceNode,
                                    RouteNodeSpec targetNode,
                                    Money amount,
                                    @Nullable Money originalAmount,
                                    @Nullable BigDecimal exchangeRate,
                                    LedgerBalanceEffectType balanceEffectType,
                                    LedgerPhaseCode phaseCode,
                                    @Nullable AccountBalancePeriodType periodType,
                                    @Nullable String periodId,
                                    @Nullable RouteReplayPolicy replayPolicy,
                                    @Nullable String replayRefLegId,
                                    Map<String, LedgerBalanceConstraintType> constraintOverrides,
                                    @Nullable String description,
                                    Map<String, Object> contextVariables) implements RouteLegSpec {

    public ImmutableRouteLegSpec {
        constraintOverrides = Map.copyOf(constraintOverrides == null ? Map.of() : constraintOverrides);
        contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
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
    public @NonNull AccountBalancePeriodType getPeriodType() {
        return periodType == null ? AccountBalancePeriodType.LIFETIME : periodType;
    }

    @Override
    public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
        return constraintOverrides;
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
    public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
        return balanceEffectType;
    }

    @Override
    public @NonNull LedgerPhaseCode getPhaseCode() {
        return phaseCode;
    }

    @Override
    public @Nullable String getPeriodId() {
        return periodId;
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

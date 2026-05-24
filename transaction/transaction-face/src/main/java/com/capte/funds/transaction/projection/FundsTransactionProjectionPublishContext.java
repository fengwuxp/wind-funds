package com.capte.funds.transaction.projection;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 交易投影正常发布上下文。
 *
 * <p>职责：承载一次资金指令主写链路成功后的交易事实、route snapshot、账本交易和生命周期结果，
 * 供交易投影构建用户账单、商户账单、运营时间线或财务视图。</p>
 *
 * <p>边界：该上下文只用于正常只读投影发布，不表达投影重放、余额修复、交易补单或账务补账。</p>
 */
@Builder
public record FundsTransactionProjectionPublishContext(@NonNull FundsInstructionSpec instruction,
                                                       @NonNull ResolvedRouteSpec resolvedRoute,
                                                       @NonNull RouteSnapshotSpec routeSnapshot,
                                                       @NonNull FundsInstructionLifecycleResult lifecycleResult,
                                                       @Nullable LedgerTransactionSpec ledgerTransaction) {

    private static final String NOT_APPLICABLE = "N/A";

    private static final String FACT_STATUS_POSTED = "POSTED";

    private static final String FACT_STATUS_HELD = "HELD";

    private static final String FACT_STATUS_RELEASED = "RELEASED";

    private static final String FACT_STATUS_REJECTED = "REJECTED";

    private static final String FACT_STATUS_COMPLETED_NO_LEDGER = "COMPLETED_NO_LEDGER";

    private static final String FACT_STATUS_PROCESSING = "PROCESSING";

    private static final String DISPLAY_STATUS_SUCCEEDED = "SUCCEEDED";

    private static final String DISPLAY_STATUS_AUTHORIZED_HOLD = "AUTHORIZED_HOLD";

    private static final String DISPLAY_STATUS_FROZEN = "FROZEN";

    private static final String DISPLAY_STATUS_RELEASED = "RELEASED";

    private static final String DISPLAY_STATUS_DECLINED = "DECLINED";

    private static final String DISPLAY_STATUS_PROCESSING = "PROCESSING";

    private static final String OPERATION_STATUS_NO_ACTION_REQUIRED = "NO_ACTION_REQUIRED";

    private static final String OPERATION_STATUS_WAITING_CAPTURE_OR_RELEASE = "WAITING_CAPTURE_OR_RELEASE";

    private static final String OPERATION_STATUS_WAITING_UNFREEZE_OR_CONSUME = "WAITING_UNFREEZE_OR_CONSUME";

    private static final String OPERATION_STATUS_WAITING_FACT_COMPLETION = "WAITING_FACT_COMPLETION";

    private static final String STATUS_MEANING_FUNDS_POSTED = "FUNDS_POSTED";

    private static final String STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED =
            "AUTHORIZATION_HELD_NOT_CAPTURED";

    private static final String STATUS_MEANING_AUTHORIZATION_DECLINED_NO_FUNDS_POSTED =
            "AUTHORIZATION_DECLINED_NO_FUNDS_POSTED";

    private static final String STATUS_MEANING_BALANCE_FROZEN_NOT_CONSUMED =
            "BALANCE_FROZEN_NOT_CONSUMED";

    private static final String STATUS_MEANING_FUNDS_RELEASED = "FUNDS_RELEASED";

    private static final String STATUS_MEANING_FACT_PROCESSING = "FACT_PROCESSING";

    private static final String STATUS_MEANING_COMPLETED_WITHOUT_LEDGER = "COMPLETED_WITHOUT_LEDGER";

    private static final String NEXT_ACTION_WAIT_FOR_CAPTURE_OR_RELEASE = "WAIT_FOR_CAPTURE_OR_RELEASE";

    private static final String NEXT_ACTION_WAIT_FOR_UNFREEZE_OR_CONSUME = "WAIT_FOR_UNFREEZE_OR_CONSUME";

    private static final String NEXT_ACTION_WAIT_FOR_FACT_COMPLETION = "WAIT_FOR_FACT_COMPLETION";

    private static final String UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION =
            "AUTHORIZATION_HOLD_IS_NOT_FINAL_CONSUMPTION";

    private static final String UNAVAILABLE_AUTHORIZATION_DECLINED = "AUTHORIZATION_DECLINED";

    private static final String UNAVAILABLE_BALANCE_FREEZE_NOT_CONSUMPTION = "BALANCE_FREEZE_IS_NOT_CONSUMPTION";

    /**
     * 生成面向用户账单、商户账单和运营时间线的只读解释摘要。
     *
     * @return 投影解释摘要
     */
    public @NonNull FundsTransactionProjectionExplanation explanation() {
        String ledgerTransactionSn = resolveLedgerTransactionSn();
        return FundsTransactionProjectionExplanation.builder()
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .fundsTransactionSn(lifecycleResult.getTransactionSn())
                .routeSnapshotId(routeSnapshot.getSnapshotId())
                .routeCode(routeSnapshot.getRouteCode())
                .ledgerTransactionSn(ledgerTransactionSn)
                .factStatus(resolveFactStatus(ledgerTransactionSn))
                .displayStatus(resolveDisplayStatus(ledgerTransactionSn))
                .operationStatus(resolveOperationStatus(ledgerTransactionSn))
                .statusMeaning(resolveStatusMeaning(ledgerTransactionSn))
                .amountSource(resolveAmountSource(ledgerTransactionSn))
                .failureReason(resolveFailureReason(ledgerTransactionSn))
                .unavailableReason(resolveUnavailableReason(ledgerTransactionSn))
                .nextAction(resolveNextAction(ledgerTransactionSn))
                .evidenceRefs(evidenceRefs(lifecycleResult.getTransactionSn(), routeSnapshot.getSnapshotId(),
                        ledgerTransactionSn))
                .externalRuleVerificationStatus(NOT_APPLICABLE)
                .build();
    }

    private @Nullable String resolveLedgerTransactionSn() {
        if (ledgerTransaction != null && StringUtils.hasText(ledgerTransaction.getSn())) {
            return ledgerTransaction.getSn();
        }
        String ledgerTransactionSn = lifecycleResult.getLedgerTransactionSn();
        if (StringUtils.hasText(ledgerTransactionSn)) {
            return ledgerTransactionSn;
        }
        return null;
    }

    private @NonNull String resolveFactStatus(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return FACT_STATUS_PROCESSING;
        }
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn) ? FACT_STATUS_HELD : FACT_STATUS_REJECTED;
            case FREEZE -> FACT_STATUS_HELD;
            case REVERSAL, UNFREEZE -> FACT_STATUS_RELEASED;
            default -> StringUtils.hasText(ledgerTransactionSn)
                    ? FACT_STATUS_POSTED
                    : FACT_STATUS_COMPLETED_NO_LEDGER;
        };
    }

    private @NonNull String resolveDisplayStatus(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return DISPLAY_STATUS_PROCESSING;
        }
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? DISPLAY_STATUS_AUTHORIZED_HOLD
                    : DISPLAY_STATUS_DECLINED;
            case FREEZE -> DISPLAY_STATUS_FROZEN;
            case REVERSAL, UNFREEZE -> DISPLAY_STATUS_RELEASED;
            default -> DISPLAY_STATUS_SUCCEEDED;
        };
    }

    private @NonNull String resolveOperationStatus(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return OPERATION_STATUS_WAITING_FACT_COMPLETION;
        }
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? OPERATION_STATUS_WAITING_CAPTURE_OR_RELEASE
                    : OPERATION_STATUS_NO_ACTION_REQUIRED;
            case FREEZE -> OPERATION_STATUS_WAITING_UNFREEZE_OR_CONSUME;
            default -> OPERATION_STATUS_NO_ACTION_REQUIRED;
        };
    }

    private @NonNull String resolveFailureReason(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return NOT_APPLICABLE;
        }
        if (instruction.getEventType() != FundsTransactionEventType.AUTHORIZE
                || StringUtils.hasText(ledgerTransactionSn)) {
            return NOT_APPLICABLE;
        }
        Object declineReason = instruction.getContextVariables().get(FundsInstructionContextKeys.DECLINE_REASON);
        String reason = declineReason == null ? null : declineReason.toString();
        return StringUtils.hasText(reason)
                ? reason
                : UNAVAILABLE_AUTHORIZATION_DECLINED;
    }

    private @NonNull String resolveStatusMeaning(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return STATUS_MEANING_FACT_PROCESSING;
        }
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED
                    : STATUS_MEANING_AUTHORIZATION_DECLINED_NO_FUNDS_POSTED;
            case FREEZE -> STATUS_MEANING_BALANCE_FROZEN_NOT_CONSUMED;
            case REVERSAL, UNFREEZE -> STATUS_MEANING_FUNDS_RELEASED;
            default -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_FUNDS_POSTED
                    : STATUS_MEANING_COMPLETED_WITHOUT_LEDGER;
        };
    }

    private @NonNull String resolveAmountSource(@Nullable String ledgerTransactionSn) {
        String ledgerRef = StringUtils.hasText(ledgerTransactionSn) ? ledgerTransactionSn : NOT_APPLICABLE;
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("instructionAmount=" + instruction.getAmount().getAmount() + " "
                + instruction.getAmount().getCurrency().name());
        joiner.add("routeLegCount=" + routeSnapshot.getLegs().size());
        joiner.add("routeSnapshot=" + routeSnapshot.getSnapshotId());
        joiner.add("ledgerTransaction=" + ledgerRef);
        return joiner.toString();
    }

    private @NonNull String resolveUnavailableReason(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return NOT_APPLICABLE;
        }
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION
                    : UNAVAILABLE_AUTHORIZATION_DECLINED;
            case FREEZE -> UNAVAILABLE_BALANCE_FREEZE_NOT_CONSUMPTION;
            default -> NOT_APPLICABLE;
        };
    }

    private @NonNull String resolveNextAction(@Nullable String ledgerTransactionSn) {
        if (!lifecycleResult.isCompleted()) {
            return NEXT_ACTION_WAIT_FOR_FACT_COMPLETION;
        }
        FundsTransactionEventType eventType = instruction.getEventType();
        if (eventType == FundsTransactionEventType.AUTHORIZE && StringUtils.hasText(ledgerTransactionSn)) {
            return NEXT_ACTION_WAIT_FOR_CAPTURE_OR_RELEASE;
        }
        if (eventType == FundsTransactionEventType.FREEZE) {
            return NEXT_ACTION_WAIT_FOR_UNFREEZE_OR_CONSUME;
        }
        return NOT_APPLICABLE;
    }

    private static @NonNull List<String> evidenceRefs(@Nullable String fundsTransactionSn,
                                                      @NonNull String routeSnapshotId,
                                                      @Nullable String ledgerTransactionSn) {
        List<String> refs = new ArrayList<>();
        if (StringUtils.hasText(fundsTransactionSn)) {
            refs.add("fundsTransaction:" + fundsTransactionSn);
        }
        refs.add("routeSnapshot:" + routeSnapshotId);
        if (StringUtils.hasText(ledgerTransactionSn)) {
            refs.add("ledgerTransaction:" + ledgerTransactionSn);
        }
        return List.copyOf(refs);
    }
}

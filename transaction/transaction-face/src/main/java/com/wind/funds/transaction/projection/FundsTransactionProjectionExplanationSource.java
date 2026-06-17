package com.wind.funds.transaction.projection;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 交易投影解释的稳定事实来源。
 *
 * <p>职责：把主写链路内存态事实或查询链路持久化事实归一成同一套投影解释口径。</p>
 *
 * <p>边界：本对象只做确定性解释转换，不查询数据库、不发布投影、不修改资金、账本或余额事实。</p>
 */
@Builder
public record FundsTransactionProjectionExplanationSource(@NonNull String businessScene,
                                                          @NonNull String businessSn,
                                                          @NonNull String fundsTransactionSn,
                                                          @NonNull RouteSnapshotSpec routeSnapshot,
                                                          @Nullable String ledgerTransactionSn,
                                                          boolean completed,
                                                          boolean failed,
                                                          @NonNull FundsTransactionEventType eventType,
                                                          @NonNull Money amount,
                                                          @NonNull Map<String, Object> contextVariables,
                                                          @Nullable String failureReasonOverride) {

    private static final String NOT_APPLICABLE = "N/A";

    private static final String FACT_STATUS_POSTED = "POSTED";

    private static final String FACT_STATUS_HELD = "HELD";

    private static final String FACT_STATUS_RELEASED = "RELEASED";

    private static final String FACT_STATUS_REJECTED = "REJECTED";

    private static final String FACT_STATUS_FAILED = "FAILED";

    private static final String FACT_STATUS_COMPLETED_NO_LEDGER = "COMPLETED_NO_LEDGER";

    private static final String FACT_STATUS_PROCESSING = "PROCESSING";

    private static final String DISPLAY_STATUS_SUCCEEDED = "SUCCEEDED";

    private static final String DISPLAY_STATUS_AUTHORIZED_HOLD = "AUTHORIZED_HOLD";

    private static final String DISPLAY_STATUS_FROZEN = "FROZEN";

    private static final String DISPLAY_STATUS_RELEASED = "RELEASED";

    private static final String DISPLAY_STATUS_DECLINED = "DECLINED";

    private static final String DISPLAY_STATUS_FAILED = "FAILED";

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

    private static final String STATUS_MEANING_FACT_FAILED = "FACT_FAILED";

    private static final String STATUS_MEANING_COMPLETED_WITHOUT_LEDGER = "COMPLETED_WITHOUT_LEDGER";

    private static final String NEXT_ACTION_WAIT_FOR_CAPTURE_OR_RELEASE = "WAIT_FOR_CAPTURE_OR_RELEASE";

    private static final String NEXT_ACTION_WAIT_FOR_UNFREEZE_OR_CONSUME = "WAIT_FOR_UNFREEZE_OR_CONSUME";

    private static final String NEXT_ACTION_WAIT_FOR_FACT_COMPLETION = "WAIT_FOR_FACT_COMPLETION";

    private static final String NEXT_ACTION_REVIEW_FAILURE = "REVIEW_FAILURE";

    private static final String UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION =
            "AUTHORIZATION_HOLD_IS_NOT_FINAL_CONSUMPTION";

    private static final String UNAVAILABLE_AUTHORIZATION_DECLINED = "AUTHORIZATION_DECLINED";

    private static final String UNAVAILABLE_BALANCE_FREEZE_NOT_CONSUMPTION = "BALANCE_FREEZE_IS_NOT_CONSUMPTION";

    private static final String UNAVAILABLE_TRANSACTION_FAILED = "TRANSACTION_FAILED";

    public FundsTransactionProjectionExplanationSource {
        AssertUtils.hasText(businessScene, "交易投影解释业务场景不能为空");
        AssertUtils.hasText(businessSn, "交易投影解释业务流水不能为空");
        AssertUtils.hasText(fundsTransactionSn, "交易投影解释资金交易流水不能为空");
        AssertUtils.notNull(routeSnapshot, "交易投影解释 RouteSnapshot 不能为空");
        AssertUtils.notNull(eventType, "交易投影解释事件类型不能为空");
        AssertUtils.notNull(amount, "交易投影解释金额不能为空");
        contextVariables = contextVariables == null ? Map.of() : Map.copyOf(contextVariables);
    }

    /**
     * 生成交易投影解释摘要。
     *
     * @return 交易投影解释摘要
     */
    public @NonNull FundsTransactionProjectionExplanation explanation() {
        return FundsTransactionProjectionExplanation.builder()
                .businessScene(businessScene)
                .businessSn(businessSn)
                .fundsTransactionSn(fundsTransactionSn)
                .routeSnapshotId(routeSnapshot.getSnapshotId())
                .routeCode(routeSnapshot.getRouteCode())
                .ledgerTransactionSn(ledgerTransactionSn)
                .factStatus(resolveFactStatus())
                .displayStatus(resolveDisplayStatus())
                .operationStatus(resolveOperationStatus())
                .statusMeaning(resolveStatusMeaning())
                .amountSource(resolveAmountSource())
                .failureReason(resolveFailureReason())
                .unavailableReason(resolveUnavailableReason())
                .nextAction(resolveNextAction())
                .evidenceRefs(evidenceRefs(fundsTransactionSn, routeSnapshot.getSnapshotId(), ledgerTransactionSn))
                .externalRuleVerificationStatus(NOT_APPLICABLE)
                .build();
    }

    private @NonNull String resolveFactStatus() {
        if (failed) {
            return FACT_STATUS_FAILED;
        }
        if (!completed) {
            return FACT_STATUS_PROCESSING;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn) ? FACT_STATUS_HELD : FACT_STATUS_REJECTED;
            case FREEZE -> FACT_STATUS_HELD;
            case REVERSAL, EXPIRE, UNFREEZE -> FACT_STATUS_RELEASED;
            default -> StringUtils.hasText(ledgerTransactionSn)
                    ? FACT_STATUS_POSTED
                    : FACT_STATUS_COMPLETED_NO_LEDGER;
        };
    }

    private @NonNull String resolveDisplayStatus() {
        if (failed) {
            return DISPLAY_STATUS_FAILED;
        }
        if (!completed) {
            return DISPLAY_STATUS_PROCESSING;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? DISPLAY_STATUS_AUTHORIZED_HOLD
                    : DISPLAY_STATUS_DECLINED;
            case FREEZE -> DISPLAY_STATUS_FROZEN;
            case REVERSAL, EXPIRE, UNFREEZE -> DISPLAY_STATUS_RELEASED;
            default -> DISPLAY_STATUS_SUCCEEDED;
        };
    }

    private @NonNull String resolveOperationStatus() {
        if (failed) {
            return OPERATION_STATUS_NO_ACTION_REQUIRED;
        }
        if (!completed) {
            return OPERATION_STATUS_WAITING_FACT_COMPLETION;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? OPERATION_STATUS_WAITING_CAPTURE_OR_RELEASE
                    : OPERATION_STATUS_NO_ACTION_REQUIRED;
            case FREEZE -> OPERATION_STATUS_WAITING_UNFREEZE_OR_CONSUME;
            default -> OPERATION_STATUS_NO_ACTION_REQUIRED;
        };
    }

    private @NonNull String resolveStatusMeaning() {
        if (failed) {
            return STATUS_MEANING_FACT_FAILED;
        }
        if (!completed) {
            return STATUS_MEANING_FACT_PROCESSING;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED
                    : STATUS_MEANING_AUTHORIZATION_DECLINED_NO_FUNDS_POSTED;
            case FREEZE -> STATUS_MEANING_BALANCE_FROZEN_NOT_CONSUMED;
            case REVERSAL, EXPIRE, UNFREEZE -> STATUS_MEANING_FUNDS_RELEASED;
            default -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_FUNDS_POSTED
                    : STATUS_MEANING_COMPLETED_WITHOUT_LEDGER;
        };
    }

    private @NonNull String resolveAmountSource() {
        String ledgerRef = StringUtils.hasText(ledgerTransactionSn) ? ledgerTransactionSn : NOT_APPLICABLE;
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("instructionAmount=" + amount.getAmount() + " " + amount.getCurrency().name());
        joiner.add("routeLegCount=" + routeSnapshot.getLegs().size());
        joiner.add("routeSnapshot=" + routeSnapshot.getSnapshotId());
        joiner.add("ledgerTransaction=" + ledgerRef);
        return joiner.toString();
    }

    private @NonNull String resolveFailureReason() {
        if (StringUtils.hasText(failureReasonOverride)) {
            return failureReasonOverride;
        }
        if (failed) {
            return UNAVAILABLE_TRANSACTION_FAILED;
        }
        if (!completed) {
            return NOT_APPLICABLE;
        }
        if (eventType != FundsTransactionEventType.AUTHORIZE || StringUtils.hasText(ledgerTransactionSn)) {
            return NOT_APPLICABLE;
        }
        Object declineReason = contextVariables.get(FundsInstructionContextKeys.DECLINE_REASON);
        String reason = declineReason == null ? null : declineReason.toString();
        return StringUtils.hasText(reason) ? reason : UNAVAILABLE_AUTHORIZATION_DECLINED;
    }

    private @NonNull String resolveUnavailableReason() {
        if (failed) {
            return UNAVAILABLE_TRANSACTION_FAILED;
        }
        if (!completed) {
            return NOT_APPLICABLE;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION
                    : UNAVAILABLE_AUTHORIZATION_DECLINED;
            case FREEZE -> UNAVAILABLE_BALANCE_FREEZE_NOT_CONSUMPTION;
            default -> NOT_APPLICABLE;
        };
    }

    private @NonNull String resolveNextAction() {
        if (failed) {
            return NEXT_ACTION_REVIEW_FAILURE;
        }
        if (!completed) {
            return NEXT_ACTION_WAIT_FOR_FACT_COMPLETION;
        }
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

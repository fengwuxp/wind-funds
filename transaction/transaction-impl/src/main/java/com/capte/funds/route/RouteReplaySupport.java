package com.capte.funds.route;

import com.wind.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

final class RouteReplaySupport {

    private RouteReplaySupport() {
    }

    static boolean isReplayInstruction(@NonNull FundsInstructionSpec instruction) {
        if (!isReplayEvent(instruction.getEventType())) {
            return false;
        }
        FundsInstructionReferenceSpec reference = instruction.getReference();
        return reference != null
                && StringUtils.hasText(reference.getReferenceSn())
                && isRouteSnapshotReference(reference.getReferenceType());
    }

    private static boolean isRouteSnapshotReference(FundsInstructionReferenceType referenceType) {
        return switch (referenceType) {
            case ORIGINAL_TRANSACTION, AUTHORIZATION, REFUND, FEE, FREEZE_ORDER -> true;
            case EXTERNAL_TRANSACTION -> false;
        };
    }

    private static boolean isReplayEvent(FundsTransactionEventType eventType) {
        return switch (eventType) {
            case REVERSAL, EXPIRE, SETTLE, AUTH_REFUND, CHARGEBACK, REFUND, FEE_REFUND, UNFREEZE -> true;
            default -> false;
        };
    }

}

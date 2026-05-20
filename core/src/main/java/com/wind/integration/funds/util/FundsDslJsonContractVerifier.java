package com.wind.integration.funds.util;

import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Verifier for DSL JSON contract samples.
 */
public final class FundsDslJsonContractVerifier {

    private FundsDslJsonContractVerifier() {
    }

    public static void verifyTransactionLayerCase(@NonNull Map<String, ?> document) {
        requireText(document, "caseId");
        verifyInstruction(asNullableMap(document.get("instruction"), "instruction"));
        verifyRoute(asNullableMap(document.get("expectedRoute"), "expectedRoute"));
        verifyPosting(asNullableMap(document.get("expectedPosting"), "expectedPosting"));
        verifyReplayRequest(asNullableMap(document.get("replayRequest"), "replayRequest"));
    }

    private static void verifyInstruction(@Nullable Map<String, ?> instruction) {
        if (instruction == null) {
            return;
        }
        verifyEnum(FundsInstructionType.class, instruction, "instructionType", "instruction.instructionType");
        verifyEnum(FundsTransactionEventType.class, instruction, "eventType", "instruction.eventType");
        verifyEnum(DefaultFundsTransactionType.class, instruction, "transactionType", "instruction.transactionType");
        verifyMoney(instruction, "amount", "instruction.amount");
        verifyMoney(instruction, "originalAmount", "instruction.originalAmount");
        verifyReference(asNullableMap(instruction.get("reference"), "instruction.reference"));
    }

    private static void verifyReference(@Nullable Map<String, ?> reference) {
        if (reference == null) {
            return;
        }
        verifyEnum(FundsInstructionReferenceType.class, reference, "referenceType", "instruction.reference.referenceType");
        requireText(reference, "referenceSn");
    }

    private static void verifyRoute(@Nullable Map<String, ?> route) {
        if (route == null) {
            return;
        }
        verifyEnum(RouteReplayType.class, route, "replayType", "expectedRoute.replayType", false);
        for (Map<String, ?> participant : childObjects(route, "participants", "expectedRoute.participants")) {
            verifyEnum(RouteParticipantRole.class, participant, "participantRole", "expectedRoute.participants.participantRole");
            verifySubjectRef(asNullableMap(participant.get("subjectRef"), "expectedRoute.participants.subjectRef"),
                    "expectedRoute.participants.subjectRef");
        }
        for (Map<String, ?> leg : childObjects(route, "legs", "expectedRoute.legs")) {
            verifyEnum(RouteLegType.class, leg, "legType", "expectedRoute.legs.legType");
            verifyNode(asNullableMap(leg.get("sourceNode"), "expectedRoute.legs.sourceNode"), "expectedRoute.legs.sourceNode");
            verifyNode(asNullableMap(leg.get("targetNode"), "expectedRoute.legs.targetNode"), "expectedRoute.legs.targetNode");
            verifyMoney(leg, "amount", "expectedRoute.legs.amount");
            verifyEnum(LedgerBalanceEffectType.class, leg, "balanceEffectType", "expectedRoute.legs.balanceEffectType");
            verifyEnum(LedgerPhaseCode.class, leg, "phaseCode", "expectedRoute.legs.phaseCode");
            verifyEnum(RouteReplayPolicy.class, leg, "replayPolicy", "expectedRoute.legs.replayPolicy");
        }
    }

    private static void verifyNode(@Nullable Map<String, ?> node, String path) {
        if (node == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        verifyEnum(FundsSubjectType.class, node, "subjectType", path + ".subjectType");
        verifyEnum(LedgerSubjectCode.class, node, "ledgerSubjectCode", path + ".ledgerSubjectCode");
    }

    private static void verifySubjectRef(@Nullable Map<String, ?> subjectRef, String path) {
        if (subjectRef == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        verifyEnum(FundsSubjectType.class, subjectRef, "subjectType", path + ".subjectType");
    }

    private static void verifyPosting(@Nullable Map<String, ?> posting) {
        if (posting == null) {
            return;
        }
        for (Map<String, ?> plan : childObjects(posting, "postingPlans", "expectedPosting.postingPlans")) {
            verifyEnum(LedgerPostingIntentType.class, plan, "intent", "expectedPosting.postingPlans.intent");
            verifyEnum(LedgerPostingScope.class, plan, "postingScope", "expectedPosting.postingPlans.postingScope");
            verifyEnum(LedgerBalanceEffectType.class, plan, "balanceEffectType", "expectedPosting.postingPlans.balanceEffectType");
            verifyEnum(LedgerPhaseCode.class, plan, "phaseCode", "expectedPosting.postingPlans.phaseCode");
            for (Map<String, ?> entry : childObjects(plan, "entries", "expectedPosting.postingPlans.entries")) {
                verifyEnum(FundsSubjectType.class, entry, "subjectType", "expectedPosting.postingPlans.entries.subjectType");
                verifyEnum(LedgerSubjectCode.class, entry, "ledgerSubjectCode",
                        "expectedPosting.postingPlans.entries.ledgerSubjectCode");
                verifyEnum(EntrySide.class, entry, "entrySide", "expectedPosting.postingPlans.entries.entrySide");
                verifyMoney(entry, "amount", "expectedPosting.postingPlans.entries.amount");
            }
        }
    }

    private static void verifyReplayRequest(@Nullable Map<String, ?> replayRequest) {
        if (replayRequest == null) {
            return;
        }
        verifyEnum(FundsSubjectType.class, replayRequest, "subjectType", "replayRequest.subjectType", false);
    }

    private static void verifyMoney(Map<String, ?> owner, String fieldName, String path) {
        Map<String, ?> money = asNullableMap(owner.get(fieldName), path);
        if (money == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        try {
            FundsDslMoneyParser.parse(money);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(path + ": " + ex.getMessage(), ex);
        }
    }

    private static <E extends Enum<E>> void verifyEnum(Class<E> enumType,
                                                       Map<String, ?> owner,
                                                       String fieldName,
                                                       String path) {
        verifyEnum(enumType, owner, fieldName, path, true);
    }

    private static <E extends Enum<E>> void verifyEnum(Class<E> enumType,
                                                       Map<String, ?> owner,
                                                       String fieldName,
                                                       String path,
                                                       boolean required) {
        Object rawValue = owner.get(fieldName);
        if (rawValue == null && !required) {
            return;
        }
        if (!(rawValue instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException(path + " is required");
        }
        try {
            Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(path + " must be " + enumType.getSimpleName(), ex);
        }
    }

    private static String requireText(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return text;
    }

    private static List<Map<String, ?>> childObjects(Map<String, ?> owner, String fieldName, String path) {
        Object rawValue = owner.get(fieldName);
        if (rawValue == null) {
            return List.of();
        }
        if (!(rawValue instanceof List<?> values)) {
            throw new IllegalArgumentException(path + " must be array");
        }
        List<Map<String, ?>> objects = new ArrayList<>(values.size());
        for (Object value : values) {
            objects.add(asMap(value, path));
        }
        return List.copyOf(objects);
    }

    private static Map<String, ?> asMap(Object value, String path) {
        Map<String, ?> result = asNullableMap(value, path);
        if (result == null) {
            throw new IllegalArgumentException(path + " must be object");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> asNullableMap(Object value, String path) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }
        throw new IllegalArgumentException(path + " must be object");
    }
}

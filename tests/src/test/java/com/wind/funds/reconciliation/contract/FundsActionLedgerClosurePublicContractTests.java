package com.wind.funds.reconciliation.contract;

import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.model.dto.FundsActionFactDTO;
import com.wind.funds.transaction.model.dto.FundsActionFactRef;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ActionFact recorded evidence 到 clearing source admission 的 Public Contract RED。
 */
class FundsActionLedgerClosurePublicContractTests {

    private static final String RECORDED_EVIDENCE_SERVICE =
            "com.wind.funds.transaction.services.FundsActionRecordedEvidenceQueryService";

    private static final String RECORDED_EVIDENCE_DTO =
            "com.wind.funds.transaction.model.dto.FundsActionRecordedEvidenceDTO";

    @Test
    void testRecordedEvidenceContractShouldExposeCompleteMatchedSiblingSet() {
        List<String> mismatches = new ArrayList<>();
        Class<?> serviceType = loadClass(RECORDED_EVIDENCE_SERVICE, mismatches);
        Class<?> evidenceType = loadClass(RECORDED_EVIDENCE_DTO, mismatches);

        verifyRecordedEvidenceService(serviceType, evidenceType, mismatches);
        verifyRecordedEvidenceDTO(evidenceType, mismatches);

        assertThat(mismatches)
                .withFailMessage("recorded evidence contract missing: %s", String.join("; ", mismatches))
                .isEmpty();
    }

    @Test
    void testClearingSourceRequestShouldAcceptOnlyStableActionRef() {
        List<String> mismatches = new ArrayList<>();
        Class<?> requestType = IdentifyClearingSplittableDetailRequest.class;

        require(hasExactlyInstanceFields(requestType, List.of(
                        "tenantId", "sourceActionFactRef", "businessLine", "splitPeriod",
                        "splitRuleCode", "splitRuleVersion")),
                mismatches, "request fields must be tenantId + sourceActionFactRef + policy fields");
        verifyFieldType(requestType, "sourceActionFactRef", StableIdentity.class, mismatches);
        verifyMethod(requestType, "getSourceActionFactRef", StableIdentity.class, List.of(), mismatches);
        verifyMethod(requestType, "setSourceActionFactRef", requestType, List.of(StableIdentity.class), mismatches);

        List<String> methodNames = Arrays.stream(requestType.getDeclaredMethods())
                .map(Method::getName)
                .toList();
        for (String legacyProperty : List.of(
                "FundsTransactionSn", "FundsTransactionDetailSn", "LedgerEntrySn")) {
            require(!methodNames.contains("get" + legacyProperty), mismatches,
                    "legacy getter must be removed: get" + legacyProperty);
            require(!methodNames.contains("set" + legacyProperty), mismatches,
                    "legacy setter must be removed: set" + legacyProperty);
        }

        assertThat(mismatches)
                .withFailMessage("clearing source request contract mismatch: %s", String.join("; ", mismatches))
                .isEmpty();
    }

    private static void verifyRecordedEvidenceService(Class<?> serviceType,
                                                      Class<?> evidenceType,
                                                      List<String> mismatches) {
        if (serviceType == null) {
            return;
        }
        require(serviceType.isInterface(), mismatches, "recorded evidence service must be an interface");
        List<Method> methods = Arrays.stream(serviceType.getDeclaredMethods())
                .filter(method -> method.getName().equals("findRecordedEvidence"))
                .toList();
        require(methods.size() == 1, mismatches, "service must expose exactly one findRecordedEvidence method");
        if (methods.size() != 1) {
            return;
        }
        Method method = methods.getFirst();
        require(Arrays.equals(method.getParameterTypes(), new Class<?>[]{FundsActionFactRef.class}),
                mismatches, "findRecordedEvidence must accept FundsActionFactRef");
        require(method.getReturnType() == Optional.class, mismatches,
                "findRecordedEvidence must return Optional");
        if (evidenceType != null) {
            require(method.getGenericReturnType().getTypeName().contains(evidenceType.getName()),
                    mismatches, "findRecordedEvidence Optional must contain FundsActionRecordedEvidenceDTO");
        }
    }

    private static void verifyRecordedEvidenceDTO(Class<?> evidenceType, List<String> mismatches) {
        if (evidenceType == null) {
            return;
        }
        require(hasExactlyInstanceFields(evidenceType, List.of(
                        "actionFact", "matchedSiblings", "recordedLedgerTransactionSn", "recordedReferenceDigest")),
                mismatches, "recorded evidence DTO fields must match the frozen contract");
        verifyFieldType(evidenceType, "actionFact", FundsActionFactDTO.class, mismatches);
        verifyFieldType(evidenceType, "matchedSiblings", List.class, mismatches);
        verifyFieldType(evidenceType, "recordedLedgerTransactionSn", String.class, mismatches);
        verifyFieldType(evidenceType, "recordedReferenceDigest", FundsActionFactDTO.SemanticDigest.class, mismatches);

        Class<?> siblingType = Arrays.stream(evidenceType.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("RecordedSiblingRef"))
                .findFirst()
                .orElse(null);
        require(siblingType != null, mismatches, "recorded evidence DTO must declare RecordedSiblingRef");
        Field siblingField = fieldNamed(evidenceType, "matchedSiblings", mismatches);
        if (siblingField != null && siblingType != null) {
            require(siblingField.getGenericType().getTypeName().contains(siblingType.getName()),
                    mismatches, "matchedSiblings must contain RecordedSiblingRef");
            verifyRecordedSibling(siblingType, mismatches);
        }

        require(Arrays.stream(evidenceType.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .noneMatch(field -> field.getType().getName().startsWith("com.wind.funds.ledger")
                                || field.getType().getName().contains("Balance")),
                mismatches, "recorded evidence DTO must expose refs, not Ledger or Balance DTOs");
        require(instanceFieldNames(FundsActionFactDTO.class).stream()
                        .map(name -> name.toLowerCase(Locale.ROOT))
                        .noneMatch(name -> name.contains("ledger") || name.contains("balance")),
                mismatches, "FundsActionFactDTO must not claim Ledger or Balance closure");
    }

    private static void verifyRecordedSibling(Class<?> siblingType, List<String> mismatches) {
        require(hasExactlyInstanceFields(siblingType, List.of(
                        "detailSn", "participantRole", "subjectId", "subjectType", "money",
                        "recordedLedgerTransactionSn")),
                mismatches, "RecordedSiblingRef fields must match the frozen contract");
        verifyFieldType(siblingType, "detailSn", String.class, mismatches);
        verifyFieldType(siblingType, "participantRole", RouteParticipantRole.class, mismatches);
        verifyFieldType(siblingType, "subjectId", String.class, mismatches);
        verifyFieldType(siblingType, "subjectType", String.class, mismatches);
        verifyFieldType(siblingType, "money", Money.class, mismatches);
        verifyFieldType(siblingType, "recordedLedgerTransactionSn", String.class, mismatches);
    }

    private static Class<?> loadClass(String className, List<String> mismatches) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            mismatches.add("missing public contract type: " + className);
            return null;
        }
    }

    private static void verifyFieldType(Class<?> owner,
                                        String fieldName,
                                        Class<?> expectedType,
                                        List<String> mismatches) {
        Field field = fieldNamed(owner, fieldName, mismatches);
        if (field != null) {
            require(field.getType() == expectedType, mismatches,
                    fieldName + " must be " + expectedType.getSimpleName());
        }
    }

    private static Field fieldNamed(Class<?> owner, String fieldName, List<String> mismatches) {
        return Arrays.stream(owner.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElseGet(() -> {
                    mismatches.add("missing field: " + owner.getSimpleName() + "." + fieldName);
                    return null;
                });
    }

    private static void verifyMethod(Class<?> owner,
                                     String methodName,
                                     Class<?> returnType,
                                     List<Class<?>> parameterTypes,
                                     List<String> mismatches) {
        Method method = Arrays.stream(owner.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameterTypes.toArray(Class[]::new)))
                .findFirst()
                .orElse(null);
        require(method != null, mismatches, "missing method: " + owner.getSimpleName() + "." + methodName);
        if (method != null) {
            require(method.getReturnType() == returnType, mismatches,
                    methodName + " must return " + returnType.getSimpleName());
        }
    }

    private static List<String> instanceFieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .toList();
    }

    private static boolean hasExactlyInstanceFields(Class<?> type, List<String> expectedFields) {
        List<String> actualFields = instanceFieldNames(type);
        return actualFields.size() == expectedFields.size() && actualFields.containsAll(expectedFields);
    }

    private static void require(boolean condition, List<String> mismatches, String message) {
        if (!condition) {
            mismatches.add(message);
        }
    }
}

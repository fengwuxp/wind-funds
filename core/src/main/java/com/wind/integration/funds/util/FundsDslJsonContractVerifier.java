package com.wind.integration.funds.util;

import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitComponentSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitRefundPolicySpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitSnapshotSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.integration.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.integration.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.integration.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.integration.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.integration.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.integration.funds.transaction.enums.FundsBenefitType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifier for DSL JSON contract samples.
 */
public final class FundsDslJsonContractVerifier {

    private static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    private static final Set<String> RESERVED_BENEFIT_CONTEXT_KEYS = Set.of(
            ImmutableFundsBenefitSnapshotSpec.Fields.benefitSnapshotId,
            ImmutableFundsBenefitSnapshotSpec.Fields.benefitSchemaVersion,
            ImmutableFundsBenefitSnapshotSpec.Fields.benefitGroupSn,
            ImmutableFundsBenefitSnapshotSpec.Fields.orderAmount,
            ImmutableFundsBenefitSnapshotSpec.Fields.userPayAmount,
            ImmutableFundsBenefitSnapshotSpec.Fields.merchantReceivableAmount,
            ImmutableFundsBenefitComponentSpec.Fields.componentSn,
            ImmutableFundsBenefitComponentSpec.Fields.benefitType,
            ImmutableFundsBenefitComponentSpec.Fields.componentType,
            ImmutableFundsBenefitComponentSpec.Fields.closureRole,
            ImmutableFundsBenefitComponentSpec.Fields.amount,
            ImmutableFundsBenefitComponentSpec.Fields.ledgerEffect,
            ImmutableFundsBenefitComponentSpec.Fields.fundingNature,
            ImmutableFundsBenefitComponentSpec.Fields.benefitReference,
            ImmutableFundsBenefitComponentSpec.Fields.refundPolicy,
            ImmutableFundsBenefitRefundPolicySpec.Fields.partialRefundStrategy,
            ImmutableFundsBenefitRefundPolicySpec.Fields.dispositions,
            "refundDisposition",
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundableAmount,
            ImmutableFundsBenefitRefundPolicySpec.Fields.nonRefundableAmount,
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundRuleVersion,
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundDecisionId,
            ImmutableFundsBenefitReferenceSpec.Fields.ruleVersion,
            "currentMarketingRule",
            "couponEligibility",
            "couponAvailable",
            "recalculatedDiscount",
            "bestCoupon",
            "activityRules",
            "userCouponBag");

    private FundsDslJsonContractVerifier() {
    }

    public static void verifyTransactionLayerCase(@NonNull Map<String, ?> document) {
        requireText(document, "caseId");
        verifyContractFixtureMetadata(document);
        verifyInstruction(asNullableMap(document.get("instruction"), "instruction"));
        verifyRoute(asNullableMap(document.get("expectedRoute"), "expectedRoute"));
        verifyPosting(asNullableMap(document.get("expectedPosting"), "expectedPosting"));
        verifyReplayRequest(asNullableMap(document.get("replayRequest"), "replayRequest"));
    }

    private static void verifyContractFixtureMetadata(Map<String, ?> document) {
        Object fixtureLevel = document.get("fixtureLevel");
        if (fixtureLevel == null) {
            return;
        }
        if (!(fixtureLevel instanceof String value) || !StringUtils.hasText(value)) {
            throw new IllegalArgumentException("fixtureLevel is required");
        }
        if (!"CONTRACT_ONLY".equals(value)) {
            return;
        }
        requireText(document, "scenarioCode");
        requiredChildTexts(document, "acceptanceIds", "acceptanceIds");
        requiredChildTexts(document, "tddIds", "tddIds");
        requiredChildTexts(document, "systemDesignRefs", "systemDesignRefs");
        Map<String, ?> validation = asMap(document.get("validation"), "validation");
        requiredChildTexts(validation, "mustPass", "validation.mustPass");
        requiredChildTexts(validation, "mustFail", "validation.mustFail");
    }

    private static void verifyInstruction(@Nullable Map<String, ?> instruction) {
        if (instruction == null) {
            return;
        }
        verifyEnum(FundsInstructionType.class, instruction, ImmutableFundsInstructionSpec.Fields.instructionType,
                "instruction.instructionType");
        verifyEnum(FundsTransactionEventType.class, instruction, ImmutableFundsInstructionSpec.Fields.eventType,
                "instruction.eventType");
        verifyEnum(DefaultFundsTransactionType.class, instruction,
                ImmutableFundsInstructionSpec.Fields.transactionType, "instruction.transactionType");
        verifyMoney(instruction, ImmutableFundsInstructionSpec.Fields.amount, "instruction.amount");
        verifyMoney(instruction, ImmutableFundsInstructionSpec.Fields.originalAmount, "instruction.originalAmount");
        verifyReference(asNullableMap(instruction.get(ImmutableFundsInstructionSpec.Fields.reference),
                "instruction.reference"));
        verifyBenefitSnapshot(asNullableMap(instruction.get(ImmutableFundsInstructionSpec.Fields.benefitSnapshot),
                "instruction.benefitSnapshot"));
    }

    private static void verifyReference(@Nullable Map<String, ?> reference) {
        if (reference == null) {
            return;
        }
        verifyEnum(FundsInstructionReferenceType.class, reference,
                ImmutableFundsInstructionReferenceSpec.Fields.referenceType, "instruction.reference.referenceType");
        requireText(reference, ImmutableFundsInstructionReferenceSpec.Fields.referenceSn);
    }

    private static void verifyBenefitSnapshot(@Nullable Map<String, ?> snapshot) {
        if (snapshot == null) {
            return;
        }
        requireText(snapshot, ImmutableFundsBenefitSnapshotSpec.Fields.benefitSnapshotId);
        requireText(snapshot, ImmutableFundsBenefitSnapshotSpec.Fields.benefitGroupSn);
        Money orderAmount = verifyMoney(snapshot, ImmutableFundsBenefitSnapshotSpec.Fields.orderAmount,
                "instruction.benefitSnapshot.orderAmount");
        Money userPayAmount = verifyMoney(snapshot, ImmutableFundsBenefitSnapshotSpec.Fields.userPayAmount,
                "instruction.benefitSnapshot.userPayAmount", false);
        verifyMoney(snapshot, ImmutableFundsBenefitSnapshotSpec.Fields.merchantReceivableAmount,
                "instruction.benefitSnapshot.merchantReceivableAmount", false, false);
        List<Map<String, ?>> components = requiredChildObjects(snapshot,
                ImmutableFundsBenefitSnapshotSpec.Fields.components,
                "instruction.benefitSnapshot.components");
        Set<String> componentSns = new HashSet<>();
        long orderDiscountAmount = 0L;
        for (Map<String, ?> component : components) {
            String componentSn = requireText(component, ImmutableFundsBenefitComponentSpec.Fields.componentSn);
            if (!componentSns.add(componentSn)) {
                throw new IllegalArgumentException("instruction.benefitSnapshot.components.componentSn must be unique");
            }
            verifyEnum(FundsBenefitType.class, component, ImmutableFundsBenefitComponentSpec.Fields.benefitType,
                    "instruction.benefitSnapshot.components.benefitType");
            verifyEnum(FundsBenefitComponentType.class, component,
                    ImmutableFundsBenefitComponentSpec.Fields.componentType,
                    "instruction.benefitSnapshot.components.componentType");
            FundsBenefitAmountClosureRole closureRole = verifyEnum(FundsBenefitAmountClosureRole.class,
                    component,
                    ImmutableFundsBenefitComponentSpec.Fields.closureRole,
                    "instruction.benefitSnapshot.components.closureRole");
            Money componentMoney = verifyMoney(component, ImmutableFundsBenefitComponentSpec.Fields.amount,
                    "instruction.benefitSnapshot.components.amount");
            if (!orderAmount.getCurrency().equals(componentMoney.getCurrency())) {
                throw new IllegalArgumentException(
                        "instruction.benefitSnapshot.components.amount currency must equal orderAmount");
            }
            if (closureRole == FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE) {
                orderDiscountAmount = addExact(orderDiscountAmount, componentMoney.getAmount(),
                        "instruction.benefitSnapshot.components.amount sum overflow");
            }
            FundsBenefitLedgerEffect ledgerEffect = verifyEnum(FundsBenefitLedgerEffect.class,
                    component,
                    ImmutableFundsBenefitComponentSpec.Fields.ledgerEffect,
                    "instruction.benefitSnapshot.components.ledgerEffect");
            verifyEnum(FundsBenefitFundingNature.class, component,
                    ImmutableFundsBenefitComponentSpec.Fields.fundingNature,
                    "instruction.benefitSnapshot.components.fundingNature");
            verifyBenefitSubjectRef(asNullableMap(component.get(
                            ImmutableFundsBenefitComponentSpec.Fields.bearerSubjectRef),
                            "instruction.benefitSnapshot.components.bearerSubjectRef"),
                    "instruction.benefitSnapshot.components.bearerSubjectRef");
            verifyBenefitSubjectRef(asNullableMap(component.get(
                            ImmutableFundsBenefitComponentSpec.Fields.beneficiarySubjectRef),
                            "instruction.benefitSnapshot.components.beneficiarySubjectRef"),
                    "instruction.benefitSnapshot.components.beneficiarySubjectRef");
            verifyBenefitSubjectRef(asNullableMap(component.get(
                            ImmutableFundsBenefitComponentSpec.Fields.fundingSubjectRef),
                            "instruction.benefitSnapshot.components.fundingSubjectRef"),
                    "instruction.benefitSnapshot.components.fundingSubjectRef");
            if (ledgerEffect == FundsBenefitLedgerEffect.POSTING_REQUIRED
                    && component.get(ImmutableFundsBenefitComponentSpec.Fields.fundingSubjectRef) == null
                    && !isTextValue(component.get(ImmutableFundsBenefitComponentSpec.Fields.fundingAccountRole))) {
                throw new IllegalArgumentException(
                        "instruction.benefitSnapshot.components funding source is required for POSTING_REQUIRED");
            }
            if (ledgerEffect == FundsBenefitLedgerEffect.NO_LEDGER
                    && component.get(ImmutableFundsBenefitComponentSpec.Fields.bearerSubjectRef) == null) {
                throw new IllegalArgumentException(
                        "instruction.benefitSnapshot.components.bearerSubjectRef is required for NO_LEDGER");
            }
            verifyBenefitReference(asNullableMap(component.get(
                            ImmutableFundsBenefitComponentSpec.Fields.benefitReference),
                            "instruction.benefitSnapshot.components.benefitReference"),
                    "instruction.benefitSnapshot.components.benefitReference",
                    ledgerEffect);
            verifyBenefitRefundPolicy(asNullableMap(component.get(
                            ImmutableFundsBenefitComponentSpec.Fields.refundPolicy),
                            "instruction.benefitSnapshot.components.refundPolicy"),
                    "instruction.benefitSnapshot.components.refundPolicy");
            verifyBenefitContext(asNullableMap(component.get(
                            ImmutableFundsBenefitComponentSpec.Fields.contextVariables),
                            "instruction.benefitSnapshot.components.contextVariables"),
                    "instruction.benefitSnapshot.components.contextVariables");
        }
        if (!orderAmount.getCurrency().equals(userPayAmount.getCurrency())) {
            throw new IllegalArgumentException("instruction.benefitSnapshot.userPayAmount currency must equal orderAmount");
        }
        long closedAmount = addExact(userPayAmount.getAmount(), orderDiscountAmount,
                "instruction.benefitSnapshot amount sum overflow");
        if (closedAmount != orderAmount.getAmount()) {
            throw new IllegalArgumentException(
                    "instruction.benefitSnapshot amount must close: "
                            + "userPayAmount + ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
        }
        verifyBenefitRefundPolicy(asNullableMap(snapshot.get(ImmutableFundsBenefitSnapshotSpec.Fields.refundPolicy),
                        "instruction.benefitSnapshot.refundPolicy"),
                "instruction.benefitSnapshot.refundPolicy");
        verifyBenefitContext(asNullableMap(snapshot.get(ImmutableFundsBenefitSnapshotSpec.Fields.contextVariables),
                        "instruction.benefitSnapshot.contextVariables"),
                "instruction.benefitSnapshot.contextVariables");
    }

    private static void verifyBenefitSubjectRef(@Nullable Map<String, ?> subjectRef, String path) {
        if (subjectRef == null) {
            return;
        }
        requireText(subjectRef, ImmutableSubjectRef.Fields.subjectType, path + ".subjectType");
        requireText(subjectRef, ImmutableSubjectRef.Fields.subjectId, path + ".subjectId");
    }

    private static void verifyBenefitReference(@Nullable Map<String, ?> reference,
                                               String path,
                                               FundsBenefitLedgerEffect ledgerEffect) {
        if (reference == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        if (ledgerEffect == FundsBenefitLedgerEffect.HOLD_ONLY
                && !isTextValue(reference.get(ImmutableFundsBenefitReferenceSpec.Fields.holdId))) {
            throw new IllegalArgumentException(path + ".holdId is required for HOLD_ONLY");
        }
        if (ledgerEffect == FundsBenefitLedgerEffect.RELEASE_ONLY
                && !isTextValue(reference.get(ImmutableFundsBenefitReferenceSpec.Fields.holdId))
                && !isTextValue(reference.get(ImmutableFundsBenefitReferenceSpec.Fields.releaseId))) {
            throw new IllegalArgumentException(path + ".holdId or releaseId is required for RELEASE_ONLY");
        }
        verifyBenefitContext(asNullableMap(reference.get(ImmutableFundsBenefitReferenceSpec.Fields.contextVariables),
                        path + ".contextVariables"),
                path + ".contextVariables");
    }

    private static void verifyBenefitRefundPolicy(@Nullable Map<String, ?> policy, String path) {
        if (policy == null) {
            return;
        }
        verifyEnum(FundsBenefitPartialRefundStrategy.class, policy,
                ImmutableFundsBenefitRefundPolicySpec.Fields.partialRefundStrategy,
                path + ".partialRefundStrategy");
        List<String> dispositions = requiredChildTexts(policy,
                ImmutableFundsBenefitRefundPolicySpec.Fields.dispositions, path + ".dispositions");
        for (String disposition : dispositions) {
            enumValue(FundsBenefitRefundDisposition.class, disposition, path + ".dispositions");
        }
        verifyMoney(policy, ImmutableFundsBenefitRefundPolicySpec.Fields.refundableAmount,
                path + ".refundableAmount", false, false);
        verifyMoney(policy, ImmutableFundsBenefitRefundPolicySpec.Fields.nonRefundableAmount,
                path + ".nonRefundableAmount", false, false);
        if (dispositions.contains(FundsBenefitRefundDisposition.NO_REFUND.name())
                && !isTextValue(policy.get(ImmutableFundsBenefitRefundPolicySpec.Fields.refundRuleVersion))
                && !isTextValue(policy.get(ImmutableFundsBenefitRefundPolicySpec.Fields.refundDecisionId))
                && !isTextValue(policy.get(ImmutableFundsBenefitRefundPolicySpec.Fields.decisionSource))) {
            throw new IllegalArgumentException(path + " NO_REFUND requires rule version or decision reference");
        }
        verifyBenefitContext(asNullableMap(policy.get(
                        ImmutableFundsBenefitRefundPolicySpec.Fields.contextVariables), path + ".contextVariables"),
                path + ".contextVariables");
    }

    private static void verifyBenefitContext(@Nullable Map<String, ?> contextVariables, String path) {
        if (contextVariables == null) {
            return;
        }
        for (String key : contextVariables.keySet()) {
            if (RESERVED_BENEFIT_CONTEXT_KEYS.contains(key)) {
                throw new IllegalArgumentException(path + " must not contain core benefit field: " + key);
            }
        }
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

    private static Money verifyMoney(Map<String, ?> owner, String fieldName, String path) {
        return verifyMoney(owner, fieldName, path, true);
    }

    private static Money verifyMoney(Map<String, ?> owner, String fieldName, String path, boolean positive) {
        return verifyMoney(owner, fieldName, path, true, positive);
    }

    private static @Nullable Money verifyMoney(Map<String, ?> owner,
                                               String fieldName,
                                               String path,
                                               boolean required,
                                               boolean positive) {
        Map<String, ?> money = asNullableMap(owner.get(fieldName), path);
        if (money == null) {
            if (required) {
                throw new IllegalArgumentException(path + " is required");
            }
            return null;
        }
        try {
            return parseMoney(money, positive);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(path + ": " + ex.getMessage(), ex);
        }
    }

    private static @NonNull Money parseMoney(@NonNull Map<String, ?> values, boolean positive) {
        Object rawCurrency = values.get(Money.Fields.currency);
        if (!(rawCurrency instanceof String currency) || !StringUtils.hasText(currency)) {
            throw new IllegalArgumentException("money.currency is required");
        }
        if (!values.containsKey(Money.Fields.amount)) {
            throw new IllegalArgumentException("money.amount is required");
        }
        long amount = parseAmount(values.get(Money.Fields.amount), positive);
        return Money.immutable(amount, CurrencyIsoCode.valueOf(currency));
    }

    private static long parseAmount(Object value, boolean positive) {
        BigInteger parsed = switch (value) {
            case Byte number -> BigInteger.valueOf(number.longValue());
            case Short number -> BigInteger.valueOf(number.longValue());
            case Integer number -> BigInteger.valueOf(number.longValue());
            case Long number -> BigInteger.valueOf(number);
            case BigInteger number -> number;
            case String text when text.matches("-?\\d+") -> new BigInteger(text);
            default -> throw new IllegalArgumentException("money.amount must be integer");
        };
        BigInteger minimum = positive ? BigInteger.ONE : BigInteger.ZERO;
        if (parsed.compareTo(minimum) < 0) {
            throw new IllegalArgumentException(positive
                    ? "money.amount must be positive"
                    : "money.amount must not be negative");
        }
        if (parsed.compareTo(LONG_MAX_VALUE) > 0) {
            throw new IllegalArgumentException("money.amount exceeds system limit");
        }
        return parsed.longValue();
    }

    private static <E extends Enum<E>> E verifyEnum(Class<E> enumType,
                                                       Map<String, ?> owner,
                                                       String fieldName,
                                                       String path) {
        return verifyEnum(enumType, owner, fieldName, path, true);
    }

    private static <E extends Enum<E>> E verifyEnum(Class<E> enumType,
                                                       Map<String, ?> owner,
                                                       String fieldName,
                                                       String path,
                                                       boolean required) {
        Object rawValue = owner.get(fieldName);
        if (rawValue == null && !required) {
            return null;
        }
        if (!(rawValue instanceof String value) || !StringUtils.hasText(value)) {
            throw new IllegalArgumentException(path + " is required");
        }
        return enumValue(enumType, value, path);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String path) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(path + " must be " + enumType.getSimpleName(), ex);
        }
    }

    private static String requireText(Map<String, ?> owner, String fieldName) {
        return requireText(owner, fieldName, fieldName);
    }

    private static String requireText(Map<String, ?> owner, String fieldName, String path) {
        Object value = owner.get(fieldName);
        if (!(value instanceof String text) || !StringUtils.hasText(text)) {
            throw new IllegalArgumentException(path + " is required");
        }
        return text;
    }

    private static boolean isTextValue(Object value) {
        return value instanceof String text && StringUtils.hasText(text);
    }

    private static long addExact(long left, long right, String message) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(message, ex);
        }
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

    private static List<Map<String, ?>> requiredChildObjects(Map<String, ?> owner, String fieldName, String path) {
        List<Map<String, ?>> result = childObjects(owner, fieldName, path);
        if (result.isEmpty()) {
            throw new IllegalArgumentException(path + " must not be empty");
        }
        return result;
    }

    private static List<String> requiredChildTexts(Map<String, ?> owner, String fieldName, String path) {
        Object rawValue = owner.get(fieldName);
        if (!(rawValue instanceof List<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException(path + " must not be empty");
        }
        List<String> texts = new ArrayList<>(values.size());
        for (Object value : values) {
            if (!(value instanceof String text) || !StringUtils.hasText(text)) {
                throw new IllegalArgumentException(path + " must contain text values");
            }
            texts.add(text);
        }
        return List.copyOf(texts);
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

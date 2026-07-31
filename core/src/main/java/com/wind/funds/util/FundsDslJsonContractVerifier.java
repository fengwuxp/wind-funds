package com.wind.funds.util;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.model.transaction.FundsBenefitSpecValidators;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.enums.RouteReplayType;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Verifier for DSL JSON contract samples.
 */
public final class FundsDslJsonContractVerifier {

    private static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    private static final Set<String> FIXTURE_LEVELS = Set.of(
            "DOC_ONLY",
            "CONTRACT_ONLY",
            "FUNDS_FLOW",
            "SERVICE_FLOW",
            "GOVERNANCE_FLOW");

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
        if (!(fixtureLevel instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("fixtureLevel is required");
        }
        if (!FIXTURE_LEVELS.contains(value)) {
            throw new IllegalArgumentException("fixtureLevel must be one of DOC_ONLY, CONTRACT_ONLY, FUNDS_FLOW, "
                    + "SERVICE_FLOW, GOVERNANCE_FLOW");
        }
        if (!"CONTRACT_ONLY".equals(value)) {
            return;
        }
        requireText(document, "scenarioCode");
        requiredChildTexts(document, "acceptanceIds", "acceptanceIds");
        requiredChildTexts(document, "tddIds", "tddIds");
        requiredChildTexts(document, "systemDesignRefs", "systemDesignRefs");
        requireText(document, "targetTestClass");
        requiredChildTexts(document, "coreAssertions", "coreAssertions");
        requiredChildTexts(document, "notDone", "notDone");
        rejectContractOnlyFundsFlowAssertions(document);
        Map<String, ?> validation = asMap(document.get("validation"), "validation");
        requiredChildTexts(validation, "mustPass", "validation.mustPass");
        requiredChildTexts(validation, "mustFail", "validation.mustFail");
    }

    private static void rejectContractOnlyFundsFlowAssertions(Map<String, ?> document) {
        for (String field : List.of("expectedRoute", "expectedPosting", "replayRequest")) {
            if (document.containsKey(field) && document.get(field) != null) {
                throw new IllegalArgumentException(
                        "CONTRACT_ONLY fixture must not contain " + field
                                + "; use FUNDS_FLOW, SERVICE_FLOW or GOVERNANCE_FLOW for flow assertions");
            }
        }
    }

    private static void verifyInstruction(@Nullable Map<String, ?> instruction) {
        if (instruction == null) {
            return;
        }
        FundsInstructionType instructionType = verifyEnum(FundsInstructionType.class, instruction,
                ImmutableFundsInstructionSpec.Fields.instructionType, "instruction.instructionType");
        FundsTransactionEventType eventType = verifyEnum(FundsTransactionEventType.class, instruction,
                ImmutableFundsInstructionSpec.Fields.eventType, "instruction.eventType");
        DefaultFundsTransactionType transactionType = verifyEnum(DefaultFundsTransactionType.class, instruction,
                ImmutableFundsInstructionSpec.Fields.transactionType, "instruction.transactionType");
        if (!DefaultFundsTransactionType.isValidInstructionCombination(
                instructionType, eventType, transactionType)) {
            throw new IllegalArgumentException(
                    "instruction instructionType/eventType/transactionType combination is invalid");
        }
        verifyMoney(instruction, ImmutableFundsInstructionSpec.Fields.amount, "instruction.amount");
        verifyMoney(instruction, ImmutableFundsInstructionSpec.Fields.originalAmount, "instruction.originalAmount");
        verifyReference(asNullableMap(instruction.get(ImmutableFundsInstructionSpec.Fields.reference),
                "instruction.reference"));
        verifyRouteContext(asNullableMap(instruction.get(ImmutableFundsInstructionSpec.Fields.contextVariables),
                        "instruction.contextVariables"),
                "instruction.contextVariables");
        rejectLegacyBenefitSnapshotField(instruction);
    }

    private static void verifyReference(@Nullable Map<String, ?> reference) {
        if (reference == null) {
            return;
        }
        verifyEnum(FundsInstructionReferenceType.class, reference,
                ImmutableFundsInstructionReferenceSpec.Fields.referenceType, "instruction.reference.referenceType");
        requireText(reference, ImmutableFundsInstructionReferenceSpec.Fields.referenceSn);
    }

    private static void rejectLegacyBenefitSnapshotField(Map<String, ?> instruction) {
        if (instruction.containsKey("benefitSnapshot")) {
            throw new IllegalArgumentException(
                    "instruction.benefitSnapshot legacy benefit snapshot DSL has been removed");
        }
    }

    private static void verifyRoute(@Nullable Map<String, ?> route) {
        if (route == null) {
            return;
        }
        verifyEnum(RouteReplayType.class, route, "replayType", "expectedRoute.replayType", false);
        verifyRouteContext(asNullableMap(route.get("contextVariables"), "expectedRoute.contextVariables"),
                "expectedRoute.contextVariables");
        Map<String, ?> routingDecision = asNullableMap(route.get("routingDecision"), "expectedRoute.routingDecision");
        verifyRoutingDecision(routingDecision);
        for (Map<String, ?> participant : childObjects(route, "participants", "expectedRoute.participants")) {
            verifyEnum(RouteParticipantRole.class, participant, "participantRole", "expectedRoute.participants.participantRole");
            JsonSubjectRef subjectRef = parseSubjectRef(
                    asNullableMap(participant.get("subjectRef"), "expectedRoute.participants.subjectRef"),
                    "expectedRoute.participants.subjectRef");
            verifyAccountHierarchySnapshot(asNullableMap(participant.get("accountHierarchySnapshot"),
                            "expectedRoute.participants.accountHierarchySnapshot"),
                    subjectRef,
                    "expectedRoute.participants.accountHierarchySnapshot");
            verifyRouteContext(asNullableMap(participant.get("contextVariables"),
                            "expectedRoute.participants.contextVariables"),
                    "expectedRoute.participants.contextVariables");
        }
        for (Map<String, ?> leg : childObjects(route, "legs", "expectedRoute.legs")) {
            verifyEnum(RouteLegType.class, leg, "legType", "expectedRoute.legs.legType");
            verifyNode(asNullableMap(leg.get("sourceNode"),
                    "expectedRoute.legs.sourceNode"), "expectedRoute.legs.sourceNode");
            verifyNode(asNullableMap(leg.get("targetNode"), "expectedRoute.legs.targetNode"), "expectedRoute.legs.targetNode");
            verifyMoney(leg, "amount", "expectedRoute.legs.amount");
            verifyEnum(LedgerBalanceEffectType.class, leg,
                    "balanceEffectType", "expectedRoute.legs.balanceEffectType");
            verifyEnum(LedgerPhaseCode.class, leg, "phaseCode", "expectedRoute.legs.phaseCode");
            verifyEnum(RouteReplayPolicy.class, leg, "replayPolicy", "expectedRoute.legs.replayPolicy");
            verifyRouteContext(asNullableMap(leg.get("contextVariables"), "expectedRoute.legs.contextVariables"),
                    "expectedRoute.legs.contextVariables");
        }
    }

    private static void verifyRoutingDecision(@Nullable Map<String, ?> routingDecision) {
        if (routingDecision == null) {
            return;
        }
        verifyRouteContext(asNullableMap(routingDecision.get("contextVariables"),
                        "expectedRoute.routingDecision.contextVariables"),
                "expectedRoute.routingDecision.contextVariables");
    }

    private static void verifyAccountHierarchySnapshot(@Nullable Map<String, ?> snapshot,
                                                       JsonSubjectRef accountRef,
                                                       String path) {
        if (snapshot == null) {
            return;
        }
        requireText(snapshot, "relationSn", path + ".relationSn");
        parseAccountHierarchyParentRef(snapshot, accountRef, path);
    }

    private static void parseAccountHierarchyParentRef(Map<String, ?> snapshot,
                                                       JsonSubjectRef accountRef,
                                                       String path) {
        String fieldName = "parentAccountRef";
        JsonSubjectRef relationRef = parseAccountHierarchySubjectRef(
                asNullableMap(snapshot.get(fieldName), path + "." + fieldName), path + "." + fieldName);
        if (sameSubject(accountRef, relationRef)) {
            throw new IllegalArgumentException(path + "." + fieldName + " must not reference accountRef itself");
        }
        if (!compatible(accountRef.tenantId(), relationRef.tenantId())) {
            throw new IllegalArgumentException(path + "." + fieldName + ".tenantId must match account tenantId");
        }
        if (!compatible(accountRef.currency(), relationRef.currency())) {
            throw new IllegalArgumentException(path + "." + fieldName + ".currency must match account currency");
        }
    }

    private static JsonSubjectRef parseAccountHierarchySubjectRef(@Nullable Map<String, ?> subjectRef, String path) {
        if (subjectRef == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        Object rawSubjectType = subjectRef.get("subjectType");
        if (!(rawSubjectType instanceof String subjectType)
                || !FundsSubjectType.isLedgerPostableName(subjectType)) {
            throw new IllegalArgumentException(path
                    + ".subjectType must be FUNDING_ACCOUNT or CREDIT_ACCOUNT");
        }
        JsonSubjectRef parsed = parseSubjectRef(subjectRef, path);
        return parsed;
    }

    private static FundsSubjectType verifyNode(@Nullable Map<String, ?> node, String path) {
        if (node == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        RouteNodeType nodeType = verifyEnum(RouteNodeType.class, node, "nodeType", path + ".nodeType", false);
        if (nodeType == RouteNodeType.PAYMENT_INSTRUMENT || nodeType == RouteNodeType.EXTERNAL_ACCOUNT) {
            throw new IllegalArgumentException(routeLegNodeLabel(path) + " must be ledger-postable");
        }
        RouteNodeRole nodeRole = verifyEnum(RouteNodeRole.class, node, "nodeRole", path + ".nodeRole", false);
        if (path.endsWith("sourceNode") && nodeRole == RouteNodeRole.TARGET) {
            throw new IllegalArgumentException(path + ".nodeRole must be SOURCE");
        }
        if (path.endsWith("targetNode") && nodeRole == RouteNodeRole.SOURCE) {
            throw new IllegalArgumentException(path + ".nodeRole must be TARGET");
        }
        FundsSubjectType subjectType = verifyEnum(FundsSubjectType.class, node, "subjectType", path + ".subjectType");
        if (!subjectType.isLedgerPostable()) {
            throw new IllegalArgumentException(routeLegNodeLabel(path) + " must be FUNDING_ACCOUNT or CREDIT_ACCOUNT");
        }
        requireText(node, "subjectId", path + ".subjectId");
        verifyEnum(LedgerSubjectCode.class, node, "ledgerSubjectCode", path + ".ledgerSubjectCode");
        return subjectType;
    }

    private static String routeLegNodeLabel(String path) {
        if (path.endsWith("sourceNode")) {
            return "RouteLeg sourceNode";
        }
        if (path.endsWith("targetNode")) {
            return "RouteLeg targetNode";
        }
        return path;
    }

    private static JsonSubjectRef parseSubjectRef(@Nullable Map<String, ?> subjectRef, String path) {
        if (subjectRef == null) {
            throw new IllegalArgumentException(path + " is required");
        }
        FundsSubjectType subjectType = verifyEnum(FundsSubjectType.class, subjectRef, "subjectType",
                path + ".subjectType");
        String subjectId = requireText(subjectRef, "subjectId", path + ".subjectId");
        Long tenantId = optionalLong(subjectRef.get("tenantId"), path + ".tenantId");
        String currency = optionalText(subjectRef.get("currency"), path + ".currency");
        return new JsonSubjectRef(subjectType, subjectId, tenantId, currency);
    }

    private static void verifyPosting(@Nullable Map<String, ?> posting) {
        if (posting == null) {
            return;
        }
        for (Map<String, ?> plan : requiredChildObjects(posting, "postingPlans",
                "expectedPosting.postingPlans")) {
            verifyEnum(LedgerPostingIntentType.class, plan, "intent", "expectedPosting.postingPlans.intent");
            verifyEnum(LedgerPostingScope.class, plan, "postingScope", "expectedPosting.postingPlans.postingScope");
            verifyEnum(LedgerBalanceEffectType.class, plan, "balanceEffectType", "expectedPosting.postingPlans.balanceEffectType");
            verifyEnum(LedgerPhaseCode.class, plan, "phaseCode", "expectedPosting.postingPlans.phaseCode");
            Map<CurrencyIsoCode, Long> debitAmounts = new EnumMap<>(CurrencyIsoCode.class);
            Map<CurrencyIsoCode, Long> creditAmounts = new EnumMap<>(CurrencyIsoCode.class);
            for (Map<String, ?> entry : requiredChildObjects(plan, "entries",
                    "expectedPosting.postingPlans.entries")) {
                verifyEnum(FundsSubjectType.class, entry, "subjectType", "expectedPosting.postingPlans.entries.subjectType");
                requireText(entry, "subjectId", "expectedPosting.postingPlans.entries.subjectId");
                verifyEnum(LedgerSubjectCode.class, entry, "ledgerSubjectCode",
                        "expectedPosting.postingPlans.entries.ledgerSubjectCode");
                CurrencyIsoCode currency = verifyEnum(CurrencyIsoCode.class, entry, "currency",
                        "expectedPosting.postingPlans.entries.currency");
                AccountBalancePeriodType periodType = verifyEnum(AccountBalancePeriodType.class, entry, "periodType",
                        "expectedPosting.postingPlans.entries.periodType");
                String periodId = requireText(entry, "periodId", "expectedPosting.postingPlans.entries.periodId");
                EntrySide entrySide = verifyEnum(EntrySide.class, entry, "entrySide",
                        "expectedPosting.postingPlans.entries.entrySide");
                Money amount = verifyMoney(entry, "amount", "expectedPosting.postingPlans.entries.amount");
                if (currency != amount.getCurrency()) {
                    throw new IllegalArgumentException("expectedPosting.postingPlans.entries entry currency must match "
                            + "amount currency");
                }
                verifyLedgerEntryPeriod(periodType, periodId);
                addPostingPlanAmount(entrySide, amount, debitAmounts, creditAmounts);
            }
            verifyPostingPlanBalanced(debitAmounts, creditAmounts);
        }
    }

    private static void addPostingPlanAmount(EntrySide entrySide,
                                             Money amount,
                                             Map<CurrencyIsoCode, Long> debitAmounts,
                                             Map<CurrencyIsoCode, Long> creditAmounts) {
        if (entrySide == EntrySide.DEBIT) {
            addAmount(debitAmounts, amount, "posting plan debit amount sum overflow");
            return;
        }
        if (entrySide == EntrySide.CREDIT) {
            addAmount(creditAmounts, amount, "posting plan credit amount sum overflow");
            return;
        }
        throw new IllegalArgumentException("expectedPosting.postingPlans.entries.entrySide must be DEBIT or CREDIT");
    }

    private static void verifyPostingPlanBalanced(Map<CurrencyIsoCode, Long> debitAmounts,
                                                  Map<CurrencyIsoCode, Long> creditAmounts) {
        if (debitAmounts.isEmpty() || creditAmounts.isEmpty()) {
            throw new IllegalArgumentException("expectedPosting.postingPlans posting plan must include debit and "
                    + "credit entries");
        }
        if (!debitAmounts.keySet().equals(creditAmounts.keySet())) {
            throw new IllegalArgumentException("expectedPosting.postingPlans posting plan currency mismatch");
        }
        if (debitAmounts.size() != 1) {
            throw new IllegalArgumentException("expectedPosting.postingPlans posting plan must use one currency");
        }
        for (Map.Entry<CurrencyIsoCode, Long> debitEntry : debitAmounts.entrySet()) {
            if (!Objects.equals(debitEntry.getValue(), creditAmounts.get(debitEntry.getKey()))) {
                throw new IllegalArgumentException("expectedPosting.postingPlans posting plan must be balanced");
            }
        }
    }

    private static void verifyLedgerEntryPeriod(AccountBalancePeriodType periodType, String periodId) {
        if (periodType == AccountBalancePeriodType.LIFETIME) {
            if (!AccountBalancePeriodType.LIFETIME.name().equals(periodId)) {
                throw new IllegalArgumentException("expectedPosting.postingPlans.entries.periodId must be LIFETIME");
            }
            return;
        }
        if (AccountBalancePeriodType.LIFETIME.name().equals(periodId)) {
            throw new IllegalArgumentException("expectedPosting.postingPlans.entries.periodId must not be LIFETIME");
        }
    }

    private static void verifyReplayRequest(@Nullable Map<String, ?> replayRequest) {
        if (replayRequest == null) {
            return;
        }
        verifyEnum(FundsSubjectType.class, replayRequest, "subjectType", "replayRequest.subjectType", false);
        verifyRouteContext(asNullableMap(replayRequest.get("contextVariables"), "replayRequest.contextVariables"),
                "replayRequest.contextVariables");
    }

    private static void verifyRouteContext(@Nullable Map<String, ?> contextVariables, String path) {
        if (contextVariables == null) {
            return;
        }
        Map<String, Object> context = asObjectValueMap(contextVariables);
        if (PaymentInstrumentSensitiveValueValidator.containsSensitiveField(context)
                || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(context)) {
            throw new IllegalArgumentException(path + " must not contain sensitive fields");
        }
        FundsBenefitSpecValidators.immutableInstructionContext(context, pathOwner(path));
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
        if (!(rawCurrency instanceof String currency) || currency.isBlank()) {
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
        if (!(rawValue instanceof String value) || value.isBlank()) {
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
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(path + " is required");
        }
        return text;
    }

    private static @Nullable String optionalText(@Nullable Object value, String path) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        throw new IllegalArgumentException(path + " must be text");
    }

    private static @Nullable Long optionalLong(@Nullable Object value, String path) {
        if (value == null) {
            return null;
        }
        return parseInteger(value, path);
    }

    private static long parseInteger(Object value, String path) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException(path + " must be integer");
    }

    private static boolean sameSubject(JsonSubjectRef left, JsonSubjectRef right) {
        return left.subjectType() == right.subjectType()
                && Objects.equals(left.subjectId(), right.subjectId())
                && compatible(left.tenantId(), right.tenantId());
    }

    private static boolean compatible(@Nullable Object left, @Nullable Object right) {
        return left == null || right == null || Objects.equals(left, right);
    }

    private static long addExact(long left, long right, String message) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(message, ex);
        }
    }

    private static void addAmount(Map<CurrencyIsoCode, Long> target, Money amount, String overflowMessage) {
        target.merge(amount.getCurrency(), amount.getAmount(),
                (left, right) -> addExact(left, right, overflowMessage));
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
            if (!(value instanceof String text) || text.isBlank()) {
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

    private static Map<String, Object> asObjectValueMap(Map<String, ?> values) {
        Map<String, Object> result = new LinkedHashMap<>(values.size());
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(result);
    }

    private static String pathOwner(String path) {
        if (path.endsWith(".contextVariables")) {
            return path.substring(0, path.length() - ".contextVariables".length());
        }
        return path;
    }

    private record JsonSubjectRef(FundsSubjectType subjectType,
                                  String subjectId,
                                  @Nullable Long tenantId,
                                  @Nullable String currency) {
    }
}

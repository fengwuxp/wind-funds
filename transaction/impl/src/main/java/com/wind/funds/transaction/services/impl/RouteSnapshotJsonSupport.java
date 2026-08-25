package com.wind.funds.transaction.services.impl;

import com.wind.funds.route.model.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.route.model.ImmutableExternalAccountRefSpec;
import com.wind.funds.route.model.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.model.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.funds.route.model.ImmutableRoutingDecisionSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableRouteSnapshotSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.jackson.WindJson;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

final class RouteSnapshotJsonSupport {

    private static final String MONEY_AMOUNT = "amount";

    private static final String MONEY_CURRENCY = "currency";

    private RouteSnapshotJsonSupport() {
    }

    static String toRouteSnapshotJson(RouteSnapshotSpec routeSnapshot) {
        return WindJson.toJsonString(routeSummary(routeSnapshot));
    }

    static RouteSnapshotSpec parseRouteSnapshot(String routeSnapshotJson, LocalDateTime defaultResolvedAt) {
        ObjectNode values = WindJson.parseObject(routeSnapshotJson, ObjectNode.class);
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(longValue(values, ImmutableRouteSnapshotSpec.Fields.tenantId))
                .snapshotId(textValue(values, ImmutableRouteSnapshotSpec.Fields.snapshotId))
                .snapshotSchemaVersion(textValue(values, ImmutableRouteSnapshotSpec.Fields.snapshotSchemaVersion))
                .routeCode(textValue(values, ImmutableRouteSnapshotSpec.Fields.routeCode))
                .routeVersion(textValue(values, ImmutableRouteSnapshotSpec.Fields.routeVersion))
                .businessScene(textValue(values, ImmutableRouteSnapshotSpec.Fields.businessScene))
                .businessSn(textValue(values, ImmutableRouteSnapshotSpec.Fields.businessSn))
                .instructionType(FundsInstructionType.valueOf(
                        textValue(values, ImmutableRouteSnapshotSpec.Fields.instructionType)))
                .eventType(FundsTransactionEventType.valueOf(
                        textValue(values, ImmutableRouteSnapshotSpec.Fields.eventType)))
                .transactionType(DefaultFundsTransactionType.valueOf(
                        textValue(values, ImmutableRouteSnapshotSpec.Fields.transactionType)))
                .participants(parseParticipants(arrayValue(values, ImmutableRouteSnapshotSpec.Fields.participants)))
                .legs(parseLegs(arrayValue(values, ImmutableRouteSnapshotSpec.Fields.legs)))
                .routingDecision(parseRoutingDecision(objectValue(values,
                        ImmutableRouteSnapshotSpec.Fields.routingDecision)))
                .paymentInstrumentRef(parsePaymentInstrumentRef(objectValue(values,
                        ImmutableRouteSnapshotSpec.Fields.paymentInstrumentRef)))
                .externalAccountRef(parseExternalAccountRef(objectValue(values,
                        ImmutableRouteSnapshotSpec.Fields.externalAccountRef)))
                .platformAccounts(parsePlatformAccounts(objectValue(values,
                        ImmutableRouteSnapshotSpec.Fields.platformAccounts)))
                .resolvedAt(parseLocalDateTime(textValue(values, ImmutableRouteSnapshotSpec.Fields.resolvedAt),
                        defaultResolvedAt))
                .expiresAt(parseLocalDateTime(textValue(values, ImmutableRouteSnapshotSpec.Fields.expiresAt), null))
                .description(textValue(values, ImmutableRouteSnapshotSpec.Fields.description))
                .contextVariables(parseObjectMap(objectValue(values,
                        ImmutableRouteSnapshotSpec.Fields.contextVariables)))
                .build();
    }

    static Map<String, Object> routeSummary(RouteSnapshotSpec routeSnapshot) {
        assertNoSensitiveFields(routeSnapshot);
        Map<String, Object> values = new TreeMap<>();
        values.put(ImmutableRouteSnapshotSpec.Fields.tenantId, routeSnapshot.getTenantId());
        values.put(ImmutableRouteSnapshotSpec.Fields.snapshotId, routeSnapshot.getSnapshotId());
        values.put(ImmutableRouteSnapshotSpec.Fields.snapshotSchemaVersion, routeSnapshot.getSnapshotSchemaVersion());
        values.put(ImmutableRouteSnapshotSpec.Fields.businessScene, routeSnapshot.getBusinessScene());
        values.put(ImmutableRouteSnapshotSpec.Fields.businessSn, routeSnapshot.getBusinessSn());
        values.put(ImmutableRouteSnapshotSpec.Fields.instructionType, routeSnapshot.getInstructionType().name());
        values.put(ImmutableRouteSnapshotSpec.Fields.eventType, routeSnapshot.getEventType().name());
        values.put(ImmutableRouteSnapshotSpec.Fields.transactionType, routeSnapshot.getTransactionType().name());
        values.put(ImmutableRouteSnapshotSpec.Fields.routeCode, routeSnapshot.getRouteCode());
        values.put(ImmutableRouteSnapshotSpec.Fields.routeVersion, routeSnapshot.getRouteVersion());
        values.put(ImmutableRouteSnapshotSpec.Fields.resolvedAt, routeSnapshot.getResolvedAt().toString());
        values.put(ImmutableRouteSnapshotSpec.Fields.expiresAt,
                routeSnapshot.getExpiresAt() == null ? null : routeSnapshot.getExpiresAt().toString());
        values.put(ImmutableRouteSnapshotSpec.Fields.description, routeSnapshot.getDescription());
        values.put(ImmutableRouteSnapshotSpec.Fields.routingDecision, routeSnapshot.getRoutingDecision() == null
                ? Map.of()
                : sortedMap(routeDecisionSummary(routeSnapshot.getRoutingDecision())));
        values.put(ImmutableRouteSnapshotSpec.Fields.paymentInstrumentRef,
                instrumentSummary(routeSnapshot.getPaymentInstrumentRef()));
        values.put(ImmutableRouteSnapshotSpec.Fields.externalAccountRef,
                externalAccountSummary(routeSnapshot.getExternalAccountRef()));
        values.put(ImmutableRouteSnapshotSpec.Fields.participants, routeSnapshot.getParticipants()
                .stream()
                .map(RouteSnapshotJsonSupport::participantSummary)
                .toList());
        values.put(ImmutableRouteSnapshotSpec.Fields.legs, routeSnapshot.getLegs()
                .stream()
                .map(RouteSnapshotJsonSupport::legSummary)
                .toList());
        values.put(ImmutableRouteSnapshotSpec.Fields.platformAccounts,
                platformAccountsSummary(routeSnapshot.getPlatformAccounts()));
        values.put(ImmutableRouteSnapshotSpec.Fields.contextVariables, sortedMap(routeSnapshot.getContextVariables()));
        return values;
    }

    static Map<String, Object> persistedRouteSummary(String routeSnapshotJson) {
        ObjectNode values = WindJson.parseObject(routeSnapshotJson, ObjectNode.class);
        return new TreeMap<>(WindJson.convertValue(values, new TypeReference<Map<String, Object>>() {
        }));
    }

    static Map<String, Object> pathOnlyRouteSummary(Map<String, Object> routeSummary) {
        Map<String, Object> values = new TreeMap<>(routeSummary);
        Object legsValue = values.get(ImmutableRouteSnapshotSpec.Fields.legs);
        if (legsValue instanceof List<?> legs) {
            values.put(ImmutableRouteSnapshotSpec.Fields.legs, legs.stream()
                    .map(RouteSnapshotJsonSupport::pathOnlyLegSummary)
                    .toList());
        }
        return values;
    }

    private static Map<String, Object> pathOnlyLegSummary(Object legValue) {
        if (!(legValue instanceof Map<?, ?> leg)) {
            return Map.of();
        }
        Map<String, Object> values = stringKeyMap(leg);
        values.remove("balanceEffectType");
        values.remove("phaseCode");
        values.remove("periodType");
        values.remove("periodId");
        values.remove("constraintOverrides");
        values.computeIfPresent(ImmutableRouteLegSpec.Fields.sourceNode,
                (key, node) -> pathOnlyNodeSummary(node));
        values.computeIfPresent(ImmutableRouteLegSpec.Fields.targetNode,
                (key, node) -> pathOnlyNodeSummary(node));
        return values;
    }

    private static Map<String, Object> pathOnlyNodeSummary(Object nodeValue) {
        if (!(nodeValue instanceof Map<?, ?> node)) {
            return Map.of();
        }
        Map<String, Object> values = stringKeyMap(node);
        values.remove("ledgerSubjectCode");
        return values;
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> values = new TreeMap<>();
        source.forEach((key, value) -> values.put(String.valueOf(key), value));
        return values;
    }

    private static void assertNoSensitiveFields(RouteSnapshotSpec routeSnapshot) {
        assertNoSensitiveContext(routeSnapshot.getContextVariables(), "routeSnapshot.contextVariables");
        routeSnapshot.getParticipants().forEach(participant -> assertNoSensitiveContext(
                participant.getContextVariables(), "routeParticipant.contextVariables"));
        routeSnapshot.getLegs().forEach(leg -> assertNoSensitiveContext(
                leg.getContextVariables(), "routeLeg.contextVariables"));
        RoutingDecisionSpec routingDecision = routeSnapshot.getRoutingDecision();
        if (routingDecision != null) {
            assertNoSensitiveContext(routingDecision.getContextVariables(), "routingDecision.contextVariables");
        }
        PaymentInstrumentRefSpec instrumentRef = routeSnapshot.getPaymentInstrumentRef();
        if (instrumentRef != null) {
            if (PaymentInstrumentSensitiveValueValidator.isRawSensitiveInstrumentNo(
                    instrumentRef.getInstrumentNo())) {
                throw new IllegalArgumentException(
                        "paymentInstrumentRef.instrumentNo must be masked or token reference");
            }
            assertNoSensitiveContext(instrumentRef.getBindingSnapshot(), "paymentInstrumentRef.bindingSnapshot");
        }
        ExternalAccountRefSpec externalAccountRef = routeSnapshot.getExternalAccountRef();
        if (externalAccountRef != null) {
            if (ExternalAccountSensitiveValueValidator.isRawSensitiveExternalAccountNo(
                    externalAccountRef.getExternalAccountNo())) {
                throw new IllegalArgumentException(
                        "externalAccountRef.externalAccountNo must be masked or token reference");
            }
            assertNoSensitiveContext(externalAccountRef.getContextVariables(),
                    "externalAccountRef.contextVariables");
        }
    }

    private static void assertNoSensitiveContext(Map<String, Object> contextVariables, String fieldName) {
        if (PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables)) {
            throw new IllegalArgumentException(fieldName + " must not contain sensitive fields");
        }
    }

    private static Map<String, Object> legSummary(RouteLegSpec leg) {
        Map<String, Object> values = new TreeMap<>();
        values.put(ImmutableRouteLegSpec.Fields.legId, leg.getLegId());
        values.put(ImmutableRouteLegSpec.Fields.sequence, leg.getSequence());
        values.put(ImmutableRouteLegSpec.Fields.legType, leg.getLegType().name());
        values.put(ImmutableRouteLegSpec.Fields.sourceNode, routeNodeSummary(leg.getSourceNode()));
        values.put(ImmutableRouteLegSpec.Fields.targetNode, routeNodeSummary(leg.getTargetNode()));
        values.put(ImmutableRouteLegSpec.Fields.amount, moneySummary(leg.getAmount()));
        values.put(ImmutableRouteLegSpec.Fields.originalAmount, moneySummary(leg.getOriginalAmount()));
        values.put(ImmutableRouteLegSpec.Fields.exchangeRate, leg.getExchangeRate());
        values.put(ImmutableRouteLegSpec.Fields.replayPolicy, leg.getReplayPolicy().name());
        values.put(ImmutableRouteLegSpec.Fields.replayRefLegId, leg.getReplayRefLegId());
        values.put(ImmutableRouteLegSpec.Fields.description, leg.getDescription());
        values.put(ImmutableRouteLegSpec.Fields.contextVariables, sortedMap(leg.getContextVariables()));
        return values;
    }

    private static Map<String, Object> participantSummary(RouteParticipantSpec participant) {
        Map<String, Object> values = new TreeMap<>();
        values.put(ImmutableRouteParticipantSpec.Fields.participantRole, participant.getParticipantRole().name());
        values.put(ImmutableRouteParticipantSpec.Fields.subjectRef, subjectSummary(participant.getSubjectRef()));
        values.put(ImmutableRouteParticipantSpec.Fields.ledgerProfileCode, participant.getLedgerProfileCode());
        values.put(ImmutableRouteParticipantSpec.Fields.currency, participant.getCurrency());
        values.put(ImmutableRouteParticipantSpec.Fields.amount, moneySummary(participant.getAmount()));
        values.put(ImmutableRouteParticipantSpec.Fields.description, participant.getDescription());
        values.put(ImmutableRouteParticipantSpec.Fields.accountHierarchySnapshot,
                accountHierarchySnapshotSummary(participant.getAccountHierarchySnapshot()));
        values.put(ImmutableRouteParticipantSpec.Fields.contextVariables,
                sortedMap(participant.getContextVariables()));
        return values;
    }

    private static Map<String, Object> externalAccountSummary(ExternalAccountRefSpec externalAccountRef) {
        Map<String, Object> values = new TreeMap<>();
        if (externalAccountRef == null) {
            return values;
        }
        values.put(ImmutableExternalAccountRefSpec.Fields.externalAccountId,
                externalAccountRef.getExternalAccountId());
        values.put(ImmutableExternalAccountRefSpec.Fields.externalAccountType,
                externalAccountRef.getExternalAccountType());
        values.put(ImmutableExternalAccountRefSpec.Fields.externalAccountNo,
                externalAccountRef.getExternalAccountNo());
        values.put(ImmutableExternalAccountRefSpec.Fields.providerCode, externalAccountRef.getProviderCode());
        values.put(ImmutableExternalAccountRefSpec.Fields.channelCode, externalAccountRef.getChannelCode());
        values.put(ImmutableExternalAccountRefSpec.Fields.currency, externalAccountRef.getCurrency());
        values.put(ImmutableExternalAccountRefSpec.Fields.countryCode, externalAccountRef.getCountryCode());
        values.put(ImmutableExternalAccountRefSpec.Fields.description, externalAccountRef.getDescription());
        values.put(ImmutableExternalAccountRefSpec.Fields.contextVariables,
                sortedMap(externalAccountRef.getContextVariables()));
        return values;
    }

    private static Map<String, Object> instrumentSummary(PaymentInstrumentRefSpec instrumentRef) {
        Map<String, Object> values = new TreeMap<>();
        if (instrumentRef == null) {
            return values;
        }
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.instrumentId, instrumentRef.getInstrumentId());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.instrumentType, instrumentRef.getInstrumentType());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.instrumentNo, instrumentRef.getInstrumentNo());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.ownerId, instrumentRef.getOwnerId());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.ownerType, instrumentRef.getOwnerType());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.tenantId, instrumentRef.getTenantId());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.currency, instrumentRef.getCurrency());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.status, instrumentRef.getStatus());
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.bindingSnapshot,
                sortedMap(instrumentRef.getBindingSnapshot()));
        values.put(ImmutablePaymentInstrumentRefSpec.Fields.description, instrumentRef.getDescription());
        return values;
    }

    private static Map<String, Object> routeDecisionSummary(RoutingDecisionSpec routingDecision) {
        Map<String, Object> values = new TreeMap<>();
        values.put(ImmutableRoutingDecisionSpec.Fields.policyCode, routingDecision.getPolicyCode());
        values.put(ImmutableRoutingDecisionSpec.Fields.matchedRules, routingDecision.getMatchedRules());
        values.put(ImmutableRoutingDecisionSpec.Fields.selectedProcessor, routingDecision.getSelectedProcessor());
        values.put(ImmutableRoutingDecisionSpec.Fields.selectedCashFundingAccount,
                routingDecision.getSelectedCashFundingAccount());
        values.put(ImmutableRoutingDecisionSpec.Fields.selectedPlatformAccount,
                routingDecision.getSelectedPlatformAccount());
        values.put(ImmutableRoutingDecisionSpec.Fields.decisionReason, routingDecision.getDecisionReason());
        values.put(ImmutableRoutingDecisionSpec.Fields.contextVariables,
                sortedMap(routingDecision.getContextVariables()));
        return values;
    }

    private static Map<String, Object> accountHierarchySnapshotSummary(AccountHierarchySnapshotSpec snapshot) {
        Map<String, Object> values = new TreeMap<>();
        if (snapshot == null) {
            return values;
        }
        values.put(ImmutableAccountHierarchySnapshotSpec.Fields.relationSn, snapshot.getRelationSn());
        values.put(ImmutableAccountHierarchySnapshotSpec.Fields.parentAccountRef,
                subjectSummary(snapshot.getParentAccountRef()));
        return values;
    }

    private static Map<String, Object> platformAccountsSummary(PlatformAccountsSnapshotSpec platformAccounts) {
        Map<String, Object> values = new TreeMap<>();
        if (platformAccounts == null) {
            return values;
        }
        values.put(ImmutablePlatformAccountsSnapshotSpec.Fields.cashFundingAccount,
                subjectSummary(platformAccounts.getCashFundingAccount()));
        values.put(ImmutablePlatformAccountsSnapshotSpec.Fields.prepaymentFundingAccount,
                subjectSummary(platformAccounts.getPrepaymentFundingAccount()));
        values.put(ImmutablePlatformAccountsSnapshotSpec.Fields.clearingFundingAccount,
                subjectSummary(platformAccounts.getClearingFundingAccount()));
        values.put(ImmutablePlatformAccountsSnapshotSpec.Fields.settlementFundingAccount,
                subjectSummary(platformAccounts.getSettlementFundingAccount()));
        values.put(ImmutablePlatformAccountsSnapshotSpec.Fields.feeFundingAccount,
                subjectSummary(platformAccounts.getFeeFundingAccount()));
        values.put(ImmutablePlatformAccountsSnapshotSpec.Fields.adjustmentFundingAccount,
                subjectSummary(platformAccounts.getAdjustmentFundingAccount()));
        return values;
    }

    private static Map<String, Object> routeNodeSummary(RouteNodeSpec node) {
        Map<String, Object> values = new TreeMap<>();
        if (node == null) {
            return values;
        }
        values.put(ImmutableRouteNodeSpec.Fields.nodeType, node.getNodeType().name());
        values.put(ImmutableRouteNodeSpec.Fields.subjectRef, subjectSummary(node.getSubjectRef()));
        values.put(ImmutableRouteNodeSpec.Fields.nodeRole, node.getNodeRole().name());
        return values;
    }

    private static Map<String, Object> subjectSummary(SubjectRef subjectRef) {
        Map<String, Object> values = new TreeMap<>();
        if (subjectRef == null) {
            return values;
        }
        values.put(ImmutableSubjectRef.Fields.subjectId, subjectRef.getSubjectId());
        values.put(ImmutableSubjectRef.Fields.subjectType, subjectRef.getSubjectType().name());
        values.put(ImmutableSubjectRef.Fields.tenantId, subjectRef.getTenantId());
        values.put(ImmutableSubjectRef.Fields.subjectName, subjectRef.getSubjectName());
        values.put(ImmutableSubjectRef.Fields.currency,
                subjectRef.getCurrency() == null ? null : subjectRef.getCurrency().name());
        values.put(ImmutableSubjectRef.Fields.ledgerProfileCode, subjectRef.getLedgerProfileCode());
        return values;
    }

    private static Map<String, Object> moneySummary(Money money) {
        Map<String, Object> values = new TreeMap<>();
        if (money == null) {
            return values;
        }
        values.put(MONEY_AMOUNT, money.getAmount());
        values.put(MONEY_CURRENCY, money.getCurrency().name());
        return values;
    }

    private static Map<String, Object> sortedMap(Map<String, Object> values) {
        return new TreeMap<>(values);
    }

    private static List<RouteParticipantSpec> parseParticipants(ArrayNode participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        List<RouteParticipantSpec> result = new ArrayList<>(participants.size());
        for (JsonNode item : participants) {
            ObjectNode value = item.asObject();
            result.add(ImmutableRouteParticipantSpec.builder()
                    .participantRole(RouteParticipantRole.valueOf(
                            textValue(value, ImmutableRouteParticipantSpec.Fields.participantRole)))
                    .subjectRef(parseSubjectRef(objectValue(value,
                            ImmutableRouteParticipantSpec.Fields.subjectRef)))
                    .ledgerProfileCode(textValue(value, ImmutableRouteParticipantSpec.Fields.ledgerProfileCode))
                    .currency(currencyValue(value, ImmutableRouteParticipantSpec.Fields.currency))
                    .amount(parseMoney(objectValue(value, ImmutableRouteParticipantSpec.Fields.amount)))
                    .description(textValue(value, ImmutableRouteParticipantSpec.Fields.description))
                    .accountHierarchySnapshot(parseAccountHierarchySnapshot(objectValue(value,
                            ImmutableRouteParticipantSpec.Fields.accountHierarchySnapshot)))
                    .contextVariables(parseObjectMap(objectValue(value,
                            ImmutableRouteParticipantSpec.Fields.contextVariables)))
                    .build());
        }
        return result;
    }

    private static List<RouteLegSpec> parseLegs(ArrayNode legs) {
        if (legs == null || legs.isEmpty()) {
            return List.of();
        }
        List<RouteLegSpec> result = new ArrayList<>(legs.size());
        for (JsonNode item : legs) {
            ObjectNode value = item.asObject();
            result.add(ImmutableRouteLegSpec.builder()
                    .legId(textValue(value, ImmutableRouteLegSpec.Fields.legId))
                    .sequence(intValue(value, ImmutableRouteLegSpec.Fields.sequence))
                    .legType(RouteLegType.valueOf(textValue(value, ImmutableRouteLegSpec.Fields.legType)))
                    .sourceNode(parseRouteNode(objectValue(value, ImmutableRouteLegSpec.Fields.sourceNode)))
                    .targetNode(parseRouteNode(objectValue(value, ImmutableRouteLegSpec.Fields.targetNode)))
                    .amount(parseMoney(objectValue(value, ImmutableRouteLegSpec.Fields.amount)))
                    .originalAmount(parseMoney(objectValue(value, ImmutableRouteLegSpec.Fields.originalAmount)))
                    .exchangeRate(decimalValue(value, ImmutableRouteLegSpec.Fields.exchangeRate))
                    .replayPolicy(RouteReplayPolicy.valueOf(textValue(value,
                            ImmutableRouteLegSpec.Fields.replayPolicy)))
                    .replayRefLegId(textValue(value, ImmutableRouteLegSpec.Fields.replayRefLegId))
                    .description(textValue(value, ImmutableRouteLegSpec.Fields.description))
                    .contextVariables(parseObjectMap(objectValue(value,
                            ImmutableRouteLegSpec.Fields.contextVariables)))
                    .build());
        }
        return result;
    }

    private static RouteNodeSpec parseRouteNode(ObjectNode value) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(value.has(ImmutableRouteNodeSpec.Fields.nodeType)
                        ? RouteNodeType.valueOf(textValue(value, ImmutableRouteNodeSpec.Fields.nodeType))
                        : RouteNodeType.SUBJECT)
                .subjectRef(parseSubjectRef(objectValue(value, ImmutableRouteNodeSpec.Fields.subjectRef)))
                .nodeRole(RouteNodeRole.valueOf(textValue(value, ImmutableRouteNodeSpec.Fields.nodeRole)))
                .build();
    }

    private static @Nullable SubjectRef parseSubjectRef(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableSubjectRef.builder()
                .tenantId(longValue(value, ImmutableSubjectRef.Fields.tenantId))
                .subjectId(textValue(value, ImmutableSubjectRef.Fields.subjectId))
                .subjectType(FundsSubjectType.valueOf(textValue(value, ImmutableSubjectRef.Fields.subjectType)))
                .subjectName(textValue(value, ImmutableSubjectRef.Fields.subjectName))
                .currency(currencyValue(value, ImmutableSubjectRef.Fields.currency))
                .ledgerProfileCode(textValue(value, ImmutableSubjectRef.Fields.ledgerProfileCode))
                .build();
    }

    private static @Nullable Money parseMoney(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Money.immutable(longValue(value, MONEY_AMOUNT),
                CurrencyIsoCode.valueOf(textValue(value, MONEY_CURRENCY)));
    }

    private static PaymentInstrumentRefSpec parsePaymentInstrumentRef(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutablePaymentInstrumentRefSpec.builder()
                .instrumentId(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.instrumentId))
                .instrumentType(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.instrumentType))
                .instrumentNo(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.instrumentNo))
                .ownerId(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.ownerId))
                .ownerType(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.ownerType))
                .tenantId(longValue(value, ImmutablePaymentInstrumentRefSpec.Fields.tenantId))
                .currency(currencyValue(value, ImmutablePaymentInstrumentRefSpec.Fields.currency))
                .status(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.status))
                .bindingSnapshot(parseObjectMap(objectValue(value,
                        ImmutablePaymentInstrumentRefSpec.Fields.bindingSnapshot)))
                .description(textValue(value, ImmutablePaymentInstrumentRefSpec.Fields.description))
                .build();
    }

    private static ExternalAccountRefSpec parseExternalAccountRef(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(textValue(value, ImmutableExternalAccountRefSpec.Fields.externalAccountId))
                .externalAccountType(textValue(value, ImmutableExternalAccountRefSpec.Fields.externalAccountType))
                .externalAccountNo(textValue(value, ImmutableExternalAccountRefSpec.Fields.externalAccountNo))
                .providerCode(textValue(value, ImmutableExternalAccountRefSpec.Fields.providerCode))
                .channelCode(textValue(value, ImmutableExternalAccountRefSpec.Fields.channelCode))
                .currency(currencyValue(value, ImmutableExternalAccountRefSpec.Fields.currency))
                .countryCode(textValue(value, ImmutableExternalAccountRefSpec.Fields.countryCode))
                .description(textValue(value, ImmutableExternalAccountRefSpec.Fields.description))
                .contextVariables(parseObjectMap(objectValue(value,
                        ImmutableExternalAccountRefSpec.Fields.contextVariables)))
                .build();
    }

    private static RoutingDecisionSpec parseRoutingDecision(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode(textValue(value, ImmutableRoutingDecisionSpec.Fields.policyCode))
                .matchedRules(parseStringList(arrayValue(value, ImmutableRoutingDecisionSpec.Fields.matchedRules)))
                .selectedProcessor(textValue(value, ImmutableRoutingDecisionSpec.Fields.selectedProcessor))
                .selectedCashFundingAccount(textValue(value,
                        ImmutableRoutingDecisionSpec.Fields.selectedCashFundingAccount))
                .selectedPlatformAccount(textValue(value,
                        ImmutableRoutingDecisionSpec.Fields.selectedPlatformAccount))
                .decisionReason(textValue(value, ImmutableRoutingDecisionSpec.Fields.decisionReason))
                .contextVariables(parseObjectMap(objectValue(value,
                        ImmutableRoutingDecisionSpec.Fields.contextVariables)))
                .build();
    }

    private static @Nullable AccountHierarchySnapshotSpec parseAccountHierarchySnapshot(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn(textValue(value, ImmutableAccountHierarchySnapshotSpec.Fields.relationSn))
                .parentAccountRef(parseSubjectRef(objectValue(value,
                        ImmutableAccountHierarchySnapshotSpec.Fields.parentAccountRef)))
                .build();
    }

    private static List<String> parseStringList(ArrayNode values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (JsonNode value : values) {
            result.add(value.asString());
        }
        return List.copyOf(result);
    }

    private static PlatformAccountsSnapshotSpec parsePlatformAccounts(ObjectNode value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutablePlatformAccountsSnapshotSpec.builder()
                .cashFundingAccount(parseSubjectRef(objectValue(value,
                        ImmutablePlatformAccountsSnapshotSpec.Fields.cashFundingAccount)))
                .prepaymentFundingAccount(parseSubjectRef(objectValue(value,
                        ImmutablePlatformAccountsSnapshotSpec.Fields.prepaymentFundingAccount)))
                .clearingFundingAccount(parseSubjectRef(objectValue(value,
                        ImmutablePlatformAccountsSnapshotSpec.Fields.clearingFundingAccount)))
                .settlementFundingAccount(parseSubjectRef(objectValue(value,
                        ImmutablePlatformAccountsSnapshotSpec.Fields.settlementFundingAccount)))
                .feeFundingAccount(parseSubjectRef(objectValue(value,
                        ImmutablePlatformAccountsSnapshotSpec.Fields.feeFundingAccount)))
                .adjustmentFundingAccount(parseSubjectRef(objectValue(value,
                        ImmutablePlatformAccountsSnapshotSpec.Fields.adjustmentFundingAccount)))
                .build();
    }

    private static LocalDateTime parseLocalDateTime(String value, LocalDateTime defaultValue) {
        return StringUtils.hasText(value) ? LocalDateTime.parse(value) : defaultValue;
    }

    private static Map<String, Object> parseObjectMap(ObjectNode value) {
        return value == null || value.isEmpty()
                ? Map.of()
                : Map.copyOf(WindJson.convertValue(value, new TypeReference<Map<String, Object>>() {
                }));
    }

    private static @Nullable ObjectNode objectValue(ObjectNode values, String field) {
        JsonNode value = values.get(field);
        return value instanceof ObjectNode objectNode ? objectNode : null;
    }

    private static @Nullable ArrayNode arrayValue(ObjectNode values, String field) {
        JsonNode value = values.get(field);
        return value instanceof ArrayNode arrayNode ? arrayNode : null;
    }

    private static @Nullable String textValue(ObjectNode values, String field) {
        JsonNode value = values.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static @Nullable CurrencyIsoCode currencyValue(ObjectNode values, String field) {
        String value = textValue(values, field);
        return value == null ? null : CurrencyIsoCode.valueOf(value);
    }

    private static @Nullable Long longValue(ObjectNode values, String field) {
        JsonNode value = values.get(field);
        return value == null || value.isNull() ? null : value.longValue();
    }

    private static int intValue(ObjectNode values, String field) {
        JsonNode value = values.get(field);
        return value == null || value.isNull() ? 0 : value.intValue();
    }

    private static java.math.BigDecimal decimalValue(ObjectNode values, String field) {
        JsonNode value = values.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

}

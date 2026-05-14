package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.integration.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.integration.funds.model.route.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class RouteSnapshotJsonSupport {

    private RouteSnapshotJsonSupport() {
    }

    static String toRouteSnapshotJson(RouteSnapshotSpec routeSnapshot) {
        return JSON.toJSONString(routeSummary(routeSnapshot));
    }

    static RouteSnapshotSpec parseRouteSnapshot(String routeSnapshotJson, LocalDateTime defaultResolvedAt) {
        JSONObject values = JSON.parseObject(routeSnapshotJson);
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(values.getLong("tenantId"))
                .snapshotId(values.getString("snapshotId"))
                .snapshotSchemaVersion(values.getString("snapshotSchemaVersion"))
                .routeCode(values.getString("routeCode"))
                .routeVersion(values.getString("routeVersion"))
                .businessScene(values.getString("businessScene"))
                .businessSn(values.getString("businessSn"))
                .instructionType(FundsInstructionType.valueOf(values.getString("instructionType")))
                .eventType(FundsTransactionEventType.valueOf(values.getString("eventType")))
                .transactionType(DefaultFundsTransactionType.valueOf(values.getString("transactionType")))
                .participants(parseParticipants(values.getJSONArray("participants")))
                .legs(parseLegs(values.getJSONArray("legs")))
                .routingDecision(parseRoutingDecision(values.getJSONObject("routingDecision")))
                .paymentInstrumentRef(parsePaymentInstrumentRef(values.getJSONObject("paymentInstrumentRef")))
                .externalAccountRef(parseExternalAccountRef(values.getJSONObject("externalAccountRef")))
                .platformAccounts(parsePlatformAccounts(values.getJSONObject("platformAccounts")))
                .resolvedAt(parseLocalDateTime(values.getString("resolvedAt"), defaultResolvedAt))
                .expiresAt(parseLocalDateTime(values.getString("expiresAt"), null))
                .description(values.getString("description"))
                .contextVariables(parseObjectMap(values.getJSONObject("contextVariables")))
                .build();
    }

    static Map<String, Object> routeSummary(RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", routeSnapshot.getTenantId());
        values.put("snapshotId", routeSnapshot.getSnapshotId());
        values.put("snapshotSchemaVersion", routeSnapshot.getSnapshotSchemaVersion());
        values.put("businessScene", routeSnapshot.getBusinessScene());
        values.put("businessSn", routeSnapshot.getBusinessSn());
        values.put("instructionType", routeSnapshot.getInstructionType().name());
        values.put("eventType", routeSnapshot.getEventType().name());
        values.put("transactionType", routeSnapshot.getTransactionType().name());
        values.put("routeCode", routeSnapshot.getRouteCode());
        values.put("routeVersion", routeSnapshot.getRouteVersion());
        values.put("resolvedAt", routeSnapshot.getResolvedAt().toString());
        values.put("expiresAt", routeSnapshot.getExpiresAt() == null ? null : routeSnapshot.getExpiresAt().toString());
        values.put("description", routeSnapshot.getDescription());
        values.put("routingDecision", routeSnapshot.getRoutingDecision() == null
                ? Map.of()
                : sortedMap(routeDecisionSummary(routeSnapshot.getRoutingDecision())));
        values.put("paymentInstrumentRef", instrumentSummary(routeSnapshot.getPaymentInstrumentRef()));
        values.put("externalAccountRef", externalAccountSummary(routeSnapshot.getExternalAccountRef()));
        values.put("participants", routeSnapshot.getParticipants()
                .stream()
                .map(RouteSnapshotJsonSupport::participantSummary)
                .toList());
        values.put("legs", routeSnapshot.getLegs()
                .stream()
                .map(RouteSnapshotJsonSupport::legSummary)
                .toList());
        values.put("platformAccounts", platformAccountsSummary(routeSnapshot.getPlatformAccounts()));
        values.put("contextVariables", sortedMap(routeSnapshot.getContextVariables()));
        return values;
    }

    private static Map<String, Object> legSummary(RouteLegSpec leg) {
        Map<String, Object> values = new TreeMap<>();
        values.put("legId", leg.getLegId());
        values.put("legType", leg.getLegType().name());
        values.put("sourceNode", routeNodeSummary(leg.getSourceNode()));
        values.put("targetNode", routeNodeSummary(leg.getTargetNode()));
        values.put("amount", moneySummary(leg.getAmount()));
        values.put("originalAmount", moneySummary(leg.getOriginalAmount()));
        values.put("exchangeRate", leg.getExchangeRate());
        values.put("balanceEffectType", leg.getBalanceEffectType().name());
        values.put("phaseCode", leg.getPhaseCode().name());
        values.put("periodType", leg.getPeriodType().name());
        values.put("periodId", leg.getPeriodId());
        values.put("constraintOverrides", sortedEnumMap(leg.getConstraintOverrides()));
        values.put("replayPolicy", leg.getReplayPolicy().name());
        values.put("replayRefLegId", leg.getReplayRefLegId());
        values.put("description", leg.getDescription());
        values.put("contextVariables", sortedMap(leg.getContextVariables()));
        return values;
    }

    private static Map<String, Object> participantSummary(RouteParticipantSpec participant) {
        Map<String, Object> values = new TreeMap<>();
        values.put("participantRole", participant.getParticipantRole().name());
        values.put("subjectRef", subjectSummary(participant.getSubjectRef()));
        values.put("ledgerProfileCode", participant.getLedgerProfileCode());
        values.put("currency", participant.getCurrency());
        values.put("amount", moneySummary(participant.getAmount()));
        values.put("description", participant.getDescription());
        values.put("contextVariables", sortedMap(participant.getContextVariables()));
        return values;
    }

    private static Map<String, Object> externalAccountSummary(ExternalAccountRefSpec externalAccountRef) {
        Map<String, Object> values = new TreeMap<>();
        if (externalAccountRef == null) {
            return values;
        }
        values.put("externalAccountId", externalAccountRef.getExternalAccountId());
        values.put("externalAccountType", externalAccountRef.getExternalAccountType());
        values.put("externalAccountNo", externalAccountRef.getExternalAccountNo());
        values.put("providerCode", externalAccountRef.getProviderCode());
        values.put("channelCode", externalAccountRef.getChannelCode());
        values.put("currency", externalAccountRef.getCurrency());
        values.put("countryCode", externalAccountRef.getCountryCode());
        values.put("description", externalAccountRef.getDescription());
        values.put("contextVariables", sortedMap(externalAccountRef.getContextVariables()));
        return values;
    }

    private static Map<String, Object> instrumentSummary(PaymentInstrumentRefSpec instrumentRef) {
        Map<String, Object> values = new TreeMap<>();
        if (instrumentRef == null) {
            return values;
        }
        values.put("instrumentId", instrumentRef.getInstrumentId());
        values.put("instrumentType", instrumentRef.getInstrumentType());
        values.put("instrumentNo", instrumentRef.getInstrumentNo());
        values.put("ownerId", instrumentRef.getOwnerId());
        values.put("ownerType", instrumentRef.getOwnerType());
        values.put("tenantId", instrumentRef.getTenantId());
        values.put("currency", instrumentRef.getCurrency());
        values.put("status", instrumentRef.getStatus());
        values.put("bindingSnapshot", sortedMap(instrumentRef.getBindingSnapshot()));
        values.put("description", instrumentRef.getDescription());
        return values;
    }

    private static Map<String, Object> routeDecisionSummary(RoutingDecisionSpec routingDecision) {
        Map<String, Object> values = new TreeMap<>();
        values.put("policyCode", routingDecision.getPolicyCode());
        values.put("matchedRules", routingDecision.getMatchedRules());
        values.put("selectedProcessor", routingDecision.getSelectedProcessor());
        values.put("selectedCashFundingAccount", routingDecision.getSelectedCashFundingAccount());
        values.put("selectedPlatformAccount", routingDecision.getSelectedPlatformAccount());
        values.put("fundingAllocations", routingDecision.getFundingAllocations()
                .stream()
                .map(RouteSnapshotJsonSupport::fundingAllocationSummary)
                .toList());
        values.put("decisionReason", routingDecision.getDecisionReason());
        values.put("contextVariables", sortedMap(routingDecision.getContextVariables()));
        return values;
    }

    private static Map<String, Object> fundingAllocationSummary(FundingAllocationDecisionSpec fundingAllocation) {
        Map<String, Object> values = new TreeMap<>();
        values.put("allocationId", fundingAllocation.getAllocationId());
        values.put("subjectRef", subjectSummary(fundingAllocation.getSubjectRef()));
        values.put("ledgerSubjectCode", enumName(fundingAllocation.getLedgerSubjectCode()));
        values.put("amount", moneySummary(fundingAllocation.getAmount()));
        values.put("priority", fundingAllocation.getPriority());
        values.put("reason", fundingAllocation.getReason());
        return values;
    }

    private static Map<String, Object> platformAccountsSummary(PlatformAccountsSnapshotSpec platformAccounts) {
        Map<String, Object> values = new TreeMap<>();
        if (platformAccounts == null) {
            return values;
        }
        values.put("cashFundingAccount", subjectSummary(platformAccounts.getCashFundingAccount()));
        values.put("prepaymentFundingAccount", subjectSummary(platformAccounts.getPrepaymentFundingAccount()));
        values.put("clearingFundingAccount", subjectSummary(platformAccounts.getClearingFundingAccount()));
        values.put("settlementFundingAccount", subjectSummary(platformAccounts.getSettlementFundingAccount()));
        values.put("feeFundingAccount", subjectSummary(platformAccounts.getFeeFundingAccount()));
        values.put("adjustmentFundingAccount", subjectSummary(platformAccounts.getAdjustmentFundingAccount()));
        return values;
    }

    private static Map<String, Object> routeNodeSummary(RouteNodeSpec node) {
        Map<String, Object> values = new TreeMap<>();
        if (node == null) {
            return values;
        }
        values.put("nodeType", node.getNodeType().name());
        values.put("subjectRef", subjectSummary(node.getSubjectRef()));
        values.put("ledgerSubjectCode", enumName(node.getLedgerSubjectCode()));
        values.put("nodeRole", node.getNodeRole().name());
        return values;
    }

    private static Map<String, Object> subjectSummary(SubjectRef subjectRef) {
        Map<String, Object> values = new TreeMap<>();
        if (subjectRef == null) {
            return values;
        }
        values.put("subjectId", subjectRef.getSubjectId());
        values.put("subjectType", subjectRef.getSubjectType().name());
        values.put("tenantId", subjectRef.getTenantId());
        values.put("subjectName", subjectRef.getSubjectName());
        values.put("currency", subjectRef.getCurrency());
        values.put("ledgerProfileCode", subjectRef.getLedgerProfileCode());
        values.put("description", subjectRef.getDescription());
        return values;
    }

    private static Map<String, Object> moneySummary(Money money) {
        Map<String, Object> values = new TreeMap<>();
        if (money == null) {
            return values;
        }
        values.put("amount", money.getAmount());
        values.put("currency", money.getCurrency().name());
        return values;
    }

    private static Map<String, Object> sortedEnumMap(Map<String, ? extends Enum<?>> values) {
        Map<String, Object> result = new TreeMap<>();
        values.forEach((key, value) -> result.put(key, enumName(value)));
        return result;
    }

    private static Map<String, Object> sortedMap(Map<String, Object> values) {
        return new TreeMap<>(values);
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static List<RouteParticipantSpec> parseParticipants(JSONArray participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        List<RouteParticipantSpec> result = new ArrayList<>(participants.size());
        for (Object item : participants) {
            JSONObject value = (JSONObject) item;
            result.add(ImmutableRouteParticipantSpec.builder()
                    .participantRole(RouteParticipantRole.valueOf(value.getString("participantRole")))
                    .subjectRef(parseSubjectRef(value.getJSONObject("subjectRef")))
                    .ledgerProfileCode(value.getString("ledgerProfileCode"))
                    .currency(value.getString("currency"))
                    .amount(parseMoney(value.getJSONObject("amount")))
                    .description(value.getString("description"))
                    .contextVariables(parseObjectMap(value.getJSONObject("contextVariables")))
                    .build());
        }
        return result;
    }

    private static List<RouteLegSpec> parseLegs(JSONArray legs) {
        if (legs == null || legs.isEmpty()) {
            return List.of();
        }
        List<RouteLegSpec> result = new ArrayList<>(legs.size());
        for (Object item : legs) {
            JSONObject value = (JSONObject) item;
            result.add(ImmutableRouteLegSpec.builder()
                    .legId(value.getString("legId"))
                    .sequence(value.getIntValue("sequence"))
                    .legType(RouteLegType.valueOf(value.getString("legType")))
                    .sourceNode(parseRouteNode(value.getJSONObject("sourceNode")))
                    .targetNode(parseRouteNode(value.getJSONObject("targetNode")))
                    .amount(parseMoney(value.getJSONObject("amount")))
                    .originalAmount(parseMoney(value.getJSONObject("originalAmount")))
                    .exchangeRate(value.getBigDecimal("exchangeRate"))
                    .balanceEffectType(LedgerBalanceEffectType.valueOf(value.getString("balanceEffectType")))
                    .phaseCode(LedgerPhaseCode.valueOf(value.getString("phaseCode")))
                    .periodType(AccountBalancePeriodType.valueOf(value.getString("periodType")))
                    .periodId(value.getString("periodId"))
                    .constraintOverrides(parseConstraintOverrides(value.getJSONObject("constraintOverrides")))
                    .replayPolicy(RouteReplayPolicy.valueOf(value.getString("replayPolicy")))
                    .replayRefLegId(value.getString("replayRefLegId"))
                    .description(value.getString("description"))
                    .contextVariables(parseObjectMap(value.getJSONObject("contextVariables")))
                    .build());
        }
        return result;
    }

    private static RouteNodeSpec parseRouteNode(JSONObject value) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(value.containsKey("nodeType")
                        ? RouteNodeType.valueOf(value.getString("nodeType")) : RouteNodeType.SUBJECT)
                .subjectRef(parseSubjectRef(value.getJSONObject("subjectRef")))
                .ledgerSubjectCode(LedgerSubjectCode.valueOf(value.getString("ledgerSubjectCode")))
                .nodeRole(RouteNodeRole.valueOf(value.getString("nodeRole")))
                .build();
    }

    private static @Nullable SubjectRef parseSubjectRef(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableSubjectRef.builder()
                .tenantId(value.getLong("tenantId"))
                .subjectId(value.getString("subjectId"))
                .subjectType(FundsSubjectType.valueOf(value.getString("subjectType")))
                .subjectName(value.getString("subjectName"))
                .currency(value.getString("currency"))
                .ledgerProfileCode(value.getString("ledgerProfileCode"))
                .description(value.getString("description"))
                .build();
    }

    private static @Nullable Money parseMoney(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Money.immutable(value.getLongValue("amount"), CurrencyIsoCode.valueOf(value.getString("currency")));
    }

    private static Map<String, LedgerBalanceConstraintType> parseConstraintOverrides(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        Map<String, LedgerBalanceConstraintType> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            result.put(entry.getKey(), LedgerBalanceConstraintType.valueOf(String.valueOf(entry.getValue())));
        }
        return Map.copyOf(result);
    }

    private static PaymentInstrumentRefSpec parsePaymentInstrumentRef(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutablePaymentInstrumentRefSpec.builder()
                .instrumentId(value.getString("instrumentId"))
                .instrumentType(value.getString("instrumentType"))
                .instrumentNo(value.getString("instrumentNo"))
                .ownerId(value.getString("ownerId"))
                .ownerType(value.getString("ownerType"))
                .tenantId(value.getLong("tenantId"))
                .currency(value.getString("currency"))
                .status(value.getString("status"))
                .bindingSnapshot(parseObjectMap(value.getJSONObject("bindingSnapshot")))
                .description(value.getString("description"))
                .build();
    }

    private static ExternalAccountRefSpec parseExternalAccountRef(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(value.getString("externalAccountId"))
                .externalAccountType(value.getString("externalAccountType"))
                .externalAccountNo(value.getString("externalAccountNo"))
                .providerCode(value.getString("providerCode"))
                .channelCode(value.getString("channelCode"))
                .currency(value.getString("currency"))
                .countryCode(value.getString("countryCode"))
                .description(value.getString("description"))
                .contextVariables(parseObjectMap(value.getJSONObject("contextVariables")))
                .build();
    }

    private static RoutingDecisionSpec parseRoutingDecision(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode(value.getString("policyCode"))
                .matchedRules(parseStringList(value.getJSONArray("matchedRules")))
                .selectedProcessor(value.getString("selectedProcessor"))
                .selectedCashFundingAccount(value.getString("selectedCashFundingAccount"))
                .selectedPlatformAccount(value.getString("selectedPlatformAccount"))
                .fundingAllocations(parseFundingAllocations(value.getJSONArray("fundingAllocations")))
                .decisionReason(value.getString("decisionReason"))
                .contextVariables(parseObjectMap(value.getJSONObject("contextVariables")))
                .build();
    }

    private static List<FundingAllocationDecisionSpec> parseFundingAllocations(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<FundingAllocationDecisionSpec> result = new ArrayList<>(values.size());
        for (Object item : values) {
            JSONObject value = (JSONObject) item;
            result.add(ImmutableFundingAllocationDecisionSpec.builder()
                    .allocationId(value.getString("allocationId"))
                    .subjectRef(parseSubjectRef(value.getJSONObject("subjectRef")))
                    .ledgerSubjectCode(parseLedgerSubjectCode(value.getString("ledgerSubjectCode")))
                    .amount(parseMoney(value.getJSONObject("amount")))
                    .priority(value.getInteger("priority"))
                    .reason(value.getString("reason"))
                    .build());
        }
        return List.copyOf(result);
    }

    private static List<String> parseStringList(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(String.valueOf(value));
        }
        return List.copyOf(result);
    }

    private static LedgerSubjectCode parseLedgerSubjectCode(String value) {
        return hasText(value) ? LedgerSubjectCode.valueOf(value) : null;
    }

    private static PlatformAccountsSnapshotSpec parsePlatformAccounts(JSONObject value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return ImmutablePlatformAccountsSnapshotSpec.builder()
                .cashFundingAccount(parseSubjectRef(value.getJSONObject("cashFundingAccount")))
                .prepaymentFundingAccount(parseSubjectRef(value.getJSONObject("prepaymentFundingAccount")))
                .clearingFundingAccount(parseSubjectRef(value.getJSONObject("clearingFundingAccount")))
                .settlementFundingAccount(parseSubjectRef(value.getJSONObject("settlementFundingAccount")))
                .feeFundingAccount(parseSubjectRef(value.getJSONObject("feeFundingAccount")))
                .adjustmentFundingAccount(parseSubjectRef(value.getJSONObject("adjustmentFundingAccount")))
                .build();
    }

    private static LocalDateTime parseLocalDateTime(String value, LocalDateTime defaultValue) {
        return hasText(value) ? LocalDateTime.parse(value) : defaultValue;
    }

    private static Map<String, Object> parseObjectMap(JSONObject value) {
        return value == null || value.isEmpty() ? Map.of() : Map.copyOf(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

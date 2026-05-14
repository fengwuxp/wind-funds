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

final class RouteSnapshotJsonSupport {

    private RouteSnapshotJsonSupport() {
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
                .selectedReserveFund(value.getString("selectedReserveFund"))
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
                .reserveFundingAccount(parseSubjectRef(value.getJSONObject("reserveFundingAccount")))
                .prepaymentFundingAccount(parseSubjectRef(value.getJSONObject("prepaymentFundingAccount")))
                .clearingFundingAccount(parseSubjectRef(value.getJSONObject("clearingFundingAccount")))
                .settlementFundingAccount(parseSubjectRef(value.getJSONObject("settlementFundingAccount")))
                .feeFundingAccount(parseSubjectRef(value.getJSONObject("feeFundingAccount")))
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

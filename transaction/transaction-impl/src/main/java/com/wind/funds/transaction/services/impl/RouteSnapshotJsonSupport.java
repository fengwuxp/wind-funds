package com.wind.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.model.route.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.funds.model.route.ImmutableRouteLegSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class RouteSnapshotJsonSupport {

    private static final String MONEY_AMOUNT = "amount";

    private static final String MONEY_CURRENCY = "currency";

    private RouteSnapshotJsonSupport() {
    }

    static String toRouteSnapshotJson(RouteSnapshotSpec routeSnapshot) {
        return JSON.toJSONString(routeSummary(routeSnapshot));
    }

    static RouteSnapshotSpec parseRouteSnapshot(String routeSnapshotJson, LocalDateTime defaultResolvedAt) {
        JSONObject values = JSON.parseObject(routeSnapshotJson);
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(values.getLong(ImmutableRouteSnapshotSpec.Fields.tenantId))
                .snapshotId(values.getString(ImmutableRouteSnapshotSpec.Fields.snapshotId))
                .snapshotSchemaVersion(values.getString(ImmutableRouteSnapshotSpec.Fields.snapshotSchemaVersion))
                .routeCode(values.getString(ImmutableRouteSnapshotSpec.Fields.routeCode))
                .routeVersion(values.getString(ImmutableRouteSnapshotSpec.Fields.routeVersion))
                .businessScene(values.getString(ImmutableRouteSnapshotSpec.Fields.businessScene))
                .businessSn(values.getString(ImmutableRouteSnapshotSpec.Fields.businessSn))
                .instructionType(FundsInstructionType.valueOf(
                        values.getString(ImmutableRouteSnapshotSpec.Fields.instructionType)))
                .eventType(FundsTransactionEventType.valueOf(
                        values.getString(ImmutableRouteSnapshotSpec.Fields.eventType)))
                .transactionType(DefaultFundsTransactionType.valueOf(
                        values.getString(ImmutableRouteSnapshotSpec.Fields.transactionType)))
                .participants(parseParticipants(values.getJSONArray(ImmutableRouteSnapshotSpec.Fields.participants)))
                .legs(parseLegs(values.getJSONArray(ImmutableRouteSnapshotSpec.Fields.legs)))
                .routingDecision(parseRoutingDecision(values.getJSONObject(
                        ImmutableRouteSnapshotSpec.Fields.routingDecision)))
                .paymentInstrumentRef(parsePaymentInstrumentRef(values.getJSONObject(
                        ImmutableRouteSnapshotSpec.Fields.paymentInstrumentRef)))
                .externalAccountRef(parseExternalAccountRef(values.getJSONObject(
                        ImmutableRouteSnapshotSpec.Fields.externalAccountRef)))
                .platformAccounts(parsePlatformAccounts(values.getJSONObject(
                        ImmutableRouteSnapshotSpec.Fields.platformAccounts)))
                .resolvedAt(parseLocalDateTime(values.getString(ImmutableRouteSnapshotSpec.Fields.resolvedAt),
                        defaultResolvedAt))
                .expiresAt(parseLocalDateTime(values.getString(ImmutableRouteSnapshotSpec.Fields.expiresAt), null))
                .description(values.getString(ImmutableRouteSnapshotSpec.Fields.description))
                .contextVariables(parseObjectMap(values.getJSONObject(
                        ImmutableRouteSnapshotSpec.Fields.contextVariables)))
                .build();
    }

    static Map<String, Object> routeSummary(RouteSnapshotSpec routeSnapshot) {
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
        values.put(ImmutableRouteLegSpec.Fields.balanceEffectType, leg.getBalanceEffectType().name());
        values.put(ImmutableRouteLegSpec.Fields.phaseCode, leg.getPhaseCode().name());
        values.put(ImmutableRouteLegSpec.Fields.periodType, leg.getPeriodType().name());
        values.put(ImmutableRouteLegSpec.Fields.periodId, leg.getPeriodId());
        values.put(ImmutableRouteLegSpec.Fields.constraintOverrides, sortedEnumMap(leg.getConstraintOverrides()));
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
        values.put(ImmutableRoutingDecisionSpec.Fields.fundingAllocations, routingDecision.getFundingAllocations()
                .stream()
                .map(RouteSnapshotJsonSupport::fundingAllocationSummary)
                .toList());
        values.put(ImmutableRoutingDecisionSpec.Fields.decisionReason, routingDecision.getDecisionReason());
        values.put(ImmutableRoutingDecisionSpec.Fields.contextVariables,
                sortedMap(routingDecision.getContextVariables()));
        return values;
    }

    private static Map<String, Object> fundingAllocationSummary(FundingAllocationDecisionSpec fundingAllocation) {
        Map<String, Object> values = new TreeMap<>();
        values.put(ImmutableFundingAllocationDecisionSpec.Fields.allocationId,
                fundingAllocation.getAllocationId());
        values.put(ImmutableFundingAllocationDecisionSpec.Fields.subjectRef,
                subjectSummary(fundingAllocation.getSubjectRef()));
        values.put(ImmutableFundingAllocationDecisionSpec.Fields.ledgerSubjectCode,
                enumName(fundingAllocation.getLedgerSubjectCode()));
        values.put(ImmutableFundingAllocationDecisionSpec.Fields.amount,
                moneySummary(fundingAllocation.getAmount()));
        values.put(ImmutableFundingAllocationDecisionSpec.Fields.priority, fundingAllocation.getPriority());
        values.put(ImmutableFundingAllocationDecisionSpec.Fields.reason, fundingAllocation.getReason());
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
        values.put(ImmutableRouteNodeSpec.Fields.ledgerSubjectCode, enumName(node.getLedgerSubjectCode()));
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
        values.put(ImmutableSubjectRef.Fields.currency, subjectRef.getCurrency());
        values.put(ImmutableSubjectRef.Fields.ledgerProfileCode, subjectRef.getLedgerProfileCode());
        values.put(ImmutableSubjectRef.Fields.description, subjectRef.getDescription());
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
        if (CollectionUtils.isEmpty(participants)) {
            return List.of();
        }
        List<RouteParticipantSpec> result = new ArrayList<>(participants.size());
        for (Object item : participants) {
            JSONObject value = (JSONObject) item;
            result.add(ImmutableRouteParticipantSpec.builder()
                    .participantRole(RouteParticipantRole.valueOf(
                            value.getString(ImmutableRouteParticipantSpec.Fields.participantRole)))
                    .subjectRef(parseSubjectRef(value.getJSONObject(
                            ImmutableRouteParticipantSpec.Fields.subjectRef)))
                    .ledgerProfileCode(value.getString(ImmutableRouteParticipantSpec.Fields.ledgerProfileCode))
                    .currency(value.getString(ImmutableRouteParticipantSpec.Fields.currency))
                    .amount(parseMoney(value.getJSONObject(ImmutableRouteParticipantSpec.Fields.amount)))
                    .description(value.getString(ImmutableRouteParticipantSpec.Fields.description))
                    .contextVariables(parseObjectMap(value.getJSONObject(
                            ImmutableRouteParticipantSpec.Fields.contextVariables)))
                    .build());
        }
        return result;
    }

    private static List<RouteLegSpec> parseLegs(JSONArray legs) {
        if (CollectionUtils.isEmpty(legs)) {
            return List.of();
        }
        List<RouteLegSpec> result = new ArrayList<>(legs.size());
        for (Object item : legs) {
            JSONObject value = (JSONObject) item;
            result.add(ImmutableRouteLegSpec.builder()
                    .legId(value.getString(ImmutableRouteLegSpec.Fields.legId))
                    .sequence(value.getIntValue(ImmutableRouteLegSpec.Fields.sequence))
                    .legType(RouteLegType.valueOf(value.getString(ImmutableRouteLegSpec.Fields.legType)))
                    .sourceNode(parseRouteNode(value.getJSONObject(ImmutableRouteLegSpec.Fields.sourceNode)))
                    .targetNode(parseRouteNode(value.getJSONObject(ImmutableRouteLegSpec.Fields.targetNode)))
                    .amount(parseMoney(value.getJSONObject(ImmutableRouteLegSpec.Fields.amount)))
                    .originalAmount(parseMoney(value.getJSONObject(ImmutableRouteLegSpec.Fields.originalAmount)))
                    .exchangeRate(value.getBigDecimal(ImmutableRouteLegSpec.Fields.exchangeRate))
                    .balanceEffectType(LedgerBalanceEffectType.valueOf(
                            value.getString(ImmutableRouteLegSpec.Fields.balanceEffectType)))
                    .phaseCode(LedgerPhaseCode.valueOf(value.getString(ImmutableRouteLegSpec.Fields.phaseCode)))
                    .periodType(AccountBalancePeriodType.valueOf(value.getString(
                            ImmutableRouteLegSpec.Fields.periodType)))
                    .periodId(value.getString(ImmutableRouteLegSpec.Fields.periodId))
                    .constraintOverrides(parseConstraintOverrides(value.getJSONObject(
                            ImmutableRouteLegSpec.Fields.constraintOverrides)))
                    .replayPolicy(RouteReplayPolicy.valueOf(value.getString(
                            ImmutableRouteLegSpec.Fields.replayPolicy)))
                    .replayRefLegId(value.getString(ImmutableRouteLegSpec.Fields.replayRefLegId))
                    .description(value.getString(ImmutableRouteLegSpec.Fields.description))
                    .contextVariables(parseObjectMap(value.getJSONObject(
                            ImmutableRouteLegSpec.Fields.contextVariables)))
                    .build());
        }
        return result;
    }

    private static RouteNodeSpec parseRouteNode(JSONObject value) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(value.containsKey(ImmutableRouteNodeSpec.Fields.nodeType)
                        ? RouteNodeType.valueOf(value.getString(ImmutableRouteNodeSpec.Fields.nodeType))
                        : RouteNodeType.SUBJECT)
                .subjectRef(parseSubjectRef(value.getJSONObject(ImmutableRouteNodeSpec.Fields.subjectRef)))
                .ledgerSubjectCode(LedgerSubjectCode.valueOf(value.getString(
                        ImmutableRouteNodeSpec.Fields.ledgerSubjectCode)))
                .nodeRole(RouteNodeRole.valueOf(value.getString(ImmutableRouteNodeSpec.Fields.nodeRole)))
                .build();
    }

    private static @Nullable SubjectRef parseSubjectRef(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return null;
        }
        return ImmutableSubjectRef.builder()
                .tenantId(value.getLong(ImmutableSubjectRef.Fields.tenantId))
                .subjectId(value.getString(ImmutableSubjectRef.Fields.subjectId))
                .subjectType(FundsSubjectType.valueOf(value.getString(ImmutableSubjectRef.Fields.subjectType)))
                .subjectName(value.getString(ImmutableSubjectRef.Fields.subjectName))
                .currency(value.getString(ImmutableSubjectRef.Fields.currency))
                .ledgerProfileCode(value.getString(ImmutableSubjectRef.Fields.ledgerProfileCode))
                .description(value.getString(ImmutableSubjectRef.Fields.description))
                .build();
    }

    private static @Nullable Money parseMoney(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return null;
        }
        return Money.immutable(value.getLongValue(MONEY_AMOUNT),
                CurrencyIsoCode.valueOf(value.getString(MONEY_CURRENCY)));
    }

    private static Map<String, LedgerBalanceConstraintType> parseConstraintOverrides(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return Map.of();
        }
        Map<String, LedgerBalanceConstraintType> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            result.put(entry.getKey(), LedgerBalanceConstraintType.valueOf(String.valueOf(entry.getValue())));
        }
        return Map.copyOf(result);
    }

    private static PaymentInstrumentRefSpec parsePaymentInstrumentRef(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return null;
        }
        return ImmutablePaymentInstrumentRefSpec.builder()
                .instrumentId(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.instrumentId))
                .instrumentType(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.instrumentType))
                .instrumentNo(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.instrumentNo))
                .ownerId(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.ownerId))
                .ownerType(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.ownerType))
                .tenantId(value.getLong(ImmutablePaymentInstrumentRefSpec.Fields.tenantId))
                .currency(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.currency))
                .status(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.status))
                .bindingSnapshot(parseObjectMap(value.getJSONObject(
                        ImmutablePaymentInstrumentRefSpec.Fields.bindingSnapshot)))
                .description(value.getString(ImmutablePaymentInstrumentRefSpec.Fields.description))
                .build();
    }

    private static ExternalAccountRefSpec parseExternalAccountRef(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return null;
        }
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(value.getString(ImmutableExternalAccountRefSpec.Fields.externalAccountId))
                .externalAccountType(value.getString(ImmutableExternalAccountRefSpec.Fields.externalAccountType))
                .externalAccountNo(value.getString(ImmutableExternalAccountRefSpec.Fields.externalAccountNo))
                .providerCode(value.getString(ImmutableExternalAccountRefSpec.Fields.providerCode))
                .channelCode(value.getString(ImmutableExternalAccountRefSpec.Fields.channelCode))
                .currency(value.getString(ImmutableExternalAccountRefSpec.Fields.currency))
                .countryCode(value.getString(ImmutableExternalAccountRefSpec.Fields.countryCode))
                .description(value.getString(ImmutableExternalAccountRefSpec.Fields.description))
                .contextVariables(parseObjectMap(value.getJSONObject(
                        ImmutableExternalAccountRefSpec.Fields.contextVariables)))
                .build();
    }

    private static RoutingDecisionSpec parseRoutingDecision(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return null;
        }
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode(value.getString(ImmutableRoutingDecisionSpec.Fields.policyCode))
                .matchedRules(parseStringList(value.getJSONArray(ImmutableRoutingDecisionSpec.Fields.matchedRules)))
                .selectedProcessor(value.getString(ImmutableRoutingDecisionSpec.Fields.selectedProcessor))
                .selectedCashFundingAccount(value.getString(
                        ImmutableRoutingDecisionSpec.Fields.selectedCashFundingAccount))
                .selectedPlatformAccount(value.getString(
                        ImmutableRoutingDecisionSpec.Fields.selectedPlatformAccount))
                .fundingAllocations(parseFundingAllocations(value.getJSONArray(
                        ImmutableRoutingDecisionSpec.Fields.fundingAllocations)))
                .decisionReason(value.getString(ImmutableRoutingDecisionSpec.Fields.decisionReason))
                .contextVariables(parseObjectMap(value.getJSONObject(
                        ImmutableRoutingDecisionSpec.Fields.contextVariables)))
                .build();
    }

    private static List<FundingAllocationDecisionSpec> parseFundingAllocations(JSONArray values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }
        List<FundingAllocationDecisionSpec> result = new ArrayList<>(values.size());
        for (Object item : values) {
            JSONObject value = (JSONObject) item;
            result.add(ImmutableFundingAllocationDecisionSpec.builder()
                    .allocationId(value.getString(ImmutableFundingAllocationDecisionSpec.Fields.allocationId))
                    .subjectRef(parseSubjectRef(value.getJSONObject(
                            ImmutableFundingAllocationDecisionSpec.Fields.subjectRef)))
                    .ledgerSubjectCode(parseLedgerSubjectCode(value.getString(
                            ImmutableFundingAllocationDecisionSpec.Fields.ledgerSubjectCode)))
                    .amount(parseMoney(value.getJSONObject(ImmutableFundingAllocationDecisionSpec.Fields.amount)))
                    .priority(value.getInteger(ImmutableFundingAllocationDecisionSpec.Fields.priority))
                    .reason(value.getString(ImmutableFundingAllocationDecisionSpec.Fields.reason))
                    .build());
        }
        return List.copyOf(result);
    }

    private static List<String> parseStringList(JSONArray values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(String.valueOf(value));
        }
        return List.copyOf(result);
    }

    private static LedgerSubjectCode parseLedgerSubjectCode(String value) {
        return StringUtils.hasText(value) ? LedgerSubjectCode.valueOf(value) : null;
    }

    private static PlatformAccountsSnapshotSpec parsePlatformAccounts(JSONObject value) {
        if (CollectionUtils.isEmpty(value)) {
            return null;
        }
        return ImmutablePlatformAccountsSnapshotSpec.builder()
                .cashFundingAccount(parseSubjectRef(value.getJSONObject(
                        ImmutablePlatformAccountsSnapshotSpec.Fields.cashFundingAccount)))
                .prepaymentFundingAccount(parseSubjectRef(value.getJSONObject(
                        ImmutablePlatformAccountsSnapshotSpec.Fields.prepaymentFundingAccount)))
                .clearingFundingAccount(parseSubjectRef(value.getJSONObject(
                        ImmutablePlatformAccountsSnapshotSpec.Fields.clearingFundingAccount)))
                .settlementFundingAccount(parseSubjectRef(value.getJSONObject(
                        ImmutablePlatformAccountsSnapshotSpec.Fields.settlementFundingAccount)))
                .feeFundingAccount(parseSubjectRef(value.getJSONObject(
                        ImmutablePlatformAccountsSnapshotSpec.Fields.feeFundingAccount)))
                .adjustmentFundingAccount(parseSubjectRef(value.getJSONObject(
                        ImmutablePlatformAccountsSnapshotSpec.Fields.adjustmentFundingAccount)))
                .build();
    }

    private static LocalDateTime parseLocalDateTime(String value, LocalDateTime defaultValue) {
        return StringUtils.hasText(value) ? LocalDateTime.parse(value) : defaultValue;
    }

    private static Map<String, Object> parseObjectMap(JSONObject value) {
        return CollectionUtils.isEmpty(value) ? Map.of() : Map.copyOf(value);
    }

}

package com.wind.integration.funds.spec;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.integration.funds.model.route.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDslContractTests {

    @Test
    void testRouteLegTypeShouldBeMovementOnly() {
        Set<String> names = enumNames(RouteLegType.class);

        assertEquals(Set.of("EXTERNAL_IN", "EXTERNAL_OUT", "INTERNAL_TRANSFER", "HOLD", "RELEASE",
                "CONSUME", "RESTORE", "ADJUST"), names);
    }

    @Test
    void testRouteLegTypeShouldNotContainBusinessEvents() {
        Set<String> names = enumNames(RouteLegType.class);

        assertTrue(Set.of("TOPUP", "PAY", "REFUND", "WITHDRAW", "AUTHORIZE", "SETTLE",
                "CHARGEBACK", "BALANCE_ADJUST", "LIMIT_ADJUST")
                .stream()
                .noneMatch(names::contains));
    }

    @Test
    void testFundsTransactionEventTypeShouldKeepStableBusinessEvents() {
        Set<String> names = enumNames(FundsTransactionEventType.class);

        assertEquals(Set.of("TOPUP", "TRANSFER", "PAY", "REFUND", "WITHDRAW", "FEE_CHARGE",
                "FEE_REFUND", "AUTHORIZE", "REVERSAL", "SETTLE", "AUTH_REFUND",
                "CHARGEBACK", "FREEZE", "UNFREEZE", "BALANCE_ADJUST", "LIMIT_ADJUST"), names);
    }

    @Test
    void testRouteReplayPolicyShouldContainReplayOnce() {
        Set<String> names = enumNames(RouteReplayPolicy.class);

        assertEquals(Set.of("FULL_ONLY", "PARTIAL_ALLOWED", "NON_REPLAYABLE", "REPLAY_ONCE"), names);
    }

    @Test
    void testRouteReplayTypeShouldUseSettlementInsteadOfCapture() {
        Set<String> names = enumNames(RouteReplayType.class);

        assertEquals(Set.of("RELEASE_HOLD", "AUTHORIZATION_SETTLEMENT", "AUTHORIZATION_REFUND",
                "REFUND", "FEE_REFUND", "CHARGEBACK", "UNFREEZE"), names);
        assertTrue(names.contains("AUTHORIZATION_SETTLEMENT"));
        assertFalse(names.contains("CAPTURE"));
    }

    @Test
    void testRouteNodeTypeShouldSeparateNodeSemanticFromFundsSubjectType() {
        Set<String> nodeTypes = enumNames(RouteNodeType.class);
        Set<String> subjectTypes = enumNames(FundsSubjectType.class);

        assertEquals(Set.of("SUBJECT", "PLATFORM_FUNDING_ACCOUNT", "EXTERNAL_ACCOUNT", "PAYMENT_INSTRUMENT"),
                nodeTypes);
        assertEquals(Set.of("FUNDING_ACCOUNT", "CREDIT_ACCOUNT", "BUDGET_GROUP"), subjectTypes);
    }

    @Test
    void testLedgerPhaseCodeShouldUseSettlementInsteadOfCapture() {
        Set<String> names = enumNames(LedgerPhaseCode.class);

        assertTrue(names.contains("SETTLEMENT"));
        assertTrue(names.contains("CHARGEBACK"));
        assertFalse(names.contains("CAPTURE"));
    }

    @Test
    void testLedgerPhaseCodeShouldNotContainBusinessState() {
        Set<String> names = enumNames(LedgerPhaseCode.class);

        assertTrue(Set.of("TOPUP", "WITHDRAW", "WITHDRAW_APPLY", "WITHDRAW_CONFIRM",
                "WITHDRAW_FAIL", "CARD_AUTH", "MERCHANT_PAY", "AUTH_REFUND", "BALANCE_ADJUST",
                "LIMIT_ADJUST")
                .stream()
                .noneMatch(names::contains));
    }

    @Test
    void testExternalAccountRefShouldOnlyDescribeExternalAccount() {
        Set<String> methods = methodNames(ExternalAccountRefSpec.class);

        assertTrue(methods.contains("getExternalAccountId"));
        assertTrue(methods.contains("getExternalAccountType"));
        assertFalse(methods.contains("getMirrorSubjectRef"));
        assertFalse(methods.contains("getMirrorSubjectCode"));
    }

    @Test
    void testRouteSnapshotShouldRequireSchemaVersion() {
        Set<String> methods = methodNames(RouteSnapshotSpec.class);

        assertTrue(methods.contains("getSnapshotSchemaVersion"));
    }

    @Test
    void testLedgerEntryShouldNotContainAccountFields() {
        Set<String> methods = methodNames(LedgerEntrySpec.class);

        assertTrue(methods.contains("getSubjectId"));
        assertTrue(methods.contains("getSubjectType"));
        assertFalse(methods.contains("getAccountId"));
        assertFalse(methods.contains("getAccountType"));
        assertFalse(methods.contains("getExternalAccountId"));
        assertFalse(methods.contains("getInstrumentId"));
    }

    @Test
    void testPaymentInstrumentRefShouldNotBeLedgerSubject() {
        Set<String> methods = methodNames(PaymentInstrumentRefSpec.class);

        assertTrue(methods.contains("getInstrumentId"));
        assertTrue(methods.contains("getBindingSnapshot"));
        assertFalse(methods.contains("getSubjectId"));
        assertFalse(methods.contains("getLedgerId"));
        assertFalse(methods.contains("getLedgerSubjectCode"));
    }

    @Test
    void testRouteLegShouldExposeReplayRefLegId() {
        RouteLegSpec routeLeg = routeLegBuilder()
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .replayRefLegId("ORIGINAL_LEG_001")
                .build();

        assertEquals(RouteReplayPolicy.PARTIAL_ALLOWED, routeLeg.getReplayPolicy());
        assertEquals("ORIGINAL_LEG_001", routeLeg.getReplayRefLegId());
    }

    @Test
    void testRouteLegShouldExposeConstraintOverrides() {
        Map<String, LedgerBalanceConstraintType> overrides = new java.util.LinkedHashMap<>();
        overrides.put("source.available", LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
        overrides.put("target.available", LedgerBalanceConstraintType.ALLOW_NEGATIVE);

        RouteLegSpec routeLeg = routeLegBuilder()
                .constraintOverrides(overrides)
                .build();
        overrides.put("source.available", LedgerBalanceConstraintType.ALLOW_NEGATIVE);

        assertEquals(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE,
                routeLeg.getConstraintOverrides().get("source.available"));
        assertEquals(LedgerBalanceConstraintType.ALLOW_NEGATIVE,
                routeLeg.getConstraintOverrides().get("target.available"));
        assertFalse(routeLeg.getConstraintOverrides().isEmpty());
    }

    @Test
    void testRouteLegShouldSupportOptionalPeriodTypeAndPeriodId() {
        RouteLegSpec routeLeg = routeLegBuilder()
                .periodType(AccountBalancePeriodType.MONTHLY)
                .periodId("2026-05")
                .build();

        assertEquals(AccountBalancePeriodType.MONTHLY, routeLeg.getPeriodType());
        assertEquals("2026-05", routeLeg.getPeriodId());
    }

    @Test
    void testRouteSnapshotShouldCarryRouteAndSchemaVersion() {
        RouteSnapshotSpec snapshot = routeSnapshotBuilder()
                .routeVersion("route-v4.1")
                .snapshotSchemaVersion("snapshot-v4")
                .build();

        assertEquals("route-v4.1", snapshot.getRouteVersion());
        assertEquals("snapshot-v4", snapshot.getSnapshotSchemaVersion());
    }

    @Test
    void testRouteSnapshotShouldContainParticipantsAndLegs() {
        RouteSnapshotSpec snapshot = routeSnapshotBuilder().build();

        assertFalse(snapshot.getParticipants().isEmpty());
        assertFalse(snapshot.getLegs().isEmpty());
        assertEquals("PARTICIPANT_PAYER", snapshot.getParticipants().getFirst().getSubjectRef().getSubjectId());
        assertEquals("LEG_001", snapshot.getLegs().getFirst().getLegId());
    }

    @Test
    void testRouteSnapshotShouldDefensivelyCopyParticipantsAndLegs() {
        List<RouteParticipantSpec> participants = new java.util.ArrayList<>();
        List<RouteLegSpec> legs = new java.util.ArrayList<>();
        participants.add(routeParticipant("PARTICIPANT_PAYER", RouteParticipantRole.PAYER));
        legs.add(routeLegBuilder().legId("LEG_001").build());

        RouteSnapshotSpec snapshot = routeSnapshotBuilder()
                .participants(participants)
                .legs(legs)
                .build();
        participants.add(routeParticipant("PARTICIPANT_SHADOW", RouteParticipantRole.PAYEE));
        legs.add(routeLegBuilder().legId("LEG_002").build());

        assertEquals(1, snapshot.getParticipants().size());
        assertEquals(1, snapshot.getLegs().size());
        assertEquals("PARTICIPANT_PAYER", snapshot.getParticipants().getFirst().getSubjectRef().getSubjectId());
        assertEquals("LEG_001", snapshot.getLegs().getFirst().getLegId());
    }

    @Test
    void testRouteSnapshotShouldPreserveInstrumentAndExternalAccount() {
        RouteSnapshotSpec snapshot = routeSnapshotBuilder()
                .paymentInstrumentRef(paymentInstrumentRef())
                .externalAccountRef(externalAccountRef())
                .build();

        assertEquals("CARD_001", snapshot.getPaymentInstrumentRef().getInstrumentId());
        assertEquals("CARD", snapshot.getPaymentInstrumentRef().getInstrumentType());
        assertEquals("binding_001", snapshot.getPaymentInstrumentRef().getBindingSnapshot().get("bindingId"));
        assertEquals("BANK_ACCOUNT_001", snapshot.getExternalAccountRef().getExternalAccountId());
        assertEquals("BANK_ACCOUNT", snapshot.getExternalAccountRef().getExternalAccountType());
        assertEquals("stripe", snapshot.getExternalAccountRef().getProviderCode());
    }

    @Test
    void testRouteSnapshotShouldPreserveOriginalCurrencySnapshot() {
        RouteLegSpec routeLeg = routeLegBuilder()
                .amount(Money.immutable(1_100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(1_000L, CurrencyIsoCode.EUR))
                .exchangeRate(new BigDecimal("1.10"))
                .build();
        RouteSnapshotSpec snapshot = routeSnapshotBuilder()
                .legs(List.of(routeLeg))
                .build();

        assertEquals(CurrencyIsoCode.USD, snapshot.getLegs().getFirst().getAmount().getCurrency());
        assertEquals(CurrencyIsoCode.EUR, snapshot.getLegs().getFirst().getOriginalAmount().getCurrency());
        assertEquals(0, new BigDecimal("1.10").compareTo(snapshot.getLegs().getFirst().getExchangeRate()));
    }

    @Test
    void testSnapshotShouldPreservePlatformAccountsForReplay() {
        RouteSnapshotSpec snapshot = routeSnapshotBuilder()
                .platformAccounts(ImmutablePlatformAccountsSnapshotSpec.builder()
                        .reserveFundingAccount(subjectRef("PLATFORM_RESERVE_USD"))
                        .prepaymentFundingAccount(subjectRef("PLATFORM_PREPAYMENT_USD"))
                        .settlementFundingAccount(subjectRef("PLATFORM_SETTLEMENT_USD"))
                        .feeFundingAccount(subjectRef("PLATFORM_FEE_USD"))
                        .build())
                .build();

        assertEquals("PLATFORM_RESERVE_USD",
                snapshot.getPlatformAccounts().getReserveFundingAccount().getSubjectId());
        assertEquals("PLATFORM_PREPAYMENT_USD",
                snapshot.getPlatformAccounts().getPrepaymentFundingAccount().getSubjectId());
        assertEquals("PLATFORM_SETTLEMENT_USD",
                snapshot.getPlatformAccounts().getSettlementFundingAccount().getSubjectId());
        assertEquals("PLATFORM_FEE_USD",
                snapshot.getPlatformAccounts().getFeeFundingAccount().getSubjectId());
    }

    @Test
    void testPaymentInstrumentAndExternalAccountSnapshotsShouldBeImmutable() {
        Map<String, Object> bindingSnapshot = new java.util.LinkedHashMap<>();
        bindingSnapshot.put("bindingId", "binding_001");
        Map<String, Object> externalContext = new java.util.LinkedHashMap<>();
        externalContext.put("wireType", "ach");

        PaymentInstrumentRefSpec paymentInstrumentRef = ImmutablePaymentInstrumentRefSpec.builder()
                .instrumentId("CARD_001")
                .instrumentType("CARD")
                .instrumentNo("****4242")
                .ownerId("USER_001")
                .ownerType("USER")
                .tenantId(1L)
                .currency(CurrencyIsoCode.USD.name())
                .bindingSnapshot(bindingSnapshot)
                .build();
        ExternalAccountRefSpec externalAccountRef = ImmutableExternalAccountRefSpec.builder()
                .externalAccountId("BANK_ACCOUNT_001")
                .externalAccountType("BANK_ACCOUNT")
                .externalAccountNo("****6789")
                .providerCode("stripe")
                .channelCode("stripe-us")
                .currency(CurrencyIsoCode.USD.name())
                .countryCode("US")
                .contextVariables(externalContext)
                .build();
        bindingSnapshot.put("bindingId", "binding_changed");
        externalContext.put("wireType", "swift");

        assertEquals("binding_001", paymentInstrumentRef.getBindingSnapshot().get("bindingId"));
        assertEquals("ach", externalAccountRef.getContextVariables().get("wireType"));
    }

    @Test
    void testLedgerTransactionShouldContainPostingPlans() {
        Set<String> methods = methodNames(com.wind.integration.funds.spec.ledger.LedgerTransactionSpec.class);

        assertTrue(methods.contains("getPostingPlans"));
        assertTrue(methods.contains("isBalanced"));
        assertTrue(methods.contains("getTotalDebitAmount"));
        assertTrue(methods.contains("getTotalCreditAmount"));
    }

    @Test
    void testLedgerPostingPlanShouldExposeRouteLegId() {
        Set<String> methods = methodNames(LedgerPostingPlanSpec.class);

        assertTrue(methods.contains("getRouteLegId"));
    }

    @Test
    void testLedgerPostingPlanShouldBeSelfBalancedObject() {
        LedgerPostingPlanSpec balancedPlan = postingPlan(List.of(
                ledgerEntry(EntrySide.DEBIT, 1_000L),
                ledgerEntry(EntrySide.CREDIT, 1_000L)
        ));
        LedgerPostingPlanSpec unbalancedPlan = postingPlan(List.of(
                ledgerEntry(EntrySide.DEBIT, 1_000L),
                ledgerEntry(EntrySide.CREDIT, 900L)
        ));

        assertEquals(Money.immutable(1_000L, CurrencyIsoCode.USD), balancedPlan.getTotalDebitAmount());
        assertEquals(Money.immutable(1_000L, CurrencyIsoCode.USD), balancedPlan.getTotalCreditAmount());
        assertTrue(balancedPlan.isBalanced());
        assertFalse(unbalancedPlan.isBalanced());
    }

    private static <E extends Enum<E>> Set<String> enumNames(Class<E> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    private static Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }

    private static LedgerPostingPlanSpec postingPlan(List<LedgerEntrySpec> entries) {
        return new TestLedgerPostingPlanSpec(List.of(new TestLedgerPostingPhaseSpec(entries)));
    }

    private static LedgerEntrySpec ledgerEntry(EntrySide entrySide, long amount) {
        return new TestLedgerEntrySpec(entrySide, Money.immutable(amount, CurrencyIsoCode.USD));
    }

    private static ImmutableRouteSnapshotSpec.ImmutableRouteSnapshotSpecBuilder routeSnapshotBuilder() {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("SNAPSHOT_001")
                .snapshotSchemaVersion("snapshot-v4")
                .routeCode("TRANSFER")
                .routeVersion("route-v4")
                .businessScene("TRANSFER")
                .businessSn("TRANSFER_001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .participants(List.of(routeParticipant("PARTICIPANT_PAYER", RouteParticipantRole.PAYER),
                        routeParticipant("PARTICIPANT_PAYEE", RouteParticipantRole.PAYEE)))
                .legs(List.of(routeLegBuilder().build()))
                .resolvedAt(LocalDateTime.of(2026, 5, 12, 12, 0))
                .contextVariables(Map.of("routeRule", "direct"));
    }

    private static ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLegBuilder() {
        return ImmutableRouteLegSpec.builder()
                .legId("LEG_001")
                .sequence(1)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(routeNode("SOURCE_FUNDING", RouteNodeRole.SOURCE))
                .targetNode(routeNode("TARGET_FUNDING", RouteNodeRole.TARGET))
                .amount(Money.immutable(1_000L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.DECREASE)
                .phaseCode(LedgerPhaseCode.TRANSFER)
                .contextVariables(Map.of("purpose", "contract-test"));
    }

    private static RouteNodeSpec routeNode(String subjectId, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef(subjectId))
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private static RouteParticipantSpec routeParticipant(String subjectId, RouteParticipantRole participantRole) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(participantRole)
                .subjectRef(subjectRef(subjectId))
                .currency(CurrencyIsoCode.USD.name())
                .amount(Money.immutable(1_000L, CurrencyIsoCode.USD))
                .contextVariables(Map.of("role", participantRole.name()))
                .build();
    }

    private static SubjectRef subjectRef(String subjectId) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .currency(CurrencyIsoCode.USD.name())
                .ledgerProfileCode("FUNDING_ACCOUNT")
                .build();
    }

    private static PaymentInstrumentRefSpec paymentInstrumentRef() {
        return ImmutablePaymentInstrumentRefSpec.builder()
                .instrumentId("CARD_001")
                .instrumentType("CARD")
                .instrumentNo("****4242")
                .ownerId("USER_001")
                .ownerType("USER")
                .tenantId(1L)
                .currency(CurrencyIsoCode.USD.name())
                .bindingSnapshot(Map.of("bindingId", "binding_001"))
                .build();
    }

    private static ExternalAccountRefSpec externalAccountRef() {
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId("BANK_ACCOUNT_001")
                .externalAccountType("BANK_ACCOUNT")
                .externalAccountNo("****6789")
                .providerCode("stripe")
                .channelCode("stripe-us")
                .currency(CurrencyIsoCode.USD.name())
                .countryCode("US")
                .contextVariables(Map.of("wireType", "ach"))
                .build();
    }

    private record TestLedgerPostingPlanSpec(List<LedgerPostingPhaseSpec> postingPhases)
            implements LedgerPostingPlanSpec {

        @Override
        public List<LedgerPostingPhaseSpec> getPostingPhases() {
            return postingPhases;
        }

        @Override
        public String getPlanId() {
            return "PLAN_001";
        }

        @Override
        public String getLedgerTransactionSn() {
            return "LEDGER_TXN_001";
        }

        @Override
        public LedgerPostingIntentType getIntent() {
            return LedgerPostingIntentType.TRANSFER;
        }
    }

    private record TestLedgerPostingPhaseSpec(List<LedgerEntrySpec> entries) implements LedgerPostingPhaseSpec {

        @Override
        public List<LedgerEntrySpec> getEntries() {
            return entries;
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.TRANSFER;
        }
    }

    private record TestLedgerEntrySpec(EntrySide entryType, Money amount) implements LedgerEntrySpec {

        @Override
        public EntrySide getEntryType() {
            return entryType;
        }

        @Override
        public Money getAmount() {
            return amount;
        }

        @Override
        public String getSubjectId() {
            return "funding_001";
        }

        @Override
        public String getSubjectType() {
            return "FUNDING_ACCOUNT";
        }

        @Override
        public LedgerSubjectCode getLedgerSubjectCode() {
            return LedgerSubjectCode.AVAILABLE;
        }

        @Override
        public LedgerSubjectCategory getLedgerSubjectCategory() {
            return LedgerSubjectCategory.ASSET;
        }

        @Override
        public String getLedgerTransactionSn() {
            return "LEDGER_TXN_001";
        }

        @Override
        public String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public String getBusinessSn() {
            return "TRANSFER_001";
        }

        @Override
        public Money getOriginalAmount() {
            return amount;
        }

        @Override
        public BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return LocalDateTime.of(2026, 5, 12, 11, 0);
        }

        @Override
        public String getDescription() {
            return "test ledger entry";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }
}

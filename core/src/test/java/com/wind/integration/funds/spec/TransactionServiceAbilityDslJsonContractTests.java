package com.wind.integration.funds.spec;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionServiceAbilityDslJsonContractTests {

    private static final String DSL_RESOURCE_DIR = "dsl/transaction-layer";

    private static final Set<String> REQUIRED_CASE_FILES = Set.of(
            "direct-platform-transfer.json",
            "direct-wallet-payment-with-fee.json",
            "reverse-refund-original-route.json",
            "authorization-approve-multi-subject.json",
            "authorization-decline-no-posting.json",
            "balance-freeze-order.json",
            "transaction-view-replay-range.json"
    );

    private static final Set<String> WRITE_SERVICE_ABILITIES = Set.of(
            "DIRECT_TRANSACTION",
            "REVERSE_TRANSACTION",
            "AUTHORIZATION_TRANSACTION",
            "BALANCE_CONTROL"
    );

    private static final Set<String> REQUIRED_SERVICE_ABILITIES = Set.of(
            "DIRECT_TRANSACTION",
            "REVERSE_TRANSACTION",
            "AUTHORIZATION_TRANSACTION",
            "BALANCE_CONTROL",
            "QUERY_AND_REPLAY"
    );

    private static final Set<String> NO_POSTING_SCENARIOS = Set.of(
            "AUTHORIZATION_DECLINE",
            "TRANSACTION_VIEW_REPLAY_RANGE"
    );

    private static final Set<String> ROUTE_REPLAY_SCENARIOS = Set.of(
            "DIRECT_REFUND"
    );

    @Test
    void testDslJsonCaseFilesShouldExist() {
        Set<String> fileNames = cases().stream()
                .map(DslCase::fileName)
                .collect(Collectors.toSet());

        assertEquals(REQUIRED_CASE_FILES, fileNames);
    }

    @Test
    void testDslJsonShouldCoverRequiredServiceAbilities() {
        Set<String> serviceAbilities = cases().stream()
                .map(DslCase::serviceAbility)
                .collect(Collectors.toSet());

        assertEquals(REQUIRED_SERVICE_ABILITIES, serviceAbilities);
    }

    @Test
    void testDslJsonShouldHaveStableIdentityAndValidation() {
        for (DslCase dslCase : cases()) {
            JSONObject value = dslCase.value();
            assertRequiredString(value, "caseId", dslCase.fileName());
            assertRequiredString(value, "serviceAbility", dslCase.fileName());
            assertRequiredString(value, "scenarioCode", dslCase.fileName());
            assertRequiredString(value, "description", dslCase.fileName());

            JSONObject validation = value.getJSONObject("validation");
            assertNotNull(validation, dslCase.fileName());
            assertFalse(validation.getJSONArray("mustPass").isEmpty(), dslCase.fileName());
            assertFalse(validation.getJSONArray("mustFail").isEmpty(), dslCase.fileName());
        }
    }

    @Test
    void testWriteAbilityInstructionShouldMatchCurrentCoreEnums() {
        for (DslCase dslCase : cases()) {
            if (!WRITE_SERVICE_ABILITIES.contains(dslCase.serviceAbility())) {
                continue;
            }
            JSONObject instruction = dslCase.value().getJSONObject("instruction");
            assertNotNull(instruction, dslCase.fileName());
            assertEnum(FundsInstructionType.class, instruction.getString("instructionType"), dslCase.fileName());
            assertEnum(FundsTransactionEventType.class, instruction.getString("eventType"), dslCase.fileName());
            assertEnum(DefaultFundsTransactionType.class, instruction.getString("transactionType"), dslCase.fileName());
            assertRequiredString(instruction, "businessScene", dslCase.fileName());
            assertRequiredString(instruction, "businessSn", dslCase.fileName());
            assertMoney(instruction.getJSONObject("amount"), dslCase.fileName());
            assertMoney(instruction.getJSONObject("originalAmount"), dslCase.fileName());
            assertRequiredString(instruction, "exchangeRate", dslCase.fileName());

            JSONObject reference = instruction.getJSONObject("reference");
            if (reference != null) {
                assertEnum(FundsInstructionReferenceType.class, reference.getString("referenceType"), dslCase.fileName());
            }
        }
    }

    @Test
    void testRouteShouldMatchCurrentCoreEnumsAndSubjectBoundary() {
        for (DslCase dslCase : cases()) {
            JSONObject route = dslCase.value().getJSONObject("expectedRoute");
            if (route == null) {
                assertTrue(NO_POSTING_SCENARIOS.contains(dslCase.scenarioCode()), dslCase.fileName());
                continue;
            }
            assertRequiredString(route, "routeCode", dslCase.fileName());
            assertRequiredString(route, "routeVersion", dslCase.fileName());
            assertRequiredString(route, "snapshotSchemaVersion", dslCase.fileName());
            JSONArray participants = route.getJSONArray("participants");
            if (participants != null) {
                for (Object participant : participants) {
                    JSONObject item = (JSONObject) participant;
                    assertEnum(RouteParticipantRole.class, item.getString("participantRole"), dslCase.fileName());
                    assertSubjectRef(item.getJSONObject("subjectRef"), dslCase.fileName());
                }
            }

            for (Object leg : route.getJSONArray("legs")) {
                JSONObject item = (JSONObject) leg;
                assertRequiredString(item, "legId", dslCase.fileName());
                assertEnum(RouteLegType.class, item.getString("legType"), dslCase.fileName());
                assertRouteNode(item.getJSONObject("sourceNode"), dslCase.fileName());
                assertRouteNode(item.getJSONObject("targetNode"), dslCase.fileName());
                assertMoney(item.getJSONObject("amount"), dslCase.fileName());
                assertEnum(LedgerBalanceEffectType.class, item.getString("balanceEffectType"), dslCase.fileName());
                assertEnum(LedgerPhaseCode.class, item.getString("phaseCode"), dslCase.fileName());
                assertEnum(RouteReplayPolicy.class, item.getString("replayPolicy"), dslCase.fileName());
            }
        }
    }

    @Test
    void testPostingPlansShouldBeBalanced() {
        for (DslCase dslCase : cases()) {
            JSONObject posting = dslCase.value().getJSONObject("expectedPosting");
            if (posting == null) {
                assertTrue(NO_POSTING_SCENARIOS.contains(dslCase.scenarioCode()), dslCase.fileName());
                continue;
            }
            for (Object plan : posting.getJSONArray("postingPlans")) {
                JSONObject item = (JSONObject) plan;
                assertRequiredString(item, "routeLegId", dslCase.fileName());
                assertEnum(LedgerPostingIntentType.class, item.getString("intent"), dslCase.fileName());
                assertEnum(LedgerPostingScope.class, item.getString("postingScope"), dslCase.fileName());
                assertEnum(LedgerBalanceEffectType.class, item.getString("balanceEffectType"), dslCase.fileName());
                assertEnum(LedgerPhaseCode.class, item.getString("phaseCode"), dslCase.fileName());
                assertBalancedEntries(item.getJSONArray("entries"), dslCase.fileName());
            }
        }
    }

    @Test
    void testNoPostingScenariosShouldNotCarryRouteOrPosting() {
        Map<String, DslCase> cases = casesByScenarioCode();

        for (String scenarioCode : NO_POSTING_SCENARIOS) {
            DslCase dslCase = cases.get(scenarioCode);
            assertNotNull(dslCase, scenarioCode);
            assertNull(dslCase.value().getJSONObject("expectedRoute"), dslCase.fileName());
            assertNull(dslCase.value().getJSONObject("expectedPosting"), dslCase.fileName());
        }
    }

    @Test
    void testReverseTransactionShouldReferenceOriginalRouteSnapshot() {
        Map<String, DslCase> cases = casesByScenarioCode();

        for (String scenarioCode : ROUTE_REPLAY_SCENARIOS) {
            DslCase dslCase = cases.get(scenarioCode);
            assertNotNull(dslCase, scenarioCode);
            JSONObject instruction = dslCase.value().getJSONObject("instruction");
            JSONObject reference = instruction.getJSONObject("reference");
            assertNotNull(reference, dslCase.fileName());
            assertEquals(FundsInstructionReferenceType.ORIGINAL_TRANSACTION.name(),
                    reference.getString("referenceType"), dslCase.fileName());
            assertRequiredString(reference.getJSONObject("contextVariables"), "referenceSnapshotId", dslCase.fileName());
            assertRequiredString(dslCase.value().getJSONObject("expectedRoute"), "referenceSnapshotId", dslCase.fileName());
        }
    }

    private static List<DslCase> cases() {
        try {
            Path directory = Path.of(Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(DSL_RESOURCE_DIR)
                    .toURI());
            try (var files = Files.list(directory)) {
                return files
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .map(TransactionServiceAbilityDslJsonContractTests::readCase)
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, DslCase> casesByScenarioCode() {
        return cases().stream()
                .collect(Collectors.toMap(DslCase::scenarioCode, Function.identity()));
    }

    private static DslCase readCase(Path path) {
        try {
            String fileName = path.getFileName().toString();
            JSONObject value = JSON.parseObject(Files.readString(path));
            return new DslCase(fileName, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void assertBalancedEntries(JSONArray entries, String fileName) {
        assertNotNull(entries, fileName);
        assertFalse(entries.isEmpty(), fileName);
        Map<String, Long> debitAmounts = entries.stream()
                .map(JSONObject.class::cast)
                .filter(entry -> EntrySide.DEBIT.name().equals(entry.getString("entrySide")))
                .collect(Collectors.groupingBy(
                        entry -> entry.getJSONObject("amount").getString("currency"),
                        Collectors.summingLong(entry -> entry.getJSONObject("amount").getLongValue("minorValue"))
                ));
        Map<String, Long> creditAmounts = entries.stream()
                .map(JSONObject.class::cast)
                .filter(entry -> EntrySide.CREDIT.name().equals(entry.getString("entrySide")))
                .collect(Collectors.groupingBy(
                        entry -> entry.getJSONObject("amount").getString("currency"),
                        Collectors.summingLong(entry -> entry.getJSONObject("amount").getLongValue("minorValue"))
                ));
        assertEquals(debitAmounts, creditAmounts, fileName);

        for (Object entry : entries) {
            JSONObject item = (JSONObject) entry;
            assertRequiredString(item, "subjectId", fileName);
            assertEnum(FundsSubjectType.class, item.getString("subjectType"), fileName);
            assertEnum(LedgerSubjectCode.class, item.getString("ledgerSubjectCode"), fileName);
            assertEnum(EntrySide.class, item.getString("entrySide"), fileName);
            assertMoney(item.getJSONObject("amount"), fileName);
        }
    }

    private static void assertSubjectRef(JSONObject subjectRef, String fileName) {
        assertNotNull(subjectRef, fileName);
        assertEnum(FundsSubjectType.class, subjectRef.getString("subjectType"), fileName);
        assertRequiredString(subjectRef, "subjectId", fileName);
        assertEnum(CurrencyIsoCode.class, subjectRef.getString("currency"), fileName);
        assertRequiredString(subjectRef, "ledgerProfileCode", fileName);
    }

    private static void assertRouteNode(JSONObject value, String fileName) {
        assertNotNull(value, fileName);
        assertEnum(RouteNodeRole.class, value.getString("nodeRole"), fileName);
        assertEnum(FundsSubjectType.class, value.getString("subjectType"), fileName);
        assertRequiredString(value, "subjectId", fileName);
        assertEnum(LedgerSubjectCode.class, value.getString("ledgerSubjectCode"), fileName);
    }

    private static void assertMoney(JSONObject value, String fileName) {
        assertNotNull(value, fileName);
        assertEnum(CurrencyIsoCode.class, value.getString("currency"), fileName);
        assertTrue(value.getLongValue("minorValue") > 0L, fileName);
    }

    private static void assertRequiredString(JSONObject value, String name, String fileName) {
        assertNotNull(value, fileName);
        String text = value.getString(name);
        assertNotNull(text, fileName + " missing " + name);
        assertFalse(text.isBlank(), fileName + " missing " + name);
    }

    private static <E extends Enum<E>> void assertEnum(Class<E> enumType, String value, String fileName) {
        assertNotNull(value, fileName + " missing enum value for " + enumType.getSimpleName());
        assertDoesNotThrow(() -> Enum.valueOf(enumType, value), fileName + " invalid " + enumType.getSimpleName());
    }

    private record DslCase(String fileName, JSONObject value) {

        private String serviceAbility() {
            return value.getString("serviceAbility");
        }

        private String scenarioCode() {
            return value.getString("scenarioCode");
        }
    }
}

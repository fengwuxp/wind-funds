package com.wind.funds.architecture;

import com.wind.funds.governance.dal.entities.ProjectionReplayTask;
import com.wind.funds.governance.projection.FundsProjectionReplayTaskDTO;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.LedgerTransactionPostingService;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金模块 Maven 依赖边界测试。
 */
class FundsModuleDependencyBoundaryTests {

    @Test
    void testTransactionLedgerReferenceResolutionShouldNotKeepWalletBridgeOrAuditSurface() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String converter : List.of(
                "transaction/impl/src/main/java/com/wind/funds/transaction/converter/"
                        + "FundsDirectTransactionInstructionConverter.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/converter/"
                        + "FundsAuthorizationInstructionConverter.java")) {
            String source = Files.readString(workspaceRoot().resolve(converter));
            if (source.contains("LedgerQueryService") || source.contains("ledgerQueryService")) {
                violations.add(converter + " still queries Ledger through Wallet");
            }
        }
        for (String retiredPath : List.of(
                "wallet/face/src/main/java/com/wind/funds/wallet/service/LedgerQueryService.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/dto/LedgerTransactionFactDTO.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/dto/LedgerEntryFactDTO.java",
                "wallet/impl/src/main/java/com/wind/funds/wallet/services/impl/DefaultLedgerQueryService.java",
                "transaction/face/src/main/java/com/wind/funds/transaction/application/"
                        + "FundsBalanceAdjustmentAuditApplicationService.java",
                "transaction/face/src/main/java/com/wind/funds/transaction/model/dto/"
                        + "FundsBalanceAdjustmentAuditDTO.java",
                "transaction/face/src/main/java/com/wind/funds/transaction/model/query/"
                        + "FundsBalanceAdjustmentAuditQuery.java",
                "transaction/face/src/main/java/com/wind/funds/transaction/enums/"
                        + "FundsBalanceAdjustmentAuditCompleteness.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/impl/"
                        + "DefaultFundsBalanceAdjustmentAuditApplicationService.java")) {
            if (Files.exists(workspaceRoot().resolve(retiredPath))) {
                violations.add(retiredPath + " still exists");
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void testTransactionOrchestrationShouldNotRemainInWalletFace() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String retiredPath : List.of(
                "wallet/face/src/main/java/com/wind/funds/wallet/application/instrument/"
                        + "PaymentInstrumentTransactionApplicationService.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/application/spend/"
                        + "SpendControlTransactionConsumptionApplicationService.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/"
                        + "AuthorizeByPaymentInstrumentRequest.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/"
                        + "CompleteAuthorizationByPaymentInstrumentRequest.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/"
                        + "ReverseAuthorizationByPaymentInstrumentRequest.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/ReceiveByInstrumentRequest.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/"
                        + "SpendControlTransactionConsumptionRequest.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/"
                        + "SpendControlBusinessConfirmedRefundCompensationRequest.java")) {
            if (Files.exists(workspaceRoot().resolve(retiredPath))) {
                violations.add(retiredPath + " still exists");
            }
        }
        for (String requiredPath : List.of(
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/"
                        + "AuthorizeByPaymentInstrumentRequest.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/"
                        + "CompleteAuthorizationByPaymentInstrumentRequest.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/"
                        + "ReverseAuthorizationByPaymentInstrumentRequest.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/"
                        + "ReceiveByInstrumentRequest.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/spend/"
                        + "SpendControlTransactionConsumptionRequest.java",
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/spend/"
                        + "SpendControlBusinessConfirmedRefundCompensationRequest.java")) {
            if (Files.notExists(workspaceRoot().resolve(requiredPath))) {
                violations.add(requiredPath + " is missing");
            }
        }

        Map<String, List<String>> forbiddenSourceTokens = Map.of(
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/impl/"
                        + "PaymentInstrumentTransactionApplicationServiceImpl.java",
                List.of(
                        "com.wind.funds.wallet.application.instrument.PaymentInstrumentTransactionApplicationService",
                        "implements PaymentInstrumentTransactionApplicationService",
                        "com.wind.funds.wallet.application.spend.SpendControlTransactionConsumptionApplicationService"),
                "transaction/impl/src/main/java/com/wind/funds/transaction/application/spend/impl/"
                        + "SpendControlTransactionConsumptionApplicationServiceImpl.java",
                List.of(
                        "com.wind.funds.wallet.application.spend.SpendControlTransactionConsumptionApplicationService",
                        "implements SpendControlTransactionConsumptionApplicationService"));
        for (Map.Entry<String, List<String>> entry : forbiddenSourceTokens.entrySet()) {
            String source = Files.readString(workspaceRoot().resolve(entry.getKey()));
            for (String token : entry.getValue()) {
                if (source.contains(token)) {
                    violations.add(entry.getKey() + " still references " + token);
                }
            }
        }

        assertThat(violations)
                .as("transaction orchestration must be internal to transaction-impl")
                .isEmpty();
    }

    /**
     * 场景：Ledger 余额投影和 posting assembler 均只有一个真实生产实现。
     * 预期：删除单实现扩展接口、单 delegate composite 及其选择路由。
     * 红线：宿主不得通过 Bean 列表或排序分叉账务组装与余额投影语义。
     */
    @Test
    void testLedgerProjectionAndAssemblerExtensionRoutersShouldCollapse() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String retiredPath : List.of(
                "core/src/main/java/com/wind/funds/ledger/LedgerBalanceProjectionService.java",
                "ledger/impl/src/main/java/com/wind/funds/ledger/CompositeLedgerPostingAssembler.java")) {
            if (Files.exists(workspaceRoot().resolve(retiredPath))) {
                violations.add(retiredPath + " still exists");
            }
        }

        Map<String, List<String>> forbiddenSourceTokens = Map.of(
                "ledger/impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java",
                List.of("List<LedgerBalanceProjectionService>", "resolveProjectionServices"),
                "ledger/impl/src/main/java/com/wind/funds/ledger/impl/LedgerBalanceProjectionServiceImpl.java",
                List.of("implements LedgerBalanceProjectionService"),
                "ledger/impl/src/main/java/com/wind/funds/ledger/posting/DefaultLedgerPostingAssembler.java",
                List.of("Ordered", "getOrder("));
        for (Map.Entry<String, List<String>> entry : forbiddenSourceTokens.entrySet()) {
            String source = Files.readString(workspaceRoot().resolve(entry.getKey()));
            for (String token : entry.getValue()) {
                if (source.contains(token)) {
                    violations.add(entry.getKey() + " still contains " + token);
                }
            }
        }

        assertThat(violations)
                .as("Ledger projection and assembler semantics must not be selected by extension routing")
                .isEmpty();
    }

    /**
     * 场景：Ledger 只暴露接收归一指令、资金动作身份和已解析 route 的高阶写命令。
     * 预期：删除 core assembler 与 raw spec 写入口，Transaction 和 projection 不再持有组装后 spec。
     * 红线：不得用兼容 overload、bridge 或 stale API baseline 保留 Consumer 拼装 posting 的能力。
     */
    @Test
    void testLedgerPostingCommandShouldOwnAssemblyAndStableIdentity() throws IOException {
        List<String> violations = new ArrayList<>();
        Path root = workspaceRoot();
        Path assemblerPath = root.resolve(
                "core/src/main/java/com/wind/funds/ledger/LedgerPostingAssembler.java");
        if (Files.exists(assemblerPath)) {
            violations.add("core LedgerPostingAssembler still exists");
        }

        List<Method> postingMethods = Stream.of(LedgerTransactionPostingService.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("post"))
                .toList();
        List<Class<?>> expectedParameterTypes = List.of(
                FundsInstructionSpec.class,
                String.class,
                ResolvedRouteSpec.class);
        if (postingMethods.size() != 1
                || postingMethods.getFirst().getReturnType() != String.class
                || !List.of(postingMethods.getFirst().getParameterTypes()).equals(expectedParameterTypes)) {
            violations.add("LedgerTransactionPostingService must expose only the high-level posting signature");
        }

        List<Method> assembledSeams = Stream.of(
                        DefaultLedgerTransactionPostingServiceImpl.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("postAssembled"))
                .filter(method -> List.of(method.getParameterTypes()).equals(List.of(LedgerTransactionSpec.class)))
                .toList();
        if (assembledSeams.size() != 1) {
            violations.add("DefaultLedgerTransactionPostingServiceImpl must keep one package-private postAssembled seam");
        } else {
            int modifiers = assembledSeams.getFirst().getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers)) {
                violations.add("DefaultLedgerTransactionPostingServiceImpl.postAssembled must be package-private");
            }
        }
        Stream.of(DefaultLedgerTransactionPostingServiceImpl.class.getDeclaredMethods())
                .filter(method -> Stream.of(method.getParameterTypes())
                        .anyMatch(LedgerTransactionSpec.class::equals))
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        || Modifier.isProtected(method.getModifiers()))
                .forEach(method -> violations.add(
                        "DefaultLedgerTransactionPostingServiceImpl still exposes raw-spec method "
                                + method.getName()));

        String baseline = Files.readString(root.resolve("core/api-baseline/stable-api.txt"));
        if (baseline.lines().anyMatch(line -> line.startsWith(
                "com.wind.funds.ledger.LedgerPostingAssembler\t"))) {
            violations.add("stable API baseline still contains LedgerPostingAssembler");
        }
        String expectedBaselineSignature = "com.wind.funds.ledger.LedgerTransactionPostingService\t"
                + "public abstract java.lang.String post("
                + "com.wind.funds.transaction.spec.FundsInstructionSpec, java.lang.String, "
                + "com.wind.funds.route.spec.ResolvedRouteSpec);";
        if (!baseline.lines().anyMatch(expectedBaselineSignature::equals)) {
            violations.add("stable API baseline is missing the high-level posting signature");
        }

        String orchestratorPath =
                "transaction/impl/src/main/java/com/wind/funds/transaction/DefaultRoutedFundsInstructionOrchestrator.java";
        String orchestrator = Files.readString(root.resolve(orchestratorPath));
        for (String forbidden : List.of(
                "LedgerPostingAssembler",
                "LedgerTransactionSpec",
                "postingAssembler",
                "ledgerTransactionPostingService.post(transaction)",
                ".ledgerTransaction(")) {
            if (orchestrator.contains(forbidden)) {
                violations.add(orchestratorPath + " still contains " + forbidden);
            }
        }
        if (!orchestrator.contains("ledgerTransactionPostingService.post(")) {
            violations.add(orchestratorPath + " is missing the high-level posting call");
        }

        String projectionContextPath = "transaction/face/src/main/java/com/wind/funds/transaction/projection/"
                + "FundsTransactionProjectionPublishContext.java";
        String projectionContext = Files.readString(root.resolve(projectionContextPath));
        if (projectionContext.contains("LedgerTransactionSpec")) {
            violations.add(projectionContextPath + " still exposes LedgerTransactionSpec");
        }

        String defaultAssemblerPath =
                "ledger/impl/src/main/java/com/wind/funds/ledger/posting/DefaultLedgerPostingAssembler.java";
        String defaultAssembler = Files.readString(root.resolve(defaultAssemblerPath));
        for (String forbidden : List.of(
                "import com.wind.funds.ledger.LedgerPostingAssembler",
                "implements LedgerPostingAssembler")) {
            if (defaultAssembler.contains(forbidden)) {
                violations.add(defaultAssemblerPath + " still contains " + forbidden);
            }
        }

        for (String path : List.of(
                "core/src/main/java/com/wind/funds/route/RouteResolver.java",
                "core/src/main/java/com/wind/funds/route/spec/ResolvedRouteSpec.java",
                "core/src/main/java/com/wind/funds/route/spec/RouteLegSpec.java",
                "core/src/main/java/com/wind/funds/transaction/FundsInstructionOrchestrator.java")) {
            if (Files.readString(root.resolve(path)).contains("LedgerPostingAssembler")) {
                violations.add(path + " still describes LedgerPostingAssembler as a core responsibility");
            }
        }

        assertThat(violations)
                .as("ledger posting command surface must be owned by Ledger and hide assembled specs")
                .isEmpty();
    }

    @Test
    void testTransactionFaceShouldNotOwnProjectionExplanationRules() throws IOException {
        List<String> violations = new ArrayList<>();
        Path root = workspaceRoot();
        String faceSourcePath = "transaction/face/src/main/java/com/wind/funds/transaction/projection/"
                + "FundsTransactionProjectionExplanationSource.java";
        if (Files.exists(root.resolve(faceSourcePath))) {
            violations.add(faceSourcePath + " still exposes projection explanation rules");
        }

        String implSourcePath = "transaction/impl/src/main/java/com/wind/funds/transaction/projection/impl/"
                + "FundsTransactionProjectionExplanationSource.java";
        Path implSource = root.resolve(implSourcePath);
        if (Files.notExists(implSource)) {
            violations.add(implSourcePath + " is missing");
        } else if (Files.readString(implSource)
                .contains("public record FundsTransactionProjectionExplanationSource")) {
            violations.add(implSourcePath + " must be package-private");
        }

        String publishContextPath = "transaction/face/src/main/java/com/wind/funds/transaction/projection/"
                + "FundsTransactionProjectionPublishContext.java";
        String publishContext = Files.readString(root.resolve(publishContextPath));
        for (String forbidden : List.of(
                "FundsTransactionProjectionExplanationSource",
                "FundsTransactionProjectionExplanation explanation(",
                "resolveLedgerTransactionSn(")) {
            if (publishContext.contains(forbidden)) {
                violations.add(publishContextPath + " still contains " + forbidden);
            }
        }

        for (String requiredPath : List.of(
                "transaction/face/src/main/java/com/wind/funds/transaction/projection/"
                        + "FundsTransactionProjectionPublisher.java",
                publishContextPath,
                "transaction/face/src/main/java/com/wind/funds/transaction/projection/"
                        + "FundsTransactionProjectionExplanation.java")) {
            if (Files.notExists(root.resolve(requiredPath))) {
                violations.add(requiredPath + " must remain in transaction-face");
            }
        }

        assertThat(violations)
                .as("transaction-face must expose projection contracts without default explanation rules")
                .isEmpty();
    }

    private static final List<String> PRODUCTION_MODULE_POMS = List.of(
            "core/pom.xml",
            "fx/impl/pom.xml",
            "ledger/face/pom.xml",
            "ledger/impl/pom.xml",
            "transaction/face/pom.xml",
            "transaction/impl/pom.xml",
            "wallet/face/pom.xml",
            "wallet/impl/pom.xml",
            "reconciliation/face/pom.xml",
            "reconciliation/impl/pom.xml",
            "governance/face/pom.xml",
            "governance/impl/pom.xml");

    private static final List<String> PROJECT_POMS = Stream.concat(
            Stream.of(
                    "pom.xml",
                    "fx/pom.xml",
                    "ledger/pom.xml",
                    "wallet/pom.xml",
                    "transaction/pom.xml",
                    "reconciliation/pom.xml",
                    "governance/pom.xml",
                    "tests/pom.xml",
                    "dependencies/pom.xml"),
            PRODUCTION_MODULE_POMS.stream()).toList();

    private static final List<String> NON_TRANSACTION_PRODUCTION_JAVA_SOURCE_SCAN_PATHS = PRODUCTION_MODULE_POMS.stream()
            .filter(pomPath -> !pomPath.startsWith("transaction/"))
            .map(pomPath -> pomPath.substring(0, pomPath.length() - "pom.xml".length()) + "src/main/java")
            .toList();

    private static final List<String> ROOT_MODULES = List.of(
            "core",
            "fx",
            "ledger",
            "wallet",
            "transaction",
            "reconciliation",
            "governance",
            "tests",
            "dependencies");

    private static final Map<String, List<String>> CAPABILITY_AGGREGATE_MODULES = Map.of(
            "fx/pom.xml", List.of("impl"),
            "ledger/pom.xml", List.of("face", "impl"),
            "wallet/pom.xml", List.of("face", "impl"),
            "transaction/pom.xml", List.of("face", "impl"),
            "reconciliation/pom.xml", List.of("face", "impl"),
            "governance/pom.xml", List.of("face", "impl"));

    private static final List<String> FACE_MODULE_POMS = List.of(
            "ledger/face/pom.xml",
            "transaction/face/pom.xml",
            "wallet/face/pom.xml",
            "reconciliation/face/pom.xml",
            "governance/face/pom.xml");

    private static final List<String> FACE_ALLOWED_FUNDS_ARTIFACTS = List.of("wind-funds-core");

    private static final List<String> CORE_FORBIDDEN_ARTIFACTS = List.of(
            "wind-funds-fx-impl",
            "wind-funds-ledger-face",
            "wind-funds-ledger-impl",
            "wind-funds-transaction-face",
            "wind-funds-transaction-impl",
            "wind-funds-wallet-face",
            "wind-funds-wallet-impl",
            "wind-funds-reconciliation-face",
            "wind-funds-reconciliation-impl",
            "wind-funds-governance-face",
            "wind-funds-governance-impl",
            "wind-funds-tests");

    private static final String CORE_FORBIDDEN_SPRING_DEPENDENCY_GROUP_TOKEN =
            "<groupId>org.springframework";

    private static final List<String> CORE_FORBIDDEN_SPRING_IMPORT_TOKENS = List.of(
            "import org.springframework.");

    private static final Map<String, List<String>> MODULE_FORBIDDEN_ARTIFACTS = Map.of(
            "fx/impl/pom.xml", List.of(
                    "wind-funds-ledger-face",
                    "wind-funds-ledger-impl",
                    "wind-funds-transaction-face",
                    "wind-funds-transaction-impl",
                    "wind-funds-wallet-face",
                    "wind-funds-wallet-impl",
                    "wind-funds-reconciliation-face",
                    "wind-funds-reconciliation-impl",
                    "wind-funds-governance-face",
                    "wind-funds-governance-impl"),
            "ledger/impl/pom.xml", List.of(
                    "wind-funds-wallet-face",
                    "wind-funds-wallet-impl",
                    "wind-funds-transaction-face",
                    "wind-funds-transaction-impl"),
            "transaction/impl/pom.xml", List.of(
                    "wind-funds-ledger-face",
                    "wind-funds-ledger-impl"),
            "wallet/impl/pom.xml", List.of(
                    "wind-funds-transaction-face",
                    "wind-funds-transaction-impl"));

    private static final Map<String, List<String>> MODULE_FORBIDDEN_SOURCE_PACKAGE_PATHS = Map.of(
            "ledger/impl/src/main/java", List.of(
                    "com/wind/funds/transaction",
                    "com/wind/funds/wallet"),
            "transaction/impl/src/main/java", List.of(
                    "com/wind/funds/fx",
                    "com/wind/funds/ledger",
                    "com/wind/funds/wallet/application",
                    "com/wind/funds/wallet/dal",
                    "com/wind/funds/wallet/mapstruct"),
            "wallet/impl/src/main/java", List.of(
                    "com/wind/funds/transaction/converter",
                    "com/wind/funds/transaction/dal",
                    "com/wind/funds/transaction/ledger",
                    "com/wind/funds/transaction/mapstruct"));

    private static final List<String> PACKAGE_GUARD_SCAN_PATHS = List.of(
            "AGENTS.md",
            "core/src/main/java",
            "fx/impl/src/main/java",
            "ledger/face/src/main/java",
            "ledger/impl/src/main/java",
            "transaction/face/src/main/java",
            "transaction/impl/src/main/java",
            "wallet/face/src/main/java",
            "wallet/impl/src/main/java",
            "reconciliation/face/src/main/java",
            "reconciliation/impl/src/main/java",
            "governance/face/src/main/java",
            "governance/impl/src/main/java",
            "tests/src/test/java",
            "docs");

    private static final List<String> FUNDS_JAVA_PACKAGE_SCAN_PATHS = List.of(
            "core/src/main/java",
            "fx/impl/src/main/java",
            "ledger/face/src/main/java",
            "ledger/impl/src/main/java",
            "transaction/face/src/main/java",
            "transaction/impl/src/main/java",
            "wallet/face/src/main/java",
            "wallet/impl/src/main/java",
            "reconciliation/face/src/main/java",
            "reconciliation/impl/src/main/java",
            "governance/face/src/main/java",
            "governance/impl/src/main/java",
            "tests/src/test/java");

    private static final List<String> NON_CANONICAL_FUNDS_PACKAGE_TOKENS = List.of(
            String.join(".", "com", "wind", "integration", "funds"),
            String.join("/", "com", "wind", "integration", "funds"));

    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("(?m)^package\\s+([^;]+);");

    private static final List<String> CORE_OWNERLESS_PACKAGE_PREFIXES = List.of(
            "com.wind.funds.spec",
            "com.wind.funds.model");

    private static final Pattern CORE_PUBLIC_IMMUTABLE_TYPE_DECLARATION = Pattern.compile(
            "(?m)^public\\s+(?:final\\s+)?(?:class|record)\\s+Immutable");

    private static final List<String> TRANSACTION_SOURCE_SCAN_PATHS = List.of(
            "transaction/face/src/main/java",
            "transaction/impl/src/main/java");

    private static final List<String> TRANSACTION_IMPL_WALLET_APPLICATION_IMPL_SCAN_PATHS = List.of(
            "transaction/impl/src/main/java/com/wind/funds/wallet/application");

    private static final List<String> WALLET_APPLICATION_IMPL_SCAN_PATHS = List.of(
            "wallet/impl/src/main/java/com/wind/funds/wallet/application");

    private static final String TRANSACTION_PAYMENT_INSTRUMENT_AUTHORIZATION_PROCESSOR_SOURCE =
            "transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/impl/"
                    + "PaymentInstrumentAuthorizationProcessor.java";

    private static final List<String> TRANSACTION_FORBIDDEN_SPEND_CONTROL_ADMISSION_TOKENS = List.of(
            "com.wind.funds.wallet.application.spend.SpendControlAdmissionApplicationService");

    private static final Map<String, List<String>> TRANSACTION_FORBIDDEN_SPEND_RULE_TOKENS = Map.of(
            "wallet spend rule evaluation or ownership service", List.of(
                    "com.wind.funds.wallet.application.spend.SpendRuleEvaluationApplicationService",
                    "com.wind.funds.wallet.service.SpendRuleDefinitionService",
                    "com.wind.funds.wallet.service.SpendRuleVersionService",
                    "com.wind.funds.wallet.service.SpendRuleBindingService",
                    "com.wind.funds.wallet.service.SpendRuleDecisionRecordService"),
            "wallet spend rule entity or mapper", List.of(
                    "com.wind.funds.wallet.dal.entities.SpendRule",
                    "com.wind.funds.wallet.dal.entities.SpendControlMovement",
                    "com.wind.funds.wallet.dal.mapper.SpendRule",
                    "com.wind.funds.wallet.dal.mapper.SpendControlMovement"),
            "wallet spend control projection model", List.of(
                    "BudgetControlProjectionDTO",
                    "BudgetControlProjectionQuery"));

    private static final List<String> WALLET_APPLICATION_FORBIDDEN_TRANSACTION_TOKENS = List.of(
            "com.wind.funds.transaction.application.",
            "com.wind.funds.transaction.model.",
            "com.wind.funds.transaction.services.",
            "com.wind.funds.transaction.FundsInstructionOrchestrator",
            "com.wind.funds.transaction.converter.");

    private static final List<String> LEDGER_DANGEROUS_CALL_SCAN_PATHS = List.of(
            "transaction/face/src/main/java",
            "transaction/impl/src/main/java",
            "wallet/face/src/main/java",
            "wallet/impl/src/main/java",
            "reconciliation/face/src/main/java",
            "reconciliation/impl/src/main/java",
            "governance/face/src/main/java",
            "governance/impl/src/main/java");

    private static final List<String> LEDGER_DANGEROUS_FACE_CALL_TOKENS = List.of(
            ".updateLedgerBalance(",
            ".deleteLedgerById(",
            ".deleteLedgerByIds(");

    private static final List<String> NON_WALLET_PRODUCTION_SOURCE_SCAN_PATHS = List.of(
            "ledger/face/src/main/java",
            "ledger/impl/src/main/java",
            "transaction/face/src/main/java",
            "transaction/impl/src/main/java",
            "reconciliation/face/src/main/java",
            "reconciliation/impl/src/main/java",
            "governance/face/src/main/java",
            "governance/impl/src/main/java");

    private static final Map<String, List<String>> WALLET_RESOURCE_SERVICE_BYPASS_TOKENS = Map.of(
            "payment instrument resource service", List.of(
                    "com.wind.funds.wallet.service.*",
                    "com.wind.funds.wallet.service.PaymentInstrumentService",
                    "com.wind.funds.wallet.service.PaymentInstrumentBindingService",
                    "com.wind.funds.wallet.service.PaymentInstrumentBindingHistoryService"),
            "funding responsibility resource service", List.of(
                    "com.wind.funds.wallet.service.SpendSubjectFundingRelationService"),
            "wallet account resource service", List.of(
                    "com.wind.funds.wallet.service.FundingAccountService",
                    "com.wind.funds.wallet.service.CreditAccountService",
                    "com.wind.funds.wallet.service.SpendControlScopeService"));

    private static final List<String> CLEARING_TRANSACTION_PRIMITIVE_SOURCE_ALLOWLIST = List.of(
            "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/clearing/impl/"
                    + "ClearingBatchApplicationServiceImpl.java");

    private static final List<String> CLEARING_TRANSACTION_PRIMITIVE_TOKENS = List.of(
            "FundsClearingTransactionService",
            "FundsClearingConfirmRequest");

    private static final List<String> SETTLEMENT_TRANSACTION_PRIMITIVE_SOURCE_ALLOWLIST = List.of(
            "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/settlement/impl/"
                    + "SettlementOrderApplicationServiceImpl.java");

    private static final List<String> SETTLEMENT_TRANSACTION_PRIMITIVE_TOKENS = List.of(
            "FundsSettlementTransactionService",
            "FundsSettlementLockRequest");

    private static final List<String> PAYOUT_TRANSACTION_PRIMITIVE_SOURCE_ALLOWLIST = List.of(
            "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/payout/impl/"
                    + "PayoutOrderApplicationServiceImpl.java");

    private static final List<String> PAYOUT_TRANSACTION_PRIMITIVE_TOKENS = List.of(
            "FundsPayoutTransactionService",
            "FundsPayoutRequest");

    private static final List<String> DIRECT_TRANSACTION_COMMAND_TOKENS = List.of(
            "FundsDirectTransactionService",
            "FundsTransactionPayRequest");

    /**
     * 场景：core 承载资金 DSL、枚举、值对象和端口契约。
     * 预期：core 不依赖任一业务 face/impl、测试模块或 DAL 基础设施。
     * 红线：核心语义不能反向依赖外围模块或实现细节。
     */
    @Test
    void testCoreShouldNotDependOnOuterModules() throws Exception {
        assertThat(dependencyArtifactIds(workspaceRoot().resolve("core/pom.xml")))
                .doesNotContain(CORE_FORBIDDEN_ARTIFACTS.toArray(String[]::new));
    }

    /**
     * 场景：资金项目按业务能力组织 Maven 模块。
     * 预期：根 POM 只声明稳定能力模块，各能力聚合 POM 自行管理 face/impl 子模块。
     * 红线：根 POM 不直接枚举两级实现路径，避免业务能力内部结构泄漏到项目根。
     */
    @Test
    void testRootShouldDelegateSubmodulesToCapabilityAggregators() throws Exception {
        assertThat(modulePaths(workspaceRoot().resolve("pom.xml")))
                .containsExactlyElementsOf(ROOT_MODULES);
        for (Map.Entry<String, List<String>> entry : CAPABILITY_AGGREGATE_MODULES.entrySet()) {
            assertThat(modulePaths(workspaceRoot().resolve(entry.getKey())))
                    .as("capability aggregator %s should own its submodules", entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }
    }

    /**
     * 场景：core 承载资金 DSL、枚举、值对象和端口契约。
     * 预期：core POM 不声明 Spring 依赖，源码不依赖 Spring 运行时类型。
     * 红线：核心契约、值对象和校验规则不能依赖具体容器或框架工具。
     */
    @Test
    void testCoreShouldNotDependOnSpringRuntime() throws Exception {
        List<String> violations = new ArrayList<>();
        String corePom = Files.readString(workspaceRoot().resolve("core/pom.xml"));
        if (corePom.contains(CORE_FORBIDDEN_SPRING_DEPENDENCY_GROUP_TOKEN)) {
            violations.add("core/pom.xml declares a Spring dependency group");
        }
        for (Path javaFile : javaSourceFiles(List.of("core/src/main/java"))) {
            String content = Files.readString(javaFile);
            for (String forbiddenToken : CORE_FORBIDDEN_SPRING_IMPORT_TOKENS) {
                if (content.contains(forbiddenToken)) {
                    violations.add(workspaceRoot().relativize(javaFile)
                            + " contains runtime Spring component token " + forbiddenToken);
                }
            }
        }

        assertThat(violations)
                .as("core must keep Spring runtime dependencies outside core")
                .isEmpty();
    }

    /**
     * 场景：core 作为跨能力资金契约内核持续演进。
     * 预期：契约按 transaction、route、ledger、wallet、fx 能力归属，默认实现留在实现模块。
     * 红线：不得恢复无归属的顶层 spec/model 包，也不得把 public Immutable 实现重新暴露到 core。
     */
    @Test
    void testCoreDslTypesShouldStayCapabilityOwnedAndImplementationFree() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : javaSourceFiles(List.of("core/src/main/java"))) {
            String content = Files.readString(javaFile);
            Matcher packageMatcher = PACKAGE_DECLARATION.matcher(content);
            if (packageMatcher.find() && CORE_OWNERLESS_PACKAGE_PREFIXES.stream()
                    .anyMatch(packageMatcher.group(1)::startsWith)) {
                violations.add(workspaceRoot().relativize(javaFile)
                        + " uses ownerless package " + packageMatcher.group(1));
            }
            if (CORE_PUBLIC_IMMUTABLE_TYPE_DECLARATION.matcher(content).find()) {
                violations.add(workspaceRoot().relativize(javaFile)
                        + " exposes a default Immutable implementation from core");
            }
        }

        assertThat(violations)
                .as("core DSL contracts must stay capability-owned and implementation-free")
                .isEmpty();
    }

    /**
     * 场景：各业务 face 模块作为跨模块契约入口。
     * 预期：face 只依赖资金 core，不依赖任一 impl、同级 face 或测试模块。
     * 红线：对外契约不能泄漏实现模块、其他能力域契约或测试资产。
     */
    @Test
    void testFaceModulesShouldOnlyDependOnCoreWithinFundsGroup() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String pomPath : FACE_MODULE_POMS) {
            for (String artifactId : dependencyArtifactIds(workspaceRoot().resolve(pomPath))) {
                if (artifactId.startsWith("wind-funds-") && !FACE_ALLOWED_FUNDS_ARTIFACTS.contains(artifactId)) {
                    violations.add(pomPath + " depends on " + artifactId);
                }
            }
        }

        assertThat(violations)
                .as("face module dependencies must stay on core contracts")
                .isEmpty();
    }

    /**
     * 场景：生产模块参与资金主链路构建。
     * 预期：生产模块不得依赖 tests 聚合模块。
     * 红线：测试夹具、测试支撑或测试配置不能进入生产模块依赖图。
     */
    @Test
    void testProductionModulesShouldNotDependOnTestsModule() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String pomPath : PRODUCTION_MODULE_POMS) {
            if (dependencyArtifactIds(workspaceRoot().resolve(pomPath)).contains("wind-funds-tests")) {
                violations.add(pomPath + " depends on wind-funds-tests");
            }
        }

        assertThat(violations)
                .as("production modules must not depend on tests module")
                .isEmpty();
    }

    /**
     * 场景：生产模块按当前已固化的 Maven 边界协作。
     * 预期：ledger-impl 不反向依赖交易/钱包，transaction-impl 不直连 ledger-face/impl，wallet-impl 不反向依赖 transaction-face/impl。
     * 红线：实现模块不能绕开上层应用契约直连下游实现细节。
     */
    @Test
    void testFundsProductionDependencyChainShouldStayOneWay() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : MODULE_FORBIDDEN_ARTIFACTS.entrySet()) {
            List<String> artifactIds = dependencyArtifactIds(workspaceRoot().resolve(entry.getKey()));
            for (String forbiddenArtifactId : entry.getValue()) {
                if (artifactIds.contains(forbiddenArtifactId)) {
                    violations.add(entry.getKey() + " depends on " + forbiddenArtifactId);
                }
            }
        }

        assertThat(violations)
                .as("funds production dependency boundaries must stay one-way")
                .isEmpty();
    }

    /**
     * 场景：实现模块的源码包归属也必须符合 `transaction -> wallet -> ledger` 分层。
     * 预期：ledger-impl 不承载 transaction/wallet 源码包，transaction-impl 和 wallet-impl 不承载下游内部实现包。
     * 红线：不能只靠 Maven 依赖约束，遗漏源码路径层面的模块越界。
     */
    @Test
    void testImplModulesShouldNotOwnOtherModuleSourcePackages() throws Exception {
        List<String> violations = new ArrayList<>();
        Path root = workspaceRoot();
        for (Map.Entry<String, List<String>> entry : MODULE_FORBIDDEN_SOURCE_PACKAGE_PATHS.entrySet()) {
            for (String forbiddenPackagePath : entry.getValue()) {
                Path packagePath = root.resolve(entry.getKey()).resolve(forbiddenPackagePath);
                if (containsJavaSource(packagePath)) {
                    violations.add(entry.getKey() + " owns forbidden source package " + forbiddenPackagePath);
                }
            }
        }

        assertThat(violations)
                .as("implementation modules must not own source packages from other modules")
                .isEmpty();
    }

    /**
     * 场景：资金域包根已经统一为 `com.wind.funds`。
     * 预期：源码、测试、规格和项目级指令不再引入旧资金域包根。
     * 红线：不得恢复旧 Wind integration funds 资金域包根。
     */
    @Test
    void testFundsPackageRootShouldStayUnderWindFunds() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path textFile : packageGuardTextFiles()) {
            String content = Files.readString(textFile);
            for (String forbiddenToken : NON_CANONICAL_FUNDS_PACKAGE_TOKENS) {
                if (content.contains(forbiddenToken)) {
                    violations.add(workspaceRoot().relativize(textFile) + " contains " + forbiddenToken);
                }
            }
        }

        assertThat(violations)
                .as("funds package root must stay under com.wind.funds")
                .isEmpty();
    }

    /**
     * 场景：资金域 Java 包根已经从历史包迁移到 `com.wind.funds`。
     * 预期：源码和测试的 Java `package` 声明均使用 `com.wind.funds` 包根。
     * 红线：不得只靠禁止旧包名文本，遗漏新文件落入其他资金域包根。
     */
    @Test
    void testFundsJavaPackageDeclarationsShouldUseWindFundsRoot() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : fundsJavaSourceFiles()) {
            Matcher matcher = PACKAGE_DECLARATION.matcher(Files.readString(javaFile));
            if (!matcher.find()) {
                violations.add(workspaceRoot().relativize(javaFile) + " misses package declaration");
                continue;
            }
            String packageName = matcher.group(1).trim();
            if (!"com.wind.funds".equals(packageName) && !packageName.startsWith("com.wind.funds.")) {
                violations.add(workspaceRoot().relativize(javaFile) + " declares " + packageName);
            }
        }

        assertThat(violations)
                .as("funds Java package declarations must stay under com.wind.funds")
                .isEmpty();
    }

    /**
     * 场景：Spend Rule 主能力归属于钱包支出控制域，交易模块只通过 wallet-face 契约协作。
     * 预期：transaction 生产源码不得直接依赖 wallet 的 Spend Rule 实体、Mapper 或预算控制投影模型。
     * 红线：除支付工具授权 facade 固化准入证据外，交易内核不能越过 wallet-face 计算、更新或查询当前 Spend Rule。
     */
    @Test
    void testTransactionShouldOnlyConsumeSpendRuleSnapshotsWithoutWalletRuleOwnership() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : transactionJavaSourceFiles()) {
            String content = Files.readString(javaFile);
            String relativePath = workspaceRoot().relativize(javaFile).toString();
            for (String forbiddenToken : TRANSACTION_FORBIDDEN_SPEND_CONTROL_ADMISSION_TOKENS) {
                if (content.contains(forbiddenToken)
                        && !TRANSACTION_PAYMENT_INSTRUMENT_AUTHORIZATION_PROCESSOR_SOURCE.equals(relativePath)) {
                    violations.add(relativePath
                            + " contains wallet spend control admission token " + forbiddenToken);
                }
            }
            for (Map.Entry<String, List<String>> forbiddenEntry : TRANSACTION_FORBIDDEN_SPEND_RULE_TOKENS.entrySet()) {
                for (String forbiddenToken : forbiddenEntry.getValue()) {
                    if (content.contains(forbiddenToken)) {
                        violations.add(relativePath
                                + " contains " + forbiddenEntry.getKey() + " token " + forbiddenToken);
                    }
                }
            }
        }

        assertThat(violations)
                .as("transaction modules must not own wallet Spend Rule capabilities")
                .isEmpty();
    }

    /**
     * 场景：transaction-impl 可以依赖 wallet-face，但不能把源码包伪装成 wallet 实现。
     * 预期：需要交易内核的 wallet facade 实现放在 transaction 包下，不能放入 com.wind.funds.wallet.application.impl。
     * 红线：不能用源码位置绕开 Maven 依赖方向和模块归属。
     */
    @Test
    void testTransactionImplShouldNotContainWalletApplicationImplementations() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : transactionImplWalletApplicationImplSourceFiles()) {
            String relativePath = workspaceRoot().relativize(javaFile).toString();
            if (relativePath.contains("/impl/")) {
                violations.add(relativePath);
            }
        }

        assertThat(violations)
                .as("transaction implementations must not own wallet source packages")
                .isEmpty();
    }

    /**
     * 场景：wallet-impl 只承载钱包资源、准入和控制事实能力。
     * 预期：wallet application impl 不依赖 transaction-face 服务、请求、DTO 或内部流水线。
     * 红线：创建或查询交易事实的用例实现必须归属 transaction-impl。
     */
    @Test
    void testWalletApplicationShouldNotUseTransactionContractsOrInternalPipeline() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : walletApplicationImplSourceFiles()) {
            String content = Files.readString(javaFile);
            for (String forbiddenToken : WALLET_APPLICATION_FORBIDDEN_TRANSACTION_TOKENS) {
                if (content.contains(forbiddenToken)) {
                    violations.add(workspaceRoot().relativize(javaFile)
                            + " contains transaction token " + forbiddenToken);
                }
            }
        }

        assertThat(violations)
                .as("wallet application implementations must not depend on transaction contracts")
                .isEmpty();
    }

    /**
     * 场景：外部资金事件消费会生成标准交易和账本事实。
     * 预期：公共契约归属 transaction-face，不再放在 wallet-face 的 application 入口下。
     * 红线：wallet application 只解释支付工具、账户能力和支出控制，不承载交易事实消费入口。
     */
    @Test
    void testExternalFundsEventContractShouldBelongToTransactionFace() {
        Path root = workspaceRoot();

        assertThat(root.resolve("transaction/face/src/main/java/com/wind/funds/transaction/application/"
                + "ExternalFundsEventApplicationService.java")).exists();
        assertThat(root.resolve("transaction/face/src/main/java/com/wind/funds/transaction/model/request/"
                + "ConsumeExternalFundsEventRequest.java")).exists();
        assertThat(root.resolve("wallet/face/src/main/java/com/wind/funds/wallet/application/external/"
                + "ExternalFundsEventApplicationService.java")).doesNotExist();
        assertThat(root.resolve("wallet/face/src/main/java/com/wind/funds/wallet/model/request/"
                + "ConsumeExternalFundsEventRequest.java")).doesNotExist();
    }

    /**
     * 场景：ledger face 中历史资源型写接口仍处于兼容期。
     * 预期：wallet、transaction、reconciliation 和 governance 不直接调用余额直改、账本交易更新或删除能力。
     * 红线：跨模块生产调用方必须通过 ledger application facade 或交易事实 / 账本分录链路。
     */
    @Test
    void testOuterModulesShouldNotCallDangerousLedgerFaceMutationMethods() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : ledgerDangerousCallScanJavaSourceFiles()) {
            String content = Files.readString(javaFile);
            for (String forbiddenToken : LEDGER_DANGEROUS_FACE_CALL_TOKENS) {
                if (content.contains(forbiddenToken)) {
                    violations.add(workspaceRoot().relativize(javaFile)
                            + " contains dangerous ledger face call " + forbiddenToken);
                }
            }
        }

        assertThat(violations)
                .as("outer modules must not bypass ledger application or posting chains")
                .isEmpty();
    }

    /**
     * 场景：公共账本契约只能创建、查询和变更状态，余额由不可变分录投影产生。
     * 预期：LedgerService 不再暴露余额直改或删除账本能力。
     * 红线：任何调用方都不能绕过 posting 与 projection 链路改写账本事实。
     */
    @Test
    void testLedgerServiceShouldNotExposeDirectBalanceMutationOrDeletion() throws IOException {
        List<String> violations = new ArrayList<>();
        List<String> methodNames = Stream.of(LedgerService.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .toList();

        for (String forbiddenMethod : List.of("updateLedgerBalance", "deleteLedgerById", "deleteLedgerByIds")) {
            if (methodNames.contains(forbiddenMethod)) {
                violations.add("LedgerService still exposes " + forbiddenMethod);
            }
        }
        if (methodNames.stream().filter("initializeRequiredLedgers"::equals).count() != 1) {
            violations.add("LedgerService must expose exactly one controlled initialization command");
        }

        for (String retiredPath : List.of(
                "core/src/main/java/com/wind/funds/ledger/spec/LedgerProfileSpec.java",
                "core/src/main/java/com/wind/funds/ledger/spec/LedgerProfileItemSpec.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/service/LedgerProfileService.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/service/SubjectLedgerInitializer.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/dto/LedgerProfileDTO.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/dto/LedgerProfileItemDTO.java",
                "wallet/face/src/main/java/com/wind/funds/wallet/model/request/InitializeSubjectLedgerRequest.java",
                "wallet/impl/src/main/java/com/wind/funds/wallet/services/impl/DefaultLedgerProfileServiceImpl.java",
                "wallet/impl/src/main/java/com/wind/funds/wallet/services/impl/DefaultSubjectLedgerInitializer.java")) {
            if (Files.exists(workspaceRoot().resolve(retiredPath))) {
                violations.add(retiredPath + " still exists");
            }
        }

        String stableApi = Files.readString(workspaceRoot().resolve("core/api-baseline/stable-api.txt"));
        for (String retiredType : List.of("LedgerProfileSpec", "LedgerProfileItemSpec")) {
            if (stableApi.contains(retiredType)) {
                violations.add("core stable API still exposes " + retiredType);
            }
        }

        List<String> productionPaths = List.of(
                "core/src/main/java",
                "ledger/face/src/main/java",
                "ledger/impl/src/main/java",
                "wallet/face/src/main/java",
                "wallet/impl/src/main/java",
                "transaction/face/src/main/java",
                "transaction/impl/src/main/java");
        for (Path source : javaSourceFiles(productionPaths)) {
            String content = Files.readString(source);
            for (String retiredToken : List.of(
                    "LedgerProfileService",
                    "SubjectLedgerInitializer",
                    "com.wind.funds.wallet.model.request.InitializeSubjectLedgerRequest")) {
                if (content.contains(retiredToken)) {
                    violations.add(workspaceRoot().relativize(source) + " still references " + retiredToken);
                }
            }
            if (source.startsWith(workspaceRoot().resolve("transaction"))
                    && (content.contains("com.wind.funds.ledger.service")
                    || content.contains("com.wind.funds.ledger.request"))) {
                violations.add(workspaceRoot().relativize(source) + " introduces forbidden transaction -> ledger-face dependency");
            }
            for (String compatibilityToken : List.of(
                    "LedgerProfileAdmissionService",
                    "LedgerProfileServiceV2",
                    "SubjectLedgerInitializerAdapter",
                    "LegacyLedgerProfile")) {
                if (content.contains(compatibilityToken)) {
                    violations.add(workspaceRoot().relativize(source) + " introduces " + compatibilityToken);
                }
            }
        }

        assertThat(violations)
                .as("Ledger profile ownership must migrate once without compatibility surfaces")
                .isEmpty();
    }

    /**
     * 场景：生产模块通过 MapStruct Converter 转换持久化模型和契约模型。
     * 预期：所有 Converter 都使用严格目标字段策略，并以 convertToXxx 命名转换方法。
     * 红线：新增目标字段不得只产生告警，Converter 不得继续使用语义过弱的 toDTO 方法名。
     */
    @Test
    void testMapStructConvertersShouldUseStrictPolicyAndExplicitMethodNames() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path converter : javaSourceFiles(List.of(
                "ledger/impl/src/main/java/com/wind/funds/ledger/mapstruct",
                "wallet/impl/src/main/java/com/wind/funds/wallet/mapstruct",
                "transaction/impl/src/main/java/com/wind/funds/transaction/mapstruct",
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/mapstruct"))) {
            if (!converter.getFileName().toString().endsWith("Converter.java")) {
                continue;
            }
            String source = Files.readString(converter);
            Path relativePath = workspaceRoot().relativize(converter);
            if (!source.contains("unmappedTargetPolicy = ReportingPolicy.ERROR")) {
                violations.add(relativePath + " does not fail on unmapped target properties");
            }
            if (Pattern.compile("\\btoDTO\\s*\\(").matcher(source).find()) {
                violations.add(relativePath + " still uses toDTO");
            }
        }

        assertThat(violations)
                .as("MapStruct converters must fail fast and name their target model explicitly")
                .isEmpty();
    }

    /**
     * 场景：生产 Mapper 使用自定义 SQL 承载锁定查询或复杂查询。
     * 预期：保留自定义方法和 SQL 语义，同时显式声明查询列。
     * 红线：生产查询不得使用 SELECT * 隐式扩大持久化投影。
     */
    @Test
    void testProductionMapperQueriesShouldUseExplicitColumnProjection() throws IOException {
        Pattern selectAllPattern = Pattern.compile(
                "(?i)\\bselect\\s+(?:distinct\\s+)?(?:[a-z_][a-z0-9_]*\\s*\\.\\s*)?\\*(?:\\s*,|\\s+from)");
        assertThat(selectAllPattern.matcher("SELECT t.* FROM t_funds_transaction t").find()).isTrue();
        List<String> violations = new ArrayList<>();
        for (Path mapper : javaSourceFiles(List.of(
                "ledger/impl/src/main/java/com/wind/funds/ledger/dal/mapper",
                "wallet/impl/src/main/java/com/wind/funds/wallet/dal/mapper",
                "transaction/impl/src/main/java/com/wind/funds/transaction/dal/mapper",
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/mapper",
                "governance/impl/src/main/java/com/wind/funds/governance/dal/mapper"))) {
            String source = Files.readString(mapper);
            Matcher matcher = selectAllPattern.matcher(source);
            if (matcher.find()) {
                violations.add(workspaceRoot().relativize(mapper).toString());
            }
        }

        assertThat(violations)
                .as("Custom mapper SQL must keep explicit column projections")
                .isEmpty();
    }

    /**
     * 场景：持久化模型、数据库列和公共契约表达生命周期、结果或布尔属性。
     * 预期：业务属性和物理列使用同一业务名，只保留数据库 is_* 与 Java 布尔属性的约规差异。
     * 红线：不得用 status/state 双命名、SQL 别名或桥接 setter 掩盖映射不一致。
     */
    @Test
    void testPersistencePropertiesShouldUseCanonicalBusinessNames() throws IOException {
        List<String> entityRoots = List.of(
                "ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities",
                "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities",
                "transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities",
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities",
                "governance/impl/src/main/java/com/wind/funds/governance/dal/entities");
        Pattern explicitColumn = Pattern.compile(
                "@Column\\(\"([^\"]+)\"\\)\\s+private\\s+[^;=]+\\s+(\\w+)\\s*;");
        Pattern bridgeSetter = Pattern.compile(
                "\\bvoid\\s+(setStatus|setReconciliationDecisionStatus)\\s*\\(");
        Map<String, String> mismatches = new LinkedHashMap<>();
        List<String> bridgeSetters = new ArrayList<>();
        for (Path entity : javaSourceFiles(entityRoots)) {
            String source = Files.readString(entity);
            Matcher matcher = explicitColumn.matcher(source);
            while (matcher.find()) {
                String column = matcher.group(1);
                String field = matcher.group(2);
                if (!column.equals(toSnakeCase(field))) {
                    mismatches.put(workspaceRoot().relativize(entity) + "#" + field, column);
                }
            }
            if (bridgeSetter.matcher(source).find()) {
                bridgeSetters.add(workspaceRoot().relativize(entity).toString());
            }
        }

        assertThat(mismatches).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities/Ledger.java#allowNegative",
                "is_allow_negative",
                "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/FundingAccount.java#platform",
                "is_platform",
                "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/PaymentInstrumentBinding.java#defaultBinding",
                "is_default"));
        assertThat(bridgeSetters).isEmpty();

        assertThat(Stream.of(ProjectionReplayTask.class.getDeclaredMethods()).map(Method::getName))
                .contains("getState", "setState")
                .doesNotContain("getStatus", "setStatus");
        assertThat(Stream.of(FundsProjectionReplayTaskDTO.class.getRecordComponents())
                .map(component -> component.getName()))
                .contains("state")
                .doesNotContain("status");
        String mapperSource = Files.readString(workspaceRoot().resolve(
                "governance/impl/src/main/java/com/wind/funds/governance/dal/mapper/ProjectionReplayTaskMapper.java"));
        assertThat(mapperSource)
                .contains("checkpoint_value, state, success_count")
                .contains("AND state IN ('CREATED', 'RUNNING')")
                .doesNotContain("status AS state", "checkpoint_value, status, success_count", "AND status IN");
    }

    /**
     * 场景：wallet 对外提供支付工具授权、收款和账户能力 application facade。
     * 预期：非 wallet 生产模块不直接拼装 wallet 资源服务完成授权准入、支付工具能力或资金责任解析。
     * 红线：跨模块调用方必须通过 wallet application/use-case facade 或交易层 canonical 入口表达业务意图。
     */
    @Test
    void testNonWalletModulesShouldNotComposeWalletResourceServicesDirectly() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : nonWalletProductionJavaSourceFiles()) {
            String content = Files.readString(javaFile);
            for (Map.Entry<String, List<String>> forbiddenEntry : WALLET_RESOURCE_SERVICE_BYPASS_TOKENS.entrySet()) {
                for (String forbiddenToken : forbiddenEntry.getValue()) {
                    if (content.contains(forbiddenToken)) {
                        violations.add(workspaceRoot().relativize(javaFile)
                                + " contains " + forbiddenEntry.getKey() + " token " + forbiddenToken);
                    }
                }
            }
        }

        assertThat(violations)
                .as("non-wallet modules must use wallet application facades instead of resource services")
                .isEmpty();
    }

    /**
     * 场景：清算批次通过交易层内部资金原语完成 {@code CLEARING -> AVAILABLE}。
     * 预期：生产源码只有清算批次编排可以消费该原语，宿主和其他模块通过清算批次应用服务协作。
     * 红线：不得绕过批次锁定、最终 Gate 和来源事实复核直接生成清算资金事实。
     */
    @Test
    void testClearingFundsPrimitiveShouldOnlyBeUsedByClearingBatchOrchestration() throws Exception {
        assertFundsPrimitiveAllowlist(CLEARING_TRANSACTION_PRIMITIVE_SOURCE_ALLOWLIST,
                CLEARING_TRANSACTION_PRIMITIVE_TOKENS, "clearing");
    }

    /**
     * 场景：结算单通过交易层内部资金原语完成 {@code AVAILABLE -> SETTLEMENT}。
     * 预期：生产源码只有结算单编排可以消费该原语，宿主通过结算单公共应用服务协作。
     * 红线：不得绕过结算状态机、最终 Gate 和来源复核直接锁定结算资金。
     */
    @Test
    void testSettlementFundsPrimitiveShouldOnlyBeUsedBySettlementOrderOrchestration() throws Exception {
        assertFundsPrimitiveAllowlist(SETTLEMENT_TRANSACTION_PRIMITIVE_SOURCE_ALLOWLIST,
                SETTLEMENT_TRANSACTION_PRIMITIVE_TOKENS, "settlement");
    }

    /**
     * 场景：出款单通过交易层内部资金原语完成成功关闭或失败回退。
     * 预期：生产源码只有出款单编排可以消费该原语，宿主通过出款单公共应用服务协作。
     * 红线：不得绕过出款状态机、回单幂等和外部引用唯一性直接生成出款资金事实。
     */
    @Test
    void testPayoutFundsPrimitiveShouldOnlyBeUsedByPayoutOrderOrchestration() throws Exception {
        assertFundsPrimitiveAllowlist(PAYOUT_TRANSACTION_PRIMITIVE_SOURCE_ALLOWLIST,
                PAYOUT_TRANSACTION_PRIMITIVE_TOKENS, "payout");
    }

    /**
     * 场景：reconciliation 编排清分、清算、结算和出款事实。
     * 预期：只消费对应阶段的专用资金原语，不直接发起普通支付。
     * 红线：清算模块不得绕过宿主清分事实和标准交易入口创建初始支付。
     */
    @Test
    void testReconciliationShouldNotUseGenericDirectTransactionCommands() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : javaSourceFiles(List.of("reconciliation/impl/src/main/java"))) {
            String content = Files.readString(javaFile);
            for (String forbiddenToken : DIRECT_TRANSACTION_COMMAND_TOKENS) {
                if (content.contains(forbiddenToken)) {
                    violations.add(workspaceRoot().relativize(javaFile)
                            + " contains generic direct transaction command " + forbiddenToken);
                }
            }
        }

        assertThat(violations)
                .as("reconciliation must consume existing funds facts or stage-specific transaction primitives")
                .isEmpty();
    }

    /**
     * 场景：两个外部资金事件并发消费同一个资金事实，唯一键失败方需要读取已提交胜者。
     * 预期：冲突恢复使用 current read，避免 MySQL REPEATABLE-READ 继续命中旧快照。
     * 红线：唯一键失败后不得继续使用普通一致性读判断胜者不存在。
     */
    @Test
    void testExternalFundsFactConflictRecoveryShouldUseCurrentRead() throws Exception {
        String mapper = Files.readString(workspaceRoot().resolve(
                "transaction/impl/src/main/java/com/wind/funds/transaction/dal/mapper/FundsTransactionMapper.java"));
        String service = Files.readString(workspaceRoot().resolve(
                "transaction/impl/src/main/java/com/wind/funds/transaction/services/impl/"
                        + "DefaultFundsInstructionLifecycleSaver.java"));

        assertThat(mapper).containsSubsequence(
                "AND external_funds_effect_type = #{externalFundsEffectType}",
                "FOR UPDATE",
                "FundsTransaction selectByExternalFundsFactForUpdate(");
        assertThat(service).contains("fundsTransactionMapper.selectByExternalFundsFactForUpdate(");
    }

    /**
     * 场景：两个出款单并发消费同一个外部回单引用，唯一键失败方需要读取已提交胜者。
     * 预期：冲突恢复使用 current read，避免 MySQL REPEATABLE-READ 继续命中旧快照。
     * 红线：唯一键失败后不得继续使用普通一致性读判断胜者不存在。
     */
    @Test
    void testPayoutReceiptConflictRecoveryShouldUseCurrentRead() throws Exception {
        String mapper = Files.readString(workspaceRoot().resolve(
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/mapper/PayoutReceiptMapper.java"));
        String service = Files.readString(workspaceRoot().resolve(
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/payout/impl/"
                        + "PayoutOrderApplicationServiceImpl.java"));

        assertThat(mapper).contains("PayoutReceipt selectBySourceForUpdate", "FOR UPDATE");
        assertThat(service).contains("payoutReceiptMapper.selectBySourceForUpdate(");
    }

    /**
     * 场景：追偿应用服务只登记责任与已完成资金结果。
     * 预期：服务只查询资金交易事实，不执行交易、余额或账本命令。
     * 红线：不得把追偿资金策略或资金执行收进 reconciliation。
     */
    @Test
    void testRecoveryOrderShouldOnlyReferenceCompletedFundsFacts() throws Exception {
        String service = Files.readString(workspaceRoot().resolve(
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/recovery/impl/"
                        + "RecoveryOrderApplicationServiceImpl.java"));

        assertThat(service)
                .contains("FundsTransactionQueryService")
                .doesNotContain("FundsDirectTransactionService",
                        "FundsBalanceControlService",
                        "FundsClearingTransactionService",
                        "FundsSettlementTransactionService",
                        "FundsPayoutTransactionService",
                        "LedgerService");
    }

    /**
     * 场景：追偿来源、幂等键或资金交易唯一键发生并发冲突。
     * 预期：冲突恢复使用 current read，能在 MySQL REPEATABLE-READ 下读取已提交胜者。
     * 红线：唯一键失败后不得继续使用普通一致性读判断胜者不存在。
     */
    @Test
    void testRecoveryConflictRecoveryShouldUseCurrentRead() throws Exception {
        String orderMapper = Files.readString(workspaceRoot().resolve(
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/mapper/RecoveryOrderMapper.java"));
        String resultMapper = Files.readString(workspaceRoot().resolve(
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/mapper/RecoveryResultMapper.java"));
        String service = Files.readString(workspaceRoot().resolve(
                "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/recovery/impl/"
                        + "RecoveryOrderApplicationServiceImpl.java"));

        assertThat(orderMapper).contains("selectBySourceForUpdate", "FOR UPDATE");
        assertThat(resultMapper).contains("selectByIdempotencyKeyForUpdate",
                "selectByFundsTransactionSnForUpdate", "FOR UPDATE");
        assertThat(service).contains("recoveryOrderMapper.selectBySourceForUpdate(",
                "recoveryResultMapper.selectByIdempotencyKeyForUpdate(",
                "recoveryResultMapper.selectByFundsTransactionSnForUpdate(");
    }

    private void assertFundsPrimitiveAllowlist(List<String> allowlist,
                                               List<String> primitiveTokens,
                                               String capability) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : nonTransactionProductionJavaSourceFiles()) {
            String relativePath = workspaceRoot().relativize(javaFile).toString();
            if (allowlist.contains(relativePath)) {
                continue;
            }
            String content = Files.readString(javaFile);
            for (String primitiveToken : primitiveTokens) {
                if (content.contains(primitiveToken)) {
                    violations.add(relativePath + " contains " + capability
                            + " transaction primitive " + primitiveToken);
                }
            }
        }

        assertThat(violations)
                .as(capability + " funds primitive must stay behind its canonical orchestration")
                .isEmpty();
    }

    private List<String> dependencyArtifactIds(Path pomPath)
            throws ParserConfigurationException, IOException, SAXException {
        NodeList dependencyNodes = parsePom(pomPath).getElementsByTagName("dependency");
        List<String> artifactIds = new ArrayList<>(dependencyNodes.getLength());
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            NodeList children = dependencyNodes.item(i).getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                if ("artifactId".equals(children.item(childIndex).getNodeName())) {
                    artifactIds.add(children.item(childIndex).getTextContent().trim());
                }
            }
        }
        return artifactIds;
    }

    private List<String> artifactIds(Path pomPath)
            throws ParserConfigurationException, IOException, SAXException {
        NodeList artifactIdNodes = parsePom(pomPath).getElementsByTagName("artifactId");
        List<String> artifactIds = new ArrayList<>(artifactIdNodes.getLength());
        for (int i = 0; i < artifactIdNodes.getLength(); i++) {
            artifactIds.add(artifactIdNodes.item(i).getTextContent().trim());
        }
        return artifactIds;
    }

    private List<String> modulePaths(Path pomPath)
            throws ParserConfigurationException, IOException, SAXException {
        NodeList moduleNodes = parsePom(pomPath).getElementsByTagName("module");
        List<String> modulePaths = new ArrayList<>(moduleNodes.getLength());
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            modulePaths.add(moduleNodes.item(i).getTextContent().trim());
        }
        return modulePaths;
    }

    private Document parsePom(Path pomPath)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        return factory.newDocumentBuilder().parse(pomPath.toFile());
    }

    private List<Path> packageGuardTextFiles() throws IOException {
        List<Path> textFiles = new ArrayList<>();
        Path root = workspaceRoot();
        for (String scanPath : PACKAGE_GUARD_SCAN_PATHS) {
            Path path = root.resolve(scanPath);
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isRegularFile(path)) {
                textFiles.add(path);
                continue;
            }
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(Files::isRegularFile)
                        .filter(this::isPackageGuardTextFile)
                        .forEach(textFiles::add);
            }
        }
        return textFiles;
    }

    private List<Path> fundsJavaSourceFiles() throws IOException {
        return javaSourceFiles(FUNDS_JAVA_PACKAGE_SCAN_PATHS);
    }

    private List<Path> transactionJavaSourceFiles() throws IOException {
        return javaSourceFiles(TRANSACTION_SOURCE_SCAN_PATHS);
    }

    private List<Path> transactionImplWalletApplicationImplSourceFiles() throws IOException {
        return javaSourceFiles(TRANSACTION_IMPL_WALLET_APPLICATION_IMPL_SCAN_PATHS);
    }

    private List<Path> walletApplicationImplSourceFiles() throws IOException {
        return javaSourceFiles(WALLET_APPLICATION_IMPL_SCAN_PATHS);
    }

    private List<Path> ledgerDangerousCallScanJavaSourceFiles() throws IOException {
        return javaSourceFiles(LEDGER_DANGEROUS_CALL_SCAN_PATHS);
    }

    private List<Path> nonWalletProductionJavaSourceFiles() throws IOException {
        return javaSourceFiles(NON_WALLET_PRODUCTION_SOURCE_SCAN_PATHS);
    }

    private List<Path> nonTransactionProductionJavaSourceFiles() throws IOException {
        return javaSourceFiles(NON_TRANSACTION_PRODUCTION_JAVA_SOURCE_SCAN_PATHS);
    }

    private List<Path> javaSourceFiles(List<String> scanPaths) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        Path root = workspaceRoot();
        for (String scanPath : scanPaths) {
            Path path = root.resolve(scanPath);
            if (!Files.exists(path)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(Files::isRegularFile)
                        .filter(javaFile -> javaFile.getFileName().toString().endsWith(".java"))
                        .forEach(javaFiles::add);
            }
        }
        return javaFiles;
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsJavaSource(Path path) throws IOException {
        if (!Files.exists(path)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(path)) {
            return files.anyMatch(javaFile -> Files.isRegularFile(javaFile)
                    && javaFile.getFileName().toString().endsWith(".java"));
        }
    }

    private boolean isPackageGuardTextFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java")
                || fileName.endsWith(".md")
                || fileName.endsWith(".xml")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml")
                || fileName.endsWith(".properties");
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(multiModuleDir)) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        if ("tests".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
    }
}

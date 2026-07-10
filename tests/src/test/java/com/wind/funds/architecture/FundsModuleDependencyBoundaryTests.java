package com.wind.funds.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金模块 Maven 依赖边界测试。
 */
class FundsModuleDependencyBoundaryTests {

    private static final List<String> PRODUCTION_MODULE_POMS = List.of(
            "core/pom.xml",
            "ledger/ledger-face/pom.xml",
            "ledger/ledger-impl/pom.xml",
            "transaction/transaction-face/pom.xml",
            "transaction/transaction-impl/pom.xml",
            "wallet/wallet-face/pom.xml",
            "wallet/wallet-impl/pom.xml",
            "reconciliation/reconciliation-face/pom.xml",
            "reconciliation/reconciliation-impl/pom.xml",
            "governance/governance-face/pom.xml",
            "governance/governance-impl/pom.xml");

    private static final List<String> FACE_MODULE_POMS = List.of(
            "ledger/ledger-face/pom.xml",
            "transaction/transaction-face/pom.xml",
            "wallet/wallet-face/pom.xml",
            "reconciliation/reconciliation-face/pom.xml",
            "governance/governance-face/pom.xml");

    private static final List<String> FACE_ALLOWED_FUNDS_ARTIFACTS = List.of("capte-funds-core");

    private static final List<String> CORE_FORBIDDEN_ARTIFACTS = List.of(
            "capte-funds-ledger-face",
            "capte-funds-ledger-impl",
            "capte-funds-transaction-face",
            "capte-funds-transaction-impl",
            "capte-funds-wallet-face",
            "capte-funds-wallet-impl",
            "capte-funds-reconciliation-face",
            "capte-funds-reconciliation-impl",
            "capte-funds-governance-face",
            "capte-funds-governance-impl",
            "capte-funds-tests",
            "catep-infrastructure-dal");

    private static final List<String> CORE_FORBIDDEN_RUNTIME_SPRING_COMPONENT_TOKENS = List.of(
            "import org.springframework.context.annotation.Primary;",
            "import org.springframework.stereotype.Component;",
            "import org.springframework.stereotype.Repository;",
            "import org.springframework.stereotype.Service;",
            "@Primary",
            "@Component",
            "@Repository",
            "@Service");

    private static final Map<String, List<String>> MODULE_FORBIDDEN_ARTIFACTS = Map.of(
            "ledger/ledger-impl/pom.xml", List.of(
                    "capte-funds-wallet-face",
                    "capte-funds-wallet-impl",
                    "capte-funds-transaction-face",
                    "capte-funds-transaction-impl"),
            "transaction/transaction-impl/pom.xml", List.of(
                    "capte-funds-ledger-face",
                    "capte-funds-ledger-impl"),
            "wallet/wallet-impl/pom.xml", List.of(
                    "capte-funds-transaction-face",
                    "capte-funds-transaction-impl"));

    private static final Map<String, List<String>> MODULE_FORBIDDEN_SOURCE_PACKAGE_PATHS = Map.of(
            "ledger/ledger-impl/src/main/java", List.of(
                    "com/wind/funds/transaction",
                    "com/wind/funds/wallet"),
            "transaction/transaction-impl/src/main/java", List.of(
                    "com/wind/funds/ledger",
                    "com/wind/funds/wallet/application",
                    "com/wind/funds/wallet/dal",
                    "com/wind/funds/wallet/mapstruct"),
            "wallet/wallet-impl/src/main/java", List.of(
                    "com/wind/funds/transaction/converter",
                    "com/wind/funds/transaction/dal",
                    "com/wind/funds/transaction/ledger",
                    "com/wind/funds/transaction/mapstruct"));

    private static final List<String> PACKAGE_GUARD_SCAN_PATHS = List.of(
            "AGENTS.md",
            "core/src/main/java",
            "ledger/ledger-face/src/main/java",
            "ledger/ledger-impl/src/main/java",
            "transaction/transaction-face/src/main/java",
            "transaction/transaction-impl/src/main/java",
            "wallet/wallet-face/src/main/java",
            "wallet/wallet-impl/src/main/java",
            "reconciliation/reconciliation-face/src/main/java",
            "reconciliation/reconciliation-impl/src/main/java",
            "governance/governance-face/src/main/java",
            "governance/governance-impl/src/main/java",
            "tests/src/test/java",
            "docs");

    private static final List<String> FUNDS_JAVA_PACKAGE_SCAN_PATHS = List.of(
            "core/src/main/java",
            "ledger/ledger-face/src/main/java",
            "ledger/ledger-impl/src/main/java",
            "transaction/transaction-face/src/main/java",
            "transaction/transaction-impl/src/main/java",
            "wallet/wallet-face/src/main/java",
            "wallet/wallet-impl/src/main/java",
            "reconciliation/reconciliation-face/src/main/java",
            "reconciliation/reconciliation-impl/src/main/java",
            "governance/governance-face/src/main/java",
            "governance/governance-impl/src/main/java",
            "tests/src/test/java");

    private static final List<String> LEGACY_FUNDS_PACKAGE_TOKENS = List.of(
            String.join(".", "com", "capte", "funds"),
            String.join("/", "com", "capte", "funds"),
            String.join(".", "com", "wind", "integration", "funds"),
            String.join("/", "com", "wind", "integration", "funds"));

    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("(?m)^package\\s+([^;]+);");

    private static final List<String> TRANSACTION_SOURCE_SCAN_PATHS = List.of(
            "transaction/transaction-face/src/main/java",
            "transaction/transaction-impl/src/main/java");

    private static final List<String> TRANSACTION_IMPL_WALLET_APPLICATION_IMPL_SCAN_PATHS = List.of(
            "transaction/transaction-impl/src/main/java/com/wind/funds/wallet/application");

    private static final List<String> WALLET_APPLICATION_IMPL_SCAN_PATHS = List.of(
            "wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application");

    private static final String TRANSACTION_WALLET_AUTHORIZATION_FACADE_SOURCE =
            "transaction/transaction-impl/src/main/java/com/wind/funds/transaction/application/instrument/impl/"
                    + "AuthorizationAdmissionApplicationServiceImpl.java";

    private static final List<String> TRANSACTION_FORBIDDEN_SPEND_CONTROL_ADMISSION_TOKENS = List.of(
            "com.wind.funds.wallet.application.spend.SpendControlAdmissionApplicationService");

    private static final Map<String, List<String>> TRANSACTION_FORBIDDEN_SPEND_RULE_TOKENS = Map.of(
            "wallet spend rule evaluation or ownership service", List.of(
                    "com.wind.funds.wallet.application.spend.SpendRuleEvaluationApplicationService",
                    "com.wind.funds.wallet.service.SpendRuleDefinitionService",
                    "com.wind.funds.wallet.service.SpendRuleVersionService",
                    "com.wind.funds.wallet.service.SpendRuleAssignmentService",
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
            "transaction/transaction-face/src/main/java",
            "transaction/transaction-impl/src/main/java",
            "wallet/wallet-face/src/main/java",
            "wallet/wallet-impl/src/main/java",
            "reconciliation/reconciliation-face/src/main/java",
            "reconciliation/reconciliation-impl/src/main/java",
            "governance/governance-face/src/main/java",
            "governance/governance-impl/src/main/java");

    private static final List<String> LEDGER_DANGEROUS_FACE_CALL_TOKENS = List.of(
            ".updateLedgerBalance(",
            ".deleteLedgerById(",
            ".deleteLedgerByIds(",
            ".updateLedgerTransaction(",
            ".deleteLedgerTransactionById(",
            ".deleteLedgerTransactionByIds(");

    private static final List<String> NON_WALLET_PRODUCTION_SOURCE_SCAN_PATHS = List.of(
            "ledger/ledger-face/src/main/java",
            "ledger/ledger-impl/src/main/java",
            "transaction/transaction-face/src/main/java",
            "transaction/transaction-impl/src/main/java",
            "reconciliation/reconciliation-face/src/main/java",
            "reconciliation/reconciliation-impl/src/main/java",
            "governance/governance-face/src/main/java",
            "governance/governance-impl/src/main/java");

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
     * 场景：core 承载资金 DSL、枚举、值对象和端口契约。
     * 预期：core 源码不声明 Spring 运行时组件或装配优先级。
     * 红线：核心契约不能把具体容器装配职责上推到 core。
     */
    @Test
    void testCoreShouldNotDeclareRuntimeSpringComponents() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : javaSourceFiles(List.of("core/src/main/java"))) {
            String content = Files.readString(javaFile);
            for (String forbiddenToken : CORE_FORBIDDEN_RUNTIME_SPRING_COMPONENT_TOKENS) {
                if (content.contains(forbiddenToken)) {
                    violations.add(workspaceRoot().relativize(javaFile)
                            + " contains runtime Spring component token " + forbiddenToken);
                }
            }
        }

        assertThat(violations)
                .as("core must keep Spring runtime component ownership outside core")
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
                if (artifactId.startsWith("capte-funds-") && !FACE_ALLOWED_FUNDS_ARTIFACTS.contains(artifactId)) {
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
            if (dependencyArtifactIds(workspaceRoot().resolve(pomPath)).contains("capte-funds-tests")) {
                violations.add(pomPath + " depends on capte-funds-tests");
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
     * 红线：不得恢复历史 Capte funds 包根或旧 Wind integration funds 资金域包根。
     */
    @Test
    void testFundsPackageRootShouldStayUnderWindFunds() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path textFile : packageGuardTextFiles()) {
            String content = Files.readString(textFile);
            for (String legacyToken : LEGACY_FUNDS_PACKAGE_TOKENS) {
                if (content.contains(legacyToken)) {
                    violations.add(workspaceRoot().relativize(textFile) + " contains " + legacyToken);
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
                        && !TRANSACTION_WALLET_AUTHORIZATION_FACADE_SOURCE.equals(relativePath)) {
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

        assertThat(root.resolve("transaction/transaction-face/src/main/java/com/wind/funds/transaction/application/"
                + "ExternalFundsEventApplicationService.java")).exists();
        assertThat(root.resolve("transaction/transaction-face/src/main/java/com/wind/funds/transaction/model/request/"
                + "ConsumeExternalFundsEventRequest.java")).exists();
        assertThat(root.resolve("wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/external/"
                + "ExternalFundsEventApplicationService.java")).doesNotExist();
        assertThat(root.resolve("wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/request/"
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
     * 场景：wallet 对外提供支付工具交易生命周期和账户能力 application facade。
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

    private List<String> dependencyArtifactIds(Path pomPath)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);

        NodeList dependencyNodes = factory.newDocumentBuilder()
                .parse(pomPath.toFile())
                .getElementsByTagName("dependency");
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

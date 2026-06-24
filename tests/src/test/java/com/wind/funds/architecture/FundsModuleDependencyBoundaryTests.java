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
            "docs",
            "openspec");

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

    private static final Map<String, List<String>> TRANSACTION_FORBIDDEN_SPEND_RULE_TOKENS = Map.of(
            "wallet spend rule application service", List.of(
                    "com.wind.funds.wallet.application.spend.",
                    "SpendControlAdmissionApplicationService",
                    "SpendControlTransactionConsumptionApplicationService",
                    "BudgetControlLimitAdjustmentApplicationService"),
            "wallet spend rule entity or mapper", List.of(
                    "com.wind.funds.wallet.dal.entities.SpendRule",
                    "com.wind.funds.wallet.dal.entities.SpendControlActivity",
                    "com.wind.funds.wallet.dal.mapper.SpendRule",
                    "com.wind.funds.wallet.dal.mapper.SpendControlActivity"),
            "wallet spend control projection model", List.of(
                    "BudgetControlProjectionDTO",
                    "BudgetControlProjectionQuery"));

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
     * 场景：Spend Rule 主能力归属于钱包支出控制域，交易模块只消费已固化决策快照。
     * 预期：transaction 生产源码不得直接依赖 wallet 的 Spend Rule 服务、实体、Mapper 或预算控制投影模型。
     * 红线：交易内核不能计算、更新或查询当前 Spend Rule，只能读取交易上下文中的历史快照。
     */
    @Test
    void testTransactionShouldOnlyConsumeSpendRuleSnapshotsWithoutWalletRuleOwnership() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path javaFile : transactionJavaSourceFiles()) {
            String content = Files.readString(javaFile);
            for (Map.Entry<String, List<String>> forbiddenEntry : TRANSACTION_FORBIDDEN_SPEND_RULE_TOKENS.entrySet()) {
                for (String forbiddenToken : forbiddenEntry.getValue()) {
                    if (content.contains(forbiddenToken)) {
                        violations.add(workspaceRoot().relativize(javaFile)
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

    private List<Path> ledgerDangerousCallScanJavaSourceFiles() throws IOException {
        return javaSourceFiles(LEDGER_DANGEROUS_CALL_SCAN_PATHS);
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

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

    private static final List<String> FACE_ALLOWED_FUNDS_ARTIFACTS = List.of("wind-funds-core");

    private static final List<String> CORE_FORBIDDEN_ARTIFACTS = List.of(
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
            "wind-funds-tests",
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

    private static final List<String> LEGACY_FUNDS_PACKAGE_TOKENS = List.of(
            String.join(".", "com", "capte", "funds"),
            String.join("/", "com", "capte", "funds"),
            String.join(".", "com", "wind", "integration", "funds"),
            String.join("/", "com", "wind", "integration", "funds"));

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
     * 预期：face 只依赖 wind-funds-core，不依赖任一 impl、同级 face 或测试模块。
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

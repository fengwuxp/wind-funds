package com.capte.funds.architecture;

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

    private static final List<String> PRODUCTION_SOURCE_DIRS = List.of(
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
            "governance/governance-impl/src/main/java");

    private static final List<String> BALANCE_CHANGED_EVENT_LISTENER_PATTERNS = List.of(
            "@EventListener",
            "@TransactionalEventListener",
            "ApplicationListener<LedgerBalanceChangedEvent",
            "ApplicationListener< LedgerBalanceChangedEvent");

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
     * 场景：账本余额投影发布余额变更观察事件。
     * 预期：生产资金域没有监听 `LedgerBalanceChangedEvent` 后反向修复余额、补账或推进事实的入口。
     * 红线：余额日志或余额变更事件只能作为观测与审计辅助，不得成为新的余额事实源。
     */
    @Test
    void testBalanceChangedEventShouldNotHaveProductionRepairListeners() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : productionSourceFiles()) {
            String source = Files.readString(sourceFile);
            if (!source.contains("LedgerBalanceChangedEvent")) {
                continue;
            }
            for (String pattern : BALANCE_CHANGED_EVENT_LISTENER_PATTERNS) {
                if (source.contains(pattern)) {
                    violations.add(workspaceRoot().relativize(sourceFile) + " contains " + pattern);
                }
            }
        }

        assertThat(violations)
                .as("LedgerBalanceChangedEvent must stay observational and must not drive balance repair")
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

    private List<Path> productionSourceFiles() throws IOException {
        List<Path> sourceFiles = new ArrayList<>();
        Path root = workspaceRoot();
        for (String sourceDir : PRODUCTION_SOURCE_DIRS) {
            Path directory = root.resolve(sourceDir);
            if (!Files.exists(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .forEach(sourceFiles::add);
            }
        }
        return sourceFiles;
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

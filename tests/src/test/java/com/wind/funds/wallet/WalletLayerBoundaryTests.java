package com.wind.funds.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钱包产品层模块边界测试。
 */
class WalletLayerBoundaryTests {

    private static final List<String> FACE_FORBIDDEN_ARTIFACTS = List.of(
            "wind-funds-ledger-face",
            "wind-funds-ledger-impl",
            "wind-funds-transaction-face",
            "wind-funds-transaction-impl",
            "wind-funds-wallet-impl");

    private static final List<String> IMPL_FORBIDDEN_ARTIFACTS = List.of(
            "wind-funds-ledger-impl",
            "wind-funds-transaction-face",
            "wind-funds-transaction-impl");

    /**
     * 场景：wallet-face 对外表达产品契约。
     * 预期：face 本地只声明 core，项目级 Wind Operator 依赖由根 POM 统一继承。
     * 红线：对外契约不能泄漏交易事实、账务实现事实或内部实现类型。
     */
    @Test
    void testWalletFaceShouldOnlyExposeCoreContracts() throws Exception {
        List<String> dependencyArtifactIds = dependencyArtifactIds(
                workspaceRoot().resolve("wallet/face/pom.xml"));

        assertThat(dependencyArtifactIds).containsExactly("wind-funds-core");
        assertThat(dependencyArtifactIds).doesNotContain(FACE_FORBIDDEN_ARTIFACTS.toArray(String[]::new));
    }

    /**
     * 场景：wallet-impl 作为钱包资源和准入能力实现。
     * 预期：impl 依赖 wallet/ledger 契约和必要基础设施，不反向依赖交易契约或交易实现。
     * 红线：钱包实现不能代持交易用例编排，也不能绕过交易层写入资金事实。
     */
    @Test
    void testWalletImplShouldUseFaceContractsWithoutImplDependencies() throws Exception {
        List<String> dependencyArtifactIds = dependencyArtifactIds(
                workspaceRoot().resolve("wallet/impl/pom.xml"));

        assertThat(dependencyArtifactIds)
                .contains("wind-funds-wallet-face",
                        "wind-funds-ledger-face");
        assertThat(dependencyArtifactIds).doesNotContain(IMPL_FORBIDDEN_ARTIFACTS.toArray(String[]::new));
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
        List<String> values = new ArrayList<>(dependencyNodes.getLength());
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            NodeList children = dependencyNodes.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if ("artifactId".equals(child.getNodeName())) {
                    values.add(child.getTextContent().trim());
                    break;
                }
            }
        }
        return values;
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

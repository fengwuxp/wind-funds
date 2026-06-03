package com.wind.funds.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;
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
            "wind-funds-transaction-impl");

    /**
     * 场景：wallet-face 对外表达产品契约。
     * 预期：face 只依赖 core/capte-domain-core，不感知交易、账务和钱包实现模块。
     * 红线：对外契约不能泄漏交易事实、账务实现事实或内部实现类型。
     */
    @Test
    void testWalletFaceShouldOnlyExposeCoreContracts() throws Exception {
        List<String> artifactIds = artifactIds(workspaceRoot().resolve("wallet/wallet-face/pom.xml"));

        assertThat(artifactIds).contains("wind-funds-wallet-face", "wind-funds-core", "capte-domain-core");
        assertThat(artifactIds).doesNotContain(FACE_FORBIDDEN_ARTIFACTS.toArray(String[]::new));
    }

    /**
     * 场景：wallet-impl 作为产品门面实现，需要编排钱包契约、交易契约和账务契约。
     * 预期：impl 只依赖 face/core 契约，不穿透到交易或账务实现包。
     * 红线：钱包层不能绕过 transaction/ledger 的服务契约直接耦合实现事实。
     */
    @Test
    void testWalletImplShouldUseFaceContractsWithoutImplDependencies() throws Exception {
        List<String> artifactIds = artifactIds(workspaceRoot().resolve("wallet/wallet-impl/pom.xml"));

        assertThat(artifactIds)
                .contains("wind-funds-wallet-impl",
                        "wind-funds-wallet-face",
                        "wind-funds-ledger-face",
                        "wind-funds-transaction-face");
        assertThat(artifactIds).doesNotContain(IMPL_FORBIDDEN_ARTIFACTS.toArray(String[]::new));
    }

    private List<String> artifactIds(Path pomPath)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);

        NodeList artifactIdNodes = factory.newDocumentBuilder()
                .parse(pomPath.toFile())
                .getElementsByTagName("artifactId");
        List<String> values = new ArrayList<>(artifactIdNodes.getLength());
        for (int i = 0; i < artifactIdNodes.getLength(); i++) {
            values.add(artifactIdNodes.item(i).getTextContent().trim());
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

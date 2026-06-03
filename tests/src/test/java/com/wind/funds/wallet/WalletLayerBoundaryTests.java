package com.wind.funds.wallet;

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
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

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

    private static final List<String> FACE_FORBIDDEN_IMPORT_PREFIXES = List.of(
            "com.wind.funds.transaction.",
            "com.wind.funds.wallet.dal.",
            "com.wind.funds.wallet.services.impl.");

    private static final List<String> IMPL_FORBIDDEN_ARTIFACTS = List.of(
            "wind-funds-ledger-impl",
            "wind-funds-transaction-impl");

    private static final List<String> IMPL_FORBIDDEN_IMPORT_PREFIXES = List.of(
            "com.wind.funds.ledger.dal.",
            "com.wind.funds.ledger.impl.",
            "com.wind.funds.route.AuthorizationFundsInstructionRouteResolver",
            "com.wind.funds.route.BalanceControlFundsInstructionRouteResolver",
            "com.wind.funds.route.CompositeRouteResolver",
            "com.wind.funds.route.DefaultRouteReplayService",
            "com.wind.funds.route.DefaultRouteSnapshotFactory",
            "com.wind.funds.route.RouteBenefitSnapshotContextSupport",
            "com.wind.funds.route.RouteReplaySupport",
            "com.wind.funds.route.TransferFundsInstructionRouteResolver",
            "com.wind.funds.route.support.PlatformAccountRouteSupport",
            "com.wind.funds.route.support.RouteParticipantFactory",
            "com.wind.funds.route.support.RouteSpecSupport",
            "com.wind.funds.route.support.RouteSubjectSupport",
            "com.wind.funds.transaction.application.impl.",
            "com.wind.funds.transaction.dal.",
            "com.wind.funds.transaction.ledger.",
            "com.wind.funds.transaction.services.impl.");

    private static final List<String> WALLET_FORBIDDEN_FACT_TOKENS = List.of(
            "t_funds_transaction",
            "t_funds_transaction_detail",
            "t_funds_frozen_order",
            "t_ledger_transaction",
            "t_ledger_posting_plan",
            "t_ledger_entry",
            "FundsTransactionMapper",
            "FundsTransactionDetailMapper",
            "FundsFrozenOrderMapper",
            "LedgerTransactionMapper",
            "LedgerPostingPlanMapper",
            "LedgerEntryMapper");

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
        assertNoImportsStartingWith(
                workspaceRoot().resolve("wallet/wallet-face/src/main/java"),
                FACE_FORBIDDEN_IMPORT_PREFIXES);
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
        assertNoImportsStartingWith(
                workspaceRoot().resolve("wallet/wallet-impl/src/main/java"),
                IMPL_FORBIDDEN_IMPORT_PREFIXES);
    }

    /**
     * 场景：支付工具、绑定关系和资金来源关系作为 route 候选数据维护。
     * 预期：钱包层可维护自身关系表，但不能直接写交易事实或账务 posting 事实。
     * 红线：支付工具或绑定关系的创建不能绕过交易编排生成交易/分录事实。
     */
    @Test
    void testWalletProductLayerShouldNotWriteTransactionOrPostingFactsDirectly() throws IOException {
        assertNoForbiddenTokens(
                workspaceRoot().resolve("wallet/wallet-impl/src/main/java"),
                WALLET_FORBIDDEN_FACT_TOKENS);
    }

    private void assertNoImportsStartingWith(Path sourceRoot, Collection<String> forbiddenPrefixes)
            throws IOException {
        List<String> violations = new ArrayList<>();
        for (SourceFile sourceFile : javaSources(sourceRoot)) {
            List<String> importStatements = sourceFile.content().lines()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .toList();
            for (String importStatement : importStatements) {
                for (String forbiddenPrefix : forbiddenPrefixes) {
                    if (importsPrefix(importStatement, forbiddenPrefix)) {
                        violations.add(sourceViolation(sourceFile.path(), importStatement));
                    }
                }
            }
        }

        assertThat(violations)
                .as("forbidden wallet layer imports")
                .isEmpty();
    }

    private void assertNoForbiddenTokens(Path sourceRoot, Collection<String> forbiddenTokens) throws IOException {
        List<String> violations = new ArrayList<>();
        for (SourceFile sourceFile : javaSources(sourceRoot)) {
            for (String forbiddenToken : forbiddenTokens) {
                if (sourceFile.content().contains(forbiddenToken)) {
                    violations.add(sourceViolation(sourceFile.path(), forbiddenToken));
                }
            }
        }

        assertThat(violations)
                .as("wallet layer must not write transaction or posting facts directly")
                .isEmpty();
    }

    private boolean importsPrefix(String importStatement, String forbiddenPrefix) {
        return importStatement.startsWith("import " + forbiddenPrefix)
                || importStatement.startsWith("import static " + forbiddenPrefix);
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

    private List<SourceFile> javaSources(Path sourceRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }
        List<Path> sourcePaths;
        try (Stream<Path> pathStream = Files.walk(sourceRoot)) {
            sourcePaths = pathStream
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        List<SourceFile> sourceFiles = new ArrayList<>(sourcePaths.size());
        for (Path sourcePath : sourcePaths) {
            sourceFiles.add(new SourceFile(sourcePath, Files.readString(sourcePath)));
        }
        return sourceFiles;
    }

    private String sourceViolation(Path sourcePath, String violation) {
        return workspaceRoot().relativize(sourcePath) + " contains " + violation;
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

    private record SourceFile(Path path, String content) {
    }
}

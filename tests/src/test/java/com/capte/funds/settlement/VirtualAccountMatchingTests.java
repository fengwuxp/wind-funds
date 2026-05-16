package com.capte.funds.settlement;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteNodeType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualAccountMatchingTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> VIRTUAL_ACCOUNT_TYPE_MARKERS = List.of(
            "VirtualAccount",
            "VirtualAcct",
            "VaCollection",
            "VaDeposit",
            "VaMatching",
            "VaMatch",
            "VaReceipt",
            "VaSuspense"
    );

    private static final List<String> FORBIDDEN_LEDGER_SUBJECT_MARKERS = List.of(
            "LedgerSubjectCode.VIRTUAL_ACCOUNT",
            "LedgerSubjectCode.VA",
            "LedgerSubjectCode.VIRTUAL_ACCT",
            "ledgerSubjectCode(VIRTUAL_ACCOUNT",
            "ledgerSubjectCode(VA",
            "ledgerSubjectCode(\"VIRTUAL_ACCOUNT\"",
            "ledgerSubjectCode(\"VA\""
    );

    private static final List<String> UNKNOWN_VA_MATCHING_MARKERS = List.of(
            "UNKNOWN_VA",
            "UNMATCHED_VA",
            "UNMATCHED_VIRTUAL_ACCOUNT",
            "VirtualAccountUnmatched",
            "VaUnmatched",
            "unknownVirtualAccount",
            "unmatchedVirtualAccount"
    );

    private static final List<String> DIRECT_BALANCE_CREDIT_MARKERS = List.of(
            "LedgerSubjectCode.AVAILABLE",
            "LedgerSubjectCode.CLEARING",
            "FundsTransactionPayRequest",
            "FundsTransactionTopupRequest",
            "FundsDirectTransactionService",
            "RoutedFundsInstructionOrchestrator",
            "TransferFundsInstructionRouteResolver",
            "DefaultLedgerTransactionPostingServiceImpl",
            "LedgerTransactionPostingService",
            "FundsInstructionLifecycleSaver",
            "FundsTransactionMapper",
            "LedgerEntryMapper",
            "LedgerBalanceProjectionMapper"
    );

    /**
     * 场景：外部 VA 到账，VA 与用户或商户订单绑定关系明确。
     * 输入：当前 route 节点类型和账本余额桶枚举。
     * 输出：VA 是否只能作为工具/外部引用，不能成为内部账务主体。
     * 预期：路由层保留工具引用能力，账本余额桶不出现 VA 专用主体。
     * 红线：VA、银行卡、PSP 或其他外部工具不得直接成为 `LedgerEntry.subjectType`。
     */
    @Test
    void testVirtualAccountShouldBeToolReferenceNotLedgerSubject() {
        Set<String> routeNodeTypes = enumNames(RouteNodeType.class);
        assertThat(routeNodeTypes).contains("PAYMENT_INSTRUMENT", "EXTERNAL_ACCOUNT");

        Set<String> ledgerSubjectCodes = enumNames(LedgerSubjectCode.class);
        assertThat(ledgerSubjectCodes)
                .doesNotContain("VA", "VIRTUAL_ACCOUNT", "VIRTUAL_ACCT", "BANK_ACCOUNT", "PSP_ACCOUNT");
        assertThat(ledgerSubjectCodes).contains("SUSPENSE");
    }

    /**
     * 场景：VA 收款匹配能力进入实现阶段。
     * 输入：扫描资金域生产源码中的虚拟账户、VA 收款或 VA 匹配类型。
     * 输出：把 VA 建成内部账务主体的违规列表。
     * 预期：VA 只能作为支付工具、外部账户或匹配快照引用。
     * 红线：不得为 VA 增加内部 ledger subject 或直接以 VA 入账。
     */
    @Test
    void testVirtualAccountMatchingShouldNotCreateLedgerSubjectForVa() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenLedgerSubjectReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("virtual account must remain a tool reference and must not become a ledger subject")
                .isEmpty();
    }

    /**
     * 场景：外部 VA 到账但无法匹配用户、商户或订单。
     * 输入：扫描资金域生产源码中未来可能出现的未知 VA 匹配处理。
     * 输出：未知 VA 直接创建可用、清算余额或资金交易事实的违规列表。
     * 预期：未知 VA 必须进入挂账、差错、补入账或人工核验路径。
     * 红线：未知 VA 不得自动增加用户或商户余额。
     */
    @Test
    void testUnknownVirtualAccountReceiptShouldEnterSuspenseNotBalance() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findUnknownVaDirectBalanceCredits(sourceRootPath));
        }

        assertThat(violations)
                .as("unknown virtual account receipts must enter suspense or exception flow, not balance directly")
                .isEmpty();
    }

    private static List<String> findForbiddenLedgerSubjectReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isVirtualAccountSource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile, FORBIDDEN_LEDGER_SUBJECT_MARKERS));
            }
        }
        return violations;
    }

    private static List<String> findUnknownVaDirectBalanceCredits(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isVirtualAccountSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAny(source, UNKNOWN_VA_MATCHING_MARKERS)
                        && containsAny(source, DIRECT_BALANCE_CREDIT_MARKERS)
                        && !source.contains("LedgerSubjectCode.SUSPENSE")) {
                    violations.add(sourceFile + ": unknown VA receipt must not credit balance directly");
                }
            }
        }
        return violations;
    }

    private static List<String> findForbiddenReferences(Path sourceFile, List<String> forbiddenMarkers)
            throws IOException {
        List<String> violations = new ArrayList<>();
        for (String line : Files.readAllLines(sourceFile)) {
            if (containsAny(line, forbiddenMarkers)) {
                violations.add(sourceFile + ": " + line.trim());
            }
        }
        return violations;
    }

    private static List<Path> listJavaSources(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isVirtualAccountSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return VIRTUAL_ACCOUNT_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean containsAny(String source, List<String> markers) {
        return markers.stream().anyMatch(source::contains);
    }

    private static <E extends Enum<E>> Set<String> enumNames(Class<E> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (containsAllProductionSourceRoots(current)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("project root not found");
    }

    private static boolean containsAllProductionSourceRoots(Path current) {
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            if (!Files.exists(current.resolve(sourceRoot))) {
                return false;
            }
        }
        return true;
    }
}

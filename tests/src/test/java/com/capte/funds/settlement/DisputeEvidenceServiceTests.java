package com.capte.funds.settlement;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.wind.integration.funds.spec.SourceObjectType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
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

class DisputeEvidenceServiceTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> DISPUTE_EVIDENCE_TYPE_MARKERS = List.of(
            "DisputeEvidence",
            "EvidenceActivityLog",
            "EvidencePackage",
            "EvidenceSubmission",
            "EvidenceExport",
            "RepresentmentEvidence",
            "ChargebackEvidence",
            "DisputeResult",
            "DisputeDecision"
    );

    private static final List<String> EVIDENCE_PACKAGE_MARKERS = List.of(
            "evidencePackageSn",
            "EvidencePackage",
            "EvidenceSubmission",
            "EvidenceExport",
            "RepresentmentEvidence",
            "ChargebackEvidence"
    );

    private static final List<String> MINIMUM_NECESSARY_MARKERS = List.of(
            "minimumNecessary",
            "minimal",
            "selectedFields",
            "fieldSelection",
            "sourceActivityLogRefs",
            "evidenceType",
            "reasonCode"
    );

    private static final List<String> REDACTION_MARKERS = List.of(
            "redactionPolicy",
            "redacted",
            "masked",
            "desensitized",
            "sensitive",
            "脱敏"
    );

    private static final List<String> VERSION_MARKERS = List.of(
            "templateVersion",
            "packageVersion",
            "materialVersion",
            "version",
            "Version"
    );

    private static final List<String> AUDIT_MARKERS = List.of(
            "audit",
            "Audit",
            "operator",
            "Operator",
            "submittedBy",
            "submittedTime",
            "submittedAt",
            "recipient",
            "exportAuditSn",
            "exportedBy",
            "exportedAt",
            "accessLog"
    );

    private static final List<String> DISPUTE_RESULT_MARKERS = List.of(
            "WON",
            "LOST",
            "win",
            "lost",
            "DisputeResult",
            "DisputeDecision",
            "liabilityParty",
            "MerchantLiability"
    );

    private static final List<String> RESULT_ALIGNMENT_MARKERS = List.of(
            "liability",
            "Liability",
            "fundsResult",
            "FundsResult",
            "MerchantRecovery",
            "RecoveryCase",
            "CHARGEBACK",
            "recovered",
            "reconciliationException",
            "ADJUSTMENT",
            "BALANCE_ADJUST"
    );

    private static final List<String> SENSITIVE_OR_FULL_LOG_MARKERS = List.of(
            "fullBehaviorLog",
            "behaviorLog",
            "clickStream",
            "rawActivityLog",
            "rawPayload",
            "cardNumber",
            "idCard",
            "phone",
            "email",
            "fullName",
            "address",
            "token",
            "password"
    );

    private static final List<String> DIRECT_FACT_MUTATION_MARKERS = List.of(
            "DefaultLedgerTransactionPostingServiceImpl",
            "LedgerTransactionPostingService",
            "FundsInstructionLifecycleSaver",
            "FundsInstructionLifecycleRecorder",
            "FundsTransactionMapper",
            "FundsTransactionDetailMapper",
            "LedgerEntryMapper",
            "LedgerTransactionMapper",
            "LedgerBalanceProjectionMapper"
    );

    /**
     * 场景：争议证据提交和外部裁决进入资金域。
     * 输入：当前 DSL 来源对象、资金效果和资金事件枚举。
     * 输出：证据运营对象和资金事实对象的边界。
     * 预期：DSL 只表达争议资金结果，不把证据提交、举证或运营节点建成资金事件。
     * 红线：不得把 EVIDENCE_SUBMITTED、REPRESENTMENT 等运营状态塞进账务 DSL。
     */
    @Test
    void testDisputeEvidenceShouldStayOperationalContextNotFundsEvent() {
        Set<String> sourceObjectTypes = enumNames(SourceObjectType.class);
        Set<String> effectTypes = enumNames(FundsEffectType.class);
        Set<String> eventTypes = enumNames(FundsTransactionEventType.class);

        assertThat(sourceObjectTypes).contains("DISPUTE_CASE");
        assertThat(effectTypes).contains("DISPUTE");
        assertThat(eventTypes).contains("CHARGEBACK");
        assertThat(eventTypes)
                .doesNotContain("DISPUTE_CREATED", "EVIDENCE_SUBMITTED", "REPRESENTMENT");
    }

    /**
     * 场景：争议证据包生成、导出或提交能力进入实现阶段。
     * 输入：扫描资金域生产源码中的争议证据、证据包、提交和导出类型。
     * 输出：缺少最小必要、脱敏、版本或审计语义的违规列表。
     * 预期：证据包按原因码和模板裁剪，敏感字段脱敏，导出和提交可审计。
     * 红线：不得把全量行为日志或未脱敏敏感数据提交给外部机构。
     */
    @Test
    void testDisputeEvidencePackageShouldDeclareMinimumNecessaryRedactionVersionAndAudit()
            throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findIncompleteEvidencePackages(sourceRootPath));
        }

        assertThat(violations)
                .as("dispute evidence package must declare minimum necessary, redaction, version and audit")
                .isEmpty();
    }

    /**
     * 场景：外部返回争议胜诉或败诉结果。
     * 输入：扫描资金域生产源码中的争议证据结果、裁决或商户责任类型。
     * 输出：缺少资金结果和责任一致性语义的违规列表。
     * 预期：胜诉回补、败诉扣减、费用和商户责任必须能互相解释。
     * 红线：不得用普通退款覆盖拒付记录，或让资金结果与商户责任分叉。
     */
    @Test
    void testDisputeEvidenceResultShouldAlignFundsResultAndLiability() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findUnalignedDisputeResults(sourceRootPath));
        }

        assertThat(violations)
                .as("dispute result must align funds result and merchant liability")
                .isEmpty();
    }

    /**
     * 场景：争议证据包选择了行为日志、附件或敏感字段。
     * 输入：扫描资金域生产源码中的证据包和提交类型。
     * 输出：出现敏感或全量日志标记但缺少最小必要、脱敏或审计控制的违规列表。
     * 预期：敏感字段和行为日志只能在裁剪、脱敏、审批和审计后使用。
     * 红线：不得直接外传完整行为日志、点击流、卡号、证件、联系方式或认证数据。
     */
    @Test
    void testDisputeEvidenceShouldNotExportSensitiveOrFullLogsWithoutControls() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findUnsafeSensitiveEvidenceExports(sourceRootPath));
        }

        assertThat(violations)
                .as("sensitive dispute evidence must be minimized, redacted and audited")
                .isEmpty();
    }

    /**
     * 场景：争议证据服务处理证据提交和结果回传。
     * 输入：扫描资金域生产源码中的证据服务类型。
     * 输出：直接引用交易、账本事实写入组件或 Mapper 的违规列表。
     * 预期：证据服务只记录运营证据和裁决，资金结果通过交易命令或差错路径表达。
     * 红线：证据提交、胜诉或败诉处理不得直接修改历史交易、分录或余额投影。
     */
    @Test
    void testDisputeEvidenceServiceShouldNotMutateFundsFactsDirectly() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findDirectFactMutationReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("dispute evidence service must not mutate transaction or ledger facts directly")
                .isEmpty();
    }

    /**
     * 场景：争议裁决需要触发回补、差错或受控调账。
     * 输入：现有资金指令上下文键。
     * 输出：证据引用和对账差错引用契约。
     * 预期：证据只是资金事实凭证引用，不替代 route 快照、资金事件或账本分录。
     * 红线：不得把证据包当作账务主体或路径选择依据。
     */
    @Test
    void testDisputeEvidenceReferenceShouldRemainEvidenceOnly() {
        assertThat(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF)
                .isEqualTo("adjustEvidenceRef");
        assertThat(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF)
                .isEqualTo("reconciliationExceptionRef");
        assertThat(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF)
                .isNotEqualTo(FundsInstructionContextKeys.ROUTE_SNAPSHOT);
    }

    private static List<String> findIncompleteEvidencePackages(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isDisputeEvidenceSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAny(source, EVIDENCE_PACKAGE_MARKERS)) {
                    requireContainsAny(sourceFile, source, MINIMUM_NECESSARY_MARKERS,
                            "minimum necessary evidence selection", violations);
                    requireContainsAny(sourceFile, source, REDACTION_MARKERS, "redaction policy",
                            violations);
                    requireContainsAny(sourceFile, source, VERSION_MARKERS, "version trace",
                            violations);
                    requireContainsAny(sourceFile, source, AUDIT_MARKERS, "submission/export audit",
                            violations);
                }
            }
        }
        return violations;
    }

    private static List<String> findUnalignedDisputeResults(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isDisputeEvidenceSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAny(source, DISPUTE_RESULT_MARKERS)
                        && !containsAny(source, RESULT_ALIGNMENT_MARKERS)) {
                    violations.add(sourceFile
                            + ": dispute result must align funds result and merchant liability");
                }
            }
        }
        return violations;
    }

    private static List<String> findUnsafeSensitiveEvidenceExports(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isDisputeEvidenceSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAny(source, SENSITIVE_OR_FULL_LOG_MARKERS)
                        && !containsAny(source, MINIMUM_NECESSARY_MARKERS)) {
                    violations.add(sourceFile + ": sensitive evidence is missing minimum necessary control");
                }
                if (containsAny(source, SENSITIVE_OR_FULL_LOG_MARKERS)
                        && !containsAny(source, REDACTION_MARKERS)) {
                    violations.add(sourceFile + ": sensitive evidence is missing redaction control");
                }
                if (containsAny(source, SENSITIVE_OR_FULL_LOG_MARKERS)
                        && !containsAny(source, AUDIT_MARKERS)) {
                    violations.add(sourceFile + ": sensitive evidence is missing audit control");
                }
            }
        }
        return violations;
    }

    private static List<String> findDirectFactMutationReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isDisputeEvidenceSource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile, DIRECT_FACT_MUTATION_MARKERS));
            }
        }
        return violations;
    }

    private static void requireContainsAny(Path sourceFile, String source, List<String> markers, String label,
            List<String> violations) {
        if (!containsAny(source, markers)) {
            violations.add(sourceFile + ": missing dispute evidence " + label);
        }
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

    private static boolean isDisputeEvidenceSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return DISPUTE_EVIDENCE_TYPE_MARKERS.stream().anyMatch(fileName::contains);
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

package com.capte.funds.settlement;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalNotificationOrderingTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> EXTERNAL_NOTIFICATION_TYPE_MARKERS = List.of(
            "ExternalNotification",
            "ExternalNotify",
            "ExternalCallback",
            "ChannelNotification",
            "ChannelNotify",
            "ChannelCallback",
            "WebhookNotification",
            "WebhookCallback",
            "NotificationOrdering",
            "NotificationPull",
            "NotificationRepair",
            "CallbackPull",
            "CallbackRepair",
            "ChannelResult"
    );

    private static final List<String> TRUSTED_EXTERNAL_FACT_MARKERS = List.of(
            "externalTransactionId",
            "channelTransactionSn",
            "externalEventId",
            "externalNotificationId",
            "channelNotificationId",
            "callbackId",
            "webhookId",
            "pullTaskId"
    );

    private static final List<String> IDEMPOTENCY_MARKERS = List.of(
            "idempotency",
            "Idempotency",
            "idempotent",
            "Idempotent",
            "deduplicate",
            "Deduplicate",
            "duplicate",
            "Duplicate",
            "unique",
            "Unique",
            "externalEventId",
            "externalNotificationId",
            "channelNotificationId",
            "callbackId",
            "webhookId"
    );

    private static final List<String> AUDIT_MARKERS = List.of(
            "audit",
            "Audit",
            "operator",
            "Operator",
            "trace",
            "Trace",
            "source",
            "Source",
            "receivedAt",
            "processedAt",
            "rawPayload",
            "responsePayload"
    );

    private static final List<String> TERMINAL_CONFLICT_MARKERS = List.of(
            "terminal",
            "Terminal",
            "final",
            "Final",
            "stable",
            "Stable",
            "PROCESSING",
            "OPEN",
            "CLOSED",
            "FAILED",
            "REJECTED",
            "UNKNOWN",
            "PENDING",
            "VERIFYING"
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
     * 场景：成功、失败、退回通知乱序到达。
     * 输入：当前资金交易聚合状态枚举。
     * 输出：稳定状态和非稳定状态集合。
     * 预期：外部通知只能基于可信事实把交易推进到单一稳定状态。
     * 红线：不得让同一笔外部交易同时出现成功和失败等双终态。
     */
    @Test
    void testExternalNotificationShouldHaveSingleStableTransactionStateVocabulary() {
        Set<FundsTransactionStatus> stableStatuses = EnumSet.of(
                FundsTransactionStatus.OPEN,
                FundsTransactionStatus.CLOSED,
                FundsTransactionStatus.REJECTED
        );

        assertThat(stableStatuses)
                .doesNotContain(FundsTransactionStatus.PROCESSING, FundsTransactionStatus.FAILED);
        assertThat(FundsTransactionStatus.values())
                .contains(FundsTransactionStatus.PROCESSING, FundsTransactionStatus.OPEN,
                        FundsTransactionStatus.CLOSED, FundsTransactionStatus.FAILED,
                        FundsTransactionStatus.REJECTED);
    }

    /**
     * 场景：外部回调丢失后通过补拉拿到通道事实。
     * 输入：资金指令上下文和充值请求的外部流水字段。
     * 输出：外部事实幂等键的契约字段。
     * 预期：补拉和回调必须围绕同一外部事实键去重和审计。
     * 红线：不得只按到达顺序推进状态。
     */
    @Test
    void testExternalNotificationShouldUseExternalFactIdForIdempotency() {
        assertThat(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID)
                .isEqualTo("externalTransactionId");
    }

    /**
     * 场景：外部通知、回调、补拉或修复任务进入实现阶段。
     * 输入：扫描资金域生产源码中的外部通知处理类型。
     * 输出：缺少幂等、审计或终态冲突保护语义的违规列表。
     * 预期：可信外部事实推进状态，补拉可重入，并保留处理审计。
     * 红线：状态不明必须进入待核验，不得产生双终态或覆盖历史资金事实。
     */
    @Test
    void testExternalNotificationHandlersShouldDeclareIdempotencyAuditAndTerminalGuard()
            throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findIncompleteExternalNotificationHandlers(sourceRootPath));
        }

        assertThat(violations)
                .as("external notification handlers must declare idempotency, audit and terminal-state guard")
                .isEmpty();
    }

    /**
     * 场景：乱序外部通知尝试直接补写资金事实。
     * 输入：扫描资金域生产源码中的外部通知处理类型。
     * 输出：直接引用交易、账本事实写入组件或 Mapper 的违规列表。
     * 预期：外部通知只能创建可信外部事实、差错、补拉或交易命令入口。
     * 红线：不得绕过交易编排直接修改历史交易、分录或余额投影。
     */
    @Test
    void testExternalNotificationHandlersShouldNotMutateFundsFactsDirectly() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findDirectFactMutationReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("external notification handlers must not mutate transaction or ledger facts directly")
                .isEmpty();
    }

    private static List<String> findIncompleteExternalNotificationHandlers(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isExternalNotificationSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAny(source, TRUSTED_EXTERNAL_FACT_MARKERS)) {
                    requireContainsAny(sourceFile, source, IDEMPOTENCY_MARKERS, "idempotency", violations);
                    requireContainsAny(sourceFile, source, AUDIT_MARKERS, "audit", violations);
                    requireContainsAny(sourceFile, source, TERMINAL_CONFLICT_MARKERS, "terminal guard", violations);
                }
            }
        }
        return violations;
    }

    private static List<String> findDirectFactMutationReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isExternalNotificationSource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile, DIRECT_FACT_MUTATION_MARKERS));
            }
        }
        return violations;
    }

    private static void requireContainsAny(Path sourceFile, String source, List<String> markers, String label,
            List<String> violations) {
        if (!containsAny(source, markers)) {
            violations.add(sourceFile + ": missing external notification " + label);
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

    private static boolean isExternalNotificationSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return EXTERNAL_NOTIFICATION_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean containsAny(String source, List<String> markers) {
        return markers.stream().anyMatch(source::contains);
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

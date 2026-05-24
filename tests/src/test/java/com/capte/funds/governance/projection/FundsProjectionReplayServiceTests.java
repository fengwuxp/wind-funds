package com.capte.funds.governance.projection;

import com.capte.funds.governance.enums.ProjectionCheckpointType;
import com.capte.funds.governance.enums.ProjectionReplayMode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 交易投影重放边界测试。
 */
class FundsProjectionReplayServiceTests {

    /**
     * 场景：运营人员发起交易投影重放，但没有指定单笔、主体、时间窗口或批次范围。
     * 输入：`VERIFY_ONLY` 模式、空范围、合法交易投影 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：在线交易投影重放必须有明确范围。
     * 红线：不得无范围全量重放，避免把投影修复任务扩散成生产批量风险。
     */
    @Test
    void testReplayWithoutBoundedRangeShouldFail() {
        FundsProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = replayRequest(FundsTransactionProjectionReplayRange.builder()
                .build());

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放必须指定单笔、主体、时间窗口或批次范围");
    }

    /**
     * 场景：运营人员发起主体维度交易投影重放，但只填写 ownerType，缺少 ownerId。
     * 输入：`VERIFY_ONLY` 模式、半截主体范围、合法交易投影 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：主体范围必须同时包含主体类型和主体 ID，不能扩大为同类型全量重放。
     * 红线：交易投影重放不得把不完整主体条件当成有效范围。
     */
    @Test
    void testReplayWithIncompleteOwnerRangeShouldFail() {
        FundsProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = replayRequest(FundsTransactionProjectionReplayRange.builder()
                .ownerType("USER")
                .build());

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放必须指定单笔、主体、时间窗口或批次范围");
    }

    /**
     * 场景：运营人员按时间窗口发起交易投影重放，但结束时间早于开始时间。
     * 输入：`VERIFY_ONLY` 模式、倒置时间窗口、合法交易投影 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：时间窗口必须是正向有界范围。
     * 红线：交易投影重放不得把无效时间窗当成有效范围，避免误触发无界或异常批量重放。
     */
    @Test
    void testReplayWithInvalidTimeRangeShouldFail() {
        FundsProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = replayRequest(FundsTransactionProjectionReplayRange.builder()
                .startTime(LocalDateTime.of(2026, 5, 20, 0, 0))
                .endTime(LocalDateTime.of(2026, 5, 19, 0, 0))
                .build());

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放必须指定单笔、主体、时间窗口或批次范围");
    }

    /**
     * 场景：调用方发起交易投影重放，但没有提供 checkpoint 流水号。
     * 输入：单笔重放范围、缺少 checkpointSn 的交易投影 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：交易投影重放必须明确处理边界流水号。
     * 红线：交易投影重放不得接收不可追踪的 checkpoint。
     */
    @Test
    void testReplayWithoutCheckpointSnShouldFail() {
        FundsProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = FundsTransactionProjectionReplayRequest.builder()
                .taskSn("TPR-202605190002")
                .mode(ProjectionReplayMode.VERIFY_ONLY)
                .viewDomain("USER_BILL")
                .replayRange(FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build())
                .checkpoint(FundsTransactionProjectionCheckpoint.builder()
                        .type(ProjectionCheckpointType.TRANSACTION_PROJECTION)
                        .checkpointSn("")
                        .build())
                .build();

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放 checkpoint 流水号不能为空");
    }

    /**
     * 场景：调用方发起交易投影重放，但 checkpoint 没有声明所属水位域。
     * 输入：单笔重放范围、缺少类型的 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：交易投影重放必须显式使用交易投影自己的 checkpoint。
     * 红线：不得让无类型 checkpoint 在交易投影、余额、归档或指标域之间被复用。
     */
    @Test
    void testReplayWithoutCheckpointTypeShouldFail() {
        FundsProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = FundsTransactionProjectionReplayRequest.builder()
                .taskSn("TPR-202605190003")
                .mode(ProjectionReplayMode.VERIFY_ONLY)
                .viewDomain("USER_BILL")
                .replayRange(FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build())
                .checkpoint(FundsTransactionProjectionCheckpoint.builder()
                        .checkpointSn("TPC-202605190003")
                        .build())
                .build();

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放 checkpoint 类型不能为空");
    }

    /**
     * 场景：治理域仍未打开归档、余额快照和指标水位编码准入。
     * 输入：当前 `ProjectionCheckpointType` 枚举集合。
     * 输出：枚举只包含交易投影 checkpoint。
     * 预期：交易投影重放入口不承接余额、归档、报表或指标水位。
     * 红线：不得用交易投影 checkpoint 替代余额水位、归档 Manifest 或指标水位。
     */
    @Test
    void testProjectionCheckpointTypeShouldRemainTransactionProjectionOnly() {
        assertThat(ProjectionCheckpointType.values())
                .containsExactly(ProjectionCheckpointType.TRANSACTION_PROJECTION);
    }

    /**
     * 场景：先以校验模式重放单笔用户账单，准备生成差异报告。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、合法交易投影 checkpoint。
     * 输出：读取事实并产出差异报告，不写影子投影，不写正式投影。
     * 预期：交易投影重放只修复只读视图，校验模式没有写副作用。
     * 红线：交易投影重放不得重新入账、不得补写交易明细、不得修改余额投影。
     */
    @Test
    void testVerifyOnlyReplayShouldCompareButNotWriteProjection() {
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayService service = newService(writer);

        FundsTransactionProjectionReplayRange replayRange = FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build();
        FundsTransactionProjectionReplayResult result = service.replay(replayRequest(replayRange));

        assertThat(result.loadedFactCount()).isEqualTo(1);
        assertThat(result.rebuiltRowCount()).isEqualTo(1);
        assertThat(result.differences()).singleElement().satisfies(difference -> {
            assertThat(difference.sourceSn()).isEqualTo("FT202605190001");
            assertThat(difference.fieldName()).isEqualTo("displayStatus");
        });
        assertThat(writer.comparedRows()).singleElement().satisfies(row -> {
            assertThat(row.projectionSn()).isEqualTo("TP-FT202605190001");
            assertThat(row.displayStatus()).isEqualTo("SUCCEEDED");
        });
        assertThat(writer.shadowWrites()).isEmpty();
        assertThat(writer.officialWrites()).isEmpty();
    }

    /**
     * 场景：运营人员以影子模式重放单笔用户账单，用于正式覆盖前的灰度核对。
     * 输入：单笔重放范围、`REBUILD_SHADOW` 模式、合法交易投影 checkpoint。
     * 输出：读取事实、比较差异并写入影子投影。
     * 预期：影子模式只写影子投影，不覆盖正式投影。
     * 红线：影子重放不得借灰度核对修改正式用户账单、交易事实、账本事实或余额投影。
     */
    @Test
    void testShadowReplayShouldWriteOnlyShadowProjection() {
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayService service = newService(writer);

        FundsTransactionProjectionReplayResult result = service.replay(replayRequest(
                ProjectionReplayMode.REBUILD_SHADOW,
                FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build()));

        assertThat(result.mode()).isEqualTo(ProjectionReplayMode.REBUILD_SHADOW);
        assertThat(result.loadedFactCount()).isEqualTo(1);
        assertThat(result.rebuiltRowCount()).isEqualTo(1);
        assertThat(writer.comparedRows()).hasSize(1);
        assertThat(writer.shadowWrites()).singleElement()
                .satisfies(row -> assertThat(row.projectionSn()).isEqualTo("TP-FT202605190001"));
        assertThat(writer.officialWrites()).isEmpty();
    }

    /**
     * 场景：影子核对完成后，运营人员以正式重建模式刷新只读用户账单。
     * 输入：单笔重放范围、`REBUILD_APPLY` 模式、合法交易投影 checkpoint。
     * 输出：读取事实、比较差异并写入正式投影。
     * 预期：正式重建只覆盖只读投影，不写影子投影。
     * 红线：正式投影重放仍不得重新入账、补写交易事实、修改账本分录或修正余额投影。
     */
    @Test
    void testApplyReplayShouldWriteOnlyOfficialProjection() {
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayService service = newService(writer);

        FundsTransactionProjectionReplayResult result = service.replay(replayRequest(
                ProjectionReplayMode.REBUILD_APPLY,
                FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build()));

        assertThat(result.mode()).isEqualTo(ProjectionReplayMode.REBUILD_APPLY);
        assertThat(result.loadedFactCount()).isEqualTo(1);
        assertThat(result.rebuiltRowCount()).isEqualTo(1);
        assertThat(writer.comparedRows()).hasSize(1);
        assertThat(writer.shadowWrites()).isEmpty();
        assertThat(writer.officialWrites()).singleElement()
                .satisfies(row -> assertThat(row.projectionSn()).isEqualTo("TP-FT202605190001"));
    }

    /**
     * 场景：运营人员重放用户账单投影，但来源事实缺少使用者解释视图所需的操作状态、下一步动作和脱敏证据。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、缺解释载荷的交易投影事实。
     * 输出：服务拒绝重建投影行。
     * 预期：错误指向缺少使用者解释视图字段。
     * 红线：用户账单、商户账单或运营时间线缺事实状态、展示状态或操作状态时，不得展示为可操作视图。
     */
    @Test
    void testReplayFactWithoutExplainabilityPayloadShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new IncompleteProjectionReplaySource(),
                new RecordingProjectionWriter());

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放缺少使用者解释视图字段")
                .hasMessageContaining("operationStatus");
    }

    /**
     * 场景：运营人员重放用户账单投影，但来源事实缺少金额来源解释。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、缺 amountSource 的交易投影事实。
     * 输出：服务拒绝重建投影行。
     * 预期：错误指向缺少 amountSource 字段。
     * 红线：用户账单、商户账单或运营时间线不能只给金额数字，必须能追溯金额来源。
     */
    @Test
    void testReplayFactWithoutAmountSourceShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new MissingAmountSourceReplaySource(),
                new RecordingProjectionWriter());

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放缺少使用者解释视图字段")
                .hasMessageContaining("amountSource");
    }

    private static FundsProjectionReplayService newService(RecordingProjectionWriter writer) {
        return new FundsProjectionReplayService(new FixedProjectionReplaySource(), writer);
    }

    private static FundsTransactionProjectionReplayRequest replayRequest(
            FundsTransactionProjectionReplayRange replayRange) {
        return replayRequest(ProjectionReplayMode.VERIFY_ONLY, replayRange);
    }

    private static FundsTransactionProjectionReplayRequest replayRequest(
            ProjectionReplayMode mode,
            FundsTransactionProjectionReplayRange replayRange) {
        return FundsTransactionProjectionReplayRequest.builder()
                .taskSn("TPR-202605190001")
                .mode(mode)
                .viewDomain("USER_BILL")
                .replayRange(replayRange)
                .checkpoint(FundsTransactionProjectionCheckpoint.builder()
                        .type(ProjectionCheckpointType.TRANSACTION_PROJECTION)
                        .checkpointSn("TPC-202605190001")
                        .build())
                .build();
    }

    private static final class FixedProjectionReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            return List.of(FundsTransactionProjectionFact.builder()
                    .viewDomain("USER_BILL")
                    .ownerType("USER")
                    .ownerId("U1001")
                    .sourceSn("FT202605190001")
                    .displayType("PAYMENT")
                    .displayStatus("SUCCEEDED")
                    .amount(100L)
                    .currency("USD")
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(explainablePayload())
                    .build());
        }
    }

    private static final class IncompleteProjectionReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            return List.of(FundsTransactionProjectionFact.builder()
                    .viewDomain("USER_BILL")
                    .ownerType("USER")
                    .ownerId("U1001")
                    .sourceSn("FT202605190001")
                    .displayType("PAYMENT")
                    .displayStatus("SUCCEEDED")
                    .amount(100L)
                    .currency("USD")
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(Map.of("businessScene", "ORDER_PAY", "factStatus", "POSTED"))
                    .build());
        }
    }

    private static final class MissingAmountSourceReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            return List.of(FundsTransactionProjectionFact.builder()
                    .viewDomain("USER_BILL")
                    .ownerType("USER")
                    .ownerId("U1001")
                    .sourceSn("FT202605190001")
                    .displayType("PAYMENT")
                    .displayStatus("SUCCEEDED")
                    .amount(100L)
                    .currency("USD")
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(Map.of(
                            "businessScene", "ORDER_PAY",
                            "factStatus", "POSTED",
                            "operationStatus", "NO_ACTION_REQUIRED",
                            "statusMeaning", "payment posted",
                            "unavailableReason", "N/A",
                            "nextAction", "N/A",
                            "evidenceRefs", List.of("routeSnapshot:RS-202605190001"),
                            "externalRuleVerificationStatus", "N/A"))
                    .build());
        }
    }

    private static Map<String, Object> explainablePayload() {
        return Map.of(
                "businessScene", "ORDER_PAY",
                "factStatus", "POSTED",
                "operationStatus", "NO_ACTION_REQUIRED",
                "statusMeaning", "payment posted",
                "amountSource", "instructionAmount=100 USD, routeSnapshot=RS-202605190001",
                "unavailableReason", "N/A",
                "nextAction", "N/A",
                "evidenceRefs", List.of("routeSnapshot:RS-202605190001"),
                "externalRuleVerificationStatus", "N/A");
    }

    private static final class RecordingProjectionWriter implements FundsTransactionProjectionWriter {

        private final List<FundsTransactionProjectionRow> comparedRows = new ArrayList<>();

        private final List<FundsTransactionProjectionRow> shadowWrites = new ArrayList<>();

        private final List<FundsTransactionProjectionRow> officialWrites = new ArrayList<>();

        @Override
        public List<FundsTransactionProjectionDifference> compare(String viewDomain,
                                                                  List<FundsTransactionProjectionRow> rebuiltRows) {
            comparedRows.addAll(rebuiltRows);
            return List.of(FundsTransactionProjectionDifference.builder()
                    .sourceSn(rebuiltRows.getFirst().sourceSn())
                    .fieldName("displayStatus")
                    .expectedValue("SUCCEEDED")
                    .actualValue("FAILED")
                    .build());
        }

        @Override
        public void upsertShadow(String taskSn, List<FundsTransactionProjectionRow> rebuiltRows) {
            shadowWrites.addAll(rebuiltRows);
        }

        @Override
        public void upsertOfficial(String taskSn, List<FundsTransactionProjectionRow> rebuiltRows) {
            officialWrites.addAll(rebuiltRows);
        }

        private List<FundsTransactionProjectionRow> comparedRows() {
            return comparedRows;
        }

        private List<FundsTransactionProjectionRow> shadowWrites() {
            return shadowWrites;
        }

        private List<FundsTransactionProjectionRow> officialWrites() {
            return officialWrites;
        }
    }
}

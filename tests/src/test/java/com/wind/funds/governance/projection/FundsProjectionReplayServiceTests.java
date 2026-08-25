package com.wind.funds.governance.projection;

import com.wind.funds.governance.enums.ProjectionCheckpointType;
import com.wind.funds.governance.enums.ProjectionReplayMode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
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
     * 场景：旧端口实现只实现不带 tenantId 的直接重放方法，却被持久任务控制面调用。
     * 预期：tenant-aware 来源和写入入口全部失败关闭，不能静默降级为无租户实现。
     */
    @Test
    void testPersistentReplayPortsShouldRejectLegacyTenantlessFallback() {
        FixedProjectionReplaySource source = new FixedProjectionReplaySource();
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsTransactionProjectionReplayRange range = FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build();
        FundsTransactionProjectionCheckpoint checkpoint = FundsTransactionProjectionCheckpoint.builder()
                .type(ProjectionCheckpointType.TRANSACTION_PROJECTION)
                .checkpointSn("0:0:0:0")
                .build();

        assertThatThrownBy(() -> source.initializeCheckpoint(1L, "USER_BILL", range))
                .hasMessageContaining("必须显式实现 tenant 有界");
        assertThatThrownBy(() -> source.loadFactBatch(1L, "USER_BILL", range, checkpoint, 100))
                .hasMessageContaining("必须显式实现 tenant 有界");
        assertThatThrownBy(() -> writer.compare(1L, "USER_BILL", List.of()))
                .hasMessageContaining("必须显式实现 tenant 有界");
        assertThatThrownBy(() -> writer.upsertShadow(1L, "TASK-1", List.of()))
                .hasMessageContaining("必须显式实现 tenant 有界");
        assertThatThrownBy(() -> writer.upsertOfficial(1L, "TASK-1", List.of()))
                .hasMessageContaining("必须显式实现 tenant 有界");
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
     * 场景：控制面、审批与审计证据尚未落地时请求影子重放。
     * 预期：服务在读取来源事实和调用 Writer 前拒绝请求。
     * 红线：不能把有界范围和 checkpoint 当成影子写入授权。
     */
    @Test
    void testShadowReplayWithoutControlPlaneShouldFailClosed() {
        RecordingProjectionReplaySource source = new RecordingProjectionReplaySource();
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayService service = new FundsProjectionReplayService(source, writer);

        assertThatThrownBy(() -> service.replay(replayRequest(
                ProjectionReplayMode.REBUILD_SHADOW,
                FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build())))
                .hasMessageContaining("交易投影重放控制面未开放")
                .hasMessageContaining("VERIFY_ONLY");

        assertThat(source.loadCalls()).isZero();
        assertThat(writer.comparedRows()).isEmpty();
        assertThat(writer.shadowWrites()).isEmpty();
        assertThat(writer.officialWrites()).isEmpty();
    }

    /**
     * 场景：控制面、审批与审计证据尚未落地时请求正式重放。
     * 预期：服务在读取来源事实和调用 Writer 前拒绝请求。
     * 红线：不能在缺少控制面时覆盖正式投影。
     */
    @Test
    void testApplyReplayWithoutControlPlaneShouldFailClosed() {
        RecordingProjectionReplaySource source = new RecordingProjectionReplaySource();
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayService service = new FundsProjectionReplayService(source, writer);

        assertThatThrownBy(() -> service.replay(replayRequest(
                ProjectionReplayMode.REBUILD_APPLY,
                FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build())))
                .hasMessageContaining("交易投影重放控制面未开放")
                .hasMessageContaining("VERIFY_ONLY");

        assertThat(source.loadCalls()).isZero();
        assertThat(writer.comparedRows()).isEmpty();
        assertThat(writer.shadowWrites()).isEmpty();
        assertThat(writer.officialWrites()).isEmpty();
    }

    /**
     * 场景：投影写入端口比较差异时错误返回了空差异列表对象。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、返回 null 差异列表的 writer。
     * 输出：服务拒绝生成重放结果。
     * 预期：差异报告列表必须显式返回，完全一致时也应返回空列表。
     * 红线：交易投影重放结果不得携带不可审计的 null 差异报告。
     */
    @Test
    void testReplayWithNullDifferencesShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(null));

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放差异列表不能为空");
    }

    /**
     * 场景：投影写入端口比较差异时返回了 null 差异项。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、差异列表中包含 null 项。
     * 输出：服务拒绝生成重放结果。
     * 预期：每个差异项都必须是完整对象。
     * 红线：交易投影重放不得输出无法被人工复核和审计追踪的空差异项。
     */
    @Test
    void testReplayWithNullDifferenceItemShouldFail() {
        List<FundsTransactionProjectionDifference> differences = new ArrayList<>();
        differences.add(null);
        FundsProjectionReplayService service = new FundsProjectionReplayService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(differences));

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放差异项不能为空");
    }

    /**
     * 场景：投影写入端口比较差异时返回了缺少字段名的差异项。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、`fieldName` 为空的差异项。
     * 输出：服务拒绝生成重放结果。
     * 预期：差异项必须说明是哪一笔来源事实的哪个字段发生差异。
     * 红线：交易投影重放不得输出无法定位字段的差异报告。
     */
    @Test
    void testReplayWithBlankDifferenceFieldNameShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(List.of(FundsTransactionProjectionDifference.builder()
                        .sourceSn("FT202605190001")
                        .fieldName("")
                        .expectedValue("SUCCEEDED")
                        .actualValue("FAILED")
                        .build())));

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放差异项字段不能为空")
                .hasMessageContaining("fieldName");
    }

    /**
     * 场景：投影写入端口比较差异时返回了缺少来源交易号的差异项。
     * 输入：单笔重放范围、`VERIFY_ONLY` 模式、`sourceSn` 为空的差异项。
     * 输出：服务拒绝生成重放结果。
     * 预期：差异项必须绑定可追踪的来源事实。
     * 红线：交易投影重放不得输出无法追溯来源事实的差异报告。
     */
    @Test
    void testReplayWithBlankDifferenceSourceSnShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(List.of(FundsTransactionProjectionDifference.builder()
                        .sourceSn("")
                        .fieldName("displayStatus")
                        .expectedValue("SUCCEEDED")
                        .actualValue("FAILED")
                        .build())));

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放差异项字段不能为空")
                .hasMessageContaining("sourceSn");
    }

    /**
     * 场景：重放源错误返回了其他视图域的交易投影事实。
     * 输入：请求视图域为 `USER_BILL`，来源事实视图域为 `MERCHANT_BILL`。
     * 输出：服务拒绝重建投影行。
     * 预期：错误指向来源事实不属于请求视图域。
     * 红线：用户账单、商户账单和运营时间线不能在同一次重放中串域覆盖。
     */
    @Test
    void testReplayFactWithMismatchedViewDomainShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new MismatchedViewDomainReplaySource(),
                new RecordingProjectionWriter());

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放来源事实不属于请求视图域")
                .hasMessageContaining("MERCHANT_BILL")
                .hasMessageContaining("USER_BILL");
    }

    /**
     * 场景：重放源错误返回了本次 sourceSn 范围外的交易投影事实。
     * 输入：请求只重放 `FT202605190001`，来源事实却是 `FT202605190999`。
     * 输出：服务拒绝重建投影行。
     * 预期：错误指向来源事实不属于请求范围。
     * 红线：交易投影重放不能因读取源异常把影响面扩大到本次授权范围外。
     */
    @Test
    void testReplayFactOutsideSourceRangeShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new OutsideSourceRangeReplaySource(),
                new RecordingProjectionWriter());

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放来源事实不属于请求范围")
                .hasMessageContaining("sourceSn")
                .hasMessageContaining("FT202605190999");
    }

    /**
     * 场景：重放源返回了缺少来源交易号的投影事实。
     * 输入：主体范围重放、来源事实 `sourceSn` 为空。
     * 输出：服务拒绝重建投影行。
     * 预期：错误指向来源事实字段不完整。
     * 红线：交易投影重放不能生成不可追踪的投影行或差异报告。
     */
    @Test
    void testReplayFactWithoutSourceSnShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new BlankSourceSnReplaySource(),
                new RecordingProjectionWriter());

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .ownerType("USER")
                .ownerId("U1001")
                .build())))
                .hasMessageContaining("交易投影重放来源事实字段不能为空")
                .hasMessageContaining("sourceSn");
    }

    /**
     * 场景：重放源返回了缺少扩展载荷的投影事实。
     * 输入：单笔重放范围、来源事实 `payload` 为空。
     * 输出：服务拒绝重建投影行。
     * 预期：错误指向来源事实字段不完整。
     * 红线：交易投影重放不得绕过使用者解释视图、证据最小化和外部规则核验字段。
     */
    @Test
    void testReplayFactWithoutPayloadShouldFail() {
        FundsProjectionReplayService service = new FundsProjectionReplayService(new NullPayloadReplaySource(),
                new RecordingProjectionWriter());

        assertThatThrownBy(() -> service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build())))
                .hasMessageContaining("交易投影重放来源事实字段不能为空")
                .hasMessageContaining("payload");
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
                    .currency(CurrencyIsoCode.USD)
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(explainablePayload())
                    .build());
        }
    }

    private static final class RecordingProjectionReplaySource implements FundsTransactionProjectionReplaySource {

        private int loadCalls;

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            loadCalls++;
            return new FixedProjectionReplaySource().loadFacts(range);
        }

        private int loadCalls() {
            return loadCalls;
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
                    .currency(CurrencyIsoCode.USD)
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(Map.of("businessScene", "ORDER_PAY", "factStatus", "POSTED"))
                    .build());
        }
    }

    private static final class MismatchedViewDomainReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            return List.of(FundsTransactionProjectionFact.builder()
                    .viewDomain("MERCHANT_BILL")
                    .ownerType("USER")
                    .ownerId("U1001")
                    .sourceSn("FT202605190001")
                    .displayType("PAYMENT")
                    .displayStatus("SUCCEEDED")
                    .amount(100L)
                    .currency(CurrencyIsoCode.USD)
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(explainablePayload())
                    .build());
        }
    }

    private static final class OutsideSourceRangeReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            return List.of(FundsTransactionProjectionFact.builder()
                    .viewDomain("USER_BILL")
                    .ownerType("USER")
                    .ownerId("U1001")
                    .sourceSn("FT202605190999")
                    .displayType("PAYMENT")
                    .displayStatus("SUCCEEDED")
                    .amount(100L)
                    .currency(CurrencyIsoCode.USD)
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(explainablePayload())
                    .build());
        }
    }

    private static final class BlankSourceSnReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            return List.of(FundsTransactionProjectionFact.builder()
                    .viewDomain("USER_BILL")
                    .ownerType("USER")
                    .ownerId("U1001")
                    .sourceSn("")
                    .displayType("PAYMENT")
                    .displayStatus("SUCCEEDED")
                    .amount(100L)
                    .currency(CurrencyIsoCode.USD)
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                    .payload(explainablePayload())
                    .build());
        }
    }

    private static final class NullPayloadReplaySource implements FundsTransactionProjectionReplaySource {

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
                    .currency(CurrencyIsoCode.USD)
                    .occurredTime(LocalDateTime.of(2026, 5, 19, 12, 0))
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
                    .currency(CurrencyIsoCode.USD)
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

    private static final class StaticDifferencesProjectionWriter implements FundsTransactionProjectionWriter {

        private final List<FundsTransactionProjectionDifference> differences;

        private StaticDifferencesProjectionWriter(List<FundsTransactionProjectionDifference> differences) {
            this.differences = differences;
        }

        @Override
        public List<FundsTransactionProjectionDifference> compare(String viewDomain,
                                                                  List<FundsTransactionProjectionRow> rebuiltRows) {
            return differences;
        }

        @Override
        public void upsertShadow(String taskSn, List<FundsTransactionProjectionRow> rebuiltRows) {
            // No-op: this test only verifies compare contract hardening.
        }

        @Override
        public void upsertOfficial(String taskSn, List<FundsTransactionProjectionRow> rebuiltRows) {
            // No-op: this test only verifies compare contract hardening.
        }
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

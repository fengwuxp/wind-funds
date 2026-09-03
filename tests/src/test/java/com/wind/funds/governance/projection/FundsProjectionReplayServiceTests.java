package com.wind.funds.governance.projection;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.governance.enums.ProjectionCheckpointType;
import com.wind.funds.governance.enums.ProjectionReplayMode;
import com.wind.funds.governance.enums.ProjectionReplayTaskState;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionFact;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionFactBatch;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionReplaySource;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionRow;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionWriter;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * 交易投影重放边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsProjectionReplayServiceTests.Config.class
})
class FundsProjectionReplayServiceTests extends AbstractFundsServiceTest {

    private static final String VIEW_DOMAIN = "USER_BILL";

    @Autowired
    private FundsProjectionReplayApplicationService projectionReplayApplicationService;

    @Autowired
    private SwitchableProjectionReplaySource switchableSource;

    @Autowired
    private SwitchableProjectionWriter switchableWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private int requestSequence;

    @BeforeEach
    void setUpProjectionReplayService() {
        requestSequence = 0;
        TransactionTemplate cleanup = new TransactionTemplate(transactionManager);
        cleanup.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        cleanup.executeWithoutResult(status -> {
            jdbcTemplate.update("DELETE FROM t_projection_replay_difference");
            jdbcTemplate.update("DELETE FROM t_funds_transaction_projection");
            jdbcTemplate.update("DELETE FROM t_projection_replay_task");
        });
        switchableSource.use(new FixedProjectionReplaySource());
        switchableWriter.use(new RecordingProjectionWriter());
    }

    @Test
    void testPublicContractShouldExposeOnlyPersistentTenantAwareReplay() throws NoSuchMethodException {
        Method initializeCheckpoint = FundsTransactionProjectionReplaySource.class.getMethod(
                "initializeCheckpoint", Long.class, String.class,
                FundsTransactionProjectionReplayRange.class);
        Method loadFactBatch = FundsTransactionProjectionReplaySource.class.getMethod(
                "loadFactBatch", Long.class, String.class,
                FundsTransactionProjectionReplayRange.class,
                FundsTransactionProjectionCheckpoint.class, int.class);
        Method compare = FundsTransactionProjectionWriter.class.getMethod(
                "compare", Long.class, String.class, List.class);
        Method upsertShadow = FundsTransactionProjectionWriter.class.getMethod(
                "upsertShadow", Long.class, String.class, List.class);
        Method upsertOfficial = FundsTransactionProjectionWriter.class.getMethod(
                "upsertOfficial", Long.class, String.class, List.class);
        boolean hasLegacyConstructor = Arrays.stream(FundsProjectionReplayService.class.getConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 2
                        && constructor.getParameterTypes()[0] == FundsTransactionProjectionReplaySource.class
                        && constructor.getParameterTypes()[1] == FundsTransactionProjectionWriter.class);
        boolean hasLegacyReplay = Arrays.stream(FundsProjectionReplayService.class.getMethods())
                .anyMatch(method -> method.getName().equals("replay"));

        assertSoftly(softly -> {
            softly.assertThat(isClassPresent(
                            "com.wind.funds.governance.projection.FundsTransactionProjectionReplayRequest"))
                    .as("legacy direct replay request must not remain public")
                    .isFalse();
            softly.assertThat(hasLegacyConstructor)
                    .as("legacy two-argument replay service constructor must be removed")
                    .isFalse();
            softly.assertThat(hasLegacyReplay)
                    .as("legacy direct replay method must be removed")
                    .isFalse();
            softly.assertThat(hasPublicMethod(FundsTransactionProjectionReplaySource.class,
                            "loadFacts", FundsTransactionProjectionReplayRange.class))
                    .as("tenantless replay source method must be removed")
                    .isFalse();
            softly.assertThat(hasPublicMethod(FundsTransactionProjectionWriter.class,
                            "compare", String.class, List.class))
                    .as("tenantless projection compare method must be removed")
                    .isFalse();
            softly.assertThat(hasPublicMethod(FundsTransactionProjectionWriter.class,
                            "upsertShadow", String.class, List.class))
                    .as("tenantless shadow write method must be removed")
                    .isFalse();
            softly.assertThat(hasPublicMethod(FundsTransactionProjectionWriter.class,
                            "upsertOfficial", String.class, List.class))
                    .as("tenantless official write method must be removed")
                    .isFalse();
            softly.assertThat(List.of(initializeCheckpoint, loadFactBatch, compare, upsertShadow, upsertOfficial))
                    .allSatisfy(method -> {
                        softly.assertThat(Modifier.isAbstract(method.getModifiers()))
                                .as("%s must be abstract", method)
                                .isTrue();
                        softly.assertThat(method.isDefault())
                                .as("%s must not provide a default fallback", method)
                                .isFalse();
                    });
        });
    }

    @Test
    void testProjectionReplaySpiShouldBelongToGovernanceImplementation() {
        List<String> publicTypeNames = List.of(
                "com.wind.funds.governance.projection.FundsTransactionProjectionReplaySource",
                "com.wind.funds.governance.projection.FundsTransactionProjectionWriter",
                "com.wind.funds.governance.projection.FundsTransactionProjectionFact",
                "com.wind.funds.governance.projection.FundsTransactionProjectionFactBatch",
                "com.wind.funds.governance.projection.FundsTransactionProjectionRow");
        List<String> internalTypeNames = List.of(
                "com.wind.funds.governance.projection.internal.FundsTransactionProjectionReplaySource",
                "com.wind.funds.governance.projection.internal.FundsTransactionProjectionWriter",
                "com.wind.funds.governance.projection.internal.FundsTransactionProjectionFact",
                "com.wind.funds.governance.projection.internal.FundsTransactionProjectionFactBatch",
                "com.wind.funds.governance.projection.internal.FundsTransactionProjectionRow");

        assertSoftly(softly -> {
            publicTypeNames.forEach(typeName -> softly.assertThat(isClassPresent(typeName))
                    .as("governance face must not publish internal replay type %s", typeName)
                    .isFalse());
            internalTypeNames.forEach(typeName -> softly.assertThat(isClassPresent(typeName))
                    .as("governance implementation must own replay type %s", typeName)
                    .isTrue());
            Arrays.stream(FundsProjectionReplayApplicationService.class.getMethods())
                    .filter(method -> method.getDeclaringClass() == FundsProjectionReplayApplicationService.class)
                    .forEach(method -> softly.assertThat(method.toGenericString())
                            .as("public replay application service must not expose internal types")
                            .doesNotContain(".projection.internal."));
        });
    }

    /**
     * 场景：运营人员发起交易投影重放，但没有指定单笔、主体、时间窗口或批次范围。
     * 输入：`VERIFY_ONLY` 模式、空范围、合法交易投影 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：在线交易投影重放必须有明确范围。
     * 红线：不得无范围全量重放，避免把投影修复任务扩散成生产批量风险。
     */
    @Test
    void testReplayWithoutBoundedRangeShouldFail() {
        FundsProjectionReplayApplicationService service = newService(new RecordingProjectionWriter());
        assertThatThrownBy(() -> createTask(service, ProjectionReplayMode.VERIFY_ONLY,
                FundsTransactionProjectionReplayRange.builder().build()))
                .hasMessageContaining("投影重放任务必须指定有界范围");
        assertThat(taskCount()).isZero();
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
        FundsProjectionReplayApplicationService service = newService(new RecordingProjectionWriter());
        assertThatThrownBy(() -> createTask(service, ProjectionReplayMode.VERIFY_ONLY,
                FundsTransactionProjectionReplayRange.builder().ownerType("USER").build()))
                .hasMessageContaining("投影重放任务必须指定有界范围");
        assertThat(taskCount()).isZero();
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
        FundsProjectionReplayApplicationService service = newService(new RecordingProjectionWriter());
        assertThatThrownBy(() -> createTask(service, ProjectionReplayMode.VERIFY_ONLY,
                FundsTransactionProjectionReplayRange.builder()
                        .startTime(LocalDateTime.of(2026, 5, 20, 0, 0))
                        .endTime(LocalDateTime.of(2026, 5, 19, 0, 0))
                        .build()))
                .hasMessageContaining("投影重放任务必须指定有界范围");
        assertThat(taskCount()).isZero();
    }

    /**
     * 场景：重放来源初始化时返回了缺少流水号的 checkpoint。
     * 输入：单笔重放范围、缺少 checkpointSn 的来源 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：交易投影重放必须明确处理边界流水号。
     * 红线：交易投影重放不得接收不可追踪的 checkpoint。
     */
    @Test
    void testReplayWithoutCheckpointSnShouldFail() {
        FundsProjectionReplayApplicationService service = newService(new CheckpointProjectionReplaySource(
                FundsTransactionProjectionCheckpoint.builder()
                        .type(ProjectionCheckpointType.TRANSACTION_PROJECTION)
                        .checkpointSn("")
                        .build()), new RecordingProjectionWriter());

        assertThatThrownBy(() -> createTask(service, ProjectionReplayMode.VERIFY_ONLY, sourceRange()))
                .hasMessageContaining("交易投影重放 checkpoint 流水号不能为空");
        assertThat(taskCount()).isZero();
    }

    /**
     * 场景：重放来源初始化时返回了未声明水位域的 checkpoint。
     * 输入：单笔重放范围、缺少类型的来源 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：交易投影重放必须显式使用交易投影自己的 checkpoint。
     * 红线：不得让无类型 checkpoint 在交易投影、余额、归档或指标域之间被复用。
     */
    @Test
    void testReplayWithoutCheckpointTypeShouldFail() {
        FundsProjectionReplayApplicationService service = newService(new CheckpointProjectionReplaySource(
                FundsTransactionProjectionCheckpoint.builder()
                        .checkpointSn("TPC-202605190003")
                        .build()), new RecordingProjectionWriter());

        assertThatThrownBy(() -> createTask(service, ProjectionReplayMode.VERIFY_ONLY, sourceRange()))
                .hasMessageContaining("交易投影重放 checkpoint 类型不能为空");
        assertThat(taskCount()).isZero();
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
    void testPersistentReplayPortsShouldRejectLegacyTenantlessFallback() throws NoSuchMethodException {
        Method initializeCheckpoint = FundsTransactionProjectionReplaySource.class.getMethod(
                "initializeCheckpoint", Long.class, String.class, FundsTransactionProjectionReplayRange.class);
        Method loadFactBatch = FundsTransactionProjectionReplaySource.class.getMethod(
                "loadFactBatch", Long.class, String.class, FundsTransactionProjectionReplayRange.class,
                FundsTransactionProjectionCheckpoint.class, int.class);
        Method compare = FundsTransactionProjectionWriter.class.getMethod(
                "compare", Long.class, String.class, List.class);
        Method upsertShadow = FundsTransactionProjectionWriter.class.getMethod(
                "upsertShadow", Long.class, String.class, List.class);
        Method upsertOfficial = FundsTransactionProjectionWriter.class.getMethod(
                "upsertOfficial", Long.class, String.class, List.class);

        assertThat(List.of(initializeCheckpoint, loadFactBatch, compare, upsertShadow, upsertOfficial))
                .allSatisfy(method -> {
                    assertThat(Modifier.isAbstract(method.getModifiers())).isTrue();
                    assertThat(method.isDefault()).isFalse();
                });
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
        FundsProjectionReplayApplicationService service = newService(writer);
        FundsTransactionProjectionReplayResult result = replay(service, ProjectionReplayMode.VERIFY_ONLY,
                sourceRange());

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
     * 场景：通过持久控制面执行影子重放。
     * 预期：任务读取来源事实并只写影子投影，不写正式投影。
     * 红线：影子重放不得绕过 tenant、任务审计或写入正式投影。
     */
    @Test
    void testPersistentShadowReplayShouldWriteShadowOnly() {
        RecordingProjectionReplaySource source = new RecordingProjectionReplaySource();
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayApplicationService service = newService(source, writer);

        replay(service, ProjectionReplayMode.REBUILD_SHADOW, sourceRange());

        assertThat(source.loadCalls()).isEqualTo(1);
        assertThat(writer.comparedRows()).hasSize(1);
        assertThat(writer.shadowWrites()).hasSize(1);
        assertThat(writer.officialWrites()).isEmpty();
    }

    /**
     * 场景：持久控制面缺少审批证据时请求正式重放。
     * 预期：服务在初始化来源和创建任务前拒绝请求。
     * 红线：不能在缺少审批和已验证影子任务时覆盖正式投影。
     */
    @Test
    void testPersistentApplyWithoutApprovalShouldFailClosed() {
        RecordingProjectionReplaySource source = new RecordingProjectionReplaySource();
        RecordingProjectionWriter writer = new RecordingProjectionWriter();
        FundsProjectionReplayApplicationService service = newService(source, writer);

        assertThatThrownBy(() -> createTask(service, ProjectionReplayMode.REBUILD_APPLY, sourceRange()))
                .hasMessageContaining("正式投影重放必须提供审批引用");

        assertThat(source.loadCalls()).isZero();
        assertThat(writer.comparedRows()).isEmpty();
        assertThat(writer.shadowWrites()).isEmpty();
        assertThat(writer.officialWrites()).isEmpty();
        assertThat(taskCount()).isZero();
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
        FundsProjectionReplayApplicationService service = newService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(null));
        assertReplayFails(service, sourceRange(), "交易投影重放差异列表不能为空");
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
        FundsProjectionReplayApplicationService service = newService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(differences));
        assertReplayFails(service, sourceRange(), "交易投影重放差异项不能为空");
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
        FundsProjectionReplayApplicationService service = newService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(List.of(FundsTransactionProjectionDifference.builder()
                        .sourceSn("FT202605190001")
                        .fieldName("")
                        .expectedValue("SUCCEEDED")
                        .actualValue("FAILED")
                        .build())));
        assertReplayFails(service, sourceRange(), "交易投影重放差异项字段不能为空", "fieldName");
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
        FundsProjectionReplayApplicationService service = newService(new FixedProjectionReplaySource(),
                new StaticDifferencesProjectionWriter(List.of(FundsTransactionProjectionDifference.builder()
                        .sourceSn("")
                        .fieldName("displayStatus")
                        .expectedValue("SUCCEEDED")
                        .actualValue("FAILED")
                        .build())));
        assertReplayFails(service, sourceRange(), "交易投影重放差异项字段不能为空", "sourceSn");
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
        FundsProjectionReplayApplicationService service = newService(new MismatchedViewDomainReplaySource(),
                new RecordingProjectionWriter());
        assertReplayFails(service, sourceRange(), "交易投影重放来源事实不属于请求视图域",
                "MERCHANT_BILL", "USER_BILL");
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
        FundsProjectionReplayApplicationService service = newService(new OutsideSourceRangeReplaySource(),
                new RecordingProjectionWriter());
        assertReplayFails(service, sourceRange(), "交易投影重放来源事实不属于请求范围",
                "sourceSn", "FT202605190999");
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
        FundsProjectionReplayApplicationService service = newService(new BlankSourceSnReplaySource(),
                new RecordingProjectionWriter());
        assertReplayFails(service, ownerRange(), "交易投影重放来源事实字段不能为空", "sourceSn");
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
        FundsProjectionReplayApplicationService service = newService(new NullPayloadReplaySource(),
                new RecordingProjectionWriter());
        assertReplayFails(service, sourceRange(), "交易投影重放来源事实字段不能为空", "payload");
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
        FundsProjectionReplayApplicationService service = newService(new IncompleteProjectionReplaySource(),
                new RecordingProjectionWriter());
        assertReplayFails(service, sourceRange(), "交易投影重放缺少使用者解释视图字段",
                "operationStatus");
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
        FundsProjectionReplayApplicationService service = newService(new MissingAmountSourceReplaySource(),
                new RecordingProjectionWriter());
        assertReplayFails(service, sourceRange(), "交易投影重放缺少使用者解释视图字段", "amountSource");
    }

    private FundsProjectionReplayApplicationService newService(RecordingProjectionWriter writer) {
        return newService(new FixedProjectionReplaySource(), writer);
    }

    private FundsProjectionReplayApplicationService newService(FundsTransactionProjectionReplaySource source,
                                                                FundsTransactionProjectionWriter writer) {
        switchableSource.use(source);
        switchableWriter.use(writer);
        return projectionReplayApplicationService;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static boolean hasPublicMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            type.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }

    private FundsProjectionReplayTaskDTO createTask(FundsProjectionReplayApplicationService service,
                                                    ProjectionReplayMode mode,
                                                    FundsTransactionProjectionReplayRange range) {
        int sequence = ++requestSequence;
        return service.createTask(CreateFundsProjectionReplayTaskRequest.builder()
                .requestSn("REPLAY-TEST-" + sequence)
                .requestDigest("digest-" + sequence)
                .tenantId(TENANT_ID)
                .viewDomain(VIEW_DOMAIN)
                .mode(mode)
                .replayRange(range)
                .reason("verify projection replay contract")
                .auditRef("AUDIT-REPLAY-TEST")
                .build(), WindOperatorFactory.system());
    }

    private FundsTransactionProjectionReplayResult replay(FundsProjectionReplayApplicationService service,
                                                           ProjectionReplayMode mode,
                                                           FundsTransactionProjectionReplayRange range) {
        FundsProjectionReplayTaskDTO task = createTask(service, mode, range);
        return runTask(service, task.taskSn());
    }

    private FundsTransactionProjectionReplayResult runTask(FundsProjectionReplayApplicationService service, String taskSn) {
        return service.runTask(RunFundsProjectionReplayTaskRequest.builder()
                .tenantId(TENANT_ID)
                .taskSn(taskSn)
                .maxBatchSize(100)
                .build(), WindOperatorFactory.system());
    }

    private void assertReplayFails(FundsProjectionReplayApplicationService service,
                                   FundsTransactionProjectionReplayRange range,
                                   String... messageParts) {
        FundsProjectionReplayTaskDTO created = createTask(service, ProjectionReplayMode.VERIFY_ONLY, range);
        Throwable failure = catchThrowable(() -> runTask(service, created.taskSn()));
        assertThat(failure).isNotNull();
        for (String messagePart : messageParts) {
            assertThat(failure).hasMessageContaining(messagePart);
        }
        FundsProjectionReplayTaskDTO unchanged = service.getTask(
                TENANT_ID, created.taskSn(), WindOperatorFactory.system());
        assertThat(unchanged.state()).isEqualTo(ProjectionReplayTaskState.CREATED);
        assertThat(unchanged.checkpoint()).isEqualTo(created.checkpoint());
        assertThat(unchanged.successCount()).isZero();
        assertThat(unchanged.differenceCount()).isZero();
    }

    private long taskCount() {
        return projectionReplayApplicationService.queryBacklog(
                TENANT_ID, 500, WindOperatorFactory.system()).size();
    }

    private static FundsTransactionProjectionReplayRange sourceRange() {
        return FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build();
    }

    private static FundsTransactionProjectionReplayRange ownerRange() {
        return FundsTransactionProjectionReplayRange.builder()
                .ownerType("USER")
                .ownerId("U1001")
                .build();
    }

    private static FundsTransactionProjectionCheckpoint checkpoint(String checkpointSn) {
        return FundsTransactionProjectionCheckpoint.builder()
                .type(ProjectionCheckpointType.TRANSACTION_PROJECTION)
                .checkpointSn(checkpointSn)
                .build();
    }

    private static final class SwitchableProjectionReplaySource
            implements FundsTransactionProjectionReplaySource {

        private FundsTransactionProjectionReplaySource delegate;

        private void use(FundsTransactionProjectionReplaySource delegate) {
            this.delegate = delegate;
        }

        @Override
        public FundsTransactionProjectionCheckpoint initializeCheckpoint(Long tenantId,
                                                                         String viewDomain,
                                                                         FundsTransactionProjectionReplayRange range) {
            return delegate.initializeCheckpoint(tenantId, viewDomain, range);
        }

        @Override
        public FundsTransactionProjectionFactBatch loadFactBatch(Long tenantId,
                                                                 String viewDomain,
                                                                 FundsTransactionProjectionReplayRange range,
                                                                 FundsTransactionProjectionCheckpoint checkpoint,
                                                                 int maxBatchSize) {
            return delegate.loadFactBatch(tenantId, viewDomain, range, checkpoint, maxBatchSize);
        }
    }

    private static final class SwitchableProjectionWriter implements FundsTransactionProjectionWriter {

        private FundsTransactionProjectionWriter delegate;

        private void use(FundsTransactionProjectionWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<FundsTransactionProjectionDifference> compare(Long tenantId,
                                                                  String viewDomain,
                                                                  List<FundsTransactionProjectionRow> rebuiltRows) {
            return delegate.compare(tenantId, viewDomain, rebuiltRows);
        }

        @Override
        public void upsertShadow(Long tenantId,
                                 String taskSn,
                                 List<FundsTransactionProjectionRow> rebuiltRows) {
            delegate.upsertShadow(tenantId, taskSn, rebuiltRows);
        }

        @Override
        public void upsertOfficial(Long tenantId,
                                   String taskSn,
                                   List<FundsTransactionProjectionRow> rebuiltRows) {
            delegate.upsertOfficial(tenantId, taskSn, rebuiltRows);
        }
    }

    private abstract static class TestProjectionReplaySource implements FundsTransactionProjectionReplaySource {

        @Override
        public FundsTransactionProjectionCheckpoint initializeCheckpoint(Long tenantId,
                                                                         String viewDomain,
                                                                         FundsTransactionProjectionReplayRange range) {
            return checkpoint("0");
        }

        @Override
        public FundsTransactionProjectionFactBatch loadFactBatch(Long tenantId,
                                                                 String viewDomain,
                                                                 FundsTransactionProjectionReplayRange range,
                                                                 FundsTransactionProjectionCheckpoint checkpoint,
                                                                 int maxBatchSize) {
            return FundsTransactionProjectionFactBatch.builder()
                    .facts(loadFacts(range))
                    .nextCheckpoint(checkpoint("1"))
                    .hasMore(false)
                    .build();
        }

        protected abstract List<FundsTransactionProjectionFact> loadFacts(
                FundsTransactionProjectionReplayRange range);
    }

    private static class FixedProjectionReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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

    private static final class CheckpointProjectionReplaySource extends FixedProjectionReplaySource {

        private final FundsTransactionProjectionCheckpoint checkpoint;

        private CheckpointProjectionReplaySource(FundsTransactionProjectionCheckpoint checkpoint) {
            this.checkpoint = checkpoint;
        }

        @Override
        public FundsTransactionProjectionCheckpoint initializeCheckpoint(Long tenantId,
                                                                         String viewDomain,
                                                                         FundsTransactionProjectionReplayRange range) {
            return checkpoint;
        }
    }

    private static final class RecordingProjectionReplaySource extends TestProjectionReplaySource {

        private int loadCalls;

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
            loadCalls++;
            return new FixedProjectionReplaySource().loadFacts(range);
        }

        private int loadCalls() {
            return loadCalls;
        }
    }

    private static final class IncompleteProjectionReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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

    private static final class MismatchedViewDomainReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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

    private static final class OutsideSourceRangeReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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

    private static final class BlankSourceSnReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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

    private static final class NullPayloadReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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

    private static final class MissingAmountSourceReplaySource extends TestProjectionReplaySource {

        @Override
        protected List<FundsTransactionProjectionFact> loadFacts(FundsTransactionProjectionReplayRange range) {
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
        public List<FundsTransactionProjectionDifference> compare(Long tenantId,
                                                                  String viewDomain,
                                                                  List<FundsTransactionProjectionRow> rebuiltRows) {
            return differences;
        }

        @Override
        public void upsertShadow(Long tenantId,
                                 String taskSn,
                                 List<FundsTransactionProjectionRow> rebuiltRows) {
            // 无操作：本测试只验证比较契约加固。
        }

        @Override
        public void upsertOfficial(Long tenantId,
                                   String taskSn,
                                   List<FundsTransactionProjectionRow> rebuiltRows) {
            // 无操作：本测试只验证比较契约加固。
        }
    }

    private static final class RecordingProjectionWriter implements FundsTransactionProjectionWriter {

        private final List<FundsTransactionProjectionRow> comparedRows = new ArrayList<>();

        private final List<FundsTransactionProjectionRow> shadowWrites = new ArrayList<>();

        private final List<FundsTransactionProjectionRow> officialWrites = new ArrayList<>();

        @Override
        public List<FundsTransactionProjectionDifference> compare(Long tenantId,
                                                                  String viewDomain,
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
        public void upsertShadow(Long tenantId,
                                 String taskSn,
                                 List<FundsTransactionProjectionRow> rebuiltRows) {
            shadowWrites.addAll(rebuiltRows);
        }

        @Override
        public void upsertOfficial(Long tenantId,
                                   String taskSn,
                                   List<FundsTransactionProjectionRow> rebuiltRows) {
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

    @Configuration
    @Import(FundsProjectionReplayService.class)
    static class Config {

        @Bean
        SwitchableProjectionReplaySource projectionReplaySource() {
            return new SwitchableProjectionReplaySource();
        }

        @Bean
        SwitchableProjectionWriter projectionWriter() {
            return new SwitchableProjectionWriter();
        }
    }
}

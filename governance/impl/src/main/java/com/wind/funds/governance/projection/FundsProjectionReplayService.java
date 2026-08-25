package com.wind.funds.governance.projection;

import com.wind.funds.governance.enums.ProjectionCheckpointType;
import com.wind.funds.governance.enums.ProjectionReplayMode;
import com.wind.funds.governance.enums.ProjectionReplayTaskState;
import com.wind.funds.governance.dal.entities.ProjectionReplayDifference;
import com.wind.funds.governance.dal.entities.ProjectionReplayTask;
import com.wind.funds.governance.dal.mapper.ProjectionReplayDifferenceMapper;
import com.wind.funds.governance.dal.mapper.ProjectionReplayTaskMapper;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 资金交易投影重放服务。
 *
 * <p>职责：校验重放请求必须有界且使用交易投影 checkpoint，加载来源事实，重建投影行并比较现有投影差异。</p>
 *
 * <p>能力：支持持久任务的 {@code VERIFY_ONLY}、影子重建和经审批的正式投影重建；旧的直接调用入口仍只开放
 * {@code VERIFY_ONLY}。</p>
 *
 * <p>边界：该服务只处理交易只读投影，不重新入账、不补写交易事实、不修改账本分录、不修改余额投影、
 * 不推进清结算或对账差错处理。</p>
 */
@Service
public class FundsProjectionReplayService implements FundsProjectionReplayApplicationService {

    private static final int MAX_BATCH_SIZE = 500;

    private static final List<String> EXPLAINABILITY_PAYLOAD_KEYS = List.of(
            "factStatus",
            "operationStatus",
            "statusMeaning",
            "amountSource",
            "unavailableReason",
            "nextAction",
            "evidenceRefs",
            "externalRuleVerificationStatus");

    private final FundsTransactionProjectionReplaySource replaySource;

    private final FundsTransactionProjectionWriter projectionWriter;

    private final @Nullable ProjectionReplayTaskMapper replayTaskMapper;

    private final @Nullable ProjectionReplayDifferenceMapper replayDifferenceMapper;

    public FundsProjectionReplayService(@NonNull FundsTransactionProjectionReplaySource replaySource,
                                        @NonNull FundsTransactionProjectionWriter projectionWriter) {
        this.replaySource = replaySource;
        this.projectionWriter = projectionWriter;
        this.replayTaskMapper = null;
        this.replayDifferenceMapper = null;
    }

    @Autowired
    public FundsProjectionReplayService(@NonNull FundsTransactionProjectionReplaySource replaySource,
                                        @NonNull FundsTransactionProjectionWriter projectionWriter,
                                        @NonNull ProjectionReplayTaskMapper replayTaskMapper,
                                        @NonNull ProjectionReplayDifferenceMapper replayDifferenceMapper) {
        this.replaySource = replaySource;
        this.projectionWriter = projectionWriter;
        this.replayTaskMapper = replayTaskMapper;
        this.replayDifferenceMapper = replayDifferenceMapper;
    }

    public @NonNull FundsTransactionProjectionReplayResult replay(
            @NonNull FundsTransactionProjectionReplayRequest request) {
        assertRequestValid(request);
        AssertUtils.isTrue(request.mode() == ProjectionReplayMode.VERIFY_ONLY,
                "交易投影重放控制面未开放，仅支持 VERIFY_ONLY");
        List<FundsTransactionProjectionFact> facts = replaySource.loadFacts(request.replayRange());
        AssertUtils.notNull(facts, "交易投影重放来源事实列表不能为空");
        List<FundsTransactionProjectionRow> rebuiltRows = facts.stream()
                .map(fact -> rebuildProjectionRow(request, fact))
                .toList();
        List<FundsTransactionProjectionDifference> differences = projectionWriter.compare(request.viewDomain(),
                rebuiltRows);
        AssertUtils.notNull(differences, "交易投影重放差异列表不能为空");
        assertDifferencesComplete(differences);
        if (request.mode() == ProjectionReplayMode.REBUILD_SHADOW) {
            projectionWriter.upsertShadow(request.taskSn(), rebuiltRows);
        } else if (request.mode() == ProjectionReplayMode.REBUILD_APPLY) {
            projectionWriter.upsertOfficial(request.taskSn(), rebuiltRows);
        }
        return FundsTransactionProjectionReplayResult.builder()
                .taskSn(request.taskSn())
                .mode(request.mode())
                .viewDomain(request.viewDomain())
                .range(request.replayRange())
                .loadedFactCount(facts.size())
                .rebuiltRowCount(rebuiltRows.size())
                .differences(differences)
                .checkpoint(request.checkpoint())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull FundsProjectionReplayTaskDTO createTask(
            @NonNull CreateFundsProjectionReplayTaskRequest request,
            @NonNull WindOperator operator) {
        ProjectionReplayTaskMapper taskMapper = requiredTaskMapper();
        validateCreateRequest(request, operator);
        ProjectionReplayTask existing = taskMapper.selectByRequest(request.tenantId(), request.requestSn());
        if (existing != null) {
            AssertUtils.equals(existing.getRequestDigest(), request.requestDigest(),
                    "投影重放任务幂等键对应的请求摘要不一致，requestSn = {}", request.requestSn());
            return toDTO(existing);
        }
        validateApplyEvidence(request);
        FundsTransactionProjectionCheckpoint checkpoint = replaySource.initializeCheckpoint(request.tenantId(),
                request.viewDomain(), request.replayRange());
        ProjectionReplayTask task = new ProjectionReplayTask();
        task.setSn("PRT-" + UUID.randomUUID().toString().replace("-", ""));
        task.setTenantId(request.tenantId());
        task.setRequestSn(request.requestSn());
        task.setRequestDigest(request.requestDigest());
        task.setViewDomain(request.viewDomain());
        task.setReplayMode(request.mode());
        copyRange(request.replayRange(), task);
        task.setCheckpointType(checkpoint.type());
        task.setCheckpointValue(checkpoint.checkpointSn());
        task.setState(ProjectionReplayTaskState.CREATED);
        task.setSuccessCount(0L);
        task.setFailedCount(0L);
        task.setSkippedCount(0L);
        task.setDifferenceCount(0L);
        task.setReplayReason(request.reason());
        task.setAuditRef(request.auditRef());
        task.setApprovalRef(request.approvalRef());
        task.setValidatedShadowTaskSn(request.validatedShadowTaskSn());
        task.setOperatorId(operator.getOperatorAsText());
        taskMapper.insertSelective(task);
        AssertUtils.notNull(task.getId(), "创建投影重放任务失败");
        return toDTO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull FundsTransactionProjectionReplayResult runTask(
            @NonNull RunFundsProjectionReplayTaskRequest request,
            @NonNull WindOperator operator) {
        validateRunRequest(request, operator);
        ProjectionReplayTaskMapper taskMapper = requiredTaskMapper();
        ProjectionReplayTask task = taskMapper.selectBySnForUpdate(request.tenantId(), request.taskSn());
        AssertUtils.notNull(task, "投影重放任务不存在，taskSn = {}", request.taskSn());
        AssertUtils.isTrue(task.getState() != ProjectionReplayTaskState.COMPLETED,
                "投影重放任务已经完成，taskSn = {}", request.taskSn());
        FundsTransactionProjectionReplayRange range = toRange(task);
        FundsTransactionProjectionCheckpoint checkpoint = toCheckpoint(task);
        FundsTransactionProjectionFactBatch batch = replaySource.loadFactBatch(task.getTenantId(),
                task.getViewDomain(), range, checkpoint, request.maxBatchSize());
        AssertUtils.notNull(batch, "投影重放事实批次不能为空");
        FundsTransactionProjectionReplayRequest replayRequest = FundsTransactionProjectionReplayRequest.builder()
                .taskSn(task.getSn())
                .mode(task.getReplayMode())
                .viewDomain(task.getViewDomain())
                .replayRange(range)
                .checkpoint(checkpoint)
                .build();
        List<FundsTransactionProjectionRow> rebuiltRows = batch.facts().stream()
                .map(fact -> rebuildProjectionRow(replayRequest, fact))
                .toList();
        List<FundsTransactionProjectionDifference> differences = projectionWriter.compare(task.getTenantId(),
                task.getViewDomain(), rebuiltRows);
        AssertUtils.notNull(differences, "交易投影重放差异列表不能为空");
        assertDifferencesComplete(differences);
        persistDifferences(task, differences);
        if (task.getReplayMode() == ProjectionReplayMode.REBUILD_SHADOW) {
            projectionWriter.upsertShadow(task.getTenantId(), task.getSn(), rebuiltRows);
        } else if (task.getReplayMode() == ProjectionReplayMode.REBUILD_APPLY) {
            projectionWriter.upsertOfficial(task.getTenantId(), task.getSn(), rebuiltRows);
        }
        task.setCheckpointType(batch.nextCheckpoint().type());
        task.setCheckpointValue(batch.nextCheckpoint().checkpointSn());
        task.setSuccessCount(task.getSuccessCount() + rebuiltRows.size());
        task.setDifferenceCount(task.getDifferenceCount() + differences.size());
        task.setState(batch.hasMore() ? ProjectionReplayTaskState.CREATED
                : ProjectionReplayTaskState.COMPLETED);
        task.setOperatorId(operator.getOperatorAsText());
        taskMapper.update(task);
        return FundsTransactionProjectionReplayResult.builder()
                .taskSn(task.getSn())
                .mode(task.getReplayMode())
                .viewDomain(task.getViewDomain())
                .range(range)
                .loadedFactCount(batch.facts().size())
                .rebuiltRowCount(rebuiltRows.size())
                .differences(differences)
                .checkpoint(batch.nextCheckpoint())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull FundsProjectionReplayTaskDTO getTask(@NonNull Long tenantId,
                                                         @NonNull String taskSn,
                                                         @NonNull WindOperator operator) {
        validateQuery(tenantId, taskSn, operator);
        ProjectionReplayTask task = requiredTaskMapper().selectBySn(tenantId, taskSn);
        AssertUtils.notNull(task, "投影重放任务不存在，taskSn = {}", taskSn);
        return toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<FundsProjectionReplayTaskDTO> queryBacklog(@NonNull Long tenantId,
                                                                  int maxSize,
                                                                  @NonNull WindOperator operator) {
        validateOperatorAndTenant(tenantId, operator);
        AssertUtils.isTrue(maxSize > 0 && maxSize <= MAX_BATCH_SIZE,
                "投影重放 backlog 查询大小必须在 1 到 {} 之间", MAX_BATCH_SIZE);
        return requiredTaskMapper().selectBacklog(tenantId, maxSize).stream().map(this::toDTO).toList();
    }

    private void validateCreateRequest(CreateFundsProjectionReplayTaskRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建投影重放任务请求不能为空");
        validateOperatorAndTenant(request.tenantId(), operator);
        AssertUtils.hasText(request.requestSn(), "投影重放任务请求流水不能为空");
        AssertUtils.hasText(request.requestDigest(), "投影重放任务请求摘要不能为空");
        AssertUtils.hasText(request.viewDomain(), "投影重放任务视图域不能为空");
        AssertUtils.notNull(request.mode(), "投影重放任务模式不能为空");
        AssertUtils.notNull(request.replayRange(), "投影重放任务范围不能为空");
        AssertUtils.isTrue(request.replayRange().isBounded(), "投影重放任务必须指定有界范围");
        AssertUtils.isTrue(!StringUtils.hasText(request.replayRange().batchType())
                        && !StringUtils.hasText(request.replayRange().batchSn()),
                "当前交易投影重放不支持批次范围");
        AssertUtils.hasText(request.reason(), "投影重放任务原因不能为空");
        AssertUtils.hasText(request.auditRef(), "投影重放任务审计引用不能为空");
    }

    private void validateApplyEvidence(CreateFundsProjectionReplayTaskRequest request) {
        if (request.mode() != ProjectionReplayMode.REBUILD_APPLY) {
            return;
        }
        AssertUtils.hasText(request.approvalRef(), "正式投影重放必须提供审批引用");
        AssertUtils.hasText(request.validatedShadowTaskSn(), "正式投影重放必须引用已验证影子任务");
        ProjectionReplayTask shadow = requiredTaskMapper().selectBySn(request.tenantId(),
                request.validatedShadowTaskSn());
        AssertUtils.notNull(shadow, "正式投影重放引用的影子任务不存在");
        AssertUtils.isTrue(shadow.getReplayMode() == ProjectionReplayMode.REBUILD_SHADOW
                        && shadow.getState() == ProjectionReplayTaskState.COMPLETED
                        && Objects.equals(shadow.getViewDomain(), request.viewDomain())
                        && Objects.equals(toRange(shadow), request.replayRange()),
                "正式投影重放引用的影子任务与当前范围不一致或尚未完成");
    }

    private void validateRunRequest(RunFundsProjectionReplayTaskRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "运行投影重放任务请求不能为空");
        validateOperatorAndTenant(request.tenantId(), operator);
        AssertUtils.hasText(request.taskSn(), "投影重放任务号不能为空");
        AssertUtils.isTrue(request.maxBatchSize() > 0 && request.maxBatchSize() <= MAX_BATCH_SIZE,
                "投影重放批次大小必须在 1 到 {} 之间", MAX_BATCH_SIZE);
    }

    private void validateQuery(Long tenantId, String taskSn, WindOperator operator) {
        validateOperatorAndTenant(tenantId, operator);
        AssertUtils.hasText(taskSn, "投影重放任务号不能为空");
    }

    private void validateOperatorAndTenant(Long tenantId, WindOperator operator) {
        AssertUtils.notNull(tenantId, "投影重放租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "投影重放 tenantId 与当前租户不一致");
        AssertUtils.notNull(operator, "投影重放操作者不能为空");
        AssertUtils.hasText(operator.getOperatorAsText(), "投影重放操作者身份不能为空");
    }

    private void persistDifferences(ProjectionReplayTask task,
                                    List<FundsTransactionProjectionDifference> differences) {
        ProjectionReplayDifferenceMapper differenceMapper = requiredDifferenceMapper();
        for (FundsTransactionProjectionDifference difference : differences) {
            ProjectionReplayDifference entity = differenceMapper.selectDifference(task.getTenantId(), task.getSn(),
                    difference.sourceSn(), difference.fieldName());
            if (entity == null) {
                entity = new ProjectionReplayDifference();
                entity.setTenantId(task.getTenantId());
                entity.setTaskSn(task.getSn());
                entity.setSourceSn(difference.sourceSn());
                entity.setFieldName(difference.fieldName());
                entity.setExpectedValue(digest(difference.expectedValue()));
                entity.setActualValue(digest(difference.actualValue()));
                differenceMapper.insertSelective(entity);
            } else {
                entity.setExpectedValue(digest(difference.expectedValue()));
                entity.setActualValue(digest(difference.actualValue()));
                differenceMapper.update(entity);
            }
        }
    }

    private String digest(@Nullable Object value) {
        if (value == null) {
            return "<NULL>";
        }
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 缺少 SHA-256", exception);
        }
    }

    private void copyRange(FundsTransactionProjectionReplayRange range, ProjectionReplayTask task) {
        task.setSourceSn(range.sourceSn());
        task.setOwnerType(range.ownerType());
        task.setOwnerId(range.ownerId());
        task.setRangeStartTime(range.startTime());
        task.setRangeEndTime(range.endTime());
        task.setBatchType(range.batchType());
        task.setBatchSn(range.batchSn());
    }

    private FundsTransactionProjectionReplayRange toRange(ProjectionReplayTask task) {
        return FundsTransactionProjectionReplayRange.builder()
                .sourceSn(task.getSourceSn())
                .ownerType(task.getOwnerType())
                .ownerId(task.getOwnerId())
                .startTime(task.getRangeStartTime())
                .endTime(task.getRangeEndTime())
                .batchType(task.getBatchType())
                .batchSn(task.getBatchSn())
                .build();
    }

    private FundsTransactionProjectionCheckpoint toCheckpoint(ProjectionReplayTask task) {
        return FundsTransactionProjectionCheckpoint.builder()
                .type(task.getCheckpointType())
                .checkpointSn(task.getCheckpointValue())
                .build();
    }

    private FundsProjectionReplayTaskDTO toDTO(ProjectionReplayTask task) {
        return FundsProjectionReplayTaskDTO.builder()
                .taskSn(task.getSn())
                .tenantId(task.getTenantId())
                .requestSn(task.getRequestSn())
                .requestDigest(task.getRequestDigest())
                .viewDomain(task.getViewDomain())
                .mode(task.getReplayMode())
                .replayRange(toRange(task))
                .state(task.getState())
                .checkpoint(toCheckpoint(task))
                .successCount(task.getSuccessCount())
                .failedCount(task.getFailedCount())
                .skippedCount(task.getSkippedCount())
                .differenceCount(task.getDifferenceCount())
                .reason(task.getReplayReason())
                .auditRef(task.getAuditRef())
                .approvalRef(task.getApprovalRef())
                .validatedShadowTaskSn(task.getValidatedShadowTaskSn())
                .operatorId(task.getOperatorId())
                .build();
    }

    private ProjectionReplayTaskMapper requiredTaskMapper() {
        AssertUtils.notNull(replayTaskMapper, "持久投影重放任务 Mapper 未装配");
        return replayTaskMapper;
    }

    private ProjectionReplayDifferenceMapper requiredDifferenceMapper() {
        AssertUtils.notNull(replayDifferenceMapper, "持久投影重放差异 Mapper 未装配");
        return replayDifferenceMapper;
    }

    private void assertRequestValid(FundsTransactionProjectionReplayRequest request) {
        AssertUtils.hasText(request.taskSn(), "交易投影重放任务号不能为空");
        AssertUtils.notNull(request.mode(), "交易投影重放模式不能为空");
        AssertUtils.hasText(request.viewDomain(), "交易投影视图域不能为空");
        AssertUtils.notNull(request.replayRange(), "交易投影重放范围不能为空");
        AssertUtils.isTrue(request.replayRange().isBounded(), "交易投影重放必须指定单笔、主体、时间窗口或批次范围");
        AssertUtils.notNull(request.checkpoint(), "交易投影重放 checkpoint 不能为空");
        AssertUtils.notNull(request.checkpoint().type(), "交易投影重放 checkpoint 类型不能为空");
        AssertUtils.hasText(request.checkpoint().checkpointSn(), "交易投影重放 checkpoint 流水号不能为空");
        AssertUtils.isTrue(request.checkpoint().type() == ProjectionCheckpointType.TRANSACTION_PROJECTION,
                "交易投影重放 checkpoint 类型必须为交易投影");
    }

    private void assertDifferencesComplete(List<FundsTransactionProjectionDifference> differences) {
        for (FundsTransactionProjectionDifference difference : differences) {
            AssertUtils.notNull(difference, "交易投影重放差异项不能为空");
            assertDifferenceHasText(difference.sourceSn(), "sourceSn");
            assertDifferenceHasText(difference.fieldName(), "fieldName");
        }
    }

    private void assertDifferenceHasText(String value, String fieldName) {
        AssertUtils.hasText(value, "交易投影重放差异项字段不能为空，field = {}", fieldName);
    }

    private FundsTransactionProjectionRow rebuildProjectionRow(FundsTransactionProjectionReplayRequest request,
                                                               FundsTransactionProjectionFact fact) {
        assertFactComplete(fact);
        assertFactInRequestScope(request, fact);
        assertExplainabilityPayload(fact);
        return FundsTransactionProjectionRow.builder()
                .projectionSn("TP-" + fact.sourceSn())
                .viewDomain(fact.viewDomain())
                .ownerType(fact.ownerType())
                .ownerId(fact.ownerId())
                .sourceSn(fact.sourceSn())
                .displayType(fact.displayType())
                .displayStatus(fact.displayStatus())
                .amount(fact.amount())
                .currency(fact.currency())
                .occurredTime(fact.occurredTime())
                .payload(Map.copyOf(fact.payload()))
                .build();
    }

    private void assertFactComplete(FundsTransactionProjectionFact fact) {
        AssertUtils.notNull(fact, "交易投影重放来源事实不能为空");
        assertFactHasText(fact.viewDomain(), "viewDomain");
        assertFactHasText(fact.ownerType(), "ownerType");
        assertFactHasText(fact.ownerId(), "ownerId");
        assertFactHasText(fact.sourceSn(), "sourceSn");
        assertFactHasText(fact.displayType(), "displayType");
        assertFactHasText(fact.displayStatus(), "displayStatus");
        assertFactNotNull(fact.currency(), "currency");
        assertFactNotNull(fact.occurredTime(), "occurredTime");
        assertFactNotNull(fact.payload(), "payload");
    }

    private void assertFactHasText(String value, String fieldName) {
        AssertUtils.hasText(value, "交易投影重放来源事实字段不能为空，field = {}", fieldName);
    }

    private void assertFactNotNull(Object value, String fieldName) {
        AssertUtils.notNull(value, "交易投影重放来源事实字段不能为空，field = {}", fieldName);
    }

    private void assertFactInRequestScope(FundsTransactionProjectionReplayRequest request,
                                          FundsTransactionProjectionFact fact) {
        AssertUtils.isTrue(request.viewDomain().equals(fact.viewDomain()),
                "交易投影重放来源事实不属于请求视图域，sourceSn = {}，actual = {}，expected = {}",
                fact.sourceSn(), fact.viewDomain(), request.viewDomain());
        FundsTransactionProjectionReplayRange range = request.replayRange();
        assertSourceInRange(range, fact);
        assertOwnerInRange(range, fact);
        assertOccurredTimeInRange(range, fact);
    }

    private void assertSourceInRange(FundsTransactionProjectionReplayRange range,
                                     FundsTransactionProjectionFact fact) {
        if (StringUtils.hasText(range.sourceSn())) {
            AssertUtils.isTrue(range.sourceSn().equals(fact.sourceSn()),
                    "交易投影重放来源事实不属于请求范围，field = sourceSn，actual = {}，expected = {}",
                    fact.sourceSn(), range.sourceSn());
        }
    }

    private void assertOwnerInRange(FundsTransactionProjectionReplayRange range,
                                    FundsTransactionProjectionFact fact) {
        if (StringUtils.hasText(range.ownerType()) && StringUtils.hasText(range.ownerId())) {
            AssertUtils.isTrue(range.ownerType().equals(fact.ownerType()) && range.ownerId().equals(fact.ownerId()),
                    "交易投影重放来源事实不属于请求范围，field = owner，actual = {}/{}，expected = {}/{}",
                    fact.ownerType(), fact.ownerId(), range.ownerType(), range.ownerId());
        }
    }

    private void assertOccurredTimeInRange(FundsTransactionProjectionReplayRange range,
                                           FundsTransactionProjectionFact fact) {
        if (range.startTime() != null && range.endTime() != null) {
            AssertUtils.isTrue(!fact.occurredTime().isBefore(range.startTime())
                            && fact.occurredTime().isBefore(range.endTime()),
                    "交易投影重放来源事实不属于请求范围，field = occurredTime，sourceSn = {}",
                    fact.sourceSn());
        }
    }

    private void assertExplainabilityPayload(FundsTransactionProjectionFact fact) {
        for (String key : EXPLAINABILITY_PAYLOAD_KEYS) {
            AssertUtils.isTrue(hasExplainabilityValue(fact.payload().get(key)),
                    "交易投影重放缺少使用者解释视图字段，sourceSn = {}，field = {}",
                    fact.sourceSn(), key);
        }
    }

    private boolean hasExplainabilityValue(Object value) {
        if (value instanceof String text) {
            return StringUtils.hasText(text);
        }
        if (value instanceof List<?> values) {
            return !CollectionUtils.isEmpty(values);
        }
        return value != null;
    }
}

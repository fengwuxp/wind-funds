package com.capte.funds.governance.projection;

import com.capte.funds.governance.enums.ProjectionCheckpointType;
import com.capte.funds.governance.enums.ProjectionReplayMode;
import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 资金交易投影重放服务。
 *
 * <p>职责：校验重放请求必须有界且使用交易投影 checkpoint，加载来源事实，重建投影行，比较现有投影差异，
 * 并根据模式选择只输出差异、写影子投影或写正式投影。</p>
 *
 * <p>能力：为交易投影提供可审计、可灰度、可幂等验证的重放入口，用于修复只读视图漂移或验证视图重建规则。</p>
 *
 * <p>边界：该服务只处理交易只读投影，不重新入账、不补写交易事实、不修改账本分录、不修改余额投影、
 * 不推进清结算或对账差错处理。</p>
 */
public class FundsProjectionReplayService {

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

    public FundsProjectionReplayService(@NonNull FundsTransactionProjectionReplaySource replaySource,
                                        @NonNull FundsTransactionProjectionWriter projectionWriter) {
        this.replaySource = replaySource;
        this.projectionWriter = projectionWriter;
    }

    public @NonNull FundsTransactionProjectionReplayResult replay(
            @NonNull FundsTransactionProjectionReplayRequest request) {
        assertRequestValid(request);
        List<FundsTransactionProjectionFact> facts = replaySource.loadFacts(request.replayRange());
        AssertUtils.notNull(facts, "交易投影重放来源事实列表不能为空");
        List<FundsTransactionProjectionRow> rebuiltRows = facts.stream()
                .map(fact -> rebuildProjectionRow(request, fact))
                .toList();
        List<FundsTransactionProjectionDifference> differences = projectionWriter.compare(request.viewDomain(),
                rebuiltRows);
        AssertUtils.notNull(differences, "交易投影重放差异列表不能为空");
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
        assertFactHasText(fact.currency(), "currency");
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

package com.capte.funds.transaction.projection;

import com.capte.funds.transaction.enums.FundsTransactionProjectionCheckpointType;
import com.capte.funds.transaction.enums.FundsTransactionProjectionReplayMode;
import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.NonNull;

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
public class FundsTransactionProjectionReplayService {

    private final FundsTransactionProjectionReplaySource replaySource;

    private final FundsTransactionProjectionWriter projectionWriter;

    public FundsTransactionProjectionReplayService(@NonNull FundsTransactionProjectionReplaySource replaySource,
                                                   @NonNull FundsTransactionProjectionWriter projectionWriter) {
        this.replaySource = replaySource;
        this.projectionWriter = projectionWriter;
    }

    public @NonNull FundsTransactionProjectionReplayResult replay(@NonNull FundsTransactionProjectionReplayRequest request) {
        assertRequestValid(request);
        List<FundsTransactionProjectionFact> facts = replaySource.loadFacts(request.replayRange());
        List<FundsTransactionProjectionRow> rebuiltRows = facts.stream()
                .map(this::rebuildProjectionRow)
                .toList();
        List<FundsTransactionProjectionDifference> differences = projectionWriter.compare(request.viewDomain(), rebuiltRows);
        if (request.mode() == FundsTransactionProjectionReplayMode.REBUILD_SHADOW) {
            projectionWriter.upsertShadow(request.taskSn(), rebuiltRows);
        } else if (request.mode() == FundsTransactionProjectionReplayMode.REBUILD_APPLY) {
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
        AssertUtils.isTrue(request.checkpoint().type() == FundsTransactionProjectionCheckpointType.TRANSACTION_PROJECTION,
                "交易投影重放不得复用余额水位、归档 Manifest 或报表 checkpoint");
    }

    private FundsTransactionProjectionRow rebuildProjectionRow(FundsTransactionProjectionFact fact) {
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
}

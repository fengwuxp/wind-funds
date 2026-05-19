package com.capte.funds.transaction.projection;

import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 资金交易投影重放服务。
 *
 * <p>该服务只负责重建只读交易投影，不重新入账、不补写交易事实、不修改余额投影。</p>
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

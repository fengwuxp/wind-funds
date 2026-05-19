package com.capte.funds.transaction.projection;

import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionCheckpoint;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionCheckpointType;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionDifference;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionFact;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionReplayMode;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionReplayRange;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionReplayRequest;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionReplayResult;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionReplaySource;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionRow;
import com.capte.funds.transaction.projection.FundsTransactionProjectionReplayService.FundsTransactionProjectionWriter;
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
class FundsTransactionProjectionReplayServiceTests {

    /**
     * 场景：运营人员发起交易投影重放，但没有指定单笔、主体、时间窗口或批次范围。
     * 输入：`VERIFY_ONLY` 模式、空范围、合法交易投影 checkpoint。
     * 输出：服务拒绝执行。
     * 预期：在线交易投影重放必须有明确范围。
     * 红线：不得无范围全量重放，避免把投影修复任务扩散成生产批量风险。
     */
    @Test
    void testReplayWithoutBoundedRangeShouldFail() {
        FundsTransactionProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = replayRequest(FundsTransactionProjectionReplayRange.builder()
                .build());

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放必须指定单笔、主体、时间窗口或批次范围");
    }

    /**
     * 场景：调用方误把余额水位当成交易投影重放 checkpoint。
     * 输入：单笔重放范围、`BALANCE_WATERMARK` checkpoint。
     * 输出：服务拒绝执行。
     * 预期：交易投影重放只能使用交易投影自己的 checkpoint。
     * 红线：不得复用余额水位、归档 Manifest 或报表 checkpoint 作为交易投影处理边界。
     */
    @Test
    void testReplayWithBalanceWatermarkCheckpointShouldFail() {
        FundsTransactionProjectionReplayService service = newService(new RecordingProjectionWriter());
        FundsTransactionProjectionReplayRequest request = FundsTransactionProjectionReplayRequest.builder()
                .taskSn("TPR-202605190002")
                .mode(FundsTransactionProjectionReplayMode.VERIFY_ONLY)
                .viewDomain("USER_BILL")
                .replayRange(FundsTransactionProjectionReplayRange.builder()
                        .sourceSn("FT202605190001")
                        .build())
                .checkpoint(FundsTransactionProjectionCheckpoint.builder()
                        .type(FundsTransactionProjectionCheckpointType.BALANCE_WATERMARK)
                        .checkpointSn("BW-202605190001")
                        .build())
                .build();

        assertThatThrownBy(() -> service.replay(request))
                .hasMessageContaining("交易投影重放不得复用余额水位、归档 Manifest 或报表 checkpoint");
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
        FundsTransactionProjectionReplayService service = newService(writer);

        FundsTransactionProjectionReplayResult result = service.replay(replayRequest(FundsTransactionProjectionReplayRange.builder()
                .sourceSn("FT202605190001")
                .build()));

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

    private static FundsTransactionProjectionReplayService newService(RecordingProjectionWriter writer) {
        return new FundsTransactionProjectionReplayService(new FixedProjectionReplaySource(), writer);
    }

    private static FundsTransactionProjectionReplayRequest replayRequest(FundsTransactionProjectionReplayRange replayRange) {
        return FundsTransactionProjectionReplayRequest.builder()
                .taskSn("TPR-202605190001")
                .mode(FundsTransactionProjectionReplayMode.VERIFY_ONLY)
                .viewDomain("USER_BILL")
                .replayRange(replayRange)
                .checkpoint(FundsTransactionProjectionCheckpoint.builder()
                        .type(FundsTransactionProjectionCheckpointType.TRANSACTION_PROJECTION)
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
                    .payload(Map.of("businessScene", "ORDER_PAY"))
                    .build());
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

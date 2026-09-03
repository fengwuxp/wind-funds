package com.wind.funds.governance.projection.internal;

import com.wind.funds.governance.projection.FundsTransactionProjectionDifference;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放写入端口，是重放服务对投影视图执行比对和写入的端口。
 *
 * <p>职责：比较现有投影与重建行差异，并按模式写入影子投影或正式只读投影。</p>
 *
 * <p>能力：支持 verify-only 差异核对、shadow 灰度核对和 apply 正式投影刷新。</p>
 *
 * <p>边界：端口只能处理投影视图数据，不得写交易事实、账本分录、余额桶或其他业务域数据。</p>
 *
 * @author wuxp
 * @since 2026-08-31
 */
public interface FundsTransactionProjectionWriter {

    @NonNull
    List<FundsTransactionProjectionDifference> compare(
            @NonNull Long tenantId,
            @NonNull String viewDomain,
            @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

    void upsertShadow(@NonNull Long tenantId,
                      @NonNull String taskSn,
                      @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

    void upsertOfficial(@NonNull Long tenantId,
                        @NonNull String taskSn,
                        @NonNull List<FundsTransactionProjectionRow> rebuiltRows);
}

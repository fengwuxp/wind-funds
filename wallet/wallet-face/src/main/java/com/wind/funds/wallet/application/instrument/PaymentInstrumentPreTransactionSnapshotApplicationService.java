package com.wind.funds.wallet.application.instrument;

import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具预交易快照应用服务。
 *
 * <p>职责：面向支付工具交易入口，在进入交易内核前只读解析工具能力、资金责任和账户能力，
 * 形成可审计、可回放的准入快照。</p>
 *
 * <p>边界：本服务不创建交易事实、route snapshot、账本交易、账目分录或余额投影；交易执行仍由
 * transaction 层账户主体型 canonical service 完成。</p>
 *
 * @author Codex
 * @date 2026-06-19
 */
public interface PaymentInstrumentPreTransactionSnapshotApplicationService {

    /**
     * 解析支付工具预交易快照。
     *
     * @param request 支付工具预交易快照解析请求
     * @return 支付工具预交易准入快照
     */
    @NonNull PaymentInstrumentPreTransactionSnapshotDTO resolvePreTransactionSnapshot(
            @NonNull ResolvePaymentInstrumentPreTransactionSnapshotRequest request);
}

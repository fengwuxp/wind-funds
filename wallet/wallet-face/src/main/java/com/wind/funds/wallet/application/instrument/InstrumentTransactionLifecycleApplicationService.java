package com.wind.funds.wallet.application.instrument;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.wallet.model.request.ReceiveByInstrumentRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具交易生命周期应用服务。
 *
 * <p>职责：面向 VA、ACH、电子钱包端点、VCC 入金端点等支付工具业务入口，完成工具能力、
 * 绑定快照、资金责任和账户能力准入，再委派账户主体型交易内核。</p>
 *
 * <p>边界：本服务不改变 transaction 层 canonical 入参；交易事实、route、账本交易、分录和余额投影仍由
 * 账户主体型交易服务和 ledger posting 链路生成。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface InstrumentTransactionLifecycleApplicationService {

    /**
     * 通过支付工具收款入口完成准入并委派充值交易内核。
     *
     * <p>典型场景包括 VA 外部打款入账、ACH 收款成功入账、外部钱包入金等。
     * 准入失败时必须在进入交易内核前中止，不生成资金交易、route、posting plan、账本交易或分录。</p>
     *
     * @param request  支付工具收款请求
     * @param operator 操作者
     * @return 充值交易流水号
     */
    @NonNull String receiveByInstrument(@NonNull ReceiveByInstrumentRequest request,
                                        @NonNull WindOperator operator);
}

package com.wind.funds.wallet.application.instrument;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.ReceiveByInstrumentRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具交易应用服务。
 *
 * <p>职责：面向 VCC、卡、VA、ACH、电子钱包端点、VCC 入金端点等支付工具业务入口，完成工具能力、
 * 绑定快照、资金责任和账户能力准入，再委派账户主体型交易内核。</p>
 *
 * <p>边界：本服务不改变 transaction 层 canonical 入参；交易事实、route、账本交易、分录和余额投影仍由
 * 账户主体型交易服务和 ledger posting 链路生成。</p>
 *
 * <p>观测：准入通过后仅把支付工具、绑定版本、资金责任和目标账务主体等轻量快照写入交易上下文，
 * 供投影解释、审计和对账定位；不得在上下文承载敏感工具明文、金额分摊或规则原文。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface PaymentInstrumentTransactionApplicationService {

    /**
     * 通过支付工具授权入口完成准入并委派授权交易内核。
     *
     * <p>典型场景包括 VCC/卡授权、外部钱包授权或通道 token 授权等。
     * 支付工具解析、资金责任解析、账户能力校验和 Spend Rule 准入均由本入口内部完成；
     * 外部调用方不应自行拼接准入和交易内核。</p>
     *
     * @param request  支付工具授权请求
     * @param operator 操作者
     * @return 授权交易流水号
     */
    @NonNull String authorizeByInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                          @NonNull WindOperator operator);

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

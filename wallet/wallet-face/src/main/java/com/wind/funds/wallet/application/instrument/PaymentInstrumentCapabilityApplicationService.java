package com.wind.funds.wallet.application.instrument;

import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentCapabilityRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具能力准入应用服务。
 *
 * <p>职责：面向交易准入、路由准入和外部业务入口解析支付工具当前动作能力与绑定快照。</p>
 *
 * <p>边界：只读取支付工具和绑定候选，不注册工具，不变更绑定，不写交易事实、路由事实、账本分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
public interface PaymentInstrumentCapabilityApplicationService {

    /**
     * 解析支付工具当前动作能力和绑定快照。
     *
     * <p>本方法只说明工具本身可用于当前动作，不代表内部账户能力、余额、额度、账期或资金责任已经通过。</p>
     *
     * @param request 解析请求
     * @return 支付工具能力准入决策
     */
    @NonNull PaymentInstrumentCapabilityDecisionDTO resolvePaymentInstrumentCapability(
            @NonNull ResolvePaymentInstrumentCapabilityRequest request);
}

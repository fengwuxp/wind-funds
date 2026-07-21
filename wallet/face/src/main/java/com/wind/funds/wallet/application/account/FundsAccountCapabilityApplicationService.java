package com.wind.funds.wallet.application.account;

import com.wind.funds.wallet.model.dto.FundsAccountCapabilityDecisionDTO;
import com.wind.funds.wallet.model.request.ResolveFundsAccountCapabilityRequest;
import org.jspecify.annotations.NonNull;

/**
 * 资金账户能力准入应用服务。
 *
 * <p>职责：面向交易准入、支付工具准入和钱包入口，解析资金账户或信用账户当前动作能力。</p>
 *
 * <p>边界：只读取账户、profile 和账户能力配置，不写交易事实、路由事实、账本分录或余额投影。
 * 支付工具能力通过后仍必须独立调用本服务或等价账户能力校验。</p>
 *
 * @author Codex
 * @date 2026-06-19
 */
public interface FundsAccountCapabilityApplicationService {

    /**
     * 解析账户当前资金动作能力。
     *
     * @param request 账户能力解析请求
     * @return 账户能力准入决策
     */
    @NonNull FundsAccountCapabilityDecisionDTO resolveFundsAccountCapability(
            @NonNull ResolveFundsAccountCapabilityRequest request);
}

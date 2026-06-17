package com.wind.funds.wallet.application.funding;

import com.wind.funds.wallet.model.dto.FundingResponsibilityDecisionDTO;
import com.wind.funds.wallet.model.request.ResolveFundingResponsibilityRequest;
import org.jspecify.annotations.NonNull;

/**
 * 资金责任解析应用服务。
 *
 * <p>职责：面向交易准入、路由准入和外部业务入口解析当前可用的默认资金责任主体。</p>
 *
 * <p>边界：只读取资金责任关系并输出决策快照，不写交易事实、路由事实、账本分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
public interface FundingResponsibilityResolutionApplicationService {

    /**
     * 解析当前默认资金责任主体。
     *
     * <p>调用方拿到结果后，应继续由交易、路由或授权准入链路校验账户能力、余额、额度和业务规则。
     * 本方法不表达扣款、不表达授权占用，也不替代交易内核的账户主体入参。</p>
     *
     * @param request 解析请求
     * @return 资金责任决策
     */
    @NonNull FundingResponsibilityDecisionDTO resolveFundingResponsibility(
            @NonNull ResolveFundingResponsibilityRequest request);
}

package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支出控制准入应用服务。
 *
 * <p>职责：面向支付工具交易入口，在进入交易内核前组合支付工具预交易快照与 Spend Rule 决策证据，
 * 形成可审计、可回放的支出控制准入结论。</p>
 *
 * <p>边界：本服务不计算规则、不创建交易事实、route snapshot、账本交易、账目分录或余额投影；
 * 规则结果由外部规则或业务决策方提供，本服务只校验证据完整性并记录 Spend Rule 决策控制事实。</p>
 *
 * @author Codex
 * @date 2026-06-19
 */
public interface SpendControlAdmissionApplicationService {

    /**
     * 解析并记录支出控制准入结论。
     *
     * <p>本方法会校验外部 Spend Rule 决策证据并固化决策记录，返回可被授权入口消费的准入快照。
     * 它不创建资金交易、route、posting、账本交易、账目分录、余额投影或控制额度变动流水。</p>
     *
     * @param request 支出控制准入解析请求
     * @return 支出控制准入结论
     */
    @NonNull SpendControlAdmissionDecisionDTO resolveSpendControlAdmission(
            @NonNull ResolveSpendControlAdmissionRequest request);
}

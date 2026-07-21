package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支出控制准入应用服务。
 *
 * <p>职责：面向支付工具交易入口，在进入交易内核前组合支付工具预交易快照，
 * 自行解析当前适用的 Spend Rule 挂载，并回读决策引用形成可审计、可回放的支出控制准入结论。</p>
 *
 * <p>边界：本服务不计算规则、不创建交易事实、route snapshot、账本交易、账目分录或余额投影；
 * 规则结果由可信规则或业务决策方预先固化；本服务不接受裸 PASSED 和摘要作为准入依据。
 * 无适用规则时返回显式 NO_APPLICABLE_RULE；当前单决策证据契约遇到多个适用挂载，或存在无法从可信上下文
 * 解析的有效挂载时 fail-closed。</p>
 *
 * @author Codex
 * @date 2026-06-19
 */
public interface SpendControlAdmissionApplicationService {

    /**
     * 解析支出控制准入结论。
     *
     * <p>本方法会解析适用挂载，并按 decisionSn 回读、核对已固化决策记录，返回可被授权入口消费的准入快照。
     * 它不创建资金交易、route、posting、账本交易、账目分录、余额投影或控制额度变动流水。</p>
     *
     * @param request 支出控制准入解析请求
     * @return 支出控制准入结论
     */
    @NonNull SpendControlAdmissionDecisionDTO resolveSpendControlAdmission(
            @NonNull ResolveSpendControlAdmissionRequest request);
}

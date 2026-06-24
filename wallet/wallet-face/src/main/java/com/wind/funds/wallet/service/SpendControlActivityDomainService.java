package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import org.jspecify.annotations.NonNull;

/**
 * 控制额度变动流水领域写服务。
 *
 * <p>职责：维护控制活动写入侧业务不变量，包括幂等、目标账户、敏感上下文、
 * 释放上限和预算控制活动边界。</p>
 *
 * <p>边界：本服务不执行 Spend Rule，不创建资金交易、route、账本交易、账目分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendControlActivityDomainService {

    /**
     * 记录控制额度变动流水。
     *
     * @param request 控制额度变动流水记录请求
     * @return 控制额度变动流水
     */
    @NonNull SpendControlActivityDTO recordActivity(@NonNull RecordSpendControlActivityRequest request);
}

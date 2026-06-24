package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import org.jspecify.annotations.NonNull;

/**
 * Spend Rule 定义领域写服务。
 *
 * <p>职责：维护规则定义、不可变版本和控制范围挂载的写侧业务不变量，
 * 包括定义幂等、版本不可原地覆盖、已发布版本挂载和挂载幂等校验。</p>
 *
 * <p>边界：本服务不执行规则、不记录决策记录、不调整控制额度、不创建交易事实、
 * route snapshot、账本交易、账目分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleDefinitionDomainService {

    /**
     * 创建 Spend Rule 定义。
     *
     * @param request 定义创建请求
     * @return 规则定义
     */
    @NonNull SpendRuleDefinitionDTO createDefinition(@NonNull CreateSpendRuleDefinitionRequest request);

    /**
     * 发布不可变 Spend Rule 版本。
     *
     * @param request 版本发布请求
     * @return 已发布规则版本
     */
    @NonNull SpendRuleVersionDTO publishVersion(@NonNull PublishSpendRuleVersionRequest request);

    /**
     * 将已发布 Spend Rule 版本挂载到控制范围。
     *
     * @param request 规则挂载请求
     * @return 规则挂载
     */
    @NonNull SpendRuleAssignmentDTO assignVersion(@NonNull AssignSpendRuleVersionRequest request);
}

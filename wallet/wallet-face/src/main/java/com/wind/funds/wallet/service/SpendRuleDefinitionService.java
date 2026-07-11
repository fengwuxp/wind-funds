package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Spend Rule 定义服务。
 *
 * <p>职责：维护规则定义、不可变版本和控制范围挂载。</p>
 *
 * <p>边界：本服务不执行规则、不记录决策记录、不调整控制额度、
 * 不创建资金交易、route snapshot、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleDefinitionService {

    /**
     * 创建 Spend Rule 定义。
     *
     * @param request 定义创建请求
     * @return 规则定义主键
     */
    @NonNull Long createDefinition(@NonNull CreateSpendRuleDefinitionRequest request);

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
    @NonNull SpendRuleBindingDTO createSpendRuleBinding(@NonNull CreateSpendRuleBindingRequest request);

    /**
     * 根据主键查询 Spend Rule 定义。
     *
     * @param id 主键
     * @return 规则定义
     */
    @NonNull SpendRuleDefinitionDTO getDefinitionById(@NonNull Long id);

    /**
     * 按租户和规则标识查找 Spend Rule 定义。
     *
     * @param tenantId 租户 ID
     * @param ruleId Spend Rule 标识
     * @return 规则定义，未找到时返回 null
     */
    @Nullable SpendRuleDefinitionDTO findDefinition(@NonNull Long tenantId, @NonNull String ruleId);
}

package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Spend Rule 定义基础服务。
 *
 * <p>职责：封装规则定义的基础持久化和按规则标识读取能力。
 * 本服务只做数据访问协调，不负责版本发布、挂载校验、规则执行或决策记录。</p>
 *
 * <p>边界：调用方若需要生产写入语义，应优先使用 {@link SpendRuleDefinitionDomainService}。
 * 本服务不创建资金交易、route snapshot、posting、LedgerEntry 或余额投影。</p>
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

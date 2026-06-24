package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Spend Rule 版本基础服务。
 *
 * <p>职责：封装规则版本基础读取能力，为规则发布、挂载和决策记录校验提供已发布版本依据。</p>
 *
 * <p>边界：本服务不发布版本、不执行规则脚本、不记录决策记录，也不创建交易或账务事实。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleVersionService {

    /**
     * 创建 Spend Rule 版本。
     *
     * @param request 版本发布请求
     * @return 规则版本主键
     */
    @NonNull Long createVersion(@NonNull PublishSpendRuleVersionRequest request);

    /**
     * 根据主键查询 Spend Rule 版本。
     *
     * @param id 主键
     * @return 规则版本
     */
    @NonNull SpendRuleVersionDTO getVersionById(@NonNull Long id);

    /**
     * 按租户、规则标识和版本号查找 Spend Rule 版本。
     *
     * @param tenantId 租户 ID
     * @param ruleId Spend Rule 标识
     * @param ruleVersion Spend Rule 版本
     * @return 规则版本，未找到时返回 null
     */
    @Nullable SpendRuleVersionDTO findVersion(@NonNull Long tenantId,
                                              @NonNull String ruleId,
                                              @NonNull String ruleVersion);

    /**
     * 获取已发布的 Spend Rule 版本。
     *
     * @param tenantId 租户 ID
     * @param ruleId Spend Rule 标识
     * @param ruleVersion Spend Rule 版本
     * @return 已发布规则版本
     */
    @NonNull SpendRuleVersionDTO getPublishedVersion(@NonNull Long tenantId,
                                                     @NonNull String ruleId,
                                                     @NonNull String ruleVersion);
}

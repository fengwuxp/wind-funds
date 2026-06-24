package com.wind.funds.wallet.service;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.model.dto.AccountHierarchyBindingDTO;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * 账户层级绑定基础服务。
 *
 * <p>职责：封装账户层级绑定当前态的基础持久化、当前 ACTIVE 关系查询和唯一性辅助判断。</p>
 *
 * <p>边界：本服务不解析账户是否存在，不创建账户、账本或交易事实，也不改变余额。</p>
 *
 * @author Codex
 * @date 2026-06-24
 */
public interface AccountHierarchyBindingService {

    /**
     * 创建账户层级绑定当前态。
     *
     * @param binding 待创建的绑定当前态
     * @return 绑定主键
     */
    @NonNull Long createAccountHierarchyBinding(@NonNull AccountHierarchyBindingDTO binding);

    /**
     * 查询账户当前 ACTIVE 层级绑定。
     *
     * @param tenantId 租户 ID
     * @param accountId 子账户 ID
     * @param accountType 子账户类型
     * @return 当前 ACTIVE 层级绑定
     */
    @NonNull Optional<AccountHierarchyBindingDTO> findActiveAccountHierarchyBinding(@NonNull Long tenantId,
                                                                                   @NonNull String accountId,
                                                                                   @NonNull FundsSubjectType accountType);

    /**
     * 判断账户是否已经存在 ACTIVE 层级绑定。
     *
     * @param binding 待校验绑定
     * @return 已存在 ACTIVE 绑定时返回 true
     */
    boolean existsActiveAccountHierarchyBinding(@NonNull AccountHierarchyBindingDTO binding);
}

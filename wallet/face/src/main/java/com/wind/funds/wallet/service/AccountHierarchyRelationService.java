package com.wind.funds.wallet.service;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyRelationRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * 账户层级关系服务。
 *
 * <p>维护资金账户、信用账户之间不可换绑的直接父子关系，并为交易路由提供关系查询。
 * 该关系只表达组织和归属，不推导资金责任，不创建交易、账本或余额事实。
 * 当前版本不引入跨节点锁；调用方并发构建同一租户的账户拓扑时必须串行提交，
 * 避免两个尚不存在的反向关系同时通过环路检查。</p>
 */
public interface AccountHierarchyRelationService {

    /**
     * 创建账户层级关系。
     *
     * <p>同一子账户重复提交相同父账户时返回已持久化关系；已存在其他父账户时拒绝。</p>
     *
     * @param request 创建请求
     * @param operator 操作人
     * @return 已持久化关系
     */
    @NonNull AccountHierarchyRelationDTO createAccountHierarchyRelation(
            @NonNull CreateAccountHierarchyRelationRequest request,
            @NonNull WindOperator operator);

    /**
     * 查询子账户的直接父账户关系。
     *
     * @param tenantId 租户 ID
     * @param accountId 子账户标识
     * @return 当前关系，不存在时返回空
     */
    @NonNull Optional<AccountHierarchyRelationDTO> findAccountHierarchyRelation(
            @NonNull Long tenantId,
            @NonNull FundsAccountId accountId);

}

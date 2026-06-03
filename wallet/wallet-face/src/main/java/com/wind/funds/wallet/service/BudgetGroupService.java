package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.BudgetGroupDTO;
import com.wind.funds.wallet.model.query.BudgetGroupQuery;
import com.wind.funds.wallet.model.request.CreateBudgetGroupRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.FundsAccountId;
import org.jspecify.annotations.NonNull;

/**
 * 预算组服务。
 *
 * <p>职责：管理预算控制主体，并在创建时初始化预算额度相关 ledger。</p>
 *
 * <p>边界：预算组不是支付工具，也不直接保存真实资金；实际扣款资金来源由支出主体资金关系确定。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface BudgetGroupService {

    /**
     * 创建预算组并显式初始化 ledger。
     *
     * <p>能力范围：创建 BudgetGroup 主体，并按 BUDGET_BASIC profile 初始化预算额度账本。</p>
     *
     * @param request 创建请求
     * @return 预算组主键
     */
    @NonNull Long createBudgetGroup(@NonNull CreateBudgetGroupRequest request);

    /**
     * 根据主键查询预算组。
     *
     * <p>能力范围：只读查询，不执行预算占用、释放或额度重算。</p>
     *
     * @param id 主键
     * @return 预算组
     */
    @NonNull BudgetGroupDTO getBudgetGroupById(@NonNull Long id);

    /**
     * 根据预算组号查询。
     *
     * <p>能力范围：按 FundsAccountId 查询预算组，不查询信用账户或真实资金账户。</p>
     *
     * @param accountId 预算组标识
     * @return 预算组
     */
    @NonNull BudgetGroupDTO getBudgetGroup(@NonNull FundsAccountId accountId);

    /**
     * 分页查询预算组。
     *
     * <p>能力范围：只读分页查询，不触发账本初始化或预算重算。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 预算组分页结果
     */
    @NonNull WindPagination<BudgetGroupDTO> queryBudgetGroups(@NonNull BudgetGroupQuery query,
                                                              @NonNull WindQuery<? extends QueryOrderField> options);
}

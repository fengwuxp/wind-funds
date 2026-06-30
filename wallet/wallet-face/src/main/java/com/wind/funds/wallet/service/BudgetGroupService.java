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
 * 预算控制范围服务。
 *
 * <p>职责：管理可命名、可归属、可查询的支出控制 scope；BudgetGroup 为历史兼容名。</p>
 *
 * <p>边界：预算控制范围不是支付工具、资金池或资金 / 信用账务主体；实际扣款资金来源由支出主体资金关系确定。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface BudgetGroupService {

    /**
     * 创建预算控制范围。
     *
     * <p>能力范围：创建 Spend Rule 可引用的控制 scope，不初始化 ledger bucket，不表达可入账余额。</p>
     *
     * @param request 创建请求
     * @return 预算控制范围主键
     */
    @NonNull Long createBudgetGroup(@NonNull CreateBudgetGroupRequest request);

    /**
     * 根据主键查询预算控制范围。
     *
     * <p>能力范围：只读查询，不执行预算占用、释放或额度重算。</p>
     *
     * @param id 主键
     * @return 预算控制范围
     */
    @NonNull BudgetGroupDTO getBudgetGroupById(@NonNull Long id);

    /**
     * 根据预算控制范围标识查询。
     *
     * <p>能力范围：按兼容标识查询预算控制范围，不查询信用账户或真实资金账户。</p>
     *
     * @param accountId 预算控制范围兼容标识
     * @return 预算控制范围
     */
    @NonNull BudgetGroupDTO getBudgetGroup(@NonNull FundsAccountId accountId);

    /**
     * 分页查询预算控制范围。
     *
     * <p>能力范围：只读分页查询，不触发账本初始化或预算重算。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 预算控制范围分页结果
     */
    @NonNull WindPagination<BudgetGroupDTO> queryBudgetGroups(@NonNull BudgetGroupQuery query,
                                                              @NonNull WindQuery<? extends QueryOrderField> options);
}

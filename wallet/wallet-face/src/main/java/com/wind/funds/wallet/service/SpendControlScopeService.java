package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendControlScopeDTO;
import com.wind.funds.wallet.model.query.SpendControlScopeQuery;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;

/**
 * 支出控制范围服务。
 *
 * <p>职责：管理可命名、可归属、可查询的 Spend Rule 控制范围。</p>
 *
 * <p>边界：支出控制范围不是支付工具、资金池或资金 / 信用账务主体；实际扣款责任主体由支出主体资金责任解析关系确定。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface SpendControlScopeService {

    /**
     * 创建支出控制范围。
     *
     * <p>能力范围：创建 Spend Rule 可引用的控制 scope，不初始化 ledger bucket，不表达可入账余额。</p>
     *
     * @param request 创建请求
     * @return 支出控制范围主键
     */
    @NonNull Long createSpendControlScope(@NonNull CreateSpendControlScopeRequest request);

    /**
     * 根据主键查询支出控制范围。
     *
     * <p>能力范围：只读查询，不执行额度占用、释放或重算。</p>
     *
     * @param id 主键
     * @return 支出控制范围
     */
    @NonNull SpendControlScopeDTO getSpendControlScopeById(@NonNull Long id);

    /**
     * 根据目标语义查询支出控制范围。
     *
     * <p>能力范围：按 Spend Rule 可引用的控制范围标识查询，不查询信用账户或真实资金账户。</p>
     *
     * @param tenantId 租户 ID
     * @param controlScopeId 支出控制范围标识
     * @param scopeType 支出控制范围业务类型
     * @return 支出控制范围
     */
    @NonNull SpendControlScopeDTO getSpendControlScope(@NonNull Long tenantId,
                                                  @NonNull String controlScopeId,
                                                  @NonNull String scopeType);

    /**
     * 分页查询支出控制范围。
     *
     * <p>能力范围：只读分页查询，不触发账本初始化或额度重算。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支出控制范围分页结果
     */
    @NonNull WindPagination<SpendControlScopeDTO> querySpendControlScopes(@NonNull SpendControlScopeQuery query,
                                                              @NonNull WindQuery<? extends QueryOrderField> options);
}

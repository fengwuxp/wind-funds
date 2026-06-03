package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.query.CreditAccountQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.FundsAccountId;
import org.jspecify.annotations.NonNull;

/**
 * 信用账户服务。
 *
 * <p>职责：管理信用类支出主体，例如共享卡对应的信用账户，并在创建时初始化额度相关 ledger。</p>
 *
 * <p>边界：不保存真实资金，不决定可用资金来源；信用账户到真实资金账户的关系由支出主体资金关系服务维护。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface CreditAccountService {

    /**
     * 创建信用账户并显式初始化 ledger。
     *
     * <p>能力范围：创建 CreditAccount 主体，并按 CREDIT_BASIC profile 初始化 LIMIT、AVAILABLE、AUTHORIZATION 等 ledger。</p>
     *
     * @param request 创建请求
     * @return 信用账户主键
     */
    @NonNull Long createCreditAccount(@NonNull CreateCreditAccountRequest request);

    /**
     * 根据主键查询信用账户。
     *
     * <p>能力范围：只读查询，不进行额度计算和账本修复。</p>
     *
     * @param id 主键
     * @return 信用账户
     */
    @NonNull CreditAccountDTO getCreditAccountById(@NonNull Long id);

    /**
     * 根据信用账户号查询。
     *
     * <p>能力范围：按 FundsAccountId 查询信用账户，不查询真实资金账户或预算组。</p>
     *
     * @param accountId 信用账户标识
     * @return 信用账户
     */
    @NonNull CreditAccountDTO getCreditAccount(@NonNull FundsAccountId accountId);

    /**
     * 分页查询信用账户。
     *
     * <p>能力范围：只读分页查询，不触发额度重算。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 信用账户分页结果
     */
    @NonNull
    WindPagination<CreditAccountDTO> queryCreditAccounts(@NonNull CreditAccountQuery query,
                                                         @NonNull WindQuery<? extends QueryOrderField> options);
}

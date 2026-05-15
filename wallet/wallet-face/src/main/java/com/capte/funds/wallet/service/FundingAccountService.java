package com.capte.funds.wallet.service;

import com.capte.funds.wallet.model.dto.FundingAccountDTO;
import com.capte.funds.wallet.model.query.FundingAccountQuery;
import com.capte.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.jspecify.annotations.NonNull;

/**
 * 真实资金账户服务。
 *
 * <p>职责：管理真正承载资金余额的 FundingAccount，并在开户时显式初始化该主体所需 ledger。</p>
 *
 * <p>边界：不处理信用额度、预算控制和支付工具绑定；交易路径中不得通过该服务自动建账。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface FundingAccountService {

    /**
     * 创建真实资金账户并显式初始化 ledger。
     *
     * <p>能力范围：创建 FundingAccount 主体，并按 LedgerProfile 初始化 required ledger。</p>
     *
     * @param request 创建请求
     * @return 资金账户主键
     */
    @NonNull Long createFundingAccount(@NonNull CreateFundingAccountRequest request);

    /**
     * 根据主键查询资金账户。
     *
     * <p>能力范围：只读查询，不初始化 ledger，不修复账户状态。</p>
     *
     * @param id 主键
     * @return 资金账户
     */
    @NonNull FundingAccountDTO getFundingAccountById(@NonNull Long id);

    /**
     * 根据账户号查询资金账户。
     *
     * <p>能力范围：按 FundsAccountId 查询真实资金账户，不兼容信用账户和预算组。</p>
     *
     * @param accountId 账户标识
     * @return 资金账户
     */
    @NonNull FundingAccountDTO getFundingAccount(@NonNull FundsAccountId accountId);

    /**
     * 分页查询资金账户。
     *
     * <p>能力范围：只读分页查询，不触发账本初始化或余额重算。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 资金账户分页结果
     */
    @NonNull
    WindPagination<FundingAccountDTO> queryFundingAccounts(@NonNull FundingAccountQuery query,
                                                           @NonNull WindQuery<? extends QueryOrderField> options);
}

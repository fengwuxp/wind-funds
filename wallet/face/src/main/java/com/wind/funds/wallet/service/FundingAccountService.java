package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.query.FundingAccountQuery;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
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
     * 按租户和账户号查询资金账户。
     *
     * <p>能力范围：用于内部服务解析已知 FundingAccount 主体；不支持信用账户和支出控制范围，不触发账本初始化。</p>
     *
     * @param tenantId 租户 ID
     * @param accountSn 资金账户号
     * @return 资金账户
     */
    @NonNull FundingAccountDTO getFundingAccount(@NonNull Long tenantId, @NonNull String accountSn);

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

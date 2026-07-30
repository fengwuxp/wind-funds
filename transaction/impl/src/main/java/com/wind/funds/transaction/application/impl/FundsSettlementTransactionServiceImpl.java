package com.wind.funds.transaction.application.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.converter.FundsSettlementInstructionConverter;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.integration.operator.WindOperator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算锁定资金命令服务实现。
 */
@Service
@AllArgsConstructor
@NullMarked
public class FundsSettlementTransactionServiceImpl implements FundsSettlementTransactionService {

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private final FundsSettlementInstructionConverter settlementInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    private final FundsAccountQueryService fundsAccountQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public String lock(FundsSettlementLockRequest request, WindOperator operator) {
        validateRequest(request, operator);
        return fundsInstructionOrchestrator.execute(settlementInstructionConverter.convert(request, operator));
    }

    private void validateRequest(FundsSettlementLockRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "结算锁定请求不能为空");
        AssertUtils.notNull(operator, "结算锁定操作人不能为空");
        AssertUtils.notNull(request.getAccountId(), "结算资金账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "结算资金账户不能是外部账户");
        AssertUtils.isFalse(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE.equals(request.getAccountId().type()),
                "支出控制范围不能作为结算资金账户");
        AssertUtils.notNull(request.getAmount(), "结算锁定金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "结算锁定金额必须大于 0");
        AssertUtils.hasText(request.getSettlementOrderSn(), "结算单流水号不能为空");
        FundsAccount account = fundsAccountQueryService.getAccount(request.getAccountId());
        AssertUtils.isTrue(account.isAvailable(), "结算资金账户不可用，accountId = {}", request.getAccountId());
        AssertUtils.equals(account.getCurrency(), request.getAmount().getCurrency(),
                "结算锁定金额币种必须与账户币种一致，accountId = {}", request.getAccountId());
    }
}

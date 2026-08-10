package com.wind.funds.transaction.application.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsPayoutTransactionService;
import com.wind.funds.transaction.converter.FundsPayoutInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsPayoutRequest;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.integration.operator.WindOperator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@NullMarked
public class FundsPayoutTransactionServiceImpl implements FundsPayoutTransactionService {

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private final FundsPayoutInstructionConverter payoutInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    private final FundsAccountQueryService fundsAccountQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public String succeed(FundsPayoutRequest request, WindOperator operator) {
        return execute(request, FundsTransactionEventType.PAYOUT_SUCCEEDED, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public String fail(FundsPayoutRequest request, WindOperator operator) {
        return execute(request, FundsTransactionEventType.PAYOUT_FAILED, operator);
    }

    private String execute(FundsPayoutRequest request,
                           FundsTransactionEventType eventType,
                           WindOperator operator) {
        validateRequest(request, operator);
        return fundsInstructionOrchestrator.execute(payoutInstructionConverter.convert(request, eventType, operator));
    }

    private void validateRequest(FundsPayoutRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "出款资金请求不能为空");
        AssertUtils.notNull(operator, "出款资金操作人不能为空");
        AssertUtils.notNull(request.getAccountId(), "出款结算资金账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "出款结算资金账户不能是外部账户");
        AssertUtils.isFalse(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE.equals(request.getAccountId().type()),
                "支出控制范围不能作为出款结算资金账户");
        AssertUtils.notNull(request.getAmount(), "出款金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "出款金额必须大于 0");
        AssertUtils.hasText(request.getPayoutOrderSn(), "出款单流水号不能为空");
        FundsAccount account = fundsAccountQueryService.getAccount(request.getAccountId());
        AssertUtils.isTrue(account.isAvailable(), "出款结算资金账户不可用，accountId = {}", request.getAccountId());
        AssertUtils.equals(account.getCurrency(), request.getAmount().getCurrency(),
                "出款金额币种必须与账户币种一致，accountId = {}", request.getAccountId());
    }
}

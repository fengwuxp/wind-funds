package com.capte.funds.transaction.application.impl;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.application.FundsAuthorizationTransactionService;
import com.capte.funds.transaction.application.FundsBalanceControlService;
import com.capte.funds.transaction.application.FundsDirectTransactionService;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.FundsInstructionOrchestrator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 资金交易命令服务实现。
 */
@AllArgsConstructor
@Slf4j
@NullMarked
@Service
public class FundsTransactionCommandServiceImpl implements FundsDirectTransactionService,
        FundsBalanceControlService,
        FundsAuthorizationTransactionService {

    private final FundsDirectTransactionInstructionConverter directTransactionInstructionConverter;

    private final FundsBalanceControlInstructionConverter balanceControlInstructionConverter;

    private final FundsAuthorizationInstructionConverter authorizationInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String topup(FundsTransactionTopupRequest request, WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "直接充值入账账户不能为空");
        AssertUtils.notNull(request.getFundsSourceAccountId(), "直接充值资金来源账户不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getFundsSourceAccountId()),
                "top-up funds source must external account");
        return execute(directTransactionInstructionConverter.convertToTopupInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String transfer(FundsTransactionTransferRequest request, WindOperator operator) {
        AssertUtils.notNull(request.getPayerAccountId(), "系统内转账付款账户不能为空");
        AssertUtils.notNull(request.getPayeeAccountId(), "系统内转账收款账户不能为空");
        AssertUtils.isFalse(Objects.equals(request.getPayeeAccountId(), request.getPayerAccountId()),
                "付款账号和收款账户不能一致");
        return execute(directTransactionInstructionConverter.convertToTransferInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String pay(FundsTransactionPayRequest request, WindOperator operator) {
        return execute(directTransactionInstructionConverter.convertToPayInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refund(FundsTransactionRefundRequest request, WindOperator operator) {
        return execute(directTransactionInstructionConverter.convertToRefundInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String withdraw(FundsTransactionWithdrawRequest request, WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "提现账户不能为空");
        AssertUtils.notNull(request.getReferenceFreezeSn(), "提现冻结流水号不能为空");
        AssertUtils.notNull(request.getPayeeId(), "提现外部收款方不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getPayeeId()),
                "withdraw payee must external account");
        return execute(directTransactionInstructionConverter.convertToWithdrawInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String fee(FundsTransactionFeeRequest request, WindOperator operator) {
        return execute(directTransactionInstructionConverter.convertToFeeInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refundFee(FundsTransactionFeeRefundRequest request, WindOperator operator) {
        return execute(directTransactionInstructionConverter.convertToFeeRefundInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String freeze(FundsBalanceFreezeRequest request, WindOperator operator) {
        return execute(balanceControlInstructionConverter.convertToFreezeInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String unfreeze(FundsBalanceUnfreezeRequest request, WindOperator operator) {
        return execute(balanceControlInstructionConverter.convertToUnfreezeInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String adjust(FundsBalanceAdjustRequest request, WindOperator operator) {
        return execute(balanceControlInstructionConverter.convertToAdjustInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String authorize(FundsAuthorizationTransactionAuthorizeRequest request, WindOperator operator) {
        return execute(authorizationInstructionConverter.convertToAuthorizeInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String reversal(FundsAuthorizationTransactionReversalRequest request, WindOperator operator) {
        return execute(authorizationInstructionConverter.convertToReversalInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String settle(FundsAuthorizationTransactionSettleRequest request, WindOperator operator) {
        return execute(authorizationInstructionConverter.convertToSettleInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String settleRefund(FundsAuthorizationTransactionRefundRequest request, WindOperator operator) {
        return execute(authorizationInstructionConverter.convertToSettleRefundInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String chargeback(FundsAuthorizationTransactionChargebackRequest request, WindOperator operator) {
        return execute(authorizationInstructionConverter.convertToChargebackInstruction(request, operator));
    }

    private @NonNull String execute(@NonNull FundsInstructionSpec instruction) {
        return fundsInstructionOrchestrator.execute(instruction);
    }
}

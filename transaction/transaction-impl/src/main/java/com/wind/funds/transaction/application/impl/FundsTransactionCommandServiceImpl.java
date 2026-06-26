package com.wind.funds.transaction.application.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.common.exception.AssertUtils;
import com.wind.common.locks.JdkLockFactory;
import com.wind.common.locks.LockFactory;
import com.wind.common.locks.WindLock;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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

    private static final String AUTHORIZATION_TRANSACTION_LOCK_PREFIX = "funds:authorization-transaction:";

    private static final LockFactory AUTHORIZATION_TRANSACTION_LOCK_FACTORY = new JdkLockFactory();

    private final FundsDirectTransactionInstructionConverter directTransactionInstructionConverter;

    private final FundsBalanceControlInstructionConverter balanceControlInstructionConverter;

    private final FundsAuthorizationInstructionConverter authorizationInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    private final FundsTransactionMapper fundsTransactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String topup(FundsTransactionTopupRequest request, WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "直接充值入账账户不能为空");
        AssertUtils.notNull(request.getFundsSourceAccountId(), "直接充值资金来源账户不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getFundsSourceAccountId()),
                "直接充值资金来源账户必须是外部账户");
        return execute(directTransactionInstructionConverter.convertToTopupInstruction(request, operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String transfer(FundsTransactionTransferRequest request, WindOperator operator) {
        AssertUtils.notNull(request.getPayerAccountId(), "系统内转账付款账户不能为空");
        AssertUtils.notNull(request.getPayeeAccountId(), "系统内转账收款账户不能为空");
        AssertUtils.isFalse(Objects.equals(request.getPayeeAccountId(), request.getPayerAccountId()),
                "付款账户和收款账户不能一致");
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
        AssertUtils.hasText(request.getReferenceFreezeSn(), "提现冻结流水号不能为空");
        AssertUtils.notNull(request.getPayeeId(), "提现外部收款方不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getPayeeId()),
                "提现外部收款方必须是外部账户");
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
        AssertUtils.notNull(request.getFeeSourceTransactionSn(), "手续费退回原费用交易流水不能为空");
        AssertUtils.notNull(request.getAccountId(), "手续费退回到账账户不能为空");
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
        return executeAuthorizationSuccessor(request.getAuthorizationTransactionSn(),
                () -> authorizationInstructionConverter.convertToReversalInstruction(request, operator),
                this::assertAuthorizationRemainingAmountSufficient);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String settle(FundsAuthorizationTransactionSettleRequest request, WindOperator operator) {
        return executeAuthorizationSuccessor(request.getAuthorizationTransactionSn(),
                () -> authorizationInstructionConverter.convertToSettleInstruction(request, operator),
                this::assertAuthorizationRemainingAmountSufficient);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String settleRefund(FundsAuthorizationTransactionRefundRequest request, WindOperator operator) {
        return executeAuthorizationSuccessor(request.getAuthorizationTransactionSn(),
                () -> authorizationInstructionConverter.convertToSettleRefundInstruction(request, operator));
    }

    private @NonNull String execute(@NonNull FundsInstructionSpec instruction) {
        return fundsInstructionOrchestrator.execute(instruction);
    }

    private @NonNull String executeAuthorizationSuccessor(
            String authorizationTransactionSn,
            Supplier<FundsInstructionSpec> instructionSupplier) {
        return executeAuthorizationSuccessor(authorizationTransactionSn, instructionSupplier, (transaction, instruction) -> {
        });
    }

    private @NonNull String executeAuthorizationSuccessor(
            String authorizationTransactionSn,
            Supplier<FundsInstructionSpec> instructionSupplier,
            BiConsumer<FundsTransaction, FundsInstructionSpec> precondition) {
        if (authorizationTransactionSn == null || authorizationTransactionSn.isBlank()) {
            return execute(instructionSupplier.get());
        }
        WindLock lock = AUTHORIZATION_TRANSACTION_LOCK_FACTORY.apply(authorizationTransactionLockKey(
                authorizationTransactionSn));
        lock.lock();
        boolean unlockImmediately = true;
        try {
            unlockImmediately = !registerTransactionCompletionUnlock(lock);
            FundsTransaction authorizationTransaction = lockAuthorizationTransaction(authorizationTransactionSn);
            FundsInstructionSpec instruction = instructionSupplier.get();
            precondition.accept(authorizationTransaction, instruction);
            return execute(instruction);
        } finally {
            if (unlockImmediately) {
                lock.unlock();
            }
        }
    }

    private static String authorizationTransactionLockKey(String authorizationTransactionSn) {
        return AUTHORIZATION_TRANSACTION_LOCK_PREFIX
                + ThreadContextTenantIdHolder.requireTenantId()
                + ":"
                + authorizationTransactionSn;
    }

    private static boolean registerTransactionCompletionUnlock(WindLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
        return true;
    }

    private FundsTransaction lockAuthorizationTransaction(String authorizationTransactionSn) {
        Long tenantId = ThreadContextTenantIdHolder.requireTenantId();
        FundsTransaction authorizationTransaction = fundsTransactionMapper.selectBySnForUpdate(tenantId,
                authorizationTransactionSn);
        AssertUtils.notNull(authorizationTransaction,
                "授权交易不存在，authorizationTransactionSn = {}", authorizationTransactionSn);
        return authorizationTransaction;
    }

    private void assertAuthorizationRemainingAmountSufficient(FundsTransaction transaction,
                                                              FundsInstructionSpec instruction) {
        long amount = instruction.getAmount().getAmount();
        long remainingAmount = transaction.getAuthorizedAmount() - transaction.getSettledAmount()
                - transaction.getReversedAmount();
        AssertUtils.isTrue(amount <= remainingAmount,
                "资金交易剩余授权可释放金额不足，sn = {}，remainingAmount = {}，amount = {}",
                transaction.getSn(), remainingAmount, amount);
    }
}

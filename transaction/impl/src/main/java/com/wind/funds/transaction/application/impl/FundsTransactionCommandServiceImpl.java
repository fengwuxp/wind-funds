package com.wind.funds.transaction.application.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.locks.JdkLockFactory;
import com.wind.common.locks.LockFactory;
import com.wind.common.locks.WindLock;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.dal.entities.table.FundsTransactionDetailNameRefs;
import com.wind.funds.transaction.dal.entities.table.FundsTransactionNameRefs;
import com.wind.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
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
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

    private static final String REFERENCED_TRANSACTION_LOCK_PREFIX = "funds:referenced-transaction:";

    private static final LockFactory REFERENCED_TRANSACTION_LOCK_FACTORY = new JdkLockFactory();

    private final FundsDirectTransactionInstructionConverter directTransactionInstructionConverter;

    private final FundsBalanceControlInstructionConverter balanceControlInstructionConverter;

    private final FundsAuthorizationInstructionConverter authorizationInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    private final FundsTransactionMapper fundsTransactionMapper;

    private final FundsTransactionDetailMapper fundsTransactionDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String topup(FundsTransactionTopupRequest request, WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "直接充值入账账户不能为空");
        AssertUtils.notNull(request.getFundsSourceAccountId(), "直接充值资金来源账户不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getFundsSourceAccountId()),
                "直接充值资金来源账户必须是外部账户");
        FundsInstructionSpec instruction = directTransactionInstructionConverter.convertToTopupInstruction(request, operator);
        FundsTransaction existingTransaction = findExternalFundsFactTransaction(instruction);
        if (existingTransaction == null || isSameBusinessRequest(existingTransaction, instruction)) {
            return execute(instruction);
        }
        AssertUtils.equals(existingTransaction.getExternalFundsFactDigest(), instruction.getExternalFundsFactDigest(),
                "外部资金事实请求参数不一致，transactionSn = {}", existingTransaction.getSn());
        AssertUtils.isTrue(existingTransaction.getStatus() == FundsTransactionStatus.CLOSED,
                "外部资金事实尚未成功完成，transactionSn = {}，status = {}",
                existingTransaction.getSn(), existingTransaction.getStatus());
        log.info("外部资金事实已完成，复用原资金交易，externalSourceCode={}, externalFundsFactSn={}, "
                        + "transactionSn={}", instruction.getExternalSourceCode(), instruction.getExternalFundsFactSn(),
                existingTransaction.getSn());
        return existingTransaction.getSn();
    }

    private @Nullable FundsTransaction findExternalFundsFactTransaction(FundsInstructionSpec instruction) {
        if (instruction.getExternalSourceCode() == null) {
            return null;
        }
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(instruction.getTenantId()))
                .and(ref.externalSourceCode.eq(instruction.getExternalSourceCode()))
                .and(ref.externalFundsFactSn.eq(instruction.getExternalFundsFactSn()))
                .and(ref.externalFundsEffectType.eq(instruction.getExternalFundsEffectType()));
        return fundsTransactionMapper.selectOneByQuery(wrapper);
    }

    private boolean isSameBusinessRequest(FundsTransaction transaction, FundsInstructionSpec instruction) {
        return Objects.equals(transaction.getBusinessScene(), instruction.getBusinessScene())
                && Objects.equals(transaction.getBusinessSn(), instruction.getBusinessSn());
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
        AssertUtils.hasText(request.getFeeSourceTransactionSn(), "手续费退回原费用交易流水不能为空");
        AssertUtils.notNull(request.getAccountId(), "手续费退回到账账户不能为空");
        return executeWithLockedReferenceTransaction(request.getFeeSourceTransactionSn(), "手续费原费用交易",
                sourceTransaction -> execute(directTransactionInstructionConverter
                        .convertToFeeRefundInstruction(request, operator)));
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
                authorizationTransaction -> {
                    request.setContextVariables(authorizationSuccessorContext(request.getContextVariables()));
                    return authorizationInstructionConverter.convertToReversalInstruction(request, operator);
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String complete(FundsAuthorizationTransactionCompleteRequest request, WindOperator operator) {
        if (request.isForceCompletion()) {
            return execute(authorizationInstructionConverter.convertToCompleteInstruction(request, operator));
        }
        return executeAuthorizationSuccessor(request.getAuthorizationTransactionSn(),
                authorizationTransaction -> {
                    request.setContextVariables(authorizationSuccessorContext(request.getContextVariables()));
                    return authorizationInstructionConverter.convertToCompleteInstruction(request, operator);
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refund(FundsAuthorizationTransactionRefundRequest request, WindOperator operator) {
        if (request.isNoAuthRefund()) {
            return execute(authorizationInstructionConverter.convertToRefundInstruction(request, operator));
        }
        return executeAuthorizationSuccessor(request.getAuthorizationTransactionSn(),
                authorizationTransaction -> {
                    request.setContextVariables(authorizationSuccessorContext(request.getContextVariables()));
                    return authorizationInstructionConverter.convertToRefundInstruction(request, operator);
                });
    }

    private @NonNull String execute(@NonNull FundsInstructionSpec instruction) {
        return fundsInstructionOrchestrator.execute(instruction);
    }

    private @NonNull String executeAuthorizationSuccessor(
            String authorizationTransactionSn,
            Function<FundsTransaction, FundsInstructionSpec> instructionFactory) {
        if (authorizationTransactionSn == null || authorizationTransactionSn.isBlank()) {
            throw new IllegalArgumentException("authorizationTransactionSn must not be blank");
        }
        return executeWithLockedReferenceTransaction(authorizationTransactionSn, "授权交易",
                authorizationTransaction -> {
                    FundsInstructionSpec instruction = instructionFactory.apply(authorizationTransaction);
                    assertAuthorizationRemainingAmountSufficient(authorizationTransaction, instruction);
                    return execute(instruction);
                });
    }

    private @NonNull String executeWithLockedReferenceTransaction(
            String referenceTransactionSn,
            String referenceName,
            Function<FundsTransaction, String> command) {
        WindLock lock = REFERENCED_TRANSACTION_LOCK_FACTORY.apply(referencedTransactionLockKey(
                referenceTransactionSn));
        lock.lock();
        boolean unlockImmediately = true;
        try {
            unlockImmediately = !registerTransactionCompletionUnlock(lock);
            FundsTransaction referenceTransaction = lockReferencedTransaction(referenceTransactionSn, referenceName);
            return command.apply(referenceTransaction);
        } finally {
            if (unlockImmediately) {
                lock.unlock();
            }
        }
    }

    private static String referencedTransactionLockKey(String referenceTransactionSn) {
        return REFERENCED_TRANSACTION_LOCK_PREFIX
                + TenantContextHolder.requireTenantId()
                + ":"
                + referenceTransactionSn;
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

    private @Nullable ReadonlyContextVariables authorizationSuccessorContext(
            @Nullable ReadonlyContextVariables requestContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (requestContext != null && requestContext.getContextVariables() != null) {
            result.putAll(requestContext.getContextVariables());
        }
        return result.isEmpty() ? requestContext : ReadonlyContextVariables.of(result);
    }

    private FundsTransaction lockReferencedTransaction(String referenceTransactionSn, String referenceName) {
        Long tenantId = TenantContextHolder.requireTenantId();
        FundsTransaction referenceTransaction = fundsTransactionMapper.selectBySnForUpdate(tenantId,
                referenceTransactionSn);
        AssertUtils.notNull(referenceTransaction,
                "{}不存在，transactionSn = {}", referenceName, referenceTransactionSn);
        return referenceTransaction;
    }

    private void assertAuthorizationRemainingAmountSufficient(FundsTransaction transaction,
                                                              FundsInstructionSpec instruction) {
        if (instruction.getEventType() != FundsTransactionEventType.COMPLETE
                && instruction.getEventType() != FundsTransactionEventType.REVERSAL) {
            return;
        }
        if (isSucceededAuthorizationSuccessor(transaction.getSn(), instruction)) {
            return;
        }
        long amount = instruction.getAmount().getAmount();
        long remainingAmount = transaction.getAuthorizedAmount() - transaction.getCompletedAmount()
                - transaction.getReversedAmount();
        AssertUtils.isTrue(amount <= remainingAmount,
                "资金交易剩余授权金额不足，sn = {}，remainingAmount = {}，amount = {}",
                transaction.getSn(), remainingAmount, amount);
    }

    private boolean isSucceededAuthorizationSuccessor(String transactionSn, FundsInstructionSpec instruction) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(instruction.getTenantId()))
                .and(ref.transactionSn.eq(transactionSn))
                .and(ref.businessScene.eq(instruction.getBusinessScene()))
                .and(ref.businessSn.eq(instruction.getBusinessSn()))
                .and(ref.eventType.eq(instruction.getEventType()));
        List<FundsTransactionDetail> details = fundsTransactionDetailMapper.selectListByQuery(wrapper);
        return !details.isEmpty()
                && details.stream().allMatch(detail -> detail.getStatus() == FundsTransactionDetailStatus.SUCCEEDED);
    }

}

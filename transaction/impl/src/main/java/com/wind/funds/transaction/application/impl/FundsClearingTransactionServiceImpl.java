package com.wind.funds.transaction.application.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsClearingTransactionService;
import com.wind.funds.transaction.converter.FundsClearingInstructionConverter;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 清算确认资金命令服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
@NullMarked
public class FundsClearingTransactionServiceImpl implements FundsClearingTransactionService {

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private final FundsClearingInstructionConverter clearingInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    private final FundsAccountQueryService fundsAccountQueryService;

    private final FundsTransactionMapper fundsTransactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public String confirm(FundsClearingConfirmRequest request, WindOperator operator) {
        validateRequest(request, operator);
        lockAndValidateSourceTransactions(request);
        String transactionSn = fundsInstructionOrchestrator.execute(clearingInstructionConverter.convert(request, operator));
        log.info("清算确认处理完成，等待事务提交，clearingBatchSn={}, transactionSn={}, accountType={}, "
                        + "accountId={}, sourceCount={}, amount={}, currency={}",
                request.getClearingBatchSn(), transactionSn, request.getAccountId().type(), request.getAccountId().id(),
                request.getSourceTransactionSns().size(), request.getAmount().getAmount(),
                request.getAmount().getCurrency());
        return transactionSn;
    }

    private void validateRequest(FundsClearingConfirmRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "清算确认请求不能为空");
        AssertUtils.notNull(operator, "清算确认操作人不能为空");
        AssertUtils.notNull(request.getAccountId(), "清算资金账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "清算资金账户不能是外部账户");
        AssertUtils.isFalse(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE.equals(request.getAccountId().type()),
                "支出控制范围不能作为清算资金账户");
        AssertUtils.notNull(request.getAmount(), "清算确认金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "清算确认金额必须大于 0");
        AssertUtils.hasText(request.getClearingBatchSn(), "清算批次流水号不能为空");
        AssertUtils.notEmpty(request.getSourceTransactionSns(), "清算来源交易流水号不能为空");
        AssertUtils.isTrue(request.getSourceTransactionSns().stream()
                        .allMatch(sourceTransactionSn -> sourceTransactionSn != null
                                && !sourceTransactionSn.isBlank()),
                "清算来源交易流水号不能为空");
        AssertUtils.isTrue(request.getSourceTransactionSns().stream().distinct().count()
                        == request.getSourceTransactionSns().size(),
                "清算来源交易流水号不能重复");
        FundsAccount account = fundsAccountQueryService.getAccount(request.getAccountId());
        AssertUtils.isTrue(account.isAvailable(), "清算资金账户不可用，accountId = {}", request.getAccountId());
        AssertUtils.equals(account.getCurrency(), request.getAmount().getCurrency(),
                "清算确认金额币种必须与账户币种一致，accountId = {}", request.getAccountId());
    }

    private void lockAndValidateSourceTransactions(FundsClearingConfirmRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        List<String> sourceTransactionSns = request.getSourceTransactionSns().stream().sorted().toList();
        for (String sourceTransactionSn : sourceTransactionSns) {
            FundsTransaction sourceTransaction = fundsTransactionMapper.selectBySnForUpdate(tenantId,
                    sourceTransactionSn);
            AssertUtils.notNull(sourceTransaction,
                    "清算来源交易不存在，fundsTransactionSn = {}", sourceTransactionSn);
            AssertUtils.isTrue(sourceTransaction.getState() == FundsTransactionState.OPEN
                            || sourceTransaction.getState() == FundsTransactionState.CLOSED,
                    "清算来源交易状态不可清算，fundsTransactionSn = {}，state = {}",
                    sourceTransactionSn, sourceTransaction.getState());
            AssertUtils.equals(request.getAmount().getCurrency(), sourceTransaction.getCurrency(),
                    "清算来源交易币种不一致，fundsTransactionSn = {}", sourceTransactionSn);
            AssertUtils.isTrue(sourceTransaction.getRefundedAmount() == null
                            || sourceTransaction.getRefundedAmount() == 0,
                    "清算来源交易已发生退款，fundsTransactionSn = {}", sourceTransactionSn);
        }
    }
}

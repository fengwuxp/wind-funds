package com.wind.funds.transaction.converter;

import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class FundsSettlementInstructionConverter {

    public @NonNull FundsInstructionSpec convert(@NonNull FundsSettlementLockRequest request,
                                                 @NonNull WindOperator operator) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.SETTLEMENT_LOCK)
                .transactionType(DefaultFundsTransactionType.SETTLEMENT)
                .amount(request.getAmount())
                .originalAmount(request.getAmount())
                .exchangeRate(BigDecimal.ONE)
                .accountId(request.getAccountId())
                .businessScene(FundsTransactionEventType.SETTLEMENT_LOCK.name())
                .businessSn(request.getSettlementOrderSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operator)
                .contextVariables(Map.of())
                .build();
    }

}

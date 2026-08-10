package com.wind.funds.transaction.converter;

import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsPayoutRequest;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class FundsPayoutInstructionConverter {

    public @NonNull FundsInstructionSpec convert(@NonNull FundsPayoutRequest request,
                                                 @NonNull FundsTransactionEventType eventType,
                                                 @NonNull WindOperator operator) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.PAYOUT)
                .amount(request.getAmount())
                .originalAmount(request.getAmount())
                .exchangeRate(BigDecimal.ONE)
                .accountId(request.getAccountId())
                .businessScene(eventType.name())
                .businessSn(request.getPayoutOrderSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operator)
                .contextVariables(Map.of())
                .build();
    }

}

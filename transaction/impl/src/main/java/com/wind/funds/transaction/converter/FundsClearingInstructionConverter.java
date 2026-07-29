package com.wind.funds.transaction.converter;

import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 清算确认请求到资金指令的转换器。
 */
@Component
public class FundsClearingInstructionConverter {

    public @NonNull FundsInstructionSpec convert(@NonNull FundsClearingConfirmRequest request,
                                                 @NonNull WindOperator operator) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.CLEARING_CONFIRM)
                .transactionType(DefaultFundsTransactionType.CLEARING)
                .amount(request.getAmount())
                .originalAmount(request.getAmount())
                .exchangeRate(BigDecimal.ONE)
                .accountId(request.getAccountId())
                .businessScene(FundsTransactionEventType.CLEARING_CONFIRM.name())
                .businessSn(request.getClearingBatchSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(Map.of())
                .build();
    }

    private FundsOperationActorSpec operationActor(WindOperator operator) {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(operator.getOperatorAsText())
                .operatorType(operator.getActorType().name())
                .operatorName(operator.getOperatorName())
                .appName(operator.getAppName())
                .contextVariables(Map.of())
                .build();
    }
}

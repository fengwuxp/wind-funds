package com.wind.funds.transaction.converter;

import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.transaction.core.Money;
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

    public @NonNull FundsInstructionSpec convertRelease(@NonNull FundsAccountId accountId,
                                                        @NonNull Money amount,
                                                        @NonNull String settlementOrderSn,
                                                        @NonNull String lockFundsTransactionSn,
                                                        @NonNull WindOperator operator) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.SETTLEMENT_RELEASE)
                .transactionType(DefaultFundsTransactionType.SETTLEMENT)
                .amount(amount)
                .originalAmount(amount)
                .exchangeRate(BigDecimal.ONE)
                .accountId(accountId)
                .reference(ImmutableFundsInstructionReferenceSpec.builder()
                        .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                        .referenceSn(lockFundsTransactionSn)
                        .contextVariables(Map.of())
                        .build())
                .businessScene(FundsTransactionEventType.SETTLEMENT_RELEASE.name())
                .businessSn(settlementOrderSn + ":RELEASE")
                .eventTime(LocalDateTime.now())
                .description("settlement release")
                .operator(operator)
                .contextVariables(Map.of())
                .build();
    }

}

package com.wind.funds.model.transaction;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 不可变资金指令实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundsInstructionSpec(@Nullable Long tenantId,
                                            FundsInstructionType instructionType,
                                            FundsTransactionEventType eventType,
                                            DefaultFundsTransactionType transactionType,
                                            Money amount,
                                            Money originalAmount,
                                            BigDecimal exchangeRate,
                                            @Nullable PaymentInstrumentRefSpec instrumentRef,
                                            @Nullable ExternalAccountRefSpec externalAccountRef,
                                            @Nullable FundsAccountId accountId,
                                            @Nullable FundsAccountId payerAccountId,
                                            @Nullable FundsAccountId payeeAccountId,
                                            @Nullable FundsAccountId payerId,
                                            @Nullable FundsAccountId payeeId,
                                            @Nullable LedgerSubjectCode payerLedgerSubjectCode,
                                            @Nullable LedgerSubjectCode payeeLedgerSubjectCode,
                                            @Nullable FundsAccountId linkedFundingAccountId,
                                            @Nullable AccountBalancePeriodType ledgerPeriodType,
                                            @Nullable String ledgerPeriodId,
                                            @Nullable FundsInstructionReferenceSpec reference,
                                            String businessScene,
                                            String businessSn,
                                            LocalDateTime eventTime,
                                            @Nullable String description,
                                            FundsOperationActorSpec operator,
                                            Map<String, Object> contextVariables) implements FundsInstructionSpec {

    public ImmutableFundsInstructionSpec {
        AssertUtils.notNull(instructionType, "fundsInstruction.instructionType must not be null");
        AssertUtils.notNull(eventType, "fundsInstruction.eventType must not be null");
        AssertUtils.notNull(transactionType, "fundsInstruction.transactionType must not be null");
        AssertUtils.notNull(amount, "fundsInstruction.amount must not be null");
        AssertUtils.notNull(eventTime, "fundsInstruction.eventTime must not be null");
        AssertUtils.notNull(operator, "fundsInstruction.operator must not be null");
        AssertUtils.hasText(businessScene, "fundsInstruction.businessScene must not be blank");
        AssertUtils.hasText(businessSn, "fundsInstruction.businessSn must not be blank");
        originalAmount = originalAmount == null ? amount : originalAmount;
        exchangeRate = exchangeRate == null ? BigDecimal.ONE : exchangeRate;
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("fundsInstruction.amount must be positive");
        }
        if (originalAmount.getAmount() <= 0) {
            throw new IllegalArgumentException("fundsInstruction.originalAmount must be positive");
        }
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("fundsInstruction.exchangeRate must be positive");
        }
        if (ledgerPeriodType == null) {
            AssertUtils.isTrue(ledgerPeriodId == null,
                    "fundsInstruction.ledgerPeriodType must not be null when ledgerPeriodId is present");
        } else if (ledgerPeriodType == AccountBalancePeriodType.LIFETIME) {
            ledgerPeriodId = AccountBalancePeriodType.LIFETIME.name();
        } else {
            AssertUtils.hasText(ledgerPeriodId,
                    "fundsInstruction.ledgerPeriodId must not be blank for non-lifetime ledgerPeriodType");
        }
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables),
                "fundsInstruction.contextVariables must not contain sensitive fields");
        contextVariables = FundsBenefitSpecValidators.immutableInstructionContext(
                contextVariables, "fundsInstruction");
    }

    @Override
    public @NonNull FundsInstructionType getInstructionType() {
        return instructionType;
    }

    @Override
    public @NonNull FundsTransactionEventType getEventType() {
        return eventType;
    }

    @Override
    public @NonNull DefaultFundsTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public @NonNull Money getAmount() {
        return amount;
    }

    @Override
    public @NonNull Money getOriginalAmount() {
        return originalAmount == null ? amount : originalAmount;
    }

    @Override
    public @NonNull BigDecimal getExchangeRate() {
        return exchangeRate == null ? BigDecimal.ONE : exchangeRate;
    }

    @Override
    public @NonNull String getBusinessScene() {
        return businessScene;
    }

    @Override
    public @NonNull String getBusinessSn() {
        return businessSn;
    }

    @Override
    public @NonNull LocalDateTime getEventTime() {
        return eventTime;
    }

    @Override
    public @NonNull FundsOperationActorSpec getOperator() {
        return operator;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }


    @Override
    public @Nullable Long getTenantId() {
        return tenantId;
    }

    @Override
    public @Nullable PaymentInstrumentRefSpec getInstrumentRef() {
        return instrumentRef;
    }

    @Override
    public @Nullable ExternalAccountRefSpec getExternalAccountRef() {
        return externalAccountRef;
    }

    @Override
    public @Nullable FundsAccountId getAccountId() {
        return accountId;
    }

    @Override
    public @Nullable FundsAccountId getPayerAccountId() {
        return payerAccountId;
    }

    @Override
    public @Nullable FundsAccountId getPayeeAccountId() {
        return payeeAccountId;
    }

    @Override
    public @Nullable FundsAccountId getPayerId() {
        return payerId;
    }

    @Override
    public @Nullable FundsAccountId getPayeeId() {
        return payeeId;
    }

    @Override
    public @Nullable LedgerSubjectCode getPayerLedgerSubjectCode() {
        return payerLedgerSubjectCode;
    }

    @Override
    public @Nullable LedgerSubjectCode getPayeeLedgerSubjectCode() {
        return payeeLedgerSubjectCode;
    }

    @Override
    public @Nullable FundsAccountId getLinkedFundingAccountId() {
        return linkedFundingAccountId;
    }

    @Override
    public @Nullable AccountBalancePeriodType getLedgerPeriodType() {
        return ledgerPeriodType;
    }

    @Override
    public @Nullable String getLedgerPeriodId() {
        return ledgerPeriodId;
    }

    @Override
    public @Nullable FundsInstructionReferenceSpec getReference() {
        return reference;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

}

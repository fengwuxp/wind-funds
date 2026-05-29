package com.capte.funds.transaction.ledger;

import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.model.FundsContextVariables;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 账本交易信息工厂。
 */
public final class LedgerTransactionSpecFactory {

    private static final WindSequenceType LEDGER_SEQUENCE_TYPE = WindSequenceType.immutable(
            "LEDGER_TRANSACTION", "LE", 6);

    private LedgerTransactionSpecFactory() {
        throw new AssertionError();
    }

    @NonNull
    public static LedgerTransactionSpec createLedgerTransaction(
            @NonNull FundsInstructionSpec instruction,
            Function<String, List<LedgerPostingPlanSpec>> factory) {
        return createLedgerTransaction(instruction, null, factory);
    }

    @NonNull
    public static LedgerTransactionSpec createLedgerTransaction(
            @NonNull FundsInstructionSpec instruction,
            @Nullable String fundsTransactionSn,
            Function<String, List<LedgerPostingPlanSpec>> factory) {
        String sn = TemporalSequenceFactory.hourNext(LEDGER_SEQUENCE_TYPE);
        return DefaultLedgerTransactionSpec.builder()
                .sn(sn)
                .tenantId(instruction.getTenantId())
                .instructionType(instruction.getInstructionType())
                .fundsTransactionSn(fundsTransactionSn)
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .status(LedgerTransactionStatus.POSTED)
                .amount(instruction.getAmount())
                .originalAmount(instruction.getOriginalAmount())
                .exchangeRate(instruction.getExchangeRate())
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .transactionTime(instruction.getEventTime())
                .description(instruction.getDescription())
                .postingPlans(factory.apply(sn))
                .contextVariables(ledgerTransactionContext(instruction))
                .build();
    }

    private static Map<String, Object> ledgerTransactionContext(FundsInstructionSpec instruction) {
        if (instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION) {
            return Map.of();
        }
        return instruction.getContextVariables();
    }

    @NonNull
    public static LedgerPostingPlanSpec postingPlan(@NonNull LedgerPostingIntentType intent,
                                                    @NonNull String ledgerTransactionSn,
                                                    @NonNull List<LedgerPostingPhaseSpec> phases) {
        return postingPlan(intent, ledgerTransactionSn, null, null, phases);
    }

    @NonNull
    public static LedgerPostingPlanSpec postingPlan(@NonNull LedgerPostingIntentType intent,
                                                    @NonNull String ledgerTransactionSn,
                                                    @Nullable LedgerPostingScope postingScope,
                                                    @Nullable LedgerBalanceEffectType balanceEffectType,
                                                    @NonNull List<LedgerPostingPhaseSpec> phases) {
        DefaultLedgerPostingPlanSpec result = DefaultLedgerPostingPlanSpec.builder()
                .planId(intent + WindConstants.UNDERLINE + ledgerTransactionSn)
                .intent(intent)
                .ledgerTransactionSn(ledgerTransactionSn)
                .postingScope(postingScope)
                .balanceEffectType(balanceEffectType)
                .postingPhases(phases)
                .build();
        AssertUtils.isTrue(result.isBalanced(), "ledger entry not balanced");
        return result;
    }

    public static LedgerPostingPhaseSpec postingPhase(@NonNull LedgerPhaseCode phaseCode,
                                                      @NonNull List<LedgerEntrySpec> entries) {
        return DefaultLedgerPostingPhaseSpec.builder()
                .phaseCode(phaseCode)
                .entries(entries)
                .build();
    }

    @Getter
    public static class DefaultLedgerTransactionSpec implements LedgerTransactionSpec {

        @Schema(description = "账户ID")
        @NotNull
        @Size(min = 12, max = 50)
        private final String sn;

        @Schema(description = "租户ID")
        private final Long tenantId;

        @Schema(description = "指令类型")
        private final FundsInstructionType instructionType;

        @Schema(description = "标准业务交易流水")
        private final String fundsTransactionSn;

        @Schema(description = "事件类型")
        @NotNull
        private final FundsTransactionEventType eventType;

        @Schema(description = "交易类型")
        private final DefaultFundsTransactionType transactionType;

        @Schema(description = "交易状态")
        @NotNull
        private final LedgerTransactionStatus status;

        @Schema(description = "交易金额，单位：分")
        @NotNull
        private final Money amount;

        @Schema(description = "原始金额，单位：分")
        private final Money originalAmount;

        @Schema(description = "汇率")
        private final BigDecimal exchangeRate;

        @Schema(description = "业务单号")
        @Size(min = 10, max = 80)
        private final String businessSn;

        @Schema(description = "业务场景")
        @NotNull
        private final String businessScene;

        @Schema(description = "关联账本交易单号")
        private final String referenceLedgerTransactionSn;

        @Schema(description = "交易时间")
        @NotNull
        private final LocalDateTime transactionTime;

        @Schema(description = "交易描述")
        private final String description;

        private final List<LedgerPostingPlanSpec> postingPlans;

        @Schema(description = "上下文变量")
        private final Map<String, Object> contextVariables;

        @Builder
        public DefaultLedgerTransactionSpec(String sn,
                                            Long tenantId,
                                            FundsInstructionType instructionType,
                                            String fundsTransactionSn,
                                            FundsTransactionEventType eventType,
                                            DefaultFundsTransactionType transactionType,
                                            LedgerTransactionStatus status,
                                            Money amount,
                                            Money originalAmount,
                                            BigDecimal exchangeRate,
                                            String businessSn,
                                            String businessScene,
                                            String referenceLedgerTransactionSn,
                                            LocalDateTime transactionTime,
                                            String description,
                                            List<LedgerPostingPlanSpec> postingPlans,
                                            Map<String, Object> contextVariables) {
            this.sn = sn;
            this.tenantId = tenantId;
            this.instructionType = instructionType;
            this.fundsTransactionSn = fundsTransactionSn;
            this.eventType = eventType;
            this.transactionType = transactionType;
            this.status = status;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.businessSn = businessSn;
            this.businessScene = businessScene;
            this.referenceLedgerTransactionSn = referenceLedgerTransactionSn;
            this.transactionTime = transactionTime;
            this.description = description;
            this.postingPlans = List.copyOf(postingPlans == null ? List.of() : postingPlans);
            this.contextVariables = FundsContextVariables.immutableCopy(contextVariables);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return originalAmount == null ? amount : originalAmount;
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return exchangeRate == null ? BigDecimal.ONE : exchangeRate;
        }
    }

    @Getter
    private static final class DefaultLedgerPostingPlanSpec implements LedgerPostingPlanSpec {

        private final String planId;

        private final String ledgerTransactionSn;

        private final String routeLegId;

        private final LedgerPostingIntentType intent;

        private final LedgerPostingScope postingScope;

        private final LedgerBalanceEffectType balanceEffectType;

        private final List<LedgerPostingPhaseSpec> postingPhases;

        @Builder
        private DefaultLedgerPostingPlanSpec(String planId,
                                             String ledgerTransactionSn,
                                             String routeLegId,
                                             LedgerPostingIntentType intent,
                                             LedgerPostingScope postingScope,
                                             LedgerBalanceEffectType balanceEffectType,
                                             List<LedgerPostingPhaseSpec> postingPhases) {
            this.planId = planId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.routeLegId = routeLegId;
            this.intent = intent;
            this.postingScope = postingScope;
            this.balanceEffectType = balanceEffectType;
            this.postingPhases = List.copyOf(postingPhases == null ? List.of() : postingPhases);
        }
    }

    @Getter
    private static final class DefaultLedgerPostingPhaseSpec implements LedgerPostingPhaseSpec {

        private final LedgerPhaseCode phaseCode;

        private final List<LedgerEntrySpec> entries;

        @Builder
        private DefaultLedgerPostingPhaseSpec(LedgerPhaseCode phaseCode,
                                              List<LedgerEntrySpec> entries) {
            this.phaseCode = phaseCode;
            this.entries = List.copyOf(entries == null ? List.of() : new ArrayList<>(entries));
        }
    }
}

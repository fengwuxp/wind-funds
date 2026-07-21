package com.wind.funds.spec.ledger;

import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 账本交易定义。
 *
 * <p>职责：
 * <ul>
 *   <li>作为账本写入口的唯一交易输入对象</li>
 *   <li>聚合本次交易的 PostingPlan、业务标识、金额和交易上下文</li>
 *   <li>提供交易级借贷平衡校验能力</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不负责资金路径选择，路径选择由 RouteResolver 完成</li>
 *   <li>不负责持久化执行，持久化由 LedgerTransactionPostingService 完成</li>
 *   <li>不表达业务订单生命周期，业务生命周期由资金交易层维护</li>
 * </ul>
 */
public interface LedgerTransactionSpec {

    @Nullable
    Long getTenantId();

    @NonNull
    String getSn();

    @Nullable
    default FundsInstructionType getInstructionType() {
        return null;
    }

    @NonNull
    FundsTransactionEventType getEventType();

    @NonNull
    Money getAmount();

    @NonNull
    default Money getOriginalAmount() {
        return getAmount();
    }

    @NonNull
    default BigDecimal getExchangeRate() {
        return BigDecimal.ONE;
    }

    @NonNull
    default CurrencyIsoCode getCurrency() {
        return getAmount().getCurrency();
    }

    String getBusinessSn();

    @Nullable
    default String getFundsTransactionSn() {
        return null;
    }

    @Nullable
    default DefaultFundsTransactionType getTransactionType() {
        return null;
    }

    @NonNull
    String getBusinessScene();

    String getReferenceLedgerTransactionSn();

    @NonNull
    LocalDateTime getTransactionTime();

    String getDescription();

    @NonNull
    List<LedgerPostingPlanSpec> getPostingPlans();

    @NonNull
    Map<String, Object> getContextVariables();

    @NotNull
    default Money getTotalDebitAmount() {
        return getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getTotalDebitAmount)
                .reduce(Money::add)
                .orElseThrow();
    }

    @NotNull
    default Money getTotalCreditAmount() {
        return getPostingPlans()
                .stream()
                .map(LedgerPostingPlanSpec::getTotalCreditAmount)
                .reduce(Money::add)
                .orElseThrow();
    }

    default boolean isBalanced() {
        List<LedgerPostingPlanSpec> postingPlans = getPostingPlans();
        return !postingPlans.isEmpty()
                && postingPlans.stream().allMatch(LedgerPostingPlanSpec::isBalanced)
                && getTotalCreditAmount().equals(getTotalDebitAmount());
    }
}

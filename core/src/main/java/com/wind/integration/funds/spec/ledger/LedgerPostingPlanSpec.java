package com.wind.integration.funds.spec.ledger;

import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.transaction.core.Money;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.beans.Transient;
import java.util.List;
import java.util.Map;

/**
 * LedgerPostingPlan（账务计划 / 记账蓝图）。
 *
 * <p>职责：
 * <ul>
 *   <li>表达一组可执行且自洽的账务计划</li>
 *   <li>聚合若干 PostingPhase，保证同一意图下的借贷平衡</li>
 *   <li>作为 LedgerTransaction 的组成部分被入账服务消费</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不负责路由解析</li>
 *   <li>不负责账本查询和余额投影</li>
 *   <li>不直接写库，只提供计划语义和校验</li>
 * </ul>
 */
public interface LedgerPostingPlanSpec {

    @NonNull
    String getPlanId();

    @NonNull
    String getLedgerTransactionSn();

    @Nullable
    default String getRouteLegId() {
        return null;
    }

    @NonNull
    LedgerPostingIntentType getIntent();

    @Nullable
    default LedgerPostingScope getPostingScope() {
        return null;
    }

    @Nullable
    default LedgerBalanceEffectType getBalanceEffectType() {
        return null;
    }

    default boolean isPhased() {
        return true;
    }

    @NonNull
    List<LedgerPostingPhaseSpec> getPostingPhases();

    @NonNull
    default Money getAmount() {
        return getTotalDebitAmount();
    }

    @Nullable
    default String getDescription() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }

    @NonNull
    @Transient
    default List<LedgerEntrySpec> getEntries() {
        return getPostingPhases().stream()
                .map(LedgerPostingPhaseSpec::getEntries)
                .flatMap(List::stream)
                .toList();
    }

    @NotNull
    default Money getTotalDebitAmount() {
        return getEntries().stream()
                .filter(e -> e.getEntryType() == EntrySide.DEBIT)
                .map(LedgerEntrySpec::getAmount)
                .reduce(Money::add)
                .orElse(Money.ZERO);
    }

    @NotNull
    default Money getTotalCreditAmount() {
        return getEntries().stream()
                .filter(e -> e.getEntryType() == EntrySide.CREDIT)
                .map(LedgerEntrySpec::getAmount)
                .reduce(Money::add)
                .orElse(Money.ZERO);
    }

    default boolean isBalanced() {
        List<LedgerEntrySpec> entries = getEntries();
        return !entries.isEmpty()
                && entries.stream().allMatch(LedgerPostingPlanSpec::isPostableEntry)
                && getTotalCreditAmount().equals(getTotalDebitAmount());
    }

    private static boolean isPostableEntry(@NonNull LedgerEntrySpec entry) {
        return StringUtils.hasText(entry.getSubjectId())
                && StringUtils.hasText(entry.getSubjectType())
                && StringUtils.hasText(entry.getLedgerTransactionSn())
                && isPositiveEntryAmount(entry);
    }

    private static boolean isPositiveEntryAmount(@NonNull LedgerEntrySpec entry) {
        return entry.getAmount().getAmount() > 0;
    }

}

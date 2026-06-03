package com.wind.funds.spec.ledger;

import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 账本条目定义。
 *
 * <p>职责：
 * <ul>
 *   <li>表达账本中的单条不可变事实分录</li>
 *   <li>承载 subject、科目、借贷方向、金额、阶段和审计上下文</li>
 *   <li>作为最终入库前的最小账务事实单元</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不负责余额计算</li>
 *   <li>不负责路由决策</li>
 *   <li>不应在写入后被修改</li>
 * </ul>
 */
public interface LedgerEntrySpec {

    @NonNull
    String getSubjectId();

    @NonNull
    String getSubjectType();

    @NonNull
    LedgerSubjectCode getLedgerSubjectCode();

    @NonNull
    LedgerSubjectCategory getLedgerSubjectCategory();

    @Nullable
    default Long getLedgerId() {
        return null;
    }

    @NonNull
    String getLedgerTransactionSn();

    @Nullable
    default String getPostingPlanSn() {
        return null;
    }

    @NonNull
    EntrySide getEntryType();

    @NonNull
    default EntrySide getEntrySide() {
        return getEntryType();
    }

    @Nullable
    default LedgerPhaseCode getPhaseCode() {
        return null;
    }

    @Nullable
    default LedgerPostingIntentType getIntent() {
        return null;
    }

    @Nullable
    default LedgerPostingScope getPostingScope() {
        return null;
    }

    @Nullable
    default LedgerBalanceEffectType getBalanceEffectType() {
        return null;
    }

    @Nullable
    default LedgerBalanceConstraintType getBalanceConstraintType() {
        return null;
    }

    @NonNull
    String getBusinessScene();

    String getBusinessSn();

    @NonNull
    Money getAmount();

    @NonNull
    default CurrencyIsoCode getCurrency() {
        return getAmount().getCurrency();
    }

    @NonNull
    Money getOriginalAmount();

    @NonNull
    BigDecimal getExchangeRate();

    @NonNull
    LocalDateTime getTransactionTime();

    String getDescription();

    @NonNull
    Map<String, Object> getContextVariables();

    @Nullable
    default String getSha256() {
        return null;
    }
}

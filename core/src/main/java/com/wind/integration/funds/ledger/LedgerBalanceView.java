package com.wind.integration.funds.ledger;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;


/**
 * LedgerBalanceView（账本余额视图 / 账户余额投影）
 *
 * <p>定义：
 * 用于表达某个资金账户在账本系统中的“多维余额投影结果”。
 *
 * <p>金融语义：
 * 不是单一余额，而是“按账本科目维度拆分后的余额结构”。
 *
 * <p>工程语义：
 * - 由 Ledger Entry 聚合计算得出（Projection）
 * - 只读视图（Read Model）
 * - 可用于对账 / 风控 / 展示
 * <p>
 * ------------------------------------------------------------
 * <h2>核心职责</h2>
 * <p>
 * 1. 表达账户在账本中的余额分桶结果
 * 2. 支持多 LedgerAccountCode 维度拆分
 * 3. 支持余额快照（Snapshot）
 * 4. 用于查询层（CQRS Read Side）
 * <p>
 * ------------------------------------------------------------
 * <h2>重要约束</h2>
 * <p>
 * ✔ 不允许写入
 * ✔ 不允许业务计算逻辑
 * ✔ 只允许 projection 生成
 *
 */
public interface LedgerBalanceView {

    /**
     * 账户标识
     */
    @NonNull
    FundsAccountId getAccountId();

    /**
     * 币种
     */
    @NonNull
    CurrencyIsoCode getCurrency();

    /**
     * 各 LedgerAccountCode 下的余额分布
     *
     * <p>key = CASH / AVAILABLE / FROZEN / PREPAYMENT ...
     */
    @NonNull
    Map<LedgerSubjectCode, LedgerBalanceBucket> getBalanceBuckets();

    /**
     * 获取指定账本科目的余额
     *
     * @param accountCode 账本科目
     * @return LedgerBalanceBucket
     */
    @Nullable
    default LedgerBalanceBucket getBalanceBucketNullable(@NonNull LedgerSubjectCode accountCode) {
        return getBalanceBuckets().get(accountCode);
    }

    @NonNull
    default LedgerBalanceBucket getBalanceBucket(@NonNull LedgerSubjectCode accountCode) {
        LedgerBalanceBucket result = getBalanceBucketNullable(accountCode);
        AssertUtils.notNull(result, "LedgerBalanceBucket not found for {} ", accountCode);
        return result;
    }

    /**
     * 获取指定账本科目的余额
     *
     * @param accountCode 账本科目
     * @return LedgerBalanceBucket
     */
    @NonNull
    default Money getBalance(@NonNull LedgerSubjectCode accountCode) {
        return Optional.ofNullable(getBalanceBucketNullable(accountCode))
                .map(LedgerBalanceBucket::balance)
                .orElseGet(() -> Money.immutable(0, getCurrency()));
    }

}
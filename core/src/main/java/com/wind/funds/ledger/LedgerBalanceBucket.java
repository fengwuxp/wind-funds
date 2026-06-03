package com.wind.funds.ledger;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 账本余额桶
 *
 * @param accountCode 账户类型
 * @param balance     账本余额
 * @param periodType  余额周期类型
 * @param periodId    周期标识
 * @param activeTime  余额生效时间
 * @param expireTime  余额失效时间，可空 = 永久有效
 * @author wuxp
 * @date 2026-04-24 13:01
 **/
@Builder
public record LedgerBalanceBucket(@NonNull LedgerSubjectCode accountCode,
                                  @NonNull Money balance,
                                  @NonNull AccountBalancePeriodType periodType,
                                  @NonNull String periodId,
                                  @NonNull LocalDateTime activeTime,
                                  @Nullable LocalDateTime expireTime) {

    /**
     * 是否可用于结算/对账
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activeTime)) {
            return false;
        }
        return expireTime == null || now.isBefore(expireTime);
    }
}
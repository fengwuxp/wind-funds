package com.capte.funds.wallet.model.dto;

import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 资金主体当前余额。
 *
 * @author Codex
 * @date 2026-05-12
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsSubjectBalanceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5498618308640773225L;

    @Schema(description = "主体表主键")
    private Long id;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "账务主体引用")
    private FundsAccountId subjectRef;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "主体是否已经初始化账本")
    private Boolean initialized;

    @Schema(description = "账本科目余额桶")
    private Map<LedgerSubjectCode, LedgerBalanceBucket> balanceBuckets;

    /**
     * 是否已经初始化账本。
     *
     * <p>用于区分“未建账”和“账本已建但余额为 0”。</p>
     *
     * @return true 表示主体已经存在至少一个账本余额桶
     */
    public boolean isInitialized() {
        return Boolean.TRUE.equals(initialized);
    }
}

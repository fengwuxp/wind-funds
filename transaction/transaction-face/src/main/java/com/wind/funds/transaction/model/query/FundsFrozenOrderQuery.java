package com.wind.funds.transaction.model.query;

import com.wind.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 资金冻结订单查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsFrozenOrderQuery {

    @Schema(description = "冻结单号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "被冻结主体 ID")
    private String subjectId;

    @Schema(description = "被冻结主体类型")
    private FundsSubjectType subjectType;

    @Schema(description = "冻结类型")
    private String freezeType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "外部业务号")
    private String businessSn;

    @Schema(description = "关联资金交易号")
    private String transactionSn;

    @Schema(description = "冻结单状态")
    private FundsFrozenOrderStatus status;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;
}

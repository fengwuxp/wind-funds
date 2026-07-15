package com.wind.funds.wallet.model.query;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支出主体资金关系查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendSubjectFundingRelationQuery {

    @Schema(description = "关系号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "支出控制主体 ID")
    private String spendSubjectId;

    @Schema(description = "支出控制主体类型")
    private FundsSubjectType spendSubjectType;

    @Schema(description = "资金责任目标主体类型")
    private FundsSubjectType targetSubjectType;

    @Schema(description = "资金责任目标主体 ID")
    private String targetSubjectId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "关系类型")
    private SpendSubjectFundingRelationType relationType;
}

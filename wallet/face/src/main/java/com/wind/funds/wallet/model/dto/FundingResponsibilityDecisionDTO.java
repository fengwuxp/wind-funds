package com.wind.funds.wallet.model.dto;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资金责任解析决策 DTO。
 *
 * @author Codex
 * @date 2026-06-16
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundingResponsibilityDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -5401527225865966440L;

    @Schema(description = "资金责任关系主键")
    private Long relationId;

    @Schema(description = "资金责任关系号")
    private String relationSn;

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

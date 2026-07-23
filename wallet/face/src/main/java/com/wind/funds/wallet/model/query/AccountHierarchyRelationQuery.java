package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 账户层级关系查询条件，所有字段均为可选条件。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class AccountHierarchyRelationQuery {

    @Schema(description = "关系号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "子账户标识")
    private FundsAccountId accountId;

    @Schema(description = "直接父账户标识")
    private FundsAccountId parentAccountId;
}

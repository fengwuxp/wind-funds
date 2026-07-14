package com.wind.funds.wallet.model.dto;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账户层级绑定 DTO。
 *
 * @author Codex
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class AccountHierarchyBindingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8582215662379749592L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "层级绑定流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "子账户 ID")
    private String accountId;

    @Schema(description = "子账户类型")
    private FundsSubjectType accountType;

    @Schema(description = "父账户 ID")
    private String parentAccountId;

    @Schema(description = "父账户类型")
    private FundsSubjectType parentAccountType;

    @Schema(description = "根账户 ID")
    private String rootAccountId;

    @Schema(description = "根账户类型")
    private FundsSubjectType rootAccountType;

    @Schema(description = "层级关系币种")
    private CurrencyIsoCode currency;

    @Schema(description = "操作人 ID")
    private String operatorId;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}

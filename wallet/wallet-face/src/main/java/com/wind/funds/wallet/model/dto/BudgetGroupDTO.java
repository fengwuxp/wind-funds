package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
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
 * 预算控制范围 DTO。
 *
 * <p>BudgetGroup 是历史兼容名，目标语义为 Spend Rule 可引用的支出控制 scope，不是账务主体。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class BudgetGroupDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5222298784204923649L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "预算控制范围标识，历史字段名仍为预算组号，不表示账务主体")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "预算控制范围归属主体 ID")
    private String ownerId;

    @Schema(description = "预算控制范围归属主体类型")
    private FundsAccountOwnerType ownerType;

    @Schema(description = "预算控制范围业务类型")
    private String budgetType;

    @Schema(description = "控制币种")
    private CurrencyIsoCode currency;

    @Schema(description = "控制周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "控制周期标识")
    private String periodId;

    @Schema(description = "控制周期策略")
    private String periodPolicy;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;

    @Schema(description = "乐观锁版本")
    private Integer version;

}

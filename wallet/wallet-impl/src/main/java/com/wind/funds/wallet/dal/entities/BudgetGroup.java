package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预算组。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(BudgetGroup.TABLE_NAME)
public class BudgetGroup implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 8450696281937455315L;

    public static final String TABLE_NAME = "t_budget_group";

    /**
     * 自增主键。
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 最后修改时间。
     */
    private LocalDateTime gmtModified;

    /**
     * 预算组流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 预算归属主体 ID。
     */
    @NotNull
    private String ownerId;

    /**
     * 预算归属主体类型。
     */
    @NotNull
    private FundsAccountOwnerType ownerType;

    /**
     * 预算组业务类型。
     */
    @NotNull
    private String budgetType;

    /**
     * 预算币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 预算周期类型。
     */
    @NotNull
    private AccountBalancePeriodType periodType;

    /**
     * 预算周期标识。
     */
    @NotNull
    private String periodId;

    /**
     * 预算周期策略。
     */
    private String periodPolicy;

    /**
     * 预算组状态。
     */
    @NotNull
    private FundsAccountStatus status;

    /**
     * 预算组说明。
     */
    private String description;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;

    /**
     * 乐观锁版本号。
     */
    @Column(version = true)
    private Integer version;
}

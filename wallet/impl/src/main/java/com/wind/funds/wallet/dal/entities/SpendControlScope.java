package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支出控制范围。
 *
 * <p>目标语义为 Spend Rule 可引用的支出控制 scope，不是账务主体。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(SpendControlScope.TABLE_NAME)
public class SpendControlScope implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 8450696281937455315L;

    public static final String TABLE_NAME = "t_spend_control_scope";

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
     * 支出控制范围标识，不表达账务主体。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 支出控制范围归属主体 ID。
     */
    @NotNull
    private String ownerId;

    /**
     * 支出控制范围归属主体类型。
     */
    @NotNull
    private FundsAccountOwnerType ownerType;

    /**
     * 支出控制范围业务类型。
     */
    @NotNull
    private String scopeType;

    /**
     * 控制币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 控制周期类型。
     */
    @NotNull
    private AccountBalancePeriodType periodType;

    /**
     * 控制周期标识。
     */
    @NotNull
    private String periodId;

    /**
     * 控制周期策略。
     */
    private String periodPolicy;

    /**
     * 支出控制范围状态。
     */
    @NotNull
    @Column("status")
    private FundsAccountState state;

    /**
     * 支出控制范围说明。
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

package com.wind.funds.wallet.dal.entities;

import com.wind.funds.ledger.enums.LedgerProfileCode;
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
 * 信用账户。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(CreditAccount.TABLE_NAME)
public class CreditAccount implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -276281330337768302L;

    public static final String TABLE_NAME = "t_credit_account";

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
     * 信用账户流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 账户归属主体 ID。
     */
    @NotNull
    private String ownerId;

    /**
     * 账户归属主体类型。
     */
    @NotNull
    private FundsAccountOwnerType ownerType;

    /**
     * 信用账户业务类型。
     */
    @NotNull
    private String accountType;

    /**
     * 账户币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 账期类型。
     */
    @NotNull
    private AccountBalancePeriodType periodType;

    /**
     * 账期标识。
     */
    @NotNull
    private String periodId;

    /**
     * 账目 profile 编码快照。
     */
    @NotNull
    private LedgerProfileCode ledgerProfileCode;

    /**
     * 账目 profile 版本快照。
     */
    @NotNull
    private Integer ledgerProfileVersion;

    /**
     * 信用账户状态。
     */
    @NotNull
    @Column("status")
    private FundsAccountState state;

    void setStatus(FundsAccountState state) {
        this.state = state;
    }

    /**
     * 账户说明。
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

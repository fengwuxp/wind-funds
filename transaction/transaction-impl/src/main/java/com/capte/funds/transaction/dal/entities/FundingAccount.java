package com.capte.funds.transaction.dal.entities;

import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.capte.funds.transaction.enums.PlatformFundingAccountRole;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 真实资金账户。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(FundingAccount.TABLE_NAME)
public class FundingAccount implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 3036292050619015915L;

    public static final String TABLE_NAME = "t_funding_account";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String ownerId;

    @NotNull
    private FundsAccountOwnerType ownerType;

    @NotNull
    private String accountType;

    @Column("is_platform")
    private Boolean platform;

    private PlatformFundingAccountRole accountRoleCode;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private LedgerProfileCode ledgerProfileCode;

    @NotNull
    private Integer ledgerProfileVersion;

    @NotNull
    private FundsAccountStatus status;

    private String description;

    private String contextVariables;

    @Column(version = true)
    private Integer version;
}

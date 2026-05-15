package com.capte.funds.transaction.dal.entities;

import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
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

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private AccountBalancePeriodType periodType;

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

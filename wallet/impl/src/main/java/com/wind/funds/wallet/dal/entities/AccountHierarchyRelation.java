package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账户层级关系。
 */
@Data
@Table(AccountHierarchyRelation.TABLE_NAME)
@FieldNameConstants
public class AccountHierarchyRelation implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -4839543394122716087L;

    public static final String TABLE_NAME = "t_account_hierarchy_relation";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String accountId;

    @NotNull
    private FundsSubjectType accountType;

    @NotNull
    private String parentAccountId;

    @NotNull
    private FundsSubjectType parentAccountType;

    @NotNull
    private CurrencyIsoCode currency;

    private String operatorId;
}

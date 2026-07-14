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
 * 账户层级绑定。
 */
@Data
@Table(AccountHierarchyBinding.TABLE_NAME)
@FieldNameConstants
public class AccountHierarchyBinding implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -6683168253469182421L;

    public static final String TABLE_NAME = "t_account_hierarchy_binding";

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
     * 层级绑定流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 子账户 ID。
     */
    @NotNull
    private String accountId;

    /**
     * 子账户类型。
     */
    @NotNull
    private FundsSubjectType accountType;

    /**
     * 父账户 ID。
     */
    @NotNull
    private String parentAccountId;

    /**
     * 父账户类型。
     */
    @NotNull
    private FundsSubjectType parentAccountType;

    /**
     * 层级关系币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 操作人 ID。
     */
    private String operatorId;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;
}

package com.capte.funds.wallet.dal.entities;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支出控制主体和真实资金账户关系。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(SpendSubjectFundingRel.TABLE_NAME)
public class SpendSubjectFundingRel implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 1242282312419314754L;

    public static final String TABLE_NAME = "t_spend_subject_funding_rel";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String spendSubjectId;

    @NotNull
    private FundsSubjectType spendSubjectType;

    @NotNull
    private String fundingAccountId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private SpendSubjectFundingRelationType relationType;

    @NotNull
    private Integer priority;

    @NotNull
    @Column("is_default")
    private Boolean defaultRelation;

    @NotNull
    private FundsAccountStatus status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String description;

    private String contextVariables;
}

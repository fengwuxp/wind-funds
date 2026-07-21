package com.wind.funds.wallet.dal.entities;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
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
     * 资金关系流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 支出控制主体 ID。
     */
    @NotNull
    private String spendSubjectId;

    /**
     * 支出控制主体类型。
     */
    @NotNull
    private FundsSubjectType spendSubjectType;

    /**
     * 目标资金主体类型。
     */
    @NotNull
    private FundsSubjectType targetSubjectType;

    /**
     * 目标资金主体 ID。
     */
    @NotNull
    private String targetSubjectId;

    /**
     * 关系适用币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 资金关系类型。
     */
    @NotNull
    private SpendSubjectFundingRelationType relationType;

    /**
     * 关系说明。
     */
    private String description;
}

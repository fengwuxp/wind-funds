package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.wallet.enums.SpendRuleVersionStatus;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 版本。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@Table(SpendRuleVersion.TABLE_NAME)
public class SpendRuleVersion implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -3373643783677375499L;

    public static final String TABLE_NAME = "t_spend_rule_version";

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
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * Spend Rule 标识。
     */
    @NotNull
    private String ruleId;

    /**
     * Spend Rule 版本。
     */
    @NotNull
    private String ruleVersion;

    /**
     * 规则规格 JSON。
     */
    @NotNull
    private String ruleSpec;

    /**
     * 规则规格摘要。
     */
    @NotNull
    private String ruleDigest;

    /**
     * 规则版本状态。
     */
    @NotNull
    private SpendRuleVersionStatus status;

    /**
     * 操作者。
     */
    @NotNull
    private String operatorId;

    /**
     * 审计引用。
     */
    @NotNull
    private String auditReferenceSn;

    /**
     * 规则版本说明。
     */
    private String description;
}

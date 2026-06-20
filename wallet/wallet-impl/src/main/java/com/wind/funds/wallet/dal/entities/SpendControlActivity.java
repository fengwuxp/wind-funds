package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支出控制活动。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@Table(SpendControlActivity.TABLE_NAME)
public class SpendControlActivity implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -9026190484962394413L;

    public static final String TABLE_NAME = "t_spend_control_activity";

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
     * 支出控制活动流水号，用于幂等、回放和审计追踪。
     */
    @NotNull
    private String activitySn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 支出控制活动类型。
     */
    @NotNull
    private SpendControlActivityType activityType;

    /**
     * 业务场景。
     */
    @NotNull
    private String businessScene;

    /**
     * 业务流水号或请求幂等号。
     */
    @NotNull
    private String businessSn;

    /**
     * 原支出控制活动流水号，用于交易消费、释放或退款补偿回链。
     */
    private String originalActivitySn;

    /**
     * 已存在的资金交易流水号，用于交易结果消费控制活动回链。
     */
    private String transactionSn;

    /**
     * 支付工具号。
     */
    @NotNull
    private String instrumentSn;

    /**
     * 支付工具动作。
     */
    @NotNull
    private PaymentInstrumentAction action;

    /**
     * 控制活动目标资金主体 ID，只允许资金账户或信用账户。
     */
    @NotNull
    private String targetSubjectId;

    /**
     * 控制活动目标资金主体类型，只允许资金账户或信用账户。
     */
    @NotNull
    private FundsSubjectType targetSubjectType;

    /**
     * 控制金额，最小货币单位。
     */
    @NotNull
    private Long amount;

    /**
     * 控制活动币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * Spend Rule 标识。
     */
    @NotNull
    private String spendRuleId;

    /**
     * Spend Rule 版本。
     */
    @NotNull
    private String spendRuleVersion;

    /**
     * Spend Rule 决策流水号。
     */
    @NotNull
    private String spendDecisionSn;

    /**
     * Spend Rule 决策结果。
     */
    @NotNull
    private SpendControlDecisionResult spendDecisionResult;

    /**
     * Spend Rule 决策摘要。
     */
    @NotNull
    private String spendDecisionDigest;

    /**
     * 预算组或预算控制范围标识，不表达账务主体。
     */
    private String budgetGroupSn;

    /**
     * 拒绝原因。
     */
    private String rejectReason;

    /**
     * 控制活动摘要，用于同活动流水幂等一致性判断。
     */
    @NotNull
    private String activityDigest;

    /**
     * 控制活动说明。
     */
    private String description;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;
}

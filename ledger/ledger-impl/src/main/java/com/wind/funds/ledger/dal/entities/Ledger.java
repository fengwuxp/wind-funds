package com.wind.funds.ledger.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 账户账本
 *
 * @author wuxp
 * @date 2026-04-14 09:47
 **/
@Table(Ledger.TABLE_NAME)
@Data
public class Ledger implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -4018515383389532849L;

    public static final String TABLE_NAME = "t_ledger";

    /**
     * 自增主键。
     */
    @Id(keyType = KeyType.Auto)
    @NotNull
    private Long id;

    /**
     * 创建时间。
     */
    @NotNull
    private LocalDateTime gmtCreate;

    /**
     * 最后修改时间。
     */
    @NotNull
    private LocalDateTime gmtModified;

    /**
     * 租户ID
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 账务主体 ID
     */
    @NotNull
    private String subjectId;

    /**
     * 账务主体类型
     */
    @NotNull
    private String subjectType;

    /**
     * 账务 profile 编码快照
     */
    @NotNull
    private String ledgerProfileCode;

    /**
     * 账务 profile 版本快照
     */
    @NotNull
    private Integer ledgerProfileVersion;

    /**
     * 账本编码
     */
    @NotNull
    private LedgerSubjectCode ledgerSubjectCode;

    /**
     * 会计科目类别快照
     */
    @NotNull
    private LedgerSubjectCategory ledgerSubjectCategory;

    /**
     * 正常余额方向
     */
    @NotNull
    private EntrySide normalBalanceSide;

    /**
     * 是否允许负余额
     */
    @NotNull
    @Column("is_allow_negative")
    private Boolean allowNegative;

    /**
     * 借方累计发生额，单位：分
     */
    @NotNull
    @Column("debit_amount")
    private Long debitAmount;

    /**
     * 贷方累计发生额，单位：分
     */
    @NotNull
    @Column("credit_amount")
    private Long creditAmount;

    /**
     * 币种
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 结算策略
     */
    @NotNull
    private String settlementPolicy;

    /**
     * 结算日切时间
     */
    @NotNull
    private LocalTime cutOffTime;

    /**
     * 周期类型
     */
    @NotNull
    private AccountBalancePeriodType periodType;

    /**
     * 周期标识，例如：
     * 年 2026
     * 月 2026-04
     * 周 2026-W16
     */
    @NotNull
    @Size(min = 2, max = 20)
    private String periodId;

    /**
     * 版本号
     */
    @Column(version = true)
    private Integer version;

    /**
     * 虚拟字段：按正常余额方向计算出的净额余额。
     *
     * @return normal balance
     */
    public Long getNormalBalance() {
        if (debitAmount == null || creditAmount == null || normalBalanceSide == null) {
            return null;
        }
        long rawBalance = debitAmount - creditAmount;
        return normalBalanceSide == EntrySide.DEBIT ? rawBalance : -rawBalance;
    }

}

package com.wind.integration.funds.spec.ledger;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalTime;

/**
 * LedgerProfile 科目配置。
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface LedgerProfileItemSpec {

    /**
     * @return 余额桶编码
     */
    @NonNull
    LedgerSubjectCode getLedgerSubjectCode();

    /**
     * @return 科目类别
     */
    @NonNull
    LedgerSubjectCategory getLedgerSubjectCategory();

    /**
     * @return 正常余额方向
     */
    @NonNull
    EntrySide getNormalBalanceSide();

    /**
     * @return 是否允许负余额
     */
    @NonNull
    Boolean getAllowNegative();

    /**
     * @return 主体初始化时是否必建
     */
    @NonNull
    Boolean getRequired();

    /**
     * @return 周期类型
     */
    @NonNull
    AccountBalancePeriodType getPeriodType();

    /**
     * @return 结算策略表达式
     */
    @NonNull
    String getSettlementPolicy();

    /**
     * @return 日切时间
     */
    @NonNull
    LocalTime getCutOffTime();

    /**
     * @return 描述
     */
    @Nullable
    String getDescription();
}

package com.wind.integration.funds.route.spec;

import com.wind.integration.funds.route.ref.SubjectRef;
import org.jspecify.annotations.Nullable;

/**
 * 平台账户解析结果快照。
 *
 * <p>平台账户本身属于资金账户，但在充值、提现、手续费、授权结算等路径中承担平台侧资金责任。
 * Route 首次解析时需要把这些账户固化到快照，后续 replay 复用原解析结果，避免平台账户配置变化导致历史资金事实漂移。</p>
 */
public interface PlatformAccountsSnapshotSpec {

    /**
     * 平台现金映射账户，用于外部入金、提现出款等平台资金承接场景。
     */
    @Nullable
    default SubjectRef getCashFundingAccount() {
        return null;
    }

    /**
     * 平台预收/预付账户，用于待清算或待确认资金暂存场景。
     */
    @Nullable
    default SubjectRef getPrepaymentFundingAccount() {
        return null;
    }

    /**
     * 平台清算账户，用于商户收款、平台清分等清算中间态。
     */
    @Nullable
    default SubjectRef getClearingFundingAccount() {
        return null;
    }

    /**
     * 平台结算账户，用于授权结算、商户结算等最终平台结算承接。
     */
    @Nullable
    default SubjectRef getSettlementFundingAccount() {
        return null;
    }

    /**
     * 平台手续费账户，用于手续费收取和手续费回退路径。
     */
    @Nullable
    default SubjectRef getFeeFundingAccount() {
        return null;
    }

    /**
     * 平台调整挂账账户，用于差错、调账、长短款核销等过渡场景。
     */
    @Nullable
    default SubjectRef getAdjustmentFundingAccount() {
        return null;
    }
}

package com.wind.integration.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * 账户类型枚举
 *
 * @author wuxp
 * @date 2026-04-09 14:20
 **/
@AllArgsConstructor
@Getter
public enum DefaultFundsAccountType implements DescriptiveEnum {

    /* ===== 资金基础设施 ===== */

    /**
     * 备付金账户（平台在银行/PSP的资金）
     */
    RESERVE_FUND(LedgerSubjectCategory.ASSET, "备付金账户"),

    /**
     * 清算中间账户（过渡态，允许正负）
     */
    CLEARING(LedgerSubjectCategory.CLEARING, "清算中间账户"),

    /**
     * 结算账户（已完成清算的资金池）
     */
    SETTLEMENT(LedgerSubjectCategory.ASSET, "结算账户"),

    /* ===== 平台账户 ===== */

    /**
     * 平台主资金账户
     */
    PLATFORM_MASTER(LedgerSubjectCategory.ASSET, "平台主账户"),

    /**
     * 平台收入账户（手续费等）
     */
    PLATFORM_REVENUE(LedgerSubjectCategory.REVENUE, "平台收入账户"),

    /**
     * 平台权益账户（留存收益等）
     */
    PLATFORM_EQUITY(LedgerSubjectCategory.EQUITY, "平台权益账户"),

    /**
     * 平台成本账户（补贴/支出）
     */
    PLATFORM_COST(LedgerSubjectCategory.EXPENSE, "平台成本账户"),

    /**
     * 平台负债账户（代管资金）
     */
    PLATFORM_LIABILITY(LedgerSubjectCategory.LIABILITY, "平台负债账户"),

    /**
     * 平台挂账账户（明确调账/挂账场景）
     */
    PLATFORM_SUSPENSE(LedgerSubjectCategory.SUSPENSE, "平台挂账账户"),

    /* ===== 用户账户 ===== */

    /**
     * 用户钱包（平台对用户的负债）
     */
    USER_WALLET(LedgerSubjectCategory.LIABILITY, "用户钱包"),

    /**
     * 全球账户（多币种/统一余额）
     */
    GLOBAL_ACCOUNT(LedgerSubjectCategory.LIABILITY, "全球账户"),

    /**
     * 返利账户
     */
    REBATE_WALLET(LedgerSubjectCategory.LIABILITY, "返利钱包"),

    /**
     * 冻结账户（风控冻结）
     */
    FROZEN(LedgerSubjectCategory.LIABILITY, "冻结账户"),

    /* ===== VCC 账户 ===== */

    /**
     * 预付卡账户
     */
    PREPAID_VCC(LedgerSubjectCategory.LIABILITY, "预付卡"),

    /**
     * 共享卡账户
     */
    SHARE_VCC(LedgerSubjectCategory.LIABILITY, "共享卡"),

    /**
     * 信用卡账户（授信负债）
     */
    CREDIT_CARD(LedgerSubjectCategory.LIABILITY, "信用卡"),

    /**
     * 预算组（仅控制额度，不直接参与资金）
     */
    BUDGET_GROUP(LedgerSubjectCategory.MEMO, "预算组"),

    /* ===== 对账 / 清算 ===== */

    /**
     * 应收账款
     */
    ACCOUNT_RECEIVABLE(LedgerSubjectCategory.ASSET, "应收账户"),

    /**
     * 应付账款
     */
    ACCOUNT_PAYABLE(LedgerSubjectCategory.LIABILITY, "应付账户"),

    /**
     * 通道结算账户（PSP/卡组织）
     */
    CHANNEL_SETTLEMENT(LedgerSubjectCategory.CLEARING, "通道结算账户"),

    /* ===== 外部账户，用于平台出入金 ===== */

    EXTERNAL_BANK(LedgerSubjectCategory.CLEARING, "银行转账（线下/线上）"),

    EXTERNAL_WALLET(LedgerSubjectCategory.CLEARING, "第三方钱包充值"),

    EXTERNAL_VA(LedgerSubjectCategory.CLEARING, "VA 账户"),

    /* ===== 商户 ===== */
    PLATFORM_MERCHANT(LedgerSubjectCategory.CLEARING, "平台商户"),
    EXTERNAL_MERCHANT(LedgerSubjectCategory.CLEARING, "外部商户"),
    ;


    /**
     * 会计科目类型（决定借贷方向、报表归类等）
     */
    private final LedgerSubjectCategory accountCategory;

    /**
     * 描述
     */
    private final String desc;


    private static final Set<DefaultFundsAccountType> USER_WALLET_TYPES = Set.of(
            USER_WALLET,
            GLOBAL_ACCOUNT
    );

    private static final Set<DefaultFundsAccountType> CREDIT_CARD_TYPES = Set.of(
            PREPAID_VCC,
            SHARE_VCC,
            CREDIT_CARD
    );


    private static final Set<DefaultFundsAccountType> EXTERNAL_ACCOUNT_TYPES = Set.of(
            EXTERNAL_BANK,
            EXTERNAL_VA,
            EXTERNAL_WALLET,
            EXTERNAL_MERCHANT
    );

    private static final Set<String> NON_EXTERNAL_ACCOUNT_TYPE_NAMES = Set.of(
            FundsSubjectType.FUNDING_ACCOUNT.name(),
            FundsSubjectType.CREDIT_ACCOUNT.name(),
            FundsSubjectType.BUDGET_GROUP.name(),
            RouteNodeType.SUBJECT.name(),
            RouteNodeType.PAYMENT_INSTRUMENT.name(),
            RouteNodeType.PLATFORM_FUNDING_ACCOUNT.name()
    );

    public static boolean isCreditCard(@NonNull FundsAccountId accountId) {
        return isCreditCard(accountId.type());
    }

    public static boolean isCreditCard(@NonNull String accountType) {
        return isCreditCard(DefaultFundsAccountType.valueOf(accountType));
    }

    public static boolean isCreditCard(@NonNull DefaultFundsAccountType accountType) {
        return CREDIT_CARD_TYPES.contains(accountType);
    }

    public static boolean isUserWalletType(@NonNull FundsAccountId accountId) {
        return isUserWalletType(accountId.type());
    }

    public static boolean isUserWalletType(@NonNull String accountType) {
        return isUserWalletType(DefaultFundsAccountType.valueOf(accountType));
    }

    public static boolean isUserWalletType(@NonNull DefaultFundsAccountType accountType) {
        return USER_WALLET_TYPES.contains(accountType);
    }


    public static boolean isExternalAccount(@NonNull FundsAccountId accountId) {
        return isExternalAccount(accountId.type());
    }

    public static boolean isExternalAccount(@NonNull String accountType) {
        if (RouteNodeType.EXTERNAL_ACCOUNT.name().equals(accountType)) {
            return true;
        }
        if (NON_EXTERNAL_ACCOUNT_TYPE_NAMES.contains(accountType)) {
            return false;
        }
        return isExternalAccount(DefaultFundsAccountType.valueOf(accountType));
    }

    public static boolean isExternalAccount(@NonNull DefaultFundsAccountType accountType) {
        return EXTERNAL_ACCOUNT_TYPES.contains(accountType);
    }
}

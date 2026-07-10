package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

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
     * 现金映射账户（平台外部资金的内部映射）。
     */
    CASH_MAPPING(LedgerSubjectCategory.ASSET, "现金映射账户"),

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
     * 返利账户（平台对用户的返利负债）
     */
    REBATE_ACCOUNT(LedgerSubjectCategory.LIABILITY, "返利账户"),

    /**
     * 冻结账户（风控冻结）
     */
    FROZEN(LedgerSubjectCategory.LIABILITY, "冻结账户"),

    /* ===== 卡账户 ===== */

    /**
     * 预付卡账户
     */
    PREPAID_CARD(LedgerSubjectCategory.LIABILITY, "预付卡账户"),

    /**
     * 共享卡账户
     */
    SHARED_CARD(LedgerSubjectCategory.LIABILITY, "共享卡账户"),

    /**
     * 信用卡账户（授信负债）
     */
    CREDIT_CARD(LedgerSubjectCategory.LIABILITY, "信用卡"),

    /**
     * 支出控制范围（仅控制额度，不直接参与资金）
     */
    SPEND_CONTROL_SCOPE(LedgerSubjectCategory.MEMO, "支出控制范围"),

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


    public static boolean isCreditCard(@NonNull FundsAccountId accountId) {
        return isCreditCard(accountId.type());
    }

    public static boolean isCreditCard(@NonNull String accountType) {
        return CreditFundsAccountType.isCreditAccountType(accountType);
    }

    public static boolean isCreditCard(@NonNull DefaultFundsAccountType accountType) {
        return CreditFundsAccountType.isCreditAccountType(accountType);
    }

    public static boolean isFundingAccountType(@NonNull FundsAccountId accountId) {
        return isFundingAccountType(accountId.type());
    }

    public static boolean isFundingAccountType(@NonNull String accountType) {
        return FundingAccountType.isFundingAccountType(accountType);
    }

    public static boolean isFundingAccountType(@NonNull DefaultFundsAccountType accountType) {
        return FundingAccountType.isFundingAccountType(accountType);
    }

    public static boolean isUserWalletType(@NonNull FundsAccountId accountId) {
        return isUserWalletType(accountId.type());
    }

    public static boolean isUserWalletType(@NonNull String accountType) {
        return UserWalletFundsAccountType.isUserWalletType(accountType);
    }

    public static boolean isUserWalletType(@NonNull DefaultFundsAccountType accountType) {
        return UserWalletFundsAccountType.isUserWalletType(accountType);
    }


    public static boolean isExternalAccount(@NonNull FundsAccountId accountId) {
        return isExternalAccount(accountId.type());
    }

    public static boolean isExternalAccount(@NonNull String accountType) {
        return ExternalFundsAccountType.isExternalAccount(accountType);
    }

    public static boolean isExternalAccount(@NonNull DefaultFundsAccountType accountType) {
        return ExternalFundsAccountType.isExternalAccount(accountType);
    }
}

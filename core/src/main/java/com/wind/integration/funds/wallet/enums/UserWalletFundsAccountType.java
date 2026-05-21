package com.wind.integration.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import com.wind.integration.funds.wallet.FundsAccountId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户钱包资金账户类型。
 *
 * <p>该分类轴只表达面向用户余额的钱包类型，不承载外部账户、平台账户或信用额度类型。
 *
 * @author Codex
 * @date 2026-05-15
 */
@AllArgsConstructor
@Getter
public enum UserWalletFundsAccountType implements DescriptiveEnum {

    /**
     * 标准用户钱包。
     */
    STANDARD(DefaultFundsAccountType.USER_WALLET, "标准用户钱包"),

    /**
     * 全球多币种账户。
     */
    GLOBAL(DefaultFundsAccountType.GLOBAL_ACCOUNT, "全球账户");

    private static final Map<DefaultFundsAccountType, UserWalletFundsAccountType> BY_DEFAULT_ACCOUNT_TYPE =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(UserWalletFundsAccountType::getAccountType,
                            Function.identity()));

    private final DefaultFundsAccountType accountType;

    private final String desc;

    public static Optional<UserWalletFundsAccountType> fromAccountType(@NonNull FundsAccountId accountId) {
        return fromAccountType(accountId.type());
    }

    public static Optional<UserWalletFundsAccountType> fromAccountType(@NonNull String accountType) {
        return fromAccountType(DefaultFundsAccountType.valueOf(accountType));
    }

    public static Optional<UserWalletFundsAccountType> fromAccountType(@NonNull DefaultFundsAccountType accountType) {
        return Optional.ofNullable(BY_DEFAULT_ACCOUNT_TYPE.get(accountType));
    }

    public static boolean isUserWalletType(@NonNull FundsAccountId accountId) {
        return isUserWalletType(accountId.type());
    }

    public static boolean isUserWalletType(@NonNull String accountType) {
        return fromAccountType(accountType).isPresent();
    }

    public static boolean isUserWalletType(@NonNull DefaultFundsAccountType accountType) {
        return fromAccountType(accountType).isPresent();
    }
}

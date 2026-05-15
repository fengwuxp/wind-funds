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
 * 信用与 VCC 资金账户类型。
 *
 * <p>该分类轴只表达预付、共享额度和信用账户形态，不承载用户钱包、外部账户或平台账户类型。
 *
 * @author Codex
 * @date 2026-05-15
 */
@AllArgsConstructor
@Getter
public enum CreditFundsAccountType implements DescriptiveEnum {

    PREPAID_VCC(DefaultFundsAccountType.PREPAID_VCC, "预付 VCC"),

    SHARED_VCC(DefaultFundsAccountType.SHARE_VCC, "共享额度 VCC"),

    CREDIT_CARD(DefaultFundsAccountType.CREDIT_CARD, "信用卡账户");

    private static final Map<DefaultFundsAccountType, CreditFundsAccountType> BY_DEFAULT_ACCOUNT_TYPE =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(CreditFundsAccountType::getLegacyAccountType,
                            Function.identity()));

    private final DefaultFundsAccountType legacyAccountType;

    private final String desc;

    public static Optional<CreditFundsAccountType> fromAccountType(@NonNull FundsAccountId accountId) {
        return fromAccountType(accountId.type());
    }

    public static Optional<CreditFundsAccountType> fromAccountType(@NonNull String accountType) {
        return fromAccountType(DefaultFundsAccountType.valueOf(accountType));
    }

    public static Optional<CreditFundsAccountType> fromAccountType(@NonNull DefaultFundsAccountType accountType) {
        return Optional.ofNullable(BY_DEFAULT_ACCOUNT_TYPE.get(accountType));
    }

    public static boolean isCreditAccountType(@NonNull FundsAccountId accountId) {
        return isCreditAccountType(accountId.type());
    }

    public static boolean isCreditAccountType(@NonNull String accountType) {
        return fromAccountType(accountType).isPresent();
    }

    public static boolean isCreditAccountType(@NonNull DefaultFundsAccountType accountType) {
        return fromAccountType(accountType).isPresent();
    }
}

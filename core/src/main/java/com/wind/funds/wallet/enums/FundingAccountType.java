package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.wallet.FundsAccountId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资金账户产品类型。
 *
 * <p>该分类轴表达平台对用户形成余额或可用资金事实的账户形态，不承载外部账户、信用额度账户或平台角色。
 *
 * @author Codex
 * @date 2026-05-17
 */
@AllArgsConstructor
@Getter
public enum FundingAccountType implements DescriptiveEnum {

    /**
     * 标准用户钱包。
     */
    USER_WALLET(DefaultFundsAccountType.USER_WALLET, "标准用户钱包"),

    /**
     * 全球多币种账户。
     */
    GLOBAL_ACCOUNT(DefaultFundsAccountType.GLOBAL_ACCOUNT, "全球账户"),

    /**
     * 返利账户，平台对用户的返利负债。
     */
    REBATE_ACCOUNT(DefaultFundsAccountType.REBATE_ACCOUNT, "返利账户"),

    /**
     * 预付卡账户，先有资金余额，再绑定卡消费。
     */
    PREPAID_CARD(DefaultFundsAccountType.PREPAID_CARD, "预付卡账户");

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = "SPEND_CONTROL_SCOPE";

    private static final Map<DefaultFundsAccountType, FundingAccountType> BY_DEFAULT_ACCOUNT_TYPE =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(FundingAccountType::getAccountType,
                            Function.identity()));

    private static final Set<String> NON_FUNDING_ACCOUNT_TYPE_NAMES = Set.of(
            FundsSubjectType.FUNDING_ACCOUNT.name(),
            FundsSubjectType.CREDIT_ACCOUNT.name(),
            SPEND_CONTROL_SCOPE_ACCOUNT_TYPE,
            RouteNodeType.SUBJECT.name(),
            RouteNodeType.PAYMENT_INSTRUMENT.name(),
            RouteNodeType.PLATFORM_FUNDING_ACCOUNT.name(),
            RouteNodeType.EXTERNAL_ACCOUNT.name()
    );

    private final DefaultFundsAccountType accountType;

    private final String desc;

    public static Optional<FundingAccountType> fromAccountType(@NonNull FundsAccountId accountId) {
        return fromAccountType(accountId.type());
    }

    public static Optional<FundingAccountType> fromAccountType(@NonNull String accountType) {
        if (NON_FUNDING_ACCOUNT_TYPE_NAMES.contains(accountType)) {
            return Optional.empty();
        }
        return fromAccountType(DefaultFundsAccountType.valueOf(accountType));
    }

    public static Optional<FundingAccountType> fromAccountType(@NonNull DefaultFundsAccountType accountType) {
        return Optional.ofNullable(BY_DEFAULT_ACCOUNT_TYPE.get(accountType));
    }

    public static boolean isFundingAccountType(@NonNull FundsAccountId accountId) {
        return isFundingAccountType(accountId.type());
    }

    public static boolean isFundingAccountType(@NonNull String accountType) {
        return fromAccountType(accountType).isPresent();
    }

    public static boolean isFundingAccountType(@NonNull DefaultFundsAccountType accountType) {
        return fromAccountType(accountType).isPresent();
    }
}

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
 * 外部资金账户类型。
 *
 * <p>该分类轴只表达外部对手方或外部账户，不承载内部资金账户、平台账户角色或会计科目分类。
 *
 * @author Codex
 * @date 2026-05-15
 */
@AllArgsConstructor
@Getter
public enum ExternalFundsAccountType implements DescriptiveEnum {

    /**
     * 外部银行账户。
     */
    BANK(DefaultFundsAccountType.EXTERNAL_BANK, "外部银行账户"),

    /**
     * 外部第三方钱包。
     */
    WALLET(DefaultFundsAccountType.EXTERNAL_WALLET, "外部钱包账户"),

    /**
     * 外部虚拟账户。
     */
    VIRTUAL_ACCOUNT(DefaultFundsAccountType.EXTERNAL_VA, "外部虚拟账户"),

    /**
     * 外部商户。
     */
    MERCHANT(DefaultFundsAccountType.EXTERNAL_MERCHANT, "外部商户账户");

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private static final Map<DefaultFundsAccountType, ExternalFundsAccountType> BY_DEFAULT_ACCOUNT_TYPE =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(ExternalFundsAccountType::getAccountType,
                            Function.identity()));

    private static final Set<String> NON_EXTERNAL_ACCOUNT_TYPE_NAMES = Set.of(
            FundsSubjectType.FUNDING_ACCOUNT.name(),
            FundsSubjectType.CREDIT_ACCOUNT.name(),
            SPEND_CONTROL_SCOPE_ACCOUNT_TYPE,
            RouteNodeType.SUBJECT.name(),
            RouteNodeType.PAYMENT_INSTRUMENT.name(),
            RouteNodeType.PLATFORM_FUNDING_ACCOUNT.name()
    );

    private final DefaultFundsAccountType accountType;

    private final String desc;

    public static Optional<ExternalFundsAccountType> fromAccountType(@NonNull FundsAccountId accountId) {
        return fromAccountType(accountId.type());
    }

    public static Optional<ExternalFundsAccountType> fromAccountType(@NonNull String accountType) {
        if (RouteNodeType.EXTERNAL_ACCOUNT.name().equals(accountType)
                || NON_EXTERNAL_ACCOUNT_TYPE_NAMES.contains(accountType)) {
            return Optional.empty();
        }
        return fromAccountType(DefaultFundsAccountType.valueOf(accountType));
    }

    public static Optional<ExternalFundsAccountType> fromAccountType(@NonNull DefaultFundsAccountType accountType) {
        return Optional.ofNullable(BY_DEFAULT_ACCOUNT_TYPE.get(accountType));
    }

    public static boolean isExternalAccount(@NonNull FundsAccountId accountId) {
        return isExternalAccount(accountId.type());
    }

    public static boolean isExternalAccount(@NonNull String accountType) {
        if (RouteNodeType.EXTERNAL_ACCOUNT.name().equals(accountType)) {
            return true;
        }
        return fromAccountType(accountType).isPresent();
    }

    public static boolean isExternalAccount(@NonNull DefaultFundsAccountType accountType) {
        return fromAccountType(accountType).isPresent();
    }
}

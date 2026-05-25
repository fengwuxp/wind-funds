package com.wind.integration.funds.util;

import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitComponentSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitRefundPolicySpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitSnapshotSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.spec.transaction.FundsBenefitComponentSpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitRefundPolicySpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitSnapshotSpec;
import com.wind.integration.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.integration.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.integration.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.integration.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.integration.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.integration.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.integration.funds.transaction.enums.FundsBenefitType;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权益快照 JSON/Map 显式装配支持。
 *
 * <p>该类型只负责把解析后的 JSON 对象结构映射回不可变资金 DSL 模型；资金不变量仍由各
 * {@code Immutable...builder().build()} 收口，不为反序列化打开空构造或字段填充入口。</p>
 */
public final class FundsBenefitSnapshotJsonSupport {

    private FundsBenefitSnapshotJsonSupport() {
    }

    public static @NonNull FundsBenefitSnapshotSpec parseSnapshot(@NonNull Map<String, ?> values) {
        return ImmutableFundsBenefitSnapshotSpec.builder()
                .benefitSnapshotId(requireText(values, "benefitSnapshotId"))
                .benefitSchemaVersion(optionalText(values, "benefitSchemaVersion"))
                .benefitGroupSn(requireText(values, "benefitGroupSn"))
                .orderSn(optionalText(values, "orderSn"))
                .pricingSnapshotSn(optionalText(values, "pricingSnapshotSn"))
                .orderAmount(requiredPositiveMoney(values, "orderAmount"))
                .userPayAmount(requiredNonNegativeMoney(values, "userPayAmount"))
                .merchantReceivableAmount(optionalNonNegativeMoney(values, "merchantReceivableAmount"))
                .components(parseComponents(requiredObjects(values, "components")))
                .refundPolicy(parseRefundPolicy(optionalObject(values, "refundPolicy")))
                .decisionSource(optionalText(values, "decisionSource"))
                .decisionTraceId(optionalText(values, "decisionTraceId"))
                .contextVariables(contextVariables(values))
                .build();
    }

    private static List<FundsBenefitComponentSpec> parseComponents(List<Map<String, ?>> values) {
        List<FundsBenefitComponentSpec> result = new ArrayList<>(values.size());
        for (Map<String, ?> value : values) {
            result.add(parseComponent(value));
        }
        return List.copyOf(result);
    }

    private static FundsBenefitComponentSpec parseComponent(Map<String, ?> value) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn(requireText(value, "componentSn"))
                .sequence(optionalInt(value, "sequence"))
                .benefitType(requiredEnum(FundsBenefitType.class, value, "benefitType"))
                .componentType(requiredEnum(FundsBenefitComponentType.class, value, "componentType"))
                .closureRole(requiredEnum(FundsBenefitAmountClosureRole.class, value, "closureRole"))
                .amount(requiredPositiveMoney(value, "amount"))
                .ledgerEffect(requiredEnum(FundsBenefitLedgerEffect.class, value, "ledgerEffect"))
                .fundingNature(requiredEnum(FundsBenefitFundingNature.class, value, "fundingNature"))
                .bearerSubjectRef(parseSubjectRef(optionalObject(value, "bearerSubjectRef")))
                .beneficiarySubjectRef(parseSubjectRef(optionalObject(value, "beneficiarySubjectRef")))
                .fundingSubjectRef(parseSubjectRef(optionalObject(value, "fundingSubjectRef")))
                .fundingAccountRole(optionalText(value, "fundingAccountRole"))
                .benefitReference(parseReference(requiredObject(value, "benefitReference")))
                .refundPolicy(parseRefundPolicy(optionalObject(value, "refundPolicy")))
                .description(optionalText(value, "description"))
                .contextVariables(contextVariables(value))
                .build();
    }

    private static @Nullable SubjectRef parseSubjectRef(@Nullable Map<String, ?> value) {
        if (value == null) {
            return null;
        }
        return ImmutableSubjectRef.builder()
                .tenantId(optionalLong(value, "tenantId"))
                .subjectId(requireText(value, "subjectId"))
                .subjectType(requiredEnum(FundsSubjectType.class, value, "subjectType"))
                .subjectName(optionalText(value, "subjectName"))
                .currency(optionalText(value, "currency"))
                .ledgerProfileCode(optionalText(value, "ledgerProfileCode"))
                .description(optionalText(value, "description"))
                .build();
    }

    private static FundsBenefitReferenceSpec parseReference(Map<String, ?> value) {
        return ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId(optionalText(value, "campaignId"))
                .couponId(optionalText(value, "couponId"))
                .voucherId(optionalText(value, "voucherId"))
                .benefitInstanceId(optionalText(value, "benefitInstanceId"))
                .holdId(optionalText(value, "holdId"))
                .writeOffId(optionalText(value, "writeOffId"))
                .releaseId(optionalText(value, "releaseId"))
                .ruleVersion(optionalText(value, "ruleVersion"))
                .externalDecisionId(optionalText(value, "externalDecisionId"))
                .contextVariables(contextVariables(value))
                .build();
    }

    private static @Nullable FundsBenefitRefundPolicySpec parseRefundPolicy(@Nullable Map<String, ?> value) {
        if (value == null) {
            return null;
        }
        return ImmutableFundsBenefitRefundPolicySpec.builder()
                .partialRefundStrategy(optionalEnum(FundsBenefitPartialRefundStrategy.class,
                        value, "partialRefundStrategy"))
                .dispositions(requiredEnums(FundsBenefitRefundDisposition.class, value, "dispositions"))
                .refundableAmount(optionalNonNegativeMoney(value, "refundableAmount"))
                .nonRefundableAmount(optionalNonNegativeMoney(value, "nonRefundableAmount"))
                .refundRuleVersion(optionalText(value, "refundRuleVersion"))
                .refundPolicyCode(optionalText(value, "refundPolicyCode"))
                .refundDecisionId(optionalText(value, "refundDecisionId"))
                .decisionSource(optionalText(value, "decisionSource"))
                .decisionTime(optionalLocalDateTime(value, "decisionTime"))
                .contextVariables(contextVariables(value))
                .build();
    }

    private static Money requiredPositiveMoney(Map<String, ?> owner, String fieldName) {
        Map<String, ?> value = requiredObject(owner, fieldName);
        return FundsDslMoneyParser.parse(value);
    }

    private static Money requiredNonNegativeMoney(Map<String, ?> owner, String fieldName) {
        Map<String, ?> value = requiredObject(owner, fieldName);
        return FundsDslMoneyParser.parseNonNegative(value);
    }

    private static @Nullable Money optionalNonNegativeMoney(Map<String, ?> owner, String fieldName) {
        Map<String, ?> value = optionalObject(owner, fieldName);
        if (value == null) {
            return null;
        }
        return FundsDslMoneyParser.parseNonNegative(value);
    }

    private static Map<String, Object> contextVariables(Map<String, ?> owner) {
        Object value = owner.get("contextVariables");
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !StringUtils.hasText(key)) {
                    throw new IllegalArgumentException("contextVariables key must be text");
                }
                result.put(key, entry.getValue());
            }
            return Map.copyOf(result);
        }
        throw new IllegalArgumentException("contextVariables must be object");
    }

    private static <E extends Enum<E>> E requiredEnum(Class<E> enumType, Map<String, ?> owner, String fieldName) {
        String value = requireText(owner, fieldName);
        return enumValue(enumType, value, fieldName);
    }

    private static <E extends Enum<E>> @Nullable E optionalEnum(Class<E> enumType,
                                                               Map<String, ?> owner,
                                                               String fieldName) {
        String value = optionalText(owner, fieldName);
        return value == null ? null : enumValue(enumType, value, fieldName);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String fieldName) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " must be " + enumType.getSimpleName(), ex);
        }
    }

    private static <E extends Enum<E>> List<E> requiredEnums(Class<E> enumType,
                                                            Map<String, ?> owner,
                                                            String fieldName) {
        Object value = owner.get(fieldName);
        if (!(value instanceof List<?> values) || CollectionUtils.isEmpty(values)) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        List<E> result = new ArrayList<>(values.size());
        for (Object item : values) {
            if (!(item instanceof String text) || !StringUtils.hasText(text)) {
                throw new IllegalArgumentException(fieldName + " must contain text values");
            }
            try {
                result.add(Enum.valueOf(enumType, text));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(fieldName + " must be " + enumType.getSimpleName(), ex);
            }
        }
        return List.copyOf(result);
    }

    private static List<Map<String, ?>> requiredObjects(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (!(value instanceof List<?> values) || CollectionUtils.isEmpty(values)) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        List<Map<String, ?>> result = new ArrayList<>(values.size());
        for (Object item : values) {
            result.add(asObject(item, fieldName));
        }
        return List.copyOf(result);
    }

    private static Map<String, ?> requiredObject(Map<String, ?> owner, String fieldName) {
        return asObject(owner.get(fieldName), fieldName);
    }

    private static @Nullable Map<String, ?> optionalObject(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        return value == null ? null : asObject(value, fieldName);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> asObject(Object value, String fieldName) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }
        throw new IllegalArgumentException(fieldName + " must be object");
    }

    private static String requireText(Map<String, ?> owner, String fieldName) {
        String result = optionalText(owner, fieldName);
        if (result == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return result;
    }

    private static @Nullable String optionalText(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        throw new IllegalArgumentException(fieldName + " must be text");
    }

    private static int optionalInt(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case Byte number -> number.intValue();
            case Short number -> number.intValue();
            case Integer number -> number;
            default -> throw new IllegalArgumentException(fieldName + " must be integer");
        };
    }

    private static @Nullable Long optionalLong(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (value == null) {
            return null;
        }
        return switch (value) {
            case Byte number -> number.longValue();
            case Short number -> number.longValue();
            case Integer number -> number.longValue();
            case Long number -> number;
            default -> throw new IllegalArgumentException(fieldName + " must be long");
        };
    }

    private static @Nullable LocalDateTime optionalLocalDateTime(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return LocalDateTime.parse(text);
        }
        throw new IllegalArgumentException(fieldName + " must be ISO local date time");
    }
}

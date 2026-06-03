package com.wind.funds.util;

import com.alibaba.fastjson2.JSON;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.model.transaction.ImmutableFundsBenefitComponentSpec;
import com.wind.funds.model.transaction.ImmutableFundsBenefitReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsBenefitRefundPolicySpec;
import com.wind.funds.model.transaction.ImmutableFundsBenefitSnapshotSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.spec.transaction.FundsBenefitComponentSpec;
import com.wind.funds.spec.transaction.FundsBenefitReferenceSpec;
import com.wind.funds.spec.transaction.FundsBenefitRefundPolicySpec;
import com.wind.funds.spec.transaction.FundsBenefitSnapshotSpec;
import com.wind.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.funds.transaction.enums.FundsBenefitType;
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
                .benefitSnapshotId(requireText(values, ImmutableFundsBenefitSnapshotSpec.Fields.benefitSnapshotId))
                .benefitSchemaVersion(optionalText(values,
                        ImmutableFundsBenefitSnapshotSpec.Fields.benefitSchemaVersion))
                .benefitGroupSn(requireText(values, ImmutableFundsBenefitSnapshotSpec.Fields.benefitGroupSn))
                .orderSn(optionalText(values, ImmutableFundsBenefitSnapshotSpec.Fields.orderSn))
                .pricingSnapshotSn(optionalText(values, ImmutableFundsBenefitSnapshotSpec.Fields.pricingSnapshotSn))
                .orderAmount(requiredPositiveMoney(values, ImmutableFundsBenefitSnapshotSpec.Fields.orderAmount))
                .userPayAmount(requiredNonNegativeMoney(values, ImmutableFundsBenefitSnapshotSpec.Fields.userPayAmount))
                .merchantReceivableAmount(optionalNonNegativeMoney(values,
                        ImmutableFundsBenefitSnapshotSpec.Fields.merchantReceivableAmount))
                .components(parseComponents(requiredObjects(values, ImmutableFundsBenefitSnapshotSpec.Fields.components)))
                .refundPolicy(parseRefundPolicy(optionalObject(values,
                        ImmutableFundsBenefitSnapshotSpec.Fields.refundPolicy)))
                .decisionSource(optionalText(values, ImmutableFundsBenefitSnapshotSpec.Fields.decisionSource))
                .decisionTraceId(optionalText(values, ImmutableFundsBenefitSnapshotSpec.Fields.decisionTraceId))
                .contextVariables(contextVariables(values, ImmutableFundsBenefitSnapshotSpec.Fields.contextVariables))
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
                .componentSn(requireText(value, ImmutableFundsBenefitComponentSpec.Fields.componentSn))
                .sequence(optionalInt(value, ImmutableFundsBenefitComponentSpec.Fields.sequence))
                .benefitType(requiredEnum(FundsBenefitType.class, value,
                        ImmutableFundsBenefitComponentSpec.Fields.benefitType))
                .componentType(requiredEnum(FundsBenefitComponentType.class, value,
                        ImmutableFundsBenefitComponentSpec.Fields.componentType))
                .closureRole(requiredEnum(FundsBenefitAmountClosureRole.class, value,
                        ImmutableFundsBenefitComponentSpec.Fields.closureRole))
                .amount(requiredPositiveMoney(value, ImmutableFundsBenefitComponentSpec.Fields.amount))
                .ledgerEffect(requiredEnum(FundsBenefitLedgerEffect.class, value,
                        ImmutableFundsBenefitComponentSpec.Fields.ledgerEffect))
                .fundingNature(requiredEnum(FundsBenefitFundingNature.class, value,
                        ImmutableFundsBenefitComponentSpec.Fields.fundingNature))
                .bearerSubjectRef(parseSubjectRef(optionalObject(value,
                        ImmutableFundsBenefitComponentSpec.Fields.bearerSubjectRef)))
                .beneficiarySubjectRef(parseSubjectRef(optionalObject(value,
                        ImmutableFundsBenefitComponentSpec.Fields.beneficiarySubjectRef)))
                .fundingSubjectRef(parseSubjectRef(optionalObject(value,
                        ImmutableFundsBenefitComponentSpec.Fields.fundingSubjectRef)))
                .fundingAccountRole(optionalText(value,
                        ImmutableFundsBenefitComponentSpec.Fields.fundingAccountRole))
                .benefitReference(parseReference(requiredObject(value,
                        ImmutableFundsBenefitComponentSpec.Fields.benefitReference)))
                .refundPolicy(parseRefundPolicy(optionalObject(value,
                        ImmutableFundsBenefitComponentSpec.Fields.refundPolicy)))
                .description(optionalText(value, ImmutableFundsBenefitComponentSpec.Fields.description))
                .contextVariables(contextVariables(value, ImmutableFundsBenefitComponentSpec.Fields.contextVariables))
                .build();
    }

    private static @Nullable SubjectRef parseSubjectRef(@Nullable Map<String, ?> value) {
        if (value == null) {
            return null;
        }
        return ImmutableSubjectRef.builder()
                .tenantId(optionalLong(value, ImmutableSubjectRef.Fields.tenantId))
                .subjectId(requireText(value, ImmutableSubjectRef.Fields.subjectId))
                .subjectType(requiredEnum(FundsSubjectType.class, value, ImmutableSubjectRef.Fields.subjectType))
                .subjectName(optionalText(value, ImmutableSubjectRef.Fields.subjectName))
                .currency(optionalText(value, ImmutableSubjectRef.Fields.currency))
                .ledgerProfileCode(optionalText(value, ImmutableSubjectRef.Fields.ledgerProfileCode))
                .description(optionalText(value, ImmutableSubjectRef.Fields.description))
                .build();
    }

    private static FundsBenefitReferenceSpec parseReference(Map<String, ?> value) {
        return ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.campaignId))
                .couponId(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.couponId))
                .voucherId(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.voucherId))
                .benefitInstanceId(optionalText(value,
                        ImmutableFundsBenefitReferenceSpec.Fields.benefitInstanceId))
                .holdId(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.holdId))
                .writeOffId(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.writeOffId))
                .releaseId(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.releaseId))
                .ruleVersion(optionalText(value, ImmutableFundsBenefitReferenceSpec.Fields.ruleVersion))
                .externalDecisionId(optionalText(value,
                        ImmutableFundsBenefitReferenceSpec.Fields.externalDecisionId))
                .contextVariables(contextVariables(value,
                        ImmutableFundsBenefitReferenceSpec.Fields.contextVariables))
                .build();
    }

    private static @Nullable FundsBenefitRefundPolicySpec parseRefundPolicy(@Nullable Map<String, ?> value) {
        if (value == null) {
            return null;
        }
        return ImmutableFundsBenefitRefundPolicySpec.builder()
                .partialRefundStrategy(optionalEnum(FundsBenefitPartialRefundStrategy.class,
                        value, ImmutableFundsBenefitRefundPolicySpec.Fields.partialRefundStrategy))
                .dispositions(requiredEnums(FundsBenefitRefundDisposition.class, value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.dispositions))
                .refundableAmount(optionalNonNegativeMoney(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.refundableAmount))
                .nonRefundableAmount(optionalNonNegativeMoney(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.nonRefundableAmount))
                .refundRuleVersion(optionalText(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.refundRuleVersion))
                .refundPolicyCode(optionalText(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.refundPolicyCode))
                .refundDecisionId(optionalText(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.refundDecisionId))
                .decisionSource(optionalText(value, ImmutableFundsBenefitRefundPolicySpec.Fields.decisionSource))
                .decisionTime(optionalLocalDateTime(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.decisionTime))
                .contextVariables(contextVariables(value,
                        ImmutableFundsBenefitRefundPolicySpec.Fields.contextVariables))
                .build();
    }

    private static Money requiredPositiveMoney(Map<String, ?> owner, String fieldName) {
        Money value = requiredMoney(owner, fieldName);
        if (value.getAmount() <= 0) {
            throw new IllegalArgumentException(fieldName + "." + Money.Fields.amount + " must be positive");
        }
        return value;
    }

    private static Money requiredNonNegativeMoney(Map<String, ?> owner, String fieldName) {
        Money value = requiredMoney(owner, fieldName);
        if (value.getAmount() < 0) {
            throw new IllegalArgumentException(fieldName + "." + Money.Fields.amount + " must not be negative");
        }
        return value;
    }

    private static @Nullable Money optionalNonNegativeMoney(Map<String, ?> owner, String fieldName) {
        Money value = optionalMoney(owner, fieldName);
        if (value == null) {
            return null;
        }
        if (value.getAmount() < 0) {
            throw new IllegalArgumentException(fieldName + "." + Money.Fields.amount + " must not be negative");
        }
        return value;
    }

    private static Money requiredMoney(Map<String, ?> owner, String fieldName) {
        Money result = optionalMoney(owner, fieldName);
        if (result == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return result;
    }

    private static @Nullable Money optionalMoney(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Money money) {
            return money;
        }
        if (value instanceof Map<?, ?> moneyValues) {
            requireMoneyField(moneyValues, fieldName, Money.Fields.amount);
            requireMoneyField(moneyValues, fieldName, Money.Fields.currency);
            return JSON.to(Money.class, value);
        }
        throw new IllegalArgumentException(fieldName + " must be Money object");
    }

    private static void requireMoneyField(Map<?, ?> value, String fieldName, String moneyFieldName) {
        if (!value.containsKey(moneyFieldName) || value.get(moneyFieldName) == null) {
            throw new IllegalArgumentException(fieldName + "." + moneyFieldName + " is required");
        }
    }

    private static Map<String, Object> contextVariables(Map<String, ?> owner, String fieldName) {
        Object value = owner.get(fieldName);
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
        throw new IllegalArgumentException(fieldName + " must be object");
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

package com.wind.funds.ledger.mapstruct;

import com.wind.jackson.WindJson;
import com.wind.funds.ledger.dal.entities.Ledger;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * 账户账本 Converter
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, imports = {
        WindJson.class,
        Money.class
})
public interface LedgerConverter {

    LedgerConverter INSTANCE = Mappers.getMapper(LedgerConverter.class);

    /**
     * 创建请求 convert to 账户账本实体
     *
     * @param request 创建请求
     * @return Ledger 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "debitAmount", ignore = true)
    @Mapping(target = "creditAmount", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "version", ignore = true)
    Ledger convertToLedger(CreateLedgerRequest request);

    /**
     * 将 Ledger 实体转换为 LedgerDTO。
     *
     * @param data Ledger 实例
     * @return LedgerDTO 实例
     */
    LedgerDTO convertToLedgerDTO(Ledger data);

    /**
     * 创建请求 convert to 账户账本条目实体
     *
     * @param data 账本交易
     * @return LedgerEntry 实例
     */
    @Mapping(target = "subjectId", expression = "java(data.getSubjectId())")
    @Mapping(target = "subjectType", expression = "java(data.getSubjectType())")
    @Mapping(target = "entrySide", source = "entryType")
    @Mapping(target = "postingRole", source = "postingRole")
    @Mapping(target = "periodType", expression = "java(data.getPeriodType())")
    @Mapping(target = "periodId", expression = "java(data.getPeriodId())")
    @Mapping(target = "amount", expression = "java(data.getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(data.getAmount().getCurrency())")
    @Mapping(target = "originalAmount", expression = "java(data.getOriginalAmount().getAmount())")
    @Mapping(target = "originalCurrency", expression = "java(data.getOriginalAmount().getCurrency())")
    @Mapping(target = "balanceConstraintType", expression = "java(enumName(data.getBalanceConstraintType()))")
    @Mapping(target = "intent", expression = "java(enumName(data.getIntent()))")
    @Mapping(target = "postingScope", expression = "java(enumName(data.getPostingScope()))")
    @Mapping(target = "balanceEffectType", expression = "java(enumName(data.getBalanceEffectType()))")
    @Mapping(target = "phaseCode", expression = "java(enumName(data.getPhaseCode()))")
    @Mapping(target = "contextVariables", expression = "java(WindJson.toJsonString(data.getContextVariables()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "fundsTransactionSn", ignore = true)
    LedgerEntry convertToLedgerEntry(LedgerEntrySpec data);

    /**
     * 将 LedgerEntry 实体转换为 LedgerEntryDTO。
     *
     * @param data LedgerEntry 实例
     * @return LedgerEntryDTO 实例
     */
    @Mapping(target = "entryType", source = "entrySide")
    @Mapping(target = "postingRole", source = "postingRole")
    @Mapping(target = "periodType", source = "periodType")
    @Mapping(target = "periodId", source = "periodId")
    @Mapping(target = "amount", expression = "java(new Money(data.getAmount(),data.getCurrency()))")
    @Mapping(
            target = "originalAmount",
            expression = "java(new Money(data.getOriginalAmount(), data.getOriginalCurrency()))"
    )
    @Mapping(
            target = "balanceConstraintType",
            expression = "java(toLedgerBalanceConstraintType(data.getBalanceConstraintType()))"
    )
    @Mapping(target = "intent", expression = "java(toLedgerPostingIntentType(data.getIntent()))")
    @Mapping(target = "postingScope", expression = "java(toLedgerPostingScope(data.getPostingScope()))")
    @Mapping(target = "balanceEffectType", expression = "java(toLedgerBalanceEffectType(data.getBalanceEffectType()))")
    @Mapping(target = "phaseCode", expression = "java(toLedgerPhaseCode(data.getPhaseCode()))")
    @Mapping(target = "contextVariables", expression = "java(parseContextVariables(data.getContextVariables()))")
    LedgerEntryDTO convertToLedgerEntryDTO(LedgerEntry data);

    /**
     * 账本交易 convert to 账户账本交易实体
     *
     * @param data 账本交易
     * @return LedgerTransactionDefinition 实例
     */
    @Mapping(target = "amount", expression = "java(data.getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(data.getAmount().getCurrency())")
    @Mapping(target = "originalAmount", expression = "java(data.getOriginalAmount().getAmount())")
    @Mapping(target = "originalCurrency", expression = "java(data.getOriginalAmount().getCurrency())")
    @Mapping(target = "exchangeRate", expression = "java(data.getExchangeRate())")
    @Mapping(
            target = "instructionType",
            expression = "java(data.getInstructionType() == null ? null : data.getInstructionType().name())"
    )
    @Mapping(target = "eventType", expression = "java(data.getEventType().name())")
    @Mapping(
            target = "transactionType",
            expression = "java(data.getTransactionType() == null ? null : data.getTransactionType().name())"
    )
    @Mapping(target = "contextVariables", expression = "java(WindJson.toJsonString(data.getContextVariables()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "debitAmount", ignore = true)
    @Mapping(target = "creditAmount", ignore = true)
    @Mapping(target = "sha256", ignore = true)
    LedgerTransaction convertToLedgerTransaction(LedgerTransactionSpec data);

    /**
     * 将 LedgerTransaction 实体转换为 LedgerTransactionDTO。
     *
     * @param data LedgerTransaction 实例
     * @return LedgerTransactionDTO 实例
     */
    @Mapping(target = "amount", expression = "java(new Money(data.getAmount(),data.getCurrency()))")
    @Mapping(
            target = "originalAmount",
            expression = "java(new Money(data.getOriginalAmount(), data.getOriginalCurrency()))"
    )
    @Mapping(target = "eventType", expression = "java(toFundsTransactionEventType(data.getEventType()))")
    @Mapping(target = "instructionType", expression = "java(toFundsInstructionType(data.getInstructionType()))")
    @Mapping(
            target = "transactionType",
            expression = "java(toDefaultFundsTransactionType(data.getTransactionType()))"
    )
    @Mapping(target = "debitAmount", expression = "java(new Money(data.getDebitAmount(), data.getCurrency()))")
    @Mapping(target = "creditAmount", expression = "java(new Money(data.getCreditAmount(), data.getCurrency()))")
    @Mapping(target = "contextVariables", expression = "java(parseContextVariables(data.getContextVariables()))")
    @Mapping(target = "entries", ignore = true)
    LedgerTransactionDTO convertToAccountLedgerTransactionDTO(LedgerTransaction data);

    default Map<String, Object> parseContextVariables(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return WindJson.parseObject(value, new TypeReference<>() {
        });
    }

    /**
     * 将枚举转换为数据库字段值。
     *
     * @param value 枚举值
     * @return 枚举名称
     */
    default String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    /**
     * 将数据库字段值转换为余额约束类型。
     *
     * @param value 数据库字段值
     * @return 余额约束类型
     */
    default LedgerBalanceConstraintType toLedgerBalanceConstraintType(String value) {
        return StringUtils.hasText(value) ? LedgerBalanceConstraintType.valueOf(value) : null;
    }

    /**
     * 将数据库字段值转换为入账意图类型。
     *
     * @param value 数据库字段值
     * @return 入账意图类型
     */
    default LedgerPostingIntentType toLedgerPostingIntentType(String value) {
        return StringUtils.hasText(value) ? LedgerPostingIntentType.valueOf(value) : null;
    }

    /**
     * 将数据库字段值转换为入账范围。
     *
     * @param value 数据库字段值
     * @return 入账范围
     */
    default LedgerPostingScope toLedgerPostingScope(String value) {
        return StringUtils.hasText(value) ? LedgerPostingScope.valueOf(value) : null;
    }

    /**
     * 将数据库字段值转换为余额影响类型。
     *
     * @param value 数据库字段值
     * @return 余额影响类型
     */
    default LedgerBalanceEffectType toLedgerBalanceEffectType(String value) {
        return StringUtils.hasText(value) ? LedgerBalanceEffectType.valueOf(value) : null;
    }

    /**
     * 将数据库字段值转换为账本阶段编码。
     *
     * @param value 数据库字段值
     * @return 账本阶段编码
     */
    default LedgerPhaseCode toLedgerPhaseCode(String value) {
        return StringUtils.hasText(value) ? LedgerPhaseCode.valueOf(value) : null;
    }

    default FundsTransactionEventType toFundsTransactionEventType(String value) {
        return StringUtils.hasText(value) ? FundsTransactionEventType.valueOf(value) : null;
    }

    default FundsInstructionType toFundsInstructionType(String value) {
        return StringUtils.hasText(value) ? FundsInstructionType.valueOf(value) : null;
    }

    default DefaultFundsTransactionType toDefaultFundsTransactionType(String value) {
        return StringUtils.hasText(value) ? DefaultFundsTransactionType.valueOf(value) : null;
    }
}

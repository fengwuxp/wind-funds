package com.wind.funds.ledger.mapstruct;

import com.alibaba.fastjson2.JSON;
import com.wind.funds.ledger.dal.entities.Ledger;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.spec.ledger.LedgerEntrySpec;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

/**
 * 账户账本 Converter
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Mapper(imports = {
        JSON.class,
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
    Ledger convertToLedger(CreateLedgerRequest request);

    /**
     * 更新请求 convert to 账户账本实体
     *
     * @param request 更新请求
     * @return Ledger 实例
     */
    Ledger convertToLedger(UpdateLedgerBalanceRequest request);


    /**
     * Ledger convert to LedgerDTO
     *
     * @param data Ledger 实例
     * @return LedgerDTO 实例
     */
    @Mapping(target = "status", source = "status")
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
    @Mapping(target = "contextVariables", expression = "java(JSON.toJSONString(data.getContextVariables()))")
    LedgerEntry convertToLedgerEntry(LedgerEntrySpec data);

    /**
     * LedgerEntry convert to LedgerEntryDTO
     *
     * @param data LedgerEntry 实例
     * @return LedgerEntryDTO 实例
     */
    @Mapping(target = "entryType", source = "entrySide")
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
    @Mapping(target = "contextVariables", expression = "java(JSON.parseObject(data.getContextVariables()))")
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
    @Mapping(target = "contextVariables", expression = "java(JSON.toJSONString(data.getContextVariables()))")
    LedgerTransaction convertToLedgerTransaction(LedgerTransactionSpec data);

    /**
     * LedgerTransaction convert to LedgerTransactionDTO
     *
     * @param data LedgerTransaction 实例
     * @return LedgerTransactionDTO 实例
     */
    @Mapping(target = "amount", expression = "java(new Money(data.getAmount(),data.getCurrency()))")
    @Mapping(
            target = "originalAmount",
            expression = "java(new Money(data.getOriginalAmount(), data.getOriginalCurrency()))"
    )
    @Mapping(target = "contextVariables", expression = "java(JSON.parseObject(data.getContextVariables()))")
    LedgerTransactionDTO convertToAccountLedgerTransactionDTO(LedgerTransaction data);

    /**
     * Enum convert to database value.
     *
     * @param value enum value
     * @return enum name
     */
    default String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    /**
     * Database value convert to balance constraint type.
     *
     * @param value database value
     * @return balance constraint type
     */
    default LedgerBalanceConstraintType toLedgerBalanceConstraintType(String value) {
        return StringUtils.hasText(value) ? LedgerBalanceConstraintType.valueOf(value) : null;
    }

    /**
     * Database value convert to posting intent type.
     *
     * @param value database value
     * @return posting intent type
     */
    default LedgerPostingIntentType toLedgerPostingIntentType(String value) {
        return StringUtils.hasText(value) ? LedgerPostingIntentType.valueOf(value) : null;
    }

    /**
     * Database value convert to posting scope.
     *
     * @param value database value
     * @return posting scope
     */
    default LedgerPostingScope toLedgerPostingScope(String value) {
        return StringUtils.hasText(value) ? LedgerPostingScope.valueOf(value) : null;
    }

    /**
     * Database value convert to balance effect type.
     *
     * @param value database value
     * @return balance effect type
     */
    default LedgerBalanceEffectType toLedgerBalanceEffectType(String value) {
        return StringUtils.hasText(value) ? LedgerBalanceEffectType.valueOf(value) : null;
    }

    /**
     * Database value convert to ledger phase code.
     *
     * @param value database value
     * @return ledger phase code
     */
    default LedgerPhaseCode toLedgerPhaseCode(String value) {
        return StringUtils.hasText(value) ? LedgerPhaseCode.valueOf(value) : null;
    }
}

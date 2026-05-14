package com.wind.integration.funds.spec;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.fx.FxResult;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundsInstructionSpecContractTests {

    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 5, 12, 10, 30);

    @Test
    void testFundsInstructionShouldExposeBusinessIdentityAndAmountSnapshot() {
        Set<String> methods = methodNames(FundsInstructionSpec.class);

        assertTrue(methods.contains("getInstructionType"));
        assertTrue(methods.contains("getEventType"));
        assertTrue(methods.contains("getBusinessScene"));
        assertTrue(methods.contains("getBusinessSn"));
        assertFalse(methods.contains("getSourceObjectType"));
        assertFalse(methods.contains("getSourceObjectSn"));
        assertTrue(methods.contains("getAmount"));
        assertTrue(methods.contains("getOriginalAmount"));
        assertTrue(methods.contains("getExchangeRate"));
        assertFalse(methods.contains("getInstructionSn"));
    }

    @Test
    void testFundsInstructionShouldNotExposeInstructionSn() {
        Set<String> methods = methodNames(FundsInstructionSpec.class);

        assertFalse(methods.contains("getInstructionSn"));
        assertFalse(methods.contains("setInstructionSn"));
    }

    @Test
    void testFundsInstructionTypeShouldUseTransactionLayerAbilityNames() {
        Set<String> names = Arrays.stream(FundsInstructionType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of("DIRECT_TRANSACTION", "AUTHORIZATION_TRANSACTION", "BALANCE_CONTROL"), names);
        assertFalse(names.contains("TRANSFER"));
        assertFalse(names.contains("AUTHORIZATION"));
    }

    @Test
    void testFundsInstructionShouldUseBookAmountAsPrimaryAmount() {
        Money bookAmount = Money.immutable(1_100L, CurrencyIsoCode.USD);
        Money originalAmount = Money.immutable(1_000L, CurrencyIsoCode.EUR);

        FundsInstructionSpec instruction = validTopupBuilder()
                .amount(bookAmount)
                .originalAmount(originalAmount)
                .exchangeRate(new BigDecimal("1.10"))
                .build();

        assertEquals(bookAmount, instruction.getAmount());
        assertEquals(CurrencyIsoCode.USD, instruction.getAmount().getCurrency());
        assertEquals(originalAmount, instruction.getOriginalAmount());
    }

    @Test
    void testFundsInstructionShouldPreserveOriginalAmountCurrencyAndExchangeRate() {
        Money bookAmount = Money.immutable(1_100L, CurrencyIsoCode.USD);
        Money originalAmount = Money.immutable(1_000L, CurrencyIsoCode.EUR);
        BigDecimal exchangeRate = new BigDecimal("1.10");

        FundsInstructionSpec instruction = validTopupBuilder()
                .amount(bookAmount)
                .originalAmount(originalAmount)
                .exchangeRate(exchangeRate)
                .build();

        assertEquals(CurrencyIsoCode.USD, instruction.getAmount().getCurrency());
        assertEquals(CurrencyIsoCode.EUR, instruction.getOriginalAmount().getCurrency());
        assertEquals(0, exchangeRate.compareTo(instruction.getExchangeRate()));
    }

    @Test
    void testFxResultShouldExposeRateIdWithoutTypo() {
        Set<String> methods = methodNames(FxResult.class);

        assertTrue(methods.contains("getRateId"));
        assertFalse(methods.contains("getReteId"));
    }

    /**
     * 场景：资金 DSL 指令暴露操作参与者。
     * 输入：反射读取 FundsInstructionSpec#getOperator。
     * 输出：方法返回类型。
     * 预期：返回中性的 FundsOperationActorSpec，避免 wind-funds 依赖具体业务域 Operator。
     */
    @Test
    void testFundsInstructionOperatorShouldUseOperationActorSpec() throws NoSuchMethodException {
        Method operatorMethod = FundsInstructionSpec.class.getMethod("getOperator");

        assertEquals(FundsOperationActorSpec.class, operatorMethod.getReturnType());
    }

    @Test
    void testFundsInstructionReferenceShouldNotOwnBusinessIdentity() {
        Set<String> methods = methodNames(FundsInstructionReferenceSpec.class);

        assertTrue(methods.contains("getReferenceSn"));
        assertTrue(methods.contains("getReferenceBusinessSn"));
        assertFalse(methods.contains("getBusinessScene"));
        assertFalse(methods.contains("getBusinessSn"));
    }

    @Test
    void testImmutableFundsInstructionShouldBuildRealTopupInstruction() {
        Money amount = Money.immutable(1_000L, CurrencyIsoCode.USD);
        FundsInstructionSpec instruction = validTopupBuilder()
                .amount(amount)
                .originalAmount(null)
                .exchangeRate(null)
                .build();

        assertEquals(FundsInstructionType.DIRECT_TRANSACTION, instruction.getInstructionType());
        assertEquals(FundsTransactionEventType.TOPUP, instruction.getEventType());
        assertEquals(DefaultFundsTransactionType.TOPUP, instruction.getTransactionType());
        assertEquals("WALLET_TOPUP", instruction.getBusinessScene());
        assertEquals("TOPUP_202605120001", instruction.getBusinessSn());
        assertEquals(amount, instruction.getAmount());
        assertSame(amount, instruction.getOriginalAmount());
        assertEquals(0, BigDecimal.ONE.compareTo(instruction.getExchangeRate()));
        assertEquals(EVENT_TIME, instruction.getEventTime());
        assertTrue(instruction.getOperator().isSystem());
    }

    @Test
    void testImmutableFundsInstructionShouldRequireBusinessSceneAndBusinessSn() {
        BaseException missingBusinessScene = assertThrows(BaseException.class,
                () -> validTopupBuilder().businessScene(" ").build());
        BaseException missingBusinessSn = assertThrows(BaseException.class,
                () -> validTopupBuilder().businessSn(null).build());

        assertTrue(missingBusinessScene.getMessage().contains("businessScene"));
        assertTrue(missingBusinessSn.getMessage().contains("businessSn"));
    }

    @Test
    void testBusinessIdentityShouldRemainUpstreamActionIdentity() {
        FundsInstructionSpec instruction = validTopupBuilder()
                .businessScene("ORDER_PAYMENT")
                .businessSn("ORDER_202605120001")
                .build();

        assertEquals("ORDER_PAYMENT", instruction.getBusinessScene());
        assertEquals("ORDER_202605120001", instruction.getBusinessSn());
    }

    @Test
    void testImmutableFundsInstructionShouldDefensivelyCopyContextVariables() {
        Map<String, Object> contextVariables = new LinkedHashMap<>();
        contextVariables.put("channel", "wire");

        FundsInstructionSpec instruction = validTopupBuilder()
                .contextVariables(contextVariables)
                .build();
        contextVariables.put("channel", "card");

        assertEquals("wire", instruction.getContextVariables().get("channel"));
        assertThrows(UnsupportedOperationException.class,
                () -> instruction.getContextVariables().put("traceId", "trace_001"));
    }

    private static ImmutableFundsInstructionSpec.ImmutableFundsInstructionSpecBuilder validTopupBuilder() {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TOPUP)
                .amount(Money.immutable(1_000L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(1_000L, CurrencyIsoCode.USD))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("WALLET_TOPUP")
                .businessSn("TOPUP_202605120001")
                .eventTime(EVENT_TIME)
                .operator(systemActor())
                .contextVariables(Map.of("channel", "wire"));
    }

    private static FundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("wind-funds-test")
                .build();
    }

    private static Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}

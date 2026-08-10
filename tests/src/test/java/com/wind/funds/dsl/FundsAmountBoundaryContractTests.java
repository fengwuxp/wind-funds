package com.wind.funds.dsl;

import com.wind.jackson.WindJson;
import com.wind.funds.support.WindOperatorTestFixture;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金金额临界值 DSL 契约测试。
 */
class FundsAmountBoundaryContractTests {

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;
    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 5, 20, 11, 0);

    /**
     * 场景：资金指令承载所有资金变化的入口金额。
     * 预期：0 金额和负金额必须在 DSL 入口显式失败。
     * 红线：方向由交易类型、route 和 entry side 表达，不能靠非正金额混入主链路。
     */
    @Test
    void testFundsInstructionShouldRejectNonPositiveAmount() {
        assertThatThrownBy(() -> fundsInstruction(0L, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.amount must be positive");
        assertThatThrownBy(() -> fundsInstruction(-1L, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.amount must be positive");
    }

    /**
     * 场景：业务层已完成 FX 决策后，把原始金额和汇率带入资金指令。
     * 预期：原始金额必须为正，汇率必须为正。
     * 红线：缺汇率或非正汇率不能悄悄落成普通同币种资金事实。
     */
    @Test
    void testFundsInstructionShouldRejectInvalidOriginalAmountOrExchangeRate() {
        assertThatThrownBy(() -> fundsInstruction(100L, Money.immutable(0L, CURRENCY), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.originalAmount must be positive");
        assertThatThrownBy(() -> fundsInstruction(100L, Money.immutable(100L, CURRENCY), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.exchangeRate must be positive");
        assertThatThrownBy(() -> fundsInstruction(100L, Money.immutable(100L, CURRENCY), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.exchangeRate must be positive");
        assertThatThrownBy(() -> fundsInstruction(100L, Money.immutable(99L, CURRENCY), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.originalAmount must equal amount for same currency");
        assertThatThrownBy(() -> fundsInstruction(100L, Money.immutable(100L, CURRENCY), new BigDecimal("2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.exchangeRate must be 1 for same currency");
    }

    /**
     * 场景：route leg 表达资金、额度或预算的路径金额。
     * 预期：leg 金额必须为正，非终身账本周期必须带 periodId。
     * 红线：route leg 不能用 0、负金额或缺周期标识进入后续 posting。
     */
    @Test
    void testRouteLegShouldRejectNonPositiveAmountAndMissingPeriodId() {
        assertThatThrownBy(() -> routeLeg(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeLeg.amount must be positive");
        assertThatThrownBy(() -> routeLeg(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeLeg.amount must be positive");
        assertThatThrownBy(() -> routeLeg(100L, AccountBalancePeriodType.DAYS, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeLeg.periodId is required for non-lifetime period");
        assertThatThrownBy(() -> routeLegWithAmountFacts(100L, Money.immutable(99L, CURRENCY), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeLeg.originalAmount must equal amount for same currency");
        assertThatThrownBy(() -> routeLegWithAmountFacts(100L, Money.immutable(100L, CURRENCY),
                new BigDecimal("2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeLeg.exchangeRate must be 1 for same currency");
    }

    /**
     * 场景：JSON 框架读取资金金额对象。
     * 预期：WindJson 复用 Money 自身的 Jackson 构造注解完成反序列化。
     * 红线：资金 DSL 不重复实现 Money 的 JSON 字段解析。
     */
    @Test
    void testMoneyJsonShouldUseMoneyAnnotatedConstructor() {
        Money money = WindJson.parseObject("""
                { "amount": 1, "currency": "USD" }
                """, Money.class);

        assertThat(money).isEqualTo(Money.immutable(1L, CURRENCY));
    }

    /**
     * 场景：Money 只表达金额事实，不替资金 DSL 决定业务正负边界。
     * 预期：Money 可承载 0，资金指令主金额仍由 DSL builder 拒绝非正数。
     * 红线：删除手写 JSON parser 后不能放宽主资金金额的正数入口。
     */
    @Test
    void testMoneyJsonShouldLeavePositiveAmountRuleToDslBuilder() {
        Money money = WindJson.parseObject("""
                { "amount": 0, "currency": "USD" }
                """, Money.class);

        assertThat(money).isEqualTo(Money.immutable(0L, CURRENCY));
        assertThatThrownBy(() -> fundsInstruction(0L, money, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.amount must be positive");
    }

    private ImmutableFundsInstructionSpec fundsInstruction(long amount, BigDecimal exchangeRate) {
        return fundsInstruction(amount, Money.immutable(amount, CURRENCY), exchangeRate);
    }

    private ImmutableFundsInstructionSpec fundsInstruction(long amount,
                                                          Money originalAmount,
                                                          BigDecimal exchangeRate) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(amount, CURRENCY))
                .originalAmount(originalAmount)
                .exchangeRate(exchangeRate)
                .businessScene("AMOUNT_BOUNDARY_DSL")
                .businessSn("BIZ-AMOUNT-001")
                .eventTime(EVENT_TIME)
                .operator(WindOperatorTestFixture.system())
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteLegSpec routeLeg(long amount) {
        return routeLeg(amount, null, null);
    }

    private ImmutableRouteLegSpec routeLeg(String legId, long amount) {
        return routeLeg(legId, amount, null, null);
    }

    private ImmutableRouteLegSpec routeLeg(String legId, long amount, FundsSubjectType sourceSubjectType) {
        return routeLeg(legId, amount, CURRENCY, sourceSubjectType, null, null);
    }

    private ImmutableRouteLegSpec routeLeg(String legId, long amount, CurrencyIsoCode currency) {
        return routeLeg(legId, amount, currency, null, null);
    }

    private ImmutableRouteLegSpec routeLeg(long amount,
                                           AccountBalancePeriodType periodType,
                                           String periodId) {
        return routeLeg("LEG-AMOUNT-001", amount, periodType, periodId);
    }

    private ImmutableRouteLegSpec routeLegWithAmountFacts(long amount,
                                                          Money originalAmount,
                                                          BigDecimal exchangeRate) {
        return ImmutableRouteLegSpec.builder()
                .legId("LEG-AMOUNT-FX")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode("FA-SOURCE-001", RouteNodeRole.SOURCE))
                .targetNode(routeNode("FA-TARGET-001", RouteNodeRole.TARGET))
                .amount(Money.immutable(amount, CURRENCY))
                .originalAmount(originalAmount)
                .exchangeRate(exchangeRate)
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteLegSpec routeLeg(String legId,
                                           long amount,
                                           AccountBalancePeriodType periodType,
                                           String periodId) {
        return routeLeg(legId, amount, CURRENCY, periodType, periodId);
    }

    private ImmutableRouteLegSpec routeLeg(String legId,
                                           long amount,
                                           CurrencyIsoCode currency,
                                           AccountBalancePeriodType periodType,
                                           String periodId) {
        return routeLeg(legId, amount, currency, FundsSubjectType.FUNDING_ACCOUNT, periodType, periodId);
    }

    private ImmutableRouteLegSpec routeLeg(String legId,
                                           long amount,
                                           CurrencyIsoCode currency,
                                           FundsSubjectType sourceSubjectType,
                                           AccountBalancePeriodType periodType,
                                           String periodId) {
        return ImmutableRouteLegSpec.builder()
                .legId(legId)
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode("FA-SOURCE-001", sourceSubjectType, RouteNodeRole.SOURCE))
                .targetNode(routeNode("FA-TARGET-001", RouteNodeRole.TARGET))
                .amount(Money.immutable(amount, currency))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .periodType(periodType)
                .periodId(periodId)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
    }

    private RouteNodeSpec routeNode(String subjectId, RouteNodeRole nodeRole) {
        return routeNode(subjectId, FundsSubjectType.FUNDING_ACCOUNT, nodeRole);
    }

    private RouteNodeSpec routeNode(String subjectId, FundsSubjectType subjectType, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef(subjectId, subjectType))
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private SubjectRef subjectRef(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private SubjectRef subjectRef(String subjectId, FundsSubjectType subjectType) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(CURRENCY.name())
                .ledgerProfileCode("DEFAULT")
                .build();
    }

}

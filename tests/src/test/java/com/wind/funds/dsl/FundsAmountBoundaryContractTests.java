package com.wind.funds.dsl;

import com.alibaba.fastjson2.JSON;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.model.route.ImmutableRouteLegSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
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
    }

    /**
     * 场景：支付工具或预算组命中的资金来源分配进入 route snapshot。
     * 预期：资金来源分配金额必须为正。
     * 红线：资金来源决策不能用非正金额伪装成候选或跳过累计闭合校验。
     */
    @Test
    void testFundingAllocationShouldRejectNonPositiveAmount() {
        assertThatThrownBy(() -> fundingAllocation(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation amount must be positive");
        assertThatThrownBy(() -> fundingAllocation(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation amount must be positive");
    }

    /**
     * 场景：支付工具或共享卡解析出真实资金账户资金来源，同时 route leg 表达实际资金路径。
     * 预期：真实资金账户 funding allocation 合计必须等于正向资金消耗 leg 合计。
     * 红线：多 leg 或多资金来源金额不闭合不得进入 ResolvedRoute 或 RouteSnapshot。
     */
    @Test
    void testRouteShouldRejectUnclosedFundingAccountAllocationAmount() {
        RouteLegSpecMismatch mismatch = routeLegSpecMismatch(100L, 80L);

        assertThatThrownBy(() -> resolvedRoute(mismatch.leg(), mismatch.routingDecision()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
        assertThatThrownBy(() -> routeSnapshot(mismatch.leg(), mismatch.routingDecision()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
    }

    /**
     * 场景：VCC 共享卡或授信支付解析出信用账户资金责任，同时 route leg 表达信用账户额度消耗。
     * 预期：信用账户 funding allocation 合计必须等于正向额度消耗 leg 合计。
     * 红线：信用账户作为核心账务主体，不能绕过 route amount 与资金责任分配闭合校验。
     */
    @Test
    void testRouteShouldRejectUnclosedCreditAccountAllocationAmount() {
        ImmutableRouteLegSpec creditLeg = routeLeg("LEG-CREDIT-001", 100L, FundsSubjectType.CREDIT_ACCOUNT);
        RoutingDecisionSpec routingDecision = routingDecision(List.of(fundingAllocation("ALLOC-CA-001",
                subjectRef("CA-CREDIT-001", FundsSubjectType.CREDIT_ACCOUNT),
                80L,
                10,
                "CREDIT_ACCOUNT")));

        assertThatThrownBy(() -> resolvedRoute(creditLeg, routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
        assertThatThrownBy(() -> routeSnapshot(creditLeg, routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
    }

    /**
     * 场景：真实资金账户 route leg 与 funding allocation 金额数值相同但币种不同。
     * 预期：ResolvedRoute 和 RouteSnapshot 构造期必须拒绝错币种闭合。
     * 红线：不能只按最小单位数值闭合资金来源，跨币种 route 不得进入后续 posting。
     */
    @Test
    void testRouteShouldRejectFundingAccountAllocationCurrencyMismatch() {
        ImmutableRouteLegSpec cnyLeg = routeLeg("LEG-AMOUNT-CNY", 100L, CurrencyIsoCode.CNY);
        RoutingDecisionSpec routingDecision = routingDecision(List.of(fundingAllocation("ALLOC-FA-USD",
                subjectRef("FA-FUNDING-USD"),
                100L,
                CurrencyIsoCode.USD,
                10,
                "REAL_FUNDING_ACCOUNT")));

        assertThatThrownBy(() -> resolvedRoute(cnyLeg, routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
        assertThatThrownBy(() -> routeSnapshot(cnyLeg, routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
    }

    /**
     * 场景：共享卡 + 预算组 + 资金账户模型同时带预算控制和真实资金来源。
     * 预期：预算组 allocation 不表达真实资金沉淀，不参与真实资金合计闭合；资金账户必须闭合。
     * 红线：不能把预算组金额当作现金来源来掩盖真实资金账户不闭合。
     */
    @Test
    void testBudgetGroupAllocationShouldNotReplaceFundingAccountClosure() {
        assertThatThrownBy(() -> resolvedRoute(routeLeg(100L),
                routingDecision(List.of(fundingAllocation("ALLOC-BUDGET-001",
                                subjectRef("BG-001", FundsSubjectType.BUDGET_GROUP),
                                100L,
                                10,
                                "BUDGET_CONTROL"),
                        fundingAllocation("ALLOC-FA-001",
                                subjectRef("FA-FUNDING-001", FundsSubjectType.FUNDING_ACCOUNT),
                                80L,
                                20,
                                "REAL_FUNDING_ACCOUNT")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");

        ImmutableResolvedRouteSpec validRoute = resolvedRoute(routeLeg(100L),
                routingDecision(List.of(fundingAllocation("ALLOC-BUDGET-002",
                                subjectRef("BG-002", FundsSubjectType.BUDGET_GROUP),
                                100L,
                                10,
                                "BUDGET_CONTROL"),
                        fundingAllocation("ALLOC-FA-002",
                                subjectRef("FA-FUNDING-002", FundsSubjectType.FUNDING_ACCOUNT),
                                100L,
                                20,
                                "REAL_FUNDING_ACCOUNT"))));
        assertThat(validRoute.getLegs()).singleElement()
                .satisfies(leg -> assertThat(leg.getAmount()).isEqualTo(Money.immutable(100L, CURRENCY)));
        assertThat(validRoute.getRoutingDecision()).satisfies(decision -> {
            assertThat(decision.getFundingAllocations())
                    .extracting(allocation -> allocation.getSubjectRef().getSubjectType())
                    .containsExactly(FundsSubjectType.BUDGET_GROUP, FundsSubjectType.FUNDING_ACCOUNT);
            assertThat(decision.getFundingAllocations())
                    .extracting(FundingAllocationDecisionSpec::getAmount)
                    .containsExactly(Money.immutable(100L, CURRENCY), Money.immutable(100L, CURRENCY));
        });
    }

    /**
     * 场景：多个真实资金账户 route leg 在同币种下累计超过系统可表达上限。
     * 预期：累计过程必须显式失败，不能发生 long 溢出后继续比较闭合结果。
     * 红线：批量路由、清结算或归档重放汇总不得用溢出后的金额入账或发布快照。
     */
    @Test
    void testRouteShouldRejectFundingAccountRouteAmountOverflow() {
        List<RouteLegSpec> legs = List.of(routeLeg("LEG-AMOUNT-001", Long.MAX_VALUE),
                routeLeg("LEG-AMOUNT-002", 1L));
        RoutingDecisionSpec routingDecision = routingDecision(List.of(fundingAllocation("ALLOC-FA-001",
                subjectRef("FA-FUNDING-001"),
                Long.MAX_VALUE,
                10,
                "REAL_FUNDING_ACCOUNT")));

        assertThatThrownBy(() -> resolvedRoute(legs, routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account route amount sum overflow");
        assertThatThrownBy(() -> routeSnapshot(legs, routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account route amount sum overflow");
    }

    /**
     * 场景：多个真实资金账户 funding allocation 在同币种下累计超过系统可表达上限。
     * 预期：累计过程必须显式失败，不能发生 long 溢出后继续通过闭合校验。
     * 红线：共享卡或多资金来源分配不得用溢出后的金额掩盖真实资金来源超上限。
     */
    @Test
    void testRouteShouldRejectFundingAllocationAmountOverflow() {
        RoutingDecisionSpec routingDecision = routingDecision(List.of(fundingAllocation("ALLOC-FA-001",
                        subjectRef("FA-FUNDING-001"),
                        Long.MAX_VALUE,
                        10,
                        "REAL_FUNDING_ACCOUNT"),
                fundingAllocation("ALLOC-FA-002",
                        subjectRef("FA-FUNDING-002"),
                        1L,
                        20,
                        "REAL_FUNDING_ACCOUNT")));

        assertThatThrownBy(() -> resolvedRoute(routeLeg(100L), routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount sum overflow");
        assertThatThrownBy(() -> routeSnapshot(routeLeg(100L), routingDecision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount sum overflow");
    }

    /**
     * 场景：JSON 框架读取资金金额对象。
     * 预期：Fastjson 复用 Money 自身的 Jackson 构造注解完成反序列化。
     * 红线：资金 DSL 不重复实现 Money 的 JSON 字段解析。
     */
    @Test
    void testMoneyJsonShouldUseMoneyAnnotatedConstructor() {
        Money money = JSON.parseObject("""
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
        Money money = JSON.parseObject("""
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
                .operator(systemOperator())
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

    private ImmutableFundingAllocationDecisionSpec fundingAllocation(long amount) {
        return fundingAllocation("ALLOC-AMOUNT-001", subjectRef("FA-FUNDING-001"), amount, 10, "AMOUNT_BOUNDARY");
    }

    private ImmutableFundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                                     SubjectRef subjectRef,
                                                                     long amount,
                                                                     Integer priority,
                                                                     String reason) {
        return fundingAllocation(allocationId, subjectRef, amount, CURRENCY, priority, reason);
    }

    private ImmutableFundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                                     SubjectRef subjectRef,
                                                                     long amount,
                                                                     CurrencyIsoCode currency,
                                                                     Integer priority,
                                                                     String reason) {
        return ImmutableFundingAllocationDecisionSpec.builder()
                .allocationId(allocationId)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .amount(Money.immutable(amount, currency))
                .priority(priority)
                .reason(reason)
                .build();
    }

    private ImmutableResolvedRouteSpec resolvedRoute(ImmutableRouteLegSpec leg, RoutingDecisionSpec routingDecision) {
        return resolvedRoute(List.of(leg), routingDecision);
    }

    private ImmutableResolvedRouteSpec resolvedRoute(List<RouteLegSpec> legs,
                                                     RoutingDecisionSpec routingDecision) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(1L)
                .routeCode("AMOUNT_BOUNDARY_ROUTE")
                .routeVersion("v1")
                .businessScene("AMOUNT_BOUNDARY_DSL")
                .businessSn("BIZ-AMOUNT-001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of())
                .legs(legs)
                .routingDecision(routingDecision)
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteSnapshotSpec routeSnapshot(ImmutableRouteLegSpec leg,
                                                     RoutingDecisionSpec routingDecision) {
        return routeSnapshot(List.of(leg), routingDecision);
    }

    private ImmutableRouteSnapshotSpec routeSnapshot(List<RouteLegSpec> legs,
                                                     RoutingDecisionSpec routingDecision) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("RS-AMOUNT-001")
                .snapshotSchemaVersion("v1")
                .routeCode("AMOUNT_BOUNDARY_ROUTE")
                .routeVersion("v1")
                .businessScene("AMOUNT_BOUNDARY_DSL")
                .businessSn("BIZ-AMOUNT-001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of())
                .legs(legs)
                .routingDecision(routingDecision)
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private RoutingDecisionSpec routingDecision(List<FundingAllocationDecisionSpec> fundingAllocations) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode("AMOUNT_BOUNDARY_POLICY")
                .matchedRules(List.of("AMOUNT_CLOSURE"))
                .fundingAllocations(fundingAllocations)
                .decisionReason("AMOUNT_BOUNDARY")
                .contextVariables(Map.of())
                .build();
    }

    private RouteLegSpecMismatch routeLegSpecMismatch(long routeLegAmount, long fundingAllocationAmount) {
        ImmutableRouteLegSpec leg = routeLeg(routeLegAmount);
        RoutingDecisionSpec routingDecision = routingDecision(List.of(fundingAllocation("ALLOC-MISMATCH-001",
                subjectRef("FA-FUNDING-001"),
                fundingAllocationAmount,
                10,
                "REAL_FUNDING_ACCOUNT")));
        return new RouteLegSpecMismatch(leg, routingDecision);
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

    private FundsOperationActorSpec systemOperator() {
        return new TestFundsOperationActorSpec(0L, "SYSTEM", "wind-funds-tests");
    }

    private record TestFundsOperationActorSpec(Long operatorId,
                                               String operatorType,
                                               String appName) implements FundsOperationActorSpec {

        @Override
        public Long getOperatorId() {
            return operatorId;
        }

        @Override
        public String getOperatorType() {
            return operatorType;
        }

        @Override
        public String getOperatorName() {
            return "System";
        }

        @Override
        public String getAppName() {
            return appName;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private record RouteLegSpecMismatch(ImmutableRouteLegSpec leg,
                                        RoutingDecisionSpec routingDecision) {
    }
}

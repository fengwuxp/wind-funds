package com.capte.funds.dsl;

import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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

    private ImmutableRouteLegSpec routeLeg(long amount,
                                           AccountBalancePeriodType periodType,
                                           String periodId) {
        return ImmutableRouteLegSpec.builder()
                .legId("LEG-AMOUNT-001")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode("FA-SOURCE-001", RouteNodeRole.SOURCE))
                .targetNode(routeNode("FA-TARGET-001", RouteNodeRole.TARGET))
                .amount(Money.immutable(amount, CURRENCY))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .periodType(periodType)
                .periodId(periodId)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableFundingAllocationDecisionSpec fundingAllocation(long amount) {
        return ImmutableFundingAllocationDecisionSpec.builder()
                .allocationId("ALLOC-AMOUNT-001")
                .subjectRef(subjectRef("FA-FUNDING-001"))
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .amount(Money.immutable(amount, CURRENCY))
                .priority(10)
                .reason("AMOUNT_BOUNDARY")
                .build();
    }

    private RouteNodeSpec routeNode(String subjectId, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef(subjectId))
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private SubjectRef subjectRef(String subjectId) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
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
}

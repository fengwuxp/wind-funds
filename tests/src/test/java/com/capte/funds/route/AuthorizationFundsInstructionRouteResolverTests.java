package com.capte.funds.route;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationFundsInstructionRouteResolverTests {

    private FundsAuthorizationInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.authorizationInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testResolveAuthorizeDeclinedShouldHaveNoLegs() {
        FundsInstructionSpec instruction = converter.convertToAuthorizeInstruction(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setAmount(FundsRouteTestSupport.amount(600L))
                        .setApproved(Boolean.FALSE)
                        .setBusinessScene("AUTH")
                        .setBusinessSn("AUTH_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_STANDARD");
        assertThat(route.getLegs()).isEmpty();
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER);
    }

    /**
     * 场景：单主体授权结算。
     * 输入：信用账户原授权存在，本次结算 400。
     * 输出：从 AUTHORIZATION 消费到平台结算账户。
     * 预期：source AUTHORIZATION 携带 MUST_NOT_BE_NEGATIVE 约束。
     * 红线：不能把不存在或不足的授权占用继续当作可结算余额。
     */
    @Test
    void testResolveSettleShouldMoveAuthorizationToServiceRevenue() {
        FundsAccountId accountId = FundsRouteTestSupport.creditAccount("credit_001");
        FundsInstructionSpec instruction = converter.convertToSettleInstruction(
                new FundsAuthorizationTransactionSettleRequest()
                        .setAccountId(accountId)
                        .setAmount(FundsRouteTestSupport.amount(400L))
                        .setBusinessScene("SETTLE")
                        .setBusinessSn("SETTLE_0001")
                        .setAuthorizationTransactionSn("AUTH_TX_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AUTHORIZATION);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.SETTLEMENT);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.CONSUME);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AUTHORIZATION);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
    }

    /**
     * 场景：共享卡授权成功。
     * 输入：信用账户、预算组和真实资金账户共同参与授权。
     * 输出：三个主体都从 AVAILABLE 占用到 AUTHORIZATION。
     * 预期：每个 source AVAILABLE 携带 MUST_NOT_BE_NEGATIVE 约束。
     * 红线：负 AVAILABLE 不能无策略继续授权。
     */
    @Test
    void testResolveAuthorizeShouldHoldSharedCardLinkedSubjects() {
        FundsInstructionSpec instruction = sharedCardInstruction(FundsTransactionEventType.AUTHORIZE);

        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_STANDARD");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER,
                        RouteParticipantRole.BUDGET_CONTROLLER,
                        RouteParticipantRole.REAL_FUNDING_SOURCE);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getSourceNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AVAILABLE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AUTHORIZATION);
        assertMustNotBeNegative(route.getLegs().get(0), FundsRouteTestSupport.creditAccount("credit_001"),
                LedgerSubjectCode.AVAILABLE);
        assertMustNotBeNegative(route.getLegs().get(1), FundsRouteTestSupport.budgetGroup("budget_001"),
                LedgerSubjectCode.AVAILABLE);
        assertMustNotBeNegative(route.getLegs().get(2), FundsRouteTestSupport.fundingAccount("funding_001"),
                LedgerSubjectCode.AVAILABLE);
        assertThat(route.getPlatformAccounts()).isNotNull();
    }

    /**
     * 场景：共享卡授权结算。
     * 输入：信用账户、预算组和真实资金账户都有授权占用。
     * 输出：三个主体都关闭或减少 AUTHORIZATION。
     * 预期：每个 source AUTHORIZATION 携带 MUST_NOT_BE_NEGATIVE 约束。
     * 红线：授权余额不足不能继续结算消费。
     */
    @Test
    void testResolveSettleShouldConsumeSharedCardControlAndFundingSubjects() {
        FundsInstructionSpec instruction = sharedCardInstruction(FundsTransactionEventType.SETTLE);

        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_STANDARD");
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getSourceNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AUTHORIZATION);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsExactly(LedgerSubjectCode.LIMIT, LedgerSubjectCode.LIMIT, LedgerSubjectCode.SETTLEMENT);
        assertMustNotBeNegative(route.getLegs().get(0), FundsRouteTestSupport.creditAccount("credit_001"),
                LedgerSubjectCode.AUTHORIZATION);
        assertMustNotBeNegative(route.getLegs().get(1), FundsRouteTestSupport.budgetGroup("budget_001"),
                LedgerSubjectCode.AUTHORIZATION);
        assertMustNotBeNegative(route.getLegs().get(2), FundsRouteTestSupport.fundingAccount("funding_001"),
                LedgerSubjectCode.AUTHORIZATION);
    }

    private FundsInstructionSpec sharedCardInstruction(FundsTransactionEventType eventType) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(FundsInstructionContextKeys.ACCOUNT_ID, FundsRouteTestSupport.creditAccount("credit_001"));
        context.put(FundsInstructionContextKeys.APPROVED, Boolean.TRUE);
        context.put(FundsInstructionContextKeys.LINKED_BUDGET_GROUP_ID,
                FundsRouteTestSupport.budgetGroup("budget_001"));
        context.put(FundsInstructionContextKeys.LINKED_FUNDING_ACCOUNT_ID,
                FundsRouteTestSupport.fundingAccount("funding_001"));
        WindOperator operator = WindOperator.system();
        String businessSn = eventType.name() + "_0001";
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(eventType)
                .transactionType(eventType == FundsTransactionEventType.AUTH_REFUND
                        ? DefaultFundsTransactionType.REFUND
                        : DefaultFundsTransactionType.PAY)
                .amount(FundsRouteTestSupport.amount(600L))
                .originalAmount(FundsRouteTestSupport.amount(600L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("CARD_AUTH")
                .businessSn(businessSn)
                .eventTime(LocalDateTime.now())
                .description("shared card")
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(operator.getOperatorId())
                        .operatorType(operator.getOperatorType().name())
                        .operatorName(operator.getOperatorName())
                        .appName(operator.getAppName())
                        .contextVariables(operator.getContextVariables())
                        .build())
                .contextVariables(Map.copyOf(context))
                .build();
    }

    private static void assertMustNotBeNegative(RouteLegSpec leg,
                                                FundsAccountId accountId,
                                                LedgerSubjectCode ledgerSubjectCode) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }
}

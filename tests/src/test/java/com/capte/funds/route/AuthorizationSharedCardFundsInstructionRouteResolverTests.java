package com.capte.funds.route;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationSharedCardFundsInstructionRouteResolverTests
        extends AuthorizationFundsInstructionRouteResolverTestSupport {

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
     * 预期：所有授权占用都进入平台 SETTLEMENT，以关闭 AUTHORIZATION 占用。
     * 红线：授权结算不得把 LIMIT 当 source 或 target。
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
                .containsOnly(LedgerSubjectCode.SETTLEMENT);
        assertNoLimitNodes(route);
        assertMustNotBeNegative(route.getLegs().get(0), FundsRouteTestSupport.creditAccount("credit_001"),
                LedgerSubjectCode.AUTHORIZATION);
        assertMustNotBeNegative(route.getLegs().get(1), FundsRouteTestSupport.budgetGroup("budget_001"),
                LedgerSubjectCode.AUTHORIZATION);
        assertMustNotBeNegative(route.getLegs().get(2), FundsRouteTestSupport.fundingAccount("funding_001"),
                LedgerSubjectCode.AUTHORIZATION);
    }
}

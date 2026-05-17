package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationFundsInstructionRouteResolverTests extends AuthorizationFundsInstructionRouteResolverTestSupport {

    @Test
    void testResolveAuthorizeDeclinedShouldHaveNoLegs() {
        FundsInstructionSpec instruction = converter.convertToAuthorizeInstruction(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setTransactionAmount(FundsRouteTestSupport.transactionAmount(600L))
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
                        .setTransactionAmount(FundsRouteTestSupport.transactionAmount(400L))
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
}

package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsAuthorizationTransactionCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

    /**
     * 场景：外部授权问询通过，信用账户需要占用可用额度。
     * 输入：CREDIT_ACCOUNT、授权金额 600、approved=true。
     * 输出：AUTHORIZATION_TRANSACTION / AUTHORIZE 指令和 HOLD route。
     * 预期：AVAILABLE 转入 AUTHORIZATION，并对 AVAILABLE 加上不得为负的约束。
     */
    @Test
    void testAuthorizeShouldBuildHoldRouteWhenApproved() {
        FundsAccountId credit = creditAccount("credit_001");

        service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(credit)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                .setApproved(Boolean.TRUE)
                .setBusinessScene("CARD_AUTH")
                .setBusinessSn("AUTH_00000001")
                .setDescription("auth"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
        assertThat(route().getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name());
        assertLeg(leg, RouteLegType.HOLD, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION, LedgerBalanceEffectType.HOLD, LedgerPhaseCode.AUTHORIZATION);
        assertThat(leg.getConstraintOverrides())
                .containsEntry(constraintKey(credit, LedgerSubjectCode.AVAILABLE),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    /**
     * 场景：外部授权问询被拒绝。
     * 输入：CREDIT_ACCOUNT、授权金额 600、approved=false 和拒绝原因。
     * 输出：AUTHORIZE 生命周期指令、拒绝原因上下文和授权主体参与方。
     * 预期：拒绝授权不生成账本 route leg，不改变余额。
     */
    @Test
    void testAuthorizeShouldNotBuildLedgerLegWhenDeclined() {
        service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(creditAccount("credit_001"))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                .setApproved(Boolean.FALSE)
                .setDeclineReason("insufficient_funds")
                .setBusinessScene("CARD_AUTH")
                .setBusinessSn("AUTH_00000002")
                .setDescription("declined"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
        assertThat(instruction.getContextVariables()).containsEntry("declineReason", "insufficient_funds");
        assertThat(route().getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name());
        assertThat(route().getLegs()).isEmpty();
    }
}

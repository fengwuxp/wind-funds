package com.capte.funds.route;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AuthorizationFundsInstructionRouteResolverTestSupport {

    protected FundsAuthorizationInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.authorizationInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    protected static void assertMustNotBeNegative(RouteLegSpec leg,
                                                  FundsAccountId accountId,
                                                  LedgerSubjectCode ledgerSubjectCode) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    protected static void assertNoLimitNodes(ResolvedRouteSpec route) {
        assertThat(route.getLegs())
                .allSatisfy(leg -> assertThat(LedgerSubjectCode.LIMIT)
                        .isNotIn(leg.getSourceNode().getLedgerSubjectCode(),
                                leg.getTargetNode().getLedgerSubjectCode()));
    }

    protected FundsInstructionSpec sharedCardInstruction(FundsTransactionEventType eventType) {
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
}

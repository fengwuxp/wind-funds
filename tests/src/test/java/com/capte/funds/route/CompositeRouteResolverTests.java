package com.capte.funds.route;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeRouteResolverTests {

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testResolveShouldDispatchToSingleMatchedResolver() {
        TransferFundsInstructionRouteResolver transferResolver = FundsRouteTestSupport.transferRouteResolver(
                FundsRouteTestSupport.noFeeProvider());
        CompositeRouteResolver resolver = new CompositeRouteResolver(List.of(transferResolver));

        ResolvedRouteSpec route = resolver.resolve(ImmutableFundsInstructionSpec.builder()
                .tenantId(FundsRouteTestSupport.TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(FundsRouteTestSupport.amount(100L))
                .originalAmount(FundsRouteTestSupport.amount(100L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("TRANSFER")
                .businessSn("TRANSFER_0001")
                .eventTime(LocalDateTime.now())
                .operator(systemActor())
                .contextVariables(Map.of(
                        FundsInstructionContextKeys.PAYER_ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"),
                        FundsInstructionContextKeys.PAYEE_ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_002")
                ))
                .build());

        assertThat(route.getRouteCode()).isEqualTo("INTERNAL_TRANSFER_STANDARD");
    }

    @Test
    void testResolveShouldFailWhenResolverMatchedMoreThanOnce() {
        RouteResolver routeResolver1 = new AlwaysSupportRouteResolver();
        RouteResolver routeResolver2 = new AlwaysSupportRouteResolver();
        CompositeRouteResolver resolver = new CompositeRouteResolver(List.of(routeResolver1, routeResolver2));

        assertThatThrownBy(() -> resolver.resolve(ImmutableFundsInstructionSpec.builder()
                .tenantId(FundsRouteTestSupport.TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(FundsRouteTestSupport.amount(100L))
                .originalAmount(FundsRouteTestSupport.amount(100L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("TRANSFER")
                .businessSn("TRANSFER_0002")
                .eventTime(LocalDateTime.now())
                .operator(systemActor())
                .contextVariables(Map.of())
                .build()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("RouteResolver 命中不唯一");
    }

    private static ImmutableFundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }

    private static final class AlwaysSupportRouteResolver implements RouteResolver {

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            return ImmutableResolvedRouteSpec.builder()
                    .tenantId(instruction.getTenantId())
                    .routeCode("ALWAYS")
                    .routeVersion("test")
                    .businessScene(instruction.getBusinessScene())
                    .businessSn(instruction.getBusinessSn())
                    .instructionType(instruction.getInstructionType())
                    .eventType(instruction.getEventType())
                    .transactionType(instruction.getTransactionType())
                    .participants(List.of())
                    .legs(List.of())
                    .resolvedAt(instruction.getEventTime())
                    .contextVariables(Map.of())
                    .build();
        }
    }
}

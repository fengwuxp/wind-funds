package com.capte.funds.route;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
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
        TransferFundsInstructionRouteResolver transferResolver = FundsRouteTestSupport.transferRouteResolver();
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

    @Test
    void testResolveShouldFailWhenNoResolverMatched() {
        CompositeRouteResolver resolver = new CompositeRouteResolver(List.of(new NeverSupportRouteResolver()));

        assertThatThrownBy(() -> resolver.resolve(instruction("TRANSFER_0003")))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到匹配的 RouteResolver");
    }

    @Test
    void testResolveShouldExcludeSelfResolver() {
        List<RouteResolver> delegates = new ArrayList<>();
        CompositeRouteResolver resolver = new CompositeRouteResolver(delegates);
        delegates.add(resolver);

        assertThatThrownBy(() -> resolver.resolve(instruction("TRANSFER_0004")))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到匹配的 RouteResolver");
    }

    @Test
    void testResolveShouldEvaluateDelegatesByOrderBeforeSupports() {
        List<String> supportCalls = new ArrayList<>();
        RouteResolver lowPriority = new RecordingRouteResolver("low", 100, false, supportCalls);
        RouteResolver highPriority = new RecordingRouteResolver("high", -100, false, supportCalls);
        RouteResolver selected = new RecordingRouteResolver("selected", 0, true, supportCalls);
        CompositeRouteResolver resolver = new CompositeRouteResolver(List.of(lowPriority, selected, highPriority));

        ResolvedRouteSpec route = resolver.resolve(instruction("TRANSFER_0005"));

        assertThat(route.getRouteCode()).isEqualTo("selected");
        assertThat(supportCalls).containsExactly("high", "selected", "low");
    }

    @Test
    void testResolveShouldUseReplayResolverWhenReplayAndNormalResolversAreMutuallyExclusive() {
        RouteResolver normalResolver = new NonReplayDirectRouteResolver();
        RouteResolver replayResolver = new ReplayOnlyRouteResolver();
        CompositeRouteResolver resolver = new CompositeRouteResolver(List.of(normalResolver, replayResolver));

        ResolvedRouteSpec route = resolver.resolve(replayInstruction("REFUND_0001"));

        assertThat(route.getRouteCode()).isEqualTo("REPLAY");
    }

    private static FundsInstructionSpec instruction(String businessSn) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(FundsRouteTestSupport.TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(FundsRouteTestSupport.amount(100L))
                .originalAmount(FundsRouteTestSupport.amount(100L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("TRANSFER")
                .businessSn(businessSn)
                .eventTime(LocalDateTime.now())
                .operator(systemActor())
                .contextVariables(Map.of())
                .build();
    }

    private static FundsInstructionSpec replayInstruction(String businessSn) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(FundsRouteTestSupport.TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(FundsRouteTestSupport.amount(100L))
                .originalAmount(FundsRouteTestSupport.amount(100L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("REFUND")
                .businessSn(businessSn)
                .eventTime(LocalDateTime.now())
                .operator(systemActor())
                .reference(ImmutableFundsInstructionReferenceSpec.builder()
                        .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                        .referenceSn("PAY_0001")
                        .build())
                .contextVariables(Map.of())
                .build();
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
            return route("ALWAYS", instruction);
        }
    }

    private static final class NeverSupportRouteResolver implements RouteResolver {

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            return false;
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            throw new AssertionError("unsupported resolver should not resolve");
        }
    }

    private static final class RecordingRouteResolver implements RouteResolver, Ordered {

        private final String routeCode;

        private final int order;

        private final boolean supports;

        private final List<String> supportCalls;

        private RecordingRouteResolver(String routeCode, int order, boolean supports, List<String> supportCalls) {
            this.routeCode = routeCode;
            this.order = order;
            this.supports = supports;
            this.supportCalls = supportCalls;
        }

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            supportCalls.add(routeCode);
            return supports;
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            return route(routeCode, instruction);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    private static final class NonReplayDirectRouteResolver implements RouteResolver {

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            return instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                    && !RouteReplaySupport.isReplayInstruction(instruction);
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            return route("NORMAL", instruction);
        }
    }

    private static final class ReplayOnlyRouteResolver implements RouteResolver {

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            return RouteReplaySupport.isReplayInstruction(instruction);
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            return route("REPLAY", instruction);
        }
    }

    private static ResolvedRouteSpec route(String routeCode, FundsInstructionSpec instruction) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(instruction.getTenantId())
                .routeCode(routeCode)
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

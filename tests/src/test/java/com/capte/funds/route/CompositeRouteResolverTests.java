package com.capte.funds.route;

import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 组合路由解析器选择边界测试。
 */
class CompositeRouteResolverTests {

    /**
     * 场景：组合解析器被上层询问是否支持某个资金指令。
     * 输入：包含一个会在 `supports` 或 `resolve` 被调用时计数的委托解析器。
     * 输出：组合解析器直接声明支持。
     * 预期：不触发任何委托解析器的 `supports` 或 `resolve`。
     * 红线：RouteResolver#supports 只能做轻量判定，不得执行委托选择、路由解析或任何可能写事实的副作用。
     */
    @Test
    void testSupportsShouldNotInvokeDelegateResolver() {
        RecordingRouteResolver delegate = new RecordingRouteResolver();
        CompositeRouteResolver resolver = new CompositeRouteResolver(List.of(delegate));

        assertThat(resolver.supports(directInstruction())).isTrue();
        assertThat(delegate.supportsCalls).isZero();
        assertThat(delegate.resolveCalls).isZero();
    }

    private FundsInstructionSpec directInstruction() {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                .businessScene("PAY")
                .businessSn("ROUTE_SUPPORTS_NO_SIDE_EFFECT")
                .eventTime(LocalDateTime.of(2026, 5, 19, 0, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(1L)
                        .operatorType("SYSTEM")
                        .appName("wind-funds-tests")
                        .build())
                .contextVariables(Map.of())
                .build();
    }

    private static final class RecordingRouteResolver implements RouteResolver {

        private int supportsCalls;

        private int resolveCalls;

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            supportsCalls++;
            return true;
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            resolveCalls++;
            throw new UnsupportedOperationException("resolve should not be called by supports");
        }
    }
}

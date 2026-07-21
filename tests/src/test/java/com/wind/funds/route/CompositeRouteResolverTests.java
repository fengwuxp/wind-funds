package com.wind.funds.route;

import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.service.PlatformFundingAccountService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        CompositeRouteResolver resolver = resolver(List.of(delegate));

        assertThat(resolver.supports(directInstruction())).isTrue();
        assertThat(delegate.supportsCalls).isZero();
        assertThat(delegate.resolveCalls).isZero();
    }

    /**
     * 场景：组合解析器执行路由解析，但没有任何委托解析器支持该指令。
     * 输入：一个 `supports=false` 的委托解析器。
     * 输出：路由解析失败。
     * 预期：只允许执行候选判定，不得调用委托 `resolve`。
     * 红线：未命中 RouteResolver 时不得尝试解析或写事实。
     */
    @Test
    void testResolveWithoutCandidateShouldNotInvokeDelegateResolve() {
        RecordingRouteResolver delegate = new RecordingRouteResolver(false);
        CompositeRouteResolver resolver = resolver(List.of(delegate));

        assertThatThrownBy(() -> resolver.resolve(directInstruction()))
                .hasMessageContaining("未找到匹配的 RouteResolver");
        assertThat(delegate.supportsCalls).isEqualTo(1);
        assertThat(delegate.resolveCalls).isZero();
    }

    /**
     * 场景：组合解析器执行路由解析，但多个委托解析器同时支持该指令。
     * 输入：两个 `supports=true` 的委托解析器。
     * 输出：路由解析失败。
     * 预期：只允许执行候选判定，不得选择任一委托继续 `resolve`。
     * 红线：RouteResolver 命中不唯一时必须整体失败，不能随机选路。
     */
    @Test
    void testResolveWithMultipleCandidatesShouldNotInvokeDelegateResolve() {
        RecordingRouteResolver first = new RecordingRouteResolver(true);
        RecordingRouteResolver second = new RecordingRouteResolver(true);
        CompositeRouteResolver resolver = resolver(List.of(first, second));

        assertThatThrownBy(() -> resolver.resolve(directInstruction()))
                .hasMessageContaining("RouteResolver 命中不唯一");
        assertThat(first.supportsCalls).isEqualTo(1);
        assertThat(second.supportsCalls).isEqualTo(1);
        assertThat(first.resolveCalls).isZero();
        assertThat(second.resolveCalls).isZero();
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
                        .operatorId("1")
                        .operatorType("SYSTEM")
                        .appName("wind-funds-tests")
                        .build())
                .contextVariables(Map.of())
                .build();
    }

    private CompositeRouteResolver resolver(List<RouteResolver> delegates) {
        FundsAccountQueryService accountQueryService = unexpectedAccountQueryService();
        return new CompositeRouteResolver(
                delegates,
                new RefundRouteAdmission(accountQueryService),
                new RouteFeeChargeAppender(
                        new RouteParticipantFactory(),
                        new PlatformAccountRouteSupport(unexpectedPlatformAccountService()),
                        accountQueryService));
    }

    private FundsAccountQueryService unexpectedAccountQueryService() {
        return new FundsAccountQueryService() {

            @Override
            public FundsAccount getAccount(FundsAccountId accountId) {
                throw new AssertionError("route post-processing must not query accounts in resolver selection tests");
            }

            @Override
            public FundsAccountBalanceView getBalance(FundsAccountId accountId) {
                throw new AssertionError("route post-processing must not query balances in resolver selection tests");
            }

            @Override
            public boolean supports(FundsAccountId accountId) {
                throw new AssertionError("route post-processing must not query account support in resolver selection tests");
            }
        };
    }

    private PlatformFundingAccountService unexpectedPlatformAccountService() {
        return new PlatformFundingAccountService() {

            @Override
            public FundsAccountId requireAccountId(CurrencyIsoCode currency, PlatformFundingAccountRole role) {
                throw new AssertionError("route post-processing must not resolve platform accounts in resolver selection tests");
            }

            @Override
            public FundsAccountId requireAccountId(Long tenantId,
                                                   CurrencyIsoCode currency,
                                                   PlatformFundingAccountRole role) {
                throw new AssertionError("route post-processing must not resolve platform accounts in resolver selection tests");
            }
        };
    }

    private static final class RecordingRouteResolver implements RouteResolver {

        private final boolean supported;

        private int supportsCalls;

        private int resolveCalls;

        private RecordingRouteResolver() {
            this(true);
        }

        private RecordingRouteResolver(boolean supported) {
            this.supported = supported;
        }

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            supportsCalls++;
            return supported;
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            resolveCalls++;
            throw new UnsupportedOperationException("resolve should not be called by supports");
        }
    }
}

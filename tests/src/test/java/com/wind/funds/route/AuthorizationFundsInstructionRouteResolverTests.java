package com.wind.funds.route;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.support.WindOperatorTestFixture;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.service.PlatformFundingAccountService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 授权交易路由解析契约测试。
 */
class AuthorizationFundsInstructionRouteResolverTests {

    private static final long TENANT_ID = 1L;

    private static final FundsAccountId USER_ACCOUNT = FundsAccountId.immutable("USER_1001",
            FundsSubjectType.FUNDING_ACCOUNT.name());

    private static final FundsAccountId SETTLEMENT_ACCOUNT = FundsAccountId.immutable("PLATFORM_SETTLEMENT_USD",
            FundsSubjectType.FUNDING_ACCOUNT.name());

    private AuthorizationFundsInstructionRouteResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        PlatformFundingAccountService platformFundingAccountService = new FixedPlatformFundingAccountService();
        FundsAccountQueryService fundsAccountQueryService = mock(FundsAccountQueryService.class);
        when(fundsAccountQueryService.getLedgerProfileCode(USER_ACCOUNT))
                .thenReturn(LedgerProfileCode.FUNDING_BASIC);
        resolver = new AuthorizationFundsInstructionRouteResolver(
                new RouteParticipantFactory(),
                new RouteSubjectSupport(fundsAccountQueryService),
                new PlatformAccountRouteSupport(platformFundingAccountService));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * 场景：授权退款请求不再暴露 refundMode，转换后的资金指令只保留外部交易引用。
     * 预期：路由解析器仍按无授权退款处理，并生成平台结算账户退回用户可用余额的路径。
     * 红线：无授权退款路由不能强依赖请求层已下线字段或内部分类标签。
     */
    @Test
    void testNoAuthRefundShouldResolveFromExternalTransactionReferenceWithoutRefundModeTag() {
        FundsInstructionSpec instruction = noAuthRefundInstructionWithoutRefundModeTag();

        assertThat(resolver.supports(instruction)).isTrue();

        ResolvedRouteSpec route = resolver.resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo(FundsRouteCodes.AUTHORIZATION_NO_AUTH_REFUND_STANDARD);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole())
                .containsExactly(RouteParticipantRole.PAYER, RouteParticipantRole.PAYEE);
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getSettlementFundingAccount()).isNotNull();
        assertThat(route.getPlatformAccounts().getSettlementFundingAccount().getSubjectId())
                .isEqualTo(SETTLEMENT_ACCOUNT.id());
        assertThat(route.getLegs()).singleElement()
                .satisfies(leg -> {
                    assertThat(leg.getLegType()).isEqualTo(RouteLegType.RESTORE);
                    assertThat(leg.getAmount()).isEqualTo(Money.immutable(40L, CurrencyIsoCode.USD));
                });
    }

    /**
     * 场景：资金指令已经显式带有退款内部分类标签，但引用仍是外部交易。
     * 预期：路由解析器尊重显式分类，不把争议退款误判为无授权退款。
     * 红线：外部交易引用只能补足缺失分类，不能覆盖已落定的退款业务分类。
     */
    @Test
    void testExplicitRefundClassificationShouldNotBeOverriddenByExternalReference() {
        FundsInstructionSpec instruction = noAuthRefundInstruction(Map.of(
                FundsInstructionContextKeys.REFUND_MODE, FundsInstructionContextKeys.REFUND_MODE_DISPUTE));

        assertThat(resolver.supports(instruction)).isFalse();
    }

    private FundsInstructionSpec noAuthRefundInstructionWithoutRefundModeTag() {
        return noAuthRefundInstruction(Map.of());
    }

    private FundsInstructionSpec noAuthRefundInstruction(Map<String, Object> contextVariables) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTH_REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(Money.immutable(40L, CurrencyIsoCode.USD))
                .accountId(USER_ACCOUNT)
                .reference(ImmutableFundsInstructionReferenceSpec.builder()
                        .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                        .externalTransactionId("CHANNEL_CAPTURE_1001")
                        .contextVariables(Map.of())
                        .build())
                .businessScene("AUTHORIZATION_NO_AUTH_REFUND")
                .businessSn("NO_AUTH_REFUND_BY_EXTERNAL_REFERENCE")
                .eventTime(LocalDateTime.of(2026, 6, 4, 0, 0))
                .operator(WindOperatorTestFixture.system())
                .contextVariables(contextVariables)
                .build();
    }

    private static final class FixedPlatformFundingAccountService implements PlatformFundingAccountService {

        @Override
        public FundsAccountId requireAccountId(CurrencyIsoCode currency, PlatformFundingAccountRole role) {
            assertThat(currency).isEqualTo(CurrencyIsoCode.USD);
            assertThat(role).isEqualTo(PlatformFundingAccountRole.SETTLEMENT);
            return SETTLEMENT_ACCOUNT;
        }

        @Override
        public FundsAccountId requireAccountId(Long tenantId, CurrencyIsoCode currency,
                                               PlatformFundingAccountRole role) {
            assertThat(tenantId).isEqualTo(TENANT_ID);
            return requireAccountId(currency, role);
        }
    }
}

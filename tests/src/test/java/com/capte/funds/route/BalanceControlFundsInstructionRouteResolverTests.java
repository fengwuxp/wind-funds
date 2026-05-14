package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceControlFundsInstructionRouteResolverTests {

    private FundsBalanceControlInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.balanceControlInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testResolveFundingBalanceAdjustShouldUsePlatformPrepayment() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(1_000L))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("ADJUST")
                .setBusinessSn("ADJUST_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("FUNDING_BALANCE_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.PREPAYMENT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getPrepaymentFundingAccount()).isNotNull();
    }

    @Test
    void testResolveFundingBalanceDecreaseShouldConstrainAvailableBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(1_000L))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("ADJUST")
                .setBusinessSn("ADJUST_0002"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("FUNDING_BALANCE_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.PREPAYMENT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getPrepaymentFundingAccount()).isNotNull();
    }

    @Test
    void testResolveCreditLimitAdjustShouldMoveLimitToAvailable() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                .setAmount(FundsRouteTestSupport.amount(2_000L))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("LIMIT")
                .setBusinessSn("LIMIT_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("CREDIT_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.LIMIT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
    }

    @Test
    void testResolveBudgetLimitAdjustShouldMoveAvailableToLimitOnDecrease() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(FundsRouteTestSupport.budgetGroup("budget_001"))
                .setAmount(FundsRouteTestSupport.amount(1_500L))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("BUDGET")
                .setBusinessSn("BUDGET_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BUDGET_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.LIMIT);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.DECREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
    }

    @Test
    void testResolveFreezeShouldConstrainAvailableBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToFreezeInstruction(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(600L))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BALANCE_FREEZE_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.HOLD, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN,
                    LedgerBalanceEffectType.HOLD, LedgerPhaseCode.FREEZE);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    @Test
    void testResolveUnfreezeShouldReleaseFrozenBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToUnfreezeInstruction(new FundsBalanceUnfreezeRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(600L))
                .setReferenceFreezeSn("FREEZE_0001")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("UNFREEZE_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BALANCE_UNFREEZE_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.RELEASE, LedgerSubjectCode.FROZEN, LedgerSubjectCode.AVAILABLE,
                    LedgerBalanceEffectType.RELEASE, LedgerPhaseCode.UNFREEZE);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.FROZEN);
        });
    }

    @Test
    void testFrozenOrderCreationShouldBeOptionalForFreezeRoute() {
        boolean dependsOnFrozenOrderService = Arrays.stream(BalanceControlFundsInstructionRouteResolver.class
                        .getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .flatMap(Arrays::stream)
                .map(Class::getSimpleName)
                .anyMatch("FundsFrozenOrderService"::equals);

        assertThat(dependsOnFrozenOrderService).isFalse();
    }

    private static void assertLeg(RouteLegSpec leg,
                                  RouteLegType legType,
                                  LedgerSubjectCode sourceLedgerSubjectCode,
                                  LedgerSubjectCode targetLedgerSubjectCode,
                                  LedgerBalanceEffectType balanceEffectType,
                                  LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(sourceLedgerSubjectCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(targetLedgerSubjectCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(balanceEffectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    private static void assertMustNotBeNegative(RouteLegSpec leg,
                                                FundsAccountId accountId,
                                                LedgerSubjectCode ledgerSubjectCode) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }
}

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

    /**
     * 场景：运营对普通资金账户做余额调增。
     * 输入：FUNDING_ACCOUNT，增加 1000，业务场景 ADJUST。
     * 输出：平台 ADJUSTMENT 账户到用户 AVAILABLE 的调账 route。
     * 预期：route 携带平台调整账户快照，用户 AVAILABLE 增加。
     * 红线：资金调增必须通过平台 ADJUSTMENT 平衡，不得凭空增加用户余额。
     */
    @Test
    void testResolveFundingBalanceAdjustShouldUsePlatformAdjustment() {
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
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.ADJUSTMENT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getAdjustmentFundingAccount()).isNotNull();
    }

    /**
     * 场景：运营对普通资金账户做余额调减。
     * 输入：FUNDING_ACCOUNT，减少 1000，业务场景 ADJUST。
     * 输出：用户 AVAILABLE 到平台 ADJUSTMENT 账户的调账 route。
     * 预期：用户 AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：余额调减不得让负 AVAILABLE 静默产生。
     */
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
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.ADJUSTMENT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getAdjustmentFundingAccount()).isNotNull();
    }

    /**
     * 场景：信用账户额度调增，释放可用额度。
     * 输入：CREDIT_ACCOUNT，增加 2000，业务场景 LIMIT。
     * 输出：LIMIT 到 AVAILABLE 的额度调整 route。
     * 预期：信用账户 AVAILABLE 增加，route 不引入真实资金主体。
     * 红线：信用额度账户不是真实资金，不得通过平台资金账户平衡。
     */
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

    /**
     * 场景：信用账户额度调减，收回可用额度。
     * 输入：CREDIT_ACCOUNT，减少 900，业务场景 LIMIT。
     * 输出：AVAILABLE 到 LIMIT 的额度调整 route。
     * 预期：信用账户 AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：额度回收不得绕过 AVAILABLE 非负约束形成超额使用。
     */
    @Test
    void testResolveCreditLimitDecreaseShouldConstrainAvailableBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.creditAccount("credit_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(900L))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("LIMIT")
                .setBusinessSn("LIMIT_0002"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("CREDIT_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    /**
     * 场景：预算组额度调增，释放可用预算。
     * 输入：BUDGET_GROUP，增加 1500，业务场景 BUDGET。
     * 输出：LIMIT 到 AVAILABLE 的额度调整 route。
     * 预期：预算组 AVAILABLE 增加，route 不引入真实资金主体。
     * 红线：预算组是控制主体，不得当作真实资金账户参与外部出入金。
     */
    @Test
    void testResolveBudgetLimitIncreaseShouldMoveLimitToAvailable() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(FundsRouteTestSupport.budgetGroup("budget_001"))
                .setAmount(FundsRouteTestSupport.amount(1_500L))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("BUDGET")
                .setBusinessSn("BUDGET_0002"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BUDGET_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.LIMIT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
    }

    /**
     * 场景：预算组额度调减，收回可用预算。
     * 输入：BUDGET_GROUP，减少 1500，业务场景 BUDGET。
     * 输出：AVAILABLE 到 LIMIT 的额度调整 route。
     * 预期：预算组 AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：预算回收不得让可用预算静默变负。
     */
    @Test
    void testResolveBudgetLimitAdjustShouldMoveAvailableToLimitOnDecrease() {
        FundsAccountId accountId = FundsRouteTestSupport.budgetGroup("budget_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(1_500L))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("BUDGET")
                .setBusinessSn("BUDGET_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BUDGET_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    /**
     * 场景：冻结普通资金账户可用余额。
     * 输入：FUNDING_ACCOUNT，冻结 600，业务场景 FREEZE。
     * 输出：AVAILABLE 到 FROZEN 的余额控制 route。
     * 预期：AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：冻结只做同主体余额桶控制，不表达消费或跨主体资金转移。
     */
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

    /**
     * 场景：释放普通资金账户冻结余额。
     * 输入：FUNDING_ACCOUNT，解冻 600，引用 FREEZE_0001。
     * 输出：FROZEN 到 AVAILABLE 的余额控制 route。
     * 预期：FROZEN 作为 source，并声明本次更新后必须非负。
     * 红线：解冻不得释放超过冻结桶剩余余额。
     */
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

    /**
     * 场景：route 解析只负责生成冻结路径，冻结单由生命周期服务独立管理。
     * 输入：BalanceControlFundsInstructionRouteResolver 构造器签名。
     * 输出：构造器不依赖 FundsFrozenOrderService。
     * 预期：route resolver 不直接创建或更新 FrozenOrder。
     * 红线：冻结事实载体不得和 route 解析职责耦合。
     */
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

package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceControlFundsInstructionRouteResolverTests extends BalanceControlFundsInstructionRouteResolverTestSupport {

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
            assertThat(leg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.PARTIAL_ALLOWED);
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

}

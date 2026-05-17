package com.capte.funds.transaction.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerPostingAssemblerTests extends DefaultLedgerPostingAssemblerTestSupport {

    /**
     * 场景：直接交易路径翻译为账务计划。
     * 输入：funding_001 向 funding_002 转账的单 leg 路径。
     * 输出：一个绑定 routeLegId 的 posting plan 和两条账本分录。
     * 预期：计划独立平衡，源账户按正常余额方向减少，目标账户增加。
     * 红线：Route -> Posting 翻译不得丢失 route leg 追溯或生成不平衡计划。
     */
    @Test
    void testAssembleShouldCreateBalancedPlan() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001", route(Map.of()));
        List<LedgerPostingPlanSpec> plans = transaction.getPostingPlans();

        assertThat(transaction.getFundsTransactionSn()).isEqualTo("FT_001");
        assertThat(plans).hasSize(1);
        LedgerPostingPlanSpec plan = plans.getFirst();
        assertThat(plan.isBalanced()).isTrue();
        assertThat(plan.getRouteLegId()).isEqualTo("LEG_001");
        assertThat(plan.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
        LedgerPostingPhaseSpec phase = plan.getPostingPhases().getFirst();
        assertThat(phase.getPhaseCode()).isEqualTo(LedgerPhaseCode.TRANSFER);
        assertThat(phase.getEntries()).hasSize(2);
        LedgerEntrySpec sourceEntry = phase.getEntries().get(0);
        LedgerEntrySpec targetEntry = phase.getEntries().get(1);
        assertThat(sourceEntry.getLedgerId()).isEqualTo(101L);
        assertThat(sourceEntry.getEntryType()).isEqualTo(EntrySide.DEBIT);
        assertThat(targetEntry.getLedgerId()).isEqualTo(102L);
        assertThat(targetEntry.getEntryType()).isEqualTo(EntrySide.CREDIT);
    }

    /**
     * 场景：不同交易事件翻译为不同账务作用域。
     * 输入：直接交易、授权占用和费用退款三类路径。
     * 输出：posting plan 与 entry 上的 postingScope。
     * 预期：直接交易为主体间转移，授权为控制占用，费用退款为费用作用域。
     * 红线：账务作用域必须在持久化前确定，不能依赖后置数据库状态补语义。
     */
    @Test
    void testAssembleShouldResolvePostingScopeBeforeLedgerPersistence() {
        DefaultLedgerPostingAssembler directAssembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));
        DefaultLedgerPostingAssembler authorizationAssembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(201L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(202L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(301L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(302L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT)
        )));
        DefaultLedgerPostingAssembler feeAssembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.FEE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        assertPostingScope(directAssembler.assemble(instruction(), "FT_001", route(Map.of())),
                LedgerPostingScope.BETWEEN_SUBJECTS);
        assertPostingScope(authorizationAssembler.assemble(instruction(), "FT_002", sharedCardRoute()),
                LedgerPostingScope.CONTROL_HOLD);
        assertPostingScope(feeAssembler.assemble(instruction(), "FT_003", feeRefundRoute()),
                LedgerPostingScope.FEE);
    }

    /**
     * 场景：RouteLeg 对余额约束提供多层覆盖。
     * 输入：同时存在主体+科目级和科目级 constraint override。
     * 输出：源、目标分录上的 balanceConstraintType。
     * 预期：优先使用最精确的主体+科目级约束，未命中时回落到科目级默认。
     * 红线：余额约束不得因 Map 顺序或宽泛 key 覆盖精确主体规则。
     */
    @Test
    void testAssembleShouldUseMostSpecificConstraintOverride() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));
        Map<String, LedgerBalanceConstraintType> overrides = Map.of(
                "FUNDING_ACCOUNT:funding_001:AVAILABLE", LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE,
                "AVAILABLE", LedgerBalanceConstraintType.PROFILE_DEFAULT
        );

        LedgerPostingPhaseSpec phase = assembler.assemble(instruction(), "FT_001", route(overrides))
                .getPostingPlans().getFirst().getPostingPhases().getFirst();

        assertThat(phase.getEntries().get(0).getBalanceConstraintType())
                .isEqualTo(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
        assertThat(phase.getEntries().get(1).getBalanceConstraintType())
                .isEqualTo(LedgerBalanceConstraintType.PROFILE_DEFAULT);
    }

    /**
     * 场景：交易路径引用的主体账本尚未初始化。
     * 输入：无法查询到账本 bucket 的转账路径。
     * 输出：Assembler 在生成分录前失败。
     * 预期：写流程缺账本直接拒绝，不自动建账。
     * 红线：Route -> Posting 不得在交易路径中隐式创建账本或降级为空入账。
     */
    @Test
    void testAssembleShouldRejectMissingLedger() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.of()));

        assertThatThrownBy(() -> assembler.assemble(instruction(), "FT_001", route(Map.of())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本不存在或不唯一");
    }

    /**
     * 场景：交易路径金额币种与账本币种不一致。
     * 输入：路径金额为 USD，但目标主体账本为 EUR。
     * 输出：Assembler 在生成分录前拒绝。
     * 预期：账本 bucket 币种必须和 route leg 金额币种一致。
     * 红线：不得把未经过换汇建模的多币种资金变化写入同一条 route leg。
     */
    @Test
    void testAssembleShouldRejectLedgerCurrencyMismatch() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, CurrencyIsoCode.EUR)
        )));

        assertThatThrownBy(() -> assembler.assemble(instruction(), "FT_001", route(Map.of())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本币种与路径金额币种不一致");
    }

    /**
     * 场景：共享卡授权同时占用信用账户、预算组和资金账户。
     * 输入：三个主体各自的 AVAILABLE -> AUTHORIZATION 控制路径。
     * 输出：三个独立 posting plan，每个 plan 两条分录。
     * 预期：每个主体的余额控制独立平衡，并保留各自 ledgerId。
     * 红线：多主体授权不得合并成一笔跨主体不透明入账。
     */
    @Test
    void testAssembleShouldCreateIndependentPlansForSharedCardSubjects() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(201L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(202L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(301L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(302L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001", sharedCardRoute());
        List<LedgerPostingPlanSpec> plans = transaction.getPostingPlans();

        assertThat(plans).hasSize(3);
        assertThat(plans).allSatisfy(plan -> {
            assertThat(plan.isBalanced()).isTrue();
            assertThat(plan.getPostingPhases()).hasSize(1);
            assertThat(plan.getPostingPhases().getFirst().getEntries()).hasSize(2);
        });
        assertThat(plans)
                .flatExtracting(LedgerPostingPlanSpec::getEntries)
                .extracting(LedgerEntrySpec::getLedgerId)
                .containsExactly(101L, 102L, 201L, 202L, 301L, 302L);
    }

    /**
     * 场景：费用退款路径翻译为独立费用退款账务意图。
     * 输入：`FEE_REFUND` 事件和费用退款 route leg。
     * 输出：posting plan 与 entry 上的 intent。
     * 预期：全部标记为 `FEE_REFUND`，不混入普通退款。
     * 红线：费用退款不得回退为普通退款，否则后续费用对账和报表口径会漂移。
     */
    @Test
    void testAssembleShouldUseFeeRefundIntentForFeeRefundEvent() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.FEE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001", feeRefundRoute());
        LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();

        assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.FEE_REFUND);
        assertThat(plan.getEntries())
                .extracting(LedgerEntrySpec::getIntent)
                .containsOnly(LedgerPostingIntentType.FEE_REFUND);
    }

}

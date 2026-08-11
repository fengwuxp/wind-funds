package com.wind.funds.ledger.posting;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.wind.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.wind.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerStateRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.Pagination;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.query.supports.QueryType;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * 默认账务计划装配器契约测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        DefaultLedgerPostingAssemblerTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DefaultLedgerPostingAssemblerTests extends AbstractFundsServiceTest {

    private static final Money AMOUNT = Money.immutable(100L, CurrencyIsoCode.USD);

    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 5, 21, 10, 0);

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    private static final Pattern SHORT_HEX_DIGEST_PATTERN = Pattern.compile("[0-9a-f]{16}");

    @Autowired
    private DefaultLedgerPostingAssembler assembler;

    @Autowired
    private RecordingLedgerService ledgerService;

    @Autowired
    private LedgerPostingPlanMapper ledgerPostingPlanMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @BeforeEach
    void setUpAssemblerTestData() {
        ledgerService.clear();
        reset(ledgerPostingPlanMapper, ledgerEntryMapper);
    }

    /**
     * 场景：资金指令指定 MONTHLY 账期但缺少 periodId。
     * 预期：指令构造阶段明确失败，且不进入账本查询。
     * 红线：route 不再承载账期，资金指令也不能静默补齐非 LIFETIME periodId。
     */
    @Test
    void testInstructionShouldRejectNonLifetimePeriodWithoutPeriodId() {
        assertThatThrownBy(() -> instruction(AccountBalancePeriodType.MONTHLY, null,
                LedgerSubjectCode.AVAILABLE))
                .hasMessageContaining("ledgerPeriodId must not be blank");

        assertThat(ledgerService.queries).isEmpty();
    }

    @Test
    void testAssembleShouldUseExplicitNonLifetimePeriodId() {
        LedgerTransactionSpec transaction = assembler.assemble(
                instruction(AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID, LedgerSubjectCode.AVAILABLE),
                "FUNDS_TX_PERIOD_002",
                resolvedRoute(routeLeg()));

        assertThat(transaction.getPostingPlans()).hasSize(1);
        assertTransferPostingFacts(transaction, AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID);
        assertThat(ledgerService.queries).hasSize(2).allSatisfy(query -> {
            assertThat(query.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
            assertThat(query.getPeriodId()).isEqualTo(MONTHLY_PERIOD_ID);
        });
    }

    @Test
    void testAssembleShouldUseLifetimePeriodIdByDefault() {
        LedgerTransactionSpec transaction = assembler.assemble(
                instruction(AccountBalancePeriodType.LIFETIME, null, LedgerSubjectCode.AVAILABLE),
                "FUNDS_TX_PERIOD_003",
                resolvedRoute(routeLeg()));

        assertThat(transaction.getPostingPlans()).hasSize(1);
        assertTransferPostingFacts(transaction, AccountBalancePeriodType.LIFETIME,
                AccountBalancePeriodType.LIFETIME.name());
        assertThat(ledgerService.queries).hasSize(2).allSatisfy(query -> {
            assertThat(query.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
            assertThat(query.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        });
    }

    /**
     * 场景：route 只携带资金路径，资金指令指定收款科目与账期。
     * 预期：assembler 独立派生科目、余额效果、阶段、期间和约束，并保留 routeLegId 追溯。
     * 红线：route 不得重新承载账务语义，posting 仍必须完整且平衡。
     */
    @Test
    void testAssembleShouldDeriveAccountingFactsOutsideRoute() {
        RouteLegSpec leg = new TestRouteLegSpec(
                "LEG-POSTING-HYBRID-001",
                RouteLegType.INTERNAL_TRANSFER,
                routeNode("source_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.SOURCE),
                routeNode("target_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.TARGET),
                Map.of());

        LedgerTransactionSpec transaction = assembler.assemble(
                instruction(AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID, LedgerSubjectCode.FROZEN),
                "FUNDS_TX_HYBRID_001",
                resolvedRoute(leg));

        LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();
        assertThat(transaction.isBalanced()).isTrue();
        assertThat(plan.isBalanced()).isTrue();
        assertThat(plan.getRouteLegId()).isEqualTo(leg.getLegId());
        assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER);
        assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS);
        assertThat(plan.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
        assertThat(plan.getContextVariables()).containsEntry("routeLegId", leg.getLegId());
        assertThat(plan.getEntries()).hasSize(2).allSatisfy(entry -> {
            assertThat(entry.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
            assertThat(entry.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
            assertThat(entry.getPeriodId()).isEqualTo(MONTHLY_PERIOD_ID);
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER);
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS);
            assertThat(entry.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
            assertThat(entry.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT);
            assertThat(entry.getContextVariables()).containsEntry("routeLegId", leg.getLegId());
        });
        assertThat(plan.getEntries())
                .extracting(LedgerEntrySpec::getLedgerSubjectCode,
                        LedgerEntrySpec::getEntryType,
                        LedgerEntrySpec::getBalanceConstraintType)
                .containsExactly(
                        tuple(LedgerSubjectCode.AVAILABLE,
                                EntrySide.DEBIT,
                                LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE),
                        tuple(LedgerSubjectCode.FROZEN,
                                EntrySide.CREDIT,
                                LedgerBalanceConstraintType.PROFILE_DEFAULT));
    }

    /**
     * 场景：所有公开资金事件进入默认 route -> posting 翻译器。
     * 预期：每个事件都映射到当前明确的 posting intent/scope，并保留 route phase/effect。
     * 红线：新增事件或边界迁移不能遗漏账务映射，也不能借默认分支静默改变既有事件语义。
     */
    @Test
    void testAssembleShouldPreserveEventToPostingMatrix() {
        List<PostingCase> cases = postingCases();
        cases.forEach(postingCase -> {
            RouteLegSpec leg = postingCaseLeg(postingCase);
            LedgerTransactionSpec transaction = assembler.assemble(
                    instruction(postingCase),
                    "FUNDS_TX_MATRIX_" + postingCase.eventType().name(),
                    resolvedRoute(postingCase, leg));

            LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();
            assertThat(plan.isBalanced()).as("%s plan balance", postingCase.eventType()).isTrue();
            assertThat(plan.getIntent()).as("%s intent", postingCase.eventType())
                    .isEqualTo(postingCase.expectedIntent());
            assertThat(plan.getPostingScope()).as("%s scope", postingCase.eventType())
                    .isEqualTo(postingCase.expectedScope());
            assertThat(plan.getBalanceEffectType()).as("%s effect", postingCase.eventType())
                    .isEqualTo(postingCase.balanceEffectType());
            assertThat(plan.getEntries()).allSatisfy(entry -> {
                assertThat(entry.getIntent()).isEqualTo(postingCase.expectedIntent());
                assertThat(entry.getPostingScope()).isEqualTo(postingCase.expectedScope());
                assertThat(entry.getBalanceEffectType()).isEqualTo(postingCase.balanceEffectType());
                assertThat(entry.getPhaseCode()).isEqualTo(postingCase.phaseCode());
            });
        });
    }

    @Test
    void testAssembleLimitAdjustIncreaseShouldBalanceControlLedgerEntries() {
        LedgerTransactionSpec transaction = assembler.assemble(
                limitAdjustInstruction(true), "FUNDS_TX_LIMIT_INC", limitAdjustRoute());

        assertThat(transaction.getPostingPlans()).hasSize(1);
        assertThat(transaction.getPostingPlans().getFirst().isBalanced()).isTrue();
        assertThat(transaction.getPostingPlans().getFirst().getEntries())
                .extracting(LedgerEntrySpec::getLedgerSubjectCode, LedgerEntrySpec::getEntryType)
                .containsExactly(
                        tuple(LedgerSubjectCode.LIMIT, EntrySide.DEBIT),
                        tuple(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT));
        assertThat(ledgerService.queries).hasSize(1);
    }

    @Test
    void testAssembleLimitAdjustDecreaseShouldBalanceControlLedgerEntries() {
        LedgerTransactionSpec transaction = assembler.assemble(
                limitAdjustInstruction(false), "FUNDS_TX_LIMIT_DEC", limitAdjustRoute());

        assertThat(transaction.getPostingPlans()).hasSize(1);
        assertThat(transaction.getPostingPlans().getFirst().isBalanced()).isTrue();
        assertThat(transaction.getPostingPlans().getFirst().getEntries())
                .extracting(LedgerEntrySpec::getLedgerSubjectCode, LedgerEntrySpec::getEntryType)
                .containsExactly(
                        tuple(LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT),
                        tuple(LedgerSubjectCode.LIMIT, EntrySide.CREDIT));
        assertThat(ledgerService.queries).hasSize(1);
    }

    /**
     * 场景：直接交易路径的 route leg ID 过长，原始 planId 会超过落库长度。
     * 输入：LIFETIME 周期，资金账户 AVAILABLE -> AVAILABLE，USD 100.00。
     * 预期：posting plan ID 被压缩到 64 字符以内，交易号和 route leg 仍可追溯。
     * 红线：长度保护不得破坏 posting plan 平衡、entry 交易号、主体、账目和借贷方向。
     */
    @Test
    void testAssembleShouldTruncateLongPlanIdWithoutLosingPostingFacts() {
        RouteLegSpec leg = routeLeg("LEG-"
                + "POSTING-PERIOD-WITH-A-VERY-LONG-ROUTE-LEG-ID-001");
        LedgerTransactionSpec transaction = assembler.assemble(
                instruction(), "FUNDS_TX_WITH_A_LONG_LEDGER_TRANSACTION_SN_001", resolvedRoute(leg));

        LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();
        String planId = plan.getPlanId();
        String rawPlanId = "TRANSFER_" + transaction.getSn() + "_" + leg.getLegId();
        String digest = planId.substring(planId.lastIndexOf('_') + 1);

        assertThat(rawPlanId).hasSizeGreaterThan(64);
        assertThat(planId).hasSizeLessThanOrEqualTo(64);
        assertThat(planId).isNotEqualTo(rawPlanId);
        assertThat(planId).startsWith("TRANSFER_" + transaction.getSn() + "_");
        assertThat(digest).matches(SHORT_HEX_DIGEST_PATTERN);
        assertThat(transaction.isBalanced()).isTrue();
        assertThat(plan.isBalanced()).isTrue();
        assertThat(plan.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
        assertThat(plan.getRouteLegId()).isEqualTo(leg.getLegId());
        assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER);
        assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS);
        assertThat(plan.getEntries()).hasSize(2).allSatisfy(entry -> {
            assertThat(entry.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
            assertThat(entry.getAmount()).isEqualTo(AMOUNT);
            assertThat(entry.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT);
        });
        assertThat(plan.getEntries())
                .extracting(LedgerEntrySpec::getSubjectId, LedgerEntrySpec::getLedgerSubjectCode,
                        LedgerEntrySpec::getEntryType)
                .containsExactly(
                        tuple("source_account", LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT),
                        tuple("target_account", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT));
    }

    /**
     * 场景：自定义 ResolvedRouteSpec/RouteLegSpec 绕过默认不可变 DSL，装配后继续改写原始嵌套上下文。
     * 预期：已生成的账务计划和分录事实保持稳定，不被追加的敏感字段污染。
     * 红线：账务事实不能因 route 上下文浅拷贝让 PAN、密钥或外部账户原文进入落库链路。
     */
    @Test
    void testAssembleShouldDefensivelyCopyNestedRouteContextVariables() {
        Map<String, Object> routeProcessorPayload = new HashMap<>();
        routeProcessorPayload.put("routeTraceId", "ROUTE-TRACE-202605270001");
        Map<String, Object> legProcessorPayload = new HashMap<>();
        legProcessorPayload.put("legTraceId", "LEG-TRACE-202605270001");
        RouteLegSpec leg = routeLeg("LEG-POSTING-CONTEXT-001",
                Map.of("legProcessorPayload", legProcessorPayload));
        LedgerTransactionSpec transaction = assembler.assemble(
                instruction(),
                "FUNDS_TX_CONTEXT_001",
                resolvedRoute(leg, Map.of("routeProcessorPayload", routeProcessorPayload)));

        routeProcessorPayload.put("secretKey", "secret-after-assemble");
        legProcessorPayload.put("pan", "PAN_AFTER_ASSEMBLE_SHOULD_NOT_LEAK");

        LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();
        assertRoutePayloadSafe(plan.getContextVariables());
        assertLegPayloadSafe(plan.getContextVariables());
        plan.getEntries().forEach(entry -> {
            assertRoutePayloadSafe(entry.getContextVariables());
            assertLegPayloadSafe(entry.getContextVariables());
        });
    }

    /**
     * 场景：退款引用的账本交易实际属于另一笔资金交易。
     * 预期：组装器在读取原 posting 前拒绝不一致的 provenance。
     * 红线：不得跨原资金交易复用账本事实。
     */
    @Test
    void testReplayShouldRejectLedgerTransactionOwnedByDifferentFundsTransaction() {
        LedgerPostingPlan originalPlan = new LedgerPostingPlan();
        originalPlan.setSn("POSTING_PLAN_ORIGINAL");
        when(ledgerPostingPlanMapper.selectListByQuery(any())).thenReturn(List.of(originalPlan));
        when(ledgerEntryMapper.selectListByQuery(any())).thenReturn(List.of(
                originalEntry("target_account", LedgerSubjectCode.SETTLEMENT),
                originalEntry("source_account", LedgerSubjectCode.AVAILABLE)));
        FundsInstructionSpec instruction = ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(AMOUNT)
                .originalAmount(AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .reference(ImmutableFundsInstructionReferenceSpec.builder()
                        .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                        .referenceSn("FUNDS_TRANSACTION_A")
                        .referenceLedgerTransactionSn("LEDGER_TRANSACTION_B")
                        .contextVariables(Map.of())
                        .build())
                .businessScene("POSTING_REPLAY_PROVENANCE")
                .businessSn("BIZ-POSTING-REPLAY-PROVENANCE-001")
                .eventTime(EVENT_TIME)
                .operator(WindOperatorFactory.system())
                .contextVariables(Map.of())
                .build();
        RouteLegSpec replayLeg = ImmutableRouteLegSpec.builder()
                .legId("REFUND_LEG")
                .sequence(1)
                .legType(RouteLegType.RESTORE)
                .sourceNode(routeNode("target_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.SOURCE))
                .targetNode(routeNode("source_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.TARGET))
                .amount(AMOUNT)
                .originalAmount(AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .replayRefLegId("ORIGINAL_LEG")
                .contextVariables(Map.of())
                .build();
        ResolvedRouteSpec route = ImmutableResolvedRouteSpec.builder()
                .tenantId(TENANT_ID)
                .routeCode("POSTING_REPLAY_PROVENANCE_ROUTE")
                .routeVersion("v1")
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .instructionType(instruction.getInstructionType())
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .participants(List.of())
                .legs(List.of(replayLeg))
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();

        assertThatThrownBy(() -> assembler.assemble(instruction, "FUNDS_TRANSACTION_REFUND", route))
                .hasMessageContaining("原账本交易与引用资金交易不一致");
    }

    private LedgerEntry originalEntry(String subjectId, LedgerSubjectCode subjectCode) {
        LedgerEntry result = new LedgerEntry();
        result.setSubjectId(subjectId);
        result.setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name());
        result.setLedgerSubjectCode(subjectCode);
        result.setPeriodType(AccountBalancePeriodType.LIFETIME);
        result.setPeriodId(AccountBalancePeriodType.LIFETIME.name());
        return result;
    }

    private void assertRoutePayloadSafe(Map<String, Object> contextVariables) {
        Object payloadValue = contextVariables.get("routeProcessorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("routeTraceId")).isEqualTo("ROUTE-TRACE-202605270001");
        assertThat(payload.containsKey("secretKey")).isFalse();
    }

    private void assertLegPayloadSafe(Map<String, Object> contextVariables) {
        Object payloadValue = contextVariables.get("legProcessorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("legTraceId")).isEqualTo("LEG-TRACE-202605270001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    private void assertTransferPostingFacts(LedgerTransactionSpec transaction,
                                            AccountBalancePeriodType periodType,
                                            String periodId) {
        assertThat(transaction.isBalanced()).isTrue();
        LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();
        assertThat(plan.isBalanced()).isTrue();
        assertThat(plan.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
        assertThat(plan.getRouteLegId()).isEqualTo("LEG-POSTING-PERIOD-001");
        assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER);
        assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS);
        assertThat(plan.getEntries()).hasSize(2).allSatisfy(entry -> {
            assertThat(entry.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
            assertThat(entry.getAmount()).isEqualTo(AMOUNT);
            assertThat(entry.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
            assertThat(entry.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT);
        });
        assertThat(ledgerService.queries).hasSize(2).allSatisfy(query -> {
            assertThat(query.getPeriodType()).isEqualTo(periodType);
            assertThat(query.getPeriodId()).isEqualTo(periodId);
        });
        assertThat(plan.getEntries())
                .extracting(LedgerEntrySpec::getPeriodType, LedgerEntrySpec::getPeriodId)
                .containsExactly(
                        tuple(periodType, periodId),
                        tuple(periodType, periodId));
        assertThat(plan.getEntries())
                .extracting(LedgerEntrySpec::getSubjectId, LedgerEntrySpec::getLedgerSubjectCode,
                        LedgerEntrySpec::getEntryType)
                .containsExactly(
                        tuple("source_account", LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT),
                        tuple("target_account", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT));
    }

    private FundsInstructionSpec instruction() {
        return instruction(null, null, LedgerSubjectCode.AVAILABLE);
    }

    private FundsInstructionSpec instruction(AccountBalancePeriodType periodType,
                                              String periodId,
                                              LedgerSubjectCode payeeLedgerSubjectCode) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(AMOUNT)
                .originalAmount(AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .businessScene("POSTING_PERIOD")
                .businessSn("BIZ-POSTING-PERIOD-001")
                .eventTime(EVENT_TIME)
                .operator(WindOperatorFactory.system())
                .payeeLedgerSubjectCode(payeeLedgerSubjectCode)
                .ledgerPeriodType(periodType)
                .ledgerPeriodId(periodId)
                .contextVariables(Map.of())
                .build();
    }

    private FundsInstructionSpec instruction(PostingCase postingCase) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(postingCase.instructionType())
                .eventType(postingCase.eventType())
                .transactionType(postingCase.transactionType())
                .amount(AMOUNT)
                .originalAmount(AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .businessScene("POSTING_MATRIX")
                .businessSn("BIZ-POSTING-MATRIX-" + postingCase.eventType().name())
                .eventTime(EVENT_TIME)
                .operator(WindOperatorFactory.system())
                .payeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .contextVariables(postingCase.eventType() == FundsTransactionEventType.LIMIT_ADJUST
                        || postingCase.eventType() == FundsTransactionEventType.BALANCE_ADJUST
                        ? Map.of(FundsInstructionContextKeys.INCREASE, true) : Map.of())
                .build();
    }

    private FundsInstructionSpec limitAdjustInstruction(boolean increase) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.LIMIT_ADJUST)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .amount(AMOUNT)
                .originalAmount(AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .businessScene("LIMIT_ADJUST")
                .businessSn("BIZ-LIMIT-ADJUST-001")
                .eventTime(EVENT_TIME)
                .operator(WindOperatorFactory.system())
                .contextVariables(Map.of(FundsInstructionContextKeys.INCREASE, increase))
                .build();
    }

    private ResolvedRouteSpec resolvedRoute(RouteLegSpec leg) {
        return resolvedRoute(leg, Map.of());
    }

    private ResolvedRouteSpec resolvedRoute(RouteLegSpec leg, Map<String, Object> contextVariables) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(TENANT_ID)
                .routeCode("POSTING_PERIOD_ROUTE")
                .routeVersion("v1")
                .businessScene("POSTING_PERIOD")
                .businessSn("BIZ-POSTING-PERIOD-001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of())
                .legs(List.of(leg))
                .resolvedAt(EVENT_TIME)
                .contextVariables(contextVariables)
                .build();
    }

    private ResolvedRouteSpec resolvedRoute(PostingCase postingCase, RouteLegSpec leg) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(TENANT_ID)
                .routeCode("POSTING_MATRIX_ROUTE")
                .routeVersion("v1")
                .businessScene("POSTING_MATRIX")
                .businessSn("BIZ-POSTING-MATRIX-" + postingCase.eventType().name())
                .instructionType(postingCase.instructionType())
                .eventType(postingCase.eventType())
                .transactionType(postingCase.transactionType())
                .participants(List.of())
                .legs(List.of(leg))
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private ResolvedRouteSpec limitAdjustRoute() {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(TENANT_ID)
                .routeCode("LIMIT_ADJUST_ROUTE")
                .routeVersion("v1")
                .businessScene("LIMIT_ADJUST")
                .businessSn("BIZ-LIMIT-ADJUST-001")
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.LIMIT_ADJUST)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .participants(List.of())
                .legs(List.of(limitAdjustLeg()))
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private RouteLegSpec routeLeg() {
        return routeLeg("LEG-POSTING-PERIOD-001");
    }

    private RouteLegSpec routeLeg(String legId) {
        return routeLeg(legId, Map.of());
    }

    private RouteLegSpec routeLeg(String legId,
                                  Map<String, Object> contextVariables) {
        return new TestRouteLegSpec(
                legId,
                RouteLegType.INTERNAL_TRANSFER,
                routeNode("source_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.SOURCE),
                routeNode("target_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.TARGET),
                contextVariables
        );
    }

    private RouteLegSpec limitAdjustLeg() {
        return new TestRouteLegSpec(
                "LEG-POSTING-PERIOD-001",
                RouteLegType.INTERNAL_TRANSFER,
                routeNode("credit_account", FundsSubjectType.CREDIT_ACCOUNT, RouteNodeRole.SOURCE),
                routeNode("credit_account", FundsSubjectType.CREDIT_ACCOUNT, RouteNodeRole.TARGET),
                Map.of()
        );
    }

    private RouteLegSpec postingCaseLeg(PostingCase postingCase) {
        if (postingCase.eventType() == FundsTransactionEventType.LIMIT_ADJUST) {
            return limitAdjustLeg();
        }
        return new TestRouteLegSpec(
                "LEG-POSTING-MATRIX-" + postingCase.eventType().name(),
                switch (postingCase.eventType()) {
                    case TOPUP -> RouteLegType.EXTERNAL_IN;
                    case WITHDRAW, PAYOUT_SUCCEEDED -> RouteLegType.EXTERNAL_OUT;
                    default -> RouteLegType.INTERNAL_TRANSFER;
                },
                routeNode("source_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.SOURCE),
                routeNode("target_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.TARGET),
                Map.of()
        );
    }

    private List<PostingCase> postingCases() {
        return List.of(
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.TOPUP,
                        DefaultFundsTransactionType.TOPUP, LedgerPhaseCode.FUND_IN,
                        LedgerBalanceEffectType.INCREASE, LedgerPostingIntentType.TOPUP,
                        LedgerPostingScope.PLATFORM_EXTERNAL),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.TRANSFER,
                        DefaultFundsTransactionType.TRANSFER, LedgerPhaseCode.TRANSFER,
                        LedgerBalanceEffectType.CONSUME, LedgerPostingIntentType.TRANSFER,
                        LedgerPostingScope.BETWEEN_SUBJECTS),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.PAY,
                        DefaultFundsTransactionType.PAY, LedgerPhaseCode.SETTLEMENT,
                        LedgerBalanceEffectType.CONSUME, LedgerPostingIntentType.TRANSFER,
                        LedgerPostingScope.BETWEEN_SUBJECTS),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.WITHDRAW,
                        DefaultFundsTransactionType.WITHDRAW, LedgerPhaseCode.FUND_OUT,
                        LedgerBalanceEffectType.DECREASE, LedgerPostingIntentType.WITHDRAWAL,
                        LedgerPostingScope.PLATFORM_EXTERNAL),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.FEE_CHARGE,
                        DefaultFundsTransactionType.FEE, LedgerPhaseCode.FEE,
                        LedgerBalanceEffectType.CONSUME, LedgerPostingIntentType.FEE,
                        LedgerPostingScope.FEE),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.CLEARING_CONFIRM,
                        DefaultFundsTransactionType.CLEARING, LedgerPhaseCode.SETTLEMENT,
                        LedgerBalanceEffectType.RELEASE, LedgerPostingIntentType.SETTLEMENT,
                        LedgerPostingScope.WITHIN_SUBJECT),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.SETTLEMENT_LOCK,
                        DefaultFundsTransactionType.SETTLEMENT, LedgerPhaseCode.SETTLEMENT,
                        LedgerBalanceEffectType.CONSUME, LedgerPostingIntentType.SETTLEMENT,
                        LedgerPostingScope.WITHIN_SUBJECT),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.PAYOUT_SUCCEEDED,
                        DefaultFundsTransactionType.PAYOUT, LedgerPhaseCode.FUND_OUT,
                        LedgerBalanceEffectType.DECREASE, LedgerPostingIntentType.WITHDRAWAL,
                        LedgerPostingScope.PLATFORM_EXTERNAL),
                new PostingCase(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.PAYOUT_FAILED,
                        DefaultFundsTransactionType.PAYOUT, LedgerPhaseCode.REFUND,
                        LedgerBalanceEffectType.RESTORE, LedgerPostingIntentType.REFUND,
                        LedgerPostingScope.BETWEEN_SUBJECTS),
                new PostingCase(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.AUTHORIZE,
                        DefaultFundsTransactionType.PAY, LedgerPhaseCode.AUTHORIZATION,
                        LedgerBalanceEffectType.HOLD, LedgerPostingIntentType.AUTHORIZATION,
                        LedgerPostingScope.CONTROL_HOLD),
                new PostingCase(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.REVERSAL,
                        DefaultFundsTransactionType.PAY, LedgerPhaseCode.REVERSAL,
                        LedgerBalanceEffectType.RELEASE, LedgerPostingIntentType.AUTHORIZATION_REVERSAL,
                        LedgerPostingScope.CONTROL_HOLD),
                new PostingCase(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.COMPLETE,
                        DefaultFundsTransactionType.PAY, LedgerPhaseCode.COMPLETION,
                        LedgerBalanceEffectType.CONSUME, LedgerPostingIntentType.AUTHORIZATION_COMPLETION,
                        LedgerPostingScope.CONTROL_CONSUME),
                new PostingCase(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.AUTH_REFUND,
                        DefaultFundsTransactionType.REFUND, LedgerPhaseCode.REFUND,
                        LedgerBalanceEffectType.RESTORE, LedgerPostingIntentType.REFUND,
                        LedgerPostingScope.BETWEEN_SUBJECTS),
                new PostingCase(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.FREEZE,
                        DefaultFundsTransactionType.BALANCE_CONTROL, LedgerPhaseCode.FREEZE,
                        LedgerBalanceEffectType.HOLD, LedgerPostingIntentType.HOLD,
                        LedgerPostingScope.WITHIN_SUBJECT),
                new PostingCase(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.UNFREEZE,
                        DefaultFundsTransactionType.BALANCE_CONTROL, LedgerPhaseCode.UNFREEZE,
                        LedgerBalanceEffectType.RELEASE, LedgerPostingIntentType.RELEASE,
                        LedgerPostingScope.CONTROL_RELEASE),
                new PostingCase(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.BALANCE_ADJUST,
                        DefaultFundsTransactionType.ADJUSTMENT, LedgerPhaseCode.ADJUSTMENT,
                        LedgerBalanceEffectType.INCREASE, LedgerPostingIntentType.ADJUSTMENT,
                        LedgerPostingScope.ADJUSTMENT),
                new PostingCase(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.LIMIT_ADJUST,
                        DefaultFundsTransactionType.ADJUSTMENT, LedgerPhaseCode.ADJUSTMENT,
                        LedgerBalanceEffectType.INCREASE, LedgerPostingIntentType.ADJUSTMENT,
                        LedgerPostingScope.ADJUSTMENT)
        );
    }

    private RouteNodeSpec routeNode(String subjectId,
                                    FundsSubjectType subjectType,
                                    RouteNodeRole nodeRole) {
        SubjectRef subjectRef = ImmutableSubjectRef.builder()
                .tenantId(TENANT_ID)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(CurrencyIsoCode.USD.name())
                .build();
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .nodeRole(nodeRole)
                .build();
    }

    private record TestRouteLegSpec(String legId,
                                    RouteLegType legType,
                                    RouteNodeSpec sourceNode,
                                    RouteNodeSpec targetNode,
                                    Map<String, Object> contextVariables) implements RouteLegSpec {

        @Override
        public String getLegId() {
            return legId;
        }

        @Override
        public RouteLegType getLegType() {
            return legType;
        }

        @Override
        public RouteNodeSpec getSourceNode() {
            return sourceNode;
        }

        @Override
        public RouteNodeSpec getTargetNode() {
            return targetNode;
        }

        @Override
        public Money getAmount() {
            return AMOUNT;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return contextVariables;
        }
    }

    private record PostingCase(FundsInstructionType instructionType,
                               FundsTransactionEventType eventType,
                               DefaultFundsTransactionType transactionType,
                               LedgerPhaseCode phaseCode,
                               LedgerBalanceEffectType balanceEffectType,
                               LedgerPostingIntentType expectedIntent,
                               LedgerPostingScope expectedScope) {
    }

    private static final class RecordingLedgerService implements LedgerService {

        private final List<LedgerQuery> queries = new ArrayList<>();

        private long ledgerId = 1L;

        void clear() {
            queries.clear();
            ledgerId = 1L;
        }

        @Override
        public Long createLedger(CreateLedgerRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateLedgerState(UpdateLedgerStateRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LedgerDTO getLedgerById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<LedgerDTO> getLedgerByIds(Collection<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WindPagination<LedgerDTO> queryLedgers(LedgerQuery query,
                                                      WindQuery<? extends QueryOrderField> options) {
            queries.add(query);
            List<LedgerDTO> records = ledgerSubjectCodes(query).stream()
                    .map(subjectCode -> ledger(query, subjectCode))
                    .toList();
            return Pagination.of(records, 1, options.getQuerySize(), QueryType.QUERY_BOTH, records.size());
        }

        private List<LedgerSubjectCode> ledgerSubjectCodes(LedgerQuery query) {
            if (query.getLedgerSubjectCode() != null) {
                return List.of(query.getLedgerSubjectCode());
            }
            if (FundsSubjectType.CREDIT_ACCOUNT.name().equals(query.getSubjectType())) {
                return List.of(LedgerSubjectCode.LIMIT,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.AUTHORIZATION);
            }
            return List.of(LedgerSubjectCode.AVAILABLE,
                    LedgerSubjectCode.FROZEN,
                    LedgerSubjectCode.AUTHORIZATION,
                    LedgerSubjectCode.CLEARING,
                    LedgerSubjectCode.SETTLEMENT,
                    LedgerSubjectCode.FEE,
                    LedgerSubjectCode.PREPAYMENT);
        }

        private LedgerDTO ledger(LedgerQuery query, LedgerSubjectCode subjectCode) {
            return new LedgerDTO()
                    .setId(ledgerId++)
                    .setGmtCreate(EVENT_TIME)
                    .setGmtModified(EVENT_TIME)
                    .setTenantId(query.getTenantId())
                    .setSubjectId(query.getSubjectId())
                    .setSubjectType(query.getSubjectType())
                    .setLedgerProfileCode("POSTING_PERIOD")
                    .setLedgerProfileVersion(1)
                    .setLedgerSubjectCode(subjectCode)
                    .setLedgerSubjectCategory(resolveSubjectCategory(subjectCode))
                    .setNormalBalanceSide(resolveNormalSide(subjectCode))
                    .setAllowNegative(Boolean.FALSE)
                    .setDebitAmount(0L)
                    .setCreditAmount(0L)
                    .setCurrency(query.getCurrency())
                    .setSettlementPolicy("RT")
                    .setCutOffTime(LocalTime.MIDNIGHT)
                    .setPeriodType(query.getPeriodType())
                    .setPeriodId(query.getPeriodId())
                    .setVersion(0);
        }

        private LedgerSubjectCategory resolveSubjectCategory(LedgerSubjectCode ledgerSubjectCode) {
            return ledgerSubjectCode == LedgerSubjectCode.LIMIT
                    ? LedgerSubjectCategory.CONTROL
                    : LedgerSubjectCategory.LIABILITY;
        }

        private EntrySide resolveNormalSide(LedgerSubjectCode ledgerSubjectCode) {
            return ledgerSubjectCode == LedgerSubjectCode.LIMIT
                    ? EntrySide.DEBIT
                    : EntrySide.CREDIT;
        }
    }

    @Configuration
    @Import(DefaultLedgerPostingAssembler.class)
    static class Config {

        @Bean
        RecordingLedgerService ledgerService() {
            return new RecordingLedgerService();
        }

        @Bean
        LedgerPostingPlanMapper ledgerPostingPlanMapper() {
            return mock(LedgerPostingPlanMapper.class);
        }

        @Bean
        LedgerEntryMapper ledgerEntryMapper() {
            return mock(LedgerEntryMapper.class);
        }

        @Bean
        LedgerTransactionMapper ledgerTransactionMapper() {
            return mock(LedgerTransactionMapper.class);
        }
    }
}

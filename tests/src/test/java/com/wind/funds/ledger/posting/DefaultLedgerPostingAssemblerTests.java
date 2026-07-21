package com.wind.funds.ledger.posting;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.request.UpdateLedgerStatusRequest;
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
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.spec.ledger.LedgerEntrySpec;
import com.wind.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
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

    @BeforeEach
    void setUpAssemblerTestData() {
        ledgerService.clear();
    }

    /**
     * 场景：自定义 RouteLegSpec 绕过 DSL 不可变模型，携带 periodType=MONTHLY 但缺 periodId。
     * 预期：posting 装配阶段明确失败，且不继续查账本。
     * 红线：assembler 不能用当前月份为非 LIFETIME 账本周期静默补齐 periodId。
     */
    @Test
    void testAssembleShouldRejectNonLifetimeLegWithoutPeriodId() {
        assertThatThrownBy(() -> assembler.assemble(
                instruction(), "FUNDS_TX_PERIOD_001", resolvedRoute(routeLeg(AccountBalancePeriodType.MONTHLY, null))))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(ledgerService.queries).isEmpty();
    }

    @Test
    void testAssembleShouldUseExplicitNonLifetimePeriodId() {
        LedgerTransactionSpec transaction = assembler.assemble(
                instruction(), "FUNDS_TX_PERIOD_002", resolvedRoute(routeLeg(AccountBalancePeriodType.MONTHLY,
                        MONTHLY_PERIOD_ID)));

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
                instruction(), "FUNDS_TX_PERIOD_003", resolvedRoute(routeLeg(AccountBalancePeriodType.LIFETIME,
                        null)));

        assertThat(transaction.getPostingPlans()).hasSize(1);
        assertTransferPostingFacts(transaction, AccountBalancePeriodType.LIFETIME,
                AccountBalancePeriodType.LIFETIME.name());
        assertThat(ledgerService.queries).hasSize(2).allSatisfy(query -> {
            assertThat(query.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
            assertThat(query.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        });
    }

    @Test
    void testAssembleLimitAdjustIncreaseShouldBalanceControlLedgerEntries() {
        LedgerTransactionSpec transaction = assembler.assemble(
                limitAdjustInstruction(), "FUNDS_TX_LIMIT_INC", limitAdjustRoute(true));

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
                limitAdjustInstruction(), "FUNDS_TX_LIMIT_DEC", limitAdjustRoute(false));

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
        RouteLegSpec leg = routeLeg(AccountBalancePeriodType.LIFETIME, null, "LEG-"
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
            assertThat(entry.getPhaseCode()).isEqualTo(LedgerPhaseCode.TRANSFER);
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
        RouteLegSpec leg = routeLeg(AccountBalancePeriodType.LIFETIME,
                null,
                "LEG-POSTING-CONTEXT-001",
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
            assertThat(entry.getPhaseCode()).isEqualTo(LedgerPhaseCode.TRANSFER);
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
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId("1")
                        .operatorType("SYSTEM")
                        .appName("funds-test")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of())
                .build();
    }

    private FundsInstructionSpec limitAdjustInstruction() {
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
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId("1")
                        .operatorType("SYSTEM")
                        .appName("funds-test")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of())
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

    private ResolvedRouteSpec limitAdjustRoute(boolean increase) {
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
                .legs(List.of(limitAdjustLeg(increase)))
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private RouteLegSpec routeLeg(AccountBalancePeriodType periodType, String periodId) {
        return routeLeg(periodType, periodId, "LEG-POSTING-PERIOD-001");
    }

    private RouteLegSpec routeLeg(AccountBalancePeriodType periodType, String periodId, String legId) {
        return routeLeg(periodType, periodId, legId, Map.of());
    }

    private RouteLegSpec routeLeg(AccountBalancePeriodType periodType,
                                  String periodId,
                                  String legId,
                                  Map<String, Object> contextVariables) {
        return new TestRouteLegSpec(
                legId,
                routeNode("source_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.SOURCE),
                routeNode("target_account", FundsSubjectType.FUNDING_ACCOUNT, RouteNodeRole.TARGET),
                periodType,
                periodId,
                LedgerBalanceEffectType.CONSUME,
                LedgerPhaseCode.TRANSFER,
                contextVariables
        );
    }

    private RouteLegSpec limitAdjustLeg(boolean increase) {
        return new TestRouteLegSpec(
                "LEG-POSTING-PERIOD-001",
                increase
                        ? routeNode("credit_account", FundsSubjectType.CREDIT_ACCOUNT,
                        LedgerSubjectCode.LIMIT, RouteNodeRole.SOURCE)
                        : routeNode("credit_account", FundsSubjectType.CREDIT_ACCOUNT,
                        LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE),
                increase
                        ? routeNode("credit_account", FundsSubjectType.CREDIT_ACCOUNT,
                        LedgerSubjectCode.AVAILABLE, RouteNodeRole.TARGET)
                        : routeNode("credit_account", FundsSubjectType.CREDIT_ACCOUNT,
                        LedgerSubjectCode.LIMIT, RouteNodeRole.TARGET),
                AccountBalancePeriodType.LIFETIME,
                null,
                increase ? LedgerBalanceEffectType.INCREASE : LedgerBalanceEffectType.DECREASE,
                LedgerPhaseCode.ADJUSTMENT,
                Map.of()
        );
    }

    private RouteNodeSpec routeNode(String subjectId,
                                    FundsSubjectType subjectType,
                                    RouteNodeRole nodeRole) {
        return routeNode(subjectId, subjectType, LedgerSubjectCode.AVAILABLE, nodeRole);
    }

    private RouteNodeSpec routeNode(String subjectId,
                                    FundsSubjectType subjectType,
                                    LedgerSubjectCode ledgerSubjectCode,
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
                .ledgerSubjectCode(ledgerSubjectCode)
                .nodeRole(nodeRole)
                .build();
    }

    private record TestRouteLegSpec(String legId,
                                    RouteNodeSpec sourceNode,
                                    RouteNodeSpec targetNode,
                                    AccountBalancePeriodType periodType,
                                    String periodId,
                                    LedgerBalanceEffectType balanceEffectType,
                                    LedgerPhaseCode phaseCode,
                                    Map<String, Object> contextVariables) implements RouteLegSpec {

        @Override
        public String getLegId() {
            return legId;
        }

        @Override
        public RouteLegType getLegType() {
            return RouteLegType.INTERNAL_TRANSFER;
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
        public LedgerBalanceEffectType getBalanceEffectType() {
            return balanceEffectType;
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return phaseCode;
        }

        @Override
        public AccountBalancePeriodType getPeriodType() {
            return periodType;
        }

        @Override
        public String getPeriodId() {
            return periodId;
        }

        @Override
        public Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return Map.of();
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return contextVariables;
        }
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
        public void updateLedgerBalance(UpdateLedgerBalanceRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateLedgerStatus(UpdateLedgerStatusRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteLedgerByIds(Long... ids) {
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
                    LedgerSubjectCode.AUTHORIZATION);
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
    }
}

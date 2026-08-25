package com.wind.funds.ledger;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerState;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPhaseSpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.funds.wallet.service.SpendControlScopeService;
import com.wind.funds.wallet.services.impl.SpendControlScopeServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static com.wind.funds.support.LedgerProjectionTestFixture.balanceEntry;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 账本交易入账编排服务事实边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        DefaultLedgerTransactionPostingServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DefaultLedgerTransactionPostingServiceImplTests extends AbstractFundsServiceTest {

    private static final String LEDGER_TRANSACTION_SN = "LT-POSTING-BOUNDARY-001";

    private static final String POSTING_PLAN_ID = "PLAN-POSTING-BOUNDARY-001";

    private static final String SOURCE_SUBJECT_ID = "posting_boundary_source";

    private static final String TARGET_SUBJECT_ID = "posting_boundary_target";

    private static final String MISMATCH_LEDGER_SUBJECT_ID = "posting_boundary_other";

    private static final String SUBJECT_TYPE = FundsSubjectType.FUNDING_ACCOUNT.name();

    private static final String SPEND_CONTROL_SCOPE_SUBJECT_TYPE = "SPEND_CONTROL_SCOPE";

    private static final String SPEND_CONTROL_SCOPE_SUBJECT_ID = "posting_boundary_budget";

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final Money TRANSACTION_AMOUNT = Money.immutable(100L, CURRENCY);

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 6, 4, 10, 0);

    private static final String MONTHLY_PERIOD_ID = "2026-06";

    private static final String COMMAND_FUNDS_TRANSACTION_SN = "FT-POSTING-COMMAND-ROOT-001";

    private static final String COMMAND_BUSINESS_SCENE = "LEDGER_POSTING_COMMAND";

    private static final String COMMAND_BUSINESS_SN = "BIZ-POSTING-COMMAND-001";

    private static final String COMMAND_ROUTE_LEG_ID = "LEG-POSTING-COMMAND-001";

    @Autowired
    private DefaultLedgerTransactionPostingServiceImpl postingService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerBalanceProjectionServiceImpl ledgerBalanceProjectionService;

    @Autowired
    private LedgerProfileCatalog ledgerProfileCatalog;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private SpendControlScopeService spendControlScopeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpPostingBoundaryTestData() {
        cleanupPostingBoundaryTestData();
    }

    @AfterEach
    void tearDownPostingBoundaryTestData() {
        cleanupPostingBoundaryTestData();
    }

    /**
     * 场景：账务计划没有任何分录。
     * 输入：POSTED 交易携带空 entries 的 posting plan。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：空账务计划不得被 LedgerTransactionService 持久化成半成品账务事实。
     */
    @Test
    void testPostShouldRejectPostingPlanWithoutEntriesBeforeLedgerFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of())))))
                .hasMessageContaining("账务计划 entries 不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：账本分录金额来自外部不可信实现，分别为空、0 和负数。
     * 输入：POSTED 交易携带非法金额分录。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：非法金额不得进入 ledger entry，也不得触发任何余额变化。
     */
    @Test
    void testPostShouldRejectInvalidEntryAmountBeforeLedgerFacts() {
        List<InvalidAmountCase> cases = List.of(
                new InvalidAmountCase(null, "账本分录金额不能为空"),
                new InvalidAmountCase(Money.immutable(0L, CURRENCY), "账本分录金额必须大于 0"),
                new InvalidAmountCase(Money.immutable(-1L, CURRENCY), "账本分录金额必须大于 0"));
        for (InvalidAmountCase invalidCase : cases) {
            LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

            assertThatThrownBy(() -> postingService.postAssembled(transaction(
                    List.of(postingPlan(List.of(
                            debitEntry(SOURCE_SUBJECT_ID, null, invalidCase.amount()),
                            creditEntry(TARGET_SUBJECT_ID, null, TRANSACTION_AMOUNT)))))))
                    .hasMessageContaining(invalidCase.expectedMessage());

            assertLedgerFactsUnchanged(jdbcTemplate, before);
        }
    }

    /**
     * 场景：posting plan 归属的账本交易流水与分录归属流水不一致。
     * 输入：POSTED 交易携带 entry.ledgerTransactionSn 指向其他交易流水。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：跨交易流水串账不得进入账本事实表。
     */
    @Test
    void testPostShouldRejectEntryTransactionSnMismatchBeforeLedgerFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        debitEntry(SOURCE_SUBJECT_ID, null, TRANSACTION_AMOUNT, "LT-OTHER-001"),
                        creditEntry(TARGET_SUBJECT_ID, null, TRANSACTION_AMOUNT)))))))
                .hasMessageContaining("账本分录交易流水与账本交易流水不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：分录没有绑定具体账本。
     * 输入：POSTED 且借贷平衡的账务计划，但 entry.ledgerId 为空。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：账本事实必须绑定明确账本，不能依赖后续投影或查询隐式推断。
     */
    @Test
    void testPostShouldRejectMissingLedgerIdBeforeLedgerFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(validPostingPlan(null)))))
                .hasMessageContaining("账本分录 ledgerId 不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：分录绑定到了存在但主体不匹配的账本。
     * 输入：entry.subjectId 为 source，但 ledgerId 指向 other 主体的 AVAILABLE 账本。
     * 输出：入账入口在事实落库和余额投影前拒绝请求，已存在账本余额保持原样。
     * 红线：ledgerId 不能作为绕过 subject、科目、币种一致性校验的捷径。
     */
    @Test
    void testPostShouldRejectEntryLedgerMismatchBeforePersistenceAndProjection() {
        Long mismatchLedgerId = createAvailableLedger(MISMATCH_LEDGER_SUBJECT_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        debitEntry(SOURCE_SUBJECT_ID, mismatchLedgerId, TRANSACTION_AMOUNT),
                        creditEntry(TARGET_SUBJECT_ID, mismatchLedgerId, TRANSACTION_AMOUNT)))))))
                .hasMessageContaining("账本分录主体与账本主体不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：分录主体匹配但绑定账本科目不匹配。
     * 输入：entry.subjectId 与 ledger.subjectId 一致，entry 声明 AVAILABLE，ledgerId 指向 FROZEN 账本。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：账本 ID 必须与分录中的科目快照一致，不能只校验主体一致。
     */
    @Test
    void testPostShouldRejectEntryLedgerSubjectCodeMismatchBeforePersistenceAndProjection() {
        Long sourceLedgerId = createLedger(
                SOURCE_SUBJECT_ID,
                SUBJECT_TYPE,
                LedgerSubjectCode.FROZEN,
                LedgerSubjectCategory.ASSET,
                Boolean.FALSE,
                CURRENCY);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        debitEntry(SOURCE_SUBJECT_ID, sourceLedgerId, TRANSACTION_AMOUNT),
                        creditEntry(TARGET_SUBJECT_ID, targetLedgerId, TRANSACTION_AMOUNT)))))))
                .hasMessageContaining("账本分录科目与账本科目不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：分录主体和科目匹配但绑定账本币种不匹配。
     * 输入：交易与分录均为 USD，ledgerId 指向同主体同科目的 CNY 账本。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：账本 ID 必须与分录币种一致，不能把跨币种账本当作同一余额桶投影。
     */
    @Test
    void testPostShouldRejectEntryLedgerCurrencyMismatchBeforePersistenceAndProjection() {
        Long sourceLedgerId = createLedger(
                SOURCE_SUBJECT_ID,
                SUBJECT_TYPE,
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                Boolean.FALSE,
                CurrencyIsoCode.CNY);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        debitEntry(SOURCE_SUBJECT_ID, sourceLedgerId, TRANSACTION_AMOUNT),
                        creditEntry(TARGET_SUBJECT_ID, targetLedgerId, TRANSACTION_AMOUNT)))))))
                .hasMessageContaining("账本分录币种与账本币种不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：分录声明本次允许负余额，但绑定账本 profile 不允许负余额。
     * 输入：entry.balanceConstraintType=ALLOW_NEGATIVE，ledger.allowNegative=false。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：分录不能越过账本 profile 授权自行放开负余额能力。
     */
    @Test
    void testPostShouldRejectAllowNegativeEntryWhenLedgerProfileDisallowsNegativeBeforeFacts() {
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        ledgerEntry(
                                SOURCE_SUBJECT_ID,
                                sourceLedgerId,
                                EntrySide.CREDIT,
                                TRANSACTION_AMOUNT,
                                LEDGER_TRANSACTION_SN,
                                LedgerSubjectCode.AVAILABLE,
                                LedgerSubjectCategory.ASSET,
                                LedgerBalanceConstraintType.ALLOW_NEGATIVE),
                        debitEntry(TARGET_SUBJECT_ID, targetLedgerId, TRANSACTION_AMOUNT)))))))
                .hasMessageContaining("账本 profile 不允许负余额");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史账本数据的科目类别与正常余额方向不一致，但分录主体、科目和币种本身都匹配。
     * 输入：AVAILABLE/ASSET 账本被异常改成 CREDIT 正常余额方向后参与入账。
     * 输出：posting 入口在账本事实和余额投影前拒绝请求。
     * 红线：入账编排不能盲信会反向计算余额的绑定账本快照。
     */
    @Test
    void testPostShouldRejectLedgerNormalBalanceSideMismatchBeforeFacts() {
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID);
        markLedgerNormalBalanceSide(sourceLedgerId, EntrySide.CREDIT);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(availableTransferPostingPlan(sourceLedgerId, targetLedgerId)))))
                .hasMessageContaining("账本科目类别与正常余额方向不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：分录绑定的账本已经被业务侧挂起。
     * 输入：源账本 status=SUSPENDED，交易和分录本身借贷平衡，posting access type 默认为 NORMAL。
     * 输出：入账入口在账务事实落库和余额投影前拒绝请求。
     * 红线：挂起账本不得继续承接普通新增交易。
     */
    @Test
    void testPostShouldRejectSuspendedLedgerForNormalPostingBeforeFacts() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        markLedgerState(sourceLedgerId, LedgerState.SUSPENDED);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(availableTransferPostingPlan(sourceLedgerId, targetLedgerId)))))
                .hasMessageContaining("账本状态不允许入账");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：VCC/预付卡业务先关闭卡，再用收口交易处理挂账或余额转出。
     * 输入：源账本 status=SUSPENDED，posting access type=CLOSING。
     * 输出：允许入账并完成余额投影。
     * 红线：SUSPENDED 只能放行显式收口入账，不能把余额关进死账。
     */
    @Test
    void testPostShouldAllowSuspendedLedgerForClosingPosting() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        markLedgerState(sourceLedgerId, LedgerState.SUSPENDED);

        postingService.postAssembled(transaction(
                List.of(postingPlan(LedgerPostingAccessType.CLOSING, List.of(
                        ledgerEntry(SOURCE_SUBJECT_ID, SUBJECT_TYPE, sourceLedgerId, EntrySide.CREDIT,
                                TRANSACTION_AMOUNT, LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE,
                                LedgerSubjectCategory.ASSET, null),
                        ledgerEntry(TARGET_SUBJECT_ID, SUBJECT_TYPE, targetLedgerId, EntrySide.DEBIT,
                                TRANSACTION_AMOUNT, LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE,
                                LedgerSubjectCategory.ASSET, null))))));

        assertThat(ledgerService.getLedgerById(sourceLedgerId).getNormalBalance()).isEqualTo(100L);
        assertThat(ledgerService.getLedgerById(sourceLedgerId).getState()).isEqualTo(LedgerState.SUSPENDED);
        assertThat(ledgerService.getLedgerById(targetLedgerId).getNormalBalance()).isEqualTo(100L);
    }

    /**
     * 场景：账本已经完成关闭后，历史链路又提交收口入账。
     * 输入：源账本 status=CLOSED，posting access type=CLOSING。
     * 输出：入账入口在账务事实落库和余额投影前拒绝请求。
     * 红线：CLOSED 是终态，不能被 closing posting 重新写入。
     */
    @Test
    void testPostShouldRejectClosedLedgerForClosingPostingBeforeFacts() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        markLedgerState(sourceLedgerId, LedgerState.CLOSED);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(LedgerPostingAccessType.CLOSING, List.of(
                        ledgerEntry(SOURCE_SUBJECT_ID, SUBJECT_TYPE, sourceLedgerId, EntrySide.CREDIT,
                                TRANSACTION_AMOUNT, LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE,
                                LedgerSubjectCategory.ASSET, null),
                        ledgerEntry(TARGET_SUBJECT_ID, SUBJECT_TYPE, targetLedgerId, EntrySide.DEBIT,
                                TRANSACTION_AMOUNT, LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE,
                                LedgerSubjectCategory.ASSET, null)))))))
                .hasMessageContaining("账本状态不允许入账");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部错误预置支出控制范围账本记录，并把它作为账本分录主体提交。
     * 输入：POSTED 交易携带 SPEND_CONTROL_SCOPE 分录，并绑定同主体同科目同币种的伪造账本。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：支出控制范围只能作为预算控制 scope 和审计上下文，不得成为核心资金账本分录主体。
     */
    @Test
    void testPostShouldRejectSpendControlScopeMoneyValueEntryBeforeLedgerFacts() {
        seedSpendControlScope(SPEND_CONTROL_SCOPE_SUBJECT_ID);
        Long budgetLedgerId = createBudgetAvailableAssetLedger(SPEND_CONTROL_SCOPE_SUBJECT_ID);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        creditEntry(SPEND_CONTROL_SCOPE_SUBJECT_ID,
                                SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                                budgetLedgerId,
                                TRANSACTION_AMOUNT),
                        debitEntry(TARGET_SUBJECT_ID, targetLedgerId, TRANSACTION_AMOUNT)))))))
                .hasMessageContaining("账本分录主体类型不允许入账");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部错误把支出控制范围包装成 LIMIT 与 AVAILABLE 额度控制调账。
     * 输入：SPEND_CONTROL_SCOPE 分录均绑定测试预置的 CONTROL 类账本记录。
     * 输出：入账入口在事实落库和余额投影前拒绝请求。
     * 红线：预算额度调整已迁移到 Spend Control Activity 和 Budget Control Projection，
     * SPEND_CONTROL_SCOPE 不得再作为任何 LedgerEntry 主体。
     */
    @Test
    void testPostShouldRejectSpendControlScopeControlEntriesBeforeLedgerFacts() {
        seedSpendControlScope(SPEND_CONTROL_SCOPE_SUBJECT_ID);
        Long budgetLimitLedgerId = createBudgetControlLedger(SPEND_CONTROL_SCOPE_SUBJECT_ID, LedgerSubjectCode.LIMIT);
        Long budgetAvailableLedgerId = createBudgetControlLedger(SPEND_CONTROL_SCOPE_SUBJECT_ID,
                LedgerSubjectCode.AVAILABLE);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(transaction(
                List.of(postingPlan(List.of(
                        debitEntry(SPEND_CONTROL_SCOPE_SUBJECT_ID,
                                SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                                budgetLimitLedgerId,
                                TRANSACTION_AMOUNT,
                                LedgerSubjectCode.LIMIT,
                                LedgerSubjectCategory.CONTROL),
                        creditEntry(SPEND_CONTROL_SCOPE_SUBJECT_ID,
                                SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                                budgetAvailableLedgerId,
                                TRANSACTION_AMOUNT,
                                LedgerSubjectCode.AVAILABLE,
                                LedgerSubjectCategory.CONTROL)))))))
                .hasMessageContaining("账本分录主体类型不允许入账");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一账本交易已经入账成功后，外部编排因重试再次提交完全相同的 LedgerTransactionSpec。
     * 输入：第一次 post 生成 ledger transaction、posting plan、entry 并更新 source / target AVAILABLE 余额；
     * 第二次使用相同 sn、相同 posting plan 和相同 entry 再次 post。
     * 输出：第二次 post 复用 LedgerTransactionService 的 existing/newlyPosted=false 幂等结果，不新增账务事实，
     * 也不再次触发余额投影。
     * 红线：重复 post 不得重复 ledger transaction、posting plan、ledger entry 或 ledger balance 变化。
     */
    @Test
    void testPostShouldNotDuplicateLedgerFactsOrBalancesForSameTransaction() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        LedgerTransactionSpec transaction = transaction(
                List.of(availableTransferPostingPlan(sourceLedgerId, targetLedgerId)));

        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        postingService.postAssembled(transaction);
        LedgerFactSnapshot afterFirstPost = ledgerFactSnapshot(jdbcTemplate);

        assertThat(afterFirstPost.transactions()).hasSize(before.transactions().size() + 1);
        assertThat(afterFirstPost.postingPlans()).hasSize(before.postingPlans().size() + 1);
        assertThat(afterFirstPost.entries()).hasSize(before.entries().size() + 2);

        postingService.postAssembled(transaction);

        assertLedgerFactsUnchanged(jdbcTemplate, afterFirstPost);
    }

    /**
     * 场景：同一账本交易流水被重复提交，但 posting plan 的余额约束发生变化。
     * 输入：交易头和借贷路径完全相同，第二次提交将源分录改为必须非负。
     * 输出：幂等摘要冲突并拒绝复用原账本交易，既有账务事实和余额保持不变。
     * 红线：账本幂等不能只比较交易头，不能把不同 posting plan 当作同一笔成功交易。
     */
    @Test
    void testPostShouldRejectSameTransactionSnWithDifferentPostingFacts() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        LedgerTransactionSpec original = transaction(
                List.of(availableTransferPostingPlan(sourceLedgerId, targetLedgerId)));
        LedgerTransactionSpec conflicting = transaction(List.of(postingPlan(List.of(
                ledgerEntry(SOURCE_SUBJECT_ID, sourceLedgerId, EntrySide.CREDIT, TRANSACTION_AMOUNT,
                        LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET,
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE),
                debitEntry(TARGET_SUBJECT_ID, targetLedgerId, TRANSACTION_AMOUNT)))));

        postingService.postAssembled(original);
        LedgerFactSnapshot afterFirstPost = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> postingService.postAssembled(conflicting))
                .hasMessageContaining("账本交易已存在但摘要不一致");
        assertLedgerFactsUnchanged(jdbcTemplate, afterFirstPost);
    }

    /**
     * 场景：周期型账本完成入账。
     * 输入：MONTHLY/2026-06 的源账户和目标账户账本、同周期账本分录。
     * 输出：LedgerEntry 持久化 periodType/periodId 周期快照。
     * 红线：分录事实不能只靠 ledgerId 间接追溯周期，避免对账、归档和重放丢失周期证据。
     */
    @Test
    void testPostShouldPersistLedgerEntryPeriodSnapshot() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID,
                AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID,
                AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID, 0L);
        LedgerTransactionSpec transaction = transaction(
                List.of(availableTransferPostingPlan(
                        sourceLedgerId, targetLedgerId, AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID)));

        postingService.postAssembled(transaction);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT period_type, period_id
                  FROM t_ledger_entry
                 WHERE ledger_transaction_sn = ?
                 ORDER BY id
                """, LEDGER_TRANSACTION_SN);
        assertThat(rows)
                .extracting(row -> row.get("PERIOD_TYPE"), row -> row.get("PERIOD_ID"))
                .containsExactly(
                        tuple(AccountBalancePeriodType.MONTHLY.name(), MONTHLY_PERIOD_ID),
                        tuple(AccountBalancePeriodType.MONTHLY.name(), MONTHLY_PERIOD_ID));
    }

    /**
     * 场景：外部编排并发重放同一账本交易入账请求。
     * 输入：两个线程使用完全相同 LedgerTransactionSpec 同时 post。
     * 输出：两个调用都完成；最终只保留一套 ledger transaction、posting plan、ledger entry 和一次余额投影。
     * 红线：并发重放不得把唯一键冲突冒泡成交易失败，也不得重复入账。
     */
    @Test
    void testPostShouldReadBackExistingLedgerTransactionForConcurrentSameTransaction() throws Exception {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        LedgerTransactionSpec transaction = transaction(
                List.of(availableTransferPostingPlan(sourceLedgerId, targetLedgerId)));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<PostAttemptResult> first = executor.submit(concurrentPostAttempt(startGate, transaction));
            Future<PostAttemptResult> second = executor.submit(concurrentPostAttempt(startGate, transaction));

            startGate.countDown();

            List<PostAttemptResult> results = List.of(first.get(), second.get());
            LedgerFactSnapshot after = ledgerFactSnapshot(jdbcTemplate);

            assertThat(results).filteredOn(PostAttemptResult::succeeded).hasSize(2);
            assertThat(after.transactions()).hasSize(before.transactions().size() + 1);
            assertThat(after.postingPlans()).hasSize(before.postingPlans().size() + 1);
            assertThat(after.entries()).hasSize(before.entries().size() + 2);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：调用方提交的资金指令与已解析路径在六个稳定身份字段之一不一致。
     * 输入：tenant、业务场景、业务号、指令类型、事件类型或交易类型发生单字段漂移。
     * 输出：Ledger 在组装和持久化前拒绝命令，账务事实和余额保持不变。
     * 红线：Ledger 不能只信任 RouteResolver 的调用顺序或任一侧自报身份。
     */
    @ParameterizedTest(name = "reject instruction/route identity mismatch: {0}")
    @ValueSource(strings = {
            "tenantId",
            "businessScene",
            "businessSn",
            "instructionType",
            "eventType",
            "transactionType"
    })
    void testPostShouldRejectInstructionRouteIdentityMismatch(String mismatchField) {
        CommandLedgerPair ledgers = prepareCommandTransferLedgers(200L);
        FundsInstructionSpec instruction = commandInstruction(
                "tenantId".equals(mismatchField) ? TENANT_ID + 1 : TENANT_ID,
                FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TRANSFER,
                DefaultFundsTransactionType.TRANSFER,
                TRANSACTION_AMOUNT,
                "businessScene".equals(mismatchField) ? "LEDGER_POSTING_COMMAND_OTHER" : COMMAND_BUSINESS_SCENE,
                "businessSn".equals(mismatchField) ? "BIZ-POSTING-COMMAND-OTHER" : COMMAND_BUSINESS_SN);
        ResolvedRouteSpec route = commandRoute(
                TENANT_ID,
                COMMAND_BUSINESS_SCENE,
                COMMAND_BUSINESS_SN,
                "instructionType".equals(mismatchField)
                        ? FundsInstructionType.AUTHORIZATION_TRANSACTION : FundsInstructionType.DIRECT_TRANSACTION,
                "eventType".equals(mismatchField)
                        ? FundsTransactionEventType.PAY : FundsTransactionEventType.TRANSFER,
                "transactionType".equals(mismatchField)
                        ? DefaultFundsTransactionType.PAY : DefaultFundsTransactionType.TRANSFER,
                TRANSACTION_AMOUNT,
                SOURCE_SUBJECT_ID,
                LedgerProfileCode.FUNDING_BASIC,
                TARGET_SUBJECT_ID,
                LedgerProfileCode.FUNDING_BASIC);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertCommandRejectedWithoutSideEffects(
                () -> postingService.post(instruction, COMMAND_FUNDS_TRANSACTION_SN, route),
                mismatchField,
                before);
        assertThat(ledgerService.getLedgerById(ledgers.sourceLedgerId()).getNormalBalance()).isEqualTo(200L);
        assertThat(ledgerService.getLedgerById(ledgers.targetLedgerId()).getNormalBalance()).isZero();
    }

    /**
     * 场景：高阶账本命令缺少租户身份。
     * 输入：instruction.tenantId 为空，route 和账本事实完整。
     * 输出：Ledger 在访问或写入账务事实前拒绝命令。
     * 红线：数据库非空约束不能替代公共资金写边界的租户校验。
     */
    @Test
    void testPostShouldRejectMissingTenantIdBeforeLedgerFacts() {
        prepareCommandTransferLedgers(200L);
        FundsInstructionSpec instruction = commandInstruction(
                null,
                FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TRANSFER,
                DefaultFundsTransactionType.TRANSFER,
                TRANSACTION_AMOUNT,
                COMMAND_BUSINESS_SCENE,
                COMMAND_BUSINESS_SN);
        ResolvedRouteSpec route = commandTransferRoute(TRANSACTION_AMOUNT, COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertCommandRejectedWithoutSideEffects(
                () -> postingService.post(instruction, COMMAND_FUNDS_TRANSACTION_SN, route),
                "tenantId",
                before);
    }

    /**
     * 场景：调用方没有提供资金动作根流水。
     * 输入：instruction 与 route 完整，但 fundsTransactionSn 为空白。
     * 输出：Ledger 在组装和持久化前拒绝命令。
     * 红线：空身份不能退化为时序 Ledger SN 或匿名幂等域。
     */
    @Test
    void testPostShouldRejectBlankFundsTransactionSnBeforeLedgerFacts() {
        prepareCommandTransferLedgers(200L);
        FundsInstructionSpec instruction = commandTransferInstruction(TRANSACTION_AMOUNT,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        ResolvedRouteSpec route = commandTransferRoute(TRANSACTION_AMOUNT, COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertCommandRejectedWithoutSideEffects(
                () -> postingService.post(instruction, " ", route),
                "fundsTransactionSn",
                before);
    }

    /**
     * 场景：调用超时后以同一动作身份和同一事实重放高阶账本命令。
     * 输入：完全相同的 instruction、fundsTransactionSn 与 route 顺序提交两次。
     * 输出：两次返回同一 persisted LedgerTransaction SN，只形成一套账务事实和一次余额变化。
     * 红线：同义重放不能依赖调用方记住首次时序 Ledger SN。
     */
    @Test
    void testPostShouldReturnSamePersistedLedgerTransactionSnForSameCommand() {
        CommandLedgerPair ledgers = prepareCommandTransferLedgers(300L);
        FundsInstructionSpec instruction = commandTransferInstruction(TRANSACTION_AMOUNT,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        ResolvedRouteSpec route = commandTransferRoute(TRANSACTION_AMOUNT, COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        String firstSn = postingService.post(instruction, COMMAND_FUNDS_TRANSACTION_SN, route);
        LedgerFactSnapshot afterFirstPost = ledgerFactSnapshot(jdbcTemplate);
        String replaySn = postingService.post(instruction, COMMAND_FUNDS_TRANSACTION_SN, route);

        assertThat(replaySn).as("same command persisted ledger transaction sn").isEqualTo(firstSn);
        assertThat(firstSn).matches("LE[0-9a-f]{48}");
        assertThat(afterFirstPost.transactions()).hasSize(before.transactions().size() + 1);
        assertThat(afterFirstPost.postingPlans()).hasSize(before.postingPlans().size() + 1);
        assertThat(afterFirstPost.entries()).hasSize(before.entries().size() + 2);
        assertLedgerFactsUnchanged(jdbcTemplate, afterFirstPost);
        assertThat(ledgerService.getLedgerById(ledgers.sourceLedgerId()).getNormalBalance()).isEqualTo(200L);
        assertThat(ledgerService.getLedgerById(ledgers.targetLedgerId()).getNormalBalance()).isEqualTo(100L);
    }

    /**
     * 场景：同一动作身份被重放，但 Money 和 route 已经改变。
     * 输入：首次以 100 USD 成功，第二次沿同 identity 提交 50 USD。
     * 输出：Ledger 以 persisted aggregate digest 冲突拒绝，原事实和余额不变。
     * 红线：稳定身份只负责定位动作，不能把异义事实吞成幂等成功。
     */
    @Test
    void testPostShouldRejectSameCommandIdentityWithDifferentFacts() {
        prepareCommandTransferLedgers(300L);
        FundsInstructionSpec originalInstruction = commandTransferInstruction(TRANSACTION_AMOUNT,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        ResolvedRouteSpec originalRoute = commandTransferRoute(TRANSACTION_AMOUNT,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        Money conflictingAmount = Money.immutable(50L, CURRENCY);
        FundsInstructionSpec conflictingInstruction = commandTransferInstruction(conflictingAmount,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        ResolvedRouteSpec conflictingRoute = commandTransferRoute(conflictingAmount,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);

        postingService.post(originalInstruction, COMMAND_FUNDS_TRANSACTION_SN, originalRoute);
        LedgerFactSnapshot afterFirstPost = ledgerFactSnapshot(jdbcTemplate);
        Throwable failure = catchThrowable(() -> postingService.post(
                conflictingInstruction, COMMAND_FUNDS_TRANSACTION_SN, conflictingRoute));

        assertThat(failure).as("same identity with different facts").isNotNull();
        assertThat(failure).hasMessageContaining("账本交易已存在但摘要不一致");
        assertLedgerFactsUnchanged(jdbcTemplate, afterFirstPost);
    }

    /**
     * 场景：两个线程并发提交同一高阶账本命令。
     * 输入：相同 instruction、root identity 与 route 同时放行。
     * 输出：两个调用返回同一 SN，最终只有一套 transaction/plan/entry 和一次余额投影。
     * 红线：并发 winner 不能迫使 loser 换一个时序 SN 重试或重复扣款。
     */
    @Test
    void testPostShouldPostOnceForConcurrentSameCommand() throws Exception {
        CommandLedgerPair ledgers = prepareCommandTransferLedgers(300L);
        FundsInstructionSpec instruction = commandTransferInstruction(TRANSACTION_AMOUNT,
                COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        ResolvedRouteSpec route = commandTransferRoute(TRANSACTION_AMOUNT, COMMAND_BUSINESS_SCENE, COMMAND_BUSINESS_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CommandPostAttemptResult> first = executor.submit(
                    concurrentCommandPostAttempt(startGate, instruction, route));
            Future<CommandPostAttemptResult> second = executor.submit(
                    concurrentCommandPostAttempt(startGate, instruction, route));

            startGate.countDown();

            List<CommandPostAttemptResult> results = List.of(first.get(), second.get());
            LedgerFactSnapshot after = ledgerFactSnapshot(jdbcTemplate);
            assertThat(results).allMatch(CommandPostAttemptResult::succeeded);
            assertThat(results.stream().map(CommandPostAttemptResult::ledgerTransactionSn).distinct().toList())
                    .as("concurrent same command ledger transaction sn")
                    .hasSize(1);
            assertThat(after.transactions()).hasSize(before.transactions().size() + 1);
            assertThat(after.postingPlans()).hasSize(before.postingPlans().size() + 1);
            assertThat(after.entries()).hasSize(before.entries().size() + 2);
            assertThat(ledgerService.getLedgerById(ledgers.sourceLedgerId()).getNormalBalance()).isEqualTo(200L);
            assertThat(ledgerService.getLedgerById(ledgers.targetLedgerId()).getNormalBalance()).isEqualTo(100L);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：同一 authorization root 下存在两个不同的 COMPLETE action，随后分别重放。
     * 输入：共享 fundsTransactionSn，但 businessSn 不同的两个完成动作。
     * 输出：两个动作生成不同稳定 SN；各自动作重放返回自己的原 SN，余额只按两个动作变化。
     * 红线：root 不是 posting action，不能把合法后继动作合并成同一账本交易。
     */
    @Test
    void testPostShouldSeparateAndReplayDifferentActionsOnSameFundsTransactionRoot() {
        String firstActionSn = "AUTH-COMPLETE-ACTION-001";
        String secondActionSn = "AUTH-COMPLETE-ACTION-002";
        seedFundingAccount(SOURCE_SUBJECT_ID, LedgerProfileCode.FUNDING_BASIC);
        seedFundingAccount(TARGET_SUBJECT_ID, LedgerProfileCode.FUNDING_MERCHANT);
        Long sourceLedgerId = createProfileLedger(
                SOURCE_SUBJECT_ID, LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE, 300L);
        Long targetLedgerId = createProfileLedger(
                TARGET_SUBJECT_ID, LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.SETTLEMENT, 0L);
        Money actionAmount = Money.immutable(40L, CURRENCY);
        FundsInstructionSpec firstInstruction = commandInstruction(
                TENANT_ID,
                FundsInstructionType.AUTHORIZATION_TRANSACTION,
                FundsTransactionEventType.COMPLETE,
                DefaultFundsTransactionType.PAY,
                actionAmount,
                "AUTHORIZATION_COMPLETE",
                firstActionSn);
        FundsInstructionSpec secondInstruction = commandInstruction(
                TENANT_ID,
                FundsInstructionType.AUTHORIZATION_TRANSACTION,
                FundsTransactionEventType.COMPLETE,
                DefaultFundsTransactionType.PAY,
                actionAmount,
                "AUTHORIZATION_COMPLETE",
                secondActionSn);
        ResolvedRouteSpec firstRoute = commandRoute(
                TENANT_ID,
                "AUTHORIZATION_COMPLETE",
                firstActionSn,
                FundsInstructionType.AUTHORIZATION_TRANSACTION,
                FundsTransactionEventType.COMPLETE,
                DefaultFundsTransactionType.PAY,
                actionAmount,
                SOURCE_SUBJECT_ID,
                LedgerProfileCode.FUNDING_BASIC,
                TARGET_SUBJECT_ID,
                LedgerProfileCode.FUNDING_MERCHANT);
        ResolvedRouteSpec secondRoute = commandRoute(
                TENANT_ID,
                "AUTHORIZATION_COMPLETE",
                secondActionSn,
                FundsInstructionType.AUTHORIZATION_TRANSACTION,
                FundsTransactionEventType.COMPLETE,
                DefaultFundsTransactionType.PAY,
                actionAmount,
                SOURCE_SUBJECT_ID,
                LedgerProfileCode.FUNDING_BASIC,
                TARGET_SUBJECT_ID,
                LedgerProfileCode.FUNDING_MERCHANT);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        String firstLedgerSn = postingService.post(firstInstruction, COMMAND_FUNDS_TRANSACTION_SN, firstRoute);
        String secondLedgerSn = postingService.post(secondInstruction, COMMAND_FUNDS_TRANSACTION_SN, secondRoute);
        String firstReplaySn = postingService.post(firstInstruction, COMMAND_FUNDS_TRANSACTION_SN, firstRoute);
        String secondReplaySn = postingService.post(secondInstruction, COMMAND_FUNDS_TRANSACTION_SN, secondRoute);
        LedgerFactSnapshot after = ledgerFactSnapshot(jdbcTemplate);

        assertThat(firstLedgerSn).isNotEqualTo(secondLedgerSn);
        assertThat(firstReplaySn).isEqualTo(firstLedgerSn);
        assertThat(secondReplaySn).isEqualTo(secondLedgerSn);
        assertThat(after.transactions()).hasSize(before.transactions().size() + 2);
        assertThat(after.postingPlans()).hasSize(before.postingPlans().size() + 2);
        assertThat(after.entries()).hasSize(before.entries().size() + 4);
        assertThat(ledgerService.getLedgerById(sourceLedgerId).getNormalBalance()).isEqualTo(220L);
        assertThat(ledgerService.getLedgerById(targetLedgerId).getNormalBalance()).isEqualTo(80L);
    }

    private LedgerTransactionSpec transaction(List<LedgerPostingPlanSpec> postingPlans) {
        return new TestLedgerTransactionSpec(postingPlans);
    }

    private LedgerPostingPlanSpec validPostingPlan(Long ledgerId) {
        return postingPlan(List.of(
                debitEntry(SOURCE_SUBJECT_ID, ledgerId, TRANSACTION_AMOUNT),
                creditEntry(TARGET_SUBJECT_ID, ledgerId, TRANSACTION_AMOUNT)));
    }

    private LedgerPostingPlanSpec availableTransferPostingPlan(Long sourceLedgerId, Long targetLedgerId) {
        return availableTransferPostingPlan(sourceLedgerId, targetLedgerId,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
    }

    private LedgerPostingPlanSpec availableTransferPostingPlan(Long sourceLedgerId,
                                                               Long targetLedgerId,
                                                               AccountBalancePeriodType periodType,
                                                               String periodId) {
        return postingPlan(List.of(
                ledgerEntry(SOURCE_SUBJECT_ID, SUBJECT_TYPE, sourceLedgerId, EntrySide.CREDIT, TRANSACTION_AMOUNT,
                        LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET,
                        null, periodType, periodId),
                ledgerEntry(TARGET_SUBJECT_ID, SUBJECT_TYPE, targetLedgerId, EntrySide.DEBIT, TRANSACTION_AMOUNT,
                        LEDGER_TRANSACTION_SN, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET,
                        null, periodType, periodId)));
    }

    private Callable<PostAttemptResult> concurrentPostAttempt(CountDownLatch startGate,
                                                              LedgerTransactionSpec transaction) {
        return () -> {
            startGate.await();
            try {
                postingService.postAssembled(transaction);
                return new PostAttemptResult(true, null);
            } catch (RuntimeException ex) {
                return new PostAttemptResult(false, ex.getMessage());
            }
        };
    }

    private CommandLedgerPair prepareCommandTransferLedgers(long sourceBalance) {
        seedFundingAccount(SOURCE_SUBJECT_ID, LedgerProfileCode.FUNDING_BASIC);
        seedFundingAccount(TARGET_SUBJECT_ID, LedgerProfileCode.FUNDING_BASIC);
        return new CommandLedgerPair(
                createProfileLedger(
                        SOURCE_SUBJECT_ID, LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE, sourceBalance),
                createProfileLedger(
                        TARGET_SUBJECT_ID, LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE, 0L));
    }

    private FundsInstructionSpec commandTransferInstruction(Money amount,
                                                            String businessScene,
                                                            String businessSn) {
        return commandInstruction(
                TENANT_ID,
                FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TRANSFER,
                DefaultFundsTransactionType.TRANSFER,
                amount,
                businessScene,
                businessSn);
    }

    private FundsInstructionSpec commandInstruction(Long tenantId,
                                                    FundsInstructionType instructionType,
                                                    FundsTransactionEventType eventType,
                                                    DefaultFundsTransactionType transactionType,
                                                    Money amount,
                                                    String businessScene,
                                                    String businessSn) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(tenantId)
                .instructionType(instructionType)
                .eventType(eventType)
                .transactionType(transactionType)
                .amount(amount)
                .originalAmount(amount)
                .exchangeRate(BigDecimal.ONE)
                .ledgerPeriodType(AccountBalancePeriodType.LIFETIME)
                .payeeLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .businessScene(businessScene)
                .businessSn(businessSn)
                .eventTime(TRANSACTION_TIME)
                .operator(WindOperatorFactory.system())
                .contextVariables(Map.of())
                .build();
    }

    private ResolvedRouteSpec commandTransferRoute(Money amount,
                                                   String businessScene,
                                                   String businessSn) {
        return commandRoute(
                TENANT_ID,
                businessScene,
                businessSn,
                FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TRANSFER,
                DefaultFundsTransactionType.TRANSFER,
                amount,
                SOURCE_SUBJECT_ID,
                LedgerProfileCode.FUNDING_BASIC,
                TARGET_SUBJECT_ID,
                LedgerProfileCode.FUNDING_BASIC);
    }

    private ResolvedRouteSpec commandRoute(Long tenantId,
                                           String businessScene,
                                           String businessSn,
                                           FundsInstructionType instructionType,
                                           FundsTransactionEventType eventType,
                                           DefaultFundsTransactionType transactionType,
                                           Money amount,
                                           String sourceSubjectId,
                                           LedgerProfileCode sourceProfile,
                                           String targetSubjectId,
                                           LedgerProfileCode targetProfile) {
        ImmutableRouteNodeSpec source = commandRouteNode(
                tenantId, sourceSubjectId, sourceProfile, RouteNodeRole.SOURCE);
        ImmutableRouteNodeSpec target = commandRouteNode(
                tenantId, targetSubjectId, targetProfile, RouteNodeRole.TARGET);
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(tenantId)
                .routeCode("LEDGER_POSTING_COMMAND_ROUTE")
                .routeVersion("v1")
                .businessScene(businessScene)
                .businessSn(businessSn)
                .instructionType(instructionType)
                .eventType(eventType)
                .transactionType(transactionType)
                .participants(List.of())
                .legs(List.of(ImmutableRouteLegSpec.builder()
                        .legId(COMMAND_ROUTE_LEG_ID)
                        .sequence(1)
                        .legType(RouteLegType.INTERNAL_TRANSFER)
                        .sourceNode(source)
                        .targetNode(target)
                        .amount(amount)
                        .originalAmount(amount)
                        .exchangeRate(BigDecimal.ONE)
                        .contextVariables(Map.of())
                        .build()))
                .resolvedAt(TRANSACTION_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteNodeSpec commandRouteNode(Long tenantId,
                                                    String subjectId,
                                                    LedgerProfileCode profileCode,
                                                    RouteNodeRole role) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .nodeRole(role)
                .subjectRef(ImmutableSubjectRef.builder()
                        .tenantId(tenantId)
                        .subjectId(subjectId)
                        .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                        .currency(CURRENCY)
                        .ledgerProfileCode(profileCode.name())
                        .build())
                .build();
    }

    private void assertCommandRejectedWithoutSideEffects(Callable<String> command,
                                                         String expectedMessage,
                                                         LedgerFactSnapshot before) {
        Throwable failure = catchThrowable(command::call);
        assertThat(failure).as("ledger posting command rejection").isNotNull();
        assertThat(failure).hasMessageContaining(expectedMessage);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private Callable<CommandPostAttemptResult> concurrentCommandPostAttempt(
            CountDownLatch startGate,
            FundsInstructionSpec instruction,
            ResolvedRouteSpec route) {
        return () -> {
            startGate.await();
            try {
                return new CommandPostAttemptResult(
                        postingService.post(instruction, COMMAND_FUNDS_TRANSACTION_SN, route), null);
            } catch (RuntimeException exception) {
                return new CommandPostAttemptResult(null, exception.getMessage());
            }
        };
    }

    private LedgerPostingPlanSpec postingPlan(List<LedgerEntrySpec> entries) {
        return postingPlan(LedgerPostingAccessType.NORMAL, entries);
    }

    private LedgerPostingPlanSpec postingPlan(LedgerPostingAccessType accessType, List<LedgerEntrySpec> entries) {
        return new TestLedgerPostingPlanSpec(List.of(new TestLedgerPostingPhaseSpec(
                LedgerPhaseCode.TRANSFER,
                entries)), accessType);
    }

    private LedgerEntrySpec debitEntry(String subjectId, Long ledgerId, Money amount) {
        return debitEntry(subjectId, ledgerId, amount, LEDGER_TRANSACTION_SN);
    }

    private LedgerEntrySpec debitEntry(String subjectId,
                                      Long ledgerId,
                                      Money amount,
                                      String ledgerTransactionSn) {
        return ledgerEntry(subjectId, ledgerId, EntrySide.DEBIT, amount, ledgerTransactionSn);
    }

    private LedgerEntrySpec creditEntry(String subjectId, Long ledgerId, Money amount) {
        return ledgerEntry(subjectId, ledgerId, EntrySide.CREDIT, amount, LEDGER_TRANSACTION_SN);
    }

    private LedgerEntrySpec creditEntry(String subjectId,
                                        String subjectType,
                                        Long ledgerId,
                                        Money amount) {
        return ledgerEntry(subjectId,
                subjectType,
                ledgerId,
                EntrySide.CREDIT,
                amount,
                LEDGER_TRANSACTION_SN,
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                null);
    }

    private LedgerEntrySpec creditEntry(String subjectId,
                                        String subjectType,
                                        Long ledgerId,
                                        Money amount,
                                        LedgerSubjectCode ledgerSubjectCode,
                                        LedgerSubjectCategory ledgerSubjectCategory) {
        return ledgerEntry(subjectId,
                subjectType,
                ledgerId,
                EntrySide.CREDIT,
                amount,
                LEDGER_TRANSACTION_SN,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                null);
    }

    private LedgerEntrySpec debitEntry(String subjectId,
                                       String subjectType,
                                       Long ledgerId,
                                       Money amount,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       LedgerSubjectCategory ledgerSubjectCategory) {
        return ledgerEntry(subjectId,
                subjectType,
                ledgerId,
                EntrySide.DEBIT,
                amount,
                LEDGER_TRANSACTION_SN,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                null);
    }

    private LedgerEntrySpec ledgerEntry(String subjectId,
                                        Long ledgerId,
                                        EntrySide entrySide,
                                        Money amount,
                                        String ledgerTransactionSn) {
        return ledgerEntry(subjectId, SUBJECT_TYPE, ledgerId, entrySide, amount, ledgerTransactionSn,
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, null,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
    }

    private LedgerEntrySpec ledgerEntry(String subjectId,
                                        Long ledgerId,
                                        EntrySide entrySide,
                                        Money amount,
                                        String ledgerTransactionSn,
                                        LedgerSubjectCode ledgerSubjectCode,
                                        LedgerSubjectCategory ledgerSubjectCategory,
                                        LedgerBalanceConstraintType balanceConstraintType) {
        return ledgerEntry(subjectId, SUBJECT_TYPE, ledgerId, entrySide, amount, ledgerTransactionSn,
                ledgerSubjectCode, ledgerSubjectCategory, balanceConstraintType,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
    }

    private LedgerEntrySpec ledgerEntry(String subjectId,
                                        String subjectType,
                                        Long ledgerId,
                                        EntrySide entrySide,
                                        Money amount,
                                        String ledgerTransactionSn,
                                        LedgerSubjectCode ledgerSubjectCode,
                                        LedgerSubjectCategory ledgerSubjectCategory,
                                        LedgerBalanceConstraintType balanceConstraintType) {
        return ledgerEntry(subjectId, subjectType, ledgerId, entrySide, amount, ledgerTransactionSn,
                ledgerSubjectCode, ledgerSubjectCategory, balanceConstraintType,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
    }

    private LedgerEntrySpec ledgerEntry(String subjectId,
                                        String subjectType,
                                        Long ledgerId,
                                        EntrySide entrySide,
                                        Money amount,
                                        String ledgerTransactionSn,
                                        LedgerSubjectCode ledgerSubjectCode,
                                        LedgerSubjectCategory ledgerSubjectCategory,
                                        LedgerBalanceConstraintType balanceConstraintType,
                                        AccountBalancePeriodType periodType,
                                        String periodId) {
        return new TestLedgerEntrySpec(
                subjectId,
                subjectType,
                ledgerId,
                entrySide,
                amount,
                ledgerTransactionSn,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                balanceConstraintType,
                periodType,
                periodId);
    }

    private Long createAvailableLedger(String subjectId) {
        return createLedger(
                subjectId,
                SUBJECT_TYPE,
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                Boolean.FALSE,
                CURRENCY);
    }

    private Long createBudgetAvailableAssetLedger(String subjectId) {
        return createLedger(
                subjectId,
                SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                Boolean.FALSE,
                CURRENCY);
    }

    private Long createBudgetControlLedger(String subjectId, LedgerSubjectCode subjectCode) {
        return createLedger(
                subjectId,
                SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                subjectCode,
                LedgerSubjectCategory.CONTROL,
                Boolean.FALSE,
                CURRENCY);
    }

    private Long createLedger(String subjectId,
                              String subjectType,
                              LedgerSubjectCode ledgerSubjectCode,
                              LedgerSubjectCategory ledgerSubjectCategory,
                              Boolean allowNegative,
                              CurrencyIsoCode currency) {
        return ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(subjectId)
                .setSubjectType(subjectType)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(ledgerSubjectCategory)
                .setNormalBalanceSide(normalBalanceSide(ledgerSubjectCode, ledgerSubjectCategory))
                .setAllowNegative(allowNegative)
                .setCurrency(currency)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
    }

    private EntrySide normalBalanceSide(LedgerSubjectCode ledgerSubjectCode,
                                        LedgerSubjectCategory ledgerSubjectCategory) {
        if (ledgerSubjectCategory != LedgerSubjectCategory.CONTROL) {
            return ledgerSubjectCategory.getNormalBalance();
        }
        return ledgerSubjectCode == LedgerSubjectCode.LIMIT ? EntrySide.DEBIT : EntrySide.CREDIT;
    }

    private void markLedgerNormalBalanceSide(Long ledgerId, EntrySide normalBalanceSide) {
        jdbcTemplate.update("UPDATE t_ledger SET normal_balance_side = ? WHERE id = ?",
                normalBalanceSide.name(),
                ledgerId);
    }

    private void markLedgerState(Long ledgerId, LedgerState state) {
        jdbcTemplate.update("UPDATE t_ledger SET status = ? WHERE id = ?", state.name(), ledgerId);
    }

    private Long createAvailableLedger(String subjectId, long initialBalance) {
        Long ledgerId = createAvailableLedger(subjectId);
        if (initialBalance != 0L) {
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledgerService.getLedgerById(ledgerId),
                    EntrySide.DEBIT,
                    initialBalance)), LedgerPostingAccessType.NORMAL);
        }
        return ledgerId;
    }

    private Long createProfileLedger(String subjectId,
                                     LedgerProfileCode profileCode,
                                     LedgerSubjectCode subjectCode,
                                     long initialBalance) {
        CreateLedgerRequest request = ledgerProfileCatalog.requiredLedgerRequests(
                        new InitializeSubjectLedgerRequest()
                                .setTenantId(TENANT_ID)
                                .setSubjectId(subjectId)
                                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                                .setCurrency(CURRENCY)
                                .setLedgerProfileCode(profileCode)
                                .setLedgerProfileVersion(1)
                                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()))
                .stream()
                .filter(candidate -> candidate.getLedgerSubjectCode() == subjectCode)
                .findFirst()
                .orElseThrow();
        Long ledgerId = ledgerService.createLedger(request);
        if (initialBalance != 0L) {
            LedgerDTO ledger = ledgerService.getLedgerById(ledgerId);
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledger,
                    ledger.getNormalBalanceSide(),
                    initialBalance)), LedgerPostingAccessType.NORMAL);
        }
        return ledgerId;
    }

    private Long createAvailableLedger(String subjectId,
                                       AccountBalancePeriodType periodType,
                                       String periodId,
                                       long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(subjectId)
                .setSubjectType(SUBJECT_TYPE)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.ASSET)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(periodType)
                .setPeriodId(periodId));
        if (initialBalance != 0L) {
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledgerService.getLedgerById(ledgerId),
                    EntrySide.DEBIT,
                    initialBalance)), LedgerPostingAccessType.NORMAL);
        }
        return ledgerId;
    }

    private void seedFundingAccount(String accountSn) {
        seedFundingAccount(accountSn, LedgerProfileCode.FUNDING_BASIC);
    }

    private void seedFundingAccount(String accountSn, LedgerProfileCode profileCode) {
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountSn);
        account.setOwnerId("owner_" + accountSn);
        account.setOwnerType(FundsAccountOwnerType.USER);
        account.setAccountType(FundingAccountType.USER_WALLET.getAccountType().name());
        account.setPlatform(Boolean.FALSE);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(profileCode);
        account.setLedgerProfileVersion(1);
        account.setState(FundsAccountState.ACTIVE);
        account.setDescription("ledger posting boundary funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void seedSpendControlScope(String spendControlScopeSn) {
        spendControlScopeService.createSpendControlScope(new CreateSpendControlScopeRequest()
                .setTenantId(TENANT_ID)
                .setSn(spendControlScopeSn)
                .setOwnerId("owner_" + spendControlScopeSn)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setScopeType(SpendRuleScopeType.SPEND_CONTROL_SCOPE.name())
                .setCurrency(CURRENCY)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name())
                .setState(FundsAccountState.ACTIVE)
                .setDescription("ledger posting boundary budget group"));
    }

    private void cleanupPostingBoundaryTestData() {
        List<String> commandLedgerTransactionSns = jdbcTemplate.queryForList("""
                SELECT sn
                FROM t_ledger_transaction
                WHERE funds_transaction_sn = ?
                """, String.class, COMMAND_FUNDS_TRANSACTION_SN);
        for (String ledgerTransactionSn : commandLedgerTransactionSns) {
            jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE ledger_transaction_sn = ?", ledgerTransactionSn);
            jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE ledger_transaction_sn = ?",
                    ledgerTransactionSn);
            jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", ledgerTransactionSn);
        }
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE ledger_transaction_sn = ?", LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE ledger_transaction_sn = ?",
                LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?, ?)",
                SOURCE_SUBJECT_ID,
                TARGET_SUBJECT_ID,
                MISMATCH_LEDGER_SUBJECT_ID,
                SPEND_CONTROL_SCOPE_SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                SOURCE_SUBJECT_ID,
                TARGET_SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_scope WHERE sn = ?", SPEND_CONTROL_SCOPE_SUBJECT_ID);
    }

    private record InvalidAmountCase(Money amount, String expectedMessage) {
    }

    private record TestLedgerTransactionSpec(List<LedgerPostingPlanSpec> postingPlans)
            implements LedgerTransactionSpec {

        private TestLedgerTransactionSpec {
            postingPlans = List.copyOf(postingPlans);
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }

        @Override
        public String getSn() {
            return LEDGER_TRANSACTION_SN;
        }

        @Override
        public FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TRANSFER;
        }

        @Override
        public Money getAmount() {
            return TRANSACTION_AMOUNT;
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-POSTING-BOUNDARY-001";
        }

        @Override
        public DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public String getBusinessScene() {
            return "LEDGER_POSTING_BOUNDARY";
        }

        @Override
        public String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
        }

        @Override
        public String getDescription() {
            return "ledger posting boundary contract";
        }

        @Override
        public List<LedgerPostingPlanSpec> getPostingPlans() {
            return postingPlans;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("traceId", "TRACE-POSTING-BOUNDARY-001");
        }
    }

    private record TestLedgerPostingPlanSpec(List<LedgerPostingPhaseSpec> postingPhases,
                                             LedgerPostingAccessType postingAccessType)
            implements LedgerPostingPlanSpec {

        private TestLedgerPostingPlanSpec {
            postingPhases = List.copyOf(postingPhases);
        }

        @Override
        public String getPlanId() {
            return POSTING_PLAN_ID;
        }

        @Override
        public String getLedgerTransactionSn() {
            return LEDGER_TRANSACTION_SN;
        }

        @Override
        public LedgerPostingIntentType getIntent() {
            return LedgerPostingIntentType.TRANSFER;
        }

        @Override
        public LedgerPostingAccessType getPostingAccessType() {
            return postingAccessType;
        }

        @Override
        public List<LedgerPostingPhaseSpec> getPostingPhases() {
            return postingPhases;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("routeTraceId", "ROUTE-POSTING-BOUNDARY-001");
        }
    }

    private record TestLedgerPostingPhaseSpec(LedgerPhaseCode phaseCode,
                                              List<LedgerEntrySpec> entries)
            implements LedgerPostingPhaseSpec {

        private TestLedgerPostingPhaseSpec {
            entries = List.copyOf(entries);
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return phaseCode;
        }

        @Override
        public List<LedgerEntrySpec> getEntries() {
            return entries;
        }
    }

    private record TestLedgerEntrySpec(String subjectId,
                                       String subjectType,
                                       Long ledgerId,
                                       EntrySide entryType,
                                       Money amount,
                                       String ledgerTransactionSn,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       LedgerSubjectCategory ledgerSubjectCategory,
                                       LedgerBalanceConstraintType balanceConstraintType,
                                       AccountBalancePeriodType periodType,
                                       String periodId)
            implements LedgerEntrySpec {

        @Override
        public String getSubjectId() {
            return subjectId;
        }

        @Override
        public String getSubjectType() {
            return subjectType;
        }

        @Override
        public LedgerSubjectCode getLedgerSubjectCode() {
            return ledgerSubjectCode;
        }

        @Override
        public LedgerSubjectCategory getLedgerSubjectCategory() {
            return ledgerSubjectCategory;
        }

        @Override
        public Long getLedgerId() {
            return ledgerId;
        }

        @Override
        public String getLedgerTransactionSn() {
            return ledgerTransactionSn;
        }

        @Override
        public EntrySide getEntryType() {
            return entryType;
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.TRANSFER;
        }

        @Override
        public LedgerPostingRole getPostingRole() {
            return LedgerPostingRole.DETAIL;
        }

        @Override
        public LedgerBalanceConstraintType getBalanceConstraintType() {
            return balanceConstraintType;
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
        public String getBusinessScene() {
            return "LEDGER_POSTING_BOUNDARY";
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-POSTING-BOUNDARY-001";
        }

        @Override
        public Money getAmount() {
            return amount;
        }

        @Override
        public Money getOriginalAmount() {
            return amount;
        }

        @Override
        public BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
        }

        @Override
        public String getDescription() {
            return "ledger posting boundary contract";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("ledgerEntrySn", "LE-POSTING-BOUNDARY-001");
        }
    }

    private record PostAttemptResult(boolean succeeded, String message) {
    }

    private record CommandLedgerPair(Long sourceLedgerId, Long targetLedgerId) {
    }

    private record CommandPostAttemptResult(String ledgerTransactionSn, String failureMessage) {

        private boolean succeeded() {
            return ledgerTransactionSn != null && failureMessage == null;
        }
    }

    @Configuration
    @Import({
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultLedgerPostingAssembler.class,
            LedgerTransactionServiceImpl.class,
            LedgerServiceImpl.class,
            SpendControlScopeServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class
    })
    static class Config {
    }
}

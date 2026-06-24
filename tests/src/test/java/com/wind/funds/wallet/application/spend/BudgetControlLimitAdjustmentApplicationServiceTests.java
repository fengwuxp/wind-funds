package com.wind.funds.wallet.application.spend;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.BudgetControlLimitAdjustmentApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.model.dto.BudgetControlLimitAdjustmentResultDTO;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.AdjustBudgetControlLimitRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.SpendControlActivityService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlActivityServiceImpl;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 预算控制额度调整应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        BudgetControlLimitAdjustmentApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BudgetControlLimitAdjustmentApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "budget_limit_adjust_credit";

    private static final String BUSINESS_SCENE = "BUDGET_LIMIT_ADJUST";

    private static final String INCREASE_BUSINESS_SN = "BUDGET_LIMIT_ADJUST_INC_001";

    private static final String REPLAY_BUSINESS_SN = "BUDGET_LIMIT_ADJUST_REPLAY_001";

    private static final String RESERVED_BUSINESS_SN = "BUDGET_LIMIT_ADJUST_RESERVED_001";

    private static final String DECREASE_BUSINESS_SN = "BUDGET_LIMIT_ADJUST_DEC_001";

    private static final String CONSUMED_BUSINESS_SN = "BUDGET_LIMIT_ADJUST_CONSUMED_001";

    private static final String REFUND_BUSINESS_SN = "BUDGET_LIMIT_ADJUST_REFUND_001";

    private static final String BUDGET_GROUP_SN = "budget_control_limit_scope";

    private static final String SPEND_RULE_ID = "sr_budget_limit_monthly";

    private static final String SPEND_RULE_VERSION = "2026-06-21.1";

    private static final String OWNER_ID = "budget_limit_adjust_owner";

    private static final String OPERATOR_ID = "ops_budget_admin";

    private static final String REASON_CODE = "BUDGET_RULE_LIMIT_CHANGE";

    private static final String AUDIT_REFERENCE_SN = "approval_budget_limit_001";

    private static final String LIMIT_INCREASE_ACTIVITY_SN = "budget_limit_increase_001";

    private static final String LIMIT_REPLAY_ACTIVITY_SN = "budget_limit_replay_001";

    private static final String LIMIT_DECREASE_ACTIVITY_SN = "budget_limit_decrease_001";

    private static final String RESERVED_ACTIVITY_SN = "budget_limit_reserved_001";

    private static final String CONSUMED_ACTIVITY_SN = "budget_limit_consumed_001";

    private static final String REFUND_COMPENSATED_ACTIVITY_SN = "budget_limit_refund_compensated_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private SpendControlActivityService spendControlActivityService;

    @Autowired
    private BudgetControlLimitAdjustmentApplicationService budgetControlLimitAdjustmentApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：运营调增预算控制额度。
     * 输入：预算组、Spend Rule、目标信用账户、规则版本、原因、操作者和审批引用完整。
     * 输出：写入额度调增控制活动，并从控制活动派生预算控制投影。
     * 红线：预算额度调整不创建资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testIncreaseLimitShouldRecordControlActivityAndProjectionWithoutFundsSideEffect() {
        prepareBudgetControlLimitAdjustmentData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        BudgetControlLimitAdjustmentResultDTO result = budgetControlLimitAdjustmentApplicationService.adjustLimit(
                adjustRequest(LIMIT_INCREASE_ACTIVITY_SN, INCREASE_BUSINESS_SN, true,
                        "sha256:budget-limit-increase"));

        assertThat(result.getActivitySn()).isEqualTo(LIMIT_INCREASE_ACTIVITY_SN);
        assertThat(result.getActivityType()).isEqualTo(SpendControlActivityType.LIMIT_INCREASED);
        assertThat(result.getBudgetGroupSn()).isEqualTo(BUDGET_GROUP_SN);
        assertThat(result.getTargetAccountId()).isEqualTo(targetAccountId());
        assertThat(result.getAmount()).isEqualTo(100L);
        assertThat(result.getIncrease()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(REASON_CODE);
        assertThat(result.getOperatorId()).isEqualTo(OPERATOR_ID);
        assertThat(result.getAuditReferenceSn()).isEqualTo(AUDIT_REFERENCE_SN);

        BudgetControlProjectionDTO projection = result.getProjection();
        assertThat(projection.getLimitIncreasedAmount()).isEqualTo(100L);
        assertThat(projection.getLimitDecreasedAmount()).isZero();
        assertThat(projection.getLimitAmount()).isEqualTo(100L);
        assertThat(projection.getReservedAmount()).isZero();
        assertThat(projection.getConsumedAmount()).isZero();
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getAvailableControlAmount()).isEqualTo(100L);
        assertThat(projection.getLastActivitySn()).isEqualTo(LIMIT_INCREASE_ACTIVITY_SN);

        List<SpendControlActivityDTO> activities = spendControlActivityService.queryActivities(
                new SpendControlActivityQuery()
                        .setTenantId(TENANT_ID)
                        .setActivitySn(LIMIT_INCREASE_ACTIVITY_SN));
        assertThat(activities).singleElement()
                .satisfies(activity -> {
                    assertThat(activity.getActivityType()).isEqualTo(SpendControlActivityType.LIMIT_INCREASED);
                    assertThat(activity.getInstrumentSn()).isNull();
                    assertThat(activity.getAction()).isNull();
                    assertThat(activity.getReasonCode()).isEqualTo(REASON_CODE);
                    assertThat(activity.getOperatorId()).isEqualTo(OPERATOR_ID);
                    assertThat(activity.getAuditReferenceSn()).isEqualTo(AUDIT_REFERENCE_SN);
                });
        assertNoTransactionFacts(INCREASE_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一预算额度调整流水按相同摘要重放。
     * 输入：同一 tenantId + activitySn + activityDigest 重复提交。
     * 输出：返回既有控制活动和同一投影，不新增重复记录。
     * 红线：幂等重放不得创建任何资金事实。
     */
    @Test
    void testAdjustLimitShouldReturnExistingWhenSameDigestReplayed() {
        prepareBudgetControlLimitAdjustmentData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        AdjustBudgetControlLimitRequest request = adjustRequest(LIMIT_REPLAY_ACTIVITY_SN, REPLAY_BUSINESS_SN,
                true, "sha256:budget-limit-replay");

        BudgetControlLimitAdjustmentResultDTO first = budgetControlLimitAdjustmentApplicationService.adjustLimit(
                request);
        BudgetControlLimitAdjustmentResultDTO replayed = budgetControlLimitAdjustmentApplicationService.adjustLimit(
                request);

        assertThat(replayed.getActivityId()).isEqualTo(first.getActivityId());
        assertThat(replayed.getActivityDigest()).isEqualTo(first.getActivityDigest());
        assertThat(activityCount(LIMIT_REPLAY_ACTIVITY_SN)).isOne();
        assertThat(replayed.getProjection().getLimitAmount()).isEqualTo(100L);
        assertNoTransactionFacts(REPLAY_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一预算额度调整流水按不同摘要重放。
     * 输入：同一 tenantId + activitySn 但 activityDigest 不同。
     * 输出：拒绝写入，已有控制活动保持不变。
     * 红线：异摘要冲突不得创建资金交易、route、posting、LedgerEntry、账本交易或余额投影。
     */
    @Test
    void testAdjustLimitShouldRejectDigestConflictWithoutFundsSideEffect() {
        prepareBudgetControlLimitAdjustmentData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        budgetControlLimitAdjustmentApplicationService.adjustLimit(adjustRequest(LIMIT_REPLAY_ACTIVITY_SN,
                REPLAY_BUSINESS_SN, true, "sha256:budget-limit-replay"));

        assertThatThrownBy(() -> budgetControlLimitAdjustmentApplicationService.adjustLimit(
                adjustRequest(LIMIT_REPLAY_ACTIVITY_SN, REPLAY_BUSINESS_SN, true,
                        "sha256:budget-limit-replay-conflict")))
                .hasMessageContaining("控制活动流水已存在但摘要不一致");

        assertThat(activityCount(LIMIT_REPLAY_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(REPLAY_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：预算额度调减低于已占用控制金额。
     * 输入：预算先调增 100，再预占 60，随后尝试调减 50。
     * 输出：调减被拒绝。
     * 红线：调减不能破坏已授权或已预占控制证据，失败路径不新增任何资金事实。
     */
    @Test
    void testDecreaseLimitShouldRejectWhenAdjustedLimitBelowOccupiedControlAmount() {
        prepareBudgetControlLimitAdjustmentData();
        budgetControlLimitAdjustmentApplicationService.adjustLimit(adjustRequest(LIMIT_INCREASE_ACTIVITY_SN,
                INCREASE_BUSINESS_SN, true, "sha256:budget-limit-increase"));
        spendControlActivityService.recordActivity(reservedRequest());
        LedgerFactSnapshot beforeReject = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> budgetControlLimitAdjustmentApplicationService.adjustLimit(
                adjustRequest(LIMIT_DECREASE_ACTIVITY_SN, DECREASE_BUSINESS_SN, false,
                        "sha256:budget-limit-decrease").setAmount(50L)))
                .hasMessageContaining("预算控制额度调减不能低于已使用或已占用控制金额");

        assertThat(activityCount(LIMIT_DECREASE_ACTIVITY_SN)).isZero();
        assertNoTransactionFacts(DECREASE_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeReject);
    }

    /**
     * 场景：预算控制额度投影同时存在额度、占用、消耗和退款补偿。
     * 输入：预算额度 100，先预占 60，再消费 60，随后退款补偿 40。
     * 输出：投影展示净消费 20、未终局控制占用 40、可用控制额度 40。
     * 红线：预算控制投影只解释 Spend Rule 控制额度，不生成资金交易、route、posting、LedgerEntry 或账本余额投影。
     */
    @Test
    void testProjectionShouldDeductNetConsumedAndOccupiedAmountFromAvailableControlLimit() {
        prepareBudgetControlLimitAdjustmentData();
        budgetControlLimitAdjustmentApplicationService.adjustLimit(adjustRequest(LIMIT_INCREASE_ACTIVITY_SN,
                INCREASE_BUSINESS_SN, true, "sha256:budget-limit-increase"));
        spendControlActivityService.recordActivity(reservedRequest());
        spendControlActivityService.recordActivity(consumedRequest());
        spendControlActivityService.recordActivity(refundCompensatedRequest());
        LedgerFactSnapshot beforeQuery = ledgerFactSnapshot(jdbcTemplate);

        BudgetControlProjectionDTO projection =
                spendControlActivityService.getBudgetControlProjection(projectionQuery());

        assertThat(projection.getLimitAmount()).isEqualTo(100L);
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(20L);
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(projection.getAvailableControlAmount()).isEqualTo(40L);
        assertNoTransactionFacts(CONSUMED_BUSINESS_SN);
        assertNoTransactionFacts(REFUND_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeQuery);
    }

    /**
     * 场景：预算额度调减低于已使用和已占用控制金额之和。
     * 输入：预算额度 100，预占 60 后消费 60，再尝试调减 50。
     * 输出：调减被拒绝。
     * 红线：已终局消费的控制金额同样占用预算额度，不能只看未释放占用。
     */
    @Test
    void testDecreaseLimitShouldRejectWhenAdjustedLimitBelowConsumedAndOccupiedControlAmount() {
        prepareBudgetControlLimitAdjustmentData();
        budgetControlLimitAdjustmentApplicationService.adjustLimit(adjustRequest(LIMIT_INCREASE_ACTIVITY_SN,
                INCREASE_BUSINESS_SN, true, "sha256:budget-limit-increase"));
        spendControlActivityService.recordActivity(reservedRequest());
        spendControlActivityService.recordActivity(consumedRequest());
        LedgerFactSnapshot beforeReject = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> budgetControlLimitAdjustmentApplicationService.adjustLimit(
                adjustRequest(LIMIT_DECREASE_ACTIVITY_SN, DECREASE_BUSINESS_SN, false,
                        "sha256:budget-limit-decrease").setAmount(50L)))
                .hasMessageContaining("预算控制额度调减不能低于已使用或已占用控制金额");

        assertThat(activityCount(LIMIT_DECREASE_ACTIVITY_SN)).isZero();
        assertNoTransactionFacts(DECREASE_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeReject);
    }

    @BeforeEach
    void setUpBudgetControlLimitAdjustmentTestData() {
        cleanupBudgetControlLimitAdjustmentTestData();
    }

    @AfterEach
    void tearDownBudgetControlLimitAdjustmentTestData() {
        cleanupBudgetControlLimitAdjustmentTestData();
    }

    private void prepareBudgetControlLimitAdjustmentData() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
    }

    private AdjustBudgetControlLimitRequest adjustRequest(String activitySn,
                                                          String businessSn,
                                                          boolean increase,
                                                          String activityDigest) {
        return new AdjustBudgetControlLimitRequest()
                .setTenantId(TENANT_ID)
                .setActivitySn(activitySn)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setBudgetGroupSn(BUDGET_GROUP_SN)
                .setTargetAccountId(targetAccountId())
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setIncrease(increase)
                .setReasonCode(REASON_CODE)
                .setOperatorId(OPERATOR_ID)
                .setAuditReferenceSn(AUDIT_REFERENCE_SN)
                .setActivityDigest(activityDigest);
    }

    private RecordSpendControlActivityRequest reservedRequest() {
        return new RecordSpendControlActivityRequest()
                .setTenantId(TENANT_ID)
                .setActivitySn(RESERVED_ACTIVITY_SN)
                .setActivityType(SpendControlActivityType.RESERVED)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(RESERVED_BUSINESS_SN)
                .setInstrumentSn("budget_limit_adjust_card")
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setTargetAccountId(targetAccountId())
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendDecisionSn("decision_budget_limit_reserved_001")
                .setSpendDecisionResult(SpendControlDecisionResult.PASSED)
                .setSpendDecisionDigest("sha256:budget-limit-reserved-decision")
                .setBudgetGroupSn(BUDGET_GROUP_SN)
                .setActivityDigest("sha256:budget-limit-reserved");
    }

    private RecordSpendControlActivityRequest consumedRequest() {
        return reservedRequest()
                .setActivitySn(CONSUMED_ACTIVITY_SN)
                .setActivityType(SpendControlActivityType.CONSUMED)
                .setBusinessSn(CONSUMED_BUSINESS_SN)
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn("budget_limit_adjust_consumed_tx_001")
                .setActivityDigest("sha256:budget-limit-consumed");
    }

    private RecordSpendControlActivityRequest refundCompensatedRequest() {
        return reservedRequest()
                .setActivitySn(REFUND_COMPENSATED_ACTIVITY_SN)
                .setActivityType(SpendControlActivityType.REFUND_COMPENSATED)
                .setBusinessSn(REFUND_BUSINESS_SN)
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn("budget_limit_adjust_refund_tx_001")
                .setAmount(40L)
                .setActivityDigest("sha256:budget-limit-refund-compensated");
    }

    private BudgetControlProjectionQuery projectionQuery() {
        return new BudgetControlProjectionQuery()
                .setTenantId(TENANT_ID)
                .setBudgetGroupSn(BUDGET_GROUP_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setTargetAccountId(targetAccountId());
    }

    private FundsAccountId targetAccountId() {
        return FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private CreateCreditAccountRequest createCreditAccountRequest() {
        return new CreateCreditAccountRequest()
                .setSn(CREDIT_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private void cleanupBudgetControlLimitAdjustmentTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_activity WHERE business_scene = ?", BUSINESS_SCENE);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
    }

    private int activityCount(String activitySn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_spend_control_activity WHERE tenant_id = ? AND activity_sn = ?",
                Integer.class, TENANT_ID, activitySn);
    }

    private void assertNoTransactionFacts(String businessSn) {
        assertThat(countRows("t_funds_transaction", businessSn)).isZero();
        assertThat(countRows("t_funds_transaction_detail", businessSn)).isZero();
        assertThat(postingPlanCount(businessSn)).isZero();
        assertThat(countRows("t_ledger_transaction", businessSn)).isZero();
        assertThat(countRows("t_ledger_entry", businessSn)).isZero();
    }

    private Integer postingPlanCount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, businessSn);
    }

    private int countRows(String tableName, String businessSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, businessSn);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            SpendControlActivityServiceImpl.class,
            BudgetControlLimitAdjustmentApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

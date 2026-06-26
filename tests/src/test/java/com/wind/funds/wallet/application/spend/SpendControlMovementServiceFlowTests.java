package com.wind.funds.wallet.application.spend;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingConcurrencyGuard;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlMovementServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
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
 * 控制额度变动流水服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendControlMovementServiceFlowTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendControlMovementServiceFlowTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "sca_credit_account";

    private static final String SECOND_CREDIT_ACCOUNT_SN = "sca_second_credit_account";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_control_movement_card";

    private static final String PAYMENT_BINDING_SN = "spend_control_movement_binding";

    private static final String FUNDING_RELATION_SN = "spend_control_movement_funding_rel";

    private static final String OWNER_ID = "spend_control_movement_owner";

    private static final String CHANNEL_CODE = "spend_control_movement_channel";

    private static final String BUSINESS_SCENE = "SPEND_CONTROL_ACTIVITY";

    private static final String BUSINESS_SN = "SPEND_CONTROL_ACTIVITY_001";

    private static final String REJECTED_BUSINESS_SN = "SPEND_CONTROL_ACTIVITY_REJECTED_001";

    private static final String SPEND_RULE_ID = "sr_vcc_activity_daily_limit";

    private static final String SPEND_RULE_VERSION = "2026-06-20.1";

    private static final String SPEND_DECISION_SN = "decision_spend_control_movement_001";

    private static final String SPEND_DECISION_DIGEST = "sha256:spend-control-activity";

    private static final String BUDGET_GROUP_SN = "budget_spend_control_movement";

    private static final String ADMISSION_ACTIVITY_SN = "activity_admission_recorded_001";

    private static final String REJECTED_ACTIVITY_SN = "activity_rejected_recorded_001";

    private static final String RESERVED_ACTIVITY_SN = "activity_reserved_001";

    private static final String RELEASED_ACTIVITY_SN = "activity_released_001";

    private static final String SECOND_RESERVED_ACTIVITY_SN = "activity_reserved_second_account_001";

    private static final String SECOND_RELEASED_ACTIVITY_SN = "activity_released_second_account_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendControlMovementService spendControlMovementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：准入已记录类型误入控制额度变动流水写入入口。
     * 输入：准入已通过，但变动类型为 ADMISSION_RECORDED。
     * 输出：直接拒绝写入。
     * 红线：Spend Rule 准入决策证据应记录为决策记录，不得继续写入控制额度变动流水。
     */
    @Test
    void testRecordAdmissionDecisionMovementShouldRejectWithoutFundsSideEffect() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);

        assertThatThrownBy(() -> spendControlMovementService.recordMovement(
                recordRequest(decision, ADMISSION_ACTIVITY_SN, SpendControlMovementType.ADMISSION_RECORDED,
                        "sha256:activity-admission-recorded")))
                .hasMessageContaining("Spend Rule 准入决策应记录为决策记录");

        assertThat(activityCount(ADMISSION_ACTIVITY_SN)).isZero();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：拒绝已记录类型误入控制额度变动流水写入入口。
     * 输入：准入被拒绝，变动类型为 REJECTED_RECORDED。
     * 输出：直接拒绝写入。
     * 红线：拒绝原因属于 Spend Rule 决策记录，不得继续写入控制额度变动流水。
     */
    @Test
    void testRecordRejectedDecisionMovementShouldRejectWithoutFundsSideEffect() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = rejectedDecision(REJECTED_BUSINESS_SN);

        assertThatThrownBy(() -> spendControlMovementService.recordMovement(
                recordRequest(decision, REJECTED_ACTIVITY_SN, SpendControlMovementType.REJECTED_RECORDED,
                        "sha256:activity-rejected-recorded")))
                .hasMessageContaining("Spend Rule 准入决策应记录为决策记录");

        assertThat(activityCount(REJECTED_ACTIVITY_SN)).isZero();
        assertNoTransactionFacts(REJECTED_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制额度变动流水按相同摘要重放。
     * 输入：同一 tenantId + movementSn + movementDigest 重复提交。
     * 输出：返回既有变动，不新增重复记录。
     * 红线：幂等重放不得创建任何资金事实。
     */
    @Test
    void testRecordMovementShouldReturnExistingWhenSameDigestReplayed() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        RecordSpendControlMovementRequest request = recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-reserved");

        SpendControlMovementDTO first = spendControlMovementService.recordMovement(request);
        SpendControlMovementDTO replayed = spendControlMovementService.recordMovement(request);

        assertThat(replayed.getId()).isEqualTo(first.getId());
        assertThat(replayed.getMovementDigest()).isEqualTo(first.getMovementDigest());
        assertThat(activityCount(RESERVED_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制额度变动流水按不同摘要重放。
     * 输入：同一 tenantId + movementSn 但 movementDigest 不同。
     * 输出：拒绝写入，已有变动保持不变。
     * 红线：幂等冲突不得创建任何资金事实。
     */
    @Test
    void testRecordMovementShouldRejectDigestConflictWithoutFundsSideEffect() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-reserved"));

        assertThatThrownBy(() -> spendControlMovementService.recordMovement(
                recordRequest(decision, RESERVED_ACTIVITY_SN, SpendControlMovementType.RESERVED,
                        "sha256:activity-reserved-conflict")))
                .hasMessageContaining("控制额度变动流水已存在但摘要不一致");

        assertThat(activityCount(RESERVED_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制额度变动流水和摘要被不同业务语义复用。
     * 输入：已存在 RESERVED 控制额度变动，再用同一 movementSn 和 movementDigest 记录 RELEASED。
     * 输出：拒绝回放，已有变动保持原语义。
     * 红线：控制额度变动幂等不能只比摘要，关键业务字段变化必须被识别为冲突，且不得创建任何资金事实。
     */
    @Test
    void testRecordMovementShouldRejectSameDigestWithDifferentSemanticFieldsWithoutFundsSideEffect() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-reserved"));

        assertThatThrownBy(() -> spendControlMovementService.recordMovement(
                recordRequest(decision, RESERVED_ACTIVITY_SN, SpendControlMovementType.RELEASED,
                        "sha256:activity-reserved")))
                .hasMessageContaining("控制额度变动流水已存在但类型不一致");

        List<SpendControlMovementDTO> movements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery().setTenantId(TENANT_ID).setMovementSn(RESERVED_ACTIVITY_SN));
        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().getMovementType()).isEqualTo(SpendControlMovementType.RESERVED);
        assertThat(activityCount(RESERVED_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：预算控制投影从控制额度变动派生。
     * 输入：同一预算组下先记录占用，再记录释放。
     * 输出：投影展示控制占用、释放和剩余控制金额。
     * 红线：投影不是账本余额，不得创建 ledger balance、route 或交易事实。
     */
    @Test
    void testBudgetControlProjectionShouldAggregateReservedAndReleasedWithoutLedgerBalance() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, RELEASED_ACTIVITY_SN,
                SpendControlMovementType.RELEASED, "sha256:activity-released").setAmount(20L));

        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setSpendRuleId(SPEND_RULE_ID)
                        .setSpendRuleVersion(SPEND_RULE_VERSION));

        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getReleasedAmount()).isEqualTo(20L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(projection.getLastMovementSn()).isEqualTo(RELEASED_ACTIVITY_SN);
        assertThat(projection.getLastMovementAt()).isNotNull();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一预算组和 Spend Rule 下存在多个目标账户控制额度变动。
     * 输入：两个信用账户分别记录控制占用。
     * 输出：传入目标账户查询投影时，只返回该账户的控制占用。
     * 红线：预算组级投影可以汇总，但账户级投影不得把其他账户或其他卡的控制占用混入。
     */
    @Test
    void testBudgetControlProjectionShouldFilterByTargetAccountWithoutMixingAccounts() {
        prepareSpendControlMovementData();
        creditAccountService.createCreditAccount(createCreditAccountRequest().setSn(SECOND_CREDIT_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, SECOND_RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-second-account-reserved")
                .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                        FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(40L));

        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setSpendRuleId(SPEND_RULE_ID)
                        .setSpendRuleVersion(SPEND_RULE_VERSION)
                        .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));

        assertThat(projection.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isZero();
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isEqualTo(60L);
        assertThat(projection.getLastMovementSn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一预算组和 Spend Rule 下其他账户仍有可释放占用。
     * 输入：主账户已释放完毕，第二个信用账户仍有 RESERVED 控制占用，再尝试释放主账户。
     * 输出：主账户释放请求被拒绝，不借用其他账户的剩余额度。
     * 红线：释放类变动的写入上限必须按目标资金账户或信用账户隔离，不得跨账户释放控制占用。
     */
    @Test
    void testReleaseActivityShouldGuardRemainingAmountByTargetAccountWithoutMixingAccounts() {
        prepareSpendControlMovementData();
        creditAccountService.createCreditAccount(createCreditAccountRequest().setSn(SECOND_CREDIT_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, RELEASED_ACTIVITY_SN,
                SpendControlMovementType.RELEASED, "sha256:activity-released"));
        spendControlMovementService.recordMovement(recordRequest(decision, SECOND_RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:activity-second-account-reserved")
                .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                        FundsSubjectType.CREDIT_ACCOUNT)));

        assertThatThrownBy(() -> spendControlMovementService.recordMovement(
                recordRequest(decision, SECOND_RELEASED_ACTIVITY_SN, SpendControlMovementType.RELEASED,
                        "sha256:activity-second-release-for-primary")))
                .hasMessageContaining("控制释放金额超过可释放占用金额");

        assertThat(activityCount(SECOND_RELEASED_ACTIVITY_SN)).isZero();
        BudgetControlProjectionDTO primaryProjection = spendControlMovementService.getBudgetControlProjection(new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setSpendRuleId(SPEND_RULE_ID)
                        .setSpendRuleVersion(SPEND_RULE_VERSION)
                        .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(primaryProjection.getRemainingControlAmount()).isZero();
        BudgetControlProjectionDTO secondProjection = spendControlMovementService.getBudgetControlProjection(new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setSpendRuleId(SPEND_RULE_ID)
                        .setSpendRuleVersion(SPEND_RULE_VERSION)
                        .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(secondProjection.getRemainingControlAmount()).isEqualTo(60L);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：查询控制额度变动时传入非资金账户或信用账户主体。
     * 输入：预算组类型的目标主体。
     * 输出：直接拒绝查询条件。
     * 红线：预算组不能被误当成控制额度变动的资金目标主体。
     */
    @Test
    void testQueryMovementsShouldRejectUnsupportedTargetSubjectType() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(TENANT_ID)
                        .setTargetAccountId(FundsAccountId.immutable(BUDGET_GROUP_SN,
                                FundsSubjectType.BUDGET_GROUP))))
                .hasMessageContaining("控制额度变动目标只能是资金账户或信用账户");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：查询预算控制投影时传入非资金账户或信用账户主体。
     * 输入：预算组类型的目标主体。
     * 输出：直接拒绝查询条件。
     * 红线：账户级预算控制投影只能按资金账户或信用账户过滤，不能把预算组重新打开成目标主体。
     */
    @Test
    void testBudgetControlProjectionShouldRejectUnsupportedTargetSubjectType() {
        prepareSpendControlMovementData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlMovementService.getBudgetControlProjection(
                new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setTargetAccountId(FundsAccountId.immutable(BUDGET_GROUP_SN,
                                FundsSubjectType.BUDGET_GROUP))))
                .hasMessageContaining("控制额度变动目标只能是资金账户或信用账户");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendControlMovementTestData() {
        cleanupSpendControlMovementTestData();
    }

    @AfterEach
    void tearDownSpendControlMovementTestData() {
        cleanupSpendControlMovementTestData();
    }

    private void prepareSpendControlMovementData() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
    }

    private SpendControlAdmissionDecisionDTO admittedDecision(String businessSn) {
        return decision(businessSn, SpendControlDecisionResult.PASSED, null);
    }

    private SpendControlAdmissionDecisionDTO rejectedDecision(String businessSn) {
        return decision(businessSn, SpendControlDecisionResult.REJECTED, "超过单卡单日授权限额");
    }

    private SpendControlAdmissionDecisionDTO decision(String businessSn,
                                                      SpendControlDecisionResult decisionResult,
                                                      String rejectReason) {
        return new SpendControlAdmissionDecisionDTO()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setAdmitted(decisionResult == SpendControlDecisionResult.PASSED)
                .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT))
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setSpendDecisionResult(decisionResult)
                .setSpendDecisionDigest(SPEND_DECISION_DIGEST)
                .setBudgetGroupSn(BUDGET_GROUP_SN)
                .setRejectReason(rejectReason);
    }

    private RecordSpendControlMovementRequest recordRequest(SpendControlAdmissionDecisionDTO decision,
                                                            String movementSn,
                                                            SpendControlMovementType movementType,
                                                            String movementDigest) {
        return new RecordSpendControlMovementRequest()
                .setTenantId(decision.getTenantId())
                .setMovementSn(movementSn)
                .setMovementType(movementType)
                .setBusinessScene(decision.getBusinessScene())
                .setBusinessSn(decision.getBusinessSn())
                .setInstrumentSn(decision.getInstrumentSn())
                .setAction(decision.getAction())
                .setTargetAccountId(decision.getTargetAccountId())
                .setAmount(decision.getAmount())
                .setCurrency(decision.getCurrency())
                .setSpendRuleId(decision.getSpendRuleId())
                .setSpendRuleVersion(decision.getSpendRuleVersion())
                .setSpendDecisionSn(decision.getSpendDecisionSn())
                .setSpendDecisionResult(decision.getSpendDecisionResult())
                .setSpendDecisionDigest(decision.getSpendDecisionDigest())
                .setBudgetGroupSn(decision.getBudgetGroupSn())
                .setRejectReason(decision.getRejectReason())
                .setMovementDigest(movementDigest);
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

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest() {
        return new CreatePaymentInstrumentRequest()
                .setSn(PAYMENT_INSTRUMENT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setInstrumentDirection(PaymentInstrumentDirection.PAYMENT)
                .setInstrumentNo("****1357")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_spend_control_movement_1357")
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(PAYMENT_BINDING_SN)
                .setRequestSn(PAYMENT_BINDING_SN + "_create")
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setSubjectId(CREDIT_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setSn(FUNDING_RELATION_SN)
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(CREDIT_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setPriority(10)
                .setDefaultRelation(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private void cleanupSpendControlMovementTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn = ?", FUNDING_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn = ?",
                PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", SECOND_CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", SECOND_CREDIT_ACCOUNT_SN);
    }

    private int activityCount(String movementSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_spend_control_movement WHERE tenant_id = ? AND movement_sn = ?",
                Integer.class, TENANT_ID, movementSn);
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
            PaymentInstrumentBindingConcurrencyGuard.class,
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            SpendControlMovementServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

package com.wind.funds.wallet.application.spend;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlActivityApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
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
 * 支出控制活动应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendControlActivityApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendControlActivityApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "sca_credit_account";

    private static final String SECOND_CREDIT_ACCOUNT_SN = "sca_second_credit_account";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_control_activity_card";

    private static final String PAYMENT_BINDING_SN = "spend_control_activity_binding";

    private static final String FUNDING_RELATION_SN = "spend_control_activity_funding_rel";

    private static final String OWNER_ID = "spend_control_activity_owner";

    private static final String CHANNEL_CODE = "spend_control_activity_channel";

    private static final String BUSINESS_SCENE = "SPEND_CONTROL_ACTIVITY";

    private static final String BUSINESS_SN = "SPEND_CONTROL_ACTIVITY_001";

    private static final String REJECTED_BUSINESS_SN = "SPEND_CONTROL_ACTIVITY_REJECTED_001";

    private static final String SPEND_RULE_ID = "sr_vcc_activity_daily_limit";

    private static final String SPEND_RULE_VERSION = "2026-06-20.1";

    private static final String SPEND_DECISION_SN = "decision_spend_control_activity_001";

    private static final String SPEND_DECISION_DIGEST = "sha256:spend-control-activity";

    private static final String BUDGET_GROUP_SN = "budget_spend_control_activity";

    private static final String ADMISSION_ACTIVITY_SN = "activity_admission_recorded_001";

    private static final String REJECTED_ACTIVITY_SN = "activity_rejected_recorded_001";

    private static final String RESERVED_ACTIVITY_SN = "activity_reserved_001";

    private static final String RELEASED_ACTIVITY_SN = "activity_released_001";

    private static final String SECOND_RESERVED_ACTIVITY_SN = "activity_reserved_second_account_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendControlAdmissionApplicationService spendControlAdmissionApplicationService;

    @Autowired
    private SpendControlActivityApplicationService spendControlActivityApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：Spend Rule 准入通过后记录准入活动。
     * 输入：支付工具、目标信用账户、规则版本和决策摘要完整。
     * 输出：控制活动持久化并可按业务流水查询。
     * 红线：控制活动记录不得创建资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testRecordAdmissionActivityShouldPersistActivityWithoutFundsSideEffect() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);

        SpendControlActivityDTO activity = spendControlActivityApplicationService.recordActivity(
                recordRequest(decision, ADMISSION_ACTIVITY_SN, SpendControlActivityType.ADMISSION_RECORDED,
                        "sha256:activity-admission-recorded"));

        assertThat(activity.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(activity.getActivitySn()).isEqualTo(ADMISSION_ACTIVITY_SN);
        assertThat(activity.getActivityType()).isEqualTo(SpendControlActivityType.ADMISSION_RECORDED);
        assertThat(activity.getBusinessScene()).isEqualTo(BUSINESS_SCENE);
        assertThat(activity.getBusinessSn()).isEqualTo(BUSINESS_SN);
        assertThat(activity.getInstrumentSn()).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(activity.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(activity.getSpendRuleId()).isEqualTo(SPEND_RULE_ID);
        assertThat(activity.getSpendDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(activity.getBudgetGroupSn()).isEqualTo(BUDGET_GROUP_SN);

        List<SpendControlActivityDTO> activities = spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery()
                        .setTenantId(TENANT_ID)
                        .setBusinessScene(BUSINESS_SCENE)
                        .setBusinessSn(BUSINESS_SN)
                        .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(activities).extracting(SpendControlActivityDTO::getActivitySn)
                .containsExactly(ADMISSION_ACTIVITY_SN);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：Spend Rule 准入拒绝后记录拒绝活动。
     * 输入：拒绝决策、拒绝原因、目标账户和规则证据完整。
     * 输出：拒绝活动持久化。
     * 红线：业务拒绝留痕不得创建交易、route、posting、LedgerEntry 或余额投影事实。
     */
    @Test
    void testRecordRejectedActivityShouldPersistReasonWithoutFundsSideEffect() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = rejectedDecision(REJECTED_BUSINESS_SN);

        SpendControlActivityDTO activity = spendControlActivityApplicationService.recordActivity(
                recordRequest(decision, REJECTED_ACTIVITY_SN, SpendControlActivityType.REJECTED_RECORDED,
                        "sha256:activity-rejected-recorded"));

        assertThat(activity.getActivitySn()).isEqualTo(REJECTED_ACTIVITY_SN);
        assertThat(activity.getActivityType()).isEqualTo(SpendControlActivityType.REJECTED_RECORDED);
        assertThat(activity.getSpendDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(activity.getRejectReason()).isEqualTo("超过单卡单日授权限额");
        assertNoTransactionFacts(REJECTED_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制活动流水按相同摘要重放。
     * 输入：同一 tenantId + activitySn + activityDigest 重复提交。
     * 输出：返回既有活动，不新增重复记录。
     * 红线：幂等重放不得创建任何资金事实。
     */
    @Test
    void testRecordActivityShouldReturnExistingWhenSameDigestReplayed() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        RecordSpendControlActivityRequest request = recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:activity-reserved");

        SpendControlActivityDTO first = spendControlActivityApplicationService.recordActivity(request);
        SpendControlActivityDTO replayed = spendControlActivityApplicationService.recordActivity(request);

        assertThat(replayed.getId()).isEqualTo(first.getId());
        assertThat(replayed.getActivityDigest()).isEqualTo(first.getActivityDigest());
        assertThat(activityCount(RESERVED_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制活动流水按不同摘要重放。
     * 输入：同一 tenantId + activitySn 但 activityDigest 不同。
     * 输出：拒绝写入，已有活动保持不变。
     * 红线：幂等冲突不得创建任何资金事实。
     */
    @Test
    void testRecordActivityShouldRejectDigestConflictWithoutFundsSideEffect() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:activity-reserved"));

        assertThatThrownBy(() -> spendControlActivityApplicationService.recordActivity(
                recordRequest(decision, RESERVED_ACTIVITY_SN, SpendControlActivityType.RESERVED,
                        "sha256:activity-reserved-conflict")))
                .hasMessageContaining("控制活动流水已存在但摘要不一致");

        assertThat(activityCount(RESERVED_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制活动流水和摘要被不同业务语义复用。
     * 输入：已存在 RESERVED 控制活动，再用同一 activitySn 和 activityDigest 记录 RELEASED。
     * 输出：拒绝回放，已有活动保持原语义。
     * 红线：控制活动幂等不能只比摘要，关键业务字段变化必须被识别为冲突，且不得创建任何资金事实。
     */
    @Test
    void testRecordActivityShouldRejectSameDigestWithDifferentSemanticFieldsWithoutFundsSideEffect() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:activity-reserved"));

        assertThatThrownBy(() -> spendControlActivityApplicationService.recordActivity(
                recordRequest(decision, RESERVED_ACTIVITY_SN, SpendControlActivityType.RELEASED,
                        "sha256:activity-reserved")))
                .hasMessageContaining("控制活动流水已存在但类型不一致");

        List<SpendControlActivityDTO> activities = spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery().setTenantId(TENANT_ID).setActivitySn(RESERVED_ACTIVITY_SN));
        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().getActivityType()).isEqualTo(SpendControlActivityType.RESERVED);
        assertThat(activityCount(RESERVED_ACTIVITY_SN)).isOne();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：预算控制投影从控制活动派生。
     * 输入：同一预算组下先记录占用，再记录释放。
     * 输出：投影展示控制占用、释放和剩余控制金额。
     * 红线：投影不是账本余额，不得创建 ledger balance、route 或交易事实。
     */
    @Test
    void testBudgetControlProjectionShouldAggregateReservedAndReleasedWithoutLedgerBalance() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:activity-reserved"));
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RELEASED_ACTIVITY_SN,
                SpendControlActivityType.RELEASED, "sha256:activity-released").setAmount(20L));

        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setSpendRuleId(SPEND_RULE_ID)
                        .setSpendRuleVersion(SPEND_RULE_VERSION));

        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getReleasedAmount()).isEqualTo(20L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(projection.getLastActivitySn()).isEqualTo(RELEASED_ACTIVITY_SN);
        assertThat(projection.getLastActivityAt()).isNotNull();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一预算组和 Spend Rule 下存在多个目标账户控制活动。
     * 输入：两个信用账户分别记录控制占用。
     * 输出：传入目标账户查询投影时，只返回该账户的控制占用。
     * 红线：预算组级投影可以汇总，但账户级投影不得把其他账户或其他卡的控制占用混入。
     */
    @Test
    void testBudgetControlProjectionShouldFilterByTargetAccountWithoutMixingAccounts() {
        prepareSpendControlActivityData();
        creditAccountService.createCreditAccount(createCreditAccountRequest().setSn(SECOND_CREDIT_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlAdmissionDecisionDTO decision = admittedDecision(BUSINESS_SN);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:activity-reserved"));
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, SECOND_RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:activity-second-account-reserved")
                .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                        FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(40L));

        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
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
        assertThat(projection.getLastActivitySn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：查询控制活动时传入非资金账户或信用账户主体。
     * 输入：预算组类型的目标主体。
     * 输出：直接拒绝查询条件。
     * 红线：预算组不能被误当成控制活动的资金目标主体。
     */
    @Test
    void testQueryActivitiesShouldRejectUnsupportedTargetSubjectType() {
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery()
                        .setTenantId(TENANT_ID)
                        .setTargetAccountId(FundsAccountId.immutable(BUDGET_GROUP_SN,
                                FundsSubjectType.BUDGET_GROUP))))
                .hasMessageContaining("控制活动目标只能是资金账户或信用账户");

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
        prepareSpendControlActivityData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlActivityApplicationService.getBudgetControlProjection(
                new BudgetControlProjectionQuery()
                        .setTenantId(TENANT_ID)
                        .setBudgetGroupSn(BUDGET_GROUP_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setTargetAccountId(FundsAccountId.immutable(BUDGET_GROUP_SN,
                                FundsSubjectType.BUDGET_GROUP))))
                .hasMessageContaining("控制活动目标只能是资金账户或信用账户");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendControlActivityTestData() {
        cleanupSpendControlActivityTestData();
    }

    @AfterEach
    void tearDownSpendControlActivityTestData() {
        cleanupSpendControlActivityTestData();
    }

    private void prepareSpendControlActivityData() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
    }

    private SpendControlAdmissionDecisionDTO admittedDecision(String businessSn) {
        return spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest(businessSn).setSpendDecisionResult(SpendControlDecisionResult.PASSED));
    }

    private SpendControlAdmissionDecisionDTO rejectedDecision(String businessSn) {
        return spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest(businessSn)
                        .setSpendDecisionResult(SpendControlDecisionResult.REJECTED)
                        .setRejectReason("超过单卡单日授权限额"));
    }

    private RecordSpendControlActivityRequest recordRequest(SpendControlAdmissionDecisionDTO decision,
                                                            String activitySn,
                                                            SpendControlActivityType activityType,
                                                            String activityDigest) {
        return new RecordSpendControlActivityRequest()
                .setTenantId(decision.getTenantId())
                .setActivitySn(activitySn)
                .setActivityType(activityType)
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
                .setActivityDigest(activityDigest);
    }

    private ResolveSpendControlAdmissionRequest admissionRequest(String businessSn) {
        return new ResolveSpendControlAdmissionRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(1)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setSpendDecisionDigest(SPEND_DECISION_DIGEST)
                .setBudgetGroupSn(BUDGET_GROUP_SN);
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
                .setExternalInstrumentId("tok_spend_control_activity_1357")
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(PAYMENT_BINDING_SN)
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

    private void cleanupSpendControlActivityTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_activity WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
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
            CreditAccountServiceImpl.class,
            PaymentInstrumentServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class,
            FundingResponsibilityResolutionApplicationServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl.class,
            SpendControlAdmissionApplicationServiceImpl.class,
            SpendControlActivityApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

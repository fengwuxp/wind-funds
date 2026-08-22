package com.wind.funds.wallet.application.spend;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.FundsAccountCapabilityApplicationService;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentCapabilityApplicationService;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
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
import com.wind.funds.wallet.model.request.SpendControlBusinessConfirmedRefundCompensationRequest;
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 交易结果消费控制额度变动流水服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendControlTransactionConsumptionApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendControlTransactionConsumptionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "sctc_credit_account";

    private static final String SECOND_CREDIT_ACCOUNT_SN = "sctc_second_credit_account";

    private static final String PAYMENT_INSTRUMENT_SN = "sctc_card";

    private static final String OWNER_ID = "sctc_owner";

    private static final String CHANNEL_CODE = "sctc_channel";

    private static final String BUSINESS_SCENE = "SPEND_CONTROL_TRANSACTION";

    private static final String OTHER_BUSINESS_SCENE = "OTHER_SPEND_CONTROL_TRANSACTION";

    private static final String BUSINESS_SN = "SPEND_CONTROL_TRANSACTION_001";

    private static final String OTHER_BUSINESS_SN = "SPEND_CONTROL_TRANSACTION_OTHER_001";

    private static final String CONSUME_ACTIVITY_SN = "activity_consumed_001";

    private static final String SECOND_CONSUME_ACTIVITY_SN = "activity_consumed_002";

    private static final String CROSS_SCENE_CONSUME_ACTIVITY_SN = "activity_cross_scene_consumed_001";

    private static final String CROSS_BUSINESS_SN_CONSUME_ACTIVITY_SN = "activity_cross_business_sn_consumed_001";

    private static final String REFUND_CONSUME_ACTIVITY_SN = "activity_refund_consumed_001";

    private static final String INCONSISTENT_CONSUME_ACTIVITY_SN = "activity_inconsistent_consumed_001";

    private static final String INCONSISTENT_LINKED_ACTIVITY_SN = "activity_inconsistent_linked_001";

    private static final String SMALL_CONSUME_ACTIVITY_SN = "activity_small_consumed_001";

    private static final String LARGE_CONSUME_ACTIVITY_SN = "activity_large_consumed_001";

    private static final String REPLAYED_RELEASE_ACTIVITY_SN = "activity_replayed_released_001";

    private static final String REFUND_ACTIVITY_SN = "activity_refund_compensated_001";

    private static final String SECOND_REFUND_ACTIVITY_SN = "activity_refund_compensated_002";

    private static final String INCONSISTENT_REFUND_ACTIVITY_SN = "activity_refund_inconsistent_001";

    private static final String UNLINKED_REFUND_ACTIVITY_SN = "activity_refund_unlinked_001";

    private static final String MISSING_REFERENCE_REFUND_ACTIVITY_SN = "activity_refund_missing_reference_tx_001";

    private static final String INCONSISTENT_REFERENCE_REFUND_ACTIVITY_SN =
            "activity_refund_inconsistent_reference_tx_001";

    private static final String OVER_REFERENCE_REFUND_ACTIVITY_SN = "activity_refund_over_reference_001";

    private static final String NON_REFUND_ACTIVITY_SN = "activity_non_refund_compensated_001";

    private static final String OVER_CONSUME_ACTIVITY_SN = "activity_over_consumed_001";

    private static final String RESERVED_ACTIVITY_SN = "activity_reserved_for_consume_001";

    private static final String SECOND_RESERVED_ACTIVITY_SN = "activity_reserved_for_second_account_001";

    private static final String SPEND_RULE_ID = "sr_vcc_transaction_daily_limit";

    private static final String SPEND_RULE_VERSION = "2026-06-20.1";

    private static final String SPEND_DECISION_SN = "decision_sctc_001";

    private static final String SPEND_DECISION_DIGEST = "sha256:sctc-decision";

    private static final String SPEND_CONTROL_SCOPE_SN = "budget_sctc";

    private static final String PERIOD_ID = "2026-07";

    private static final String FUNDS_TRANSACTION_SN = "funds_transaction_sctc_001";

    private static final String CROSS_SCENE_TRANSACTION_SN = "funds_transaction_sctc_cross_scene_001";

    private static final String CROSS_BUSINESS_SN_TRANSACTION_SN = "funds_transaction_sctc_cross_business_sn_001";

    private static final String SMALL_CONSUME_TRANSACTION_SN = "funds_transaction_sctc_small_consume_001";

    private static final String LARGE_CONSUME_TRANSACTION_SN = "funds_transaction_sctc_large_consume_001";

    private static final String REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_001";

    private static final String INCONSISTENT_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_inconsistent_001";

    private static final String UNLINKED_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_unlinked_001";

    private static final String MISSING_REFERENCE_REFUND_TRANSACTION_SN =
            "funds_transaction_sctc_refund_missing_reference_tx_001";

    private static final String INCONSISTENT_REFERENCE_REFUND_TRANSACTION_SN =
            "funds_transaction_sctc_refund_inconsistent_reference_tx_001";

    private static final String OVER_REFERENCE_REFUND_TRANSACTION_SN =
            "funds_transaction_sctc_refund_over_reference_001";

    private static final String TENANT_MISMATCH_ACTIVITY_SN = "activity_tenant_mismatch_001";

    private static final String LIMIT_INCREASE_ACTIVITY_SN = "activity_limit_increased_for_confirmed_refund_001";

    private static final String CONFIRMED_REFUND_ACTIVITY_SN = "activity_confirmed_refund_compensated_001";

    private static final String OVER_CONFIRMED_REFUND_ACTIVITY_SN = "activity_confirmed_refund_over_consumed_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendControlMovementService spendControlMovementService;

    @Autowired
    private SpendControlTransactionConsumptionApplicationService spendControlTransactionConsumptionApplicationService;

    @Autowired
    private FundsAccountCapabilityApplicationService fundsAccountCapabilityApplicationService;

    @Autowired
    private PaymentInstrumentCapabilityApplicationService paymentInstrumentCapabilityApplicationService;

    @Autowired
    private FundsTransactionQueryService fundsTransactionQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：资金交易成功后消费已预留的 Spend Rule 控制额度变动。
     * 输入：已有 RESERVED 控制额度变动和已存在的成功资金交易事实。
     * 输出：记录 CONSUMED 控制额度变动，回链原控制额度变动和原交易流水，并更新预算控制投影消费金额。
     * 红线：消费控制额度变动不得创建资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testConsumeReservedControlActivityShouldRecordConsumedWithoutFundsSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlMovementDTO activity = spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));

        assertThat(activity.getMovementSn()).isEqualTo(CONSUME_ACTIVITY_SN);
        assertThat(activity.getMovementType()).isEqualTo(SpendControlMovementType.CONSUMED);
        assertThat(activity.getOriginalMovementSn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(activity.getTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(activity.getControlScopeId()).isEqualTo(SPEND_CONTROL_SCOPE_SN);
        assertThat(activity.getPeriodId()).isEqualTo(PERIOD_ID);
        assertThat(activity.getAmount()).isEqualTo(60L);

        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(60L);
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastMovementSn()).isEqualTo(CONSUME_ACTIVITY_SN);

        assertThat(queryActivity(CONSUME_ACTIVITY_SN).getOriginalMovementSn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(queryActivity(CONSUME_ACTIVITY_SN).getTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        SpendControlMovementDTO replayed = spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        assertThat(replayed.getId()).isEqualTo(activity.getId());
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：控制额度消费请求租户与当前线程租户不一致。
     * 输入：当前线程租户为 1，请求 tenantId 为 2。
     * 输出：应用层入口直接拒绝，不写控制额度变动、资金交易或账本事实。
     * 红线：交易结果消费、释放或退款补偿写控制事实前必须守住租户边界。
     */
    @Test
    void testConsumeShouldRejectTenantMismatchWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(TENANT_MISMATCH_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-tenant-mismatch").setTenantId(TENANT_ID + 1)))
                .hasMessageContaining("控制额度变动 tenantId 与当前租户不一致");

        assertThat(activityCount(TENANT_MISMATCH_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：VCC 退款找不到原控制流水，但业务侧确认支付工具、周期和金额可以补偿。
     * 输入：当前周期已有 100 控制额度、100 控制占用、60 控制消费，业务确认补偿 20。
     * 输出：记录 REFUND_COMPENSATED 控制补偿流水，不要求原控制流水或资金交易流水。
     * 红线：业务确认型控制补偿不得创建资金交易、route、posting、LedgerEntry 或账本余额投影事实。
     */
    @Test
    void testCompensateBusinessConfirmedRefundShouldRecordRefundCompensationWithoutOriginalMovement() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(limitIncreaseRequest());
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
        spendControlMovementService.recordMovement(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlMovementDTO activity =
                spendControlTransactionConsumptionApplicationService.compensateBusinessConfirmedRefund(
                        confirmedRefundRequest(CONFIRMED_REFUND_ACTIVITY_SN).setAmount(20L));

        assertThat(activity.getMovementSn()).isEqualTo(CONFIRMED_REFUND_ACTIVITY_SN);
        assertThat(activity.getMovementType()).isEqualTo(SpendControlMovementType.REFUND_COMPENSATED);
        assertThat(activity.getOriginalMovementSn()).isNull();
        assertThat(activity.getTransactionSn()).isNull();
        assertThat(activity.getAuditReferenceSn()).isEqualTo("audit_sctc_confirmed_refund");
        assertThat(activity.getAmount()).isEqualTo(20L);
        assertThat(activity.getMovementDigest()).startsWith("sha256:");

        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getLimitAmount()).isEqualTo(100L);
        assertThat(projection.getReservedAmount()).isEqualTo(100L);
        assertThat(queryActivity(CONSUME_ACTIVITY_SN).getAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(40L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(projection.getAvailableControlAmount()).isEqualTo(20L);
        assertThat(activityCount(CONFIRMED_REFUND_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：业务确认型退款补偿同一变动流水被相同不可变业务事实重放。
     * 输入：同一 movementSn、同一业务事实，变更说明和上下文变量后重放。
     * 输出：复用已记录的 REFUND_COMPENSATED 控制补偿流水，资金侧生成的 movementDigest 保持一致。
     * 红线：description/contextVariables 不是不可变业务事实，不得进入控制额度变动摘要。
     */
    @Test
    void testCompensateBusinessConfirmedRefundShouldReplayWithGeneratedMovementDigest() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(limitIncreaseRequest());
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN));
        SpendControlMovementDTO first =
                spendControlTransactionConsumptionApplicationService.compensateBusinessConfirmedRefund(
                        confirmedRefundRequest(CONFIRMED_REFUND_ACTIVITY_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlMovementDTO replayed =
                spendControlTransactionConsumptionApplicationService.compensateBusinessConfirmedRefund(
                        confirmedRefundRequest(CONFIRMED_REFUND_ACTIVITY_SN)
                                .setDescription("业务确认型退款补偿重放")
                                .setContextVariables("{\"traceId\":\"ignored-on-replay\"}"));

        assertThat(replayed.getMovementSn()).isEqualTo(first.getMovementSn());
        assertThat(replayed.getMovementDigest()).isEqualTo(first.getMovementDigest());
        assertThat(activityCount(CONFIRMED_REFUND_ACTIVITY_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：业务确认型退款补偿同一变动流水被不同不可变业务事实重放。
     * 输入：同一 movementSn，变更退款补偿原因码后重放。
     * 输出：请求被拒绝，不新增控制额度变动。
     * 红线：reasonCode 属于业务确认事实，必须进入资金侧生成的 movementDigest。
     */
    @Test
    void testCompensateBusinessConfirmedRefundSameMovementSnWithChangedBusinessFactShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(limitIncreaseRequest());
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN));
        spendControlTransactionConsumptionApplicationService.compensateBusinessConfirmedRefund(
                confirmedRefundRequest(CONFIRMED_REFUND_ACTIVITY_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.compensateBusinessConfirmedRefund(
                confirmedRefundRequest(CONFIRMED_REFUND_ACTIVITY_SN).setReasonCode("MANUAL_CONFIRMED_REFUND")))
                .hasMessageContaining("控制额度变动流水已存在但摘要不一致");

        assertThat(activityCount(CONFIRMED_REFUND_ACTIVITY_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：业务确认型退款补偿金额超过当前周期净消费金额。
     * 输入：当前周期只有 60 控制消费，尝试补偿 70。
     * 输出：请求被拒绝，不写新的控制补偿流水。
     * 红线：控制补偿不能把周期净消费打成负数，也不能让可用控制额度超过周期额度。
     */
    @Test
    void testCompensateBusinessConfirmedRefundShouldRejectAmountOverNetConsumed() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(limitIncreaseRequest());
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.compensateBusinessConfirmedRefund(
                confirmedRefundRequest(OVER_CONFIRMED_REFUND_ACTIVITY_SN).setAmount(70L)))
                .hasMessageContaining("退款控制补偿金额超过当前周期净消费控制金额");

        assertThat(activityCount(OVER_CONFIRMED_REFUND_ACTIVITY_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支出控制范围和 Spend Rule 下存在多个目标账户，交易成功只消费其中一个账户的控制占用。
     * 输入：两个信用账户分别存在 RESERVED 控制额度变动，其中一个账户发生成功资金交易并记录 CONSUMED。
     * 输出：按目标账户查询预算控制投影时，只解释该账户的占用和消费。
     * 红线：交易消费服务不得让同支出控制范围下其他账户或其他卡的控制额度变动污染当前账户投影。
     */
    @Test
    void testConsumeProjectionShouldFilterTargetAccountWithoutMixingOtherAccountMovements() {
        prepareSpendControlTransactionConsumptionData();
        creditAccountService.createCreditAccount(createCreditAccountRequest().setSn(SECOND_CREDIT_ACCOUNT_SN));
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, SECOND_RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-second-account-reserved")
                .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                        FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(40L));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));

        BudgetControlProjectionDTO primaryProjection = spendControlMovementService.getBudgetControlProjection(projectionQuery()
                        .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(primaryProjection.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(primaryProjection.getReservedAmount()).isEqualTo(60L);
        assertThat(primaryProjection.getConsumedAmount()).isEqualTo(60L);
        assertThat(primaryProjection.getReleasedAmount()).isZero();
        assertThat(primaryProjection.getRemainingControlAmount()).isZero();
        assertThat(primaryProjection.getLastMovementSn()).isEqualTo(CONSUME_ACTIVITY_SN);

        BudgetControlProjectionDTO secondProjection = spendControlMovementService.getBudgetControlProjection(projectionQuery()
                        .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(secondProjection.getReservedAmount()).isEqualTo(40L);
        assertThat(secondProjection.getConsumedAmount()).isZero();
        assertThat(secondProjection.getReleasedAmount()).isZero();
        assertThat(secondProjection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(secondProjection.getLastMovementSn()).isEqualTo(SECOND_RESERVED_ACTIVITY_SN);
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方把已关闭的退款资金交易事实误用到 Spend Rule 控制消费。
     * 输入：已有 RESERVED 控制额度变动和 CLOSED REFUND 资金交易事实。
     * 输出：请求被拒绝，不写新的控制额度变动。
     * 红线：退款交易只能走退款补偿语义，不得被降级为普通成功消费，也不得写 route、posting 或账本事实。
     */
    @Test
    void testConsumeWithRefundTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_FOR_CONSUME_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 60L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(REFUND_CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-consume-refund-conflict")))
                .hasMessageContaining("控制消费不能使用退款交易事实");

        assertThat(activityCount(REFUND_CONSUME_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方把其他业务场景下的成功资金交易事实误用于当前 Spend Rule 控制消费。
     * 输入：已有 RESERVED 控制额度变动和不同业务场景的 CLOSED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的控制额度变动。
     * 红线：控制额度变动只能消费同一业务场景下的资金交易事实，不得跨业务域串用交易流水，也不得写 route、posting 或账本事实。
     */
    @Test
    void testConsumeWithDifferentBusinessSceneTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(CROSS_SCENE_TRANSACTION_SN, OTHER_BUSINESS_SCENE, BUSINESS_SN,
                DefaultFundsTransactionType.PAY, FundsTransactionState.CLOSED, 60L, CurrencyIsoCode.USD, null);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CROSS_SCENE_CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        CROSS_SCENE_TRANSACTION_SN, "sha256:sctc-cross-scene-consumed")))
                .hasMessageContaining("资金交易业务场景不一致");

        assertThat(activityCount(CROSS_SCENE_CONSUME_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(CROSS_SCENE_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方把同业务场景但不同业务流水的成功资金交易事实误用于当前 Spend Rule 控制消费。
     * 输入：已有 RESERVED 控制额度变动和同业务场景、不同业务流水的 CLOSED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的控制额度变动。
     * 红线：成功消费只能解释同一业务流水的交易事实，不得跨订单串用交易流水，也不得写 route、posting 或账本事实。
     */
    @Test
    void testConsumeWithDifferentBusinessSnTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(CROSS_BUSINESS_SN_TRANSACTION_SN, OTHER_BUSINESS_SN, 60L,
                CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CROSS_BUSINESS_SN_CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        CROSS_BUSINESS_SN_TRANSACTION_SN, "sha256:sctc-cross-business-sn-consumed")))
                .hasMessageContaining("资金交易业务流水不一致");

        assertThat(activityCount(CROSS_BUSINESS_SN_CONSUME_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(CROSS_BUSINESS_SN_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：三个线程并发消费同一控制额度变动流水且摘要相同。
     * 输入：已有 RESERVED 控制额度变动和已存在的成功资金交易事实。
     * 输出：只有一条 CONSUMED 控制额度变动，三个调用都回到同一变动事实。
     * 红线：并发唯一键冲突必须按幂等回读处理，不得抛出数据库异常或生成重复资金、route、posting、账本事实。
     */
    @Test
    void testConcurrentConsumeSameMovementSnWithSameDigestShouldReadBackExistingMovement() throws Exception {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        int versionBeforeConsumption = creditAccountVersion();
        SpendControlTransactionConsumptionApplicationService concurrentService = concurrentConsumptionService(3);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Callable<SpendControlMovementDTO> command = () -> withTenant(() -> concurrentService.consume(
                    consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                            "sha256:sctc-consumed")));

            Future<SpendControlMovementDTO> first = executor.submit(command);
            Future<SpendControlMovementDTO> second = executor.submit(command);
            Future<SpendControlMovementDTO> third = executor.submit(command);

            SpendControlMovementDTO firstActivity = first.get(10, TimeUnit.SECONDS);
            SpendControlMovementDTO secondActivity = second.get(10, TimeUnit.SECONDS);
            SpendControlMovementDTO thirdActivity = third.get(10, TimeUnit.SECONDS);
            assertThat(firstActivity.getId()).isEqualTo(secondActivity.getId());
            assertThat(firstActivity.getId()).isEqualTo(thirdActivity.getId());
            assertThat(firstActivity.getMovementType()).isEqualTo(SpendControlMovementType.CONSUMED);
            assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
            assertThat(creditAccountVersion()).isEqualTo(versionBeforeConsumption + 1);
            assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
            assertLedgerFactsUnchanged(jdbcTemplate, before);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：同一原占用变动下，同一资金交易流水被多个控制消费变动累计解释超过交易金额。
     * 输入：原占用金额大于交易金额，先消费 40，再用同一交易尝试消费 30。
     * 输出：第二次消费被拒绝，不写新的控制额度变动。
     * 红线：同一原控制额度变动不能把同一交易流水累计解释成超过资金交易金额的控制消费。
     */
    @Test
    void testConsumeSameTransactionOverTransactionAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed").setAmount(40L));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(SECOND_CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-second-consumed").setAmount(30L)))
                .hasMessageContaining("控制消费累计金额超过资金交易金额");

        assertThat(activityCount(SECOND_CONSUME_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制额度变动流水和摘要被错误地从释放语义复用到 consume。
     * 输入：已存在 RELEASED 控制额度变动，再用同一 movementSn 和 movementDigest 调用 consume。
     * 输出：请求被拒绝，不返回类型不匹配的旧变动。
     * 红线：幂等回放必须保持控制额度变动语义一致，不能只凭摘要复用不同类型、不同交易状态的控制额度变动。
     */
    @Test
    void testConsumeSameMovementSnAndDigestWithDifferentMovementTypeShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlMovementService.recordMovement(recordRequest(decision, REPLAYED_RELEASE_ACTIVITY_SN,
                SpendControlMovementType.RELEASED, "sha256:sctc-replayed-activity")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setDescription("可信释放事实释放 Spend Rule 控制占用"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(REPLAYED_RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-replayed-activity")))
                .hasMessageContaining("控制额度变动流水已存在但类型不一致");

        SpendControlMovementDTO replayedActivity = queryActivity(REPLAYED_RELEASE_ACTIVITY_SN);
        assertThat(replayedActivity.getMovementType()).isEqualTo(SpendControlMovementType.RELEASED);
        assertThat(replayedActivity.getTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(activityCount(REPLAYED_RELEASE_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款资金事实存在，但调用方没有提供产品策略授权审计证据。
     * 输入：已有 RESERVED、CONSUMED 控制额度变动和成功退款资金交易，退款补偿请求缺少原因、操作者和审计引用。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：资金退款本身不足以恢复周期控制额度，REFUND_COMPENSATED 必须能追溯产品策略授权。
     */
    @Test
    void testRefundWithoutPolicyAuditEvidenceShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-without-policy-audit").setAmount(40L)))
                .hasMessageContaining("退款控制补偿原因码不能为空");

        assertThat(activityCount(REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款交易成功后对已消费 Spend Rule 控制额度变动做补偿。
     * 输入：已有 RESERVED、CONSUMED 控制额度变动和成功退款资金交易事实。
     * 输出：记录 REFUND_COMPENSATED 控制额度变动，减少净消费但不恢复已消费的控制占用。
     * 红线：退款补偿只消费既有退款事实，不新增交易、route、posting 或支付工具 REFUND 方向。
     */
    @Test
    void testRefundConsumedControlActivityShouldRecordCompensationWithoutFundsSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlMovementDTO activity = spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-compensated")
                        .setAmount(40L)
                        .setDescription("退款成功后补偿 Spend Rule 控制消耗"));

        assertThat(activity.getMovementSn()).isEqualTo(REFUND_ACTIVITY_SN);
        assertThat(activity.getMovementType()).isEqualTo(SpendControlMovementType.REFUND_COMPENSATED);
        assertThat(activity.getOriginalMovementSn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(activity.getTransactionSn()).isEqualTo(REFUND_TRANSACTION_SN);
        assertThat(activity.getReasonCode()).isEqualTo("PRODUCT_POLICY_REFUND_RESTORE");
        assertThat(activity.getOperatorId()).isEqualTo("spend-control-refund-service");
        assertThat(activity.getAuditReferenceSn()).isEqualTo("audit:sctc-refund-policy");

        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(20L);
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastMovementSn()).isEqualTo(REFUND_ACTIVITY_SN);

        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款交易引用了原资金交易，但该原交易没有对应的已消费 Spend Rule 控制额度变动。
     * 输入：已有 RESERVED 控制额度变动、原资金交易事实和成功退款交易事实，但没有 CONSUMED 控制额度变动。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：退款控制补偿必须基于已消费控制事实，不得只凭退款交易引用生成控制补偿，也不得写 route、posting 或账本事实。
     */
    @Test
    void testRefundWithoutConsumedReferenceShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        insertFundsTransaction(UNLINKED_REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_UNLINKED_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(UNLINKED_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        UNLINKED_REFUND_TRANSACTION_SN, "sha256:sctc-refund-unlinked")
                        .setAmount(40L)
                        .setDescription("退款成功但没有已消费控制额度变动时拒绝补偿")))
                .hasMessageContaining("退款交易未关联已消费控制额度变动");

        assertThat(activityCount(UNLINKED_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(UNLINKED_REFUND_TRANSACTION_SN)).isOne();
        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isZero();
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isEqualTo(60L);
        assertThat(projection.getLastMovementSn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史脏控制额度变动引用了不存在的原消费资金交易。
     * 输入：已有 RESERVED 和 CONSUMED 控制额度变动，退款交易引用的原消费交易事实不存在。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：退款补偿必须同时基于已消费控制额度变动和真实原消费交易事实，不能只凭控制额度变动回链生成补偿。
     */
    @Test
    void testRefundWithMissingReferencedFundsTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        spendControlMovementService.recordMovement(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setAmount(40L));
        insertFundsTransaction(MISSING_REFERENCE_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_MISSING_REFERENCE_TX_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(MISSING_REFERENCE_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        MISSING_REFERENCE_REFUND_TRANSACTION_SN, "sha256:sctc-refund-missing-reference-tx")
                        .setAmount(40L)
                        .setDescription("退款引用的原消费交易不存在时拒绝补偿")))
                .hasMessageContaining("退款交易引用的原消费交易不存在");

        assertThat(activityCount(MISSING_REFERENCE_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isZero();
        assertThat(fundsTransactionCount(MISSING_REFERENCE_REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史脏事实或绕过入口留下了与原占用业务流水不一致的已消费控制额度变动。
     * 输入：已有 RESERVED 控制额度变动、业务流水不一致的 CONSUMED 控制额度变动和引用原交易的成功退款事实。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：退款补偿必须基于与原占用一致的已消费控制事实，不得借历史脏事实重新解释控制占用。
     */
    @Test
    void testRefundWithInconsistentReferencedConsumedMovementShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 40L, CurrencyIsoCode.USD);
        spendControlMovementService.recordMovement(recordRequest(decision, INCONSISTENT_CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-inconsistent-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setBusinessSn(OTHER_BUSINESS_SN)
                .setAmount(40L));
        insertFundsTransaction(INCONSISTENT_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_INCONSISTENT_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(INCONSISTENT_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        INCONSISTENT_REFUND_TRANSACTION_SN, "sha256:sctc-refund-inconsistent")
                        .setAmount(40L)
                        .setDescription("退款引用的已消费控制额度变动与原占用不一致时拒绝补偿")))
                .hasMessageContaining("被引用已消费控制额度变动业务流水不一致");

        assertThat(activityCount(INCONSISTENT_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(INCONSISTENT_CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(INCONSISTENT_REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款交易引用的原消费资金交易与原占用业务流水不一致。
     * 输入：已有 RESERVED、CONSUMED 控制额度变动，退款交易引用同流水控制额度变动但原消费资金交易 businessSn 不一致。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：退款补偿必须同时校验已消费控制额度变动和原消费资金交易事实，不得只凭控制额度变动回链放过交易脏事实。
     */
    @Test
    void testRefundWithInconsistentReferencedFundsTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, OTHER_BUSINESS_SN, 40L, CurrencyIsoCode.USD);
        spendControlMovementService.recordMovement(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setAmount(40L));
        insertFundsTransaction(INCONSISTENT_REFERENCE_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_INCONSISTENT_REFERENCE_TX_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(INCONSISTENT_REFERENCE_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        INCONSISTENT_REFERENCE_REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-inconsistent-reference-tx")
                        .setAmount(40L)
                        .setDescription("退款引用的原消费资金交易与原占用不一致时拒绝补偿")))
                .hasMessageContaining("退款交易引用的原消费交易业务流水不一致");

        assertThat(activityCount(INCONSISTENT_REFERENCE_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(INCONSISTENT_REFERENCE_REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一原占用下存在多笔已消费控制额度变动，退款交易只引用其中一笔较小消费。
     * 输入：已消费 20 和 40 的控制额度变动，退款交易引用 20 的原交易但请求补偿 40。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：退款补偿金额只能基于退款交易引用的已消费控制额度变动净额，
     * 不得借用同一原占用下其他消费变动的净额。
     */
    @Test
    void testRefundOverReferencedConsumedAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(SMALL_CONSUME_TRANSACTION_SN, BUSINESS_SN, 20L, CurrencyIsoCode.USD);
        insertSucceededFundsTransaction(LARGE_CONSUME_TRANSACTION_SN, BUSINESS_SN + "_LARGE", 40L, CurrencyIsoCode.USD);
        spendControlMovementService.recordMovement(recordRequest(decision, SMALL_CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-small-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(SMALL_CONSUME_TRANSACTION_SN)
                .setAmount(20L));
        spendControlMovementService.recordMovement(recordRequest(decision, LARGE_CONSUME_ACTIVITY_SN,
                SpendControlMovementType.CONSUMED, "sha256:sctc-large-consumed")
                .setOriginalMovementSn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(LARGE_CONSUME_TRANSACTION_SN)
                .setAmount(40L));
        insertFundsTransaction(OVER_REFERENCE_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_OVER_REFERENCE_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                SMALL_CONSUME_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(OVER_REFERENCE_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        OVER_REFERENCE_REFUND_TRANSACTION_SN, "sha256:sctc-refund-over-reference")
                        .setAmount(40L)
                        .setDescription("退款补偿金额超过被引用消费控制额度变动净额时拒绝")))
                .hasMessageContaining("退款控制补偿金额超过被引用已消费控制金额");

        assertThat(activityCount(OVER_REFERENCE_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(SMALL_CONSUME_ACTIVITY_SN)).isOne();
        assertThat(activityCount(LARGE_CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(OVER_REFERENCE_REFUND_TRANSACTION_SN)).isOne();
        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getConsumedAmount()).isEqualTo(60L);
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastMovementSn()).isEqualTo(LARGE_CONSUME_ACTIVITY_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方把普通成功交易事实误用到退款控制补偿。
     * 输入：已有 RESERVED、CONSUMED 控制额度变动和 CLOSED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的退款控制补偿变动。
     * 红线：退款控制补偿只能解释已有退款交易事实，不得把普通成功交易降级为退款补偿语义。
     */
    @Test
    void testRefundWithNonRefundTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(NON_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-non-refund-compensated")))
                .hasMessageContaining("退款控制补偿必须使用退款交易事实");

        assertThat(activityCount(NON_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getConsumedAmount()).isEqualTo(60L);
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastMovementSn()).isEqualTo(CONSUME_ACTIVITY_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一原占用变动下，同一退款交易流水被多个补偿变动累计解释超过退款交易金额。
     * 输入：已消费 100，退款交易金额 40，先补偿 25，再用同一退款交易尝试补偿 20。
     * 输出：第二次补偿被拒绝，不写新的退款控制补偿变动。
     * 红线：同一原控制额度变动不能把同一退款交易累计解释成超过退款交易金额的控制补偿。
     */
    @Test
    void testRefundSameTransactionOverTransactionAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 100L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed").setAmount(100L));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-compensated").setAmount(25L));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(SECOND_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-second-refund-compensated").setAmount(20L)))
                .hasMessageContaining("退款控制补偿累计金额超过资金交易金额");

        assertThat(activityCount(SECOND_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(REFUND_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一原占用变动下累计消费超过剩余额度。
     * 输入：已消费 50 的原控制占用，再尝试消费 20。
     * 输出：请求被拒绝，不写新的控制额度变动。
     * 红线：失败路径不得新增交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testConsumeOverRemainingControlAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed").setAmount(50L));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(OVER_CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-over-consumed").setAmount(20L)))
                .hasMessageContaining("控制消费金额超过原占用剩余额度");

        assertThat(activityCount(OVER_CONSUME_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款补偿后再次消费同一原控制占用。
     * 输入：原占用 100，已消费 60、退款补偿 20，再尝试消费 50。
     * 输出：第二次消费按总消费后的剩余占用 40 拒绝，不把退款补偿当作恢复控制占用。
     * 红线：退款只减少净消费，不得重新打开已消费的 authorization reservation。
     */
    @Test
    void testConsumeAfterRefundShouldUseGrossConsumedAmountForRemainingReservation() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 110L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed").setAmount(60L));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionState.CLOSED, 20L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        spendControlTransactionConsumptionApplicationService.refund(
                policyAuthorizedRefundRequest(REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-compensated").setAmount(20L));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(SECOND_CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        FUNDS_TRANSACTION_SN, "sha256:sctc-post-refund-consumed")
                        .setAmount(50L)))
                .hasMessageContaining("控制消费金额超过原占用剩余额度")
                .hasMessageContaining("remainingControlAmount = 40");

        assertThat(activityCount(SECOND_CONSUME_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(activityCount(REFUND_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制额度变动流水被不同摘要重放。
     * 输入：已存在 CONSUMED 控制额度变动，再用同一流水和不同摘要重试。
     * 输出：请求被拒绝，不新增控制额度变动。
     * 红线：幂等冲突不得改写资金交易或账本事实。
     */
    @Test
    void testConsumeSameMovementSnWithDifferentDigestShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlMovementService.recordMovement(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlMovementType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed-conflict")))
                .hasMessageContaining("控制额度变动流水已存在但摘要不一致");

        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendControlTransactionConsumptionTestData() {
        cleanupSpendControlTransactionConsumptionTestData();
    }

    @AfterEach
    void tearDownSpendControlTransactionConsumptionTestData() {
        cleanupSpendControlTransactionConsumptionTestData();
    }

    private void prepareSpendControlTransactionConsumptionData() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
    }

    private SpendControlAdmissionDecisionDTO admittedDecision() {
        return new SpendControlAdmissionDecisionDTO()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setAdmitted(true)
                .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT))
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setSpendDecisionResult(SpendControlDecisionResult.PASSED)
                .setSpendDecisionDigest(SPEND_DECISION_DIGEST)
                .setControlScopeId(SPEND_CONTROL_SCOPE_SN);
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
                .setControlScopeId(decision.getControlScopeId())
                .setPeriodId(PERIOD_ID)
                .setMovementDigest(movementDigest);
    }

    private SpendControlTransactionConsumptionRequest consumptionRequest(String movementSn,
                                                                         String originalMovementSn,
                                                                         String transactionSn,
                                                                         String movementDigest) {
        return new SpendControlTransactionConsumptionRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(movementSn)
                .setOriginalMovementSn(originalMovementSn)
                .setTransactionSn(transactionSn)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setMovementDigest(movementDigest)
                .setDescription("交易成功后消费 Spend Rule 控制占用");
    }

    private SpendControlTransactionConsumptionRequest policyAuthorizedRefundRequest(String movementSn,
                                                                                     String originalMovementSn,
                                                                                     String transactionSn,
                                                                                     String movementDigest) {
        return consumptionRequest(movementSn, originalMovementSn, transactionSn, movementDigest)
                .setReasonCode("PRODUCT_POLICY_REFUND_RESTORE")
                .setOperatorId("spend-control-refund-service")
                .setAuditReferenceSn("audit:sctc-refund-policy");
    }

    private SpendControlBusinessConfirmedRefundCompensationRequest confirmedRefundRequest(String movementSn) {
        return new SpendControlBusinessConfirmedRefundCompensationRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(movementSn)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn("SPEND_CONTROL_CONFIRMED_REFUND_001")
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(40L)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setControlScopeId(SPEND_CONTROL_SCOPE_SN)
                .setPeriodId(PERIOD_ID)
                .setReasonCode("BUSINESS_CONFIRMED_REFUND")
                .setOperatorId("system")
                .setAuditReferenceSn("audit_sctc_confirmed_refund")
                .setDescription("业务确认型退款补偿 Spend Rule 控制额度");
    }

    private RecordSpendControlMovementRequest limitIncreaseRequest() {
        return new RecordSpendControlMovementRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(LIMIT_INCREASE_ACTIVITY_SN)
                .setMovementType(SpendControlMovementType.LIMIT_INCREASED)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn("SPEND_CONTROL_LIMIT_INCREASE_001")
                .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setControlScopeId(SPEND_CONTROL_SCOPE_SN)
                .setPeriodId(PERIOD_ID)
                .setReasonCode("INITIALIZE_TEST_LIMIT")
                .setOperatorId("system")
                .setAuditReferenceSn("audit_sctc_limit")
                .setMovementDigest("sha256:sctc-limit-increased");
    }

    private BudgetControlProjectionQuery projectionQuery() {
        return new BudgetControlProjectionQuery()
                .setTenantId(TENANT_ID)
                .setControlScopeId(SPEND_CONTROL_SCOPE_SN)
                .setPeriodId(PERIOD_ID)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION);
    }

    private SpendControlMovementDTO queryActivity(String movementSn) {
        List<SpendControlMovementDTO> movements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery().setTenantId(TENANT_ID).setMovementSn(movementSn));
        assertThat(movements).hasSize(1);
        return movements.getFirst();
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
                .setState(FundsAccountState.ACTIVE);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest() {
        return new CreatePaymentInstrumentRequest()
                .setSn(PAYMENT_INSTRUMENT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setFlowDirection(PaymentInstrumentFlowDirection.OUTBOUND)
                .setInstrumentNo("****2468")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_sctc_2468")
                .setCurrency(CurrencyIsoCode.USD)
                .setState(FundsAccountState.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setSubjectId(CREDIT_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(CREDIT_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE);
    }

    private void insertSucceededFundsTransaction(String transactionSn,
                                                 String businessSn,
                                                 Long amount,
                                                 CurrencyIsoCode currency) {
        insertFundsTransaction(transactionSn, businessSn, DefaultFundsTransactionType.PAY,
                FundsTransactionState.CLOSED, amount, currency, null);
    }

    private void insertFundsTransaction(String transactionSn,
                                        String businessSn,
                                        DefaultFundsTransactionType transactionType,
                                        FundsTransactionState state,
                                        Long amount,
                                        CurrencyIsoCode currency,
                                        String referenceTransactionSn) {
        insertFundsTransaction(transactionSn, BUSINESS_SCENE, businessSn, transactionType, state, amount, currency,
                referenceTransactionSn);
    }

    private void insertFundsTransaction(String transactionSn,
                                        String businessScene,
                                        String businessSn,
                                        DefaultFundsTransactionType transactionType,
                                        FundsTransactionState state,
                                        Long amount,
                                        CurrencyIsoCode currency,
                                        String referenceTransactionSn) {
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction (
                            sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                            reference_transaction_sn, status, amount, currency, authorized_amount, reversed_amount, completed_amount,
                            refunded_amount, declined_amount, fee_amount, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, 0, 0, 0, 0)
                        """,
                transactionSn,
                TENANT_ID,
                FundsTransactionMode.DIRECT.name(),
                transactionType.name(),
                businessScene,
                businessSn,
                referenceTransactionSn,
                state.name(),
                amount,
                currency.name(),
                amount);
    }

    private int activityCount(String movementSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_spend_control_movement WHERE tenant_id = ? AND movement_sn = ?",
                Integer.class, TENANT_ID, movementSn);
    }

    private int fundsTransactionCount(String transactionSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_transaction WHERE tenant_id = ? AND sn = ?",
                Integer.class, TENANT_ID, transactionSn);
    }

    private int creditAccountVersion() {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM t_credit_account WHERE tenant_id = ? AND sn = ?",
                Integer.class,
                TENANT_ID,
                CREDIT_ACCOUNT_SN);
    }

    private SpendControlTransactionConsumptionApplicationService concurrentConsumptionService(int concurrentInserts) {
        CountDownLatch recordReady = new CountDownLatch(concurrentInserts);
        SpendControlMovementService activityService = (SpendControlMovementService) Proxy.newProxyInstance(
                SpendControlMovementService.class.getClassLoader(),
                new Class<?>[]{SpendControlMovementService.class},
                (proxy, method, args) -> {
                    if ("recordMovement".equals(method.getName())) {
                        recordReady.countDown();
                        assertThat(recordReady.await(5, TimeUnit.SECONDS)).isTrue();
                    }
                    try {
                        return method.invoke(spendControlMovementService, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
        });
        return new SpendControlTransactionConsumptionApplicationServiceImpl(
                activityService,
                paymentInstrumentCapabilityApplicationService,
                fundsTransactionQueryService);
    }

    private <T> T withTenant(Callable<T> command) throws Exception {
        TenantContextHolder.setTenantId(TENANT_ID);
        try {
            return command.call();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void cleanupSpendControlTransactionConsumptionTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE sn LIKE 'funds_transaction_sctc_%'");
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID,
                CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn = ?",
                PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", SECOND_CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", SECOND_CREDIT_ACCOUNT_SN);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class,
            SpendControlMovementServiceImpl.class,
            SpendControlTransactionConsumptionApplicationServiceImpl.class,
            DefaultFundsTransactionQueryService.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

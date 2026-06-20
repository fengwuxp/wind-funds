package com.wind.funds.wallet.application.spend;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.FundsAccountCapabilityApplicationService;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlActivityApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
import com.wind.funds.wallet.dal.mapper.SpendControlActivityMapper;
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
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
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
 * 交易结果消费支出控制活动服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendControlTransactionConsumptionApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendControlTransactionConsumptionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "sctc_credit_account";

    private static final String PAYMENT_INSTRUMENT_SN = "sctc_card";

    private static final String PAYMENT_BINDING_SN = "sctc_binding";

    private static final String FUNDING_RELATION_SN = "sctc_funding_rel";

    private static final String OWNER_ID = "sctc_owner";

    private static final String CHANNEL_CODE = "sctc_channel";

    private static final String BUSINESS_SCENE = "SPEND_CONTROL_TRANSACTION";

    private static final String OTHER_BUSINESS_SCENE = "OTHER_SPEND_CONTROL_TRANSACTION";

    private static final String BUSINESS_SN = "SPEND_CONTROL_TRANSACTION_001";

    private static final String OTHER_BUSINESS_SN = "SPEND_CONTROL_TRANSACTION_OTHER_001";

    private static final String CONSUME_ACTIVITY_SN = "activity_consumed_001";

    private static final String CROSS_SCENE_CONSUME_ACTIVITY_SN = "activity_cross_scene_consumed_001";

    private static final String CROSS_BUSINESS_SN_CONSUME_ACTIVITY_SN = "activity_cross_business_sn_consumed_001";

    private static final String REFUND_CONSUME_ACTIVITY_SN = "activity_refund_consumed_001";

    private static final String RELEASE_ACTIVITY_SN = "activity_released_001";

    private static final String REFUND_ACTIVITY_SN = "activity_refund_compensated_001";

    private static final String UNLINKED_REFUND_ACTIVITY_SN = "activity_refund_unlinked_001";

    private static final String NON_REFUND_ACTIVITY_SN = "activity_non_refund_compensated_001";

    private static final String OVER_CONSUME_ACTIVITY_SN = "activity_over_consumed_001";

    private static final String RESERVED_ACTIVITY_SN = "activity_reserved_for_consume_001";

    private static final String SPEND_RULE_ID = "sr_vcc_transaction_daily_limit";

    private static final String SPEND_RULE_VERSION = "2026-06-20.1";

    private static final String SPEND_DECISION_SN = "decision_sctc_001";

    private static final String SPEND_DECISION_DIGEST = "sha256:sctc-decision";

    private static final String BUDGET_GROUP_SN = "budget_sctc";

    private static final String FUNDS_TRANSACTION_SN = "funds_transaction_sctc_001";

    private static final String CROSS_SCENE_TRANSACTION_SN = "funds_transaction_sctc_cross_scene_001";

    private static final String CROSS_BUSINESS_SN_TRANSACTION_SN = "funds_transaction_sctc_cross_business_sn_001";

    private static final String FAILED_TRANSACTION_SN = "funds_transaction_sctc_failed_001";

    private static final String REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_001";

    private static final String UNLINKED_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_unlinked_001";

    private static final String FAILED_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_failed_001";

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
    private SpendControlTransactionConsumptionApplicationService spendControlTransactionConsumptionApplicationService;

    @Autowired
    private FundsAccountCapabilityApplicationService fundsAccountCapabilityApplicationService;

    @Autowired
    private FundsTransactionQueryService fundsTransactionQueryService;

    @Autowired
    private SpendControlActivityMapper spendControlActivityMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：资金交易成功后消费已预留的 Spend Rule 控制活动。
     * 输入：已有 RESERVED 控制活动和已存在的成功资金交易事实。
     * 输出：记录 CONSUMED 控制活动，回链原控制活动和原交易流水，并更新预算控制投影消费金额。
     * 红线：消费控制活动不得创建资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testConsumeReservedControlActivityShouldRecordConsumedWithoutFundsSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlActivityDTO activity = spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));

        assertThat(activity.getActivitySn()).isEqualTo(CONSUME_ACTIVITY_SN);
        assertThat(activity.getActivityType()).isEqualTo(SpendControlActivityType.CONSUMED);
        assertThat(activity.getOriginalActivitySn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(activity.getTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(activity.getAmount()).isEqualTo(60L);

        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(60L);
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastActivitySn()).isEqualTo(CONSUME_ACTIVITY_SN);

        assertThat(queryActivity(CONSUME_ACTIVITY_SN).getOriginalActivitySn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(queryActivity(CONSUME_ACTIVITY_SN).getTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        SpendControlActivityDTO replayed = spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        assertThat(replayed.getId()).isEqualTo(activity.getId());
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方把已关闭的退款资金交易事实误用到 Spend Rule 控制消费。
     * 输入：已有 RESERVED 控制活动和 CLOSED REFUND 资金交易事实。
     * 输出：请求被拒绝，不写新的控制活动。
     * 红线：退款交易只能走退款补偿语义，不得被降级为普通成功消费，也不得写 route、posting 或账本事实。
     */
    @Test
    void testConsumeWithRefundTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_FOR_CONSUME_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 60L, CurrencyIsoCode.USD,
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
     * 输入：已有 RESERVED 控制活动和不同业务场景的 CLOSED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的控制活动。
     * 红线：控制活动只能消费同一业务场景下的资金交易事实，不得跨业务域串用交易流水，也不得写 route、posting 或账本事实。
     */
    @Test
    void testConsumeWithDifferentBusinessSceneTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(CROSS_SCENE_TRANSACTION_SN, OTHER_BUSINESS_SCENE, BUSINESS_SN,
                DefaultFundsTransactionType.PAY, FundsTransactionStatus.CLOSED, 60L, CurrencyIsoCode.USD, null);
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
     * 输入：已有 RESERVED 控制活动和同业务场景、不同业务流水的 CLOSED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的控制活动。
     * 红线：成功消费只能解释同一业务流水的交易事实，不得跨订单串用交易流水，也不得写 route、posting 或账本事实。
     */
    @Test
    void testConsumeWithDifferentBusinessSnTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
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
     * 场景：两个线程并发消费同一控制活动流水且摘要相同。
     * 输入：已有 RESERVED 控制活动和已存在的成功资金交易事实。
     * 输出：只有一条 CONSUMED 控制活动，两个调用都回到同一活动事实。
     * 红线：并发唯一键冲突必须按幂等回读处理，不得抛出数据库异常或生成重复资金、route、posting、账本事实。
     */
    @Test
    void testConcurrentConsumeSameActivitySnWithSameDigestShouldReadBackExistingActivity() throws Exception {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        SpendControlTransactionConsumptionApplicationService concurrentService = concurrentConsumptionService(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<SpendControlActivityDTO> command = () -> withTenant(() -> concurrentService.consume(
                    consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                            "sha256:sctc-consumed")));

            Future<SpendControlActivityDTO> first = executor.submit(command);
            Future<SpendControlActivityDTO> second = executor.submit(command);

            SpendControlActivityDTO firstActivity = first.get(10, TimeUnit.SECONDS);
            SpendControlActivityDTO secondActivity = second.get(10, TimeUnit.SECONDS);
            assertThat(firstActivity.getId()).isEqualTo(secondActivity.getId());
            assertThat(firstActivity.getActivityType()).isEqualTo(SpendControlActivityType.CONSUMED);
            assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
            assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
            assertLedgerFactsUnchanged(jdbcTemplate, before);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：资金交易失败后释放已预留的 Spend Rule 控制活动。
     * 输入：已有 RESERVED 控制活动和失败资金交易事实。
     * 输出：记录 RELEASED 控制活动，预算控制投影释放金额增加。
     * 红线：释放控制活动不得创建或修改资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testReleaseReservedControlActivityShouldRecordReleasedWithoutFundsSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(FAILED_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_FAILED_001",
                DefaultFundsTransactionType.PAY, FundsTransactionStatus.FAILED, 60L, CurrencyIsoCode.USD, null);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlActivityDTO activity = spendControlTransactionConsumptionApplicationService.release(
                consumptionRequest(RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FAILED_TRANSACTION_SN,
                        "sha256:sctc-released").setDescription("交易失败后释放 Spend Rule 控制占用"));

        assertThat(activity.getActivitySn()).isEqualTo(RELEASE_ACTIVITY_SN);
        assertThat(activity.getActivityType()).isEqualTo(SpendControlActivityType.RELEASED);
        assertThat(activity.getOriginalActivitySn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(activity.getTransactionSn()).isEqualTo(FAILED_TRANSACTION_SN);

        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isZero();
        assertThat(projection.getReleasedAmount()).isEqualTo(60L);
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastActivitySn()).isEqualTo(RELEASE_ACTIVITY_SN);

        assertThat(fundsTransactionCount(FAILED_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：上游返回失败的退款交易事实，调用方误用 release 释放原控制占用。
     * 输入：已有 RESERVED 控制活动和失败 REFUND 资金交易事实。
     * 输出：请求被拒绝，不写新的控制活动。
     * 红线：退款交易只能走退款补偿语义，不得被降级为普通失败释放，也不得写 route、posting 或账本事实。
     */
    @Test
    void testReleaseWithFailedRefundTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(FAILED_REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_FAILED_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.FAILED, 60L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.release(
                consumptionRequest(RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FAILED_REFUND_TRANSACTION_SN,
                        "sha256:sctc-release-refund-conflict")))
                .hasMessageContaining("控制释放不能使用退款交易事实");

        assertThat(activityCount(RELEASE_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(FAILED_REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款交易成功后对已消费 Spend Rule 控制活动做补偿。
     * 输入：已有 RESERVED、CONSUMED 控制活动和成功退款资金交易事实。
     * 输出：记录 REFUND_COMPENSATED 控制活动，并按净消费更新预算控制投影。
     * 红线：退款补偿只消费既有退款事实，不新增交易、route、posting 或支付工具 REFUND 方向。
     */
    @Test
    void testRefundConsumedControlActivityShouldRecordCompensationWithoutFundsSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlActivityDTO activity = spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-compensated")
                        .setAmount(40L)
                        .setDescription("退款成功后补偿 Spend Rule 控制消耗"));

        assertThat(activity.getActivitySn()).isEqualTo(REFUND_ACTIVITY_SN);
        assertThat(activity.getActivityType()).isEqualTo(SpendControlActivityType.REFUND_COMPENSATED);
        assertThat(activity.getOriginalActivitySn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertThat(activity.getTransactionSn()).isEqualTo(REFUND_TRANSACTION_SN);

        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(20L);
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(projection.getLastActivitySn()).isEqualTo(REFUND_ACTIVITY_SN);

        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款交易引用了原资金交易，但该原交易没有对应的已消费 Spend Rule 控制活动。
     * 输入：已有 RESERVED 控制活动、原资金交易事实和成功退款交易事实，但没有 CONSUMED 控制活动。
     * 输出：请求被拒绝，不写新的退款控制补偿活动。
     * 红线：退款控制补偿必须基于已消费控制事实，不得只凭退款交易引用生成控制补偿，也不得写 route、posting 或账本事实。
     */
    @Test
    void testRefundWithoutConsumedReferenceShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        insertFundsTransaction(UNLINKED_REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_UNLINKED_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(UNLINKED_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        UNLINKED_REFUND_TRANSACTION_SN, "sha256:sctc-refund-unlinked")
                        .setAmount(40L)
                        .setDescription("退款成功但没有已消费控制活动时拒绝补偿")))
                .hasMessageContaining("退款交易未关联已消费控制活动");

        assertThat(activityCount(UNLINKED_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(UNLINKED_REFUND_TRANSACTION_SN)).isOne();
        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isZero();
        assertThat(projection.getReleasedAmount()).isZero();
        assertThat(projection.getRemainingControlAmount()).isEqualTo(60L);
        assertThat(projection.getLastActivitySn()).isEqualTo(RESERVED_ACTIVITY_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方把普通成功交易事实误用到退款控制补偿。
     * 输入：已有 RESERVED、CONSUMED 控制活动和 CLOSED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的退款控制补偿活动。
     * 红线：退款控制补偿只能解释已有退款交易事实，不得把普通成功交易降级为退款补偿语义。
     */
    @Test
    void testRefundWithNonRefundTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(NON_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-non-refund-compensated")))
                .hasMessageContaining("退款控制补偿必须使用退款交易事实");

        assertThat(activityCount(NON_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getConsumedAmount()).isEqualTo(60L);
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastActivitySn()).isEqualTo(CONSUME_ACTIVITY_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一原占用活动下累计消费超过剩余额度。
     * 输入：已消费 50 的原控制占用，再尝试消费 20。
     * 输出：请求被拒绝，不写新的控制活动。
     * 红线：失败路径不得新增交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     */
    @Test
    void testConsumeOverRemainingControlAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
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
     * 场景：同一控制活动流水被不同摘要重放。
     * 输入：已存在 CONSUMED 控制活动，再用同一流水和不同摘要重试。
     * 输出：请求被拒绝，不新增控制活动。
     * 红线：幂等冲突不得改写资金交易或账本事实。
     */
    @Test
    void testConsumeSameActivitySnWithDifferentDigestShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed-conflict")))
                .hasMessageContaining("控制活动流水已存在但摘要不一致");

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
        return spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest().setSpendDecisionResult(SpendControlDecisionResult.PASSED));
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
                .setActivityDigest(activityDigest);
    }

    private SpendControlTransactionConsumptionRequest consumptionRequest(String activitySn,
                                                                         String originalActivitySn,
                                                                         String transactionSn,
                                                                         String activityDigest) {
        return new SpendControlTransactionConsumptionRequest()
                .setTenantId(TENANT_ID)
                .setActivitySn(activitySn)
                .setOriginalActivitySn(originalActivitySn)
                .setTransactionSn(transactionSn)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setActivityDigest(activityDigest)
                .setDescription("交易成功后消费 Spend Rule 控制占用");
    }

    private ResolveSpendControlAdmissionRequest admissionRequest() {
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
                .setBusinessSn(BUSINESS_SN)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setSpendDecisionDigest(SPEND_DECISION_DIGEST)
                .setBudgetGroupSn(BUDGET_GROUP_SN);
    }

    private BudgetControlProjectionQuery projectionQuery() {
        return new BudgetControlProjectionQuery()
                .setTenantId(TENANT_ID)
                .setBudgetGroupSn(BUDGET_GROUP_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION);
    }

    private SpendControlActivityDTO queryActivity(String activitySn) {
        List<SpendControlActivityDTO> activities = spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery().setTenantId(TENANT_ID).setActivitySn(activitySn));
        assertThat(activities).hasSize(1);
        return activities.getFirst();
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
                .setInstrumentNo("****2468")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_sctc_2468")
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

    private void insertSucceededFundsTransaction(String transactionSn,
                                                 String businessSn,
                                                 Long amount,
                                                 CurrencyIsoCode currency) {
        insertFundsTransaction(transactionSn, businessSn, DefaultFundsTransactionType.PAY,
                FundsTransactionStatus.CLOSED, amount, currency, null);
    }

    private void insertFundsTransaction(String transactionSn,
                                        String businessSn,
                                        DefaultFundsTransactionType transactionType,
                                        FundsTransactionStatus status,
                                        Long amount,
                                        CurrencyIsoCode currency,
                                        String referenceTransactionSn) {
        insertFundsTransaction(transactionSn, BUSINESS_SCENE, businessSn, transactionType, status, amount, currency,
                referenceTransactionSn);
    }

    private void insertFundsTransaction(String transactionSn,
                                        String businessScene,
                                        String businessSn,
                                        DefaultFundsTransactionType transactionType,
                                        FundsTransactionStatus status,
                                        Long amount,
                                        CurrencyIsoCode currency,
                                        String referenceTransactionSn) {
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction (
                            sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                            reference_transaction_sn, status, amount, currency, authorized_amount, reversed_amount, settled_amount,
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
                status.name(),
                amount,
                currency.name(),
                amount);
    }

    private int activityCount(String activitySn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_spend_control_activity WHERE tenant_id = ? AND activity_sn = ?",
                Integer.class, TENANT_ID, activitySn);
    }

    private int fundsTransactionCount(String transactionSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_transaction WHERE tenant_id = ? AND sn = ?",
                Integer.class, TENANT_ID, transactionSn);
    }

    private SpendControlTransactionConsumptionApplicationService concurrentConsumptionService(int concurrentInserts) {
        SpendControlActivityApplicationService activityService = new SpendControlActivityApplicationServiceImpl(
                gatedSpendControlActivityMapper(concurrentInserts),
                fundsAccountCapabilityApplicationService);
        return new SpendControlTransactionConsumptionApplicationServiceImpl(activityService, fundsTransactionQueryService);
    }

    private SpendControlActivityMapper gatedSpendControlActivityMapper(int concurrentInserts) {
        CountDownLatch insertReady = new CountDownLatch(concurrentInserts);
        return (SpendControlActivityMapper) Proxy.newProxyInstance(
                SpendControlActivityMapper.class.getClassLoader(),
                new Class<?>[]{SpendControlActivityMapper.class},
                (proxy, method, args) -> {
                    if ("insertSelective".equals(method.getName())) {
                        insertReady.countDown();
                        assertThat(insertReady.await(5, TimeUnit.SECONDS)).isTrue();
                    }
                    try {
                        return method.invoke(spendControlActivityMapper, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    private <T> T withTenant(Callable<T> command) throws Exception {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        try {
            return command.call();
        } finally {
            ThreadContextTenantIdHolder.remove();
        }
    }

    private void cleanupSpendControlTransactionConsumptionTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_activity WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE sn LIKE 'funds_transaction_sctc_%'");
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn = ?", FUNDING_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn = ?",
                PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
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
            SpendControlTransactionConsumptionApplicationServiceImpl.class,
            DefaultFundsTransactionQueryService.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

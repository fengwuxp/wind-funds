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
import com.wind.funds.wallet.application.spend.impl.SpendControlActivityApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
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

    private static final String SECOND_CREDIT_ACCOUNT_SN = "sctc_second_credit_account";

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

    private static final String SECOND_CONSUME_ACTIVITY_SN = "activity_consumed_002";

    private static final String CROSS_SCENE_CONSUME_ACTIVITY_SN = "activity_cross_scene_consumed_001";

    private static final String CROSS_BUSINESS_SN_CONSUME_ACTIVITY_SN = "activity_cross_business_sn_consumed_001";

    private static final String REFUND_CONSUME_ACTIVITY_SN = "activity_refund_consumed_001";

    private static final String INCONSISTENT_CONSUME_ACTIVITY_SN = "activity_inconsistent_consumed_001";

    private static final String INCONSISTENT_LINKED_ACTIVITY_SN = "activity_inconsistent_linked_001";

    private static final String SMALL_CONSUME_ACTIVITY_SN = "activity_small_consumed_001";

    private static final String LARGE_CONSUME_ACTIVITY_SN = "activity_large_consumed_001";

    private static final String RELEASE_ACTIVITY_SN = "activity_released_001";

    private static final String SECOND_RELEASE_ACTIVITY_SN = "activity_released_002";

    private static final String CROSS_BUSINESS_SN_RELEASE_ACTIVITY_SN = "activity_cross_business_sn_released_001";

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

    private static final String BUDGET_GROUP_SN = "budget_sctc";

    private static final String FUNDS_TRANSACTION_SN = "funds_transaction_sctc_001";

    private static final String CROSS_SCENE_TRANSACTION_SN = "funds_transaction_sctc_cross_scene_001";

    private static final String CROSS_BUSINESS_SN_TRANSACTION_SN = "funds_transaction_sctc_cross_business_sn_001";

    private static final String SMALL_CONSUME_TRANSACTION_SN = "funds_transaction_sctc_small_consume_001";

    private static final String LARGE_CONSUME_TRANSACTION_SN = "funds_transaction_sctc_large_consume_001";

    private static final String FAILED_TRANSACTION_SN = "funds_transaction_sctc_failed_001";

    private static final String CROSS_BUSINESS_SN_FAILED_TRANSACTION_SN =
            "funds_transaction_sctc_cross_business_sn_failed_001";

    private static final String REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_001";

    private static final String INCONSISTENT_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_inconsistent_001";

    private static final String UNLINKED_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_unlinked_001";

    private static final String MISSING_REFERENCE_REFUND_TRANSACTION_SN =
            "funds_transaction_sctc_refund_missing_reference_tx_001";

    private static final String INCONSISTENT_REFERENCE_REFUND_TRANSACTION_SN =
            "funds_transaction_sctc_refund_inconsistent_reference_tx_001";

    private static final String OVER_REFERENCE_REFUND_TRANSACTION_SN =
            "funds_transaction_sctc_refund_over_reference_001";

    private static final String FAILED_REFUND_TRANSACTION_SN = "funds_transaction_sctc_refund_failed_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendControlActivityApplicationService spendControlActivityApplicationService;

    @Autowired
    private SpendControlTransactionConsumptionApplicationService spendControlTransactionConsumptionApplicationService;

    @Autowired
    private FundsAccountCapabilityApplicationService fundsAccountCapabilityApplicationService;

    @Autowired
    private FundsTransactionQueryService fundsTransactionQueryService;

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
     * 场景：同一预算组和 Spend Rule 下存在多个目标账户，交易成功只消费其中一个账户的控制占用。
     * 输入：两个信用账户分别存在 RESERVED 控制活动，其中一个账户发生成功资金交易并记录 CONSUMED。
     * 输出：按目标账户查询预算控制投影时，只解释该账户的占用和消费。
     * 红线：交易消费服务不得让同预算组下其他账户或其他卡的控制活动污染当前账户投影。
     */
    @Test
    void testConsumeProjectionShouldFilterTargetAccountWithoutMixingOtherAccountActivities() {
        prepareSpendControlTransactionConsumptionData();
        creditAccountService.createCreditAccount(createCreditAccountRequest().setSn(SECOND_CREDIT_ACCOUNT_SN));
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, SECOND_RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-second-account-reserved")
                .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                        FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(40L));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed"));

        BudgetControlProjectionDTO primaryProjection = spendControlActivityApplicationService
                .getBudgetControlProjection(projectionQuery()
                        .setTargetAccountId(FundsAccountId.immutable(CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(primaryProjection.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(primaryProjection.getReservedAmount()).isEqualTo(60L);
        assertThat(primaryProjection.getConsumedAmount()).isEqualTo(60L);
        assertThat(primaryProjection.getReleasedAmount()).isZero();
        assertThat(primaryProjection.getRemainingControlAmount()).isZero();
        assertThat(primaryProjection.getLastActivitySn()).isEqualTo(CONSUME_ACTIVITY_SN);

        BudgetControlProjectionDTO secondProjection = spendControlActivityApplicationService
                .getBudgetControlProjection(projectionQuery()
                        .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                                FundsSubjectType.CREDIT_ACCOUNT)));
        assertThat(secondProjection.getReservedAmount()).isEqualTo(40L);
        assertThat(secondProjection.getConsumedAmount()).isZero();
        assertThat(secondProjection.getReleasedAmount()).isZero();
        assertThat(secondProjection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(secondProjection.getLastActivitySn()).isEqualTo(SECOND_RESERVED_ACTIVITY_SN);
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
     * 场景：同一原占用活动下，同一资金交易流水被多个控制消费活动累计解释超过交易金额。
     * 输入：原占用金额大于交易金额，先消费 40，再用同一交易尝试消费 30。
     * 输出：第二次消费被拒绝，不写新的控制活动。
     * 红线：同一原控制活动不能把同一交易流水累计解释成超过资金交易金额的控制消费。
     */
    @Test
    void testConsumeSameTransactionOverTransactionAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
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
        insertFundsTransaction(FAILED_TRANSACTION_SN, BUSINESS_SN,
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
     * 场景：调用方把同业务场景但不同业务流水的失败交易事实误用于当前 Spend Rule 控制释放。
     * 输入：已有 RESERVED 控制活动和同业务场景、不同业务流水的 FAILED PAY 资金交易事实。
     * 输出：请求被拒绝，不写新的释放控制活动。
     * 红线：失败释放只能解释同一业务流水的交易终局，不得跨订单释放控制占用，也不得写 route、posting 或账本事实。
     */
    @Test
    void testReleaseWithDifferentBusinessSnTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertFundsTransaction(CROSS_BUSINESS_SN_FAILED_TRANSACTION_SN, OTHER_BUSINESS_SN,
                DefaultFundsTransactionType.PAY, FundsTransactionStatus.FAILED, 60L, CurrencyIsoCode.USD, null);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.release(
                consumptionRequest(CROSS_BUSINESS_SN_RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        CROSS_BUSINESS_SN_FAILED_TRANSACTION_SN, "sha256:sctc-cross-business-sn-released")))
                .hasMessageContaining("资金交易业务流水不一致");

        assertThat(activityCount(CROSS_BUSINESS_SN_RELEASE_ACTIVITY_SN)).isZero();
        assertThat(fundsTransactionCount(CROSS_BUSINESS_SN_FAILED_TRANSACTION_SN)).isOne();
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
     * 场景：同一原占用活动下，同一失败交易流水被多个释放活动累计解释超过交易金额。
     * 输入：原占用金额大于失败交易金额，先释放 40，再用同一交易尝试释放 30。
     * 输出：第二次释放被拒绝，不写新的控制活动。
     * 红线：同一原控制活动不能把同一交易流水累计解释成超过资金交易金额的控制释放。
     */
    @Test
    void testReleaseSameTransactionOverTransactionAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
        insertFundsTransaction(FAILED_TRANSACTION_SN, BUSINESS_SN,
                DefaultFundsTransactionType.PAY, FundsTransactionStatus.FAILED, 60L, CurrencyIsoCode.USD, null);
        spendControlTransactionConsumptionApplicationService.release(
                consumptionRequest(RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FAILED_TRANSACTION_SN,
                        "sha256:sctc-released").setAmount(40L));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.release(
                consumptionRequest(SECOND_RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FAILED_TRANSACTION_SN,
                        "sha256:sctc-second-released").setAmount(30L)))
                .hasMessageContaining("控制释放累计金额超过资金交易金额");

        assertThat(activityCount(SECOND_RELEASE_ACTIVITY_SN)).isZero();
        assertThat(activityCount(RELEASE_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FAILED_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史脏事实留下了同一原占用下目标账户不一致的派生控制活动。
     * 输入：主账户已有 RESERVED 和 CONSUMED，异账户挂载 REFUND_COMPENSATED 到同一 originalActivitySn 后再尝试释放主账户。
     * 输出：释放请求被拒绝，不借异账户补偿虚增当前账户可释放额度。
     * 红线：交易消费链路必须按原占用目标账户解释控制活动，不能跨账户复用控制额度，也不得写 route、posting 或账本事实。
     */
    @Test
    void testReleaseWithInconsistentLinkedTargetAccountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        creditAccountService.createCreditAccount(createCreditAccountRequest().setSn(SECOND_CREDIT_ACCOUNT_SN));
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, INCONSISTENT_LINKED_ACTIVITY_SN,
                SpendControlActivityType.REFUND_COMPENSATED, "sha256:sctc-inconsistent-linked")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(REFUND_TRANSACTION_SN)
                .setTargetAccountId(FundsAccountId.immutable(SECOND_CREDIT_ACCOUNT_SN,
                        FundsSubjectType.CREDIT_ACCOUNT))
                .setAmount(60L));
        insertFundsTransaction(FAILED_TRANSACTION_SN, BUSINESS_SN,
                DefaultFundsTransactionType.PAY, FundsTransactionStatus.FAILED, 60L, CurrencyIsoCode.USD, null);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.release(
                consumptionRequest(RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FAILED_TRANSACTION_SN,
                        "sha256:sctc-release-inconsistent-linked")))
                .hasMessageContaining("关联控制活动目标账户不一致");

        assertThat(activityCount(RELEASE_ACTIVITY_SN)).isZero();
        assertThat(activityCount(INCONSISTENT_LINKED_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FAILED_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一控制活动流水和摘要被错误地从 release 语义复用到 consume。
     * 输入：已存在 RELEASED 控制活动，再用同一 activitySn 和 activityDigest 调用 consume。
     * 输出：请求被拒绝，不返回类型不匹配的旧活动。
     * 红线：幂等回放必须保持控制活动语义一致，不能只凭摘要复用不同类型、不同交易状态的控制活动。
     */
    @Test
    void testConsumeSameActivitySnAndDigestWithDifferentActivityTypeShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 60L, CurrencyIsoCode.USD);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, REPLAYED_RELEASE_ACTIVITY_SN,
                SpendControlActivityType.RELEASED, "sha256:sctc-replayed-activity")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setDescription("交易失败后释放 Spend Rule 控制占用"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(REPLAYED_RELEASE_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-replayed-activity")))
                .hasMessageContaining("控制活动流水已存在但类型不一致");

        SpendControlActivityDTO replayedActivity = queryActivity(REPLAYED_RELEASE_ACTIVITY_SN);
        assertThat(replayedActivity.getActivityType()).isEqualTo(SpendControlActivityType.RELEASED);
        assertThat(replayedActivity.getTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(activityCount(REPLAYED_RELEASE_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
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
     * 场景：历史脏控制活动引用了不存在的原消费资金交易。
     * 输入：已有 RESERVED 和 CONSUMED 控制活动，退款交易引用的原消费交易事实不存在。
     * 输出：请求被拒绝，不写新的退款控制补偿活动。
     * 红线：退款补偿必须同时基于已消费控制活动和真实原消费交易事实，不能只凭控制活动回链生成补偿。
     */
    @Test
    void testRefundWithMissingReferencedFundsTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlActivityType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setAmount(40L));
        insertFundsTransaction(MISSING_REFERENCE_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_MISSING_REFERENCE_TX_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(MISSING_REFERENCE_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
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
     * 场景：历史脏事实或绕过入口留下了与原占用业务流水不一致的已消费控制活动。
     * 输入：已有 RESERVED 控制活动、业务流水不一致的 CONSUMED 控制活动和引用原交易的成功退款事实。
     * 输出：请求被拒绝，不写新的退款控制补偿活动。
     * 红线：退款补偿必须基于与原占用一致的已消费控制事实，不得借历史脏事实重新解释控制占用。
     */
    @Test
    void testRefundWithInconsistentReferencedConsumedActivityShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 40L, CurrencyIsoCode.USD);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, INCONSISTENT_CONSUME_ACTIVITY_SN,
                SpendControlActivityType.CONSUMED, "sha256:sctc-inconsistent-consumed")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setBusinessSn(OTHER_BUSINESS_SN)
                .setAmount(40L));
        insertFundsTransaction(INCONSISTENT_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_INCONSISTENT_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(INCONSISTENT_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        INCONSISTENT_REFUND_TRANSACTION_SN, "sha256:sctc-refund-inconsistent")
                        .setAmount(40L)
                        .setDescription("退款引用的已消费控制活动与原占用不一致时拒绝补偿")))
                .hasMessageContaining("被引用已消费控制活动业务流水不一致");

        assertThat(activityCount(INCONSISTENT_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(INCONSISTENT_CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(FUNDS_TRANSACTION_SN)).isOne();
        assertThat(fundsTransactionCount(INCONSISTENT_REFUND_TRANSACTION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：退款交易引用的原消费资金交易与原占用业务流水不一致。
     * 输入：已有 RESERVED、CONSUMED 控制活动，退款交易引用同流水控制活动但原消费资金交易 businessSn 不一致。
     * 输出：请求被拒绝，不写新的退款控制补偿活动。
     * 红线：退款补偿必须同时校验已消费控制活动和原消费资金交易事实，不得只凭控制活动回链放过交易脏事实。
     */
    @Test
    void testRefundWithInconsistentReferencedFundsTransactionShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, OTHER_BUSINESS_SN, 40L, CurrencyIsoCode.USD);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, CONSUME_ACTIVITY_SN,
                SpendControlActivityType.CONSUMED, "sha256:sctc-consumed")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(FUNDS_TRANSACTION_SN)
                .setAmount(40L));
        insertFundsTransaction(INCONSISTENT_REFERENCE_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_INCONSISTENT_REFERENCE_TX_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(INCONSISTENT_REFERENCE_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
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
     * 场景：同一原占用下存在多笔已消费控制活动，退款交易只引用其中一笔较小消费。
     * 输入：已消费 20 和 40 的控制活动，退款交易引用 20 的原交易但请求补偿 40。
     * 输出：请求被拒绝，不写新的退款控制补偿活动。
     * 红线：退款补偿金额只能基于退款交易引用的已消费控制活动净额，
     * 不得借用同一原占用下其他消费活动的净额。
     */
    @Test
    void testRefundOverReferencedConsumedAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved"));
        insertSucceededFundsTransaction(SMALL_CONSUME_TRANSACTION_SN, BUSINESS_SN, 20L, CurrencyIsoCode.USD);
        insertSucceededFundsTransaction(LARGE_CONSUME_TRANSACTION_SN, BUSINESS_SN + "_LARGE", 40L, CurrencyIsoCode.USD);
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, SMALL_CONSUME_ACTIVITY_SN,
                SpendControlActivityType.CONSUMED, "sha256:sctc-small-consumed")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(SMALL_CONSUME_TRANSACTION_SN)
                .setAmount(20L));
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, LARGE_CONSUME_ACTIVITY_SN,
                SpendControlActivityType.CONSUMED, "sha256:sctc-large-consumed")
                .setOriginalActivitySn(RESERVED_ACTIVITY_SN)
                .setTransactionSn(LARGE_CONSUME_TRANSACTION_SN)
                .setAmount(40L));
        insertFundsTransaction(OVER_REFERENCE_REFUND_TRANSACTION_SN,
                "SPEND_CONTROL_TRANSACTION_REFUND_OVER_REFERENCE_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                SMALL_CONSUME_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(OVER_REFERENCE_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN,
                        OVER_REFERENCE_REFUND_TRANSACTION_SN, "sha256:sctc-refund-over-reference")
                        .setAmount(40L)
                        .setDescription("退款补偿金额超过被引用消费控制活动净额时拒绝")))
                .hasMessageContaining("退款控制补偿金额超过被引用已消费控制金额");

        assertThat(activityCount(OVER_REFERENCE_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(SMALL_CONSUME_ACTIVITY_SN)).isOne();
        assertThat(activityCount(LARGE_CONSUME_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(OVER_REFERENCE_REFUND_TRANSACTION_SN)).isOne();
        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery());
        assertThat(projection.getConsumedAmount()).isEqualTo(60L);
        assertThat(projection.getRemainingControlAmount()).isZero();
        assertThat(projection.getLastActivitySn()).isEqualTo(LARGE_CONSUME_ACTIVITY_SN);
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
     * 场景：同一原占用活动下，同一退款交易流水被多个补偿活动累计解释超过退款交易金额。
     * 输入：已消费 100，退款交易金额 40，先补偿 25，再用同一退款交易尝试补偿 20。
     * 输出：第二次补偿被拒绝，不写新的退款控制补偿活动。
     * 红线：同一原控制活动不能把同一退款交易累计解释成超过退款交易金额的控制补偿。
     */
    @Test
    void testRefundSameTransactionOverTransactionAmountShouldFailWithoutSideEffect() {
        prepareSpendControlTransactionConsumptionData();
        SpendControlAdmissionDecisionDTO decision = admittedDecision();
        spendControlActivityApplicationService.recordActivity(recordRequest(decision, RESERVED_ACTIVITY_SN,
                SpendControlActivityType.RESERVED, "sha256:sctc-reserved").setAmount(100L));
        insertSucceededFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, 100L, CurrencyIsoCode.USD);
        spendControlTransactionConsumptionApplicationService.consume(
                consumptionRequest(CONSUME_ACTIVITY_SN, RESERVED_ACTIVITY_SN, FUNDS_TRANSACTION_SN,
                        "sha256:sctc-consumed").setAmount(100L));
        insertFundsTransaction(REFUND_TRANSACTION_SN, "SPEND_CONTROL_TRANSACTION_REFUND_001",
                DefaultFundsTransactionType.REFUND, FundsTransactionStatus.CLOSED, 40L, CurrencyIsoCode.USD,
                FUNDS_TRANSACTION_SN);
        spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-refund-compensated").setAmount(25L));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.refund(
                consumptionRequest(SECOND_REFUND_ACTIVITY_SN, RESERVED_ACTIVITY_SN, REFUND_TRANSACTION_SN,
                        "sha256:sctc-second-refund-compensated").setAmount(20L)))
                .hasMessageContaining("退款控制补偿累计金额超过资金交易金额");

        assertThat(activityCount(SECOND_REFUND_ACTIVITY_SN)).isZero();
        assertThat(activityCount(REFUND_ACTIVITY_SN)).isOne();
        assertThat(fundsTransactionCount(REFUND_TRANSACTION_SN)).isOne();
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
                .setBudgetGroupSn(BUDGET_GROUP_SN);
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
        CountDownLatch recordReady = new CountDownLatch(concurrentInserts);
        SpendControlActivityApplicationService activityService = (SpendControlActivityApplicationService) Proxy.newProxyInstance(
                SpendControlActivityApplicationService.class.getClassLoader(),
                new Class<?>[]{SpendControlActivityApplicationService.class},
                (proxy, method, args) -> {
                    if ("recordActivity".equals(method.getName())) {
                        recordReady.countDown();
                        assertThat(recordReady.await(5, TimeUnit.SECONDS)).isTrue();
                    }
                    try {
                        return method.invoke(spendControlActivityApplicationService, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
        return new SpendControlTransactionConsumptionApplicationServiceImpl(activityService, fundsTransactionQueryService);
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
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", SECOND_CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", SECOND_CREDIT_ACCOUNT_SN);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            CreditAccountServiceImpl.class,
            PaymentInstrumentServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            SpendControlActivityApplicationServiceImpl.class,
            SpendControlTransactionConsumptionApplicationServiceImpl.class,
            DefaultFundsTransactionQueryService.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

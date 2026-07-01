package com.wind.funds.wallet.application.spend;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.model.dto.SpendRuleEvaluationDecisionDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.EvaluateSpendRuleRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.application.spend.impl.SpendRuleEvaluationApplicationServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlMovementServiceImpl;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.services.impl.SpendRuleAssignmentServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDefinitionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleVersionServiceImpl;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spend Rule 规则评估应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendRuleEvaluationApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendRuleEvaluationApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String RULE_ID = "sr_evaluation_single_amount";

    private static final String RULE_VERSION = "2026-06-30.1";

    private static final String RULE_DIGEST = "sha256:spend-rule-evaluation-single-amount";

    private static final String PERIOD_RULE_ID = "sr_evaluation_period_amount";

    private static final String PERIOD_RULE_DIGEST = "sha256:spend-rule-evaluation-period-amount";

    private static final String COUNT_RULE_ID = "sr_evaluation_period_count";

    private static final String COUNT_RULE_DIGEST = "sha256:spend-rule-evaluation-period-count";

    private static final String MCC_RULE_ID = "sr_evaluation_mcc";

    private static final String MCC_RULE_DIGEST = "sha256:spend-rule-evaluation-mcc";

    private static final String MCC_ALLOW_RULE_ID = "sr_evaluation_mcc_allow";

    private static final String MCC_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-mcc-allow";

    private static final String COUNTRY_RULE_ID = "sr_evaluation_country";

    private static final String COUNTRY_RULE_DIGEST = "sha256:spend-rule-evaluation-country";

    private static final String COUNTRY_ALLOW_RULE_ID = "sr_evaluation_country_allow";

    private static final String COUNTRY_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-country-allow";

    private static final String CARD_DATA_INPUT_RULE_ID = "sr_evaluation_card_data_input";

    private static final String CARD_DATA_INPUT_RULE_DIGEST = "sha256:spend-rule-evaluation-card-data-input";

    private static final String CARD_DATA_INPUT_ALLOW_RULE_ID = "sr_evaluation_card_data_input_allow";

    private static final String CARD_DATA_INPUT_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-card-data-input-allow";

    private static final String MERCHANT_ID_RULE_ID = "sr_evaluation_merchant_id";

    private static final String MERCHANT_ID_RULE_DIGEST = "sha256:spend-rule-evaluation-merchant-id";

    private static final String MERCHANT_ID_ALLOW_RULE_ID = "sr_evaluation_merchant_id_allow";

    private static final String MERCHANT_ID_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-merchant-id-allow";

    private static final String PAN_ENTRY_MODE_RULE_ID = "sr_evaluation_pan_entry_mode";

    private static final String PAN_ENTRY_MODE_RULE_DIGEST = "sha256:spend-rule-evaluation-pan-entry-mode";

    private static final String PAN_ENTRY_MODE_ALLOW_RULE_ID = "sr_evaluation_pan_entry_mode_allow";

    private static final String PAN_ENTRY_MODE_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-pan-entry-mode-allow";

    private static final String CONTROL_SCOPE_ID = "scope_spend_rule_evaluation";

    private static final String PERIOD_ID = "2026-07";

    private static final String TARGET_ACCOUNT_ID = "credit_spend_rule_evaluation";

    private static final String BUSINESS_SCENE = "SPEND_RULE_EVALUATION";

    private static final String BUSINESS_SN = "SPEND_RULE_EVALUATION_001";

    private static final String RULE_SPEC = """
            {"limitSpec":{"amountLimit":{"amount":100,"currency":"USD"}}}
            """;

    private static final String PERIOD_RULE_SPEC = """
            {"counterSpec":{"windowMode":"CALENDAR_MONTH","aggregationBasis":"AUTHORIZED_AMOUNT"},"limitSpec":{"amountLimit":{"amount":100,"currency":"USD"}}}
            """;

    private static final String COUNT_RULE_SPEC = """
            {"counterSpec":{"windowMode":"CALENDAR_MONTH","aggregationBasis":"AUTHORIZATION_COUNT"},"limitSpec":{"countLimit":{"maxCount":3}}}
            """;

    private static final String MCC_RULE_SPEC = """
            {"limitSpec":{"merchantCategoryControl":{"deniedMccCodes":["7995","6051"],"allowedMccCodes":[]}}}
            """;

    private static final String MCC_ALLOW_RULE_SPEC = """
            {"limitSpec":{"merchantCategoryControl":{"allowedMccCodes":["5812"]}}}
            """;

    private static final String COUNTRY_RULE_SPEC = """
            {"limitSpec":{"merchantCountryControl":{"deniedCountryCodes":["CU","IR"],"allowedCountryCodes":[]}}}
            """;

    private static final String COUNTRY_ALLOW_RULE_SPEC = """
            {"limitSpec":{"merchantCountryControl":{"allowedCountryCodes":["US"]}}}
            """;

    private static final String CARD_DATA_INPUT_RULE_SPEC = """
            {"limitSpec":{"cardDataInputCapabilityControl":{"deniedCardDataInputCapabilities":["MAGNETIC_STRIPE"],"allowedCardDataInputCapabilities":[]}}}
            """;

    private static final String CARD_DATA_INPUT_ALLOW_RULE_SPEC = """
            {"limitSpec":{"cardDataInputCapabilityControl":{"allowedCardDataInputCapabilities":["EMV_CHIP"]}}}
            """;

    private static final String MERCHANT_ID_RULE_SPEC = """
            {"limitSpec":{"merchantIdControl":{"deniedMerchantIds":["MID-RISK-001"],"allowedMerchantIds":[]}}}
            """;

    private static final String MERCHANT_ID_ALLOW_RULE_SPEC = """
            {"limitSpec":{"merchantIdControl":{"allowedMerchantIds":["MID-CONTRACT-001"]}}}
            """;

    private static final String PAN_ENTRY_MODE_RULE_SPEC = """
            {"limitSpec":{"panEntryModeControl":{"deniedPanEntryModes":["MANUAL"],"allowedPanEntryModes":[]}}}
            """;

    private static final String PAN_ENTRY_MODE_ALLOW_RULE_SPEC = """
            {"limitSpec":{"panEntryModeControl":{"allowedPanEntryModes":["CONTACTLESS"]}}}
            """;

    @Autowired
    private SpendRuleDefinitionService spendRuleDefinitionService;

    @Autowired
    private SpendRuleEvaluationApplicationService spendRuleEvaluationApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：单笔金额超过已发布规则版本的单笔限额。
     * 输入：规则限额 100 USD，请求评估金额 101 USD。
     * 输出：返回拒绝评估结论和稳定决策摘要候选。
     * 红线：评估只读，不写决策记录、控制额度变动、资金交易、route、posting、LedgerEntry 或账本投影。
     */
    @Test
    void testEvaluateSingleAmountLimitShouldRejectWithoutFundsSideEffect() {
        publishRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision =
                spendRuleEvaluationApplicationService.evaluate(evaluateRequest().setAmount(101L));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("超过单笔限额");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(decision.getAmount()).isEqualTo(101L);
        assertNoEvaluationSideEffects(before);
    }

    /**
     * 场景：单笔金额等于已发布规则版本的单笔限额。
     * 输入：规则限额 100 USD，请求评估金额 100 USD。
     * 输出：返回通过评估结论；重复评估的决策摘要稳定一致。
     * 红线：评估通过也不代表授权成功，不写决策记录、控制额度变动或资金事实。
     */
    @Test
    void testEvaluateSingleAmountLimitShouldPassWithStableDigestWithoutFundsSideEffect() {
        publishRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO first =
                spendRuleEvaluationApplicationService.evaluate(evaluateRequest().setAmount(100L));
        SpendRuleEvaluationDecisionDTO replayed =
                spendRuleEvaluationApplicationService.evaluate(evaluateRequest().setAmount(100L));

        assertThat(first.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(first.getRejectReason()).isNull();
        assertThat(first.getDecisionDigest()).startsWith("sha256:");
        assertThat(replayed.getDecisionDigest()).isEqualTo(first.getDecisionDigest());
        assertThat(first.getRuleId()).isEqualTo(RULE_ID);
        assertThat(first.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(first.getAmount()).isEqualTo(100L);
        assertNoEvaluationSideEffects(before);
    }

    /**
     * 场景：周期金额规则下当前周期可用控制额度不足。
     * 输入：周期额度 100 USD，既有控制占用 80 USD，请求评估金额 30 USD。
     * 输出：返回拒绝评估结论。
     * 红线：周期评估只读预算控制投影，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePeriodAmountLimitShouldRejectByProjectionWithoutFundsSideEffect() {
        publishPeriodRuleVersion();
        insertSpendControlMovement("limit_period", SpendControlMovementType.LIMIT_INCREASED, 100L);
        insertSpendControlMovement("reserved_period", SpendControlMovementType.RESERVED, 80L);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluatePeriodRequest().setAmount(30L));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("周期可用额度不足");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(PERIOD_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(countSpendControlMovement(PERIOD_RULE_ID)).isEqualTo(2);
        assertNoSpendRuleDecisionRecord(PERIOD_RULE_ID);
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：周期次数规则下当前周期次数已达到上限。
     * 输入：周期次数上限 3，当前周期已有 3 条控制占用流水，请求评估下一笔授权。
     * 输出：返回拒绝评估结论。
     * 红线：周期次数评估只读既有控制流水，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePeriodCountLimitShouldRejectByMovementCountWithoutFundsSideEffect() {
        publishCountRuleVersion();
        insertSpendControlMovement(COUNT_RULE_ID, "count_period_001", SpendControlMovementType.RESERVED, 1L);
        insertSpendControlMovement(COUNT_RULE_ID, "count_period_002", SpendControlMovementType.RESERVED, 1L);
        insertSpendControlMovement(COUNT_RULE_ID, "count_period_003", SpendControlMovementType.RESERVED, 1L);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluatePeriodRequest().setRuleId(COUNT_RULE_ID).setAmount(1L));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("周期次数超限");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(COUNT_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(countSpendControlMovement(COUNT_RULE_ID)).isEqualTo(3);
        assertNoSpendRuleDecisionRecord(COUNT_RULE_ID);
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一笔授权先占用后消费，只应计为一次周期次数。
     * 输入：周期次数上限 3，当前周期已有 2 笔授权尝试，其中第 1 笔同时存在 RESERVED 和 CONSUMED 生命周期流水。
     * 输出：下一笔授权评估仍返回通过。
     * 红线：周期次数评估按原占用流水去重，不把同一授权生命周期的多条控制流水重复计数。
     */
    @Test
    void testEvaluatePeriodCountLimitShouldDeduplicateReservedAndConsumedLifecycle() {
        publishCountRuleVersion();
        insertSpendControlMovement(COUNT_RULE_ID, "count_lifecycle_001", SpendControlMovementType.RESERVED, 1L);
        insertSpendControlMovement(COUNT_RULE_ID, "count_lifecycle_001_consumed", SpendControlMovementType.CONSUMED,
                1L, "count_lifecycle_001", "tx_count_lifecycle_001");
        insertSpendControlMovement(COUNT_RULE_ID, "count_lifecycle_002", SpendControlMovementType.RESERVED, 1L);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluatePeriodRequest().setRuleId(COUNT_RULE_ID).setAmount(1L));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(COUNT_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(countSpendControlMovement(COUNT_RULE_ID)).isEqualTo(3);
        assertNoSpendRuleDecisionRecord(COUNT_RULE_ID);
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：MCC 黑名单规则命中拒绝。
     * 输入：规则拒绝 MCC 7995，请求评估 MCC 为 7995。
     * 输出：返回拒绝评估结论。
     * 红线：MCC 评估只读请求事实和已发布规则版本，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantCategoryDeniedMccShouldRejectWithoutFundsSideEffect() {
        publishMccRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MCC_RULE_ID)
                        .setMerchantCategoryCode("7995"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("商户类别不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(MCC_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(MCC_RULE_ID);
        assertThat(countSpendControlMovement(MCC_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：MCC 不在白名单规则中。
     * 输入：规则只允许 MCC 5812，请求评估 MCC 为 7995。
     * 输出：返回拒绝评估结论。
     * 红线：白名单评估缺省黑名单时应按空集合处理，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantCategoryAllowListMissShouldRejectWithoutFundsSideEffect() {
        publishMccAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MCC_ALLOW_RULE_ID)
                        .setMerchantCategoryCode("7995"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("商户类别不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(MCC_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(MCC_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(MCC_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：MCC 命中白名单规则。
     * 输入：规则只允许 MCC 5812，请求评估 MCC 为 5812。
     * 输出：返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantCategoryAllowListHitShouldPassWithoutFundsSideEffect() {
        publishMccAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MCC_ALLOW_RULE_ID)
                        .setMerchantCategoryCode("5812"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(MCC_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(MCC_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(MCC_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：商户国家黑名单规则命中拒绝。
     * 输入：规则拒绝国家 CU，请求评估商户国家为 CU。
     * 输出：返回拒绝评估结论。
     * 红线：国家评估只读请求事实和已发布规则版本，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantCountryDeniedShouldRejectWithoutFundsSideEffect() {
        publishCountryRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(COUNTRY_RULE_ID)
                        .setMerchantCountryCode("CU"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("商户国家不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(COUNTRY_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(COUNTRY_RULE_ID);
        assertThat(countSpendControlMovement(COUNTRY_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：商户国家命中白名单规则。
     * 输入：规则只允许国家 US，请求评估商户国家为 us。
     * 输出：返回通过评估结论。
     * 红线：国家大小写归一化后判断，评估通过仍不代表授权成功。
     */
    @Test
    void testEvaluateMerchantCountryAllowListHitShouldPassWithoutFundsSideEffect() {
        publishCountryAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(COUNTRY_ALLOW_RULE_ID)
                        .setMerchantCountryCode("us"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(COUNTRY_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(COUNTRY_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(COUNTRY_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业员工卡拒绝磁条降级交易。
     * 输入：规则拒绝卡数据输入能力 MAGNETIC_STRIPE，请求评估卡数据输入能力为 MAGNETIC_STRIPE。
     * 输出：返回拒绝评估结论。
     * 红线：卡数据输入能力评估只读请求事实和已发布规则版本，不保存 PAN/CVV，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCardDataInputCapabilityDeniedShouldRejectWithoutFundsSideEffect() {
        publishCardDataInputRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(CARD_DATA_INPUT_RULE_ID)
                        .setCardDataInputCapability("MAGNETIC_STRIPE"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("卡数据输入能力不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(CARD_DATA_INPUT_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(CARD_DATA_INPUT_RULE_ID);
        assertThat(countSpendControlMovement(CARD_DATA_INPUT_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业员工卡允许芯片交易。
     * 输入：规则只允许卡数据输入能力 EMV_CHIP，请求评估卡数据输入能力为 emv_chip。
     * 输出：大小写归一化后返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCardDataInputCapabilityAllowListHitShouldPassWithoutFundsSideEffect() {
        publishCardDataInputAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(CARD_DATA_INPUT_ALLOW_RULE_ID)
                        .setCardDataInputCapability("emv_chip"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(CARD_DATA_INPUT_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(CARD_DATA_INPUT_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(CARD_DATA_INPUT_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡拒绝风险商户标识。
     * 输入：规则拒绝商户标识 MID-RISK-001，请求评估商户标识为 MID-RISK-001。
     * 输出：返回拒绝评估结论。
     * 红线：商户标识评估只读请求事实和已发布规则版本，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantIdDeniedShouldRejectWithoutFundsSideEffect() {
        publishMerchantIdRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MERCHANT_ID_RULE_ID)
                        .setMerchantId("MID-RISK-001"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("商户标识不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(MERCHANT_ID_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(MERCHANT_ID_RULE_ID);
        assertThat(countSpendControlMovement(MERCHANT_ID_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡只允许指定合作商户标识。
     * 输入：规则只允许商户标识 MID-CONTRACT-001，请求评估商户标识为 MID-CONTRACT-001。
     * 输出：返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantIdAllowListHitShouldPassWithoutFundsSideEffect() {
        publishMerchantIdAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MERCHANT_ID_ALLOW_RULE_ID)
                        .setMerchantId("MID-CONTRACT-001"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(MERCHANT_ID_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(MERCHANT_ID_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(MERCHANT_ID_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡拒绝手工录入卡信息交易。
     * 输入：规则拒绝 PAN 录入方式 MANUAL，请求评估 PAN 录入方式为 manual。
     * 输出：大小写归一化后返回拒绝评估结论。
     * 红线：PAN 录入方式评估只读请求事实和已发布规则版本，不保存 PAN 原文，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePanEntryModeDeniedShouldRejectWithoutFundsSideEffect() {
        publishPanEntryModeRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(PAN_ENTRY_MODE_RULE_ID)
                        .setPanEntryMode("manual"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("PAN 录入方式不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(PAN_ENTRY_MODE_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(PAN_ENTRY_MODE_RULE_ID);
        assertThat(countSpendControlMovement(PAN_ENTRY_MODE_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡只允许非接触式录入。
     * 输入：规则只允许 PAN 录入方式 CONTACTLESS，请求评估 PAN 录入方式为 contactless。
     * 输出：大小写归一化后返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePanEntryModeAllowListHitShouldPassWithoutFundsSideEffect() {
        publishPanEntryModeAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(PAN_ENTRY_MODE_ALLOW_RULE_ID)
                        .setPanEntryMode("contactless"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(PAN_ENTRY_MODE_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(PAN_ENTRY_MODE_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(PAN_ENTRY_MODE_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private void assertNoEvaluationSideEffects(LedgerFactSnapshot before) {
        assertNoSpendRuleDecisionRecord();
        assertNoSpendControlMovement();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendRuleEvaluationTestData() {
        cleanupSpendRuleEvaluationTestData();
    }

    @AfterEach
    void tearDownSpendRuleEvaluationTestData() {
        cleanupSpendRuleEvaluationTestData();
    }

    private void publishRuleVersion() {
        publishRuleVersion(RULE_ID, RULE_DIGEST, RULE_SPEC);
    }

    private void publishPeriodRuleVersion() {
        publishRuleVersion(PERIOD_RULE_ID, PERIOD_RULE_DIGEST, PERIOD_RULE_SPEC);
    }

    private void publishCountRuleVersion() {
        publishRuleVersion(COUNT_RULE_ID, COUNT_RULE_DIGEST, COUNT_RULE_SPEC, SpendRuleType.COUNT_LIMIT, "周期次数限额");
    }

    private void publishMccRuleVersion() {
        publishRuleVersion(MCC_RULE_ID, MCC_RULE_DIGEST, MCC_RULE_SPEC, SpendRuleType.MERCHANT_CATEGORY,
                "MCC 黑名单控制");
    }

    private void publishMccAllowRuleVersion() {
        publishRuleVersion(MCC_ALLOW_RULE_ID, MCC_ALLOW_RULE_DIGEST, MCC_ALLOW_RULE_SPEC,
                SpendRuleType.MERCHANT_CATEGORY, "MCC 白名单控制");
    }

    private void publishCountryRuleVersion() {
        publishRuleVersion(COUNTRY_RULE_ID, COUNTRY_RULE_DIGEST, COUNTRY_RULE_SPEC,
                SpendRuleType.COUNTRY, "商户国家黑名单控制");
    }

    private void publishCountryAllowRuleVersion() {
        publishRuleVersion(COUNTRY_ALLOW_RULE_ID, COUNTRY_ALLOW_RULE_DIGEST, COUNTRY_ALLOW_RULE_SPEC,
                SpendRuleType.COUNTRY, "商户国家白名单控制");
    }

    private void publishCardDataInputRuleVersion() {
        publishRuleVersion(CARD_DATA_INPUT_RULE_ID, CARD_DATA_INPUT_RULE_DIGEST, CARD_DATA_INPUT_RULE_SPEC,
                SpendRuleType.CARD_DATA_INPUT_CAPABILITY, "卡数据输入能力黑名单控制");
    }

    private void publishCardDataInputAllowRuleVersion() {
        publishRuleVersion(CARD_DATA_INPUT_ALLOW_RULE_ID, CARD_DATA_INPUT_ALLOW_RULE_DIGEST,
                CARD_DATA_INPUT_ALLOW_RULE_SPEC, SpendRuleType.CARD_DATA_INPUT_CAPABILITY, "卡数据输入能力白名单控制");
    }

    private void publishMerchantIdRuleVersion() {
        publishRuleVersion(MERCHANT_ID_RULE_ID, MERCHANT_ID_RULE_DIGEST, MERCHANT_ID_RULE_SPEC,
                SpendRuleType.MERCHANT_ID, "商户标识黑名单控制");
    }

    private void publishMerchantIdAllowRuleVersion() {
        publishRuleVersion(MERCHANT_ID_ALLOW_RULE_ID, MERCHANT_ID_ALLOW_RULE_DIGEST, MERCHANT_ID_ALLOW_RULE_SPEC,
                SpendRuleType.MERCHANT_ID, "商户标识白名单控制");
    }

    private void publishPanEntryModeRuleVersion() {
        publishRuleVersion(PAN_ENTRY_MODE_RULE_ID, PAN_ENTRY_MODE_RULE_DIGEST, PAN_ENTRY_MODE_RULE_SPEC,
                SpendRuleType.PAN_ENTRY_MODE, "PAN 录入方式黑名单控制");
    }

    private void publishPanEntryModeAllowRuleVersion() {
        publishRuleVersion(PAN_ENTRY_MODE_ALLOW_RULE_ID, PAN_ENTRY_MODE_ALLOW_RULE_DIGEST,
                PAN_ENTRY_MODE_ALLOW_RULE_SPEC, SpendRuleType.PAN_ENTRY_MODE, "PAN 录入方式白名单控制");
    }

    private void publishRuleVersion(String ruleId, String ruleDigest, String ruleSpec) {
        publishRuleVersion(ruleId, ruleDigest, ruleSpec, SpendRuleType.AMOUNT_LIMIT, "单笔授权限额");
    }

    private void publishRuleVersion(String ruleId,
                                    String ruleDigest,
                                    String ruleSpec,
                                    SpendRuleType ruleType,
                                    String ruleName) {
        spendRuleDefinitionService.createDefinition(new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(ruleId)
                .setRuleName(ruleName)
                .setRuleType(ruleType)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("用于验证 Spend Rule 评估服务"));
        spendRuleDefinitionService.publishVersion(new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(ruleId)
                .setRuleVersion(RULE_VERSION)
                .setRuleSpec(ruleSpec)
                .setRuleDigest(ruleDigest)
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:SR-HN-002-SPEND-RULE-LIGHTWEIGHT-EVALUATOR")
                .setDescription("发布单笔限额规则版本"));
    }

    private EvaluateSpendRuleRequest evaluateRequest() {
        return new EvaluateSpendRuleRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN);
    }

    private EvaluateSpendRuleRequest evaluatePeriodRequest() {
        return evaluateRequest()
                .setRuleId(PERIOD_RULE_ID)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(PERIOD_ID)
                .setTargetAccountId(FundsAccountId.immutable(TARGET_ACCOUNT_ID, FundsSubjectType.CREDIT_ACCOUNT));
    }

    private void insertSpendControlMovement(String movementSn,
                                            SpendControlMovementType movementType,
                                            Long amount) {
        insertSpendControlMovement(PERIOD_RULE_ID, movementSn, movementType, amount);
    }

    private void insertSpendControlMovement(String ruleId,
                                            String movementSn,
                                            SpendControlMovementType movementType,
                                            Long amount) {
        insertSpendControlMovement(ruleId, movementSn, movementType, amount, null, null);
    }

    private void insertSpendControlMovement(String ruleId,
                                            String movementSn,
                                            SpendControlMovementType movementType,
                                            Long amount,
                                            String originalMovementSn,
                                            String transactionSn) {
        jdbcTemplate.update("""
                INSERT INTO t_spend_control_movement(
                    movement_sn, tenant_id, movement_type, business_scene, business_sn,
                    original_movement_sn, transaction_sn, instrument_sn, action, target_subject_id,
                    target_subject_type, amount, currency, spend_rule_id, spend_rule_version,
                    spend_decision_sn, spend_decision_result, spend_decision_digest, budget_group_sn,
                    period_id, reason_code, operator_id, audit_reference_sn, movement_digest)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movementSn,
                TENANT_ID,
                movementType.name(),
                BUSINESS_SCENE,
                movementSn + "_business",
                originalMovementSn,
                transactionSn,
                "card_spend_rule_evaluation",
                PaymentInstrumentAction.AUTHORIZE.name(),
                TARGET_ACCOUNT_ID,
                FundsSubjectType.CREDIT_ACCOUNT.name(),
                amount,
                CurrencyIsoCode.USD.name(),
                ruleId,
                RULE_VERSION,
                "decision_" + movementSn,
                SpendControlDecisionResult.PASSED.name(),
                "sha256:decision-" + movementSn,
                CONTROL_SCOPE_ID,
                PERIOD_ID,
                "PERIOD_LIMIT_TEST",
                "codex",
                "grant:SR-HN-002-SPEND-RULE-LIGHTWEIGHT-EVALUATOR",
                "sha256:movement-" + movementSn);
    }

    private void cleanupSpendRuleEvaluationTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, PERIOD_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, COUNT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, MCC_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, MCC_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, COUNTRY_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, COUNTRY_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, MERCHANT_ID_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, MERCHANT_ID_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PERIOD_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PERIOD_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PERIOD_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PERIOD_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MCC_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, COUNTRY_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, CARD_DATA_INPUT_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, MERCHANT_ID_ALLOW_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, PAN_ENTRY_MODE_ALLOW_RULE_ID);
    }

    private void assertNoSpendRuleDecisionRecord() {
        assertNoSpendRuleDecisionRecord(RULE_ID);
    }

    private void assertNoSpendRuleDecisionRecord(String ruleId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_rule_decision_record
                WHERE tenant_id = ? AND rule_id = ?
                """, Integer.class, TENANT_ID, ruleId);
        assertThat(count).isZero();
    }

    private void assertNoSpendControlMovement() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_control_movement
                WHERE tenant_id = ? AND spend_rule_id = ?
                """, Integer.class, TENANT_ID, RULE_ID);
        assertThat(count).isZero();
    }

    private Integer countSpendControlMovement(String ruleId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_control_movement
                WHERE tenant_id = ? AND spend_rule_id = ?
                """, Integer.class, TENANT_ID, ruleId);
    }

    private void assertNoTransactionFacts() {
        assertThat(countRows("t_funds_transaction")).isZero();
        assertThat(countRows("t_funds_transaction_detail")).isZero();
        assertThat(postingPlanCount()).isZero();
        assertThat(countRows("t_ledger_transaction")).isZero();
        assertThat(countRows("t_ledger_entry")).isZero();
    }

    private Integer postingPlanCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    @Configuration
    @Import({
            SpendRuleDefinitionServiceImpl.class,
            SpendRuleVersionServiceImpl.class,
            SpendRuleAssignmentServiceImpl.class,
            SpendControlMovementServiceImpl.class,
            SpendRuleEvaluationApplicationServiceImpl.class
    })
    static class Config {

        @Bean
        FundsAccountQueryService fundsAccountQueryService() {
            return new FundsAccountQueryService() {

                @Override
                public FundsAccount getAccount(FundsAccountId accountId) {
                    throw new UnsupportedOperationException("Spend Rule evaluation projection test does not write movement");
                }

                @Override
                public FundsAccountBalanceView getBalance(FundsAccountId accountId) {
                    throw new UnsupportedOperationException("Spend Rule evaluation projection test does not query balance");
                }

                @Override
                public boolean supports(FundsAccountId accountId) {
                    throw new UnsupportedOperationException("Spend Rule evaluation projection test does not check support");
                }
            };
        }
    }
}

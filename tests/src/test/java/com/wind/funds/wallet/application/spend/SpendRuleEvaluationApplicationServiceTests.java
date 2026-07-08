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

import java.time.LocalDateTime;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final String ROLLING_COUNT_RULE_ID = "sr_evaluation_rolling_count";

    private static final String ROLLING_COUNT_RULE_DIGEST = "sha256:spend-rule-evaluation-rolling-count";

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

    private static final String POS_CATEGORY_RULE_ID = "sr_evaluation_pos_category";

    private static final String POS_CATEGORY_RULE_DIGEST = "sha256:spend-rule-evaluation-pos-category";

    private static final String POS_CATEGORY_ALLOW_RULE_ID = "sr_evaluation_pos_category_allow";

    private static final String POS_CATEGORY_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-pos-category-allow";

    private static final String CVV_REQUIRED_RULE_ID = "sr_evaluation_cvv_required";

    private static final String CVV_REQUIRED_RULE_DIGEST = "sha256:spend-rule-evaluation-cvv-required";

    private static final String PROCESSING_TYPE_RULE_ID = "sr_evaluation_processing_type";

    private static final String PROCESSING_TYPE_RULE_DIGEST = "sha256:spend-rule-evaluation-processing-type";

    private static final String PROCESSING_TYPE_ALLOW_RULE_ID = "sr_evaluation_processing_type_allow";

    private static final String PROCESSING_TYPE_ALLOW_RULE_DIGEST = "sha256:spend-rule-evaluation-processing-type-allow";

    private static final String POSTAL_CODE_VERIFICATION_RULE_ID = "sr_evaluation_postal_code_verification";

    private static final String POSTAL_CODE_VERIFICATION_RULE_DIGEST =
            "sha256:spend-rule-evaluation-postal-code-verification";

    private static final String POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID =
            "sr_evaluation_postal_code_verification_allow";

    private static final String POSTAL_CODE_VERIFICATION_ALLOW_RULE_DIGEST =
            "sha256:spend-rule-evaluation-postal-code-verification-allow";

    private static final String CURRENCY_RULE_ID = "sr_evaluation_currency";

    private static final String CURRENCY_RULE_DIGEST = "sha256:spend-rule-evaluation-currency";

    private static final String TIME_WINDOW_RULE_ID = "sr_evaluation_time_window";

    private static final String TIME_WINDOW_RULE_DIGEST = "sha256:spend-rule-evaluation-time-window";

    private static final String MULTI_CONTROL_RULE_ID = "sr_evaluation_multi_control";

    private static final String MULTI_CONTROL_RULE_DIGEST = "sha256:spend-rule-evaluation-multi-control";

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

    private static final String ROLLING_COUNT_RULE_SPEC = """
            {"counterSpec":{"windowMode":"ROLLING","windowSizeMinutes":15,"aggregationBasis":"AUTHORIZATION_COUNT"},"limitSpec":{"countLimit":{"maxCount":3}}}
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

    private static final String POS_CATEGORY_RULE_SPEC = """
            {"limitSpec":{"pointOfServiceCategoryControl":{"deniedPointOfServiceCategories":["AUTOMATED_TELLER_MACHINE"],"allowedPointOfServiceCategories":[]}}}
            """;

    private static final String POS_CATEGORY_ALLOW_RULE_SPEC = """
            {"limitSpec":{"pointOfServiceCategoryControl":{"allowedPointOfServiceCategories":["AUTOMATED_FUEL_DISPENSER"]}}}
            """;

    private static final String CVV_REQUIRED_RULE_SPEC = """
            {"limitSpec":{"cvvControl":{"required":true}}}
            """;

    private static final String PROCESSING_TYPE_RULE_SPEC = """
            {"limitSpec":{"cardTransactionProcessingTypeControl":{"deniedCardTransactionProcessingTypes":["PIN_CHANGE"],"allowedCardTransactionProcessingTypes":[]}}}
            """;

    private static final String PROCESSING_TYPE_ALLOW_RULE_SPEC = """
            {"limitSpec":{"cardTransactionProcessingTypeControl":{"allowedCardTransactionProcessingTypes":["CASH"]}}}
            """;

    private static final String POSTAL_CODE_VERIFICATION_RULE_SPEC = """
            {"limitSpec":{"postalCodeVerificationControl":{"deniedVerificationResults":["NO_MATCH"],"allowedVerificationResults":[]}}}
            """;

    private static final String POSTAL_CODE_VERIFICATION_ALLOW_RULE_SPEC = """
            {"limitSpec":{"postalCodeVerificationControl":{"allowedVerificationResults":["MATCH"]}}}
            """;

    private static final String CURRENCY_RULE_SPEC = """
            {"limitSpec":{"currencyControl":{"allowedCurrencies":["USD"],"deniedCurrencies":["EUR"]}}}
            """;

    private static final String TIME_WINDOW_RULE_SPEC = """
            {"limitSpec":{"timeWindowControl":{"allowedWindows":[{"startTime":"09:00","endTime":"18:00"}]}}}
            """;

    private static final String MULTI_CONTROL_RULE_SPEC = """
            {"limitSpec":{"amountLimit":{"amount":100,"currency":"USD"},"currencyControl":{"allowedCurrencies":["USD"]}}}
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
     * 场景：接入方误把多个控制项放进同一个已发布规则规格。
     * 输入：同一 ruleSpec 同时包含单笔金额限额和币种白名单，请求币种允许但金额超限。
     * 输出：评估服务 fail-fast，要求上游拆成多条规则并合成最终裁决。
     * 红线：不得静默只评估一个控制项并放过另一个控制项；失败仍不写决策记录、控制流水或资金事实。
     */
    @Test
    void testEvaluateMultiControlRuleSpecShouldFailFastWithoutFundsSideEffect() {
        publishMultiControlRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MULTI_CONTROL_RULE_ID)
                        .setAmount(101L)))
                .hasMessageContaining("仅支持单一控制项");

        assertNoSpendRuleDecisionRecord(MULTI_CONTROL_RULE_ID);
        assertThat(countSpendControlMovement(MULTI_CONTROL_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
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
     * 场景：企业卡 15 分钟滚动窗口内授权次数达到上限。
     * 输入：滚动窗口次数上限 3，过去 15 分钟已有 3 笔授权占用流水，请求评估下一笔授权。
     * 输出：返回拒绝评估结论。
     * 红线：滚动窗口评估只读既有控制流水，不要求周期标识，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateRollingCountLimitShouldRejectByWindowMovementsWithoutFundsSideEffect() {
        LocalDateTime authorizationTime = LocalDateTime.of(2026, 7, 1, 10, 0);
        publishRollingCountRuleVersion();
        insertSpendControlMovement(ROLLING_COUNT_RULE_ID, "rolling_count_001", SpendControlMovementType.RESERVED,
                1L, authorizationTime.minusMinutes(14));
        insertSpendControlMovement(ROLLING_COUNT_RULE_ID, "rolling_count_002", SpendControlMovementType.RESERVED,
                1L, authorizationTime.minusMinutes(10));
        insertSpendControlMovement(ROLLING_COUNT_RULE_ID, "rolling_count_003", SpendControlMovementType.RESERVED,
                1L, authorizationTime.minusMinutes(1));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRollingCountRequest(authorizationTime));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("滚动窗口次数超限");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(ROLLING_COUNT_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(countSpendControlMovement(ROLLING_COUNT_RULE_ID)).isEqualTo(3);
        assertNoSpendRuleDecisionRecord(ROLLING_COUNT_RULE_ID);
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡历史授权均已滑出 15 分钟滚动窗口。
     * 输入：滚动窗口次数上限 3，既有 3 笔授权占用流水均早于窗口起点。
     * 输出：返回通过评估结论。
     * 红线：滚动窗口不退化为周期次数统计，不因同周期历史流水误拒绝当前授权。
     */
    @Test
    void testEvaluateRollingCountLimitShouldIgnoreMovementsBeforeWindowStart() {
        LocalDateTime authorizationTime = LocalDateTime.of(2026, 7, 1, 10, 0);
        publishRollingCountRuleVersion();
        insertSpendControlMovement(ROLLING_COUNT_RULE_ID, "rolling_count_old_001",
                SpendControlMovementType.RESERVED, 1L, authorizationTime.minusMinutes(16));
        insertSpendControlMovement(ROLLING_COUNT_RULE_ID, "rolling_count_old_002",
                SpendControlMovementType.RESERVED, 1L, authorizationTime.minusMinutes(20));
        insertSpendControlMovement(ROLLING_COUNT_RULE_ID, "rolling_count_old_003",
                SpendControlMovementType.RESERVED, 1L, authorizationTime.minusMinutes(30));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRollingCountRequest(authorizationTime));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(ROLLING_COUNT_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(countSpendControlMovement(ROLLING_COUNT_RULE_ID)).isEqualTo(3);
        assertNoSpendRuleDecisionRecord(ROLLING_COUNT_RULE_ID);
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
     * 场景：MCC 命中白名单规则，且请求 MCC 带有前后空白。
     * 输入：规则只允许 MCC 5812，请求评估 MCC 为 " 5812 "。
     * 输出：按同一个 MCC 返回通过评估结论。
     * 红线：输入格式空白不能让白名单命中误拒，也不能新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateMerchantCategoryAllowListHitShouldTrimRequestMccWithoutFundsSideEffect() {
        publishMccAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(MCC_ALLOW_RULE_ID)
                        .setMerchantCategoryCode(" 5812 "));

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

    /**
     * 场景：企业差旅卡拒绝 ATM 终端消费。
     * 输入：规则拒绝 POS 类别 AUTOMATED_TELLER_MACHINE，请求评估 POS 类别为 automated_teller_machine。
     * 输出：大小写归一化后返回拒绝评估结论。
     * 红线：POS 类别评估只读请求事实和已发布规则版本，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePointOfServiceCategoryDeniedShouldRejectWithoutFundsSideEffect() {
        publishPosCategoryRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(POS_CATEGORY_RULE_ID)
                        .setPointOfServiceCategory("automated_teller_machine"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("POS 类别不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(POS_CATEGORY_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(POS_CATEGORY_RULE_ID);
        assertThat(countSpendControlMovement(POS_CATEGORY_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：车队卡只允许自助加油终端。
     * 输入：规则只允许 POS 类别 AUTOMATED_FUEL_DISPENSER，请求评估 POS 类别为 automated_fuel_dispenser。
     * 输出：大小写归一化后返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePointOfServiceCategoryAllowListHitShouldPassWithoutFundsSideEffect() {
        publishPosCategoryAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(POS_CATEGORY_ALLOW_RULE_ID)
                        .setPointOfServiceCategory("automated_fuel_dispenser"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(POS_CATEGORY_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(POS_CATEGORY_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(POS_CATEGORY_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：电商卡要求持卡人在交易时提供 CVV。
     * 输入：规则要求 CVV，请求未提供 CVV 事实。
     * 输出：返回拒绝评估结论。
     * 红线：CVV 规则只接收是否提供 CVV 的布尔事实，不保存 CVV 原文，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCvvRequiredMissingShouldRejectWithoutFundsSideEffect() {
        publishCvvRequiredRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(CVV_REQUIRED_RULE_ID)
                        .setCvvProvided(false));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("未提供 CVV");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(CVV_REQUIRED_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(CVV_REQUIRED_RULE_ID);
        assertThat(countSpendControlMovement(CVV_REQUIRED_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：电商卡要求 CVV 且请求已提供 CVV 事实。
     * 输入：规则要求 CVV，请求提供 CVV 事实。
     * 输出：返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不保存 CVV 原文，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCvvRequiredProvidedShouldPassWithoutFundsSideEffect() {
        publishCvvRequiredRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(CVV_REQUIRED_RULE_ID)
                        .setCvvProvided(true));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(CVV_REQUIRED_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(CVV_REQUIRED_RULE_ID);
        assertThat(countSpendControlMovement(CVV_REQUIRED_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡拒绝 PIN 变更类处理类型。
     * 输入：规则拒绝卡交易处理类型 PIN_CHANGE，请求评估处理类型为 pin_change。
     * 输出：大小写归一化后返回拒绝评估结论。
     * 红线：处理类型评估只读请求事实和已发布规则版本，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCardTransactionProcessingTypeDeniedShouldRejectWithoutFundsSideEffect() {
        publishProcessingTypeRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(PROCESSING_TYPE_RULE_ID)
                        .setCardTransactionProcessingType("pin_change"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("卡交易处理类型不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(PROCESSING_TYPE_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(PROCESSING_TYPE_RULE_ID);
        assertThat(countSpendControlMovement(PROCESSING_TYPE_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡只允许取现类处理类型。
     * 输入：规则只允许卡交易处理类型 CASH，请求评估处理类型为 cash。
     * 输出：大小写归一化后返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCardTransactionProcessingTypeAllowListHitShouldPassWithoutFundsSideEffect() {
        publishProcessingTypeAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(PROCESSING_TYPE_ALLOW_RULE_ID)
                        .setCardTransactionProcessingType("cash"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(PROCESSING_TYPE_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(PROCESSING_TYPE_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(PROCESSING_TYPE_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：电商卡要求 AVS 邮编校验命中，拒绝邮编校验不匹配的授权。
     * 输入：规则拒绝邮编校验结果 NO_MATCH，请求评估结果为 no_match。
     * 输出：大小写归一化后返回拒绝评估结论。
     * 红线：邮编校验规则只接收 AVS 校验结果，不保存邮编或街道地址原文，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePostalCodeVerificationNoMatchShouldRejectWithoutFundsSideEffect() {
        publishPostalCodeVerificationRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(POSTAL_CODE_VERIFICATION_RULE_ID)
                        .setPostalCodeVerificationResult("no_match"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("邮编校验结果不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(POSTAL_CODE_VERIFICATION_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(POSTAL_CODE_VERIFICATION_RULE_ID);
        assertThat(countSpendControlMovement(POSTAL_CODE_VERIFICATION_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：电商卡只允许 AVS 邮编校验匹配的授权。
     * 输入：规则只允许邮编校验结果 MATCH，请求评估结果为 match。
     * 输出：大小写归一化后返回通过评估结论。
     * 红线：评估通过仍不代表授权成功，不保存邮编或街道地址原文，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluatePostalCodeVerificationAllowListHitShouldPassWithoutFundsSideEffect() {
        publishPostalCodeVerificationAllowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID)
                        .setPostalCodeVerificationResult("match"));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID);
        assertThat(countSpendControlMovement(POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡只允许指定币种授权。
     * 输入：规则允许 USD 且拒绝 EUR，请求评估币种为 EUR。
     * 输出：返回拒绝评估结论。
     * 红线：币种控制只读请求事实和已发布规则版本，不新增控制流水、决策记录或资金事实。
     */
    @Test
    void testEvaluateCurrencyDeniedShouldRejectWithoutFundsSideEffect() {
        publishCurrencyRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(CURRENCY_RULE_ID)
                        .setCurrency(CurrencyIsoCode.EUR));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("币种不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(CURRENCY_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(CURRENCY_RULE_ID);
        assertThat(countSpendControlMovement(CURRENCY_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡使用允许币种授权。
     * 输入：规则允许 USD 且拒绝 EUR，请求评估币种为 USD。
     * 输出：返回通过评估结论。
     * 红线：评估通过只是准入前候选证据，不代表资金可用或授权成功。
     */
    @Test
    void testEvaluateCurrencyAllowedShouldPassWithoutFundsSideEffect() {
        publishCurrencyRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(CURRENCY_RULE_ID)
                        .setCurrency(CurrencyIsoCode.USD));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(CURRENCY_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(CURRENCY_RULE_ID);
        assertThat(countSpendControlMovement(CURRENCY_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡只允许工作时间段授权。
     * 输入：规则允许 09:00 到 18:00，请求评估时间为 20:30。
     * 输出：返回拒绝评估结论。
     * 红线：时间窗口控制只判断调用方传入的本地业务时间，不做时区换算、调度重置或资金事实写入。
     */
    @Test
    void testEvaluateTimeWindowOutsideAllowedWindowShouldRejectWithoutFundsSideEffect() {
        publishTimeWindowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(TIME_WINDOW_RULE_ID)
                        .setAuthorizationTime(LocalDateTime.of(2026, 7, 2, 20, 30)));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("时间窗口不允许");
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(TIME_WINDOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(TIME_WINDOW_RULE_ID);
        assertThat(countSpendControlMovement(TIME_WINDOW_RULE_ID)).isZero();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业卡在允许的工作时间段内授权。
     * 输入：规则允许 09:00 到 18:00，请求评估时间为 09:00。
     * 输出：返回通过评估结论。
     * 红线：窗口起点闭区间、终点开区间；评估通过仍不代表授权成功。
     */
    @Test
    void testEvaluateTimeWindowAtAllowedWindowStartShouldPassWithoutFundsSideEffect() {
        publishTimeWindowRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleEvaluationDecisionDTO decision = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest()
                        .setRuleId(TIME_WINDOW_RULE_ID)
                        .setAuthorizationTime(LocalDateTime.of(2026, 7, 2, 9, 0)));

        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getRejectReason()).isNull();
        assertThat(decision.getDecisionDigest()).startsWith("sha256:");
        assertThat(decision.getRuleId()).isEqualTo(TIME_WINDOW_RULE_ID);
        assertThat(decision.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertNoSpendRuleDecisionRecord(TIME_WINDOW_RULE_ID);
        assertThat(countSpendControlMovement(TIME_WINDOW_RULE_ID)).isZero();
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

    private void publishRollingCountRuleVersion() {
        publishRuleVersion(ROLLING_COUNT_RULE_ID, ROLLING_COUNT_RULE_DIGEST, ROLLING_COUNT_RULE_SPEC,
                SpendRuleType.COUNT_LIMIT, "滚动窗口次数限额");
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

    private void publishPosCategoryRuleVersion() {
        publishRuleVersion(POS_CATEGORY_RULE_ID, POS_CATEGORY_RULE_DIGEST, POS_CATEGORY_RULE_SPEC,
                SpendRuleType.POINT_OF_SERVICE_CATEGORY, "POS 类别黑名单控制");
    }

    private void publishPosCategoryAllowRuleVersion() {
        publishRuleVersion(POS_CATEGORY_ALLOW_RULE_ID, POS_CATEGORY_ALLOW_RULE_DIGEST,
                POS_CATEGORY_ALLOW_RULE_SPEC, SpendRuleType.POINT_OF_SERVICE_CATEGORY, "POS 类别白名单控制");
    }

    private void publishCvvRequiredRuleVersion() {
        publishRuleVersion(CVV_REQUIRED_RULE_ID, CVV_REQUIRED_RULE_DIGEST, CVV_REQUIRED_RULE_SPEC,
                SpendRuleType.CVV_REQUIRED, "CVV 必填控制");
    }

    private void publishProcessingTypeRuleVersion() {
        publishRuleVersion(PROCESSING_TYPE_RULE_ID, PROCESSING_TYPE_RULE_DIGEST, PROCESSING_TYPE_RULE_SPEC,
                SpendRuleType.CARD_TRANSACTION_PROCESSING_TYPE, "卡交易处理类型黑名单控制");
    }

    private void publishProcessingTypeAllowRuleVersion() {
        publishRuleVersion(PROCESSING_TYPE_ALLOW_RULE_ID, PROCESSING_TYPE_ALLOW_RULE_DIGEST,
                PROCESSING_TYPE_ALLOW_RULE_SPEC, SpendRuleType.CARD_TRANSACTION_PROCESSING_TYPE,
                "卡交易处理类型白名单控制");
    }

    private void publishPostalCodeVerificationRuleVersion() {
        publishRuleVersion(POSTAL_CODE_VERIFICATION_RULE_ID, POSTAL_CODE_VERIFICATION_RULE_DIGEST,
                POSTAL_CODE_VERIFICATION_RULE_SPEC, SpendRuleType.POSTAL_CODE_VERIFICATION,
                "邮编校验结果黑名单控制");
    }

    private void publishPostalCodeVerificationAllowRuleVersion() {
        publishRuleVersion(POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID, POSTAL_CODE_VERIFICATION_ALLOW_RULE_DIGEST,
                POSTAL_CODE_VERIFICATION_ALLOW_RULE_SPEC, SpendRuleType.POSTAL_CODE_VERIFICATION,
                "邮编校验结果白名单控制");
    }

    private void publishCurrencyRuleVersion() {
        publishRuleVersion(CURRENCY_RULE_ID, CURRENCY_RULE_DIGEST, CURRENCY_RULE_SPEC,
                SpendRuleType.CURRENCY, "币种控制");
    }

    private void publishTimeWindowRuleVersion() {
        publishRuleVersion(TIME_WINDOW_RULE_ID, TIME_WINDOW_RULE_DIGEST, TIME_WINDOW_RULE_SPEC,
                SpendRuleType.TIME_WINDOW, "时间窗口控制");
    }

    private void publishMultiControlRuleVersion() {
        publishRuleVersion(MULTI_CONTROL_RULE_ID, MULTI_CONTROL_RULE_DIGEST, MULTI_CONTROL_RULE_SPEC,
                SpendRuleType.AMOUNT_LIMIT, "复合控制规则");
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
                .setAuditReferenceSn("grant:SPEND-RULE-LIGHTWEIGHT-EVALUATOR")
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

    private EvaluateSpendRuleRequest evaluateRollingCountRequest(LocalDateTime authorizationTime) {
        return evaluatePeriodRequest()
                .setRuleId(ROLLING_COUNT_RULE_ID)
                .setPeriodId(null)
                .setAuthorizationTime(authorizationTime);
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
                                            LocalDateTime movementTime) {
        insertSpendControlMovement(ruleId, movementSn, movementType, amount, null, null, movementTime);
    }

    private void insertSpendControlMovement(String ruleId,
                                            String movementSn,
                                            SpendControlMovementType movementType,
                                            Long amount,
                                            String originalMovementSn,
                                            String transactionSn) {
        insertSpendControlMovement(ruleId, movementSn, movementType, amount, originalMovementSn, transactionSn, null);
    }

    private void insertSpendControlMovement(String ruleId,
                                            String movementSn,
                                            SpendControlMovementType movementType,
                                            Long amount,
                                            String originalMovementSn,
                                            String transactionSn,
                                            LocalDateTime movementTime) {
        LocalDateTime occurredAt = movementTime == null ? LocalDateTime.now() : movementTime;
        jdbcTemplate.update("""
                INSERT INTO t_spend_control_movement(
                    gmt_create, gmt_modified, movement_sn, tenant_id, movement_type, business_scene, business_sn,
                    original_movement_sn, transaction_sn, instrument_sn, action, target_subject_id,
                    target_subject_type, amount, currency, spend_rule_id, spend_rule_version,
                    spend_decision_sn, spend_decision_result, spend_decision_digest, budget_group_sn,
                    period_id, reason_code, operator_id, audit_reference_sn, movement_digest)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                occurredAt,
                occurredAt,
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
                "grant:SPEND-RULE-LIGHTWEIGHT-EVALUATOR",
                "sha256:movement-" + movementSn);
    }

    private void cleanupSpendRuleEvaluationTestData() {
        for (String ruleId : spendRuleEvaluationRuleIds()) {
            jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                    TENANT_ID, ruleId);
            jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                    TENANT_ID, ruleId);
            jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                    TENANT_ID, ruleId);
            jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                    TENANT_ID, ruleId);
            jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                    TENANT_ID, ruleId);
        }
    }

    private String[] spendRuleEvaluationRuleIds() {
        return new String[]{
                RULE_ID,
                PERIOD_RULE_ID,
                COUNT_RULE_ID,
                MCC_RULE_ID,
                MCC_ALLOW_RULE_ID,
                COUNTRY_RULE_ID,
                COUNTRY_ALLOW_RULE_ID,
                CARD_DATA_INPUT_RULE_ID,
                CARD_DATA_INPUT_ALLOW_RULE_ID,
                MERCHANT_ID_RULE_ID,
                MERCHANT_ID_ALLOW_RULE_ID,
                PAN_ENTRY_MODE_RULE_ID,
                PAN_ENTRY_MODE_ALLOW_RULE_ID,
                POS_CATEGORY_RULE_ID,
                POS_CATEGORY_ALLOW_RULE_ID,
                CVV_REQUIRED_RULE_ID,
                PROCESSING_TYPE_RULE_ID,
                PROCESSING_TYPE_ALLOW_RULE_ID,
                POSTAL_CODE_VERIFICATION_RULE_ID,
                POSTAL_CODE_VERIFICATION_ALLOW_RULE_ID,
                CURRENCY_RULE_ID,
                TIME_WINDOW_RULE_ID,
                MULTI_CONTROL_RULE_ID,
                ROLLING_COUNT_RULE_ID
        };
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

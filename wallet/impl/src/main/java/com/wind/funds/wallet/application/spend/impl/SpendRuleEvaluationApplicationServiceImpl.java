package com.wind.funds.wallet.application.spend.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.application.spend.SpendRuleEvaluationApplicationService;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.dto.SpendRuleEvaluationDecisionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.EvaluateSpendRuleRequest;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.service.SpendRuleVersionService;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spend Rule 规则评估应用服务实现。
 *
 * @author Codex
 * @date 2026-06-30
 */
@Service
@AllArgsConstructor
@Slf4j
public class SpendRuleEvaluationApplicationServiceImpl implements SpendRuleEvaluationApplicationService {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    private static final String AMOUNT_LIMIT_REJECT_REASON = "超过单笔限额";

    private static final String PERIOD_AMOUNT_REJECT_REASON = "周期可用额度不足";

    private static final String PERIOD_COUNT_REJECT_REASON = "周期次数超限";

    private static final String ROLLING_COUNT_REJECT_REASON = "滚动窗口次数超限";

    private static final String MERCHANT_CATEGORY_REJECT_REASON = "商户类别不允许";

    private static final String MERCHANT_ID_REJECT_REASON = "商户标识不允许";

    private static final String MERCHANT_COUNTRY_REJECT_REASON = "商户国家不允许";

    private static final String CARD_DATA_INPUT_CAPABILITY_REJECT_REASON = "卡数据输入能力不允许";

    private static final String CARD_TRANSACTION_PROCESSING_TYPE_REJECT_REASON = "卡交易处理类型不允许";

    private static final String CVV_REQUIRED_REJECT_REASON = "未提供 CVV";

    private static final String PAN_ENTRY_MODE_REJECT_REASON = "PAN 录入方式不允许";

    private static final String POINT_OF_SERVICE_CATEGORY_REJECT_REASON = "POS 类别不允许";

    private static final String POSTAL_CODE_VERIFICATION_REJECT_REASON = "邮编校验结果不允许";

    private static final String CURRENCY_REJECT_REASON = "币种不允许";

    private static final String TIME_WINDOW_REJECT_REASON = "时间窗口不允许";

    private static final String MULTIPLE_CONTROL_UNSUPPORTED_MESSAGE =
            "Spend Rule evaluator 仅支持单一控制项，复杂多规则裁决由上游合成";

    private static final String COUNTER_SPEC_KEY = "counterSpec";

    private static final String LIMIT_SPEC_KEY = "limitSpec";

    private static final String AMOUNT_LIMIT_KEY = "amountLimit";

    private static final String COUNT_LIMIT_KEY = "countLimit";

    private static final String MERCHANT_CATEGORY_CONTROL_KEY = "merchantCategoryControl";

    private static final String MERCHANT_ID_CONTROL_KEY = "merchantIdControl";

    private static final String MERCHANT_COUNTRY_CONTROL_KEY = "merchantCountryControl";

    private static final String CARD_DATA_INPUT_CAPABILITY_CONTROL_KEY = "cardDataInputCapabilityControl";

    private static final String CARD_TRANSACTION_PROCESSING_TYPE_CONTROL_KEY = "cardTransactionProcessingTypeControl";

    private static final String CVV_CONTROL_KEY = "cvvControl";

    private static final String PAN_ENTRY_MODE_CONTROL_KEY = "panEntryModeControl";

    private static final String POINT_OF_SERVICE_CATEGORY_CONTROL_KEY = "pointOfServiceCategoryControl";

    private static final String POSTAL_CODE_VERIFICATION_CONTROL_KEY = "postalCodeVerificationControl";

    private static final String CURRENCY_CONTROL_KEY = "currencyControl";

    private static final String TIME_WINDOW_CONTROL_KEY = "timeWindowControl";

    private static final String AMOUNT_KEY = "amount";

    private static final String CURRENCY_KEY = "currency";

    private static final String MAX_COUNT_KEY = "maxCount";

    private static final String WINDOW_MODE_KEY = "windowMode";

    private static final String WINDOW_SIZE_MINUTES_KEY = "windowSizeMinutes";

    private static final String ROLLING_WINDOW_MODE = "ROLLING";

    private static final String REQUIRED_KEY = "required";

    private static final String DENIED_MCC_CODES_KEY = "deniedMccCodes";

    private static final String ALLOWED_MCC_CODES_KEY = "allowedMccCodes";

    private static final String DENIED_MERCHANT_IDS_KEY = "deniedMerchantIds";

    private static final String ALLOWED_MERCHANT_IDS_KEY = "allowedMerchantIds";

    private static final String DENIED_COUNTRY_CODES_KEY = "deniedCountryCodes";

    private static final String ALLOWED_COUNTRY_CODES_KEY = "allowedCountryCodes";

    private static final String DENIED_CARD_DATA_INPUT_CAPABILITIES_KEY = "deniedCardDataInputCapabilities";

    private static final String ALLOWED_CARD_DATA_INPUT_CAPABILITIES_KEY = "allowedCardDataInputCapabilities";

    private static final String DENIED_CARD_TRANSACTION_PROCESSING_TYPES_KEY = "deniedCardTransactionProcessingTypes";

    private static final String ALLOWED_CARD_TRANSACTION_PROCESSING_TYPES_KEY = "allowedCardTransactionProcessingTypes";

    private static final String DENIED_PAN_ENTRY_MODES_KEY = "deniedPanEntryModes";

    private static final String ALLOWED_PAN_ENTRY_MODES_KEY = "allowedPanEntryModes";

    private static final String DENIED_POINT_OF_SERVICE_CATEGORIES_KEY = "deniedPointOfServiceCategories";

    private static final String ALLOWED_POINT_OF_SERVICE_CATEGORIES_KEY = "allowedPointOfServiceCategories";

    private static final String DENIED_VERIFICATION_RESULTS_KEY = "deniedVerificationResults";

    private static final String ALLOWED_VERIFICATION_RESULTS_KEY = "allowedVerificationResults";

    private static final String DENIED_CURRENCIES_KEY = "deniedCurrencies";

    private static final String ALLOWED_CURRENCIES_KEY = "allowedCurrencies";

    private static final String ALLOWED_WINDOWS_KEY = "allowedWindows";

    private static final String START_TIME_KEY = "startTime";

    private static final String END_TIME_KEY = "endTime";

    private final SpendRuleVersionService spendRuleVersionService;

    private final SpendControlMovementService spendControlMovementService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleEvaluationDecisionDTO evaluate(@NonNull EvaluateSpendRuleRequest request) {
        validateRequest(request);
        SpendRuleVersionDTO version = spendRuleVersionService.getPublishedVersion(
                request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        JSONObject ruleSpec = ruleSpecOf(version.getRuleSpec());
        assertSingleExecutableControl(ruleSpec);
        SpendControlDecisionResult result = evaluateRule(request, ruleSpec);
        String rejectReason = rejectReason(result, ruleSpec);
        SpendRuleEvaluationDecisionDTO decision = toDecision(request, result, rejectReason);
        log.info("Spend Rule 评估完成，tenantId={}, ruleId={}, ruleVersion={}, businessScene={}, businessSn={}, "
                        + "action={}, amount={}, currency={}, decisionResult={}, rejectReason={}, decisionDigest={}",
                request.getTenantId(), request.getRuleId(), request.getRuleVersion(), request.getBusinessScene(),
                request.getBusinessSn(), request.getAction(), request.getAmount(), request.getCurrency(),
                decision.getDecisionResult(), decision.getRejectReason(), decision.getDecisionDigest());
        return decision;
    }

    private void validateRequest(EvaluateSpendRuleRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getAmount(), "交易金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "交易金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
    }

    private JSONObject ruleSpecOf(String ruleSpec) {
        JSONObject result = JSON.parseObject(ruleSpec);
        AssertUtils.notNull(result, "Spend Rule 规则规格不能为空");
        return result;
    }

    private void assertSingleExecutableControl(JSONObject ruleSpec) {
        long controlCount = List.of(
                        hasCardDataInputCapabilityControl(ruleSpec),
                        hasCardTransactionProcessingTypeControl(ruleSpec),
                        hasCvvControl(ruleSpec),
                        hasPanEntryModeControl(ruleSpec),
                        hasPointOfServiceCategoryControl(ruleSpec),
                        hasPostalCodeVerificationControl(ruleSpec),
                        hasCurrencyControl(ruleSpec),
                        hasTimeWindowControl(ruleSpec),
                        hasMerchantIdControl(ruleSpec),
                        hasMerchantCountryControl(ruleSpec),
                        hasMerchantCategoryControl(ruleSpec),
                        hasCountLimit(ruleSpec),
                        hasAmountLimit(ruleSpec))
                .stream()
                .filter(Boolean::booleanValue)
                .count();
        AssertUtils.isTrue(controlCount <= 1L, MULTIPLE_CONTROL_UNSUPPORTED_MESSAGE);
    }

    private AmountLimit amountLimitOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject amountLimit = limitSpec.getJSONObject(AMOUNT_LIMIT_KEY);
        AssertUtils.notNull(amountLimit, "Spend Rule 规则规格缺少 limitSpec.amountLimit");
        Long amount = amountLimit.getLong(AMOUNT_KEY);
        String currencyCode = amountLimit.getString(CURRENCY_KEY);
        AssertUtils.notNull(amount, "Spend Rule 单笔限额金额不能为空");
        AssertUtils.isTrue(amount > 0L, "Spend Rule 单笔限额金额必须大于 0");
        AssertUtils.hasText(currencyCode, "Spend Rule 单笔限额币种不能为空");
        return new AmountLimit(amount, CurrencyIsoCode.valueOf(currencyCode));
    }

    private CountLimit countLimitOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject countLimit = limitSpec.getJSONObject(COUNT_LIMIT_KEY);
        AssertUtils.notNull(countLimit, "Spend Rule 规则规格缺少 limitSpec.countLimit");
        Integer maxCount = countLimit.getInteger(MAX_COUNT_KEY);
        AssertUtils.notNull(maxCount, "Spend Rule 周期次数上限不能为空");
        AssertUtils.isTrue(maxCount > 0, "Spend Rule 周期次数上限必须大于 0");
        return new CountLimit(maxCount);
    }

    private RollingWindow rollingWindowOf(JSONObject ruleSpec) {
        JSONObject counterSpec = ruleSpec.getJSONObject(COUNTER_SPEC_KEY);
        AssertUtils.notNull(counterSpec, "Spend Rule 滚动窗口规则规格缺少 counterSpec");
        Integer windowSizeMinutes = counterSpec.getInteger(WINDOW_SIZE_MINUTES_KEY);
        AssertUtils.notNull(windowSizeMinutes, "Spend Rule 滚动窗口分钟数不能为空");
        AssertUtils.isTrue(windowSizeMinutes > 0, "Spend Rule 滚动窗口分钟数必须大于 0");
        return new RollingWindow(windowSizeMinutes);
    }

    private SpendControlDecisionResult evaluateRule(EvaluateSpendRuleRequest request, JSONObject ruleSpec) {
        if (hasCardDataInputCapabilityControl(ruleSpec)) {
            return evaluateCardDataInputCapability(request, cardDataInputCapabilityControlOf(ruleSpec));
        }
        if (hasCardTransactionProcessingTypeControl(ruleSpec)) {
            return evaluateCardTransactionProcessingType(request, cardTransactionProcessingTypeControlOf(ruleSpec));
        }
        if (hasCvvControl(ruleSpec)) {
            return evaluateCvvRequired(request, cvvRequiredOf(ruleSpec));
        }
        if (hasPanEntryModeControl(ruleSpec)) {
            return evaluatePanEntryMode(request, panEntryModeControlOf(ruleSpec));
        }
        if (hasPointOfServiceCategoryControl(ruleSpec)) {
            return evaluatePointOfServiceCategory(request, pointOfServiceCategoryControlOf(ruleSpec));
        }
        if (hasPostalCodeVerificationControl(ruleSpec)) {
            return evaluatePostalCodeVerification(request, postalCodeVerificationControlOf(ruleSpec));
        }
        if (hasCurrencyControl(ruleSpec)) {
            return evaluateCurrency(request, currencyControlOf(ruleSpec));
        }
        if (hasTimeWindowControl(ruleSpec)) {
            return evaluateTimeWindow(request, timeWindowControlOf(ruleSpec));
        }
        if (hasMerchantIdControl(ruleSpec)) {
            return evaluateMerchantId(request, merchantIdControlOf(ruleSpec));
        }
        if (hasMerchantCountryControl(ruleSpec)) {
            return evaluateMerchantCountry(request, merchantCountryControlOf(ruleSpec));
        }
        if (hasMerchantCategoryControl(ruleSpec)) {
            return evaluateMerchantCategory(request, merchantCategoryControlOf(ruleSpec));
        }
        if (hasCountLimit(ruleSpec)) {
            CountLimit countLimit = countLimitOf(ruleSpec);
            if (isRollingWindowCounter(ruleSpec)) {
                return evaluateRollingCountLimit(request, countLimit, rollingWindowOf(ruleSpec));
            }
            return evaluatePeriodCountLimit(request, countLimit);
        }
        AmountLimit amountLimit = amountLimitOf(ruleSpec);
        assertSameCurrency(request, amountLimit);
        if (hasCounterSpec(ruleSpec)) {
            return evaluatePeriodAmountLimit(request);
        }
        return evaluateSingleAmountLimit(request, amountLimit);
    }

    private void assertSameCurrency(EvaluateSpendRuleRequest request, AmountLimit amountLimit) {
        AssertUtils.isTrue(request.getCurrency() == amountLimit.currency(),
                "Spend Rule 限额币种与请求币种不一致，ruleCurrency = {}，requestCurrency = {}",
                amountLimit.currency(),
                request.getCurrency());
    }

    private boolean hasCounterSpec(JSONObject ruleSpec) {
        return ruleSpec.getJSONObject(COUNTER_SPEC_KEY) != null;
    }

    private boolean isRollingWindowCounter(JSONObject ruleSpec) {
        JSONObject counterSpec = ruleSpec.getJSONObject(COUNTER_SPEC_KEY);
        if (counterSpec == null) {
            return false;
        }
        String windowMode = counterSpec.getString(WINDOW_MODE_KEY);
        return StringUtils.hasText(windowMode) && ROLLING_WINDOW_MODE.equals(normalizeUpperCode(windowMode));
    }

    private boolean hasCountLimit(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(COUNT_LIMIT_KEY) != null;
    }

    private boolean hasAmountLimit(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(AMOUNT_LIMIT_KEY) != null;
    }

    private boolean hasMerchantCategoryControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(MERCHANT_CATEGORY_CONTROL_KEY) != null;
    }

    private boolean hasMerchantIdControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(MERCHANT_ID_CONTROL_KEY) != null;
    }

    private boolean hasMerchantCountryControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(MERCHANT_COUNTRY_CONTROL_KEY) != null;
    }

    private boolean hasCardDataInputCapabilityControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(CARD_DATA_INPUT_CAPABILITY_CONTROL_KEY) != null;
    }

    private boolean hasCardTransactionProcessingTypeControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(CARD_TRANSACTION_PROCESSING_TYPE_CONTROL_KEY) != null;
    }

    private boolean hasCvvControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(CVV_CONTROL_KEY) != null;
    }

    private boolean hasPanEntryModeControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(PAN_ENTRY_MODE_CONTROL_KEY) != null;
    }

    private boolean hasPointOfServiceCategoryControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(POINT_OF_SERVICE_CATEGORY_CONTROL_KEY) != null;
    }

    private boolean hasPostalCodeVerificationControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(POSTAL_CODE_VERIFICATION_CONTROL_KEY) != null;
    }

    private boolean hasCurrencyControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(CURRENCY_CONTROL_KEY) != null;
    }

    private boolean hasTimeWindowControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(TIME_WINDOW_CONTROL_KEY) != null;
    }

    private MerchantCategoryControl merchantCategoryControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject merchantCategoryControl = limitSpec.getJSONObject(MERCHANT_CATEGORY_CONTROL_KEY);
        AssertUtils.notNull(merchantCategoryControl, "Spend Rule 规则规格缺少 limitSpec.merchantCategoryControl");
        return new MerchantCategoryControl(
                mccCodes(merchantCategoryControl, DENIED_MCC_CODES_KEY),
                mccCodes(merchantCategoryControl, ALLOWED_MCC_CODES_KEY));
    }

    private Set<String> mccCodes(JSONObject merchantCategoryControl, String key) {
        List<String> codes = merchantCategoryControl.getList(key, String.class);
        if (codes == null) {
            return Set.of();
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    private MerchantIdControl merchantIdControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject merchantIdControl = limitSpec.getJSONObject(MERCHANT_ID_CONTROL_KEY);
        AssertUtils.notNull(merchantIdControl, "Spend Rule 规则规格缺少 limitSpec.merchantIdControl");
        return new MerchantIdControl(
                merchantIds(merchantIdControl, DENIED_MERCHANT_IDS_KEY),
                merchantIds(merchantIdControl, ALLOWED_MERCHANT_IDS_KEY));
    }

    private Set<String> merchantIds(JSONObject merchantIdControl, String key) {
        List<String> merchantIds = merchantIdControl.getList(key, String.class);
        if (merchantIds == null) {
            return Set.of();
        }
        return merchantIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    private MerchantCountryControl merchantCountryControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject merchantCountryControl = limitSpec.getJSONObject(MERCHANT_COUNTRY_CONTROL_KEY);
        AssertUtils.notNull(merchantCountryControl, "Spend Rule 规则规格缺少 limitSpec.merchantCountryControl");
        return new MerchantCountryControl(
                countryCodes(merchantCountryControl, DENIED_COUNTRY_CODES_KEY),
                countryCodes(merchantCountryControl, ALLOWED_COUNTRY_CODES_KEY));
    }

    private Set<String> countryCodes(JSONObject merchantCountryControl, String key) {
        return normalizedCodes(merchantCountryControl, key);
    }

    private CardDataInputCapabilityControl cardDataInputCapabilityControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject cardDataInputCapabilityControl = limitSpec.getJSONObject(CARD_DATA_INPUT_CAPABILITY_CONTROL_KEY);
        AssertUtils.notNull(cardDataInputCapabilityControl,
                "Spend Rule 规则规格缺少 limitSpec.cardDataInputCapabilityControl");
        return new CardDataInputCapabilityControl(
                normalizedCodes(cardDataInputCapabilityControl, DENIED_CARD_DATA_INPUT_CAPABILITIES_KEY),
                normalizedCodes(cardDataInputCapabilityControl, ALLOWED_CARD_DATA_INPUT_CAPABILITIES_KEY));
    }

    private CardTransactionProcessingTypeControl cardTransactionProcessingTypeControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject cardTransactionProcessingTypeControl =
                limitSpec.getJSONObject(CARD_TRANSACTION_PROCESSING_TYPE_CONTROL_KEY);
        AssertUtils.notNull(cardTransactionProcessingTypeControl,
                "Spend Rule 规则规格缺少 limitSpec.cardTransactionProcessingTypeControl");
        return new CardTransactionProcessingTypeControl(
                normalizedCodes(cardTransactionProcessingTypeControl, DENIED_CARD_TRANSACTION_PROCESSING_TYPES_KEY),
                normalizedCodes(cardTransactionProcessingTypeControl, ALLOWED_CARD_TRANSACTION_PROCESSING_TYPES_KEY));
    }

    private PanEntryModeControl panEntryModeControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject panEntryModeControl = limitSpec.getJSONObject(PAN_ENTRY_MODE_CONTROL_KEY);
        AssertUtils.notNull(panEntryModeControl, "Spend Rule 规则规格缺少 limitSpec.panEntryModeControl");
        return new PanEntryModeControl(
                normalizedCodes(panEntryModeControl, DENIED_PAN_ENTRY_MODES_KEY),
                normalizedCodes(panEntryModeControl, ALLOWED_PAN_ENTRY_MODES_KEY));
    }

    private CvvRequired cvvRequiredOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject cvvControl = limitSpec.getJSONObject(CVV_CONTROL_KEY);
        AssertUtils.notNull(cvvControl, "Spend Rule 规则规格缺少 limitSpec.cvvControl");
        Boolean required = cvvControl.getBoolean(REQUIRED_KEY);
        AssertUtils.notNull(required, "Spend Rule CVV 必填配置不能为空");
        return new CvvRequired(required);
    }

    private PointOfServiceCategoryControl pointOfServiceCategoryControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject pointOfServiceCategoryControl = limitSpec.getJSONObject(POINT_OF_SERVICE_CATEGORY_CONTROL_KEY);
        AssertUtils.notNull(pointOfServiceCategoryControl,
                "Spend Rule 规则规格缺少 limitSpec.pointOfServiceCategoryControl");
        return new PointOfServiceCategoryControl(
                normalizedCodes(pointOfServiceCategoryControl, DENIED_POINT_OF_SERVICE_CATEGORIES_KEY),
                normalizedCodes(pointOfServiceCategoryControl, ALLOWED_POINT_OF_SERVICE_CATEGORIES_KEY));
    }

    private PostalCodeVerificationControl postalCodeVerificationControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject postalCodeVerificationControl = limitSpec.getJSONObject(POSTAL_CODE_VERIFICATION_CONTROL_KEY);
        AssertUtils.notNull(postalCodeVerificationControl,
                "Spend Rule 规则规格缺少 limitSpec.postalCodeVerificationControl");
        return new PostalCodeVerificationControl(
                normalizedCodes(postalCodeVerificationControl, DENIED_VERIFICATION_RESULTS_KEY),
                normalizedCodes(postalCodeVerificationControl, ALLOWED_VERIFICATION_RESULTS_KEY));
    }

    private CurrencyControl currencyControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject currencyControl = limitSpec.getJSONObject(CURRENCY_CONTROL_KEY);
        AssertUtils.notNull(currencyControl, "Spend Rule 规则规格缺少 limitSpec.currencyControl");
        return new CurrencyControl(
                currencies(currencyControl, DENIED_CURRENCIES_KEY),
                currencies(currencyControl, ALLOWED_CURRENCIES_KEY));
    }

    private Set<CurrencyIsoCode> currencies(JSONObject control, String key) {
        List<String> codes = control.getList(key, String.class);
        if (codes == null) {
            return Set.of();
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(SpendRuleEvaluationApplicationServiceImpl::normalizeUpperCode)
                .map(CurrencyIsoCode::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    private TimeWindowControl timeWindowControlOf(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        AssertUtils.notNull(limitSpec, "Spend Rule 规则规格缺少 limitSpec");
        JSONObject timeWindowControl = limitSpec.getJSONObject(TIME_WINDOW_CONTROL_KEY);
        AssertUtils.notNull(timeWindowControl, "Spend Rule 规则规格缺少 limitSpec.timeWindowControl");
        List<JSONObject> windows = timeWindowControl.getList(ALLOWED_WINDOWS_KEY, JSONObject.class);
        AssertUtils.isTrue(windows != null && !windows.isEmpty(), "Spend Rule 时间窗口不能为空");
        return new TimeWindowControl(windows.stream()
                .map(this::timeWindowOf)
                .collect(Collectors.toUnmodifiableList()));
    }

    private TimeWindow timeWindowOf(JSONObject window) {
        String startTime = window.getString(START_TIME_KEY);
        String endTime = window.getString(END_TIME_KEY);
        AssertUtils.hasText(startTime, "Spend Rule 时间窗口开始时间不能为空");
        AssertUtils.hasText(endTime, "Spend Rule 时间窗口结束时间不能为空");
        LocalTime parsedStartTime = LocalTime.parse(startTime);
        LocalTime parsedEndTime = LocalTime.parse(endTime);
        AssertUtils.isTrue(!parsedStartTime.equals(parsedEndTime), "Spend Rule 时间窗口开始和结束时间不能相同");
        return new TimeWindow(parsedStartTime, parsedEndTime);
    }

    private Set<String> normalizedCodes(JSONObject control, String key) {
        List<String> codes = control.getList(key, String.class);
        if (codes == null) {
            return Set.of();
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(SpendRuleEvaluationApplicationServiceImpl::normalizeUpperCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    private SpendControlDecisionResult evaluateSingleAmountLimit(EvaluateSpendRuleRequest request,
                                                                 AmountLimit amountLimit) {
        if (request.getAmount() > amountLimit.amount()) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateMerchantCategory(EvaluateSpendRuleRequest request,
                                                                MerchantCategoryControl control) {
        AssertUtils.hasText(request.getMerchantCategoryCode(), "商户类别规则评估 MCC 不能为空");
        String merchantCategoryCode = request.getMerchantCategoryCode().trim();
        if (control.deniedMccCodes().contains(merchantCategoryCode)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedMccCodes().isEmpty()
                && !control.allowedMccCodes().contains(merchantCategoryCode)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateMerchantId(EvaluateSpendRuleRequest request,
                                                          MerchantIdControl control) {
        AssertUtils.hasText(request.getMerchantId(), "商户标识规则评估 MID 不能为空");
        String merchantId = request.getMerchantId().trim();
        if (control.deniedMerchantIds().contains(merchantId)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedMerchantIds().isEmpty()
                && !control.allowedMerchantIds().contains(merchantId)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateMerchantCountry(EvaluateSpendRuleRequest request,
                                                               MerchantCountryControl control) {
        AssertUtils.hasText(request.getMerchantCountryCode(), "商户国家规则评估国家代码不能为空");
        String merchantCountryCode = normalizeUpperCode(request.getMerchantCountryCode());
        if (control.deniedCountryCodes().contains(merchantCountryCode)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedCountryCodes().isEmpty()
                && !control.allowedCountryCodes().contains(merchantCountryCode)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateCardDataInputCapability(EvaluateSpendRuleRequest request,
                                                                       CardDataInputCapabilityControl control) {
        AssertUtils.hasText(request.getCardDataInputCapability(), "卡数据输入能力规则评估卡数据输入能力不能为空");
        String cardDataInputCapability = normalizeUpperCode(request.getCardDataInputCapability());
        if (control.deniedCardDataInputCapabilities().contains(cardDataInputCapability)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedCardDataInputCapabilities().isEmpty()
                && !control.allowedCardDataInputCapabilities().contains(cardDataInputCapability)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateCardTransactionProcessingType(
            EvaluateSpendRuleRequest request,
            CardTransactionProcessingTypeControl control) {
        AssertUtils.hasText(request.getCardTransactionProcessingType(), "卡交易处理类型规则评估处理类型不能为空");
        String processingType = normalizeUpperCode(request.getCardTransactionProcessingType());
        if (control.deniedCardTransactionProcessingTypes().contains(processingType)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedCardTransactionProcessingTypes().isEmpty()
                && !control.allowedCardTransactionProcessingTypes().contains(processingType)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateCvvRequired(EvaluateSpendRuleRequest request,
                                                           CvvRequired cvvRequired) {
        if (cvvRequired.required() && !Boolean.TRUE.equals(request.getCvvProvided())) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluatePanEntryMode(EvaluateSpendRuleRequest request,
                                                            PanEntryModeControl control) {
        AssertUtils.hasText(request.getPanEntryMode(), "PAN 录入方式规则评估录入方式不能为空");
        String panEntryMode = normalizeUpperCode(request.getPanEntryMode());
        if (control.deniedPanEntryModes().contains(panEntryMode)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedPanEntryModes().isEmpty()
                && !control.allowedPanEntryModes().contains(panEntryMode)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluatePointOfServiceCategory(EvaluateSpendRuleRequest request,
                                                                      PointOfServiceCategoryControl control) {
        AssertUtils.hasText(request.getPointOfServiceCategory(), "POS 类别规则评估 POS 类别不能为空");
        String pointOfServiceCategory = normalizeUpperCode(request.getPointOfServiceCategory());
        if (control.deniedPointOfServiceCategories().contains(pointOfServiceCategory)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedPointOfServiceCategories().isEmpty()
                && !control.allowedPointOfServiceCategories().contains(pointOfServiceCategory)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluatePostalCodeVerification(EvaluateSpendRuleRequest request,
                                                                      PostalCodeVerificationControl control) {
        AssertUtils.hasText(request.getPostalCodeVerificationResult(), "邮编校验规则评估校验结果不能为空");
        String verificationResult = normalizeUpperCode(request.getPostalCodeVerificationResult());
        if (control.deniedVerificationResults().contains(verificationResult)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedVerificationResults().isEmpty()
                && !control.allowedVerificationResults().contains(verificationResult)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateCurrency(EvaluateSpendRuleRequest request,
                                                        CurrencyControl control) {
        CurrencyIsoCode currency = request.getCurrency();
        if (control.deniedCurrencies().contains(currency)) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedCurrencies().isEmpty() && !control.allowedCurrencies().contains(currency)) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateTimeWindow(EvaluateSpendRuleRequest request,
                                                          TimeWindowControl control) {
        AssertUtils.notNull(request.getAuthorizationTime(), "时间窗口规则评估授权时间不能为空");
        LocalTime authorizationTime = request.getAuthorizationTime().toLocalTime();
        boolean matched = control.allowedWindows().stream()
                .anyMatch(window -> window.contains(authorizationTime));
        return matched ? SpendControlDecisionResult.PASSED : SpendControlDecisionResult.REJECTED;
    }

    private SpendControlDecisionResult evaluatePeriodCountLimit(EvaluateSpendRuleRequest request,
                                                                CountLimit countLimit) {
        AssertUtils.hasText(request.getControlScopeId(), "周期次数规则评估控制范围标识不能为空");
        AssertUtils.hasText(request.getPeriodId(), "周期次数规则评估控制周期标识不能为空");
        List<SpendControlMovementDTO> movements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setControlScopeId(request.getControlScopeId())
                        .setPeriodId(request.getPeriodId())
                        .setCurrency(request.getCurrency())
                        .setSpendRuleId(request.getRuleId())
                        .setSpendRuleVersion(request.getRuleVersion())
                        .setTargetAccountId(request.getTargetAccountId()));
        long usedCount = periodUsageCount(movements);
        if (usedCount >= countLimit.maxCount()) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private SpendControlDecisionResult evaluateRollingCountLimit(EvaluateSpendRuleRequest request,
                                                                 CountLimit countLimit,
                                                                 RollingWindow rollingWindow) {
        AssertUtils.hasText(request.getControlScopeId(), "滚动窗口次数规则评估控制范围标识不能为空");
        AssertUtils.notNull(request.getAuthorizationTime(), "滚动窗口次数规则评估授权时间不能为空");
        LocalDateTime windowStart = rollingWindowStart(request, rollingWindow);
        List<SpendControlMovementDTO> movements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setControlScopeId(request.getControlScopeId())
                        .setCurrency(request.getCurrency())
                        .setSpendRuleId(request.getRuleId())
                        .setSpendRuleVersion(request.getRuleVersion())
                        .setTargetAccountId(request.getTargetAccountId())
                        .setGmtCreateMin(windowStart)
                        .setGmtCreateMax(request.getAuthorizationTime()));
        long usedCount = rollingWindowUsageCount(movements, request.getAuthorizationTime(), rollingWindow);
        if (usedCount >= countLimit.maxCount()) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private long periodUsageCount(List<SpendControlMovementDTO> movements) {
        return movements.stream()
                .filter(this::isCountedPeriodMovement)
                .map(this::periodUsageIdentity)
                .distinct()
                .count();
    }

    private long rollingWindowUsageCount(List<SpendControlMovementDTO> movements,
                                         LocalDateTime authorizationTime,
                                         RollingWindow rollingWindow) {
        LocalDateTime windowStart = rollingWindowStart(authorizationTime, rollingWindow);
        return movements.stream()
                .filter(this::isCountedPeriodMovement)
                .filter(movement -> isWithinRollingWindow(movement, windowStart, authorizationTime))
                .map(this::periodUsageIdentity)
                .distinct()
                .count();
    }

    private LocalDateTime rollingWindowStart(EvaluateSpendRuleRequest request, RollingWindow rollingWindow) {
        return rollingWindowStart(request.getAuthorizationTime(), rollingWindow);
    }

    private LocalDateTime rollingWindowStart(LocalDateTime authorizationTime, RollingWindow rollingWindow) {
        return authorizationTime.minusMinutes(rollingWindow.windowSizeMinutes());
    }

    private boolean isWithinRollingWindow(SpendControlMovementDTO movement,
                                          LocalDateTime windowStart,
                                          LocalDateTime authorizationTime) {
        LocalDateTime movementTime = movement.getGmtCreate();
        return movementTime != null && !movementTime.isBefore(windowStart) && !movementTime.isAfter(authorizationTime);
    }

    private boolean isCountedPeriodMovement(SpendControlMovementDTO movement) {
        return movement.getMovementType() == SpendControlMovementType.RESERVED
                || movement.getMovementType() == SpendControlMovementType.CONSUMED;
    }

    private String periodUsageIdentity(SpendControlMovementDTO movement) {
        if (StringUtils.hasText(movement.getOriginalMovementSn())) {
            return movement.getOriginalMovementSn();
        }
        if (StringUtils.hasText(movement.getMovementSn())) {
            return movement.getMovementSn();
        }
        if (StringUtils.hasText(movement.getTransactionSn())) {
            return movement.getTransactionSn();
        }
        return movement.getBusinessScene() + ":" + movement.getBusinessSn();
    }

    private SpendControlDecisionResult evaluatePeriodAmountLimit(EvaluateSpendRuleRequest request) {
        AssertUtils.hasText(request.getControlScopeId(), "周期额度规则评估控制范围标识不能为空");
        AssertUtils.hasText(request.getPeriodId(), "周期额度规则评估控制周期标识不能为空");
        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                new BudgetControlProjectionQuery()
                        .setTenantId(request.getTenantId())
                        .setControlScopeId(request.getControlScopeId())
                        .setPeriodId(request.getPeriodId())
                        .setCurrency(request.getCurrency())
                        .setSpendRuleId(request.getRuleId())
                        .setSpendRuleVersion(request.getRuleVersion())
                        .setTargetAccountId(request.getTargetAccountId()));
        AssertUtils.notNull(projection.getAvailableControlAmount(), "周期可用额度不能为空");
        if (projection.getAvailableControlAmount() < request.getAmount()) {
            return SpendControlDecisionResult.REJECTED;
        }
        return SpendControlDecisionResult.PASSED;
    }

    private String rejectReason(SpendControlDecisionResult result, JSONObject ruleSpec) {
        if (result == SpendControlDecisionResult.PASSED) {
            return null;
        }
        if (hasCardDataInputCapabilityControl(ruleSpec)) {
            return CARD_DATA_INPUT_CAPABILITY_REJECT_REASON;
        }
        if (hasCardTransactionProcessingTypeControl(ruleSpec)) {
            return CARD_TRANSACTION_PROCESSING_TYPE_REJECT_REASON;
        }
        if (hasCvvControl(ruleSpec)) {
            return CVV_REQUIRED_REJECT_REASON;
        }
        if (hasPanEntryModeControl(ruleSpec)) {
            return PAN_ENTRY_MODE_REJECT_REASON;
        }
        if (hasPointOfServiceCategoryControl(ruleSpec)) {
            return POINT_OF_SERVICE_CATEGORY_REJECT_REASON;
        }
        if (hasPostalCodeVerificationControl(ruleSpec)) {
            return POSTAL_CODE_VERIFICATION_REJECT_REASON;
        }
        if (hasCurrencyControl(ruleSpec)) {
            return CURRENCY_REJECT_REASON;
        }
        if (hasTimeWindowControl(ruleSpec)) {
            return TIME_WINDOW_REJECT_REASON;
        }
        if (hasMerchantIdControl(ruleSpec)) {
            return MERCHANT_ID_REJECT_REASON;
        }
        if (hasMerchantCountryControl(ruleSpec)) {
            return MERCHANT_COUNTRY_REJECT_REASON;
        }
        if (hasMerchantCategoryControl(ruleSpec)) {
            return MERCHANT_CATEGORY_REJECT_REASON;
        }
        if (hasCountLimit(ruleSpec)) {
            return isRollingWindowCounter(ruleSpec) ? ROLLING_COUNT_REJECT_REASON : PERIOD_COUNT_REJECT_REASON;
        }
        return hasCounterSpec(ruleSpec) ? PERIOD_AMOUNT_REJECT_REASON : AMOUNT_LIMIT_REJECT_REASON;
    }

    private SpendRuleEvaluationDecisionDTO toDecision(EvaluateSpendRuleRequest request,
                                                      SpendControlDecisionResult result,
                                                      String rejectReason) {
        return new SpendRuleEvaluationDecisionDTO()
                .setTenantId(request.getTenantId())
                .setRuleId(request.getRuleId())
                .setRuleVersion(request.getRuleVersion())
                .setAction(request.getAction())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setDecisionResult(result)
                .setRejectReason(rejectReason)
                .setDecisionDigest("sha256:" + decisionDigest(request, result, rejectReason));
    }

    private String decisionDigest(EvaluateSpendRuleRequest request,
                                  SpendControlDecisionResult result,
                                  String rejectReason) {
        Map<String, Object> digestValues = new TreeMap<>();
        digestValues.put("tenantId", request.getTenantId());
        digestValues.put("ruleId", request.getRuleId());
        digestValues.put("ruleVersion", request.getRuleVersion());
        digestValues.put("action", request.getAction());
        digestValues.put("amount", request.getAmount());
        digestValues.put("currency", request.getCurrency());
        digestValues.put("businessScene", request.getBusinessScene());
        digestValues.put("businessSn", request.getBusinessSn());
        digestValues.put("merchantCategoryCode",
                request.getMerchantCategoryCode() == null ? "" : request.getMerchantCategoryCode().trim());
        digestValues.put("merchantId", request.getMerchantId() == null ? "" : request.getMerchantId().trim());
        digestValues.put("merchantCountryCode",
                request.getMerchantCountryCode() == null ? "" : normalizeUpperCode(request.getMerchantCountryCode()));
        digestValues.put("cardDataInputCapability", request.getCardDataInputCapability() == null
                ? ""
                : normalizeUpperCode(request.getCardDataInputCapability()));
        digestValues.put("cardTransactionProcessingType", request.getCardTransactionProcessingType() == null
                ? ""
                : normalizeUpperCode(request.getCardTransactionProcessingType()));
        digestValues.put("cvvProvided", request.getCvvProvided() == null ? "" : request.getCvvProvided());
        digestValues.put("panEntryMode", request.getPanEntryMode() == null
                ? ""
                : normalizeUpperCode(request.getPanEntryMode()));
        digestValues.put("pointOfServiceCategory", request.getPointOfServiceCategory() == null
                ? ""
                : normalizeUpperCode(request.getPointOfServiceCategory()));
        digestValues.put("postalCodeVerificationResult", request.getPostalCodeVerificationResult() == null
                ? ""
                : normalizeUpperCode(request.getPostalCodeVerificationResult()));
        digestValues.put("authorizationTime", request.getAuthorizationTime() == null
                ? ""
                : request.getAuthorizationTime().toString());
        digestValues.put("controlScopeId", request.getControlScopeId() == null ? "" : request.getControlScopeId());
        digestValues.put("periodId", request.getPeriodId() == null ? "" : request.getPeriodId());
        digestValues.put("targetAccountId", targetAccountDigest(request));
        digestValues.put("decisionResult", result);
        digestValues.put("rejectReason", rejectReason == null ? "" : rejectReason);
        return sha256(JSON.toJSONString(digestValues));
    }

    private String targetAccountDigest(EvaluateSpendRuleRequest request) {
        if (request.getTargetAccountId() == null) {
            return "";
        }
        return request.getTargetAccountId().type() + ":" + request.getTargetAccountId().id();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
        }
    }

    private static String normalizeUpperCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private record AmountLimit(Long amount, CurrencyIsoCode currency) {
    }

    private record CountLimit(Integer maxCount) {
    }

    private record RollingWindow(Integer windowSizeMinutes) {
    }

    private record MerchantCategoryControl(Set<String> deniedMccCodes, Set<String> allowedMccCodes) {
    }

    private record MerchantIdControl(Set<String> deniedMerchantIds, Set<String> allowedMerchantIds) {
    }

    private record MerchantCountryControl(Set<String> deniedCountryCodes, Set<String> allowedCountryCodes) {
    }

    private record CardDataInputCapabilityControl(Set<String> deniedCardDataInputCapabilities,
                                                  Set<String> allowedCardDataInputCapabilities) {
    }

    private record CardTransactionProcessingTypeControl(Set<String> deniedCardTransactionProcessingTypes,
                                                        Set<String> allowedCardTransactionProcessingTypes) {
    }

    private record CvvRequired(Boolean required) {
    }

    private record PanEntryModeControl(Set<String> deniedPanEntryModes, Set<String> allowedPanEntryModes) {
    }

    private record PointOfServiceCategoryControl(Set<String> deniedPointOfServiceCategories,
                                                 Set<String> allowedPointOfServiceCategories) {
    }

    private record PostalCodeVerificationControl(Set<String> deniedVerificationResults,
                                                 Set<String> allowedVerificationResults) {
    }

    private record CurrencyControl(Set<CurrencyIsoCode> deniedCurrencies, Set<CurrencyIsoCode> allowedCurrencies) {
    }

    private record TimeWindowControl(List<TimeWindow> allowedWindows) {
    }

    private record TimeWindow(LocalTime startTime, LocalTime endTime) {

        private boolean contains(LocalTime time) {
            if (startTime.isBefore(endTime)) {
                return !time.isBefore(startTime) && time.isBefore(endTime);
            }
            return !time.isBefore(startTime) || time.isBefore(endTime);
        }
    }
}

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
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
public class SpendRuleEvaluationApplicationServiceImpl implements SpendRuleEvaluationApplicationService {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    private static final String AMOUNT_LIMIT_REJECT_REASON = "超过单笔限额";

    private static final String PERIOD_AMOUNT_REJECT_REASON = "周期可用额度不足";

    private static final String PERIOD_COUNT_REJECT_REASON = "周期次数超限";

    private static final String MERCHANT_CATEGORY_REJECT_REASON = "商户类别不允许";

    private static final String MERCHANT_COUNTRY_REJECT_REASON = "商户国家不允许";

    private static final String CARD_DATA_INPUT_CAPABILITY_REJECT_REASON = "卡数据输入能力不允许";

    private static final String COUNTER_SPEC_KEY = "counterSpec";

    private static final String LIMIT_SPEC_KEY = "limitSpec";

    private static final String AMOUNT_LIMIT_KEY = "amountLimit";

    private static final String COUNT_LIMIT_KEY = "countLimit";

    private static final String MERCHANT_CATEGORY_CONTROL_KEY = "merchantCategoryControl";

    private static final String MERCHANT_COUNTRY_CONTROL_KEY = "merchantCountryControl";

    private static final String CARD_DATA_INPUT_CAPABILITY_CONTROL_KEY = "cardDataInputCapabilityControl";

    private static final String AMOUNT_KEY = "amount";

    private static final String CURRENCY_KEY = "currency";

    private static final String MAX_COUNT_KEY = "maxCount";

    private static final String DENIED_MCC_CODES_KEY = "deniedMccCodes";

    private static final String ALLOWED_MCC_CODES_KEY = "allowedMccCodes";

    private static final String DENIED_COUNTRY_CODES_KEY = "deniedCountryCodes";

    private static final String ALLOWED_COUNTRY_CODES_KEY = "allowedCountryCodes";

    private static final String DENIED_CARD_DATA_INPUT_CAPABILITIES_KEY = "deniedCardDataInputCapabilities";

    private static final String ALLOWED_CARD_DATA_INPUT_CAPABILITIES_KEY = "allowedCardDataInputCapabilities";

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
        SpendControlDecisionResult result = evaluateRule(request, ruleSpec);
        String rejectReason = rejectReason(result, ruleSpec);
        return toDecision(request, result, rejectReason);
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

    private SpendControlDecisionResult evaluateRule(EvaluateSpendRuleRequest request, JSONObject ruleSpec) {
        if (hasCardDataInputCapabilityControl(ruleSpec)) {
            return evaluateCardDataInputCapability(request, cardDataInputCapabilityControlOf(ruleSpec));
        }
        if (hasMerchantCountryControl(ruleSpec)) {
            return evaluateMerchantCountry(request, merchantCountryControlOf(ruleSpec));
        }
        if (hasMerchantCategoryControl(ruleSpec)) {
            return evaluateMerchantCategory(request, merchantCategoryControlOf(ruleSpec));
        }
        if (hasCountLimit(ruleSpec)) {
            return evaluatePeriodCountLimit(request, countLimitOf(ruleSpec));
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

    private boolean hasCountLimit(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(COUNT_LIMIT_KEY) != null;
    }

    private boolean hasMerchantCategoryControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(MERCHANT_CATEGORY_CONTROL_KEY) != null;
    }

    private boolean hasMerchantCountryControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(MERCHANT_COUNTRY_CONTROL_KEY) != null;
    }

    private boolean hasCardDataInputCapabilityControl(JSONObject ruleSpec) {
        JSONObject limitSpec = ruleSpec.getJSONObject(LIMIT_SPEC_KEY);
        return limitSpec != null && limitSpec.getJSONObject(CARD_DATA_INPUT_CAPABILITY_CONTROL_KEY) != null;
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
        if (control.deniedMccCodes().contains(request.getMerchantCategoryCode())) {
            return SpendControlDecisionResult.REJECTED;
        }
        if (!control.allowedMccCodes().isEmpty()
                && !control.allowedMccCodes().contains(request.getMerchantCategoryCode())) {
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

    private SpendControlDecisionResult evaluatePeriodCountLimit(EvaluateSpendRuleRequest request,
                                                                CountLimit countLimit) {
        AssertUtils.hasText(request.getControlScopeId(), "周期次数规则评估控制范围标识不能为空");
        AssertUtils.hasText(request.getPeriodId(), "周期次数规则评估控制周期标识不能为空");
        List<SpendControlMovementDTO> movements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setControlScopeId(request.getControlScopeId())
                        .setBudgetGroupSn(request.getControlScopeId())
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

    private long periodUsageCount(List<SpendControlMovementDTO> movements) {
        return movements.stream()
                .filter(this::isCountedPeriodMovement)
                .map(this::periodUsageIdentity)
                .distinct()
                .count();
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
        if (hasMerchantCountryControl(ruleSpec)) {
            return MERCHANT_COUNTRY_REJECT_REASON;
        }
        if (hasMerchantCategoryControl(ruleSpec)) {
            return MERCHANT_CATEGORY_REJECT_REASON;
        }
        if (hasCountLimit(ruleSpec)) {
            return PERIOD_COUNT_REJECT_REASON;
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
                request.getMerchantCategoryCode() == null ? "" : request.getMerchantCategoryCode());
        digestValues.put("merchantCountryCode",
                request.getMerchantCountryCode() == null ? "" : normalizeUpperCode(request.getMerchantCountryCode()));
        digestValues.put("cardDataInputCapability", request.getCardDataInputCapability() == null
                ? ""
                : normalizeUpperCode(request.getCardDataInputCapability()));
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

    private record MerchantCategoryControl(Set<String> deniedMccCodes, Set<String> allowedMccCodes) {
    }

    private record MerchantCountryControl(Set<String> deniedCountryCodes, Set<String> allowedCountryCodes) {
    }

    private record CardDataInputCapabilityControl(Set<String> deniedCardDataInputCapabilities,
                                                  Set<String> allowedCardDataInputCapabilities) {
    }
}

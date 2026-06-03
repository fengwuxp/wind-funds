package com.wind.funds.wallet.services.impl;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.model.dto.LedgerProfileDTO;
import com.wind.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.wind.funds.wallet.model.dto.NegativeAvailablePolicyDTO;
import com.wind.funds.wallet.service.LedgerProfileService;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.spec.ledger.SettlementPolicySpec;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 静态 LedgerProfile 服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
public class DefaultLedgerProfileServiceImpl implements LedgerProfileService {

    private static final Integer PROFILE_VERSION = 1;

    private static final Map<LedgerProfileCode, LedgerProfileDTO> PROFILES = initProfiles();

    @Override
    public @NonNull LedgerProfileDTO getProfile(@NonNull LedgerProfileCode profileCode) {
        LedgerProfileDTO result = PROFILES.get(profileCode);
        AssertUtils.notNull(result, "LedgerProfile 不存在，profileCode = {}", profileCode);
        return result;
    }

    @Override
    public @NonNull LedgerProfileItemDTO getRequiredItem(@NonNull LedgerProfileCode profileCode,
                                                         @NonNull LedgerSubjectCode subjectCode) {
        Map<LedgerSubjectCode, LedgerProfileItemDTO> items = getProfile(profileCode).getItems().stream()
                .collect(Collectors.toMap(LedgerProfileItemDTO::getLedgerSubjectCode, Function.identity()));
        LedgerProfileItemDTO result = items.get(subjectCode);
        AssertUtils.notNull(result, "LedgerProfileItem 不存在，profileCode = {}, subjectCode = {}",
                profileCode, subjectCode);
        return result;
    }

    private static Map<LedgerProfileCode, LedgerProfileDTO> initProfiles() {
        return Map.of(
                LedgerProfileCode.FUNDING_BASIC,
                profile(LedgerProfileCode.FUNDING_BASIC, FundsSubjectType.FUNDING_ACCOUNT, List.of(
                        item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, true),
                        item(LedgerSubjectCode.FROZEN, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false)
                ), fundingNegativePolicy()),
                LedgerProfileCode.FUNDING_MERCHANT,
                profile(LedgerProfileCode.FUNDING_MERCHANT, FundsSubjectType.FUNDING_ACCOUNT, List.of(
                        item(LedgerSubjectCode.CLEARING, LedgerSubjectCategory.CLEARING, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, true),
                        item(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.FROZEN, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.ADJUSTMENT, LedgerSubjectCategory.SUSPENSE, EntrySide.CREDIT, false)
                ), fundingNegativePolicy()),
                LedgerProfileCode.CREDIT_BASIC,
                profile(LedgerProfileCode.CREDIT_BASIC, FundsSubjectType.CREDIT_ACCOUNT, List.of(
                        item(LedgerSubjectCode.LIMIT, LedgerSubjectCategory.CONTROL, EntrySide.DEBIT, false),
                        item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, true),
                        item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, false)
                ), creditNegativePolicy()),
                LedgerProfileCode.BUDGET_BASIC,
                profile(LedgerProfileCode.BUDGET_BASIC, FundsSubjectType.BUDGET_GROUP, List.of(
                        item(LedgerSubjectCode.LIMIT, LedgerSubjectCategory.CONTROL, EntrySide.DEBIT, false),
                        item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, true),
                        item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, false)
                ), budgetNegativePolicy()),
                LedgerProfileCode.FUNDING_PLATFORM,
                profile(LedgerProfileCode.FUNDING_PLATFORM, FundsSubjectType.FUNDING_ACCOUNT, List.of(
                        item(LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.DEBIT, false),
                        item(LedgerSubjectCode.PREPAYMENT, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.CLEARING, LedgerSubjectCategory.CLEARING, EntrySide.DEBIT, true)
                                .setNegativeAvailablePolicy(platformClearingNegativePolicy()),
                        item(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.FEE, LedgerSubjectCategory.REVENUE, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.ADJUSTMENT, LedgerSubjectCategory.SUSPENSE, EntrySide.DEBIT, true)
                                .setNegativeAvailablePolicy(platformAdjustmentNegativePolicy())
                ))
        );
    }

    private static LedgerProfileDTO profile(LedgerProfileCode code,
                                            FundsSubjectType subjectType,
                                            List<LedgerProfileItemDTO> items) {
        return new LedgerProfileDTO()
                .setCode(code)
                .setVersion(PROFILE_VERSION)
                .setSubjectType(subjectType)
                .setItems(items);
    }

    private static LedgerProfileDTO profile(LedgerProfileCode code,
                                            FundsSubjectType subjectType,
                                            List<LedgerProfileItemDTO> items,
                                            NegativeAvailablePolicyDTO availableNegativePolicy) {
        return profile(code, subjectType, items.stream()
                .map(item -> attachAvailableNegativePolicy(item, availableNegativePolicy))
                .toList());
    }

    private static LedgerProfileItemDTO item(LedgerSubjectCode code,
                                             LedgerSubjectCategory category,
                                             EntrySide normalBalanceSide,
                                             boolean allowNegative) {
        return new LedgerProfileItemDTO()
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(category)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setRequired(Boolean.TRUE)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setSettlementPolicy(SettlementPolicySpec.RT.getRaw())
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setDescription(code.getDesc());
    }

    private static LedgerProfileItemDTO attachAvailableNegativePolicy(LedgerProfileItemDTO item,
                                                                      NegativeAvailablePolicyDTO policy) {
        if (item.getLedgerSubjectCode() == LedgerSubjectCode.AVAILABLE
                && Boolean.TRUE.equals(item.getAllowNegative())) {
            return item.setNegativeAvailablePolicy(policy);
        }
        return item;
    }

    private static NegativeAvailablePolicyDTO fundingNegativePolicy() {
        return policy(
                "FUNDING_AVAILABLE_CONTROLLED_NEGATIVE",
                "风控、对账、追偿、结算抵扣、后续入金抵扣、人工处理"
        );
    }

    private static NegativeAvailablePolicyDTO creditNegativePolicy() {
        return policy(
                "CREDIT_AVAILABLE_CONTROLLED_NEGATIVE",
                "新授权策略、额度治理、账龄、报表"
        );
    }

    private static NegativeAvailablePolicyDTO budgetNegativePolicy() {
        return policy(
                "BUDGET_AVAILABLE_CONTROLLED_NEGATIVE",
                "新授权策略、预算治理、周期报表"
        );
    }

    private static NegativeAvailablePolicyDTO platformClearingNegativePolicy() {
        return policy(
                "PLATFORM_CLEARING_CONTROLLED_NEGATIVE",
                "对账、差错核销、人工处理"
        );
    }

    private static NegativeAvailablePolicyDTO platformAdjustmentNegativePolicy() {
        return policy(
                "PLATFORM_ADJUSTMENT_CONTROLLED_NEGATIVE",
                "调账审批、差错核销、人工处理"
        );
    }

    private static NegativeAvailablePolicyDTO policy(String code, String governancePath) {
        return new NegativeAvailablePolicyDTO()
                .setPolicyCode(code)
                .setPolicyVersion(PROFILE_VERSION)
                .setRequireSourceFact(Boolean.TRUE)
                .setRequireReason(Boolean.TRUE)
                .setRequireApprovalOrRiskRule(Boolean.TRUE)
                .setRequireRiskStatus(Boolean.TRUE)
                .setRequireSingleLimit(Boolean.TRUE)
                .setRequireCumulativeLimit(Boolean.TRUE)
                .setRequireAgingTracking(Boolean.TRUE)
                .setRecheckFutureTransaction(Boolean.TRUE)
                .setGovernancePath(governancePath);
    }
}

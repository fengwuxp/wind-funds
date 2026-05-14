package com.capte.funds.transaction.services.impl;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.capte.funds.transaction.model.dto.LedgerProfileDTO;
import com.capte.funds.transaction.model.dto.LedgerProfileItemDTO;
import com.capte.funds.transaction.services.LedgerProfileService;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.SettlementPolicySpec;
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
                )),
                LedgerProfileCode.CREDIT_BASIC,
                profile(LedgerProfileCode.CREDIT_BASIC, FundsSubjectType.CREDIT_ACCOUNT, List.of(
                        item(LedgerSubjectCode.LIMIT, LedgerSubjectCategory.CONTROL, EntrySide.DEBIT, false),
                        item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, true),
                        item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, false)
                )),
                LedgerProfileCode.BUDGET_BASIC,
                profile(LedgerProfileCode.BUDGET_BASIC, FundsSubjectType.BUDGET_GROUP, List.of(
                        item(LedgerSubjectCode.LIMIT, LedgerSubjectCategory.CONTROL, EntrySide.DEBIT, false),
                        item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, true),
                        item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, false)
                )),
                LedgerProfileCode.FUNDING_PLATFORM,
                profile(LedgerProfileCode.FUNDING_PLATFORM, FundsSubjectType.FUNDING_ACCOUNT, List.of(
                        item(LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.DEBIT, false),
                        item(LedgerSubjectCode.PREPAYMENT, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.CLEARING, LedgerSubjectCategory.CLEARING, EntrySide.DEBIT, true),
                        item(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.FEE, LedgerSubjectCategory.REVENUE, EntrySide.CREDIT, false),
                        item(LedgerSubjectCode.ADJUSTMENT, LedgerSubjectCategory.SUSPENSE, EntrySide.DEBIT, true)
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
}

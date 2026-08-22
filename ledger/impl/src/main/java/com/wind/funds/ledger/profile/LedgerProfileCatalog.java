package com.wind.funds.ledger.profile;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.spec.SettlementPolicySpec;
import com.wind.funds.route.enums.FundsSubjectType;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 账本内部静态配置目录。
 *
 * @author wuxp
 * @since 2026-08-20
 */
@Component
public class LedgerProfileCatalog {

    private static final Integer PROFILE_VERSION = 1;

    private static final Map<LedgerProfileCode, Profile> PROFILES = Map.of(
            LedgerProfileCode.FUNDING_BASIC,
            profile(LedgerProfileCode.FUNDING_BASIC, FundsSubjectType.FUNDING_ACCOUNT, List.of(
                    item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, true),
                    item(LedgerSubjectCode.FROZEN, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                    item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false)
            )),
            LedgerProfileCode.FUNDING_MERCHANT,
            profile(LedgerProfileCode.FUNDING_MERCHANT, FundsSubjectType.FUNDING_ACCOUNT, List.of(
                    item(LedgerSubjectCode.CLEARING, LedgerSubjectCategory.CLEARING, EntrySide.CREDIT, false),
                    item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, true),
                    item(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                    item(LedgerSubjectCode.FROZEN, LedgerSubjectCategory.LIABILITY, EntrySide.CREDIT, false),
                    item(LedgerSubjectCode.ADJUSTMENT, LedgerSubjectCategory.SUSPENSE, EntrySide.CREDIT, false)
            )),
            LedgerProfileCode.CREDIT_BASIC,
            profile(LedgerProfileCode.CREDIT_BASIC, FundsSubjectType.CREDIT_ACCOUNT, List.of(
                    item(LedgerSubjectCode.LIMIT, LedgerSubjectCategory.CONTROL, EntrySide.DEBIT, false),
                    item(LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, true),
                    item(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, false),
                    item(LedgerSubjectCode.OUTSTANDING, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT, false)
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

    /**
     * 将内部配置解析为现有建账请求。
     *
     * @param request 受控初始化请求
     * @return 必需账本请求
     */
    public @NonNull List<CreateLedgerRequest> requiredLedgerRequests(
            @NonNull InitializeSubjectLedgerRequest request) {
        Profile profile = requireProfile(request.getLedgerProfileCode());
        AssertUtils.isTrue(profile.version().equals(request.getLedgerProfileVersion()),
                "LedgerProfile 版本不匹配，profileCode = {}, expectedVersion = {}, actualVersion = {}",
                profile.code(), profile.version(), request.getLedgerProfileVersion());
        AssertUtils.isTrue(profile.subjectType() == request.getSubjectType(),
                "LedgerProfile 主体类型不匹配，profileCode = {}, profileSubjectType = {}, subjectType = {}",
                profile.code(), profile.subjectType(), request.getSubjectType());
        return profile.items().stream()
                .map(item -> toCreateRequest(request, profile, item))
                .toList();
    }

    /**
     * 校验配置包含指定必需账本科目。
     *
     * @param profileCode 配置编码
     * @param subjectCodes 必需科目
     */
    public void requireItems(@NonNull LedgerProfileCode profileCode, @NonNull LedgerSubjectCode... subjectCodes) {
        Profile profile = requireProfile(profileCode);
        for (LedgerSubjectCode subjectCode : subjectCodes) {
            AssertUtils.isTrue(profile.items().stream().anyMatch(item -> item.subjectCode() == subjectCode),
                    "LedgerProfileItem 不存在，profileCode = {}, subjectCode = {}", profileCode, subjectCode);
        }
    }

    /**
     * 校验耐久账本与目录派生的预期账本一致。
     *
     * @param expected 目录派生的建账请求
     * @param actual 耐久账本
     */
    public void assertLedgerMatches(@NonNull CreateLedgerRequest expected, @NonNull LedgerDTO actual) {
        AssertUtils.isTrue(Objects.equals(actual.getTenantId(), expected.getTenantId())
                        && Objects.equals(actual.getSubjectId(), expected.getSubjectId())
                        && Objects.equals(actual.getSubjectType(), expected.getSubjectType())
                        && Objects.equals(actual.getCurrency(), expected.getCurrency())
                        && Objects.equals(actual.getLedgerProfileCode(), expected.getLedgerProfileCode())
                        && Objects.equals(actual.getLedgerProfileVersion(), expected.getLedgerProfileVersion())
                        && actual.getLedgerSubjectCode() == expected.getLedgerSubjectCode()
                        && actual.getLedgerSubjectCategory() == expected.getLedgerSubjectCategory()
                        && actual.getNormalBalanceSide() == expected.getNormalBalanceSide()
                        && Objects.equals(actual.getAllowNegative(), expected.getAllowNegative())
                        && actual.getPeriodType() == expected.getPeriodType()
                        && Objects.equals(actual.getPeriodId(), expected.getPeriodId())
                        && Objects.equals(actual.getSettlementPolicy(), expected.getSettlementPolicy())
                        && Objects.equals(actual.getCutOffTime(), expected.getCutOffTime()),
                "账本与 LedgerProfile 不一致，ledgerId = {}, profileCode = {}, subjectCode = {}",
                actual.getId(), actual.getLedgerProfileCode(), actual.getLedgerSubjectCode());
    }

    private Profile requireProfile(LedgerProfileCode profileCode) {
        Profile result = PROFILES.get(profileCode);
        AssertUtils.notNull(result, "LedgerProfile 不存在，profileCode = {}", profileCode);
        return result;
    }

    private CreateLedgerRequest toCreateRequest(InitializeSubjectLedgerRequest request,
                                                Profile profile,
                                                Item item) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? item.periodType() : request.getPeriodType();
        String periodId = resolvePeriodId(request, periodType);
        return new CreateLedgerRequest()
                .setTenantId(request.getTenantId())
                .setSubjectId(request.getSubjectId())
                .setSubjectType(request.getSubjectType().name())
                .setCurrency(request.getCurrency())
                .setLedgerProfileCode(profile.code().name())
                .setLedgerProfileVersion(profile.version())
                .setLedgerSubjectCode(item.subjectCode())
                .setLedgerSubjectCategory(item.category())
                .setNormalBalanceSide(item.normalBalanceSide())
                .setAllowNegative(item.allowNegative())
                .setPeriodType(periodType)
                .setPeriodId(periodId)
                .setSettlementPolicy(item.settlementPolicy())
                .setCutOffTime(item.cutOffTime());
    }

    private String resolvePeriodId(InitializeSubjectLedgerRequest request,
                                   AccountBalancePeriodType periodType) {
        if (periodType == AccountBalancePeriodType.LIFETIME) {
            return AccountBalancePeriodType.LIFETIME.name();
        }
        AssertUtils.hasText(request.getPeriodId(), "非生命周期账本周期 periodId 不能为空");
        return request.getPeriodId();
    }

    private static Profile profile(LedgerProfileCode code,
                                   FundsSubjectType subjectType,
                                   List<Item> items) {
        return new Profile(code, PROFILE_VERSION, subjectType, items);
    }

    private static Item item(LedgerSubjectCode code,
                             LedgerSubjectCategory category,
                             EntrySide normalBalanceSide,
                             boolean allowNegative) {
        return new Item(code, category, normalBalanceSide, allowNegative,
                AccountBalancePeriodType.LIFETIME, SettlementPolicySpec.RT.getRaw(), LocalTime.MIDNIGHT);
    }

    private record Profile(LedgerProfileCode code,
                           Integer version,
                           FundsSubjectType subjectType,
                           List<Item> items) {
    }

    private record Item(LedgerSubjectCode subjectCode,
                        LedgerSubjectCategory category,
                        EntrySide normalBalanceSide,
                        boolean allowNegative,
                        AccountBalancePeriodType periodType,
                        String settlementPolicy,
                        LocalTime cutOffTime) {
    }
}

package com.wind.funds.wallet.services.impl;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.wallet.model.dto.LedgerProfileDTO;
import com.wind.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.wind.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.funds.wallet.service.LedgerProfileService;
import com.wind.funds.wallet.service.SubjectLedgerInitializer;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认账务主体账本初始化器。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class DefaultSubjectLedgerInitializer implements SubjectLedgerInitializer {

    private final LedgerProfileService ledgerProfileService;

    private final LedgerService ledgerService;

    @Override
    public @NonNull Map<LedgerSubjectCode, Long> initializeRequiredLedgers(
            @NonNull InitializeSubjectLedgerRequest request) {
        AssertUtils.isFalse(request.getSubjectType() == FundsSubjectType.BUDGET_GROUP,
                "预算组不是核心资金账务主体，不允许初始化账本，subjectId = {}", request.getSubjectId());
        LedgerProfileDTO profile = ledgerProfileService.getProfile(request.getLedgerProfileCode());
        AssertUtils.isTrue(profile.getSubjectType() == request.getSubjectType(),
                "LedgerProfile 主体类型不匹配，profileCode = {}, profileSubjectType = {}, subjectType = {}",
                profile.getCode(),
                profile.getSubjectType(),
                request.getSubjectType());
        Map<LedgerSubjectCode, Long> result = new EnumMap<>(LedgerSubjectCode.class);
        for (LedgerProfileItemDTO item : profile.getItems()) {
            if (!Boolean.TRUE.equals(item.getRequired())) {
                continue;
            }
            AccountBalancePeriodType periodType = request.getPeriodType() == null
                    ? item.getPeriodType() : request.getPeriodType();
            String periodId = resolvePeriodId(request, periodType);
            LedgerDTO existingLedger = findExistingLedger(request, item, periodType, periodId);
            Long ledgerId = existingLedger == null
                    ? createLedger(request, profile, item, periodType, periodId)
                    : reuseExistingLedger(request, profile, item, existingLedger);
            result.put(item.getLedgerSubjectCode(), ledgerId);
        }
        return result;
    }

    private LedgerDTO findExistingLedger(InitializeSubjectLedgerRequest request,
                                         LedgerProfileItemDTO item,
                                         AccountBalancePeriodType periodType,
                                         String periodId) {
        List<LedgerDTO> records = ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(request.getTenantId())
                        .setSubjectId(request.getSubjectId())
                        .setSubjectType(request.getSubjectType().name())
                        .setCurrency(request.getCurrency())
                        .setLedgerSubjectCode(item.getLedgerSubjectCode())
                        .setPeriodType(periodType)
                        .setPeriodId(periodId),
                DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isTrue(records.size() <= 1,
                "账本唯一桶配置不唯一，subjectId = {}, ledgerSubjectCode = {}, currency = {}, periodType = {}, periodId = {}",
                request.getSubjectId(),
                item.getLedgerSubjectCode(),
                request.getCurrency(),
                periodType,
                periodId);
        return records.isEmpty() ? null : records.getFirst();
    }

    private Long createLedger(InitializeSubjectLedgerRequest request,
                              LedgerProfileDTO profile,
                              LedgerProfileItemDTO item,
                              AccountBalancePeriodType periodType,
                              String periodId) {
        return ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(request.getTenantId())
                .setSubjectId(request.getSubjectId())
                .setSubjectType(request.getSubjectType().name())
                .setCurrency(request.getCurrency())
                .setLedgerProfileCode(profile.getCode().name())
                .setLedgerProfileVersion(profile.getVersion())
                .setLedgerSubjectCode(item.getLedgerSubjectCode())
                .setLedgerSubjectCategory(item.getLedgerSubjectCategory())
                .setNormalBalanceSide(item.getNormalBalanceSide())
                .setAllowNegative(item.getAllowNegative())
                .setPeriodType(periodType)
                .setPeriodId(periodId)
                .setSettlementPolicy(item.getSettlementPolicy())
                .setCutOffTime(item.getCutOffTime()));
    }

    private String resolvePeriodId(InitializeSubjectLedgerRequest request, AccountBalancePeriodType periodType) {
        if (periodType == AccountBalancePeriodType.LIFETIME) {
            return AccountBalancePeriodType.LIFETIME.name();
        }
        AssertUtils.hasText(request.getPeriodId(), "非生命周期账本周期 periodId 不能为空");
        return request.getPeriodId();
    }

    private Long reuseExistingLedger(InitializeSubjectLedgerRequest request,
                                     LedgerProfileDTO profile,
                                     LedgerProfileItemDTO item,
                                     LedgerDTO existingLedger) {
        AssertUtils.isTrue(Objects.equals(existingLedger.getLedgerProfileCode(), profile.getCode().name())
                        && Objects.equals(existingLedger.getLedgerProfileVersion(), profile.getVersion())
                        && existingLedger.getLedgerSubjectCategory() == item.getLedgerSubjectCategory()
                        && existingLedger.getNormalBalanceSide() == item.getNormalBalanceSide()
                        && Objects.equals(existingLedger.getAllowNegative(), item.getAllowNegative())
                        && Objects.equals(existingLedger.getSettlementPolicy(), item.getSettlementPolicy())
                        && Objects.equals(existingLedger.getCutOffTime(), item.getCutOffTime()),
                "已存在账本与初始化 profile 不一致，subjectId = {}, ledgerSubjectCode = {}, ledgerId = {}",
                request.getSubjectId(),
                item.getLedgerSubjectCode(),
                existingLedger.getId());
        return existingLedger.getId();
    }
}

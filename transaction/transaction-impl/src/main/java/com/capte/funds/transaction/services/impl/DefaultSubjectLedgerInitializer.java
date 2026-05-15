package com.capte.funds.transaction.services.impl;

import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.transaction.model.dto.LedgerProfileDTO;
import com.capte.funds.transaction.model.dto.LedgerProfileItemDTO;
import com.capte.funds.transaction.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.transaction.services.LedgerProfileService;
import com.capte.funds.transaction.services.SubjectLedgerInitializer;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

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
            Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
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
                    .setPeriodId(periodType.formatPeriodId())
                    .setSettlementPolicy(item.getSettlementPolicy())
                    .setCutOffTime(item.getCutOffTime()));
            result.put(item.getLedgerSubjectCode(), ledgerId);
        }
        return result;
    }
}

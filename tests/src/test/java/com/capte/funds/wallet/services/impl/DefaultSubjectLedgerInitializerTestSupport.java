package com.capte.funds.wallet.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.capte.funds.wallet.service.LedgerProfileService;
import com.wind.common.query.WindPagination;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.SettlementPolicySpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;

import java.lang.reflect.Proxy;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

abstract class DefaultSubjectLedgerInitializerTestSupport {

    protected static LedgerProfileItemDTO item(LedgerSubjectCode code,
                                               EntrySide normalBalanceSide,
                                               boolean allowNegative,
                                               boolean required) {
        return new LedgerProfileItemDTO()
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(LedgerSubjectCategory.CONTROL)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setRequired(required)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setSettlementPolicy(SettlementPolicySpec.RT.getRaw())
                .setCutOffTime(LocalTime.MIDNIGHT);
    }

    protected static LedgerDTO ledger(Long id,
                                      LedgerSubjectCode code,
                                      EntrySide normalBalanceSide,
                                      boolean allowNegative) {
        return new LedgerDTO()
                .setId(id)
                .setTenantId(1L)
                .setSubjectId("credit_001")
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(LedgerSubjectCategory.CONTROL)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.formatPeriodId())
                .setSettlementPolicy(SettlementPolicySpec.RT.getRaw())
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setDebitAmount(0L)
                .setCreditAmount(0L);
    }

    @SuppressWarnings("unchecked")
    protected static LedgerService ledgerService(List<CreateLedgerRequest> requests, List<LedgerDTO> ledgers) {
        AtomicLong sequence = new AtomicLong(100L);
        return (LedgerService) Proxy.newProxyInstance(
                LedgerService.class.getClassLoader(),
                new Class<?>[]{LedgerService.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "LedgerServiceProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if ("createLedger".equals(method.getName())) {
                        requests.add((CreateLedgerRequest) args[0]);
                        return sequence.incrementAndGet();
                    }
                    if ("queryLedgers".equals(method.getName())) {
                        return pagination(filterLedgers((LedgerQuery) args[0], ledgers));
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static List<LedgerDTO> filterLedgers(LedgerQuery query, List<LedgerDTO> ledgers) {
        return ledgers.stream()
                .filter(ledger -> matches(query.getTenantId(), ledger.getTenantId()))
                .filter(ledger -> matches(query.getSubjectId(), ledger.getSubjectId()))
                .filter(ledger -> matches(query.getSubjectType(), ledger.getSubjectType()))
                .filter(ledger -> matches(query.getLedgerSubjectCode(), ledger.getLedgerSubjectCode()))
                .filter(ledger -> matches(query.getCurrency(), ledger.getCurrency()))
                .filter(ledger -> matches(query.getPeriodType(), ledger.getPeriodType()))
                .filter(ledger -> matches(query.getPeriodId(), ledger.getPeriodId()))
                .toList();
    }

    private static boolean matches(Object expected, Object actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    @SuppressWarnings("unchecked")
    private static WindPagination<LedgerDTO> pagination(List<LedgerDTO> ledgers) {
        return (WindPagination<LedgerDTO>) Proxy.newProxyInstance(
                WindPagination.class.getClassLoader(),
                new Class<?>[]{WindPagination.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "WindPaginationProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if ("getRecords".equals(method.getName())) {
                        return ledgers;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    protected static LedgerProfileService ledgerProfileService(LedgerProfileDTO profile) {
        return new LedgerProfileService() {
            @Override
            public LedgerProfileDTO getProfile(LedgerProfileCode profileCode) {
                assertThat(profileCode).isEqualTo(profile.getCode());
                return profile;
            }

            @Override
            public LedgerProfileItemDTO getRequiredItem(LedgerProfileCode profileCode,
                                                        LedgerSubjectCode subjectCode) {
                throw new UnsupportedOperationException("getRequiredItem");
            }
        };
    }
}

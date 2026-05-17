package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsTransactionTestSupport;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.mybatisflex.core.BaseMapper;
import com.wind.common.util.WindObjectDigestUtils;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

abstract class LedgerTransactionServiceImplTestSupport {

    protected static LedgerPostingPlanSpec postingPlanWithContext(LedgerPostingPhaseSpec phase,
                                                                  Map<String, Object> contextVariables) {
        return postingPlanWithRouteLeg(phase, null, contextVariables);
    }

    protected static LedgerPostingPlanSpec postingPlanWithRouteLeg(LedgerPostingPhaseSpec phase,
                                                                   String routeLegId,
                                                                   Map<String, Object> contextVariables) {
        LedgerPostingPlanSpec delegate = LedgerTransactionSpecFactory.postingPlan(
                LedgerPostingIntentType.TRANSFER, "LEDGER_TXN_0001", List.of(phase));
        return new LedgerPostingPlanSpec() {

            @Override
            public String getPlanId() {
                return delegate.getPlanId();
            }

            @Override
            public String getLedgerTransactionSn() {
                return delegate.getLedgerTransactionSn();
            }

            @Override
            public String getRouteLegId() {
                return routeLegId;
            }

            @Override
            public LedgerPostingIntentType getIntent() {
                return delegate.getIntent();
            }

            @Override
            public LedgerPostingScope getPostingScope() {
                return delegate.getPostingScope();
            }

            @Override
            public LedgerBalanceEffectType getBalanceEffectType() {
                return delegate.getBalanceEffectType();
            }

            @Override
            public List<LedgerPostingPhaseSpec> getPostingPhases() {
                return delegate.getPostingPhases();
            }

            @Override
            public Map<String, Object> getContextVariables() {
                return contextVariables;
            }
        };
    }

    protected static LedgerEntrySpec entry(String subjectId, EntrySide entrySide) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                subjectId,
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                "LEDGER_TXN_0001",
                "TRANSFER_TEST",
                "BUSINESS_SN_0001",
                100L,
                CurrencyIsoCode.USD,
                LocalDateTime.of(2026, 5, 7, 10, 0)
        ).setBalanceEffectType(LedgerBalanceEffectType.CONSUME)
                .setPostingScope(LedgerPostingScope.BETWEEN_SUBJECTS)
                .setPhaseCode(LedgerPhaseCode.TRANSFER)
                .setIntent(LedgerPostingIntentType.TRANSFER)
                .setContextVariables(Map.of());
    }

    protected static String stableEntryHash(LedgerEntry entry) {
        return WindObjectDigestUtils.sha256WithNames(entry, List.of(
                LedgerEntry.Fields.tenantId,
                LedgerEntry.Fields.subjectId,
                LedgerEntry.Fields.subjectType,
                LedgerEntry.Fields.ledgerSubjectCode,
                LedgerEntry.Fields.ledgerSubjectCategory,
                LedgerEntry.Fields.entrySide,
                LedgerEntry.Fields.intent,
                LedgerEntry.Fields.postingScope,
                LedgerEntry.Fields.balanceEffectType,
                LedgerEntry.Fields.phaseCode,
                LedgerEntry.Fields.businessScene,
                LedgerEntry.Fields.businessSn,
                LedgerEntry.Fields.amount,
                LedgerEntry.Fields.currency,
                LedgerEntry.Fields.originalAmount,
                LedgerEntry.Fields.originalCurrency,
                LedgerEntry.Fields.exchangeRate,
                LedgerEntry.Fields.transactionTime
        ));
    }

    protected static String stablePostingPlanHash(LedgerPostingPlan plan) {
        return WindObjectDigestUtils.sha256WithNames(plan, List.of(
                LedgerPostingPlan.Fields.tenantId,
                LedgerPostingPlan.Fields.routeLegId,
                LedgerPostingPlan.Fields.intent,
                LedgerPostingPlan.Fields.postingScope,
                LedgerPostingPlan.Fields.balanceEffectType,
                LedgerPostingPlan.Fields.phaseCode,
                LedgerPostingPlan.Fields.amount,
                LedgerPostingPlan.Fields.currency,
                LedgerPostingPlan.Fields.debitAmount,
                LedgerPostingPlan.Fields.creditAmount
        ));
    }

    protected static String stableTransactionHash(LedgerTransaction transaction) {
        return WindObjectDigestUtils.sha256WithNames(transaction, List.of(
                LedgerTransaction.Fields.tenantId,
                LedgerTransaction.Fields.instructionType,
                LedgerTransaction.Fields.eventType,
                LedgerTransaction.Fields.transactionType,
                LedgerTransaction.Fields.businessScene,
                LedgerTransaction.Fields.businessSn,
                LedgerTransaction.Fields.amount,
                LedgerTransaction.Fields.currency,
                LedgerTransaction.Fields.originalAmount,
                LedgerTransaction.Fields.originalCurrency,
                LedgerTransaction.Fields.exchangeRate,
                LedgerTransaction.Fields.debitAmount,
                LedgerTransaction.Fields.creditAmount,
                LedgerTransaction.Fields.transactionTime,
                LedgerTransaction.Fields.referenceLedgerTransactionSn
        ));
    }

    @SuppressWarnings("unchecked")
    protected static <T extends BaseMapper<?>> T mapper(Class<T> mapperType, Consumer<Object> insertHandler) {
        return mapper(mapperType, insertHandler, () -> null);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends BaseMapper<?>> T mapper(Class<T> mapperType,
                                                        Consumer<Object> insertHandler,
                                                        Supplier<Object> selectOneHandler) {
        return (T) Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return invokeObjectMethod(proxy, method, args);
                    }
                    if ("insertSelective".equals(method.getName())) {
                        insertHandler.accept(args[0]);
                        return 1;
                    }
                    if ("selectOneByQuery".equals(method.getName())) {
                        return selectOneHandler.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "Proxy(" + proxy.getClass().getInterfaces()[0].getSimpleName() + ")";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }
}

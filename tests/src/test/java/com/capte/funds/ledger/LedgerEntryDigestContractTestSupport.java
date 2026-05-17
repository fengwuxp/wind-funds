package com.capte.funds.ledger;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.impl.LedgerTransactionServiceImpl;
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
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

abstract class LedgerEntryDigestContractTestSupport {

    protected static LedgerEntry firstPersistedEntry() {
        List<LedgerEntry> entries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> ((LedgerTransaction) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerPostingPlanMapper.class, entity -> ((LedgerPostingPlan) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    entries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(110L, CurrencyIsoCode.EUR))
                .exchangeRate(new BigDecimal("1.10"))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(LedgerTransactionSpecFactory.postingPlan(
                        LedgerPostingIntentType.TRANSFER, "LEDGER_TXN_0001", List.of(phase))))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        return entries.getFirst();
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

    private static LedgerEntrySpec entry(String subjectId, EntrySide entrySide) {
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
                .setOriginalAmount(Money.immutable(110L, CurrencyIsoCode.EUR))
                .setExchangeRate(new BigDecimal("1.10"))
                .setContextVariables(Map.of());
    }

    @SuppressWarnings("unchecked")
    private static <T extends BaseMapper<?>> T mapper(Class<T> mapperType, Consumer<Object> insertHandler) {
        return mapper(mapperType, insertHandler, () -> null);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BaseMapper<?>> T mapper(Class<T> mapperType,
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
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }
}

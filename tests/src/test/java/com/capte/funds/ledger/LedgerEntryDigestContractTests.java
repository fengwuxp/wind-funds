package com.capte.funds.ledger;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.mybatisflex.core.BaseMapper;
import com.wind.common.util.WindObjectDigestUtils;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerReconcileStatus;
import com.wind.integration.funds.ledger.enums.LedgerSettlementStatus;
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
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账本分录摘要契约测试。
 */
class LedgerEntryDigestContractTests {

    /**
     * 场景：同一笔账本分录重建后应得到稳定摘要。
     * 输入：生产 LedgerTransactionServiceImpl 生成的账本分录。
     * 输出：分录级 sha256 摘要与测试侧契约字段重算结果。
     * 预期：摘要只由稳定账务事实字段决定，重算结果与持久化摘要一致。
     * 红线：不得把 entry sn、数据库 ID、posting plan sn 或审计时间纳入幂等摘要。
     */
    @Test
    void testLedgerEntryDigestShouldUseStableAccountingFieldsOnly() {
        LedgerEntry entry = firstPersistedEntry();
        String originalHash = entry.getSha256();

        assertThat(stableEntryHash(entry)).isEqualTo(originalHash);

        entry.setSn("LGE999999");
        entry.setId(999L);
        entry.setLedgerTransactionSn("LEDGER_TXN_OTHER");
        entry.setPostingPlanSn("PLAN_OTHER");
        entry.setFundsTransactionSn("FUNDS_TXN_OTHER");
        entry.setLedgerId(999L);
        entry.setGmtCreate(LocalDateTime.of(2026, 5, 8, 11, 0));
        entry.setGmtModified(LocalDateTime.of(2026, 5, 8, 11, 30));
        entry.setDescription("changed display text");
        entry.setContextVariables("{\"traceId\":\"TRACE_999\"}");
        entry.setSettlementStatus(LedgerSettlementStatus.FAILED);
        entry.setSettlementCompletedTime(LocalDateTime.of(2026, 5, 8, 12, 0));
        entry.setReconcileStatus(LedgerReconcileStatus.MATCHED);
        entry.setReconciliationBatch("RECON_202605080001");
        entry.setReconciliationCompletedTime(LocalDateTime.of(2026, 5, 8, 12, 30));

        assertThat(stableEntryHash(entry)).isEqualTo(originalHash);
    }

    /**
     * 场景：账本分录摘要必须覆盖核心账务语义，防止重建时把不同账务事实误判为同一事实。
     * 输入：生产 LedgerTransactionServiceImpl 生成的账本分录，依次改变账目类别、intent、scope、effect 和 phase。
     * 输出：变更前后的分录级 sha256 摘要。
     * 预期：任一核心账务语义字段变化都会改变摘要。
     * 红线：不得只用金额、主体和业务流水生成摘要，导致冻结、消费、费用等不同语义互相覆盖。
     */
    @Test
    void testLedgerEntryDigestShouldIncludeAccountingSemantics() {
        LedgerEntry entry = firstPersistedEntry();
        String originalHash = entry.getSha256();

        entry.setLedgerSubjectCategory(LedgerSubjectCategory.ASSET);
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY);
        entry.setIntent(LedgerPostingIntentType.FEE.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setIntent(LedgerPostingIntentType.TRANSFER.name());
        entry.setPostingScope(LedgerPostingScope.WITHIN_SUBJECT.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setPostingScope(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        entry.setBalanceEffectType(LedgerBalanceEffectType.RESTORE.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setBalanceEffectType(LedgerBalanceEffectType.CONSUME.name());
        entry.setPhaseCode(LedgerPhaseCode.FEE.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);
    }

    /**
     * 场景：跨币种或锁汇账务分录重建时，汇率必须参与摘要。
     * 输入：原币 EUR、目标币 USD、汇率 1.10 的账本分录。
     * 输出：修改汇率前后的分录级 sha256 摘要。
     * 预期：汇率变化会改变摘要。
     * 红线：不得忽略汇率导致不同外汇事实命中同一幂等摘要。
     */
    @Test
    void testLedgerEntryDigestShouldIncludeExchangeRate() {
        LedgerEntry entry = firstPersistedEntry();
        String originalHash = entry.getSha256();

        entry.setExchangeRate(new BigDecimal("1.20"));

        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);
    }

    private static LedgerEntry firstPersistedEntry() {
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

    private static String stableEntryHash(LedgerEntry entry) {
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

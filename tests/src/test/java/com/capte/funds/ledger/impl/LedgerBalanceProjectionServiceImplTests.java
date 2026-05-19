package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.ImmutableFundsBalanceView;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账本余额投影边界测试。
 */
class LedgerBalanceProjectionServiceImplTests {

    private static final Long LEDGER_ID = 1001L;

    private static final String ACCOUNT_ID = "funding_user_balance_log";

    private static final String ACCOUNT_TYPE = "FUNDING_ACCOUNT";

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    /**
     * 场景：余额投影已经完成账本余额更新，但余额变更事件发布器临时不可用。
     * 输入：AVAILABLE 账本期初余额 100，一笔借方入账 25；事件发布器抛出运行时异常。
     * 输出：投影服务吞掉观察事件失败并保留余额更新请求。
     * 预期：余额变更事件只是业务切入/观察口，不是余额事实源。
     * 红线：余额变更日志或事件失败不得回滚、阻断或篡改 Ledger 余额投影事实。
     */
    @Test
    void testProjectShouldKeepLedgerBalanceUpdateWhenBalanceChangedEventPublishFails() {
        setApplicationEventPublisher(event -> {
            throw new IllegalStateException("balance log sink unavailable");
        });
        RecordingLedgerService ledgerService = new RecordingLedgerService(availableLedger());
        LedgerBalanceProjectionServiceImpl projectionService = new LedgerBalanceProjectionServiceImpl(
                new FixedFundsAccountQueryService(balanceView(100L)),
                ledgerService);

        projectionService.project(List.of(ledgerEntry(25L)));

        assertThat(ledgerService.updateRequests()).singleElement().satisfies(request -> {
            assertThat(request.getId()).isEqualTo(LEDGER_ID);
            assertThat(request.getDebitAmountDelta()).isEqualTo(25L);
            assertThat(request.getCreditAmountDelta()).isZero();
            assertThat(request.getMinimumNormalBalance()).isZero();
        });
    }

    @AfterEach
    void resetApplicationEventPublisher() {
        setApplicationEventPublisher(event -> {
        });
    }

    private static LedgerDTO availableLedger() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 19, 12, 0);
        return new LedgerDTO()
                .setId(LEDGER_ID)
                .setGmtCreate(now)
                .setGmtModified(now)
                .setSubjectId(ACCOUNT_ID)
                .setSubjectType(ACCOUNT_TYPE)
                .setTenantId(1L)
                .setLedgerProfileCode("FUNDING_ACCOUNT_STANDARD")
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.ASSET)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(false)
                .setDebitAmount(100L)
                .setCreditAmount(0L)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name())
                .setVersion(1);
    }

    private static FundsAccountBalanceView balanceView(long availableAmount) {
        return ImmutableFundsBalanceView.builder()
                .tenantId(1L)
                .accountId(FundsAccountId.immutable(ACCOUNT_ID, ACCOUNT_TYPE))
                .currency(CURRENCY)
                .balanceBuckets(Map.of(
                        LedgerSubjectCode.AVAILABLE,
                        balanceBucket(LedgerSubjectCode.AVAILABLE, availableAmount),
                        LedgerSubjectCode.FROZEN,
                        balanceBucket(LedgerSubjectCode.FROZEN, 0L),
                        LedgerSubjectCode.AUTHORIZATION,
                        balanceBucket(LedgerSubjectCode.AUTHORIZATION, 0L)))
                .build();
    }

    private static LedgerBalanceBucket balanceBucket(LedgerSubjectCode ledgerSubjectCode, long amount) {
        return LedgerBalanceBucket.builder()
                .accountCode(ledgerSubjectCode)
                .balance(Money.immutable(amount, CURRENCY))
                .periodType(AccountBalancePeriodType.LIFETIME)
                .periodId(AccountBalancePeriodType.LIFETIME.name())
                .activeTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }

    private static LedgerEntrySpec ledgerEntry(long amount) {
        return TestLedgerEntrySpec.builder()
                .subjectId(ACCOUNT_ID)
                .subjectType(ACCOUNT_TYPE)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .ledgerSubjectCategory(LedgerSubjectCategory.ASSET)
                .ledgerId(LEDGER_ID)
                .ledgerTransactionSn("LT-BALANCE-LOG-001")
                .entryType(EntrySide.DEBIT)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .businessScene("BALANCE_LOG_BOUNDARY")
                .businessSn("BALANCE_LOG_BOUNDARY_001")
                .amount(Money.immutable(amount, CURRENCY))
                .originalAmount(Money.immutable(amount, CURRENCY))
                .exchangeRate(BigDecimal.ONE)
                .transactionTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                .description("balance log boundary")
                .contextVariables(Map.of("ledgerEntrySn", "LE-BALANCE-LOG-001"))
                .sha256("sha256-balance-log-001")
                .build();
    }

    private static void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        try {
            Method method = SpringEventPublishUtils.class.getDeclaredMethod(
                    "setApplicationEventPublisher",
                    ApplicationEventPublisher.class);
            method.setAccessible(true);
            method.invoke(null, publisher);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置 Spring 事件发布器失败", ex);
        }
    }

    private record FixedFundsAccountQueryService(FundsAccountBalanceView balanceView) implements FundsAccountQueryService {

        @Override
        public FundsAccount getAccount(FundsAccountId accountId) {
            throw new UnsupportedOperationException("account lookup is not needed by balance projection test");
        }

        @Override
        public FundsAccountBalanceView getBalance(FundsAccountId accountId) {
            return balanceView;
        }

        @Override
        public boolean supports(FundsAccountId accountId) {
            return true;
        }
    }

    private static final class RecordingLedgerService implements LedgerService {

        private final LedgerDTO ledger;

        private final List<UpdateLedgerBalanceRequest> updateRequests = new ArrayList<>();

        private RecordingLedgerService(LedgerDTO ledger) {
            this.ledger = ledger;
        }

        @Override
        public Long createLedger(CreateLedgerRequest request) {
            throw new UnsupportedOperationException("ledger creation is not needed by balance projection test");
        }

        @Override
        public void updateLedgerBalance(UpdateLedgerBalanceRequest request) {
            updateRequests.add(request);
        }

        @Override
        public void deleteLedgerByIds(Long... ids) {
            throw new UnsupportedOperationException("ledger deletion is not needed by balance projection test");
        }

        @Override
        public LedgerDTO getLedgerById(Long id) {
            return ledger;
        }

        @Override
        public List<LedgerDTO> getLedgerByIds(Collection<Long> ids) {
            return List.of(ledger);
        }

        @Override
        public WindPagination<LedgerDTO> queryLedgers(LedgerQuery query,
                                                      WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("ledger query is not needed by balance projection test");
        }

        private List<UpdateLedgerBalanceRequest> updateRequests() {
            return updateRequests;
        }
    }

    @Getter
    private static final class TestLedgerEntrySpec implements LedgerEntrySpec {

        private final String subjectId;

        private final String subjectType;

        private final LedgerSubjectCode ledgerSubjectCode;

        private final LedgerSubjectCategory ledgerSubjectCategory;

        private final Long ledgerId;

        private final String ledgerTransactionSn;

        private final EntrySide entryType;

        private final LedgerPhaseCode phaseCode;

        private final String businessScene;

        private final String businessSn;

        private final Money amount;

        private final Money originalAmount;

        private final BigDecimal exchangeRate;

        private final LocalDateTime transactionTime;

        private final String description;

        private final Map<String, Object> contextVariables;

        private final String sha256;

        @Builder
        private TestLedgerEntrySpec(String subjectId,
                                    String subjectType,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    LedgerSubjectCategory ledgerSubjectCategory,
                                    Long ledgerId,
                                    String ledgerTransactionSn,
                                    EntrySide entryType,
                                    LedgerPhaseCode phaseCode,
                                    String businessScene,
                                    String businessSn,
                                    Money amount,
                                    Money originalAmount,
                                    BigDecimal exchangeRate,
                                    LocalDateTime transactionTime,
                                    String description,
                                    Map<String, Object> contextVariables,
                                    String sha256) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
            this.ledgerSubjectCode = ledgerSubjectCode;
            this.ledgerSubjectCategory = ledgerSubjectCategory;
            this.ledgerId = ledgerId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.entryType = entryType;
            this.phaseCode = phaseCode;
            this.businessScene = businessScene;
            this.businessSn = businessSn;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.transactionTime = transactionTime;
            this.description = description;
            this.contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
            this.sha256 = sha256;
        }
    }
}

package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.capte.funds.wallet.ImmutableFundsAccount;
import com.capte.funds.wallet.ImmutableFundsBalanceView;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract class LedgerBalanceProjectionServiceImplTestSupport {

    protected static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId) {
        return newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L);
    }

    protected static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId,
                                                                          LedgerSubjectCode ledgerSubjectCode,
                                                                          Long ledgerId) {
        return newFundsAccountQueryService(accountId, ledgerSubjectCode, ledgerId, 1_000L);
    }

    protected static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId,
                                                                          LedgerSubjectCode ledgerSubjectCode,
                                                                          Long ledgerId,
                                                                          long balance) {
        return new FundsAccountQueryService() {

            @Override
            public @NonNull FundsAccount getAccount(@NonNull FundsAccountId ignored) {
                return ImmutableFundsAccount.builder()
                        .id(1L)
                        .tenantId(1L)
                        .accountId(accountId)
                        .owner(FundsAccountOwner.of("user_001", FundsAccountOwnerType.USER))
                        .status(FundsAccountStatus.ACTIVE)
                        .currency(CurrencyIsoCode.USD)
                        .accountLedgerIds(Map.of(ledgerSubjectCode, ledgerId))
                        .version(1)
                        .build();
            }

            @Override
            public @NonNull FundsAccountBalanceView getBalance(@NonNull FundsAccountId ignored) {
                LedgerBalanceBucket bucket = LedgerBalanceBucket.builder()
                        .accountCode(ledgerSubjectCode)
                        .balance(Money.immutable(balance, CurrencyIsoCode.USD))
                        .periodType(AccountBalancePeriodType.LIFETIME)
                        .periodId(AccountBalancePeriodType.LIFETIME.name())
                        .activeTime(LocalDateTime.of(2026, 5, 7, 12, 0))
                        .build();
                return ImmutableFundsBalanceView.builder()
                        .id(1L)
                        .tenantId(1L)
                        .accountId(accountId)
                        .currency(CurrencyIsoCode.USD)
                        .balanceBuckets(Map.of(ledgerSubjectCode, bucket))
                        .build();
            }

            @Override
            public boolean supports(@NonNull FundsAccountId ignored) {
                return true;
            }
        };
    }

    protected static List<Object> captureSpringEvents() throws Exception {
        List<Object> events = new ArrayList<>();
        setSpringEventPublisher(events::add);
        return events;
    }

    protected static void rejectSpringEvents() throws Exception {
        setSpringEventPublisher(event -> {
            throw new IllegalStateException("balance event sink failed");
        });
    }

    private static void setSpringEventPublisher(ApplicationEventPublisher publisher) throws Exception {
        Method method = SpringEventPublishUtils.class.getDeclaredMethod(
                "setApplicationEventPublisher",
                ApplicationEventPublisher.class
        );
        method.setAccessible(true);
        method.invoke(null, publisher);
    }

    protected static FundsTransactionTestSupport.MutableLedgerEntrySpec entry(FundsAccountId accountId,
                                                                              Long ledgerId,
                                                                              EntrySide entrySide,
                                                                              long amount) {
        return entry(accountId, ledgerId, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL,
                entrySide, amount);
    }

    protected static FundsTransactionTestSupport.MutableLedgerEntrySpec entry(FundsAccountId accountId,
                                                                              Long ledgerId,
                                                                              LedgerSubjectCode ledgerSubjectCode,
                                                                              LedgerSubjectCategory ledgerSubjectCategory,
                                                                              EntrySide entrySide,
                                                                              long amount) {
        LocalDateTime transactionTime = LocalDateTime.of(2026, 5, 7, 12, 0);
        return FundsTransactionTestSupport.ledgerEntrySpec(
                accountId.id(),
                accountId.type(),
                ledgerSubjectCode,
                ledgerSubjectCategory,
                entrySide,
                "ledger_txn_001",
                "TEST",
                "biz_00000001",
                amount,
                CurrencyIsoCode.USD,
                transactionTime
        ).setLedgerId(ledgerId)
                .setSha256("");
    }

    protected static LedgerDTO ledger(Long id, EntrySide normalBalanceSide) {
        return ledger(id, normalBalanceSide, false);
    }

    protected static LedgerDTO ledger(Long id, EntrySide normalBalanceSide, boolean allowNegative) {
        return ledger(id, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, normalBalanceSide,
                allowNegative);
    }

    protected static LedgerDTO ledger(Long id,
                                      LedgerSubjectCode ledgerSubjectCode,
                                      LedgerSubjectCategory ledgerSubjectCategory,
                                      EntrySide normalBalanceSide) {
        return ledger(id, ledgerSubjectCode, ledgerSubjectCategory, normalBalanceSide, false);
    }

    protected static LedgerDTO ledger(Long id,
                                      LedgerSubjectCode ledgerSubjectCode,
                                      LedgerSubjectCategory ledgerSubjectCategory,
                                      EntrySide normalBalanceSide,
                                      boolean allowNegative) {
        return new LedgerDTO()
                .setId(id)
                .setGmtCreate(LocalDateTime.of(2026, 5, 7, 12, 0))
                .setGmtModified(LocalDateTime.of(2026, 5, 7, 12, 0))
                .setSubjectId("funding_001")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(ledgerSubjectCategory)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setDebitAmount(0L)
                .setCreditAmount(1_000L)
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name());
    }

    protected static class RecordingLedgerService implements LedgerService {

        private final LedgerDTO ledger;

        protected final List<UpdateLedgerBalanceRequest> updateRequests = new ArrayList<>();

        protected RecordingLedgerService(LedgerDTO ledger) {
            this.ledger = ledger;
        }

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException("createLedger");
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            updateRequests.add(request);
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException("deleteLedgerByIds");
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            return ledger;
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            throw new UnsupportedOperationException("getLedgerByIds");
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(
                @NonNull LedgerQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("queryLedgers");
        }
    }
}

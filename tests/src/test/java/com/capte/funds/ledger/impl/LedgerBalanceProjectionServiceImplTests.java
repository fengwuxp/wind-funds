package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.ImmutableFundsAccount;
import com.capte.funds.wallet.ImmutableFundsBalanceView;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerBalanceProjectionServiceImplTests {

    @Test
    void testProjectShouldUseDebitNormalBalanceSideForAssetSubjects() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(
                100L,
                LedgerSubjectCode.CASH,
                LedgerSubjectCategory.ASSET,
                EntrySide.DEBIT
        ));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.CASH, 100L),
                ledgerService
        );
        List<Object> events = captureSpringEvents();

        service.project(List.of(entry(
                accountId,
                100L,
                LedgerSubjectCode.CASH,
                LedgerSubjectCategory.ASSET,
                EntrySide.DEBIT,
                300L
        )));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getId()).isEqualTo(100L);
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getCreditAmountDelta()).isEqualTo(0L);
        assertThat(updateRequest.getMinimumNormalBalance()).isZero();
        assertThat(events).hasSize(1);
        LedgerBalanceChangedEvent event = (LedgerBalanceChangedEvent) events.getFirst();
        assertThat(event.getSubjectId()).isEqualTo(accountId.id());
        assertThat(event.getSubjectType()).isEqualTo(accountId.type());
        assertThat(event.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.CASH);
        assertThat(event.getBeforeBalance()).isEqualTo(1_000L);
        assertThat(event.getBalance()).isEqualTo(1_300L);
    }

    @Test
    void projectShouldUseLedgerNormalBalanceSideForControlSubjects() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );
        List<Object> events = captureSpringEvents();

        service.project(List.of(entry(accountId, 99L, EntrySide.CREDIT, 300L)));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getId()).isEqualTo(99L);
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(0L);
        assertThat(updateRequest.getCreditAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getMinimumNormalBalance()).isZero();
        assertThat(events).hasSize(1);
        LedgerBalanceChangedEvent event = (LedgerBalanceChangedEvent) events.getFirst();
        assertThat(event.getSubjectId()).isEqualTo(accountId.id());
        assertThat(event.getSubjectType()).isEqualTo(accountId.type());
        assertThat(event.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        assertThat(event.getBeforeBalance()).isEqualTo(1_000L);
        assertThat(event.getBalance()).isEqualTo(1_300L);
    }

    @Test
    void projectShouldAllowNegativeWhenProfileAndEntryAllowIt() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );
        captureSpringEvents();

        service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 1_200L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.ALLOW_NEGATIVE)));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(1_200L);
        assertThat(updateRequest.getMinimumNormalBalance()).isNull();
    }

    @Test
    void testProjectShouldRejectAllowNegativeWhenLedgerProfileDisallowsNegative() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, false)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 1_200L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.ALLOW_NEGATIVE))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本 profile 不允许负余额")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    @Test
    void projectShouldKeepMustNotBeNegativeConstraintWhenProfileAllowsNegative() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );
        captureSpringEvents();

        service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 300L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE)));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getMinimumNormalBalance()).isZero();
    }

    @Test
    void testProjectShouldRejectMustNotBeNegativeWhenCurrentBalanceAlreadyNegative() {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L, -100L),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 300L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额不允许为负")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining("beforeBalance = -100");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    @Test
    void testProjectShouldRejectMustNotBeNegativeWhenProjectionWouldBreakBalanceFloor() {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L, 100L),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 300L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额不足")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining("beforeBalance = 100")
                .hasMessageContaining("afterBalance = -200");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    @Test
    void testProjectShouldRejectEntriesFromDifferentFundsAccounts() {
        FundsAccountId firstAccountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        FundsAccountId secondAccountId = FundsAccountId.immutable("funding_002", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.DEBIT));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(firstAccountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(
                entry(firstAccountId, 99L, EntrySide.DEBIT, 100L),
                entry(secondAccountId, 99L, EntrySide.CREDIT, 100L)
        )))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额投影只允许处理同一资金账户的分录");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    @Test
    void testProjectShouldRejectEntryWhenLedgerBelongsToAnotherSubjectBeforeUpdate() {
        FundsAccountId entryAccountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO anotherSubjectLedger = ledger(99L, EntrySide.DEBIT)
                .setSubjectId("funding_002")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(anotherSubjectLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(entryAccountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(entryAccountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体与账本主体不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    @Test
    void testProjectShouldRejectEntryWhenLedgerSubjectCodeDoesNotMatchBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO cashLedger = ledger(99L, LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.DEBIT);
        RecordingLedgerService ledgerService = new RecordingLedgerService(cashLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录科目与账本科目不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    @Test
    void testProjectShouldRejectEntryWhenLedgerCurrencyDoesNotMatchBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO eurLedger = ledger(99L, EntrySide.DEBIT)
                .setCurrency(CurrencyIsoCode.EUR);
        RecordingLedgerService ledgerService = new RecordingLedgerService(eurLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录币种与账本币种不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    private static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId) {
        return newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L);
    }

    private static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId,
                                                                        LedgerSubjectCode ledgerSubjectCode,
                                                                        Long ledgerId) {
        return newFundsAccountQueryService(accountId, ledgerSubjectCode, ledgerId, 1_000L);
    }

    private static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId,
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

    private static List<Object> captureSpringEvents() throws Exception {
        List<Object> events = new ArrayList<>();
        Method method = SpringEventPublishUtils.class.getDeclaredMethod(
                "setApplicationEventPublisher",
                ApplicationEventPublisher.class
        );
        method.setAccessible(true);
        method.invoke(null, (ApplicationEventPublisher) events::add);
        return events;
    }

    private static FundsTransactionTestSupport.MutableLedgerEntrySpec entry(FundsAccountId accountId,
                                                                            Long ledgerId,
                                                                            EntrySide entrySide,
                                                                            long amount) {
        return entry(accountId, ledgerId, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, entrySide, amount);
    }

    private static FundsTransactionTestSupport.MutableLedgerEntrySpec entry(FundsAccountId accountId,
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

    private static LedgerDTO ledger(Long id, EntrySide normalBalanceSide) {
        return ledger(id, normalBalanceSide, false);
    }

    private static LedgerDTO ledger(Long id, EntrySide normalBalanceSide, boolean allowNegative) {
        return ledger(id, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, normalBalanceSide, allowNegative);
    }

    private static LedgerDTO ledger(Long id,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    LedgerSubjectCategory ledgerSubjectCategory,
                                    EntrySide normalBalanceSide) {
        return ledger(id, ledgerSubjectCode, ledgerSubjectCategory, normalBalanceSide, false);
    }

    private static LedgerDTO ledger(Long id,
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

    private static class RecordingLedgerService implements LedgerService {

        private final LedgerDTO ledger;

        private final List<UpdateLedgerBalanceRequest> updateRequests = new ArrayList<>();

        private RecordingLedgerService(LedgerDTO ledger) {
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

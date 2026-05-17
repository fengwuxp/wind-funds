package com.capte.funds.ledger;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.dto.LedgerEntryDTO;
import com.capte.funds.ledger.dto.LedgerTransactionCreateResult;
import com.capte.funds.ledger.dto.LedgerTransactionDTO;
import com.capte.funds.ledger.query.LedgerEntryQuery;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.query.LedgerTransactionQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.request.UpdateLedgerTransactionRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.ledger.service.LedgerTransactionService;
import com.capte.funds.support.FundsTransactionTestSupport;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.LedgerBalanceProjectionService;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class LedgerTransactionPostingTestSupport {

    protected LedgerTransactionSpec transaction() {
        String ledgerTransactionSn = "LE_000000000001";
        List<LedgerEntrySpec> entries = List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        );
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, entries);
        return transaction(ledgerTransactionSn, List.of(plan));
    }

    protected LedgerTransactionSpec transaction(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
        return transaction(ledgerTransactionSn, plans, CurrencyIsoCode.USD);
    }

    protected LedgerTransactionSpec transaction(String ledgerTransactionSn,
                                                List<LedgerPostingPlanSpec> plans,
                                                CurrencyIsoCode currency) {
        return transaction(ledgerTransactionSn, plans, currency, LedgerTransactionStatus.POSTED);
    }

    protected LedgerTransactionSpec transaction(String ledgerTransactionSn,
                                                List<LedgerPostingPlanSpec> plans,
                                                CurrencyIsoCode currency,
                                                LedgerTransactionStatus status) {
        return LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn(ledgerTransactionSn)
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .status(status)
                .amount(Money.immutable(100L, currency))
                .originalAmount(Money.immutable(100L, currency))
                .exchangeRate(BigDecimal.ONE)
                .businessSn("TRANSFER_000000000001")
                .businessScene("TRANSFER")
                .transactionTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .description("transfer")
                .postingPlans(plans)
                .contextVariables(Map.of())
                .build();
    }

    protected FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide, String ledgerTransactionSn) {
        return entry(entrySide, ledgerTransactionSn, 100L);
    }

    protected FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide,
                                                                       String ledgerTransactionSn,
                                                                       long amount) {
        return entry(entrySide, ledgerTransactionSn, amount, CurrencyIsoCode.USD);
    }

    protected FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide,
                                                                       String ledgerTransactionSn,
                                                                       long amount,
                                                                       CurrencyIsoCode currency) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                "funding_001",
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                entrySide,
                ledgerTransactionSn,
                "TRANSFER",
                "TRANSFER_000000000001",
                amount,
                currency,
                LocalDateTime.of(2026, 5, 10, 10, 0)
        ).setLedgerId(1L);
    }

    protected LedgerPostingPlanSpec postingPlan(String ledgerTransactionSn, List<LedgerEntrySpec> entries) {
        return LedgerTransactionSpecFactory.postingPlan(LedgerPostingIntentType.TRANSFER,
                ledgerTransactionSn, List.of(LedgerTransactionSpecFactory.postingPhase(
                        LedgerPhaseCode.TRANSFER, entries)));
    }

    protected RecordingLedgerService defaultLedgerService() {
        RecordingLedgerService ledgerService = new RecordingLedgerService();
        ledgerService.addLedger(ledger(1L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, CurrencyIsoCode.USD));
        return ledgerService;
    }

    protected LedgerDTO ledger(Long id,
                               String subjectId,
                               String subjectType,
                               LedgerSubjectCode ledgerSubjectCode,
                               LedgerSubjectCategory ledgerSubjectCategory,
                               CurrencyIsoCode currency) {
        return new LedgerDTO()
                .setId(id)
                .setSubjectId(subjectId)
                .setSubjectType(subjectType)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(ledgerSubjectCategory)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(false)
                .setDebitAmount(0L)
                .setCreditAmount(0L)
                .setCurrency(currency);
    }

    protected LedgerPostingPlanSpec uncheckedPostingPlan(String ledgerTransactionSn,
                                                         String planId,
                                                         List<LedgerEntrySpec> entries) {
        return new UncheckedLedgerPostingPlanSpec(planId, ledgerTransactionSn, entries);
    }

    protected LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
        return uncheckedTransaction(ledgerTransactionSn, Money.immutable(100L, CurrencyIsoCode.USD), plans);
    }

    protected LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn,
                                                         Money amount,
                                                         List<LedgerPostingPlanSpec> plans) {
        Money originalAmount = amount == null ? null : Money.immutable(amount.getAmount(), amount.getCurrency());
        return uncheckedTransaction(ledgerTransactionSn, amount, originalAmount, BigDecimal.ONE, plans);
    }

    protected LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn,
                                                         Money amount,
                                                         Money originalAmount,
                                                         BigDecimal exchangeRate,
                                                         List<LedgerPostingPlanSpec> plans) {
        return new UncheckedLedgerTransactionSpec(ledgerTransactionSn, amount, originalAmount, exchangeRate, plans);
    }

    static final class UncheckedLedgerPostingPlanSpec implements LedgerPostingPlanSpec {

        private final String planId;

        private final String ledgerTransactionSn;

        private final List<LedgerEntrySpec> entries;

        UncheckedLedgerPostingPlanSpec(String planId,
                                       String ledgerTransactionSn,
                                       List<LedgerEntrySpec> entries) {
            this.planId = planId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.entries = entries;
        }

        @Override
        public @NonNull String getPlanId() {
            return planId;
        }

        @Override
        public @NonNull String getLedgerTransactionSn() {
            return ledgerTransactionSn;
        }

        @Override
        public @NonNull LedgerPostingIntentType getIntent() {
            return LedgerPostingIntentType.TRANSFER;
        }

        @Override
        public @NonNull List<LedgerEntrySpec> getEntries() {
            return entries;
        }

        @Override
        public @NonNull List<LedgerPostingPhaseSpec> getPostingPhases() {
            return List.of();
        }
    }

    static final class UncheckedLedgerTransactionSpec implements LedgerTransactionSpec {

        private final String ledgerTransactionSn;

        private final Money amount;

        private final Money originalAmount;

        private final BigDecimal exchangeRate;

        private final List<LedgerPostingPlanSpec> plans;

        UncheckedLedgerTransactionSpec(String ledgerTransactionSn,
                                       Money amount,
                                       Money originalAmount,
                                       BigDecimal exchangeRate,
                                       List<LedgerPostingPlanSpec> plans) {
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.plans = plans;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getSn() {
            return ledgerTransactionSn;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TRANSFER;
        }

        @Override
        public @NonNull LedgerTransactionStatus getStatus() {
            return LedgerTransactionStatus.POSTED;
        }

        @Override
        public @NonNull Money getAmount() {
            return amount;
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return originalAmount;
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return exchangeRate;
        }

        @Override
        public String getBusinessSn() {
            return "TRANSFER_000000000001";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public @NonNull LocalDateTime getTransactionTime() {
            return LocalDateTime.of(2026, 5, 10, 10, 0);
        }

        @Override
        public String getDescription() {
            return "transfer";
        }

        @Override
        public @NonNull List<LedgerPostingPlanSpec> getPostingPlans() {
            return plans;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static final class RecordingProjectionService implements LedgerBalanceProjectionService {

        private final boolean supported;

        final List<List<LedgerEntrySpec>> projectedEntries = new ArrayList<>();

        RecordingProjectionService(boolean supported) {
            this.supported = supported;
        }

        @Override
        public void project(@NonNull List<LedgerEntrySpec> entries) {
            projectedEntries.add(entries);
        }

        @Override
        public boolean supports(@NonNull FundsAccountId accountId) {
            return supported;
        }
    }

    static final class RecordingLedgerService implements LedgerService {

        private final Map<Long, LedgerDTO> ledgers = new LinkedHashMap<>();

        void addLedger(LedgerDTO ledger) {
            ledgers.put(ledger.getId(), ledger);
        }

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            LedgerDTO ledger = ledgers.get(id);
            if (ledger == null) {
                throw new BaseException("账户账本不存在");
            }
            return ledger;
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            return ids.stream()
                    .map(this::getLedgerById)
                    .toList();
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                               @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException();
        }
    }

    static final class RecordingLedgerTransactionService implements LedgerTransactionService {

        final List<LedgerTransactionSpec> createdTransactions = new ArrayList<>();

        private final boolean createResultCreated;

        RecordingLedgerTransactionService() {
            this(true);
        }

        RecordingLedgerTransactionService(boolean createResultCreated) {
            this.createResultCreated = createResultCreated;
        }

        @Override
        public @NonNull LedgerTransactionCreateResult createLedgerTransaction(@NonNull LedgerTransactionSpec transaction) {
            createdTransactions.add(transaction);
            return new LedgerTransactionCreateResult()
                    .setLedgerTransactionId(1L)
                    .setCreated(createResultCreated);
        }

        @Override
        public void updateLedgerTransaction(@NonNull UpdateLedgerTransactionRequest request) {
        }

        @Override
        public void deleteLedgerTransactionByIds(@NonNull Long... ids) {
        }

        @Override
        public @NonNull LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull WindPagination<LedgerTransactionDTO> queryAccountLedgerTransactions(
                @NonNull LedgerTransactionQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(
                @NonNull LedgerEntryQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException();
        }
    }
}

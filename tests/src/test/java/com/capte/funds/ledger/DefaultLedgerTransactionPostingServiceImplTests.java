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
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.integration.funds.route.enums.FundsSubjectType;
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
import com.wind.integration.funds.route.enums.RouteNodeType;
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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingServiceImplTests {

    @Test
    void testPostShouldCreateLedgerTransactionAndProjectEntries() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        service.post(transaction);

        assertThat(transactionService.createdTransactions).containsExactly(transaction);
        assertThat(projectionService.projectedEntries).hasSize(1);
        assertThat(projectionService.projectedEntries.getFirst()).hasSize(2);
    }

    @Test
    void testPostShouldSkipProjectionWhenLedgerTransactionAlreadyExists() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService(false);
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        service.post(transaction);

        assertThat(transactionService.createdTransactions).containsExactly(transaction);
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectNullLedgerTransactionBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));

        assertThatThrownBy(() -> service.post(null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易不能为空");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectEmptyPostingPlansBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = uncheckedTransaction("LE_000000000009", List.of());

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易 postingPlans 不能为空")
                .hasMessageContaining("LE_000000000009");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldFailWhenNoProjectionServiceSupportsAccount() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(false);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到支持的账本余额投影服务");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldFailWhenMultipleProjectionServicesSupportAccount() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService firstProjectionService = new RecordingProjectionService(true);
        RecordingProjectionService secondProjectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(firstProjectionService, secondProjectionService));
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额投影服务不唯一");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(firstProjectionService.projectedEntries).isEmpty();
        assertThat(secondProjectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectUnbalancedPostingPlanEvenWhenTransactionIsBalanced() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000002";
        LedgerPostingPlanSpec debitHeavyPlan = uncheckedPostingPlan(ledgerTransactionSn, "DEBIT_HEAVY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 100L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 1L)
        ));
        LedgerPostingPlanSpec creditHeavyPlan = uncheckedPostingPlan(ledgerTransactionSn, "CREDIT_HEAVY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 1L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 100L)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(debitHeavyPlan, creditHeavyPlan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(debitHeavyPlan.isBalanced()).isFalse();
        assertThat(creditHeavyPlan.isBalanced()).isFalse();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划不平衡")
                .hasMessageContaining("DEBIT_HEAVY");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectCurrencyMismatchInsidePostingPlanBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000007";
        LedgerPostingPlanSpec mixedCurrencyPlan = uncheckedPostingPlan(ledgerTransactionSn, "MIXED_CURRENCY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 100L, CurrencyIsoCode.USD),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 100L, CurrencyIsoCode.EUR)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(mixedCurrencyPlan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("记账计划币种不一致")
                .hasMessageContaining("MIXED_CURRENCY")
                .hasMessageContaining(CurrencyIsoCode.USD.name())
                .hasMessageContaining(CurrencyIsoCode.EUR.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectNonPositiveEntryAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000008";
        LedgerPostingPlanSpec zeroAmountPlan = uncheckedPostingPlan(ledgerTransactionSn, "ZERO_AMOUNT", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 0L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 0L)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(zeroAmountPlan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(zeroAmountPlan.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录金额必须大于 0")
                .hasMessageContaining("ZERO_AMOUNT")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectEntryWithoutLedgerIdBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000003";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutLedgerId = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutLedgerId.setLedgerId(null);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entryWithoutLedgerId,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录 ledgerId 不能为空")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectLedgerBindingMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        RecordingLedgerService ledgerService = defaultLedgerService();
        ledgerService.addLedger(ledger(2L, "funding_002", FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, CurrencyIsoCode.USD));
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, ledgerService, List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000010";
        FundsTransactionTestSupport.MutableLedgerEntrySpec mismatchedEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        mismatchedEntry.setLedgerId(2L);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                mismatchedEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体与账本主体不一致")
                .hasMessageContaining("ledgerId = 2");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectExternalAccountEntryBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000004";
        FundsTransactionTestSupport.MutableLedgerEntrySpec externalEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        externalEntry.setSubjectId("external_bank_001");
        externalEntry.setSubjectType(RouteNodeType.EXTERNAL_ACCOUNT.name());
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                externalEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining("external_bank_001")
                .hasMessageContaining(RouteNodeType.EXTERNAL_ACCOUNT.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectEntryWithoutSubjectTypeBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000005";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutSubjectType = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutSubjectType.setSubjectType(null);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entryWithoutSubjectType,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    @Test
    void testPostShouldRejectUnknownSubjectTypeBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000006";
        FundsTransactionTestSupport.MutableLedgerEntrySpec unknownSubjectTypeEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        unknownSubjectTypeEntry.setSubjectType("UNKNOWN_ACCOUNT");
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                unknownSubjectTypeEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining("UNKNOWN_ACCOUNT")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    private LedgerTransactionSpec transaction() {
        String ledgerTransactionSn = "LE_000000000001";
        List<LedgerEntrySpec> entries = List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        );
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, entries);
        return transaction(ledgerTransactionSn, List.of(plan));
    }

    private LedgerTransactionSpec transaction(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
        return LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn(ledgerTransactionSn)
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(100L, CurrencyIsoCode.USD))
                .exchangeRate(BigDecimal.ONE)
                .businessSn("TRANSFER_000000000001")
                .businessScene("TRANSFER")
                .transactionTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .description("transfer")
                .postingPlans(plans)
                .contextVariables(Map.of())
                .build();
    }

    private FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide, String ledgerTransactionSn) {
        return entry(entrySide, ledgerTransactionSn, 100L);
    }

    private FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide,
                                                                     String ledgerTransactionSn,
                                                                     long amount) {
        return entry(entrySide, ledgerTransactionSn, amount, CurrencyIsoCode.USD);
    }

    private FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide,
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

    private LedgerPostingPlanSpec postingPlan(String ledgerTransactionSn, List<LedgerEntrySpec> entries) {
        return LedgerTransactionSpecFactory.postingPlan(LedgerPostingIntentType.TRANSFER,
                ledgerTransactionSn, List.of(LedgerTransactionSpecFactory.postingPhase(
                        LedgerPhaseCode.TRANSFER, entries)));
    }

    private RecordingLedgerService defaultLedgerService() {
        RecordingLedgerService ledgerService = new RecordingLedgerService();
        ledgerService.addLedger(ledger(1L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, CurrencyIsoCode.USD));
        return ledgerService;
    }

    private LedgerDTO ledger(Long id,
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

    private LedgerPostingPlanSpec uncheckedPostingPlan(String ledgerTransactionSn,
                                                       String planId,
                                                       List<LedgerEntrySpec> entries) {
        return new UncheckedLedgerPostingPlanSpec(planId, ledgerTransactionSn, entries);
    }

    private LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
        return new UncheckedLedgerTransactionSpec(ledgerTransactionSn, plans);
    }

    private static final class UncheckedLedgerPostingPlanSpec implements LedgerPostingPlanSpec {

        private final String planId;

        private final String ledgerTransactionSn;

        private final List<LedgerEntrySpec> entries;

        private UncheckedLedgerPostingPlanSpec(String planId,
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

    private static final class UncheckedLedgerTransactionSpec implements LedgerTransactionSpec {

        private final String ledgerTransactionSn;

        private final List<LedgerPostingPlanSpec> plans;

        private UncheckedLedgerTransactionSpec(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
            this.ledgerTransactionSn = ledgerTransactionSn;
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
            return Money.immutable(100L, CurrencyIsoCode.USD);
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

    private static final class RecordingProjectionService implements LedgerBalanceProjectionService {

        private final boolean supported;

        private final List<List<LedgerEntrySpec>> projectedEntries = new ArrayList<>();

        private RecordingProjectionService(boolean supported) {
            this.supported = supported;
        }

        @Override
        public void project(@NonNull List<LedgerEntrySpec> entries) {
            projectedEntries.add(entries);
        }

        @Override
        public boolean support(@NonNull FundsAccountId accountId) {
            return supported;
        }
    }

    private static final class RecordingLedgerService implements LedgerService {

        private final Map<Long, LedgerDTO> ledgers = new LinkedHashMap<>();

        private void addLedger(LedgerDTO ledger) {
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

    private static final class RecordingLedgerTransactionService implements LedgerTransactionService {

        private final List<LedgerTransactionSpec> createdTransactions = new ArrayList<>();

        private final boolean createResultCreated;

        private RecordingLedgerTransactionService() {
            this(true);
        }

        private RecordingLedgerTransactionService(boolean createResultCreated) {
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

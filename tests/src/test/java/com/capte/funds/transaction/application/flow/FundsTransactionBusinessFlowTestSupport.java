package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.ledger.DefaultLedgerPostingAssembler;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertPostingBalanced;
import static org.assertj.core.api.Assertions.assertThat;

abstract class FundsTransactionBusinessFlowTestSupport {

    protected static final Long TENANT_ID = 1L;

    protected static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    protected static final LocalDateTime ACTIVE_TIME = LocalDateTime.of(2026, 5, 14, 0, 0);

    protected FundsTransactionCommandServiceImpl service;

    protected InMemoryLedgerBook ledgerBook;

    protected RecordingLifecycleSaver lifecycleSaver;

    @BeforeEach
    void setUp() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        ledgerBook = new InMemoryLedgerBook();
        seedLedgers();
        PlatformFundingAccountService platformFundingAccountService = platformFundingAccountService();
        RouteSubjectSupport routeSubjectSupport = new RouteSubjectSupport();
        PlatformAccountRouteSupport platformAccountRouteSupport = new PlatformAccountRouteSupport(
                platformFundingAccountService);
        RouteParticipantFactory routeParticipantFactory = new RouteParticipantFactory();
        lifecycleSaver = new RecordingLifecycleSaver();
        RouteResolver routeResolver = new CompositeRouteResolver(List.of(
                new DefaultRouteReplayService(lifecycleSaver),
                new TransferFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new BalanceControlFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new AuthorizationFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport)
        ));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                new DefaultLedgerPostingAssembler(ledgerBook),
                ledgerBook,
                lifecycleSaver
        );
        service = new FundsTransactionCommandServiceImpl(
                new FundsDirectTransactionInstructionConverter(platformFundingAccountService,
                        FundsRouteTestSupport.accountQueryService(CURRENCY)),
                new FundsBalanceControlInstructionConverter(FundsRouteTestSupport.accountQueryService(CURRENCY)),
                new FundsAuthorizationInstructionConverter(FundsRouteTestSupport.accountQueryService(CURRENCY)),
                orchestrator
        );
    }

    @AfterEach
    void tearDown() {
        ThreadContextTenantIdHolder.remove();
    }

    private void seedLedgers() {
        List<FundsAccountId> fundingAccounts = List.of(
                fundingAccount("funding_user"),
                fundingAccount("funding_user_a"),
                fundingAccount("funding_user_b"),
                fundingAccount("merchant_001"),
                fundingAccount("funding_adjust_user")
        );
        fundingAccounts.forEach(accountId -> {
            ledgerBook.ensureLedger(accountId, LedgerSubjectCode.AVAILABLE, 0L);
            ledgerBook.ensureLedger(accountId, LedgerSubjectCode.FROZEN, 0L);
            ledgerBook.ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT, 0L);
        });
        FundsAccountId credit = creditAccount("credit_001");
        ledgerBook.ensureLedger(credit, LedgerSubjectCode.LIMIT, 100L);
        ledgerBook.ensureLedger(credit, LedgerSubjectCode.AVAILABLE, 0L);
        ledgerBook.ensureLedger(credit, LedgerSubjectCode.AUTHORIZATION, 0L);
        FundsAccountId authorizedCredit = creditAccount("credit_auth_001");
        ledgerBook.ensureLedger(authorizedCredit, LedgerSubjectCode.LIMIT, 100L);
        ledgerBook.ensureLedger(authorizedCredit, LedgerSubjectCode.AVAILABLE, 500L);
        ledgerBook.ensureLedger(authorizedCredit, LedgerSubjectCode.AUTHORIZATION, 0L);
        FundsAccountId sharedCredit = creditAccount("credit_shared_001");
        ledgerBook.ensureLedger(sharedCredit, LedgerSubjectCode.LIMIT, 300L);
        ledgerBook.ensureLedger(sharedCredit, LedgerSubjectCode.AVAILABLE, 500L);
        ledgerBook.ensureLedger(sharedCredit, LedgerSubjectCode.AUTHORIZATION, 0L);
        FundsAccountId budgetGroup = budgetGroup("budget_001");
        ledgerBook.ensureLedger(budgetGroup, LedgerSubjectCode.LIMIT, 0L);
        ledgerBook.ensureLedger(budgetGroup, LedgerSubjectCode.AVAILABLE, 100L);
        ledgerBook.ensureLedger(budgetGroup, LedgerSubjectCode.AUTHORIZATION, 0L);
        FundsAccountId sharedBudgetGroup = budgetGroup("budget_shared_001");
        ledgerBook.ensureLedger(sharedBudgetGroup, LedgerSubjectCode.LIMIT, 0L);
        ledgerBook.ensureLedger(sharedBudgetGroup, LedgerSubjectCode.AVAILABLE, 400L);
        ledgerBook.ensureLedger(sharedBudgetGroup, LedgerSubjectCode.AUTHORIZATION, 0L);
        FundsAccountId sharedFunding = fundingAccount("funding_shared_001");
        ledgerBook.ensureLedger(sharedFunding, LedgerSubjectCode.AVAILABLE, 300L);
        ledgerBook.ensureLedger(sharedFunding, LedgerSubjectCode.AUTHORIZATION, 0L);
        ledgerBook.ensureLedger(cashMappingAccount(), LedgerSubjectCode.CASH, 10_000L);
        ledgerBook.ensureLedger(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L);
        ledgerBook.ensureLedger(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L);
        ledgerBook.ensureLedger(feeAccount(), LedgerSubjectCode.FEE, 0L);
        ledgerBook.ensureLedger(adjustmentAccount(), LedgerSubjectCode.ADJUSTMENT, 0L);
    }

    protected void topup(FundsAccountId accountId, long amount, String businessSn) {
        service.topup(new FundsTransactionTopupRequest()
                .setAccountId(accountId)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn(businessSn + "_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("TOPUP")
                .setBusinessSn(businessSn)
                .setDescription("topup"), WindOperator.system());
    }

    protected String pay(FundsAccountId payer, FundsAccountId merchant, long amount, String businessSn) {
        return pay(payer, merchant, amount, null, businessSn);
    }

    protected String pay(FundsAccountId payer,
                       FundsAccountId merchant,
                       long amount,
                       @Nullable FeeSpec feeSpec,
                       String businessSn) {
        return service.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(merchant)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setFeeSpec(feeSpec)
                .setBusinessScene("PAY")
                .setBusinessSn(businessSn)
                .setDescription("pay"), WindOperator.system());
    }

    protected void refund(FundsAccountId accountId, FundsAccountId merchant, long amount, String businessSn) {
        service.refund(new FundsTransactionRefundRequest()
                .setAccountId(accountId)
                .setPayerId(merchant)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(amount(amount))
                .setBusinessScene("REFUND")
                .setBusinessSn(businessSn)
                .setDescription("refund"), WindOperator.system());
    }

    protected void refundFee(FundsAccountId accountId, long amount, String feeTransactionSn, String businessSn) {
        service.refundFee(new FundsTransactionFeeRefundRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setFeeSourceTransactionSn(feeTransactionSn)
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("fee refund"), WindOperator.system());
    }

    protected void transfer(FundsAccountId payer, FundsAccountId payee, long amount, String businessSn) {
        service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn(businessSn)
                .setDescription("transfer"), WindOperator.system());
    }

    protected String freeze(FundsAccountId accountId, long amount, String businessSn) {
        return service.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setBusinessScene("FREEZE")
                .setBusinessSn(businessSn)
                .setDescription("freeze"), WindOperator.system());
    }

    protected void unfreeze(FundsAccountId accountId, long amount, String referenceFreezeSn, String businessSn) {
        service.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setReferenceFreezeSn(referenceFreezeSn)
                .setBusinessScene("UNFREEZE")
                .setBusinessSn(businessSn)
                .setDescription("unfreeze"), WindOperator.system());
    }

    protected void withdraw(FundsAccountId accountId, long amount, String businessSn) {
        service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsAccountId.immutable("external_bank_001", DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(businessSn + "_FREEZE")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn(businessSn)
                .setDescription("withdraw"), WindOperator.system());
    }

    protected void adjust(FundsAccountId accountId,
                        long amount,
                        boolean increase,
                        String businessScene,
                        String businessSn) {
        service.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setIncrease(increase)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setAdjustReason("adjust reason")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn)
                .setDescription("adjust"), WindOperator.system());
    }

    protected String authorize(FundsAccountId accountId, long amount, String businessSn) {
        return service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setApproved(true)
                .setBusinessScene("AUTHORIZE")
                .setBusinessSn(businessSn)
                .setAuthorizedTime(ACTIVE_TIME)
                .setDescription("authorize"), WindOperator.system());
    }

    protected String authorizeDeclined(FundsAccountId accountId,
                                     long amount,
                                     String declineReason,
                                     String businessSn) {
        return service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setApproved(Boolean.FALSE)
                .setDeclineReason(declineReason)
                .setBusinessScene("AUTHORIZE")
                .setBusinessSn(businessSn)
                .setAuthorizedTime(ACTIVE_TIME)
                .setDescription("authorize declined"), WindOperator.system());
    }

    protected String authorizeSharedCard(FundsAccountId accountId,
                                       FundsAccountId budgetGroupId,
                                       FundsAccountId fundingAccountId,
                                       long amount,
                                       String businessSn) {
        return service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setApproved(true)
                .setBusinessScene("AUTHORIZE")
                .setBusinessSn(businessSn)
                .setAuthorizedTime(ACTIVE_TIME)
                .setContextVariables(contextVariables(Map.of(
                        FundsInstructionContextKeys.LINKED_BUDGET_GROUP_ID, budgetGroupId,
                        FundsInstructionContextKeys.LINKED_FUNDING_ACCOUNT_ID, fundingAccountId)))
                .setDescription("shared card authorize"), WindOperator.system());
    }

    protected void reversal(FundsAccountId accountId, long amount, String authorizationSn, String businessSn) {
        service.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene("REVERSAL")
                .setBusinessSn(businessSn)
                .setReversalTime(ACTIVE_TIME)
                .setDescription("reversal"), WindOperator.system());
    }

    protected String settle(FundsAccountId accountId, long amount, String authorizationSn, String businessSn) {
        return service.settle(new FundsAuthorizationTransactionSettleRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene("SETTLE")
                .setBusinessSn(businessSn)
                .setSettleTime(ACTIVE_TIME)
                .setDescription("settle"), WindOperator.system());
    }

    protected void authRefund(FundsAccountId accountId, long amount, String settlementSn, String businessSn) {
        service.settleRefund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setAuthorizationTransactionSn(settlementSn)
                .setBusinessScene("AUTH_REFUND")
                .setBusinessSn(businessSn)
                .setRefundTime(ACTIVE_TIME)
                .setDescription("auth refund"), WindOperator.system());
    }

    protected List<FundsSubjectBalanceDTO> balances(FundsAccountId... accountIds) {
        return List.of(accountIds).stream()
                .map(ledgerBook::balance)
                .toList();
    }

    protected void assertPostedTransactions(int expectedSize) {
        assertThat(ledgerBook.postedTransactions).hasSize(expectedSize);
        ledgerBook.postedTransactions.forEach(transaction -> {
            assertPostingBalanced(transaction);
            assertThat(transaction.getFundsTransactionSn()).isNotBlank();
        });
        assertThat(lifecycleSaver.succeededLedgerTransactionSns).hasSize(expectedSize);
    }

    protected static void assertEntriesForSubject(LedgerTransactionSpec transaction,
                                                FundsAccountId subjectRef,
                                                LedgerSubjectCode... subjectCodes) {
        assertThat(entriesOf(transaction).stream()
                .filter(entry -> Objects.equals(entry.getSubjectId(), subjectRef.id())
                        && Objects.equals(entry.getSubjectType(), subjectRef.type()))
                .map(LedgerEntrySpec::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(subjectCodes);
    }

    protected static void assertNoEntriesForSubject(LedgerTransactionSpec transaction,
                                                  FundsAccountId subjectRef) {
        assertThat(entriesOf(transaction).stream()
                .filter(entry -> Objects.equals(entry.getSubjectId(), subjectRef.id())
                        && Objects.equals(entry.getSubjectType(), subjectRef.type()))
                .toList())
                .isEmpty();
    }

    protected static void assertNoLedgerSubject(LedgerTransactionSpec transaction,
                                              LedgerSubjectCode subjectCode) {
        assertThat(entriesOf(transaction).stream()
                .map(LedgerEntrySpec::getLedgerSubjectCode)
                .toList())
                .doesNotContain(subjectCode);
    }

    private static List<LedgerEntrySpec> entriesOf(LedgerTransactionSpec transaction) {
        return transaction.getPostingPlans().stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .toList();
    }

    protected static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    protected static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT.name());
    }

    protected static FundsAccountId budgetGroup(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.BUDGET_GROUP.name());
    }

    protected static FundsAccountId cashMappingAccount() {
        return platformAccount(PlatformFundingAccountRole.CASH_MAPPING);
    }

    protected static FundsAccountId prepaymentAccount() {
        return platformAccount(PlatformFundingAccountRole.PREPAYMENT);
    }

    protected static FundsAccountId settlementAccount() {
        return platformAccount(PlatformFundingAccountRole.SETTLEMENT);
    }

    protected static FundsAccountId feeAccount() {
        return platformAccount(PlatformFundingAccountRole.FEE);
    }

    protected static FundsAccountId adjustmentAccount() {
        return platformAccount(PlatformFundingAccountRole.ADJUSTMENT);
    }

    private static FundsAccountId platformAccount(PlatformFundingAccountRole role) {
        return FundsAccountId.immutable("platform_" + role.name().toLowerCase(), FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    private static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    protected static FeeSpec fixedFeeSpec(long feeAmount) {
        return FeeSpec.builder()
                .feeType(DefaultFeeType.FEE.getCode())
                .fixedFee(Math.toIntExact(feeAmount))
                .build();
    }

    private static WritableContextVariables contextVariables(Map<String, Object> variables) {
        TestContextVariables result = new TestContextVariables();
        variables.forEach(result::putVariable);
        return result;
    }

    private static PlatformFundingAccountService platformFundingAccountService() {
        return new PlatformFundingAccountService() {
            @Override
            public FundsAccountId requireAccountId(CurrencyIsoCode currency, PlatformFundingAccountRole role) {
                return requireAccountId(TENANT_ID, currency, role);
            }

            @Override
            public FundsAccountId requireAccountId(Long tenantId, CurrencyIsoCode currency,
                                                   PlatformFundingAccountRole role) {
                return platformAccount(role);
            }
        };
    }

    private record LedgerKey(Long tenantId,
                             String subjectId,
                             String subjectType,
                             LedgerSubjectCode subjectCode,
                             AccountBalancePeriodType periodType,
                             String periodId) {

        private static LedgerKey of(FundsAccountId accountId, LedgerSubjectCode subjectCode) {
            return new LedgerKey(TENANT_ID, accountId.id(), accountId.type(), subjectCode,
                    AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
        }

        private static LedgerKey of(LedgerQuery query) {
            return new LedgerKey(query.getTenantId(), query.getSubjectId(), query.getSubjectType(),
                    query.getLedgerSubjectCode(), query.getPeriodType(), query.getPeriodId());
        }
    }

    private static final class TestContextVariables implements WritableContextVariables {

        private final Map<String, Object> variables = new LinkedHashMap<>();

        @Override
        public WritableContextVariables putVariable(String name, Object val) {
            variables.put(name, val);
            return this;
        }

        @Override
        public WritableContextVariables removeVariable(String name) {
            variables.remove(name);
            return this;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.copyOf(variables);
        }
    }

    protected static final class InMemoryLedgerBook implements LedgerService, LedgerTransactionPostingService {

        private final Map<LedgerKey, LedgerDTO> ledgers = new LinkedHashMap<>();

        private final Map<Long, LedgerDTO> ledgersById = new LinkedHashMap<>();

        final List<LedgerTransactionSpec> postedTransactions = new ArrayList<>();

        private long nextLedgerId = 1L;

        private void ensureLedger(FundsAccountId accountId, LedgerSubjectCode subjectCode, long initialBalance) {
            LedgerKey key = LedgerKey.of(accountId, subjectCode);
            if (ledgers.containsKey(key)) {
                return;
            }
            LedgerDTO ledger = new LedgerDTO()
                    .setId(nextLedgerId++)
                    .setGmtCreate(ACTIVE_TIME)
                    .setGmtModified(ACTIVE_TIME)
                    .setTenantId(TENANT_ID)
                    .setSubjectId(accountId.id())
                    .setSubjectType(accountId.type())
                    .setLedgerProfileCode("TEST")
                    .setLedgerProfileVersion(1)
                    .setLedgerSubjectCode(subjectCode)
                    .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                    .setNormalBalanceSide(EntrySide.CREDIT)
                    .setAllowNegative(true)
                    .setDebitAmount(initialBalance < 0 ? -initialBalance : 0L)
                    .setCreditAmount(initialBalance > 0 ? initialBalance : 0L)
                    .setCurrency(CURRENCY)
                    .setSettlementPolicy("IMMEDIATE")
                    .setCutOffTime(LocalTime.MIDNIGHT)
                    .setPeriodType(AccountBalancePeriodType.LIFETIME)
                    .setPeriodId(AccountBalancePeriodType.LIFETIME.name())
                    .setVersion(1);
            ledgers.put(key, ledger);
            ledgersById.put(ledger.getId(), ledger);
        }

        FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
            Map<LedgerSubjectCode, LedgerBalanceBucket> buckets = new LinkedHashMap<>();
            ledgers.entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getKey().subjectId(), accountId.id())
                            && Objects.equals(entry.getKey().subjectType(), accountId.type()))
                    .forEach(entry -> {
                        LedgerDTO ledger = entry.getValue();
                        buckets.put(ledger.getLedgerSubjectCode(), LedgerBalanceBucket.builder()
                                .accountCode(ledger.getLedgerSubjectCode())
                                .balance(Money.immutable(ledger.getNormalBalance(), CURRENCY))
                                .periodType(ledger.getPeriodType())
                                .periodId(ledger.getPeriodId())
                                .activeTime(ACTIVE_TIME)
                                .build());
                    });
            return new FundsSubjectBalanceDTO()
                    .setId(1L)
                    .setTenantId(TENANT_ID)
                    .setSubjectRef(accountId)
                    .setCurrency(CURRENCY)
                    .setInitialized(!buckets.isEmpty())
                    .setBalanceBuckets(buckets);
        }

        @Override
        public void post(LedgerTransactionSpec transaction) {
            assertPostingBalanced(transaction);
            postedTransactions.add(transaction);
            transaction.getPostingPlans().stream()
                    .map(LedgerPostingPlanSpec::getEntries)
                    .flatMap(List::stream)
                    .forEach(this::applyEntry);
        }

        private void applyEntry(LedgerEntrySpec entry) {
            LedgerDTO ledger = ledgersById.get(entry.getLedgerId());
            assertThat(ledger)
                    .as("ledger %s for %s/%s", entry.getLedgerId(), entry.getSubjectId(),
                            entry.getLedgerSubjectCode())
                    .isNotNull();
            if (entry.getEntryType() == EntrySide.DEBIT) {
                ledger.setDebitAmount(ledger.getDebitAmount() + entry.getAmount().getAmount());
            } else {
                ledger.setCreditAmount(ledger.getCreditAmount() + entry.getAmount().getAmount());
            }
            ledger.setGmtModified(entry.getTransactionTime());
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                                @NonNull WindQuery<? extends QueryOrderField> options) {
            LedgerDTO ledger = ledgers.get(LedgerKey.of(query));
            return pagination(ledger == null ? List.of() : List.of(ledger));
        }

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException("createLedger");
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            throw new UnsupportedOperationException("updateLedgerBalance");
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException("deleteLedgerByIds");
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            LedgerDTO ledger = ledgersById.get(id);
            assertThat(ledger).as("ledger %s", id).isNotNull();
            return ledger;
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            return ids.stream().map(this::getLedgerById).toList();
        }
    }

    protected static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver,
            FundsTransactionQueryService {

        private final AtomicInteger transactionSequence = new AtomicInteger();

        final List<String> succeededLedgerTransactionSns = new ArrayList<>();

        final Map<String, RouteSnapshotSpec> routeSnapshots = new LinkedHashMap<>();

        private final Map<String, RouteSnapshotSpec> freezeOrderSnapshots = new LinkedHashMap<>();

        private final List<ConsumedReplayLeg> consumedReplayLegs = new ArrayList<>();

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                      @NonNull ResolvedRouteSpec resolvedRoute,
                                                                      @NonNull RouteSnapshotSpec routeSnapshot) {
            String transactionSn = "FT_" + String.format("%06d", transactionSequence.incrementAndGet());
            routeSnapshots.put(transactionSn, routeSnapshot);
            if (instruction.getReference() == null
                    && instruction.getEventType() == FundsTransactionEventType.FREEZE) {
                freezeOrderSnapshots.put(transactionSn, routeSnapshot);
            }
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn(transactionSn)
                    .setTransactionDetailSns(List.of(transactionSn + "_DETAIL"))
                    .setCompleted(false);
        }

        @Override
        public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                                  @NonNull FundsInstructionLifecycleResult result,
                                  @Nullable String ledgerTransactionSn) {
            RouteSnapshotSpec routeSnapshot = routeSnapshots.get(result.getTransactionSn());
            if (instruction.getReference() != null && routeSnapshot != null) {
                recordConsumedReplayLegs(instruction, routeSnapshot);
            }
            if (ledgerTransactionSn != null) {
                succeededLedgerTransactionSns.add(ledgerTransactionSn);
            }
        }

        @Override
        public void markFailed(@NonNull FundsInstructionSpec instruction,
                               @NonNull FundsInstructionLifecycleResult result,
                               @NonNull Throwable cause) {
            throw new AssertionError("unexpected lifecycle failure", cause);
        }

        @Override
        public @NonNull Optional<FundsTransactionDTO> queryFundsTransaction(@NonNull String transactionSn) {
            return Optional.empty();
        }

        @Override
        public @NonNull List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(@NonNull String referenceTransactionSn,
                                            @NonNull FundsTransactionEventType eventType,
                                            @NonNull String replayRefLegId) {
            return false;
        }

        @Override
        public @NonNull Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                                         @NonNull FundsTransactionEventType eventType,
                                                         @NonNull String replayRefLegId,
                                                         @NonNull CurrencyIsoCode currency) {
            long amount = consumedReplayLegs
                    .stream()
                    .filter(leg -> replayRefLegId.equals(leg.replayRefLegId()))
                    .filter(leg -> leg.eventType() == eventType)
                    .filter(leg -> leg.currency() == currency)
                    .mapToLong(ConsumedReplayLeg::amount)
                    .sum();
            return Money.immutable(amount, currency);
        }

        private void recordConsumedReplayLegs(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
            routeSnapshot.getLegs()
                    .stream()
                    .filter(leg -> leg.getReplayRefLegId() != null && !leg.getReplayRefLegId().isBlank())
                    .map(leg -> new ConsumedReplayLeg(instruction.getEventType(), leg.getReplayRefLegId(),
                            leg.getAmount().getAmount(), leg.getAmount().getCurrency()))
                    .forEach(consumedReplayLegs::add);
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            return Optional.ofNullable(routeSnapshots.get(transactionSn));
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.ofNullable(freezeOrderSnapshots.get(freezeOrderSn));
        }
    }

    private record ConsumedReplayLeg(FundsTransactionEventType eventType,
                                     String replayRefLegId,
                                     long amount,
                                     CurrencyIsoCode currency) {
    }

    @SuppressWarnings("unchecked")
    private static WindPagination<LedgerDTO> pagination(List<LedgerDTO> records) {
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
                        return records;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}

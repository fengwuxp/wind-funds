package com.capte.funds.transaction.flow;

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
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.ledger.DefaultLedgerPostingAssembler;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
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
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertPostingBalanced;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionBusinessFlowIntegrationTests {

    private static final Long TENANT_ID = 1L;

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final LocalDateTime ACTIVE_TIME = LocalDateTime.of(2026, 5, 14, 0, 0);

    private FundsTransactionCommandServiceImpl service;

    private InMemoryLedgerBook ledgerBook;

    private RecordingLifecycleSaver lifecycleSaver;

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

    /**
     * 场景：用户充值后付款给商户，后续由商户原路退款一部分。
     * 输入：充值 100、付款 40、退款 20。
     * 输出：用户 AVAILABLE、商户 SETTLEMENT、平台 CASH/PREPAYMENT 余额快照。
     * 预期：最终用户可用余额 +80，商户结算余额 +20，平台预收款归零，所有已入账交易借贷平衡。
     */
    @Test
    void testTopupPayRefundShouldKeepLedgerBalances() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId merchant = fundingAccount("merchant_001");
        BalanceSnapshot before = snapshot(balances(user, merchant, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "TOPUP_PAY_REFUND_TOPUP");
        pay(user, merchant, 40L, "TOPUP_PAY_REFUND_PAY");
        refund(user, merchant, 20L, "TOPUP_PAY_REFUND_REFUND");

        BalanceSnapshot after = snapshot(balances(user, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(user, LedgerSubjectCode.AVAILABLE, 80L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 80L, CURRENCY);
        assertBucket(ledgerBook.balance(merchant), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);
        assertPostedTransactions(3);
    }

    /**
     * 场景：用户充值后带业务显式手续费付款，后续商户对付款主金额做部分退款。
     * 输入：充值 100、付款 40、固定手续费 5、退款 20。
     * 输出：用户 AVAILABLE、商户 SETTLEMENT、平台 FEE 和平台 CASH/PREPAYMENT 余额快照。
     * 预期：用户充值后可用 +100，付款后用户 -45、商户 +40、平台费用 +5，退款后用户 +20、商户 -20。
     * 红线：手续费由交易层请求显式传入并独立入账，退款只回补付款主金额，不冲回平台手续费。
     */
    @Test
    void testTopupPayWithFeeRefundShouldKeepLedgerBalances() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId merchant = fundingAccount("merchant_001");
        FundsAccountId fee = feeAccount();
        BalanceSnapshot before = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "TOPUP_PAY_FEE_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(user, merchant, 40L, fixedFeeSpec(5L), "TOPUP_PAY_FEE_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(user, LedgerSubjectCode.AVAILABLE, -45L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        refund(user, merchant, 20L, "TOPUP_PAY_FEE_REFUND_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, -20L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 75L, CURRENCY);
        assertBucket(ledgerBook.balance(merchant), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);
        assertBucket(ledgerBook.balance(fee), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertPostedTransactions(3);
    }

    /**
     * 场景：用户充值后先冻结余额，再确认提现出款。
     * 输入：充值 100、冻结 60、提现 60。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照。
     * 预期：提现成功扣减已冻结的 FROZEN，完成后用户可用余额保留 40，冻结余额归零。
     */
    @Test
    void testTopupFreezeWithdrawShouldKeepLedgerBalances() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "TOPUP_FREEZE_WITHDRAW_TOPUP");
        freeze(user, 60L, "TOPUP_FREEZE_WITHDRAW_FREEZE");
        withdraw(user, 60L, "TOPUP_FREEZE_WITHDRAW_WITHDRAW");

        BalanceSnapshot after = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(3);
    }

    /**
     * 场景：用户 A 充值后转给 B，B 支付给商户，并把剩余资金冻结后提现。
     * 输入：A 充值 150、A 转 B 100、B 付款 40、B 冻结 60、B 提现 60。
     * 输出：A/B/商户/平台的余额快照。
     * 预期：A 保留 50，B 可用和冻结归零，商户结算余额 +40，平台 CASH 净减少 90。
     */
    @Test
    void testTopupTransferPayWithdrawShouldKeepLedgerBalances() {
        FundsAccountId userA = fundingAccount("funding_user_a");
        FundsAccountId userB = fundingAccount("funding_user_b");
        FundsAccountId merchant = fundingAccount("merchant_001");
        BalanceSnapshot before = snapshot(balances(userA, userB, merchant, cashMappingAccount(), prepaymentAccount()));

        topup(userA, 150L, "TOPUP_TRANSFER_PAY_WITHDRAW_TOPUP");
        transfer(userA, userB, 100L, "TOPUP_TRANSFER_PAY_WITHDRAW_TRANSFER");
        pay(userB, merchant, 40L, "TOPUP_TRANSFER_PAY_WITHDRAW_PAY");
        freeze(userB, 60L, "TOPUP_TRANSFER_PAY_WITHDRAW_FREEZE");
        withdraw(userB, 60L, "TOPUP_TRANSFER_PAY_WITHDRAW_WITHDRAW");

        BalanceSnapshot after = snapshot(balances(userA, userB, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(userA, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(userB, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(userB, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -90L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(ledgerBook.balance(userA), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(ledgerBook.balance(userB), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(ledgerBook.balance(merchant), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertPostedTransactions(5);
    }

    private void seedLedgers() {
        List<FundsAccountId> fundingAccounts = List.of(
                fundingAccount("funding_user"),
                fundingAccount("funding_user_a"),
                fundingAccount("funding_user_b"),
                fundingAccount("merchant_001")
        );
        fundingAccounts.forEach(accountId -> {
            ledgerBook.ensureLedger(accountId, LedgerSubjectCode.AVAILABLE, 0L);
            ledgerBook.ensureLedger(accountId, LedgerSubjectCode.FROZEN, 0L);
            ledgerBook.ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT, 0L);
        });
        ledgerBook.ensureLedger(cashMappingAccount(), LedgerSubjectCode.CASH, 10_000L);
        ledgerBook.ensureLedger(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L);
        ledgerBook.ensureLedger(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L);
        ledgerBook.ensureLedger(feeAccount(), LedgerSubjectCode.FEE, 0L);
    }

    private void topup(FundsAccountId accountId, long amount, String businessSn) {
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

    private void pay(FundsAccountId payer, FundsAccountId merchant, long amount, String businessSn) {
        pay(payer, merchant, amount, null, businessSn);
    }

    private void pay(FundsAccountId payer,
                     FundsAccountId merchant,
                     long amount,
                     @Nullable FeeSpec feeSpec,
                     String businessSn) {
        service.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(merchant)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setFeeSpec(feeSpec)
                .setBusinessScene("PAY")
                .setBusinessSn(businessSn)
                .setDescription("pay"), WindOperator.system());
    }

    private void refund(FundsAccountId accountId, FundsAccountId merchant, long amount, String businessSn) {
        service.refund(new FundsTransactionRefundRequest()
                .setAccountId(accountId)
                .setPayerId(merchant)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(amount(amount))
                .setBusinessScene("REFUND")
                .setBusinessSn(businessSn)
                .setDescription("refund"), WindOperator.system());
    }

    private void transfer(FundsAccountId payer, FundsAccountId payee, long amount, String businessSn) {
        service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn(businessSn)
                .setDescription("transfer"), WindOperator.system());
    }

    private void freeze(FundsAccountId accountId, long amount, String businessSn) {
        service.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setBusinessScene("FREEZE")
                .setBusinessSn(businessSn)
                .setDescription("freeze"), WindOperator.system());
    }

    private void withdraw(FundsAccountId accountId, long amount, String businessSn) {
        service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsAccountId.immutable("external_bank_001", DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(businessSn + "_FREEZE")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn(businessSn)
                .setDescription("withdraw"), WindOperator.system());
    }

    private List<FundsSubjectBalanceDTO> balances(FundsAccountId... accountIds) {
        return List.of(accountIds).stream()
                .map(ledgerBook::balance)
                .toList();
    }

    private void assertPostedTransactions(int expectedSize) {
        assertThat(ledgerBook.postedTransactions).hasSize(expectedSize);
        ledgerBook.postedTransactions.forEach(transaction -> {
            assertPostingBalanced(transaction);
            assertThat(transaction.getFundsTransactionSn()).isNotBlank();
        });
        assertThat(lifecycleSaver.succeededLedgerTransactionSns).hasSize(expectedSize);
    }

    private static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    private static FundsAccountId cashMappingAccount() {
        return platformAccount(PlatformFundingAccountRole.CASH_MAPPING);
    }

    private static FundsAccountId prepaymentAccount() {
        return platformAccount(PlatformFundingAccountRole.PREPAYMENT);
    }

    private static FundsAccountId settlementAccount() {
        return platformAccount(PlatformFundingAccountRole.SETTLEMENT);
    }

    private static FundsAccountId feeAccount() {
        return platformAccount(PlatformFundingAccountRole.FEE);
    }

    private static FundsAccountId platformAccount(PlatformFundingAccountRole role) {
        return FundsAccountId.immutable("platform_" + role.name().toLowerCase(), FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    private static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    private static FeeSpec fixedFeeSpec(long feeAmount) {
        return FeeSpec.builder()
                .feeType(DefaultFeeType.FEE.getCode())
                .fixedFee(Math.toIntExact(feeAmount))
                .build();
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

    private static final class InMemoryLedgerBook implements LedgerService, LedgerTransactionPostingService {

        private final Map<LedgerKey, LedgerDTO> ledgers = new LinkedHashMap<>();

        private final Map<Long, LedgerDTO> ledgersById = new LinkedHashMap<>();

        private final List<LedgerTransactionSpec> postedTransactions = new ArrayList<>();

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

        private FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
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

    private static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver,
            FundsTransactionQueryService {

        private final AtomicInteger transactionSequence = new AtomicInteger();

        private final List<String> succeededLedgerTransactionSns = new ArrayList<>();

        private final Map<String, RouteSnapshotSpec> routeSnapshots = new LinkedHashMap<>();

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
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn(transactionSn)
                    .setTransactionDetailSns(List.of(transactionSn + "_DETAIL"))
                    .setCompleted(false);
        }

        @Override
        public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                                  @NonNull FundsInstructionLifecycleResult result,
                                  @Nullable String ledgerTransactionSn) {
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
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            return Optional.ofNullable(routeSnapshots.get(transactionSn));
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.empty();
        }
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

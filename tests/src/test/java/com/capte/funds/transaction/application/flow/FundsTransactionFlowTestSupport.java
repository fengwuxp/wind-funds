package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.entities.table.LedgerEntryNameRefs;
import com.capte.funds.ledger.dal.entities.table.LedgerNameRefs;
import com.capte.funds.ledger.dal.entities.table.LedgerPostingPlanNameRefs;
import com.capte.funds.ledger.dal.entities.table.LedgerTransactionNameRefs;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.capte.funds.transaction.application.FundsBalanceControlService;
import com.capte.funds.transaction.application.FundsDirectTransactionService;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.ledger.DefaultLedgerPostingAssembler;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.capte.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.capte.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.capte.funds.wallet.ImmutableFundsAccount;
import com.capte.funds.wallet.ImmutableFundsBalanceView;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.supports.DefaultPageQueryOptions;
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
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金交易流程测试基座。
 *
 * <p>服务层流程测试只替换外部账户查询和平台账户发现边界；交易转换、路由解析、编排器、记账翻译、
 * lifecycle saver、posting service、mapper 和 H2 表结构均使用真实实现，确保断言落在持久化事实上。</p>
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class
})
abstract class FundsTransactionFlowTestSupport extends AbstractFundsServiceTest {

    private static final LocalDateTime ACTIVE_TIME = LocalDateTime.of(2026, 5, 18, 0, 0);

    private static final int MAX_LEDGER_BUCKET_SIZE = 50;

    @Autowired
    protected FundsDirectTransactionService directTransactionService;

    @Autowired
    protected FundsBalanceControlService balanceControlService;

    @Autowired
    protected LedgerService ledgerService;

    @Autowired
    private LedgerTransactionMapper ledgerTransactionMapper;

    @Autowired
    private LedgerPostingPlanMapper ledgerPostingPlanMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @BeforeEach
    void setUp() {
        seedLedgers();
    }

    private void seedLedgers() {
        FundsAccountId user = fundingAccount("funding_user");
        ensureLedger(user, LedgerSubjectCode.AVAILABLE, 0L);
        ensureLedger(user, LedgerSubjectCode.FROZEN, 0L);
        ensureLedger(cashMappingAccount(), LedgerSubjectCode.CASH, 10_000L);
        ensureLedger(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L);
    }

    private void ensureLedger(FundsAccountId accountId,
                              LedgerSubjectCode ledgerSubjectCode,
                              long initialBalance) {
        Optional<LedgerDTO> existing = findLedger(accountId, ledgerSubjectCode);
        if (existing.isPresent()) {
            return;
        }
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setLedgerProfileCode("TEST")
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        if (initialBalance != 0L) {
            ledgerService.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                    .setId(ledgerId)
                    .setCreditAmountDelta(initialBalance > 0L ? initialBalance : null)
                    .setDebitAmountDelta(initialBalance < 0L ? -initialBalance : null));
        }
    }

    protected void ensureLedger(FundsAccountId accountId, LedgerSubjectCode ledgerSubjectCode) {
        ensureLedger(accountId, ledgerSubjectCode, 0L);
    }

    protected void topup(FundsAccountId accountId, long amount, String businessSn) {
        directTransactionService.topup(new FundsTransactionTopupRequest()
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

    protected void pay(FundsAccountId accountId,
                       FundsAccountId payeeId,
                       LedgerSubjectCode payeeLedgerCode,
                       long amount,
                       String businessSn) {
        directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(accountId)
                .setPayeeId(payeeId)
                .setPayeeLedgerCode(payeeLedgerCode)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("PAY")
                .setBusinessSn(businessSn)
                .setDescription("pay"), WindOperator.system());
    }

    protected void refund(FundsAccountId accountId,
                          FundsAccountId payerId,
                          LedgerSubjectCode payerLedgerCode,
                          long amount,
                          String businessSn) {
        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(accountId)
                .setPayerId(payerId)
                .setPayerLedgerCode(payerLedgerCode)
                .setAmount(amount(amount))
                .setBusinessScene("REFUND")
                .setBusinessSn(businessSn)
                .setDescription("refund"), WindOperator.system());
    }

    protected String freeze(FundsAccountId accountId, long amount, String businessSn) {
        return balanceControlService.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setBusinessScene("FREEZE")
                .setBusinessSn(businessSn)
                .setDescription("freeze"), WindOperator.system());
    }

    protected void unfreeze(FundsAccountId accountId, long amount, String referenceFreezeSn, String businessSn) {
        balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setReferenceFreezeSn(referenceFreezeSn)
                .setBusinessScene("UNFREEZE")
                .setBusinessSn(businessSn)
                .setDescription("unfreeze"), WindOperator.system());
    }

    protected List<FundsSubjectBalanceDTO> balances(FundsAccountId... accountIds) {
        return List.of(accountIds).stream()
                .map(this::balance)
                .toList();
    }

    protected FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
        Map<LedgerSubjectCode, LedgerBalanceBucket> buckets = balanceBuckets(accountId);
        return new FundsSubjectBalanceDTO()
                .setId(1L)
                .setTenantId(TENANT_ID)
                .setSubjectRef(accountId)
                .setCurrency(CURRENCY)
                .setInitialized(!buckets.isEmpty())
                .setBalanceBuckets(buckets);
    }

    protected void assertPostedTransactions(int expectedSize) {
        List<LedgerTransaction> transactions = ledgerTransactions();
        assertThat(transactions).hasSize(expectedSize);
        transactions.forEach(this::assertValidPostedTransaction);
    }

    protected List<LedgerTransaction> ledgerTransactions() {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .orderBy(ref.id.asc());
        return ledgerTransactionMapper.selectListByQuery(wrapper);
    }

    protected LedgerTransaction ledgerTransactionByBusinessSn(String businessSn) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn));
        LedgerTransaction result = ledgerTransactionMapper.selectOneByQuery(wrapper);
        assertThat(result).as("ledger transaction for businessSn %s", businessSn).isNotNull();
        return result;
    }

    protected List<LedgerPostingPlan> postingPlansOf(LedgerTransaction transaction) {
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.ledgerTransactionSn.eq(transaction.getSn()))
                .orderBy(ref.id.asc());
        return ledgerPostingPlanMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerEntry> entriesOf(LedgerTransaction transaction) {
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.ledgerTransactionSn.eq(transaction.getSn()))
                .orderBy(ref.id.asc());
        return ledgerEntryMapper.selectListByQuery(wrapper);
    }

    protected FundsFrozenOrder frozenOrderByBusinessSn(String businessSn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn));
        FundsFrozenOrder result = fundsFrozenOrderMapper.selectOneByQuery(wrapper);
        assertThat(result).as("frozen order for businessSn %s", businessSn).isNotNull();
        return result;
    }

    protected static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    protected static FundsAccountId cashMappingAccount() {
        return platformAccount(PlatformFundingAccountRole.CASH_MAPPING);
    }

    protected static FundsAccountId prepaymentAccount() {
        return platformAccount(PlatformFundingAccountRole.PREPAYMENT);
    }

    private static FundsAccountId platformAccount(PlatformFundingAccountRole role) {
        return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    private Optional<LedgerDTO> findLedger(FundsAccountId accountId, LedgerSubjectCode subjectCode) {
        return findLedgers(accountId).stream()
                .filter(ledger -> ledger.getLedgerSubjectCode() == subjectCode)
                .findFirst();
    }

    private List<LedgerDTO> findLedgers(FundsAccountId accountId) {
        LedgerQuery query = new LedgerQuery()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setCurrency(CURRENCY)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name());
        return ledgerService.queryLedgers(query, DefaultPageQueryOptions.defaults(MAX_LEDGER_BUCKET_SIZE))
                .getRecords()
                .stream()
                .toList();
    }

    private Map<LedgerSubjectCode, LedgerBalanceBucket> balanceBuckets(FundsAccountId accountId) {
        Map<LedgerSubjectCode, LedgerBalanceBucket> result = new EnumMap<>(LedgerSubjectCode.class);
        findLedgers(accountId).forEach(ledger -> result.put(ledger.getLedgerSubjectCode(),
                LedgerBalanceBucket.builder()
                        .accountCode(ledger.getLedgerSubjectCode())
                        .balance(Money.immutable(ledger.getNormalBalance(), CURRENCY))
                        .periodType(ledger.getPeriodType())
                        .periodId(ledger.getPeriodId())
                        .activeTime(ACTIVE_TIME)
                        .build()));
        return result;
    }

    private void assertValidPostedTransaction(LedgerTransaction transaction) {
        assertThat(transaction.getFundsTransactionSn()).isNotBlank();
        assertThat(transaction.getBalanced()).isTrue();
        assertThat(transaction.getDebitAmount()).isEqualTo(transaction.getCreditAmount());
        assertThat(postingPlansOf(transaction))
                .isNotEmpty()
                .allSatisfy(plan -> {
                    assertThat(plan.getBalanced()).isTrue();
                    assertThat(plan.getDebitAmount()).isEqualTo(plan.getCreditAmount());
                });
        assertThat(entriesOf(transaction)).isNotEmpty();
    }

    private static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    @Configuration
    @Import({
            FundsDirectTransactionInstructionConverter.class,
            FundsBalanceControlInstructionConverter.class,
            FundsAuthorizationInstructionConverter.class,
            RouteParticipantFactory.class,
            RouteSubjectSupport.class,
            PlatformAccountRouteSupport.class,
            DefaultRouteReplayService.class,
            TransferFundsInstructionRouteResolver.class,
            BalanceControlFundsInstructionRouteResolver.class,
            AuthorizationFundsInstructionRouteResolver.class,
            CompositeRouteResolver.class,
            DefaultRouteSnapshotFactory.class,
            DefaultLedgerPostingAssembler.class,
            DefaultRoutedFundsInstructionOrchestrator.class,
            FundsTransactionCommandServiceImpl.class,
            LedgerServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class,
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultFundsInstructionLifecycleSaver.class,
            DefaultFundsFrozenOrderLifecycleSaver.class,
            DelegatingFundsInstructionLifecycleRecorder.class,
            DefaultFundsTransactionQueryService.class
    })
    static class Config {

        @Bean
        PlatformFundingAccountService platformFundingAccountService() {
            return new PlatformFundingAccountService() {
                @Override
                public FundsAccountId requireAccountId(CurrencyIsoCode currency, PlatformFundingAccountRole role) {
                    return requireAccountId(TENANT_ID, currency, role);
                }

                @Override
                public FundsAccountId requireAccountId(Long tenantId,
                                                       CurrencyIsoCode currency,
                                                       PlatformFundingAccountRole role) {
                    return platformAccount(role);
                }
            };
        }

        @Bean
        @Primary
        FundsAccountQueryService fundsAccountQueryService(LedgerMapper ledgerMapper) {
            return new LedgerBackedFundsAccountQueryService(ledgerMapper);
        }
    }

    private static final class LedgerBackedFundsAccountQueryService implements FundsAccountQueryService {

        private final LedgerMapper ledgerMapper;

        private LedgerBackedFundsAccountQueryService(LedgerMapper ledgerMapper) {
            this.ledgerMapper = ledgerMapper;
        }

        @Override
        public @NonNull FundsAccount getAccount(@NonNull FundsAccountId accountId) {
            Map<LedgerSubjectCode, Long> ledgerIds = findLedgers(accountId).stream()
                    .collect(Collectors.toMap(
                            LedgerDTO::getLedgerSubjectCode,
                            LedgerDTO::getId,
                            (left, right) -> left,
                            () -> new EnumMap<>(LedgerSubjectCode.class)));
            return ImmutableFundsAccount.builder()
                    .id(1L)
                    .tenantId(TENANT_ID)
                    .accountId(accountId)
                    .owner(FundsAccountOwner.of("owner_" + accountId.id(), FundsAccountOwnerType.USER))
                    .status(FundsAccountStatus.ACTIVE)
                    .currency(CURRENCY)
                    .accountLedgerIds(ledgerIds)
                    .version(0)
                    .build();
        }

        @Override
        public @NonNull FundsAccountBalanceView getBalance(@NonNull FundsAccountId accountId) {
            return ImmutableFundsBalanceView.builder()
                    .id(1L)
                    .tenantId(TENANT_ID)
                    .accountId(accountId)
                    .currency(CURRENCY)
                    .balanceBuckets(balanceBuckets(accountId))
                    .build();
        }

        @Override
        public boolean supports(@NonNull FundsAccountId accountId) {
            return true;
        }

        private List<LedgerDTO> findLedgers(FundsAccountId accountId) {
            LedgerNameRefs ref = LedgerNameRefs.ledger;
            QueryWrapper wrapper = QueryWrapper.create().from(ref)
                    .where(ref.tenantId.eq(TENANT_ID))
                    .and(ref.subjectId.eq(accountId.id()))
                    .and(ref.subjectType.eq(accountId.type()))
                    .and(ref.currency.eq(CURRENCY))
                    .and(ref.periodType.eq(AccountBalancePeriodType.LIFETIME))
                    .and(ref.periodId.eq(AccountBalancePeriodType.LIFETIME.name()))
                    .orderBy(ref.id.asc());
            return ledgerMapper.selectListByQuery(wrapper).stream()
                    .map(toLedgerDTO())
                    .toList();
        }

        private Map<LedgerSubjectCode, LedgerBalanceBucket> balanceBuckets(FundsAccountId accountId) {
            Map<LedgerSubjectCode, LedgerBalanceBucket> result = new EnumMap<>(LedgerSubjectCode.class);
            findLedgers(accountId).forEach(ledger -> result.put(ledger.getLedgerSubjectCode(),
                    LedgerBalanceBucket.builder()
                            .accountCode(ledger.getLedgerSubjectCode())
                            .balance(Money.immutable(ledger.getNormalBalance(), CURRENCY))
                            .periodType(ledger.getPeriodType())
                            .periodId(ledger.getPeriodId())
                            .activeTime(ACTIVE_TIME)
                            .build()));
            return result;
        }

        private Function<com.capte.funds.ledger.dal.entities.Ledger, LedgerDTO> toLedgerDTO() {
            return ledger -> new LedgerDTO()
                    .setId(ledger.getId())
                    .setGmtCreate(ledger.getGmtCreate())
                    .setGmtModified(ledger.getGmtModified())
                    .setTenantId(ledger.getTenantId())
                    .setSubjectId(ledger.getSubjectId())
                    .setSubjectType(ledger.getSubjectType())
                    .setLedgerProfileCode(ledger.getLedgerProfileCode())
                    .setLedgerProfileVersion(ledger.getLedgerProfileVersion())
                    .setLedgerSubjectCode(ledger.getLedgerSubjectCode())
                    .setLedgerSubjectCategory(ledger.getLedgerSubjectCategory())
                    .setNormalBalanceSide(ledger.getNormalBalanceSide())
                    .setAllowNegative(ledger.getAllowNegative())
                    .setDebitAmount(ledger.getDebitAmount())
                    .setCreditAmount(ledger.getCreditAmount())
                    .setCurrency(ledger.getCurrency())
                    .setSettlementPolicy(ledger.getSettlementPolicy())
                    .setCutOffTime(ledger.getCutOffTime())
                    .setPeriodType(ledger.getPeriodType())
                    .setPeriodId(ledger.getPeriodId())
                    .setVersion(ledger.getVersion());
        }
    }
}

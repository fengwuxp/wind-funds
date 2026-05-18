package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.ledger.DefaultLedgerPostingAssembler;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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

/**
 * 资金交易流程测试基座。
 *
 * <p>该基座通过 Spring 注入 command service、converter、route resolver、orchestrator 和 posting
 * assembler，只把外部持久化边界替换成内存账本与生命周期记录器，保证业务流程用例走真实内部链路。</p>
 */
@SpringJUnitConfig(FundsTransactionFlowTestSupport.Config.class)
abstract class FundsTransactionFlowTestSupport {

    protected static final Long TENANT_ID = 1L;

    protected static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final LocalDateTime ACTIVE_TIME = LocalDateTime.of(2026, 5, 18, 0, 0);

    @Autowired
    protected FundsTransactionCommandServiceImpl service;

    @Autowired
    protected InMemoryLedgerBook ledgerBook;

    @Autowired
    protected RecordingLifecycleRecorder lifecycleRecorder;

    @BeforeEach
    void setUp() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        ledgerBook.reset();
        lifecycleRecorder.reset();
        seedLedgers();
    }

    @AfterEach
    void tearDown() {
        ThreadContextTenantIdHolder.remove();
    }

    private void seedLedgers() {
        FundsAccountId user = fundingAccount("funding_user");
        ledgerBook.ensureLedger(user, LedgerSubjectCode.AVAILABLE, 0L);
        ledgerBook.ensureLedger(user, LedgerSubjectCode.FROZEN, 0L);
        ledgerBook.ensureLedger(cashMappingAccount(), LedgerSubjectCode.CASH, 10_000L);
        ledgerBook.ensureLedger(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L);
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

    protected List<FundsSubjectBalanceDTO> balances(FundsAccountId... accountIds) {
        return List.of(accountIds).stream().map(ledgerBook::balance).toList();
    }

    protected void assertPostedTransactions(int expectedSize) {
        assertThat(ledgerBook.postedTransactions).hasSize(expectedSize);
        ledgerBook.postedTransactions.forEach(FundsTransactionFlowTestSupport::assertValidPostedTransaction);
        assertThat(lifecycleRecorder.succeededLedgerTransactionSns).hasSize(expectedSize);
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

    private static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    private static void assertValidPostedTransaction(LedgerTransactionSpec transaction) {
        assertPostingBalanced(transaction);
        assertThat(transaction.getFundsTransactionSn()).isNotBlank();
    }

    protected static final class InMemoryLedgerBook implements LedgerService, LedgerTransactionPostingService {

        private final Map<LedgerKey, LedgerDTO> ledgers = new LinkedHashMap<>();

        private final Map<Long, LedgerDTO> ledgersById = new LinkedHashMap<>();

        final List<LedgerTransactionSpec> postedTransactions = new ArrayList<>();

        private long nextLedgerId = 1L;

        private void reset() {
            ledgers.clear();
            ledgersById.clear();
            postedTransactions.clear();
            nextLedgerId = 1L;
        }

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
                    .setAllowNegative(false)
                    .setDebitAmount(initialBalance < 0 ? -initialBalance : 0L)
                    .setCreditAmount(initialBalance > 0 ? initialBalance : 0L)
                    .setCurrency(CURRENCY)
                    .setSettlementPolicy("RT")
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
        public void post(@NonNull LedgerTransactionSpec transaction) {
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

    protected static final class RecordingLifecycleRecorder implements FundsInstructionLifecycleRecorder,
            FundsTransactionQueryService {

        private final AtomicInteger transactionSequence = new AtomicInteger();

        final List<String> succeededLedgerTransactionSns = new ArrayList<>();

        private final Map<String, RouteSnapshotSpec> routeSnapshots = new LinkedHashMap<>();

        private final Map<String, RouteSnapshotSpec> freezeOrderSnapshots = new LinkedHashMap<>();

        private final List<ConsumedReplayLeg> consumedReplayLegs = new ArrayList<>();

        private void reset() {
            transactionSequence.set(0);
            succeededLedgerTransactionSns.clear();
            routeSnapshots.clear();
            freezeOrderSnapshots.clear();
            consumedReplayLegs.clear();
        }

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
            if (instruction.getEventType() == FundsTransactionEventType.FREEZE) {
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
            long amount = consumedReplayLegs.stream()
                    .filter(leg -> replayRefLegId.equals(leg.replayRefLegId()))
                    .filter(leg -> leg.eventType() == eventType)
                    .filter(leg -> leg.currency() == currency)
                    .mapToLong(ConsumedReplayLeg::amount)
                    .sum();
            return Money.immutable(amount, currency);
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            return Optional.ofNullable(routeSnapshots.get(transactionSn));
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.ofNullable(freezeOrderSnapshots.get(freezeOrderSn));
        }

        private void recordConsumedReplayLegs(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
            routeSnapshot.getLegs().stream()
                    .filter(leg -> leg.getReplayRefLegId() != null && !leg.getReplayRefLegId().isBlank())
                    .map(leg -> new ConsumedReplayLeg(instruction.getEventType(), leg.getReplayRefLegId(),
                            leg.getAmount().getAmount(), leg.getAmount().getCurrency()))
                    .forEach(consumedReplayLegs::add);
        }
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
            CompositeRouteResolver.class,
            DefaultRouteSnapshotFactory.class,
            DefaultLedgerPostingAssembler.class,
            DefaultRoutedFundsInstructionOrchestrator.class,
            FundsTransactionCommandServiceImpl.class
    })
    static class Config {

        @Bean
        InMemoryLedgerBook ledgerBook() {
            return new InMemoryLedgerBook();
        }

        @Bean
        @Primary
        RecordingLifecycleRecorder lifecycleRecorder() {
            return new RecordingLifecycleRecorder();
        }

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
        FundsAccountQueryService fundsAccountQueryService() {
            return new FundsAccountQueryService() {
                @Override
                public @NonNull FundsAccount getAccount(@NonNull FundsAccountId accountId) {
                    return new TestFundsAccount(accountId);
                }

                @Override
                public @NonNull FundsAccountBalanceView getBalance(@NonNull FundsAccountId accountId) {
                    throw new UnsupportedOperationException("balance is not required by flow converters");
                }

                @Override
                public boolean supports(@NonNull FundsAccountId accountId) {
                    return true;
                }
            };
        }
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

    private record ConsumedReplayLeg(FundsTransactionEventType eventType,
                                     String replayRefLegId,
                                     long amount,
                                     CurrencyIsoCode currency) {
    }

    private record TestFundsAccount(FundsAccountId accountId) implements FundsAccount {

        @Override
        public Long getId() {
            return 1L;
        }

        @Override
        public FundsAccountId getAccountId() {
            return accountId;
        }

        @Override
        public FundsAccountOwner getOwner() {
            return FundsAccountOwner.of("owner_001", FundsAccountOwnerType.USER);
        }

        @Override
        public FundsAccountStatus getStatus() {
            return FundsAccountStatus.ACTIVE;
        }

        @Override
        public Map<LedgerSubjectCode, Long> getAccountLedgerIds() {
            return Map.of();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public CurrencyIsoCode getCurrency() {
            return CURRENCY;
        }

        @Override
        public Integer getVersion() {
            return 0;
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
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

package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
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
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.support.FundsTransactionTestSupport;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金交易编排流程测试基类。
 *
 * <p>测试通过最小 Spring 上下文注入 command service、converter、route resolver 和 orchestrator；
 * Recording 组件仅用于捕获 route、lifecycle、posting 边界输出，避免替代被测内部编排链路。</p>
 */
@SpringJUnitConfig(FundsTransactionOrchestrationFlowTestSupport.Config.class)
abstract class FundsTransactionOrchestrationFlowTestSupport {
    protected static final Long TENANT_ID = 1L;

    protected static final com.wind.transaction.core.enums.CurrencyIsoCode CURRENCY =
            com.wind.transaction.core.enums.CurrencyIsoCode.USD;

    @Autowired
    protected RecordingRouteResolver routeResolver;

    @Autowired
    protected RecordingLifecycleSaver lifecycleSaver;

    @Autowired
    protected RecordingLedgerPostingAssembler postingAssembler;

    @Autowired
    protected RecordingPostingService postingService;

    @Autowired
    protected RecordingTransactionQueryService transactionQueryService;

    @Autowired
    protected FundsAuthorizationInstructionConverter authorizationInstructionConverter;

    @Autowired
    protected FundsTransactionCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        routeResolver.reset();
        lifecycleSaver.reset();
        postingAssembler.reset();
        postingService.reset();
        transactionQueryService.reset();
    }

    @AfterEach
    void tearDown() {
        ThreadContextTenantIdHolder.remove();
    }

    protected RouteSnapshotSpec originalSettlementSnapshot() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalAuthorizationSnapshot());
        FundsInstructionSpec settleInstruction = authorizationInstructionConverter.convertToSettleInstruction(
                new FundsAuthorizationTransactionSettleRequest()
                        .setAccountId(creditAccount("credit_001"))
                        .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                        .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                        .setBusinessScene("CARD_SETTLE")
                        .setBusinessSn("SETTLE_0001")
                        .setDescription("settle"),
                WindOperator.system());
        ResolvedRouteSpec route = routeResolver.delegate.resolve(settleInstruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    protected RouteSnapshotSpec originalFreezeSnapshot() {
        FundsInstructionSpec freezeInstruction = new FundsBalanceControlInstructionConverter(
                FundsRouteTestSupport.accountQueryService(CURRENCY))
                .convertToFreezeInstruction(new com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest()
                        .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                        .setAmount(amount(100L))
                        .setBusinessScene("FREEZE")
                        .setBusinessSn("FREEZE_0001")
                        .setDescription("freeze"), WindOperator.system());
        ResolvedRouteSpec route = routeResolver.delegate.resolve(freezeInstruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    protected RouteSnapshotSpec originalAuthorizationSnapshot() {
        FundsInstructionSpec authorizeInstruction = authorizationInstructionConverter.convertToAuthorizeInstruction(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(creditAccount("credit_001"))
                        .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                        .setApproved(Boolean.TRUE)
                        .setBusinessScene("CARD_AUTH")
                        .setBusinessSn("AUTH_0001")
                        .setDescription("authorize"),
                WindOperator.system());
        ResolvedRouteSpec route = routeResolver.delegate.resolve(authorizeInstruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    protected static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    protected static com.wind.transaction.core.Money amount(long value) {
        return com.wind.transaction.core.Money.immutable(value, CURRENCY);
    }

    protected static final class RecordingRouteResolver implements RouteResolver {

        private final RouteResolver delegate;

        final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        RecordingRouteResolver(RouteResolver delegate) {
            this.delegate = delegate;
        }

        private void reset() {
            instruction.set(null);
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return delegate.supports(instruction);
        }

        @Override
        public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
            this.instruction.set(instruction);
            return delegate.resolve(instruction);
        }
    }

    protected static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver {

        final AtomicReference<ResolvedRouteSpec> beforePostingRoute = new AtomicReference<>();

        final AtomicReference<String> succeededLedgerTransactionSn = new AtomicReference<>();

        private void reset() {
            beforePostingRoute.set(null);
            succeededLedgerTransactionSn.set(null);
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                      @NonNull ResolvedRouteSpec resolvedRoute,
                                                                      @NonNull RouteSnapshotSpec routeSnapshot) {
            beforePostingRoute.set(resolvedRoute);
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn("FT_001")
                    .setTransactionDetailSns(List.of("FTD_001"))
                    .setCompleted(false);
        }

        @Override
        public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                                  @NonNull FundsInstructionLifecycleResult result,
                                  @Nullable String ledgerTransactionSn) {
            succeededLedgerTransactionSn.set(ledgerTransactionSn);
        }

        @Override
        public void markFailed(@NonNull FundsInstructionSpec instruction,
                               @NonNull FundsInstructionLifecycleResult result,
                               @NonNull Throwable cause) {
            throw new AssertionError("unexpected failure", cause);
        }
    }

    protected static final class RecordingLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec> {

        final AtomicReference<ResolvedRouteSpec> route = new AtomicReference<>();

        private void reset() {
            route.set(null);
        }

        @Override
        public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                       @NonNull String fundsTransactionSn,
                                                       @NonNull ResolvedRouteSpec resolvedRoute) {
            route.set(resolvedRoute);
            return LedgerTransactionSpecFactory.createLedgerTransaction(instruction, fundsTransactionSn,
                    ledgerTransactionSn -> {
                        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(
                                LedgerPhaseCode.TRANSFER,
                                List.of(entry(ledgerTransactionSn, EntrySide.DEBIT),
                                        entry(ledgerTransactionSn, EntrySide.CREDIT))
                        );
                        return List.of(LedgerTransactionSpecFactory.postingPlan(
                                LedgerPostingIntentType.TRANSFER,
                                ledgerTransactionSn,
                                null,
                                LedgerBalanceEffectType.CONSUME,
                                List.of(phase)
                        ));
                    });
        }

        @Override
        public boolean supports(@NonNull ResolvedRouteSpec resolvedRoute) {
            return true;
        }
    }

    private static LedgerEntrySpec entry(String ledgerTransactionSn, EntrySide entrySide) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                entrySide == EntrySide.DEBIT ? "funding_001" : "funding_002",
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                ledgerTransactionSn,
                "TRANSFER",
                "BIZ_0001",
                100L,
                CURRENCY,
                LocalDateTime.of(2026, 5, 9, 12, 0)
        ).setBalanceEffectType(LedgerBalanceEffectType.CONSUME)
                .setIntent(LedgerPostingIntentType.TRANSFER)
                .setPhaseCode(LedgerPhaseCode.TRANSFER)
                .setContextVariables(Map.of());
    }

    protected static final class RecordingPostingService implements LedgerTransactionPostingService {

        final AtomicReference<LedgerTransactionSpec> transaction = new AtomicReference<>();

        private void reset() {
            transaction.set(null);
        }

        @Override
        public void post(LedgerTransactionSpec transaction) {
            this.transaction.set(transaction);
        }
    }

    protected static final class RecordingTransactionQueryService implements FundsTransactionQueryService {

        final Map<String, RouteSnapshotSpec> routeSnapshots = new ConcurrentHashMap<>();

        final Map<String, RouteSnapshotSpec> freezeOrderSnapshots = new ConcurrentHashMap<>();

        private void reset() {
            routeSnapshots.clear();
            freezeOrderSnapshots.clear();
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
            return Money.immutable(0L, currency);
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
            DefaultRouteSnapshotFactory.class,
            DefaultRoutedFundsInstructionOrchestrator.class,
            FundsTransactionCommandServiceImpl.class
    })
    static class Config {

        @Bean
        RecordingTransactionQueryService transactionQueryService() {
            return new RecordingTransactionQueryService();
        }

        @Bean
        CompositeRouteResolver delegateRouteResolver(DefaultRouteReplayService replayService,
                                                     TransferFundsInstructionRouteResolver transferResolver,
                                                     BalanceControlFundsInstructionRouteResolver balanceResolver,
                                                     AuthorizationFundsInstructionRouteResolver authorizationResolver) {
            return new CompositeRouteResolver(List.of(replayService, transferResolver, balanceResolver,
                    authorizationResolver));
        }

        @Bean
        @Primary
        RecordingRouteResolver routeResolver(CompositeRouteResolver delegate) {
            return new RecordingRouteResolver(delegate);
        }

        @Bean
        @Primary
        RecordingLifecycleSaver lifecycleSaver() {
            return new RecordingLifecycleSaver();
        }

        @Bean
        @Primary
        RecordingLedgerPostingAssembler postingAssembler() {
            return new RecordingLedgerPostingAssembler();
        }

        @Bean
        @Primary
        RecordingPostingService postingService() {
            return new RecordingPostingService();
        }

        @Bean
        PlatformFundingAccountService platformFundingAccountService() {
            return new PlatformFundingAccountService() {
                @Override
                public FundsAccountId requireAccountId(com.wind.transaction.core.enums.CurrencyIsoCode currency,
                                                       PlatformFundingAccountRole role) {
                    return requireAccountId(TENANT_ID, currency, role);
                }

                @Override
                public FundsAccountId requireAccountId(Long tenantId,
                                                       com.wind.transaction.core.enums.CurrencyIsoCode currency,
                                                       PlatformFundingAccountRole role) {
                    return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                            FundsSubjectType.FUNDING_ACCOUNT);
                }
            };
        }

        @Bean
        FundsAccountQueryService fundsAccountQueryService() {
            return FundsRouteTestSupport.accountQueryService(CURRENCY);
        }
    }
}

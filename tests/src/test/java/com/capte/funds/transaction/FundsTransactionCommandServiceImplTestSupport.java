package com.capte.funds.transaction;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.FundsInstructionOrchestrator;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

abstract class FundsTransactionCommandServiceImplTestSupport {

    protected static final Long TENANT_ID = 1L;

    protected static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    protected RecordingOrchestrator orchestrator;

    protected FundsTransactionCommandServiceImpl service;

    protected RouteResolver routeResolver;

    protected RecordingTransactionQueryService transactionQueryService;

    protected FundsAuthorizationInstructionConverter authorizationInstructionConverter;

    protected FundsBalanceControlInstructionConverter balanceControlInstructionConverter;

    @BeforeEach
    void setUp() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        orchestrator = new RecordingOrchestrator();
        PlatformFundingAccountService platformFundingAccountService = platformFundingAccountService();
        RouteSubjectSupport routeSubjectSupport = new RouteSubjectSupport();
        PlatformAccountRouteSupport platformAccountRouteSupport = new PlatformAccountRouteSupport(platformFundingAccountService);
        transactionQueryService = new RecordingTransactionQueryService();
        authorizationInstructionConverter = new FundsAuthorizationInstructionConverter(accountQueryService(CURRENCY));
        balanceControlInstructionConverter = new FundsBalanceControlInstructionConverter(accountQueryService(CURRENCY));
        service = new FundsTransactionCommandServiceImpl(
                new FundsDirectTransactionInstructionConverter(platformFundingAccountService, accountQueryService(CURRENCY)),
                balanceControlInstructionConverter,
                authorizationInstructionConverter,
                orchestrator);
        RouteParticipantFactory routeParticipantFactory = new RouteParticipantFactory();
        routeResolver = new CompositeRouteResolver(List.of(
                new DefaultRouteReplayService(transactionQueryService),
                new TransferFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new BalanceControlFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new AuthorizationFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport)
        ));
    }

    @AfterEach
    void tearDown() {
        ThreadContextTenantIdHolder.remove();
    }

    protected FundsInstructionSpec instruction() {
        return orchestrator.instruction.get();
    }

    protected ResolvedRouteSpec route() {
        return routeResolver.resolve(instruction());
    }

    protected RouteSnapshotSpec originalAuthorizationSnapshot() {
        FundsInstructionSpec authorizeInstruction = authorizationInstructionConverter.convertToAuthorizeInstruction(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(creditAccount("credit_001"))
                        .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                        .setApproved(Boolean.TRUE)
                        .setBusinessScene("CARD_AUTH")
                        .setBusinessSn("AUTH_00000001")
                        .setDescription("auth"),
                WindOperator.system());
        return new DefaultRouteSnapshotFactory().createSnapshot(routeResolver.resolve(authorizeInstruction));
    }

    protected RouteSnapshotSpec originalSettlementSnapshot() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalAuthorizationSnapshot());
        FundsInstructionSpec settleInstruction = authorizationInstructionConverter.convertToSettleInstruction(
                new FundsAuthorizationTransactionSettleRequest()
                        .setAccountId(creditAccount("credit_001"))
                        .setTransactionAmount(TransactionAmount.sameCurrency(amount(200L)))
                        .setAuthorizationTransactionSn("AUTH_TX_00000001")
                        .setBusinessScene("CARD_SETTLE")
                        .setBusinessSn("SETTLE_00000001")
                        .setDescription("settle"),
                WindOperator.system());
        return new DefaultRouteSnapshotFactory().createSnapshot(routeResolver.resolve(settleInstruction));
    }

    protected RouteSnapshotSpec originalFreezeSnapshot() {
        FundsInstructionSpec freezeInstruction = balanceControlInstructionConverter.convertToFreezeInstruction(
                new FundsBalanceFreezeRequest()
                        .setAccountId(fundingAccount("funding_001"))
                        .setAmount(amount(200L))
                        .setBusinessScene("FREEZE")
                        .setBusinessSn("FREEZE_00000001")
                        .setDescription("freeze"),
                WindOperator.system());
        return new DefaultRouteSnapshotFactory().createSnapshot(routeResolver.resolve(freezeInstruction));
    }

    protected static void assertLeg(RouteLegSpec leg,
                                    RouteLegType legType,
                                    LedgerSubjectCode fromCode,
                                    LedgerSubjectCode toCode,
                                    LedgerBalanceEffectType effectType,
                                    LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(fromCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(toCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(effectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    protected static void assertNoLimitNodes(ResolvedRouteSpec route) {
        assertThat(route.getLegs())
                .allSatisfy(leg -> assertThat(LedgerSubjectCode.LIMIT)
                        .isNotIn(leg.getSourceNode().getLedgerSubjectCode(),
                                leg.getTargetNode().getLedgerSubjectCode()));
    }

    protected static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    protected static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    protected static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    protected static FundsBalanceAdjustRequest adjustRequest(FundsAccountId accountId,
                                                             long amount,
                                                             Boolean increase,
                                                             String businessScene,
                                                             String businessSn) {
        return new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setIncrease(increase)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setAdjustReason("adjust reason")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn);
    }

    protected static WritableContextVariables budgetGovernanceContext() {
        return new SimpleContextVariables()
                .putVariable(FundsInstructionContextKeys.BUDGET_PERIOD_ID, "BUDGET_2026_M05")
                .putVariable(FundsInstructionContextKeys.BUDGET_GOVERNANCE_POLICY_CODE,
                        "BUDGET_OVERUSE_GOVERNANCE")
                .putVariable(FundsInstructionContextKeys.BUDGET_REPORT_MARKER, "BUDGET_REPORT_2026_M05");
    }

    protected static String constraintKey(FundsAccountId accountId, LedgerSubjectCode subjectCode) {
        return accountId.type() + ":" + accountId.id() + ":" + subjectCode.name();
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
                return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                        FundsSubjectType.FUNDING_ACCOUNT);
            }
        };
    }

    private static FundsAccountQueryService accountQueryService(CurrencyIsoCode currency) {
        return new FundsAccountQueryService() {
            @Override
            public @org.jspecify.annotations.NonNull FundsAccount getAccount(
                    @org.jspecify.annotations.NonNull FundsAccountId accountId) {
                return new TestFundsAccount(accountId, currency);
            }

            @Override
            public @org.jspecify.annotations.NonNull FundsAccountBalanceView getBalance(
                    @org.jspecify.annotations.NonNull FundsAccountId accountId) {
                throw new UnsupportedOperationException("balance is not required by this test");
            }

            @Override
            public boolean supports(@org.jspecify.annotations.NonNull FundsAccountId accountId) {
                return true;
            }
        };
    }

    private record TestFundsAccount(FundsAccountId accountId,
                                    CurrencyIsoCode currency) implements FundsAccount {

        @Override
        public @org.jspecify.annotations.NonNull Long getId() {
            return 1L;
        }

        @Override
        public @org.jspecify.annotations.NonNull FundsAccountId getAccountId() {
            return accountId;
        }

        @Override
        public @org.jspecify.annotations.NonNull FundsAccountOwner getOwner() {
            return FundsAccountOwner.of("owner_001", FundsAccountOwnerType.USER);
        }

        @Override
        public @org.jspecify.annotations.NonNull FundsAccountStatus getStatus() {
            return FundsAccountStatus.ACTIVE;
        }

        @Override
        public @org.jspecify.annotations.NonNull Map<LedgerSubjectCode, Long> getAccountLedgerIds() {
            return Map.of();
        }

        @Override
        public CurrencyIsoCode getCurrency() {
            return currency;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public @org.jspecify.annotations.NonNull Integer getVersion() {
            return 0;
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }
    }

    private static final class SimpleContextVariables implements WritableContextVariables {

        private final Map<String, Object> variables = new HashMap<>();

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

    protected static final class RecordingTransactionQueryService implements FundsTransactionQueryService {

        protected final Map<String, RouteSnapshotSpec> routeSnapshots = new ConcurrentHashMap<>();

        protected final Map<String, RouteSnapshotSpec> freezeOrderSnapshots = new ConcurrentHashMap<>();

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
            return Optional.ofNullable(freezeOrderSnapshots.get(freezeOrderSn));
        }
    }

    protected static final class RecordingOrchestrator implements FundsInstructionOrchestrator<FundsInstructionSpec> {

        private final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        @Override
        public String execute(FundsInstructionSpec spec) {
            instruction.set(spec);
            return "FT_CAPTURED";
        }

        @Override
        public boolean supports(Class<FundsInstructionSpec> specType) {
            return FundsInstructionSpec.class.isAssignableFrom(specType);
        }
    }
}

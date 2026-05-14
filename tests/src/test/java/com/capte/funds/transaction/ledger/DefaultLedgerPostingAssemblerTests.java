package com.capte.funds.transaction.ledger;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerPostingAssemblerTests {

    private static final Long TENANT_ID = 1L;

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    @Test
    void assembleShouldCreateBalancedPlan() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001", route(Map.of()));
        List<LedgerPostingPlanSpec> plans = transaction.getPostingPlans();

        assertThat(transaction.getFundsTransactionSn()).isEqualTo("FT_001");
        assertThat(plans).hasSize(1);
        LedgerPostingPlanSpec plan = plans.getFirst();
        assertThat(plan.isBalanced()).isTrue();
        assertThat(plan.getRouteLegId()).isEqualTo("LEG_001");
        assertThat(plan.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
        LedgerPostingPhaseSpec phase = plan.getPostingPhases().getFirst();
        assertThat(phase.getPhaseCode()).isEqualTo(LedgerPhaseCode.TRANSFER);
        assertThat(phase.getEntries()).hasSize(2);
        LedgerEntrySpec sourceEntry = phase.getEntries().get(0);
        LedgerEntrySpec targetEntry = phase.getEntries().get(1);
        assertThat(sourceEntry.getLedgerId()).isEqualTo(101L);
        assertThat(sourceEntry.getEntryType()).isEqualTo(EntrySide.DEBIT);
        assertThat(targetEntry.getLedgerId()).isEqualTo(102L);
        assertThat(targetEntry.getEntryType()).isEqualTo(EntrySide.CREDIT);
    }

    @Test
    void assembleShouldResolvePostingScopeBeforeLedgerPersistence() {
        DefaultLedgerPostingAssembler directAssembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));
        DefaultLedgerPostingAssembler authorizationAssembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(201L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(202L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(301L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(302L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT)
        )));
        DefaultLedgerPostingAssembler feeAssembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.FEE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        assertPostingScope(directAssembler.assemble(instruction(), "FT_001", route(Map.of())),
                LedgerPostingScope.BETWEEN_SUBJECTS);
        assertPostingScope(authorizationAssembler.assemble(instruction(), "FT_002", sharedCardRoute()),
                LedgerPostingScope.CONTROL_HOLD);
        assertPostingScope(feeAssembler.assemble(instruction(), "FT_003", feeRefundRoute()),
                LedgerPostingScope.FEE);
    }

    @Test
    void assembleShouldUseMostSpecificConstraintOverride() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));
        Map<String, LedgerBalanceConstraintType> overrides = Map.of(
                "FUNDING_ACCOUNT:funding_001:AVAILABLE", LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE,
                "AVAILABLE", LedgerBalanceConstraintType.PROFILE_DEFAULT
        );

        LedgerPostingPhaseSpec phase = assembler.assemble(instruction(), "FT_001", route(overrides))
                .getPostingPlans().getFirst().getPostingPhases().getFirst();

        assertThat(phase.getEntries().get(0).getBalanceConstraintType())
                .isEqualTo(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
        assertThat(phase.getEntries().get(1).getBalanceConstraintType())
                .isEqualTo(LedgerBalanceConstraintType.PROFILE_DEFAULT);
    }

    @Test
    void assembleShouldRejectMissingLedger() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.of()));

        assertThatThrownBy(() -> assembler.assemble(instruction(), "FT_001", route(Map.of())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本不存在或不唯一");
    }

    @Test
    void testAssembleShouldRejectLedgerCurrencyMismatch() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, CurrencyIsoCode.EUR)
        )));

        assertThatThrownBy(() -> assembler.assemble(instruction(), "FT_001", route(Map.of())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本币种与路径金额币种不一致");
    }

    /**
     * 场景：RouteLeg 未显式提供 periodId，但账期类型为默认生命周期账期。
     * 输入：`periodType=LIFETIME` 且 `periodId=null` 的路径。
     * 输出：组装得到的账本分录 ledgerId 列表。
     * 预期：Assembler 自动补齐 `LIFETIME` 账期标识，并正确命中生命周期账本。
     */
    @Test
    void testAssembleShouldDefaultLifetimePeriodIdWhenLegDoesNotProvideIt() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001",
                route(new MissingPeriodIdRouteLeg()));

        LedgerPostingPhaseSpec phase = transaction.getPostingPlans().getFirst().getPostingPhases().getFirst();
        assertThat(phase.getEntries())
                .extracting(LedgerEntrySpec::getLedgerId)
                .containsExactly(101L, 102L);
    }

    /**
     * 场景：同主体同科目存在多个不同账期的账本 bucket。
     * 输入：月账期路径，目标 periodId 为 `2026-05`。
     * 输出：组装得到的账本分录 ledgerId 列表。
     * 预期：Assembler 只能命中同周期账本，不得串到 `2026-04` 等其他 period bucket。
     */
    @Test
    void testAssembleShouldUsePeriodKeyForLedgerLookup() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-05"),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-05"),
                ledger(201L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-04"),
                ledger(202L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-04")
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001",
                route(new MonthlyRouteLeg("2026-05")));

        LedgerPostingPhaseSpec phase = transaction.getPostingPlans().getFirst().getPostingPhases().getFirst();
        assertThat(phase.getEntries())
                .extracting(LedgerEntrySpec::getLedgerId)
                .containsExactly(101L, 102L)
                .doesNotContain(201L, 202L);
    }

    @Test
    void assembleShouldCreateIndependentPlansForSharedCardSubjects() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "credit_001", FundsSubjectType.CREDIT_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(201L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(202L, "budget_001", FundsSubjectType.BUDGET_GROUP, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT),
                ledger(301L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(302L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT, LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001", sharedCardRoute());
        List<LedgerPostingPlanSpec> plans = transaction.getPostingPlans();

        assertThat(plans).hasSize(3);
        assertThat(plans).allSatisfy(plan -> {
            assertThat(plan.isBalanced()).isTrue();
            assertThat(plan.getPostingPhases()).hasSize(1);
            assertThat(plan.getPostingPhases().getFirst().getEntries()).hasSize(2);
        });
        assertThat(plans)
                .flatExtracting(LedgerPostingPlanSpec::getEntries)
                .extracting(LedgerEntrySpec::getLedgerId)
                .containsExactly(101L, 102L, 201L, 202L, 301L, 302L);
    }

    @Test
    void assembleShouldUseFeeRefundIntentForFeeRefundEvent() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.FEE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001", feeRefundRoute());
        LedgerPostingPlanSpec plan = transaction.getPostingPlans().getFirst();

        assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.FEE_REFUND);
        assertThat(plan.getEntries())
                .extracting(LedgerEntrySpec::getIntent)
                .containsOnly(LedgerPostingIntentType.FEE_REFUND);
    }

    private static FundsInstructionSpec instruction() {
        return new SimpleInstruction();
    }

    private static void assertPostingScope(LedgerTransactionSpec transaction, LedgerPostingScope expectedScope) {
        assertThat(transaction.getPostingPlans()).isNotEmpty();
        assertThat(transaction.getPostingPlans())
                .allSatisfy(plan -> {
                    assertThat(plan.getPostingScope()).isEqualTo(expectedScope);
                    assertThat(plan.getEntries())
                            .extracting(LedgerEntrySpec::getPostingScope)
                            .containsOnly(expectedScope);
                });
    }

    private static ResolvedRouteSpec route(Map<String, LedgerBalanceConstraintType> overrides) {
        return new SimpleResolvedRoute(List.of(new SimpleRouteLeg(overrides)));
    }

    private static ResolvedRouteSpec route(RouteLegSpec leg) {
        return new SimpleResolvedRoute(List.of(leg));
    }

    private static ResolvedRouteSpec sharedCardRoute() {
        return new SimpleResolvedRoute(FundsTransactionEventType.AUTHORIZE, DefaultFundsTransactionType.PAY, List.of(
                new SharedCardRouteLeg("AUTHORIZATION_1", "credit_001", FundsSubjectType.CREDIT_ACCOUNT),
                new SharedCardRouteLeg("AUTHORIZATION_2", "budget_001", FundsSubjectType.BUDGET_GROUP),
                new SharedCardRouteLeg("AUTHORIZATION_3", "funding_001", FundsSubjectType.FUNDING_ACCOUNT)
        ));
    }

    private static ResolvedRouteSpec feeRefundRoute() {
        return new SimpleResolvedRoute(FundsTransactionEventType.FEE_REFUND, DefaultFundsTransactionType.REFUND,
                List.of(new FeeRefundRouteLeg()));
    }

    private static Map.Entry<LedgerKey, LedgerDTO> ledger(Long id,
                                                          String subjectId,
                                                          LedgerSubjectCode subjectCode,
                                                          EntrySide normalBalanceSide) {
        return ledger(id, subjectId, FundsSubjectType.FUNDING_ACCOUNT, subjectCode, normalBalanceSide);
    }

    private static Map.Entry<LedgerKey, LedgerDTO> ledger(Long id,
                                                          String subjectId,
                                                          LedgerSubjectCode subjectCode,
                                                          EntrySide normalBalanceSide,
                                                          CurrencyIsoCode currency) {
        return ledger(id, subjectId, FundsSubjectType.FUNDING_ACCOUNT, subjectCode, normalBalanceSide, currency);
    }

    private static Map.Entry<LedgerKey, LedgerDTO> ledger(Long id,
                                                          String subjectId,
                                                          FundsSubjectType subjectType,
                                                          LedgerSubjectCode subjectCode,
                                                          EntrySide normalBalanceSide) {
        return ledger(id, subjectId, subjectType, subjectCode, normalBalanceSide, CURRENCY);
    }

    private static Map.Entry<LedgerKey, LedgerDTO> ledger(Long id,
                                                          String subjectId,
                                                          FundsSubjectType subjectType,
                                                          LedgerSubjectCode subjectCode,
                                                          EntrySide normalBalanceSide,
                                                          CurrencyIsoCode currency) {
        return ledger(id, subjectId, subjectType, subjectCode, normalBalanceSide, currency,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
    }

    private static Map.Entry<LedgerKey, LedgerDTO> ledger(Long id,
                                                          String subjectId,
                                                          LedgerSubjectCode subjectCode,
                                                          EntrySide normalBalanceSide,
                                                          AccountBalancePeriodType periodType,
                                                          String periodId) {
        return ledger(id, subjectId, FundsSubjectType.FUNDING_ACCOUNT, subjectCode, normalBalanceSide,
                CURRENCY, periodType, periodId);
    }

    private static Map.Entry<LedgerKey, LedgerDTO> ledger(Long id,
                                                          String subjectId,
                                                          FundsSubjectType subjectType,
                                                          LedgerSubjectCode subjectCode,
                                                          EntrySide normalBalanceSide,
                                                          CurrencyIsoCode currency,
                                                          AccountBalancePeriodType periodType,
                                                          String periodId) {
        LedgerDTO ledger = new LedgerDTO()
                .setId(id)
                .setTenantId(TENANT_ID)
                .setSubjectId(subjectId)
                .setSubjectType(subjectType.name())
                .setLedgerSubjectCode(subjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(normalBalanceSide)
                .setCurrency(currency)
                .setPeriodType(periodType)
                .setPeriodId(periodId);
        return Map.entry(new LedgerKey(TENANT_ID, subjectId, subjectType.name(), subjectCode, periodType, periodId), ledger);
    }

    private static LedgerService ledgerService(Map<LedgerKey, LedgerDTO> ledgers) {
        return new LedgerService() {
            @Override
            public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                                    @NonNull WindQuery<? extends QueryOrderField> options) {
                LedgerKey key = new LedgerKey(query.getTenantId(), query.getSubjectId(), query.getSubjectType(),
                        query.getLedgerSubjectCode(), query.getPeriodType(), query.getPeriodId());
                LedgerDTO ledger = ledgers.get(key);
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
                throw new UnsupportedOperationException("getLedgerById");
            }

            @Override
            public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
                throw new UnsupportedOperationException("getLedgerByIds");
            }
        };
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

    private record LedgerKey(Long tenantId,
                             String subjectId,
                             String subjectType,
                             LedgerSubjectCode subjectCode,
                             AccountBalancePeriodType periodType,
                             String periodId) {
    }

    private static final class SimpleResolvedRoute implements ResolvedRouteSpec {

        private final FundsTransactionEventType eventType;

        private final DefaultFundsTransactionType transactionType;

        private final List<RouteLegSpec> legs;

        private SimpleResolvedRoute(List<RouteLegSpec> legs) {
            this(FundsTransactionEventType.TOPUP, DefaultFundsTransactionType.TRANSFER, legs);
        }

        private SimpleResolvedRoute(FundsTransactionEventType eventType,
                                    DefaultFundsTransactionType transactionType,
                                    List<RouteLegSpec> legs) {
            this.eventType = eventType;
            this.transactionType = transactionType;
            this.legs = legs;
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "WALLET_DIRECT";
        }

        @Override
        public @NonNull String getRouteVersion() {
            return "v2";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "BIZ_0001";
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return transactionType;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of();
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return legs;
        }

        @Override
        public @NonNull LocalDateTime getResolvedAt() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private static final class SimpleInstruction implements FundsInstructionSpec {

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TOPUP;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CURRENCY);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return getAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public @Nullable PaymentInstrumentRefSpec getInstrumentRef() {
            return null;
        }

        @Override
        public @Nullable ExternalAccountRefSpec getExternalAccountRef() {
            return null;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return null;
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "BIZ_0001";
        }

        @Override
        public @NonNull LocalDateTime getEventTime() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @Nullable String getDescription() {
            return "transfer";
        }

        @Override
        public @NonNull FundsOperationActorSpec getOperator() {
            return systemActor();
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private static FundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }

    private static final class SimpleRouteLeg implements RouteLegSpec {

        private final Map<String, LedgerBalanceConstraintType> overrides;

        private final RouteNodeSpec sourceNode = new SimpleRouteNode("funding_001", RouteNodeRole.SOURCE);

        private final RouteNodeSpec targetNode = new SimpleRouteNode("funding_002", RouteNodeRole.TARGET);

        private SimpleRouteLeg(Map<String, LedgerBalanceConstraintType> overrides) {
            this.overrides = overrides;
        }

        @Override
        public @NonNull String getLegId() {
            return "LEG_001";
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.INTERNAL_TRANSFER;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return sourceNode;
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return targetNode;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CURRENCY);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return getAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return LedgerBalanceEffectType.CONSUME;
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.TRANSFER;
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return AccountBalancePeriodType.LIFETIME;
        }

        @Override
        public String getPeriodId() {
            return AccountBalancePeriodType.LIFETIME.name();
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return overrides;
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return RouteReplayPolicy.FULL_ONLY;
        }
    }

    private static final class MissingPeriodIdRouteLeg implements RouteLegSpec {

        private final SimpleRouteLeg delegate = new SimpleRouteLeg(Map.of());

        @Override
        public @NonNull String getLegId() {
            return delegate.getLegId();
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return delegate.getLegType();
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return delegate.getSourceNode();
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return delegate.getTargetNode();
        }

        @Override
        public @NonNull Money getAmount() {
            return delegate.getAmount();
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return delegate.getOriginalAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return delegate.getExchangeRate();
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return delegate.getBalanceEffectType();
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return delegate.getPhaseCode();
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return delegate.getPeriodType();
        }

        @Override
        public String getPeriodId() {
            return null;
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return delegate.getConstraintOverrides();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return delegate.getReplayPolicy();
        }
    }

    private static final class MonthlyRouteLeg implements RouteLegSpec {

        private final SimpleRouteLeg delegate = new SimpleRouteLeg(Map.of());

        private final String periodId;

        private MonthlyRouteLeg(String periodId) {
            this.periodId = periodId;
        }

        @Override
        public @NonNull String getLegId() {
            return delegate.getLegId();
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return delegate.getLegType();
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return delegate.getSourceNode();
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return delegate.getTargetNode();
        }

        @Override
        public @NonNull Money getAmount() {
            return delegate.getAmount();
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return delegate.getOriginalAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return delegate.getExchangeRate();
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return delegate.getBalanceEffectType();
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return delegate.getPhaseCode();
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return AccountBalancePeriodType.MONTHLY;
        }

        @Override
        public String getPeriodId() {
            return periodId;
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return delegate.getConstraintOverrides();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return delegate.getReplayPolicy();
        }
    }

    private static final class SimpleRouteNode implements RouteNodeSpec {

        private final SubjectRef subjectRef;

        private final LedgerSubjectCode subjectCode;

        private final RouteNodeRole nodeRole;

        private SimpleRouteNode(String subjectId, RouteNodeRole nodeRole) {
            this(new SimpleSubjectRef(subjectId), LedgerSubjectCode.AVAILABLE, nodeRole);
        }

        private SimpleRouteNode(SubjectRef subjectRef, LedgerSubjectCode subjectCode, RouteNodeRole nodeRole) {
            this.subjectRef = subjectRef;
            this.subjectCode = subjectCode;
            this.nodeRole = nodeRole;
        }

        @Override
        public @NonNull RouteNodeType getNodeType() {
            return RouteNodeType.SUBJECT;
        }

        @Override
        public @NonNull SubjectRef getSubjectRef() {
            return subjectRef;
        }

        @Override
        public @NonNull LedgerSubjectCode getLedgerSubjectCode() {
            return subjectCode;
        }

        @Override
        public @NonNull RouteNodeRole getNodeRole() {
            return nodeRole;
        }
    }

    private static final class SharedCardRouteLeg implements RouteLegSpec {

        private final String legId;

        private final RouteNodeSpec sourceNode;

        private final RouteNodeSpec targetNode;

        private SharedCardRouteLeg(String legId, String subjectId, FundsSubjectType subjectType) {
            this.legId = legId;
            SubjectRef subjectRef = new SimpleSubjectRef(subjectId, subjectType);
            this.sourceNode = new SimpleRouteNode(subjectRef, LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE);
            this.targetNode = new SimpleRouteNode(subjectRef, LedgerSubjectCode.AUTHORIZATION, RouteNodeRole.TARGET);
        }

        @Override
        public @NonNull String getLegId() {
            return legId;
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.HOLD;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return sourceNode;
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return targetNode;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CURRENCY);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return getAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return LedgerBalanceEffectType.HOLD;
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.AUTHORIZATION;
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return AccountBalancePeriodType.LIFETIME;
        }

        @Override
        public String getPeriodId() {
            return AccountBalancePeriodType.LIFETIME.name();
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return Map.of();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return RouteReplayPolicy.PARTIAL_ALLOWED;
        }
    }

    private static final class FeeRefundRouteLeg implements RouteLegSpec {

        private final RouteNodeSpec sourceNode = new SimpleRouteNode(new SimpleSubjectRef("funding_001"),
                LedgerSubjectCode.FEE, RouteNodeRole.SOURCE);

        private final RouteNodeSpec targetNode = new SimpleRouteNode(new SimpleSubjectRef("funding_002"),
                LedgerSubjectCode.AVAILABLE, RouteNodeRole.TARGET);

        @Override
        public @NonNull String getLegId() {
            return "FEE_REFUND_001";
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.RESTORE;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return sourceNode;
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return targetNode;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(30L, CURRENCY);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return getAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return LedgerBalanceEffectType.RESTORE;
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.REFUND;
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return AccountBalancePeriodType.LIFETIME;
        }

        @Override
        public String getPeriodId() {
            return AccountBalancePeriodType.LIFETIME.name();
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return Map.of();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return RouteReplayPolicy.PARTIAL_ALLOWED;
        }
    }

    private static final class SimpleSubjectRef implements SubjectRef {

        private final String subjectId;

        private final FundsSubjectType subjectType;

        private SimpleSubjectRef(String subjectId) {
            this(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
        }

        private SimpleSubjectRef(String subjectId, FundsSubjectType subjectType) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }

        @Override
        public @NonNull String getSubjectId() {
            return subjectId;
        }

        @Override
        public @NonNull FundsSubjectType getSubjectType() {
            return subjectType;
        }
    }
}

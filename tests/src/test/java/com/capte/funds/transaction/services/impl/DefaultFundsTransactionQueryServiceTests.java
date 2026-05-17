package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.integration.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.integration.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DefaultFundsTransactionQueryServiceTests {

    /**
     * 场景：后续撤销、结算、退款或拒付需要沿首次保存的 RouteSnapshot 回放。
     * 输入：交易事实已保存带参与方、路径和路由决策的快照。
     * 输出：按交易号解析后的 RouteSnapshotSpec。
     * 预期：快照保留 routeCode、schemaVersion、participants、legs、routingDecision 和账户引用信息。
     * 红线：查询服务只读交易事实，不重新选路也不依赖生命周期写侧实现。
     */
    @Test
    void testFindRouteSnapshotByTransactionSnShouldReadSavedSnapshotForReplay() {
        DefaultFundsTransactionQueryService queryService = queryService(transactionWithRouteSnapshot(),
                List.of(), null);

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByTransactionSn("FT_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getParticipants()).hasSize(2);
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
        assertThat(snapshot.getSnapshotSchemaVersion()).isEqualTo("v4");
        assertThat(snapshot.getRoutingDecision().getPolicyCode()).isEqualTo("LOWEST_COST");
        assertThat(snapshot.getRoutingDecision().getSelectedCashFundingAccount()).isEqualTo("PF_CASH_USD");
        assertThat(snapshot.getRoutingDecision().getSelectedPlatformAccount()).isEqualTo("PF_SETTLEMENT_USD");
        assertThat(snapshot.getRoutingDecision().getFundingAllocations()).singleElement()
                .satisfies(allocation -> {
                    assertThat(allocation.getAllocationId()).isEqualTo("ALLOC_001");
                    assertThat(allocation.getPriority()).isEqualTo(1);
                    assertThat(allocation.getReason()).isEqualTo("default source");
                });
        assertThat(snapshot.getPaymentInstrumentRef().getTenantId()).isEqualTo(1L);
        assertThat(snapshot.getPaymentInstrumentRef().getDescription()).isEqualTo("primary card");
        assertThat(snapshot.getExternalAccountRef().getDescription()).isEqualTo("bank account");
        assertThat(snapshot.getExternalAccountRef().getContextVariables())
                .containsEntry("externalTransactionId", "EXT_001");
        assertThat(snapshot.getPlatformAccounts().getCashFundingAccount().getSubjectId())
                .isEqualTo("platform_cash_usd");
        assertThat(snapshot.getPlatformAccounts().getAdjustmentFundingAccount().getSubjectId())
                .isEqualTo("platform_adjustment_usd");
        assertThat(snapshot.getParticipants().getFirst().getSubjectRef().getLedgerProfileCode())
                .isEqualTo("CREDIT_BASIC");
    }

    /**
     * 场景：replay、运营查询和后续展示投影都应复用已保存的交易事实。
     * 输入：一条主交易和两条主体视角明细。
     * 输出：主交易 DTO 与按 id 排序的 FundsTransactionDetailDTO 列表。
     * 预期：查询服务只读交易事实，完整返回交易类型、金额、参与方角色和资金效果。
     * 红线：查询服务不得修改交易事实、明细或账本事实。
     */
    @Test
    void testQueryFundsTransactionShouldReuseFactsForReplayAndProjection() {
        FundsTransactionDetail firstDetail = detail("FTD_001", RouteParticipantRole.AUTH_HOLDER);
        FundsTransactionDetail secondDetail = detail("FTD_002", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        secondDetail.setId(403L);
        secondDetail.setSubjectId("platform_revenue_001");
        secondDetail.setSubjectType("FUNDING_ACCOUNT");
        secondDetail.setFundsEffectType(FundsEffectType.DIRECT);
        DefaultFundsTransactionQueryService queryService = queryService(transaction(),
                List.of(firstDetail, secondDetail), null);

        assertThat(queryService.queryFundsTransaction("FT_001")).hasValueSatisfying(result -> {
            assertThat(result.getSn()).isEqualTo("FT_001");
            assertThat(result.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
            assertThat(result.getAmount()).isEqualTo(1_000L);
            assertThat(result.getAuthorizedAmount()).isZero();
        });
        assertThat(queryService.queryFundsTransactionDetails("FT_001"))
                .extracting("sn", "participantRole", "fundsEffectType")
                .containsExactly(
                        tuple("FTD_001", RouteParticipantRole.AUTH_HOLDER, FundsEffectType.HOLD),
                        tuple("FTD_002", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, FundsEffectType.DIRECT)
                );
    }

    /**
     * 场景：解冻请求以冻结单号作为引用时，需要定位原冻结交易并回放原冻结路径。
     * 输入：冻结单号绑定原资金交易号，原资金交易保存 RouteSnapshot。
     * 输出：按冻结单号解析得到的 RouteSnapshotSpec。
     * 预期：查询服务返回原冻结路径快照，不在解冻链路重新解析路径。
     * 红线：冻结单引用不得触发 route resolver 或 lifecycle recorder 写侧逻辑。
     */
    @Test
    void testFindRouteSnapshotByFreezeOrderSnShouldReadOriginalTransactionSnapshot() {
        FundsFrozenOrder frozenOrder = new FundsFrozenOrder();
        frozenOrder.setSn("FO_001");
        frozenOrder.setTransactionSn("FT_001");
        DefaultFundsTransactionQueryService queryService = queryService(transactionWithRouteSnapshot(),
                List.of(), frozenOrder);

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByFreezeOrderSn("FO_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
    }

    /**
     * 场景：冻结单自身已经保存 RouteSnapshot，且不再绑定标准资金交易号。
     * 输入：冻结单 contextVariables 中带有原冻结 RouteSnapshot。
     * 输出：按冻结单号解析得到的 RouteSnapshotSpec。
     * 预期：查询服务优先从冻结单事实自身读取快照，不再依赖 FundsTransaction。
     * 红线：冻结单已有快照时不得额外查询或回写标准资金交易。
     */
    @Test
    void testFindRouteSnapshotByFreezeOrderSnShouldReadFreezeOrderOwnSnapshotFirst() {
        RouteSnapshotSpec routeSnapshot = routeSnapshot();
        FundsFrozenOrder frozenOrder = new FundsFrozenOrder();
        frozenOrder.setSn("FO_001");
        frozenOrder.setContextVariables(JSON.toJSONString(Map.of(
                FundsInstructionContextKeys.ROUTE_SNAPSHOT, RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot)
        )));
        DefaultFundsTransactionQueryService queryService = new DefaultFundsTransactionQueryService(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> {
                            throw new AssertionError("冻结单自身有 RouteSnapshot 时不应查询 FundsTransaction");
                        }
                ),
                detailMapper(List.of()),
                frozenOrderMapper(frozenOrder)
        );

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByFreezeOrderSn("FO_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
    }

    private static DefaultFundsTransactionQueryService queryService(FundsTransaction transaction,
                                                                    List<FundsTransactionDetail> details,
                                                                    FundsFrozenOrder frozenOrder) {
        return new DefaultFundsTransactionQueryService(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction
                ),
                detailMapper(details),
                frozenOrderMapper(frozenOrder)
        );
    }

    private static FundsTransactionDetailMapper detailMapper(List<FundsTransactionDetail> details) {
        return FundsAccountServiceTestSupport.mapper(
                FundsTransactionDetailMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> {
                    throw new UnsupportedOperationException("selectOneByQuery");
                },
                query -> details,
                entity -> {
                    throw new UnsupportedOperationException("update");
                }
        );
    }

    private static FundsFrozenOrderMapper frozenOrderMapper(FundsFrozenOrder frozenOrder) {
        return FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> frozenOrder
        );
    }

    private static FundsTransaction transactionWithRouteSnapshot() {
        FundsTransaction transaction = transaction();
        transaction.setRouteSnapshot(RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot()));
        return transaction;
    }

    private static RouteSnapshotSpec routeSnapshot() {
        ResolvedRouteSpec route = new SnapshotMetadataResolvedRoute(1_000L);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    private static FundsTransaction transaction() {
        FundsTransaction transaction = new FundsTransaction();
        transaction.setId(401L);
        transaction.setSn("FT_001");
        transaction.setTenantId(1L);
        transaction.setBusinessScene("CARD_AUTH");
        transaction.setBusinessSn("AUTH_BUSINESS_0001");
        transaction.setTransactionMode(FundsTransactionMode.AUTHORIZATION);
        transaction.setTransactionType(DefaultFundsTransactionType.PAY);
        transaction.setStatus(FundsTransactionStatus.PROCESSING);
        transaction.setAmount(1_000L);
        transaction.setCurrency(CurrencyIsoCode.USD);
        transaction.setAuthorizedAmount(0L);
        transaction.setReversedAmount(0L);
        transaction.setSettledAmount(0L);
        transaction.setRefundedAmount(0L);
        transaction.setDeclinedAmount(0L);
        transaction.setFeeAmount(0L);
        return transaction;
    }

    private static FundsTransactionDetail detail(String sn, RouteParticipantRole participantRole) {
        FundsTransactionDetail detail = new FundsTransactionDetail();
        detail.setId(402L);
        detail.setSn(sn);
        detail.setTenantId(1L);
        detail.setTransactionSn("FT_001");
        detail.setBusinessScene("CARD_AUTH");
        detail.setBusinessSn("AUTH_BUSINESS_0001");
        detail.setTransactionType(DefaultFundsTransactionType.PAY);
        detail.setEventType(FundsTransactionEventType.AUTHORIZE);
        detail.setSubjectId(participantRole == RouteParticipantRole.AUTH_HOLDER ? "credit_001" : "platform_revenue_001");
        detail.setSubjectType(participantRole == RouteParticipantRole.AUTH_HOLDER ? "CREDIT_ACCOUNT" : "FUNDING_ACCOUNT");
        detail.setParticipantRole(participantRole);
        detail.setRequestHash("same_hash");
        detail.setFundsEffectType(FundsEffectType.HOLD);
        detail.setAmount(1_000L);
        detail.setCurrency(CurrencyIsoCode.USD);
        detail.setStatus(FundsTransactionDetailStatus.PROCESSING);
        return detail;
    }

    private static final class SnapshotMetadataResolvedRoute extends SimpleResolvedRoute {

        private SnapshotMetadataResolvedRoute(long amount) {
            super(amount);
        }

        @Override
        public @Nullable RoutingDecisionSpec getRoutingDecision() {
            return ImmutableRoutingDecisionSpec.builder()
                    .policyCode("LOWEST_COST")
                    .matchedRules(List.of("USD_ONLY"))
                    .selectedProcessor("VISA_A")
                    .selectedCashFundingAccount("PF_CASH_USD")
                    .selectedPlatformAccount("PF_SETTLEMENT_USD")
                    .fundingAllocations(List.of(ImmutableFundingAllocationDecisionSpec.builder()
                            .allocationId("ALLOC_001")
                            .subjectRef(new MetadataSubjectRef("funding_001", FundsSubjectType.FUNDING_ACCOUNT,
                                    "funding", "FUNDING_BASIC"))
                            .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                            .amount(Money.immutable(1_000L, CurrencyIsoCode.USD))
                            .priority(1)
                            .reason("default source")
                            .build()))
                    .decisionReason("default routing")
                    .contextVariables(Map.of("riskLevel", "LOW"))
                    .build();
        }

        @Override
        public @Nullable PaymentInstrumentRefSpec getPaymentInstrumentRef() {
            return ImmutablePaymentInstrumentRefSpec.builder()
                    .tenantId(1L)
                    .instrumentId("PI_001")
                    .instrumentType("SHARED_CARD")
                    .instrumentNo("411111******1111")
                    .ownerId("credit_001")
                    .ownerType("CREDIT_ACCOUNT")
                    .currency(CurrencyIsoCode.USD.name())
                    .status("ACTIVE")
                    .bindingSnapshot(Map.of("bindingId", "BIND_001"))
                    .description("primary card")
                    .build();
        }

        @Override
        public @Nullable ExternalAccountRefSpec getExternalAccountRef() {
            return ImmutableExternalAccountRefSpec.builder()
                    .externalAccountId("BANK_001")
                    .externalAccountType("BANK")
                    .externalAccountNo("1234")
                    .providerCode("BANK_A")
                    .channelCode("ACH")
                    .currency(CurrencyIsoCode.USD.name())
                    .countryCode("US")
                    .description("bank account")
                    .contextVariables(Map.of("externalTransactionId", "EXT_001"))
                    .build();
        }

        @Override
        public @Nullable PlatformAccountsSnapshotSpec getPlatformAccounts() {
            return com.wind.integration.funds.model.route.ImmutablePlatformAccountsSnapshotSpec.builder()
                    .cashFundingAccount(new MetadataSubjectRef("platform_cash_usd", FundsSubjectType.FUNDING_ACCOUNT,
                            "cash mapping", "FUNDING_PLATFORM"))
                    .adjustmentFundingAccount(new MetadataSubjectRef("platform_adjustment_usd",
                            FundsSubjectType.FUNDING_ACCOUNT, "adjustment", "FUNDING_PLATFORM"))
                    .build();
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(
                    new SimpleParticipant(RouteParticipantRole.AUTH_HOLDER,
                            new MetadataSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT, "credit",
                                    "CREDIT_BASIC"), 1_000L),
                    new SimpleParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT,
                            new MetadataSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT,
                                    "cash-mapping", "FUNDING_PLATFORM"), 1_000L)
            );
        }
    }

    private static class SimpleResolvedRoute implements ResolvedRouteSpec {

        private final long amount;

        private SimpleResolvedRoute(long amount) {
            this.amount = amount;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getRouteVersion() {
            return "v2";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "AUTH_BUSINESS_0001";
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.AUTHORIZATION_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.AUTHORIZE;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.PAY;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(
                    new SimpleParticipant(RouteParticipantRole.AUTH_HOLDER,
                            new SimpleSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT), amount),
                    new SimpleParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT,
                            new SimpleSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT), amount)
            );
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return List.of(new SimpleLeg(amount));
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

    private static final class SimpleParticipant implements RouteParticipantSpec {

        private final RouteParticipantRole role;

        private final SubjectRef subjectRef;

        private final long amount;

        private SimpleParticipant(RouteParticipantRole role, SubjectRef subjectRef, long amount) {
            this.role = role;
            this.subjectRef = subjectRef;
            this.amount = amount;
        }

        @Override
        public @NonNull RouteParticipantRole getParticipantRole() {
            return role;
        }

        @Override
        public @NonNull SubjectRef getSubjectRef() {
            return subjectRef;
        }

        @Override
        public @Nullable String getLedgerProfileCode() {
            return role == RouteParticipantRole.AUTH_HOLDER ? "CREDIT_BASIC" : "FUNDING_PLATFORM";
        }

        @Override
        public @Nullable String getCurrency() {
            return CurrencyIsoCode.USD.name();
        }

        @Override
        public @Nullable Money getAmount() {
            return Money.immutable(amount, CurrencyIsoCode.USD);
        }
    }

    private static final class SimpleLeg implements RouteLegSpec {

        private final long amount;

        private SimpleLeg(long amount) {
            this.amount = amount;
        }

        @Override
        public @NonNull String getLegId() {
            return "LEG_001";
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.HOLD;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return new SimpleNode(new SimpleSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT),
                    LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE);
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return new SimpleNode(new SimpleSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT),
                    LedgerSubjectCode.AUTHORIZATION, RouteNodeRole.TARGET);
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(amount, CurrencyIsoCode.USD);
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
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return Map.of();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return RouteReplayPolicy.FULL_ONLY;
        }
    }

    private static final class SimpleNode implements RouteNodeSpec {

        private final SubjectRef subjectRef;

        private final LedgerSubjectCode subjectCode;

        private final RouteNodeRole nodeRole;

        private SimpleNode(SubjectRef subjectRef, LedgerSubjectCode subjectCode, RouteNodeRole nodeRole) {
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

    private static class SimpleSubjectRef implements SubjectRef {

        private final String subjectId;

        private final FundsSubjectType subjectType;

        private SimpleSubjectRef(String subjectId, FundsSubjectType subjectType) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
        }

        @Override
        public Long getTenantId() {
            return 1L;
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

    private static final class MetadataSubjectRef extends SimpleSubjectRef {

        private final String subjectName;

        private final String ledgerProfileCode;

        private MetadataSubjectRef(String subjectId, FundsSubjectType subjectType, String subjectName,
                                   String ledgerProfileCode) {
            super(subjectId, subjectType);
            this.subjectName = subjectName;
            this.ledgerProfileCode = ledgerProfileCode;
        }

        @Override
        public @Nullable String getSubjectName() {
            return subjectName;
        }

        @Override
        public @Nullable String getCurrency() {
            return CurrencyIsoCode.USD.name();
        }

        @Override
        public @Nullable String getLedgerProfileCode() {
            return ledgerProfileCode;
        }

        @Override
        public @Nullable String getDescription() {
            return subjectName + " subject";
        }
    }
}

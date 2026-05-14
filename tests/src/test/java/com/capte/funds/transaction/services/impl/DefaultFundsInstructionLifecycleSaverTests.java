package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.integration.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class DefaultFundsInstructionLifecycleSaverTests {

    /**
     * 场景：生命周期保存器作为写侧端口，防止交易事实查询能力重新混入 saver。
     * 输入：反射读取 FundsInstructionLifecycleSaver 声明的方法。
     * 输出：方法名称集合和返回类型集合。
     * 预期：契约只保留 beforePosting、markSucceeded、markFailed，不出现 query/find/get/list 等查询职责。
     */
    @Test
    void testLifecycleSaverShouldNotContainQueryResponsibilities() {
        Method[] methods = FundsInstructionLifecycleSaver.class.getDeclaredMethods();
        List<String> methodNames = Arrays.stream(methods)
                .map(Method::getName)
                .toList();

        assertThat(methodNames)
                .containsExactlyInAnyOrder("beforePosting", "markSucceeded", "markFailed");
        assertThat(methodNames)
                .noneMatch(DefaultFundsInstructionLifecycleSaverTests::isQueryMethodName);
        assertThat(Arrays.stream(methods).map(Method::getReturnType).toList())
                .containsExactlyInAnyOrder(FundsInstructionLifecycleResult.class, Void.TYPE, Void.TYPE);
    }

    /**
     * 场景：一笔授权交易包含持卡主体和平台预留主体两个参与方。
     * 输入：AUTHORIZATION 指令、包含两个 participants 的 RouteSnapshot。
     * 输出：一条资金主交易和两条主体视角生命周期明细。
     * 预期：主交易只创建一条，明细按参与方创建，均处于 PROCESSING 且资金效果为 HOLD。
     */
    @Test
    void testLifecycleSaverShouldCreateOneTransactionAndManyParticipantDetails() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        FundsInstructionSpec instruction = new SimpleInstruction();
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);

        FundsInstructionLifecycleResult result = saver.beforePosting(instruction, route, snapshot);

        assertThat(result.getTransactionSn()).startsWith("FT");
        assertThat(result.getTransactionDetailSns()).hasSize(2);
        assertThat(result.isCompleted()).isFalse();
        FundsTransaction transaction = insertedTransaction.get();
        assertThat(transaction.getTransactionMode()).isEqualTo(FundsTransactionMode.AUTHORIZATION);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.PROCESSING);
        assertThat(transaction.getBusinessSn()).isEqualTo("AUTH_BUSINESS_0001");
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getRouteSnapshot()).isNotBlank();
        assertThat(insertedDetails).hasSize(2);
        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getTransactionSn)
                .containsOnly(result.getTransactionSn());
        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getFundsEffectType)
                .containsOnly(FundsEffectType.HOLD);
        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getStatus)
                .containsOnly(FundsTransactionDetailStatus.PROCESSING);
        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getParticipantRole)
                .containsExactlyInAnyOrder(RouteParticipantRole.AUTH_HOLDER, RouteParticipantRole.PLATFORM_RESERVE);
    }

    /**
     * 场景：同一上游业务事实被不同交易类型重复送达。
     * 输入：同租户、同 businessScene、同 businessSn，但交易类型不同的资金指令。
     * 输出：资金生命周期保存结果和主交易查询条件。
     * 预期：按 tenantId + businessScene + businessSn 命中既有主交易，不把 transactionType 纳入主交易幂等键。
     */
    @Test
    void testFundsTransactionShouldBeUniqueByTenantSceneBusinessSn() {
        FundsTransaction existingTransaction = transaction();
        AtomicReference<QueryWrapper> transactionQuery = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new AssertionError("same business key should reuse existing transaction");
                        },
                        query -> {
                            transactionQuery.set(query);
                            return existingTransaction;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);

        FundsInstructionLifecycleResult result = saver.beforePosting(
                new TransactionTypeChangedInstruction(), route, new DefaultRouteSnapshotFactory().createSnapshot(route));

        assertThat(result.getTransactionSn()).isEqualTo(existingTransaction.getSn());
        Map<String, Object> queryValues = queryValues(transactionQuery.get());
        assertThat(queryValues)
                .containsEntry("tenant_id", 1L)
                .containsEntry("business_scene", "CARD_AUTH")
                .containsEntry("business_sn", "AUTH_BUSINESS_0001");
        assertThat(queryValues).doesNotContainKey("transaction_type");
    }

    /**
     * 场景：`REPLAY_ONCE` 事件写入生命周期明细。
     * 输入：包含 replayRefLegId 的回放 RouteSnapshot。
     * 输出：交易明细 contextVariables。
     * 预期：明细记录本次成功消费的原 RouteLeg ID，供后续二次 replay 判断使用。
     */
    @Test
    void testReplayOnceDetailShouldRecordConsumedReplayLegIds() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L, "SOURCE_LEG_001");

        saver.beforePosting(new SimpleInstruction(), route, new DefaultRouteSnapshotFactory().createSnapshot(route));

        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getContextVariables)
                .allSatisfy(contextVariables -> assertThat(contextVariables)
                        .contains("\"replayConsumedLegIds\":[\"SOURCE_LEG_001\"]"));
    }

    /**
     * 场景：共享卡授权同时占用信用、预算和真实资金三个主体。
     * 输入：共享卡授权 RouteSnapshot，包含三类主体参与方。
     * 输出：一条主交易和三条主体视角生命周期明细。
     * 预期：主交易金额保持业务本金 1000，不按三条 detail 金额求和成 3000。
     */
    @Test
    void testTransactionAmountShouldNotSumParticipantDetails() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SharedCardResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);

        FundsInstructionLifecycleResult result = saver.beforePosting(new SimpleInstruction(), route, snapshot);

        assertThat(result.getTransactionDetailSns()).hasSize(3);
        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getParticipantRole)
                .containsExactlyInAnyOrder(RouteParticipantRole.AUTH_HOLDER,
                        RouteParticipantRole.BUDGET_CONTROLLER,
                        RouteParticipantRole.REAL_FUNDING_SOURCE);
        assertThat(insertedDetails)
                .extracting(FundsTransactionDetail::getAmount)
                .containsOnly(1_000L);
        assertThat(insertedTransaction.get().getAmount()).isEqualTo(1_000L);
    }

    /**
     * 场景：授权被拒绝时不会产生账本交易，但仍需要保存稳定的交易明细事实。
     * 输入：APPROVED=false 的授权明细，markSucceeded 时 ledgerTransactionSn 为空。
     * 输出：被更新的明细状态、账本交易号、主交易聚合状态和拒付累计金额。
     * 预期：明细状态为 REJECTED，ledgerTransactionSn 保持为空，主交易状态为 REJECTED，且不占用结算后拒付金额。
     */
    @Test
    void testLifecycleSaverShouldAllowSucceededDetailWithoutLedgerTransactionForRejectedAuthorization() {
        FundsTransaction transaction = transaction();
        FundsTransactionDetail detail = rejectedAuthorizationDetail("FTD_001", RouteParticipantRole.AUTH_HOLDER);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        AtomicReference<FundsTransactionDetail> updatedDetail = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, detail, updatedTransaction,
                updatedDetail);

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), null);

        assertThat(updatedDetail.get().getStatus()).isEqualTo(FundsTransactionDetailStatus.REJECTED);
        assertThat(updatedDetail.get().getLedgerTransactionSn()).isNull();
        assertThat(updatedTransaction.get().getDeclinedAmount()).isZero();
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.REJECTED);
    }

    @Test
    void markSucceededShouldUpdateDetailsAndTransactionSummary() {
        FundsTransaction transaction = transaction();
        FundsTransactionDetail detail = detail("FTD_001", RouteParticipantRole.AUTH_HOLDER);
        FundsTransactionDetail platformDetail = detail("FTD_002", RouteParticipantRole.PLATFORM_RESERVE);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> updatedDetails = new ArrayList<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction,
                        entity -> {
                            updatedTransaction.set((FundsTransaction) entity);
                            return 1;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> detailQueryIndex.getAndIncrement() == 0 ? detail : platformDetail,
                        entity -> {
                            updatedDetails.add((FundsTransactionDetail) entity);
                            return 1;
                        }
                )
        );

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002")), "LE_001");

        assertThat(updatedDetails).hasSize(2);
        assertThat(updatedDetails)
                .extracting(FundsTransactionDetail::getStatus)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED);
        assertThat(updatedDetails)
                .extracting(FundsTransactionDetail::getLedgerTransactionSn)
                .containsOnly("LE_001");
        assertThat(updatedTransaction.get().getAuthorizedAmount()).isEqualTo(1_000L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
    }

    @Test
    void markSucceededShouldSummarizeSharedCardTransactionOnce() {
        FundsTransaction transaction = transaction();
        List<FundsTransactionDetail> details = List.of(
                detail("FTD_001", RouteParticipantRole.AUTH_HOLDER),
                detail("FTD_002", RouteParticipantRole.BUDGET_CONTROLLER),
                detail("FTD_003", RouteParticipantRole.REAL_FUNDING_SOURCE)
        );
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> updatedDetails = new ArrayList<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction,
                        entity -> {
                            updatedTransaction.set((FundsTransaction) entity);
                            return 1;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> details.get(detailQueryIndex.getAndIncrement()),
                        entity -> {
                            updatedDetails.add((FundsTransactionDetail) entity);
                            return 1;
                        }
                )
        );

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002", "FTD_003")), "LE_001");

        assertThat(updatedDetails).hasSize(3);
        assertThat(updatedDetails)
                .extracting(FundsTransactionDetail::getStatus)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED);
        assertThat(updatedTransaction.get().getAuthorizedAmount()).isEqualTo(1_000L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
    }

    @Test
    void markSucceededShouldSummarizeDirectTransactionAndFeeSeparately() {
        FundsTransaction transaction = transaction();
        transaction.setTransactionMode(FundsTransactionMode.DIRECT);
        transaction.setTransactionType(DefaultFundsTransactionType.TRANSFER);
        FundsTransactionDetail payerDetail = directDetail("FTD_001", RouteParticipantRole.PAYER, 1_000L);
        FundsTransactionDetail payeeDetail = directDetail("FTD_002", RouteParticipantRole.PAYEE, 1_000L);
        FundsTransactionDetail feeDetail = directDetail("FTD_003", RouteParticipantRole.FEE_RECEIVER, 30L);
        List<FundsTransactionDetail> details = List.of(payerDetail, payeeDetail, feeDetail);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction,
                        entity -> {
                            updatedTransaction.set((FundsTransaction) entity);
                            return 1;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> details.get(detailQueryIndex.getAndIncrement()),
                        entity -> 1
                )
        );

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002", "FTD_003")), "LE_001");

        assertThat(updatedTransaction.get().getSettledAmount()).isEqualTo(1_000L);
        assertThat(updatedTransaction.get().getFeeAmount()).isEqualTo(30L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    @Test
    void markSucceededShouldSummarizeStandaloneFeeAsFeeAmount() {
        FundsTransaction transaction = transaction();
        transaction.setTransactionMode(FundsTransactionMode.DIRECT);
        transaction.setTransactionType(DefaultFundsTransactionType.FEE);
        FundsTransactionDetail payerDetail = directDetail("FTD_001", RouteParticipantRole.PAYER, 30L);
        payerDetail.setTransactionType(DefaultFundsTransactionType.FEE);
        payerDetail.setEventType(FundsTransactionEventType.FEE_CHARGE);
        FundsTransactionDetail feeDetail = directDetail("FTD_002", RouteParticipantRole.FEE_RECEIVER, 30L);
        feeDetail.setTransactionType(DefaultFundsTransactionType.FEE);
        feeDetail.setEventType(FundsTransactionEventType.FEE_CHARGE);
        List<FundsTransactionDetail> details = List.of(payerDetail, feeDetail);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction,
                        entity -> {
                            updatedTransaction.set((FundsTransaction) entity);
                            return 1;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> details.get(detailQueryIndex.getAndIncrement()),
                        entity -> 1
                )
        );

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002")), "LE_001");

        assertThat(updatedTransaction.get().getSettledAmount()).isZero();
        assertThat(updatedTransaction.get().getFeeAmount()).isEqualTo(30L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    @Test
    void markSucceededShouldCloseWhenRefundConsumesRemainingSettledReversibleAmount() {
        FundsTransaction transaction = transaction();
        transaction.setStatus(FundsTransactionStatus.OPEN);
        transaction.setSettledAmount(1_000L);
        transaction.setRefundedAmount(600L);
        transaction.setDeclinedAmount(200L);
        FundsTransactionDetail refundDetail = returnDetail("FTD_001", FundsTransactionEventType.REFUND, 200L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, refundDetail, updatedTransaction);

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001");

        assertThat(updatedTransaction.get().getRefundedAmount()).isEqualTo(800L);
        assertThat(updatedTransaction.get().getDeclinedAmount()).isEqualTo(200L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    @Test
    void markSucceededShouldCloseWhenChargebackConsumesRemainingSettledReversibleAmount() {
        FundsTransaction transaction = transaction();
        transaction.setStatus(FundsTransactionStatus.OPEN);
        transaction.setSettledAmount(1_000L);
        transaction.setRefundedAmount(600L);
        transaction.setDeclinedAmount(200L);
        FundsTransactionDetail chargebackDetail = returnDetail("FTD_001", FundsTransactionEventType.CHARGEBACK,
                200L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, chargebackDetail,
                updatedTransaction);

        saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001");

        assertThat(updatedTransaction.get().getRefundedAmount()).isEqualTo(600L);
        assertThat(updatedTransaction.get().getDeclinedAmount()).isEqualTo(400L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    @Test
    void markSucceededShouldRejectRefundWhenSettledReversibleAmountInsufficient() {
        FundsTransaction transaction = transaction();
        transaction.setStatus(FundsTransactionStatus.OPEN);
        transaction.setSettledAmount(1_000L);
        transaction.setRefundedAmount(700L);
        transaction.setDeclinedAmount(200L);
        FundsTransactionDetail refundDetail = returnDetail("FTD_001", FundsTransactionEventType.REFUND, 200L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, refundDetail, updatedTransaction);

        assertThatThrownBy(() -> saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已结算可回退金额不足");
        assertThat(updatedTransaction).hasValue(null);
    }

    @Test
    void markSucceededShouldRejectChargebackWhenSettledReversibleAmountInsufficient() {
        FundsTransaction transaction = transaction();
        transaction.setStatus(FundsTransactionStatus.OPEN);
        transaction.setSettledAmount(1_000L);
        transaction.setRefundedAmount(700L);
        transaction.setDeclinedAmount(200L);
        FundsTransactionDetail chargebackDetail = returnDetail("FTD_001", FundsTransactionEventType.CHARGEBACK,
                200L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, chargebackDetail,
                updatedTransaction);

        assertThatThrownBy(() -> saver.markSucceeded(new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已结算可回退金额不足");
        assertThat(updatedTransaction).hasValue(null);
    }

    @Test
    void beforePostingShouldReuseCompletedDetails() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver createSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);
        createSaver.beforePosting(new SimpleInstruction(), route, snapshot);
        insertedDetails.forEach(detail -> {
            detail.setStatus(FundsTransactionDetailStatus.SUCCEEDED);
            detail.setLedgerTransactionSn("LE_001");
        });
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver reuseSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedTransaction.get()
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedDetails.get(detailQueryIndex.getAndIncrement())
                )
        );

        FundsInstructionLifecycleResult result = reuseSaver.beforePosting(new SimpleInstruction(), route, snapshot);

        assertThat(result.getTransactionSn()).isEqualTo(insertedTransaction.get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(insertedDetails.get(0).getSn(), insertedDetails.get(1).getSn());
        assertThat(result.getLedgerTransactionSn()).isEqualTo("LE_001");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void beforePostingShouldRejectChangedRouteForSameBusinessEvent() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver createSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);
        createSaver.beforePosting(new SimpleInstruction(), route, snapshot);
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver reuseSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedTransaction.get()
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedDetails.get(detailQueryIndex.getAndIncrement())
                )
        );
        ResolvedRouteSpec changedRoute = new SimpleResolvedRoute(2_000L);
        RouteSnapshotSpec changedSnapshot = new DefaultRouteSnapshotFactory().createSnapshot(changedRoute);

        assertThatThrownBy(() -> reuseSaver.beforePosting(new SimpleInstruction(), changedRoute, changedSnapshot))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金交易明细请求参数不一致");
    }

    /**
     * 场景：同一业务流水重复送达，但本次 RouteSnapshot 的快照号和解析时间重新生成。
     * 输入：同一 FundsInstruction 和同一路由语义，第二次仅替换 snapshotId、resolvedAt、expiresAt。
     * 输出：生命周期保存结果。
     * 预期：requestHash 只绑定稳定业务事实和路由语义，忽略快照流水与审计时间后幂等复用既有明细。
     */
    @Test
    void beforePostingShouldIgnoreRouteSnapshotMetadataWhenComparingRequestHash() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver createSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);
        createSaver.beforePosting(new SimpleInstruction(), route, snapshot);
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver reuseSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedTransaction.get()
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedDetails.get(detailQueryIndex.getAndIncrement())
                )
        );
        RouteSnapshotSpec regeneratedSnapshot = copySnapshotWithMetadata(snapshot, "AUTH_BUSINESS_0001_ROUTE_RETRY",
                LocalDateTime.of(2026, 5, 9, 12, 1), LocalDateTime.of(2026, 5, 10, 12, 1));

        FundsInstructionLifecycleResult result = reuseSaver.beforePosting(new SimpleInstruction(), route,
                regeneratedSnapshot);

        assertThat(result.getTransactionSn()).isEqualTo(insertedTransaction.get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(insertedDetails.get(0).getSn(), insertedDetails.get(1).getSn());
    }

    @Test
    void beforePostingShouldReuseReferencedFundsTransactionOnlyForSnapshotReference() {
        FundsTransaction referencedTransaction = transaction();
        AtomicInteger transactionQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new AssertionError("should not create transaction");
                        },
                        query -> transactionQueryIndex.getAndIncrement() == 0 ? referencedTransaction : null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);

        FundsInstructionLifecycleResult result = saver.beforePosting(
                new ReferencedInstruction(FundsInstructionReferenceType.AUTHORIZATION), route, snapshot);

        assertThat(result.getTransactionSn()).isEqualTo(referencedTransaction.getSn());
        assertThat(transactionQueryIndex).hasValue(1);
    }

    @Test
    void beforePostingShouldNotReuseFundsTransactionForFreezeOrderReference() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        AtomicInteger transactionQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> {
                            transactionQueryIndex.incrementAndGet();
                            return null;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);

        FundsInstructionLifecycleResult result = saver.beforePosting(
                new ReferencedInstruction(FundsInstructionReferenceType.FREEZE_ORDER), route, snapshot);

        assertThat(result.getTransactionSn()).isEqualTo(insertedTransaction.get().getSn());
        assertThat(insertedTransaction.get().getReferenceTransactionSn()).isEqualTo("FT_001");
        assertThat(transactionQueryIndex).hasValue(1);
    }

    /**
     * 场景：后续撤销、结算、退款或拒付需要沿首次保存的 RouteSnapshot 回放。
     * 输入：生命周期保存器写入带参与方、路径和路由决策的快照，再通过查询服务按交易号读取。
     * 输出：解析后的 RouteSnapshotSpec。
     * 预期：快照保留 routeCode、schemaVersion、participants、legs、routingDecision 和账户引用信息。
     */
    @Test
    void testFundsTransactionQueryServiceShouldReadSavedRouteSnapshotForReplay() {
        FundsTransaction transaction = transaction();
        transaction.setRouteSnapshot(null);
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver createSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction inserted = (FundsTransaction) entity;
                            inserted.setId(501L);
                            insertedTransaction.set(inserted);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SnapshotMetadataResolvedRoute(1_000L);
        createSaver.beforePosting(new SimpleInstruction(), route, new DefaultRouteSnapshotFactory().createSnapshot(route));
        transaction.setRouteSnapshot(insertedTransaction.get().getRouteSnapshot());
        DefaultFundsTransactionQueryService queryService = new DefaultFundsTransactionQueryService(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> null
                )
        );

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByTransactionSn("FT_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getParticipants()).hasSize(2);
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
        assertThat(snapshot.getSnapshotSchemaVersion()).isEqualTo("v4");
        assertThat(snapshot.getRoutingDecision().getPolicyCode()).isEqualTo("LOWEST_COST");
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
        assertThat(snapshot.getParticipants().getFirst().getSubjectRef().getLedgerProfileCode()).isEqualTo("CREDIT_BASIC");
    }

    /**
     * 场景：replay、运营查询和后续展示投影都应复用已保存的交易事实。
     * 输入：构造一条主交易和两条主体视角明细，通过 FundsTransactionQueryService 查询。
     * 输出：主交易 DTO 与按 id 排序的 FundsTransactionDetailDTO 列表。
     * 预期：查询服务只读交易事实，完整返回交易类型、金额、参与方角色和资金效果。
     */
    @Test
    void testFundsTransactionQueryServiceShouldReuseFactsForReplayAndProjection() {
        FundsTransaction transaction = transaction();
        FundsTransactionDetail firstDetail = detail("FTD_001", RouteParticipantRole.AUTH_HOLDER);
        FundsTransactionDetail secondDetail = detail("FTD_002", RouteParticipantRole.PLATFORM_RESERVE);
        secondDetail.setId(403L);
        secondDetail.setSubjectId("platform_revenue_001");
        secondDetail.setSubjectType("FUNDING_ACCOUNT");
        secondDetail.setFundsEffectType(FundsEffectType.DIRECT);
        DefaultFundsTransactionQueryService queryService = new DefaultFundsTransactionQueryService(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> {
                            throw new UnsupportedOperationException("selectOneByQuery");
                        },
                        query -> List.of(firstDetail, secondDetail),
                        entity -> {
                            throw new UnsupportedOperationException("update");
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> null
                )
        );

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
                        tuple("FTD_002", RouteParticipantRole.PLATFORM_RESERVE, FundsEffectType.DIRECT)
                );
    }

    /**
     * 场景：解冻请求以冻结单号作为引用时，需要定位原冻结交易并回放原冻结路径。
     * 输入：冻结单号绑定原资金交易号，原资金交易保存 RouteSnapshot。
     * 输出：按冻结单号解析得到的 RouteSnapshotSpec。
     * 预期：查询服务返回原冻结路径快照，不在解冻链路重新解析路径。
     */
    @Test
    void testFundsTransactionQueryServiceShouldReadFreezeOrderSnapshotForReplay() {
        FundsTransaction transaction = transaction();
        transaction.setRouteSnapshot(null);
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver createSaver = new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction inserted = (FundsTransaction) entity;
                            inserted.setId(501L);
                            insertedTransaction.set(inserted);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SnapshotMetadataResolvedRoute(1_000L);
        createSaver.beforePosting(new SimpleInstruction(), route, new DefaultRouteSnapshotFactory().createSnapshot(route));
        transaction.setRouteSnapshot(insertedTransaction.get().getRouteSnapshot());
        FundsFrozenOrder frozenOrder = new FundsFrozenOrder();
        frozenOrder.setSn("FO_001");
        frozenOrder.setTransactionSn("FT_001");
        DefaultFundsTransactionQueryService queryService = new DefaultFundsTransactionQueryService(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> frozenOrder
                )
        );

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByFreezeOrderSn("FO_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
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

    private static FundsTransactionDetail returnDetail(String sn, FundsTransactionEventType eventType, long amount) {
        FundsTransactionDetail detail = detail(sn, RouteParticipantRole.AUTH_HOLDER);
        detail.setTransactionType(DefaultFundsTransactionType.REFUND);
        detail.setEventType(eventType);
        detail.setFundsEffectType(FundsEffectType.RETURN);
        detail.setAmount(amount);
        return detail;
    }

    private static FundsTransactionDetail rejectedAuthorizationDetail(String sn, RouteParticipantRole participantRole) {
        FundsTransactionDetail detail = detail(sn, participantRole);
        detail.setContextVariables("{\"" + FundsInstructionContextKeys.APPROVED + "\":false}");
        return detail;
    }

    private static boolean isQueryMethodName(String methodName) {
        return methodName.startsWith("query")
                || methodName.startsWith("find")
                || methodName.startsWith("get")
                || methodName.startsWith("list")
                || methodName.startsWith("count")
                || methodName.startsWith("search");
    }

    private static RouteSnapshotSpec copySnapshotWithMetadata(RouteSnapshotSpec snapshot,
                                                              String snapshotId,
                                                              LocalDateTime resolvedAt,
                                                              LocalDateTime expiresAt) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(snapshot.getTenantId())
                .snapshotId(snapshotId)
                .snapshotSchemaVersion(snapshot.getSnapshotSchemaVersion())
                .routeCode(snapshot.getRouteCode())
                .routeVersion(snapshot.getRouteVersion())
                .businessScene(snapshot.getBusinessScene())
                .businessSn(snapshot.getBusinessSn())
                .instructionType(snapshot.getInstructionType())
                .eventType(snapshot.getEventType())
                .transactionType(snapshot.getTransactionType())
                .participants(snapshot.getParticipants())
                .legs(snapshot.getLegs())
                .routingDecision(snapshot.getRoutingDecision())
                .paymentInstrumentRef(snapshot.getPaymentInstrumentRef())
                .externalAccountRef(snapshot.getExternalAccountRef())
                .platformAccounts(snapshot.getPlatformAccounts())
                .resolvedAt(resolvedAt)
                .expiresAt(expiresAt)
                .description(snapshot.getDescription())
                .contextVariables(snapshot.getContextVariables())
                .build();
    }

    private static Map<String, Object> queryValues(QueryWrapper query) {
        Map<String, Object> result = new LinkedHashMap<>();
        QueryCondition condition = whereCondition(query);
        while (condition != null) {
            if (condition.checkEffective()) {
                QueryColumn column = condition.getColumn();
                if (column != null) {
                    result.put(column.getName(), condition.getValue());
                }
            }
            condition = nextCondition(condition);
        }
        return result;
    }

    private static QueryCondition whereCondition(QueryWrapper query) {
        try {
            Field field = query.getClass().getSuperclass().getDeclaredField("whereQueryCondition");
            field.setAccessible(true);
            return (QueryCondition) field.get(query);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取 QueryWrapper 查询条件失败", exception);
        }
    }

    private static QueryCondition nextCondition(QueryCondition condition) {
        try {
            Field field = QueryCondition.class.getDeclaredField("next");
            field.setAccessible(true);
            return (QueryCondition) field.get(condition);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取 QueryCondition 链路失败", exception);
        }
    }

    private static DefaultFundsInstructionLifecycleSaver lifecycleSaver(FundsTransaction transaction,
                                                                        FundsTransactionDetail detail,
                                                                        AtomicReference<FundsTransaction> updated) {
        return lifecycleSaver(transaction, detail, updated, new AtomicReference<>());
    }

    private static DefaultFundsInstructionLifecycleSaver lifecycleSaver(FundsTransaction transaction,
                                                                        FundsTransactionDetail detail,
                                                                        AtomicReference<FundsTransaction> updated,
                                                                        AtomicReference<FundsTransactionDetail> updatedDetail) {
        return new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction,
                        entity -> {
                            updated.set((FundsTransaction) entity);
                            return 1;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> detail,
                        entity -> {
                            updatedDetail.set((FundsTransactionDetail) entity);
                            return 1;
                        }
                )
        );
    }

    private static FundsTransactionDetail directDetail(String sn, RouteParticipantRole participantRole, long amount) {
        FundsTransactionDetail detail = detail(sn, participantRole);
        detail.setTransactionType(DefaultFundsTransactionType.TRANSFER);
        detail.setEventType(FundsTransactionEventType.TRANSFER);
        detail.setFundsEffectType(FundsEffectType.DIRECT);
        detail.setAmount(amount);
        return detail;
    }

    private static class SimpleInstruction implements FundsInstructionSpec {

        @Override
        public Long getTenantId() {
            return 1L;
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
        public @NonNull Money getAmount() {
            return Money.immutable(1_000L, CurrencyIsoCode.USD);
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
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "AUTH_BUSINESS_0001";
        }

        @Override
        public @NonNull LocalDateTime getEventTime() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @Nullable String getDescription() {
            return "auth";
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

    private static final class TransactionTypeChangedInstruction extends SimpleInstruction {

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.REFUND;
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

    private static final class ReferencedInstruction extends SimpleInstruction {

        private final FundsInstructionReferenceType referenceType;

        private ReferencedInstruction(FundsInstructionReferenceType referenceType) {
            this.referenceType = referenceType;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return new SimpleReference(referenceType);
        }
    }

    private static final class SimpleReference implements FundsInstructionReferenceSpec {

        private final FundsInstructionReferenceType referenceType;

        private SimpleReference(FundsInstructionReferenceType referenceType) {
            this.referenceType = referenceType;
        }

        @Override
        public @NonNull FundsInstructionReferenceType getReferenceType() {
            return referenceType;
        }

        @Override
        public @Nullable String getReferenceSn() {
            return "FT_001";
        }

        @Override
        public @Nullable String getReferenceBusinessSn() {
            return null;
        }

        @Override
        public @Nullable String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public @Nullable String getExternalTransactionId() {
            return null;
        }

        @Override
        public @Nullable String getAuthCode() {
            return null;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private static class SimpleResolvedRoute implements ResolvedRouteSpec {

        private final long amount;

        private final String replayRefLegId;

        private SimpleResolvedRoute(long amount) {
            this(amount, null);
        }

        private SimpleResolvedRoute(long amount, String replayRefLegId) {
            this.amount = amount;
            this.replayRefLegId = replayRefLegId;
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
                    new SimpleParticipant(RouteParticipantRole.AUTH_HOLDER, new SimpleSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT), amount),
                    new SimpleParticipant(RouteParticipantRole.PLATFORM_RESERVE, new SimpleSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT), amount)
            );
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return List.of(new SimpleLeg(amount, replayRefLegId));
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

    private static final class SharedCardResolvedRoute implements ResolvedRouteSpec {

        private final long amount;

        private SharedCardResolvedRoute(long amount) {
            this.amount = amount;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "CARD_AUTH_SHARED";
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
                    new SimpleParticipant(RouteParticipantRole.BUDGET_CONTROLLER,
                            new SimpleSubjectRef("budget_001", FundsSubjectType.BUDGET_GROUP), amount),
                    new SimpleParticipant(RouteParticipantRole.REAL_FUNDING_SOURCE,
                            new SimpleSubjectRef("funding_001", FundsSubjectType.FUNDING_ACCOUNT), amount)
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
                    .selectedReserveFund("RF_US_1")
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
                    .instrumentType("SHARE_VCC")
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
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(
                    new SimpleParticipant(RouteParticipantRole.AUTH_HOLDER,
                            new MetadataSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT, "credit",
                                    "CREDIT_BASIC"), 1_000L),
                    new SimpleParticipant(RouteParticipantRole.PLATFORM_RESERVE,
                            new MetadataSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT,
                                    "reserve", "FUNDING_PLATFORM"), 1_000L)
            );
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

        private final String replayRefLegId;

        private SimpleLeg(long amount) {
            this(amount, null);
        }

        private SimpleLeg(long amount, String replayRefLegId) {
            this.amount = amount;
            this.replayRefLegId = replayRefLegId;
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

        @Override
        public @Nullable String getReplayRefLegId() {
            return replayRefLegId;
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

package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsInstructionLifecycleSaverTests {

    @Test
    void testLifecycleSaverShouldSupportFundsTransactionEventsOnly() {
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction(), detail("FTD_001",
                RouteParticipantRole.AUTH_HOLDER), new AtomicReference<>());

        assertThat(saver.supports(new SimpleInstruction())).isTrue();
        assertThat(saver.supports(new BalanceControlInstruction(FundsTransactionEventType.FREEZE))).isFalse();
        assertThat(saver.supports(new BalanceControlInstruction(FundsTransactionEventType.UNFREEZE))).isFalse();
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
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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
                .containsExactlyInAnyOrder(RouteParticipantRole.AUTH_HOLDER, RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
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
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), null);

        assertThat(updatedDetail.get().getStatus()).isEqualTo(FundsTransactionDetailStatus.REJECTED);
        assertThat(updatedDetail.get().getLedgerTransactionSn()).isNull();
        assertThat(updatedTransaction.get().getDeclinedAmount()).isZero();
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.REJECTED);
    }

    /**
     * 场景：授权类资金交易成功后更新参与方明细和主交易汇总。
     * 输入：持卡主体明细、平台资金主体明细和账本交易号。
     * 输出：明细状态、明细账本交易号、主交易授权金额和状态。
     * 预期：所有明细成功并绑定同一账本交易号，主交易累计授权金额后保持 OPEN。
     * 红线：授权成功只更新交易生命周期汇总，不得伪造冻结单消费语义。
     */
    @Test
    void testMarkSucceededShouldUpdateDetailsAndTransactionSummary() {
        FundsTransaction transaction = transaction();
        FundsTransactionDetail detail = detail("FTD_001", RouteParticipantRole.AUTH_HOLDER);
        FundsTransactionDetail platformDetail = detail("FTD_002", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> updatedDetails = new ArrayList<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
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

    /**
     * 场景：共享卡授权包含持卡人、预算控制方和真实出资方三类主体视角。
     * 输入：三条同金额 HOLD 明细和一个账本交易号。
     * 输出：三条成功明细和主交易授权金额。
     * 预期：主交易授权金额只汇总一次，不因主体视角增多而重复累计。
     * 红线：多参与方明细不得放大真实交易金额。
     */
    @Test
    void testMarkSucceededShouldSummarizeSharedCardTransactionOnce() {
        FundsTransaction transaction = transaction();
        List<FundsTransactionDetail> details = List.of(
                detail("FTD_001", RouteParticipantRole.AUTH_HOLDER),
                detail("FTD_002", RouteParticipantRole.BUDGET_CONTROLLER),
                detail("FTD_003", RouteParticipantRole.REAL_FUNDING_SOURCE)
        );
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> updatedDetails = new ArrayList<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002", "FTD_003")), "LE_001");

        assertThat(updatedDetails).hasSize(3);
        assertThat(updatedDetails)
                .extracting(FundsTransactionDetail::getStatus)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED);
        assertThat(updatedTransaction.get().getAuthorizedAmount()).isEqualTo(1_000L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
    }

    /**
     * 场景：直接转账同时包含本金和手续费明细。
     * 输入：付款方、收款方本金明细和手续费接收方明细。
     * 输出：主交易 settledAmount、feeAmount 和终态。
     * 预期：本金进入 settledAmount，手续费进入 feeAmount，交易关闭。
     * 红线：手续费不得混入本金结算金额。
     */
    @Test
    void testMarkSucceededShouldSummarizeDirectTransactionAndFeeSeparately() {
        FundsTransaction transaction = transaction();
        transaction.setTransactionMode(FundsTransactionMode.DIRECT);
        transaction.setTransactionType(DefaultFundsTransactionType.TRANSFER);
        FundsTransactionDetail payerDetail = directDetail("FTD_001", RouteParticipantRole.PAYER, 1_000L);
        FundsTransactionDetail payeeDetail = directDetail("FTD_002", RouteParticipantRole.PAYEE, 1_000L);
        FundsTransactionDetail feeDetail = directDetail("FTD_003", RouteParticipantRole.FEE_RECEIVER, 30L);
        List<FundsTransactionDetail> details = List.of(payerDetail, payeeDetail, feeDetail);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002", "FTD_003")), "LE_001");

        assertThat(updatedTransaction.get().getSettledAmount()).isEqualTo(1_000L);
        assertThat(updatedTransaction.get().getFeeAmount()).isEqualTo(30L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    /**
     * 场景：独立收费交易只有手续费语义。
     * 输入：FEE 类型交易和两条 FEE_CHARGE 明细。
     * 输出：主交易 settledAmount、feeAmount 和终态。
     * 预期：settledAmount 保持 0，feeAmount 汇总收费金额，交易关闭。
     * 红线：独立费用不得被记作本金结算。
     */
    @Test
    void testMarkSucceededShouldSummarizeStandaloneFeeAsFeeAmount() {
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
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001", "FTD_002")), "LE_001");

        assertThat(updatedTransaction.get().getSettledAmount()).isZero();
        assertThat(updatedTransaction.get().getFeeAmount()).isEqualTo(30L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    /**
     * 场景：提现出款成功时指令携带冻结单引用。
     * 输入：WITHDRAW 交易、提现明细和 referenceType=FREEZE_ORDER 的指令。
     * 输出：提现明细状态、账本交易号、主交易结算金额和终态。
     * 预期：提现作为独立资金事实完成结算，不在生命周期汇总中改写冻结单。
     * 红线：冻结单引用只定位前置余额控制，不表达消费、扣划或跨主体转移。
     */
    @Test
    void testMarkSucceededShouldNotMutateReferencedFrozenOrderForWithdraw() {
        FundsTransaction transaction = directTransaction(DefaultFundsTransactionType.WITHDRAW);
        FundsTransactionDetail withdrawDetail = withdrawDetail("FTD_001", 60L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        AtomicReference<FundsTransactionDetail> updatedDetail = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, withdrawDetail, updatedTransaction,
                updatedDetail);

        saver.markSucceeded(new FreezeOrderReferencedWithdrawInstruction("FO_001", 60L),
                new FundsInstructionLifecycleResult()
                        .setTransactionSn("FT_001")
                        .setTransactionDetailSns(List.of("FTD_001")), "LE_001");

        assertThat(updatedDetail.get().getStatus()).isEqualTo(FundsTransactionDetailStatus.SUCCEEDED);
        assertThat(updatedDetail.get().getLedgerTransactionSn()).isEqualTo("LE_001");
        assertThat(updatedTransaction.get().getSettledAmount()).isEqualTo(60L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

}

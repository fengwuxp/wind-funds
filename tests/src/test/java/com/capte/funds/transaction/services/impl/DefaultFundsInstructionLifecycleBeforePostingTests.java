package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SharedCardResolvedRoute;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleResolvedRoute;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.TransactionTypeChangedInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.newLifecycleSaver;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.queryValues;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.transaction;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsInstructionLifecycleBeforePostingTests {

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
                .containsExactlyInAnyOrder(RouteParticipantRole.AUTH_HOLDER,
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
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
                        .contains("\"replayConsumedLegIds\":[\"SOURCE_LEG_001\"]")
                        .contains("\"replayConsumedLegAmounts\":{\"SOURCE_LEG_001\":1000}"));
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
}

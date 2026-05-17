package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.FreezeOrderReferencedWithdrawInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.detail;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.directDetail;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.directTransaction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.lifecycleSaver;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.newLifecycleSaver;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.rejectedAuthorizationDetail;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.transaction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.withdrawDetail;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsInstructionLifecycleSuccessSummaryTests {

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

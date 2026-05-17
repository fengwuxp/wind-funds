package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.directDetail;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.newLifecycleSaver;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.transaction;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsInstructionLifecycleFeeSummaryTests {

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
}

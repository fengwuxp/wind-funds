package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsInstructionLifecycleSaverReversalSummaryTests {

    /**
     * 场景：退款消耗最后一段已结算可回退金额。
     * 输入：已结算 1000、已退款 600、已拒付 200，再退款 200。
     * 输出：主交易 refundedAmount、declinedAmount 和状态。
     * 预期：退款金额累计到 800，交易可回退金额归零后关闭。
     * 红线：退款不得超过已结算且尚未退款/拒付的可回退余额。
     */
    @Test
    void testMarkSucceededShouldCloseWhenRefundConsumesRemainingSettledReversibleAmount() {
        FundsTransaction transaction = transaction();
        transaction.setStatus(FundsTransactionStatus.OPEN);
        transaction.setSettledAmount(1_000L);
        transaction.setRefundedAmount(600L);
        transaction.setDeclinedAmount(200L);
        FundsTransactionDetail refundDetail = returnDetail("FTD_001", FundsTransactionEventType.REFUND, 200L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, refundDetail, updatedTransaction);

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001");

        assertThat(updatedTransaction.get().getRefundedAmount()).isEqualTo(800L);
        assertThat(updatedTransaction.get().getDeclinedAmount()).isEqualTo(200L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    /**
     * 场景：拒付消耗最后一段已结算可回退金额。
     * 输入：已结算 1000、已退款 600、已拒付 200，再拒付 200。
     * 输出：主交易 refundedAmount、declinedAmount 和状态。
     * 预期：拒付金额累计到 400，交易可回退金额归零后关闭。
     * 红线：拒付不得直接修改账本事实，只能通过独立逆向资金事实累计交易视图。
     */
    @Test
    void testMarkSucceededShouldCloseWhenChargebackConsumesRemainingSettledReversibleAmount() {
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

        saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001");

        assertThat(updatedTransaction.get().getRefundedAmount()).isEqualTo(600L);
        assertThat(updatedTransaction.get().getDeclinedAmount()).isEqualTo(400L);
        assertThat(updatedTransaction.get().getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
    }

    /**
     * 场景：退款金额超过已结算可回退余额。
     * 输入：已结算 1000、已退款 700、已拒付 200，再退款 200。
     * 输出：余额不足异常。
     * 预期：拒绝更新主交易汇总。
     * 红线：退款不得透支已结算可回退金额。
     */
    @Test
    void testMarkSucceededShouldRejectRefundWhenSettledReversibleAmountInsufficient() {
        FundsTransaction transaction = transaction();
        transaction.setStatus(FundsTransactionStatus.OPEN);
        transaction.setSettledAmount(1_000L);
        transaction.setRefundedAmount(700L);
        transaction.setDeclinedAmount(200L);
        FundsTransactionDetail refundDetail = returnDetail("FTD_001", FundsTransactionEventType.REFUND, 200L);
        AtomicReference<FundsTransaction> updatedTransaction = new AtomicReference<>();
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction, refundDetail, updatedTransaction);

        assertThatThrownBy(() -> saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已结算可回退金额不足");
        assertThat(updatedTransaction).hasValue(null);
    }

    /**
     * 场景：拒付金额超过已结算可回退余额。
     * 输入：已结算 1000、已退款 700、已拒付 200，再拒付 200。
     * 输出：余额不足异常。
     * 预期：拒绝更新主交易汇总。
     * 红线：拒付不得透支已结算可回退金额。
     */
    @Test
    void testMarkSucceededShouldRejectChargebackWhenSettledReversibleAmountInsufficient() {
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

        assertThatThrownBy(() -> saver.markSucceeded(new SimpleInstruction(), new FundsInstructionLifecycleResult()
                .setTransactionSn("FT_001")
                .setTransactionDetailSns(List.of("FTD_001")), "LE_001"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已结算可回退金额不足");
        assertThat(updatedTransaction).hasValue(null);
    }
}

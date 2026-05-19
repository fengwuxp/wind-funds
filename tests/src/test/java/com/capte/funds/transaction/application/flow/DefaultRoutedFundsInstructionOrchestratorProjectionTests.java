package com.capte.funds.transaction.application.flow;

import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.projection.FundsTransactionProjectionPublishContext;
import com.capte.funds.transaction.projection.FundsTransactionProjectionPublisher;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易主链路投影入口服务层流程测试。
 */
@Import(DefaultRoutedFundsInstructionOrchestratorProjectionTests.Config.class)
class DefaultRoutedFundsInstructionOrchestratorProjectionTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private RecordingProjectionPublisher projectionPublisher;

    /**
     * 场景：真实资金服务链路完成充值和付款，付款事务提交后发布普通交易投影。
     * 输入：H2 中真实账户、账本、route、lifecycle saver、posting service 和 recording 投影发布端口。
     * 输出：投影上下文包含真实资金指令、route snapshot、完成态生命周期结果和账本交易流水。
     * 预期：普通交易投影入口由真实 Spring Bean 和 afterCommit 事务同步触发。
     * 红线：交易成功后不能缺失普通交易投影入口，且投影上下文不得停留在未完成生命周期结果。
     */
    @Test
    void testSuccessfulPostingShouldPublishProjectionAfterCommitWithCompletedLifecycle() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        topup(payer, 100L, "PROJECTION_SUCCESS_TOPUP");
        projectionPublisher.clear();

        String transactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "PROJECTION_SUCCESS_PAY");

        FundsTransactionDTO fundsTransaction = fundsTransaction(transactionSn);
        List<FundsTransactionDetailDTO> details = fundsTransactionDetails(transactionSn);
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_SUCCESS_PAY");

        assertThat(projectionPublisher.contexts()).singleElement().satisfies(context -> {
            assertThat(context.instruction().getBusinessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(context.resolvedRoute().getBusinessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(context.routeSnapshot().getBusinessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo(transactionSn);
            assertThat(context.lifecycleResult().getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(context.lifecycleResult().isCompleted()).isTrue();
            assertThat(context.ledgerTransaction()).isNotNull();
            assertThat(context.ledgerTransaction().getSn()).isEqualTo(ledgerTransaction.getSn());
        });
        assertThat(fundsTransaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(details).isNotEmpty()
                .allSatisfy(detail -> {
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.SUCCEEDED);
                    assertThat(detail.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
                });
        assertPostedTransactions(2);
    }

    /**
     * 场景：普通交易投影发布端口故障，但交易事实和账本事实已经成功提交。
     * 输入：测试态投影发布端口抛出运行时异常。
     * 输出：付款服务返回交易流水，交易、明细、账本交易、posting plan、entry 和余额变化均保留。
     * 预期：投影失败被隔离，后续由治理模块按交易投影重放修复只读视图。
     * 红线：普通投影失败不得回滚资金交易事实、账本事实或余额投影。
     */
    @Test
    void testProjectionFailureShouldNotRollbackCommittedFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_failure_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "PROJECTION_FAILURE_TOPUP");
        projectionPublisher.clear();
        projectionPublisher.failOnce();

        var beforePay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));

        String transactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "PROJECTION_FAILURE_PAY");

        var afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        FundsTransactionDTO fundsTransaction = fundsTransaction(transactionSn);
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_FAILURE_PAY");

        assertOnlyBalanceDeltas(beforePay, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(projectionPublisher.invocationCount()).isEqualTo(1);
        assertThat(projectionPublisher.contexts()).isEmpty();
        assertThat(fundsTransaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(ledgerTransaction.getFundsTransactionSn()).isEqualTo(transactionSn);
        assertThat(entriesOf(ledgerTransaction)).isNotEmpty();
        assertThat(postingPlansOf(ledgerTransaction)).isNotEmpty();
        assertPostedTransactions(2);
    }

    @AfterEach
    void clearProjectionPublisher() {
        projectionPublisher.clear();
    }

    @Configuration
    static class Config {

        @Bean
        RecordingProjectionPublisher recordingProjectionPublisher() {
            return new RecordingProjectionPublisher();
        }
    }

    static final class RecordingProjectionPublisher implements FundsTransactionProjectionPublisher {

        private final List<FundsTransactionProjectionPublishContext> contexts = new ArrayList<>();

        private final AtomicBoolean failNext = new AtomicBoolean();

        private int invocationCount;

        @Override
        public void publish(FundsTransactionProjectionPublishContext context) {
            invocationCount++;
            if (failNext.compareAndSet(true, false)) {
                throw new IllegalStateException("projection unavailable");
            }
            contexts.add(context);
        }

        private List<FundsTransactionProjectionPublishContext> contexts() {
            return contexts;
        }

        private int invocationCount() {
            return invocationCount;
        }

        private void failOnce() {
            failNext.set(true);
        }

        private void clear() {
            contexts.clear();
            failNext.set(false);
            invocationCount = 0;
        }
    }
}

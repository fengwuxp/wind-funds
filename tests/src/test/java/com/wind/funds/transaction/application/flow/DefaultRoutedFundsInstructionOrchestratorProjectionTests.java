package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.projection.FundsTransactionProjectionPublishContext;
import com.wind.funds.transaction.projection.FundsTransactionProjectionPublisher;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 交易主链路投影入口服务层流程测试。
 */
@Import(DefaultRoutedFundsInstructionOrchestratorProjectionTests.Config.class)
class DefaultRoutedFundsInstructionOrchestratorProjectionTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private RecordingProjectionPublisher projectionPublisher;

    private @NonNull FundsTransactionProjectionPublishContext singleProjectionContext() {
        List<FundsTransactionProjectionPublishContext> contexts = projectionPublisher.contexts();
        assertThat(contexts).hasSize(1);
        return contexts.getFirst();
    }

    /**
     * 场景：真实资金服务链路完成充值和付款，付款事务提交后发布普通交易投影。
     * 输入：H2 中真实账户、账本、route、lifecycle saver、posting service 和 recording 投影发布端口。
     * 输出：投影上下文包含真实资金指令、route snapshot、完成态生命周期结果、账本交易流水和只读解释摘要。
     * 预期：普通交易投影入口由真实 Spring Bean 和 afterCommit 事务同步触发。
     * 红线：交易成功后不能缺失普通交易投影入口，且投影上下文不得停留在未完成生命周期结果。
     */
    @Test
    void testSuccessfulPostingShouldPublishProjectionAfterCommitWithCompletedLifecycle() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_payee");
        ensureFundingAccount(payee, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        var beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "PROJECTION_SUCCESS_TOPUP");
        var afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        projectionPublisher.clear();

        String transactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "PROJECTION_SUCCESS_PAY");
        var afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        FundsTransactionDTO fundsTransaction = fundsTransaction(transactionSn);
        List<FundsTransactionDetailDTO> details = fundsTransactionDetails(transactionSn);
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_SUCCESS_PAY");

        assertThat(singleProjectionContext()).satisfies(context -> {
            assertThat(context.instruction().getBusinessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(context.resolvedRoute().getBusinessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(context.routeSnapshot().getBusinessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo(transactionSn);
            assertThat(context.lifecycleResult().getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(context.lifecycleResult().isCompleted()).isTrue();
            var explanation = context.explanation();
            assertThat(explanation.businessScene()).isEqualTo("PAY");
            assertThat(explanation.businessSn()).isEqualTo("PROJECTION_SUCCESS_PAY");
            assertThat(explanation.fundsTransactionSn()).isEqualTo(transactionSn);
            assertThat(explanation.routeSnapshotId()).isEqualTo(context.routeSnapshot().getSnapshotId());
            assertThat(explanation.routeCode()).isEqualTo(context.routeSnapshot().getRouteCode());
            assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(explanation.factStatus()).isEqualTo("POSTED");
            assertThat(explanation.displayStatus()).isEqualTo("SUCCEEDED");
            assertThat(explanation.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
            assertThat(explanation.statusMeaning()).isEqualTo("FUNDS_POSTED");
            assertThat(explanation.amountSource())
                    .isEqualTo("instructionAmount=70 USD, routeLegCount=1, routeSnapshot="
                            + context.routeSnapshot().getSnapshotId() + ", ledgerTransaction="
                            + ledgerTransaction.getSn());
            assertThat(explanation.failureReason()).isEqualTo("N/A");
            assertThat(explanation.unavailableReason()).isEqualTo("N/A");
            assertThat(explanation.nextAction()).isEqualTo("N/A");
            assertThat(explanation.evidenceRefs())
                    .contains("fundsTransaction:" + transactionSn,
                            "routeSnapshot:" + context.routeSnapshot().getSnapshotId(),
                            "ledgerTransaction:" + ledgerTransaction.getSn());
            assertThat(explanation.externalRuleVerificationStatus()).isEqualTo("N/A");
            var payload = explanation.payload();
            assertThat(payload)
                    .containsEntry("businessScene", "PAY")
                    .containsEntry("businessSn", "PROJECTION_SUCCESS_PAY")
                    .containsEntry("fundsTransactionSn", transactionSn)
                    .containsEntry("routeSnapshotId", context.routeSnapshot().getSnapshotId())
                    .containsEntry("routeCode", context.routeSnapshot().getRouteCode())
                    .containsEntry("ledgerTransactionSn", ledgerTransaction.getSn())
                    .containsEntry("factStatus", "POSTED")
                    .containsEntry("displayStatus", "SUCCEEDED")
                    .containsEntry("operationStatus", "NO_ACTION_REQUIRED")
                    .containsEntry("statusMeaning", "FUNDS_POSTED")
                    .containsEntry("amountSource", "instructionAmount=70 USD, routeLegCount=1, routeSnapshot="
                            + context.routeSnapshot().getSnapshotId() + ", ledgerTransaction="
                            + ledgerTransaction.getSn())
                    .containsEntry("failureReason", "N/A")
                    .containsEntry("unavailableReason", "N/A")
                    .containsEntry("nextAction", "N/A")
                    .containsEntry("externalRuleVerificationStatus", "N/A");
            assertThat(payload.get("evidenceRefs"))
                    .asList()
                    .contains("routeSnapshot:" + context.routeSnapshot().getSnapshotId());
        });
        assertThat(fundsTransaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(details).isNotEmpty()
                .allSatisfy(detail -> {
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.SUCCEEDED);
                    assertThat(detail.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
                });
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_SUCCESS_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_SUCCESS_PAY", 2, 2);
    }

    /**
     * 场景：真实资金服务链路完成充值和授权占用，授权事务提交后发布普通交易投影。
     * 输入：账户 AVAILABLE 余额 100，授权批准 60。
     * 输出：投影解释摘要标记为授权占用，并给出等待 capture 或 release 的下一动作。
     * 预期：授权占用不能展示成已完成消费结果。
     * 红线：交易投影不得把 AUTHORIZE 占用态误导为最终付款成功。
     */
    @Test
    void testAuthorizationHoldProjectionExplanationShouldNotDisplayAsFinalPaymentSuccess() {
        FundsAccountId user = fundingAccount("funding_user");
        var beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "PROJECTION_AUTH_TOPUP");
        var afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        projectionPublisher.clear();

        String authorizationSn = authorize(user, 60L, true, "PROJECTION_AUTH_AUTHORIZE");
        var afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_AUTH_AUTHORIZE");
        assertThat(singleProjectionContext()).satisfies(context -> {
            assertThat(context.instruction().getBusinessSn()).isEqualTo("PROJECTION_AUTH_AUTHORIZE");
            assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo(authorizationSn);
            assertThat(context.lifecycleResult().getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(context.lifecycleResult().isCompleted()).isTrue();
            var explanation = context.explanation();
            assertThat(explanation.businessScene()).isEqualTo("AUTHORIZATION");
            assertThat(explanation.businessSn()).isEqualTo("PROJECTION_AUTH_AUTHORIZE");
            assertThat(explanation.fundsTransactionSn()).isEqualTo(authorizationSn);
            assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(explanation.factStatus()).isEqualTo("HELD");
            assertThat(explanation.displayStatus()).isEqualTo("AUTHORIZED_HOLD");
            assertThat(explanation.operationStatus()).isEqualTo("WAITING_CAPTURE_OR_RELEASE");
            assertThat(explanation.statusMeaning()).isEqualTo("AUTHORIZATION_HELD_NOT_CAPTURED");
            assertThat(explanation.amountSource())
                    .isEqualTo("instructionAmount=60 USD, routeLegCount=1, routeSnapshot="
                            + context.routeSnapshot().getSnapshotId() + ", ledgerTransaction="
                            + ledgerTransaction.getSn());
            assertThat(explanation.unavailableReason())
                    .isEqualTo("AUTHORIZATION_HOLD_IS_NOT_FINAL_CONSUMPTION");
            assertThat(explanation.nextAction()).isEqualTo("WAIT_FOR_CAPTURE_OR_RELEASE");
            assertThat(explanation.payload())
                    .containsEntry("displayStatus", "AUTHORIZED_HOLD")
                    .containsEntry("operationStatus", "WAITING_CAPTURE_OR_RELEASE")
                    .containsEntry("nextAction", "WAIT_FOR_CAPTURE_OR_RELEASE");
        });
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_AUTH_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_AUTH_AUTHORIZE", 1, 2);
    }

    /**
     * 场景：风控或额度判断拒绝授权，授权事务提交后发布普通交易投影。
     * 输入：账户 AVAILABLE 余额 100，授权 approved=false，拒绝原因为 RISK_DECLINED。
     * 输出：投影解释摘要标记为拒绝，并保留业务层拒绝原因。
     * 预期：运营时间线可以解释失败阶段和失败原因。
     * 红线：授权拒绝不得只展示技术状态或 N/A 失败原因，也不得写入账本事实。
     */
    @Test
    void testAuthorizationDeclinedProjectionExplanationShouldExposeDeclineReason() {
        FundsAccountId user = fundingAccount("funding_user");
        var beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "PROJECTION_AUTH_DECLINE_TOPUP");
        var afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        var beforeDeclineFacts = ledgerFactSnapshot();
        projectionPublisher.clear();

        String authorizationSn = declineAuthorization(user, 60L, "RISK_DECLINED",
                "PROJECTION_AUTH_DECLINE");
        var afterDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeDeclineFacts);

        assertThat(singleProjectionContext()).satisfies(context -> {
            assertThat(context.instruction().getBusinessSn()).isEqualTo("PROJECTION_AUTH_DECLINE");
            assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo(authorizationSn);
            assertThat(context.lifecycleResult().getLedgerTransactionSn()).isNull();
            assertThat(context.lifecycleResult().isCompleted()).isTrue();
            assertThat(context.routeSnapshot().getLegs()).isEmpty();
            var explanation = context.explanation();
            assertThat(explanation.factStatus()).isEqualTo("REJECTED");
            assertThat(explanation.displayStatus()).isEqualTo("DECLINED");
            assertThat(explanation.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
            assertThat(explanation.statusMeaning()).isEqualTo("AUTHORIZATION_DECLINED_NO_FUNDS_POSTED");
            assertThat(explanation.amountSource())
                    .isEqualTo("instructionAmount=60 USD, routeLegCount=0, routeSnapshot="
                            + context.routeSnapshot().getSnapshotId() + ", ledgerTransaction=N/A");
            assertThat(explanation.failureReason()).isEqualTo("RISK_DECLINED");
            assertThat(explanation.unavailableReason()).isEqualTo("AUTHORIZATION_DECLINED");
            assertThat(explanation.nextAction()).isEqualTo("N/A");
            assertThat(explanation.evidenceRefs())
                    .contains("fundsTransaction:" + authorizationSn,
                            "routeSnapshot:" + context.routeSnapshot().getSnapshotId());
            assertThat(explanation.evidenceRefs())
                    .noneMatch(ref -> ref.startsWith("ledgerTransaction:"));
            assertThat(explanation.payload())
                    .containsEntry("displayStatus", "DECLINED")
                    .containsEntry("operationStatus", "NO_ACTION_REQUIRED")
                    .containsEntry("failureReason", "RISK_DECLINED")
                    .containsEntry("unavailableReason", "AUTHORIZATION_DECLINED");
        });
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_AUTH_DECLINE_TOPUP", 3, 4);
        assertThat(fundsTransactionsByBusinessSn("PROJECTION_AUTH_DECLINE"))
                .as("rejected funds transactions for businessSn PROJECTION_AUTH_DECLINE")
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getState()).isEqualTo(FundsTransactionState.REJECTED);
                    assertNoLedgerFactsForFundsTransaction(transaction.getSn());
                });
        assertThat(fundsTransactionDetailsByBusinessSn("PROJECTION_AUTH_DECLINE"))
                .as("rejected funds transaction details for businessSn PROJECTION_AUTH_DECLINE")
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                });
    }

    /**
     * 场景：真实资金服务链路完成充值和余额冻结，冻结事务提交后发布普通交易投影。
     * 输入：账户 AVAILABLE 余额 100，冻结 40。
     * 输出：投影解释摘要标记为冻结占用，并给出等待解冻或扣划的下一动作。
     * 预期：余额冻结不能展示成已完成消费结果。
     * 红线：交易投影不得把 FREEZE 控制态误导为最终付款成功。
     */
    @Test
    void testFreezeProjectionExplanationShouldNotDisplayAsFinalPaymentSuccess() {
        FundsAccountId user = fundingAccount("funding_user");
        var beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "PROJECTION_FREEZE_TOPUP");
        var afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        projectionPublisher.clear();

        String freezeSn = freeze(user, 40L, "PROJECTION_FREEZE_HOLD");
        var afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_FREEZE_HOLD");
        assertThat(singleProjectionContext()).satisfies(context -> {
            assertThat(context.instruction().getBusinessSn()).isEqualTo("PROJECTION_FREEZE_HOLD");
            assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo(freezeSn);
            assertThat(context.lifecycleResult().getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(context.lifecycleResult().isCompleted()).isTrue();
            var explanation = context.explanation();
            assertThat(explanation.businessScene()).isEqualTo("FREEZE");
            assertThat(explanation.businessSn()).isEqualTo("PROJECTION_FREEZE_HOLD");
            assertThat(explanation.fundsTransactionSn()).isEqualTo(freezeSn);
            assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(explanation.factStatus()).isEqualTo("HELD");
            assertThat(explanation.displayStatus()).isEqualTo("FROZEN");
            assertThat(explanation.operationStatus()).isEqualTo("WAITING_UNFREEZE_OR_CONSUME");
            assertThat(explanation.statusMeaning()).isEqualTo("BALANCE_FROZEN_NOT_CONSUMED");
            assertThat(explanation.amountSource())
                    .isEqualTo("instructionAmount=40 USD, routeLegCount=1, routeSnapshot="
                            + context.routeSnapshot().getSnapshotId() + ", ledgerTransaction="
                            + ledgerTransaction.getSn());
            assertThat(explanation.unavailableReason()).isEqualTo("BALANCE_FREEZE_IS_NOT_CONSUMPTION");
            assertThat(explanation.nextAction()).isEqualTo("WAIT_FOR_UNFREEZE_OR_CONSUME");
            assertThat(explanation.payload())
                    .containsEntry("displayStatus", "FROZEN")
                    .containsEntry("operationStatus", "WAITING_UNFREEZE_OR_CONSUME")
                    .containsEntry("nextAction", "WAIT_FOR_UNFREEZE_OR_CONSUME");
        });
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_FREEZE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("PROJECTION_FREEZE_HOLD", 0, 0, 1, 2);
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
        ensureFundingAccount(payee, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        var beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "PROJECTION_FAILURE_TOPUP");
        var afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        projectionPublisher.clear();
        projectionPublisher.failOnce();

        String transactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "PROJECTION_FAILURE_PAY");

        var afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        FundsTransactionDTO fundsTransaction = fundsTransaction(transactionSn);
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_FAILURE_PAY");
        List<LedgerPostingPlan> postingPlans = postingPlansOf(ledgerTransaction);
        List<LedgerEntry> entries = entriesOf(ledgerTransaction);

        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(projectionPublisher.invocationCount()).isEqualTo(1);
        assertThat(projectionPublisher.contexts()).isEmpty();
        assertThat(fundsTransaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(ledgerTransaction.getFundsTransactionSn()).isEqualTo(transactionSn);
        assertThat(ledgerTransaction.getEventType()).isEqualTo(FundsTransactionEventType.PAY.name());
        assertThat(ledgerTransaction.getBusinessSn()).isEqualTo("PROJECTION_FAILURE_PAY");
        assertThat(ledgerTransaction.getDebitAmount()).isEqualTo(40L);
        assertThat(ledgerTransaction.getCreditAmount()).isEqualTo(40L);
        assertThat(postingPlans).singleElement().satisfies(plan -> {
            assertThat(plan.getFundsTransactionSn()).isEqualTo(transactionSn);
            assertThat(plan.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(plan.getRouteLegId()).isEqualTo(FundsRouteLegIds.PAY);
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
            assertThat(plan.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME.name());
            assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT.name());
            assertThat(plan.getAmount()).isEqualTo(40L);
            assertThat(plan.getCurrency()).isEqualTo(CURRENCY);
            assertThat(plan.getDebitAmount()).isEqualTo(40L);
            assertThat(plan.getCreditAmount()).isEqualTo(40L);
        });
        String postingPlanSn = postingPlans.getFirst().getSn();
        assertThat(entries).hasSize(2).allSatisfy(entry -> {
            assertThat(entry.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
            assertThat(entry.getPostingPlanSn()).isEqualTo(postingPlanSn);
            assertThat(entry.getFundsTransactionSn()).isEqualTo(transactionSn);
            assertThat(entry.getBusinessSn()).isEqualTo("PROJECTION_FAILURE_PAY");
            assertThat(entry.getAmount()).isEqualTo(40L);
            assertThat(entry.getCurrency()).isEqualTo(CURRENCY);
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
            assertThat(entry.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME.name());
            assertThat(entry.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT.name());
        });
        assertThat(entries)
                .extracting(LedgerEntry::getSubjectId, LedgerEntry::getLedgerSubjectCode, LedgerEntry::getEntrySide)
                .containsExactlyInAnyOrder(
                        tuple(payer.id(), LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT),
                        tuple(payee.id(), LedgerSubjectCode.SETTLEMENT, EntrySide.CREDIT));
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_FAILURE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("PROJECTION_FAILURE_PAY", 2, 2);
    }

    /**
     * 场景：UNFREEZE 已提交，但事务后投影发布失败，随后仅从解冻事实恢复本次路径。
     * 输入：账户先充值并冻结 40，再解冻 15；解冻后的投影发布端口抛出异常。
     * 输出：解冻记录仍能恢复 UNFREEZE RouteSnapshot，读取过程不改写账本事实或余额。
     * 预期：发布失败不丢失解冻路径，后续投影恢复可以从持久化事实重新读取。
     * 红线：不得回退到原 FREEZE 路径伪装本次 UNFREEZE，也不得靠内存发布上下文恢复。
     */
    @Test
    void testUnfreezeRouteShouldRemainRecoverableFromReleaseFactAfterPublisherFailure() {
        FundsAccountId user = fundingAccount("funding_user");
        var beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "PROJECTION_UNFREEZE_TOPUP");
        var afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 40L, "PROJECTION_UNFREEZE_FREEZE");
        var afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        projectionPublisher.clear();
        projectionPublisher.failOnce();

        unfreeze(user, 15L, freezeSn, "PROJECTION_UNFREEZE_RELEASE");
        var afterUnfreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterUnfreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, 15L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -15L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(projectionPublisher.invocationCount()).isEqualTo(1);
        assertThat(projectionPublisher.contexts()).isEmpty();

        var releaseOrder = frozenOrderByBusinessSn("PROJECTION_UNFREEZE_RELEASE");
        var beforeReadFacts = ledgerFactSnapshot();
        var recovered = fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(releaseOrder.getSn());

        assertThat(recovered).isPresent();
        assertLedgerTransactionFactsUnchanged(beforeReadFacts);
        assertThat(snapshot(balances(user, cashMappingAccount(), prepaymentAccount())))
                .isEqualTo(afterUnfreeze);
        assertThat(recovered.orElseThrow()).satisfies(routeSnapshot -> {
            assertThat(routeSnapshot.getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
            assertThat(routeSnapshot.getBusinessSn()).isEqualTo("PROJECTION_UNFREEZE_RELEASE");
            assertThat(routeSnapshot.getLegs()).singleElement().satisfies(routeLeg -> {
                assertThat(routeLeg.getLegType()).isEqualTo(RouteLegType.RELEASE);
            });
        });
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

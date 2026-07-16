package com.wind.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainQuery;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.core.WritableContextVariables;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 交易投影解释查询服务流程测试。
 */
class FundsTransactionProjectionExplainApplicationServiceTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsTransactionProjectionExplainApplicationService projectionExplainApplicationService;

    /**
     * 场景：普通付款交易已经成功入账，运营或账单侧按资金交易流水查询投影解释。
     * 输入：用户充值 100 后向收款方付款 70。
     * 输出：解释摘要从落库交易事实、RouteSnapshot 和交易明细生成 POSTED/SUCCEEDED 口径。
     * 预期：查询只读，不生成新的交易、账本或余额事实。
     * 红线：交易投影解释不得反写资金交易、账本交易、分录或余额投影。
     */
    @Test
    void testPostedFundsTransactionShouldExplainFromPersistedFactsWithoutSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_explain_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "PROJECTION_EXPLAIN_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String transactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "PROJECTION_EXPLAIN_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_EXPLAIN_PAY");
        LedgerFactSnapshot beforeExplainFacts = ledgerFactSnapshot();

        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(transactionSn)
                        .build());

        assertThat(explanation.businessScene()).isEqualTo("PAY");
        assertThat(explanation.businessSn()).isEqualTo("PROJECTION_EXPLAIN_PAY");
        assertThat(explanation.fundsTransactionSn()).isEqualTo(transactionSn);
        assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
        assertThat(explanation.factStatus()).isEqualTo("POSTED");
        assertThat(explanation.displayStatus()).isEqualTo("SUCCEEDED");
        assertThat(explanation.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
        assertThat(explanation.statusMeaning()).isEqualTo("FUNDS_POSTED");
        assertThat(explanation.amountSource())
                .isEqualTo("instructionAmount=70 USD, routeLegCount=1, routeSnapshot="
                        + explanation.routeSnapshotId() + ", ledgerTransaction=" + ledgerTransaction.getSn());
        assertThat(explanation.failureReason()).isEqualTo("N/A");
        assertThat(explanation.unavailableReason()).isEqualTo("N/A");
        assertThat(explanation.nextAction()).isEqualTo("N/A");
        assertThat(explanation.evidenceRefs())
                .contains("fundsTransaction:" + transactionSn,
                        "routeSnapshot:" + explanation.routeSnapshotId(),
                        "ledgerTransaction:" + ledgerTransaction.getSn());
        assertThat(explanation.payload())
                .containsEntry("displayStatus", "SUCCEEDED")
                .containsEntry("operationStatus", "NO_ACTION_REQUIRED")
                .containsEntry("statusMeaning", "FUNDS_POSTED");
        assertLedgerTransactionFactsUnchanged(beforeExplainFacts);
        assertThat(snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount())))
                .isEqualTo(afterPay);
    }

    /**
     * 场景：授权被风控或额度规则拒绝后，运营侧按交易流水查询失败原因。
     * 输入：授权 approved=false，拒绝原因为 RISK_DECLINED。
     * 输出：解释摘要展示 REJECTED/DECLINED，失败原因来自已保存交易明细上下文。
     * 预期：授权拒绝仍可被解释，但不得展示为已入账或待 capture。
     * 红线：授权拒绝不得生成账本事实，也不得在投影中误导为成功资金结果。
     */
    @Test
    void testRejectedAuthorizationShouldExplainDeclineReasonFromPersistedDetail() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "PROJECTION_EXPLAIN_AUTH_TOPUP");
        LedgerFactSnapshot beforeDeclineFacts = ledgerFactSnapshot();

        String authorizationSn = declineAuthorization(user, 60L, "RISK_DECLINED",
                "PROJECTION_EXPLAIN_AUTH_DECLINE");

        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(authorizationSn)
                        .build());

        assertThat(explanation.businessScene()).isEqualTo("AUTHORIZATION");
        assertThat(explanation.businessSn()).isEqualTo("PROJECTION_EXPLAIN_AUTH_DECLINE");
        assertThat(explanation.fundsTransactionSn()).isEqualTo(authorizationSn);
        assertThat(explanation.ledgerTransactionSn()).isNull();
        assertThat(explanation.factStatus()).isEqualTo("REJECTED");
        assertThat(explanation.displayStatus()).isEqualTo("DECLINED");
        assertThat(explanation.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
        assertThat(explanation.statusMeaning()).isEqualTo("AUTHORIZATION_DECLINED_NO_FUNDS_POSTED");
        assertThat(explanation.failureReason()).isEqualTo("RISK_DECLINED");
        assertThat(explanation.unavailableReason()).isEqualTo("AUTHORIZATION_DECLINED");
        assertThat(explanation.evidenceRefs())
                .contains("fundsTransaction:" + authorizationSn,
                        "routeSnapshot:" + explanation.routeSnapshotId());
        assertThat(explanation.evidenceRefs())
                .noneMatch(ref -> ref.startsWith("ledgerTransaction:"));
        assertLedgerTransactionFactsUnchanged(beforeDeclineFacts);
        assertNoLedgerFactsForFundsTransaction(authorizationSn);
    }

    /**
     * 场景：已完成授权发生外部争议，资金结果通过 settleRefund 承接。
     * 输入：授权 60、完成 60、争议退款 40，并携带 dispute 审计字段。
     * 输出：解释摘要必须展示 DISPUTE_REFUND 语义，并暴露外部争议引用。
     * 预期：争议退款可与普通退款、无授权退款区分。
     * 红线：投影解释不得把带 dispute 上下文的 AUTH_REFUND 退化为普通 FUNDS_POSTED。
     */
    @Test
    void testDisputeAuthorizationRefundShouldExplainDisputeContextFromPersistedFacts() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "PROJECTION_EXPLAIN_DISPUTE_TOPUP");
        String authorizationSn = authorize(user, 60L, true, "PROJECTION_EXPLAIN_DISPUTE_AUTHORIZE");
        settleAuthorization(user, 60L, authorizationSn, "PROJECTION_EXPLAIN_DISPUTE_CAPTURE");
        String refundSn = authorizationTransactionService.settleRefund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_PROJECTION_202606180001")
                .setExternalDisputeRef("DISPUTE_CASE_PROJECTION_202606180001")
                .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                .setBusinessSn("PROJECTION_EXPLAIN_DISPUTE_RETURN")
                .setDescription("authorization dispute refund")
                .setContextVariables(WritableContextVariables.of(Map.of(
                        "caseOwner", "ops-team-a"))), WindOperator.system());
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_EXPLAIN_DISPUTE_RETURN");
        LedgerFactSnapshot beforeExplainFacts = ledgerFactSnapshot();

        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(refundSn)
                        .build());

        assertThat(explanation.businessScene()).isEqualTo("AUTHORIZATION_DISPUTE_REFUND");
        assertThat(explanation.businessSn()).isEqualTo("PROJECTION_EXPLAIN_DISPUTE_RETURN");
        assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
        assertThat(explanation.factStatus()).isEqualTo("POSTED");
        assertThat(explanation.displayStatus()).isEqualTo("DISPUTE_REFUNDED");
        assertThat(explanation.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
        assertThat(explanation.statusMeaning()).isEqualTo("DISPUTE_REFUND_POSTED");
        assertThat(explanation.failureReason()).isEqualTo("N/A");
        assertThat(explanation.unavailableReason()).isEqualTo("N/A");
        assertThat(explanation.nextAction()).isEqualTo("N/A");
        assertThat(explanation.evidenceRefs())
                .contains("fundsTransaction:" + refundSn,
                        "ledgerTransaction:" + ledgerTransaction.getSn(),
                        "externalDisputeRef:DISPUTE_CASE_PROJECTION_202606180001",
                        "disputeVoucherRef:DISPUTE_EVIDENCE_PROJECTION_202606180001");
        assertThat(explanation.payload())
                .containsEntry("displayStatus", "DISPUTE_REFUNDED")
                .containsEntry("statusMeaning", "DISPUTE_REFUND_POSTED")
                .containsEntry(FundsInstructionContextKeys.REFUND_MODE,
                        FundsInstructionContextKeys.REFUND_MODE_DISPUTE)
                .containsEntry(FundsInstructionContextKeys.DISPUTE_MODE, "CHARGEBACK")
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF,
                        "DISPUTE_CASE_PROJECTION_202606180001");
        assertLedgerTransactionFactsUnchanged(beforeExplainFacts);
    }

    /**
     * 场景：普通直接交易退款已经成功入账，运营侧按退款资金交易流水查询投影解释。
     * 输入：付款 70 后退款 30。
     * 输出：解释摘要展示 REFUNDED/FUNDS_REFUNDED，而不是普通付款成功口径。
     * 预期：普通退款可与付款、争议退款和无授权退款区分。
     * 红线：投影解释不得把 REFUND 事件退化为 SUCCEEDED/FUNDS_POSTED。
     */
    @Test
    void testDirectRefundShouldExplainRefundedStatusFromPersistedFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_refund_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "PROJECTION_EXPLAIN_REFUND_TOPUP");
        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "PROJECTION_EXPLAIN_REFUND_PAY");

        refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 30L, "PROJECTION_EXPLAIN_REFUND_RETURN");
        String refundSn = fundsTransactionsByBusinessSn("PROJECTION_EXPLAIN_REFUND_RETURN").getFirst().getSn();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PROJECTION_EXPLAIN_REFUND_RETURN");
        LedgerFactSnapshot beforeExplainFacts = ledgerFactSnapshot();
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));

        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(refundSn)
                        .build());

        assertThat(explanation.businessScene()).isEqualTo("REFUND");
        assertThat(explanation.businessSn()).isEqualTo("PROJECTION_EXPLAIN_REFUND_RETURN");
        assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
        assertThat(explanation.factStatus()).isEqualTo("POSTED");
        assertThat(explanation.displayStatus()).isEqualTo("REFUNDED");
        assertThat(explanation.statusMeaning()).isEqualTo("FUNDS_REFUNDED");
        assertThat(explanation.evidenceRefs())
                .contains("fundsTransaction:" + refundSn,
                        "ledgerTransaction:" + ledgerTransaction.getSn());
        assertThat(explanation.payload())
                .containsEntry("displayStatus", "REFUNDED")
                .containsEntry("statusMeaning", "FUNDS_REFUNDED");
        assertLedgerTransactionFactsUnchanged(beforeExplainFacts);
        assertThat(snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount())))
                .isEqualTo(afterRefund);
    }

    /**
     * 场景：外部 capture 没有内部授权记录，但后续需要按外部引用入账退款。
     * 输入：外部已扣款事实通过无授权退款返回 40，并携带外部引用和退款原因。
     * 输出：解释摘要展示 NO_AUTH_REFUNDED，并透出外部原始事实引用。
     * 预期：无授权退款不被误读为普通授权链退款或争议退款。
     * 红线：投影解释不得丢失 no-auth refund 的外部追溯字段。
     */
    @Test
    void testNoAuthRefundShouldExplainExternalReferenceFromPersistedFacts() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "PROJECTION_EXPLAIN_NO_AUTH_REFUND_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "PROJECTION_EXPLAIN_NO_AUTH_REFUND_CAPTURE");

        String refundSn = refundWithoutAuthorization(user, 40L,
                "PROJECTION_EXPLAIN_NO_AUTH_REFUND_RETURN");
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(
                "PROJECTION_EXPLAIN_NO_AUTH_REFUND_RETURN");
        LedgerFactSnapshot beforeExplainFacts = ledgerFactSnapshot();

        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(refundSn)
                        .build());

        assertThat(explanation.businessScene()).isEqualTo("AUTHORIZATION_NO_AUTH_REFUND");
        assertThat(explanation.businessSn()).isEqualTo("PROJECTION_EXPLAIN_NO_AUTH_REFUND_RETURN");
        assertThat(explanation.ledgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
        assertThat(explanation.factStatus()).isEqualTo("POSTED");
        assertThat(explanation.displayStatus()).isEqualTo("NO_AUTH_REFUNDED");
        assertThat(explanation.statusMeaning()).isEqualTo("NO_AUTH_REFUND_POSTED");
        assertThat(explanation.evidenceRefs())
                .contains("fundsTransaction:" + refundSn,
                        "ledgerTransaction:" + ledgerTransaction.getSn(),
                        "externalReferenceSn:processor_capture_202606030001");
        assertThat(explanation.payload())
                .containsEntry("displayStatus", "NO_AUTH_REFUNDED")
                .containsEntry("statusMeaning", "NO_AUTH_REFUND_POSTED")
                .containsEntry(FundsInstructionContextKeys.REFUND_MODE,
                        FundsInstructionContextKeys.REFUND_MODE_NO_AUTH)
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_REFERENCE_SN,
                        "processor_capture_202606030001")
                .containsEntry(FundsInstructionContextKeys.REFUND_REASON,
                        "external capture refunded without internal authorization");
        assertLedgerTransactionFactsUnchanged(beforeExplainFacts);
    }

    /**
     * 场景：历史交易事实存在，但 RouteSnapshot 被破坏或缺失。
     * 输入：成功付款后清空该资金交易的 RouteSnapshot。
     * 输出：投影解释 fail-fast，要求治理重放或人工修复来源事实。
     * 预期：缺快照时不得根据当前关系重新选路或伪造投影。
     * 红线：投影解释不能在缺少原路径快照时继续展示为可信资金结果。
     */
    @Test
    void testMissingRouteSnapshotShouldFailFastWithoutRebuildingProjection() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_missing_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "PROJECTION_EXPLAIN_MISSING_TOPUP");
        String transactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "PROJECTION_EXPLAIN_MISSING_PAY");
        LedgerFactSnapshot beforeExplainFacts = ledgerFactSnapshot();

        clearFundsTransactionRouteSnapshot(transactionSn);

        assertThatThrownBy(() -> projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(transactionSn)
                        .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RouteSnapshot");
        assertLedgerTransactionFactsUnchanged(beforeExplainFacts);
    }
}

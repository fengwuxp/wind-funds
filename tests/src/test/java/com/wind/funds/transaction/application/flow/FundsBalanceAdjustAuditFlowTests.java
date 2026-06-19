package com.wind.funds.transaction.application.flow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.spec.SourceObjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 余额调账审计业务流测试。
 */
class FundsBalanceAdjustAuditFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：外部钱包或发卡处理商已经形成终局余额事实，我侧需要将同一资金账户可用余额纠偏为负。
     * 输入：用户已有 50 可用余额，外部终局事实要求减少 80，并携带来源、审批、证据、责任、对账和负余额策略。
     * 输出：同主体 AVAILABLE 被纠偏到 -30，平台调账挂账户增加 80，交易明细保留审计上下文。
     * 预期：受控负可用只在账本 profile 允许负余额且请求审计字段齐全时成立。
     * 红线：不能把外部异常包装成普通消费透支，不能缺少外部终局事件、余额快照、责任或对账回链。
     */
    @Test
    void testExternalBalanceAnomalyAdjustCanCreateControlledNegativeAvailableWithAuditFacts() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId adjustmentAccount = fundingAccount("platform_adjustment");
        allowNegativeLedger(user, LedgerSubjectCode.AVAILABLE);
        ensureLedger(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT);
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), adjustmentAccount));

        topup(user, 50L, "BALANCE_ADJUST_AUDIT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), adjustmentAccount));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY));

        String businessSn = "BALANCE_ADJUST_EXTERNAL_ANOMALY";
        balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user, businessSn), WindOperator.system());

        BalanceSnapshot afterAdjust = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), adjustmentAccount));
        assertOnlyBalanceDeltas(afterTopup, afterAdjust,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 80L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, -30L, CURRENCY);
        assertBucket(balance(adjustmentAccount), LedgerSubjectCode.ADJUSTMENT, 80L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_ADJUST_AUDIT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, 2, 1, 2);
        assertLedgerFactsFollowRouteSnapshot(businessSn);
        fundsTransactionDetailsByBusinessSn(businessSn).forEach(this::assertExternalAnomalyAuditContext);
        assertExternalAnomalyAuditContextInRouteSnapshot(businessSn);
    }

    /**
     * 场景：外部余额异常纠偏缺少终局外部事实、余额快照、对账差错或责任归属。
     * 输入：分别提交缺少外部终局事件、缺少外部余额快照、缺少对账差错和缺少责任引用的请求。
     * 输出：请求在生成资金事实前被拒绝，余额和账务事实均不变化。
     * 预期：外部异常纠偏比普通调账多一层终局事实与对账回链门禁。
     * 红线：pending、accepted、processing、人工备注或无证据外部差异不得入账。
     */
    @Test
    void testExternalBalanceAnomalyAdjustWithoutRequiredEvidenceShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_FINAL_EVENT")
                .setExternalFinalEventRef(null), WindOperator.system()))
                .hasMessageContaining("外部余额异常纠偏缺少外部终局事件引用");
        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_SNAPSHOT")
                .setExternalBalanceSnapshotRef(null), WindOperator.system()))
                .hasMessageContaining("外部余额异常纠偏缺少外部余额快照引用");
        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_RECON")
                .setReconciliationExceptionRef(null), WindOperator.system()))
                .hasMessageContaining("外部余额异常纠偏缺少对账差错引用");
        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_RESPONSIBILITY")
                .setResponsibilityRef(null), WindOperator.system()))
                .hasMessageContaining("外部余额异常纠偏缺少责任归属引用");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_EXTERNAL_MISSING_FINAL_EVENT");
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_EXTERNAL_MISSING_SNAPSHOT");
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_EXTERNAL_MISSING_RECON");
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_EXTERNAL_MISSING_RESPONSIBILITY");
    }

    private void assertExternalAnomalyAuditContext(FundsTransactionDetail detail) {
        JSONObject values = JSON.parseObject(detail.getContextVariables());
        assertThat(values.getString(FundsInstructionContextKeys.SOURCE_TYPE))
                .isEqualTo(SourceObjectType.EXTERNAL_BALANCE_ANOMALY.name());
        assertThat(values.getString(FundsInstructionContextKeys.SOURCE_SN))
                .isEqualTo("EXT_BALANCE_ANOMALY_202606170001");
        assertThat(values.getString(FundsInstructionContextKeys.EXTERNAL_FINAL_EVENT_REF))
                .isEqualTo("ISSUER_FINAL_EVENT_202606170001");
        assertThat(values.getString(FundsInstructionContextKeys.EXTERNAL_BALANCE_SNAPSHOT_REF))
                .isEqualTo("ISSUER_BALANCE_SNAPSHOT_202606170001");
        assertThat(values.getString(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF))
                .isEqualTo("RECON_DIFF_202606170001");
        assertThat(values.getString(FundsInstructionContextKeys.RECONCILIATION_RERUN_REF))
                .isEqualTo("RECON_RERUN_202606170001");
        assertThat(values.getString(FundsInstructionContextKeys.RESPONSIBILITY_REF))
                .isEqualTo("RECOVERY_CASE_202606170001");
        assertThat(values.getString(FundsInstructionContextKeys.REASON_CODE))
                .isEqualTo("EXTERNAL_TERMINAL_BALANCE_DEFICIT");
        assertThat(values.getBoolean(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE)).isTrue();
        assertThat(detail.getContextVariables())
                .contains(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT)
                .contains(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT);
    }

    private void assertExternalAnomalyAuditContextInRouteSnapshot(String businessSn) {
        String transactionSn = fundsTransactionsByBusinessSn(businessSn).getFirst().getSn();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transactionSn))
                .as("balance adjust route snapshot must carry audit backlinks for businessSn %s", businessSn)
                .hasValueSatisfying(this::assertExternalAnomalyAuditRouteContext);
    }

    private void assertExternalAnomalyAuditRouteContext(RouteSnapshotSpec routeSnapshot) {
        assertThat(routeSnapshot.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.SOURCE_TYPE,
                        SourceObjectType.EXTERNAL_BALANCE_ANOMALY.name())
                .containsEntry(FundsInstructionContextKeys.SOURCE_SN,
                        "EXT_BALANCE_ANOMALY_202606170001")
                .containsEntry(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF,
                        "EVIDENCE_EXTERNAL_BALANCE_ANOMALY_202606170001")
                .containsEntry(FundsInstructionContextKeys.APPROVAL_REF,
                        "APPROVAL_EXTERNAL_BALANCE_ANOMALY_202606170001")
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_FINAL_EVENT_REF,
                        "ISSUER_FINAL_EVENT_202606170001")
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_BALANCE_SNAPSHOT_REF,
                        "ISSUER_BALANCE_SNAPSHOT_202606170001")
                .containsEntry(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF,
                        "RECON_DIFF_202606170001")
                .containsEntry(FundsInstructionContextKeys.RECONCILIATION_RERUN_REF,
                        "RECON_RERUN_202606170001")
                .containsEntry(FundsInstructionContextKeys.RESPONSIBILITY_REF,
                        "RECOVERY_CASE_202606170001")
                .containsEntry(FundsInstructionContextKeys.REASON_CODE,
                        "EXTERNAL_TERMINAL_BALANCE_DEFICIT")
                .containsEntry(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE, Boolean.TRUE)
                .containsEntry(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE,
                        "EXT_DEFICIT_RECOVERY")
                .containsEntry(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS,
                        "RECOVERY_PENDING")
                .doesNotContainKey(FundsInstructionContextKeys.EXTERNAL_ACCOUNT_REF);
    }

    private static FundsBalanceAdjustRequest externalBalanceAnomalyAdjustRequest(FundsAccountId accountId,
                                                                                 String businessSn) {
        return new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(80L, CURRENCY))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("EXTERNAL_BALANCE_ANOMALY")
                .setBusinessSn(businessSn)
                .setSourceType(SourceObjectType.EXTERNAL_BALANCE_ANOMALY)
                .setSourceSn("EXT_BALANCE_ANOMALY_202606170001")
                .setReasonCode("EXTERNAL_TERMINAL_BALANCE_DEFICIT")
                .setExternalInstitutionRef("ISSUER_HN")
                .setExternalAccountRef("VCC_ACCOUNT_TOKEN_001")
                .setExternalFinalEventRef("ISSUER_FINAL_EVENT_202606170001")
                .setExternalBalanceSnapshotRef("ISSUER_BALANCE_SNAPSHOT_202606170001")
                .setReconciliationExceptionRef("RECON_DIFF_202606170001")
                .setReconciliationRerunRef("RECON_RERUN_202606170001")
                .setResponsibilityRef("RECOVERY_CASE_202606170001")
                .setAdjustReason("external issuer terminal balance deficit")
                .setAdjustEvidenceRef("EVIDENCE_EXTERNAL_BALANCE_ANOMALY_202606170001")
                .setApprovalRef("APPROVAL_EXTERNAL_BALANCE_ANOMALY_202606170001")
                .setAllowNegativeBalance(Boolean.TRUE)
                .setNegativeAvailablePolicyCode("EXT_DEFICIT_RECOVERY")
                .setNegativeAvailableRiskStatus("RECOVERY_PENDING")
                .setNegativeAvailableSingleLimit(Money.immutable(100L, CURRENCY))
                .setNegativeAvailableCumulativeLimit(Money.immutable(500L, CURRENCY))
                .setNegativeAvailableAgingStartedAt(LocalDateTime.of(2026, 6, 17, 10, 0))
                .setDescription("external terminal balance anomaly adjustment");
    }
}

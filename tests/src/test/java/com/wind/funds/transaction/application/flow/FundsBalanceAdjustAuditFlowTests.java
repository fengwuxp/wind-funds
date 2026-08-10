package com.wind.funds.transaction.application.flow;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.SourceObjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.application.FundsBalanceAdjustmentAuditApplicationService;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.enums.FundsBalanceAdjustmentAuditCompleteness;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.model.dto.FundsBalanceAdjustmentAuditDTO;
import com.wind.funds.transaction.model.query.FundsBalanceAdjustmentAuditQuery;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.jackson.WindJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;

import tools.jackson.core.type.TypeReference;

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

    @Autowired
    private FundsBalanceAdjustmentAuditApplicationService balanceAdjustmentAuditApplicationService;

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
        balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user, businessSn), WindOperatorFactory.system());

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
                .setExternalFinalEventRef(null), WindOperatorFactory.system()))
                .hasMessageContaining("外部余额异常纠偏缺少外部终局事件引用");
        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_SNAPSHOT")
                .setExternalBalanceSnapshotRef(null), WindOperatorFactory.system()))
                .hasMessageContaining("外部余额异常纠偏缺少外部余额快照引用");
        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_RECON")
                .setReconciliationExceptionRef(null), WindOperatorFactory.system()))
                .hasMessageContaining("外部余额异常纠偏缺少对账差错引用");
        assertThatThrownBy(() -> balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user,
                "BALANCE_ADJUST_EXTERNAL_MISSING_RESPONSIBILITY")
                .setResponsibilityRef(null), WindOperatorFactory.system()))
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

    /**
     * 场景：运营或对账人员需要按业务号或资金交易号追溯外部余额异常纠偏的审计链路。
     * 输入：已完成的外部余额异常余额调账交易。
     * 输出：查询结果返回交易事实、RouteSnapshot 回链、账本交易与分录摘要、过滤后的审计上下文。
     * 预期：查询为只读聚合能力，多次查询不新增或修改账本事实、交易事实和余额投影。
     * 红线：不得把敏感外部账户引用暴露到审计查询结果，不得为了查询审计补写 route、ledger 或 projection。
     */
    @Test
    void testExternalBalanceAnomalyAdjustAuditCanBeQueriedWithoutSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId adjustmentAccount = fundingAccount("platform_adjustment");
        allowNegativeLedger(user, LedgerSubjectCode.AVAILABLE);
        ensureLedger(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT);
        topup(user, 50L, "BALANCE_ADJUST_AUDIT_QUERY_TOPUP");

        String businessScene = "EXTERNAL_BALANCE_ANOMALY";
        String businessSn = "BALANCE_ADJUST_EXTERNAL_AUDIT_QUERY";
        balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user, businessSn), WindOperatorFactory.system());
        String transactionSn = fundsTransactionsByBusinessSn(businessSn).getFirst().getSn();
        BalanceSnapshot beforeAuditBalance = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(),
                adjustmentAccount));
        LedgerFactSnapshot beforeAuditFacts = ledgerFactSnapshot();

        FundsBalanceAdjustmentAuditDTO byBusinessSn = balanceAdjustmentAuditApplicationService.findByBusinessSn(
                        new FundsBalanceAdjustmentAuditQuery()
                                .setTenantId(TENANT_ID)
                                .setBusinessScene(businessScene)
                                .setBusinessSn(businessSn))
                .orElseThrow();
        FundsBalanceAdjustmentAuditDTO byTransactionSn = balanceAdjustmentAuditApplicationService.findByTransactionSn(
                        new FundsBalanceAdjustmentAuditQuery()
                                .setTenantId(TENANT_ID)
                                .setFundsTransactionSn(transactionSn))
                .orElseThrow();

        assertThat(byBusinessSn).usingRecursiveComparison().isEqualTo(byTransactionSn);
        assertThat(byBusinessSn.getAuditCompleteness())
                .isEqualTo(FundsBalanceAdjustmentAuditCompleteness.COMPLETE);
        assertThat(byBusinessSn.getFundsTransactionSn()).isEqualTo(transactionSn);
        assertThat(byBusinessSn.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(byBusinessSn.getBusinessScene()).isEqualTo(businessScene);
        assertThat(byBusinessSn.getBusinessSn()).isEqualTo(businessSn);
        assertThat(byBusinessSn.getAmount()).isEqualTo(80L);
        assertThat(byBusinessSn.getCurrency()).isEqualTo(CURRENCY);
        assertThat(byBusinessSn.isRouteSnapshotPresent()).isTrue();
        assertThat(byBusinessSn.isLedgerFactsPresent()).isTrue();
        assertThat(byBusinessSn.getLedgerTransactionCount()).isOne();
        assertThat(byBusinessSn.getLedgerEntryCount()).isEqualTo(2);
        assertThat(byBusinessSn.getPrimaryLedgerTransactionSn()).isNotBlank();
        assertThat(byBusinessSn.getLedgerEntries())
                .hasSize(2)
                .allSatisfy(entry -> {
                    assertThat(entry.getFundsTransactionSn()).isEqualTo(transactionSn);
                    assertThat(entry.getBusinessScene()).isEqualTo(businessScene);
                    assertThat(entry.getBusinessSn()).isEqualTo(businessSn);
                    assertThat(entry.getAmount()).isEqualTo(80L);
                    assertThat(entry.getCurrency()).isEqualTo(CURRENCY.name());
                });
        assertThat(byBusinessSn.getAuditContextVariables())
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
                .doesNotContainKey(FundsInstructionContextKeys.EXTERNAL_ACCOUNT_REF);
        assertThat(balanceAdjustmentAuditApplicationService.findByBusinessSn(new FundsBalanceAdjustmentAuditQuery()
                .setTenantId(TENANT_ID)
                .setBusinessScene(businessScene)
                .setBusinessSn("BALANCE_ADJUST_EXTERNAL_AUDIT_QUERY_NOT_FOUND"))).isEmpty();

        BalanceSnapshot afterAuditBalance = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(),
                adjustmentAccount));
        assertOnlyBalanceDeltas(beforeAuditBalance, afterAuditBalance,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeAuditFacts);
        assertPostedTransactions(2);
    }

    /**
     * 场景：历史或异常数据中交易事实和 RouteSnapshot 仍存在，但账本交易与分录事实缺失。
     * 输入：已完成的外部余额异常调账交易，随后清理该交易关联的账本事实模拟不完整审计链路。
     * 输出：按业务流水查询仍能定位交易事实，并返回账本不完整状态。
     * 预期：审计查询以资金交易事实为主轴，不能因为账本事实缺失就误判为未找到。
     * 红线：查询不能为了补齐审计结果而重建 ledger transaction、posting plan 或 LedgerEntry。
     */
    @Test
    void testBalanceAdjustmentAuditByBusinessSnShouldExposeIncompleteLedgerFacts() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId adjustmentAccount = fundingAccount("platform_adjustment");
        allowNegativeLedger(user, LedgerSubjectCode.AVAILABLE);
        ensureLedger(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT);
        topup(user, 50L, "BALANCE_ADJUST_INCOMPLETE_LEDGER_TOPUP");

        String businessScene = "EXTERNAL_BALANCE_ANOMALY";
        String businessSn = "BALANCE_ADJUST_INCOMPLETE_LEDGER";
        balanceControlService.adjust(externalBalanceAnomalyAdjustRequest(user, businessSn), WindOperatorFactory.system());
        String transactionSn = fundsTransactionsByBusinessSn(businessSn).getFirst().getSn();
        clearLedgerFactsForFundsTransaction(transactionSn);
        BalanceSnapshot beforeAuditBalance = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(),
                adjustmentAccount));
        LedgerFactSnapshot beforeAuditFacts = ledgerFactSnapshot();

        FundsBalanceAdjustmentAuditDTO audit = balanceAdjustmentAuditApplicationService.findByBusinessSn(
                        new FundsBalanceAdjustmentAuditQuery()
                                .setTenantId(TENANT_ID)
                                .setBusinessScene(businessScene)
                                .setBusinessSn(businessSn))
                .orElseThrow();

        assertThat(audit.getFundsTransactionSn()).isEqualTo(transactionSn);
        assertThat(audit.getAuditCompleteness())
                .isEqualTo(FundsBalanceAdjustmentAuditCompleteness.INCOMPLETE_LEDGER);
        assertThat(audit.isRouteSnapshotPresent()).isTrue();
        assertThat(audit.isLedgerFactsPresent()).isFalse();
        assertThat(audit.getLedgerTransactionCount()).isZero();
        assertThat(audit.getLedgerEntryCount()).isZero();
        assertThat(audit.getAuditContextVariables())
                .containsEntry(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF,
                        "RECON_DIFF_202606170001")
                .doesNotContainKey(FundsInstructionContextKeys.EXTERNAL_ACCOUNT_REF);
        assertNoLedgerFactsForFundsTransaction(transactionSn);
        BalanceSnapshot afterAuditBalance = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(),
                adjustmentAccount));
        assertOnlyBalanceDeltas(beforeAuditBalance, afterAuditBalance,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeAuditFacts);
    }

    private void assertExternalAnomalyAuditContext(FundsTransactionDetail detail) {
        Map<String, Object> values = WindJson.parseObject(detail.getContextVariables(), new TypeReference<>() {
        });
        assertThat(values.get(FundsInstructionContextKeys.SOURCE_TYPE))
                .isEqualTo(SourceObjectType.EXTERNAL_BALANCE_ANOMALY.name());
        assertThat(values.get(FundsInstructionContextKeys.SOURCE_SN))
                .isEqualTo("EXT_BALANCE_ANOMALY_202606170001");
        assertThat(values.get(FundsInstructionContextKeys.EXTERNAL_FINAL_EVENT_REF))
                .isEqualTo("ISSUER_FINAL_EVENT_202606170001");
        assertThat(values.get(FundsInstructionContextKeys.EXTERNAL_BALANCE_SNAPSHOT_REF))
                .isEqualTo("ISSUER_BALANCE_SNAPSHOT_202606170001");
        assertThat(values.get(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF))
                .isEqualTo("RECON_DIFF_202606170001");
        assertThat(values.get(FundsInstructionContextKeys.RECONCILIATION_RERUN_REF))
                .isEqualTo("RECON_RERUN_202606170001");
        assertThat(values.get(FundsInstructionContextKeys.RESPONSIBILITY_REF))
                .isEqualTo("RECOVERY_CASE_202606170001");
        assertThat(values.get(FundsInstructionContextKeys.REASON_CODE))
                .isEqualTo("EXTERNAL_TERMINAL_BALANCE_DEFICIT");
        assertThat(values.get(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE)).isEqualTo(true);
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

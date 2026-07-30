package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.payout.PayoutOrderApplicationService;
import com.wind.funds.reconciliation.application.payout.impl.PayoutOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.application.settlement.SettlementOrderApplicationService;
import com.wind.funds.reconciliation.application.settlement.impl.SettlementOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutOperationStatus;
import com.wind.funds.reconciliation.enums.PayoutOrderStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutOrderDTO;
import com.wind.funds.reconciliation.model.dto.PayoutSubmissionAdmissionDecisionDTO;
import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CreatePayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.HandlePayoutReceiptRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.SubmitPayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.funds.reconciliation.service.PayoutSubmissionAuthority;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        PayoutOrderApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class PayoutOrderApplicationServiceTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private PayoutOrderApplicationService payoutOrderApplicationService;

    @Autowired
    private SettlementOrderApplicationService settlementOrderApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestPayoutSubmissionAuthority payoutSubmissionAuthority;

    @BeforeEach
    void clearPayoutFacts() {
        payoutSubmissionAuthority.allow();
        jdbcTemplate.update("DELETE FROM t_payout_receipt");
        jdbcTemplate.update("DELETE FROM t_payout_order");
        jdbcTemplate.update("DELETE FROM t_settlement_order_item");
        jdbcTemplate.update("DELETE FROM t_settlement_order");
        jdbcTemplate.update("DELETE FROM t_clearing_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    @Test
    void testAcceptedThenSucceededShouldOnlyPostTerminalFundsOnce() {
        FundsAccountId accountId = fundingAccount("payout_success_merchant");
        PayoutOrderDTO payout = newSubmittedPayout(accountId, 600L, "success");
        var beforeReceipts = snapshot(balances(accountId, cashMappingAccount(), prepaymentAccount()));

        PayoutOrderDTO accepted = payoutOrderApplicationService.handleReceipt(
                receipt(payout, PayoutOrderStatus.ACCEPTED, 600L, "accepted", "external-success"),
                WindOperatorFactory.system());
        assertThat(accepted.getFactStatus()).isEqualTo(PayoutOrderStatus.ACCEPTED);
        assertThat(accepted.getDisplayStatus()).isEqualTo(PayoutDisplayStatus.PROCESSING);
        assertNoFundsOrLedgerFactsForBusinessSn(payout.getSn());
        assertOnlyBalanceDeltas(beforeReceipts, snapshot(balances(accountId, cashMappingAccount(), prepaymentAccount())),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        HandlePayoutReceiptRequest successReceipt = receipt(
                payout, PayoutOrderStatus.SUCCEEDED, 600L, "success", "external-success");
        PayoutOrderDTO succeeded = payoutOrderApplicationService.handleReceipt(
                successReceipt, WindOperatorFactory.system());
        PayoutOrderDTO replay = payoutOrderApplicationService.handleReceipt(
                successReceipt, WindOperatorFactory.system());

        assertThat(succeeded.getFactStatus()).isEqualTo(PayoutOrderStatus.SUCCEEDED);
        assertThat(succeeded.getCompletionFundsTransactionSn()).isNotBlank();
        assertThat(replay.getCompletionFundsTransactionSn()).isEqualTo(succeeded.getCompletionFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeReceipts, snapshot(balances(accountId, cashMappingAccount(), prepaymentAccount())),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, -600L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 600L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(ledgerTransactionByBusinessSn(payout.getSn()).getEventType())
                .isEqualTo(FundsTransactionEventType.PAYOUT_SUCCEEDED.name());
        assertSingleFundsAndLedgerFactsForBusinessSn(payout.getSn(), 3, 4);

        PayoutOrderDTO conflicted = payoutOrderApplicationService.handleReceipt(
                receipt(payout, PayoutOrderStatus.FAILED, 600L, "late-failure", "external-success"),
                WindOperatorFactory.system());
        assertThat(conflicted.getFactStatus()).isEqualTo(PayoutOrderStatus.MISMATCHED);
        assertThat(conflicted.getRollbackFundsTransactionSn()).isNull();
        assertOnlyBalanceDeltas(beforeReceipts, snapshot(balances(accountId, cashMappingAccount(), prepaymentAccount())),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, -600L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 600L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_payout_receipt WHERE payout_order_sn = ?", Integer.class, payout.getSn()))
                .isEqualTo(3);
    }

    @Test
    void testFailedReceiptShouldRestoreSettlementOnlyOnce() {
        FundsAccountId accountId = fundingAccount("payout_failure_merchant");
        PayoutOrderDTO payout = newSubmittedPayout(accountId, 300L, "failure");
        var beforeReceipt = snapshot(balances(accountId, cashMappingAccount()));
        HandlePayoutReceiptRequest failureReceipt = receipt(
                payout, PayoutOrderStatus.FAILED, 300L, "failure", "external-failure")
                .setFailureCode("DECLINED")
                .setFailureReason("beneficiary rejected");

        PayoutOrderDTO failed = payoutOrderApplicationService.handleReceipt(
                failureReceipt, WindOperatorFactory.system());
        PayoutOrderDTO replay = payoutOrderApplicationService.handleReceipt(
                failureReceipt, WindOperatorFactory.system());

        assertThat(failed.getFactStatus()).isEqualTo(PayoutOrderStatus.FAILED);
        assertThat(failed.getRollbackFundsTransactionSn()).isNotBlank();
        assertThat(replay.getRollbackFundsTransactionSn()).isEqualTo(failed.getRollbackFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeReceipt, snapshot(balances(accountId, cashMappingAccount())),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, -300L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 300L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY));
        assertThat(ledgerTransactionByBusinessSn(payout.getSn()).getEventType())
                .isEqualTo(FundsTransactionEventType.PAYOUT_FAILED.name());
        assertSingleFundsAndLedgerFactsForBusinessSn(payout.getSn(), 1, 2);
    }

    @Test
    void testCrossOrderExternalReferenceShouldMismatchWithoutSecondFundsFacts() {
        FundsAccountId firstAccountId = fundingAccount("payout_ext_owner");
        FundsAccountId secondAccountId = fundingAccount("payout_ext_conflict");
        PayoutOrderDTO first = newSubmittedPayout(firstAccountId, 200L, "external-ref-owner");
        PayoutOrderDTO second = newSubmittedPayout(secondAccountId, 300L, "external-ref-conflict");

        PayoutOrderDTO succeeded = payoutOrderApplicationService.handleReceipt(
                receipt(first, PayoutOrderStatus.SUCCEEDED, 200L, "external-ref-owner", "shared-reference"),
                WindOperatorFactory.system());
        var beforeConflict = snapshot(balances(secondAccountId, cashMappingAccount(), prepaymentAccount()));
        PayoutOrderDTO conflicted = payoutOrderApplicationService.handleReceipt(
                receipt(second, PayoutOrderStatus.SUCCEEDED, 300L, "external-ref-conflict", "shared-reference"),
                WindOperatorFactory.system());

        assertThat(succeeded.getFactStatus()).isEqualTo(PayoutOrderStatus.SUCCEEDED);
        assertThat(conflicted.getFactStatus()).isEqualTo(PayoutOrderStatus.MISMATCHED);
        assertThat(conflicted.getOperationStatus()).isEqualTo(PayoutOperationStatus.REVIEW_REQUIRED);
        assertSingleFundsAndLedgerFactsForBusinessSn(first.getSn(), 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn(second.getSn());
        assertOnlyBalanceDeltas(beforeConflict,
                snapshot(balances(secondAccountId, cashMappingAccount(), prepaymentAccount())),
                delta(secondAccountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
    }

    @Test
    void testConcurrentCrossOrderReceiptReferenceShouldProduceOneFundsFact() throws Exception {
        FundsAccountId firstAccountId = fundingAccount("payout_race_first");
        FundsAccountId secondAccountId = fundingAccount("payout_race_second");
        PayoutOrderDTO first = newSubmittedPayout(firstAccountId, 125L, "receipt-race-first");
        PayoutOrderDTO second = newSubmittedPayout(secondAccountId, 175L, "receipt-race-second");
        HandlePayoutReceiptRequest firstReceipt = receipt(
                first, PayoutOrderStatus.SUCCEEDED, 125L, "shared-receipt", "external-first");
        HandlePayoutReceiptRequest secondReceipt = receipt(
                second, PayoutOrderStatus.SUCCEEDED, 175L, "shared-receipt", "external-second");
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ReceiptAttempt> firstAttempt = executor.submit(
                    concurrentReceiptAttempt(startGate, firstReceipt));
            Future<ReceiptAttempt> secondAttempt = executor.submit(
                    concurrentReceiptAttempt(startGate, secondReceipt));
            startGate.countDown();
            List<ReceiptAttempt> attempts = List.of(
                    firstAttempt.get(10, TimeUnit.SECONDS), secondAttempt.get(10, TimeUnit.SECONDS));

            assertThat(attempts).allSatisfy(attempt -> assertThat(attempt.failure()).isNull());
            assertThat(attempts).extracting(attempt -> attempt.result().getFactStatus())
                    .containsExactlyInAnyOrder(PayoutOrderStatus.SUCCEEDED, PayoutOrderStatus.MISMATCHED);
            PayoutOrderDTO firstResult = payoutOrderApplicationService.getOrder(TENANT_ID, first.getSn());
            PayoutOrderDTO secondResult = payoutOrderApplicationService.getOrder(TENANT_ID, second.getSn());
            PayoutOrderDTO succeeded = firstResult.getFactStatus() == PayoutOrderStatus.SUCCEEDED
                    ? firstResult : secondResult;
            PayoutOrderDTO mismatched = succeeded == firstResult ? secondResult : firstResult;
            assertThat(mismatched.getFactStatus()).isEqualTo(PayoutOrderStatus.MISMATCHED);
            assertSingleFundsAndLedgerFactsForBusinessSn(succeeded.getSn(), 3, 4);
            assertNoFundsOrLedgerFactsForBusinessSn(mismatched.getSn());
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_payout_receipt WHERE external_receipt_ref = ?",
                    Integer.class, "receipt-shared-receipt")).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testMismatchAndReturnedShouldNotCreateFundsFacts() {
        FundsAccountId accountId = fundingAccount("payout_mismatch_merchant");
        PayoutOrderDTO mismatchedPayout = newSubmittedPayout(accountId, 200L, "mismatch");
        var beforeMismatch = snapshot(balance(accountId));

        PayoutOrderDTO mismatched = payoutOrderApplicationService.handleReceipt(
                receipt(mismatchedPayout, PayoutOrderStatus.SUCCEEDED, 199L, "wrong-amount", "external-mismatch"),
                WindOperatorFactory.system());

        assertThat(mismatched.getFactStatus()).isEqualTo(PayoutOrderStatus.MISMATCHED);
        assertNoFundsOrLedgerFactsForBusinessSn(mismatchedPayout.getSn());
        assertOnlyBalanceDeltas(beforeMismatch, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        clearPayoutFacts();
        FundsAccountId returnedAccountId = fundingAccount("payout_returned_merchant");
        PayoutOrderDTO returnedPayout = newSubmittedPayout(returnedAccountId, 200L, "returned");
        var beforeReturn = snapshot(balance(returnedAccountId));
        PayoutOrderDTO returned = payoutOrderApplicationService.handleReceipt(
                receipt(returnedPayout, PayoutOrderStatus.RETURNED, 200L, "returned", "external-returned"),
                WindOperatorFactory.system());
        assertThat(returned.getFactStatus()).isEqualTo(PayoutOrderStatus.RETURNED);
        assertThat(returned.getOperationStatus()).isEqualTo(PayoutOperationStatus.REVIEW_REQUIRED);
        assertNoFundsOrLedgerFactsForBusinessSn(returnedPayout.getSn());
        assertOnlyBalanceDeltas(beforeReturn, snapshot(balance(returnedAccountId)),
                delta(returnedAccountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
    }

    @Test
    void testSubmitShouldFailClosedWhenPayoutGateIsMissing() {
        FundsAccountId accountId = fundingAccount("payout_gate_merchant");
        PayoutOrderDTO payout = newCreatedPayout(accountId, 100L, "gate");

        assertThatThrownBy(() -> payoutOrderApplicationService.submitOrder(
                submitRequest(payout, "missing-run-result"), WindOperatorFactory.system()))
                .hasMessageContaining("出款对账 Gate 未通过");
        assertThat(payoutOrderApplicationService.getOrder(TENANT_ID, payout.getSn()).getFactStatus())
                .isEqualTo(PayoutOrderStatus.CREATED);
        assertNoFundsOrLedgerFactsForBusinessSn(payout.getSn());
    }

    @Test
    void testSubmitShouldFailClosedWhenHostAuthorityRejects() {
        FundsAccountId accountId = fundingAccount("payout_auth_merchant");
        PayoutOrderDTO payout = newCreatedPayout(accountId, 100L, "authority");
        String runResultSn = prepareGate(ReconciliationGateObjectType.PAYOUT, payout.getSn(), "payout-authority");
        payoutSubmissionAuthority.reject();

        assertThatThrownBy(() -> payoutOrderApplicationService.submitOrder(
                submitRequest(payout, runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("宿主权威出款准入未通过");
        assertThat(payoutOrderApplicationService.getOrder(TENANT_ID, payout.getSn()).getFactStatus())
                .isEqualTo(PayoutOrderStatus.CREATED);
        assertNoFundsOrLedgerFactsForBusinessSn(payout.getSn());
    }

    private PayoutOrderDTO newSubmittedPayout(FundsAccountId accountId, long amount, String suffix) {
        PayoutOrderDTO payout = newCreatedPayout(accountId, amount, suffix);
        String runResultSn = prepareGate(ReconciliationGateObjectType.PAYOUT, payout.getSn(), "payout-" + suffix);
        PayoutOrderDTO submitted = payoutOrderApplicationService.submitOrder(
                submitRequest(payout, runResultSn), WindOperatorFactory.system());
        assertThat(submitted.getFactStatus()).isEqualTo(PayoutOrderStatus.SUBMITTED);
        assertThat(submitted.getAdmissionDecisionDigest()).hasSize(64);
        assertThat(submitted.getAdmissionEvidenceRefs()).containsExactly("authority:payout-admission");
        assertNoFundsOrLedgerFactsForBusinessSn(payout.getSn());
        return submitted;
    }

    private PayoutOrderDTO newCreatedPayout(FundsAccountId accountId, long amount, String suffix) {
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 1_000L, "PAYOUT_TOPUP_" + suffix);
        insertConfirmedClearingBatch(accountId, "CLB_PAYOUT_" + suffix, amount);
        SettlementOrderDTO created = settlementOrderApplicationService.createOrder(
                new CreateSettlementOrderRequest()
                        .setTenantId(TENANT_ID)
                        .setClearingBatchSns(List.of("CLB_PAYOUT_" + suffix))
                        .setSettlementPeriod("2026-07")
                        .setSettlementMode(SettlementMode.INTERMEDIARY_ACCOUNT)
                        .setSettlementDestination(SettlementDestination.EXTERNAL_ENDPOINT)
                        .setTriggerMode(SettlementTriggerMode.HOST_COMMAND)
                        .setTimezone("Asia/Shanghai")
                        .setCutoff("23:00")
                        .setPolicyCode("MERCHANT_EXTERNAL_PAYOUT")
                        .setPolicyVersion("v1")
                        .setPolicyApprovalRef("POLICY_APPROVAL_" + suffix),
                WindOperatorFactory.system());
        settlementOrderApplicationService.submitOrder(new SubmitSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn()), WindOperatorFactory.system());
        settlementOrderApplicationService.approveOrder(new ApproveSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn())
                .setSettlementApprovalRef("SETTLEMENT_APPROVAL_" + suffix), WindOperatorFactory.system());
        String settlementRunResultSn = prepareGate(
                ReconciliationGateObjectType.SETTLEMENT, created.getSn(), "settlement-" + suffix);
        settlementOrderApplicationService.lockOrder(new LockSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn())
                .setReconciliationRunResultSn(settlementRunResultSn), WindOperatorFactory.system());
        PayoutOrderDTO payout = payoutOrderApplicationService.createOrder(
                new CreatePayoutOrderRequest().setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn()),
                WindOperatorFactory.system());
        PayoutOrderDTO replay = payoutOrderApplicationService.createOrder(
                new CreatePayoutOrderRequest().setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn()),
                WindOperatorFactory.system());
        assertThat(replay.getSn()).isEqualTo(payout.getSn());
        return payout;
    }

    private SubmitPayoutOrderRequest submitRequest(PayoutOrderDTO payout, String runResultSn) {
        return new SubmitPayoutOrderRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(payout.getSn())
                .setPayoutAccountRef("payout-account-ref")
                .setPayeeEndpointRef("payee-endpoint-ref")
                .setChannelRef("channel-ref")
                .setApprovalRef("approval-ref")
                .setExternalRuleVerificationEvidence(new ExternalRuleVerificationEvidenceDTO()
                        .setEvidenceRef("rule:evidence")
                        .setRuleSource("host-policy")
                        .setVersionOrPublishedAt("v1")
                        .setEffectiveDate(LocalDate.now().minusDays(1))
                        .setApplicableScope("merchant-payout")
                        .setJurisdiction("TEST")
                        .setVerifiedAt(LocalDate.now())
                        .setConfirmedBy("test-owner")
                        .setStatus(ExternalRuleVerificationStatus.VERIFIED))
                .setReconciliationRunResultSn(runResultSn);
    }

    private HandlePayoutReceiptRequest receipt(PayoutOrderDTO payout,
                                               PayoutOrderStatus status,
                                               long amount,
                                               String suffix,
                                               String externalReference) {
        return new HandlePayoutReceiptRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(payout.getSn())
                .setChannelRef("channel-ref")
                .setExternalReceiptRef("receipt-" + suffix)
                .setExternalReference(externalReference)
                .setStatus(status)
                .setAmount(amount)
                .setCurrency(CURRENCY)
                .setSourceReceiptDigest(FundsStableHashSupport.sha256("receipt:" + suffix))
                .setEvidenceRef("receipt:evidence:" + suffix)
                .setExternalOccurredAt(LocalDateTime.now());
    }

    private void insertConfirmedClearingBatch(FundsAccountId accountId, String sn, long amount) {
        String amountDigest = FundsStableHashSupport.sha256Json(Map.of(
                "source", sn, "amount", amount, "currency", CURRENCY.name()));
        jdbcTemplate.update("""
                        INSERT INTO t_clearing_batch (
                            sn, tenant_id, subject_type, subject_id, currency, business_line, clearing_period,
                            clearing_rule_code, clearing_rule_version, candidate_count, total_amount, amount_digest,
                            funds_transaction_sn, status, created_by, confirmed_by, confirmed_time
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                sn, TENANT_ID, accountId.type(), accountId.id(), CURRENCY.name(), "ACQUIRING", "2026-07",
                "CLEARING_RULE", "v1", 1, amount, amountDigest, "FT_" + sn,
                "CONFIRMED", "system", "system", LocalDateTime.now());
    }

    private Callable<ReceiptAttempt> concurrentReceiptAttempt(CountDownLatch startGate,
                                                               HandlePayoutReceiptRequest request) {
        return () -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                startGate.await();
                return new ReceiptAttempt(payoutOrderApplicationService.handleReceipt(
                        request, WindOperatorFactory.system()), null);
            } catch (RuntimeException exception) {
                return new ReceiptAttempt(null, exception.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        };
    }

    private String prepareGate(ReconciliationGateObjectType objectType, String objectSn, String suffix) {
        String batchSn = "recon_batch_" + suffix;
        String referenceSourceRef = "internal:" + suffix;
        String comparisonSourceRef = "external:" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                objectType, objectSn, "recon-rule-v1", "report:" + suffix,
                referenceSourceRef, comparisonSourceRef);
        return reconciliationRunResultApplicationService.recordRunResult(
                new RecordReconciliationRunResultRequest().setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn)
                        .setMatchResults(List.of(new ReconciliationMatchResultItem()
                                .setReferenceSourceRef(referenceSourceRef)
                                .setComparisonSourceRef(comparisonSourceRef)
                                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                                .setEvidenceRef("report:" + suffix + "#line-1"))),
                WindOperatorFactory.system()).getSn();
    }

    @Configuration
    @Import({
            SettlementOrderApplicationServiceImpl.class,
            PayoutOrderApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class
    })
    static class Config {

        @Bean
        TestPayoutSubmissionAuthority payoutSubmissionAuthority() {
            return new TestPayoutSubmissionAuthority();
        }
    }

    static final class TestPayoutSubmissionAuthority implements PayoutSubmissionAuthority {

        private boolean passed = true;

        @Override
        public PayoutSubmissionAdmissionDecisionDTO authorize(PayoutOrderDTO order,
                                                               SubmitPayoutOrderRequest request,
                                                               com.wind.integration.operator.WindOperator operator) {
            return new PayoutSubmissionAdmissionDecisionDTO()
                    .setPassed(passed)
                    .setBlockingReason(passed ? null : "host rejected")
                    .setDecisionDigest(FundsStableHashSupport.sha256("authority:payout-admission"))
                    .setEvidenceRefs(List.of("authority:payout-admission"))
                    .setExpiresAt(LocalDateTime.now().plusMinutes(5));
        }

        void allow() {
            passed = true;
        }

        void reject() {
            passed = false;
        }
    }

    private record ReceiptAttempt(PayoutOrderDTO result, String failure) {
    }
}

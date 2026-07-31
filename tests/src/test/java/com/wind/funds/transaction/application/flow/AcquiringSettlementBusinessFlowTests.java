package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.clearing.ClearingBatchApplicationService;
import com.wind.funds.reconciliation.application.clearing.ClearingCandidateApplicationService;
import com.wind.funds.reconciliation.application.clearing.ClearingSplitBatchApplicationService;
import com.wind.funds.reconciliation.application.clearing.ClearingSplittableDetailApplicationService;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingBatchApplicationServiceImpl;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingCandidateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingSplitBatchApplicationServiceImpl;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingSplittableDetailApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.payout.PayoutOrderApplicationService;
import com.wind.funds.reconciliation.application.payout.impl.PayoutOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.application.settlement.SettlementOrderApplicationService;
import com.wind.funds.reconciliation.application.settlement.impl.SettlementOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ClearingBatchStatus;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutOrderStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementOrderStatus;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import com.wind.funds.reconciliation.model.dto.ClearingBatchDTO;
import com.wind.funds.reconciliation.model.dto.ClearingCandidateDTO;
import com.wind.funds.reconciliation.model.dto.ClearingSplitBatchDTO;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutOrderDTO;
import com.wind.funds.reconciliation.model.dto.PayoutSubmissionAdmissionDecisionDTO;
import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CreatePayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.HandlePayoutReceiptRequest;
import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.SubmitPayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.funds.reconciliation.service.PayoutSubmissionAuthority;
import com.wind.funds.reconciliation.services.impl.ClearingSettlementGateConsumerServiceImpl;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.integration.operator.WindOperator;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
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

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 收单 capture 到商户出款的公共资金能力组合验收。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        AcquiringSettlementBusinessFlowTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class AcquiringSettlementBusinessFlowTests extends FundsTransactionFlowTestSupport {

    private static final String BUSINESS_LINE = "ACQUIRING";

    private static final String PERIOD = "2026-07-31";

    private static final String RULE_CODE = "ACQUIRING_MERCHANT_GROSS";

    private static final String RULE_VERSION = "v1";

    private static final long CAPTURE_AMOUNT = 700L;

    @Autowired
    private ClearingSplittableDetailApplicationService clearingSplittableDetailApplicationService;

    @Autowired
    private ClearingSplitBatchApplicationService clearingSplitBatchApplicationService;

    @Autowired
    private ClearingCandidateApplicationService clearingCandidateApplicationService;

    @Autowired
    private ClearingBatchApplicationService clearingBatchApplicationService;

    @Autowired
    private SettlementOrderApplicationService settlementOrderApplicationService;

    @Autowired
    private PayoutOrderApplicationService payoutOrderApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearAcquiringSettlementFacts() {
        jdbcTemplate.update("DELETE FROM t_payout_receipt");
        jdbcTemplate.update("DELETE FROM t_payout_order");
        jdbcTemplate.update("DELETE FROM t_settlement_order_item");
        jdbcTemplate.update("DELETE FROM t_settlement_order");
        jdbcTemplate.update("DELETE FROM t_clearing_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch");
        jdbcTemplate.update("DELETE FROM t_clearing_candidate");
        jdbcTemplate.update("DELETE FROM t_clearing_split_result_snapshot");
        jdbcTemplate.update("DELETE FROM t_clearing_split_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_split_batch");
        jdbcTemplate.update("DELETE FROM t_clearing_splittable_detail");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    /**
     * 场景：上层已确认 capture 700，商户完成清分、清算、结算锁定和外部出款。
     * 结果：capture 只进入 CLEARING；清分不动账；清算进入 AVAILABLE；结算锁定后成功出款到 CASH。
     * 红线：不在资金底座实现 PSP、卡组织、商户入网、KYB、PCI 或清分规则计算。
     */
    @Test
    void testCaptureShouldReachPayoutThroughSharedSettlementCapabilities() {
        FundsAccountId payer = fundingAccount("acquiring_payer");
        FundsAccountId merchant = fundingAccount("acquiring_merchant");
        prepareAccount(payer);
        prepareMerchantAccount(merchant);
        topup(payer, 1_000L, "ACQUIRING_TOPUP");
        var beforeCapture = snapshot(balances(payer, merchant, cashMappingAccount()));

        CaptureFact capture = capture(payer, merchant);

        assertOnlyBalanceDeltas(beforeCapture, snapshot(balances(payer, merchant, cashMappingAccount())),
                delta(payer, LedgerSubjectCode.AVAILABLE, -CAPTURE_AMOUNT, CURRENCY),
                delta(merchant, LedgerSubjectCode.CLEARING, CAPTURE_AMOUNT, CURRENCY),
                delta(merchant, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY));

        ClearingBatchDTO clearingBatch = clear(capture);
        SettlementOrderDTO settlementOrder = settle(clearingBatch, merchant);
        var beforePayout = snapshot(balances(merchant, cashMappingAccount()));

        PayoutOrderDTO payoutOrder = payout(settlementOrder);

        assertThat(payoutOrder.getFactStatus()).isEqualTo(PayoutOrderStatus.SUCCEEDED);
        assertThat(payoutOrder.getCompletionFundsTransactionSn()).isNotBlank();
        assertOnlyBalanceDeltas(beforePayout, snapshot(balances(merchant, cashMappingAccount())),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, -CAPTURE_AMOUNT, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, CAPTURE_AMOUNT, CURRENCY));
    }

    private CaptureFact capture(FundsAccountId payer, FundsAccountId merchant) {
        FundsTransactionPayRequest request = new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(merchant)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.CLEARING)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(CAPTURE_AMOUNT, CURRENCY)))
                .setBusinessScene("ACQUIRING_CAPTURE")
                .setBusinessSn("ACQUIRING_CAPTURE_001")
                .setDescription("confirmed acquiring capture");

        String transactionSn = directTransactionService.pay(request, WindOperatorFactory.system());
        String replay = directTransactionService.pay(request, WindOperatorFactory.system());
        FundsTransactionDetailDTO payeeDetail = fundsTransactionDetails(transactionSn).stream()
                .filter(detail -> detail.getParticipantRole() == RouteParticipantRole.PAYEE)
                .findFirst()
                .orElseThrow();
        LedgerEntry clearingEntry = entriesByFundsTransactionSn(transactionSn).stream()
                .filter(entry -> merchant.id().equals(entry.getSubjectId()))
                .filter(entry -> entry.getLedgerSubjectCode() == LedgerSubjectCode.CLEARING)
                .findFirst()
                .orElseThrow();

        assertThat(replay).isEqualTo(transactionSn);
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transactionSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getParticipants())
                        .filteredOn(participant -> participant.getParticipantRole() == RouteParticipantRole.PAYEE)
                        .extracting(participant -> participant.getLedgerProfileCode())
                        .containsExactly(LedgerProfileCode.FUNDING_MERCHANT.name()));
        assertSingleFundsAndLedgerFactsForBusinessSn("ACQUIRING_CAPTURE_001", 2, 1, 2);
        assertLedgerFactsFollowRouteSnapshot("ACQUIRING_CAPTURE_001");
        return new CaptureFact(merchant, transactionSn, payeeDetail.getSn(), clearingEntry.getSn());
    }

    private ClearingBatchDTO clear(CaptureFact capture) {
        String evidenceRef = "acquiring-capture-evidence:001";
        String runResultSn = prepareGate(
                ReconciliationGateObjectType.CLEARING, capture.detailSn(), "split", evidenceRef);
        var beforeSplit = ledgerFactSnapshot();
        IdentifyClearingSplittableDetailRequest identifyRequest = new IdentifyClearingSplittableDetailRequest()
                .setTenantId(TENANT_ID)
                .setFundsTransactionSn(capture.transactionSn())
                .setFundsTransactionDetailSn(capture.detailSn())
                .setLedgerEntrySn(capture.ledgerEntrySn())
                .setReconciliationRunResultSn(runResultSn)
                .setBusinessLine(BUSINESS_LINE)
                .setSplitPeriod(PERIOD)
                .setSplitRuleCode(RULE_CODE)
                .setSplitRuleVersion(RULE_VERSION);
        ClearingSplittableDetailDTO splittable = clearingSplittableDetailApplicationService
                .identifySplittableDetail(identifyRequest, WindOperatorFactory.system());
        ClearingSplittableDetailDTO splittableReplay = clearingSplittableDetailApplicationService
                .identifySplittableDetail(identifyRequest, WindOperatorFactory.system());
        ClearingSplitBatchDTO splitBatch = clearingSplitBatchApplicationService.createBatch(
                new CreateClearingSplitBatchRequest().setTenantId(TENANT_ID)
                        .setSplittableDetailSns(List.of(splittable.getSn())), WindOperatorFactory.system());
        clearingSplitBatchApplicationService.submitBatch(new SubmitClearingSplitBatchRequest()
                .setTenantId(TENANT_ID).setSplitBatchSn(splitBatch.getSn()), WindOperatorFactory.system());
        ClearingSplitBatchDTO confirmedSplit = clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID)
                        .setSplitBatchSn(splitBatch.getSn()), WindOperatorFactory.system());

        assertThat(splittableReplay.getSn()).isEqualTo(splittable.getSn());
        assertThat(splittable.getBusinessLine()).isEqualTo(BUSINESS_LINE);
        assertThat(splittable.getReconciliationEvidenceRefs()).containsExactly(evidenceRef);
        assertThat(confirmedSplit.getTotalAmount()).isEqualTo(CAPTURE_AMOUNT);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeSplit);

        String splitResultSn = clearingSplitBatchApplicationService
                .getResultSnapshots(TENANT_ID, splitBatch.getSn()).getFirst().getSn();
        CreateClearingCandidateRequest candidateRequest = new CreateClearingCandidateRequest()
                .setTenantId(TENANT_ID)
                .setSplitResultSn(splitResultSn)
                .setClearingPeriod(PERIOD)
                .setClearingRuleCode(RULE_CODE)
                .setClearingRuleVersion(RULE_VERSION)
                .setClearingAvailableTime(LocalDateTime.now().minusMinutes(1));
        ClearingCandidateDTO candidate = clearingCandidateApplicationService.createCandidate(
                candidateRequest, WindOperatorFactory.system());
        ClearingCandidateDTO candidateReplay = clearingCandidateApplicationService.createCandidate(
                candidateRequest, WindOperatorFactory.system());
        ClearingBatchDTO clearingBatch = clearingBatchApplicationService.createBatch(
                new CreateClearingBatchRequest().setTenantId(TENANT_ID)
                        .setCandidateSns(List.of(candidate.getSn())), WindOperatorFactory.system());
        clearingBatchApplicationService.submitBatch(new SubmitClearingBatchRequest()
                .setTenantId(TENANT_ID).setClearingBatchSn(clearingBatch.getSn()), WindOperatorFactory.system());
        var beforeClearing = snapshot(balance(capture.merchant()));
        ClearingBatchDTO confirmed = clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID)
                        .setClearingBatchSn(clearingBatch.getSn()), WindOperatorFactory.system());
        ClearingBatchDTO replay = clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID)
                        .setClearingBatchSn(clearingBatch.getSn()), WindOperatorFactory.system());

        assertThat(candidateReplay.getSn()).isEqualTo(candidate.getSn());
        assertThat(confirmed.getStatus()).isEqualTo(ClearingBatchStatus.CONFIRMED);
        assertThat(replay.getFundsTransactionSn()).isEqualTo(confirmed.getFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeClearing, snapshot(balance(capture.merchant())),
                delta(capture.merchant(), LedgerSubjectCode.CLEARING, -CAPTURE_AMOUNT, CURRENCY),
                delta(capture.merchant(), LedgerSubjectCode.AVAILABLE, CAPTURE_AMOUNT, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(clearingBatch.getSn(), 1, 1, 2);
        return confirmed;
    }

    private SettlementOrderDTO settle(ClearingBatchDTO clearingBatch, FundsAccountId merchant) {
        SettlementOrderDTO created = settlementOrderApplicationService.createOrder(
                new CreateSettlementOrderRequest()
                        .setTenantId(TENANT_ID)
                        .setClearingBatchSns(List.of(clearingBatch.getSn()))
                        .setSettlementPeriod("2026-07")
                        .setSettlementMode(SettlementMode.INTERMEDIARY_ACCOUNT)
                        .setSettlementDestination(SettlementDestination.EXTERNAL_ENDPOINT)
                        .setTriggerMode(SettlementTriggerMode.HOST_COMMAND)
                        .setTimezone("Asia/Shanghai")
                        .setCutoff("23:00")
                        .setPolicyCode("ACQUIRING_MERCHANT_SETTLEMENT")
                        .setPolicyVersion(RULE_VERSION)
                        .setPolicyApprovalRef("acquiring-settlement-policy-approval"),
                WindOperatorFactory.system());
        settlementOrderApplicationService.submitOrder(new SubmitSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn()), WindOperatorFactory.system());
        settlementOrderApplicationService.approveOrder(new ApproveSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setSettlementOrderSn(created.getSn())
                .setSettlementApprovalRef("acquiring-settlement-approval"), WindOperatorFactory.system());
        String runResultSn = prepareGate(
                ReconciliationGateObjectType.SETTLEMENT, created.getSn(), "settlement", "settlement-evidence:001");
        var beforeLock = snapshot(balance(merchant));
        LockSettlementOrderRequest request = new LockSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setSettlementOrderSn(created.getSn())
                .setReconciliationRunResultSn(runResultSn);
        SettlementOrderDTO locked = settlementOrderApplicationService.lockOrder(
                request, WindOperatorFactory.system());
        SettlementOrderDTO replay = settlementOrderApplicationService.lockOrder(
                request, WindOperatorFactory.system());

        assertThat(locked.getStatus()).isEqualTo(SettlementOrderStatus.LOCKED);
        assertThat(replay.getLockFundsTransactionSn()).isEqualTo(locked.getLockFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeLock, snapshot(balance(merchant)),
                delta(merchant, LedgerSubjectCode.AVAILABLE, -CAPTURE_AMOUNT, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, CAPTURE_AMOUNT, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(created.getSn(), 1, 1, 2);
        return locked;
    }

    private PayoutOrderDTO payout(SettlementOrderDTO settlementOrder) {
        CreatePayoutOrderRequest createRequest = new CreatePayoutOrderRequest()
                .setTenantId(TENANT_ID)
                .setSettlementOrderSn(settlementOrder.getSn());
        PayoutOrderDTO created = payoutOrderApplicationService.createOrder(
                createRequest, WindOperatorFactory.system());
        PayoutOrderDTO createReplay = payoutOrderApplicationService.createOrder(
                createRequest, WindOperatorFactory.system());
        String runResultSn = prepareGate(
                ReconciliationGateObjectType.PAYOUT, created.getSn(), "payout", "payout-evidence:001");
        payoutOrderApplicationService.submitOrder(new SubmitPayoutOrderRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(created.getSn())
                .setPayoutAccountRef("merchant-payout-account:001")
                .setPayeeEndpointRef("merchant-bank-endpoint:001")
                .setChannelRef("acquiring-payout-channel:001")
                .setApprovalRef("acquiring-payout-approval:001")
                .setExternalRuleVerificationEvidence(verifiedPayoutRule())
                .setReconciliationRunResultSn(runResultSn), WindOperatorFactory.system());
        HandlePayoutReceiptRequest receipt = new HandlePayoutReceiptRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(created.getSn())
                .setChannelRef("acquiring-payout-channel:001")
                .setExternalReceiptRef("acquiring-payout-receipt:001")
                .setExternalReference("external-acquiring-payout:001")
                .setStatus(PayoutOrderStatus.SUCCEEDED)
                .setAmount(CAPTURE_AMOUNT)
                .setCurrency(CURRENCY)
                .setSourceReceiptDigest(FundsStableHashSupport.sha256("acquiring-payout-receipt:001"))
                .setEvidenceRef("acquiring-payout-receipt-evidence:001")
                .setExternalOccurredAt(LocalDateTime.now());
        PayoutOrderDTO completed = payoutOrderApplicationService.handleReceipt(
                receipt, WindOperatorFactory.system());
        PayoutOrderDTO replay = payoutOrderApplicationService.handleReceipt(
                receipt, WindOperatorFactory.system());

        assertThat(createReplay.getSn()).isEqualTo(created.getSn());
        assertThat(replay.getCompletionFundsTransactionSn()).isEqualTo(completed.getCompletionFundsTransactionSn());
        assertSingleFundsAndLedgerFactsForBusinessSn(created.getSn(), 3, 2, 4);
        return completed;
    }

    private String prepareGate(ReconciliationGateObjectType objectType,
                               String objectSn,
                               String suffix,
                               String evidenceRef) {
        String batchSn = "acquiring-reconciliation:" + suffix;
        String referenceSourceRef = "internal:" + suffix;
        String comparisonSourceRef = "evidence:" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                objectType, objectSn, RULE_VERSION, evidenceRef, referenceSourceRef, comparisonSourceRef);
        return reconciliationRunResultApplicationService.recordRunResult(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn)
                        .setMatchResults(List.of(new ReconciliationMatchResultItem()
                                .setReferenceSourceRef(referenceSourceRef)
                                .setComparisonSourceRef(comparisonSourceRef)
                                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                                .setEvidenceRef(evidenceRef + "#line-1"))),
                WindOperatorFactory.system()).getSn();
    }

    private ExternalRuleVerificationEvidenceDTO verifiedPayoutRule() {
        return new ExternalRuleVerificationEvidenceDTO()
                .setEvidenceRef("acquiring-payout-rule-evidence:001")
                .setRuleSource("host-acquiring-payout-policy")
                .setVersionOrPublishedAt(RULE_VERSION)
                .setEffectiveDate(LocalDate.now().minusDays(1))
                .setApplicableScope("acquiring-merchant-payout")
                .setJurisdiction("TEST")
                .setVerifiedAt(LocalDate.now())
                .setConfirmedBy("test-owner")
                .setStatus(ExternalRuleVerificationStatus.VERIFIED);
    }

    private void prepareAccount(FundsAccountId accountId) {
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
    }

    private void prepareMerchantAccount(FundsAccountId accountId) {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setSn(accountId.id())
                .setTenantId(TENANT_ID)
                .setOwnerId("merchant-owner:001")
                .setOwnerType(FundsAccountOwnerType.MERCHANT)
                .setAccountType(accountId.type())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CURRENCY));
    }

    @Configuration
    @Import({
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class,
            ClearingSettlementGateConsumerServiceImpl.class,
            ClearingSplittableDetailApplicationServiceImpl.class,
            ClearingSplitBatchApplicationServiceImpl.class,
            ClearingCandidateApplicationServiceImpl.class,
            ClearingBatchApplicationServiceImpl.class,
            SettlementOrderApplicationServiceImpl.class,
            PayoutOrderApplicationServiceImpl.class
    })
    static class Config {

        @Bean
        PayoutSubmissionAuthority payoutSubmissionAuthority() {
            return new PayoutSubmissionAuthority() {
                @Override
                public PayoutSubmissionAdmissionDecisionDTO authorize(PayoutOrderDTO order,
                                                                       SubmitPayoutOrderRequest request,
                                                                       WindOperator operator) {
                    return new PayoutSubmissionAdmissionDecisionDTO()
                            .setPassed(true)
                            .setDecisionDigest(FundsStableHashSupport.sha256("acquiring-payout-authority"))
                            .setEvidenceRefs(List.of("authority:acquiring-payout"))
                            .setExpiresAt(LocalDateTime.now().plusMinutes(5));
                }
            };
        }
    }

    private record CaptureFact(FundsAccountId merchant,
                               String transactionSn,
                               String detailSn,
                               String ledgerEntrySn) {
    }
}

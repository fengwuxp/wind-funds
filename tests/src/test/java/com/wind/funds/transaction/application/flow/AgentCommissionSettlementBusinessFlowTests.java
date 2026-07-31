package com.wind.funds.transaction.application.flow;

import com.alibaba.fastjson2.JSON;
import com.wind.core.ReadonlyContextVariables;
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
import com.wind.funds.reconciliation.enums.ClearingSplittableDetailStatus;
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
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
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
import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 两级代理分佣结算业务流程测试。
 *
 * <p>上层业务已经完成利润、GMV、归因和审批计算，本测试只验证资金底座能把两笔独立收益分配事实
 * 复用标准交易、清分、清算、结算和出款能力推进，不引入代理关系或佣金计算内核。</p>
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        AgentCommissionSettlementBusinessFlowTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class AgentCommissionSettlementBusinessFlowTests extends FundsTransactionFlowTestSupport {

    private static final String BUSINESS_LINE = "AGENT_COMMISSION";

    private static final String SPLIT_PERIOD = "2026-07-30";

    private static final String RULE_CODE = "TWO_LEVEL_AGENT_COMMISSION";

    private static final String RULE_VERSION = "v1";

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
    void clearCommissionSettlementFacts() {
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
     * 场景：平台把同一收益池拆成用户代理净佣金 70 和平台员工二级分润 30。
     * 结果：两笔资金分配独立进入 CLEARING、清分确认无账务副作用、清算后进入 AVAILABLE；
     * 用户代理完成外部出款，员工分润保持独立 SETTLEMENT 锁定。
     * 红线：不得把两级收益净额合并，不得在清分确认时入账，不得新增 Commission 专用资金内核。
     */
    @Test
    void testTwoLevelCommissionShouldSettleIndependentlyThroughSharedFundsCapabilities() {
        FundsAccountId platform = fundingAccount("cmp_platform");
        FundsAccountId userAgentRef = payableAccount("cmp_agent");
        FundsAccountId employeeRef = payableAccount("cmp_employee");
        FundsAccountId userAgent = fundingAccount(userAgentRef.id());
        FundsAccountId platformEmployee = fundingAccount(employeeRef.id());
        prepareAccount(platform);
        preparePayableAccount(userAgentRef);
        preparePayableAccount(employeeRef);
        topup(platform, 1_000L, "COMMISSION_PLATFORM_TOPUP");
        var afterTopup = snapshot(balances(platform, userAgent, platformEmployee, cashMappingAccount()));

        CommissionAllocation agentAllocation = allocateCommission(
                platform, userAgentRef, 70L, "AGENT_NET_COMMISSION", "agent");
        CommissionAllocation employeeAllocation = allocateCommission(
                platform, employeeRef, 30L, "EMPLOYEE_TIER2_REVENUE_SHARE", "employee");

        assertOnlyBalanceDeltas(afterTopup,
                snapshot(balances(platform, userAgent, platformEmployee, cashMappingAccount())),
                delta(platform, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(userAgent, LedgerSubjectCode.CLEARING, 70L, CURRENCY),
                delta(platformEmployee, LedgerSubjectCode.CLEARING, 30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY));

        LockedCommission agentLocked = clearAndLock(agentAllocation, SettlementDestination.EXTERNAL_ENDPOINT);
        LockedCommission employeeLocked = clearAndLock(
                employeeAllocation, SettlementDestination.INTERNAL_ACCOUNT);

        var beforePayout = snapshot(balances(userAgent, platformEmployee, cashMappingAccount()));
        PayoutOrderDTO payout = payout(agentLocked, "agent");

        assertThat(payout.getFactStatus()).isEqualTo(PayoutOrderStatus.SUCCEEDED);
        assertThat(payout.getCompletionFundsTransactionSn()).isNotBlank();
        assertOnlyBalanceDeltas(beforePayout,
                snapshot(balances(userAgent, platformEmployee, cashMappingAccount())),
                delta(userAgent, LedgerSubjectCode.SETTLEMENT, -70L, CURRENCY),
                delta(platformEmployee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 70L, CURRENCY));
        assertThat(employeeLocked.settlementOrder().getNetAmount()).isEqualTo(30L);
        assertBucket(balance(platformEmployee), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);
    }

    /**
     * 场景：佣金分配事实已经进入代理 CLEARING，但清分前对账证据缺失。
     * 结果：可清分识别失败关闭且不生成新的资金、route、posting 或账本事实，金额保持在 CLEARING。
     */
    @Test
    void testCommissionShouldRemainInClearingWhenReconciliationEvidenceIsMissing() {
        FundsAccountId platform = fundingAccount("cmp_block_platform");
        FundsAccountId userAgentRef = payableAccount("cmp_block_agent");
        FundsAccountId userAgent = fundingAccount(userAgentRef.id());
        prepareAccount(platform);
        preparePayableAccount(userAgentRef);
        topup(platform, 100L, "COMMISSION_BLOCKED_TOPUP");
        CommissionAllocation allocation = allocateCommission(
                platform, userAgentRef, 20L, "AGENT_NET_COMMISSION", "blocked");
        var before = ledgerFactSnapshot();

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                identifyRequest(allocation, "missing-reconciliation-result"), WindOperatorFactory.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getSn()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_clearing_splittable_detail", Integer.class)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
        assertBucket(balance(userAgent), LedgerSubjectCode.CLEARING, 20L, CURRENCY);
        assertBucket(balance(userAgent), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    private CommissionAllocation allocateCommission(FundsAccountId platform,
                                                    FundsAccountId beneficiary,
                                                    long amount,
                                                    String businessScene,
                                                    String suffix) {
        String businessSn = "COMMISSION_ALLOCATION_" + suffix.toUpperCase();
        FundsTransactionPayRequest request = new FundsTransactionPayRequest()
                .setAccountId(platform)
                .setPayeeId(beneficiary)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.CLEARING)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "revenueEntitlementRef", "entitlement:" + suffix,
                        "revenueAttributionSnapshotRef", "attribution:two-level:" + suffix,
                        "revenueRuleRef", RULE_CODE + ":" + RULE_VERSION,
                        "revenueApprovalRef", "approval:" + suffix)))
                .setDescription("approved commission allocation");

        String transactionSn = directTransactionService.pay(request, WindOperatorFactory.system());
        String replay = directTransactionService.pay(request, WindOperatorFactory.system());
        FundsTransactionDetailDTO payeeDetail = fundsTransactionDetails(transactionSn).stream()
                .filter(detail -> detail.getParticipantRole() == RouteParticipantRole.PAYEE)
                .findFirst()
                .orElseThrow();
        LedgerEntry clearingEntry = entriesByFundsTransactionSn(transactionSn).stream()
                .filter(entry -> beneficiary.id().equals(entry.getSubjectId()))
                .filter(entry -> entry.getLedgerSubjectCode() == LedgerSubjectCode.CLEARING)
                .findFirst()
                .orElseThrow();

        assertThat(replay).isEqualTo(transactionSn);
        assertThat(JSON.parseObject(payeeDetail.getContextVariables()))
                .as("direct transaction details must not persist host revenue context")
                .isEmpty();
        assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, 2, 1, 2);
        assertLedgerFactsFollowRouteSnapshot(businessSn);
        return new CommissionAllocation(
                fundingAccount(beneficiary.id()), amount, suffix, businessSn,
                transactionSn, payeeDetail.getSn(), clearingEntry.getSn());
    }

    private LockedCommission clearAndLock(CommissionAllocation allocation,
                                          SettlementDestination destination) {
        String commissionEvidenceRef = "commission-evidence-bundle:" + allocation.suffix();
        String splitRunResultSn = prepareGate(ReconciliationGateObjectType.CLEARING,
                allocation.detailSn(), "split-" + allocation.suffix(), commissionEvidenceRef);
        var beforeSplit = ledgerFactSnapshot();
        ClearingSplittableDetailDTO splittable = clearingSplittableDetailApplicationService
                .identifySplittableDetail(identifyRequest(allocation, splitRunResultSn), WindOperatorFactory.system());
        ClearingSplittableDetailDTO splittableReplay = clearingSplittableDetailApplicationService
                .identifySplittableDetail(identifyRequest(allocation, splitRunResultSn), WindOperatorFactory.system());
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
        assertThat(splittable.getSplitRuleCode()).isEqualTo(RULE_CODE);
        assertThat(splittable.getSplitRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(splittable.getReconciliationEvidenceRefs()).containsExactly(commissionEvidenceRef);
        assertThat(splittable.getSourceDigest()).isNotBlank();
        assertThat(confirmedSplit.getTotalAmount()).isEqualTo(allocation.amount());
        assertLedgerFactsUnchanged(jdbcTemplate, beforeSplit);

        String splitResultSn = clearingSplitBatchApplicationService
                .getResultSnapshots(TENANT_ID, splitBatch.getSn()).getFirst().getSn();
        CreateClearingCandidateRequest candidateRequest = new CreateClearingCandidateRequest()
                .setTenantId(TENANT_ID)
                .setSplitResultSn(splitResultSn)
                .setClearingPeriod(SPLIT_PERIOD)
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
        var beforeClearing = snapshot(balance(allocation.beneficiary()));
        ClearingBatchDTO confirmedClearing = clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID)
                        .setClearingBatchSn(clearingBatch.getSn()), WindOperatorFactory.system());
        ClearingBatchDTO clearingReplay = clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID)
                        .setClearingBatchSn(clearingBatch.getSn()), WindOperatorFactory.system());

        assertThat(candidateReplay.getSn()).isEqualTo(candidate.getSn());
        assertThat(confirmedClearing.getStatus()).isEqualTo(ClearingBatchStatus.CONFIRMED);
        assertThat(clearingReplay.getFundsTransactionSn()).isEqualTo(confirmedClearing.getFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeClearing, snapshot(balance(allocation.beneficiary())),
                delta(allocation.beneficiary(), LedgerSubjectCode.CLEARING, -allocation.amount(), CURRENCY),
                delta(allocation.beneficiary(), LedgerSubjectCode.AVAILABLE, allocation.amount(), CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(clearingBatch.getSn(), 1, 1, 2);

        SettlementOrderDTO settlementOrder = settlementOrderApplicationService.createOrder(
                settlementRequest(confirmedClearing, destination), WindOperatorFactory.system());
        settlementOrderApplicationService.submitOrder(new SubmitSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(settlementOrder.getSn()), WindOperatorFactory.system());
        settlementOrderApplicationService.approveOrder(new ApproveSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setSettlementOrderSn(settlementOrder.getSn())
                .setSettlementApprovalRef("settlement-approval:" + allocation.suffix()),
                WindOperatorFactory.system());
        String settlementRunResultSn = prepareGate(ReconciliationGateObjectType.SETTLEMENT,
                settlementOrder.getSn(), "settlement-" + allocation.suffix());
        var beforeLock = snapshot(balance(allocation.beneficiary()));
        SettlementOrderDTO locked = settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(settlementOrder.getSn())
                        .setReconciliationRunResultSn(settlementRunResultSn), WindOperatorFactory.system());
        SettlementOrderDTO lockReplay = settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(settlementOrder.getSn())
                        .setReconciliationRunResultSn(settlementRunResultSn), WindOperatorFactory.system());

        assertThat(locked.getStatus()).isEqualTo(SettlementOrderStatus.LOCKED);
        assertThat(lockReplay.getLockFundsTransactionSn()).isEqualTo(locked.getLockFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeLock, snapshot(balance(allocation.beneficiary())),
                delta(allocation.beneficiary(), LedgerSubjectCode.AVAILABLE, -allocation.amount(), CURRENCY),
                delta(allocation.beneficiary(), LedgerSubjectCode.SETTLEMENT, allocation.amount(), CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(settlementOrder.getSn(), 1, 1, 2);
        return new LockedCommission(allocation, locked);
    }

    private PayoutOrderDTO payout(LockedCommission commission, String suffix) {
        PayoutOrderDTO created = payoutOrderApplicationService.createOrder(
                new CreatePayoutOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(commission.settlementOrder().getSn()), WindOperatorFactory.system());
        PayoutOrderDTO createReplay = payoutOrderApplicationService.createOrder(
                new CreatePayoutOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(commission.settlementOrder().getSn()), WindOperatorFactory.system());
        String payoutRunResultSn = prepareGate(
                ReconciliationGateObjectType.PAYOUT, created.getSn(), "payout-" + suffix);
        payoutOrderApplicationService.submitOrder(new SubmitPayoutOrderRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(created.getSn())
                .setPayoutAccountRef("payout-account:" + suffix)
                .setPayeeEndpointRef("payee-endpoint:" + suffix)
                .setChannelRef("channel:" + suffix)
                .setApprovalRef("payout-approval:" + suffix)
                .setExternalRuleVerificationEvidence(verifiedPayoutRule(suffix))
                .setReconciliationRunResultSn(payoutRunResultSn), WindOperatorFactory.system());
        HandlePayoutReceiptRequest receipt = new HandlePayoutReceiptRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(created.getSn())
                .setChannelRef("channel:" + suffix)
                .setExternalReceiptRef("receipt:" + suffix)
                .setExternalReference("external-payout:" + suffix)
                .setStatus(PayoutOrderStatus.SUCCEEDED)
                .setAmount(commission.allocation().amount())
                .setCurrency(CURRENCY)
                .setSourceReceiptDigest(FundsStableHashSupport.sha256("receipt:" + suffix))
                .setEvidenceRef("payout-evidence:" + suffix)
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

    private IdentifyClearingSplittableDetailRequest identifyRequest(CommissionAllocation allocation,
                                                                    String reconciliationRunResultSn) {
        return new IdentifyClearingSplittableDetailRequest()
                .setTenantId(TENANT_ID)
                .setFundsTransactionSn(allocation.transactionSn())
                .setFundsTransactionDetailSn(allocation.detailSn())
                .setLedgerEntrySn(allocation.ledgerEntrySn())
                .setReconciliationRunResultSn(reconciliationRunResultSn)
                .setBusinessLine(BUSINESS_LINE)
                .setSplitPeriod(SPLIT_PERIOD)
                .setSplitRuleCode(RULE_CODE)
                .setSplitRuleVersion(RULE_VERSION);
    }

    private CreateSettlementOrderRequest settlementRequest(ClearingBatchDTO clearingBatch,
                                                            SettlementDestination destination) {
        return new CreateSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSns(List.of(clearingBatch.getSn()))
                .setSettlementPeriod("2026-07")
                .setSettlementMode(SettlementMode.INTERMEDIARY_ACCOUNT)
                .setSettlementDestination(destination)
                .setTriggerMode(SettlementTriggerMode.HOST_COMMAND)
                .setTimezone("Asia/Shanghai")
                .setCutoff("23:00")
                .setPolicyCode("AGENT_COMMISSION_SETTLEMENT")
                .setPolicyVersion(RULE_VERSION)
                .setPolicyApprovalRef("commission-policy-approval");
    }

    private ExternalRuleVerificationEvidenceDTO verifiedPayoutRule(String suffix) {
        return new ExternalRuleVerificationEvidenceDTO()
                .setEvidenceRef("rule-evidence:" + suffix)
                .setRuleSource("host-commission-payout-policy")
                .setVersionOrPublishedAt(RULE_VERSION)
                .setEffectiveDate(LocalDate.now().minusDays(1))
                .setApplicableScope("agent-commission-payout")
                .setJurisdiction("TEST")
                .setVerifiedAt(LocalDate.now())
                .setConfirmedBy("test-owner")
                .setStatus(ExternalRuleVerificationStatus.VERIFIED);
    }

    private String prepareGate(ReconciliationGateObjectType objectType,
                               String objectSn,
                               String suffix) {
        return prepareGate(objectType, objectSn, suffix, "report:" + suffix);
    }

    private String prepareGate(ReconciliationGateObjectType objectType,
                               String objectSn,
                               String suffix,
                               String evidenceRef) {
        String batchSn = "commission-reconciliation:" + suffix;
        String referenceSourceRef = "internal:" + suffix;
        String comparisonSourceRef = "evidence:" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                objectType, objectSn, RULE_VERSION, evidenceRef,
                referenceSourceRef, comparisonSourceRef);
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

    private void prepareAccount(FundsAccountId accountId) {
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
    }

    private void preparePayableAccount(FundsAccountId accountId) {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setSn(accountId.id())
                .setTenantId(TENANT_ID)
                .setOwnerId("owner_" + accountId.id())
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(accountId.type())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_MERCHANT));
    }

    private static FundsAccountId payableAccount(String accountId) {
        return FundsAccountId.immutable(accountId, DefaultFundsAccountType.ACCOUNT_PAYABLE.name());
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
                            .setDecisionDigest(FundsStableHashSupport.sha256("commission-payout-authority"))
                            .setEvidenceRefs(List.of("authority:commission-payout"))
                            .setExpiresAt(LocalDateTime.now().plusMinutes(5));
                }
            };
        }
    }

    private record CommissionAllocation(FundsAccountId beneficiary,
                                        long amount,
                                        String suffix,
                                        String businessSn,
                                        String transactionSn,
                                        String detailSn,
                                        String ledgerEntrySn) {
    }

    private record LockedCommission(CommissionAllocation allocation,
                                    SettlementOrderDTO settlementOrder) {
    }
}

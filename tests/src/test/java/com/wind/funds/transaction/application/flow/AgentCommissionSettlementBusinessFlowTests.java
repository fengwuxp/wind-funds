package com.wind.funds.transaction.application.flow;

import com.wind.jackson.WindJson;
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
import com.wind.funds.reconciliation.enums.ClearingBatchState;
import com.wind.funds.reconciliation.enums.ClearingSplittableAdmissionResult;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementOrderState;
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
import com.wind.funds.reconciliation.model.request.CancelSettlementOrderRequest;
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
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.RestoreClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.SubmitPayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.funds.reconciliation.service.PayoutSubmissionAuthority;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.enums.SourceObjectType;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
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
import org.assertj.core.api.SoftAssertions;
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
import tools.jackson.core.type.TypeReference;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

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

        assertThat(payout.getState()).isEqualTo(PayoutOrderState.SUCCEEDED);
        assertThat(payout.getCompletionFundsTransactionSn()).isNotBlank();
        assertOnlyBalanceDeltas(beforePayout,
                snapshot(balances(userAgent, platformEmployee, cashMappingAccount())),
                delta(userAgent, LedgerSubjectCode.SETTLEMENT, -70L, CURRENCY),
                delta(platformEmployee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -70L, CURRENCY));
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

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.EXCLUDED);
        assertThat(result.getSn()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_clearing_splittable_detail", Integer.class)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
        assertBucket(balance(userAgent), LedgerSubjectCode.CLEARING, 20L, CURRENCY);
        assertBucket(balance(userAgent), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    /**
     * 场景：佣金已经从 CLEARING 清算到 AVAILABLE 后，上游确认原分配需要全额扣回。
     * 输入：携带原资金交易、审批、证据、责任和对账差错引用的独立余额调账请求。
     * 输出：受益人 AVAILABLE 减少，平台 ADJUSTMENT 减少，重放不生成重复事实。
     * 红线：不得覆盖原佣金交易、route、detail、posting、entry 或已确认清算批次。
     */
    @Test
    void testCommissionAdjustmentAfterClearingShouldAppendAuditableFactsWithoutOverwritingAllocation() {
        FundsAccountId platform = fundingAccount("cmp_adjust_platform");
        FundsAccountId beneficiaryRef = payableAccount("cmp_adjust_agent");
        FundsAccountId beneficiary = fundingAccount(beneficiaryRef.id());
        FundsAccountId adjustmentAccount = fundingAccount("platform_adjustment");
        prepareAccount(platform);
        preparePayableAccount(beneficiaryRef);
        ensureLedger(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT);
        topup(platform, 100L, "COMMISSION_ADJUST_PLATFORM_TOPUP");
        CommissionAllocation allocation = allocateCommission(
                platform, beneficiaryRef, 20L, "AGENT_NET_COMMISSION", "adjust");
        ClearingBatchDTO confirmedClearing = clearCommission(allocation);
        var originalTransactions = jdbcTemplate.queryForList(
                "SELECT * FROM t_funds_transaction WHERE tenant_id = ? AND sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn());
        var originalDetails = jdbcTemplate.queryForList(
                "SELECT * FROM t_funds_transaction_detail WHERE tenant_id = ? AND transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn());
        var originalLedgerTransactions = jdbcTemplate.queryForList(
                "SELECT * FROM t_ledger_transaction WHERE tenant_id = ? AND funds_transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn());
        var originalPostingPlans = jdbcTemplate.queryForList(
                "SELECT * FROM t_ledger_posting_plan WHERE tenant_id = ? AND funds_transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn());
        var originalEntries = jdbcTemplate.queryForList(
                "SELECT * FROM t_ledger_entry WHERE tenant_id = ? AND funds_transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn());
        var beforeAdjust = snapshot(balances(beneficiary, adjustmentAccount));
        var beforeAdjustLedgerFacts = ledgerFactSnapshot();
        String adjustBusinessSn = "COMMISSION_AVAILABLE_ADJUST";
        FundsBalanceAdjustRequest adjustRequest = new FundsBalanceAdjustRequest()
                .setAccountId(beneficiary)
                .setAmount(Money.immutable(allocation.amount(), CURRENCY))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("COMMISSION_SETTLEMENT_ADJUSTMENT")
                .setBusinessSn(adjustBusinessSn)
                .setAdjustReason("commission corrected after clearing")
                .setAdjustEvidenceRef("commission-adjust-evidence:" + allocation.suffix())
                .setSourceType(SourceObjectType.FUNDS_TRANSACTION)
                .setSourceSn(allocation.transactionSn())
                .setReasonCode("COMMISSION_CORRECTION_AFTER_CLEARING")
                .setResponsibilityRef("commission-adjustment-case:" + allocation.suffix())
                .setApprovalRef("commission-adjust-approval:" + allocation.suffix())
                .setReconciliationExceptionRef("commission-reconciliation-difference:" + allocation.suffix())
                .setReconciliationRerunRef("commission-reconciliation-rerun:" + allocation.suffix())
                .setDescription("commission correction after clearing");

        String[] adjustmentTransactionSn = new String[1];
        Throwable failure = catchThrowable(() -> adjustmentTransactionSn[0] = balanceControlService.adjust(
                adjustRequest, WindOperatorFactory.system()));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(failure)
                .as("commission adjustment must produce a balanced posting plan")
                .isNull();
        if (failure == null) {
            String replay = balanceControlService.adjust(adjustRequest, WindOperatorFactory.system());
            assertThat(replay).isEqualTo(adjustmentTransactionSn[0]);
            assertOnlyBalanceDeltas(beforeAdjust, snapshot(balances(beneficiary, adjustmentAccount)),
                    delta(beneficiary, LedgerSubjectCode.AVAILABLE, -allocation.amount(), CURRENCY),
                    delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, -allocation.amount(), CURRENCY));
            assertSingleFundsAndLedgerFactsForBusinessSn(adjustBusinessSn, 2, 1, 2);
            assertLedgerFactsFollowRouteSnapshot(adjustBusinessSn);
            fundsTransactionDetails(adjustmentTransactionSn[0]).forEach(detail -> assertThat(
                    WindJson.parseObject(detail.getContextVariables(), new TypeReference<Map<String, Object>>() {
                    }))
                    .containsEntry(FundsInstructionContextKeys.SOURCE_TYPE, SourceObjectType.FUNDS_TRANSACTION.name())
                    .containsEntry(FundsInstructionContextKeys.SOURCE_SN, allocation.transactionSn())
                    .containsEntry(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF,
                            adjustRequest.getAdjustEvidenceRef())
                    .containsEntry(FundsInstructionContextKeys.APPROVAL_REF, adjustRequest.getApprovalRef())
                    .containsEntry(FundsInstructionContextKeys.RESPONSIBILITY_REF,
                            adjustRequest.getResponsibilityRef()));
        } else {
            assertOnlyBalanceDeltas(beforeAdjust, snapshot(balances(beneficiary, adjustmentAccount)),
                    delta(beneficiary, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                    delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY));
            assertLedgerTransactionFactsUnchanged(beforeAdjustLedgerFacts);
            assertNoFundsOrLedgerFactsForBusinessSn(adjustBusinessSn);
        }
        assertThat(clearingBatchApplicationService.getBatch(TENANT_ID, confirmedClearing.getSn()))
                .satisfies(current -> {
                    assertThat(current.getState()).isEqualTo(ClearingBatchState.CONFIRMED);
                    assertThat(current.getFundsTransactionSn())
                            .isEqualTo(confirmedClearing.getFundsTransactionSn());
                    assertThat(current.getAmountDigest()).isEqualTo(confirmedClearing.getAmountDigest());
                });
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM t_funds_transaction WHERE tenant_id = ? AND sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn())).isEqualTo(originalTransactions);
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM t_funds_transaction_detail WHERE tenant_id = ? AND transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn())).isEqualTo(originalDetails);
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM t_ledger_transaction WHERE tenant_id = ? AND funds_transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn())).isEqualTo(originalLedgerTransactions);
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM t_ledger_posting_plan WHERE tenant_id = ? AND funds_transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn())).isEqualTo(originalPostingPlans);
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM t_ledger_entry WHERE tenant_id = ? AND funds_transaction_sn = ? ORDER BY id",
                TENANT_ID, allocation.transactionSn())).isEqualTo(originalEntries);
        softly.assertAll();
    }

    /**
     * 场景：佣金结算已经锁定 SETTLEMENT，但尚未创建或提交出款单。
     * 输入：调用现有结算单取消入口尝试撤销 LOCKED 结算单。
     * 输出：请求失败关闭，结算单继续 LOCKED，余额与资金、route、posting、entry 均不变化。
     * 红线：当前公共入口不得把 LOCKED 误当可取消，也不得覆盖历史结算事实。
     */
    @Test
    void testLockedCommissionSettlementCancellationShouldFailClosedWithoutSideEffects() {
        FundsAccountId platform = fundingAccount("cmp_locked_platform");
        FundsAccountId beneficiaryRef = payableAccount("cmp_locked_agent");
        FundsAccountId beneficiary = fundingAccount(beneficiaryRef.id());
        prepareAccount(platform);
        preparePayableAccount(beneficiaryRef);
        topup(platform, 100L, "COMMISSION_LOCKED_PLATFORM_TOPUP");
        CommissionAllocation allocation = allocateCommission(
                platform, beneficiaryRef, 20L, "AGENT_NET_COMMISSION", "locked");
        LockedCommission locked = clearAndLock(allocation, SettlementDestination.EXTERNAL_ENDPOINT);
        var beforeBalance = snapshot(balance(beneficiary));
        var beforeLedgerFacts = ledgerFactSnapshot();
        var beforeTransactions = jdbcTemplate.queryForList("SELECT * FROM t_funds_transaction ORDER BY id");
        var beforeDetails = jdbcTemplate.queryForList("SELECT * FROM t_funds_transaction_detail ORDER BY id");

        assertThatThrownBy(() -> settlementOrderApplicationService.cancelOrder(
                new CancelSettlementOrderRequest()
                        .setTenantId(TENANT_ID)
                        .setSettlementOrderSn(locked.settlementOrder().getSn())
                        .setReason("commission correction after settlement lock"),
                WindOperatorFactory.system()))
                .hasMessageContaining("只有 DRAFT 或 REVIEWING 结算单可以取消");

        assertOnlyBalanceDeltas(beforeBalance, snapshot(balance(beneficiary)),
                delta(beneficiary, LedgerSubjectCode.CLEARING, 0L, CURRENCY),
                delta(beneficiary, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(beneficiary, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerFactsUnchanged(jdbcTemplate, beforeLedgerFacts);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM t_funds_transaction ORDER BY id"))
                .isEqualTo(beforeTransactions);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM t_funds_transaction_detail ORDER BY id"))
                .isEqualTo(beforeDetails);
        assertThat(settlementOrderApplicationService.getOrder(TENANT_ID, locked.settlementOrder().getSn()))
                .satisfies(current -> {
                    assertThat(current.getState()).isEqualTo(SettlementOrderState.LOCKED);
                    assertThat(current.getLockFundsTransactionSn())
                            .isEqualTo(locked.settlementOrder().getLockFundsTransactionSn());
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_payout_order WHERE settlement_order_sn = ?",
                Integer.class, locked.settlementOrder().getSn())).isZero();
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
        assertThat(WindJson.parseObject(payeeDetail.getContextVariables(), new TypeReference<Map<String, Object>>() {
        }))
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
        ClearingBatchDTO confirmedClearing = clearCommission(allocation);
        SettlementOrderDTO settlementOrder = settlementOrderApplicationService.createOrder(
                settlementRequest(confirmedClearing, destination), WindOperatorFactory.system());
        settlementOrderApplicationService.submitOrder(new SubmitSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(settlementOrder.getSn()), WindOperatorFactory.system());
        settlementOrderApplicationService.approveOrder(new ApproveSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setSettlementOrderSn(settlementOrder.getSn())
                .setSettlementApprovalRef("settlement-approval:" + allocation.suffix()),
                WindOperatorFactory.system());
        String settlementRunResultSn = prepareGate("SETTLEMENT_LOCK",
                settlementOrder.getSn(), "settlement-" + allocation.suffix());
        var beforeLock = snapshot(balance(allocation.beneficiary()));
        SettlementOrderDTO locked = settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(settlementOrder.getSn()), WindOperatorFactory.system());
        SettlementOrderDTO lockReplay = settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(settlementOrder.getSn()), WindOperatorFactory.system());

        assertThat(locked.getState()).isEqualTo(SettlementOrderState.LOCKED);
        assertThat(lockReplay.getLockFundsTransactionSn()).isEqualTo(locked.getLockFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeLock, snapshot(balance(allocation.beneficiary())),
                delta(allocation.beneficiary(), LedgerSubjectCode.AVAILABLE, -allocation.amount(), CURRENCY),
                delta(allocation.beneficiary(), LedgerSubjectCode.SETTLEMENT, allocation.amount(), CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(settlementOrder.getSn(), 1, 1, 2);
        return new LockedCommission(allocation, locked);
    }

    private ClearingBatchDTO clearCommission(CommissionAllocation allocation) {
        String commissionEvidenceRef = "commission-evidence-bundle:" + allocation.suffix();
        String splitRunResultSn = prepareGate("CLEARING_SPLITTABLE_IDENTIFY",
                allocation.detailSn(), "split-" + allocation.suffix(), commissionEvidenceRef);
        var beforeSplit = ledgerFactSnapshot();
        ClearingSplittableDetailDTO splittable = clearingSplittableDetailApplicationService
                .identifySplittableDetail(identifyRequest(allocation, splitRunResultSn), WindOperatorFactory.system());
        ClearingSplittableDetailDTO splittableReplay = clearingSplittableDetailApplicationService
                .identifySplittableDetail(identifyRequest(allocation, splitRunResultSn), WindOperatorFactory.system());
        ClearingSplitBatchDTO splitBatch = clearingSplitBatchApplicationService.createBatch(
                new CreateClearingSplitBatchRequest().setTenantId(TENANT_ID)
                        .setSplittableDetailSns(List.of(splittable.getSn())), WindOperatorFactory.system());
        prepareGate("CLEARING_SPLIT_CONFIRM_ITEM", splitBatch.getSn() + ":" + splittable.getSn(),
                "split-confirm-" + allocation.suffix(), commissionEvidenceRef);
        clearingSplitBatchApplicationService.submitBatch(new SubmitClearingSplitBatchRequest()
                .setTenantId(TENANT_ID).setSplitBatchSn(splitBatch.getSn()), WindOperatorFactory.system());
        ClearingSplitBatchDTO confirmedSplit = clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID)
                        .setSplitBatchSn(splitBatch.getSn()), WindOperatorFactory.system());

        assertThat(splittableReplay.getSn()).isEqualTo(splittable.getSn());
        assertThat(splittable.getBusinessLine()).isEqualTo(BUSINESS_LINE);
        assertThat(splittable.getSplitRuleCode()).isEqualTo(RULE_CODE);
        assertThat(splittable.getSplitRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(splittable.getReconciliationEvidenceRefs())
                .contains(commissionEvidenceRef)
                .anyMatch(ref -> ref.startsWith("RGE"))
                .anyMatch(ref -> ref.startsWith("run:"));
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
        ClearingCandidateDTO blockedCandidate = clearingCandidateApplicationService.createCandidate(
                candidateRequest, WindOperatorFactory.system());
        prepareGate("CLEARING_CONFIRM_ITEM", blockedCandidate.getSn(),
                "clearing-confirm-" + allocation.suffix(), commissionEvidenceRef);
        ClearingCandidateDTO candidate = clearingCandidateApplicationService.restoreCandidate(
                new RestoreClearingCandidateRequest().setTenantId(TENANT_ID)
                        .setCandidateSn(blockedCandidate.getSn()), WindOperatorFactory.system());
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
        assertThat(confirmedClearing.getState()).isEqualTo(ClearingBatchState.CONFIRMED);
        assertThat(clearingReplay.getFundsTransactionSn()).isEqualTo(confirmedClearing.getFundsTransactionSn());
        assertOnlyBalanceDeltas(beforeClearing, snapshot(balance(allocation.beneficiary())),
                delta(allocation.beneficiary(), LedgerSubjectCode.CLEARING, -allocation.amount(), CURRENCY),
                delta(allocation.beneficiary(), LedgerSubjectCode.AVAILABLE, allocation.amount(), CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(clearingBatch.getSn(), 1, 1, 2);

        return confirmedClearing;
    }

    private PayoutOrderDTO payout(LockedCommission commission, String suffix) {
        PayoutOrderDTO created = payoutOrderApplicationService.createOrder(
                new CreatePayoutOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(commission.settlementOrder().getSn()), WindOperatorFactory.system());
        PayoutOrderDTO createReplay = payoutOrderApplicationService.createOrder(
                new CreatePayoutOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(commission.settlementOrder().getSn()), WindOperatorFactory.system());
        String payoutRunResultSn = prepareGate(
                "PAYOUT_SUBMIT", created.getSn(), "payout-" + suffix);
        payoutOrderApplicationService.submitOrder(new SubmitPayoutOrderRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(created.getSn())
                .setPayoutAccountRef("payout-account:" + suffix)
                .setPayeeEndpointRef("payee-endpoint:" + suffix)
                .setChannelRef("channel:" + suffix)
                .setApprovalRef("payout-approval:" + suffix)
                .setExternalRuleVerificationEvidence(verifiedPayoutRule(suffix)), WindOperatorFactory.system());
        HandlePayoutReceiptRequest receipt = new HandlePayoutReceiptRequest()
                .setTenantId(TENANT_ID)
                .setPayoutOrderSn(created.getSn())
                .setChannelRef("channel:" + suffix)
                .setExternalReceiptRef("receipt:" + suffix)
                .setExternalReference("external-payout:" + suffix)
                .setState(PayoutOrderState.SUCCEEDED)
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

    private String prepareGate(String stageKind,
                               String objectSn,
                               String suffix) {
        return prepareGate(stageKind, objectSn, suffix, "report:" + suffix);
    }

    private String prepareGate(String stageKind,
                               String objectSn,
                               String suffix,
                               String evidenceRef) {
        String batchSn = "commission-reconciliation:" + suffix;
        String referenceSourceRef = "internal:" + suffix;
        String comparisonSourceRef = "evidence:" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                stageKind, objectSn, RULE_VERSION, evidenceRef,
                referenceSourceRef, comparisonSourceRef);
        return reconciliationRunResultApplicationService.executeStrictExact(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn),
                WindOperatorFactory.system()).getSn();
    }

    private void prepareAccount(FundsAccountId accountId) {
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
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

package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.clearing.ClearingBatchApplicationService;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingBatchApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ClearingBatchState;
import com.wind.funds.reconciliation.enums.ClearingCandidateState;
import com.wind.funds.reconciliation.model.dto.ClearingBatchDTO;
import com.wind.funds.reconciliation.model.query.ClearingBatchQuery;
import com.wind.funds.reconciliation.model.request.ConfirmClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.ReplaceClearingBatchCandidatesRequest;
import com.wind.funds.reconciliation.model.request.ReturnClearingBatchToDraftRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingBatchRequest;
import com.wind.funds.transaction.application.FundsClearingTransactionService;
import com.wind.funds.transaction.application.impl.FundsClearingTransactionServiceImpl;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.operator.WindOperator;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 清算批次候选锁定、最终准入和资金确认集成测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        ClearingBatchApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class ClearingBatchApplicationServiceTests extends FundsTransactionFlowTestSupport {

    private static final String BUSINESS_LINE = "ACQUIRING";

    private static final String CLEARING_PERIOD = "2026-07-28";

    @Autowired
    private ClearingBatchApplicationService clearingBatchApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FailAfterConfirmFundsClearingTransactionService failAfterConfirmFundsClearingTransactionService;

    @BeforeEach
    void clearClearingFacts() {
        jdbcTemplate.update("DELETE FROM t_clearing_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch");
        jdbcTemplate.update("DELETE FROM t_clearing_candidate");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    @Test
    void testDraftReturnAndConfirmShouldKeepCandidateAndFundsFactsConsistent() {
        FundsAccountId accountId = fundingAccount("cb_merchant");
        prepareClearingBalance(accountId, 1_000L, "success");
        String first = prepareCandidate(accountId, 600L, "001");
        String second = prepareCandidate(accountId, 400L, "002");

        ClearingBatchDTO batch = clearingBatchApplicationService.createBatch(
                new CreateClearingBatchRequest().setTenantId(TENANT_ID).setCandidateSns(List.of(first)),
                WindOperatorFactory.system());
        batch = clearingBatchApplicationService.replaceDraftCandidates(
                new ReplaceClearingBatchCandidatesRequest()
                        .setTenantId(TENANT_ID)
                        .setClearingBatchSn(batch.getSn())
                        .setCandidateSns(List.of(second, first)),
                WindOperatorFactory.system());
        batch = clearingBatchApplicationService.submitBatch(new SubmitClearingBatchRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSn(batch.getSn()), WindOperatorFactory.system());

        assertThat(batch.getState()).isEqualTo(ClearingBatchState.REVIEWING);
        assertThat(batch.getCandidateCount()).isEqualTo(2);
        assertThat(batch.getTotalAmount()).isEqualTo(1_000L);
        assertThat(candidateStatuses(first, second)).containsOnly(ClearingCandidateState.LOCKED.name());

        batch = clearingBatchApplicationService.returnToDraft(new ReturnClearingBatchToDraftRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSn(batch.getSn())
                .setReason("复核清算范围"), WindOperatorFactory.system());
        assertThat(batch.getState()).isEqualTo(ClearingBatchState.DRAFT);
        assertThat(candidateStatuses(first, second)).containsOnly(ClearingCandidateState.READY.name());

        batch = clearingBatchApplicationService.submitBatch(new SubmitClearingBatchRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSn(batch.getSn()), WindOperatorFactory.system());
        var before = snapshot(balance(accountId));
        ClearingBatchDTO confirmed = clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID).setClearingBatchSn(batch.getSn()),
                WindOperatorFactory.system());

        assertThat(confirmed.getState()).isEqualTo(ClearingBatchState.CONFIRMED);
        assertThat(confirmed.getFundsTransactionSn()).isNotBlank();
        assertThat(candidateStatuses(first, second)).containsOnly(ClearingCandidateState.CLEARED.name());
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.CLEARING, -1_000L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 1_000L, CURRENCY));
        assertThat(clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID).setClearingBatchSn(batch.getSn()),
                WindOperatorFactory.system()).getFundsTransactionSn()).isEqualTo(confirmed.getFundsTransactionSn());
    }

    @Test
    void testDeterministicFundsRejectionShouldFailBatchAndBlockCandidates() {
        FundsAccountId accountId = fundingAccount("cb_insufficient");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        String candidateSn = prepareCandidate(accountId, 100L, "011");
        ClearingBatchDTO batch = createAndSubmit(candidateSn);

        assertThatThrownBy(() -> clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID).setClearingBatchSn(batch.getSn()),
                WindOperatorFactory.system()))
                .isInstanceOf(LedgerPostingRejectedException.class)
                .hasMessageContaining("账本余额不足");

        ClearingBatchDTO failed = clearingBatchApplicationService.getBatch(TENANT_ID, batch.getSn());
        assertThat(failed.getState()).isEqualTo(ClearingBatchState.FAILED);
        assertThat(failed.getFundsTransactionSn()).isNotBlank();
        assertThat(candidateStatuses(candidateSn)).containsExactly(ClearingCandidateState.BLOCKED.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_funds_transaction WHERE sn = ?", String.class,
                failed.getFundsTransactionSn())).isEqualTo(FundsTransactionState.FAILED.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ledger_transaction WHERE funds_transaction_sn = ?", Integer.class,
                failed.getFundsTransactionSn())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT error_code FROM t_funds_transaction_detail WHERE transaction_sn = ?", String.class,
                failed.getFundsTransactionSn())).isEqualTo("LEDGER_POSTING_REJECTED");
    }

    @Test
    void testFailureBeforeFundsFactShouldKeepReviewingAndLocked() {
        FundsAccountId missingAccount = fundingAccount("cb_missing");
        String candidateSn = prepareCandidate(missingAccount, 100L, "021");
        ClearingBatchDTO batch = createAndSubmit(candidateSn);

        assertThatThrownBy(() -> clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID).setClearingBatchSn(batch.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("资金主体不存在");

        assertThat(clearingBatchApplicationService.getBatch(TENANT_ID, batch.getSn()).getState())
                .isEqualTo(ClearingBatchState.REVIEWING);
        assertThat(candidateStatuses(candidateSn)).containsExactly(ClearingCandidateState.LOCKED.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_transaction WHERE business_sn = ?", Integer.class,
                batch.getSn())).isZero();
    }

    @Test
    void testTechnicalFailureAfterFundsPostingShouldRollbackAndKeepReviewingAndLocked() {
        FundsAccountId accountId = fundingAccount("cb_unknown");
        prepareClearingBalance(accountId, 100L, "unknown");
        String candidateSn = prepareCandidate(accountId, 100L, "031");
        ClearingBatchDTO batch = createAndSubmit(candidateSn);
        var before = snapshot(balance(accountId));
        failAfterConfirmFundsClearingTransactionService.failNext();

        assertThatThrownBy(() -> clearingBatchApplicationService.confirmBatch(
                new ConfirmClearingBatchRequest().setTenantId(TENANT_ID).setClearingBatchSn(batch.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("simulated clearing result unknown");

        assertThat(clearingBatchApplicationService.getBatch(TENANT_ID, batch.getSn()).getState())
                .isEqualTo(ClearingBatchState.REVIEWING);
        assertThat(candidateStatuses(candidateSn)).containsExactly(ClearingCandidateState.LOCKED.name());
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.CLEARING, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_transaction WHERE business_sn = ?", Integer.class,
                batch.getSn())).isZero();
    }

    @Test
    void testCreateBatchShouldRejectUnboundedCandidateListBeforeQueryingCandidates() {
        List<String> candidateSns = IntStream.rangeClosed(1,
                        CreateClearingBatchRequest.MAX_CANDIDATE_COUNT + 1)
                .mapToObj(index -> "CLC_LIMIT_" + index)
                .toList();

        assertThatThrownBy(() -> clearingBatchApplicationService.createBatch(
                new CreateClearingBatchRequest().setTenantId(TENANT_ID).setCandidateSns(candidateSns),
                WindOperatorFactory.system()))
                .hasMessageContaining("单个清算批次候选数量不能超过 1000");
    }

    @Test
    void testQueryShouldDiscoverClearingBatchesByStatusAndAge() {
        FundsAccountId accountId = fundingAccount("cb_query");
        String candidateSn = prepareCandidate(accountId, 100L, "041");
        ClearingBatchDTO created = clearingBatchApplicationService.createBatch(
                new CreateClearingBatchRequest().setTenantId(TENANT_ID).setCandidateSns(List.of(candidateSn)),
                WindOperatorFactory.system());

        WindPagination<ClearingBatchDTO> result = clearingBatchApplicationService.queryBatches(
                new ClearingBatchQuery()
                        .setState(ClearingBatchState.DRAFT)
                        .setGmtModifiedMax(LocalDateTime.now().plusMinutes(1)),
                DefaultPageQueryOptions.defaults(10));

        assertThat(result.getRecords()).extracting(ClearingBatchDTO::getSn).containsExactly(created.getSn());
    }

    private ClearingBatchDTO createAndSubmit(String candidateSn) {
        ClearingBatchDTO batch = clearingBatchApplicationService.createBatch(
                new CreateClearingBatchRequest().setTenantId(TENANT_ID).setCandidateSns(List.of(candidateSn)),
                WindOperatorFactory.system());
        return clearingBatchApplicationService.submitBatch(new SubmitClearingBatchRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSn(batch.getSn()), WindOperatorFactory.system());
    }

    private void prepareClearingBalance(FundsAccountId accountId, long amount, String suffix) {
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        FundsAccountId payer = fundingAccount("cb_payer_" + suffix);
        ensureFundingAccount(payer);
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payer)
                .setFundsSourceAccountId(FundsAccountId.immutable(
                        "external_clearing_batch_" + suffix, DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("CLEARING_BATCH_TOPUP_CHANNEL_" + suffix)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setBusinessScene("CLEARING_BATCH_TEST_TOPUP")
                .setBusinessSn("CLEARING_BATCH_TEST_TOPUP_" + suffix), WindOperatorFactory.system());
        directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(accountId)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.CLEARING)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setBusinessScene("CLEARING_BATCH_TEST_PAY")
                .setBusinessSn("CLEARING_BATCH_TEST_PAY_" + suffix), WindOperatorFactory.system());
    }

    private String prepareCandidate(FundsAccountId accountId, long amount, String suffix) {
        String transactionSn = "clearing_source_tx_" + suffix;
        String transactionDetailSn = "clearing_source_detail_" + suffix;
        String reconciliationBatchSn = "clearing_recon_batch_" + suffix;
        String routeSnapshot = "{\"routeCode\":\"DIRECT_PAY_STANDARD\",\"routeVersion\":\"v1\",\"legs\":[{\"legId\":\""
                + suffix + "\"}]}";
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction (
                            sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                            status, amount, currency, completed_amount, refunded_amount, declined_amount,
                            route_snapshot, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, 0)
                        """,
                transactionSn, TENANT_ID, FundsTransactionMode.DIRECT.name(), DefaultFundsTransactionType.PAY.name(),
                BUSINESS_LINE, "clearing_source_business_" + suffix, FundsTransactionState.CLOSED.name(), amount,
                CURRENCY.name(), amount, routeSnapshot);
        String referenceSourceRef = "internal:" + transactionDetailSn;
        String comparisonSourceRef = "external:" + transactionDetailSn;
        String candidateSn = "CLC" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, reconciliationBatchSn,
                "CLEARING_CONFIRM_ITEM", candidateSn, "recon-rule-1",
                "report:clearing-" + suffix, referenceSourceRef, comparisonSourceRef);
        String runResultSn = reconciliationRunResultApplicationService.executeStrictExact(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(reconciliationBatchSn),
                WindOperatorFactory.system()).getSn();
        String resultDigest = jdbcTemplate.queryForObject(
                "SELECT result_digest FROM t_reconciliation_run_result WHERE sn = ?", String.class, runResultSn);
        String splittableDetailSn = "CSD" + suffix;
        String sourceDigest = FundsStableHashSupport.sha256Json(Map.of("source", suffix));
        String candidateDigest = FundsStableHashSupport.sha256Json(Map.of("candidate", suffix));
        jdbcTemplate.update("""
                        INSERT INTO t_clearing_candidate (
                            sn, tenant_id, split_result_sn, split_batch_sn, splittable_detail_sn,
                            subject_type, subject_id, currency, business_line, clearing_period, amount,
                            funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn,
                            posting_plan_sn, ledger_entry_sn, route_snapshot_digest, clearing_available_time,
                            clearing_rule_code, clearing_rule_version, gate_evidence_ref,
                            reconciliation_evidence_refs, source_digest,
                            candidate_digest, active_splittable_detail_sn, status, created_by
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'READY', 'system')
                        """,
                candidateSn, TENANT_ID, "split_result_" + suffix, "split_batch_" + suffix,
                splittableDetailSn, accountId.type(), accountId.id(), CURRENCY.name(), BUSINESS_LINE,
                CLEARING_PERIOD, amount, transactionSn, transactionDetailSn, "ledger_tx_" + suffix,
                "posting_plan_" + suffix, "ledger_entry_" + suffix,
                FundsStableHashSupport.sha256Json(Map.of("routeSnapshot", routeSnapshot)),
                LocalDateTime.now().minusMinutes(1), "MERCHANT_DAILY_CLEARING", "1",
                "gate-evidence:" + suffix, "[\"report:clearing-" + suffix + "\"]", sourceDigest, candidateDigest,
                splittableDetailSn);
        return candidateSn;
    }

    private List<String> candidateStatuses(String... candidateSns) {
        return jdbcTemplate.queryForList("""
                        SELECT status FROM t_clearing_candidate
                        WHERE sn IN (%s)
                        ORDER BY sn
                        """.formatted(String.join(",", java.util.Collections.nCopies(candidateSns.length, "?"))),
                String.class, (Object[]) candidateSns);
    }

    @Configuration
    @Import({
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class,
            ClearingBatchApplicationServiceImpl.class
    })
    static class Config {

        @Bean
        @Primary
        FailAfterConfirmFundsClearingTransactionService failAfterConfirmFundsClearingTransactionService(
                FundsClearingTransactionServiceImpl delegate) {
            return new FailAfterConfirmFundsClearingTransactionService(delegate);
        }
    }

    private static final class FailAfterConfirmFundsClearingTransactionService
            implements FundsClearingTransactionService {

        private final FundsClearingTransactionService delegate;

        private boolean failNext;

        private FailAfterConfirmFundsClearingTransactionService(FundsClearingTransactionService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String confirm(FundsClearingConfirmRequest request,
                              WindOperator operator) {
            String result = delegate.confirm(request, operator);
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("simulated clearing result unknown");
            }
            return result;
        }

        private void failNext() {
            failNext = true;
        }
    }
}

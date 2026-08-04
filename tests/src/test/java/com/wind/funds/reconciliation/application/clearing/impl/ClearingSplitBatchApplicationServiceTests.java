package com.wind.funds.reconciliation.application.clearing.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.clearing.ClearingSplitBatchApplicationService;
import com.wind.funds.reconciliation.application.clearing.ClearingCandidateApplicationService;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingCandidateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ClearingSplitBatchStatus;
import com.wind.funds.reconciliation.enums.ClearingCandidateStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ClearingSplitBatchDTO;
import com.wind.funds.reconciliation.model.dto.ClearingSplitResultSnapshotDTO;
import com.wind.funds.reconciliation.model.dto.ClearingCandidateDTO;
import com.wind.funds.reconciliation.model.query.ClearingCandidateQuery;
import com.wind.funds.reconciliation.model.query.ClearingSplitBatchQuery;
import com.wind.funds.reconciliation.model.request.CancelClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.ExcludeClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.LockClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.ReleaseClearingCandidateLockRequest;
import com.wind.funds.reconciliation.model.request.RestoreClearingCandidateRequest;
import com.wind.funds.reconciliation.services.impl.ClearingSettlementGateConsumerServiceImpl;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingSplitBatchRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 清分批次和不可变结果快照流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ClearingSplitBatchApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClearingSplitBatchApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String SUBJECT_ID = "merchant_funding_001";

    private static final String BUSINESS_LINE = "ACQUIRING";

    private static final String SPLIT_PERIOD = "2026-07-27";

    private static final String SPLIT_RULE_CODE = "MERCHANT_DAILY_SPLIT";

    private static final String SPLIT_RULE_VERSION = "1";

    @Autowired
    private ClearingSplitBatchApplicationService clearingSplitBatchApplicationService;

    @Autowired
    private ClearingCandidateApplicationService clearingCandidateApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：清分批次公共状态契约只表达草稿、复核、确认和取消。
     * 结果：CONFIRMED 是不可变结果快照形成后的终态，不保留未落地的候选消费关闭状态。
     */
    @Test
    void testStatusContractShouldUseConfirmedAsTerminalState() {
        assertThat(ClearingSplitBatchStatus.values()).containsExactly(
                ClearingSplitBatchStatus.DRAFT,
                ClearingSplitBatchStatus.REVIEWING,
                ClearingSplitBatchStatus.CONFIRMED,
                ClearingSplitBatchStatus.CANCELLED);
    }

    @BeforeEach
    void prepareFacts() {
        jdbcTemplate.update("DELETE FROM t_clearing_candidate");
        jdbcTemplate.update("DELETE FROM t_clearing_split_result_snapshot");
        jdbcTemplate.update("DELETE FROM t_clearing_split_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_split_batch");
        jdbcTemplate.update("DELETE FROM t_clearing_splittable_detail");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM t_funds_transaction");
    }

    /**
     * 场景：同一账务主体、币种、业务线、周期和规则的两条明细创建清分批次。
     * 结果：形成单主体 DRAFT，重复创建命中原批次，成员和金额摘要不重复。
     */
    @Test
    void testCreateShouldBuildSingleSubjectDraftIdempotently() {
        String first = prepareSplittableDetail("001", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 600L);
        String second = prepareSplittableDetail("002", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 400L);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplitBatchDTO created = clearingSplitBatchApplicationService.createBatch(
                createRequest(first, second), WindOperatorFactory.system());
        ClearingSplitBatchDTO replay = clearingSplitBatchApplicationService.createBatch(
                createRequest(second, first), WindOperatorFactory.system());

        assertThat(created.getStatus()).isEqualTo(ClearingSplitBatchStatus.DRAFT);
        assertThat(created.getSubjectType()).isEqualTo("FUNDING_ACCOUNT");
        assertThat(created.getSubjectId()).isEqualTo(SUBJECT_ID);
        assertThat(created.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(created.getBusinessLine()).isEqualTo(BUSINESS_LINE);
        assertThat(created.getSplitPeriod()).isEqualTo(SPLIT_PERIOD);
        assertThat(created.getDetailCount()).isEqualTo(2);
        assertThat(created.getTotalAmount()).isEqualTo(1000L);
        assertThat(created.getBatchDigest()).hasSize(64);
        assertThat(replay.getSn()).isEqualTo(created.getSn());
        assertThat(batchCount()).isOne();
        assertThat(memberCount()).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：宿主按状态和修改时间扫描待处理清分批次。
     * 结果：只返回符合条件的当前租户批次。
     */
    @Test
    void testQueryShouldDiscoverSplitBatchesByStatusAndAge() {
        String detail = prepareSplittableDetail("003", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 1000L);
        ClearingSplitBatchDTO created = clearingSplitBatchApplicationService.createBatch(
                createRequest(detail), WindOperatorFactory.system());
        jdbcTemplate.update("""
                INSERT INTO t_clearing_split_batch (
                    sn, tenant_id, subject_type, subject_id, currency, business_line, split_period,
                    split_rule_code, split_rule_version, detail_count, total_amount, member_digest,
                    batch_digest, active_batch_digest, status, created_by, submitted_by, submitted_time,
                    confirmed_by, confirmed_time, cancelled_by, cancelled_time, cancel_reason
                )
                SELECT sn, 2, subject_type, subject_id, currency, business_line, split_period,
                       split_rule_code, split_rule_version, detail_count, total_amount, member_digest,
                       batch_digest, active_batch_digest, status, created_by, submitted_by, submitted_time,
                       confirmed_by, confirmed_time, cancelled_by, cancelled_time, cancel_reason
                FROM t_clearing_split_batch
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, created.getSn());

        WindPagination<ClearingSplitBatchDTO> result = clearingSplitBatchApplicationService.queryBatches(
                new ClearingSplitBatchQuery()
                        .setStatus(ClearingSplitBatchStatus.DRAFT)
                        .setGmtModifiedMax(LocalDateTime.now().plusMinutes(1)),
                DefaultPageQueryOptions.defaults(10));

        assertThat(result.getRecords()).extracting(ClearingSplitBatchDTO::getSn).containsExactly(created.getSn());
        assertThatThrownBy(() -> clearingSplitBatchApplicationService.queryBatches(
                new ClearingSplitBatchQuery().setTenantId(2L),
                DefaultPageQueryOptions.defaults(10)))
                .hasMessageContaining("tenantId 与当前租户不一致");
    }

    /**
     * 场景：调用方一次提交超过单批容量的可清分明细。
     * 结果：在查询明细和创建批次前快速失败。
     */
    @Test
    void testCreateShouldRejectUnboundedDetailListBeforeQueryingDetails() {
        List<String> detailSns = IntStream.rangeClosed(1, 1001)
                .mapToObj(index -> "CSD_LIMIT_" + index)
                .toList();

        assertThatThrownBy(() -> clearingSplitBatchApplicationService.createBatch(
                new CreateClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplittableDetailSns(detailSns),
                WindOperatorFactory.system()))
                .hasMessageContaining("单个清分批次明细数量不能超过 1000");
    }

    /**
     * 场景：调用方尝试把不同账务主体的明细放入一个清分批次。
     * 结果：创建失败且不留下批次或成员。
     */
    @Test
    void testCreateShouldRejectMixedSubjects() {
        String first = prepareSplittableDetail("011", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 600L);
        String second = prepareSplittableDetail("012", "merchant_funding_002",
                CurrencyIsoCode.USD, BUSINESS_LINE, 400L);

        assertThatThrownBy(() -> clearingSplitBatchApplicationService.createBatch(
                createRequest(first, second), WindOperatorFactory.system()))
                .hasMessageContaining("同一账务主体");
        assertThat(batchCount()).isZero();
        assertThat(memberCount()).isZero();
    }

    /**
     * 场景：调用方尝试跨币种或跨业务线混批。
     * 结果：批次边界校验快速失败。
     */
    @Test
    void testCreateShouldRejectMixedCurrencyAndBusinessLine() {
        String usd = prepareSplittableDetail("021", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 600L);
        String eur = prepareSplittableDetail("022", SUBJECT_ID, CurrencyIsoCode.EUR, BUSINESS_LINE, 400L);

        assertThatThrownBy(() -> clearingSplitBatchApplicationService.createBatch(
                createRequest(usd, eur), WindOperatorFactory.system()))
                .hasMessageContaining("同一币种");

        String payout = prepareSplittableDetail("023", SUBJECT_ID, CurrencyIsoCode.USD, "GLOBAL_PAYOUT", 400L);
        assertThatThrownBy(() -> clearingSplitBatchApplicationService.createBatch(
                createRequest(usd, payout), WindOperatorFactory.system()))
                .hasMessageContaining("同一业务线");
    }

    /**
     * 场景：一条明细已被有效批次占用，随后取消原批次再重新入批。
     * 结果：有效占用期间数据库唯一约束拒绝重复入批，取消后允许新批次承接。
     */
    @Test
    void testCancelShouldReleaseActiveMembershipForRebatching() {
        String first = prepareSplittableDetail("031", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 600L);
        String second = prepareSplittableDetail("032", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 400L);
        ClearingSplitBatchDTO original = clearingSplitBatchApplicationService.createBatch(
                createRequest(first), WindOperatorFactory.system());

        assertThatThrownBy(() -> clearingSplitBatchApplicationService.createBatch(
                createRequest(first, second), WindOperatorFactory.system()))
                .hasMessageContaining("已进入其他有效清分批次");

        ClearingSplitBatchDTO cancelled = clearingSplitBatchApplicationService.cancelBatch(
                new CancelClearingSplitBatchRequest()
                        .setTenantId(TENANT_ID)
                        .setSplitBatchSn(original.getSn())
                        .setReason("重新选择清分范围"),
                WindOperatorFactory.system());
        ClearingSplitBatchDTO recreated = clearingSplitBatchApplicationService.createBatch(
                createRequest(first), WindOperatorFactory.system());

        assertThat(cancelled.getStatus()).isEqualTo(ClearingSplitBatchStatus.CANCELLED);
        assertThat(recreated.getSn()).isNotEqualTo(original.getSn());
        assertThat(recreated.getDetailCount()).isOne();
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：批次通过复核并确认，随后重复确认并查询结果。
     * 结果：每条成员生成一条不可变快照，重复确认不重复写，且没有账务副作用。
     */
    @Test
    void testConfirmShouldFreezeResultSnapshotsIdempotently() {
        String first = prepareSplittableDetail("041", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 600L);
        String second = prepareSplittableDetail("042", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 400L);
        ClearingSplitBatchDTO created = clearingSplitBatchApplicationService.createBatch(
                createRequest(first, second), WindOperatorFactory.system());
        ClearingSplitBatchDTO reviewing = clearingSplitBatchApplicationService.submitBatch(
                new SubmitClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplitBatchDTO confirmed = clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());
        ClearingSplitBatchDTO replay = clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());
        List<ClearingSplitResultSnapshotDTO> snapshots = clearingSplitBatchApplicationService
                .getResultSnapshots(TENANT_ID, created.getSn());

        assertThat(reviewing.getStatus()).isEqualTo(ClearingSplitBatchStatus.REVIEWING);
        assertThat(confirmed.getStatus()).isEqualTo(ClearingSplitBatchStatus.CONFIRMED);
        assertThat(replay.getSn()).isEqualTo(confirmed.getSn());
        assertThat(snapshots).hasSize(2)
                .allSatisfy(snapshot -> {
                    assertThat(snapshot.getSplitBatchSn()).isEqualTo(created.getSn());
                    assertThat(snapshot.getSubjectId()).isEqualTo(SUBJECT_ID);
                    assertThat(snapshot.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(snapshot.getBusinessLine()).isEqualTo(BUSINESS_LINE);
                    assertThat(snapshot.getAmount()).isPositive();
                    assertThat(snapshot.getSnapshotDigest()).hasSize(64);
                });
        assertThat(snapshotCount()).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResultSnapshotShouldReadBlankEvidenceRefsAsEmptyList() {
        String detailSn = prepareConfirmedSnapshot("043");
        String snapshotSn = snapshotSn(detailSn);
        jdbcTemplate.update("UPDATE t_clearing_split_result_snapshot SET reconciliation_evidence_refs = ' ' WHERE sn = ?",
                snapshotSn);

        ClearingSplitResultSnapshotDTO snapshot = clearingSplitBatchApplicationService.getResultSnapshots(
                        TENANT_ID,
                        jdbcTemplate.queryForObject(
                                "SELECT split_batch_sn FROM t_clearing_split_batch_detail WHERE splittable_detail_sn = ?",
                                String.class,
                                detailSn))
                .stream()
                .filter(item -> snapshotSn.equals(item.getSn()))
                .findFirst()
                .orElseThrow();

        assertThat(snapshot.getReconciliationEvidenceRefs()).isEmpty();
    }

    /**
     * 场景：批次进入复核后，来源交易发生退款并推进版本。
     * 结果：确认失败关闭，不生成部分快照，批次仍保持 REVIEWING。
     */
    @Test
    void testConfirmShouldFailWhenSourceTransactionChanges() {
        String detail = prepareSplittableDetail("051", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 1000L);
        ClearingSplitBatchDTO created = clearingSplitBatchApplicationService.createBatch(
                createRequest(detail), WindOperatorFactory.system());
        clearingSplitBatchApplicationService.submitBatch(
                new SubmitClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());
        jdbcTemplate.update("""
                UPDATE t_funds_transaction
                SET refunded_amount = 100, version = version + 1
                WHERE sn = ?
                """, transactionSn("051"));

        assertThatThrownBy(() -> clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("来源交易已变化");
        assertThat(clearingSplitBatchApplicationService.getBatch(TENANT_ID, created.getSn()).getStatus())
                .isEqualTo(ClearingSplitBatchStatus.REVIEWING);
        assertThat(snapshotCount()).isZero();
    }

    /**
     * 场景：来源交易聚合因其他合法明细推进乐观锁版本，但当前明细、退款和 RouteSnapshot 均未变化。
     * 结果：清分确认仍消费当前不可变明细事实，不被聚合技术版本误阻断。
     */
    @Test
    void testConfirmShouldAllowUnrelatedAggregateVersionAdvance() {
        String detail = prepareSplittableDetail("052", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 1000L);
        ClearingSplitBatchDTO created = clearingSplitBatchApplicationService.createBatch(
                createRequest(detail), WindOperatorFactory.system());
        clearingSplitBatchApplicationService.submitBatch(
                new SubmitClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());
        jdbcTemplate.update("UPDATE t_funds_transaction SET version = version + 1 WHERE sn = ?",
                transactionSn("052"));

        ClearingSplitBatchDTO confirmed = clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());

        assertThat(confirmed.getStatus()).isEqualTo(ClearingSplitBatchStatus.CONFIRMED);
        assertThat(snapshotCount()).isOne();
    }

    /**
     * 场景：批次进入复核后，可清分成员金额被异常改写但来源交易版本未变化。
     * 结果：确认按批次摘要失败关闭，不以漂移后的成员生成结果快照。
     */
    @Test
    void testConfirmShouldFailWhenBatchMemberFactsChange() {
        String detail = prepareSplittableDetail("061", SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 1000L);
        ClearingSplitBatchDTO created = clearingSplitBatchApplicationService.createBatch(
                createRequest(detail), WindOperatorFactory.system());
        clearingSplitBatchApplicationService.submitBatch(
                new SubmitClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system());
        jdbcTemplate.update("UPDATE t_clearing_splittable_detail SET amount = 999 WHERE sn = ?", detail);

        assertThatThrownBy(() -> clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(created.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("批次摘要与当前成员事实不一致");
        assertThat(clearingSplitBatchApplicationService.getBatch(TENANT_ID, created.getSn()).getStatus())
                .isEqualTo(ClearingSplitBatchStatus.REVIEWING);
        assertThat(snapshotCount()).isZero();
    }

    /**
     * 场景：已确认清分快照生成清算候选，重复扫描同一快照和账期参数。
     * 结果：账期已到且 Gate 通过时为 READY，重复请求复用同一候选，不产生资金事实。
     */
    @Test
    void testCreateCandidateShouldFreezeSnapshotFactsIdempotently() {
        String detail = prepareConfirmedSnapshot("071");
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        CreateClearingCandidateRequest request = createCandidateRequest(
                snapshotSn(detail), LocalDateTime.now().minusMinutes(1));

        ClearingCandidateDTO created = clearingCandidateApplicationService.createCandidate(
                request, WindOperatorFactory.system());
        ClearingCandidateDTO replay = clearingCandidateApplicationService.createCandidate(
                request, WindOperatorFactory.system());

        assertThat(created.getStatus()).isEqualTo(ClearingCandidateStatus.READY);
        assertThat(created.getAmount()).isEqualTo(1000L);
        assertThat(created.getSplittableDetailSn()).isEqualTo(detail);
        assertThat(created.getCandidateDigest()).hasSize(64);
        assertThat(replay.getSn()).isEqualTo(created.getSn());
        assertThat(candidateCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：宿主按状态和账期扫描可继续处理的清算候选。
     * 结果：只返回账期上界内的候选。
     */
    @Test
    void testQueryShouldDiscoverCandidatesByStatusAndAvailableTime() {
        String detail = prepareConfirmedSnapshot("074");
        ClearingCandidateDTO created = clearingCandidateApplicationService.createCandidate(
                createCandidateRequest(snapshotSn(detail), LocalDateTime.now().minusMinutes(1)),
                WindOperatorFactory.system());

        WindPagination<ClearingCandidateDTO> result = clearingCandidateApplicationService.queryCandidates(
                new ClearingCandidateQuery()
                        .setStatus(ClearingCandidateStatus.READY)
                        .setClearingAvailableTimeMax(LocalDateTime.now()),
                DefaultPageQueryOptions.defaults(10));

        assertThat(result.getRecords()).extracting(ClearingCandidateDTO::getSn).containsExactly(created.getSn());
    }

    /**
     * 场景：同一可清分明细以不同最早清算时间再次生成候选。
     * 结果：有效候选唯一占用阻止重复候选，不能依靠不同摘要绕过。
     */
    @Test
    void testCreateCandidateShouldRejectSecondActiveCandidateForSameDetail() {
        String detail = prepareConfirmedSnapshot("073");
        String splitResultSn = snapshotSn(detail);
        clearingCandidateApplicationService.createCandidate(createCandidateRequest(
                splitResultSn, LocalDateTime.now().minusMinutes(2)), WindOperatorFactory.system());

        assertThatThrownBy(() -> clearingCandidateApplicationService.createCandidate(
                createCandidateRequest(splitResultSn, LocalDateTime.now().minusMinutes(1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("同一可清分明细已有有效清算候选");
        assertThat(candidateCount()).isOne();
    }

    /**
     * 场景：候选先等待账期，随后被排除，再由上层恢复并锁定到清算批次。
     * 结果：恢复重新执行账期和 Gate，只有 READY 才能锁定；锁定不创建清算资金事实。
     */
    @Test
    void testCandidateShouldSupportWaitingExclusionRestoreAndLockWithoutFundsMutation() {
        String detail = prepareConfirmedSnapshot("072");
        String splitResultSn = snapshotSn(detail);
        ClearingCandidateDTO waiting = clearingCandidateApplicationService.createCandidate(
                createCandidateRequest(splitResultSn, LocalDateTime.now().plusHours(1)),
                WindOperatorFactory.system());
        assertThat(waiting.getStatus()).isEqualTo(ClearingCandidateStatus.WAITING_PERIOD);

        ClearingCandidateDTO excluded = clearingCandidateApplicationService.excludeCandidate(
                new ExcludeClearingCandidateRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(waiting.getSn())
                        .setReason("退款核对待人工确认"),
                WindOperatorFactory.system());
        assertThat(excluded.getStatus()).isEqualTo(ClearingCandidateStatus.EXCLUDED);

        jdbcTemplate.update("""
                UPDATE t_clearing_candidate
                SET clearing_available_time = ?
                WHERE tenant_id = ? AND sn = ?
                """, LocalDateTime.now().minusMinutes(1), TENANT_ID, waiting.getSn());
        ClearingCandidateDTO restored = clearingCandidateApplicationService.restoreCandidate(
                new RestoreClearingCandidateRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(waiting.getSn()),
                WindOperatorFactory.system());
        LedgerFactSnapshot beforeLock = ledgerFactSnapshot(jdbcTemplate);
        ClearingCandidateDTO locked = clearingCandidateApplicationService.lockCandidate(
                new LockClearingCandidateRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(restored.getSn())
                        .setClearingBatchSn("CLEARING-BATCH-001"),
                WindOperatorFactory.system());

        assertThat(restored.getStatus()).isEqualTo(ClearingCandidateStatus.READY);
        assertThat(locked.getStatus()).isEqualTo(ClearingCandidateStatus.LOCKED);
        assertThat(locked.getLockedClearingBatchSn()).isEqualTo("CLEARING-BATCH-001");

        ClearingCandidateDTO unknownResult = clearingCandidateApplicationService.restoreCandidate(
                new RestoreClearingCandidateRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(locked.getSn()),
                WindOperatorFactory.system());
        assertThat(unknownResult.getStatus()).isEqualTo(ClearingCandidateStatus.LOCKED);
        assertThatThrownBy(() -> clearingCandidateApplicationService.releaseCandidateLock(
                new ReleaseClearingCandidateLockRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(locked.getSn())
                        .setClearingBatchSn("CLEARING-BATCH-OTHER"),
                WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class);

        ClearingCandidateDTO released = clearingCandidateApplicationService.releaseCandidateLock(
                new ReleaseClearingCandidateLockRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(locked.getSn())
                        .setClearingBatchSn("CLEARING-BATCH-001"),
                WindOperatorFactory.system());
        ClearingCandidateDTO replayRelease = clearingCandidateApplicationService.releaseCandidateLock(
                new ReleaseClearingCandidateLockRequest()
                        .setTenantId(TENANT_ID)
                        .setCandidateSn(locked.getSn())
                        .setClearingBatchSn("CLEARING-BATCH-001"),
                WindOperatorFactory.system());

        assertThat(released.getStatus()).isEqualTo(ClearingCandidateStatus.READY);
        assertThat(released.getLockedClearingBatchSn()).isNull();
        assertThat(replayRelease.getSn()).isEqualTo(released.getSn());
        assertLedgerFactsUnchanged(jdbcTemplate, beforeLock);
    }

    private String prepareConfirmedSnapshot(String suffix) {
        String detail = prepareSplittableDetail(suffix, SUBJECT_ID, CurrencyIsoCode.USD, BUSINESS_LINE, 1000L);
        ClearingSplitBatchDTO batch = clearingSplitBatchApplicationService.createBatch(
                createRequest(detail), WindOperatorFactory.system());
        clearingSplitBatchApplicationService.submitBatch(
                new SubmitClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(batch.getSn()),
                WindOperatorFactory.system());
        clearingSplitBatchApplicationService.confirmBatch(
                new ConfirmClearingSplitBatchRequest().setTenantId(TENANT_ID).setSplitBatchSn(batch.getSn()),
                WindOperatorFactory.system());
        return detail;
    }

    private String snapshotSn(String detailSn) {
        return clearingSplitBatchApplicationService.getResultSnapshots(
                        TENANT_ID, jdbcTemplate.queryForObject(
                                "SELECT split_batch_sn FROM t_clearing_split_batch_detail WHERE splittable_detail_sn = ?",
                                String.class, detailSn))
                .stream()
                .filter(snapshot -> detailSn.equals(snapshot.getSplittableDetailSn()))
                .findFirst()
                .orElseThrow()
                .getSn();
    }

    private CreateClearingSplitBatchRequest createRequest(String... splittableDetailSns) {
        return new CreateClearingSplitBatchRequest()
                .setTenantId(TENANT_ID)
                .setSplittableDetailSns(List.of(splittableDetailSns));
    }

    private CreateClearingCandidateRequest createCandidateRequest(String splitResultSn,
                                                                   LocalDateTime clearingAvailableTime) {
        return new CreateClearingCandidateRequest()
                .setTenantId(TENANT_ID)
                .setSplitResultSn(splitResultSn)
                .setClearingPeriod("2026-07-28")
                .setClearingRuleCode("MERCHANT_DAILY_CLEARING")
                .setClearingRuleVersion("1")
                .setClearingAvailableTime(clearingAvailableTime);
    }

    private String prepareSplittableDetail(String suffix,
                                           String subjectId,
                                           CurrencyIsoCode currency,
                                           String businessLine,
                                           long amount) {
        String transactionSn = transactionSn(suffix);
        String transactionDetailSn = "split_tx_detail_" + suffix;
        String reconciliationBatchSn = "split_recon_batch_" + suffix;
        String splittableSn = "CSD" + suffix;
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
                businessLine, "split_business_" + suffix, FundsTransactionStatus.CLOSED.name(), amount,
                currency.name(), amount, routeSnapshot);
        String referenceSourceRef = "internal:" + transactionDetailSn;
        String comparisonSourceRef = "external:" + transactionDetailSn;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, reconciliationBatchSn,
                ReconciliationGateObjectType.CLEARING, transactionDetailSn, "recon-rule-1",
                "report:split-" + suffix, referenceSourceRef, comparisonSourceRef);
        String runResultSn = reconciliationRunResultApplicationService.recordRunResult(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(reconciliationBatchSn)
                        .setMatchResults(List.of(new ReconciliationMatchResultItem()
                                .setReferenceSourceRef(referenceSourceRef)
                                .setComparisonSourceRef(comparisonSourceRef)
                                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                                .setEvidenceRef("report:split-" + suffix + "#line-1"))),
                WindOperatorFactory.system()).getSn();
        String resultDigest = jdbcTemplate.queryForObject(
                "SELECT result_digest FROM t_reconciliation_run_result WHERE sn = ?", String.class, runResultSn);
        jdbcTemplate.update("""
                        INSERT INTO t_clearing_splittable_detail (
                            sn, tenant_id, funds_transaction_sn,
                            funds_transaction_detail_sn, ledger_transaction_sn, posting_plan_sn, ledger_entry_sn,
                            subject_type, subject_id, currency, amount, refund_amount, business_line,
                            split_period, split_rule_code, split_rule_version,
                            status, reconciliation_decision_status,
                            reconciliation_run_result_sn, reconciliation_result_digest,
                            reconciliation_evidence_refs, route_snapshot_digest, source_digest, created_by
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'FUNDING_ACCOUNT', ?, ?, ?, 0, ?, ?, ?, ?,
                                  'SPLIT_READY', 'PASSED', ?, ?, ?, ?, ?, 'system')
                        """,
                splittableSn, TENANT_ID, transactionSn, transactionDetailSn,
                "split_ledger_tx_" + suffix, "split_posting_plan_" + suffix,
                "split_ledger_entry_" + suffix, subjectId, currency.name(), amount, businessLine,
                SPLIT_PERIOD, SPLIT_RULE_CODE, SPLIT_RULE_VERSION, runResultSn, resultDigest,
                "[\"report:split-" + suffix + "\"]",
                FundsStableHashSupport.sha256Json(Map.of("routeSnapshot", routeSnapshot)),
                ("b" + suffix).repeat(64).substring(0, 64));
        return splittableSn;
    }

    private String transactionSn(String suffix) {
        return "split_funds_tx_" + suffix;
    }

    private int batchCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_split_batch", Integer.class);
    }

    private int memberCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_split_batch_detail", Integer.class);
    }

    private int snapshotCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_split_result_snapshot", Integer.class);
    }

    private int candidateCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_candidate", Integer.class);
    }

    @Configuration
    @Import({
            DefaultFundsTransactionQueryService.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class,
            ClearingSettlementGateConsumerServiceImpl.class,
            ClearingSplitBatchApplicationServiceImpl.class,
            ClearingCandidateApplicationServiceImpl.class
    })
    static class Config {
    }
}

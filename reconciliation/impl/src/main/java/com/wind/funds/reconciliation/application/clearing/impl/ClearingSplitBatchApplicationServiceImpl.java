package com.wind.funds.reconciliation.application.clearing.impl;

import com.wind.jackson.WindJson;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.application.clearing.ClearingSplitBatchApplicationService;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitBatch;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitBatchDetail;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitResultSnapshot;
import com.wind.funds.reconciliation.dal.entities.ClearingSplittableDetail;
import com.wind.funds.reconciliation.dal.mapper.ClearingSplitBatchDetailMapper;
import com.wind.funds.reconciliation.dal.mapper.ClearingSplitBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ClearingSplitResultSnapshotMapper;
import com.wind.funds.reconciliation.dal.mapper.ClearingSplittableDetailMapper;
import com.wind.funds.reconciliation.dal.entities.table.ClearingSplitBatchNameRefs;
import com.wind.funds.reconciliation.enums.ClearingSplitBatchStatus;
import com.wind.funds.reconciliation.enums.ClearingSplittableDetailStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.model.dto.ClearingSplitBatchDTO;
import com.wind.funds.reconciliation.model.dto.ClearingSplitResultSnapshotDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.query.ClearingSplitBatchQuery;
import com.wind.funds.reconciliation.model.request.CancelClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingSplitBatchRequest;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 清分批次应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ClearingSplitBatchApplicationServiceImpl implements ClearingSplitBatchApplicationService {

    private static final WindSequenceType BATCH_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_SPLIT_BATCH", "CSB", 6);

    private static final WindSequenceType BATCH_DETAIL_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_SPLIT_BATCH_DETAIL", "CBD", 6);

    private static final WindSequenceType RESULT_SNAPSHOT_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_SPLIT_RESULT_SNAPSHOT", "CRS", 6);

    private final ClearingSplitBatchMapper clearingSplitBatchMapper;

    private final ClearingSplitBatchDetailMapper clearingSplitBatchDetailMapper;

    private final ClearingSplitResultSnapshotMapper clearingSplitResultSnapshotMapper;

    private final ClearingSplittableDetailMapper clearingSplittableDetailMapper;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingSplitBatchDTO createBatch(CreateClearingSplitBatchRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        List<String> detailSns = request.getSplittableDetailSns().stream().sorted().toList();
        AssertUtils.isTrue(detailSns.stream().distinct().count() == detailSns.size(),
                "可清分明细流水号不能重复");
        List<ClearingSplittableDetail> details = clearingSplittableDetailMapper.selectBySns(
                request.getTenantId(), detailSns);
        AssertUtils.isTrue(details.size() == detailSns.size(), "存在未找到的可清分明细");
        validateBatchBoundary(details);

        ClearingSplitBatch candidate = toBatch(request.getTenantId(), details, operator);
        ClearingSplitBatch existing = clearingSplitBatchMapper.selectByDigest(
                request.getTenantId(), candidate.getBatchDigest());
        if (existing != null) {
            return toDTO(existing);
        }
        AssertUtils.isTrue(clearingSplitBatchDetailMapper.countActiveMemberships(
                request.getTenantId(), detailSns) == 0, "可清分明细已进入其他有效清分批次");
        try {
            clearingSplitBatchMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ClearingSplitBatch winner = clearingSplitBatchMapper.selectByDigest(
                    request.getTenantId(), candidate.getBatchDigest());
            AssertUtils.notNull(winner, "清分批次唯一键冲突后未找到幂等结果");
            return toDTO(winner);
        }
        AssertUtils.notNull(candidate.getId(), "创建清分批次失败");
        for (ClearingSplittableDetail detail : details) {
            clearingSplitBatchDetailMapper.insertSelective(toBatchDetail(candidate, detail, operator));
        }
        ClearingSplitBatch saved = clearingSplitBatchMapper.selectBySn(request.getTenantId(), candidate.getSn());
        log.info("清分批次创建完成，tenantId = {}, splitBatchSn = {}, detailCount = {}",
                request.getTenantId(), candidate.getSn(), details.size());
        return toDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingSplitBatchDTO submitBatch(SubmitClearingSplitBatchRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSplitBatchSn(), operator);
        ClearingSplitBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getSplitBatchSn());
        if (batch.getStatus() == ClearingSplitBatchStatus.REVIEWING) {
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getStatus() == ClearingSplitBatchStatus.DRAFT,
                "只有 DRAFT 清分批次可以提交复核，status = {}", batch.getStatus());
        batch.setStatus(ClearingSplitBatchStatus.REVIEWING);
        batch.setSubmittedBy(operator.getOperatorAsText());
        batch.setSubmittedTime(LocalDateTime.now());
        AssertUtils.isTrue(clearingSplitBatchMapper.update(batch) == 1, "提交清分批次复核失败");
        return toDTO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingSplitBatchDTO confirmBatch(ConfirmClearingSplitBatchRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSplitBatchSn(), operator);
        ClearingSplitBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getSplitBatchSn());
        if (batch.getStatus() == ClearingSplitBatchStatus.CONFIRMED) {
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getStatus() == ClearingSplitBatchStatus.REVIEWING,
                "只有 REVIEWING 清分批次可以确认，status = {}", batch.getStatus());
        List<ClearingSplittableDetail> details = batchDetails(batch);
        for (ClearingSplittableDetail detail : details) {
            validateCurrentSource(batch, detail, operator);
        }
        for (ClearingSplittableDetail detail : details) {
            clearingSplitResultSnapshotMapper.insertSelective(toSnapshot(batch, detail, operator));
        }
        batch.setStatus(ClearingSplitBatchStatus.CONFIRMED);
        batch.setConfirmedBy(operator.getOperatorAsText());
        batch.setConfirmedTime(LocalDateTime.now());
        AssertUtils.isTrue(clearingSplitBatchMapper.update(batch) == 1, "确认清分批次失败");
        log.info("清分批次确认完成，tenantId = {}, splitBatchSn = {}, snapshotCount = {}",
                batch.getTenantId(), batch.getSn(), details.size());
        return toDTO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingSplitBatchDTO cancelBatch(CancelClearingSplitBatchRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSplitBatchSn(), operator);
        AssertUtils.hasText(request.getReason(), "清分批次取消原因不能为空");
        ClearingSplitBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getSplitBatchSn());
        if (batch.getStatus() == ClearingSplitBatchStatus.CANCELLED) {
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getStatus() == ClearingSplitBatchStatus.DRAFT
                        || batch.getStatus() == ClearingSplitBatchStatus.REVIEWING,
                "只有 DRAFT 或 REVIEWING 清分批次可以取消，status = {}", batch.getStatus());
        AssertUtils.isTrue(clearingSplitBatchDetailMapper.releaseActiveMembership(batch.getTenantId(), batch.getSn())
                        == batch.getDetailCount(),
                "释放清分批次成员占用失败");
        AssertUtils.isTrue(clearingSplitBatchMapper.releaseActiveDigest(batch.getTenantId(), batch.getSn()) == 1,
                "释放清分批次幂等占用失败");
        batch.setActiveBatchDigest(null);
        batch.setStatus(ClearingSplitBatchStatus.CANCELLED);
        batch.setCancelledBy(operator.getOperatorAsText());
        batch.setCancelledTime(LocalDateTime.now());
        batch.setCancelReason(request.getReason());
        AssertUtils.isTrue(clearingSplitBatchMapper.update(batch) == 1, "取消清分批次失败");
        return toDTO(batch);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ClearingSplitBatchDTO getBatch(Long tenantId, String splitBatchSn) {
        validateQuery(tenantId, splitBatchSn);
        return toDTO(requiredBatch(tenantId, splitBatchSn));
    }

    @Override
    @Transactional(readOnly = true)
    public WindPagination<ClearingSplitBatchDTO> queryBatches(
            ClearingSplitBatchQuery query,
            WindQuery<? extends QueryOrderField> options) {
        AssertUtils.notNull(query, "清分批次查询条件不能为空");
        AssertUtils.notNull(options, "清分批次查询选项不能为空");
        Long currentTenantId = TenantContextHolder.requireTenantId();
        Long tenantId = query.getTenantId() == null ? currentTenantId : query.getTenantId();
        AssertUtils.equals(currentTenantId, tenantId, "清分批次查询 tenantId 与当前租户不一致");
        ClearingSplitBatchNameRefs batch = ClearingSplitBatchNameRefs.clearingSplitBatch;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(batch)
                .where(batch.tenantId.eq(tenantId))
                .and(batch.status.eq(query.getStatus()))
                .and(batch.gmtModified.le(query.getGmtModifiedMax()))
                .orderBy(batch.gmtModified.asc(), batch.id.asc());
        return MybatisQueryHelper.<ClearingSplitBatch, ClearingSplitBatchDTO>query(wrapper)
                .counter(clearingSplitBatchMapper::selectCountByQuery)
                .resultQueryFunc(clearingSplitBatchMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ClearingSplitResultSnapshotDTO> getResultSnapshots(Long tenantId, String splitBatchSn) {
        validateQuery(tenantId, splitBatchSn);
        requiredBatch(tenantId, splitBatchSn);
        return clearingSplitResultSnapshotMapper.selectByBatchSn(tenantId, splitBatchSn)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ClearingSplitBatch toBatch(Long tenantId,
                                       List<ClearingSplittableDetail> details,
                                       WindOperator operator) {
        ClearingSplittableDetail first = details.getFirst();
        String memberDigest = memberDigest(details);
        long totalAmount = totalAmount(details);
        ClearingSplitBatch result = new ClearingSplitBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(tenantId);
        result.setSubjectType(first.getSubjectType());
        result.setSubjectId(first.getSubjectId());
        result.setCurrency(first.getCurrency());
        result.setBusinessLine(first.getBusinessLine());
        result.setSplitPeriod(first.getSplitPeriod());
        result.setSplitRuleCode(first.getSplitRuleCode());
        result.setSplitRuleVersion(first.getSplitRuleVersion());
        result.setDetailCount(details.size());
        result.setTotalAmount(totalAmount);
        result.setMemberDigest(memberDigest);
        result.setBatchDigest(batchDigest(tenantId, details, totalAmount, memberDigest));
        result.setActiveBatchDigest(result.getBatchDigest());
        result.setStatus(ClearingSplitBatchStatus.DRAFT);
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private ClearingSplitBatchDetail toBatchDetail(ClearingSplitBatch batch,
                                                   ClearingSplittableDetail detail,
                                                   WindOperator operator) {
        ClearingSplitBatchDetail result = new ClearingSplitBatchDetail();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_DETAIL_SEQUENCE_TYPE));
        result.setTenantId(batch.getTenantId());
        result.setSplitBatchSn(batch.getSn());
        result.setSplittableDetailSn(detail.getSn());
        result.setActiveSplittableDetailSn(detail.getSn());
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private ClearingSplitResultSnapshot toSnapshot(ClearingSplitBatch batch,
                                                    ClearingSplittableDetail detail,
                                                    WindOperator operator) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("splitBatchSn", batch.getSn());
        facts.put("splittableDetailSn", detail.getSn());
        facts.put("subjectType", detail.getSubjectType());
        facts.put("subjectId", detail.getSubjectId());
        facts.put("currency", detail.getCurrency());
        facts.put("businessLine", detail.getBusinessLine());
        facts.put("splitPeriod", detail.getSplitPeriod());
        facts.put("amount", detail.getAmount());
        facts.put("fundsTransactionSn", detail.getFundsTransactionSn());
        facts.put("fundsTransactionDetailSn", detail.getFundsTransactionDetailSn());
        facts.put("ledgerTransactionSn", detail.getLedgerTransactionSn());
        facts.put("postingPlanSn", detail.getPostingPlanSn());
        facts.put("ledgerEntrySn", detail.getLedgerEntrySn());
        facts.put("routeSnapshotDigest", detail.getRouteSnapshotDigest());
        facts.put("splitRuleCode", detail.getSplitRuleCode());
        facts.put("splitRuleVersion", detail.getSplitRuleVersion());
        facts.put("reconciliationRunResultSn", detail.getReconciliationRunResultSn());
        facts.put("reconciliationResultDigest", detail.getReconciliationResultDigest());
        facts.put("reconciliationEvidenceRefs", parseEvidenceRefs(detail.getReconciliationEvidenceRefs()));
        facts.put("sourceDigest", detail.getSourceDigest());
        ClearingSplitResultSnapshot result = new ClearingSplitResultSnapshot();
        result.setSn(TemporalSequenceFactory.hourNext(RESULT_SNAPSHOT_SEQUENCE_TYPE));
        result.setTenantId(batch.getTenantId());
        result.setSplitBatchSn(batch.getSn());
        result.setSplittableDetailSn(detail.getSn());
        result.setSubjectType(detail.getSubjectType());
        result.setSubjectId(detail.getSubjectId());
        result.setCurrency(detail.getCurrency());
        result.setBusinessLine(detail.getBusinessLine());
        result.setSplitPeriod(detail.getSplitPeriod());
        result.setAmount(detail.getAmount());
        result.setFundsTransactionSn(detail.getFundsTransactionSn());
        result.setFundsTransactionDetailSn(detail.getFundsTransactionDetailSn());
        result.setLedgerTransactionSn(detail.getLedgerTransactionSn());
        result.setPostingPlanSn(detail.getPostingPlanSn());
        result.setLedgerEntrySn(detail.getLedgerEntrySn());
        result.setRouteSnapshotDigest(detail.getRouteSnapshotDigest());
        result.setSplitRuleCode(detail.getSplitRuleCode());
        result.setSplitRuleVersion(detail.getSplitRuleVersion());
        result.setReconciliationRunResultSn(detail.getReconciliationRunResultSn());
        result.setReconciliationResultDigest(detail.getReconciliationResultDigest());
        result.setReconciliationEvidenceRefs(detail.getReconciliationEvidenceRefs());
        result.setSourceDigest(detail.getSourceDigest());
        result.setSnapshotDigest(FundsStableHashSupport.sha256Json(facts));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private List<ClearingSplittableDetail> batchDetails(ClearingSplitBatch batch) {
        List<ClearingSplitBatchDetail> members = clearingSplitBatchDetailMapper.selectByBatchSn(
                batch.getTenantId(), batch.getSn());
        AssertUtils.isTrue(members.size() == batch.getDetailCount(), "清分批次成员数量与批次摘要不一致");
        List<String> detailSns = members.stream().map(ClearingSplitBatchDetail::getSplittableDetailSn).toList();
        List<ClearingSplittableDetail> details = clearingSplittableDetailMapper.selectBySns(
                batch.getTenantId(), detailSns);
        AssertUtils.isTrue(details.size() == batch.getDetailCount(), "清分批次来源明细不完整");
        validateBatchBoundary(details);
        long totalAmount = totalAmount(details);
        String memberDigest = memberDigest(details);
        AssertUtils.isTrue(totalAmount == batch.getTotalAmount()
                        && memberDigest.equals(batch.getMemberDigest())
                        && batchDigest(batch.getTenantId(), details, totalAmount, memberDigest)
                        .equals(batch.getBatchDigest()),
                "清分批次摘要与当前成员事实不一致");
        return details;
    }

    private String memberDigest(List<ClearingSplittableDetail> details) {
        return FundsStableHashSupport.sha256Json(details.stream()
                .map(ClearingSplittableDetail::getSourceDigest)
                .sorted()
                .toList());
    }

    private long totalAmount(List<ClearingSplittableDetail> details) {
        return details.stream()
                .map(ClearingSplittableDetail::getAmount)
                .reduce(0L, Math::addExact);
    }

    private String batchDigest(Long tenantId,
                               List<ClearingSplittableDetail> details,
                               long totalAmount,
                               String memberDigest) {
        ClearingSplittableDetail first = details.getFirst();
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", tenantId);
        facts.put("subjectType", first.getSubjectType());
        facts.put("subjectId", first.getSubjectId());
        facts.put("currency", first.getCurrency());
        facts.put("businessLine", first.getBusinessLine());
        facts.put("splitPeriod", first.getSplitPeriod());
        facts.put("splitRuleCode", first.getSplitRuleCode());
        facts.put("splitRuleVersion", first.getSplitRuleVersion());
        facts.put("detailCount", details.size());
        facts.put("totalAmount", totalAmount);
        facts.put("memberDigest", memberDigest);
        return FundsStableHashSupport.sha256Json(facts);
    }

    private void validateCurrentSource(ClearingSplitBatch batch,
                                       ClearingSplittableDetail detail,
                                       WindOperator operator) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(
                detail.getFundsTransactionSn()).orElse(null);
        AssertUtils.notNull(transaction, "清分来源交易不存在，fundsTransactionSn = {}", detail.getFundsTransactionSn());
        AssertUtils.isTrue(Objects.equals(transaction.getTenantId(), batch.getTenantId())
                        && defaultAmount(transaction.getRefundedAmount()) == detail.getRefundAmount()
                        && defaultAmount(transaction.getRefundedAmount()) == 0
                        && (transaction.getStatus() == FundsTransactionStatus.OPEN
                        || transaction.getStatus() == FundsTransactionStatus.CLOSED)
                        && transaction.getCurrency() == detail.getCurrency(),
                "清分来源交易已变化，fundsTransactionSn = {}", detail.getFundsTransactionSn());
        AssertUtils.hasText(transaction.getRouteSnapshot(),
                "清分来源交易 RouteSnapshot 不存在，fundsTransactionSn = {}", detail.getFundsTransactionSn());
        String currentRouteDigest = FundsStableHashSupport.sha256Json(
                Map.of("routeSnapshot", transaction.getRouteSnapshot()));
        AssertUtils.isTrue(currentRouteDigest.equals(detail.getRouteSnapshotDigest()),
                "清分来源交易 RouteSnapshot 已变化，fundsTransactionSn = {}", detail.getFundsTransactionSn());
        ReconciliationGateDecisionDTO decision = reconciliationGateApplicationService.checkGate(
                new CheckReconciliationGateRequest()
                        .setTenantId(batch.getTenantId())
                        .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                        .setGateObjectSn(detail.getFundsTransactionDetailSn())
                        .setReconciliationRunResultSn(detail.getReconciliationRunResultSn()),
                operator);
        AssertUtils.isTrue(decision.isPassed()
                        && Objects.equals(decision.getReconciliationResultDigest(),
                        detail.getReconciliationResultDigest()),
                "清分确认时对账 Gate 未通过或证据已变化，splittableDetailSn = {}", detail.getSn());
    }

    private void validateBatchBoundary(List<ClearingSplittableDetail> details) {
        ClearingSplittableDetail first = details.getFirst();
        for (ClearingSplittableDetail detail : details) {
            AssertUtils.isTrue(detail.getStatus() == ClearingSplittableDetailStatus.SPLIT_READY,
                    "只有 SPLIT_READY 明细可以进入清分批次，splittableDetailSn = {}", detail.getSn());
            AssertUtils.isTrue(Objects.equals(first.getSubjectType(), detail.getSubjectType())
                            && Objects.equals(first.getSubjectId(), detail.getSubjectId()),
                    "一个清分批次必须属于同一账务主体");
            AssertUtils.isTrue(first.getCurrency() == detail.getCurrency(),
                    "一个清分批次必须使用同一币种");
            AssertUtils.isTrue(Objects.equals(first.getBusinessLine(), detail.getBusinessLine()),
                    "一个清分批次必须属于同一业务线");
            AssertUtils.isTrue(Objects.equals(first.getSplitPeriod(), detail.getSplitPeriod()),
                    "一个清分批次必须属于同一清分周期");
            AssertUtils.isTrue(Objects.equals(first.getSplitRuleCode(), detail.getSplitRuleCode())
                            && Objects.equals(first.getSplitRuleVersion(), detail.getSplitRuleVersion()),
                    "一个清分批次必须使用同一清分规则版本");
        }
    }

    private ClearingSplitBatch requiredBatch(Long tenantId, String splitBatchSn) {
        ClearingSplitBatch result = clearingSplitBatchMapper.selectBySn(tenantId, splitBatchSn);
        AssertUtils.notNull(result, "清分批次不存在，splitBatchSn = {}", splitBatchSn);
        return result;
    }

    private ClearingSplitBatch requiredBatchForUpdate(Long tenantId, String splitBatchSn) {
        ClearingSplitBatch result = clearingSplitBatchMapper.selectBySnForUpdate(tenantId, splitBatchSn);
        AssertUtils.notNull(result, "清分批次不存在，splitBatchSn = {}", splitBatchSn);
        return result;
    }

    private void validateCreateRequest(CreateClearingSplitBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "清分批次创建请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "清分批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "清分批次 tenantId 与当前租户不一致");
        AssertUtils.notEmpty(request.getSplittableDetailSns(), "可清分明细流水号不能为空");
        AssertUtils.isTrue(request.getSplittableDetailSns().size()
                        <= CreateClearingSplitBatchRequest.MAX_SPLITTABLE_DETAIL_COUNT,
                "单个清分批次明细数量不能超过 {}",
                CreateClearingSplitBatchRequest.MAX_SPLITTABLE_DETAIL_COUNT);
        AssertUtils.isTrue(request.getSplittableDetailSns().stream().allMatch(StringUtils::hasText),
                "可清分明细流水号不能为空");
        AssertUtils.notNull(operator, "清分批次创建操作人不能为空");
    }

    private void validateCommandRequest(Long tenantId, String splitBatchSn, WindOperator operator) {
        validateQuery(tenantId, splitBatchSn);
        AssertUtils.notNull(operator, "清分批次操作人不能为空");
    }

    private void validateQuery(Long tenantId, String splitBatchSn) {
        AssertUtils.notNull(tenantId, "清分批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "清分批次 tenantId 与当前租户不一致");
        AssertUtils.hasText(splitBatchSn, "清分批次流水号不能为空");
    }

    private ClearingSplitBatchDTO toDTO(ClearingSplitBatch source) {
        return new ClearingSplitBatchDTO()
                .setId(source.getId())
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSubjectType(source.getSubjectType())
                .setSubjectId(source.getSubjectId())
                .setCurrency(source.getCurrency())
                .setBusinessLine(source.getBusinessLine())
                .setSplitPeriod(source.getSplitPeriod())
                .setSplitRuleCode(source.getSplitRuleCode())
                .setSplitRuleVersion(source.getSplitRuleVersion())
                .setDetailCount(source.getDetailCount())
                .setTotalAmount(source.getTotalAmount())
                .setMemberDigest(source.getMemberDigest())
                .setBatchDigest(source.getBatchDigest())
                .setStatus(source.getStatus())
                .setCreatedTime(source.getGmtCreate())
                .setSubmittedTime(source.getSubmittedTime())
                .setConfirmedTime(source.getConfirmedTime())
                .setCancelledTime(source.getCancelledTime())
                .setCancelReason(source.getCancelReason());
    }

    private ClearingSplitResultSnapshotDTO toDTO(ClearingSplitResultSnapshot source) {
        return new ClearingSplitResultSnapshotDTO()
                .setId(source.getId())
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSplitBatchSn(source.getSplitBatchSn())
                .setSplittableDetailSn(source.getSplittableDetailSn())
                .setSubjectType(source.getSubjectType())
                .setSubjectId(source.getSubjectId())
                .setCurrency(source.getCurrency())
                .setBusinessLine(source.getBusinessLine())
                .setSplitPeriod(source.getSplitPeriod())
                .setAmount(source.getAmount())
                .setFundsTransactionSn(source.getFundsTransactionSn())
                .setFundsTransactionDetailSn(source.getFundsTransactionDetailSn())
                .setLedgerTransactionSn(source.getLedgerTransactionSn())
                .setPostingPlanSn(source.getPostingPlanSn())
                .setLedgerEntrySn(source.getLedgerEntrySn())
                .setRouteSnapshotDigest(source.getRouteSnapshotDigest())
                .setSplitRuleCode(source.getSplitRuleCode())
                .setSplitRuleVersion(source.getSplitRuleVersion())
                .setReconciliationRunResultSn(source.getReconciliationRunResultSn())
                .setReconciliationResultDigest(source.getReconciliationResultDigest())
                .setReconciliationEvidenceRefs(parseEvidenceRefs(source.getReconciliationEvidenceRefs()))
                .setSourceDigest(source.getSourceDigest())
                .setSnapshotDigest(source.getSnapshotDigest())
                .setCreatedTime(source.getGmtCreate());
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? WindJson.parseArray(value, String.class) : List.of();
    }

    private long defaultAmount(Long value) {
        return value == null ? 0L : value;
    }
}

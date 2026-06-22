package com.wind.funds.wallet.application.spend.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.application.spend.SpendRuleDefinitionApplicationService;
import com.wind.funds.wallet.dal.entities.SpendRuleAssignment;
import com.wind.funds.wallet.dal.entities.SpendRuleDecisionLog;
import com.wind.funds.wallet.dal.entities.SpendRuleDefinition;
import com.wind.funds.wallet.dal.entities.SpendRuleVersion;
import com.wind.funds.wallet.dal.entities.table.SpendRuleAssignmentNameRefs;
import com.wind.funds.wallet.dal.entities.table.SpendRuleDecisionLogNameRefs;
import com.wind.funds.wallet.dal.entities.table.SpendRuleDefinitionNameRefs;
import com.wind.funds.wallet.dal.entities.table.SpendRuleVersionNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleAssignmentMapper;
import com.wind.funds.wallet.dal.mapper.SpendRuleDecisionLogMapper;
import com.wind.funds.wallet.dal.mapper.SpendRuleDefinitionMapper;
import com.wind.funds.wallet.dal.mapper.SpendRuleVersionMapper;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleAssignmentExplanationStatus;
import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.enums.SpendRuleDefinitionStatus;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleVersionStatus;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Spend Rule 定义应用服务实现。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Service
@AllArgsConstructor
public class SpendRuleDefinitionApplicationServiceImpl implements SpendRuleDefinitionApplicationService {

    private final SpendRuleDefinitionMapper spendRuleDefinitionMapper;

    private final SpendRuleVersionMapper spendRuleVersionMapper;

    private final SpendRuleAssignmentMapper spendRuleAssignmentMapper;

    private final SpendRuleDecisionLogMapper spendRuleDecisionLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleDefinitionDTO createDefinition(
            @NonNull CreateSpendRuleDefinitionRequest request) {
        validateCreateDefinitionRequest(request);
        SpendRuleDefinition existing = findDefinition(request.getTenantId(), request.getRuleId());
        if (existing != null) {
            assertSameDefinition(request, existing);
            return toDefinitionDTO(existing);
        }
        SpendRuleDefinition entity = toDefinitionEntity(request);
        try {
            spendRuleDefinitionMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentDefinitionAfterInsertConflict(request, exception);
        }
        AssertUtils.notNull(entity.getId(), "创建 Spend Rule 定义失败，ruleId = {}", request.getRuleId());
        return toDefinitionDTO(spendRuleDefinitionMapper.selectOneById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleVersionDTO publishVersion(
            @NonNull PublishSpendRuleVersionRequest request) {
        validatePublishVersionRequest(request);
        SpendRuleDefinition definition = findDefinition(request.getTenantId(), request.getRuleId());
        AssertUtils.notNull(definition, "Spend Rule 定义不存在，ruleId = {}", request.getRuleId());
        AssertUtils.isTrue(definition.getStatus() == SpendRuleDefinitionStatus.ACTIVE,
                "Spend Rule 定义不可用，ruleId = {}",
                request.getRuleId());
        SpendRuleVersion existing = findVersion(request.getTenantId(), request.getRuleId(), request.getRuleVersion());
        if (existing != null) {
            assertSamePublishedVersion(request, existing);
            return toVersionDTO(existing);
        }
        SpendRuleVersion entity = toVersionEntity(request);
        try {
            spendRuleVersionMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentVersionAfterInsertConflict(request, exception);
        }
        AssertUtils.notNull(entity.getId(), "发布 Spend Rule 版本失败，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
        return toVersionDTO(spendRuleVersionMapper.selectOneById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleAssignmentDTO assignVersion(
            @NonNull AssignSpendRuleVersionRequest request) {
        validateAssignmentRequest(request);
        assertPublishedVersionExists(request.getTenantId(), request.getRuleId(), request.getRuleVersion());
        SpendRuleAssignment existing = findAssignment(request.getTenantId(), request.getAssignmentSn());
        if (existing != null) {
            assertSameAssignment(request, existing);
            return toAssignmentDTO(existing);
        }
        SpendRuleAssignment entity = toAssignmentEntity(request);
        try {
            spendRuleAssignmentMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentAssignmentAfterInsertConflict(request, exception);
        }
        AssertUtils.notNull(entity.getId(), "挂载 Spend Rule 版本失败，assignmentSn = {}",
                request.getAssignmentSn());
        return toAssignmentDTO(spendRuleAssignmentMapper.selectOneById(entity.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendRuleAssignmentDTO> queryAssignments(
            @NonNull SpendRuleAssignmentQuery query) {
        validateAssignmentQuery(query);
        return spendRuleAssignmentMapper.selectListByQuery(toAssignmentQueryWrapper(query)).stream()
                .map(this::toAssignmentDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleAssignmentExplanationDTO explainAssignment(
            @NonNull SpendRuleAssignmentExplainQuery query) {
        validateAssignmentExplainQuery(query);
        SpendRuleAssignment assignment = findAssignment(query.getTenantId(), query.getAssignmentSn());
        AssertUtils.notNull(assignment, "Spend Rule 挂载不存在，assignmentSn = {}", query.getAssignmentSn());
        LocalDateTime evaluatedAt = resolveEvaluationTime(query.getExplainAt());
        SpendRuleAssignmentExplanationStatus status = resolveExplanationStatus(assignment, evaluatedAt);
        return new SpendRuleAssignmentExplanationDTO()
                .setAssignment(toAssignmentDTO(assignment))
                .setEvaluatedAt(evaluatedAt)
                .setEffective(status == SpendRuleAssignmentExplanationStatus.EFFECTIVE)
                .setExplanationStatus(status)
                .setExplanationMessage(status.getDesc())
                .setEvidenceRefs(toAssignmentEvidenceRefs(assignment));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleDecisionLogDTO recordDecision(
            @NonNull RecordSpendRuleDecisionLogRequest request) {
        validateDecisionLogRequest(request);
        assertPublishedVersionExists(request.getTenantId(), request.getRuleId(), request.getRuleVersion());
        assertAssignmentMatchesIfPresent(request);
        SpendRuleDecisionLog existing = findDecisionLog(request.getTenantId(), request.getDecisionSn());
        if (existing != null) {
            assertSameDecisionLog(request, existing);
            return toDecisionLogDTO(existing);
        }
        SpendRuleDecisionLog entity = toDecisionLogEntity(request);
        try {
            spendRuleDecisionLogMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentDecisionLogAfterInsertConflict(request, exception);
        }
        AssertUtils.notNull(entity.getId(), "记录 Spend Rule 决策日志失败，decisionSn = {}",
                request.getDecisionSn());
        return toDecisionLogDTO(spendRuleDecisionLogMapper.selectOneById(entity.getId()));
    }

    private void validateCreateDefinitionRequest(CreateSpendRuleDefinitionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleName(), "Spend Rule 名称不能为空");
        AssertUtils.notNull(request.getRuleType(), "Spend Rule 类型不能为空");
        AssertUtils.notNull(request.getRuleDomain(), "Spend Rule 规则域不能为空");
    }

    private void validatePublishVersionRequest(PublishSpendRuleVersionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.hasText(request.getRuleSpec(), "Spend Rule 规则规格不能为空");
        AssertUtils.hasText(request.getRuleDigest(), "Spend Rule 规则摘要不能为空");
        AssertUtils.hasText(request.getOperatorId(), "Spend Rule 发布操作者不能为空");
        AssertUtils.hasText(request.getAuditReferenceSn(), "Spend Rule 发布审计引用不能为空");
    }

    private void validateAssignmentRequest(AssignSpendRuleVersionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getAssignmentSn(), "Spend Rule 挂载流水号不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getScopeType(), "Spend Rule 挂载范围类型不能为空");
        AssertUtils.hasText(request.getScopeId(), "Spend Rule 挂载范围标识不能为空");
        AssertUtils.notNull(request.getPriority(), "Spend Rule 挂载优先级不能为空");
        AssertUtils.notNull(request.getConflictPolicy(), "Spend Rule 挂载冲突策略不能为空");
        AssertUtils.notNull(request.getEffectiveFrom(), "Spend Rule 挂载生效开始时间不能为空");
        AssertUtils.notNull(request.getEffectiveTo(), "Spend Rule 挂载生效结束时间不能为空");
        AssertUtils.isTrue(request.getEffectiveTo().isAfter(request.getEffectiveFrom()),
                "Spend Rule 挂载生效结束时间必须晚于开始时间");
    }

    private void validateDecisionLogRequest(RecordSpendRuleDecisionLogRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getDecisionSn(), "Spend Rule 决策流水号不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getScopeType(), "Spend Rule 决策范围类型不能为空");
        AssertUtils.hasText(request.getScopeId(), "Spend Rule 决策范围标识不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getAmount(), "交易金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "交易金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.notNull(request.getDecisionResult(), "Spend Rule 决策结果不能为空");
        AssertUtils.hasText(request.getDecisionDigest(), "Spend Rule 决策摘要不能为空");
        if (request.getDecisionResult() == SpendControlDecisionResult.REJECTED) {
            AssertUtils.hasText(request.getRejectReason(), "Spend Rule 拒绝原因不能为空");
        } else {
            AssertUtils.isTrue(request.getRejectReason() == null, "非拒绝 Spend Rule 决策不能携带拒绝原因");
        }
        if (request.getScopeType() == SpendRuleScopeType.PAYMENT_INSTRUMENT) {
            AssertUtils.hasText(request.getInstrumentSn(), "支付工具范围的 Spend Rule 决策必须携带支付工具号");
            AssertUtils.isTrue(Objects.equals(request.getScopeId(), request.getInstrumentSn()),
                    "Spend Rule 决策支付工具号与控制范围不一致，decisionSn = {}",
                    request.getDecisionSn());
        }
    }

    private void validateAssignmentQuery(SpendRuleAssignmentQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
    }

    private void validateAssignmentExplainQuery(SpendRuleAssignmentExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getAssignmentSn(), "Spend Rule 挂载流水号不能为空");
    }

    private void assertPublishedVersionExists(Long tenantId, String ruleId, String ruleVersion) {
        SpendRuleVersion version = findVersion(tenantId, ruleId, ruleVersion);
        AssertUtils.notNull(version, "Spend Rule 版本不存在，ruleId = {}, ruleVersion = {}", ruleId, ruleVersion);
        AssertUtils.isTrue(version.getStatus() == SpendRuleVersionStatus.PUBLISHED,
                "Spend Rule 版本未发布，ruleId = {}, ruleVersion = {}",
                ruleId,
                ruleVersion);
    }

    private void assertAssignmentMatchesIfPresent(RecordSpendRuleDecisionLogRequest request) {
        if (request.getAssignmentSn() == null) {
            return;
        }
        SpendRuleAssignment assignment = findAssignment(request.getTenantId(), request.getAssignmentSn());
        AssertUtils.notNull(assignment, "Spend Rule 挂载不存在，assignmentSn = {}", request.getAssignmentSn());
        AssertUtils.isTrue(assignment.getStatus() == SpendRuleAssignmentStatus.ACTIVE,
                "Spend Rule 挂载不可用，assignmentSn = {}",
                request.getAssignmentSn());
        assertAssignmentEffectiveNow(request, assignment);
        AssertUtils.isTrue(Objects.equals(assignment.getRuleId(), request.getRuleId())
                        && Objects.equals(assignment.getRuleVersion(), request.getRuleVersion())
                        && assignment.getScopeType() == request.getScopeType()
                        && Objects.equals(assignment.getScopeId(), request.getScopeId()),
                "Spend Rule 决策日志与挂载不一致，decisionSn = {}",
                request.getDecisionSn());
    }

    private void assertAssignmentEffectiveNow(RecordSpendRuleDecisionLogRequest request,
                                              SpendRuleAssignment assignment) {
        LocalDateTime now = LocalDateTime.now();
        AssertUtils.isTrue(!now.isBefore(assignment.getEffectiveFrom()) && now.isBefore(assignment.getEffectiveTo()),
                "Spend Rule 挂载未在当前时间生效，assignmentSn = {}",
                request.getAssignmentSn());
    }

    private SpendRuleDefinition findDefinition(Long tenantId, String ruleId) {
        SpendRuleDefinitionNameRefs ref = SpendRuleDefinitionNameRefs.spendRuleDefinition;
        return spendRuleDefinitionMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.ruleId.eq(ruleId)));
    }

    private SpendRuleVersion findVersion(Long tenantId, String ruleId, String ruleVersion) {
        SpendRuleVersionNameRefs ref = SpendRuleVersionNameRefs.spendRuleVersion;
        return spendRuleVersionMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.ruleId.eq(ruleId))
                .and(ref.ruleVersion.eq(ruleVersion)));
    }

    private SpendRuleAssignment findAssignment(Long tenantId, String assignmentSn) {
        SpendRuleAssignmentNameRefs ref = SpendRuleAssignmentNameRefs.spendRuleAssignment;
        return spendRuleAssignmentMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.assignmentSn.eq(assignmentSn)));
    }

    private QueryWrapper toAssignmentQueryWrapper(SpendRuleAssignmentQuery query) {
        SpendRuleAssignmentNameRefs ref = SpendRuleAssignmentNameRefs.spendRuleAssignment;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.assignmentSn.eq(query.getAssignmentSn()))
                .and(ref.ruleId.eq(query.getRuleId()))
                .and(ref.ruleVersion.eq(query.getRuleVersion()))
                .and(ref.scopeType.eq(query.getScopeType()))
                .and(ref.scopeId.eq(query.getScopeId()))
                .and(ref.status.eq(query.getStatus()));
        if (Boolean.TRUE.equals(query.getEffectiveOnly())) {
            LocalDateTime effectiveAt = resolveEvaluationTime(query.getEffectiveAt());
            wrapper.and(ref.status.eq(SpendRuleAssignmentStatus.ACTIVE))
                    .and(ref.effectiveFrom.le(effectiveAt))
                    .and(ref.effectiveTo.gt(effectiveAt));
        }
        wrapper.orderBy(ref.priority.asc(), ref.id.asc());
        return wrapper;
    }

    private LocalDateTime resolveEvaluationTime(LocalDateTime evaluationTime) {
        if (evaluationTime == null) {
            return LocalDateTime.now();
        }
        return evaluationTime;
    }

    private SpendRuleAssignmentExplanationStatus resolveExplanationStatus(SpendRuleAssignment assignment,
                                                                          LocalDateTime evaluatedAt) {
        if (assignment.getStatus() != SpendRuleAssignmentStatus.ACTIVE) {
            return SpendRuleAssignmentExplanationStatus.DISABLED;
        }
        if (evaluatedAt.isBefore(assignment.getEffectiveFrom())) {
            return SpendRuleAssignmentExplanationStatus.NOT_YET_EFFECTIVE;
        }
        if (!evaluatedAt.isBefore(assignment.getEffectiveTo())) {
            return SpendRuleAssignmentExplanationStatus.EXPIRED;
        }
        return SpendRuleAssignmentExplanationStatus.EFFECTIVE;
    }

    private List<String> toAssignmentEvidenceRefs(SpendRuleAssignment assignment) {
        return List.of(
                "spendRule:" + assignment.getRuleId(),
                "spendRuleVersion:" + assignment.getRuleId() + "@" + assignment.getRuleVersion(),
                "spendRuleAssignment:" + assignment.getAssignmentSn(),
                "spendRuleScope:" + assignment.getScopeType() + ":" + assignment.getScopeId());
    }

    private SpendRuleDecisionLog findDecisionLog(Long tenantId, String decisionSn) {
        SpendRuleDecisionLogNameRefs ref = SpendRuleDecisionLogNameRefs.spendRuleDecisionLog;
        return spendRuleDecisionLogMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.decisionSn.eq(decisionSn)));
    }

    private SpendRuleDefinitionDTO readIdempotentDefinitionAfterInsertConflict(
            CreateSpendRuleDefinitionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleDefinition existing = findDefinition(request.getTenantId(), request.getRuleId());
        if (existing == null) {
            throw exception;
        }
        assertSameDefinition(request, existing);
        return toDefinitionDTO(existing);
    }

    private SpendRuleVersionDTO readIdempotentVersionAfterInsertConflict(
            PublishSpendRuleVersionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleVersion existing = findVersion(request.getTenantId(), request.getRuleId(), request.getRuleVersion());
        if (existing == null) {
            throw exception;
        }
        assertSamePublishedVersion(request, existing);
        return toVersionDTO(existing);
    }

    private SpendRuleAssignmentDTO readIdempotentAssignmentAfterInsertConflict(
            AssignSpendRuleVersionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleAssignment existing = findAssignment(request.getTenantId(), request.getAssignmentSn());
        if (existing == null) {
            throw exception;
        }
        assertSameAssignment(request, existing);
        return toAssignmentDTO(existing);
    }

    private SpendRuleDecisionLogDTO readIdempotentDecisionLogAfterInsertConflict(
            RecordSpendRuleDecisionLogRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleDecisionLog existing = findDecisionLog(request.getTenantId(), request.getDecisionSn());
        if (existing == null) {
            throw exception;
        }
        assertSameDecisionLog(request, existing);
        return toDecisionLogDTO(existing);
    }

    private void assertSameDefinition(CreateSpendRuleDefinitionRequest request, SpendRuleDefinition existing) {
        AssertUtils.isTrue(Objects.equals(existing.getRuleName(), request.getRuleName())
                        && existing.getRuleType() == request.getRuleType()
                        && existing.getRuleDomain() == request.getRuleDomain(),
                "Spend Rule 定义已存在但内容不一致，ruleId = {}",
                request.getRuleId());
    }

    private void assertSamePublishedVersion(PublishSpendRuleVersionRequest request, SpendRuleVersion existing) {
        AssertUtils.isTrue(existing.getStatus() == SpendRuleVersionStatus.PUBLISHED,
                "Spend Rule 版本状态不可重复发布，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
        AssertUtils.isTrue(Objects.equals(existing.getRuleDigest(), request.getRuleDigest())
                        && Objects.equals(existing.getRuleSpec(), request.getRuleSpec()),
                "Spend Rule 版本已发布但内容摘要不一致，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
    }

    private void assertSameAssignment(AssignSpendRuleVersionRequest request, SpendRuleAssignment existing) {
        AssertUtils.isTrue(Objects.equals(existing.getRuleId(), request.getRuleId())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion())
                        && existing.getScopeType() == request.getScopeType()
                        && Objects.equals(existing.getScopeId(), request.getScopeId())
                        && Objects.equals(existing.getPriority(), request.getPriority())
                        && existing.getConflictPolicy() == request.getConflictPolicy()
                        && Objects.equals(existing.getEffectiveFrom(), request.getEffectiveFrom())
                        && Objects.equals(existing.getEffectiveTo(), request.getEffectiveTo()),
                "Spend Rule 挂载已存在但内容不一致，assignmentSn = {}",
                request.getAssignmentSn());
    }

    private void assertSameDecisionLog(RecordSpendRuleDecisionLogRequest request, SpendRuleDecisionLog existing) {
        AssertUtils.isTrue(Objects.equals(existing.getRuleId(), request.getRuleId())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion())
                        && Objects.equals(existing.getAssignmentSn(), request.getAssignmentSn())
                        && existing.getScopeType() == request.getScopeType()
                        && Objects.equals(existing.getScopeId(), request.getScopeId())
                        && Objects.equals(existing.getInstrumentSn(), request.getInstrumentSn())
                        && existing.getAction() == request.getAction()
                        && Objects.equals(existing.getAmount(), request.getAmount())
                        && existing.getCurrency() == request.getCurrency()
                        && Objects.equals(existing.getBusinessScene(), request.getBusinessScene())
                        && Objects.equals(existing.getBusinessSn(), request.getBusinessSn())
                        && existing.getDecisionResult() == request.getDecisionResult()
                        && Objects.equals(existing.getRejectReason(), request.getRejectReason())
                        && Objects.equals(existing.getDecisionDigest(), request.getDecisionDigest()),
                "Spend Rule 决策流水已存在但内容不一致，decisionSn = {}",
                request.getDecisionSn());
    }

    private SpendRuleDefinition toDefinitionEntity(CreateSpendRuleDefinitionRequest request) {
        SpendRuleDefinition result = new SpendRuleDefinition();
        result.setTenantId(request.getTenantId());
        result.setRuleId(request.getRuleId());
        result.setRuleName(request.getRuleName());
        result.setRuleType(request.getRuleType());
        result.setRuleDomain(request.getRuleDomain());
        result.setStatus(SpendRuleDefinitionStatus.ACTIVE);
        result.setDescription(request.getDescription());
        return result;
    }

    private SpendRuleVersion toVersionEntity(PublishSpendRuleVersionRequest request) {
        SpendRuleVersion result = new SpendRuleVersion();
        result.setTenantId(request.getTenantId());
        result.setRuleId(request.getRuleId());
        result.setRuleVersion(request.getRuleVersion());
        result.setRuleSpec(request.getRuleSpec());
        result.setRuleDigest(request.getRuleDigest());
        result.setStatus(SpendRuleVersionStatus.PUBLISHED);
        result.setOperatorId(request.getOperatorId());
        result.setAuditReferenceSn(request.getAuditReferenceSn());
        result.setDescription(request.getDescription());
        return result;
    }

    private SpendRuleAssignment toAssignmentEntity(AssignSpendRuleVersionRequest request) {
        SpendRuleAssignment result = new SpendRuleAssignment();
        result.setTenantId(request.getTenantId());
        result.setAssignmentSn(request.getAssignmentSn());
        result.setRuleId(request.getRuleId());
        result.setRuleVersion(request.getRuleVersion());
        result.setScopeType(request.getScopeType());
        result.setScopeId(request.getScopeId());
        result.setPriority(request.getPriority());
        result.setConflictPolicy(request.getConflictPolicy());
        result.setEffectiveFrom(request.getEffectiveFrom());
        result.setEffectiveTo(request.getEffectiveTo());
        result.setStatus(SpendRuleAssignmentStatus.ACTIVE);
        result.setDescription(request.getDescription());
        return result;
    }

    private SpendRuleDecisionLog toDecisionLogEntity(RecordSpendRuleDecisionLogRequest request) {
        SpendRuleDecisionLog result = new SpendRuleDecisionLog();
        result.setTenantId(request.getTenantId());
        result.setDecisionSn(request.getDecisionSn());
        result.setRuleId(request.getRuleId());
        result.setRuleVersion(request.getRuleVersion());
        result.setAssignmentSn(request.getAssignmentSn());
        result.setScopeType(request.getScopeType());
        result.setScopeId(request.getScopeId());
        result.setInstrumentSn(request.getInstrumentSn());
        result.setAction(request.getAction());
        result.setAmount(request.getAmount());
        result.setCurrency(request.getCurrency());
        result.setBusinessScene(request.getBusinessScene());
        result.setBusinessSn(request.getBusinessSn());
        result.setDecisionResult(request.getDecisionResult());
        result.setRejectReason(request.getRejectReason());
        result.setDecisionDigest(request.getDecisionDigest());
        return result;
    }

    private SpendRuleDefinitionDTO toDefinitionDTO(SpendRuleDefinition entity) {
        return new SpendRuleDefinitionDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setRuleId(entity.getRuleId())
                .setRuleName(entity.getRuleName())
                .setRuleType(entity.getRuleType())
                .setRuleDomain(entity.getRuleDomain())
                .setStatus(entity.getStatus())
                .setDescription(entity.getDescription());
    }

    private SpendRuleVersionDTO toVersionDTO(SpendRuleVersion entity) {
        return new SpendRuleVersionDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setRuleId(entity.getRuleId())
                .setRuleVersion(entity.getRuleVersion())
                .setRuleSpec(entity.getRuleSpec())
                .setRuleDigest(entity.getRuleDigest())
                .setStatus(entity.getStatus())
                .setOperatorId(entity.getOperatorId())
                .setAuditReferenceSn(entity.getAuditReferenceSn())
                .setDescription(entity.getDescription());
    }

    private SpendRuleAssignmentDTO toAssignmentDTO(SpendRuleAssignment entity) {
        return new SpendRuleAssignmentDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setAssignmentSn(entity.getAssignmentSn())
                .setRuleId(entity.getRuleId())
                .setRuleVersion(entity.getRuleVersion())
                .setScopeType(entity.getScopeType())
                .setScopeId(entity.getScopeId())
                .setPriority(entity.getPriority())
                .setConflictPolicy(entity.getConflictPolicy())
                .setEffectiveFrom(entity.getEffectiveFrom())
                .setEffectiveTo(entity.getEffectiveTo())
                .setStatus(entity.getStatus())
                .setDescription(entity.getDescription());
    }

    private SpendRuleDecisionLogDTO toDecisionLogDTO(SpendRuleDecisionLog entity) {
        return new SpendRuleDecisionLogDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setTenantId(entity.getTenantId())
                .setDecisionSn(entity.getDecisionSn())
                .setRuleId(entity.getRuleId())
                .setRuleVersion(entity.getRuleVersion())
                .setAssignmentSn(entity.getAssignmentSn())
                .setScopeType(entity.getScopeType())
                .setScopeId(entity.getScopeId())
                .setInstrumentSn(entity.getInstrumentSn())
                .setAction(entity.getAction())
                .setAmount(entity.getAmount())
                .setCurrency(entity.getCurrency())
                .setBusinessScene(entity.getBusinessScene())
                .setBusinessSn(entity.getBusinessSn())
                .setDecisionResult(entity.getDecisionResult())
                .setRejectReason(entity.getRejectReason())
                .setDecisionDigest(entity.getDecisionDigest());
    }
}

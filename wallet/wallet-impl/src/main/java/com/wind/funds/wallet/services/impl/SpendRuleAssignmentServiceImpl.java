package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.SpendRuleAssignment;
import com.wind.funds.wallet.dal.entities.table.SpendRuleAssignmentNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleAssignmentMapper;
import com.wind.funds.wallet.enums.SpendRuleAssignmentExplanationStatus;
import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.service.SpendRuleAssignmentService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spend Rule 挂载基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleAssignmentServiceImpl implements SpendRuleAssignmentService {

    private static final int ASSIGNMENT_QUERY_PAGE_SIZE = 100;

    private final SpendRuleAssignmentMapper spendRuleAssignmentMapper;

    @Override
    public @NonNull Long createAssignment(
            @NonNull AssignSpendRuleVersionRequest request) {
        SpendRuleAssignment entity = toEntity(request);
        spendRuleAssignmentMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "挂载 Spend Rule 版本失败，assignmentSn = {}",
                request.getAssignmentSn());
        return entity.getId();
    }

    @Override
    public @NonNull SpendRuleAssignmentDTO getAssignmentById(@NonNull Long id) {
        SpendRuleAssignment entity = spendRuleAssignmentMapper.selectOneById(id);
        AssertUtils.notNull(entity, "Spend Rule 挂载不存在，id = {}", id);
        return toDTO(entity);
    }

    @Override
    public @Nullable SpendRuleAssignmentDTO findAssignment(@NonNull Long tenantId,
                                                           @NonNull String assignmentSn) {
        SpendRuleAssignment entity = findAssignmentEntity(tenantId, assignmentSn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendRuleAssignmentDTO> queryAssignments(
            @NonNull SpendRuleAssignmentQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        validateAssignmentQuery(query);
        return MybatisQueryHelper.<SpendRuleAssignment, SpendRuleAssignmentDTO>query(toQueryWrapper(query, options))
                .counter(spendRuleAssignmentMapper::selectCountByQuery)
                .resultQueryFunc(spendRuleAssignmentMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendRuleAssignmentDTO> queryAssignments(
            @NonNull SpendRuleAssignmentQuery query) {
        return queryAssignments(query, DefaultPageQueryOptions.defaults(ASSIGNMENT_QUERY_PAGE_SIZE)).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleAssignmentExplanationDTO explainAssignment(
            @NonNull SpendRuleAssignmentExplainQuery query) {
        validateAssignmentExplainQuery(query);
        SpendRuleAssignmentDTO assignment = findAssignment(query.getTenantId(), query.getAssignmentSn());
        AssertUtils.notNull(assignment, "Spend Rule 挂载不存在，assignmentSn = {}", query.getAssignmentSn());
        LocalDateTime evaluatedAt = resolveEvaluationTime(query.getExplainAt());
        SpendRuleAssignmentExplanationStatus status = resolveExplanationStatus(assignment, evaluatedAt);
        return new SpendRuleAssignmentExplanationDTO()
                .setAssignment(assignment)
                .setEvaluatedAt(evaluatedAt)
                .setEffective(status == SpendRuleAssignmentExplanationStatus.EFFECTIVE)
                .setExplanationStatus(status)
                .setExplanationMessage(status.getDesc())
                .setEvidenceRefs(toAssignmentEvidenceRefs(assignment));
    }

    @Override
    public @NonNull SpendRuleAssignmentDTO getActiveAssignment(@NonNull Long tenantId,
                                                               @NonNull String assignmentSn) {
        SpendRuleAssignmentDTO assignment = findAssignment(tenantId, assignmentSn);
        AssertUtils.notNull(assignment, "Spend Rule 挂载不存在，assignmentSn = {}", assignmentSn);
        AssertUtils.isTrue(assignment.getStatus() == SpendRuleAssignmentStatus.ACTIVE,
                "Spend Rule 挂载不可用，assignmentSn = {}",
                assignmentSn);
        return assignment;
    }

    private void validateAssignmentQuery(SpendRuleAssignmentQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
    }

    private void validateAssignmentExplainQuery(SpendRuleAssignmentExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getAssignmentSn(), "Spend Rule 挂载流水号不能为空");
    }

    private SpendRuleAssignment findAssignmentEntity(Long tenantId, String assignmentSn) {
        SpendRuleAssignmentNameRefs ref = SpendRuleAssignmentNameRefs.spendRuleAssignment;
        return spendRuleAssignmentMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.assignmentSn.eq(assignmentSn)));
    }

    private QueryWrapper toQueryWrapper(SpendRuleAssignmentQuery query,
                                        WindQuery<? extends QueryOrderField> options) {
        SpendRuleAssignmentNameRefs ref = SpendRuleAssignmentNameRefs.spendRuleAssignment;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
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

    private SpendRuleAssignmentExplanationStatus resolveExplanationStatus(SpendRuleAssignmentDTO assignment,
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

    private List<String> toAssignmentEvidenceRefs(SpendRuleAssignmentDTO assignment) {
        return List.of(
                "spendRule:" + assignment.getRuleId(),
                "spendRuleVersion:" + assignment.getRuleId() + "@" + assignment.getRuleVersion(),
                "spendRuleAssignment:" + assignment.getAssignmentSn(),
                "spendRuleScope:" + assignment.getScopeType() + ":" + assignment.getScopeId());
    }

    private SpendRuleAssignment toEntity(AssignSpendRuleVersionRequest request) {
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

    private SpendRuleAssignmentDTO toDTO(SpendRuleAssignment entity) {
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
}

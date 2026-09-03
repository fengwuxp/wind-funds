package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.SpendRuleBinding;
import com.wind.funds.wallet.dal.entities.table.SpendRuleBindingNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleBindingMapper;
import com.wind.funds.wallet.enums.SpendRuleBindingExplanationStatus;
import com.wind.funds.wallet.enums.SpendRuleBindingState;
import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.dto.SpendRuleBindingExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleBindingExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleBindingQuery;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.ResumeSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.RetireSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.SuspendSpendRuleBindingRequest;
import com.wind.funds.wallet.service.SpendRuleBindingService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
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
public class SpendRuleBindingServiceImpl implements SpendRuleBindingService {

    private static final int BINDING_QUERY_PAGE_SIZE = 100;

    private static final WindSequenceType SPEND_RULE_BINDING_SEQUENCE_TYPE =
            WindSequenceType.immutable("SPEND_RULE_BINDING", "SRB", 6);

    private final SpendRuleBindingMapper spendRuleBindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DataIntegrityViolationException.class)
    public @NonNull SpendRuleBindingDTO createSpendRuleBinding(
            @NonNull CreateSpendRuleBindingRequest request) {
        SpendRuleBinding entity = toEntity(request);
        spendRuleBindingMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "挂载 Spend Rule 版本失败，ruleId = {}, ruleVersion = {}, auditReferenceSn = {}",
                request.getRuleId(),
                request.getRuleVersion(),
                request.getAuditReferenceSn());
        SpendRuleBindingDTO result = findSpendRuleBinding(request.getTenantId(), entity.getSn());
        AssertUtils.notNull(result, "挂载 Spend Rule 版本后回读失败，sn = {}", entity.getSn());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendSpendRuleBinding(@NonNull SuspendSpendRuleBindingRequest request) {
        validateSuspendRequest(request);
        SpendRuleBinding entity = getSpendRuleBindingEntity(request.getTenantId(), request.getSn());
        AssertUtils.isTrue(entity.getState() == SpendRuleBindingState.ACTIVE,
                "只有有效的 Spend Rule 挂载可以暂停，sn = {}",
                request.getSn());
        updateBindingState(entity, SpendRuleBindingState.SUSPENDED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeSpendRuleBinding(@NonNull ResumeSpendRuleBindingRequest request) {
        validateResumeRequest(request);
        SpendRuleBinding entity = getSpendRuleBindingEntity(request.getTenantId(), request.getSn());
        AssertUtils.isTrue(entity.getState() == SpendRuleBindingState.SUSPENDED,
                "只有已暂停的 Spend Rule 挂载可以恢复，sn = {}",
                request.getSn());
        updateBindingState(entity, SpendRuleBindingState.ACTIVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retireSpendRuleBinding(@NonNull RetireSpendRuleBindingRequest request) {
        validateRetireRequest(request);
        SpendRuleBinding entity = getSpendRuleBindingEntity(request.getTenantId(), request.getSn());
        AssertUtils.isTrue(entity.getState() == SpendRuleBindingState.ACTIVE
                        || entity.getState() == SpendRuleBindingState.SUSPENDED,
                "只有有效或已暂停的 Spend Rule 挂载可以退役，sn = {}",
                request.getSn());
        updateBindingState(entity, SpendRuleBindingState.RETIRED);
    }

    @Override
    public @Nullable SpendRuleBindingDTO findSpendRuleBinding(@NonNull Long tenantId,
                                                           @NonNull String sn) {
        SpendRuleBinding entity = findSpendRuleBindingEntity(tenantId, sn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendRuleBindingDTO> querySpendRuleBindings(
            @NonNull SpendRuleBindingQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return MybatisQueryHelper.<SpendRuleBinding, SpendRuleBindingDTO>query(toQueryWrapper(query, options))
                .counter(spendRuleBindingMapper::selectCountByQuery)
                .resultQueryFunc(spendRuleBindingMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendRuleBindingDTO> querySpendRuleBindings(
            @NonNull SpendRuleBindingQuery query) {
        return querySpendRuleBindings(query, DefaultPageQueryOptions.defaults(BINDING_QUERY_PAGE_SIZE)).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleBindingExplanationDTO explainSpendRuleBinding(
            @NonNull SpendRuleBindingExplainQuery query) {
        validateBindingExplainQuery(query);
        SpendRuleBindingDTO binding = findSpendRuleBinding(query.getTenantId(), query.getSn());
        AssertUtils.notNull(binding, "Spend Rule 挂载不存在，sn = {}", query.getSn());
        LocalDateTime evaluatedAt = resolveEvaluationTime(query.getExplainAt());
        SpendRuleBindingExplanationStatus status = resolveExplanationStatus(binding, evaluatedAt);
        return new SpendRuleBindingExplanationDTO()
                .setBinding(binding)
                .setEvaluatedAt(evaluatedAt)
                .setEffective(status == SpendRuleBindingExplanationStatus.EFFECTIVE)
                .setExplanationStatus(status)
                .setBindingSummary(status.getDesc())
                .setEvidenceRefs(toBindingEvidenceRefs(binding));
    }

    @Override
    public @NonNull SpendRuleBindingDTO getActiveSpendRuleBinding(@NonNull Long tenantId,
                                                               @NonNull String sn) {
        SpendRuleBindingDTO binding = findSpendRuleBinding(tenantId, sn);
        AssertUtils.notNull(binding, "Spend Rule 挂载不存在，sn = {}", sn);
        AssertUtils.isTrue(binding.getState() == SpendRuleBindingState.ACTIVE,
                "Spend Rule 挂载不可用，sn = {}",
                sn);
        return binding;
    }

    private void validateBindingExplainQuery(SpendRuleBindingExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getSn(), "Spend Rule 挂载流水号不能为空");
    }

    private void validateSuspendRequest(SuspendSpendRuleBindingRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getSn(), "Spend Rule 挂载流水号不能为空");
    }

    private void validateResumeRequest(ResumeSpendRuleBindingRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getSn(), "Spend Rule 挂载流水号不能为空");
    }

    private void validateRetireRequest(RetireSpendRuleBindingRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getSn(), "Spend Rule 挂载流水号不能为空");
    }

    private SpendRuleBinding getSpendRuleBindingEntity(Long tenantId, String sn) {
        SpendRuleBinding entity = findSpendRuleBindingEntity(tenantId, sn);
        AssertUtils.notNull(entity, "Spend Rule 挂载不存在，sn = {}", sn);
        return entity;
    }

    private void updateBindingState(SpendRuleBinding binding,
                                     SpendRuleBindingState state) {
        SpendRuleBindingNameRefs ref = SpendRuleBindingNameRefs.spendRuleBinding;
        SpendRuleBinding entity = UpdateEntity.of(SpendRuleBinding.class);
        UpdateWrapper<SpendRuleBinding> updateWrapper = UpdateWrapper.of(entity);
        updateWrapper.set(ref.state, state, true);
        AssertUtils.isTrue(spendRuleBindingMapper.updateByQuery(entity, QueryWrapper.create()
                        .where(ref.tenantId.eq(binding.getTenantId()))
                        .and(ref.sn.eq(binding.getSn()))
                        .and(ref.state.eq(binding.getState()))) == 1,
                "Spend Rule 挂载已变更，请重试，sn = {}",
                binding.getSn());
    }

    private SpendRuleBinding findSpendRuleBindingEntity(Long tenantId, String sn) {
        SpendRuleBindingNameRefs ref = SpendRuleBindingNameRefs.spendRuleBinding;
        return spendRuleBindingMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(sn)));
    }

    private QueryWrapper toQueryWrapper(SpendRuleBindingQuery query,
                                        WindQuery<? extends QueryOrderField> options) {
        SpendRuleBindingNameRefs ref = SpendRuleBindingNameRefs.spendRuleBinding;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.sn.eq(query.getSn()))
                .and(ref.ruleId.eq(query.getRuleId()))
                .and(ref.ruleVersion.eq(query.getRuleVersion()))
                .and(ref.scopeType.eq(query.getScopeType()))
                .and(ref.scopeId.eq(query.getScopeId()))
                .and(ref.state.eq(query.getState()))
                .and(ref.auditReferenceSn.eq(query.getAuditReferenceSn()));
        if (Boolean.TRUE.equals(query.getEffectiveOnly())) {
            LocalDateTime effectiveAt = resolveEvaluationTime(query.getEffectiveAt());
            wrapper.and(ref.state.eq(SpendRuleBindingState.ACTIVE))
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

    private SpendRuleBindingExplanationStatus resolveExplanationStatus(SpendRuleBindingDTO binding,
                                                                          LocalDateTime evaluatedAt) {
        return switch (binding.getState()) {
            case SUSPENDED -> SpendRuleBindingExplanationStatus.SUSPENDED;
            case RETIRED -> SpendRuleBindingExplanationStatus.RETIRED;
            case ACTIVE -> {
                if (evaluatedAt.isBefore(binding.getEffectiveFrom())) {
                    yield SpendRuleBindingExplanationStatus.NOT_YET_EFFECTIVE;
                }
                if (!evaluatedAt.isBefore(binding.getEffectiveTo())) {
                    yield SpendRuleBindingExplanationStatus.EXPIRED;
                }
                yield SpendRuleBindingExplanationStatus.EFFECTIVE;
            }
        };
    }

    private List<String> toBindingEvidenceRefs(SpendRuleBindingDTO binding) {
        return List.of(
                "spendRule:" + binding.getRuleId(),
                "spendRuleVersion:" + binding.getRuleId() + "@" + binding.getRuleVersion(),
                "spendRuleBinding:" + binding.getSn(),
                "spendRuleScope:" + binding.getScopeType() + ":" + binding.getScopeId());
    }

    private SpendRuleBinding toEntity(CreateSpendRuleBindingRequest request) {
        SpendRuleBinding result = new SpendRuleBinding();
        result.setTenantId(request.getTenantId());
        result.setSn(TemporalSequenceFactory.hourNext(SPEND_RULE_BINDING_SEQUENCE_TYPE));
        result.setRuleId(request.getRuleId());
        result.setRuleVersion(request.getRuleVersion());
        result.setScopeType(request.getScopeType());
        result.setScopeId(request.getScopeId());
        result.setPriority(request.getPriority());
        result.setConflictPolicy(request.getConflictPolicy());
        result.setEffectiveFrom(request.getEffectiveFrom());
        result.setEffectiveTo(request.getEffectiveTo());
        result.setState(SpendRuleBindingState.ACTIVE);
        result.setAuditReferenceSn(request.getAuditReferenceSn());
        result.setDescription(request.getDescription());
        return result;
    }

    private SpendRuleBindingDTO toDTO(SpendRuleBinding entity) {
        return new SpendRuleBindingDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setSn(entity.getSn())
                .setRuleId(entity.getRuleId())
                .setRuleVersion(entity.getRuleVersion())
                .setScopeType(entity.getScopeType())
                .setScopeId(entity.getScopeId())
                .setPriority(entity.getPriority())
                .setConflictPolicy(entity.getConflictPolicy())
                .setEffectiveFrom(entity.getEffectiveFrom())
                .setEffectiveTo(entity.getEffectiveTo())
                .setState(entity.getState())
                .setAuditReferenceSn(entity.getAuditReferenceSn())
                .setDescription(entity.getDescription());
    }
}

package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.SpendRuleDecisionLog;
import com.wind.funds.wallet.dal.entities.table.SpendRuleDecisionLogNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleDecisionLogMapper;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionLogQuery;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import com.wind.funds.wallet.service.SpendRuleDecisionLogService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Spend Rule 决策记录基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDecisionLogServiceImpl implements SpendRuleDecisionLogService {

    private final SpendRuleDecisionLogMapper spendRuleDecisionLogMapper;

    @Override
    public @NonNull Long createDecisionLog(
            @NonNull RecordSpendRuleDecisionLogRequest request) {
        SpendRuleDecisionLog entity = toDecisionLogEntity(request);
        spendRuleDecisionLogMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "记录 Spend Rule 决策记录失败，decisionSn = {}",
                request.getDecisionSn());
        return entity.getId();
    }

    @Override
    public @NonNull SpendRuleDecisionLogDTO getDecisionLogById(@NonNull Long id) {
        SpendRuleDecisionLog entity = spendRuleDecisionLogMapper.selectOneById(id);
        AssertUtils.notNull(entity, "Spend Rule 决策记录不存在，id = {}", id);
        return toDTO(entity);
    }

    @Override
    public @Nullable SpendRuleDecisionLogDTO findDecisionLog(@NonNull Long tenantId,
                                                             @NonNull String decisionSn) {
        SpendRuleDecisionLog entity = findDecisionLogEntity(tenantId, decisionSn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendRuleDecisionLogDTO> queryDecisionLogs(
            @NonNull SpendRuleDecisionLogQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        validateDecisionLogQuery(query);
        return MybatisQueryHelper.<SpendRuleDecisionLog, SpendRuleDecisionLogDTO>query(
                        toDecisionLogQueryWrapper(query, options))
                .counter(spendRuleDecisionLogMapper::selectCountByQuery)
                .resultQueryFunc(spendRuleDecisionLogMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private void validateDecisionLogQuery(SpendRuleDecisionLogQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.isTrue(hasDecisionLogNarrowCondition(query),
                "至少提供一个 Spend Rule 决策查询条件");
    }

    private boolean hasDecisionLogNarrowCondition(SpendRuleDecisionLogQuery query) {
        return StringUtils.hasText(query.getDecisionSn())
                || StringUtils.hasText(query.getRuleId())
                || StringUtils.hasText(query.getRuleVersion())
                || StringUtils.hasText(query.getAssignmentSn())
                || query.getScopeType() != null
                || StringUtils.hasText(query.getScopeId())
                || StringUtils.hasText(query.getInstrumentSn())
                || query.getAction() != null
                || query.getCurrency() != null
                || StringUtils.hasText(query.getBusinessScene())
                || StringUtils.hasText(query.getBusinessSn())
                || query.getDecisionResult() != null;
    }

    private SpendRuleDecisionLog findDecisionLogEntity(Long tenantId, String decisionSn) {
        SpendRuleDecisionLogNameRefs ref = SpendRuleDecisionLogNameRefs.spendRuleDecisionLog;
        return spendRuleDecisionLogMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.decisionSn.eq(decisionSn)));
    }

    private QueryWrapper toDecisionLogQueryWrapper(SpendRuleDecisionLogQuery query,
                                                   WindQuery<? extends QueryOrderField> options) {
        SpendRuleDecisionLogNameRefs ref = SpendRuleDecisionLogNameRefs.spendRuleDecisionLog;
        return MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.decisionSn.eq(query.getDecisionSn()))
                .and(ref.ruleId.eq(query.getRuleId()))
                .and(ref.ruleVersion.eq(query.getRuleVersion()))
                .and(ref.assignmentSn.eq(query.getAssignmentSn()))
                .and(ref.scopeType.eq(query.getScopeType()))
                .and(ref.scopeId.eq(query.getScopeId()))
                .and(ref.instrumentSn.eq(query.getInstrumentSn()))
                .and(ref.action.eq(query.getAction()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.businessScene.eq(query.getBusinessScene()))
                .and(ref.businessSn.eq(query.getBusinessSn()))
                .and(ref.decisionResult.eq(query.getDecisionResult()))
                .orderBy(ref.id.asc());
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

    private SpendRuleDecisionLogDTO toDTO(SpendRuleDecisionLog entity) {
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

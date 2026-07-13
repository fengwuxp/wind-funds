package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.SpendRuleDecisionRecord;
import com.wind.funds.wallet.dal.entities.table.SpendRuleDecisionRecordNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleDecisionRecordMapper;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionRecordDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionRecordQuery;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
import com.wind.funds.wallet.service.SpendRuleBindingService;
import com.wind.funds.wallet.service.SpendRuleDecisionRecordService;
import com.wind.funds.wallet.service.SpendRuleVersionService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Spend Rule 决策记录服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDecisionRecordServiceImpl implements SpendRuleDecisionRecordService {

    private static final int DECISION_RECORD_QUERY_PAGE_SIZE = 100;

    private final SpendRuleDecisionRecordMapper spendRuleDecisionRecordMapper;

    private final SpendRuleVersionService spendRuleVersionService;

    private final SpendRuleBindingService spendRuleBindingService;

    private @NonNull Long insertDecisionRecord(
            @NonNull RecordSpendRuleDecisionRecordRequest request) {
        SpendRuleDecisionRecord entity = toDecisionRecordEntity(request);
        spendRuleDecisionRecordMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "记录 Spend Rule 决策记录失败，decisionSn = {}",
                request.getDecisionSn());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleDecisionRecordDTO recordDecision(
            @NonNull RecordSpendRuleDecisionRecordRequest request) {
        validateDecisionRecordRequest(request);
        spendRuleVersionService.getPublishedVersion(request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        assertBindingMatchesIfPresent(request);
        SpendRuleDecisionRecordDTO existing = findDecisionRecord(request.getTenantId(), request.getDecisionSn());
        if (existing != null) {
            assertSameDecisionRecord(request, existing);
            return existing;
        }
        try {
            Long decisionRecordId = insertDecisionRecord(request);
            return getDecisionRecordById(decisionRecordId);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentDecisionRecordAfterInsertConflict(request, exception);
        }
    }

    @Override
    public @NonNull SpendRuleDecisionRecordDTO getDecisionRecordById(@NonNull Long id) {
        SpendRuleDecisionRecord entity = spendRuleDecisionRecordMapper.selectOneById(id);
        AssertUtils.notNull(entity, "Spend Rule 决策记录不存在，id = {}", id);
        return toDTO(entity);
    }

    @Override
    public @Nullable SpendRuleDecisionRecordDTO findDecisionRecord(@NonNull Long tenantId,
                                                             @NonNull String decisionSn) {
        SpendRuleDecisionRecord entity = findDecisionRecordEntity(tenantId, decisionSn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendRuleDecisionRecordDTO> queryDecisionRecords(
            @NonNull SpendRuleDecisionRecordQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        validateDecisionRecordQuery(query);
        return MybatisQueryHelper.<SpendRuleDecisionRecord, SpendRuleDecisionRecordDTO>query(
                        toDecisionRecordQueryWrapper(query, options))
                .counter(spendRuleDecisionRecordMapper::selectCountByQuery)
                .resultQueryFunc(spendRuleDecisionRecordMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendRuleDecisionRecordDTO> queryDecisions(
            @NonNull SpendRuleDecisionRecordQuery query) {
        return queryDecisionRecords(query, DefaultPageQueryOptions.defaults(DECISION_RECORD_QUERY_PAGE_SIZE))
                .getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleDecisionExplanationDTO explainDecision(
            @NonNull SpendRuleDecisionExplainQuery query) {
        validateDecisionExplainQuery(query);
        SpendRuleDecisionRecordDTO decision = findDecisionRecord(query.getTenantId(), query.getDecisionSn());
        AssertUtils.notNull(decision, "Spend Rule 决策记录不存在，decisionSn = {}", query.getDecisionSn());
        return new SpendRuleDecisionExplanationDTO()
                .setDecision(decision)
                .setDecisionSummary(toDecisionSummary(decision))
                .setEvidenceRefs(toDecisionEvidenceRefs(decision));
    }

    private void validateDecisionRecordRequest(RecordSpendRuleDecisionRecordRequest request) {
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
        SpendRuleDigestValidator.assertSha256Digest(request.getDecisionDigest(), "Spend Rule 决策摘要");
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

    private void assertBindingMatchesIfPresent(RecordSpendRuleDecisionRecordRequest request) {
        if (request.getSpendRuleBindingSn() == null) {
            return;
        }
        SpendRuleBindingDTO binding =
                spendRuleBindingService.getActiveSpendRuleBinding(request.getTenantId(), request.getSpendRuleBindingSn());
        assertBindingEffectiveAt(request, binding, LocalDateTime.now());
        AssertUtils.isTrue(Objects.equals(binding.getRuleId(), request.getRuleId())
                        && Objects.equals(binding.getRuleVersion(), request.getRuleVersion())
                        && binding.getScopeType() == request.getScopeType()
                        && Objects.equals(binding.getScopeId(), request.getScopeId()),
                "Spend Rule 决策记录与挂载不一致，decisionSn = {}",
                request.getDecisionSn());
    }

    private void assertBindingEffectiveAt(RecordSpendRuleDecisionRecordRequest request,
                                          SpendRuleBindingDTO binding,
                                          LocalDateTime effectiveAt) {
        AssertUtils.isTrue(!effectiveAt.isBefore(binding.getEffectiveFrom())
                        && effectiveAt.isBefore(binding.getEffectiveTo()),
                "Spend Rule 挂载未在生效时间点生效，spendRuleBindingSn = {}",
                request.getSpendRuleBindingSn());
    }

    private SpendRuleDecisionRecordDTO readIdempotentDecisionRecordAfterInsertConflict(
            RecordSpendRuleDecisionRecordRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleDecisionRecordDTO existing = findDecisionRecord(request.getTenantId(), request.getDecisionSn());
        if (existing == null) {
            throw exception;
        }
        assertSameDecisionRecord(request, existing);
        return existing;
    }

    private void assertSameDecisionRecord(RecordSpendRuleDecisionRecordRequest request,
                                       SpendRuleDecisionRecordDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getRuleId(), request.getRuleId())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion())
                        && Objects.equals(existing.getSpendRuleBindingSn(), request.getSpendRuleBindingSn())
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

    private void validateDecisionRecordQuery(SpendRuleDecisionRecordQuery query) {
        AssertUtils.isTrue(hasDecisionRecordNarrowCondition(query),
                "至少提供一个 Spend Rule 决策查询条件");
    }

    private void validateDecisionExplainQuery(SpendRuleDecisionExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getDecisionSn(), "Spend Rule 决策流水号不能为空");
    }

    private String toDecisionSummary(SpendRuleDecisionRecordDTO decision) {
        if (decision.getDecisionResult() == SpendControlDecisionResult.REJECTED) {
            return decision.getDecisionResult().getDesc() + "：" + decision.getRejectReason();
        }
        return decision.getDecisionResult().getDesc();
    }

    private List<String> toDecisionEvidenceRefs(SpendRuleDecisionRecordDTO decision) {
        List<String> refs = new ArrayList<>();
        refs.add("spendRule:" + decision.getRuleId());
        refs.add("spendRuleVersion:" + decision.getRuleId() + "@" + decision.getRuleVersion());
        if (StringUtils.hasText(decision.getSpendRuleBindingSn())) {
            refs.add("spendRuleBinding:" + decision.getSpendRuleBindingSn());
        }
        refs.add("spendRuleScope:" + decision.getScopeType() + ":" + decision.getScopeId());
        refs.add("spendRuleDecision:" + decision.getDecisionSn());
        if (decision.getId() != null) {
            refs.add("spendRuleDecisionRecord:" + decision.getId());
        }
        if (StringUtils.hasText(decision.getInstrumentSn())) {
            refs.add("paymentInstrument:" + decision.getInstrumentSn());
        }
        refs.add("spendRuleBusiness:" + decision.getBusinessScene() + ":" + decision.getBusinessSn());
        return List.copyOf(refs);
    }

    private boolean hasDecisionRecordNarrowCondition(SpendRuleDecisionRecordQuery query) {
        return StringUtils.hasText(query.getDecisionSn())
                || StringUtils.hasText(query.getRuleId())
                || StringUtils.hasText(query.getRuleVersion())
                || StringUtils.hasText(query.getSpendRuleBindingSn())
                || query.getScopeType() != null
                || StringUtils.hasText(query.getScopeId())
                || StringUtils.hasText(query.getInstrumentSn())
                || query.getAction() != null
                || query.getCurrency() != null
                || StringUtils.hasText(query.getBusinessScene())
                || StringUtils.hasText(query.getBusinessSn())
                || query.getDecisionResult() != null;
    }

    private SpendRuleDecisionRecord findDecisionRecordEntity(Long tenantId, String decisionSn) {
        SpendRuleDecisionRecordNameRefs ref = SpendRuleDecisionRecordNameRefs.spendRuleDecisionRecord;
        return spendRuleDecisionRecordMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.decisionSn.eq(decisionSn)));
    }

    private QueryWrapper toDecisionRecordQueryWrapper(SpendRuleDecisionRecordQuery query,
                                                   WindQuery<? extends QueryOrderField> options) {
        SpendRuleDecisionRecordNameRefs ref = SpendRuleDecisionRecordNameRefs.spendRuleDecisionRecord;
        return MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.decisionSn.eq(query.getDecisionSn()))
                .and(ref.ruleId.eq(query.getRuleId()))
                .and(ref.ruleVersion.eq(query.getRuleVersion()))
                .and(ref.spendRuleBindingSn.eq(query.getSpendRuleBindingSn()))
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

    private SpendRuleDecisionRecord toDecisionRecordEntity(RecordSpendRuleDecisionRecordRequest request) {
        SpendRuleDecisionRecord result = new SpendRuleDecisionRecord();
        result.setTenantId(request.getTenantId());
        result.setDecisionSn(request.getDecisionSn());
        result.setRuleId(request.getRuleId());
        result.setRuleVersion(request.getRuleVersion());
        result.setSpendRuleBindingSn(request.getSpendRuleBindingSn());
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

    private SpendRuleDecisionRecordDTO toDTO(SpendRuleDecisionRecord entity) {
        return new SpendRuleDecisionRecordDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setTenantId(entity.getTenantId())
                .setDecisionSn(entity.getDecisionSn())
                .setRuleId(entity.getRuleId())
                .setRuleVersion(entity.getRuleVersion())
                .setSpendRuleBindingSn(entity.getSpendRuleBindingSn())
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

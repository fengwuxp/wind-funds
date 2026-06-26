package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryColumn;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.SpendRuleDecisionRecord;
import com.wind.funds.wallet.dal.mapper.SpendRuleDecisionRecordMapper;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionRecordDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionRecordQuery;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
import com.wind.funds.wallet.service.SpendRuleAssignmentService;
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

    private static final String TABLE_NAME = SpendRuleDecisionRecord.TABLE_NAME;

    private static final QueryColumn ID = new QueryColumn(TABLE_NAME, "id");

    private static final QueryColumn TENANT_ID = new QueryColumn(TABLE_NAME, "tenant_id");

    private static final QueryColumn DECISION_SN = new QueryColumn(TABLE_NAME, "decision_sn");

    private static final QueryColumn RULE_ID = new QueryColumn(TABLE_NAME, "rule_id");

    private static final QueryColumn RULE_VERSION = new QueryColumn(TABLE_NAME, "rule_version");

    private static final QueryColumn ASSIGNMENT_SN = new QueryColumn(TABLE_NAME, "assignment_sn");

    private static final QueryColumn SCOPE_TYPE = new QueryColumn(TABLE_NAME, "scope_type");

    private static final QueryColumn SCOPE_ID = new QueryColumn(TABLE_NAME, "scope_id");

    private static final QueryColumn INSTRUMENT_SN = new QueryColumn(TABLE_NAME, "instrument_sn");

    private static final QueryColumn ACTION = new QueryColumn(TABLE_NAME, "action");

    private static final QueryColumn CURRENCY = new QueryColumn(TABLE_NAME, "currency");

    private static final QueryColumn BUSINESS_SCENE = new QueryColumn(TABLE_NAME, "business_scene");

    private static final QueryColumn BUSINESS_SN = new QueryColumn(TABLE_NAME, "business_sn");

    private static final QueryColumn DECISION_RESULT = new QueryColumn(TABLE_NAME, "decision_result");

    private final SpendRuleDecisionRecordMapper spendRuleDecisionRecordMapper;

    private final SpendRuleVersionService spendRuleVersionService;

    private final SpendRuleAssignmentService spendRuleAssignmentService;

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
        assertAssignmentMatchesIfPresent(request);
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
                .setAdmitted(decision.getDecisionResult() == SpendControlDecisionResult.PASSED)
                .setExplanationMessage(toDecisionExplanationMessage(decision))
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

    private void assertAssignmentMatchesIfPresent(RecordSpendRuleDecisionRecordRequest request) {
        if (request.getAssignmentSn() == null) {
            return;
        }
        SpendRuleAssignmentDTO assignment =
                spendRuleAssignmentService.getActiveAssignment(request.getTenantId(), request.getAssignmentSn());
        assertAssignmentEffectiveNow(request, assignment);
        AssertUtils.isTrue(Objects.equals(assignment.getRuleId(), request.getRuleId())
                        && Objects.equals(assignment.getRuleVersion(), request.getRuleVersion())
                        && assignment.getScopeType() == request.getScopeType()
                        && Objects.equals(assignment.getScopeId(), request.getScopeId()),
                "Spend Rule 决策记录与挂载不一致，decisionSn = {}",
                request.getDecisionSn());
    }

    private void assertAssignmentEffectiveNow(RecordSpendRuleDecisionRecordRequest request,
                                              SpendRuleAssignmentDTO assignment) {
        LocalDateTime now = LocalDateTime.now();
        AssertUtils.isTrue(!now.isBefore(assignment.getEffectiveFrom()) && now.isBefore(assignment.getEffectiveTo()),
                "Spend Rule 挂载未在当前时间生效，assignmentSn = {}",
                request.getAssignmentSn());
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

    private void validateDecisionRecordQuery(SpendRuleDecisionRecordQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.isTrue(hasDecisionRecordNarrowCondition(query),
                "至少提供一个 Spend Rule 决策查询条件");
    }

    private void validateDecisionExplainQuery(SpendRuleDecisionExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getDecisionSn(), "Spend Rule 决策流水号不能为空");
    }

    private String toDecisionExplanationMessage(SpendRuleDecisionRecordDTO decision) {
        if (decision.getDecisionResult() == SpendControlDecisionResult.REJECTED) {
            return decision.getDecisionResult().getDesc() + "：" + decision.getRejectReason();
        }
        return decision.getDecisionResult().getDesc();
    }

    private List<String> toDecisionEvidenceRefs(SpendRuleDecisionRecordDTO decision) {
        List<String> refs = new ArrayList<>();
        refs.add("spendRule:" + decision.getRuleId());
        refs.add("spendRuleVersion:" + decision.getRuleId() + "@" + decision.getRuleVersion());
        if (StringUtils.hasText(decision.getAssignmentSn())) {
            refs.add("spendRuleAssignment:" + decision.getAssignmentSn());
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

    private SpendRuleDecisionRecord findDecisionRecordEntity(Long tenantId, String decisionSn) {
        return spendRuleDecisionRecordMapper.selectOneByQuery(QueryWrapper.create()
                .from(TABLE_NAME)
                .where(TENANT_ID.eq(tenantId))
                .and(DECISION_SN.eq(decisionSn)));
    }

    private QueryWrapper toDecisionRecordQueryWrapper(SpendRuleDecisionRecordQuery query,
                                                   WindQuery<? extends QueryOrderField> options) {
        return MybatisQueryHelper.from(options).select()
                .from(TABLE_NAME)
                .where(TENANT_ID.eq(query.getTenantId()))
                .and(DECISION_SN.eq(query.getDecisionSn()))
                .and(RULE_ID.eq(query.getRuleId()))
                .and(RULE_VERSION.eq(query.getRuleVersion()))
                .and(ASSIGNMENT_SN.eq(query.getAssignmentSn()))
                .and(SCOPE_TYPE.eq(query.getScopeType()))
                .and(SCOPE_ID.eq(query.getScopeId()))
                .and(INSTRUMENT_SN.eq(query.getInstrumentSn()))
                .and(ACTION.eq(query.getAction()))
                .and(CURRENCY.eq(query.getCurrency()))
                .and(BUSINESS_SCENE.eq(query.getBusinessScene()))
                .and(BUSINESS_SN.eq(query.getBusinessSn()))
                .and(DECISION_RESULT.eq(query.getDecisionResult()))
                .orderBy(ID.asc());
    }

    private SpendRuleDecisionRecord toDecisionRecordEntity(RecordSpendRuleDecisionRecordRequest request) {
        SpendRuleDecisionRecord result = new SpendRuleDecisionRecord();
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

    private SpendRuleDecisionRecordDTO toDTO(SpendRuleDecisionRecord entity) {
        return new SpendRuleDecisionRecordDTO()
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

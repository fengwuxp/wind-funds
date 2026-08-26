package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.dal.entities.SpendControlMovement;
import com.wind.funds.wallet.dal.entities.table.SpendControlMovementNameRefs;
import com.wind.funds.wallet.dal.mapper.CreditAccountMapper;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.dal.mapper.SpendControlMovementMapper;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.mapstruct.SpendControlMovementConverter;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 控制额度变动流水服务实现。
 *
 */
@Service
@AllArgsConstructor
@Slf4j
public class SpendControlMovementServiceImpl implements SpendControlMovementService {

    private static final String SHA256_PREFIX = "sha256:";

    private static final String RESERVATION_DIGEST_DOMAIN = "wallet.spend-control.reservation";

    private static final String CONSUMPTION_DIGEST_DOMAIN = "wallet.spend-control.consumption";

    private static final String RELEASE_DIGEST_DOMAIN = "wallet.spend-control.release";

    private static final String REFUND_COMPENSATION_DIGEST_DOMAIN = "wallet.spend-control.refund-compensation";

    private static final int CONTROL_MOVEMENT_QUERY_PAGE_SIZE = 500;

    private final SpendControlMovementMapper spendControlMovementMapper;

    private final FundingAccountMapper fundingAccountMapper;

    private final CreditAccountMapper creditAccountMapper;

    private final FundsAccountQueryService fundsAccountQueryService;

    private @NonNull Long insertSpendControlMovement(@NonNull RecordSpendControlMovementRequest request) {
        SpendControlMovement entity = SpendControlMovementConverter.INSTANCE.convertToSpendControlMovement(request);
        spendControlMovementMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "记录控制额度变动流水失败，movementSn = {}", request.getMovementSn());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO recordMovement(@NonNull RecordSpendControlMovementRequest request) {
        validateIdempotencyBoundary(request);
        SpendControlMovementDTO existing = findSpendControlMovement(request.getTenantId(), request.getMovementSn());
        if (existing != null) {
            assertSameMovement(request, existing);
            logIdempotentMovementReuse(existing);
            return existing;
        }
        validateRecordRequest(request);
        FundsSubjectType targetSubjectType = targetSubjectType(request.getTargetAccountId());
        Integer version = lockTargetAccountVersion(request, targetSubjectType);
        AssertUtils.notNull(version,
                "控制额度变动目标账户不存在，accountId = {}，tenantId = {}",
                request.getTargetAccountId(),
                request.getTenantId());
        SpendControlMovement concurrentExisting = spendControlMovementMapper.selectByMovementSnWithSharedLock(
                request.getTenantId(), request.getMovementSn());
        if (concurrentExisting != null) {
            SpendControlMovementDTO result = toDTO(concurrentExisting);
            assertSameMovement(request, result);
            logIdempotentMovementReuse(result);
            return result;
        }
        Long id;
        try {
            id = insertSpendControlMovement(request);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentMovementAfterInsertConflict(request, exception);
        }
        assertMovementAmountAllowed(request);
        AssertUtils.isTrue(incrementTargetAccountVersion(request, targetSubjectType, version) == 1,
                "控制额度变动目标账户并发冲突，请重试，accountId = {}",
                request.getTargetAccountId());
        SpendControlMovementDTO result = getSpendControlMovementById(id);
        log.info("控制额度变动记录完成，等待事务提交，tenantId = {}, movementSn = {}, movementType = {}, "
                        + "businessScene = {}, businessSn = {}, transactionSn = {}, amount = {}, currency = {}",
                result.getTenantId(), result.getMovementSn(), result.getMovementType(), result.getBusinessScene(),
                result.getBusinessSn(), result.getTransactionSn(), result.getAmount(), result.getCurrency());
        return result;
    }

    @Override
    public @NonNull SpendControlMovementDTO getSpendControlMovementById(@NonNull Long id) {
        SpendControlMovement result = spendControlMovementMapper.selectOneById(id);
        AssertUtils.notNull(result, "控制额度变动流水不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @Nullable SpendControlMovementDTO findSpendControlMovement(@NonNull Long tenantId,
                                                                      @NonNull String movementSn) {
        SpendControlMovement entity = findMovementEntity(tenantId, movementSn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendControlMovementDTO> querySpendControlMovements(
            @NonNull SpendControlMovementQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return MybatisQueryHelper.<SpendControlMovement, SpendControlMovementDTO>query(toQueryWrapper(query, options))
                .counter(spendControlMovementMapper::selectCountByQuery)
                .resultQueryFunc(spendControlMovementMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendControlMovementDTO> queryMovements(@NonNull SpendControlMovementQuery query) {
        validateQuery(query);
        return queryMovementsByPage(query);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull BudgetControlProjectionDTO getBudgetControlProjection(
            @NonNull BudgetControlProjectionQuery query) {
        validateProjectionQuery(query);
        return toProjection(query, queryBudgetProjectionMovements(query));
    }

    private void validateIdempotencyBoundary(RecordSpendControlMovementRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getMovementSn(), "控制额度变动流水号不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getMovementDigest(), "控制额度变动摘要");
    }

    private void validateRecordRequest(RecordSpendControlMovementRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getMovementSn(), "控制额度变动流水号不能为空");
        AssertUtils.notNull(request.getMovementType(), "控制额度变动类型不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "控制额度变动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getMovementDigest(), "控制额度变动摘要");
        assertNoSensitiveContextVariables(request.getContextVariables());
        assertTargetAccountSupported(request);
        if (request.getMovementType().isLimitAdjustmentMovement()) {
            assertAuditFields(request, "预算控制额度调整");
        } else {
            AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
            AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
            if (request.getMovementType() == SpendControlMovementType.REFUND_COMPENSATED) {
                assertAuditFields(request, "退款控制补偿");
            }
            if (!isBusinessConfirmedRefundCompensation(request)) {
                AssertUtils.hasText(request.getSpendDecisionSn(), "Spend Rule 决策流水号不能为空");
                AssertUtils.notNull(request.getSpendDecisionResult(), "Spend Rule 决策结果不能为空");
                SpendRuleDigestValidator.assertSha256Digest(request.getSpendDecisionDigest(), "Spend Rule 决策摘要");
            }
        }
        if (request.getMovementType().isBudgetProjectionMovement()) {
            AssertUtils.hasText(controlScopeId(request), "预算控制额度变动必须提供控制范围标识");
            AssertUtils.hasText(request.getPeriodId(), "预算控制额度变动必须提供周期标识");
        }
    }

    private boolean isBusinessConfirmedRefundCompensation(RecordSpendControlMovementRequest request) {
        return request.getMovementType() == SpendControlMovementType.REFUND_COMPENSATED
                && request.getAction() == PaymentInstrumentAction.REFUND
                && !StringUtils.hasText(request.getOriginalMovementSn());
    }

    private void assertAuditFields(RecordSpendControlMovementRequest request, String actionName) {
        AssertUtils.hasText(request.getReasonCode(), "{}原因码不能为空", actionName);
        AssertUtils.hasText(request.getOperatorId(), "{}操作者不能为空", actionName);
        AssertUtils.hasText(request.getAuditReferenceSn(), "{}审计引用不能为空", actionName);
    }

    private void assertTargetAccountSupported(RecordSpendControlMovementRequest request) {
        targetSubjectType(request.getTargetAccountId());
        FundsAccount account = fundsAccountQueryService.getAccount(
                request.getTenantId(), request.getTargetAccountId());
        AssertUtils.isTrue(Objects.equals(account.getTenantId(), request.getTenantId()),
                "控制额度变动目标账户租户不匹配，accountId = {}，tenantId = {}",
                request.getTargetAccountId(),
                request.getTenantId());
        AssertUtils.isTrue(account.getCurrency() == request.getCurrency(),
                "控制额度变动目标账户币种不匹配，accountId = {}，currency = {}",
                request.getTargetAccountId(),
                request.getCurrency());
    }

    private SpendControlMovementDTO readIdempotentMovementAfterInsertConflict(
            RecordSpendControlMovementRequest request,
            DataIntegrityViolationException exception) {
        SpendControlMovement existing = spendControlMovementMapper.selectByMovementSnWithSharedLock(
                request.getTenantId(), request.getMovementSn());
        if (existing == null) {
            throw exception;
        }
        SpendControlMovementDTO result = toDTO(existing);
        assertSameMovement(request, result);
        logIdempotentMovementReuse(result);
        return result;
    }

    private void logIdempotentMovementReuse(SpendControlMovementDTO movement) {
        log.info("控制额度变动幂等复用，tenantId = {}, movementSn = {}, movementType = {}, businessScene = {}, "
                        + "businessSn = {}, transactionSn = {}, amount = {}, currency = {}",
                movement.getTenantId(), movement.getMovementSn(), movement.getMovementType(),
                movement.getBusinessScene(), movement.getBusinessSn(), movement.getTransactionSn(),
                movement.getAmount(), movement.getCurrency());
    }

    private void assertSameMovement(RecordSpendControlMovementRequest request, SpendControlMovementDTO existing) {
        assertSameMovementDigest(request, existing);
        AssertUtils.notNull(request.getTargetAccountId(), "控制额度变动目标账户不能为空");
        assertSameMovementIdentity(request, existing);
        assertSameMovementTarget(request, existing);
        assertSameMovementAmountAndRule(request, existing);
        assertSameMovementDecision(request, existing);
        assertSameMovementAudit(request, existing);
    }

    private void assertSameMovementIdentity(RecordSpendControlMovementRequest request,
                                            SpendControlMovementDTO existing) {
        AssertUtils.isTrue(existing.getMovementType() == request.getMovementType(),
                "控制额度变动流水已存在但类型不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getBusinessScene(), request.getBusinessScene()),
                "控制额度变动流水已存在但业务场景不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getBusinessSn(), request.getBusinessSn()),
                "控制额度变动流水已存在但业务流水不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getOriginalMovementSn(), request.getOriginalMovementSn()),
                "控制额度变动流水已存在但原控制额度变动流水不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getTransactionSn(), request.getTransactionSn()),
                "控制额度变动流水已存在但资金交易流水不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getInstrumentSn(), request.getInstrumentSn()),
                "控制额度变动流水已存在但支付工具号不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(existing.getAction() == request.getAction(),
                "控制额度变动流水已存在但支付工具动作不一致，movementSn = {}",
                request.getMovementSn());
    }

    private void assertSameMovementTarget(RecordSpendControlMovementRequest request,
                                          SpendControlMovementDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getTargetAccountId(), request.getTargetAccountId()),
                "控制额度变动流水已存在但目标账户不一致，movementSn = {}",
                request.getMovementSn());
    }

    private void assertSameMovementAmountAndRule(RecordSpendControlMovementRequest request,
                                                 SpendControlMovementDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getAmount(), request.getAmount()),
                "控制额度变动流水已存在但控制金额不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(existing.getCurrency() == request.getCurrency(),
                "控制额度变动流水已存在但币种不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getSpendRuleId(), request.getSpendRuleId()),
                "控制额度变动流水已存在但 Spend Rule 标识不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getSpendRuleVersion(), request.getSpendRuleVersion()),
                "控制额度变动流水已存在但 Spend Rule 版本不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getControlScopeId(), controlScopeId(request)),
                "控制额度变动流水已存在但控制范围标识不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getPeriodId(), request.getPeriodId()),
                "控制额度变动流水已存在但控制周期标识不一致，movementSn = {}",
                request.getMovementSn());
    }

    private void assertSameMovementDecision(RecordSpendControlMovementRequest request,
                                            SpendControlMovementDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getSpendDecisionSn(), request.getSpendDecisionSn()),
                "控制额度变动流水已存在但决策流水不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(existing.getSpendDecisionResult() == request.getSpendDecisionResult(),
                "控制额度变动流水已存在但决策结果不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getSpendDecisionDigest(), request.getSpendDecisionDigest()),
                "控制额度变动流水已存在但决策摘要不一致，movementSn = {}",
                request.getMovementSn());
    }

    private void assertSameMovementAudit(RecordSpendControlMovementRequest request,
                                         SpendControlMovementDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getReasonCode(), request.getReasonCode()),
                "控制额度变动流水已存在但调整原因码不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getOperatorId(), request.getOperatorId()),
                "控制额度变动流水已存在但操作者不一致，movementSn = {}",
                request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(existing.getAuditReferenceSn(), request.getAuditReferenceSn()),
                "控制额度变动流水已存在但审计引用不一致，movementSn = {}",
                request.getMovementSn());
    }

    private void assertSameMovementDigest(RecordSpendControlMovementRequest request,
                                          SpendControlMovementDTO existing) {
        if (Objects.equals(existing.getMovementDigest(), request.getMovementDigest())) {
            return;
        }
        DigestMigration migration = digestMigration(request);
        AssertUtils.isTrue(migration != null
                        && request.getMovementDigest().equals(SHA256_PREFIX
                        + FundsStableHashSupport.sha256CanonicalJson(migration.domain(), migration.facts()))
                        && existing.getMovementDigest().equals(SHA256_PREFIX
                        + FundsStableHashSupport.sha256Json(migration.facts())),
                "控制额度变动流水已存在但摘要不一致，movementSn = {}",
                request.getMovementSn());
    }

    private @Nullable DigestMigration digestMigration(RecordSpendControlMovementRequest request) {
        return switch (request.getMovementType()) {
            case RESERVED -> new DigestMigration(RESERVATION_DIGEST_DOMAIN, reservationDigestFacts(request));
            case CONSUMED -> new DigestMigration(CONSUMPTION_DIGEST_DOMAIN, transactionDigestFacts(request));
            case RELEASED -> new DigestMigration(RELEASE_DIGEST_DOMAIN, transactionDigestFacts(request));
            case REFUND_COMPENSATED -> new DigestMigration(
                    REFUND_COMPENSATION_DIGEST_DOMAIN, refundCompensationDigestFacts(request));
            default -> null;
        };
    }

    private Map<String, Object> reservationDigestFacts(RecordSpendControlMovementRequest request) {
        Map<String, Object> values = new TreeMap<>();
        values.put("amount", request.getAmount());
        values.put("businessScene", request.getBusinessScene());
        values.put("businessSn", request.getBusinessSn());
        values.put("controlScopeId", request.getControlScopeId());
        values.put("currency", request.getCurrency().name());
        values.put("instrumentSn", request.getInstrumentSn());
        values.put("movementSn", request.getMovementSn());
        values.put("periodId", request.getPeriodId());
        values.put("spendDecisionSn", request.getSpendDecisionSn());
        values.put("spendRuleId", request.getSpendRuleId());
        values.put("spendRuleVersion", request.getSpendRuleVersion());
        values.put("targetAccountId", targetAccountDigest(request));
        values.put("tenantId", request.getTenantId());
        values.put("transactionSn", request.getTransactionSn());
        return values;
    }

    private Map<String, Object> transactionDigestFacts(RecordSpendControlMovementRequest request) {
        Map<String, Object> values = new TreeMap<>();
        values.put("amount", request.getAmount());
        values.put("authorizationTransactionSn", request.getTransactionSn());
        values.put("businessScene", request.getBusinessScene());
        values.put("businessSn", request.getBusinessSn());
        values.put("currency", request.getCurrency().name());
        values.put("movementSn", request.getMovementSn());
        values.put("originalMovementSn", request.getOriginalMovementSn());
        values.put("tenantId", request.getTenantId());
        return values;
    }

    private Map<String, Object> refundCompensationDigestFacts(RecordSpendControlMovementRequest request) {
        Map<String, Object> values = new TreeMap<>();
        values.put("amount", request.getAmount());
        values.put("auditReferenceSn", request.getAuditReferenceSn());
        values.put("businessScene", request.getBusinessScene());
        values.put("businessSn", request.getBusinessSn());
        values.put("controlScopeId", request.getControlScopeId());
        values.put("currency", request.getCurrency().name());
        values.put("instrumentSn", request.getInstrumentSn());
        values.put("periodId", request.getPeriodId());
        values.put("reasonCode", request.getReasonCode());
        values.put("spendRuleId", request.getSpendRuleId());
        values.put("spendRuleVersion", request.getSpendRuleVersion());
        values.put("targetAccountId", targetAccountDigest(request));
        values.put("tenantId", request.getTenantId());
        return values;
    }

    private String targetAccountDigest(RecordSpendControlMovementRequest request) {
        return request.getTargetAccountId().type() + ":" + request.getTargetAccountId().id();
    }

    private Integer lockTargetAccountVersion(RecordSpendControlMovementRequest request,
                                             FundsSubjectType targetSubjectType) {
        // ponytail: 账户行级串行化是有意的粗粒度控制；
        // 仅在确认存在锁竞争后再按控制范围拆分。
        return switch (targetSubjectType) {
            case FUNDING_ACCOUNT -> fundingAccountMapper.selectVersionBySnForUpdate(
                    request.getTenantId(), request.getTargetAccountId().id());
            case CREDIT_ACCOUNT -> creditAccountMapper.selectVersionBySnForUpdate(
                    request.getTenantId(), request.getTargetAccountId().id());
            default -> null;
        };
    }

    private int incrementTargetAccountVersion(RecordSpendControlMovementRequest request,
                                              FundsSubjectType targetSubjectType,
                                              Integer expectedVersion) {
        return switch (targetSubjectType) {
            case FUNDING_ACCOUNT -> fundingAccountMapper.incrementVersionIfMatch(
                    request.getTenantId(), request.getTargetAccountId().id(), expectedVersion);
            case CREDIT_ACCOUNT -> creditAccountMapper.incrementVersionIfMatch(
                    request.getTenantId(), request.getTargetAccountId().id(), expectedVersion);
            default -> 0;
        };
    }

    private void assertMovementAmountAllowed(RecordSpendControlMovementRequest request) {
        BudgetControlProjectionQuery query = new BudgetControlProjectionQuery()
                .setTenantId(request.getTenantId())
                .setControlScopeId(controlScopeId(request))
                .setPeriodId(request.getPeriodId())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setTargetAccountId(request.getTargetAccountId());
        List<SpendControlMovementDTO> movements = spendControlMovementMapper
                .selectBudgetProjectionMovementsWithSharedLock(
                        request.getTenantId(), request.getCurrency(), request.getSpendRuleId(),
                        request.getSpendRuleVersion(), controlScopeId(request), request.getPeriodId(),
                        request.getTargetAccountId().id(), targetSubjectType(request.getTargetAccountId()),
                        CONTROL_MOVEMENT_QUERY_PAGE_SIZE + 1)
                .stream()
                .map(this::toDTO)
                .toList();
        AssertUtils.isTrue(movements.size() <= CONTROL_MOVEMENT_QUERY_PAGE_SIZE,
                "控制额度变动流水查询超过单次读取上限，tenantId = {}, total = {}",
                request.getTenantId(), movements.size());
        BudgetControlProjectionDTO projection = toProjection(query, movements);
        switch (request.getMovementType()) {
            case LIMIT_INCREASED, LIMIT_DECREASED -> assertLimitAmountAllowed(request, projection);
            case RESERVED -> assertReservationAmountAllowed(request, projection, movements);
            case CONSUMED -> assertConsumptionAmountAllowed(request, projection);
            case REFUND_COMPENSATED -> assertRefundCompensationAmountAllowed(request, projection);
            case RELEASED -> assertReleaseAmountAllowed(request, projection);
        }
    }

    private void assertLimitAmountAllowed(RecordSpendControlMovementRequest request,
                                          BudgetControlProjectionDTO projection) {
        long committedControlAmount = projection.getConsumedAmount() + projection.getRemainingControlAmount();
        AssertUtils.isTrue(projection.getLimitAmount() >= committedControlAmount,
                "预算控制额度不能低于已使用或已占用控制金额，movementSn = {}, "
                        + "limitAmount = {}, committedControlAmount = {}",
                request.getMovementSn(),
                projection.getLimitAmount(),
                committedControlAmount);
    }

    private void assertReservationAmountAllowed(RecordSpendControlMovementRequest request,
                                                BudgetControlProjectionDTO projection,
                                                List<SpendControlMovementDTO> movements) {
        boolean configuredLimit = movements.stream()
                .anyMatch(movement -> movement.getMovementType().isLimitAdjustmentMovement());
        if (!configuredLimit) {
            return;
        }
        AssertUtils.isTrue(projection.getAvailableControlAmount() >= 0L,
                "控制占用金额超过可用控制额度，movementSn = {}, availableControlAmount = {}, amount = {}",
                request.getMovementSn(),
                projection.getAvailableControlAmount(),
                request.getAmount());
    }

    private void assertConsumptionAmountAllowed(RecordSpendControlMovementRequest request,
                                                BudgetControlProjectionDTO projection) {
        AssertUtils.isTrue(projection.getRemainingControlAmount() >= 0L,
                "控制消费金额超过剩余占用金额，movementSn = {}, remainingControlAmount = {}, amount = {}",
                request.getMovementSn(),
                projection.getRemainingControlAmount(),
                request.getAmount());
    }

    private void assertReleaseAmountAllowed(RecordSpendControlMovementRequest request,
                                            BudgetControlProjectionDTO projection) {
        AssertUtils.isTrue(projection.getRemainingControlAmount() >= 0L,
                "控制释放金额超过可释放占用金额，movementSn = {}, remainingControlAmount = {}, amount = {}",
                request.getMovementSn(),
                projection.getRemainingControlAmount(),
                request.getAmount());
    }

    private void assertRefundCompensationAmountAllowed(RecordSpendControlMovementRequest request,
                                                       BudgetControlProjectionDTO projection) {
        AssertUtils.isTrue(projection.getConsumedAmount() >= 0L,
                "退款控制补偿金额超过当前周期净消费控制金额，movementSn = {}, consumedAmount = {}, amount = {}",
                request.getMovementSn(),
                projection.getConsumedAmount(),
                request.getAmount());
        AssertUtils.isTrue(projection.getAvailableControlAmount() <= projection.getLimitAmount(),
                "退款控制补偿后可用控制额度不能超过周期控制额度，movementSn = {}, limitAmount = {}, "
                        + "availableAfterCompensation = {}",
                request.getMovementSn(),
                projection.getLimitAmount(),
                projection.getAvailableControlAmount());
    }

    private void validateQuery(SpendControlMovementQuery query) {
        AssertUtils.isTrue(hasNarrowCondition(query), "控制额度变动流水查询必须至少提供一个过滤条件");
        if (query.getTargetAccountId() != null) {
            targetSubjectType(query.getTargetAccountId());
        }
    }

    private void validateProjectionQuery(BudgetControlProjectionQuery query) {
        AssertUtils.hasText(controlScopeId(query), "支出控制范围标识不能为空");
        AssertUtils.hasText(query.getPeriodId(), "预算控制周期标识不能为空");
        AssertUtils.notNull(query.getCurrency(), "币种不能为空");
        if (query.getTargetAccountId() != null) {
            targetSubjectType(query.getTargetAccountId());
        }
    }

    private List<SpendControlMovementDTO> queryBudgetProjectionMovements(BudgetControlProjectionQuery query) {
        return queryMovementsByPage(new SpendControlMovementQuery()
                .setTenantId(query.getTenantId())
                .setControlScopeId(controlScopeId(query))
                .setPeriodId(query.getPeriodId())
                .setCurrency(query.getCurrency())
                .setSpendRuleId(query.getSpendRuleId())
                .setSpendRuleVersion(query.getSpendRuleVersion())
                .setTargetAccountId(query.getTargetAccountId()));
    }

    private List<SpendControlMovementDTO> queryMovementsByPage(SpendControlMovementQuery query) {
        WindPagination<SpendControlMovementDTO> page = querySpendControlMovements(
                query,
                DefaultPageQueryOptions.defaults(CONTROL_MOVEMENT_QUERY_PAGE_SIZE));
        AssertUtils.isTrue(page.getTotal() <= CONTROL_MOVEMENT_QUERY_PAGE_SIZE,
                "控制额度变动流水查询超过单次读取上限，tenantId = {}, total = {}",
                query.getTenantId(),
                page.getTotal());
        return page.getRecords();
    }

    private BudgetControlProjectionDTO toProjection(BudgetControlProjectionQuery query,
                                                    List<SpendControlMovementDTO> movements) {
        List<SpendControlMovementDTO> budgetMovements = budgetProjectionMovements(movements);
        long limitIncreasedAmount = sumByType(budgetMovements, SpendControlMovementType.LIMIT_INCREASED);
        long limitDecreasedAmount = sumByType(budgetMovements, SpendControlMovementType.LIMIT_DECREASED);
        long limitAmount = limitIncreasedAmount - limitDecreasedAmount;
        long reservedAmount = sumByType(budgetMovements, SpendControlMovementType.RESERVED);
        long consumedAmount = consumedAmount(budgetMovements);
        long releasedAmount = releasedAmount(budgetMovements);
        long remainingControlAmount = reservedAmount - grossConsumedAmount(budgetMovements) - releasedAmount;
        long availableControlAmount = limitAmount - consumedAmount - remainingControlAmount;
        SpendControlMovementDTO lastActivity = budgetMovements.isEmpty() ? null : budgetMovements.getLast();
        String controlScopeId = controlScopeId(query);
        return new BudgetControlProjectionDTO()
                .setTenantId(query.getTenantId())
                .setControlScopeId(controlScopeId)
                .setPeriodId(query.getPeriodId())
                .setCurrency(query.getCurrency())
                .setSpendRuleId(query.getSpendRuleId())
                .setSpendRuleVersion(query.getSpendRuleVersion())
                .setTargetAccountId(query.getTargetAccountId())
                .setLimitIncreasedAmount(limitIncreasedAmount)
                .setLimitDecreasedAmount(limitDecreasedAmount)
                .setLimitAmount(limitAmount)
                .setReservedAmount(reservedAmount)
                .setConsumedAmount(consumedAmount)
                .setReleasedAmount(releasedAmount)
                .setRemainingControlAmount(remainingControlAmount)
                .setAvailableControlAmount(availableControlAmount)
                .setLastMovementSn(lastActivity == null ? null : lastActivity.getMovementSn())
                .setLastMovementAt(lastActivity == null ? null : lastActivity.getGmtCreate());
    }

    private boolean hasNarrowCondition(SpendControlMovementQuery query) {
        return StringUtils.hasText(query.getMovementSn())
                || query.getMovementType() != null
                || StringUtils.hasText(query.getBusinessScene())
                || StringUtils.hasText(query.getBusinessSn())
                || StringUtils.hasText(query.getOriginalMovementSn())
                || StringUtils.hasText(query.getTransactionSn())
                || StringUtils.hasText(query.getInstrumentSn())
                || query.getTargetAccountId() != null
                || query.getCurrency() != null
                || StringUtils.hasText(query.getSpendRuleId())
                || StringUtils.hasText(query.getSpendRuleVersion())
                || StringUtils.hasText(query.getControlScopeId())
                || StringUtils.hasText(query.getPeriodId())
                || query.getGmtCreateMin() != null
                || query.getGmtCreateMax() != null;
    }

    private List<SpendControlMovementDTO> budgetProjectionMovements(List<SpendControlMovementDTO> movements) {
        return movements.stream()
                .filter(activity -> activity.getMovementType().isBudgetProjectionMovement())
                .toList();
    }

    private long consumedAmount(List<SpendControlMovementDTO> movements) {
        long grossConsumedAmount = grossConsumedAmount(movements);
        long refundCompensatedAmount = sumByType(movements, SpendControlMovementType.REFUND_COMPENSATED);
        return grossConsumedAmount - refundCompensatedAmount;
    }

    private long grossConsumedAmount(List<SpendControlMovementDTO> movements) {
        return sumByType(movements, SpendControlMovementType.CONSUMED);
    }

    private long releasedAmount(List<SpendControlMovementDTO> movements) {
        return movements.stream()
                .filter(movement -> movement.getMovementType().isReleaseMovement())
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
    }

    private long sumByType(List<SpendControlMovementDTO> movements, SpendControlMovementType movementType) {
        return movements.stream()
                .filter(movement -> movement.getMovementType() == movementType)
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
    }

    private void assertNoSensitiveContextVariables(String contextVariables) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(contextVariables);
    }

    private SpendControlMovement findMovementEntity(Long tenantId, String movementSn) {
        SpendControlMovementNameRefs ref = SpendControlMovementNameRefs.spendControlMovement;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.movementSn.eq(movementSn));
        return spendControlMovementMapper.selectOneByQuery(wrapper);
    }

    private QueryWrapper toQueryWrapper(SpendControlMovementQuery query,
                                        WindQuery<? extends QueryOrderField> options) {
        SpendControlMovementNameRefs ref = SpendControlMovementNameRefs.spendControlMovement;
        String controlScopeId = controlScopeId(query);
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.movementSn.eq(query.getMovementSn()))
                .and(ref.movementType.eq(query.getMovementType()))
                .and(ref.businessScene.eq(query.getBusinessScene()))
                .and(ref.businessSn.eq(query.getBusinessSn()))
                .and(ref.originalMovementSn.eq(query.getOriginalMovementSn()))
                .and(ref.transactionSn.eq(query.getTransactionSn()))
                .and(ref.instrumentSn.eq(query.getInstrumentSn()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.spendRuleId.eq(query.getSpendRuleId()))
                .and(ref.spendRuleVersion.eq(query.getSpendRuleVersion()))
                .and(ref.controlScopeId.eq(controlScopeId))
                .and(ref.periodId.eq(query.getPeriodId()))
                .and(ref.gmtCreate.ge(query.getGmtCreateMin()))
                .and(ref.gmtCreate.le(query.getGmtCreateMax()));
        if (query.getTargetAccountId() != null) {
            wrapper.and(ref.targetSubjectId.eq(query.getTargetAccountId().id()))
                    .and(ref.targetSubjectType.eq(targetSubjectType(query.getTargetAccountId())));
        }
        wrapper.orderBy(ref.id.asc());
        return wrapper;
    }

    private SpendControlMovementDTO toDTO(SpendControlMovement entity) {
        return SpendControlMovementConverter.INSTANCE.convertToSpendControlMovementDTO(entity);
    }

    private String controlScopeId(RecordSpendControlMovementRequest request) {
        return request.getControlScopeId();
    }

    private String controlScopeId(BudgetControlProjectionQuery query) {
        return query.getControlScopeId();
    }

    private String controlScopeId(SpendControlMovementQuery query) {
        return query.getControlScopeId();
    }

    private FundsSubjectType targetSubjectType(FundsAccountId accountId) {
        boolean supported = FundsSubjectType.FUNDING_ACCOUNT.name().equals(accountId.type())
                || FundsSubjectType.CREDIT_ACCOUNT.name().equals(accountId.type());
        AssertUtils.isTrue(supported, "控制额度变动目标只能是资金账户或信用账户，targetAccountId = {}", accountId);
        return FundsSubjectType.valueOf(accountId.type());
    }

    private record DigestMigration(String domain, Map<String, Object> facts) {
    }
}

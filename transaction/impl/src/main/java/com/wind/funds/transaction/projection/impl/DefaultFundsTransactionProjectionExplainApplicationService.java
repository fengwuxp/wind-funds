package com.wind.funds.transaction.projection.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainQuery;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanationSource;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanBatch;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanCursor;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanQuery;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.transaction.core.Money;
import com.wind.jackson.WindJson;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 默认交易投影解释查询应用服务。
 *
 * <p>该实现只读取资金交易查询服务暴露的稳定事实，不直接访问 Mapper，不反写交易、账本或余额事实。</p>
 */
@Service
@AllArgsConstructor
public class DefaultFundsTransactionProjectionExplainApplicationService
        implements FundsTransactionProjectionExplainApplicationService {

    private static final int MAX_SCAN_BATCH_SIZE = 500;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final FundsTransactionMapper fundsTransactionMapper;

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @Override
    public @NonNull FundsTransactionProjectionExplanation explain(
            @NonNull FundsTransactionProjectionExplainQuery query) {
        AssertUtils.notNull(query, "交易投影解释查询条件不能为空");
        AssertUtils.hasText(query.fundsTransactionSn(), "交易投影解释资金交易流水不能为空");
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(query.fundsTransactionSn())
                .orElseThrow(() -> new IllegalArgumentException("资金交易不存在，transactionSn = "
                        + query.fundsTransactionSn()));
        RouteSnapshotSpec routeSnapshot = fundsTransactionQueryService
                .findRouteSnapshotByTransactionSn(query.fundsTransactionSn())
                .orElseThrow(() -> new IllegalArgumentException("资金交易缺少 RouteSnapshot，transactionSn = "
                        + query.fundsTransactionSn()));
        assertRouteSnapshotMatchesTransaction(transaction, routeSnapshot);
        List<FundsTransactionDetailDTO> details = fundsTransactionQueryService
                .queryFundsTransactionDetails(query.fundsTransactionSn());
        AssertUtils.notEmpty(details, "交易投影解释缺少资金交易明细，transactionSn = {}",
                query.fundsTransactionSn());
        FundsTransactionDetailDTO primaryDetail = resolvePrimaryDetail(details);
        return FundsTransactionProjectionExplanationSource.builder()
                .businessScene(primaryDetail.getBusinessScene())
                .businessSn(primaryDetail.getBusinessSn())
                .fundsTransactionSn(transaction.getSn())
                .routeSnapshot(routeSnapshot)
                .ownerType(primaryDetail.getSubjectType())
                .ownerId(primaryDetail.getSubjectId())
                .occurredTime(primaryDetail.getGmtCreate())
                .ledgerTransactionSn(primaryDetail.getLedgerTransactionSn())
                .completed(isCompleted(primaryDetail.getState()))
                .failed(primaryDetail.getState() == FundsTransactionDetailState.FAILED)
                .eventType(primaryDetail.getEventType())
                .amount(Money.immutable(primaryDetail.getAmount(), primaryDetail.getCurrency()))
                .contextVariables(parseContextVariables(primaryDetail.getContextVariables()))
                .failureReasonOverride(resolveFailureReason(primaryDetail))
                .build()
                .explanation(transaction);
    }

    @Override
    public @NonNull FundsTransactionProjectionScanCursor initializeScanCursor(
            @NonNull FundsTransactionProjectionScanQuery query) {
        validateScanQuery(query, false);
        long transactionUpperBound = fundsTransactionMapper.selectProjectionUpperBound(query.tenantId(),
                query.sourceSn(), query.ownerType(), query.ownerId(), query.startTime(), query.endTime());
        long frozenOrderUpperBound = fundsFrozenOrderMapper.selectProjectionUpperBound(query.tenantId(),
                query.sourceSn(), query.ownerType(), query.ownerId(), query.startTime(), query.endTime());
        return FundsTransactionProjectionScanCursor.initial(transactionUpperBound, frozenOrderUpperBound);
    }

    @Override
    public @NonNull FundsTransactionProjectionScanBatch scan(
            @NonNull FundsTransactionProjectionScanQuery query) {
        validateScanQuery(query, true);
        FundsTransactionProjectionScanCursor cursor = query.cursor();
        List<FundsTransactionProjectionExplanation> facts = new ArrayList<>();
        int remaining = query.maxBatchSize();
        long lastTransactionId = cursor.lastTransactionId();
        if (lastTransactionId < cursor.transactionUpperBoundId()) {
            List<FundsTransaction> transactions = fundsTransactionMapper.scanProjectionFacts(query.tenantId(),
                    query.sourceSn(), query.ownerType(), query.ownerId(), query.startTime(), query.endTime(),
                    lastTransactionId, cursor.transactionUpperBoundId(), remaining);
            lastTransactionId = transactions.isEmpty()
                    ? cursor.transactionUpperBoundId() : transactions.getLast().getId();
            facts.addAll(transactions.stream()
                    .map(transaction -> explain(FundsTransactionProjectionExplainQuery.builder()
                            .fundsTransactionSn(transaction.getSn())
                            .build()))
                    .filter(explanation -> matchesEvent(query, explanation.eventType()))
                    .toList());
            remaining -= transactions.size();
        }

        long lastFrozenOrderId = cursor.lastFrozenOrderId();
        if (remaining > 0 && lastTransactionId >= cursor.transactionUpperBoundId()
                && lastFrozenOrderId < cursor.frozenOrderUpperBoundId()) {
            List<FundsFrozenOrder> frozenOrders = fundsFrozenOrderMapper.scanProjectionFacts(query.tenantId(),
                    query.sourceSn(), query.ownerType(), query.ownerId(), query.startTime(), query.endTime(),
                    lastFrozenOrderId, cursor.frozenOrderUpperBoundId(), remaining);
            lastFrozenOrderId = frozenOrders.isEmpty()
                    ? cursor.frozenOrderUpperBoundId() : frozenOrders.getLast().getId();
            facts.addAll(frozenOrders.stream()
                    .map(this::explainFrozenOrder)
                    .filter(explanation -> matchesEvent(query, explanation.eventType()))
                    .toList());
        }

        FundsTransactionProjectionScanCursor nextCursor = new FundsTransactionProjectionScanCursor(
                lastTransactionId, cursor.transactionUpperBoundId(), lastFrozenOrderId,
                cursor.frozenOrderUpperBoundId());
        boolean hasMore = nextCursor.lastTransactionId() < nextCursor.transactionUpperBoundId()
                || nextCursor.lastFrozenOrderId() < nextCursor.frozenOrderUpperBoundId();
        return FundsTransactionProjectionScanBatch.builder()
                .facts(facts)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private FundsTransactionProjectionExplanation explainFrozenOrder(FundsFrozenOrder order) {
        Map<String, Object> context = parseContextVariables(order.getContextVariables());
        FundsTransactionEventType eventType = resolveFrozenOrderEventType(context);
        RouteSnapshotSpec routeSnapshot = fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(order.getSn())
                .orElseThrow(() -> new IllegalArgumentException("资金冻结事实缺少 RouteSnapshot，freezeOrderSn = "
                        + order.getSn()));
        return FundsTransactionProjectionExplanationSource.builder()
                .businessScene(order.getBusinessScene())
                .businessSn(order.getBusinessSn())
                .fundsTransactionSn(order.getSn())
                .routeSnapshot(routeSnapshot)
                .ownerType(order.getSubjectType().name())
                .ownerId(order.getSubjectId())
                .occurredTime(order.getGmtCreate())
                .ledgerTransactionSn(order.getFreezeLedgerTransactionSn())
                .completed(isFrozenOrderCompleted(order, eventType))
                .failed(false)
                .eventType(eventType)
                .amount(Money.immutable(order.getAmount(), order.getCurrency()))
                .contextVariables(context)
                .build()
                .explanation();
    }

    private FundsTransactionEventType resolveFrozenOrderEventType(Map<String, Object> context) {
        Object value = context.get(FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE);
        return value == null ? FundsTransactionEventType.FREEZE
                : FundsTransactionEventType.valueOf(value.toString());
    }

    private boolean isFrozenOrderCompleted(FundsFrozenOrder order, FundsTransactionEventType eventType) {
        if (eventType == FundsTransactionEventType.UNFREEZE) {
            return order.getState() == FundsFrozenOrderState.RELEASED
                    || order.getState() == FundsFrozenOrderState.CLOSED;
        }
        return order.getState() == FundsFrozenOrderState.FROZEN
                || order.getState() == FundsFrozenOrderState.PARTIALLY_RELEASED
                || order.getState() == FundsFrozenOrderState.RELEASED
                || order.getState() == FundsFrozenOrderState.CLOSED;
    }

    private boolean matchesEvent(FundsTransactionProjectionScanQuery query,
                                 FundsTransactionEventType eventType) {
        return query.eventTypes().isEmpty() || query.eventTypes().contains(eventType);
    }

    private void validateScanQuery(FundsTransactionProjectionScanQuery query, boolean requireCursor) {
        AssertUtils.notNull(query, "交易投影扫描条件不能为空");
        AssertUtils.notNull(query.tenantId(), "交易投影扫描租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), query.tenantId(),
                "交易投影扫描 tenantId 与当前租户不一致");
        AssertUtils.isTrue(query.isBounded(), "交易投影扫描必须指定单笔、主体或时间窗口范围");
        AssertUtils.isTrue(query.maxBatchSize() > 0 && query.maxBatchSize() <= MAX_SCAN_BATCH_SIZE,
                "交易投影扫描批次大小必须在 1 到 {} 之间", MAX_SCAN_BATCH_SIZE);
        if (requireCursor) {
            AssertUtils.notNull(query.cursor(), "交易投影扫描 cursor 不能为空");
        }
    }

    private void assertRouteSnapshotMatchesTransaction(FundsTransactionDTO transaction,
                                                       RouteSnapshotSpec routeSnapshot) {
        AssertUtils.equals(transaction.getBusinessScene(), routeSnapshot.getBusinessScene(),
                "交易投影解释 RouteSnapshot 业务场景不一致，transactionSn = {}", transaction.getSn());
        AssertUtils.equals(transaction.getBusinessSn(), routeSnapshot.getBusinessSn(),
                "交易投影解释 RouteSnapshot 业务流水不一致，transactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(transaction.getTransactionType() == routeSnapshot.getTransactionType(),
                "交易投影解释 RouteSnapshot 交易类型不一致，transactionSn = {}", transaction.getSn());
    }

    private FundsTransactionDetailDTO resolvePrimaryDetail(List<FundsTransactionDetailDTO> details) {
        return details.stream()
                .filter(detail -> detail.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER)
                .max((left, right) -> Long.compare(nullableId(left), nullableId(right)))
                .orElse(details.getFirst());
    }

    private long nullableId(FundsTransactionDetailDTO detail) {
        Long id = detail.getId();
        return id == null ? 0L : id;
    }

    private boolean isCompleted(FundsTransactionDetailState state) {
        return state == FundsTransactionDetailState.SUCCEEDED
                || state == FundsTransactionDetailState.REJECTED
                || state == FundsTransactionDetailState.FAILED;
    }

    private @NonNull Map<String, Object> parseContextVariables(@Nullable String contextVariables) {
        if (!StringUtils.hasText(contextVariables)) {
            return Map.of();
        }
        Map<String, Object> values = WindJson.parseObject(contextVariables, new TypeReference<>() {
        });
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private @Nullable String resolveFailureReason(FundsTransactionDetailDTO primaryDetail) {
        if (primaryDetail.getState() != FundsTransactionDetailState.FAILED) {
            return null;
        }
        if (StringUtils.hasText(primaryDetail.getErrorMessage())) {
            return primaryDetail.getErrorMessage();
        }
        return primaryDetail.getErrorCode();
    }
}

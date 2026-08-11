package com.wind.funds.transaction.services.impl;

import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.wind.funds.transaction.dal.entities.table.FundsTransactionDetailNameRefs;
import com.wind.funds.transaction.dal.entities.table.FundsTransactionNameRefs;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.mapstruct.FundsTransactionConverter;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 默认资金交易事实查询服务。
 *
 * @author Codex
 * @date 2026-05-12
 */
@Service
@AllArgsConstructor
public class DefaultFundsTransactionQueryService implements FundsTransactionQueryService {

    private final FundsTransactionMapper fundsTransactionMapper;

    private final FundsTransactionDetailMapper fundsTransactionDetailMapper;

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @Override
    public @NonNull Optional<FundsTransactionDTO> queryFundsTransaction(@NonNull String transactionSn) {
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        return Optional.ofNullable(findTransactionBySnNullable(transactionSn))
                .map(FundsTransactionConverter.INSTANCE::convertToFundsTransactionDTO);
    }

    @Override
    public @NonNull Optional<FundsTransactionDTO> findFundsTransactionByBusiness(@NonNull Long tenantId,
                                                                                 @NonNull String businessScene,
                                                                                 @NonNull String businessSn) {
        AssertUtils.notNull(tenantId, "资金交易租户 ID 不能为空");
        AssertUtils.hasText(businessScene, "资金交易业务场景不能为空");
        AssertUtils.hasText(businessSn, "资金交易业务流水不能为空");
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.businessScene.eq(businessScene))
                .and(ref.businessSn.eq(businessSn));
        return Optional.ofNullable(fundsTransactionMapper.selectOneByQuery(wrapper))
                .map(FundsTransactionConverter.INSTANCE::convertToFundsTransactionDTO);
    }

    @Override
    public @NonNull Optional<FundsTransactionDTO> findFundsTransactionByExternalFundsFact(
            @NonNull Long tenantId,
            @NonNull String externalSourceCode,
            @NonNull String externalFundsFactSn,
            @NonNull FundsEffectType effectType) {
        AssertUtils.notNull(tenantId, "资金交易租户 ID 不能为空");
        AssertUtils.hasText(externalSourceCode, "外部资金事实来源编码不能为空");
        AssertUtils.hasText(externalFundsFactSn, "外部资金事实流水不能为空");
        AssertUtils.notNull(effectType, "外部资金事实效果不能为空");
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.externalSourceCode.eq(externalSourceCode))
                .and(ref.externalFundsFactSn.eq(externalFundsFactSn))
                .and(ref.externalFundsEffectType.eq(effectType));
        return Optional.ofNullable(fundsTransactionMapper.selectOneByQuery(wrapper))
                .map(FundsTransactionConverter.INSTANCE::convertToFundsTransactionDTO);
    }

    @Override
    public @NonNull List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull String transactionSn) {
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.transactionSn.eq(transactionSn))
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper)
                .stream()
                .map(FundsTransactionConverter.INSTANCE::convertToFundsTransactionDetailDTO)
                .toList();
    }

    @Override
    public boolean hasConsumedReplayLeg(@NonNull String referenceTransactionSn,
                                        @NonNull FundsTransactionEventType eventType,
                                        @NonNull String replayRefLegId) {
        AssertUtils.hasText(referenceTransactionSn, "原资金交易流水号不能为空");
        AssertUtils.notNull(eventType, "资金交易事件类型不能为空");
        AssertUtils.hasText(replayRefLegId, "RouteReplay 原 legId 不能为空");
        return queryConsumedReplayDetails(referenceTransactionSn, eventType)
                .stream()
                .anyMatch(detail -> isReplayLegConsumed(detail, replayRefLegId));
    }

    @Override
    public @NonNull Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                                     @NonNull FundsTransactionEventType eventType,
                                                     @NonNull String replayRefLegId,
                                                     @NonNull CurrencyIsoCode currency) {
        return sumConsumedReplayLegAmount(referenceTransactionSn, eventType, replayRefLegId, currency, null, null);
    }

    @Override
    public @NonNull Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                                     @NonNull FundsTransactionEventType eventType,
                                                     @NonNull String replayRefLegId,
                                                     @NonNull CurrencyIsoCode currency,
                                                     @Nullable String excludedBusinessScene,
                                                     @Nullable String excludedBusinessSn) {
        AssertUtils.hasText(referenceTransactionSn, "原资金交易流水号不能为空");
        AssertUtils.notNull(eventType, "资金交易事件类型不能为空");
        AssertUtils.hasText(replayRefLegId, "RouteReplay 原 legId 不能为空");
        AssertUtils.notNull(currency, "RouteReplay 币种不能为空");
        if (isFreezeOrderUnfreezeConsumption(eventType, replayRefLegId)) {
            return sumFrozenOrderReleasedAmount(referenceTransactionSn, currency,
                    excludedBusinessScene, excludedBusinessSn);
        }
        Map<String, Long> consumedAmounts = new LinkedHashMap<>();
        for (FundsTransactionDetail detail : queryConsumedReplayDetails(referenceTransactionSn, eventType)) {
            if (sameBusinessEvent(detail, excludedBusinessScene, excludedBusinessSn)) {
                continue;
            }
            Long amount = consumedReplayLegAmount(detail, replayRefLegId);
            if (amount == null) {
                continue;
            }
            AssertUtils.isTrue(detail.getCurrency() == currency,
                    "RouteReplay 已消费金额币种不一致，referenceSn = {}，eventType = {}，legId = {}",
                    referenceTransactionSn, eventType, replayRefLegId);
            String consumeKey = detail.getBusinessScene() + ":" + detail.getBusinessSn();
            consumedAmounts.merge(consumeKey, amount, Math::max);
        }
        long result = consumedAmounts.values().stream().mapToLong(Long::longValue).sum();
        return Money.immutable(result, currency);
    }

    private boolean sameBusinessEvent(FundsTransactionDetail detail,
                                      @Nullable String businessScene,
                                      @Nullable String businessSn) {
        return StringUtils.hasText(businessScene)
                && StringUtils.hasText(businessSn)
                && businessScene.equals(detail.getBusinessScene())
                && businessSn.equals(detail.getBusinessSn());
    }

    @Override
    public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        FundsTransaction transaction = findTransactionBySnNullable(transactionSn);
        if (transaction == null || !StringUtils.hasText(transaction.getRouteSnapshot())) {
            return Optional.empty();
        }
        return Optional.of(RouteSnapshotJsonSupport.parseRouteSnapshot(
                transaction.getRouteSnapshot(), transaction.getGmtCreate()));
    }

    @Override
    public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
        AssertUtils.hasText(freezeOrderSn, "冻结单号不能为空");
        FundsFrozenOrder order = findFreezeOrderBySnNullable(freezeOrderSn);
        if (order == null) {
            return Optional.empty();
        }
        Optional<RouteSnapshotSpec> frozenOrderSnapshot = findRouteSnapshotInFreezeOrder(order);
        if (frozenOrderSnapshot.isPresent()) {
            return frozenOrderSnapshot;
        }
        return StringUtils.hasText(order.getTransactionSn())
                ? findRouteSnapshotByTransactionSn(order.getTransactionSn())
                : Optional.empty();
    }

    private FundsTransaction findTransactionBySnNullable(String sn) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(sn));
        return fundsTransactionMapper.selectOneByQuery(wrapper);
    }

    private FundsFrozenOrder findFreezeOrderBySnNullable(String sn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(sn));
        return fundsFrozenOrderMapper.selectOneByQuery(wrapper);
    }

    private boolean isReplayLegConsumed(FundsTransactionDetail detail, String replayRefLegId) {
        return consumedReplayLegAmount(detail, replayRefLegId) != null;
    }

    private Long consumedReplayLegAmount(FundsTransactionDetail detail, String replayRefLegId) {
        Long amount = consumedReplayLegAmountFromContext(detail, replayRefLegId);
        return amount == null ? freezeOrderWithdrawConsumedAmount(detail, replayRefLegId) : amount;
    }

    private Long consumedReplayLegAmountFromContext(FundsTransactionDetail detail, String replayRefLegId) {
        if (!StringUtils.hasText(detail.getContextVariables())) {
            return null;
        }
        Map<String, Object> values = WindJson.parseObject(detail.getContextVariables(), new TypeReference<>() {
        });
        Object consumedAmountsValue = values.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS);
        if (consumedAmountsValue instanceof Map<?, ?> replayConsumedAmounts
                && replayConsumedAmounts.get(replayRefLegId) instanceof Number amount) {
            return amount.longValue();
        }
        Object consumedLegIdsValue = values.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_IDS);
        return consumedLegIdsValue instanceof List<?> replayConsumedLegIds && replayConsumedLegIds.contains(replayRefLegId)
                ? detail.getAmount() : null;
    }

    private Long freezeOrderWithdrawConsumedAmount(FundsTransactionDetail detail, String replayRefLegId) {
        if (detail.getEventType() != FundsTransactionEventType.WITHDRAW
                || !FundsRouteLegIds.FREEZE.equals(replayRefLegId)
                || detail.getParticipantRole() == RouteParticipantRole.FEE_RECEIVER
                || !StringUtils.hasText(detail.getReferenceDetailSn())) {
            return null;
        }
        return detail.getAmount();
    }

    private boolean isFreezeOrderUnfreezeConsumption(FundsTransactionEventType eventType, String replayRefLegId) {
        return eventType == FundsTransactionEventType.UNFREEZE
                && FundsRouteLegIds.FREEZE.equals(replayRefLegId);
    }

    private Money sumFrozenOrderReleasedAmount(String freezeOrderSn,
                                               CurrencyIsoCode currency,
                                               @Nullable String excludedBusinessScene,
                                               @Nullable String excludedBusinessSn) {
        FundsFrozenOrder order = findFreezeOrderBySnNullable(freezeOrderSn);
        if (order == null) {
            return Money.immutable(0L, currency);
        }
        AssertUtils.isTrue(order.getCurrency() == currency,
                "RouteReplay 已释放金额币种不一致，referenceSn = {}，eventType = {}，legId = {}",
                freezeOrderSn, FundsTransactionEventType.UNFREEZE, FundsRouteLegIds.FREEZE);
        long releasedAmount = defaultAmount(order.getReleasedAmount());
        long excludedAmount = sumExcludedFrozenOrderReleaseAmount(order, excludedBusinessScene, excludedBusinessSn);
        AssertUtils.isTrue(releasedAmount >= excludedAmount,
                "冻结单释放金额事实不一致，referenceSn = {}，releasedAmount = {}，excludedAmount = {}",
                freezeOrderSn, releasedAmount, excludedAmount);
        return Money.immutable(releasedAmount - excludedAmount, currency);
    }

    private long sumExcludedFrozenOrderReleaseAmount(FundsFrozenOrder originalOrder,
                                                    @Nullable String excludedBusinessScene,
                                                    @Nullable String excludedBusinessSn) {
        if (!StringUtils.hasText(excludedBusinessScene) || !StringUtils.hasText(excludedBusinessSn)) {
            return 0L;
        }
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(originalOrder.getTenantId()))
                .and(ref.businessScene.eq(excludedBusinessScene))
                .and(ref.businessSn.eq(excludedBusinessSn))
                .and(ref.state.eq(FundsFrozenOrderState.RELEASED))
                .and(ref.currency.eq(originalOrder.getCurrency()));
        return fundsFrozenOrderMapper.selectListByQuery(wrapper)
                .stream()
                .filter(order -> isReleaseRecordFor(order, originalOrder.getSn()))
                .mapToLong(FundsFrozenOrder::getAmount)
                .sum();
    }

    private boolean isReleaseRecordFor(FundsFrozenOrder order, String freezeOrderSn) {
        if (!StringUtils.hasText(order.getContextVariables())) {
            return false;
        }
        Map<String, Object> values = parseContextVariables(order.getContextVariables());
        return Objects.equals(values.get(FundsInstructionContextKeys.REFERENCE_FREEZE_SN), freezeOrderSn)
                && FundsTransactionEventType.UNFREEZE.name()
                .equals(values.get(FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE));
    }

    private long defaultAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private List<FundsTransactionDetail> queryConsumedReplayDetails(String referenceTransactionSn,
                                                                    FundsTransactionEventType eventType) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.referenceDetailSn.eq(referenceTransactionSn))
                .and(ref.eventType.eq(eventType))
                .and(ref.state.eq(FundsTransactionDetailState.SUCCEEDED))
                .and(ref.ledgerTransactionSn.isNotNull())
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper);
    }

    private Optional<RouteSnapshotSpec> findRouteSnapshotInFreezeOrder(FundsFrozenOrder order) {
        if (!StringUtils.hasText(order.getContextVariables())) {
            return Optional.empty();
        }
        Map<String, Object> values = parseContextVariables(order.getContextVariables());
        Object routeSnapshotValue = values.get(FundsInstructionContextKeys.ROUTE_SNAPSHOT);
        String routeSnapshot = routeSnapshotValue instanceof String value ? value : null;
        if (!StringUtils.hasText(routeSnapshot)) {
            return Optional.empty();
        }
        return Optional.of(RouteSnapshotJsonSupport.parseRouteSnapshot(routeSnapshot, order.getGmtCreate()));
    }

    private Map<String, Object> parseContextVariables(String contextVariables) {
        return WindJson.parseObject(contextVariables, new TypeReference<>() {
        });
    }
}

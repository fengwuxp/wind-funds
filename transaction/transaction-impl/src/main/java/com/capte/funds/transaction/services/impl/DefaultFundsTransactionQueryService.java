package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.capte.funds.transaction.dal.entities.table.FundsTransactionDetailNameRefs;
import com.capte.funds.transaction.dal.entities.table.FundsTransactionNameRefs;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.mapstruct.FundsTransactionConverter;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
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
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.referenceDetailSn.eq(referenceTransactionSn))
                .and(ref.eventType.eq(eventType))
                .and(ref.status.eq(FundsTransactionDetailStatus.SUCCEEDED))
                .and(ref.ledgerTransactionSn.isNotNull())
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper)
                .stream()
                .anyMatch(detail -> isReplayLegConsumed(detail, replayRefLegId));
    }

    @Override
    public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        FundsTransaction transaction = findTransactionBySnNullable(transactionSn);
        if (transaction == null || !hasText(transaction.getRouteSnapshot())) {
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
        return hasText(order.getTransactionSn())
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
        if (!hasText(detail.getContextVariables())) {
            return false;
        }
        JSONObject values = JSON.parseObject(detail.getContextVariables());
        JSONArray replayConsumedLegIds = values.getJSONArray(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_IDS);
        return replayConsumedLegIds != null && replayConsumedLegIds.contains(replayRefLegId);
    }

    private Optional<RouteSnapshotSpec> findRouteSnapshotInFreezeOrder(FundsFrozenOrder order) {
        if (!hasText(order.getContextVariables())) {
            return Optional.empty();
        }
        JSONObject values = JSON.parseObject(order.getContextVariables());
        String routeSnapshot = values.getString(FundsInstructionContextKeys.ROUTE_SNAPSHOT);
        if (!hasText(routeSnapshot)) {
            return Optional.empty();
        }
        return Optional.of(RouteSnapshotJsonSupport.parseRouteSnapshot(routeSnapshot, order.getGmtCreate()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

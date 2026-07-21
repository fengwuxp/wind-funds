package com.wind.funds.transaction.projection.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainQuery;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanationSource;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
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

    private final FundsTransactionQueryService fundsTransactionQueryService;

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
                .ledgerTransactionSn(primaryDetail.getLedgerTransactionSn())
                .completed(isCompleted(primaryDetail.getStatus()))
                .failed(primaryDetail.getStatus() == FundsTransactionDetailStatus.FAILED)
                .eventType(primaryDetail.getEventType())
                .amount(Money.immutable(primaryDetail.getAmount(), primaryDetail.getCurrency()))
                .contextVariables(parseContextVariables(primaryDetail.getContextVariables()))
                .failureReasonOverride(resolveFailureReason(primaryDetail))
                .build()
                .explanation(transaction);
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

    private boolean isCompleted(FundsTransactionDetailStatus status) {
        return status == FundsTransactionDetailStatus.SUCCEEDED
                || status == FundsTransactionDetailStatus.REJECTED
                || status == FundsTransactionDetailStatus.FAILED;
    }

    private @NonNull Map<String, Object> parseContextVariables(@Nullable String contextVariables) {
        if (!StringUtils.hasText(contextVariables)) {
            return Map.of();
        }
        JSONObject values = JSON.parseObject(contextVariables);
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private @Nullable String resolveFailureReason(FundsTransactionDetailDTO primaryDetail) {
        if (primaryDetail.getStatus() != FundsTransactionDetailStatus.FAILED) {
            return null;
        }
        if (StringUtils.hasText(primaryDetail.getErrorMessage())) {
            return primaryDetail.getErrorMessage();
        }
        return primaryDetail.getErrorCode();
    }
}

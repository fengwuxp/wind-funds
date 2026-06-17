package com.wind.funds.transaction.application.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.application.FundsBenefitFundingApplicationService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.funds.transaction.model.dto.FundsBenefitFundingSourceDTO;
import com.wind.funds.transaction.model.request.FundsBenefitFundingApplyRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingReverseRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccountId;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 权益让利资金交易应用服务实现。
 *
 * <p>该实现只负责编排权益让利资金事实到标准直接交易入口，不直接写 route、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Service
@AllArgsConstructor
public class FundsBenefitFundingApplicationServiceImpl implements FundsBenefitFundingApplicationService {

    private static final String BENEFIT_FUNDING = "benefitFunding";

    private static final String BENEFIT_FUNDING_NATURE_CODE = "benefitFundingNatureCode";

    private static final String BENEFIT_LEDGER_EFFECT_CODE = "benefitLedgerEffectCode";

    private static final String BENEFIT_ORIGINAL_ORDER_SN = "benefitOriginalOrderSn";

    private static final String BENEFIT_REFERENCE_TRANSACTION_SN = "benefitReferenceTransactionSn";

    private static final String BENEFIT_SOURCE_SUMMARY = "benefitFundingSources";

    private static final String BENEFIT_REFUND_REASON = "benefitRefundReason";

    private static final String BENEFIT_REVERSE_REASON = "benefitReverseReason";

    private final FundsDirectTransactionService directTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String apply(@NonNull FundsBenefitFundingApplyRequest request,
                                 @NonNull WindOperator operator) {
        assertApplyRequest(request);
        FundsAccountId costBearer = toAccountId(request.getCostBearerSubjectRef(), "权益让利承担方");
        FundsAccountId receiver = toAccountId(request.getBenefitReceiverSubjectRef(), "权益让利受益方");
        return directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(costBearer)
                .setPayeeId(receiver)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(request.getAmount()))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(applyContext(request)))
                .setDescription("benefit funding apply"), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String refund(@NonNull FundsBenefitFundingRefundRequest request,
                                  @NonNull WindOperator operator) {
        assertRefundRequest(request);
        OriginalBenefitRoute route = originalBenefitRoute(request.getReferenceBenefitTransactionSn());
        return directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(route.costBearerAccountId())
                .setPayerId(route.benefitReceiverAccountId())
                .setPayerLedgerCode(route.benefitReceiverLedgerCode())
                .setAmount(request.getAmount())
                .setReferenceTransactionSn(request.getReferenceBenefitTransactionSn())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(refundContext(request)))
                .setDescription("benefit funding refund"), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String reverse(@NonNull FundsBenefitFundingReverseRequest request,
                                   @NonNull WindOperator operator) {
        assertReverseRequest(request);
        OriginalBenefitRoute route = originalBenefitRoute(request.getReferenceBenefitTransactionSn());
        return directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(route.costBearerAccountId())
                .setPayerId(route.benefitReceiverAccountId())
                .setPayerLedgerCode(route.benefitReceiverLedgerCode())
                .setAmount(request.getAmount())
                .setReferenceTransactionSn(request.getReferenceBenefitTransactionSn())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(reverseContext(request)))
                .setDescription("benefit funding reverse"), operator);
    }

    private void assertApplyRequest(@NonNull FundsBenefitFundingApplyRequest request) {
        AssertUtils.notNull(request.getTenantId(), "权益让利 tenantId 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "权益让利 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getBusinessScene(), "权益让利业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "权益让利业务流水不能为空");
        AssertUtils.hasText(request.getOriginalOrderSn(), "权益让利原始订单号不能为空");
        AssertUtils.notNull(request.getCostBearerSubjectRef(), "权益让利承担方不能为空");
        AssertUtils.notNull(request.getBenefitReceiverSubjectRef(), "权益让利受益方不能为空");
        AssertUtils.notNull(request.getAmount(), "权益让利金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "权益让利金额必须大于 0");
        AssertUtils.notNull(request.getFundingNature(), "权益让利资金性质不能为空");
        AssertUtils.notNull(request.getLedgerEffect(), "权益让利账务效果不能为空");
        AssertUtils.isTrue(request.getLedgerEffect() == FundsBenefitLedgerEffect.POSTING_REQUIRED,
                "权益让利账务效果暂仅支持 POSTING_REQUIRED，ledgerEffect = {}", request.getLedgerEffect());
        AssertUtils.notEmpty(request.getBenefitFundingSources(), "权益让利来源不能为空");
    }

    private void assertRefundRequest(@NonNull FundsBenefitFundingRefundRequest request) {
        AssertUtils.notNull(request.getTenantId(), "权益让利退款 tenantId 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "权益让利退款 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReferenceBenefitTransactionSn(), "原权益让利资金交易流水不能为空");
        AssertUtils.notNull(request.getAmount(), "权益让利退款金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "权益让利退款金额必须大于 0");
        AssertUtils.hasText(request.getBusinessScene(), "权益让利退款业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "权益让利退款业务流水不能为空");
        AssertUtils.hasText(request.getOriginalOrderSn(), "权益让利退款原始订单号不能为空");
    }

    private void assertReverseRequest(@NonNull FundsBenefitFundingReverseRequest request) {
        AssertUtils.notNull(request.getTenantId(), "权益让利撤销 tenantId 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "权益让利撤销 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReferenceBenefitTransactionSn(), "原权益让利资金交易流水不能为空");
        AssertUtils.notNull(request.getAmount(), "权益让利撤销金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "权益让利撤销金额必须大于 0");
        AssertUtils.hasText(request.getBusinessScene(), "权益让利撤销业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "权益让利撤销业务流水不能为空");
        AssertUtils.hasText(request.getOriginalOrderSn(), "权益让利撤销原始订单号不能为空");
    }

    private OriginalBenefitRoute originalBenefitRoute(@NonNull String referenceBenefitTransactionSn) {
        RouteSnapshotSpec snapshot = fundsTransactionQueryService.findRouteSnapshotByTransactionSn(referenceBenefitTransactionSn)
                .orElseThrow(() -> new IllegalArgumentException("原权益让利资金交易 RouteSnapshot 不存在，transactionSn = "
                        + referenceBenefitTransactionSn));
        Optional<RouteLegSpec> sourceLeg = snapshot.getLegs()
                .stream()
                .filter(leg -> leg.getLegType() == RouteLegType.INTERNAL_TRANSFER)
                .findFirst();
        AssertUtils.isTrue(sourceLeg.isPresent(), "原权益让利资金交易缺少可回放的资金路径，transactionSn = {}",
                referenceBenefitTransactionSn);
        RouteLegSpec leg = sourceLeg.get();
        return new OriginalBenefitRoute(
                toAccountId(leg.getSourceNode().getSubjectRef(), "原权益让利承担方"),
                toAccountId(leg.getTargetNode().getSubjectRef(), "原权益让利受益方"),
                leg.getTargetNode().getLedgerSubjectCode());
    }

    private FundsAccountId toAccountId(@NonNull SubjectRef subjectRef, @NonNull String roleName) {
        AssertUtils.hasText(subjectRef.getSubjectId(), "{}主体 ID 不能为空", roleName);
        AssertUtils.notNull(subjectRef.getSubjectType(), "{}主体类型不能为空", roleName);
        AssertUtils.isTrue(subjectRef.getSubjectType() == FundsSubjectType.FUNDING_ACCOUNT
                        || subjectRef.getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "{}必须是资金账户或信用账户，subjectType = {}", roleName, subjectRef.getSubjectType());
        return FundsAccountId.immutable(subjectRef.getSubjectId(), subjectRef.getSubjectType().name());
    }

    private Map<String, Object> applyContext(@NonNull FundsBenefitFundingApplyRequest request) {
        Map<String, Object> result = mergeContext(request.getContextVariables());
        result.put(BENEFIT_FUNDING, Boolean.TRUE);
        result.put(BENEFIT_FUNDING_NATURE_CODE, request.getFundingNature().name());
        result.put(BENEFIT_LEDGER_EFFECT_CODE, request.getLedgerEffect().name());
        result.put(BENEFIT_ORIGINAL_ORDER_SN, request.getOriginalOrderSn());
        if (request.getReferenceTransactionSn() != null) {
            result.put(BENEFIT_REFERENCE_TRANSACTION_SN, request.getReferenceTransactionSn());
        }
        result.put(BENEFIT_SOURCE_SUMMARY, request.getBenefitFundingSources()
                .stream()
                .map(this::sourceSummary)
                .toList());
        return Map.copyOf(result);
    }

    private Map<String, Object> refundContext(@NonNull FundsBenefitFundingRefundRequest request) {
        Map<String, Object> result = mergeContext(request.getContextVariables());
        result.put(BENEFIT_FUNDING, Boolean.TRUE);
        result.put(BENEFIT_ORIGINAL_ORDER_SN, request.getOriginalOrderSn());
        if (request.getReferenceTransactionSn() != null) {
            result.put(BENEFIT_REFERENCE_TRANSACTION_SN, request.getReferenceTransactionSn());
        }
        if (request.getRefundReason() != null) {
            result.put(BENEFIT_REFUND_REASON, request.getRefundReason());
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> reverseContext(@NonNull FundsBenefitFundingReverseRequest request) {
        Map<String, Object> result = mergeContext(request.getContextVariables());
        result.put(BENEFIT_FUNDING, Boolean.TRUE);
        result.put(BENEFIT_ORIGINAL_ORDER_SN, request.getOriginalOrderSn());
        if (request.getReferenceTransactionSn() != null) {
            result.put(BENEFIT_REFERENCE_TRANSACTION_SN, request.getReferenceTransactionSn());
        }
        if (request.getReverseReason() != null) {
            result.put(BENEFIT_REVERSE_REASON, request.getReverseReason());
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> mergeContext(@Nullable ReadonlyContextVariables contextVariables) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (contextVariables != null && contextVariables.getContextVariables() != null) {
            result.putAll(contextVariables.getContextVariables());
        }
        return result;
    }

    private Map<String, Object> sourceSummary(@NonNull FundsBenefitFundingSourceDTO source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source.getSourceType() != null) {
            result.put("sourceType", source.getSourceType().name());
        }
        if (source.getSourceId() != null) {
            result.put("sourceId", source.getSourceId());
        }
        if (source.getRuleId() != null) {
            result.put("ruleId", source.getRuleId());
        }
        if (source.getRuleVersion() != null) {
            result.put("sourceRuleRefVersion", source.getRuleVersion());
        }
        if (source.getAmount() != null) {
            result.put("sourceContributionMinor", source.getAmount().getAmount());
            result.put("sourceContributionCurrency", source.getAmount().getCurrency().name());
        }
        return Map.copyOf(result);
    }

    private record OriginalBenefitRoute(FundsAccountId costBearerAccountId,
                                        FundsAccountId benefitReceiverAccountId,
                                        LedgerSubjectCode benefitReceiverLedgerCode) {
    }
}

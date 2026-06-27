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
import com.wind.funds.transaction.application.FundsBenefitContributionTransactionService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.model.request.FundsBenefitFundingRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingSettleRequest;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 让利出资记账交易服务实现。
 *
 * <p>该实现只负责编排已决策让利出资结果到标准直接交易入口，不直接写 route、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Service
@AllArgsConstructor
public class FundsBenefitContributionTransactionServiceImpl implements FundsBenefitContributionTransactionService {

    private static final String BENEFIT_FUNDING = "benefitFunding";

    private static final String BENEFIT_FUNDING_NATURE_CODE = "benefitFundingNatureCode";

    private static final String BENEFIT_ORIGINAL_ORDER_SN = "benefitOriginalOrderSn";

    private static final String BENEFIT_REFERENCE_TRANSACTION_SN = "benefitReferenceTransactionSn";

    private static final String BENEFIT_REFUND_REASON = "benefitRefundReason";

    private static final Set<String> FORBIDDEN_BENEFIT_CONTEXT_KEYS = Set.of(
            "amount",
            "costbearersubjectref",
            "benefitreceiversubjectref",
            "benefitsettlementsubjectref",
            "fundingnature",
            "fundingnaturecode",
            "benefitfundingsources",
            "ledgereffect",
            "benefitfundingdigest",
            "sourcetype",
            "sourceid",
            "ruleid",
            "ruleversion");

    private static final Set<FundsBenefitFundingNature> SUPPORTED_SETTLE_FUNDING_NATURES = Set.of(
            FundsBenefitFundingNature.MERCHANT_BORNE,
            FundsBenefitFundingNature.PLATFORM_OWN_FUNDS,
            FundsBenefitFundingNature.PARTNER_FUNDED);

    private final FundsDirectTransactionService directTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String settle(@NonNull FundsBenefitFundingSettleRequest request,
                                  @NonNull WindOperator operator) {
        assertSettleRequest(request);
        assertLightweightContext(request.getContextVariables());
        FundsAccountId costBearer = toAccountId(request.getCostBearerSubjectRef(), "权益让利承担方");
        FundsAccountId receiver = toAccountId(request.getBenefitReceiverSubjectRef(), "权益让利承接账务主体");
        return directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(costBearer)
                .setPayeeId(receiver)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(request.getAmount()))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(settleContext(request)))
                .setDescription("benefit funding settle"), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String refund(@NonNull FundsBenefitFundingRefundRequest request,
                                  @NonNull WindOperator operator) {
        assertRefundRequest(request);
        assertLightweightContext(request.getContextVariables());
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

    private void assertSettleRequest(@NonNull FundsBenefitFundingSettleRequest request) {
        AssertUtils.notNull(request.getTenantId(), "权益让利 tenantId 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "权益让利 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getBusinessScene(), "权益让利业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "权益让利业务流水不能为空");
        AssertUtils.hasText(request.getOriginalOrderSn(), "权益让利原始订单号不能为空");
        AssertUtils.notNull(request.getCostBearerSubjectRef(), "权益让利承担方不能为空");
        AssertUtils.notNull(request.getBenefitReceiverSubjectRef(), "权益让利承接账务主体不能为空");
        AssertUtils.notNull(request.getAmount(), "权益让利金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "权益让利金额必须大于 0");
        AssertUtils.notNull(request.getFundingNature(), "权益让利资金性质不能为空");
        AssertUtils.isTrue(SUPPORTED_SETTLE_FUNDING_NATURES.contains(request.getFundingNature()),
                "权益让利结算仅支持平台、商户或合作方出资责任记账，不支持返利、佣金、分润、储值负债释放或无资金转移解释事实，fundingNature = {}",
                request.getFundingNature());
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
                toAccountId(leg.getTargetNode().getSubjectRef(), "原权益让利承接账务主体"),
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

    private Map<String, Object> settleContext(@NonNull FundsBenefitFundingSettleRequest request) {
        Map<String, Object> result = mergeContext(request.getContextVariables());
        result.put(BENEFIT_FUNDING, Boolean.TRUE);
        result.put(BENEFIT_FUNDING_NATURE_CODE, request.getFundingNature().name());
        result.put(BENEFIT_ORIGINAL_ORDER_SN, request.getOriginalOrderSn());
        if (request.getReferenceTransactionSn() != null) {
            result.put(BENEFIT_REFERENCE_TRANSACTION_SN, request.getReferenceTransactionSn());
        }
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

    private Map<String, Object> mergeContext(@Nullable ReadonlyContextVariables contextVariables) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (contextVariables != null && contextVariables.getContextVariables() != null) {
            result.putAll(contextVariables.getContextVariables());
        }
        return result;
    }

    private void assertLightweightContext(@Nullable ReadonlyContextVariables contextVariables) {
        if (contextVariables == null || contextVariables.getContextVariables() == null) {
            return;
        }
        for (String key : contextVariables.getContextVariables().keySet()) {
            String normalizedKey = normalizeBenefitContextKey(key);
            AssertUtils.isTrue(!FORBIDDEN_BENEFIT_CONTEXT_KEYS.contains(normalizedKey),
                    "让利出资扩展上下文不得承载核心金额、分摊或规则事实，key = {}", key);
        }
    }

    private static String normalizeBenefitContextKey(@Nullable String key) {
        if (key == null) {
            return "";
        }
        return key.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
    }

    private record OriginalBenefitRoute(FundsAccountId costBearerAccountId,
                                        FundsAccountId benefitReceiverAccountId,
                                        LedgerSubjectCode benefitReceiverLedgerCode) {
    }
}

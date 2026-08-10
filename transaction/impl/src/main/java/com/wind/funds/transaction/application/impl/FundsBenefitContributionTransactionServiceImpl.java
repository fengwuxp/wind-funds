package com.wind.funds.transaction.application.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.application.FundsBenefitContributionTransactionService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.model.request.FundsBenefitContributionRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitContributionSettleRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    // 该上下文 key 参与资金交易幂等摘要，已落库名称保持稳定。
    private static final String BENEFIT_CONTRIBUTION_MARKER = "benefitFunding";

    private static final String BENEFIT_CONTRIBUTION_FUNDING_NATURE_CODE = "benefitFundingNatureCode";

    private static final String BENEFIT_ORIGINAL_ORDER_SN = "benefitOriginalOrderSn";

    private static final String BENEFIT_REFERENCE_TRANSACTION_SN = "benefitReferenceTransactionSn";

    private static final String BENEFIT_REFUND_REASON = "benefitRefundReason";

    private static final Set<String> FORBIDDEN_BENEFIT_CONTEXT_KEYS = Set.of(
            "amount",
            "costbeareraccountid",
            "costbearersubjectref",
            "benefitreceiveraccountid",
            "benefitreceiversubjectref",
            "benefitreceiverledgersubjectcode",
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

    private static final Set<LedgerSubjectCode> SUPPORTED_RECEIVER_LEDGER_SUBJECT_CODES = Set.of(
            LedgerSubjectCode.CLEARING,
            LedgerSubjectCode.SETTLEMENT);

    private final FundsDirectTransactionService directTransactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String settle(@NonNull FundsBenefitContributionSettleRequest request,
                                  @NonNull WindOperator operator) {
        assertSettleRequest(request);
        assertLightweightContext(request.getContextVariables());
        FundsAccountId costBearer = requireAccountId(request.getCostBearerAccountId(), "权益让利承担方");
        FundsAccountId receiver = requireAccountId(request.getBenefitReceiverAccountId(), "权益让利承接账务主体");
        return directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(costBearer)
                .setPayeeId(receiver)
                .setPayeeLedgerSubjectCode(request.getBenefitReceiverLedgerSubjectCode())
                .setTransactionAmount(TransactionAmount.sameCurrency(request.getAmount()))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(settleContext(request)))
                .setDescription("benefit contribution settle"), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String refund(@NonNull FundsBenefitContributionRefundRequest request,
                                  @NonNull WindOperator operator) {
        assertRefundRequest(request);
        assertLightweightContext(request.getContextVariables());
        return directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(request.getAmount()))
                .setReferenceTransactionSn(request.getReferenceBenefitTransactionSn())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(refundContext(request)))
                .setDescription("benefit contribution refund"), operator);
    }

    private void assertSettleRequest(@NonNull FundsBenefitContributionSettleRequest request) {
        AssertUtils.notNull(request.getTenantId(), "权益让利 tenantId 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "权益让利 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getBusinessScene(), "权益让利业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "权益让利业务流水不能为空");
        AssertUtils.hasText(request.getOriginalOrderSn(), "权益让利原始订单号不能为空");
        AssertUtils.notNull(request.getCostBearerAccountId(), "权益让利承担方不能为空");
        AssertUtils.notNull(request.getBenefitReceiverAccountId(), "权益让利承接账务主体不能为空");
        AssertUtils.notNull(request.getBenefitReceiverLedgerSubjectCode(), "权益让利承接目标账目不能为空");
        AssertUtils.isTrue(SUPPORTED_RECEIVER_LEDGER_SUBJECT_CODES
                        .contains(request.getBenefitReceiverLedgerSubjectCode()),
                "权益让利承接目标账目仅支持 CLEARING 或 SETTLEMENT，ledgerSubjectCode = {}",
                request.getBenefitReceiverLedgerSubjectCode());
        AssertUtils.notNull(request.getAmount(), "权益让利金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "权益让利金额必须大于 0");
        AssertUtils.notNull(request.getFundingNature(), "权益让利资金性质不能为空");
        AssertUtils.isTrue(SUPPORTED_SETTLE_FUNDING_NATURES.contains(request.getFundingNature()),
                "权益让利结算仅支持平台、商户或合作方出资责任记账，不支持返利、佣金、分润、储值负债释放或无资金转移解释事实，fundingNature = {}",
                request.getFundingNature());
    }

    private void assertRefundRequest(@NonNull FundsBenefitContributionRefundRequest request) {
        AssertUtils.notNull(request.getTenantId(), "权益让利退款 tenantId 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "权益让利退款 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReferenceBenefitTransactionSn(), "原让利出资记账交易流水不能为空");
        AssertUtils.notNull(request.getAmount(), "权益让利退款金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "权益让利退款金额必须大于 0");
        AssertUtils.hasText(request.getBusinessScene(), "权益让利退款业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "权益让利退款业务流水不能为空");
        AssertUtils.hasText(request.getOriginalOrderSn(), "权益让利退款原始订单号不能为空");
    }

    private FundsAccountId requireAccountId(@NonNull FundsAccountId accountId, @NonNull String roleName) {
        AssertUtils.hasText(accountId.id(), "{}账户 ID 不能为空", roleName);
        AssertUtils.hasText(accountId.type(), "{}账户类型不能为空", roleName);
        AssertUtils.isTrue(FundsSubjectType.isLedgerPostableName(accountId.type()),
                "{}必须是资金账户或信用账户，accountType = {}", roleName, accountId.type());
        return accountId;
    }

    private Map<String, Object> settleContext(@NonNull FundsBenefitContributionSettleRequest request) {
        Map<String, Object> result = mergeContext(request.getContextVariables());
        result.put(BENEFIT_CONTRIBUTION_MARKER, Boolean.TRUE);
        result.put(BENEFIT_CONTRIBUTION_FUNDING_NATURE_CODE, request.getFundingNature().name());
        result.put(BENEFIT_ORIGINAL_ORDER_SN, request.getOriginalOrderSn());
        if (request.getReferenceTransactionSn() != null) {
            result.put(BENEFIT_REFERENCE_TRANSACTION_SN, request.getReferenceTransactionSn());
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> refundContext(@NonNull FundsBenefitContributionRefundRequest request) {
        Map<String, Object> result = mergeContext(request.getContextVariables());
        result.put(BENEFIT_CONTRIBUTION_MARKER, Boolean.TRUE);
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
        assertLightweightContextValue(contextVariables.getContextVariables(), List.of());
    }

    private void assertLightweightContextValue(@Nullable Object value, @NonNull List<String> parentKeyPaths) {
        if (value instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().toString();
                String normalizedKey = normalizeBenefitContextKey(key);
                List<String> keyPaths = new ArrayList<>(parentKeyPaths.size() + 1);
                keyPaths.add(normalizedKey);
                parentKeyPaths.forEach(parentKeyPath -> keyPaths.add(parentKeyPath + normalizedKey));
                AssertUtils.isTrue(keyPaths.stream().noneMatch(FORBIDDEN_BENEFIT_CONTEXT_KEYS::contains),
                        "让利出资扩展上下文不得承载核心金额、分摊或规则事实，key = {}", key);
                for (String pathSegment : key.split("[^A-Za-z0-9]+")) {
                    AssertUtils.isTrue(!FORBIDDEN_BENEFIT_CONTEXT_KEYS
                                    .contains(normalizeBenefitContextKey(pathSegment)),
                            "让利出资扩展上下文不得承载核心金额、分摊或规则事实，key = {}", key);
                }
                assertLightweightContextValue(entry.getValue(), keyPaths);
            }
        } else if (value instanceof Iterable<?> values) {
            for (Object nestedValue : values) {
                assertLightweightContextValue(nestedValue, parentKeyPaths);
            }
        } else if (value instanceof Object[] values) {
            for (Object nestedValue : values) {
                assertLightweightContextValue(nestedValue, parentKeyPaths);
            }
        }
    }

    private static String normalizeBenefitContextKey(@Nullable String key) {
        if (key == null) {
            return "";
        }
        return key.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

}

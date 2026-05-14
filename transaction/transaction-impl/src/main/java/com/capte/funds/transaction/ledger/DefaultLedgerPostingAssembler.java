package com.capte.funds.transaction.ledger;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSettlementStatus;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认 Route -> Posting 翻译器。
 *
 * <p>职责：
 * <ul>
 *   <li>把 ResolvedRoute 中的每个 RouteLeg 翻译为独立平衡的 PostingPlan</li>
 *   <li>根据账本正常余额方向推导借贷方向</li>
 *   <li>校验每个 PostingPlan 的借贷平衡</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不选择 Route</li>
 *   <li>不自动建账，找不到账本时直接失败</li>
 *   <li>不执行 LedgerTransaction 持久化</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class DefaultLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec> {

    private static final String KEY_SEPARATOR = ":";

    private final LedgerService ledgerService;

    /**
     * 生成完整账本交易。
     *
     * <p>能力范围：把已解析资金路径翻译为 LedgerTransactionSpec，并保证每个 RouteLeg 对应的
     * PostingPlan 独立借贷平衡。不负责路径选择、账本创建或账本写入。</p>
     *
     * @param instruction 资金指令
     * @param fundsTransactionSn 资金交易流水号
     * @param resolvedRoute 已解析资金路径
     * @return 账本交易定义
     */
    @Override
    public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                   @NonNull String fundsTransactionSn,
                                                   @NonNull ResolvedRouteSpec resolvedRoute) {
        return LedgerTransactionSpecFactory.createLedgerTransaction(instruction, fundsTransactionSn,
                ledgerTransactionSn -> assemblePlans(ledgerTransactionSn, resolvedRoute));
    }

    /**
     * 判断是否支持该路径。
     *
     * <p>能力范围：默认实现支持所有存在 RouteLeg 的路径；空路径代表无账务影响，由编排器短路处理。</p>
     *
     * @param resolvedRoute 已解析资金路径
     * @return true 表示该路径存在可翻译的账务步骤
     */
    @Override
    public boolean support(@NonNull ResolvedRouteSpec resolvedRoute) {
        return !resolvedRoute.getLegs().isEmpty();
    }

    private List<LedgerPostingPlanSpec> assemblePlans(String ledgerTransactionSn,
                                                      ResolvedRouteSpec resolvedRoute) {
        List<LedgerPostingPlanSpec> result = new ArrayList<>();
        for (RouteLegSpec leg : resolvedRoute.getLegs()) {
            LedgerPostingPlanSpec plan = assembleLeg(ledgerTransactionSn, resolvedRoute, leg);
            AssertUtils.isTrue(plan.isBalanced(),
                    "RouteLeg 生成的账务计划不平衡，legId = {}", leg.getLegId());
            result.add(plan);
        }
        return result;
    }

    private LedgerPostingPlanSpec assembleLeg(String ledgerTransactionSn,
                                              ResolvedRouteSpec resolvedRoute,
                                              RouteLegSpec leg) {
        List<LedgerEntrySpec> entries = List.of(
                toEntry(ledgerTransactionSn, resolvedRoute, leg, leg.getSourceNode(), MovementDirection.DECREASE),
                toEntry(ledgerTransactionSn, resolvedRoute, leg, leg.getTargetNode(), MovementDirection.INCREASE)
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(leg.getPhaseCode(), entries);
        return DefaultLedgerPostingPlanSpec.builder()
                .planId(buildPlanId(resolveIntent(resolvedRoute), ledgerTransactionSn, leg))
                .ledgerTransactionSn(ledgerTransactionSn)
                .routeLegId(leg.getLegId())
                .intent(resolveIntent(resolvedRoute))
                .postingScope(null)
                .balanceEffectType(leg.getBalanceEffectType())
                .postingPhases(List.of(phase))
                .description(leg.getDescription())
                .contextVariables(mergedContext(resolvedRoute, leg))
                .build();
    }

    private LedgerEntrySpec toEntry(String ledgerTransactionSn,
                                    ResolvedRouteSpec resolvedRoute,
                                    RouteLegSpec leg,
                                    RouteNodeSpec node,
                                    MovementDirection direction) {
        LedgerDTO ledger = requireLedger(resolvedRoute, leg, node);
        EntrySide entrySide = resolveEntrySide(ledger.getNormalBalanceSide(), direction);
        SubjectRef subjectRef = node.getSubjectRef();
        return DefaultLedgerEntrySpec.builder()
                .ledgerId(ledger.getId())
                .subjectId(subjectRef.getSubjectId())
                .subjectType(subjectRef.getSubjectType().name())
                .ledgerSubjectCode(node.getLedgerSubjectCode())
                .ledgerSubjectCategory(ledger.getLedgerSubjectCategory())
                .entryType(entrySide)
                .ledgerTransactionSn(ledgerTransactionSn)
                .amount(leg.getAmount())
                .originalAmount(leg.getOriginalAmount())
                .exchangeRate(leg.getExchangeRate())
                .businessScene(resolvedRoute.getBusinessScene())
                .businessSn(resolvedRoute.getBusinessSn())
                .transactionTime(resolvedRoute.getResolvedAt())
                .settlementStatus(LedgerSettlementStatus.SETTLED)
                .description(leg.getDescription())
                .balanceConstraintType(resolveBalanceConstraintType(leg, node))
                .intent(resolveIntent(resolvedRoute))
                .balanceEffectType(leg.getBalanceEffectType())
                .phaseCode(leg.getPhaseCode())
                .contextVariables(mergedContext(resolvedRoute, leg))
                .build();
    }

    private LedgerDTO requireLedger(ResolvedRouteSpec resolvedRoute,
                                    RouteLegSpec leg,
                                    RouteNodeSpec node) {
        SubjectRef subjectRef = node.getSubjectRef();
        LedgerQuery query = new LedgerQuery()
                .setTenantId(resolvedRoute.getTenantId())
                .setSubjectId(subjectRef.getSubjectId())
                .setSubjectType(subjectRef.getSubjectType().name())
                .setLedgerSubjectCode(node.getLedgerSubjectCode())
                .setCurrency(leg.getAmount().getCurrency())
                .setPeriodType(leg.getPeriodType())
                .setPeriodId(resolvePeriodId(leg));
        List<LedgerDTO> records = ledgerService.queryLedgers(query, DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isTrue(records.size() == 1,
                "账本不存在或不唯一，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                subjectRef.getSubjectId(), subjectRef.getSubjectType(), node.getLedgerSubjectCode());
        LedgerDTO ledger = records.getFirst();
        AssertUtils.equals(ledger.getCurrency(), leg.getAmount().getCurrency(),
                "账本币种与路径金额币种不一致，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                subjectRef.getSubjectId(), subjectRef.getSubjectType(), node.getLedgerSubjectCode());
        return ledger;
    }

    private String resolvePeriodId(RouteLegSpec leg) {
        String periodId = leg.getPeriodId();
        if (periodId != null && !periodId.isBlank()) {
            return periodId;
        }
        return leg.getPeriodType().formatPeriodId();
    }

    private EntrySide resolveEntrySide(EntrySide normalBalanceSide, MovementDirection direction) {
        AssertUtils.notNull(normalBalanceSide, "账本正常余额方向不能为空");
        if (direction == MovementDirection.INCREASE) {
            return normalBalanceSide;
        }
        return normalBalanceSide == EntrySide.DEBIT ? EntrySide.CREDIT : EntrySide.DEBIT;
    }

    private LedgerBalanceConstraintType resolveBalanceConstraintType(RouteLegSpec leg, RouteNodeSpec node) {
        Map<String, LedgerBalanceConstraintType> overrides = leg.getConstraintOverrides();
        SubjectRef subjectRef = node.getSubjectRef();
        LedgerSubjectCode subjectCode = node.getLedgerSubjectCode();
        LedgerBalanceConstraintType result = overrides.get(fullConstraintKey(subjectRef, subjectCode));
        if (result != null) {
            return result;
        }
        result = overrides.get(subjectConstraintKey(subjectRef, subjectCode));
        if (result != null) {
            return result;
        }
        return overrides.getOrDefault(subjectCode.name(), LedgerBalanceConstraintType.PROFILE_DEFAULT);
    }

    private String fullConstraintKey(SubjectRef subjectRef, LedgerSubjectCode subjectCode) {
        return subjectRef.getSubjectType().name()
                + KEY_SEPARATOR
                + subjectRef.getSubjectId()
                + KEY_SEPARATOR
                + subjectCode.name();
    }

    private String subjectConstraintKey(SubjectRef subjectRef, LedgerSubjectCode subjectCode) {
        return subjectRef.getSubjectId() + KEY_SEPARATOR + subjectCode.name();
    }

    private LedgerPostingIntentType resolveIntent(ResolvedRouteSpec resolvedRoute) {
        FundsTransactionEventType eventType = resolvedRoute.getEventType();
        return switch (eventType) {
            case AUTHORIZE -> LedgerPostingIntentType.AUTHORIZATION;
            case REVERSAL -> LedgerPostingIntentType.AUTHORIZATION_REVERSAL;
            case SETTLE -> LedgerPostingIntentType.AUTHORIZATION_SETTLEMENT;
            case AUTH_REFUND, REFUND -> LedgerPostingIntentType.REFUND;
            case FEE_REFUND -> LedgerPostingIntentType.FEE_REFUND;
            case CHARGEBACK -> LedgerPostingIntentType.REVERSAL;
            case FREEZE -> LedgerPostingIntentType.HOLD;
            case UNFREEZE -> LedgerPostingIntentType.AUTHORIZATION_REVERSAL;
            case BALANCE_ADJUST, LIMIT_ADJUST -> LedgerPostingIntentType.ADJUSTMENT;
            case WITHDRAW -> LedgerPostingIntentType.WITHDRAWAL;
            case TOPUP, TRANSFER, PAY, FEE_CHARGE -> resolvePostedIntent(resolvedRoute.getTransactionType());
        };
    }

    private LedgerPostingIntentType resolvePostedIntent(DefaultFundsTransactionType transactionType) {
        return switch (transactionType) {
            case TOPUP -> LedgerPostingIntentType.TOPUP;
            case WITHDRAW -> LedgerPostingIntentType.WITHDRAWAL;
            case TRANSFER, PAY -> LedgerPostingIntentType.TRANSFER;
            case FEE -> LedgerPostingIntentType.FEE;
            case REFUND -> LedgerPostingIntentType.REFUND;
            case ADJUSTMENT -> LedgerPostingIntentType.ADJUSTMENT;
        };
    }

    private String buildPlanId(LedgerPostingIntentType intent, String ledgerTransactionSn, RouteLegSpec leg) {
        return intent.name() + "_" + ledgerTransactionSn + "_" + leg.getLegId();
    }

    private Map<String, Object> mergedContext(ResolvedRouteSpec resolvedRoute, RouteLegSpec leg) {
        Map<String, Object> result = new LinkedHashMap<>(resolvedRoute.getContextVariables());
        result.putAll(leg.getContextVariables());
        result.put("routeLegId", leg.getLegId());
        if (leg.getReplayRefLegId() != null && !leg.getReplayRefLegId().isBlank()) {
            result.put("replayRefLegId", leg.getReplayRefLegId());
        }
        result.put("replayPolicy", leg.getReplayPolicy().name());
        return Map.copyOf(result);
    }

    private enum MovementDirection {
        INCREASE,
        DECREASE
    }

    @Getter
    private static final class DefaultLedgerPostingPlanSpec implements LedgerPostingPlanSpec {

        private final String planId;

        private final String ledgerTransactionSn;

        private final String routeLegId;

        private final LedgerPostingIntentType intent;

        private final LedgerPostingScope postingScope;

        private final LedgerBalanceEffectType balanceEffectType;

        private final List<LedgerPostingPhaseSpec> postingPhases;

        private final String description;

        private final Map<String, Object> contextVariables;

        @Builder
        private DefaultLedgerPostingPlanSpec(String planId,
                                             String ledgerTransactionSn,
                                             String routeLegId,
                                             LedgerPostingIntentType intent,
                                             LedgerPostingScope postingScope,
                                             LedgerBalanceEffectType balanceEffectType,
                                             List<LedgerPostingPhaseSpec> postingPhases,
                                             String description,
                                             Map<String, Object> contextVariables) {
            this.planId = planId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.routeLegId = routeLegId;
            this.intent = intent;
            this.postingScope = postingScope;
            this.balanceEffectType = balanceEffectType;
            this.postingPhases = List.copyOf(postingPhases == null ? List.of() : postingPhases);
            this.description = description;
            this.contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
        }
    }

    @Getter
    private static final class DefaultLedgerEntrySpec implements LedgerEntrySpec {

        private final String subjectId;

        private final String subjectType;

        private final LedgerSubjectCode ledgerSubjectCode;

        private final LedgerSubjectCategory ledgerSubjectCategory;

        private final Long ledgerId;

        private final String ledgerTransactionSn;

        private final EntrySide entryType;

        private final LedgerPhaseCode phaseCode;

        private final LedgerPostingIntentType intent;

        private final LedgerPostingScope postingScope;

        private final LedgerBalanceEffectType balanceEffectType;

        private final LedgerBalanceConstraintType balanceConstraintType;

        private final String businessScene;

        private final String businessSn;

        private final Money amount;

        private final Money originalAmount;

        private final BigDecimal exchangeRate;

        private final LocalDateTime transactionTime;

        private final LedgerSettlementStatus settlementStatus;

        private final String description;

        private final Map<String, Object> contextVariables;

        @Builder
        private DefaultLedgerEntrySpec(String subjectId,
                                       String subjectType,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       LedgerSubjectCategory ledgerSubjectCategory,
                                       Long ledgerId,
                                       String ledgerTransactionSn,
                                       EntrySide entryType,
                                       LedgerPhaseCode phaseCode,
                                       LedgerPostingIntentType intent,
                                       LedgerPostingScope postingScope,
                                       LedgerBalanceEffectType balanceEffectType,
                                       LedgerBalanceConstraintType balanceConstraintType,
                                       String businessScene,
                                       String businessSn,
                                       Money amount,
                                       Money originalAmount,
                                       BigDecimal exchangeRate,
                                       LocalDateTime transactionTime,
                                       LedgerSettlementStatus settlementStatus,
                                       @Nullable String description,
                                       Map<String, Object> contextVariables) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
            this.ledgerSubjectCode = ledgerSubjectCode;
            this.ledgerSubjectCategory = ledgerSubjectCategory;
            this.ledgerId = ledgerId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.entryType = entryType;
            this.phaseCode = phaseCode;
            this.intent = intent;
            this.postingScope = postingScope;
            this.balanceEffectType = balanceEffectType;
            this.balanceConstraintType = balanceConstraintType;
            this.businessScene = businessScene;
            this.businessSn = businessSn;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.transactionTime = transactionTime;
            this.settlementStatus = settlementStatus;
            this.description = description;
            this.contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}

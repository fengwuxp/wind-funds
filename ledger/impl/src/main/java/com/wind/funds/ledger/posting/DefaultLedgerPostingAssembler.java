package com.wind.funds.ledger.posting;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.LedgerPostingAssembler;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.FundsContextVariables;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.spec.ledger.LedgerEntrySpec;
import com.wind.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class DefaultLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec>, Ordered {

    private static final String KEY_SEPARATOR = ":";

    private static final String PLAN_ID_SEPARATOR = "_";

    private static final int POSTING_PLAN_ID_MAX_LENGTH = 64;

    private static final int POSTING_PLAN_ID_DIGEST_LENGTH = 16;

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
    public boolean supports(@NonNull ResolvedRouteSpec resolvedRoute) {
        return !resolvedRoute.getLegs().isEmpty();
    }

    private List<LedgerPostingPlanSpec> assemblePlans(String ledgerTransactionSn,
                                                      ResolvedRouteSpec resolvedRoute) {
        List<LedgerPostingPlanSpec> result = new ArrayList<>();
        Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots = new HashMap<>();
        for (RouteLegSpec leg : resolvedRoute.getLegs()) {
            LedgerPostingPlanSpec plan = assembleLeg(ledgerTransactionSn, resolvedRoute, leg, ledgerSnapshots);
            AssertUtils.isTrue(plan.isBalanced(),
                    "RouteLeg 生成的账务计划不平衡，legId = {}", leg.getLegId());
            result.add(plan);
        }
        return result;
    }

    private LedgerPostingPlanSpec assembleLeg(String ledgerTransactionSn,
                                              ResolvedRouteSpec resolvedRoute,
                                              RouteLegSpec leg,
                                              Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots) {
        LedgerPostingIntentType intent = resolveIntent(resolvedRoute);
        LedgerPostingScope postingScope = resolvePostingScope(intent, leg.getPhaseCode());
        List<LedgerEntrySpec> entries = List.of(
                toEntry(ledgerTransactionSn, resolvedRoute, leg, leg.getSourceNode(),
                        resolveSourceDirection(resolvedRoute, leg),
                        intent, postingScope, ledgerSnapshots),
                toEntry(ledgerTransactionSn, resolvedRoute, leg, leg.getTargetNode(),
                        resolveTargetDirection(resolvedRoute, leg),
                        intent, postingScope, ledgerSnapshots)
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(leg.getPhaseCode(), entries);
        return DefaultLedgerPostingPlanSpec.builder()
                .planId(buildPlanId(intent, ledgerTransactionSn, leg))
                .ledgerTransactionSn(ledgerTransactionSn)
                .routeLegId(leg.getLegId())
                .intent(intent)
                .postingScope(postingScope)
                .balanceEffectType(leg.getBalanceEffectType())
                .postingPhases(List.of(phase))
                .description(leg.getDescription())
                .contextVariables(mergedContext(resolvedRoute, leg))
                .build();
    }

    private MovementDirection resolveSourceDirection(ResolvedRouteSpec resolvedRoute, RouteLegSpec leg) {
        if (resolvedRoute.getEventType() == FundsTransactionEventType.LIMIT_ADJUST) {
            return resolveLimitAdjustDirection(leg);
        }
        return MovementDirection.DECREASE;
    }

    private MovementDirection resolveTargetDirection(ResolvedRouteSpec resolvedRoute, RouteLegSpec leg) {
        if (resolvedRoute.getEventType() == FundsTransactionEventType.LIMIT_ADJUST) {
            return resolveLimitAdjustDirection(leg);
        }
        return MovementDirection.INCREASE;
    }

    private MovementDirection resolveLimitAdjustDirection(RouteLegSpec leg) {
        return switch (leg.getBalanceEffectType()) {
            case INCREASE -> MovementDirection.INCREASE;
            case DECREASE -> MovementDirection.DECREASE;
            default -> throw new IllegalArgumentException("LIMIT_ADJUST only supports INCREASE or DECREASE effect");
        };
    }

    private LedgerEntrySpec toEntry(String ledgerTransactionSn,
                                    ResolvedRouteSpec resolvedRoute,
                                    RouteLegSpec leg,
                                    RouteNodeSpec node,
                                    MovementDirection direction,
                                    LedgerPostingIntentType intent,
                                    LedgerPostingScope postingScope,
                                    Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots) {
        LedgerDTO ledger = requireLedger(resolvedRoute, leg, node, ledgerSnapshots);
        EntrySide entrySide = resolveEntrySide(ledger.getNormalBalanceSide(), direction);
        SubjectRef subjectRef = node.getSubjectRef();
        return DefaultLedgerEntrySpec.builder()
                .ledgerId(ledger.getId())
                .periodType(ledger.getPeriodType())
                .periodId(ledger.getPeriodId())
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
                .description(leg.getDescription())
                .balanceConstraintType(resolveBalanceConstraintType(leg, node))
                .intent(intent)
                .postingScope(postingScope)
                .postingRole(LedgerPostingRole.DETAIL)
                .balanceEffectType(leg.getBalanceEffectType())
                .phaseCode(leg.getPhaseCode())
                .contextVariables(mergedContext(resolvedRoute, leg))
                .build();
    }

    private LedgerDTO requireLedger(ResolvedRouteSpec resolvedRoute,
                                    RouteLegSpec leg,
                                    RouteNodeSpec node,
                                    Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots) {
        SubjectRef subjectRef = node.getSubjectRef();
        LedgerBucketGroupKey key = new LedgerBucketGroupKey(
                resolvedRoute.getTenantId(),
                subjectRef.getSubjectId(),
                subjectRef.getSubjectType(),
                leg.getAmount().getCurrency(),
                leg.getPeriodType(),
                resolvePeriodId(leg));
        Map<LedgerSubjectCode, LedgerDTO> ledgers = ledgerSnapshots.computeIfAbsent(key, this::loadLedgers);
        LedgerDTO ledger = ledgers.get(node.getLedgerSubjectCode());
        AssertUtils.notNull(ledger,
                "账本不存在或不唯一，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, periodType = {}, periodId = {}",
                subjectRef.getSubjectId(),
                subjectRef.getSubjectType(),
                node.getLedgerSubjectCode(),
                key.periodType(),
                key.periodId());
        AssertUtils.equals(ledger.getCurrency(), leg.getAmount().getCurrency(),
                "账本币种与路径金额币种不一致，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                subjectRef.getSubjectId(), subjectRef.getSubjectType(), node.getLedgerSubjectCode());
        return ledger;
    }

    private Map<LedgerSubjectCode, LedgerDTO> loadLedgers(LedgerBucketGroupKey key) {
        List<LedgerDTO> records = ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(key.tenantId())
                        .setSubjectId(key.subjectId())
                        .setSubjectType(key.subjectType().name())
                        .setCurrency(key.currency())
                        .setPeriodType(key.periodType())
                        .setPeriodId(key.periodId()),
                DefaultPageQueryOptions.result(LedgerSubjectCode.values().length)).getRecords();
        return records.stream()
                .collect(Collectors.toMap(LedgerDTO::getLedgerSubjectCode, Function.identity()));
    }

    private String resolvePeriodId(RouteLegSpec leg) {
        AccountBalancePeriodType periodType = leg.getPeriodType();
        AssertUtils.notNull(periodType, "账本周期类型不能为空");
        if (periodType == AccountBalancePeriodType.LIFETIME) {
            return AccountBalancePeriodType.LIFETIME.name();
        }
        String periodId = leg.getPeriodId();
        AssertUtils.hasText(periodId, "非生命周期账本周期 periodId 不能为空");
        return periodId;
    }

    private record LedgerBucketGroupKey(Long tenantId,
                                        String subjectId,
                                        FundsSubjectType subjectType,
                                        CurrencyIsoCode currency,
                                        AccountBalancePeriodType periodType,
                                        String periodId) {
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
            case COMPLETE -> LedgerPostingIntentType.AUTHORIZATION_COMPLETION;
            case AUTH_REFUND, REFUND -> LedgerPostingIntentType.REFUND;
            case FEE_REFUND -> LedgerPostingIntentType.FEE_REFUND;
            case FREEZE -> LedgerPostingIntentType.HOLD;
            case UNFREEZE -> LedgerPostingIntentType.AUTHORIZATION_REVERSAL;
            case BALANCE_ADJUST, LIMIT_ADJUST -> LedgerPostingIntentType.ADJUSTMENT;
            case WITHDRAW -> LedgerPostingIntentType.WITHDRAWAL;
            case CLEARING_CONFIRM -> LedgerPostingIntentType.SETTLEMENT;
            case SETTLEMENT_LOCK -> LedgerPostingIntentType.SETTLEMENT;
            case PAYOUT_SUCCEEDED -> LedgerPostingIntentType.WITHDRAWAL;
            case PAYOUT_FAILED -> LedgerPostingIntentType.REFUND;
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
            case CLEARING -> LedgerPostingIntentType.SETTLEMENT;
            case SETTLEMENT -> LedgerPostingIntentType.SETTLEMENT;
            case PAYOUT -> LedgerPostingIntentType.WITHDRAWAL;
            case ADJUSTMENT -> LedgerPostingIntentType.ADJUSTMENT;
        };
    }

    private LedgerPostingScope resolvePostingScope(LedgerPostingIntentType intent, LedgerPhaseCode phaseCode) {
        return switch (intent) {
            case TOPUP, WITHDRAWAL -> LedgerPostingScope.PLATFORM_EXTERNAL;
            case FEE, FEE_REFUND, FEE_REVERSAL -> LedgerPostingScope.FEE;
            case ADJUSTMENT -> LedgerPostingScope.ADJUSTMENT;
            case HOLD -> resolveHoldPostingScope(phaseCode);
            case AUTHORIZATION, AUTHORIZATION_REVERSAL -> LedgerPostingScope.CONTROL_HOLD;
            case AUTHORIZATION_COMPLETION -> LedgerPostingScope.CONTROL_CONSUME;
            case SETTLEMENT -> LedgerPostingScope.WITHIN_SUBJECT;
            default -> LedgerPostingScope.BETWEEN_SUBJECTS;
        };
    }

    private LedgerPostingScope resolveHoldPostingScope(LedgerPhaseCode phaseCode) {
        return switch (phaseCode) {
            case FREEZE, UNFREEZE -> LedgerPostingScope.WITHIN_SUBJECT;
            default -> LedgerPostingScope.CONTROL_HOLD;
        };
    }

    private String buildPlanId(LedgerPostingIntentType intent, String ledgerTransactionSn, RouteLegSpec leg) {
        String rawPlanId = intent.name()
                + PLAN_ID_SEPARATOR
                + ledgerTransactionSn
                + PLAN_ID_SEPARATOR
                + leg.getLegId();
        if (rawPlanId.length() <= POSTING_PLAN_ID_MAX_LENGTH) {
            return rawPlanId;
        }
        String digest = FundsStableHashSupport.sha256(rawPlanId).substring(0, POSTING_PLAN_ID_DIGEST_LENGTH);
        String prefix = intent.name() + PLAN_ID_SEPARATOR + ledgerTransactionSn;
        int maxPrefixLength = POSTING_PLAN_ID_MAX_LENGTH - PLAN_ID_SEPARATOR.length() - digest.length();
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return prefix + PLAN_ID_SEPARATOR + digest;
    }

    private Map<String, Object> mergedContext(ResolvedRouteSpec resolvedRoute, RouteLegSpec leg) {
        Map<String, Object> result = new LinkedHashMap<>(resolvedRoute.getContextVariables());
        result.putAll(leg.getContextVariables());
        result.put("routeLegId", leg.getLegId());
        if (StringUtils.hasText(leg.getReplayRefLegId())) {
            result.put("replayRefLegId", leg.getReplayRefLegId());
        }
        result.put("replayPolicy", leg.getReplayPolicy().name());
        return FundsContextVariables.immutableCopy(result);
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

        private final AccountBalancePeriodType periodType;

        private final String periodId;

        private final String ledgerTransactionSn;

        private final EntrySide entryType;

        private final LedgerPhaseCode phaseCode;

        private final LedgerPostingIntentType intent;

        private final LedgerPostingScope postingScope;

        private final LedgerPostingRole postingRole;

        private final LedgerBalanceEffectType balanceEffectType;

        private final LedgerBalanceConstraintType balanceConstraintType;

        private final String businessScene;

        private final String businessSn;

        private final Money amount;

        private final Money originalAmount;

        private final BigDecimal exchangeRate;

        private final LocalDateTime transactionTime;

        private final String description;

        private final Map<String, Object> contextVariables;

        @Builder
        private DefaultLedgerEntrySpec(String subjectId,
                                       String subjectType,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       LedgerSubjectCategory ledgerSubjectCategory,
                                       Long ledgerId,
                                       AccountBalancePeriodType periodType,
                                       String periodId,
                                       String ledgerTransactionSn,
                                       EntrySide entryType,
                                       LedgerPhaseCode phaseCode,
                                       LedgerPostingIntentType intent,
                                       LedgerPostingScope postingScope,
                                       LedgerPostingRole postingRole,
                                       LedgerBalanceEffectType balanceEffectType,
                                       LedgerBalanceConstraintType balanceConstraintType,
                                       String businessScene,
                                       String businessSn,
                                       Money amount,
                                       Money originalAmount,
                                       BigDecimal exchangeRate,
                                       LocalDateTime transactionTime,
                                       @Nullable String description,
                                       Map<String, Object> contextVariables) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
            this.ledgerSubjectCode = ledgerSubjectCode;
            this.ledgerSubjectCategory = ledgerSubjectCategory;
            this.ledgerId = ledgerId;
            this.periodType = periodType;
            this.periodId = periodId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.entryType = entryType;
            this.phaseCode = phaseCode;
            this.intent = intent;
            this.postingScope = postingScope;
            this.postingRole = postingRole;
            this.balanceEffectType = balanceEffectType;
            this.balanceConstraintType = balanceConstraintType;
            this.businessScene = businessScene;
            this.businessSn = businessSn;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.transactionTime = transactionTime;
            this.description = description;
            this.contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}

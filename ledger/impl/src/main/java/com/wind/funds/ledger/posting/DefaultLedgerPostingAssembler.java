package com.wind.funds.ledger.posting;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.dal.entities.table.LedgerEntryNameRefs;
import com.wind.funds.ledger.dal.entities.table.LedgerPostingPlanNameRefs;
import com.wind.funds.ledger.dal.entities.table.LedgerTransactionNameRefs;
import com.wind.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.wind.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.wind.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.common.exception.AssertUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.support.FundsContextVariables;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPhaseSpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
public class DefaultLedgerPostingAssembler {

    private static final String KEY_SEPARATOR = ":";

    private static final String PLAN_ID_SEPARATOR = "_";

    private static final String FEE_ROUTE_LEG_ID = "FEE";

    private static final int POSTING_PLAN_ID_MAX_LENGTH = 64;

    private static final int POSTING_PLAN_ID_DIGEST_LENGTH = 16;

    private final LedgerService ledgerService;

    private final LedgerProfileCatalog ledgerProfileCatalog;

    private final LedgerPostingPlanMapper ledgerPostingPlanMapper;

    private final LedgerEntryMapper ledgerEntryMapper;

    private final LedgerTransactionMapper ledgerTransactionMapper;

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
    public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                   @NonNull String fundsTransactionSn,
                                                   @NonNull ResolvedRouteSpec resolvedRoute) {
        return LedgerTransactionSpecFactory.createLedgerTransaction(instruction, fundsTransactionSn,
                ledgerTransactionSn -> assemblePlans(instruction, ledgerTransactionSn, resolvedRoute));
    }

    private List<LedgerPostingPlanSpec> assemblePlans(FundsInstructionSpec instruction,
                                                      String ledgerTransactionSn,
                                                      ResolvedRouteSpec resolvedRoute) {
        List<LedgerPostingPlanSpec> result = new ArrayList<>();
        Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots = new HashMap<>();
        for (RouteLegSpec leg : resolvedRoute.getLegs()) {
            LedgerPostingPlanSpec plan = assembleLeg(instruction, ledgerTransactionSn, resolvedRoute, leg,
                    ledgerSnapshots);
            AssertUtils.isTrue(plan.isBalanced(),
                    "RouteLeg 生成的账务计划不平衡，legId = {}", leg.getLegId());
            result.add(plan);
        }
        return result;
    }

    private LedgerPostingPlanSpec assembleLeg(FundsInstructionSpec instruction,
                                              String ledgerTransactionSn,
                                              ResolvedRouteSpec resolvedRoute,
                                              RouteLegSpec leg,
                                              Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots) {
        OriginalPosting originalPosting = resolveOriginalPosting(instruction, leg);
        PostingSemantics semantics = resolvePostingSemantics(instruction, resolvedRoute, leg, originalPosting);
        LedgerPostingIntentType intent = resolveIntent(resolvedRoute, leg);
        LedgerPostingScope postingScope = resolvePostingScope(intent, semantics.phaseCode());
        List<LedgerEntrySpec> entries = List.of(
                toEntry(ledgerTransactionSn, resolvedRoute, leg, leg.getSourceNode(),
                        semantics.sourceSubjectCode(), semantics.sourceDirection(), semantics.sourceConstraint(),
                        semantics, intent, postingScope, ledgerSnapshots),
                toEntry(ledgerTransactionSn, resolvedRoute, leg, leg.getTargetNode(),
                        semantics.targetSubjectCode(), semantics.targetDirection(),
                        LedgerBalanceConstraintType.PROFILE_DEFAULT,
                        semantics, intent, postingScope, ledgerSnapshots)
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(semantics.phaseCode(), entries);
        return DefaultLedgerPostingPlanSpec.builder()
                .planId(buildPlanId(intent, ledgerTransactionSn, leg))
                .ledgerTransactionSn(ledgerTransactionSn)
                .routeLegId(leg.getLegId())
                .intent(intent)
                .postingScope(postingScope)
                .balanceEffectType(semantics.balanceEffectType())
                .postingPhases(List.of(phase))
                .description(leg.getDescription())
                .contextVariables(mergedContext(resolvedRoute, leg))
                .build();
    }

    private PostingSemantics resolvePostingSemantics(FundsInstructionSpec instruction,
                                                      ResolvedRouteSpec route,
                                                      RouteLegSpec leg,
                                                      @Nullable OriginalPosting originalPosting) {
        LedgerBalanceEffectType balanceEffectType = resolveBalanceEffectType(instruction, route, leg);
        AccountBalancePeriodType periodType = originalPosting == null
                ? resolveInstructionPeriodType(instruction) : originalPosting.periodType();
        String periodId = originalPosting == null
                ? resolveInstructionPeriodId(instruction, periodType) : originalPosting.periodId();
        MovementDirection sourceDirection;
        MovementDirection targetDirection;
        if (route.getEventType() == FundsTransactionEventType.BALANCE_ADJUST
                || route.getEventType() == FundsTransactionEventType.LIMIT_ADJUST) {
            sourceDirection = adjustmentDirection(balanceEffectType);
            targetDirection = sourceDirection;
        } else if (leg.getLegType() == RouteLegType.EXTERNAL_IN) {
            sourceDirection = MovementDirection.INCREASE;
            targetDirection = MovementDirection.INCREASE;
        } else if (leg.getLegType() == RouteLegType.EXTERNAL_OUT) {
            sourceDirection = MovementDirection.DECREASE;
            targetDirection = MovementDirection.DECREASE;
        } else {
            sourceDirection = MovementDirection.DECREASE;
            targetDirection = MovementDirection.INCREASE;
        }
        return new PostingSemantics(
                resolveSubjectCode(instruction, route, leg, leg.getSourceNode(), true, originalPosting),
                resolveSubjectCode(instruction, route, leg, leg.getTargetNode(), false, originalPosting),
                periodType,
                periodId,
                balanceEffectType,
                resolvePhaseCode(route, leg),
                resolveSourceConstraint(route, leg, balanceEffectType),
                sourceDirection,
                targetDirection);
    }

    private LedgerEntrySpec toEntry(String ledgerTransactionSn,
                                    ResolvedRouteSpec route,
                                    RouteLegSpec leg,
                                    RouteNodeSpec node,
                                    LedgerSubjectCode subjectCode,
                                    MovementDirection direction,
                                    LedgerBalanceConstraintType balanceConstraint,
                                    PostingSemantics semantics,
                                    LedgerPostingIntentType intent,
                                    LedgerPostingScope postingScope,
                                    Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots) {
        LedgerDTO ledger = requireLedger(route, leg, node, subjectCode, semantics, ledgerSnapshots);
        EntrySide entrySide = resolveEntrySide(ledger.getNormalBalanceSide(), direction);
        SubjectRef subjectRef = node.getSubjectRef();
        return DefaultLedgerEntrySpec.builder()
                .ledgerId(ledger.getId())
                .periodType(ledger.getPeriodType())
                .periodId(ledger.getPeriodId())
                .subjectId(subjectRef.getSubjectId())
                .subjectType(subjectRef.getSubjectType().name())
                .ledgerSubjectCode(subjectCode)
                .ledgerSubjectCategory(ledger.getLedgerSubjectCategory())
                .entryType(entrySide)
                .ledgerTransactionSn(ledgerTransactionSn)
                .amount(leg.getAmount())
                .originalAmount(leg.getOriginalAmount())
                .exchangeRate(leg.getExchangeRate())
                .businessScene(route.getBusinessScene())
                .businessSn(route.getBusinessSn())
                .transactionTime(route.getResolvedAt())
                .description(leg.getDescription())
                .balanceConstraintType(balanceConstraint)
                .intent(intent)
                .postingScope(postingScope)
                .postingRole(LedgerPostingRole.DETAIL)
                .balanceEffectType(semantics.balanceEffectType())
                .phaseCode(semantics.phaseCode())
                .contextVariables(mergedContext(route, leg))
                .build();
    }

    private LedgerDTO requireLedger(ResolvedRouteSpec route,
                                    RouteLegSpec leg,
                                    RouteNodeSpec node,
                                    LedgerSubjectCode subjectCode,
                                    PostingSemantics semantics,
                                    Map<LedgerBucketGroupKey, Map<LedgerSubjectCode, LedgerDTO>> ledgerSnapshots) {
        SubjectRef subjectRef = node.getSubjectRef();
        LedgerBucketGroupKey key = new LedgerBucketGroupKey(
                route.getTenantId(),
                subjectRef.getSubjectId(),
                subjectRef.getSubjectType(),
                leg.getAmount().getCurrency(),
                semantics.periodType(),
                semantics.periodId());
        Map<LedgerSubjectCode, LedgerDTO> ledgers = ledgerSnapshots.computeIfAbsent(key, this::loadLedgers);
        LedgerDTO ledger = ledgers.get(subjectCode);
        AssertUtils.notNull(ledger,
                "账本不存在或不唯一，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, periodType = {}, periodId = {}",
                subjectRef.getSubjectId(), subjectRef.getSubjectType(), subjectCode, key.periodType(), key.periodId());
        AssertUtils.equals(ledger.getCurrency(), leg.getAmount().getCurrency(),
                "账本币种与路径金额币种不一致，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}",
                subjectRef.getSubjectId(), subjectRef.getSubjectType(), subjectCode);
        assertCatalogIntegrity(key, ledger);
        if (route.getEventType() == FundsTransactionEventType.SETTLEMENT_RELEASE) {
            LedgerProfileCode profileCode = requireProfileCode(ledger.getLedgerProfileCode());
            ledgerProfileCatalog.requireItems(profileCode,
                    LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN);
        }
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
        Map<LedgerSubjectCode, LedgerDTO> result = new LinkedHashMap<>();
        for (LedgerDTO record : records) {
            AssertUtils.isFalse(result.containsKey(record.getLedgerSubjectCode()),
                    "账本不存在或不唯一，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, periodType = {}, periodId = {}",
                    key.subjectId(), key.subjectType(), record.getLedgerSubjectCode(), key.periodType(), key.periodId());
            result.put(record.getLedgerSubjectCode(), record);
        }
        return result;
    }

    private void assertCatalogIntegrity(LedgerBucketGroupKey key, LedgerDTO ledger) {
        CreateLedgerRequest expected = ledgerProfileCatalog.requiredLedgerRequests(
                        new InitializeSubjectLedgerRequest()
                                .setTenantId(key.tenantId())
                                .setSubjectId(key.subjectId())
                                .setSubjectType(key.subjectType())
                                .setCurrency(key.currency())
                                .setLedgerProfileCode(requireProfileCode(ledger.getLedgerProfileCode()))
                                .setLedgerProfileVersion(ledger.getLedgerProfileVersion())
                                .setPeriodType(key.periodType())
                                .setPeriodId(key.periodId()))
                .stream()
                .filter(candidate -> candidate.getLedgerSubjectCode() == ledger.getLedgerSubjectCode())
                .findFirst()
                .orElse(null);
        AssertUtils.notNull(expected,
                "LedgerProfileItem 不存在，profileCode = {}, subjectCode = {}",
                ledger.getLedgerProfileCode(), ledger.getLedgerSubjectCode());
        ledgerProfileCatalog.assertLedgerMatches(expected, ledger);
    }

    private LedgerProfileCode requireProfileCode(String rawProfileCode) {
        for (LedgerProfileCode candidate : LedgerProfileCode.values()) {
            if (candidate.name().equals(rawProfileCode)) {
                return candidate;
            }
        }
        AssertUtils.isTrue(false, "LedgerProfile 不存在，profileCode = {}", rawProfileCode);
        throw new IllegalStateException("unreachable");
    }

    private AccountBalancePeriodType resolveInstructionPeriodType(FundsInstructionSpec instruction) {
        AccountBalancePeriodType periodType = instruction.getLedgerPeriodType();
        return periodType == null ? AccountBalancePeriodType.LIFETIME : periodType;
    }

    private String resolveInstructionPeriodId(FundsInstructionSpec instruction,
                                              AccountBalancePeriodType periodType) {
        if (periodType == AccountBalancePeriodType.LIFETIME) {
            return AccountBalancePeriodType.LIFETIME.name();
        }
        String periodId = instruction.getLedgerPeriodId();
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

    private @Nullable OriginalPosting resolveOriginalPosting(FundsInstructionSpec instruction, RouteLegSpec leg) {
        if (!StringUtils.hasText(leg.getReplayRefLegId())) {
            return null;
        }
        var reference = instruction.getReference();
        AssertUtils.notNull(reference, "回放账务事实必须引用原资金交易");
        AssertUtils.hasText(reference.getReferenceSn(), "回放账务事实必须引用原资金交易");
        Long tenantId = instruction.getTenantId();
        AssertUtils.notNull(tenantId, "回放账务事实 tenantId 不能为空");
        String ledgerTransactionSn = reference.getReferenceLedgerTransactionSn();
        LedgerTransactionNameRefs transaction = LedgerTransactionNameRefs.ledgerTransaction;
        if (StringUtils.hasText(ledgerTransactionSn)) {
            LedgerTransaction originalTransaction = ledgerTransactionMapper.selectOneByQuery(QueryWrapper.create()
                    .from(transaction)
                    .where(transaction.tenantId.eq(tenantId))
                    .and(transaction.sn.eq(ledgerTransactionSn))
                    .and(transaction.fundsTransactionSn.eq(reference.getReferenceSn())));
            AssertUtils.notNull(originalTransaction,
                    "原账本交易与引用资金交易不一致，ledgerTransactionSn = {}, fundsTransactionSn = {}",
                    ledgerTransactionSn, reference.getReferenceSn());
        } else {
            List<LedgerTransaction> originalTransactions = ledgerTransactionMapper.selectListByQuery(
                    QueryWrapper.create()
                            .from(transaction)
                            .where(transaction.tenantId.eq(tenantId))
                            .and(transaction.fundsTransactionSn.eq(reference.getReferenceSn())));
            AssertUtils.isTrue(originalTransactions.size() == 1,
                    "原资金交易对应账本交易不存在或不唯一，fundsTransactionSn = {}",
                    reference.getReferenceSn());
            ledgerTransactionSn = originalTransactions.getFirst().getSn();
        }
        LedgerPostingPlanNameRefs plan = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        List<LedgerPostingPlan> plans = ledgerPostingPlanMapper.selectListByQuery(QueryWrapper.create()
                .from(plan)
                .where(plan.tenantId.eq(tenantId))
                .and(plan.ledgerTransactionSn.eq(ledgerTransactionSn))
                .and(plan.routeLegId.eq(leg.getReplayRefLegId())));
        AssertUtils.isTrue(plans.size() == 1,
                "原记账计划不存在或不唯一，ledgerTransactionSn = {}, routeLegId = {}",
                ledgerTransactionSn, leg.getReplayRefLegId());
        LedgerPostingPlan originalPlan = plans.getFirst();
        LedgerEntryNameRefs entry = LedgerEntryNameRefs.ledgerEntry;
        List<LedgerEntry> entries = ledgerEntryMapper.selectListByQuery(QueryWrapper.create()
                .from(entry)
                .where(entry.tenantId.eq(tenantId))
                .and(entry.ledgerTransactionSn.eq(ledgerTransactionSn))
                .and(entry.postingPlanSn.eq(originalPlan.getSn())));
        AssertUtils.isTrue(entries.size() == 2,
                "原记账计划分录不完整，ledgerTransactionSn = {}, routeLegId = {}, count = {}",
                ledgerTransactionSn, leg.getReplayRefLegId(), entries.size());
        List<LedgerPeriod> periods = entries.stream()
                .map(item -> new LedgerPeriod(item.getPeriodType(), item.getPeriodId()))
                .distinct()
                .toList();
        AssertUtils.isTrue(periods.size() == 1,
                "原记账计划账期不唯一，ledgerTransactionSn = {}, routeLegId = {}",
                ledgerTransactionSn, leg.getReplayRefLegId());
        return new OriginalPosting(entries, periods.getFirst().periodType(), periods.getFirst().periodId());
    }

    private LedgerSubjectCode resolveSubjectCode(FundsInstructionSpec instruction,
                                                 ResolvedRouteSpec route,
                                                 RouteLegSpec leg,
                                                 RouteNodeSpec node,
                                                 boolean source,
                                                 @Nullable OriginalPosting originalPosting) {
        if (isFeeChargeLeg(route, leg)) {
            return source ? LedgerSubjectCode.AVAILABLE : LedgerSubjectCode.FEE;
        }
        LedgerSubjectCode platformSubjectCode = resolvePlatformSubjectCode(route.getPlatformAccounts(),
                node.getSubjectRef());
        if (platformSubjectCode != null) {
            return platformSubjectCode;
        }
        return switch (route.getEventType()) {
            case TOPUP, TRANSFER -> LedgerSubjectCode.AVAILABLE;
            case PAY -> source ? LedgerSubjectCode.AVAILABLE
                    : requireSubjectCode(instruction.getPayeeLedgerSubjectCode(), "收款账本科目不能为空");
            case REFUND -> originalPosting == null
                    ? source
                    ? requireSubjectCode(instruction.getPayerLedgerSubjectCode(), "退款出资账本科目不能为空")
                    : LedgerSubjectCode.AVAILABLE
                    : resolveOriginalSubjectCode(originalPosting, node.getSubjectRef());
            case FEE_REFUND -> resolveOriginalSubjectCode(originalPosting, node.getSubjectRef());
            case WITHDRAW -> LedgerSubjectCode.FROZEN;
            case FEE_CHARGE -> source ? LedgerSubjectCode.AVAILABLE : LedgerSubjectCode.FEE;
            case AUTHORIZE -> source ? LedgerSubjectCode.AVAILABLE : LedgerSubjectCode.AUTHORIZATION;
            case REVERSAL -> source ? LedgerSubjectCode.AUTHORIZATION : LedgerSubjectCode.AVAILABLE;
            case COMPLETE -> resolveCompletionSubjectCode(leg, node, source);
            case AUTH_REFUND -> source && node.getSubjectRef().getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT
                    ? LedgerSubjectCode.OUTSTANDING : LedgerSubjectCode.AVAILABLE;
            case FREEZE -> source ? LedgerSubjectCode.AVAILABLE : LedgerSubjectCode.FROZEN;
            case UNFREEZE -> source ? LedgerSubjectCode.FROZEN : LedgerSubjectCode.AVAILABLE;
            case BALANCE_ADJUST -> LedgerSubjectCode.AVAILABLE;
            case LIMIT_ADJUST -> resolveLimitAdjustSubjectCode(instruction, source);
            case CLEARING_CONFIRM -> source ? LedgerSubjectCode.CLEARING : LedgerSubjectCode.AVAILABLE;
            case SETTLEMENT_LOCK -> source ? LedgerSubjectCode.AVAILABLE : LedgerSubjectCode.SETTLEMENT;
            case SETTLEMENT_RELEASE -> source ? LedgerSubjectCode.SETTLEMENT : LedgerSubjectCode.AVAILABLE;
            case PAYOUT_SUCCEEDED -> source ? LedgerSubjectCode.SETTLEMENT : LedgerSubjectCode.PREPAYMENT;
            case PAYOUT_FAILED -> source ? LedgerSubjectCode.SETTLEMENT : LedgerSubjectCode.AVAILABLE;
        };
    }

    private LedgerSubjectCode resolveCompletionSubjectCode(RouteLegSpec leg, RouteNodeSpec node, boolean source) {
        if (source) {
            return StringUtils.hasText(leg.getReplayRefLegId())
                    ? LedgerSubjectCode.AUTHORIZATION : LedgerSubjectCode.AVAILABLE;
        }
        return node.getSubjectRef().getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT
                ? LedgerSubjectCode.OUTSTANDING : LedgerSubjectCode.SETTLEMENT;
    }

    private LedgerSubjectCode resolveLimitAdjustSubjectCode(FundsInstructionSpec instruction, boolean source) {
        boolean increase = Boolean.TRUE.equals(instruction.getContextVariables().get(FundsContextVariables.INCREASE));
        return increase == source ? LedgerSubjectCode.LIMIT : LedgerSubjectCode.AVAILABLE;
    }

    private LedgerSubjectCode resolveOriginalSubjectCode(@Nullable OriginalPosting originalPosting,
                                                         SubjectRef subjectRef) {
        AssertUtils.notNull(originalPosting, "回放账务缺少原记账事实");
        List<LedgerSubjectCode> subjectCodes = originalPosting.entries().stream()
                .filter(entry -> entry.getSubjectId().equals(subjectRef.getSubjectId())
                        && entry.getSubjectType().equals(subjectRef.getSubjectType().name()))
                .map(LedgerEntry::getLedgerSubjectCode)
                .distinct()
                .toList();
        AssertUtils.isTrue(subjectCodes.size() == 1,
                "原记账主体科目不存在或不唯一，subjectId = {}, subjectType = {}",
                subjectRef.getSubjectId(), subjectRef.getSubjectType());
        return subjectCodes.getFirst();
    }

    private @Nullable LedgerSubjectCode resolvePlatformSubjectCode(
            @Nullable PlatformAccountsSnapshotSpec platformAccounts,
            SubjectRef subjectRef) {
        if (platformAccounts == null) {
            return null;
        }
        if (sameSubject(platformAccounts.getCashFundingAccount(), subjectRef)) {
            return LedgerSubjectCode.CASH;
        }
        if (sameSubject(platformAccounts.getPrepaymentFundingAccount(), subjectRef)) {
            return LedgerSubjectCode.PREPAYMENT;
        }
        if (sameSubject(platformAccounts.getClearingFundingAccount(), subjectRef)) {
            return LedgerSubjectCode.CLEARING;
        }
        if (sameSubject(platformAccounts.getSettlementFundingAccount(), subjectRef)) {
            return LedgerSubjectCode.SETTLEMENT;
        }
        if (sameSubject(platformAccounts.getFeeFundingAccount(), subjectRef)) {
            return LedgerSubjectCode.FEE;
        }
        return sameSubject(platformAccounts.getAdjustmentFundingAccount(), subjectRef)
                ? LedgerSubjectCode.ADJUSTMENT : null;
    }

    private boolean sameSubject(@Nullable SubjectRef expected, SubjectRef actual) {
        return expected != null
                && expected.getSubjectId().equals(actual.getSubjectId())
                && expected.getSubjectType() == actual.getSubjectType();
    }

    private LedgerSubjectCode requireSubjectCode(@Nullable LedgerSubjectCode subjectCode, String message) {
        AssertUtils.notNull(subjectCode, message);
        return subjectCode;
    }

    private LedgerBalanceEffectType resolveBalanceEffectType(FundsInstructionSpec instruction,
                                                             ResolvedRouteSpec route,
                                                             RouteLegSpec leg) {
        if (isFeeChargeLeg(route, leg)) {
            return LedgerBalanceEffectType.CONSUME;
        }
        return switch (route.getEventType()) {
            case TOPUP, BALANCE_ADJUST, LIMIT_ADJUST -> resolveAdjustmentEffect(instruction, route.getEventType());
            case TRANSFER, PAY, FEE_CHARGE, COMPLETE -> LedgerBalanceEffectType.CONSUME;
            case REFUND, FEE_REFUND, AUTH_REFUND, PAYOUT_FAILED -> LedgerBalanceEffectType.RESTORE;
            case WITHDRAW, PAYOUT_SUCCEEDED -> leg.getLegType() == RouteLegType.EXTERNAL_OUT
                    ? LedgerBalanceEffectType.DECREASE : LedgerBalanceEffectType.CONSUME;
            case AUTHORIZE, FREEZE -> LedgerBalanceEffectType.HOLD;
            case REVERSAL, UNFREEZE, CLEARING_CONFIRM, SETTLEMENT_RELEASE -> LedgerBalanceEffectType.RELEASE;
            case SETTLEMENT_LOCK -> LedgerBalanceEffectType.CONSUME;
        };
    }

    private LedgerBalanceEffectType resolveAdjustmentEffect(FundsInstructionSpec instruction,
                                                            FundsTransactionEventType eventType) {
        if (eventType == FundsTransactionEventType.TOPUP) {
            return LedgerBalanceEffectType.INCREASE;
        }
        Object increase = instruction.getContextVariables().get(FundsContextVariables.INCREASE);
        AssertUtils.isTrue(increase instanceof Boolean, "调账方向 increase 不能为空");
        return Boolean.TRUE.equals(increase)
                ? LedgerBalanceEffectType.INCREASE : LedgerBalanceEffectType.DECREASE;
    }

    private LedgerPhaseCode resolvePhaseCode(ResolvedRouteSpec route, RouteLegSpec leg) {
        if (isFeeChargeLeg(route, leg)) {
            return LedgerPhaseCode.FEE;
        }
        return switch (route.getEventType()) {
            case TOPUP -> leg.getLegType() == RouteLegType.EXTERNAL_IN
                    ? LedgerPhaseCode.FUND_IN : LedgerPhaseCode.SETTLEMENT;
            case TRANSFER -> LedgerPhaseCode.TRANSFER;
            case PAY -> LedgerPhaseCode.SETTLEMENT;
            case REFUND, FEE_REFUND, AUTH_REFUND, PAYOUT_FAILED -> LedgerPhaseCode.REFUND;
            case WITHDRAW, PAYOUT_SUCCEEDED -> leg.getLegType() == RouteLegType.EXTERNAL_OUT
                    ? LedgerPhaseCode.FUND_OUT : LedgerPhaseCode.SETTLEMENT;
            case FEE_CHARGE -> LedgerPhaseCode.FEE;
            case AUTHORIZE -> LedgerPhaseCode.AUTHORIZATION;
            case REVERSAL -> LedgerPhaseCode.REVERSAL;
            case COMPLETE -> LedgerPhaseCode.COMPLETION;
            case FREEZE -> LedgerPhaseCode.FREEZE;
            case UNFREEZE -> LedgerPhaseCode.UNFREEZE;
            case BALANCE_ADJUST, LIMIT_ADJUST -> LedgerPhaseCode.ADJUSTMENT;
            case CLEARING_CONFIRM, SETTLEMENT_LOCK, SETTLEMENT_RELEASE -> LedgerPhaseCode.SETTLEMENT;
        };
    }

    private LedgerBalanceConstraintType resolveSourceConstraint(ResolvedRouteSpec route,
                                                                RouteLegSpec leg,
                                                                LedgerBalanceEffectType balanceEffectType) {
        if (isFeeChargeLeg(route, leg)) {
            return LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE;
        }
        return switch (route.getEventType()) {
            case TOPUP, REFUND, FEE_REFUND, AUTH_REFUND -> LedgerBalanceConstraintType.PROFILE_DEFAULT;
            case WITHDRAW, PAYOUT_SUCCEEDED -> leg.getLegType() == RouteLegType.EXTERNAL_OUT
                    ? LedgerBalanceConstraintType.PROFILE_DEFAULT
                    : LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE;
            case BALANCE_ADJUST, LIMIT_ADJUST -> balanceEffectType == LedgerBalanceEffectType.DECREASE
                    ? LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE
                    : LedgerBalanceConstraintType.PROFILE_DEFAULT;
            default -> LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE;
        };
    }

    private MovementDirection adjustmentDirection(LedgerBalanceEffectType balanceEffectType) {
        return switch (balanceEffectType) {
            case INCREASE -> MovementDirection.INCREASE;
            case DECREASE -> MovementDirection.DECREASE;
            default -> throw new IllegalArgumentException("adjustment only supports INCREASE or DECREASE effect");
        };
    }

    private LedgerPostingIntentType resolveIntent(ResolvedRouteSpec resolvedRoute, RouteLegSpec leg) {
        if (isFeeChargeLeg(resolvedRoute, leg)) {
            return LedgerPostingIntentType.FEE;
        }
        FundsTransactionEventType eventType = resolvedRoute.getEventType();
        return switch (eventType) {
            case AUTHORIZE -> LedgerPostingIntentType.AUTHORIZATION;
            case REVERSAL -> LedgerPostingIntentType.AUTHORIZATION_REVERSAL;
            case COMPLETE -> LedgerPostingIntentType.AUTHORIZATION_COMPLETION;
            case AUTH_REFUND, REFUND -> LedgerPostingIntentType.REFUND;
            case FEE_REFUND -> LedgerPostingIntentType.FEE_REFUND;
            case FREEZE -> LedgerPostingIntentType.HOLD;
            case UNFREEZE -> LedgerPostingIntentType.RELEASE;
            case BALANCE_ADJUST, LIMIT_ADJUST -> LedgerPostingIntentType.ADJUSTMENT;
            case WITHDRAW -> LedgerPostingIntentType.WITHDRAWAL;
            case CLEARING_CONFIRM -> LedgerPostingIntentType.SETTLEMENT;
            case SETTLEMENT_LOCK, SETTLEMENT_RELEASE -> LedgerPostingIntentType.SETTLEMENT;
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
            case BALANCE_CONTROL -> throw new IllegalArgumentException(
                    "balance-control transaction type requires FREEZE or UNFREEZE event");
            case ADJUSTMENT -> LedgerPostingIntentType.ADJUSTMENT;
        };
    }

    private boolean isFeeChargeLeg(ResolvedRouteSpec route, RouteLegSpec leg) {
        PlatformAccountsSnapshotSpec platformAccounts = route.getPlatformAccounts();
        return route.getEventType() != FundsTransactionEventType.FEE_REFUND
                && FEE_ROUTE_LEG_ID.equals(leg.getLegId())
                && platformAccounts != null
                && sameSubject(platformAccounts.getFeeFundingAccount(), leg.getTargetNode().getSubjectRef());
    }

    private LedgerPostingScope resolvePostingScope(LedgerPostingIntentType intent, LedgerPhaseCode phaseCode) {
        return switch (intent) {
            case TOPUP, WITHDRAWAL -> LedgerPostingScope.PLATFORM_EXTERNAL;
            case FEE, FEE_REFUND, FEE_REVERSAL -> LedgerPostingScope.FEE;
            case ADJUSTMENT -> LedgerPostingScope.ADJUSTMENT;
            case HOLD -> resolveHoldPostingScope(phaseCode);
            case RELEASE -> LedgerPostingScope.CONTROL_RELEASE;
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

    private record PostingSemantics(LedgerSubjectCode sourceSubjectCode,
                                    LedgerSubjectCode targetSubjectCode,
                                    AccountBalancePeriodType periodType,
                                    String periodId,
                                    LedgerBalanceEffectType balanceEffectType,
                                    LedgerPhaseCode phaseCode,
                                    LedgerBalanceConstraintType sourceConstraint,
                                    MovementDirection sourceDirection,
                                    MovementDirection targetDirection) {
    }

    private record OriginalPosting(List<LedgerEntry> entries,
                                   AccountBalancePeriodType periodType,
                                   String periodId) {
    }

    private record LedgerPeriod(AccountBalancePeriodType periodType, String periodId) {
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

}

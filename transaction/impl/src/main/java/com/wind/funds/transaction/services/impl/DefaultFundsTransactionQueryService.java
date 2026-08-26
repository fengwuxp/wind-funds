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
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.mapstruct.FundsTransactionConverter;
import com.wind.funds.transaction.model.dto.FundsActionFactDTO;
import com.wind.funds.transaction.model.dto.FundsActionFactRef;
import com.wind.funds.transaction.model.dto.FundsActionRecordedEvidenceDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.query.FundsActionFactQuery;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.services.FundsActionRecordedEvidenceQueryService;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 默认资金交易事实查询服务。
 *
 * @author Codex
 * @date 2026-05-12
 */
@Service
@AllArgsConstructor
public class DefaultFundsTransactionQueryService
        implements FundsTransactionQueryService, FundsActionRecordedEvidenceQueryService {

    private static final String ACTION_IDENTITY_MARKER = ":primary:";

    private static final String RECOVERY_ACTION_IDENTITY_MARKER = ":recovery:";

    private static final String AUTHORIZATION_ACTION_IDENTITY_MARKER = ":authorize:";

    private static final String COMPLETE_ACTION_IDENTITY_MARKER = ":complete:";

    private static final String RELEASE_ACTION_IDENTITY_PREFIX = "release:v1:";

    private static final String RELEASE_INTENT_REF_PREFIX = "release-intent:v1:";

    private static final String RELEASE_ATTEMPT_REF_PREFIX = "release-attempt:v1:";

    private static final String ACTION_KIND_PRIMARY = "primary";

    private static final String ACTION_KIND_RECOVERY = "recovery/adjustment";

    private static final String ACTION_KIND_AUTHORIZE = "authorize";

    private static final String ACTION_KIND_COMPLETE = "complete";

    private static final String ACTION_KIND_RELEASE = "release";

    private static final String DOMAIN_OUTCOME_OWNER = "funds-transaction";

    private static final String OUTCOME_SUCCEEDED = "succeeded";

    private static final String OUTCOME_FAILED = "failed";

    private static final String OUTCOME_REJECTED = "rejected";

    private static final String EFFECT_PROVEN_FULL = "proven-full";

    private static final String EFFECT_PROVEN_ZERO = "proven-zero";

    private static final String PROVENANCE_EXECUTION = "execution";

    private static final String PROVENANCE_REPLAYED_ORIGINAL_ROUTE = "replayed-original-route";

    private static final String ORIGINAL_FACT_TYPE = "funds-action";

    private static final String ORIGINAL_FACT_RELATION = "reverses-confirmed-effect";

    private static final String AUTHORIZATION_COMPLETE_ORIGINAL_FACT_RELATION = "consumes-authorized-effect";

    private static final String AUTHORIZATION_RELEASE_ORIGINAL_FACT_RELATION = "releases-authorized-effect";

    private static final String LEDGER_POSTING_REJECTED_ERROR_CODE = "LEDGER_POSTING_REJECTED";

    private static final String SEMANTIC_DIGEST_ALGORITHM = "SHA-256";

    private static final String SEMANTIC_DIGEST_DOMAIN = "transaction.action.pay.projection";

    private static final String SEMANTIC_DIGEST_FIELDS_VERSION = "transaction.action.pay.projection.v1";

    private static final String RECOVERY_SEMANTIC_DIGEST_DOMAIN = "transaction.action.recovery.projection";

    private static final String RECOVERY_SEMANTIC_DIGEST_FIELDS_VERSION =
            "transaction.action.recovery.projection.v1";

    private static final String AUTHORIZATION_SEMANTIC_DIGEST_DOMAIN =
            "transaction.action.authorization.projection";

    private static final String AUTHORIZATION_SEMANTIC_DIGEST_FIELDS_VERSION =
            "transaction.action.authorization.projection.v1";

    private static final String COMPLETE_SEMANTIC_DIGEST_DOMAIN =
            "transaction.action.complete.projection";

    private static final String COMPLETE_SEMANTIC_DIGEST_FIELDS_VERSION =
            "transaction.action.complete.projection.v1";

    private static final String RELEASE_SEMANTIC_DIGEST_DOMAIN =
            "transaction.action.release.projection";

    private static final String RELEASE_SEMANTIC_DIGEST_FIELDS_VERSION =
            "transaction.action.release.projection.v1";

    private static final String ROUTE_SNAPSHOT_OWNER_NAMESPACE = "funds-route-snapshot";

    private static final String RECORDED_REFERENCE_DIGEST_DOMAIN = "transaction.action.recorded-reference";

    private static final String RECORDED_REFERENCE_DIGEST_FIELDS_VERSION =
            "transaction.action.recorded-reference.v1";

    private final FundsTransactionMapper fundsTransactionMapper;

    private final FundsTransactionDetailMapper fundsTransactionDetailMapper;

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @Override
    public @NonNull List<FundsActionFactDTO> queryFundsActionFacts(@NonNull FundsActionFactQuery query) {
        AssertUtils.notNull(query.getTenantId(), "资金动作事实租户 ID 不能为空");
        AssertUtils.hasText(query.getBusinessScene(), "资金动作事实业务场景不能为空");
        AssertUtils.hasText(query.getBusinessSn(), "资金动作事实业务流水不能为空");
        List<FundsTransaction> transactions = queryTransactionsByBusiness(
                query.getTenantId(), query.getBusinessScene(), query.getBusinessSn());
        if (transactions.size() > 1) {
            return List.of();
        }
        if (transactions.isEmpty()) {
            return queryAuthorizationSuccessorActionFacts(query);
        }
        FundsTransaction transaction = transactions.getFirst();
        List<FundsTransactionDetail> details = queryTransactionDetailsByBusiness(
                query.getTenantId(), query.getBusinessScene(), query.getBusinessSn());
        return containsOnlyMainActionDetails(transaction, details) ? toActionFacts(transaction) : List.of();
    }

    @Override
    public @NonNull Optional<FundsActionFactDTO> findFundsActionFact(@NonNull FundsActionFactRef ref) {
        AssertUtils.notNull(ref.getTenantId(), "资金动作事实引用租户 ID 不能为空");
        AssertUtils.hasText(ref.getIdentity(), "资金动作事实稳定身份不能为空");
        ReleaseActionIdentity releaseIdentity = parseReleaseActionIdentity(ref.getIdentity());
        if (releaseIdentity != null) {
            FundsTransaction transaction = findTransactionBySnNullable(
                    ref.getTenantId(), releaseIdentity.authorizationSn());
            return transaction == null ? Optional.empty()
                    : findAuthorizationReleaseActionFact(transaction, ref, releaseIdentity);
        }
        int markerIndex = actionIdentityMarkerIndex(ref.getIdentity());
        if (markerIndex <= 0) {
            return Optional.empty();
        }
        String transactionSn = ref.getIdentity().substring(0, markerIndex);
        FundsTransaction transaction = findTransactionBySnNullable(ref.getTenantId(), transactionSn);
        if (transaction == null) {
            return Optional.empty();
        }
        if (ref.getIdentity().startsWith(transactionSn + COMPLETE_ACTION_IDENTITY_MARKER)) {
            return findAuthorizationCompleteActionFact(transaction, ref);
        }
        if (!hasUnambiguousMainActionBusinessKey(transaction)) {
            return Optional.empty();
        }
        return toActionFacts(transaction).stream()
                .filter(actionFact -> actionFact.getIdentity().equals(ref))
                .findFirst();
    }

    @Override
    public @NonNull Optional<FundsActionRecordedEvidenceDTO> findRecordedEvidence(
            @NonNull FundsActionFactRef actionFactRef) {
        AssertUtils.notNull(actionFactRef.getTenantId(), "资金动作事实引用租户 ID 不能为空");
        AssertUtils.hasText(actionFactRef.getIdentity(), "资金动作事实稳定身份不能为空");
        int markerIndex = actionFactRef.getIdentity().lastIndexOf(ACTION_IDENTITY_MARKER);
        if (markerIndex <= 0) {
            return Optional.empty();
        }
        String transactionSn = actionFactRef.getIdentity().substring(0, markerIndex);
        if (!actionFactRef.getIdentity().equals(actionIdentity(transactionSn, 0))) {
            return Optional.empty();
        }
        FundsTransaction transaction = findTransactionBySnNullable(actionFactRef.getTenantId(), transactionSn);
        PayActionProjection projection = transaction == null ? null : verifiedPayActionProjection(transaction);
        if (projection == null || !projection.succeeded()) {
            return Optional.empty();
        }
        FundsActionFactDTO actionFact = toActionFact(
                transaction, projection.routeSnapshot(), projection.principal(), 0, true);
        if (!actionFactRef.equals(actionFact.getIdentity())) {
            return Optional.empty();
        }
        List<FundsActionRecordedEvidenceDTO.RecordedSiblingRef> siblings = projection.matchedDetails().stream()
                .sorted(java.util.Comparator.comparing((FundsTransactionDetail detail) ->
                                detail.getParticipantRole().name())
                        .thenComparing(FundsTransactionDetail::getSn))
                .map(this::toRecordedSiblingRef)
                .toList();
        String ledgerTransactionSn = projection.principal().getLedgerTransactionSn();
        FundsActionFactDTO.SemanticDigest digest = recordedReferenceDigest(
                actionFact, siblings, ledgerTransactionSn);
        return Optional.of(new FundsActionRecordedEvidenceDTO(
                actionFact, siblings, ledgerTransactionSn, digest));
    }

    private FundsActionRecordedEvidenceDTO.RecordedSiblingRef toRecordedSiblingRef(
            FundsTransactionDetail detail) {
        return new FundsActionRecordedEvidenceDTO.RecordedSiblingRef(
                detail.getSn(),
                detail.getParticipantRole(),
                detail.getSubjectId(),
                detail.getSubjectType(),
                Money.immutable(detail.getAmount(), detail.getCurrency()),
                detail.getLedgerTransactionSn());
    }

    private FundsActionFactDTO.SemanticDigest recordedReferenceDigest(
            FundsActionFactDTO actionFact,
            List<FundsActionRecordedEvidenceDTO.RecordedSiblingRef> siblings,
            String ledgerTransactionSn) {
        Map<String, Object> values = new TreeMap<>();
        values.put("actionSemanticDigest", actionFact.getSemanticDigest().getValue());
        values.put("recordedLedgerTransactionSn", ledgerTransactionSn);
        values.put("matchedSiblings", siblings.stream().map(sibling -> Map.of(
                "detailSn", sibling.getDetailSn(),
                "participantRole", sibling.getParticipantRole().name(),
                "subjectId", sibling.getSubjectId(),
                "subjectType", sibling.getSubjectType(),
                "amount", sibling.getMoney().getAmount(),
                "currency", sibling.getMoney().getCurrency().name(),
                "recordedLedgerTransactionSn", sibling.getRecordedLedgerTransactionSn())).toList());
        return new FundsActionFactDTO.SemanticDigest(
                SEMANTIC_DIGEST_ALGORITHM,
                FundsStableHashSupport.sha256CanonicalJson(RECORDED_REFERENCE_DIGEST_DOMAIN, values),
                RECORDED_REFERENCE_DIGEST_FIELDS_VERSION);
    }

    @Override
    public @NonNull Optional<FundsTransactionDTO> findFundsTransactionBySn(@NonNull Long tenantId,
                                                                          @NonNull String transactionSn) {
        AssertUtils.notNull(tenantId, "资金交易租户 ID 不能为空");
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        return Optional.ofNullable(findTransactionBySnNullable(tenantId, transactionSn))
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
    public @NonNull List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull Long tenantId,
                                                                                 @NonNull String transactionSn) {
        AssertUtils.notNull(tenantId, "资金交易明细租户 ID 不能为空");
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.transactionSn.eq(transactionSn))
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper)
                .stream()
                .map(FundsTransactionConverter.INSTANCE::convertToFundsTransactionDetailDTO)
                .toList();
    }

    @Override
    public boolean hasConsumedReplayLeg(@NonNull Long tenantId,
                                        @NonNull String referenceTransactionSn,
                                        @NonNull FundsTransactionEventType eventType,
                                        @NonNull String replayRefLegId) {
        AssertUtils.notNull(tenantId, "RouteReplay 租户 ID 不能为空");
        AssertUtils.hasText(referenceTransactionSn, "原资金交易流水号不能为空");
        AssertUtils.notNull(eventType, "资金交易事件类型不能为空");
        AssertUtils.hasText(replayRefLegId, "RouteReplay 原 legId 不能为空");
        return queryConsumedReplayDetails(tenantId, referenceTransactionSn, eventType)
                .stream()
                .anyMatch(detail -> isReplayLegConsumed(detail, replayRefLegId));
    }

    @Override
    public @NonNull Money sumConsumedReplayLegAmount(@NonNull Long tenantId,
                                                     @NonNull String referenceTransactionSn,
                                                     @NonNull FundsTransactionEventType eventType,
                                                     @NonNull String replayRefLegId,
                                                     @NonNull CurrencyIsoCode currency) {
        return sumConsumedReplayLegAmount(
                tenantId, referenceTransactionSn, eventType, replayRefLegId, currency, null, null);
    }

    @Override
    public @NonNull Money sumConsumedReplayLegAmount(@NonNull Long tenantId,
                                                     @NonNull String referenceTransactionSn,
                                                     @NonNull FundsTransactionEventType eventType,
                                                     @NonNull String replayRefLegId,
                                                     @NonNull CurrencyIsoCode currency,
                                                     @Nullable String excludedBusinessScene,
                                                     @Nullable String excludedBusinessSn) {
        AssertUtils.notNull(tenantId, "RouteReplay 租户 ID 不能为空");
        AssertUtils.hasText(referenceTransactionSn, "原资金交易流水号不能为空");
        AssertUtils.notNull(eventType, "资金交易事件类型不能为空");
        AssertUtils.hasText(replayRefLegId, "RouteReplay 原 legId 不能为空");
        AssertUtils.notNull(currency, "RouteReplay 币种不能为空");
        if (isFreezeOrderUnfreezeConsumption(eventType, replayRefLegId)) {
            return sumFrozenOrderReleasedAmount(tenantId, referenceTransactionSn, currency,
                    excludedBusinessScene, excludedBusinessSn);
        }
        Map<String, Long> consumedAmounts = new LinkedHashMap<>();
        for (FundsTransactionDetail detail : queryConsumedReplayDetails(
                tenantId, referenceTransactionSn, eventType)) {
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
    public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull Long tenantId,
                                                                                 @NonNull String transactionSn) {
        AssertUtils.notNull(tenantId, "RouteSnapshot 租户 ID 不能为空");
        AssertUtils.hasText(transactionSn, "资金交易流水号不能为空");
        FundsTransaction transaction = findTransactionBySnNullable(tenantId, transactionSn);
        if (transaction == null || !StringUtils.hasText(transaction.getRouteSnapshot())) {
            return Optional.empty();
        }
        return Optional.of(RouteSnapshotJsonSupport.parseRouteSnapshot(
                transaction.getRouteSnapshot(), transaction.getGmtCreate()));
    }

    @Override
    public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull Long tenantId,
                                                                                 @NonNull String freezeOrderSn) {
        AssertUtils.notNull(tenantId, "冻结单 RouteSnapshot 租户 ID 不能为空");
        AssertUtils.hasText(freezeOrderSn, "冻结单号不能为空");
        FundsFrozenOrder order = findFreezeOrderBySnNullable(tenantId, freezeOrderSn);
        if (order == null) {
            return Optional.empty();
        }
        Optional<RouteSnapshotSpec> frozenOrderSnapshot = findRouteSnapshotInFreezeOrder(order);
        if (frozenOrderSnapshot.isPresent()) {
            return frozenOrderSnapshot;
        }
        return StringUtils.hasText(order.getTransactionSn())
                ? findRouteSnapshotByTransactionSn(tenantId, order.getTransactionSn())
                : Optional.empty();
    }

    private FundsTransaction findTransactionBySnNullable(Long tenantId, String sn) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(sn));
        return fundsTransactionMapper.selectOneByQuery(wrapper);
    }

    private List<FundsTransaction> queryTransactionsByBusiness(Long tenantId,
                                                               String businessScene,
                                                               String businessSn) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.businessScene.eq(businessScene))
                .and(ref.businessSn.eq(businessSn))
                .orderBy(ref.id.asc());
        return fundsTransactionMapper.selectListByQuery(wrapper);
    }

    private boolean containsOnlyMainActionDetails(FundsTransaction transaction,
                                                  List<FundsTransactionDetail> details) {
        FundsTransactionEventType expectedEventType = mainActionEventType(transaction);
        return expectedEventType != null && details.stream().allMatch(detail ->
                Objects.equals(transaction.getSn(), detail.getTransactionSn())
                        && detail.getEventType() == expectedEventType);
    }

    private boolean hasUnambiguousMainActionBusinessKey(FundsTransaction transaction) {
        List<FundsTransaction> transactions = queryTransactionsByBusiness(
                transaction.getTenantId(), transaction.getBusinessScene(), transaction.getBusinessSn());
        if (transactions.size() != 1 || !Objects.equals(transactions.getFirst().getSn(), transaction.getSn())) {
            return false;
        }
        return containsOnlyMainActionDetails(transaction, queryTransactionDetailsByBusiness(
                transaction.getTenantId(), transaction.getBusinessScene(), transaction.getBusinessSn()));
    }

    private @Nullable FundsTransactionEventType mainActionEventType(FundsTransaction transaction) {
        if (transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION
                && transaction.getTransactionType() == DefaultFundsTransactionType.PAY) {
            return FundsTransactionEventType.AUTHORIZE;
        }
        if (transaction.getTransactionType() == DefaultFundsTransactionType.PAY) {
            return FundsTransactionEventType.PAY;
        }
        return transaction.getTransactionType() == DefaultFundsTransactionType.REFUND
                ? FundsTransactionEventType.REFUND : null;
    }

    private List<FundsActionFactDTO> queryAuthorizationSuccessorActionFacts(FundsActionFactQuery query) {
        AuthorizationSuccessorActionGroup group = resolveAuthorizationSuccessorActionGroup(
                query.getTenantId(), query.getBusinessScene(), query.getBusinessSn(), null);
        if (group == null) {
            return List.of();
        }
        return group.eventType() == FundsTransactionEventType.COMPLETE
                ? toAuthorizationCompleteActionFacts(
                group.transaction(), query.getBusinessScene(), query.getBusinessSn())
                : toAuthorizationReleaseActionFacts(
                group.transaction(), query.getBusinessScene(), query.getBusinessSn());
    }

    private Optional<FundsActionFactDTO> findAuthorizationCompleteActionFact(FundsTransaction transaction,
                                                                              FundsActionFactRef ref) {
        String prefix = transaction.getSn() + COMPLETE_ACTION_IDENTITY_MARKER;
        String actionKey = ref.getIdentity().substring(prefix.length());
        int separator = actionKey.indexOf(':');
        if (separator <= 0 || separator == actionKey.length() - 1) {
            return Optional.empty();
        }
        String businessScene = actionKey.substring(0, separator);
        String businessSn = actionKey.substring(separator + 1);
        AuthorizationSuccessorActionGroup group = resolveAuthorizationSuccessorActionGroup(
                transaction.getTenantId(), businessScene, businessSn, transaction.getSn());
        if (group == null || group.eventType() != FundsTransactionEventType.COMPLETE) {
            return Optional.empty();
        }
        return toAuthorizationCompleteActionFacts(transaction, businessScene, businessSn).stream()
                .filter(actionFact -> actionFact.getIdentity().equals(ref))
                .findFirst();
    }

    private Optional<FundsActionFactDTO> findAuthorizationReleaseActionFact(
            FundsTransaction transaction,
            FundsActionFactRef ref,
            ReleaseActionIdentity identity) {
        AuthorizationSuccessorActionGroup group = resolveAuthorizationSuccessorActionGroup(
                transaction.getTenantId(), identity.businessScene(), identity.businessSn(), transaction.getSn());
        if (group == null || group.eventType() != FundsTransactionEventType.REVERSAL) {
            return Optional.empty();
        }
        return toAuthorizationReleaseActionFacts(
                transaction, identity.businessScene(), identity.businessSn()).stream()
                .filter(actionFact -> actionFact.getIdentity().equals(ref))
                .findFirst();
    }

    private @Nullable AuthorizationSuccessorActionGroup resolveAuthorizationSuccessorActionGroup(
            Long tenantId,
            String businessScene,
            String businessSn,
            @Nullable String expectedTransactionSn) {
        if (!queryTransactionsByBusiness(tenantId, businessScene, businessSn).isEmpty()) {
            return null;
        }
        List<FundsTransactionDetail> details = queryTransactionDetailsByBusiness(
                tenantId, businessScene, businessSn);
        if (details.isEmpty()) {
            return null;
        }
        FundsTransactionDetail first = details.getFirst();
        String transactionSn = first.getTransactionSn();
        FundsTransactionEventType eventType = first.getEventType();
        if (!StringUtils.hasText(transactionSn)
                || (expectedTransactionSn != null && !expectedTransactionSn.equals(transactionSn))
                || (eventType != FundsTransactionEventType.COMPLETE
                && eventType != FundsTransactionEventType.REVERSAL)
                || details.stream().anyMatch(detail -> !Objects.equals(transactionSn, detail.getTransactionSn())
                        || detail.getEventType() != eventType)) {
            return null;
        }
        FundsTransaction transaction = findTransactionBySnNullable(tenantId, transactionSn);
        return transaction == null ? null : new AuthorizationSuccessorActionGroup(transaction, eventType);
    }

    private List<FundsTransactionDetail> queryTransactionDetailsByBusiness(Long tenantId,
                                                                            String businessScene,
                                                                            String businessSn) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.businessScene.eq(businessScene))
                .and(ref.businessSn.eq(businessSn))
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper);
    }

    private List<FundsActionFactDTO> toActionFacts(FundsTransaction transaction) {
        if (transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION
                && transaction.getTransactionType() == DefaultFundsTransactionType.PAY) {
            return toAuthorizationActionFacts(transaction);
        }
        return transaction.getTransactionType() == DefaultFundsTransactionType.PAY
                ? toPayActionFacts(transaction)
                : toRecoveryActionFacts(transaction);
    }

    private List<FundsActionFactDTO> toAuthorizationActionFacts(FundsTransaction transaction) {
        AuthorizationActionProjection projection = verifiedAuthorizationActionProjection(transaction);
        if (projection == null) {
            return List.of();
        }
        try {
            return List.of(toAuthorizationActionFact(transaction, projection));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<FundsActionFactDTO> toAuthorizationCompleteActionFacts(FundsTransaction transaction,
                                                                         String businessScene,
                                                                         String businessSn) {
        AuthorizationCompleteActionProjection projection = verifiedAuthorizationCompleteActionProjection(
                transaction, businessScene, businessSn);
        if (projection == null) {
            return List.of();
        }
        try {
            return List.of(toAuthorizationCompleteActionFact(transaction, projection));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<FundsActionFactDTO> toAuthorizationReleaseActionFacts(FundsTransaction transaction,
                                                                        String businessScene,
                                                                        String businessSn) {
        AuthorizationReleaseActionProjection projection = verifiedAuthorizationReleaseActionProjection(
                transaction, businessScene, businessSn);
        if (projection == null) {
            return List.of();
        }
        try {
            return List.of(toAuthorizationReleaseActionFact(transaction, projection));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<FundsActionFactDTO> toPayActionFacts(FundsTransaction transaction) {
        PayActionProjection projection = verifiedPayActionProjection(transaction);
        if (projection == null) {
            return List.of();
        }
        FundsActionFactDTO principalFact = toActionFact(transaction, projection.routeSnapshot(),
                projection.principal(), 0, projection.succeeded());
        return projection.fee() == null
                ? List.of(principalFact)
                : List.of(principalFact, toActionFact(transaction, projection.routeSnapshot(), projection.fee(),
                1, projection.succeeded()));
    }

    private List<FundsActionFactDTO> toRecoveryActionFacts(FundsTransaction transaction) {
        RecoveryActionProjection projection = verifiedRecoveryActionProjection(transaction);
        return projection == null ? List.of() : List.of(toRecoveryActionFact(transaction, projection));
    }

    private @Nullable RecoveryActionProjection verifiedRecoveryActionProjection(FundsTransaction transaction) {
        if (transaction.getTransactionMode() != FundsTransactionMode.DIRECT
                || transaction.getTransactionType() != DefaultFundsTransactionType.REFUND
                || !StringUtils.hasText(transaction.getReferenceTransactionSn())
                || !StringUtils.hasText(transaction.getRouteSnapshot())) {
            return null;
        }
        FundsTransaction originalTransaction = findTransactionBySnNullable(
                transaction.getTenantId(), transaction.getReferenceTransactionSn());
        PayActionProjection originalProjection = originalTransaction == null
                ? null : verifiedPayActionProjection(originalTransaction);
        if (originalProjection == null || !originalProjection.succeeded() || originalProjection.fee() != null) {
            return null;
        }
        RecoveryFactProjection recoveryProjection = verifiedRecoveryFactProjection(
                transaction, originalTransaction, originalProjection);
        if (recoveryProjection == null
                || !matchesRecoveryCumulative(originalTransaction, originalProjection)) {
            return null;
        }
        FundsActionFactDTO originalActionFact = toActionFact(originalTransaction,
                originalProjection.routeSnapshot(), originalProjection.principal(), 0, true);
        return new RecoveryActionProjection(
                recoveryProjection.principal(), originalActionFact, recoveryProjection.succeeded());
    }

    private @Nullable RecoveryFactProjection verifiedRecoveryFactProjection(
            FundsTransaction transaction,
            FundsTransaction originalTransaction,
            PayActionProjection originalProjection) {
        if (transaction.getTransactionMode() != FundsTransactionMode.DIRECT
                || transaction.getTransactionType() != DefaultFundsTransactionType.REFUND
                || !Objects.equals(transaction.getTenantId(), originalTransaction.getTenantId())
                || !Objects.equals(transaction.getReferenceTransactionSn(), originalTransaction.getSn())
                || !StringUtils.hasText(transaction.getRouteSnapshot())) {
            return null;
        }
        RouteSnapshotSpec routeSnapshot = parsePayRouteSnapshot(transaction);
        if (routeSnapshot == null || !matchesRecoveryRoot(transaction, routeSnapshot)) {
            return null;
        }
        List<FundsTransactionDetail> details = queryTransactionDetails(transaction);
        List<FundsTransactionDetail> matchedDetails = matchRouteParticipants(transaction, routeSnapshot, details);
        if (matchedDetails.isEmpty()) {
            return null;
        }
        List<FundsTransactionDetail> principalDetails = matchedDetails.stream()
                .filter(detail -> detail.getParticipantRole() != RouteParticipantRole.PAYEE)
                .filter(detail -> detail.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER)
                .toList();
        List<FundsTransactionDetail> payeeDetails = matchedDetails.stream()
                .filter(detail -> detail.getParticipantRole() == RouteParticipantRole.PAYEE)
                .toList();
        if (principalDetails.size() != 1 || payeeDetails.size() != 1
                || matchedDetails.stream().anyMatch(
                detail -> detail.getParticipantRole() == RouteParticipantRole.FEE_RECEIVER)) {
            return null;
        }
        FundsTransactionDetail principal = principalDetails.getFirst();
        FundsTransactionDetail payee = payeeDetails.getFirst();
        if (!sameMoney(principal, payee)
                || !Objects.equals(transaction.getAmount(), principal.getAmount())
                || transaction.getCurrency() != principal.getCurrency()
                || !matchesRecoveryLeg(routeSnapshot, originalProjection.routeSnapshot(), principal, payee)
                || !matchesRecoveryReferences(matchedDetails, originalTransaction, originalProjection)) {
            return null;
        }
        boolean succeeded = isRecoveryProvenFull(transaction, matchedDetails, principal);
        boolean provenZero = isProvenZero(transaction, matchedDetails);
        if (succeeded == provenZero) {
            return null;
        }
        return new RecoveryFactProjection(principal, succeeded);
    }

    private @Nullable PayActionProjection verifiedPayActionProjection(FundsTransaction transaction) {
        if (transaction.getTransactionMode() != FundsTransactionMode.DIRECT
                || transaction.getTransactionType() != DefaultFundsTransactionType.PAY
                || !StringUtils.hasText(transaction.getRouteSnapshot())) {
            return null;
        }
        RouteSnapshotSpec routeSnapshot = parsePayRouteSnapshot(transaction);
        if (routeSnapshot == null || !matchesPayRoot(transaction, routeSnapshot)) {
            return null;
        }
        List<FundsTransactionDetail> details = queryTransactionDetails(transaction);
        List<FundsTransactionDetail> matchedDetails = matchRouteParticipants(transaction, routeSnapshot, details);
        if (matchedDetails.isEmpty()) {
            return null;
        }
        List<FundsTransactionDetail> principalDetails = matchedDetails.stream()
                .filter(detail -> detail.getParticipantRole() != RouteParticipantRole.PAYEE)
                .filter(detail -> detail.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER)
                .toList();
        List<FundsTransactionDetail> payeeDetails = matchedDetails.stream()
                .filter(detail -> detail.getParticipantRole() == RouteParticipantRole.PAYEE)
                .toList();
        List<FundsTransactionDetail> feeDetails = matchedDetails.stream()
                .filter(detail -> detail.getParticipantRole() == RouteParticipantRole.FEE_RECEIVER)
                .toList();
        if (principalDetails.size() != 1 || payeeDetails.size() != 1 || feeDetails.size() > 1) {
            return null;
        }
        FundsTransactionDetail principal = principalDetails.getFirst();
        FundsTransactionDetail payee = payeeDetails.getFirst();
        FundsTransactionDetail fee = feeDetails.isEmpty() ? null : feeDetails.getFirst();
        if (!sameMoney(principal, payee)
                || transaction.getAmount() == null
                || !Objects.equals(transaction.getAmount(), principal.getAmount())
                || transaction.getCurrency() != principal.getCurrency()
                || !matchesPayLegs(routeSnapshot, principal, payee, fee)) {
            return null;
        }
        boolean succeeded = isProvenFull(transaction, matchedDetails, principal, fee);
        boolean provenZero = isProvenZero(transaction, matchedDetails);
        return succeeded == provenZero ? null
                : new PayActionProjection(routeSnapshot, matchedDetails, principal, fee, succeeded);
    }

    private @Nullable AuthorizationActionProjection verifiedAuthorizationActionProjection(
            FundsTransaction transaction) {
        if (transaction.getTransactionMode() != FundsTransactionMode.AUTHORIZATION
                || transaction.getTransactionType() != DefaultFundsTransactionType.PAY
                || !StringUtils.hasText(transaction.getRouteSnapshot())) {
            return null;
        }
        RouteSnapshotSpec routeSnapshot = parsePayRouteSnapshot(transaction);
        if (routeSnapshot == null || !matchesAuthorizationRoot(transaction, routeSnapshot)) {
            return null;
        }
        List<FundsTransactionDetail> authorizationDetails = queryTransactionDetails(transaction).stream()
                .filter(detail -> detail.getEventType() == FundsTransactionEventType.AUTHORIZE)
                .toList();
        List<FundsTransactionDetail> matchedDetails = matchRouteParticipants(
                transaction, routeSnapshot, authorizationDetails);
        if (matchedDetails.isEmpty() || !matchesAuthorizationResponsibility(routeSnapshot)) {
            return null;
        }
        boolean succeeded = isAuthorizationProvenFull(transaction, matchedDetails);
        boolean rejected = isAuthorizationProvenZero(transaction, routeSnapshot, matchedDetails);
        return succeeded == rejected || succeeded && !matchesAuthorizationLegs(routeSnapshot, matchedDetails) ? null
                : new AuthorizationActionProjection(routeSnapshot, matchedDetails, succeeded);
    }

    private @Nullable AuthorizationCompleteActionProjection verifiedAuthorizationCompleteActionProjection(
            FundsTransaction transaction,
            String businessScene,
            String businessSn) {
        if (!StringUtils.hasText(businessScene) || !StringUtils.hasText(businessSn)) {
            return null;
        }
        AuthorizationActionProjection authorization = verifiedAuthorizationActionProjection(transaction);
        if (authorization == null || !authorization.succeeded()) {
            return null;
        }
        List<FundsTransactionDetail> completeDetails = queryTransactionDetails(transaction).stream()
                .filter(detail -> detail.getEventType() == FundsTransactionEventType.COMPLETE)
                .toList();
        List<FundsTransactionDetail> actionDetails = completeDetails.stream()
                .filter(detail -> businessScene.equals(detail.getBusinessScene()))
                .filter(detail -> businessSn.equals(detail.getBusinessSn()))
                .toList();
        AuthorizationCompleteActionProjection projection = verifiedAuthorizationCompleteGroup(
                transaction, authorization, businessScene, businessSn, actionDetails);
        return projection != null && matchesAuthorizationCompleteCumulative(
                transaction, authorization, completeDetails) ? projection : null;
    }

    private @Nullable AuthorizationCompleteActionProjection verifiedAuthorizationCompleteGroup(
            FundsTransaction transaction,
            AuthorizationActionProjection authorization,
            String businessScene,
            String businessSn,
            List<FundsTransactionDetail> details) {
        if (details.isEmpty() || authorization.routeSnapshot().getPlatformAccounts() == null) {
            return null;
        }
        SubjectRef settlementSubject = authorization.routeSnapshot().getPlatformAccounts()
                .getSettlementFundingAccount();
        FundsTransactionDetail first = details.getFirst();
        if (settlementSubject == null || first.getAmount() == null || first.getAmount() <= 0
                || first.getCurrency() == null) {
            return null;
        }
        Money money = Money.immutable(first.getAmount(), first.getCurrency());
        String authorizationLedgerSn = authorization.details().getFirst().getLedgerTransactionSn();
        String completeLedgerSn = first.getLedgerTransactionSn();
        if (!StringUtils.hasText(authorizationLedgerSn) || !StringUtils.hasText(completeLedgerSn)
                || details.stream().anyMatch(detail -> !matchesAuthorizationCompleteDetail(
                transaction, detail, businessScene, businessSn, money, authorizationLedgerSn, completeLedgerSn))) {
            return null;
        }
        List<FundsTransactionDetail> responsibilityDetails = matchAuthorizationCompleteResponsibilities(
                transaction, authorization.routeSnapshot(), details, money);
        if (responsibilityDetails.isEmpty()) {
            return null;
        }
        List<FundsTransactionDetail> settlementDetails = details.stream()
                .filter(detail -> matchesAuthorizationSuccessorSubject(
                        transaction, detail, settlementSubject,
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, money))
                .toList();
        if (settlementDetails.size() != 1
                || details.size() != responsibilityDetails.size() + 1
                || responsibilityDetails.contains(settlementDetails.getFirst())
                || !matchesAuthorizationCompleteReplay(authorization.routeSnapshot(), responsibilityDetails,
                settlementDetails.getFirst(), money)) {
            return null;
        }
        return new AuthorizationCompleteActionProjection(
                authorization, details, money, businessScene, businessSn, completeLedgerSn);
    }

    private boolean matchesAuthorizationCompleteDetail(FundsTransaction transaction,
                                                       FundsTransactionDetail detail,
                                                       String businessScene,
                                                       String businessSn,
                                                       Money money,
                                                       String authorizationLedgerSn,
                                                       String completeLedgerSn) {
        return Objects.equals(detail.getTenantId(), transaction.getTenantId())
                && Objects.equals(detail.getTransactionSn(), transaction.getSn())
                && Objects.equals(detail.getBusinessScene(), businessScene)
                && Objects.equals(detail.getBusinessSn(), businessSn)
                && detail.getTransactionType() == DefaultFundsTransactionType.PAY
                && detail.getEventType() == FundsTransactionEventType.COMPLETE
                && detail.getFundsEffectType() == FundsEffectType.CONSUME
                && detail.getState() == FundsTransactionDetailState.SUCCEEDED
                && !StringUtils.hasText(detail.getErrorCode())
                && !StringUtils.hasText(detail.getErrorMessage())
                && Objects.equals(detail.getAmount(), money.getAmount())
                && detail.getCurrency() == money.getCurrency()
                && Objects.equals(detail.getReferenceDetailSn(), transaction.getSn())
                && Objects.equals(detail.getReferenceLedgerTransactionSn(), authorizationLedgerSn)
                && Objects.equals(detail.getLedgerTransactionSn(), completeLedgerSn);
    }

    private List<FundsTransactionDetail> matchAuthorizationCompleteResponsibilities(
            FundsTransaction transaction,
            RouteSnapshotSpec routeSnapshot,
            List<FundsTransactionDetail> details,
            Money money) {
        Map<String, FundsTransactionDetail> matches = new LinkedHashMap<>();
        for (RouteParticipantSpec participant : routeSnapshot.getParticipants()) {
            SubjectRef subjectRef = participant.getSubjectRef();
            if (subjectRef == null) {
                return List.of();
            }
            List<FundsTransactionDetail> candidates = details.stream()
                    .filter(detail -> matchesAuthorizationSuccessorSubject(
                            transaction, detail, subjectRef, participant.getParticipantRole(), money))
                    .toList();
            if (candidates.size() != 1
                    || matches.put(candidates.getFirst().getSn(), candidates.getFirst()) != null) {
                return List.of();
            }
        }
        return matches.size() == routeSnapshot.getParticipants().size()
                ? new ArrayList<>(matches.values()) : List.of();
    }

    private boolean matchesAuthorizationSuccessorSubject(FundsTransaction transaction,
                                                         FundsTransactionDetail detail,
                                                         SubjectRef subjectRef,
                                                         RouteParticipantRole role,
                                                         Money money) {
        return Objects.equals(subjectRef.getTenantId(), transaction.getTenantId())
                && Objects.equals(subjectRef.getSubjectId(), detail.getSubjectId())
                && subjectRef.getSubjectType() != null
                && Objects.equals(subjectRef.getSubjectType().name(), detail.getSubjectType())
                && (subjectRef.getCurrency() == null || subjectRef.getCurrency() == detail.getCurrency())
                && detail.getParticipantRole() == role
                && Objects.equals(detail.getAmount(), money.getAmount())
                && detail.getCurrency() == money.getCurrency();
    }

    private boolean matchesAuthorizationCompleteReplay(RouteSnapshotSpec routeSnapshot,
                                                       List<FundsTransactionDetail> responsibilityDetails,
                                                       FundsTransactionDetail settlementDetail,
                                                       Money money) {
        if (routeSnapshot.getLegs().size() != responsibilityDetails.size()) {
            return false;
        }
        Map<String, Map<String, Long>> expectedReplayAmounts = new LinkedHashMap<>();
        for (RouteLegSpec leg : routeSnapshot.getLegs()) {
            List<FundsTransactionDetail> sourceDetails = responsibilityDetails.stream()
                    .filter(detail -> matchesNode(leg.getTargetNode(), detail))
                    .toList();
            if (sourceDetails.size() != 1 || !validAuthorizationSuccessorMoney(money, leg.getAmount())) {
                return false;
            }
            FundsTransactionDetail sourceDetail = sourceDetails.getFirst();
            expectedReplayAmounts.computeIfAbsent(sourceDetail.getSn(), ignored -> new LinkedHashMap<>())
                    .put(leg.getLegId(), money.getAmount());
            FundsTransactionDetail targetDetail = leg.getSourceNode().getSubjectRef().getSubjectType()
                    == FundsSubjectType.CREDIT_ACCOUNT ? sourceDetail : settlementDetail;
            expectedReplayAmounts.computeIfAbsent(targetDetail.getSn(), ignored -> new LinkedHashMap<>())
                    .put(leg.getLegId(), money.getAmount());
        }
        if (expectedReplayAmounts.size() != responsibilityDetails.size() + 1) {
            return false;
        }
        return expectedReplayAmounts.entrySet().stream().allMatch(entry -> {
            FundsTransactionDetail detail = detailsBySn(responsibilityDetails, settlementDetail, entry.getKey());
            return detail != null && matchesConsumedReplayLegs(detail, entry.getValue());
        });
    }

    private @Nullable FundsTransactionDetail detailsBySn(List<FundsTransactionDetail> responsibilityDetails,
                                                         FundsTransactionDetail settlementDetail,
                                                         String detailSn) {
        if (Objects.equals(settlementDetail.getSn(), detailSn)) {
            return settlementDetail;
        }
        return responsibilityDetails.stream()
                .filter(detail -> Objects.equals(detail.getSn(), detailSn))
                .findFirst()
                .orElse(null);
    }

    private boolean validAuthorizationSuccessorMoney(Money successorMoney, Money authorizationMoney) {
        return authorizationMoney != null
                && successorMoney.getAmount() > 0
                && successorMoney.getCurrency() == authorizationMoney.getCurrency()
                && successorMoney.getAmount() <= authorizationMoney.getAmount();
    }

    private boolean matchesConsumedReplayLegs(FundsTransactionDetail detail, Map<String, Long> expectedAmounts) {
        if (!StringUtils.hasText(detail.getContextVariables())) {
            return false;
        }
        try {
            Map<String, Object> values = parseContextVariables(detail.getContextVariables());
            if (FundsAuthorizationTransactionCompleteRequest.COMPLETION_MODE_FORCE.equalsIgnoreCase(
                    Objects.toString(values.get(FundsInstructionContextKeys.COMPLETION_MODE), null))) {
                return false;
            }
            Object legIdsValue = values.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_IDS);
            Object amountsValue = values.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS);
            if (!(legIdsValue instanceof List<?> legIds) || !(amountsValue instanceof Map<?, ?> amounts)
                    || legIds.size() != expectedAmounts.size() || amounts.size() != expectedAmounts.size()) {
                return false;
            }
            return expectedAmounts.entrySet().stream().allMatch(entry -> legIds.contains(entry.getKey())
                    && amounts.get(entry.getKey()) instanceof Number amount
                    && matchesExactAmount(amount, entry.getValue()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean matchesExactAmount(Number actual, long expected) {
        try {
            return new BigDecimal(actual.toString()).compareTo(BigDecimal.valueOf(expected)) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean matchesAuthorizationCompleteCumulative(
            FundsTransaction transaction,
            AuthorizationActionProjection authorization,
            List<FundsTransactionDetail> completeDetails) {
        Long authorizedAmount = transaction.getAuthorizedAmount();
        Long completedAmount = transaction.getCompletedAmount();
        Long reversedAmount = transaction.getReversedAmount();
        if (authorizedAmount == null || authorizedAmount <= 0 || completedAmount == null || completedAmount < 0
                || reversedAmount == null || reversedAmount < 0) {
            return false;
        }
        try {
            if (Math.addExact(completedAmount, reversedAmount) > authorizedAmount) {
                return false;
            }
        } catch (ArithmeticException exception) {
            return false;
        }
        Map<AuthorizationSuccessorActionKey, List<FundsTransactionDetail>> groups = new LinkedHashMap<>();
        for (FundsTransactionDetail detail : completeDetails) {
            groups.computeIfAbsent(new AuthorizationSuccessorActionKey(
                    detail.getBusinessScene(), detail.getBusinessSn()), ignored -> new ArrayList<>()).add(detail);
        }
        long verifiedCompleted = 0L;
        for (Map.Entry<AuthorizationSuccessorActionKey, List<FundsTransactionDetail>> entry : groups.entrySet()) {
            List<FundsTransactionDetail> group = entry.getValue();
            long succeededCount = group.stream()
                    .filter(detail -> detail.getState() == FundsTransactionDetailState.SUCCEEDED)
                    .count();
            if (succeededCount == 0) {
                continue;
            }
            if (succeededCount != group.size()) {
                return false;
            }
            AuthorizationCompleteActionProjection projection = verifiedAuthorizationCompleteGroup(
                    transaction, authorization, entry.getKey().businessScene(), entry.getKey().businessSn(), group);
            if (projection == null) {
                return false;
            }
            try {
                verifiedCompleted = Math.addExact(verifiedCompleted, projection.money().getAmount());
            } catch (ArithmeticException exception) {
                return false;
            }
        }
        return verifiedCompleted == completedAmount;
    }

    private @Nullable AuthorizationReleaseActionProjection verifiedAuthorizationReleaseActionProjection(
            FundsTransaction transaction,
            String businessScene,
            String businessSn) {
        if (!StringUtils.hasText(businessScene) || !StringUtils.hasText(businessSn)) {
            return null;
        }
        AuthorizationActionProjection authorization = verifiedAuthorizationActionProjection(transaction);
        if (authorization == null || !authorization.succeeded()) {
            return null;
        }
        List<FundsTransactionDetail> transactionDetails = queryTransactionDetails(transaction);
        List<FundsTransactionDetail> completeDetails = transactionDetails.stream()
                .filter(detail -> detail.getEventType() == FundsTransactionEventType.COMPLETE)
                .toList();
        List<FundsTransactionDetail> releaseDetails = transactionDetails.stream()
                .filter(detail -> detail.getEventType() == FundsTransactionEventType.REVERSAL)
                .toList();
        List<FundsTransactionDetail> actionDetails = releaseDetails.stream()
                .filter(detail -> businessScene.equals(detail.getBusinessScene()))
                .filter(detail -> businessSn.equals(detail.getBusinessSn()))
                .toList();
        AuthorizationReleaseActionProjection projection = verifiedAuthorizationReleaseGroup(
                transaction, authorization, businessScene, businessSn, actionDetails);
        return projection != null
                && matchesAuthorizationCompleteCumulative(transaction, authorization, completeDetails)
                && matchesAuthorizationReleaseCumulative(transaction, authorization, releaseDetails)
                ? projection : null;
    }

    private @Nullable AuthorizationReleaseActionProjection verifiedAuthorizationReleaseGroup(
            FundsTransaction transaction,
            AuthorizationActionProjection authorization,
            String businessScene,
            String businessSn,
            List<FundsTransactionDetail> details) {
        if (details.isEmpty()) {
            return null;
        }
        FundsTransactionDetail first = details.getFirst();
        if (first.getAmount() == null || first.getAmount() <= 0 || first.getCurrency() == null) {
            return null;
        }
        Money money = Money.immutable(first.getAmount(), first.getCurrency());
        String authorizationLedgerSn = authorization.details().getFirst().getLedgerTransactionSn();
        String releaseLedgerSn = first.getLedgerTransactionSn();
        if (!StringUtils.hasText(authorizationLedgerSn) || !StringUtils.hasText(releaseLedgerSn)
                || Objects.equals(authorizationLedgerSn, releaseLedgerSn)
                || details.stream().anyMatch(detail -> !matchesAuthorizationReleaseDetail(
                transaction, detail, businessScene, businessSn, money, authorizationLedgerSn, releaseLedgerSn))) {
            return null;
        }
        List<FundsTransactionDetail> responsibilityDetails = matchAuthorizationReleaseResponsibilities(
                transaction, authorization.routeSnapshot(), details, money);
        if (responsibilityDetails.size() != details.size()
                || !matchesAuthorizationReleaseReplay(
                authorization.routeSnapshot(), responsibilityDetails, money)) {
            return null;
        }
        return new AuthorizationReleaseActionProjection(
                authorization, details, money, businessScene, businessSn, releaseLedgerSn);
    }

    private boolean matchesAuthorizationReleaseDetail(FundsTransaction transaction,
                                                       FundsTransactionDetail detail,
                                                       String businessScene,
                                                       String businessSn,
                                                       Money money,
                                                       String authorizationLedgerSn,
                                                       String releaseLedgerSn) {
        return Objects.equals(detail.getTenantId(), transaction.getTenantId())
                && Objects.equals(detail.getTransactionSn(), transaction.getSn())
                && Objects.equals(detail.getBusinessScene(), businessScene)
                && Objects.equals(detail.getBusinessSn(), businessSn)
                && detail.getTransactionType() == DefaultFundsTransactionType.PAY
                && detail.getEventType() == FundsTransactionEventType.REVERSAL
                && detail.getFundsEffectType() == FundsEffectType.RELEASE
                && detail.getState() == FundsTransactionDetailState.SUCCEEDED
                && !StringUtils.hasText(detail.getErrorCode())
                && !StringUtils.hasText(detail.getErrorMessage())
                && Objects.equals(detail.getAmount(), money.getAmount())
                && detail.getCurrency() == money.getCurrency()
                && Objects.equals(detail.getReferenceDetailSn(), transaction.getSn())
                && Objects.equals(detail.getReferenceLedgerTransactionSn(), authorizationLedgerSn)
                && Objects.equals(detail.getLedgerTransactionSn(), releaseLedgerSn);
    }

    private List<FundsTransactionDetail> matchAuthorizationReleaseResponsibilities(
            FundsTransaction transaction,
            RouteSnapshotSpec routeSnapshot,
            List<FundsTransactionDetail> details,
            Money money) {
        Map<String, FundsTransactionDetail> matches = new LinkedHashMap<>();
        for (RouteParticipantSpec participant : routeSnapshot.getParticipants()) {
            SubjectRef subjectRef = participant.getSubjectRef();
            if (subjectRef == null) {
                return List.of();
            }
            List<FundsTransactionDetail> candidates = details.stream()
                    .filter(detail -> matchesAuthorizationSuccessorSubject(
                            transaction, detail, subjectRef, participant.getParticipantRole(), money))
                    .toList();
            if (candidates.size() != 1
                    || matches.put(candidates.getFirst().getSn(), candidates.getFirst()) != null) {
                return List.of();
            }
        }
        return matches.size() == routeSnapshot.getParticipants().size()
                ? new ArrayList<>(matches.values()) : List.of();
    }

    private boolean matchesAuthorizationReleaseReplay(RouteSnapshotSpec routeSnapshot,
                                                       List<FundsTransactionDetail> details,
                                                       Money money) {
        if (routeSnapshot.getLegs().size() != details.size()) {
            return false;
        }
        Map<String, FundsTransactionDetail> matches = new LinkedHashMap<>();
        for (RouteLegSpec leg : routeSnapshot.getLegs()) {
            List<FundsTransactionDetail> candidates = details.stream()
                    .filter(detail -> matchesNode(leg.getSourceNode(), detail)
                            && matchesNode(leg.getTargetNode(), detail))
                    .toList();
            if (candidates.size() != 1 || !validAuthorizationSuccessorMoney(money, leg.getAmount())) {
                return false;
            }
            FundsTransactionDetail detail = candidates.getFirst();
            if (matches.put(detail.getSn(), detail) != null
                    || !matchesConsumedReplayLegs(detail, Map.of(leg.getLegId(), money.getAmount()))) {
                return false;
            }
        }
        return matches.size() == details.size();
    }

    private boolean matchesAuthorizationReleaseCumulative(
            FundsTransaction transaction,
            AuthorizationActionProjection authorization,
            List<FundsTransactionDetail> releaseDetails) {
        Long authorizedAmount = transaction.getAuthorizedAmount();
        Long completedAmount = transaction.getCompletedAmount();
        Long reversedAmount = transaction.getReversedAmount();
        if (authorizedAmount == null || authorizedAmount <= 0 || completedAmount == null || completedAmount < 0
                || reversedAmount == null || reversedAmount < 0) {
            return false;
        }
        try {
            if (Math.addExact(completedAmount, reversedAmount) > authorizedAmount) {
                return false;
            }
        } catch (ArithmeticException exception) {
            return false;
        }
        Map<AuthorizationSuccessorActionKey, List<FundsTransactionDetail>> groups = new LinkedHashMap<>();
        for (FundsTransactionDetail detail : releaseDetails) {
            groups.computeIfAbsent(new AuthorizationSuccessorActionKey(
                    detail.getBusinessScene(), detail.getBusinessSn()), ignored -> new ArrayList<>()).add(detail);
        }
        long verifiedReversed = 0L;
        for (Map.Entry<AuthorizationSuccessorActionKey, List<FundsTransactionDetail>> entry : groups.entrySet()) {
            List<FundsTransactionDetail> group = entry.getValue();
            long succeededCount = group.stream()
                    .filter(detail -> detail.getState() == FundsTransactionDetailState.SUCCEEDED)
                    .count();
            if (succeededCount == 0) {
                continue;
            }
            if (succeededCount != group.size()) {
                return false;
            }
            AuthorizationReleaseActionProjection projection = verifiedAuthorizationReleaseGroup(
                    transaction, authorization, entry.getKey().businessScene(), entry.getKey().businessSn(), group);
            if (projection == null) {
                return false;
            }
            try {
                verifiedReversed = Math.addExact(verifiedReversed, projection.money().getAmount());
            } catch (ArithmeticException exception) {
                return false;
            }
        }
        return verifiedReversed == reversedAmount;
    }

    private boolean matchesAuthorizationRoot(FundsTransaction transaction, RouteSnapshotSpec routeSnapshot) {
        return Objects.equals(routeSnapshot.getTenantId(), transaction.getTenantId())
                && StringUtils.hasText(routeSnapshot.getSnapshotId())
                && StringUtils.hasText(routeSnapshot.getSnapshotSchemaVersion())
                && StringUtils.hasText(routeSnapshot.getRouteVersion())
                && Objects.equals(routeSnapshot.getBusinessScene(), transaction.getBusinessScene())
                && Objects.equals(routeSnapshot.getBusinessSn(), transaction.getBusinessSn())
                && routeSnapshot.getInstructionType() == FundsInstructionType.AUTHORIZATION_TRANSACTION
                && routeSnapshot.getEventType() == FundsTransactionEventType.AUTHORIZE
                && routeSnapshot.getTransactionType() == DefaultFundsTransactionType.PAY
                && FundsRouteCodes.AUTHORIZATION_STANDARD.equals(routeSnapshot.getRouteCode())
                && transaction.getAmount() != null
                && transaction.getAmount() > 0
                && transaction.getCurrency() != null
                && !routeSnapshot.getParticipants().isEmpty();
    }

    private boolean matchesAuthorizationResponsibility(RouteSnapshotSpec routeSnapshot) {
        List<RouteParticipantSpec> participants = routeSnapshot.getParticipants();
        if (participants.size() == 1) {
            return matchesAuthorizationParticipant(
                    participants.getFirst(), RouteParticipantRole.PAYER, FundsSubjectType.FUNDING_ACCOUNT);
        }
        if (participants.size() != 2) {
            return false;
        }
        List<RouteParticipantSpec> creditParticipants = participants.stream()
                .filter(participant -> matchesAuthorizationParticipant(
                        participant, RouteParticipantRole.AUTH_HOLDER, FundsSubjectType.CREDIT_ACCOUNT))
                .toList();
        List<RouteParticipantSpec> fundingParticipants = participants.stream()
                .filter(participant -> matchesAuthorizationParticipant(
                        participant, RouteParticipantRole.REAL_FUNDING_SOURCE, FundsSubjectType.FUNDING_ACCOUNT))
                .toList();
        if (creditParticipants.size() != 1 || fundingParticipants.size() != 1) {
            return false;
        }
        AccountHierarchySnapshotSpec hierarchy = creditParticipants.getFirst().getAccountHierarchySnapshot();
        return hierarchy != null
                && StringUtils.hasText(hierarchy.getRelationSn())
                && sameAuthorizationSubject(hierarchy.getParentAccountRef(),
                fundingParticipants.getFirst());
    }

    private boolean matchesAuthorizationParticipant(RouteParticipantSpec participant,
                                                    RouteParticipantRole role,
                                                    FundsSubjectType subjectType) {
        SubjectRef subjectRef = participant.getSubjectRef();
        return participant.getParticipantRole() == role
                && subjectRef.getSubjectType() == subjectType;
    }

    private boolean sameAuthorizationSubject(SubjectRef hierarchySubject, RouteParticipantSpec participant) {
        SubjectRef participantSubject = participant.getSubjectRef();
        return Objects.equals(hierarchySubject.getTenantId(), participantSubject.getTenantId())
                && hierarchySubject.getSubjectType() == participantSubject.getSubjectType()
                && Objects.equals(hierarchySubject.getSubjectId(), participantSubject.getSubjectId())
                && hierarchySubject.getCurrency() != null
                && participant.getCurrency() != null
                && hierarchySubject.getCurrency() == participant.getCurrency();
    }

    private boolean matchesAuthorizationLegs(RouteSnapshotSpec routeSnapshot,
                                             List<FundsTransactionDetail> details) {
        if (routeSnapshot.getLegs().isEmpty()) {
            return false;
        }
        if (routeSnapshot.getLegs().size() != details.size()) {
            return false;
        }
        Map<String, RouteLegSpec> matches = new LinkedHashMap<>();
        for (FundsTransactionDetail detail : details) {
            List<RouteLegSpec> candidates = routeSnapshot.getLegs().stream()
                    .filter(leg -> matchesAuthorizationLeg(leg, detail))
                    .toList();
            if (candidates.size() != 1
                    || matches.put(candidates.getFirst().getLegId(), candidates.getFirst()) != null) {
                return false;
            }
        }
        return matches.size() == routeSnapshot.getLegs().size();
    }

    private boolean matchesAuthorizationLeg(RouteLegSpec leg, FundsTransactionDetail detail) {
        return leg.getLegType() == RouteLegType.HOLD
                && leg.getSequence() > 0
                && Objects.equals(leg.getLegId(), FundsRouteLegIds.AUTHORIZATION_PREFIX + leg.getSequence())
                && !StringUtils.hasText(leg.getReplayRefLegId())
                && matchesLegContents(leg, detail, detail)
                && matchesMoney(leg.getOriginalAmount(), detail);
    }

    private boolean isAuthorizationProvenFull(FundsTransaction transaction,
                                              List<FundsTransactionDetail> details) {
        return details.stream().allMatch(detail -> detail.getState() == FundsTransactionDetailState.SUCCEEDED)
                && details.stream().allMatch(detail -> StringUtils.hasText(detail.getLedgerTransactionSn()))
                && details.stream().map(FundsTransactionDetail::getLedgerTransactionSn).distinct().count() == 1
                && details.stream().allMatch(detail -> !StringUtils.hasText(detail.getErrorCode())
                && !StringUtils.hasText(detail.getErrorMessage()))
                && Objects.equals(transaction.getAuthorizedAmount(), transaction.getAmount())
                && zero(transaction.getDeclinedAmount())
                && zero(transaction.getFeeAmount());
    }

    private boolean isAuthorizationProvenZero(FundsTransaction transaction,
                                              RouteSnapshotSpec routeSnapshot,
                                              List<FundsTransactionDetail> details) {
        return transaction.getState() == FundsTransactionState.REJECTED
                && routeSnapshot.getLegs().isEmpty()
                && details.stream().allMatch(detail -> detail.getState() == FundsTransactionDetailState.REJECTED)
                && details.stream().allMatch(detail -> !StringUtils.hasText(detail.getLedgerTransactionSn()))
                && hasZeroAggregates(transaction);
    }

    private @Nullable RouteSnapshotSpec parsePayRouteSnapshot(FundsTransaction transaction) {
        try {
            return RouteSnapshotJsonSupport.parseRouteSnapshot(
                    transaction.getRouteSnapshot(), transaction.getGmtCreate());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean matchesPayRoot(FundsTransaction transaction, RouteSnapshotSpec routeSnapshot) {
        return Objects.equals(routeSnapshot.getTenantId(), transaction.getTenantId())
                && StringUtils.hasText(routeSnapshot.getSnapshotId())
                && Objects.equals(routeSnapshot.getBusinessScene(), transaction.getBusinessScene())
                && Objects.equals(routeSnapshot.getBusinessSn(), transaction.getBusinessSn())
                && routeSnapshot.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && routeSnapshot.getEventType() == FundsTransactionEventType.PAY
                && routeSnapshot.getTransactionType() == DefaultFundsTransactionType.PAY;
    }

    private List<FundsTransactionDetail> matchRouteParticipants(FundsTransaction transaction,
                                                                RouteSnapshotSpec routeSnapshot,
                                                                List<FundsTransactionDetail> details) {
        if (details.isEmpty() || details.size() != routeSnapshot.getParticipants().size()) {
            return List.of();
        }
        Map<String, FundsTransactionDetail> matches = new LinkedHashMap<>();
        for (RouteParticipantSpec participant : routeSnapshot.getParticipants()) {
            List<FundsTransactionDetail> candidates = details.stream()
                    .filter(detail -> matchesParticipant(transaction, routeSnapshot, participant, detail))
                    .toList();
            if (candidates.size() != 1 || matches.put(candidates.getFirst().getSn(), candidates.getFirst()) != null) {
                return List.of();
            }
        }
        return matches.size() == details.size() ? new ArrayList<>(matches.values()) : List.of();
    }

    private boolean matchesParticipant(FundsTransaction transaction,
                                       RouteSnapshotSpec routeSnapshot,
                                       RouteParticipantSpec participant,
                                       FundsTransactionDetail detail) {
        Money participantMoney = participant.getAmount();
        SubjectRef participantSubject = participant.getSubjectRef();
        return participantMoney != null
                && participantSubject != null
                && Objects.equals(detail.getTenantId(), transaction.getTenantId())
                && Objects.equals(detail.getTransactionSn(), transaction.getSn())
                && Objects.equals(detail.getBusinessScene(), transaction.getBusinessScene())
                && Objects.equals(detail.getBusinessSn(), transaction.getBusinessSn())
                && detail.getTransactionType() == transaction.getTransactionType()
                && detail.getEventType() == routeSnapshot.getEventType()
                && Objects.equals(detail.getSubjectId(), participantSubject.getSubjectId())
                && Objects.equals(detail.getSubjectType(), participantSubject.getSubjectType().name())
                && Objects.equals(participantSubject.getTenantId(), transaction.getTenantId())
                && participant.getCurrency() == detail.getCurrency()
                && (participantSubject.getCurrency() == null
                || participantSubject.getCurrency() == detail.getCurrency())
                && detail.getParticipantRole() == participant.getParticipantRole()
                && Objects.equals(detail.getAmount(), participantMoney.getAmount())
                && detail.getCurrency() == participantMoney.getCurrency()
                && detail.getFundsEffectType() == expectedFundsEffectType(transaction, routeSnapshot)
                && StringUtils.hasText(detail.getRequestHash());
    }

    private FundsEffectType expectedFundsEffectType(FundsTransaction transaction, RouteSnapshotSpec routeSnapshot) {
        if (transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION
                && routeSnapshot.getEventType() == FundsTransactionEventType.AUTHORIZE) {
            return FundsEffectType.HOLD;
        }
        return transaction.getTransactionType() == DefaultFundsTransactionType.REFUND
                ? FundsEffectType.RETURN
                : FundsEffectType.DIRECT;
    }

    private boolean matchesRecoveryRoot(FundsTransaction transaction, RouteSnapshotSpec routeSnapshot) {
        return Objects.equals(routeSnapshot.getTenantId(), transaction.getTenantId())
                && StringUtils.hasText(routeSnapshot.getSnapshotId())
                && Objects.equals(routeSnapshot.getBusinessScene(), transaction.getBusinessScene())
                && Objects.equals(routeSnapshot.getBusinessSn(), transaction.getBusinessSn())
                && routeSnapshot.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && routeSnapshot.getEventType() == FundsTransactionEventType.REFUND
                && routeSnapshot.getTransactionType() == DefaultFundsTransactionType.REFUND
                && FundsRouteCodes.DIRECT_REFUND_REPLAY.equals(routeSnapshot.getRouteCode());
    }

    private boolean matchesRecoveryLeg(RouteSnapshotSpec routeSnapshot,
                                       RouteSnapshotSpec originalRouteSnapshot,
                                       FundsTransactionDetail principal,
                                       FundsTransactionDetail payee) {
        if (routeSnapshot.getLegs().size() != 1 || originalRouteSnapshot.getLegs().size() != 1) {
            return false;
        }
        RouteLegSpec recoveryLeg = routeSnapshot.getLegs().getFirst();
        RouteLegSpec originalLeg = originalRouteSnapshot.getLegs().getFirst();
        return recoveryLeg.getLegType() == RouteLegType.RESTORE
                && Objects.equals(recoveryLeg.getReplayRefLegId(), originalLeg.getLegId())
                && matchesLegContents(recoveryLeg, payee, principal)
                && exactReverse(recoveryLeg, originalLeg)
                && validRecoveryMoney(recoveryLeg.getAmount(), originalLeg.getAmount());
    }

    private boolean exactReverse(RouteLegSpec recoveryLeg, RouteLegSpec originalLeg) {
        return sameRouteSubject(recoveryLeg.getSourceNode(), originalLeg.getTargetNode())
                && sameRouteSubject(recoveryLeg.getTargetNode(), originalLeg.getSourceNode());
    }

    private boolean sameRouteSubject(RouteNodeSpec first, RouteNodeSpec second) {
        if (first == null || second == null
                || first.getSubjectRef() == null || second.getSubjectRef() == null) {
            return false;
        }
        SubjectRef firstSubject = first.getSubjectRef();
        SubjectRef secondSubject = second.getSubjectRef();
        return first.getNodeType() == second.getNodeType()
                && Objects.equals(firstSubject.getTenantId(), secondSubject.getTenantId())
                && firstSubject.getSubjectType() == secondSubject.getSubjectType()
                && Objects.equals(firstSubject.getSubjectId(), secondSubject.getSubjectId())
                && Objects.equals(firstSubject.getCurrency(), secondSubject.getCurrency());
    }

    private boolean validRecoveryMoney(Money recoveryMoney, Money originalMoney) {
        return recoveryMoney != null
                && originalMoney != null
                && recoveryMoney.getAmount() > 0
                && recoveryMoney.getCurrency() == originalMoney.getCurrency()
                && recoveryMoney.getAmount() <= originalMoney.getAmount();
    }

    private boolean matchesRecoveryReferences(List<FundsTransactionDetail> details,
                                              FundsTransaction originalTransaction,
                                              PayActionProjection originalProjection) {
        String originalLedgerTransactionSn = originalProjection.principal().getLedgerTransactionSn();
        return StringUtils.hasText(originalLedgerTransactionSn)
                && details.stream().allMatch(detail -> Objects.equals(
                detail.getReferenceDetailSn(), originalTransaction.getSn()))
                && details.stream().allMatch(detail -> Objects.equals(
                detail.getReferenceLedgerTransactionSn(), originalLedgerTransactionSn));
    }

    private boolean matchesRecoveryCumulative(FundsTransaction originalTransaction,
                                              PayActionProjection originalProjection) {
        Long originalAmount = originalProjection.principal().getAmount();
        Long refundedAmount = originalTransaction.getRefundedAmount();
        if (originalAmount == null || originalAmount <= 0 || refundedAmount == null
                || refundedAmount < 0 || refundedAmount > originalAmount) {
            return false;
        }
        long verifiedAmount = 0L;
        for (FundsTransaction recovery : queryRecoveryTransactions(originalTransaction)) {
            RecoveryFactProjection projection = verifiedRecoveryFactProjection(
                    recovery, originalTransaction, originalProjection);
            if (projection == null || !projection.succeeded()) {
                continue;
            }
            try {
                verifiedAmount = Math.addExact(verifiedAmount, projection.principal().getAmount());
            } catch (ArithmeticException exception) {
                return false;
            }
        }
        return verifiedAmount == refundedAmount && verifiedAmount <= originalAmount;
    }

    private List<FundsTransaction> queryRecoveryTransactions(FundsTransaction originalTransaction) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(originalTransaction.getTenantId()))
                .and(ref.referenceTransactionSn.eq(originalTransaction.getSn()))
                .and(ref.transactionMode.eq(FundsTransactionMode.DIRECT))
                .and(ref.transactionType.eq(DefaultFundsTransactionType.REFUND))
                .orderBy(ref.id.asc());
        return fundsTransactionMapper.selectListByQuery(wrapper);
    }

    private boolean matchesPayLegs(RouteSnapshotSpec routeSnapshot,
                                   FundsTransactionDetail principal,
                                   FundsTransactionDetail payee,
                                   @Nullable FundsTransactionDetail fee) {
        List<RouteLegSpec> payLegs = routeSnapshot.getLegs().stream()
                .filter(leg -> FundsRouteLegIds.PAY.equals(leg.getLegId()))
                .toList();
        List<RouteLegSpec> feeLegs = routeSnapshot.getLegs().stream()
                .filter(leg -> FundsRouteLegIds.FEE.equals(leg.getLegId()))
                .toList();
        if (payLegs.size() != 1 || routeSnapshot.getLegs().size() != 1 + feeLegs.size()
                || !matchesLeg(payLegs.getFirst(), principal, payee)) {
            return false;
        }
        return fee == null ? feeLegs.isEmpty()
                : feeLegs.size() == 1 && matchesLeg(feeLegs.getFirst(), principal, fee);
    }

    private boolean matchesLeg(RouteLegSpec leg,
                               FundsTransactionDetail source,
                               FundsTransactionDetail target) {
        return leg.getLegType() == RouteLegType.INTERNAL_TRANSFER
                && matchesLegContents(leg, source, target);
    }

    private boolean matchesLegContents(RouteLegSpec leg,
                                       FundsTransactionDetail source,
                                       FundsTransactionDetail target) {
        return leg.getSourceNode() != null
                && leg.getTargetNode() != null
                && leg.getSourceNode().getNodeRole() == RouteNodeRole.SOURCE
                && leg.getTargetNode().getNodeRole() == RouteNodeRole.TARGET
                && matchesMoney(leg.getAmount(), target)
                && matchesNode(leg.getSourceNode(), source)
                && matchesNode(leg.getTargetNode(), target);
    }

    private boolean matchesNode(RouteNodeSpec node, FundsTransactionDetail detail) {
        SubjectRef subjectRef = node.getSubjectRef();
        return subjectRef != null
                && Objects.equals(subjectRef.getTenantId(), detail.getTenantId())
                && Objects.equals(subjectRef.getSubjectId(), detail.getSubjectId())
                && Objects.equals(subjectRef.getSubjectType().name(), detail.getSubjectType())
                && (subjectRef.getCurrency() == null || subjectRef.getCurrency() == detail.getCurrency());
    }

    private boolean isProvenFull(FundsTransaction transaction,
                                 List<FundsTransactionDetail> details,
                                 FundsTransactionDetail principal,
                                 @Nullable FundsTransactionDetail fee) {
        long expectedFee = fee == null ? 0L : fee.getAmount();
        return validSucceededPayLifecycle(transaction)
                && details.stream().allMatch(detail -> detail.getState() == FundsTransactionDetailState.SUCCEEDED)
                && details.stream().allMatch(detail -> StringUtils.hasText(detail.getLedgerTransactionSn()))
                && details.stream().map(FundsTransactionDetail::getLedgerTransactionSn).distinct().count() == 1
                && details.stream().allMatch(detail -> !StringUtils.hasText(detail.getErrorCode())
                && !StringUtils.hasText(detail.getErrorMessage()))
                && Objects.equals(transaction.getCompletedAmount(), principal.getAmount())
                && Objects.equals(transaction.getFeeAmount(), expectedFee)
                && validSucceededPayAggregates(transaction);
    }

    private boolean isProvenZero(FundsTransaction transaction, List<FundsTransactionDetail> details) {
        return transaction.getState() == FundsTransactionState.FAILED
                && details.stream().allMatch(detail -> detail.getState() == FundsTransactionDetailState.FAILED)
                && details.stream().allMatch(detail -> LEDGER_POSTING_REJECTED_ERROR_CODE.equals(detail.getErrorCode()))
                && details.stream().allMatch(detail -> !StringUtils.hasText(detail.getLedgerTransactionSn()))
                && hasZeroAggregates(transaction);
    }

    private boolean isRecoveryProvenFull(FundsTransaction transaction,
                                         List<FundsTransactionDetail> details,
                                         FundsTransactionDetail principal) {
        return transaction.getState() == FundsTransactionState.CLOSED
                && details.stream().allMatch(detail -> detail.getState() == FundsTransactionDetailState.SUCCEEDED)
                && details.stream().allMatch(detail -> StringUtils.hasText(detail.getLedgerTransactionSn()))
                && details.stream().map(FundsTransactionDetail::getLedgerTransactionSn).distinct().count() == 1
                && details.stream().allMatch(detail -> !StringUtils.hasText(detail.getErrorCode())
                && !StringUtils.hasText(detail.getErrorMessage()))
                && zero(transaction.getAuthorizedAmount())
                && zero(transaction.getReversedAmount())
                && zero(transaction.getCompletedAmount())
                && Objects.equals(transaction.getRefundedAmount(), principal.getAmount())
                && zero(transaction.getDeclinedAmount())
                && zero(transaction.getFeeAmount());
    }

    private boolean validSucceededPayLifecycle(FundsTransaction transaction) {
        long refundedAmount = transaction.getRefundedAmount();
        if (refundedAmount > 0 && refundedAmount < transaction.getCompletedAmount()) {
            return transaction.getState() == FundsTransactionState.OPEN;
        }
        return transaction.getState() == FundsTransactionState.CLOSED;
    }

    private boolean validSucceededPayAggregates(FundsTransaction transaction) {
        return zero(transaction.getAuthorizedAmount())
                && zero(transaction.getReversedAmount())
                && zero(transaction.getDeclinedAmount())
                && transaction.getRefundedAmount() >= 0
                && transaction.getRefundedAmount() <= transaction.getCompletedAmount();
    }

    private boolean hasZeroAggregates(FundsTransaction transaction) {
        return zero(transaction.getAuthorizedAmount())
                && zero(transaction.getReversedAmount())
                && zero(transaction.getCompletedAmount())
                && zero(transaction.getRefundedAmount())
                && zero(transaction.getDeclinedAmount())
                && zero(transaction.getFeeAmount());
    }

    private boolean zero(@Nullable Long amount) {
        return amount != null && amount == 0L;
    }

    private boolean sameMoney(FundsTransactionDetail first, FundsTransactionDetail second) {
        return Objects.equals(first.getAmount(), second.getAmount()) && first.getCurrency() == second.getCurrency();
    }

    private boolean matchesMoney(Money money, FundsTransactionDetail detail) {
        return money != null
                && Objects.equals(money.getAmount(), detail.getAmount())
                && money.getCurrency() == detail.getCurrency();
    }

    private List<FundsTransactionDetail> queryTransactionDetails(FundsTransaction transaction) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(transaction.getTenantId()))
                .and(ref.transactionSn.eq(transaction.getSn()))
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper);
    }

    private FundsActionFactDTO toActionFact(FundsTransaction transaction,
                                             RouteSnapshotSpec routeSnapshot,
                                             FundsTransactionDetail detail,
                                             int actionIndex,
                                             boolean succeeded) {
        Money money = Money.immutable(detail.getAmount(), detail.getCurrency());
        FundsActionFactDTO.FundsEffect fundsEffect = new FundsActionFactDTO.FundsEffect(
                succeeded ? EFFECT_PROVEN_FULL : EFFECT_PROVEN_ZERO,
                succeeded ? money : null);
        FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef = new FundsActionFactDTO.RouteSnapshotRef(
                transaction.getTenantId(), new FundsActionFactDTO.StableIdentity(
                ROUTE_SNAPSHOT_OWNER_NAMESPACE, routeSnapshot.getSnapshotId()));
        FundsActionFactDTO.FundsRouteProvenance provenance = new FundsActionFactDTO.FundsRouteProvenance(
                null, money, routeSnapshotRef, PROVENANCE_EXECUTION);
        FundsActionFactDTO.DomainOutcome outcome = new FundsActionFactDTO.DomainOutcome(
                DOMAIN_OUTCOME_OWNER,
                succeeded ? OUTCOME_SUCCEEDED : OUTCOME_FAILED);
        String attemptRef = transaction.getSn() + ":" + detail.getBusinessScene() + ":" + detail.getBusinessSn()
                + ":" + detail.getEventType().name();
        String identity = actionIdentity(transaction.getSn(), actionIndex);
        FundsActionFactDTO.SemanticDigest semanticDigest = actionSemanticDigest(
                transaction, detail, identity, attemptRef, money, outcome, fundsEffect, routeSnapshotRef);
        return new FundsActionFactDTO(
                new FundsActionFactRef(transaction.getTenantId(), identity),
                transaction.getSn(),
                attemptRef,
                ACTION_KIND_PRIMARY,
                money,
                outcome,
                fundsEffect,
                semanticDigest,
                List.of(),
                List.of(provenance));
    }

    private FundsActionFactDTO toAuthorizationActionFact(FundsTransaction transaction,
                                                         AuthorizationActionProjection projection) {
        Money money = Money.immutable(transaction.getAmount(), transaction.getCurrency());
        FundsActionFactDTO.FundsEffect fundsEffect = new FundsActionFactDTO.FundsEffect(
                projection.succeeded() ? EFFECT_PROVEN_FULL : EFFECT_PROVEN_ZERO,
                projection.succeeded() ? money : null);
        FundsActionFactDTO.DomainOutcome outcome = new FundsActionFactDTO.DomainOutcome(
                DOMAIN_OUTCOME_OWNER, projection.succeeded() ? OUTCOME_SUCCEEDED : OUTCOME_REJECTED);
        FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef = new FundsActionFactDTO.RouteSnapshotRef(
                transaction.getTenantId(), new FundsActionFactDTO.StableIdentity(
                ROUTE_SNAPSHOT_OWNER_NAMESPACE, projection.routeSnapshot().getSnapshotId()));
        FundsActionFactDTO.FundsRouteProvenance provenance = new FundsActionFactDTO.FundsRouteProvenance(
                null, money, routeSnapshotRef, PROVENANCE_EXECUTION);
        String attemptRef = transaction.getSn() + ":" + transaction.getBusinessScene() + ":"
                + transaction.getBusinessSn() + ":" + FundsTransactionEventType.AUTHORIZE.name();
        String identity = transaction.getSn() + AUTHORIZATION_ACTION_IDENTITY_MARKER + "0";
        FundsActionFactDTO.SemanticDigest semanticDigest = authorizationActionSemanticDigest(
                transaction, projection, identity, attemptRef, money, outcome, fundsEffect, routeSnapshotRef);
        return new FundsActionFactDTO(
                new FundsActionFactRef(transaction.getTenantId(), identity),
                transaction.getSn(),
                attemptRef,
                ACTION_KIND_AUTHORIZE,
                money,
                outcome,
                fundsEffect,
                semanticDigest,
                List.of(),
                List.of(provenance));
    }

    private FundsActionFactDTO toAuthorizationCompleteActionFact(
            FundsTransaction transaction,
            AuthorizationCompleteActionProjection projection) {
        FundsActionFactDTO authorizationFact = toAuthorizationActionFact(
                transaction, projection.authorization());
        Money money = projection.money();
        FundsActionFactDTO.OriginalFundsFactRef originalFactRef =
                new FundsActionFactDTO.OriginalFundsFactRef(
                        transaction.getTenantId(), ORIGINAL_FACT_TYPE,
                        authorizationFact.getIdentity().getIdentity(),
                        AUTHORIZATION_COMPLETE_ORIGINAL_FACT_RELATION, money);
        FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef = authorizationFact
                .getRouteProvenance().getFirst().getRouteSnapshotRef();
        List<FundsActionFactDTO.FundsRouteProvenance> provenance = projection.authorization()
                .routeSnapshot().getLegs().stream()
                .map(ignored -> new FundsActionFactDTO.FundsRouteProvenance(
                        originalFactRef, money, routeSnapshotRef, PROVENANCE_REPLAYED_ORIGINAL_ROUTE))
                .toList();
        FundsActionFactDTO.DomainOutcome outcome = new FundsActionFactDTO.DomainOutcome(
                DOMAIN_OUTCOME_OWNER, OUTCOME_SUCCEEDED);
        FundsActionFactDTO.FundsEffect fundsEffect = new FundsActionFactDTO.FundsEffect(
                EFFECT_PROVEN_FULL, money);
        String identity = transaction.getSn() + COMPLETE_ACTION_IDENTITY_MARKER
                + projection.businessScene() + ":" + projection.businessSn();
        String attemptRef = transaction.getSn() + ":" + projection.businessScene() + ":"
                + projection.businessSn() + ":" + FundsTransactionEventType.COMPLETE.name();
        FundsActionFactDTO.SemanticDigest semanticDigest = completeActionSemanticDigest(
                transaction, projection, identity, attemptRef, outcome, fundsEffect, originalFactRef,
                routeSnapshotRef);
        return new FundsActionFactDTO(
                new FundsActionFactRef(transaction.getTenantId(), identity),
                transaction.getSn(),
                attemptRef,
                ACTION_KIND_COMPLETE,
                money,
                outcome,
                fundsEffect,
                semanticDigest,
                List.of(originalFactRef),
                provenance);
    }

    private FundsActionFactDTO toAuthorizationReleaseActionFact(
            FundsTransaction transaction,
            AuthorizationReleaseActionProjection projection) {
        FundsActionFactDTO authorizationFact = toAuthorizationActionFact(
                transaction, projection.authorization());
        Money money = projection.money();
        FundsActionFactDTO.OriginalFundsFactRef originalFactRef =
                new FundsActionFactDTO.OriginalFundsFactRef(
                        transaction.getTenantId(), ORIGINAL_FACT_TYPE,
                        authorizationFact.getIdentity().getIdentity(),
                        AUTHORIZATION_RELEASE_ORIGINAL_FACT_RELATION, money);
        FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef = authorizationFact
                .getRouteProvenance().getFirst().getRouteSnapshotRef();
        List<FundsActionFactDTO.FundsRouteProvenance> provenance = projection.authorization()
                .routeSnapshot().getLegs().stream()
                .map(ignored -> new FundsActionFactDTO.FundsRouteProvenance(
                        originalFactRef, money, routeSnapshotRef, PROVENANCE_REPLAYED_ORIGINAL_ROUTE))
                .toList();
        FundsActionFactDTO.DomainOutcome outcome = new FundsActionFactDTO.DomainOutcome(
                DOMAIN_OUTCOME_OWNER, OUTCOME_SUCCEEDED);
        FundsActionFactDTO.FundsEffect fundsEffect = new FundsActionFactDTO.FundsEffect(
                EFFECT_PROVEN_FULL, money);
        String identity = releaseActionIdentity(
                transaction.getSn(), projection.businessScene(), projection.businessSn());
        String intentRef = releaseIntentRef(
                transaction.getSn(), projection.businessScene(), projection.businessSn());
        String attemptRef = releaseAttemptRef(
                transaction.getSn(), projection.businessScene(), projection.businessSn());
        FundsActionFactDTO.SemanticDigest semanticDigest = releaseActionSemanticDigest(
                transaction, projection, authorizationFact, identity, intentRef, attemptRef,
                outcome, fundsEffect, originalFactRef, routeSnapshotRef);
        return new FundsActionFactDTO(
                new FundsActionFactRef(transaction.getTenantId(), identity),
                intentRef,
                attemptRef,
                ACTION_KIND_RELEASE,
                money,
                outcome,
                fundsEffect,
                semanticDigest,
                List.of(originalFactRef),
                provenance);
    }

    private FundsActionFactDTO toRecoveryActionFact(FundsTransaction transaction,
                                                    RecoveryActionProjection projection) {
        Money money = Money.immutable(projection.principal().getAmount(), projection.principal().getCurrency());
        FundsActionFactDTO originalActionFact = projection.originalActionFact();
        FundsActionFactDTO.OriginalFundsFactRef originalFactRef = new FundsActionFactDTO.OriginalFundsFactRef(
                transaction.getTenantId(), ORIGINAL_FACT_TYPE, originalActionFact.getIdentity().getIdentity(),
                ORIGINAL_FACT_RELATION, money);
        FundsActionFactDTO.FundsEffect fundsEffect = new FundsActionFactDTO.FundsEffect(
                projection.succeeded() ? EFFECT_PROVEN_FULL : EFFECT_PROVEN_ZERO,
                projection.succeeded() ? money : null);
        FundsActionFactDTO.DomainOutcome outcome = new FundsActionFactDTO.DomainOutcome(
                DOMAIN_OUTCOME_OWNER, projection.succeeded() ? OUTCOME_SUCCEEDED : OUTCOME_FAILED);
        FundsActionFactDTO.RouteSnapshotRef originalRouteSnapshotRef = originalActionFact
                .getRouteProvenance().getFirst().getRouteSnapshotRef();
        FundsActionFactDTO.FundsRouteProvenance provenance = new FundsActionFactDTO.FundsRouteProvenance(
                originalFactRef, money, originalRouteSnapshotRef, PROVENANCE_REPLAYED_ORIGINAL_ROUTE);
        String attemptRef = transaction.getSn() + ":" + projection.principal().getBusinessScene() + ":"
                + projection.principal().getBusinessSn() + ":" + projection.principal().getEventType().name();
        String identity = transaction.getSn() + RECOVERY_ACTION_IDENTITY_MARKER + "0";
        FundsActionFactDTO.SemanticDigest semanticDigest = recoveryActionSemanticDigest(
                transaction, projection.principal(), identity, attemptRef, money, outcome, fundsEffect,
                originalFactRef, originalRouteSnapshotRef);
        return new FundsActionFactDTO(
                new FundsActionFactRef(transaction.getTenantId(), identity),
                transaction.getSn(),
                attemptRef,
                ACTION_KIND_RECOVERY,
                money,
                outcome,
                fundsEffect,
                semanticDigest,
                List.of(originalFactRef),
                List.of(provenance));
    }

    private FundsActionFactDTO.SemanticDigest recoveryActionSemanticDigest(
            FundsTransaction transaction,
            FundsTransactionDetail detail,
            String identity,
            String attemptRef,
            Money money,
            FundsActionFactDTO.DomainOutcome outcome,
            FundsActionFactDTO.FundsEffect fundsEffect,
            FundsActionFactDTO.OriginalFundsFactRef originalFactRef,
            FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", transaction.getTenantId());
        values.put("identity", identity);
        values.put("intentRef", transaction.getSn());
        values.put("attemptRef", attemptRef);
        values.put("actionKind", ACTION_KIND_RECOVERY);
        values.put("amount", money.getAmount());
        values.put("currency", money.getCurrency().name());
        values.put("subjectId", detail.getSubjectId());
        values.put("subjectType", detail.getSubjectType());
        values.put("participantRole", detail.getParticipantRole().name());
        values.put("outcomeOwner", outcome.getOwner());
        values.put("outcomeCode", outcome.getCode());
        values.put("effectKind", fundsEffect.getEffectKind());
        values.put("provenAmount", fundsEffect.getProvenMoney() == null
                ? null : fundsEffect.getProvenMoney().getAmount());
        values.put("originalFactType", originalFactRef.getFactType());
        values.put("originalFactId", originalFactRef.getFactId());
        values.put("originalRelationRole", originalFactRef.getRelationRole());
        values.put("routeSnapshotOwner", routeSnapshotRef.getIdentity().getOwnerNamespace());
        values.put("routeSnapshotId", routeSnapshotRef.getIdentity().getValue());
        return new FundsActionFactDTO.SemanticDigest(
                SEMANTIC_DIGEST_ALGORITHM,
                FundsStableHashSupport.sha256CanonicalJson(RECOVERY_SEMANTIC_DIGEST_DOMAIN, values),
                RECOVERY_SEMANTIC_DIGEST_FIELDS_VERSION);
    }

    private FundsActionFactDTO.SemanticDigest completeActionSemanticDigest(
            FundsTransaction transaction,
            AuthorizationCompleteActionProjection projection,
            String identity,
            String attemptRef,
            FundsActionFactDTO.DomainOutcome outcome,
            FundsActionFactDTO.FundsEffect fundsEffect,
            FundsActionFactDTO.OriginalFundsFactRef originalFactRef,
            FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", transaction.getTenantId());
        values.put("identity", identity);
        values.put("intentRef", transaction.getSn());
        values.put("attemptRef", attemptRef);
        values.put("actionKind", ACTION_KIND_COMPLETE);
        values.put("amount", projection.money().getAmount());
        values.put("currency", projection.money().getCurrency().name());
        values.put("outcomeOwner", outcome.getOwner());
        values.put("outcomeCode", outcome.getCode());
        values.put("effectKind", fundsEffect.getEffectKind());
        values.put("provenAmount", fundsEffect.getProvenMoney().getAmount());
        values.put("originalFactType", originalFactRef.getFactType());
        values.put("originalFactId", originalFactRef.getFactId());
        values.put("originalRelationRole", originalFactRef.getRelationRole());
        values.put("routeSnapshotOwner", routeSnapshotRef.getIdentity().getOwnerNamespace());
        values.put("routeSnapshotId", routeSnapshotRef.getIdentity().getValue());
        values.put("completeLedgerTransactionSn", projection.completeLedgerSn());
        values.put("completeDetails", projection.details().stream()
                .map(this::completeDetailDigestValues)
                .toList());
        values.put("route", RouteSnapshotJsonSupport.pathOnlyRouteSummary(
                RouteSnapshotJsonSupport.routeSummary(projection.authorization().routeSnapshot())));
        return new FundsActionFactDTO.SemanticDigest(
                SEMANTIC_DIGEST_ALGORITHM,
                FundsStableHashSupport.sha256CanonicalJson(COMPLETE_SEMANTIC_DIGEST_DOMAIN, values),
                COMPLETE_SEMANTIC_DIGEST_FIELDS_VERSION);
    }

    private Map<String, Object> completeDetailDigestValues(FundsTransactionDetail detail) {
        Map<String, Object> values = new TreeMap<>();
        values.put("subjectId", detail.getSubjectId());
        values.put("subjectType", detail.getSubjectType());
        values.put("participantRole", detail.getParticipantRole().name());
        values.put("amount", detail.getAmount());
        values.put("currency", detail.getCurrency().name());
        values.put("ledgerTransactionSn", detail.getLedgerTransactionSn());
        values.put("referenceDetailSn", detail.getReferenceDetailSn());
        values.put("referenceLedgerTransactionSn", detail.getReferenceLedgerTransactionSn());
        return values;
    }

    private FundsActionFactDTO.SemanticDigest releaseActionSemanticDigest(
            FundsTransaction transaction,
            AuthorizationReleaseActionProjection projection,
            FundsActionFactDTO authorizationFact,
            String identity,
            String intentRef,
            String attemptRef,
            FundsActionFactDTO.DomainOutcome outcome,
            FundsActionFactDTO.FundsEffect fundsEffect,
            FundsActionFactDTO.OriginalFundsFactRef originalFactRef,
            FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", transaction.getTenantId());
        values.put("identity", identity);
        values.put("intentRef", intentRef);
        values.put("attemptRef", attemptRef);
        values.put("actionKind", ACTION_KIND_RELEASE);
        values.put("businessScene", projection.businessScene());
        values.put("businessSn", projection.businessSn());
        values.put("amount", projection.money().getAmount());
        values.put("currency", projection.money().getCurrency().name());
        values.put("outcomeOwner", outcome.getOwner());
        values.put("outcomeCode", outcome.getCode());
        values.put("effectKind", fundsEffect.getEffectKind());
        values.put("provenAmount", fundsEffect.getProvenMoney().getAmount());
        values.put("provenCurrency", fundsEffect.getProvenMoney().getCurrency().name());
        values.put("originalFactType", originalFactRef.getFactType());
        values.put("originalFactId", originalFactRef.getFactId());
        values.put("originalFactSemanticDigestAlgorithm",
                authorizationFact.getSemanticDigest().getAlgorithm());
        values.put("originalFactSemanticDigest", authorizationFact.getSemanticDigest().getValue());
        values.put("originalFactSemanticDigestFieldsVersion",
                authorizationFact.getSemanticDigest().getCoveredFieldsVersion());
        values.put("originalRelationRole", originalFactRef.getRelationRole());
        values.put("originalAllocatedAmount", originalFactRef.getAllocatedMoney().getAmount());
        values.put("originalAllocatedCurrency", originalFactRef.getAllocatedMoney().getCurrency().name());
        values.put("routeSnapshotOwner", routeSnapshotRef.getIdentity().getOwnerNamespace());
        values.put("routeSnapshotId", routeSnapshotRef.getIdentity().getValue());
        values.put("releaseLedgerTransactionSn", projection.releaseLedgerSn());
        values.put("releaseDetails", projection.details().stream()
                .sorted(java.util.Comparator.comparing((FundsTransactionDetail detail) ->
                                detail.getParticipantRole().name())
                        .thenComparing(FundsTransactionDetail::getSubjectId)
                        .thenComparing(FundsTransactionDetail::getSubjectType))
                .map(this::releaseDetailDigestValues)
                .toList());
        values.put("route", RouteSnapshotJsonSupport.pathOnlyRouteSummary(
                RouteSnapshotJsonSupport.routeSummary(projection.authorization().routeSnapshot())));
        return new FundsActionFactDTO.SemanticDigest(
                SEMANTIC_DIGEST_ALGORITHM,
                FundsStableHashSupport.sha256CanonicalJson(RELEASE_SEMANTIC_DIGEST_DOMAIN, values),
                RELEASE_SEMANTIC_DIGEST_FIELDS_VERSION);
    }

    private Map<String, Object> releaseDetailDigestValues(FundsTransactionDetail detail) {
        Map<String, Object> values = new TreeMap<>();
        values.put("businessScene", detail.getBusinessScene());
        values.put("businessSn", detail.getBusinessSn());
        values.put("eventType", detail.getEventType().name());
        values.put("fundsEffectType", detail.getFundsEffectType().name());
        values.put("state", detail.getState().name());
        values.put("subjectId", detail.getSubjectId());
        values.put("subjectType", detail.getSubjectType());
        values.put("participantRole", detail.getParticipantRole().name());
        values.put("amount", detail.getAmount());
        values.put("currency", detail.getCurrency().name());
        values.put("ledgerTransactionSn", detail.getLedgerTransactionSn());
        values.put("referenceDetailSn", detail.getReferenceDetailSn());
        values.put("referenceLedgerTransactionSn", detail.getReferenceLedgerTransactionSn());
        values.put("replayConsumedLegAmounts", releaseReplayConsumedLegAmounts(detail));
        return values;
    }

    private Map<String, Long> releaseReplayConsumedLegAmounts(FundsTransactionDetail detail) {
        Map<String, Object> context = parseContextVariables(detail.getContextVariables());
        Object amountsValue = context.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS);
        if (!(amountsValue instanceof Map<?, ?> amounts)) {
            throw new IllegalArgumentException("release replay consumed leg amounts are missing");
        }
        Map<String, Long> result = new TreeMap<>();
        for (Map.Entry<?, ?> entry : amounts.entrySet()) {
            if (!(entry.getKey() instanceof String legId) || !(entry.getValue() instanceof Number amount)) {
                throw new IllegalArgumentException("release replay consumed leg amount is invalid");
            }
            result.put(legId, new BigDecimal(amount.toString()).longValueExact());
        }
        return result;
    }

    private FundsActionFactDTO.SemanticDigest actionSemanticDigest(
            FundsTransaction transaction,
            FundsTransactionDetail detail,
            String identity,
            String attemptRef,
            Money money,
            FundsActionFactDTO.DomainOutcome outcome,
            FundsActionFactDTO.FundsEffect fundsEffect,
            FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", transaction.getTenantId());
        values.put("identity", identity);
        values.put("intentRef", transaction.getSn());
        values.put("attemptRef", attemptRef);
        values.put("actionKind", ACTION_KIND_PRIMARY);
        values.put("amount", money.getAmount());
        values.put("currency", money.getCurrency().name());
        values.put("subjectId", detail.getSubjectId());
        values.put("subjectType", detail.getSubjectType());
        values.put("participantRole", detail.getParticipantRole().name());
        values.put("outcomeOwner", outcome.getOwner());
        values.put("outcomeCode", outcome.getCode());
        values.put("effectKind", fundsEffect.getEffectKind());
        values.put("provenAmount", fundsEffect.getProvenMoney() == null
                ? null : fundsEffect.getProvenMoney().getAmount());
        values.put("routeSnapshotOwner", routeSnapshotRef.getIdentity().getOwnerNamespace());
        values.put("routeSnapshotId", routeSnapshotRef.getIdentity().getValue());
        return new FundsActionFactDTO.SemanticDigest(
                SEMANTIC_DIGEST_ALGORITHM,
                FundsStableHashSupport.sha256CanonicalJson(SEMANTIC_DIGEST_DOMAIN, values),
                SEMANTIC_DIGEST_FIELDS_VERSION);
    }

    private FundsActionFactDTO.SemanticDigest authorizationActionSemanticDigest(
            FundsTransaction transaction,
            AuthorizationActionProjection projection,
            String identity,
            String attemptRef,
            Money money,
            FundsActionFactDTO.DomainOutcome outcome,
            FundsActionFactDTO.FundsEffect fundsEffect,
            FundsActionFactDTO.RouteSnapshotRef routeSnapshotRef) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", transaction.getTenantId());
        values.put("identity", identity);
        values.put("intentRef", transaction.getSn());
        values.put("attemptRef", attemptRef);
        values.put("actionKind", ACTION_KIND_AUTHORIZE);
        values.put("amount", money.getAmount());
        values.put("currency", money.getCurrency().name());
        values.put("businessScene", transaction.getBusinessScene());
        values.put("businessSn", transaction.getBusinessSn());
        values.put("outcomeOwner", outcome.getOwner());
        values.put("outcomeCode", outcome.getCode());
        values.put("effectKind", fundsEffect.getEffectKind());
        values.put("provenAmount", fundsEffect.getProvenMoney() == null
                ? null : fundsEffect.getProvenMoney().getAmount());
        values.put("routeSnapshotOwner", routeSnapshotRef.getIdentity().getOwnerNamespace());
        values.put("routeSnapshotId", routeSnapshotRef.getIdentity().getValue());
        values.put("authorizationDetails", projection.details().stream()
                .map(this::authorizationDetailDigestValues)
                .toList());
        values.put("route", RouteSnapshotJsonSupport.pathOnlyRouteSummary(
                RouteSnapshotJsonSupport.routeSummary(projection.routeSnapshot())));
        return new FundsActionFactDTO.SemanticDigest(
                SEMANTIC_DIGEST_ALGORITHM,
                FundsStableHashSupport.sha256CanonicalJson(AUTHORIZATION_SEMANTIC_DIGEST_DOMAIN, values),
                AUTHORIZATION_SEMANTIC_DIGEST_FIELDS_VERSION);
    }

    private Map<String, Object> authorizationDetailDigestValues(FundsTransactionDetail detail) {
        Map<String, Object> values = new TreeMap<>();
        values.put("subjectId", detail.getSubjectId());
        values.put("subjectType", detail.getSubjectType());
        values.put("participantRole", detail.getParticipantRole().name());
        values.put("amount", detail.getAmount());
        values.put("currency", detail.getCurrency().name());
        values.put("state", detail.getState().name());
        values.put("requestHash", detail.getRequestHash());
        values.put("ledgerTransactionSn", detail.getLedgerTransactionSn());
        values.put("errorCode", detail.getErrorCode());
        return values;
    }

    private String releaseActionIdentity(String authorizationSn, String businessScene, String businessSn) {
        return RELEASE_ACTION_IDENTITY_PREFIX + encodeReleaseIdentityPart(authorizationSn) + ":"
                + encodeReleaseIdentityPart(businessScene) + ":" + encodeReleaseIdentityPart(businessSn);
    }

    private String releaseIntentRef(String authorizationSn, String businessScene, String businessSn) {
        return RELEASE_INTENT_REF_PREFIX + encodeReleaseIdentityPart(authorizationSn) + ":"
                + encodeReleaseIdentityPart(businessScene) + ":" + encodeReleaseIdentityPart(businessSn);
    }

    private String releaseAttemptRef(String authorizationSn, String businessScene, String businessSn) {
        return RELEASE_ATTEMPT_REF_PREFIX + encodeReleaseIdentityPart(authorizationSn) + ":"
                + encodeReleaseIdentityPart(businessScene) + ":" + encodeReleaseIdentityPart(businessSn)
                + ":" + FundsTransactionEventType.REVERSAL.name();
    }

    private @Nullable ReleaseActionIdentity parseReleaseActionIdentity(String identity) {
        if (!identity.startsWith(RELEASE_ACTION_IDENTITY_PREFIX)) {
            return null;
        }
        String[] encodedParts = identity.substring(RELEASE_ACTION_IDENTITY_PREFIX.length()).split(":", -1);
        if (encodedParts.length != 3) {
            return null;
        }
        String authorizationSn = decodeReleaseIdentityPart(encodedParts[0]);
        String businessScene = decodeReleaseIdentityPart(encodedParts[1]);
        String businessSn = decodeReleaseIdentityPart(encodedParts[2]);
        if (!StringUtils.hasText(authorizationSn)
                || !StringUtils.hasText(businessScene)
                || !StringUtils.hasText(businessSn)
                || !identity.equals(releaseActionIdentity(authorizationSn, businessScene, businessSn))) {
            return null;
        }
        return new ReleaseActionIdentity(authorizationSn, businessScene, businessSn);
    }

    private String encodeReleaseIdentityPart(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private @Nullable String decodeReleaseIdentityPart(String encodedValue) {
        try {
            String value = new String(Base64.getUrlDecoder().decode(encodedValue), StandardCharsets.UTF_8);
            return encodedValue.equals(encodeReleaseIdentityPart(value)) ? value : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String actionIdentity(String transactionSn, int actionIndex) {
        return transactionSn + ACTION_IDENTITY_MARKER + actionIndex;
    }

    private int actionIdentityMarkerIndex(String identity) {
        return Math.max(identity.lastIndexOf(COMPLETE_ACTION_IDENTITY_MARKER),
                Math.max(identity.lastIndexOf(AUTHORIZATION_ACTION_IDENTITY_MARKER),
                        Math.max(identity.lastIndexOf(ACTION_IDENTITY_MARKER),
                                identity.lastIndexOf(RECOVERY_ACTION_IDENTITY_MARKER))));
    }

    private record AuthorizationActionProjection(RouteSnapshotSpec routeSnapshot,
                                                 List<FundsTransactionDetail> details,
                                                 boolean succeeded) {
    }

    private record AuthorizationCompleteActionProjection(AuthorizationActionProjection authorization,
                                                         List<FundsTransactionDetail> details,
                                                         Money money,
                                                         String businessScene,
                                                         String businessSn,
                                                         String completeLedgerSn) {
    }

    private record AuthorizationReleaseActionProjection(AuthorizationActionProjection authorization,
                                                        List<FundsTransactionDetail> details,
                                                        Money money,
                                                        String businessScene,
                                                        String businessSn,
                                                        String releaseLedgerSn) {
    }

    private record AuthorizationSuccessorActionKey(String businessScene, String businessSn) {
    }

    private record AuthorizationSuccessorActionGroup(FundsTransaction transaction,
                                                     FundsTransactionEventType eventType) {
    }

    private record ReleaseActionIdentity(String authorizationSn, String businessScene, String businessSn) {
    }

    private record PayActionProjection(RouteSnapshotSpec routeSnapshot,
                                       List<FundsTransactionDetail> matchedDetails,
                                       FundsTransactionDetail principal,
                                       @Nullable FundsTransactionDetail fee,
                                       boolean succeeded) {
    }

    private record RecoveryActionProjection(FundsTransactionDetail principal,
                                            FundsActionFactDTO originalActionFact,
                                            boolean succeeded) {
    }

    private record RecoveryFactProjection(FundsTransactionDetail principal, boolean succeeded) {
    }

    private FundsFrozenOrder findFreezeOrderBySnNullable(Long tenantId, String sn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(sn));
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

    private Money sumFrozenOrderReleasedAmount(Long tenantId,
                                               String freezeOrderSn,
                                               CurrencyIsoCode currency,
                                               @Nullable String excludedBusinessScene,
                                               @Nullable String excludedBusinessSn) {
        FundsFrozenOrder order = findFreezeOrderBySnNullable(tenantId, freezeOrderSn);
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

    private List<FundsTransactionDetail> queryConsumedReplayDetails(Long tenantId,
                                                                    String referenceTransactionSn,
                                                                    FundsTransactionEventType eventType) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.referenceDetailSn.eq(referenceTransactionSn))
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

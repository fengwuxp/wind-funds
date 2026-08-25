package com.wind.funds.transaction.application.instrument.impl;

import com.wind.jackson.WindJson;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.route.model.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.application.instrument.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.transaction.application.instrument.CompleteAuthorizationByPaymentInstrumentRequest;
import com.wind.funds.transaction.application.instrument.ReceiveByInstrumentRequest;
import com.wind.funds.transaction.application.instrument.ReverseAuthorizationByPaymentInstrumentRequest;
import com.wind.funds.transaction.application.spend.SpendControlTransactionConsumptionRequest;
import com.wind.funds.transaction.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
import com.wind.funds.transaction.application.support.ExternalFundsRailResolver;
import com.wind.funds.transaction.application.support.ExternalFundsRailResolver.ExternalFundsRailDecision;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Transaction Provider 内部支付工具交易编排服务。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Slf4j
@Service
@AllArgsConstructor
public class PaymentInstrumentTransactionApplicationServiceImpl {

    private static final String SHA256_PREFIX = "sha256:";

    private static final String CONTROL_CONSUME_MOVEMENT_DOMAIN = "SPEND_CONTROL_AUTHORIZATION_CONSUME";

    private static final String CONTROL_RELEASE_MOVEMENT_DOMAIN = "SPEND_CONTROL_AUTHORIZATION_RELEASE";

    private static final String CONTROL_CONSUME_DIGEST_DOMAIN = "wallet.spend-control.consumption";

    private static final String CONTROL_RELEASE_DIGEST_DOMAIN = "wallet.spend-control.release";

    private final PaymentInstrumentAuthorizationProcessor authorizationProcessor;

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final FundsDirectTransactionService directTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final FundsAuthorizationTransactionService authorizationTransactionService;

    private final SpendControlTransactionConsumptionApplicationServiceImpl spendControlConsumptionService;

    private final SpendControlMovementService spendControlMovementService;

    @Transactional(rollbackFor = Exception.class)
    public @NonNull String authorizeByInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                 @NonNull WindOperator operator) {
        String transactionSn = authorizationProcessor.authorizeByInstrument(request, operator);
        log.info("支付工具授权完成，等待事务提交，tenantId={}, businessScene={}, businessSn={}, "
                        + "transactionSn={}, amount={}, currency={}",
                request.getTenantId(), request.getBusinessScene(), request.getBusinessSn(), transactionSn,
                request.getAmount(), request.getCurrency());
        return transactionSn;
    }

    @Transactional(rollbackFor = Exception.class)
    public @NonNull String completeAuthorizationByInstrument(
            @NonNull CompleteAuthorizationByPaymentInstrumentRequest request,
            @NonNull WindOperator operator) {
        validateCompletionRequest(request);
        FundsTransactionDTO authorization = getPaymentInstrumentAuthorization(
                request.getTenantId(), request.getAuthorizationTransactionSn(), request.getCurrency(), "支付工具授权完成");
        PaymentInstrumentRefSpec instrumentRef = getPaymentInstrumentRef(authorization);
        FundsAccountId authorizationAccountId = authorizationAccountId(instrumentRef, authorization.getSn());
        String authorizationSn = authorizationTransactionService.complete(
                toCompleteRequest(request, authorizationAccountId), operator);
        consumeControlReservation(request, authorization, authorizationSn);
        log.info("支付工具授权完成扣款处理完成，等待事务提交，tenantId={}, authorizationTransactionSn={}, "
                        + "businessScene={}, businessSn={}, transactionSn={}, amount={}, currency={}",
                request.getTenantId(), request.getAuthorizationTransactionSn(), request.getBusinessScene(),
                request.getBusinessSn(), authorizationSn, request.getAmount(), request.getCurrency());
        return authorizationSn;
    }

    @Transactional(rollbackFor = Exception.class)
    public @NonNull String reverseAuthorizationByInstrument(
            @NonNull ReverseAuthorizationByPaymentInstrumentRequest request,
            @NonNull WindOperator operator) {
        validateReversalRequest(request);
        FundsTransactionDTO authorization = getPaymentInstrumentAuthorization(
                request.getTenantId(), request.getAuthorizationTransactionSn(), request.getCurrency(), "支付工具授权撤销");
        PaymentInstrumentRefSpec instrumentRef = getPaymentInstrumentRef(authorization);
        FundsAccountId authorizationAccountId = authorizationAccountId(instrumentRef, authorization.getSn());
        String authorizationSn = authorizationTransactionService.reversal(
                toReversalRequest(request, authorizationAccountId), operator);
        releaseControlReservation(request, authorization, authorizationSn);
        log.info("支付工具授权撤销处理完成，等待事务提交，tenantId={}, authorizationTransactionSn={}, "
                        + "businessScene={}, businessSn={}, transactionSn={}, amount={}, currency={}",
                request.getTenantId(), request.getAuthorizationTransactionSn(), request.getBusinessScene(),
                request.getBusinessSn(), authorizationSn, request.getAmount(), request.getCurrency());
        return authorizationSn;
    }

    @Transactional(rollbackFor = Exception.class)
    public @NonNull String receiveByInstrument(@NonNull ReceiveByInstrumentRequest request,
                                               @NonNull WindOperator operator) {
        validateReceiveRequest(request);
        String establishedTransactionSn = findEstablishedReceiveReplay(request);
        if (establishedTransactionSn != null) {
            log.info("支付工具收款幂等复用，tenantId={}, externalSourceCode={}, externalFundsFactSn={}, "
                            + "businessScene={}, businessSn={}, transactionSn={}, amount={}, currency={}",
                    request.getTenantId(), request.getExternalSourceCode(), request.getExternalFundsFactSn(),
                    request.getBusinessScene(), request.getBusinessSn(), establishedTransactionSn,
                    request.getAmount(), request.getCurrency());
            return establishedTransactionSn;
        }
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                preTransactionSnapshotApplicationService.resolvePreTransactionSnapshot(toPreTransactionRequest(request));
        AssertUtils.isTrue(Boolean.TRUE.equals(snapshot.getReady()), "支付工具收款预交易快照未就绪");
        String transactionSn = directTransactionService.topup(convertToTopupRequest(request, snapshot), operator);
        log.info("支付工具收款完成，等待事务提交，tenantId={}, externalSourceCode={}, externalFundsFactSn={}, "
                        + "businessScene={}, businessSn={}, transactionSn={}, amount={}, currency={}",
                request.getTenantId(), request.getExternalSourceCode(), request.getExternalFundsFactSn(),
                request.getBusinessScene(), request.getBusinessSn(), transactionSn, request.getAmount(),
                request.getCurrency());
        return transactionSn;
    }

    private @Nullable String findEstablishedReceiveReplay(ReceiveByInstrumentRequest request) {
        Optional<FundsTransactionDTO> existing = fundsTransactionQueryService.findFundsTransactionByExternalFundsFact(
                request.getTenantId(), request.getExternalSourceCode(), request.getExternalFundsFactSn(),
                FundsEffectType.DIRECT);
        if (existing.isEmpty()) {
            return null;
        }
        FundsTransactionDTO transaction = existing.get();
        AssertUtils.isTrue(transaction.getState() == FundsTransactionState.CLOSED,
                "外部资金事实尚未成功完成，transactionSn = {}，status = {}",
                transaction.getSn(), transaction.getState());
        RouteSnapshotSpec routeSnapshot = fundsTransactionQueryService
                .findRouteSnapshotByTransactionSn(transaction.getSn())
                .orElse(null);
        AssertUtils.notNull(routeSnapshot, "已成立支付工具收款缺少 RouteSnapshot，transactionSn = {}",
                transaction.getSn());
        PaymentInstrumentRefSpec instrumentRef = routeSnapshot.getPaymentInstrumentRef();
        AssertUtils.notNull(instrumentRef, "已成立支付工具收款缺少支付工具快照，transactionSn = {}",
                transaction.getSn());
        Integer bindingVersion = bindingVersion(instrumentRef);
        AssertUtils.isTrue(transaction.getTransactionMode() == FundsTransactionMode.DIRECT
                        && transaction.getTransactionType() == DefaultFundsTransactionType.TOPUP
                        && Objects.equals(transaction.getAmount(), request.getAmount())
                        && transaction.getCurrency() == request.getCurrency()
                        && Objects.equals(instrumentRef.getInstrumentId(), request.getInstrumentSn())
                        && Objects.equals(bindingVersion, request.getExpectedBindingVersion()),
                "已成立支付工具收款请求参数不一致，transactionSn = {}，externalFundsFactSn = {}",
                transaction.getSn(), request.getExternalFundsFactSn());
        return transaction.getSn();
    }

    private @Nullable Integer bindingVersion(PaymentInstrumentRefSpec instrumentRef) {
        Object value = instrumentRef.getBindingSnapshot().get("bindingVersion");
        return value instanceof Number number ? number.intValue() : null;
    }

    private void validateReversalRequest(ReverseAuthorizationByPaymentInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "支付工具授权撤销 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getAuthorizationTransactionSn(), "原授权资金交易号不能为空");
        AssertUtils.notNull(request.getAmount(), "撤销金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "撤销金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "撤销币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "撤销业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "撤销业务流水号不能为空");
    }

    private void validateCompletionRequest(CompleteAuthorizationByPaymentInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "支付工具授权完成 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getAuthorizationTransactionSn(), "原授权资金交易号不能为空");
        AssertUtils.notNull(request.getAmount(), "完成金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "完成金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "完成币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "完成业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "完成业务流水号不能为空");
    }

    private FundsTransactionDTO getPaymentInstrumentAuthorization(
            Long tenantId, String authorizationTransactionSn, CurrencyIsoCode currency, String actionName) {
        FundsTransactionDTO authorization = fundsTransactionQueryService
                .queryFundsTransaction(authorizationTransactionSn)
                .orElse(null);
        AssertUtils.notNull(authorization, "原授权资金交易不存在，authorizationTransactionSn = {}",
                authorizationTransactionSn);
        AssertUtils.isTrue(Objects.equals(authorization.getTenantId(), tenantId),
                "原授权资金交易租户不一致，authorizationTransactionSn = {}", authorizationTransactionSn);
        AssertUtils.isTrue(authorization.getTransactionMode() == FundsTransactionMode.AUTHORIZATION
                        && authorization.getTransactionType() == DefaultFundsTransactionType.PAY,
                "{}必须引用 PAY 授权交易，authorizationTransactionSn = {}", actionName, authorizationTransactionSn);
        AssertUtils.isTrue(authorization.getCurrency() == currency,
                "{}币种与原授权不一致，authorizationTransactionSn = {}", actionName, authorizationTransactionSn);
        return authorization;
    }

    private PaymentInstrumentRefSpec getPaymentInstrumentRef(FundsTransactionDTO authorization) {
        RouteSnapshotSpec routeSnapshot = fundsTransactionQueryService
                .findRouteSnapshotByTransactionSn(authorization.getSn())
                .orElse(null);
        AssertUtils.notNull(routeSnapshot, "支付工具授权缺少 RouteSnapshot，transactionSn = {}",
                authorization.getSn());
        PaymentInstrumentRefSpec instrumentRef = routeSnapshot.getPaymentInstrumentRef();
        AssertUtils.notNull(instrumentRef, "支付工具授权缺少支付工具快照，transactionSn = {}",
                authorization.getSn());
        AssertUtils.isTrue(instrumentRef.getTenantId() == null
                        || Objects.equals(instrumentRef.getTenantId(), authorization.getTenantId()),
                "支付工具授权快照租户不一致，transactionSn = {}", authorization.getSn());
        return instrumentRef;
    }

    private FundsAccountId authorizationAccountId(PaymentInstrumentRefSpec instrumentRef, String transactionSn) {
        Object subjectId = instrumentRef.getBindingSnapshot().get("subjectId");
        Object subjectType = instrumentRef.getBindingSnapshot().get("subjectType");
        AssertUtils.isTrue(subjectId instanceof String value && StringUtils.hasText(value),
                "支付工具授权快照缺少账务主体 ID，transactionSn = {}", transactionSn);
        AssertUtils.isTrue(subjectType instanceof String value && FundsSubjectType.isLedgerPostableName(value),
                "支付工具授权快照账务主体类型非法，transactionSn = {}", transactionSn);
        return FundsAccountId.immutable((String) subjectId, (String) subjectType);
    }

    private FundsAuthorizationTransactionReversalRequest toReversalRequest(
            ReverseAuthorizationByPaymentInstrumentRequest request,
            FundsAccountId authorizationAccountId) {
        return new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(authorizationAccountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(
                        request.getAmount(), request.getCurrency())))
                .setAuthorizationTransactionSn(request.getAuthorizationTransactionSn())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setReversalTime(request.getReversalTime())
                .setDescription(request.getDescription());
    }

    private FundsAuthorizationTransactionCompleteRequest toCompleteRequest(
            CompleteAuthorizationByPaymentInstrumentRequest request,
            FundsAccountId authorizationAccountId) {
        return new FundsAuthorizationTransactionCompleteRequest()
                .setAccountId(authorizationAccountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(
                        request.getAmount(), request.getCurrency())))
                .setAuthorizationTransactionSn(request.getAuthorizationTransactionSn())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setCompletedTime(request.getCompletedTime())
                .setDescription(request.getDescription());
    }

    private void consumeControlReservation(CompleteAuthorizationByPaymentInstrumentRequest request,
                                           FundsTransactionDTO authorization,
                                           String authorizationSn) {
        SpendControlMovementDTO reservation = findControlReservation(authorization);
        if (reservation == null) {
            return;
        }
        String movementSn = controlConsumeMovementSn(request);
        spendControlConsumptionService.consume(new SpendControlTransactionConsumptionRequest()
                .setTenantId(request.getTenantId())
                .setMovementSn(movementSn)
                .setOriginalMovementSn(reservation.getMovementSn())
                .setTransactionSn(authorizationSn)
                .setBusinessScene(authorization.getBusinessScene())
                .setBusinessSn(authorization.getBusinessSn())
                .setTargetAccountId(reservation.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setMovementDigest(controlConsumeMovementDigest(request, reservation, movementSn))
                .setDescription(request.getDescription())
                .setContextVariables(completionContext(request)));
    }

    private String controlConsumeMovementSn(CompleteAuthorizationByPaymentInstrumentRequest request) {
        return FundsStableHashSupport.sha256(CONTROL_CONSUME_MOVEMENT_DOMAIN
                + "|" + request.getTenantId()
                + "|" + request.getAuthorizationTransactionSn()
                + "|" + request.getBusinessScene()
                + "|" + request.getBusinessSn());
    }

    private String controlConsumeMovementDigest(CompleteAuthorizationByPaymentInstrumentRequest request,
                                                SpendControlMovementDTO reservation,
                                                String movementSn) {
        Map<String, Object> values = new TreeMap<>();
        values.put("amount", request.getAmount());
        values.put("authorizationTransactionSn", request.getAuthorizationTransactionSn());
        values.put("businessScene", request.getBusinessScene());
        values.put("businessSn", request.getBusinessSn());
        values.put("currency", request.getCurrency().name());
        values.put("movementSn", movementSn);
        values.put("originalMovementSn", reservation.getMovementSn());
        values.put("tenantId", request.getTenantId());
        return SHA256_PREFIX + FundsStableHashSupport.sha256CanonicalJson(CONTROL_CONSUME_DIGEST_DOMAIN, values);
    }

    private String completionContext(CompleteAuthorizationByPaymentInstrumentRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("completionBusinessScene", request.getBusinessScene());
        values.put("completionBusinessSn", request.getBusinessSn());
        return WindJson.toJsonString(values);
    }

    private void releaseControlReservation(ReverseAuthorizationByPaymentInstrumentRequest request,
                                           FundsTransactionDTO authorization,
                                           String authorizationSn) {
        SpendControlMovementDTO reservation = findControlReservation(authorization);
        if (reservation == null) {
            return;
        }
        String movementSn = controlReleaseMovementSn(request);
        spendControlConsumptionService.release(new SpendControlTransactionConsumptionRequest()
                .setTenantId(request.getTenantId())
                .setMovementSn(movementSn)
                .setOriginalMovementSn(reservation.getMovementSn())
                .setTransactionSn(authorizationSn)
                .setBusinessScene(authorization.getBusinessScene())
                .setBusinessSn(authorization.getBusinessSn())
                .setTargetAccountId(reservation.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setMovementDigest(controlReleaseMovementDigest(request, reservation, movementSn))
                .setDescription(request.getDescription())
                .setContextVariables(reversalContext(request)));
    }

    private @Nullable SpendControlMovementDTO findControlReservation(FundsTransactionDTO authorization) {
        if (!StringUtils.hasText(authorization.getContextVariables())) {
            return null;
        }
        Map<String, Object> context = WindJson.parseObject(authorization.getContextVariables(), new TypeReference<>() {
        });
        if (context == null) {
            return null;
        }
        Object spendRuleDecision = context.get(FundsInstructionContextKeys.SPEND_RULE_DECISION);
        if (!(spendRuleDecision instanceof Map<?, ?> decision)) {
            return null;
        }
        String reservationSn = Objects.toString(decision.get("controlReservationSn"), null);
        if (!StringUtils.hasText(reservationSn)) {
            return null;
        }
        SpendControlMovementDTO reservation = spendControlMovementService.findSpendControlMovement(
                authorization.getTenantId(), reservationSn);
        AssertUtils.notNull(reservation, "支付工具授权控制预留不存在，transactionSn = {}, movementSn = {}",
                authorization.getSn(), reservationSn);
        return reservation;
    }

    private String controlReleaseMovementSn(ReverseAuthorizationByPaymentInstrumentRequest request) {
        return FundsStableHashSupport.sha256(CONTROL_RELEASE_MOVEMENT_DOMAIN
                + "|" + request.getTenantId()
                + "|" + request.getAuthorizationTransactionSn()
                + "|" + request.getBusinessScene()
                + "|" + request.getBusinessSn());
    }

    private String controlReleaseMovementDigest(ReverseAuthorizationByPaymentInstrumentRequest request,
                                                SpendControlMovementDTO reservation,
                                                String movementSn) {
        Map<String, Object> values = new TreeMap<>();
        values.put("amount", request.getAmount());
        values.put("authorizationTransactionSn", request.getAuthorizationTransactionSn());
        values.put("businessScene", request.getBusinessScene());
        values.put("businessSn", request.getBusinessSn());
        values.put("currency", request.getCurrency().name());
        values.put("movementSn", movementSn);
        values.put("originalMovementSn", reservation.getMovementSn());
        values.put("tenantId", request.getTenantId());
        return SHA256_PREFIX + FundsStableHashSupport.sha256CanonicalJson(CONTROL_RELEASE_DIGEST_DOMAIN, values);
    }

    private String reversalContext(ReverseAuthorizationByPaymentInstrumentRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("reversalBusinessScene", request.getBusinessScene());
        values.put("reversalBusinessSn", request.getBusinessSn());
        return WindJson.toJsonString(values);
    }

    private void validateReceiveRequest(ReceiveByInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "支付工具收款 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAmount(), "收款金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "收款金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "收款币种不能为空");
        AssertUtils.notNull(request.getFundsSourceAccountId(), "收款外部资金来源账户不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getFundsSourceAccountId()),
                "收款外部资金来源账户必须是外部账户");
        AssertUtils.hasText(request.getExternalRailCode(), "收款外部 rail 编码不能为空");
        AssertUtils.hasText(request.getChannelTransactionSn(), "收款渠道交易流水不能为空");
        AssertUtils.hasText(request.getExternalSourceCode(), "收款外部资金事实来源编码不能为空");
        AssertUtils.hasText(request.getExternalFundsFactSn(), "收款外部资金事实流水不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "收款业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "收款业务流水号不能为空");
        AssertUtils.notNull(request.getExpectedBindingVersion(), "支付工具收款绑定版本不能为空");
    }

    private ResolvePaymentInstrumentPreTransactionSnapshotRequest toPreTransactionRequest(
            ReceiveByInstrumentRequest request) {
        return new ResolvePaymentInstrumentPreTransactionSnapshotRequest()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(PaymentInstrumentAction.RECEIVE)
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(PaymentInstrumentBindingRole.RECEIVE_SUBJECT)
                .setExpectedBindingVersion(request.getExpectedBindingVersion())
                .setRelationType(SpendSubjectFundingRelationType.SETTLEMENT_TARGET)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn());
    }

    private FundsTransactionTopupRequest convertToTopupRequest(ReceiveByInstrumentRequest request,
                                                               PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        PaymentInstrumentCapabilityDecisionDTO instrument = snapshot.getPaymentInstrumentCapability();
        AssertUtils.hasText(instrument.getChannelCode(), "支付工具接入渠道编码不能为空");
        ExternalFundsRailDecision railDecision = ExternalFundsRailResolver.requireReceiveRailDecision(
                instrument.getInstrumentType(), request.getExternalRailCode());
        return new FundsTransactionTopupRequest()
                .setAccountId(snapshot.getTargetAccountId())
                .setFundsSourceAccountId(request.getFundsSourceAccountId())
                .setChannel(railDecision.transactionChannel())
                .setExternalRailCode(railDecision.externalRailCode())
                .setChannelTransactionSn(request.getChannelTransactionSn())
                .setProviderCode(instrument.getChannelCode())
                .setExternalSourceCode(request.getExternalSourceCode())
                .setExternalFundsFactSn(request.getExternalFundsFactSn())
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(request.getAmount(),
                        request.getCurrency())))
                .setPaymentInstrumentRef(paymentInstrumentRef(snapshot))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(receiveContext(snapshot)))
                .setDescription(request.getDescription());
    }

    private Map<String, Object> receiveContext(PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        PaymentInstrumentCapabilityDecisionDTO instrument = snapshot.getPaymentInstrumentCapability();
        FundsAccountId targetAccountId = snapshot.getTargetAccountId();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("instrumentSn", snapshot.getInstrumentSn());
        values.put("instrumentAction", PaymentInstrumentAction.RECEIVE.name());
        values.put("instrumentBindingRole", snapshot.getBindingRole().name());
        values.put("instrumentBindingSn", instrument.getBindingSn());
        values.put("instrumentBindingVersion", instrument.getBindingVersion());
        values.put("fundingRelationSn", snapshot.getFundingResponsibility().getRelationSn());
        values.put("fundingRelationType", snapshot.getRelationType().name());
        values.put("targetAccountId", targetAccountId.id());
        values.put("targetAccountType", targetAccountId.type());
        return Map.copyOf(values);
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        PaymentInstrumentCapabilityDecisionDTO instrument = snapshot.getPaymentInstrumentCapability();
        AssertUtils.notNull(instrument, "支付工具快照不能为空");
        assertPaymentInstrumentSnapshotReady(instrument);
        return ImmutablePaymentInstrumentRefSpec.builder()
                .tenantId(instrument.getTenantId())
                .instrumentId(instrument.getInstrumentSn())
                .instrumentType(instrument.getInstrumentType())
                .instrumentNo(instrument.getInstrumentNo())
                .ownerId(instrument.getOwnerId())
                .ownerType(instrument.getOwnerType().name())
                .currency(instrument.getCurrency())
                .status(instrument.getState().name())
                .bindingSnapshot(bindingSnapshot(snapshot, instrument))
                .description(instrument.getDescription())
                .build();
    }

    private void assertPaymentInstrumentSnapshotReady(PaymentInstrumentCapabilityDecisionDTO instrument) {
        AssertUtils.hasText(instrument.getInstrumentSn(), "支付工具快照工具号不能为空");
        AssertUtils.hasText(instrument.getInstrumentNo(), "支付工具快照展示号不能为空");
        AssertUtils.hasText(instrument.getOwnerId(), "支付工具快照归属主体 ID 不能为空");
        AssertUtils.notNull(instrument.getOwnerType(), "支付工具快照归属主体类型不能为空");
        AssertUtils.hasText(instrument.getInstrumentType(), "支付工具快照类型不能为空");
        AssertUtils.notNull(instrument.getCurrency(), "支付工具快照币种不能为空");
        AssertUtils.notNull(instrument.getState(), "支付工具快照状态不能为空");
        AssertUtils.hasText(instrument.getBindingSn(), "支付工具绑定快照绑定号不能为空");
        AssertUtils.notNull(instrument.getBindingVersion(), "支付工具绑定快照版本不能为空");
        AssertUtils.notNull(instrument.getBindingRole(), "支付工具绑定快照角色不能为空");
        AssertUtils.notNull(instrument.getSubjectType(), "支付工具绑定快照主体类型不能为空");
        AssertUtils.hasText(instrument.getSubjectId(), "支付工具绑定快照主体 ID 不能为空");
    }

    private Map<String, Object> bindingSnapshot(PaymentInstrumentPreTransactionSnapshotDTO snapshot,
                                                PaymentInstrumentCapabilityDecisionDTO instrument) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingSn", instrument.getBindingSn());
        values.put("bindingVersion", instrument.getBindingVersion());
        values.put("bindingRole", instrument.getBindingRole().name());
        values.put("subjectType", instrument.getSubjectType().name());
        values.put("subjectId", instrument.getSubjectId());
        values.put("admissionAction", snapshot.getAction().name());
        return Map.copyOf(values);
    }
}

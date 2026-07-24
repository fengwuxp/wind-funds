package com.wind.funds.transaction.application.instrument.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.application.support.ExternalFundsRailResolver;
import com.wind.funds.transaction.application.support.ExternalFundsRailResolver.ExternalFundsRailDecision;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentTransactionApplicationService;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.ReceiveByInstrumentRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 支付工具交易应用服务实现。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentTransactionApplicationServiceImpl
        implements PaymentInstrumentTransactionApplicationService {

    private final PaymentInstrumentAuthorizationProcessor authorizationProcessor;

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final FundsDirectTransactionService directTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String authorizeByInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                 @NonNull WindOperator operator) {
        return authorizationProcessor.authorizeByInstrument(request, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String receiveByInstrument(@NonNull ReceiveByInstrumentRequest request,
                                               @NonNull WindOperator operator) {
        validateReceiveRequest(request);
        String establishedTransactionSn = findEstablishedReceiveReplay(request);
        if (establishedTransactionSn != null) {
            return establishedTransactionSn;
        }
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                preTransactionSnapshotApplicationService.resolvePreTransactionSnapshot(toPreTransactionRequest(request));
        AssertUtils.isTrue(Boolean.TRUE.equals(snapshot.getReady()), "支付工具收款预交易快照未就绪");
        return directTransactionService.topup(convertToTopupRequest(request, snapshot), operator);
    }

    private @Nullable String findEstablishedReceiveReplay(ReceiveByInstrumentRequest request) {
        Optional<FundsTransactionDTO> existing = fundsTransactionQueryService.findFundsTransactionByExternalFundsFact(
                request.getTenantId(), request.getExternalSourceCode(), request.getExternalFundsFactSn(),
                FundsEffectType.DIRECT);
        if (existing.isEmpty()) {
            return null;
        }
        FundsTransactionDTO transaction = existing.get();
        AssertUtils.isTrue(transaction.getStatus() == FundsTransactionStatus.CLOSED,
                "外部资金事实尚未成功完成，transactionSn = {}，status = {}",
                transaction.getSn(), transaction.getStatus());
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
                .currency(instrument.getCurrency().name())
                .status(instrument.getStatus().name())
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
        AssertUtils.notNull(instrument.getStatus(), "支付工具快照状态不能为空");
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

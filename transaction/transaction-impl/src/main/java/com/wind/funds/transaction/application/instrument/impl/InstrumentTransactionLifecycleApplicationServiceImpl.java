package com.wind.funds.transaction.application.instrument.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.instrument.AuthorizationAdmissionApplicationService;
import com.wind.funds.wallet.application.instrument.InstrumentTransactionLifecycleApplicationService;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.application.support.WalletExternalFundsRailSupport;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.PayOutByRailRequest;
import com.wind.funds.wallet.model.request.ReceiveByInstrumentRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付工具交易生命周期应用服务实现。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
@AllArgsConstructor
public class InstrumentTransactionLifecycleApplicationServiceImpl
        implements InstrumentTransactionLifecycleApplicationService {

    private final AuthorizationAdmissionApplicationService authorizationAdmissionApplicationService;

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final FundsDirectTransactionService directTransactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String authorizeByInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                 @NonNull WindOperator operator) {
        return authorizationAdmissionApplicationService.authorizeByInstrument(request, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String receiveByInstrument(@NonNull ReceiveByInstrumentRequest request,
                                               @NonNull WindOperator operator) {
        validateReceiveRequest(request);
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                preTransactionSnapshotApplicationService.resolvePreTransactionSnapshot(toPreTransactionRequest(request));
        AssertUtils.isTrue(Boolean.TRUE.equals(snapshot.getReady()), "支付工具收款预交易快照未就绪");
        return directTransactionService.topup(convertToTopupRequest(request, snapshot), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String payOutByRail(@NonNull PayOutByRailRequest request,
                                        @NonNull WindOperator operator) {
        validatePayOutByRailRequest(request);
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                preTransactionSnapshotApplicationService.resolvePreTransactionSnapshot(toPreTransactionRequest(request));
        AssertUtils.isTrue(Boolean.TRUE.equals(snapshot.getReady()), "支付工具出款预交易快照未就绪");
        AssertUtils.equals(request.getPayoutSourceAccountId(), snapshot.getTargetAccountId(),
                "支付工具出款资金来源账户与预交易快照目标账户不一致");
        return directTransactionService.withdraw(convertToWithdrawRequest(request, snapshot), operator);
    }

    private void validateReceiveRequest(ReceiveByInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "支付工具收款 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAmount(), "收款金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "收款金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "收款币种不能为空");
        AssertUtils.notNull(request.getFundsSourceAccountId(), "收款外部资金来源账户不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getFundsSourceAccountId()),
                "收款外部资金来源账户必须是外部账户");
        AssertUtils.hasText(request.getChannelCode(), "收款渠道编码不能为空");
        AssertUtils.hasText(request.getChannelTransactionSn(), "收款渠道交易流水不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "收款业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "收款业务流水号不能为空");
        AssertUtils.notNull(request.getExpectedBindingVersion(), "支付工具收款绑定版本不能为空");
    }

    private void validatePayOutByRailRequest(PayOutByRailRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "支付工具出款 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getPayoutSourceAccountId(), "出款资金来源账户不能为空");
        AssertUtils.notNull(request.getPayeeAccountId(), "出款外部收款账户不能为空");
        AssertUtils.isTrue(DefaultFundsAccountType.isExternalAccount(request.getPayeeAccountId()),
                "出款外部收款账户必须是外部账户");
        AssertUtils.hasText(request.getReferenceFreezeSn(), "出款提现冻结流水号不能为空");
        AssertUtils.notNull(request.getAmount(), "出款金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "出款金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "出款币种不能为空");
        AssertUtils.hasText(request.getRailCode(), "出款 rail 编码不能为空");
        WalletExternalFundsRailSupport.requirePayoutRailCode(request.getRailCode());
        AssertUtils.hasText(request.getReceiverReference(), "出款收款人引用不能为空");
        AssertUtils.hasText(request.getExternalPayoutSn(), "外部出款流水不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "出款业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "出款业务流水号不能为空");
        AssertUtils.notNull(request.getExpectedBindingVersion(), "支付工具出款绑定版本不能为空");
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

    private ResolvePaymentInstrumentPreTransactionSnapshotRequest toPreTransactionRequest(
            PayOutByRailRequest request) {
        return new ResolvePaymentInstrumentPreTransactionSnapshotRequest()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(PaymentInstrumentAction.WITHDRAW)
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(request.getExpectedBindingVersion())
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn());
    }

    private FundsTransactionTopupRequest convertToTopupRequest(ReceiveByInstrumentRequest request,
                                                               PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        return new FundsTransactionTopupRequest()
                .setAccountId(snapshot.getTargetAccountId())
                .setFundsSourceAccountId(request.getFundsSourceAccountId())
                .setChannel(WalletExternalFundsRailSupport.resolveReceiveChannel(request.getChannelCode()))
                .setChannelTransactionSn(request.getChannelTransactionSn())
                .setChannelId(request.getChannelId())
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(request.getAmount(),
                        request.getCurrency())))
                .setPaymentInstrumentRef(paymentInstrumentRef(snapshot))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(receiveContext(snapshot)))
                .setDescription(request.getDescription());
    }

    private FundsTransactionWithdrawRequest convertToWithdrawRequest(
            PayOutByRailRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        return new FundsTransactionWithdrawRequest()
                .setAccountId(snapshot.getTargetAccountId())
                .setPayeeId(request.getPayeeAccountId())
                .setReferenceFreezeSn(request.getReferenceFreezeSn())
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(request.getAmount(),
                        request.getCurrency())))
                .setPaymentInstrumentRef(paymentInstrumentRef(snapshot))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(payoutContext(request, snapshot)))
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

    private Map<String, Object> payoutContext(PayOutByRailRequest request,
                                              PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        PaymentInstrumentCapabilityDecisionDTO instrument = snapshot.getPaymentInstrumentCapability();
        FundsAccountId targetAccountId = snapshot.getTargetAccountId();
        FundsAccountId payeeAccountId = request.getPayeeAccountId();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("instrumentSn", snapshot.getInstrumentSn());
        values.put("instrumentAction", PaymentInstrumentAction.WITHDRAW.name());
        values.put("instrumentBindingRole", snapshot.getBindingRole().name());
        values.put("instrumentBindingSn", instrument.getBindingSn());
        values.put("instrumentBindingVersion", instrument.getBindingVersion());
        values.put("fundingRelationSn", snapshot.getFundingResponsibility().getRelationSn());
        values.put("fundingRelationType", snapshot.getRelationType().name());
        values.put("targetAccountId", targetAccountId.id());
        values.put("targetAccountType", targetAccountId.type());
        values.put("payoutRailCode", WalletExternalFundsRailSupport.requirePayoutRailCode(request.getRailCode()));
        values.put("receiverReference", request.getReceiverReference());
        values.put("externalPayoutSn", request.getExternalPayoutSn());
        values.put("referenceFreezeSn", request.getReferenceFreezeSn());
        values.put("payeeAccountId", payeeAccountId.id());
        values.put("payeeAccountType", payeeAccountId.type());
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

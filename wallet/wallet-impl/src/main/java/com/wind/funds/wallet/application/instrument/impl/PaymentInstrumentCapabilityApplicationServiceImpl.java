package com.wind.funds.wallet.application.instrument.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentCapabilityApplicationService;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingState;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentQuery;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentCapabilityRequest;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付工具能力准入应用服务实现。
 *
 * @author Codex
 * @date 2026-06-16
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentCapabilityApplicationServiceImpl
        implements PaymentInstrumentCapabilityApplicationService {

    private final PaymentInstrumentService paymentInstrumentService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull PaymentInstrumentCapabilityDecisionDTO resolvePaymentInstrumentCapability(
            @NonNull ResolvePaymentInstrumentCapabilityRequest request) {
        validateRequest(request);
        PaymentInstrumentDTO instrument = resolveInstrument(request);
        assertInstrumentCanUse(instrument, request);
        PaymentInstrumentBindingDTO binding = request.getBindingRole() == null ? null : resolveBinding(request);
        assertBindingVersionMatched(binding, request);
        return toDecision(instrument, binding, request);
    }

    private void validateRequest(ResolvePaymentInstrumentCapabilityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "支付工具能力准入 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.isTrue(request.getExpectedBindingVersion() == null || request.getBindingRole() != null,
                "校验支付工具绑定版本时绑定角色不能为空");
    }

    private PaymentInstrumentDTO resolveInstrument(ResolvePaymentInstrumentCapabilityRequest request) {
        List<PaymentInstrumentDTO> records = paymentInstrumentService.queryPaymentInstruments(
                new PaymentInstrumentQuery()
                        .setTenantId(request.getTenantId())
                        .setSn(request.getInstrumentSn()),
                DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isFalse(records.isEmpty(), "支付工具不存在，instrumentSn = {}", request.getInstrumentSn());
        AssertUtils.isTrue(records.size() == 1, "支付工具不唯一，instrumentSn = {}", request.getInstrumentSn());
        return records.getFirst();
    }

    private void assertInstrumentCanUse(PaymentInstrumentDTO instrument,
                                        ResolvePaymentInstrumentCapabilityRequest request) {
        AssertUtils.isTrue(instrument.getStatus() == FundsAccountStatus.ACTIVE,
                "支付工具状态不可用，instrumentSn = {}",
                request.getInstrumentSn());
        AssertUtils.isTrue(instrument.getCurrency() == request.getCurrency(),
                "支付工具币种与请求币种不一致，instrumentSn = {}",
                request.getInstrumentSn());
        AssertUtils.isTrue(isCurrentEffective(instrument.getValidFrom(), instrument.getValidTo()),
                "支付工具不在当前有效期内，instrumentSn = {}",
                request.getInstrumentSn());
        AssertUtils.isTrue(supportsAction(instrument.getFlowDirection(), request.getAction()),
                "支付工具资金流向不支持当前动作，instrumentSn = {}, action = {}",
                request.getInstrumentSn(),
                request.getAction());
    }

    private PaymentInstrumentBindingDTO resolveBinding(ResolvePaymentInstrumentCapabilityRequest request) {
        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(request.getTenantId())
                        .setInstrumentSn(request.getInstrumentSn())
                        .setBindingRole(request.getBindingRole())
                        .setCurrency(request.getCurrency())
                        .setDefaultBinding(Boolean.TRUE)
                        .setState(PaymentInstrumentBindingState.ACTIVE),
                DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isFalse(records.isEmpty(),
                "默认支付工具绑定不存在，instrumentSn = {}, bindingRole = {}, currency = {}",
                request.getInstrumentSn(),
                request.getBindingRole(),
                request.getCurrency());
        AssertUtils.isTrue(records.size() == 1,
                "默认支付工具绑定不唯一，instrumentSn = {}, bindingRole = {}, currency = {}",
                request.getInstrumentSn(),
                request.getBindingRole(),
                request.getCurrency());
        return records.getFirst();
    }

    private void assertBindingVersionMatched(PaymentInstrumentBindingDTO binding,
                                             ResolvePaymentInstrumentCapabilityRequest request) {
        if (request.getExpectedBindingVersion() == null) {
            return;
        }
        AssertUtils.notNull(binding, "校验支付工具绑定版本时绑定快照不能为空");
        AssertUtils.isTrue(request.getExpectedBindingVersion().equals(binding.getVersion()),
                "支付工具绑定版本已变更，bindingSn = {}, expectedVersion = {}, actualVersion = {}",
                binding.getSn(),
                request.getExpectedBindingVersion(),
                binding.getVersion());
    }

    private boolean isCurrentEffective(LocalDateTime validFrom, LocalDateTime validTo) {
        LocalDateTime now = LocalDateTime.now();
        return (validFrom == null || !validFrom.isAfter(now)) && (validTo == null || validTo.isAfter(now));
    }

    private boolean supportsAction(PaymentInstrumentFlowDirection direction, PaymentInstrumentAction action) {
        return switch (action) {
            case RECEIVE -> direction == PaymentInstrumentFlowDirection.INBOUND
                    || direction == PaymentInstrumentFlowDirection.BIDIRECTIONAL;
            case REFUND -> true;
            default -> direction == PaymentInstrumentFlowDirection.OUTBOUND
                    || direction == PaymentInstrumentFlowDirection.BIDIRECTIONAL;
        };
    }

    private PaymentInstrumentCapabilityDecisionDTO toDecision(PaymentInstrumentDTO instrument,
                                                              PaymentInstrumentBindingDTO binding,
                                                              ResolvePaymentInstrumentCapabilityRequest request) {
        PaymentInstrumentCapabilityDecisionDTO decision = new PaymentInstrumentCapabilityDecisionDTO()
                .setTenantId(request.getTenantId())
                .setInstrumentId(instrument.getId())
                .setInstrumentSn(instrument.getSn())
                .setInstrumentNo(instrument.getInstrumentNo())
                .setOwnerId(instrument.getOwnerId())
                .setOwnerType(instrument.getOwnerType())
                .setInstrumentType(instrument.getInstrumentType())
                .setFlowDirection(instrument.getFlowDirection())
                .setChannelCode(instrument.getChannelCode())
                .setAction(request.getAction())
                .setCurrency(request.getCurrency())
                .setStatus(instrument.getStatus())
                .setDescription(instrument.getDescription());
        if (binding == null) {
            return decision;
        }
        return decision.setBindingId(binding.getId())
                .setBindingSn(binding.getSn())
                .setBindingRole(binding.getBindingRole())
                .setSubjectId(binding.getSubjectId())
                .setSubjectType(binding.getSubjectType())
                .setBindingVersion(binding.getVersion())
                .setDefaultBinding(binding.getDefaultBinding());
    }
}

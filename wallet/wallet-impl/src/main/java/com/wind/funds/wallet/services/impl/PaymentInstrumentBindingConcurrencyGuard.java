package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBindingGuard;
import com.wind.funds.wallet.dal.mapper.PaymentInstrumentBindingGuardMapper;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 支付工具绑定并发保护组件。
 *
 * @author Codex
 * @date 2026-06-25
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentBindingConcurrencyGuard {

    private static final String ACTIVE_DEFAULT_BINDING_GUARD = "ACTIVE_DEFAULT_BINDING";

    private final PaymentInstrumentBindingGuardMapper paymentInstrumentBindingGuardMapper;

    public void lockActiveDefaultBindingScope(@NonNull PaymentInstrumentBindingDTO binding) {
        if (!requiresActiveDefaultGuard(binding)) {
            return;
        }
        assertGuardScopePresent(binding);
        Long guardId = paymentInstrumentBindingGuardMapper.selectGuardIdForUpdate(binding.getTenantId(),
                binding.getInstrumentSn(),
                binding.getBindingRole().name(),
                binding.getCurrency().name(),
                ACTIVE_DEFAULT_BINDING_GUARD);
        if (guardId != null) {
            return;
        }
        ensureGuardRow(binding);
        guardId = paymentInstrumentBindingGuardMapper.selectGuardIdForUpdate(binding.getTenantId(),
                binding.getInstrumentSn(),
                binding.getBindingRole().name(),
                binding.getCurrency().name(),
                ACTIVE_DEFAULT_BINDING_GUARD);
        AssertUtils.notNull(guardId,
                "支付工具默认绑定并发保护行不存在，instrumentSn = {}, bindingRole = {}, currency = {}",
                binding.getInstrumentSn(),
                binding.getBindingRole(),
                binding.getCurrency());
    }

    private boolean requiresActiveDefaultGuard(PaymentInstrumentBindingDTO binding) {
        return Boolean.TRUE.equals(binding.getDefaultBinding()) && binding.getStatus() == FundsAccountStatus.ACTIVE;
    }

    private void assertGuardScopePresent(PaymentInstrumentBindingDTO binding) {
        AssertUtils.notNull(binding.getTenantId(), "支付工具默认绑定 tenantId 不能为空");
        AssertUtils.hasText(binding.getInstrumentSn(), "支付工具默认绑定 instrumentSn 不能为空");
        AssertUtils.notNull(binding.getBindingRole(), "支付工具默认绑定 bindingRole 不能为空");
        AssertUtils.notNull(binding.getCurrency(), "支付工具默认绑定 currency 不能为空");
    }

    private void ensureGuardRow(PaymentInstrumentBindingDTO binding) {
        PaymentInstrumentBindingGuard guard = new PaymentInstrumentBindingGuard();
        guard.setTenantId(binding.getTenantId());
        guard.setInstrumentSn(binding.getInstrumentSn());
        guard.setBindingRole(binding.getBindingRole());
        guard.setCurrency(binding.getCurrency());
        guard.setGuardType(ACTIVE_DEFAULT_BINDING_GUARD);
        try {
            paymentInstrumentBindingGuardMapper.insertSelective(guard);
        } catch (DuplicateKeyException ignored) {
            // 并发下其他事务已创建同一 scope 的保护行，后续 FOR UPDATE 会串行化该 scope。
        }
    }
}

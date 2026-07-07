package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.locks.JdkLockFactory;
import com.wind.common.locks.LockFactory;
import com.wind.common.locks.WindLock;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBindingGuard;
import com.wind.funds.wallet.dal.mapper.PaymentInstrumentBindingGuardMapper;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private static final String ACTIVE_PRIORITY_BINDING_GUARD_PREFIX = "ACTIVE_PRIORITY_BINDING:";

    private static final String LOCK_PREFIX = "funds:payment-instrument-binding:";

    private static final LockFactory LOCK_FACTORY = new JdkLockFactory();

    private final PaymentInstrumentBindingGuardMapper paymentInstrumentBindingGuardMapper;

    public void lockActiveDefaultBindingScope(@NonNull PaymentInstrumentBindingDTO binding) {
        if (!requiresActiveDefaultGuard(binding)) {
            return;
        }
        lockGuardScope(binding, ACTIVE_DEFAULT_BINDING_GUARD, "支付工具默认绑定");
    }

    public void lockActivePriorityBindingScope(@NonNull PaymentInstrumentBindingDTO binding) {
        if (binding.getStatus() != FundsAccountStatus.ACTIVE) {
            return;
        }
        int priority = binding.getPriority() == null ? 0 : binding.getPriority();
        lockGuardScope(binding, ACTIVE_PRIORITY_BINDING_GUARD_PREFIX + priority, "支付工具绑定优先级");
    }

    private void lockGuardScope(PaymentInstrumentBindingDTO binding, String guardType, String scopeName) {
        assertGuardScopePresent(binding, scopeName);
        lockJvmScope(binding, guardType);
        Long guardId = selectGuardIdForUpdate(binding, guardType);
        if (guardId != null) {
            return;
        }
        ensureGuardRow(binding, guardType);
        guardId = selectGuardIdForUpdate(binding, guardType);
        AssertUtils.notNull(guardId,
                "{}并发保护行不存在，instrumentSn = {}, bindingRole = {}, currency = {}, guardType = {}",
                scopeName,
                binding.getInstrumentSn(),
                binding.getBindingRole(),
                binding.getCurrency(),
                guardType);
    }

    private void lockJvmScope(PaymentInstrumentBindingDTO binding, String guardType) {
        WindLock lock = LOCK_FACTORY.apply(LOCK_PREFIX
                + binding.getTenantId()
                + ":"
                + binding.getInstrumentSn()
                + ":"
                + binding.getBindingRole().name()
                + ":"
                + binding.getCurrency().name()
                + ":"
                + guardType);
        lock.lock();
        boolean unlockImmediately = true;
        try {
            unlockImmediately = !registerTransactionCompletionUnlock(lock);
        } finally {
            if (unlockImmediately) {
                lock.unlock();
            }
        }
    }

    private static boolean registerTransactionCompletionUnlock(WindLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
        return true;
    }

    private boolean requiresActiveDefaultGuard(PaymentInstrumentBindingDTO binding) {
        return Boolean.TRUE.equals(binding.getDefaultBinding()) && binding.getStatus() == FundsAccountStatus.ACTIVE;
    }

    private void assertGuardScopePresent(PaymentInstrumentBindingDTO binding, String scopeName) {
        AssertUtils.notNull(binding.getTenantId(), "{} tenantId 不能为空", scopeName);
        AssertUtils.hasText(binding.getInstrumentSn(), "{} instrumentSn 不能为空", scopeName);
        AssertUtils.notNull(binding.getBindingRole(), "{} bindingRole 不能为空", scopeName);
        AssertUtils.notNull(binding.getCurrency(), "{} currency 不能为空", scopeName);
    }

    private Long selectGuardIdForUpdate(PaymentInstrumentBindingDTO binding, String guardType) {
        return paymentInstrumentBindingGuardMapper.selectGuardIdForUpdate(binding.getTenantId(),
                binding.getInstrumentSn(),
                binding.getBindingRole().name(),
                binding.getCurrency().name(),
                guardType);
    }

    private void ensureGuardRow(PaymentInstrumentBindingDTO binding, String guardType) {
        PaymentInstrumentBindingGuard guard = new PaymentInstrumentBindingGuard();
        guard.setTenantId(binding.getTenantId());
        guard.setInstrumentSn(binding.getInstrumentSn());
        guard.setBindingRole(binding.getBindingRole());
        guard.setCurrency(binding.getCurrency());
        guard.setGuardType(guardType);
        try {
            paymentInstrumentBindingGuardMapper.insertSelective(guard);
        } catch (DuplicateKeyException ignored) {
            // 并发下其他事务已创建同一 scope 的保护行，后续 FOR UPDATE 会串行化该 scope。
        }
    }
}

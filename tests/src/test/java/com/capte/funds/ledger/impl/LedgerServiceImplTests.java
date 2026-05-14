package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.Ledger;
import com.capte.funds.ledger.dal.mapper.LedgerMapper;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.integration.funds.ledger.enums.EntrySide;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerServiceImplTests {

    @Test
    void updateLedgerBalanceShouldRejectBrokenMinimumNormalBalanceBeforeUpdate() {
        AtomicInteger updateCount = new AtomicInteger();
        LedgerServiceImpl service = new LedgerServiceImpl(mapper(creditNormalLedger(), updateCount, 1));

        assertThatThrownBy(() -> service.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                .setId(99L)
                .setDebitAmountDelta(1_200L)
                .setCreditAmountDelta(0L)
                .setMinimumNormalBalance(0L)))
                .isInstanceOf(RuntimeException.class);

        assertThat(updateCount).hasValue(0);
    }

    @Test
    void updateLedgerBalanceShouldAllowSatisfiedMinimumNormalBalance() {
        AtomicInteger updateCount = new AtomicInteger();
        LedgerServiceImpl service = new LedgerServiceImpl(mapper(creditNormalLedger(), updateCount, 1));

        service.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                .setId(99L)
                .setDebitAmountDelta(300L)
                .setCreditAmountDelta(0L)
                .setMinimumNormalBalance(0L));

        assertThat(updateCount).hasValue(1);
    }

    @Test
    void testNormalBalanceShouldNotBePersisted() {
        assertThat(Arrays.stream(Ledger.class.getDeclaredFields())
                .map(field -> field.getName()))
                .contains("debitAmount", "creditAmount", "normalBalanceSide")
                .doesNotContain("normalBalance");
    }

    private static Ledger creditNormalLedger() {
        Ledger result = new Ledger();
        result.setId(99L);
        result.setDebitAmount(0L);
        result.setCreditAmount(1_000L);
        result.setNormalBalanceSide(EntrySide.CREDIT);
        result.setVersion(0);
        return result;
    }

    private static LedgerMapper mapper(Ledger ledger, AtomicInteger updateCount, int updateResult) {
        return (LedgerMapper) Proxy.newProxyInstance(
                LedgerMapper.class.getClassLoader(),
                new Class<?>[]{LedgerMapper.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return invokeObjectMethod(proxy, method, args);
                    }
                    if ("selectOneById".equals(method.getName())) {
                        return ledger;
                    }
                    if ("updateByQuery".equals(method.getName())) {
                        updateCount.incrementAndGet();
                        assertThat(args[1]).isInstanceOf(QueryWrapper.class);
                        return updateResult;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "Proxy(" + proxy.getClass().getInterfaces()[0].getSimpleName() + ")";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }
}

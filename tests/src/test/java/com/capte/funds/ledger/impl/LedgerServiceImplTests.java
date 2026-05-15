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

    /**
     * 场景：更新账本余额会突破最低正常余额约束。
     * 输入：贷方正常余额账本当前正常余额 1000，本次借方增加 1200，最低正常余额为 0。
     * 输出：更新失败且不写库。
     * 预期：服务在执行 update 前拒绝会导致余额跌破下限的请求。
     * 红线：账本余额约束不得依赖数据库更新后补偿，不能先写入再发现透支。
     */
    @Test
    void testUpdateLedgerBalanceShouldRejectBrokenMinimumNormalBalanceBeforeUpdate() {
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

    /**
     * 场景：更新账本余额后仍满足最低正常余额约束。
     * 输入：贷方正常余额账本当前正常余额 1000，本次借方增加 300，最低正常余额为 0。
     * 输出：执行一次余额更新。
     * 预期：正常余额仍为正时允许持久化增量。
     * 红线：余额控制只拦截破坏不变量的更新，不得误伤合法账务变动。
     */
    @Test
    void testUpdateLedgerBalanceShouldAllowSatisfiedMinimumNormalBalance() {
        AtomicInteger updateCount = new AtomicInteger();
        LedgerServiceImpl service = new LedgerServiceImpl(mapper(creditNormalLedger(), updateCount, 1));

        service.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                .setId(99L)
                .setDebitAmountDelta(300L)
                .setCreditAmountDelta(0L)
                .setMinimumNormalBalance(0L));

        assertThat(updateCount).hasValue(1);
    }

    /**
     * 场景：账本实体字段保护。
     * 输入：Ledger 实体字段集合。
     * 输出：确认只持久化借方、贷方和正常余额方向，不持久化派生正常余额。
     * 预期：normalBalance 作为派生口径不进入 Entity 字段。
     * 红线：不得把可推导余额快照误作为账本事实字段持久化。
     */
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

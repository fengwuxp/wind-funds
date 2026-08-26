package com.wind.funds.transaction.services.impl;

import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FrozenOrder 公共 CRUD 退役契约。
 */
class FundsFrozenOrderServiceImplTests {

    private static final List<String> LEGACY_FACE_TYPES = List.of(
            "com.wind.funds.transaction.services.FundsFrozenOrderService",
            "com.wind.funds.transaction.model.dto.FundsFrozenOrderDTO",
            "com.wind.funds.transaction.model.query.FundsFrozenOrderQuery",
            "com.wind.funds.transaction.model.request.CreateFundsFrozenOrderRequest"
    );

    private static final List<String> LEGACY_IMPL_TYPES = List.of(
            "com.wind.funds.transaction.services.impl.FundsFrozenOrderServiceImpl",
            "com.wind.funds.transaction.mapstruct.FundsFrozenOrderConverter"
    );

    @Test
    void testLegacyFrozenOrderPublicCrudTypesShouldBeAbsent() {
        assertThat(presentTypes(LEGACY_FACE_TYPES))
                .as("FrozenOrder 不得继续暴露无 Consumer 的公共 CRUD surface")
                .isEmpty();
    }

    @Test
    void testLegacyFrozenOrderCrudImplShouldBeAbsentAndInternalLifecycleShouldRemain() {
        assertThat(FundsBalanceControlService.class).isNotNull();
        assertThat(DefaultFundsFrozenOrderLifecycleSaver.class).isNotNull();
        assertThat(FundsTransactionQueryService.class).isNotNull();
        assertThat(presentTypes(LEGACY_IMPL_TYPES))
                .as("FrozenOrder 真实事实应由余额控制与内部 lifecycle 承重，不保留平行 CRUD 实现")
                .isEmpty();
    }

    private static List<String> presentTypes(List<String> typeNames) {
        return typeNames.stream()
                .filter(FundsFrozenOrderServiceImplTests::isTypePresent)
                .toList();
    }

    private static boolean isTypePresent(String typeName) {
        try {
            Class.forName(typeName, false, FundsFrozenOrderServiceImplTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}

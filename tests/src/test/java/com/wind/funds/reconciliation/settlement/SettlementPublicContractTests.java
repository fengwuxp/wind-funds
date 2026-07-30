package com.wind.funds.reconciliation.settlement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SettlementPublicContractTests {

    @Test
    void testWaveOneSettlementContractShouldBeAvailableFromFaceModule() {
        assertThatCode(SettlementPublicContractTests::assertSettlementContract)
                .doesNotThrowAnyException();
    }

    private static void assertSettlementContract() throws Exception {
        Class<?> service = Class.forName(
                "com.wind.funds.reconciliation.application.settlement.SettlementOrderApplicationService");

        assertThat(service.isInterface()).isTrue();
        assertMethod(service, "createOrder", "CreateSettlementOrderRequest");
        assertMethod(service, "submitOrder", "SubmitSettlementOrderRequest");
        assertMethod(service, "returnToDraft", "ReturnSettlementOrderToDraftRequest");
        assertMethod(service, "approveOrder", "ApproveSettlementOrderRequest");
        assertMethod(service, "cancelOrder", "CancelSettlementOrderRequest");
        assertMethod(service, "lockOrder", "LockSettlementOrderRequest");
        assertThat(service.getMethod("getOrder", Long.class, String.class).getReturnType().getSimpleName())
                .isEqualTo("SettlementOrderDTO");
    }

    private static void assertMethod(Class<?> service, String methodName, String requestTypeName) {
        assertThat(service.getMethods())
                .anySatisfy(method -> {
                    assertThat(method.getName()).isEqualTo(methodName);
                    assertThat(method.getParameterTypes())
                            .extracting(Class::getSimpleName)
                            .containsExactly(requestTypeName, "WindOperator");
                    assertThat(method.getReturnType().getSimpleName()).isEqualTo("SettlementOrderDTO");
                });
    }
}

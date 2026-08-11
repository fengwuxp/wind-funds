package com.wind.funds.reconciliation.settlement;

import com.wind.funds.reconciliation.enums.SettlementReleaseDisposition;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseAuthorityContextDTO;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseDecisionDTO;
import com.wind.funds.reconciliation.model.request.ReleaseSettlementOrderRequest;
import com.wind.funds.reconciliation.service.SettlementReleaseAuthority;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

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
        assertMethod(service, "releaseOrder", "ReleaseSettlementOrderRequest");
        assertThat(service.getMethod("getOrder", Long.class, String.class).getReturnType().getSimpleName())
                .isEqualTo("SettlementOrderDTO");
        assertThat(SettlementReleaseDisposition.values()).containsExactly(SettlementReleaseDisposition.FROZEN);
        assertThat(Arrays.stream(ReleaseSettlementOrderRequest.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())))
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("tenantId", "settlementOrderSn", "reconciliationRunResultSn",
                        "reconciliationResultDigest", "coverageStatus", "coverageDigest", "watermark", "cutoff",
                        "ruleVersion", "ruleDecisionDigest", "currentLineageBatchSn", "lateDataStatus",
                        "resultReplacementStatus", "lineageSupersessionStatus", "approvalRef", "reason", "evidenceRefs");
        assertThat(SettlementReleaseAuthority.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("authorize");
        assertThat(SettlementReleaseAuthorityContextDTO.class.getDeclaredFields())
                .extracting(Field::getName)
                .contains("settlementOrder", "payoutOrder", "request", "gateDecision",
                        "originalLockRouteSnapshotDigest", "releaseRequestDigest");
        assertThat(SettlementReleaseDecisionDTO.class.getDeclaredFields())
                .extracting(Field::getName)
                .contains("releaseAllowed", "releaseDisposition", "decisionDigest", "evidenceRefs",
                        "expiresAt", "authorizedBy", "authorizedAt", "blockingReason");
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

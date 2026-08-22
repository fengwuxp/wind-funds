package com.wind.funds.reconciliation.contract;

import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.integration.operator.WindOperator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationGatePublicContractTests {

    @Test
    void testGateContractShouldUseExactStageReference() {
        assertThat(instanceFieldNames(CheckReconciliationGateRequest.class))
                .containsExactlyInAnyOrder("tenantId", "stageRef");
        assertThat(fieldType(CheckReconciliationGateRequest.class, "stageRef").getSimpleName())
                .isEqualTo("GateStageRef");
    }

    @Test
    void testGateContractShouldExposeOnlyRequirementPublicationAndEvaluation() {
        assertThat(ReconciliationGateApplicationService.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("recordGateRequirement", "checkGate", "inspectGate");
    }

    @Test
    void testGateContractShouldEvaluateCurrentRequirementByStageReference() {
        for (String methodName : List.of("checkGate", "inspectGate")) {
            Method method = methodNamed(ReconciliationGateApplicationService.class, methodName);
            assertThat(method.getReturnType()).isEqualTo(ReconciliationGateDecisionDTO.class);
            assertThat(method.getParameterTypes())
                    .containsExactly(CheckReconciliationGateRequest.class, WindOperator.class);
        }
    }

    @Test
    void testGateContractShouldPublishVersionedMandatoryPairsWithExpectedCurrent() {
        Method record = methodNamed(ReconciliationGateApplicationService.class, "recordGateRequirement");

        assertThat(record.getParameterTypes()).hasSize(2);
        assertThat(record.getParameterTypes()[1]).isEqualTo(WindOperator.class);
        Class<?> requestType = record.getParameterTypes()[0];
        assertThat(instanceFieldNames(requestType)).containsExactlyInAnyOrder(
                "tenantId", "stageRef", "requirementVersion", "requiredPairs",
                "expectedCurrentRequirementRef", "evidenceRefs");
        assertThat(fieldType(requestType, "stageRef").getSimpleName()).isEqualTo("GateStageRef");
        assertThat(fieldType(requestType, "expectedCurrentRequirementRef").getSimpleName())
                .isEqualTo("GateRequirementRef");
        assertThat(requestType.getDeclaredFields())
                .filteredOn(field -> field.getName().equals("requiredPairs"))
                .singleElement()
                .satisfies(field -> assertThat(field.getGenericType().getTypeName()).contains("RequiredPairRef"));
    }

    private static List<String> instanceFieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .toList();
    }

    private static Class<?> fieldType(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .map(Field::getType)
                .orElseThrow(() -> new AssertionError("Missing public contract field: " + fieldName));
    }

    private static Method methodNamed(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing public contract method: " + name));
    }
}

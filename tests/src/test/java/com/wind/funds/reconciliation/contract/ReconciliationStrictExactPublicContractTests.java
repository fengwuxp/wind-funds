package com.wind.funds.reconciliation.contract;

import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.integration.operator.WindOperator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationStrictExactPublicContractTests {

    @Test
    void testStrictExactContractShouldRemoveCallerSubmittedMatchResults() {
        assertThat(instanceFieldNames(RecordReconciliationRunResultRequest.class))
                .containsExactlyInAnyOrder("tenantId", "reconciliationBatchSn");
    }

    @Test
    void testStrictExactContractShouldRemoveCallerMatchAssertionType() {
        assertThatThrownBy(() -> Class.forName(
                "com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void testStrictExactContractShouldReplaceCallerRecordEntry() {
        assertThat(ReconciliationRunResultApplicationService.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("executeStrictExact", "getRunResult", "queryMatchResults");
    }

    @Test
    void testStrictExactContractShouldExposeNarrowProviderCommand() {
        Method execute = methodNamed(ReconciliationRunResultApplicationService.class, "executeStrictExact");

        assertThat(execute.getReturnType()).isEqualTo(ReconciliationRunResultDTO.class);
        assertThat(execute.getParameterTypes()).hasSize(2);
        assertThat(execute.getParameterTypes()[1]).isEqualTo(WindOperator.class);
        assertThat(instanceFieldNames(execute.getParameterTypes()[0]))
                .containsExactlyInAnyOrder("tenantId", "reconciliationBatchSn");
    }

    private static List<String> instanceFieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .toList();
    }

    private static Method methodNamed(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing public contract method: " + name));
    }
}

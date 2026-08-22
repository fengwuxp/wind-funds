package com.wind.funds.transaction.services;

import com.wind.funds.transaction.model.dto.FundsActionFactDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规范化 ActionFact 最小只读契约 RED。
 */
class FundsActionFactContractTests {

    @Test
    void testFundsTransactionQueryServiceShouldExposeCanonicalActionFactQueries() throws NoSuchMethodException {
        List<Method> methods = Arrays.asList(FundsTransactionQueryService.class.getMethods());

        Method listQuery = methods.stream()
                .filter(method -> method.getName().equals("queryFundsActionFacts"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "FundsTransactionQueryService 缺少 queryFundsActionFacts ActionFact 集合查询"));
        assertThat(listQuery.getParameterCount()).isOne();
        assertThat(listQuery.getParameterTypes()[0].getSimpleName()).isEqualTo("FundsActionFactQuery");
        assertThat(listQuery.getReturnType()).isEqualTo(List.class);

        Method identityQuery = methods.stream()
                .filter(method -> method.getName().equals("findFundsActionFact"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "FundsTransactionQueryService 缺少 findFundsActionFact ActionFact 唯一查询"));
        assertThat(identityQuery.getParameterCount()).isOne();
        assertThat(identityQuery.getParameterTypes()[0].getSimpleName()).isEqualTo("FundsActionFactRef");
        assertThat(identityQuery.getReturnType()).isEqualTo(Optional.class);

        assertThat(FundsActionFactDTO.class.getMethod("getSemanticDigest").getReturnType())
                .isEqualTo(FundsActionFactDTO.SemanticDigest.class);
        assertThat(FundsActionFactDTO.FundsRouteProvenance.class.getMethod("getRouteSnapshotRef").getReturnType())
                .isEqualTo(FundsActionFactDTO.RouteSnapshotRef.class);
        assertThat(FundsActionFactDTO.RouteSnapshotRef.class.getMethod("getIdentity").getReturnType())
                .isEqualTo(FundsActionFactDTO.StableIdentity.class);
    }

    @Test
    void testRecoveryActionFactShouldExposeOriginalFactAllocationAndRouteProvenance() throws NoSuchMethodException {
        Method originalFacts = Arrays.stream(FundsActionFactDTO.class.getMethods())
                .filter(method -> method.getName().equals("getOriginalFundsFactRefs"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "FundsActionFactDTO 缺少 getOriginalFundsFactRefs 原事实引用"));
        assertThat(originalFacts.getReturnType()).isEqualTo(List.class);

        Class<?> originalFactRef = Arrays.stream(FundsActionFactDTO.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("OriginalFundsFactRef"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FundsActionFactDTO 缺少 OriginalFundsFactRef"));
        assertThat(originalFactRef.getMethod("getTenantId").getReturnType()).isEqualTo(Long.class);
        assertThat(originalFactRef.getMethod("getFactType").getReturnType()).isEqualTo(String.class);
        assertThat(originalFactRef.getMethod("getFactId").getReturnType()).isEqualTo(String.class);
        assertThat(originalFactRef.getMethod("getRelationRole").getReturnType()).isEqualTo(String.class);
        assertThat(originalFactRef.getMethod("getAllocatedMoney").getReturnType().getSimpleName())
                .isEqualTo("Money");
        assertThat(FundsActionFactDTO.FundsRouteProvenance.class
                .getMethod("getOriginalFundsFactRef").getReturnType()).isEqualTo(originalFactRef);
    }
}

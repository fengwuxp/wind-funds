package com.wind.funds.wallet.services.impl;

import com.wind.funds.wallet.dal.entities.AccountHierarchyRelation;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyRelationRequest;
import com.wind.funds.wallet.service.AccountHierarchyRelationService;
import com.wind.integration.operator.WindOperator;
import org.junit.jupiter.api.Test;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * 账户层级关系契约测试。
 */
class AccountHierarchyRelationContractTests {

    @Test
    void testCreateRelationContractShouldOnlyAcceptBusinessRelationFacts()
            throws IntrospectionException, NoSuchMethodException {
        assertThat(fieldNames(CreateAccountHierarchyRelationRequest.class))
                .containsExactlyInAnyOrder("tenantId", "accountId", "parentAccountId")
                .doesNotContain("sn", "currency", "operatorId", "contextVariables");
        String[] relationProperties = Arrays.stream(Introspector.getBeanInfo(
                        AccountHierarchyRelationDTO.class, Object.class).getPropertyDescriptors())
                .map(property -> property.getName())
                .toArray(String[]::new);
        Method createMethod = AccountHierarchyRelationService.class.getMethod(
                "createAccountHierarchyRelation",
                CreateAccountHierarchyRelationRequest.class,
                WindOperator.class);

        assertSoftly(softly -> {
            softly.assertThat(createMethod.getReturnType())
                    .isEqualTo(AccountHierarchyRelationDTO.class);
            softly.assertThat(relationProperties).doesNotContain("id");
        });
    }

    @Test
    void testRelationPersistenceContractShouldUseRelationNamingAndNoContextVariables() throws IOException {
        assertThat(AccountHierarchyRelation.TABLE_NAME).isEqualTo("t_account_hierarchy_relation");
        assertThat(fieldNames(AccountHierarchyRelation.class)).doesNotContain("contextVariables");
        assertThat(fieldNames(AccountHierarchyRelationDTO.class)).doesNotContain("contextVariables");

        String schema = readJdbcSchema();
        assertThat(schema)
                .contains("CREATE TABLE `t_account_hierarchy_relation`")
                .contains("UNIQUE KEY `uk_account_hierarchy_relation_account`")
                .doesNotContain("t_account_hierarchy_binding");
    }

    private String[] fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .toArray(String[]::new);
    }

    private String readJdbcSchema() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/jdbc-schema.sql")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

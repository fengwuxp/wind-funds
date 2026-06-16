package com.wind.funds.wallet.services.impl;

import com.wind.funds.wallet.dal.entities.AccountHierarchyBinding;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyBindingRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账户层级绑定契约测试。
 */
class AccountHierarchyBindingContractTests {

    /**
     * 场景：账户层级绑定收窄为当前 ACTIVE 关系来源。
     * 输入：账户层级绑定持久化实体和创建请求。
     * 输出：对外契约字段集合。
     * 预期：不再暴露 validFrom/validTo 有效期窗口。
     * 红线：账户层级来源不得提前引入未闭环的排期生效或历史窗口语义。
     */
    @Test
    void testAccountHierarchyBindingContractShouldNotExposeValidityWindow() {
        assertThat(fieldNames(AccountHierarchyBinding.class))
                .doesNotContain("validFrom", "validTo");
        assertThat(fieldNames(CreateAccountHierarchyBindingRequest.class))
                .doesNotContain("validFrom", "validTo");
    }

    /**
     * 场景：H2 schema 承载账户层级绑定测试表。
     * 输入：测试数据库 schema。
     * 输出：t_account_hierarchy_binding 表结构。
     * 预期：不再包含 valid_from/valid_to 字段。
     * 红线：测试表结构不得保留已经从契约删除的有效期窗口列。
     */
    @Test
    void testAccountHierarchyBindingSchemaShouldNotExposeValidityWindow() throws IOException {
        String schema = readJdbcSchema();
        String tableDefinition = schema.substring(schema.indexOf("CREATE TABLE `t_account_hierarchy_binding`"),
                schema.indexOf("DEFAULT CHARSET = utf8mb4 COMMENT = '账户层级绑定表';"));

        assertThat(tableDefinition)
                .doesNotContain("`valid_from`", "`valid_to`");
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

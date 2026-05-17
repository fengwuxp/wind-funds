package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.dto.FundsFrozenOrderDTO;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class FundsFrozenOrderCompatibilityContractTests {

    /**
     * 场景：冻结入账流水由后续链路生成。
     * 输入：检查请求模型和实体模型上的 freezeLedgerTransactionSn 字段约束。
     * 输出：字段不带 NotBlank/NotNull 强约束。
     * 预期：调用方创建冻结单时无需传入冻结账本交易流水。
     * 红线：不得把异步生成的账本流水变成创建冻结单的强制入参。
     */
    @Test
    void testFreezeLedgerTransactionSnShouldNotBeCallerRequired() throws NoSuchFieldException {
        Field requestField = CreateFundsFrozenOrderRequest.class.getDeclaredField("freezeLedgerTransactionSn");
        Field entityField = FundsFrozenOrder.class.getDeclaredField("freezeLedgerTransactionSn");

        assertThat(requestField.isAnnotationPresent(NotBlank.class)).isFalse();
        assertThat(entityField.isAnnotationPresent(NotNull.class)).isFalse();
    }

    /**
     * 场景：冻结单历史消费字段仍存在于兼容模型中。
     * 输入：冻结单状态枚举、Entity 和 DTO 的历史消费字段。
     * 输出：消费状态、consumedAmount、consumeTime 均标记为 Deprecated。
     * 预期：新代码不得继续把冻结单当作消费、扣划或跨主体价值转移载体。
     * 红线：兼容字段不得被误读为 P0 目标态冻结消费能力。
     */
    @Test
    void testFrozenOrderConsumptionCompatibilityFieldsShouldBeDeprecated() throws NoSuchFieldException {
        assertThat(FundsFrozenOrderStatus.class.getField("PARTIALLY_CONSUMED")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrderStatus.class.getField("CONSUMED")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrder.class.getDeclaredField("consumedAmount")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrder.class.getDeclaredField("consumeTime")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrderDTO.class.getDeclaredField("consumedAmount")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrderDTO.class.getDeclaredField("consumeTime")
                .isAnnotationPresent(Deprecated.class)).isTrue();
    }

    /**
     * 场景：测试库表结构允许先登记冻结单再生成冻结入账流水。
     * 输入：H2/MySQL Mode 测试 schema 中的冻结单表定义。
     * 输出：freeze_ledger_transaction_sn 默认为 NULL，status 默认为 CREATED。
     * 预期：测试 schema 与冻结生命周期设计保持一致。
     * 红线：测试 DDL 不得重新把冻结账本流水变成创建冻结单的硬前置条件。
     */
    @Test
    void testJdbcSchemaShouldAllowPendingFreezeLedgerTransactionSn() throws IOException {
        String schema = new String(Objects.requireNonNull(getClass().getClassLoader()
                .getResourceAsStream("jdbc-schema.sql")).readAllBytes(), StandardCharsets.UTF_8);
        String freezeLedgerTransactionSnLine = schema.lines()
                .filter(line -> line.contains("`freeze_ledger_transaction_sn`"))
                .findFirst()
                .orElseThrow();

        assertThat(freezeLedgerTransactionSnLine).contains("DEFAULT NULL");
        assertThat(freezeLedgerTransactionSnLine).doesNotContain("NOT NULL");
        assertThat(schema.lines()
                .filter(line -> line.contains("`status`") && line.contains("冻结单状态"))
                .findFirst()
                .orElseThrow()).contains("DEFAULT 'CREATED'");
    }
}

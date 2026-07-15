package com.wind.funds.wallet.services.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 支付工具绑定契约测试。
 */
class PaymentInstrumentBindingContractTests {

    @Test
    void testPaymentInstrumentBindingSubjectUniqueKeyShouldBeTenantScoped() throws IOException {
        String schema = readJdbcSchema();
        String tableDefinition = schema.substring(schema.indexOf("CREATE TABLE `t_payment_instrument_binding`"),
                schema.indexOf("DEFAULT CHARSET = utf8mb4 COMMENT = '支付工具绑定表';"));

        assertThat(tableDefinition)
                .contains("UNIQUE KEY `uk_payment_instrument_binding_subject` "
                        + "(`tenant_id`, `instrument_sn`, `binding_role`, `subject_type`, `subject_id`, `currency`)");
    }

    private String readJdbcSchema() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/jdbc-schema.sql")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

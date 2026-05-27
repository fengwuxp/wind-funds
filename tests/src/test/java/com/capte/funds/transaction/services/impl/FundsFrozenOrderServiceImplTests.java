package com.capte.funds.transaction.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.capte.funds.transaction.services.FundsFrozenOrderService;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金冻结订单服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsFrozenOrderServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundsFrozenOrderServiceImplTests extends AbstractFundsServiceTest {

    private static final String FROZEN_ORDER_SN = "frozen_order_service_sensitive";

    private static final String BUSINESS_SN = "FROZEN_ORDER_SERVICE_SENSITIVE";

    private static final String UNQUOTED_PAYMENT_CONTEXT_VARIABLES =
            "{processorPayload:{secretKey:\"secret-value\"";

    private static final String UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES =
            "{externalAccount:{bankAccountNo:\"123456789012\"";

    @Autowired
    private FundsFrozenOrderService fundsFrozenOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：运营手工创建冻结单时把外部账户号或通道密钥放入扩展上下文。
     * 输入：contextVariables 含嵌套敏感字段，或坏 JSON 未加引号敏感字段名。
     * 输出：创建被拒绝，不留下冻结单、账本或账务事实。
     * 红线：冻结单管理对象不得成为外部账户号、PAN、CVV 或 token secret 的旁路存储。
     */
    @Test
    void testCreateFundsFrozenOrderShouldRejectSensitiveContextVariablesWithoutFrozenOrderFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundsFrozenOrderService.createFundsFrozenOrder(createFrozenOrderRequest()
                .setContextVariables("{\"externalAccount\":{\"bankAccountNo\":\"123456789012\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive funds frozen order fields");
        assertThatThrownBy(() -> fundsFrozenOrderService.createFundsFrozenOrder(createFrozenOrderRequest()
                .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive funds frozen order fields");
        assertThatThrownBy(() -> fundsFrozenOrderService.createFundsFrozenOrder(createFrozenOrderRequest()
                .setContextVariables(UNQUOTED_PAYMENT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive funds frozen order fields");
        assertThatThrownBy(() -> fundsFrozenOrderService.createFundsFrozenOrder(createFrozenOrderRequest()
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive funds frozen order fields");

        assertThat(countFrozenOrders()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpFundsFrozenOrderServiceTestData() {
        cleanupFundsFrozenOrderServiceTestData();
    }

    @AfterEach
    void tearDownFundsFrozenOrderServiceTestData() {
        cleanupFundsFrozenOrderServiceTestData();
    }

    private void cleanupFundsFrozenOrderServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_funds_frozen_order WHERE sn = ? OR business_sn = ?",
                FROZEN_ORDER_SN,
                BUSINESS_SN);
    }

    private CreateFundsFrozenOrderRequest createFrozenOrderRequest() {
        return new CreateFundsFrozenOrderRequest()
                .setSn(FROZEN_ORDER_SN)
                .setTenantId(TENANT_ID)
                .setSubjectId("funding_order_subject")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setFreezeType("MANUAL_HOLD")
                .setBusinessScene("FROZEN_ORDER_SERVICE")
                .setBusinessSn(BUSINESS_SN)
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD);
    }

    private long countFrozenOrders() {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_frozen_order WHERE sn = ? OR business_sn = ?",
                Long.class,
                FROZEN_ORDER_SN,
                BUSINESS_SN);
        return result;
    }

    @Configuration
    @Import(FundsFrozenOrderServiceImpl.class)
    static class Config {
    }
}

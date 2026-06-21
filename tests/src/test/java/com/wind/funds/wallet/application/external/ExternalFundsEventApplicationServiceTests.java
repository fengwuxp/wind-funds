package com.wind.funds.wallet.application.external;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.external.impl.ExternalFundsEventApplicationServiceImpl;
import com.wind.funds.wallet.model.request.ConsumeExternalFundsEventRequest;
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

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 外部资金事件消费应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ExternalFundsEventApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExternalFundsEventApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String BUSINESS_SCENE = "EXTERNAL_FUNDS_EVENT";

    private static final String BUSINESS_SN = "EXTERNAL_FUNDS_EVENT_001";

    private static final String TARGET_ACCOUNT_SN = "external_event_target_acc";

    @Autowired
    private ExternalFundsEventApplicationService externalFundsEventApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：ACH、银行文件或渠道回调确认了一笔外部资金事件，但 wallet 尚未接入归一资金事实内核。
     * 输入：外部事件流水、事件类型、目标账户主体、金额、币种、原交易引用和对账差异引用齐全。
     * 输出：服务层入口在生成任何资金事实前失败。
     * 红线：未完成外部事件归一编排前，不得生成资金交易、route、posting plan、账本交易、分录或余额投影。
     */
    @Test
    void testConsumeShouldFailFastBeforeNormalizedFundsFactKernelIsEnabled() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> externalFundsEventApplicationService.consume(consumeRequest(), WindOperator.system()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("外部资金事件消费尚未接入归一资金事实内核");

        assertNoFundsOrLedgerFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpExternalFundsEventTestData() {
        cleanupExternalFundsEventTestData();
    }

    @AfterEach
    void tearDownExternalFundsEventTestData() {
        cleanupExternalFundsEventTestData();
    }

    private void cleanupExternalFundsEventTestData() {
        jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn IN (
                    SELECT sn FROM t_ledger_transaction
                    WHERE business_scene = ? AND business_sn = ?
                )
                """, BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_frozen_order WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
    }

    private ConsumeExternalFundsEventRequest consumeRequest() {
        return new ConsumeExternalFundsEventRequest()
                .setTenantId(TENANT_ID)
                .setExternalEventSn("bank_event_001")
                .setExternalEventType("ACH_CREDIT_CONFIRMED")
                .setTargetAccountId(FundsAccountId.immutable(TARGET_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT))
                .setAmount(90L)
                .setCurrency(CurrencyIsoCode.USD)
                .setOriginalTransactionSn("original_funds_tx_001")
                .setReconciliationDifferenceSn("recon_diff_001")
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setDescription("external funds event contract");
    }

    private void assertNoFundsOrLedgerFacts() {
        assertThat(countRows("t_funds_transaction")).isZero();
        assertThat(countRows("t_funds_transaction_detail")).isZero();
        assertThat(countRows("t_ledger_transaction")).isZero();
        assertThat(countRows("t_ledger_entry")).isZero();
        assertThat(postingPlanCount()).isZero();
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private Integer postingPlanCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    @Configuration
    @Import(ExternalFundsEventApplicationServiceImpl.class)
    static class Config {
    }
}

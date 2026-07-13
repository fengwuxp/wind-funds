package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.enums.SpendRuleBindingStatus;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.ResumeSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.RetireSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.SuspendSpendRuleBindingRequest;
import com.wind.funds.wallet.service.SpendRuleBindingService;
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

import java.time.LocalDateTime;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spend Rule 挂载基础服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendRuleBindingServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendRuleBindingServiceTests extends AbstractFundsServiceTest {

    private static final String RULE_ID = "sr_binding_service_lifecycle";

    private static final String RULE_VERSION = "2026-07-13.1";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_rule_binding_service_card";

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.now().withNano(0).minusDays(1);

    private static final LocalDateTime EFFECTIVE_TO = LocalDateTime.now().withNano(0).plusDays(30);

    @Autowired
    private SpendRuleBindingService spendRuleBindingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：Spend Rule 挂载按允许状态机执行暂停、恢复、退役。
     * 输入：ACTIVE 挂载依次执行 suspend、resume、retire。
     * 输出：状态持久化为 SUSPENDED、ACTIVE、RETIRED，创建审计引用保持不变。
     * 红线：生命周期命令只改变控制事实，不创建交易或账本事实。
     */
    @Test
    void testLifecycleCommandsShouldPersistAllowedTransitionsWithoutFundsSideEffect() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        Long bindingId = spendRuleBindingService.createSpendRuleBinding(
                createBindingRequest("grant:srb-lifecycle-allowed"));
        SpendRuleBindingDTO binding = spendRuleBindingService.getSpendRuleBindingById(bindingId);

        spendRuleBindingService.suspendSpendRuleBinding(suspendRequest(binding.getSn()));
        SpendRuleBindingDTO suspended = spendRuleBindingService.getSpendRuleBindingById(bindingId);
        spendRuleBindingService.resumeSpendRuleBinding(resumeRequest(binding.getSn()));
        SpendRuleBindingDTO resumed = spendRuleBindingService.getSpendRuleBindingById(bindingId);
        spendRuleBindingService.retireSpendRuleBinding(retireRequest(binding.getSn()));
        SpendRuleBindingDTO retired = spendRuleBindingService.getSpendRuleBindingById(bindingId);

        assertThat(binding.getStatus()).isEqualTo(SpendRuleBindingStatus.ACTIVE);
        assertThat(suspended.getStatus()).isEqualTo(SpendRuleBindingStatus.SUSPENDED);
        assertThat(suspended.getAuditReferenceSn()).isEqualTo("grant:srb-lifecycle-allowed");
        assertThat(resumed.getStatus()).isEqualTo(SpendRuleBindingStatus.ACTIVE);
        assertThat(resumed.getAuditReferenceSn()).isEqualTo("grant:srb-lifecycle-allowed");
        assertThat(retired.getStatus()).isEqualTo(SpendRuleBindingStatus.RETIRED);
        assertThat(retired.getAuditReferenceSn()).isEqualTo("grant:srb-lifecycle-allowed");
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：Spend Rule 挂载拒绝重复命令和非法状态迁移。
     * 输入：ACTIVE 直接恢复、SUSPENDED 重复暂停、RETIRED 再恢复或再退役。
     * 输出：命令被拒绝，状态保持在原状态。
     * 红线：错误动作不能被当作幂等成功。
     */
    @Test
    void testLifecycleCommandsShouldRejectWrongTransitions() {
        Long bindingId = spendRuleBindingService.createSpendRuleBinding(
                createBindingRequest("grant:srb-lifecycle-reject"));
        SpendRuleBindingDTO binding = spendRuleBindingService.getSpendRuleBindingById(bindingId);

        assertThatThrownBy(() -> spendRuleBindingService.resumeSpendRuleBinding(resumeRequest(binding.getSn())))
                .hasMessageContaining("只有已暂停的 Spend Rule 挂载可以恢复");
        assertThat(spendRuleBindingService.getSpendRuleBindingById(bindingId).getStatus())
                .isEqualTo(SpendRuleBindingStatus.ACTIVE);

        spendRuleBindingService.suspendSpendRuleBinding(suspendRequest(binding.getSn()));
        assertThatThrownBy(() -> spendRuleBindingService.suspendSpendRuleBinding(suspendRequest(binding.getSn())))
                .hasMessageContaining("只有有效的 Spend Rule 挂载可以暂停");
        assertThat(spendRuleBindingService.getSpendRuleBindingById(bindingId).getStatus())
                .isEqualTo(SpendRuleBindingStatus.SUSPENDED);

        spendRuleBindingService.retireSpendRuleBinding(retireRequest(binding.getSn()));
        assertThatThrownBy(() -> spendRuleBindingService.resumeSpendRuleBinding(resumeRequest(binding.getSn())))
                .hasMessageContaining("只有已暂停的 Spend Rule 挂载可以恢复");
        assertThatThrownBy(() -> spendRuleBindingService.retireSpendRuleBinding(retireRequest(binding.getSn())))
                .hasMessageContaining("只有有效或已暂停的 Spend Rule 挂载可以退役");
        assertThat(spendRuleBindingService.getSpendRuleBindingById(bindingId).getStatus())
                .isEqualTo(SpendRuleBindingStatus.RETIRED);
    }

    /**
     * 场景：Spend Rule 挂载生命周期命令必须带上挂载身份。
     * 输入：缺少挂载流水号的暂停请求。
     * 输出：请求被拒绝。
     * 红线：资金底座不依赖外部 requestSn，也不靠审计引用定位生命周期目标。
     */
    @Test
    void testLifecycleCommandsShouldValidateBindingIdentity() {
        Long bindingId = spendRuleBindingService.createSpendRuleBinding(
                createBindingRequest("grant:srb-lifecycle-validation"));

        assertThatThrownBy(() -> spendRuleBindingService.suspendSpendRuleBinding(
                suspendRequest(null)))
                .hasMessageContaining("Spend Rule 挂载流水号不能为空");
        assertThat(spendRuleBindingService.getSpendRuleBindingById(bindingId).getStatus())
                .isEqualTo(SpendRuleBindingStatus.ACTIVE);
    }

    @BeforeEach
    void setUpSpendRuleBindingServiceTestData() {
        cleanupSpendRuleBindingServiceTestData();
    }

    @AfterEach
    void tearDownSpendRuleBindingServiceTestData() {
        cleanupSpendRuleBindingServiceTestData();
    }

    private CreateSpendRuleBindingRequest createBindingRequest(String auditReferenceSn) {
        return new CreateSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setScopeId(PAYMENT_INSTRUMENT_SN)
                .setPriority(10)
                .setConflictPolicy(SpendRuleConflictPolicy.DENY_OVERRIDES)
                .setEffectiveFrom(EFFECTIVE_FROM)
                .setEffectiveTo(EFFECTIVE_TO)
                .setAuditReferenceSn(auditReferenceSn)
                .setDescription("挂载 Spend Rule 版本");
    }

    private SuspendSpendRuleBindingRequest suspendRequest(String sn) {
        return new SuspendSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(sn)
                .setDescription("暂停 Spend Rule 挂载");
    }

    private ResumeSpendRuleBindingRequest resumeRequest(String sn) {
        return new ResumeSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(sn)
                .setDescription("恢复 Spend Rule 挂载");
    }

    private RetireSpendRuleBindingRequest retireRequest(String sn) {
        return new RetireSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(sn)
                .setDescription("退役 Spend Rule 挂载");
    }

    private void cleanupSpendRuleBindingServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_rule_binding WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
    }

    private void assertNoTransactionFacts() {
        assertThat(countRows("t_funds_transaction")).isZero();
        assertThat(countRows("t_funds_transaction_detail")).isZero();
        assertThat(countRows("t_ledger_transaction")).isZero();
        assertThat(countRows("t_ledger_entry")).isZero();
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ?",
                Integer.class, "SPEND_RULE_BINDING_SERVICE");
    }

    @Configuration
    @Import({
            SpendRuleBindingServiceImpl.class
    })
    static class Config {
    }
}

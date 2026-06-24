package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.model.dto.AccountHierarchyBindingDTO;
import com.wind.funds.wallet.service.AccountHierarchyBindingService;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账户层级绑定基础服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        AccountHierarchyBindingServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountHierarchyBindingServiceImplTests extends AbstractFundsServiceTest {

    private static final String ACTIVE_BINDING_SN = "account_hierarchy_binding_active_service";

    private static final String SUSPENDED_BINDING_SN = "account_hierarchy_binding_suspended_service";

    private static final String ACTIVE_ACCOUNT_ID = "child_account_hierarchy_service";

    private static final String SUSPENDED_ACCOUNT_ID = "suspended_child_account_hierarchy_service";

    private static final String PARENT_ACCOUNT_ID = "parent_account_hierarchy_service";

    private static final String ROOT_ACCOUNT_ID = "root_account_hierarchy_service";

    private static final String OPERATOR_ID = "ops_account_hierarchy_service";

    @Autowired
    private AccountHierarchyBindingService accountHierarchyBindingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：账户层级编排服务下沉前，基础服务负责创建并返回当前 ACTIVE 关系来源。
     * 输入：一条 ACTIVE 账户层级绑定当前态。
     * 输出：绑定主键、当前 ACTIVE 查询结果和重复 ACTIVE 判断。
     * 预期：基础服务只写层级绑定表，不生成账户、交易或账本事实。
     * 红线：账户层级绑定不是账务主体，不得产生 ledger 或 transaction side effect。
     */
    @Test
    void testCreateAndFindActiveAccountHierarchyBindingShouldNotCreateLedgerFacts() {
        AccountHierarchyBindingDTO binding = binding(ACTIVE_BINDING_SN,
                ACTIVE_ACCOUNT_ID,
                FundsAccountStatus.ACTIVE);

        Long id = accountHierarchyBindingService.createAccountHierarchyBinding(binding);
        Optional<AccountHierarchyBindingDTO> activeBinding =
                accountHierarchyBindingService.findActiveAccountHierarchyBinding(TENANT_ID,
                        ACTIVE_ACCOUNT_ID,
                        FundsSubjectType.FUNDING_ACCOUNT);

        assertThat(id).isNotNull();
        assertThat(activeBinding).isPresent();
        assertThat(activeBinding.orElseThrow())
                .extracting(AccountHierarchyBindingDTO::getSn,
                        AccountHierarchyBindingDTO::getAccountId,
                        AccountHierarchyBindingDTO::getAccountType,
                        AccountHierarchyBindingDTO::getParentAccountId,
                        AccountHierarchyBindingDTO::getRootAccountId,
                        AccountHierarchyBindingDTO::getCurrency,
                        AccountHierarchyBindingDTO::getStatus,
                        AccountHierarchyBindingDTO::getOperatorId)
                .containsExactly(ACTIVE_BINDING_SN,
                        ACTIVE_ACCOUNT_ID,
                        FundsSubjectType.FUNDING_ACCOUNT,
                        PARENT_ACCOUNT_ID,
                        ROOT_ACCOUNT_ID,
                        CurrencyIsoCode.USD,
                        FundsAccountStatus.ACTIVE,
                        OPERATOR_ID);
        assertThat(accountHierarchyBindingService.existsActiveAccountHierarchyBinding(binding)).isTrue();
        assertThat(countRows("t_ledger", "subject_id", ACTIVE_ACCOUNT_ID)).isZero();
        assertThat(countRows("t_funds_transaction", "business_sn", ACTIVE_BINDING_SN)).isZero();
        assertThat(countRows("t_ledger_transaction", "business_sn", ACTIVE_BINDING_SN)).isZero();
    }

    /**
     * 场景：基础服务存在非 ACTIVE 层级绑定记录。
     * 输入：一条 INACTIVE 账户层级绑定当前态。
     * 输出：当前 ACTIVE 查询结果和重复 ACTIVE 判断。
     * 预期：非 ACTIVE 记录不作为路由账户层级快照来源。
     * 红线：停用关系不得参与交易路由解释。
     */
    @Test
    void testSuspendedAccountHierarchyBindingShouldNotBeActiveSource() {
        AccountHierarchyBindingDTO binding = binding(SUSPENDED_BINDING_SN,
                SUSPENDED_ACCOUNT_ID,
                FundsAccountStatus.SUSPENDED);

        accountHierarchyBindingService.createAccountHierarchyBinding(binding);

        assertThat(accountHierarchyBindingService.findActiveAccountHierarchyBinding(TENANT_ID,
                SUSPENDED_ACCOUNT_ID,
                FundsSubjectType.FUNDING_ACCOUNT)).isEmpty();
        assertThat(accountHierarchyBindingService.existsActiveAccountHierarchyBinding(binding)).isFalse();
        assertThat(countRows("t_ledger", "subject_id", SUSPENDED_ACCOUNT_ID)).isZero();
    }

    private AccountHierarchyBindingDTO binding(String sn, String accountId, FundsAccountStatus status) {
        return new AccountHierarchyBindingDTO()
                .setSn(sn)
                .setTenantId(TENANT_ID)
                .setAccountId(accountId)
                .setAccountType(FundsSubjectType.FUNDING_ACCOUNT)
                .setParentAccountId(PARENT_ACCOUNT_ID)
                .setParentAccountType(FundsSubjectType.FUNDING_ACCOUNT)
                .setRootAccountId(ROOT_ACCOUNT_ID)
                .setRootAccountType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CURRENCY)
                .setStatus(status)
                .setOperatorId(OPERATOR_ID)
                .setContextVariables("{\"scenario\":\"account_hierarchy_service\"}");
    }

    private long countRows(String tableName, String columnName, String value) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value);
    }

    @Configuration
    @Import(AccountHierarchyBindingServiceImpl.class)
    static class Config {
    }
}

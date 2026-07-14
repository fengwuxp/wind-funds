package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.enums.FundsSubjectType;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private static final String CURRENT_BINDING_SN = "account_hierarchy_binding_current_service";

    private static final String CONCURRENT_CURRENT_BINDING_SN = "account_hierarchy_binding_current_concurrent_a";

    private static final String CONCURRENT_CURRENT_BINDING_SN_2 = "account_hierarchy_binding_current_concurrent_b";

    private static final String CURRENT_ACCOUNT_ID = "child_account_hierarchy_service";

    private static final String CONCURRENT_CURRENT_ACCOUNT_ID = "child_account_hierarchy_concurrent_service";

    private static final String PARENT_ACCOUNT_ID = "parent_account_hierarchy_service";

    private static final String OPERATOR_ID = "ops_account_hierarchy_service";

    @Autowired
    private AccountHierarchyBindingService accountHierarchyBindingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：账户层级编排服务下沉前，基础服务负责创建并返回当前关系来源。
     * 输入：一条账户层级绑定当前态。
     * 输出：绑定主键、当前查询结果和重复关系判断。
     * 预期：基础服务只写层级绑定表，不生成账户、交易或账本事实。
     * 红线：账户层级绑定不是账务主体，不得产生 ledger 或 transaction side effect。
     */
    @Test
    void testCreateAndFindCurrentAccountHierarchyBindingShouldNotCreateLedgerFacts() {
        AccountHierarchyBindingDTO binding = binding(CURRENT_BINDING_SN,
                CURRENT_ACCOUNT_ID);

        Long id = accountHierarchyBindingService.createAccountHierarchyBinding(binding);
        Optional<AccountHierarchyBindingDTO> currentBinding =
                accountHierarchyBindingService.findCurrentAccountHierarchyBinding(TENANT_ID,
                        CURRENT_ACCOUNT_ID,
                        FundsSubjectType.FUNDING_ACCOUNT);

        assertThat(id).isNotNull();
        assertThat(currentBinding).isPresent();
        assertThat(currentBinding.orElseThrow())
                .extracting(AccountHierarchyBindingDTO::getSn,
                        AccountHierarchyBindingDTO::getAccountId,
                        AccountHierarchyBindingDTO::getAccountType,
                        AccountHierarchyBindingDTO::getParentAccountId,
                        AccountHierarchyBindingDTO::getCurrency,
                        AccountHierarchyBindingDTO::getOperatorId)
                .containsExactly(CURRENT_BINDING_SN,
                        CURRENT_ACCOUNT_ID,
                        FundsSubjectType.FUNDING_ACCOUNT,
                        PARENT_ACCOUNT_ID,
                        CurrencyIsoCode.USD,
                        OPERATOR_ID);
        assertThat(accountHierarchyBindingService.existsCurrentAccountHierarchyBinding(binding)).isTrue();
        assertThat(countRows("t_ledger", "subject_id", CURRENT_ACCOUNT_ID)).isZero();
        assertThat(countRows("t_funds_transaction", "business_sn", CURRENT_BINDING_SN)).isZero();
        assertThat(countRows("t_ledger_transaction", "business_sn", CURRENT_BINDING_SN)).isZero();
    }

    /**
     * 场景：两个请求并发为同一子账户创建当前层级绑定。
     * 输入：同租户、同子账户、同主体类型，两个不同绑定 SN 同时提交。
     * 输出：只有一个绑定创建成功，另一个被当前态唯一性拒绝。
     * 红线：账户层级快照不得在并发写入后退化为“最新 id 获胜”。
     */
    @Test
    void testCreateAccountHierarchyBindingShouldSerializeConcurrentCurrentBindings() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BindingAttemptResult> first = executor.submit(concurrentBindingAttempt(startGate,
                    binding(CONCURRENT_CURRENT_BINDING_SN,
                            CONCURRENT_CURRENT_ACCOUNT_ID)));
            Future<BindingAttemptResult> second = executor.submit(concurrentBindingAttempt(startGate,
                    binding(CONCURRENT_CURRENT_BINDING_SN_2,
                            CONCURRENT_CURRENT_ACCOUNT_ID)));

            startGate.countDown();

            List<BindingAttemptResult> results = List.of(first.get(), second.get());

            assertThat(results).filteredOn(BindingAttemptResult::succeeded).hasSize(1);
            assertThat(results).filteredOn(result -> !result.succeeded())
                    .singleElement()
                    .satisfies(result -> assertThat(result.message()).contains("账户层级绑定关系已存在"));
            assertThat(countRows("t_account_hierarchy_binding", "account_id", CONCURRENT_CURRENT_ACCOUNT_ID)).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private AccountHierarchyBindingDTO binding(String sn, String accountId) {
        return new AccountHierarchyBindingDTO()
                .setSn(sn)
                .setTenantId(TENANT_ID)
                .setAccountId(accountId)
                .setAccountType(FundsSubjectType.FUNDING_ACCOUNT)
                .setParentAccountId(PARENT_ACCOUNT_ID)
                .setParentAccountType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CURRENCY)
                .setOperatorId(OPERATOR_ID)
                .setContextVariables("{\"scenario\":\"account_hierarchy_service\"}");
    }

    private long countRows(String tableName, String columnName, String value) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value);
    }

    private Callable<BindingAttemptResult> concurrentBindingAttempt(CountDownLatch startGate,
                                                                    AccountHierarchyBindingDTO binding) {
        return () -> {
            startGate.await();
            try {
                accountHierarchyBindingService.createAccountHierarchyBinding(binding);
                return new BindingAttemptResult(true, null);
            } catch (RuntimeException ex) {
                return new BindingAttemptResult(false, ex.getMessage());
            }
        };
    }

    @Configuration
    @Import(AccountHierarchyBindingServiceImpl.class)
    static class Config {
    }

    private record BindingAttemptResult(boolean succeeded, String message) {
    }
}

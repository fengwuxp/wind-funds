package com.wind.funds.wallet.services.impl;

import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.query.CreditAccountQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import org.assertj.core.api.SoftAssertions;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 信用账户公共查询面的租户隔离测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        CreditAccountServiceTenantIsolationTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CreditAccountServiceTenantIsolationTests extends AbstractFundsServiceTest {

    private static final Long FOREIGN_TENANT_ID = 2L;

    private static final String LOCAL_ACCOUNT_SN = "credit_tenant_local";

    private static final String FOREIGN_ACCOUNT_SN = "credit_tenant_foreign";

    private static final String LOCAL_OWNER_ID = "credit_owner_local";

    private static final String FOREIGN_OWNER_ID = "credit_owner_foreign";

    private static final String LOCAL_CONTEXT_TOKEN = "credit_context_local";

    private static final String FOREIGN_CONTEXT_TOKEN = "credit_context_foreign";

    private static final String FOREIGN_DESCRIPTION = "credit_description_foreign";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：公共信用账户查询只暴露显式 tenant 契约。
     * 输入：反射检查 CreditAccountService 与 CreditAccountQuery。
     * 输出：旧 raw-id/FundsAccountId getter 不存在，tenant+sn/query 保留且 query tenant 必填。
     * 红线：不得以兼容重载或可空 tenant 继续暴露跨租户信用账户事实。
     */
    @Test
    void testCreditAccountServiceShouldExposeOnlyTenantScopedQueryContract() throws NoSuchFieldException {
        boolean tenantRequired = CreditAccountQuery.class.getDeclaredField("tenantId")
                .isAnnotationPresent(NotNull.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(hasPublicMethod("getCreditAccountById", Long.class))
                    .as("raw id getter")
                    .isFalse();
            softly.assertThat(hasPublicMethod("getCreditAccount", FundsAccountId.class))
                    .as("FundsAccountId getter")
                    .isFalse();
            softly.assertThat(hasPublicMethod("getCreditAccount", Long.class, String.class))
                    .as("tenant scoped getter")
                    .isTrue();
            softly.assertThat(hasPublicMethod(
                            "queryCreditAccounts", CreditAccountQuery.class, WindQuery.class))
                    .as("tenant scoped query")
                    .isTrue();
            softly.assertThat(tenantRequired)
                    .as("query tenant @NotNull")
                    .isTrue();
        });
    }

    /**
     * 场景：直接调用公共信用账户查询服务时缺少 tenant。
     * 输入：已存在两个租户账户，CreditAccountQuery 未设置 tenantId。
     * 输出：查询在读取前被拒绝，账户、Ledger 和交易事实保持不变。
     * 红线：null tenant 不得被 MyBatis-Flex 忽略为无租户宽查询。
     */
    @Test
    void testQueryCreditAccountsShouldRejectMissingTenantWithoutFactsMutation() {
        createCreditAccount(TENANT_ID, LOCAL_ACCOUNT_SN, LOCAL_OWNER_ID, LOCAL_CONTEXT_TOKEN, null);
        createCreditAccount(FOREIGN_TENANT_ID, FOREIGN_ACCOUNT_SN, FOREIGN_OWNER_ID,
                FOREIGN_CONTEXT_TOKEN, FOREIGN_DESCRIPTION);
        CreditFactSnapshot before = creditFactSnapshot();

        Throwable failure = catchThrowable(() -> creditAccountService.queryCreditAccounts(
                new CreditAccountQuery(),
                DefaultPageQueryOptions.defaults(10)));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(failure)
                    .as("missing tenant rejection")
                    .isNotNull()
                    .hasMessageContaining("租户 ID 不能为空");
            softly.assertThat(creditFactSnapshot())
                    .as("missing tenant query facts")
                    .isEqualTo(before);
        });
    }

    /**
     * 场景：同租户查询真实信用账户，并尝试读取另一租户账户。
     * 输入：tenant 1/2 各有一个已初始化 CREDIT_BASIC required Ledger 的 CreditAccount。
     * 输出：同租户 get/query 返回本租户账户，foreign get 不存在且 foreign query 为空。
     * 红线：异常和结果不得泄露外租户 owner、状态、币种、类型、账期、profile 或上下文。
     */
    @Test
    void testTenantScopedCreditAccountQueriesShouldNotCrossTenantBoundary() {
        createCreditAccount(TENANT_ID, LOCAL_ACCOUNT_SN, LOCAL_OWNER_ID, LOCAL_CONTEXT_TOKEN, null);
        createCreditAccount(FOREIGN_TENANT_ID, FOREIGN_ACCOUNT_SN, FOREIGN_OWNER_ID,
                FOREIGN_CONTEXT_TOKEN, FOREIGN_DESCRIPTION);
        CreditFactSnapshot before = creditFactSnapshot();

        CreditAccountDTO localAccount = creditAccountService.getCreditAccount(TENANT_ID, LOCAL_ACCOUNT_SN);
        List<CreditAccountDTO> localAccounts = creditAccountService.queryCreditAccounts(
                new CreditAccountQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(LOCAL_ACCOUNT_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        Throwable foreignFailure = catchThrowable(
                () -> creditAccountService.getCreditAccount(TENANT_ID, FOREIGN_ACCOUNT_SN));
        List<CreditAccountDTO> foreignAccounts = creditAccountService.queryCreditAccounts(
                new CreditAccountQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(FOREIGN_ACCOUNT_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(localAccount.getTenantId()).isEqualTo(TENANT_ID);
            softly.assertThat(localAccount.getSn()).isEqualTo(LOCAL_ACCOUNT_SN);
            softly.assertThat(localAccount.getOwnerId()).isEqualTo(LOCAL_OWNER_ID);
            softly.assertThat(localAccount.getState()).isEqualTo(FundsAccountState.ACTIVE);
            softly.assertThat(localAccount.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
            softly.assertThat(localAccount.getAccountType()).isEqualTo(CreditFundsAccountType.SHARED_CARD.name());
            softly.assertThat(localAccount.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
            softly.assertThat(localAccount.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
            softly.assertThat(localAccount.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC);
            softly.assertThat(localAccount.getLedgerProfileVersion()).isEqualTo(1);
            softly.assertThat(localAccount.getContextVariables()).contains(LOCAL_CONTEXT_TOKEN);
            softly.assertThat(localAccounts)
                    .extracting(CreditAccountDTO::getSn)
                    .containsExactly(LOCAL_ACCOUNT_SN);
            softly.assertThat(foreignFailure)
                    .isNotNull()
                    .hasMessageContaining("信用账户不存在");
            softly.assertThat(foreignFailure.getMessage()).doesNotContain(
                    FOREIGN_OWNER_ID,
                    FOREIGN_DESCRIPTION,
                    FOREIGN_CONTEXT_TOKEN,
                    CreditFundsAccountType.SHARED_CARD.name(),
                    FundsAccountState.ACTIVE.name(),
                    CurrencyIsoCode.USD.name(),
                    AccountBalancePeriodType.LIFETIME.name(),
                    LedgerProfileCode.CREDIT_BASIC.name(),
                    "tenantId = " + FOREIGN_TENANT_ID,
                    "tenantId=" + FOREIGN_TENANT_ID);
            softly.assertThat(foreignAccounts).isEmpty();
            softly.assertThat(creditFactSnapshot())
                    .as("tenant scoped query facts")
                    .isEqualTo(before);
        });
    }

    @BeforeEach
    void setUpCreditAccountFacts() {
        cleanUpCreditAccountFacts();
    }

    @AfterEach
    void tearDownCreditAccountFacts() {
        cleanUpCreditAccountFacts();
    }

    private boolean hasPublicMethod(String methodName, Class<?>... parameterTypes) {
        try {
            CreditAccountService.class.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private void createCreditAccount(Long tenantId,
                                     String accountSn,
                                     String ownerId,
                                     String contextToken,
                                     String description) {
        creditAccountService.createCreditAccount(new CreateCreditAccountRequest()
                .setTenantId(tenantId)
                .setSn(accountSn)
                .setOwnerId(ownerId)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setDescription(description)
                .setContextVariables("{\"memo\":\"" + contextToken + "\"}"));
    }

    private CreditFactSnapshot creditFactSnapshot() {
        return new CreditFactSnapshot(
                queryRows("t_credit_account"),
                queryRows("t_funds_transaction"),
                queryRows("t_funds_transaction_detail"),
                ledgerFactSnapshot(jdbcTemplate));
    }

    private List<Map<String, Object>> queryRows(String tableName) {
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName + " ORDER BY id")
                .stream()
                .map(LinkedHashMap::new)
                .map(Collections::unmodifiableMap)
                .toList();
    }

    private void cleanUpCreditAccountFacts() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                LOCAL_ACCOUNT_SN,
                FOREIGN_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn IN (?, ?)",
                LOCAL_ACCOUNT_SN,
                FOREIGN_ACCOUNT_SN);
    }

    private record CreditFactSnapshot(List<Map<String, Object>> creditAccounts,
                                      List<Map<String, Object>> transactions,
                                      List<Map<String, Object>> transactionDetails,
                                      LedgerFactSnapshot ledgerFacts) {

        private CreditFactSnapshot {
            creditAccounts = List.copyOf(creditAccounts);
            transactions = List.copyOf(transactions);
            transactionDetails = List.copyOf(transactionDetails);
        }
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            CreditAccountServiceImpl.class
    })
    static class Config {
    }
}

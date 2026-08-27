package com.wind.funds.wallet.services.impl;

import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.query.FundingAccountQuery;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.service.FundingAccountService;
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
 * 真实资金账户公共查询面的租户隔离测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundingAccountServiceTenantIsolationTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundingAccountServiceTenantIsolationTests extends AbstractFundsServiceTest {

    private static final Long FOREIGN_TENANT_ID = 2L;

    private static final String LOCAL_ACCOUNT_SN = "funding_tenant_local";

    private static final String FOREIGN_ACCOUNT_SN = "funding_tenant_foreign";

    private static final String LOCAL_OWNER_ID = "funding_owner_local";

    private static final String FOREIGN_OWNER_ID = "funding_owner_foreign";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：公共资金账户查询只暴露显式 tenant 契约。
     * 输入：反射检查 FundingAccountService 与 FundingAccountQuery。
     * 输出：旧 raw-id/FundsAccountId getter 不存在，tenant+sn/query 保留且 query tenant 必填。
     * 红线：不得以兼容重载或可空 tenant 继续暴露跨租户账户事实。
     */
    @Test
    void testFundingAccountServiceShouldExposeOnlyTenantScopedQueryContract() throws NoSuchFieldException {
        boolean tenantRequired = FundingAccountQuery.class.getDeclaredField("tenantId")
                .isAnnotationPresent(NotNull.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(hasPublicMethod("getFundingAccountById", Long.class))
                    .as("raw id getter")
                    .isFalse();
            softly.assertThat(hasPublicMethod("getFundingAccount", FundsAccountId.class))
                    .as("FundsAccountId getter")
                    .isFalse();
            softly.assertThat(hasPublicMethod("getFundingAccount", Long.class, String.class))
                    .as("tenant scoped getter")
                    .isTrue();
            softly.assertThat(hasPublicMethod(
                            "queryFundingAccounts", FundingAccountQuery.class, WindQuery.class))
                    .as("tenant scoped query")
                    .isTrue();
            softly.assertThat(tenantRequired)
                    .as("query tenant @NotNull")
                    .isTrue();
        });
    }

    /**
     * 场景：直接调用公共查询服务时缺少 tenant。
     * 输入：已存在两个租户账户，FundingAccountQuery 未设置 tenantId。
     * 输出：查询在读取前被拒绝，账户、Ledger 和交易事实保持不变。
     * 红线：null tenant 不得被 MyBatis-Flex 忽略为无租户宽查询。
     */
    @Test
    void testQueryFundingAccountsShouldRejectMissingTenantWithoutFactsMutation() {
        createFundingAccount(TENANT_ID, LOCAL_ACCOUNT_SN, LOCAL_OWNER_ID);
        createFundingAccount(FOREIGN_TENANT_ID, FOREIGN_ACCOUNT_SN, FOREIGN_OWNER_ID);
        FundingFactSnapshot before = fundingFactSnapshot();

        Throwable failure = catchThrowable(() -> fundingAccountService.queryFundingAccounts(
                new FundingAccountQuery(),
                DefaultPageQueryOptions.defaults(10)));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(failure)
                    .as("missing tenant rejection")
                    .isNotNull()
                    .hasMessageContaining("租户 ID 不能为空");
            softly.assertThat(fundingFactSnapshot())
                    .as("missing tenant query facts")
                    .isEqualTo(before);
        });
    }

    /**
     * 场景：同租户查询真实资金账户，并尝试读取另一租户账户。
     * 输入：tenant 1/2 各有一个已初始化 required Ledger 的 FundingAccount。
     * 输出：同租户 get/query 返回本租户账户，foreign get 不存在且 foreign query 为空。
     * 红线：异常和结果不得泄露外租户 owner、状态、币种、类型、profile 或上下文。
     */
    @Test
    void testTenantScopedFundingAccountQueriesShouldNotCrossTenantBoundary() {
        createFundingAccount(TENANT_ID, LOCAL_ACCOUNT_SN, LOCAL_OWNER_ID);
        createFundingAccount(FOREIGN_TENANT_ID, FOREIGN_ACCOUNT_SN, FOREIGN_OWNER_ID);
        FundingFactSnapshot before = fundingFactSnapshot();

        FundingAccountDTO localAccount = fundingAccountService.getFundingAccount(TENANT_ID, LOCAL_ACCOUNT_SN);
        List<FundingAccountDTO> localAccounts = fundingAccountService.queryFundingAccounts(
                new FundingAccountQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(LOCAL_ACCOUNT_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        Throwable foreignFailure = catchThrowable(
                () -> fundingAccountService.getFundingAccount(TENANT_ID, FOREIGN_ACCOUNT_SN));
        List<FundingAccountDTO> foreignAccounts = fundingAccountService.queryFundingAccounts(
                new FundingAccountQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(FOREIGN_ACCOUNT_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(localAccount.getTenantId()).isEqualTo(TENANT_ID);
            softly.assertThat(localAccount.getSn()).isEqualTo(LOCAL_ACCOUNT_SN);
            softly.assertThat(localAccount.getOwnerId()).isEqualTo(LOCAL_OWNER_ID);
            softly.assertThat(localAccount.getState()).isEqualTo(FundsAccountState.ACTIVE);
            softly.assertThat(localAccount.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
            softly.assertThat(localAccount.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC);
            softly.assertThat(localAccounts)
                    .extracting(FundingAccountDTO::getSn)
                    .containsExactly(LOCAL_ACCOUNT_SN);
            softly.assertThat(foreignFailure)
                    .isNotNull()
                    .hasMessageContaining("资金账户不存在");
            softly.assertThat(foreignFailure.getMessage()).doesNotContain(
                    FOREIGN_OWNER_ID,
                    FundingAccountType.USER_WALLET.name(),
                    FundsAccountState.ACTIVE.name(),
                    CurrencyIsoCode.USD.name(),
                    LedgerProfileCode.FUNDING_BASIC.name(),
                    "tenantId = " + FOREIGN_TENANT_ID,
                    "tenantId=" + FOREIGN_TENANT_ID);
            softly.assertThat(foreignAccounts).isEmpty();
            softly.assertThat(fundingFactSnapshot())
                    .as("tenant scoped query facts")
                    .isEqualTo(before);
        });
    }

    @BeforeEach
    void setUpFundingAccountFacts() {
        cleanUpFundingAccountFacts();
    }

    @AfterEach
    void tearDownFundingAccountFacts() {
        cleanUpFundingAccountFacts();
    }

    private boolean hasPublicMethod(String methodName, Class<?>... parameterTypes) {
        try {
            FundingAccountService.class.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private void createFundingAccount(Long tenantId, String accountSn, String ownerId) {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setTenantId(tenantId)
                .setSn(accountSn)
                .setOwnerId(ownerId)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC));
    }

    private FundingFactSnapshot fundingFactSnapshot() {
        return new FundingFactSnapshot(
                queryRows("t_funding_account"),
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

    private void cleanUpFundingAccountFacts() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                LOCAL_ACCOUNT_SN,
                FOREIGN_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                LOCAL_ACCOUNT_SN,
                FOREIGN_ACCOUNT_SN);
    }

    private record FundingFactSnapshot(List<Map<String, Object>> fundingAccounts,
                                       List<Map<String, Object>> transactions,
                                       List<Map<String, Object>> transactionDetails,
                                       LedgerFactSnapshot ledgerFacts) {

        private FundingFactSnapshot {
            fundingAccounts = List.copyOf(fundingAccounts);
            transactions = List.copyOf(transactions);
            transactionDetails = List.copyOf(transactionDetails);
        }
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class
    })
    static class Config {
    }
}

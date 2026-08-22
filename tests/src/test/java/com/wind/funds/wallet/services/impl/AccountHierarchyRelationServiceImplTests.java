package com.wind.funds.wallet.services.impl;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import com.wind.funds.wallet.model.query.AccountHierarchyRelationQuery;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyRelationRequest;
import com.wind.funds.wallet.service.AccountHierarchyRelationService;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 账户层级关系服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        AccountHierarchyRelationServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountHierarchyRelationServiceImplTests extends AbstractFundsServiceTest {

    private static final String CHILD = "hierarchy_relation_child";

    private static final String PARENT = "hierarchy_relation_parent";

    private static final String OTHER_PARENT = "hierarchy_relation_other_parent";

    private static final String FROZEN_CHILD = "hierarchy_relation_frozen_child";

    private static final String SUSPENDED_PARENT = "hierarchy_relation_suspended_parent";

    private static final String CLOSED_CHILD = "hierarchy_relation_closed_child";

    private static final String EUR_PARENT = "hierarchy_relation_eur_parent";

    @Autowired
    private AccountHierarchyRelationService accountHierarchyRelationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpAccounts() {
        insertFundingAccount(CHILD, CurrencyIsoCode.USD, FundsAccountState.ACTIVE);
        insertFundingAccount(PARENT, CurrencyIsoCode.USD, FundsAccountState.ACTIVE);
        insertFundingAccount(OTHER_PARENT, CurrencyIsoCode.USD, FundsAccountState.ACTIVE);
        insertFundingAccount(FROZEN_CHILD, CurrencyIsoCode.USD, FundsAccountState.FROZEN);
        insertFundingAccount(SUSPENDED_PARENT, CurrencyIsoCode.USD, FundsAccountState.SUSPENDED);
        insertFundingAccount(CLOSED_CHILD, CurrencyIsoCode.USD, FundsAccountState.CLOSED);
        insertFundingAccount(EUR_PARENT, CurrencyIsoCode.EUR, FundsAccountState.ACTIVE);
    }

    @AfterEach
    void tearDownAccounts() {
        jdbcTemplate.update("DELETE FROM t_account_hierarchy_relation WHERE account_id LIKE 'hierarchy_relation_%'");
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn LIKE 'hierarchy_relation_%'");
    }

    @Test
    void testCreateRelationShouldGenerateFactAndRemainIdempotentForSameParent() {
        CreateAccountHierarchyRelationRequest request = relation(CHILD, PARENT);

        Long firstId = accountHierarchyRelationService.createAccountHierarchyRelation(
                request, WindOperatorFactory.system());
        Long retryId = accountHierarchyRelationService.createAccountHierarchyRelation(
                request, WindOperatorFactory.system());
        AccountHierarchyRelationDTO relation = accountHierarchyRelationService
                .findAccountHierarchyRelation(TENANT_ID, account(CHILD))
                .orElseThrow();

        assertThat(retryId).isEqualTo(firstId);
        assertThat(relation.getSn()).isNotBlank();
        assertThat(relation.getAccountId()).isEqualTo(CHILD);
        assertThat(relation.getParentAccountId()).isEqualTo(PARENT);
        assertThat(relation.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(relation.getOperatorId()).isEqualTo(WindOperatorFactory.system().getOperatorAsText());
        assertThat(countRows("t_account_hierarchy_relation", "account_id", CHILD)).isOne();
        assertThat(countRows("t_ledger", "subject_id", CHILD)).isZero();
        assertThat(countRows("t_funds_transaction", "business_sn", relation.getSn())).isZero();
    }

    @Test
    void testCreateRelationShouldRejectDifferentParentForExistingChild() {
        accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CHILD, PARENT), WindOperatorFactory.system());

        assertThatThrownBy(() -> accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CHILD, OTHER_PARENT), WindOperatorFactory.system()))
                .hasMessageContaining("已存在其他父账户关系");
    }

    @Test
    void testCreateRelationShouldAllowFrozenAndSuspendedButRejectClosedAccount() {
        Long id = accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(FROZEN_CHILD, SUSPENDED_PARENT), WindOperatorFactory.system());

        assertThat(id).isNotNull();
        assertThatThrownBy(() -> accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CLOSED_CHILD, PARENT), WindOperatorFactory.system()))
                .hasMessageContaining("CLOSED");
    }

    @Test
    void testCreateRelationShouldRejectSelfCurrencyMismatchAndCycle() {
        assertThatThrownBy(() -> accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CHILD, CHILD), WindOperatorFactory.system()))
                .hasMessageContaining("父账户不能等于子账户");
        assertThatThrownBy(() -> accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CHILD, EUR_PARENT), WindOperatorFactory.system()))
                .hasMessageContaining("币种必须一致");

        accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CHILD, PARENT), WindOperatorFactory.system());
        assertThatThrownBy(() -> accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(PARENT, CHILD), WindOperatorFactory.system()))
                .hasMessageContaining("形成环路");
    }

    @Test
    void testQueryRelationsShouldSupportParentAccountFilter() {
        accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(CHILD, PARENT), WindOperatorFactory.system());
        accountHierarchyRelationService.createAccountHierarchyRelation(
                relation(FROZEN_CHILD, PARENT), WindOperatorFactory.system());

        assertThat(accountHierarchyRelationService.queryAccountHierarchyRelations(
                        new AccountHierarchyRelationQuery()
                                .setTenantId(TENANT_ID)
                                .setParentAccountId(account(PARENT)),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords())
                .extracting(AccountHierarchyRelationDTO::getAccountId)
                .containsExactly(CHILD, FROZEN_CHILD);
    }

    private CreateAccountHierarchyRelationRequest relation(String child, String parent) {
        return new CreateAccountHierarchyRelationRequest()
                .setTenantId(TENANT_ID)
                .setAccountId(account(child))
                .setParentAccountId(account(parent));
    }

    private FundsAccountId account(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private void insertFundingAccount(String sn, CurrencyIsoCode currency, FundsAccountState state) {
        jdbcTemplate.update("""
                        INSERT INTO t_funding_account
                            (sn, tenant_id, owner_id, owner_type, account_type, is_platform, currency,
                             ledger_profile_code, ledger_profile_version, status, version)
                        VALUES (?, ?, ?, 'USER', 'USER_WALLET', 0, ?, 'FUNDING_BASIC', 1, ?, 0)
                        """,
                sn, TENANT_ID, "hierarchy_owner", currency.name(), state.name());
    }

    private long countRows(String tableName, String columnName, String value) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value);
    }

    @Configuration
    @Import({
            AccountHierarchyRelationServiceImpl.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class
    })
    static class Config {

        @Bean
        LedgerService ledgerService() {
            return (LedgerService) Proxy.newProxyInstance(
                    LedgerService.class.getClassLoader(),
                    new Class<?>[]{LedgerService.class},
                    (proxy, method, args) -> null);
        }
    }
}

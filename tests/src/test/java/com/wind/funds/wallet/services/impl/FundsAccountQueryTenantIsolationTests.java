package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountOwner;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.transaction.core.Money;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static com.wind.transaction.core.enums.CurrencyIsoCode.USD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金账户查询的租户隔离测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsAccountQueryTenantIsolationTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundsAccountQueryTenantIsolationTests extends AbstractFundsServiceTest {

    private static final Long FOREIGN_TENANT_ID = 2L;

    private static final String LOCAL_FUNDING_ACCOUNT_SN = "faq_tenant_funding_local";

    private static final String FOREIGN_FUNDING_ACCOUNT_SN = "faq_tenant_funding_foreign";

    private static final String LOCAL_CREDIT_ACCOUNT_SN = "faq_tenant_credit_local";

    private static final String FOREIGN_CREDIT_ACCOUNT_SN = "faq_tenant_credit_foreign";

    private static final String LOCAL_FUNDING_OWNER_ID = "faq_funding_owner_local";

    private static final String FOREIGN_FUNDING_OWNER_ID = "faq_funding_owner_foreign";

    private static final String LOCAL_CREDIT_OWNER_ID = "faq_credit_owner_local";

    private static final String FOREIGN_CREDIT_OWNER_ID = "faq_credit_owner_foreign";

    private static final long FUNDING_BALANCE = 101L;

    private static final long CREDIT_BALANCE = 202L;

    private static final List<String> QUERY_METHOD_NAMES = List.of(
            "getAccount",
            "getLedgerProfileCode",
            "getBalance",
            "supports"
    );

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private FundsAccountQueryService fundsAccountQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUpAccountFacts() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?, ?)",
                LOCAL_FUNDING_ACCOUNT_SN,
                FOREIGN_FUNDING_ACCOUNT_SN,
                LOCAL_CREDIT_ACCOUNT_SN,
                FOREIGN_CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                LOCAL_FUNDING_ACCOUNT_SN,
                FOREIGN_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn IN (?, ?)",
                LOCAL_CREDIT_ACCOUNT_SN,
                FOREIGN_CREDIT_ACCOUNT_SN);
    }

    @Test
    void testFundsAccountQueryServiceShouldRequireTenantScopedPublicContract() {
        SoftAssertions.assertSoftly(softly -> QUERY_METHOD_NAMES.forEach(methodName -> {
            softly.assertThat(hasPublicMethod(methodName, Long.class, FundsAccountId.class))
                    .as("%s 应显式接收 tenantId", methodName)
                    .isTrue();
            softly.assertThat(hasPublicMethod(methodName, FundsAccountId.class))
                    .as("%s 不应保留无 tenant 重载", methodName)
                    .isFalse();
        }));
    }

    @Test
    void testFundingAccountQueriesShouldNotCrossTenantBoundary() {
        createFundingAccount(TENANT_ID, LOCAL_FUNDING_ACCOUNT_SN, LOCAL_FUNDING_OWNER_ID);
        createFundingAccount(FOREIGN_TENANT_ID, FOREIGN_FUNDING_ACCOUNT_SN, FOREIGN_FUNDING_OWNER_ID);
        seedLedgerBalances(TENANT_ID, FundsSubjectType.FUNDING_ACCOUNT, LOCAL_FUNDING_ACCOUNT_SN,
                FUNDING_BALANCE, 3);
        seedLedgerBalances(FOREIGN_TENANT_ID, FundsSubjectType.FUNDING_ACCOUNT, FOREIGN_FUNDING_ACCOUNT_SN,
                FUNDING_BALANCE + 1, 3);

        FundsAccountId localAccountId = FundsAccountId.immutable(
                LOCAL_FUNDING_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
        assertSameTenantQueries(localAccountId, LOCAL_FUNDING_OWNER_ID, LedgerProfileCode.FUNDING_BASIC,
                Set.of(FundsAccountCapability.RECEIVE, FundsAccountCapability.PAY, FundsAccountCapability.WITHDRAW),
                Set.of(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN, LedgerSubjectCode.AUTHORIZATION),
                FUNDING_BALANCE);

        AccountFactSnapshot before = accountFactSnapshot();
        FundsAccountId foreignAccountId = FundsAccountId.immutable(
                FOREIGN_FUNDING_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
        assertForeignTenantQueriesFailClosed(foreignAccountId, FOREIGN_FUNDING_OWNER_ID,
                FundingAccountType.USER_WALLET.name(),
                LedgerProfileCode.FUNDING_BASIC,
                Set.of(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN, LedgerSubjectCode.AUTHORIZATION),
                FUNDING_BALANCE + 1,
                before);
    }

    @Test
    void testCreditAccountQueriesShouldNotCrossTenantBoundary() {
        createCreditAccount(TENANT_ID, LOCAL_CREDIT_ACCOUNT_SN, LOCAL_CREDIT_OWNER_ID);
        createCreditAccount(FOREIGN_TENANT_ID, FOREIGN_CREDIT_ACCOUNT_SN, FOREIGN_CREDIT_OWNER_ID);
        seedLedgerBalances(TENANT_ID, FundsSubjectType.CREDIT_ACCOUNT, LOCAL_CREDIT_ACCOUNT_SN,
                CREDIT_BALANCE, 4);
        seedLedgerBalances(FOREIGN_TENANT_ID, FundsSubjectType.CREDIT_ACCOUNT, FOREIGN_CREDIT_ACCOUNT_SN,
                CREDIT_BALANCE + 1, 4);

        FundsAccountId localAccountId = FundsAccountId.immutable(
                LOCAL_CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT);
        assertSameTenantQueries(localAccountId, LOCAL_CREDIT_OWNER_ID, LedgerProfileCode.CREDIT_BASIC,
                Set.of(FundsAccountCapability.PAY),
                Set.of(LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.OUTSTANDING),
                CREDIT_BALANCE);

        AccountFactSnapshot before = accountFactSnapshot();
        FundsAccountId foreignAccountId = FundsAccountId.immutable(
                FOREIGN_CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT);
        assertForeignTenantQueriesFailClosed(foreignAccountId, FOREIGN_CREDIT_OWNER_ID,
                CreditFundsAccountType.SHARED_CARD.name(),
                LedgerProfileCode.CREDIT_BASIC,
                Set.of(LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.OUTSTANDING),
                CREDIT_BALANCE + 1,
                before);
    }

    private void assertSameTenantQueries(FundsAccountId accountId,
                                         String ownerId,
                                         LedgerProfileCode ledgerProfileCode,
                                         Set<FundsAccountCapability> capabilities,
                                         Set<LedgerSubjectCode> ledgerSubjectCodes,
                                         long balanceAmount) {
        FundsAccount account = (FundsAccount) invokeAccountQuery("getAccount", TENANT_ID, accountId);
        LedgerProfileCode profile = (LedgerProfileCode) invokeAccountQuery(
                "getLedgerProfileCode", TENANT_ID, accountId);
        FundsAccountBalanceView balance = (FundsAccountBalanceView) invokeAccountQuery(
                "getBalance", TENANT_ID, accountId);
        boolean supported = (boolean) invokeAccountQuery("supports", TENANT_ID, accountId);

        assertThat(account.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(account.getAccountId()).isEqualTo(accountId);
        assertThat(account.getOwner()).isEqualTo(FundsAccountOwner.of(ownerId, FundsAccountOwnerType.USER));
        assertThat(account.getState()).isEqualTo(FundsAccountState.ACTIVE);
        assertThat(account.getCurrency()).isEqualTo(USD);
        assertThat(account.getCapabilities()).containsExactlyInAnyOrderElementsOf(capabilities);
        assertThat(account.getVersion()).isZero();
        assertThat(profile).isEqualTo(ledgerProfileCode);
        assertThat(supported).isTrue();

        assertThat(balance.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(balance.getAccountId()).isEqualTo(accountId);
        assertThat(balance.getCurrency()).isEqualTo(USD);
        assertThat(balance.getLedgerProfileCode()).isEqualTo(ledgerProfileCode);
        assertThat(balance.getBalanceBuckets())
                .containsOnlyKeys(ledgerSubjectCodes.toArray(LedgerSubjectCode[]::new));
        assertThat(balance.getBalanceBuckets().values()).allSatisfy(bucket -> {
            assertThat(bucket.balance()).isEqualTo(Money.immutable(balanceAmount, USD));
            assertThat(bucket.periodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
            assertThat(bucket.periodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        });
    }

    private void assertForeignTenantQueriesFailClosed(FundsAccountId foreignAccountId,
                                                       String foreignOwnerId,
                                                       String foreignAccountType,
                                                       LedgerProfileCode foreignProfileCode,
                                                       Set<LedgerSubjectCode> foreignLedgerSubjectCodes,
                                                       long foreignBalanceAmount,
                                                       AccountFactSnapshot expectedFacts) {
        InvocationOutcome account = invokeSafely("getAccount", TENANT_ID, foreignAccountId);
        InvocationOutcome profile = invokeSafely("getLedgerProfileCode", TENANT_ID, foreignAccountId);
        InvocationOutcome balance = invokeSafely("getBalance", TENANT_ID, foreignAccountId);
        Object supported = invokeAccountQuery("supports", TENANT_ID, foreignAccountId);

        SoftAssertions.assertSoftly(softly -> {
            assertStrongQueryFailedClosed(softly, "account", account);
            assertStrongQueryFailedClosed(softly, "profile", profile);
            assertStrongQueryFailedClosed(softly, "balance", balance);
            softly.assertThat(supported).as("foreign supports").isEqualTo(false);
            softly.assertThat(accountFactSnapshot()).as("查询前后资金事实").isEqualTo(expectedFacts);
            List.of(account, profile, balance).forEach(outcome -> {
                String message = outcome.failure() == null ? "" : outcome.failure().getMessage();
                List<String> protectedTokens = new ArrayList<>(List.of(
                        foreignOwnerId,
                        foreignAccountType,
                        FundsAccountState.ACTIVE.name(),
                        USD.name(),
                        foreignProfileCode.name(),
                        AccountBalancePeriodType.LIFETIME.name(),
                        FundsAccountCapability.RECEIVE.name(),
                        FundsAccountCapability.PAY.name(),
                        FundsAccountCapability.WITHDRAW.name(),
                        String.valueOf(foreignBalanceAmount),
                        Money.immutable(foreignBalanceAmount, USD).toString(),
                        "tenantId = " + FOREIGN_TENANT_ID,
                        "tenantId=" + FOREIGN_TENANT_ID,
                        "\"tenantId\":" + FOREIGN_TENANT_ID
                ));
                foreignLedgerSubjectCodes.stream()
                        .map(Enum::name)
                        .forEach(protectedTokens::add);
                softly.assertThat(message).doesNotContain(protectedTokens.toArray(String[]::new));
            });
        });
    }

    private void assertStrongQueryFailedClosed(SoftAssertions softly,
                                               String queryName,
                                               InvocationOutcome outcome) {
        softly.assertThat(outcome.value()).as("foreign %s value", queryName).isNull();
        softly.assertThat(outcome.failure()).as("foreign %s failure", queryName)
                .isNotNull()
                .hasMessageContaining("资金主体不存在");
    }

    private boolean hasPublicMethod(String methodName, Class<?>... parameterTypes) {
        try {
            FundsAccountQueryService.class.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private Object invokeAccountQuery(String methodName, Long tenantId, FundsAccountId accountId) {
        try {
            Method method;
            Object[] arguments;
            if (hasPublicMethod(methodName, Long.class, FundsAccountId.class)) {
                method = FundsAccountQueryService.class.getMethod(methodName, Long.class, FundsAccountId.class);
                arguments = new Object[]{tenantId, accountId};
            } else {
                method = FundsAccountQueryService.class.getMethod(methodName, FundsAccountId.class);
                arguments = new Object[]{accountId};
            }
            return method.invoke(fundsAccountQueryService, arguments);
        } catch (InvocationTargetException exception) {
            throw propagate(exception.getCause());
        } catch (ReflectiveOperationException exception) {
            String message = "调用资金账户查询公共方法失败，methodName = " + methodName;
            throw new IllegalStateException(message, exception);
        }
    }

    private InvocationOutcome invokeSafely(String methodName, Long tenantId, FundsAccountId accountId) {
        try {
            return new InvocationOutcome(invokeAccountQuery(methodName, tenantId, accountId), null);
        } catch (RuntimeException exception) {
            return new InvocationOutcome(null, exception);
        }
    }

    private RuntimeException propagate(Throwable cause) {
        if (cause instanceof RuntimeException exception) {
            return exception;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("资金账户查询异常", cause);
    }

    private void createFundingAccount(Long tenantId, String accountSn, String ownerId) {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setSn(accountSn)
                .setTenantId(tenantId)
                .setOwnerId(ownerId)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC));
    }

    private void createCreditAccount(Long tenantId, String accountSn, String ownerId) {
        creditAccountService.createCreditAccount(new CreateCreditAccountRequest()
                .setSn(accountSn)
                .setTenantId(tenantId)
                .setOwnerId(ownerId)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(USD));
    }

    private void seedLedgerBalances(Long tenantId,
                                    FundsSubjectType subjectType,
                                    String accountSn,
                                    long balanceAmount,
                                    int expectedLedgerCount) {
        int updated = jdbcTemplate.update("""
                        UPDATE t_ledger
                        SET debit_amount = CASE WHEN normal_balance_side = 'DEBIT' THEN ? ELSE 0 END,
                            credit_amount = CASE WHEN normal_balance_side = 'CREDIT' THEN ? ELSE 0 END
                        WHERE tenant_id = ? AND subject_type = ? AND subject_id = ?
                        """,
                balanceAmount,
                balanceAmount,
                tenantId,
                subjectType.name(),
                accountSn);
        assertThat(updated).isEqualTo(expectedLedgerCount);
    }

    private AccountFactSnapshot accountFactSnapshot() {
        return new AccountFactSnapshot(
                queryRows("t_funding_account"),
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

    private record InvocationOutcome(Object value, RuntimeException failure) {
    }

    private record AccountFactSnapshot(List<Map<String, Object>> fundingAccounts,
                                       List<Map<String, Object>> creditAccounts,
                                       List<Map<String, Object>> transactions,
                                       List<Map<String, Object>> transactionDetails,
                                       LedgerFactSnapshot ledgerFacts) {

        private AccountFactSnapshot {
            fundingAccounts = List.copyOf(fundingAccounts);
            creditAccounts = List.copyOf(creditAccounts);
            transactions = List.copyOf(transactions);
            transactionDetails = List.copyOf(transactionDetails);
        }
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

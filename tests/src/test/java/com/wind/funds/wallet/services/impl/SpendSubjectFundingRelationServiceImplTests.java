package com.wind.funds.wallet.services.impl;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
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

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 支出主体资金责任解析关系服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendSubjectFundingRelationServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendSubjectFundingRelationServiceImplTests extends AbstractFundsServiceTest {

    private static final String FUNDING_ACCOUNT_SN = "funding_relation_target";

    private static final String LONG_FUNDING_ACCOUNT_SN =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private static final String SECOND_FUNDING_ACCOUNT_SN = "funding_relation_second_target";

    private static final String CREDIT_TARGET_SN = "credit_relation_target_subject";

    private static final String SUSPENDED_CREDIT_TARGET_SN = "credit_rel_suspended_target";

    private static final String SPEND_SUBJECT_ID = "credit_relation_subject";

    private static final String LONG_SPEND_SUBJECT_ID =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private static final String OWNER_ID = "owner_relation_service";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateSpendSubjectFundingRelationShouldNotPostLedgerOrChangeFundingAccountBalance() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest(FUNDING_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        List<LedgerDTO> fundingLedgersBefore = loadFundingAccountLedgers(FUNDING_ACCOUNT_SN);

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, FUNDING_ACCOUNT_SN));

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                scopeQuery(SPEND_SUBJECT_ID, SpendSubjectFundingRelationType.FUNDING_SOURCE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(relationId).isPositive();
        assertThat(records).singleElement()
                .satisfies(relation -> {
                    assertThat(relation.getSn()).isNotBlank();
                    assertThat(relation.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(relation.getSpendSubjectId()).isEqualTo(SPEND_SUBJECT_ID);
                    assertThat(relation.getSpendSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(relation.getTargetSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
                    assertThat(relation.getTargetSubjectId()).isEqualTo(FUNDING_ACCOUNT_SN);
                    assertThat(relation.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(relation.getRelationType()).isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE);
                });
        assertThat(loadFundingAccountLedgers(FUNDING_ACCOUNT_SN))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(fundingLedgersBefore);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldSupportSixtyFourCharSubjectRefs() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest(LONG_FUNDING_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                LONG_SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, LONG_FUNDING_ACCOUNT_SN));

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(LONG_SPEND_SUBJECT_ID)
                        .setTargetSubjectId(LONG_FUNDING_ACCOUNT_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(relationId).isPositive();
        assertThat(records).singleElement()
                .satisfies(relation -> {
                    assertThat(relation.getSpendSubjectId()).isEqualTo(LONG_SPEND_SUBJECT_ID);
                    assertThat(relation.getTargetSubjectId()).isEqualTo(LONG_FUNDING_ACCOUNT_SN);
                });
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectMissingFundingAccountWithoutRelation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, "missing_relation_target")))
                .hasMessageContaining("资金账户不存在");

        assertThat(countRelations(SPEND_SUBJECT_ID)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectUnavailableFundingAccountWithoutRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest(FUNDING_ACCOUNT_SN)
                .setStatus(FundsAccountStatus.SUSPENDED));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, FUNDING_ACCOUNT_SN)))
                .hasMessageContaining("资金账户不可作为资金责任目标主体");

        assertThat(countRelations(SPEND_SUBJECT_ID)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectCurrencyMismatchWithoutRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest(FUNDING_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, FUNDING_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.CNY)))
                .hasMessageContaining("资金账户币种与资金责任解析关系币种不一致");

        assertThat(countRelations(SPEND_SUBJECT_ID)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectDuplicateScope() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest(FUNDING_ACCOUNT_SN));
        fundingAccountService.createFundingAccount(createFundingAccountRequest(SECOND_FUNDING_ACCOUNT_SN));
        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, FUNDING_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.FUNDING_ACCOUNT, SECOND_FUNDING_ACCOUNT_SN)))
                .isInstanceOf(RuntimeException.class);

        assertThat(countRelations(SPEND_SUBJECT_ID)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldSupportCreditAccountTargetSubject() {
        creditAccountService.createCreditAccount(createCreditAccountRequest(CREDIT_TARGET_SN, FundsAccountStatus.ACTIVE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.CREDIT_ACCOUNT, CREDIT_TARGET_SN));

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setTargetSubjectId(CREDIT_TARGET_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(relationId).isPositive();
        assertThat(records).singleElement()
                .satisfies(relation -> {
                    assertThat(relation.getTargetSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(relation.getTargetSubjectId()).isEqualTo(CREDIT_TARGET_SN);
                    assertThat(relation.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                });
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectUnavailableCreditAccountTargetWithoutRelation() {
        creditAccountService.createCreditAccount(createCreditAccountRequest(SUSPENDED_CREDIT_TARGET_SN,
                FundsAccountStatus.SUSPENDED));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest(
                SPEND_SUBJECT_ID, FundsSubjectType.CREDIT_ACCOUNT, SUSPENDED_CREDIT_TARGET_SN)))
                .hasMessageContaining("资金责任目标主体不可用");

        assertThat(countRelations(SPEND_SUBJECT_ID)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendSubjectFundingRelationTestData() {
        cleanupSpendSubjectFundingRelationTestData();
    }

    @AfterEach
    void tearDownSpendSubjectFundingRelationTestData() {
        cleanupSpendSubjectFundingRelationTestData();
    }

    private void cleanupSpendSubjectFundingRelationTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id IN (?, ?)",
                TENANT_ID,
                SPEND_SUBJECT_ID,
                LONG_SPEND_SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                FUNDING_ACCOUNT_SN,
                LONG_FUNDING_ACCOUNT_SN,
                SECOND_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                CREDIT_TARGET_SN,
                SUSPENDED_CREDIT_TARGET_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                FUNDING_ACCOUNT_SN,
                LONG_FUNDING_ACCOUNT_SN,
                SECOND_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn IN (?, ?)",
                CREDIT_TARGET_SN,
                SUSPENDED_CREDIT_TARGET_SN);
    }

    private CreateFundingAccountRequest createFundingAccountRequest(String sn) {
        return new CreateFundingAccountRequest()
                .setSn(sn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
    }

    private CreateSpendSubjectFundingRelationRequest createRelationRequest(String spendSubjectId,
                                                                           FundsSubjectType targetSubjectType,
                                                                           String targetSubjectId) {
        return new CreateSpendSubjectFundingRelationRequest()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(spendSubjectId)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(targetSubjectType)
                .setTargetSubjectId(targetSubjectId)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE);
    }

    private SpendSubjectFundingRelationQuery scopeQuery(String spendSubjectId,
                                                        SpendSubjectFundingRelationType relationType) {
        return new SpendSubjectFundingRelationQuery()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(spendSubjectId)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(relationType);
    }

    private List<LedgerDTO> loadFundingAccountLedgers(String accountSn) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(accountSn)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private CreateCreditAccountRequest createCreditAccountRequest(String sn, FundsAccountStatus status) {
        return new CreateCreditAccountRequest()
                .setSn(sn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setStatus(status);
    }

    private long countRelations(String spendSubjectId) {
        Long result = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM t_spend_subject_funding_rel
                        WHERE tenant_id = ? AND spend_subject_id = ?
                        """,
                Long.class,
                TENANT_ID,
                spendSubjectId);
        return result == null ? 0L : result;
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class
    })
    static class Config {
    }
}

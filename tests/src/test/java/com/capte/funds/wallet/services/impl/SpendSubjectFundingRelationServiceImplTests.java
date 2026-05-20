package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.capte.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.capte.funds.wallet.model.request.CreateFundingAccountRequest;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.capte.funds.wallet.service.FundingAccountService;
import com.capte.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.FundingAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.SpendSubjectFundingRelationType;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 支出主体资金来源关系服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendSubjectFundingRelationServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendSubjectFundingRelationServiceImplTests extends AbstractFundsServiceTest {

    private static final String RELATION_SN = "spend_funding_rel_service";

    private static final String FUNDING_ACCOUNT_SN = "funding_relation_target";

    private static final String SPEND_SUBJECT_ID = "credit_relation_subject";

    private static final String OWNER_ID = "owner_relation_service";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateSpendSubjectFundingRelationShouldNotPostLedgerOrChangeFundingAccountBalance() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        LedgerFacts before = loadLedgerFacts();
        List<LedgerDTO> fundingLedgersBefore = loadFundingAccountLedgers();

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest());

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setFundingAccountId(FUNDING_ACCOUNT_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setDefaultRelation(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(relationId).isPositive();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst())
                .satisfies(relation -> {
                    assertThat(relation.getSn()).isEqualTo(RELATION_SN);
                    assertThat(relation.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(relation.getSpendSubjectId()).isEqualTo(SPEND_SUBJECT_ID);
                    assertThat(relation.getSpendSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(relation.getFundingAccountId()).isEqualTo(FUNDING_ACCOUNT_SN);
                    assertThat(relation.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(relation.getRelationType())
                            .isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE);
                    assertThat(relation.getPriority()).isEqualTo(20);
                    assertThat(relation.getDefaultRelation()).isTrue();
                    assertThat(relation.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
                });
        assertThat(loadFundingAccountLedgers())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(fundingLedgersBefore);
        assertLedgerFacts(before);
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
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn = ?", RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn = ?", FUNDING_ACCOUNT_SN);
    }

    private CreateFundingAccountRequest createFundingAccountRequest() {
        return new CreateFundingAccountRequest()
                .setSn(FUNDING_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
    }

    private CreateSpendSubjectFundingRelationRequest createRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setSn(RELATION_SN)
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(SPEND_SUBJECT_ID)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setFundingAccountId(FUNDING_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setPriority(20)
                .setDefaultRelation(Boolean.TRUE);
    }

    private List<LedgerDTO> loadFundingAccountLedgers() {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(FUNDING_ACCOUNT_SN)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private LedgerFacts loadLedgerFacts() {
        return new LedgerFacts(
                countRows("t_ledger"),
                countRows("t_ledger_transaction"),
                countRows("t_ledger_posting_plan"),
                countRows("t_ledger_entry"));
    }

    private void assertLedgerFacts(LedgerFacts expected) {
        assertThat(loadLedgerFacts()).isEqualTo(expected);
    }

    private long countRows(String tableName) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return result;
    }

    private record LedgerFacts(long ledgers, long transactions, long postingPlans, long entries) {
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class
    })
    static class Config {
    }
}

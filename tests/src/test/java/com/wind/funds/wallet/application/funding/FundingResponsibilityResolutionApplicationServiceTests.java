package com.wind.funds.wallet.application.funding;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.FundingResponsibilityDecisionDTO;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.ResolveFundingResponsibilityRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
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
 * 资金责任解析应用服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundingResponsibilityResolutionApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundingResponsibilityResolutionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String FUNDING_ACCOUNT_SN = "app_funding_target";

    private static final String CREDIT_ACCOUNT_SN = "app_credit_target";

    private static final String SPEND_SUBJECT_ID = "app_spend_subject";

    private static final String OWNER_ID = "app_owner_subject";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private FundingResponsibilityResolutionApplicationService resolutionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testResolveFundingResponsibilityShouldReturnCurrentDefaultFundingAccountDecisionWithoutLedgerSideEffect() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundingResponsibilityDecisionDTO decision =
                resolutionApplicationService.resolveFundingResponsibility(resolveRequest());

        assertThat(decision)
                .satisfies(result -> {
                    assertThat(result.getRelationId()).isEqualTo(relationId);
                    assertThat(result.getRelationSn()).isNotBlank();
                    assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(result.getSpendSubjectId()).isEqualTo(SPEND_SUBJECT_ID);
                    assertThat(result.getSpendSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(result.getTargetSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
                    assertThat(result.getTargetSubjectId()).isEqualTo(FUNDING_ACCOUNT_SN);
                    assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(result.getRelationType()).isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE);
                });
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResolveFundingResponsibilityShouldReturnCurrentDefaultCreditAccountDecisionWithoutLedgerSideEffect() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createCreditRelationRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundingResponsibilityDecisionDTO decision =
                resolutionApplicationService.resolveFundingResponsibility(resolveRequest());

        assertThat(decision)
                .satisfies(result -> {
                    assertThat(result.getRelationId()).isEqualTo(relationId);
                    assertThat(result.getRelationSn()).isNotBlank();
                    assertThat(result.getTargetSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(result.getTargetSubjectId()).isEqualTo(CREDIT_ACCOUNT_SN);
                    assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(result.getRelationType()).isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE);
                });
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResolveFundingResponsibilityShouldRejectMissingDefaultRelationWithoutLedgerSideEffect() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> resolutionApplicationService.resolveFundingResponsibility(resolveRequest()))
                .hasMessageContaining("资金责任关系不存在");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResolveFundingResponsibilityShouldRejectTenantMismatchWithoutLedgerSideEffect() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> resolutionApplicationService.resolveFundingResponsibility(resolveRequest()
                .setTenantId(TENANT_ID + 1)))
                .hasMessageContaining("资金责任解析 tenantId 与当前租户不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpFundingResponsibilityResolutionTestData() {
        cleanupFundingResponsibilityResolutionTestData();
    }

    @AfterEach
    void tearDownFundingResponsibilityResolutionTestData() {
        cleanupFundingResponsibilityResolutionTestData();
    }

    private void cleanupFundingResponsibilityResolutionTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID,
                SPEND_SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                FUNDING_ACCOUNT_SN,
                CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn = ?", FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
    }

    private ResolveFundingResponsibilityRequest resolveRequest() {
        return new ResolveFundingResponsibilityRequest()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(SPEND_SUBJECT_ID)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE);
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
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .setState(FundsAccountState.ACTIVE);
    }

    private CreateCreditAccountRequest createCreditAccountRequest() {
        return new CreateCreditAccountRequest()
                .setSn(CREDIT_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setState(FundsAccountState.ACTIVE);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(SPEND_SUBJECT_ID)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setTargetSubjectId(FUNDING_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE);
    }

    private CreateSpendSubjectFundingRelationRequest createCreditRelationRequest() {
        return createFundingRelationRequest()
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_ACCOUNT_SN);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            FundingResponsibilityResolutionApplicationServiceImpl.class
    })
    static class Config {
    }
}

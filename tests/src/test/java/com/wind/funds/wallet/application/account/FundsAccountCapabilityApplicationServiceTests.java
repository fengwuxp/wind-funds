package com.wind.funds.wallet.application.account;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.dto.FundsAccountCapabilityDecisionDTO;
import com.wind.funds.wallet.model.request.ResolveFundsAccountCapabilityRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
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
 * 资金账户能力准入应用服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsAccountCapabilityApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundsAccountCapabilityApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String RECEIVE_ONLY_FUNDING_ACCOUNT_SN = "cap_receive_funding";

    private static final String CREDIT_ACCOUNT_SN = "capability_credit_account";

    private static final String FROZEN_FUNDING_ACCOUNT_SN = "capability_frozen_funding";

    private static final String OWNER_ID = "capability_owner";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private FundsAccountCapabilityApplicationService capabilityApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：资金账户产品配置显式收窄为只收款账户。
     * 输入：FUNDING_BASIC profile，但 contextVariables.fundsAccountCapabilities = ["RECEIVE"]。
     * 输出：账户能力准入返回 RECEIVE，付款和提现均不可用。
     * 红线：账户能力配置只能收窄账户可用范围，不能让后续支付工具准入绕过账户能力。
     */
    @Test
    void testResolveFundingAccountCapabilityShouldUseExplicitContextCapabilities() {
        fundingAccountService.createFundingAccount(createReceiveOnlyFundingAccountRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundsAccountCapabilityDecisionDTO decision = capabilityApplicationService
                .resolveFundsAccountCapability(resolveRequest(RECEIVE_ONLY_FUNDING_ACCOUNT_SN,
                        FundsSubjectType.FUNDING_ACCOUNT));

        assertThat(decision.getAccountId()).isEqualTo(FundsAccountId.immutable(
                RECEIVE_ONLY_FUNDING_ACCOUNT_SN,
                FundsSubjectType.FUNDING_ACCOUNT));
        assertThat(decision.getState()).isEqualTo(FundsAccountState.ACTIVE);
        assertThat(decision.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(decision.getCapabilities()).containsExactly(FundsAccountCapability.RECEIVE);
        assertThat(decision.getCanReceive()).isTrue();
        assertThat(decision.getCanPay()).isFalse();
        assertThat(decision.getCanWithdraw()).isFalse();
        assertThat(decision.getCapabilitySource()).isEqualTo("CONTEXT_VARIABLES");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：信用账户产品配置试图扩大为收款账户。
     * 输入：CREDIT_BASIC profile，但 contextVariables.fundsAccountCapabilities 包含 RECEIVE。
     * 输出：查询准入失败。
     * 红线：显式配置不得突破 ledger profile 的安全能力基线。
     */
    @Test
    void testResolveCreditAccountCapabilityShouldRejectCapabilityOutsideProfileBaseline() {
        creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setContextVariables("{\"fundsAccountCapabilities\":[\"PAY\",\"RECEIVE\"]}"));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> capabilityApplicationService.resolveFundsAccountCapability(
                resolveRequest(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT)))
                .hasMessageContaining("账户显式能力不能超出 profile 安全能力");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：冻结资金账户具备收款能力，但发起付款准入检查。
     * 输入：FROZEN 资金账户，profile 默认能力包含 PAY。
     * 输出：能力快照仍可解释 profile 能力，但状态门禁拒绝付款。
     * 红线：能力来源通过不等于当前状态允许出账。
     */
    @Test
    void testResolveFrozenFundingAccountCapabilityShouldKeepStatusAdmissionClosed() {
        fundingAccountService.createFundingAccount(createFrozenFundingAccountRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundsAccountCapabilityDecisionDTO decision = capabilityApplicationService
                .resolveFundsAccountCapability(resolveRequest(FROZEN_FUNDING_ACCOUNT_SN,
                        FundsSubjectType.FUNDING_ACCOUNT));

        assertThat(decision.getCapabilities()).containsExactlyInAnyOrder(FundsAccountCapability.RECEIVE,
                FundsAccountCapability.PAY,
                FundsAccountCapability.WITHDRAW);
        assertThat(decision.getCanReceive()).isTrue();
        assertThat(decision.getCanPay()).isFalse();
        assertThat(decision.getCanWithdraw()).isFalse();
        assertThat(decision.getCapabilitySource()).isEqualTo("LEDGER_PROFILE");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResolveFundsAccountCapabilityShouldRejectTenantMismatchWithoutLedgerSideEffect() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> capabilityApplicationService.resolveFundsAccountCapability(
                resolveRequest(RECEIVE_ONLY_FUNDING_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT)
                        .setTenantId(TENANT_ID + 1)))
                .hasMessageContaining("资金账户能力准入 tenantId 与当前租户不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpFundsAccountCapabilityApplicationServiceTestData() {
        cleanupFundsAccountCapabilityApplicationServiceTestData();
    }

    @AfterEach
    void tearDownFundsAccountCapabilityApplicationServiceTestData() {
        cleanupFundsAccountCapabilityApplicationServiceTestData();
    }

    private void cleanupFundsAccountCapabilityApplicationServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                RECEIVE_ONLY_FUNDING_ACCOUNT_SN,
                CREDIT_ACCOUNT_SN,
                FROZEN_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                RECEIVE_ONLY_FUNDING_ACCOUNT_SN,
                FROZEN_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
    }

    private ResolveFundsAccountCapabilityRequest resolveRequest(String accountSn, FundsSubjectType subjectType) {
        return new ResolveFundsAccountCapabilityRequest()
                .setTenantId(TENANT_ID)
                .setAccountId(FundsAccountId.immutable(accountSn, subjectType))
                .setCurrency(CurrencyIsoCode.USD);
    }

    private CreateFundingAccountRequest createReceiveOnlyFundingAccountRequest() {
        return baseFundingAccountRequest(RECEIVE_ONLY_FUNDING_ACCOUNT_SN, FundsAccountState.ACTIVE)
                .setContextVariables("{\"fundsAccountCapabilities\":[\"RECEIVE\"]}");
    }

    private CreateFundingAccountRequest createFrozenFundingAccountRequest() {
        return baseFundingAccountRequest(FROZEN_FUNDING_ACCOUNT_SN, FundsAccountState.FROZEN);
    }

    private CreateFundingAccountRequest baseFundingAccountRequest(String sn, FundsAccountState state) {
        return new CreateFundingAccountRequest()
                .setSn(sn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .setState(state);
    }

    private CreateCreditAccountRequest createCreditAccountRequest() {
        return new CreateCreditAccountRequest()
                .setSn(CREDIT_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setState(FundsAccountState.ACTIVE);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class
    })
    static class Config {
    }
}

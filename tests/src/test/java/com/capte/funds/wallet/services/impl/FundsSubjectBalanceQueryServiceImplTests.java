package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.capte.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.FundingAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
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

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金主体余额查询服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsSubjectBalanceQueryServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundsSubjectBalanceQueryServiceImplTests extends AbstractFundsServiceTest {

    private static final String UNINITIALIZED_ACCOUNT_SN = "funding_balance_query_uninitialized";

    private static final String OWNER_ID = "owner_balance_query";

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：批量查询资金主体当前余额时，主体存在但账本尚未初始化。
     * 输入：FUNDING_ACCOUNT 主体存在，t_ledger 没有对应余额桶。
     * 输出：返回未初始化余额视图，不自动创建账本。
     * 红线：余额查询只读账本投影，不初始化账本、不修复余额、不写交易或分录事实。
     */
    @Test
    void testQueryCurrentBalancesShouldReportUninitializedSubjectWithoutLedgerMutation() {
        insertFundingAccountWithoutLedgers();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<FundsSubjectBalanceDTO> balances = balanceQueryService.queryCurrentBalances(balanceQuery());

        assertThat(balances)
                .singleElement()
                .satisfies(balance -> {
                    assertThat(balance.isInitialized()).isFalse();
                    assertThat(balance.getSubjectRef())
                            .isEqualTo(FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN,
                                    FundsSubjectType.FUNDING_ACCOUNT));
                    assertThat(balance.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(balance.getBalanceBuckets()).isEmpty();
                });
        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：必需余额查询用于交易或控制链路前置校验，主体存在但账本缺失。
     * 输入：FUNDING_ACCOUNT 主体存在，t_ledger 没有对应余额桶。
     * 输出：明确失败，提示资金主体账本不存在。
     * 红线：必需查询不得用空余额冒充可用余额，也不得自动补账本后继续。
     */
    @Test
    void testGetRequiredCurrentBalanceShouldRejectUninitializedSubjectWithoutLedgerMutation() {
        insertFundingAccountWithoutLedgers();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.getRequiredCurrentBalance(balanceQuery()))
                .hasMessageContaining("资金主体账本不存在");

        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpFundsSubjectBalanceQueryServiceTestData() {
        cleanupFundsSubjectBalanceQueryServiceTestData();
    }

    @AfterEach
    void tearDownFundsSubjectBalanceQueryServiceTestData() {
        cleanupFundsSubjectBalanceQueryServiceTestData();
    }

    private void cleanupFundsSubjectBalanceQueryServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", UNINITIALIZED_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn = ?", UNINITIALIZED_ACCOUNT_SN);
    }

    private FundsSubjectBalanceQuery balanceQuery() {
        return new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN,
                        FundsSubjectType.FUNDING_ACCOUNT)))
                .setCurrency(CURRENCY);
    }

    private void insertFundingAccountWithoutLedgers() {
        FundingAccount account = new FundingAccount();
        account.setSn(UNINITIALIZED_ACCOUNT_SN);
        account.setTenantId(TENANT_ID);
        account.setOwnerId(OWNER_ID);
        account.setOwnerType(FundsAccountOwnerType.USER);
        account.setAccountType(FundingAccountType.USER_WALLET.name());
        account.setPlatform(Boolean.FALSE);
        account.setCurrency(CurrencyIsoCode.USD);
        account.setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("funds subject balance query boundary test");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private long countLedgers() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger WHERE subject_id = ?",
                Long.class,
                UNINITIALIZED_ACCOUNT_SN);
        return result;
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

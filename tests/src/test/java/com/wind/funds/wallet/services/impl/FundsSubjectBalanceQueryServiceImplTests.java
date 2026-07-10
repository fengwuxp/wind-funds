package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.dal.entities.SpendControlScope;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.SpendControlScopeMapper;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
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

import java.time.LocalTime;
import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
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

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = "SPEND_CONTROL_SCOPE";

    private static final String UNINITIALIZED_ACCOUNT_SN = "fbal_query_uninit";

    private static final String SECOND_ACCOUNT_SN = "fbal_query_second";

    private static final String MISSING_ACCOUNT_SN = "fbal_query_missing";

    private static final String OWNER_ID = "owner_balance_query";

    private static final String SECOND_OWNER_ID = "owner_balance_query_second";

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    private static final String NEXT_MONTH_PERIOD_ID = "2026-06";

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private SpendControlScopeMapper spendControlScopeMapper;

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

    /**
     * 场景：余额查询请求携带重复资金主体。
     * 输入：subjectRefs 中同一个 FUNDING_ACCOUNT 出现两次。
     * 输出：参数校验失败。
     * 红线：批量余额查询不得返回重复视图，避免交易前置校验重复汇总同一余额桶。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectDuplicateSubjectRefsWithoutLedgerMutation() {
        FundsAccountId subjectRef = FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN,
                FundsSubjectType.FUNDING_ACCOUNT);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(subjectRef, subjectRef))
                .setCurrency(CURRENCY)))
                .hasMessageContaining("资金主体余额查询 subjectRefs 不能重复");

        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：批量余额查询的资金主体不存在。
     * 输入：FUNDING_ACCOUNT 主体引用不存在。
     * 输出：明确失败，提示资金主体不存在。
     * 红线：余额查询不得把不存在主体解释为未初始化余额，也不得补建账户或账本。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectMissingSubjectWithoutLedgerMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(FundsAccountId.immutable(MISSING_ACCOUNT_SN,
                        FundsSubjectType.FUNDING_ACCOUNT)))
                .setCurrency(CURRENCY)))
                .hasMessageContaining("资金主体不存在");

        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：必需余额查询误传多个资金主体。
     * 输入：getRequiredCurrentBalance 的 subjectRefs 包含两个不同主体。
     * 输出：参数校验失败。
     * 红线：必需余额只能作为单主体前置校验，不能把多主体结果折叠成一个余额视图。
     */
    @Test
    void testGetRequiredCurrentBalanceShouldRejectMultipleSubjectsWithoutLedgerMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(
                        FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT),
                        FundsAccountId.immutable(SECOND_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT)))
                .setCurrency(CURRENCY)))
                .hasMessageContaining("资金主体必需余额查询 subjectRefs 只能包含一个主体");

        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营或交易前置校验批量查询多个资金主体的同一余额桶。
     * 输入：两个 FUNDING_ACCOUNT 主体均已初始化 AVAILABLE 和 FROZEN，查询顺序与入参相反，并只查询 AVAILABLE。
     * 输出：结果顺序与入参一致，只返回被请求的 AVAILABLE 余额桶。
     * 红线：余额查询只能筛选已有账本投影，不得因批量查询重排主体、补建科目或写入账本事实。
     */
    @Test
    void testQueryCurrentBalancesShouldKeepSubjectOrderAndFilterLedgerBucketsWithoutLedgerMutation() {
        insertFundingAccountWithLifetimeLedgers(UNINITIALIZED_ACCOUNT_SN, OWNER_ID);
        insertFundingAccountWithLifetimeLedgers(SECOND_ACCOUNT_SN, SECOND_OWNER_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<FundsSubjectBalanceDTO> balances = balanceQueryService.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(
                        FundsAccountId.immutable(SECOND_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT),
                        FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT)))
                .setCurrency(CURRENCY)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE)));

        assertThat(balances)
                .extracting(FundsSubjectBalanceDTO::getSubjectRef)
                .containsExactly(
                        FundsAccountId.immutable(SECOND_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT),
                        FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT));
        assertThat(balances).allSatisfy(balance -> {
            assertThat(balance.isInitialized()).isTrue();
            assertThat(balance.getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.AVAILABLE);
            LedgerBalanceBucket bucket = balance.getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE);
            assertThat(bucket.periodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
            assertThat(bucket.periodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        });
        assertThat(countLedgers()).isEqualTo(4);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：必需余额查询指定某个余额桶，但主体只初始化了其他余额桶。
     * 输入：FUNDING_ACCOUNT 已初始化 AVAILABLE，必需查询 FROZEN。
     * 输出：明确失败，提示资金主体账本不完整。
     * 红线：交易前置校验不得用“已有其他账本”替代被请求的余额桶。
     */
    @Test
    void testGetRequiredCurrentBalanceShouldRejectMissingRequestedBucketWithoutLedgerMutation() {
        insertFundingAccountWithoutLedgers();
        createLifetimeLedger(UNINITIALIZED_ACCOUNT_SN, LedgerSubjectCode.AVAILABLE);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.getRequiredCurrentBalance(balanceQuery()
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.FROZEN))))
                .hasMessageContaining("资金主体账本不完整");

        assertThat(countLedgers()).isEqualTo(1);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：查询周期型余额时调用方遗漏 periodId。
     * 输入：periodType=MONTHLY，periodId=null。
     * 输出：参数校验失败。
     * 红线：非 LIFETIME 周期不得退化成默认生命周期余额，也不得在失败时写任何账本事实。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectNonLifetimePeriodWithoutPeriodIdAndLedgerMutation() {
        insertFundingAccountWithLifetimeLedgers(UNINITIALIZED_ACCOUNT_SN, OWNER_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.queryCurrentBalances(balanceQuery()
                .setPeriodType(AccountBalancePeriodType.MONTHLY)))
                .hasMessageContaining("资金主体余额查询 periodId 不能为空");

        assertThat(countLedgers()).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：生命周期余额查询显式传入空白 periodId。
     * 输入：FUNDING_ACCOUNT 已初始化 AVAILABLE / LIFETIME，查询 periodType=LIFETIME、periodId=空白字符串。
     * 输出：按 LIFETIME 默认周期返回 AVAILABLE 余额桶。
     * 红线：LIFETIME 查询不得因外部空白 periodId 退化为未初始化，也不得写任何账本事实。
     */
    @Test
    void testQueryCurrentBalancesShouldNormalizeBlankLifetimePeriodIdWithoutLedgerMutation() {
        insertFundingAccountWithoutLedgers();
        createLifetimeLedger(UNINITIALIZED_ACCOUNT_SN, LedgerSubjectCode.AVAILABLE);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundsSubjectBalanceDTO balance = balanceQueryService.getRequiredCurrentBalance(balanceQuery()
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId("   ")
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE)));

        LedgerBalanceBucket bucket = balance.getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE);
        assertThat(balance.isInitialized()).isTrue();
        assertThat(bucket.periodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(bucket.periodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        assertThat(countLedgers()).isEqualTo(1);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一资金主体同时存在生命周期余额和月度预算型余额桶。
     * 输入：FUNDING_ACCOUNT 初始化 AVAILABLE / LIFETIME 与 AVAILABLE / MONTHLY / 2026-05。
     * 输出：默认查询只返回 LIFETIME，月度查询只返回指定月份。
     * 红线：账本周期是余额隔离键，余额查询不得把生命周期余额和月度余额混用。
     */
    @Test
    void testQueryCurrentBalancesShouldKeepLifetimeAndMonthlyPeriodIsolatedWithoutLedgerMutation() {
        insertFundingAccountWithoutLedgers();
        createLedger(UNINITIALIZED_ACCOUNT_SN, LedgerSubjectCode.AVAILABLE,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
        createLedger(UNINITIALIZED_ACCOUNT_SN, LedgerSubjectCode.AVAILABLE,
                AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundsSubjectBalanceDTO lifetimeBalance = balanceQueryService.getRequiredCurrentBalance(balanceQuery()
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE)));
        FundsSubjectBalanceDTO monthlyBalance = balanceQueryService.getRequiredCurrentBalance(balanceQuery()
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(MONTHLY_PERIOD_ID)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE)));

        LedgerBalanceBucket lifetimeBucket = lifetimeBalance.getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE);
        assertThat(lifetimeBucket.periodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(lifetimeBucket.periodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        LedgerBalanceBucket monthlyBucket = monthlyBalance.getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE);
        assertThat(monthlyBucket.periodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(monthlyBucket.periodId()).isEqualTo(MONTHLY_PERIOD_ID);
        assertThat(countLedgers()).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方查询不存在的月度余额桶。
     * 输入：主体已有 LIFETIME 与 2026-05 月度 AVAILABLE，查询 2026-06 月度 AVAILABLE。
     * 输出：普通查询返回未初始化视图，必需查询明确失败。
     * 红线：余额查询不能因为目标月份缺桶而回退到 LIFETIME 或其他月份。
     */
    @Test
    void testQueryCurrentBalancesShouldNotFallbackToOtherPeriodWithoutLedgerMutation() {
        insertFundingAccountWithoutLedgers();
        createLedger(UNINITIALIZED_ACCOUNT_SN, LedgerSubjectCode.AVAILABLE,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
        createLedger(UNINITIALIZED_ACCOUNT_SN, LedgerSubjectCode.AVAILABLE,
                AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<FundsSubjectBalanceDTO> balances = balanceQueryService.queryCurrentBalances(balanceQuery()
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(NEXT_MONTH_PERIOD_ID)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE)));

        assertThat(balances)
                .singleElement()
                .satisfies(balance -> {
                    assertThat(balance.isInitialized()).isFalse();
                    assertThat(balance.getBalanceBuckets()).isEmpty();
                });
        assertThatThrownBy(() -> balanceQueryService.getRequiredCurrentBalance(balanceQuery()
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(NEXT_MONTH_PERIOD_ID)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE))))
                .hasMessageContaining("资金主体账本不存在");
        assertThat(countLedgers()).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史支出控制范围记录仍存在，但目标态余额查询只接受核心资金账务主体。
     * 输入：t_spend_control_scope 有支出控制范围元数据，subjectRefs 传入 SPEND_CONTROL_SCOPE。
     * 输出：余额查询按资金主体不存在拒绝，不返回 initialized=false 的余额视图。
     * 红线：支出控制范围是控制范围，不得通过余额查询继续冒充 ledger 余额主体。
     */
    @Test
    void testQueryCurrentBalancesShouldRejectSpendControlScopeSubjectEvenWhenSpendControlScopeExists() {
        insertSpendControlScopeWithoutLedgers();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> balanceQueryService.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN,
                        SPEND_CONTROL_SCOPE_ACCOUNT_TYPE)))
                .setCurrency(CURRENCY)))
                .hasMessageContaining("资金主体不存在");

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
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                UNINITIALIZED_ACCOUNT_SN,
                SECOND_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                UNINITIALIZED_ACCOUNT_SN,
                SECOND_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_spend_control_scope WHERE sn = ?", UNINITIALIZED_ACCOUNT_SN);
    }

    private FundsSubjectBalanceQuery balanceQuery() {
        return new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(FundsAccountId.immutable(UNINITIALIZED_ACCOUNT_SN,
                        FundsSubjectType.FUNDING_ACCOUNT)))
                .setCurrency(CURRENCY);
    }

    private void insertFundingAccountWithoutLedgers() {
        insertFundingAccountWithoutLedgers(UNINITIALIZED_ACCOUNT_SN, OWNER_ID);
    }

    private void insertFundingAccountWithoutLedgers(String accountSn, String ownerId) {
        FundingAccount account = new FundingAccount();
        account.setSn(accountSn);
        account.setTenantId(TENANT_ID);
        account.setOwnerId(ownerId);
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

    private void insertFundingAccountWithLifetimeLedgers(String accountSn, String ownerId) {
        insertFundingAccountWithoutLedgers(accountSn, ownerId);
        createLifetimeLedger(accountSn, LedgerSubjectCode.AVAILABLE);
        createLifetimeLedger(accountSn, LedgerSubjectCode.FROZEN);
    }

    private void insertSpendControlScopeWithoutLedgers() {
        SpendControlScope spendControlScope = new SpendControlScope();
        spendControlScope.setSn(UNINITIALIZED_ACCOUNT_SN);
        spendControlScope.setTenantId(TENANT_ID);
        spendControlScope.setOwnerId(OWNER_ID);
        spendControlScope.setOwnerType(FundsAccountOwnerType.USER);
        spendControlScope.setScopeType(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE);
        spendControlScope.setCurrency(CurrencyIsoCode.USD);
        spendControlScope.setPeriodType(AccountBalancePeriodType.LIFETIME);
        spendControlScope.setPeriodId(AccountBalancePeriodType.LIFETIME.name());
        spendControlScope.setStatus(FundsAccountStatus.ACTIVE);
        spendControlScope.setDescription("budget group must not be a balance subject");
        spendControlScope.setVersion(0);
        spendControlScopeMapper.insertSelective(spendControlScope);
    }

    private void createLifetimeLedger(String accountSn, LedgerSubjectCode ledgerSubjectCode) {
        createLedger(accountSn, ledgerSubjectCode, AccountBalancePeriodType.LIFETIME,
                AccountBalancePeriodType.LIFETIME.name());
    }

    private void createLedger(String accountSn,
                              LedgerSubjectCode ledgerSubjectCode,
                              AccountBalancePeriodType periodType,
                              String periodId) {
        ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountSn)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(periodType)
                .setPeriodId(periodId));
    }

    private long countLedgers() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger WHERE subject_id IN (?, ?)",
                Long.class,
                UNINITIALIZED_ACCOUNT_SN,
                SECOND_ACCOUNT_SN);
        return result;
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

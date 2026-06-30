package com.wind.funds.transaction.application.external;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.wind.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.wind.funds.route.CompositeRouteResolver;
import com.wind.funds.route.DefaultRouteReplayService;
import com.wind.funds.route.DefaultRouteSnapshotFactory;
import com.wind.funds.route.TransferFundsInstructionRouteResolver;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.transaction.application.ExternalFundsEventApplicationService;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.transaction.application.external.impl.ExternalFundsEventApplicationServiceImpl;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.transaction.model.request.ConsumeExternalFundsEventRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.wallet.services.impl.AccountHierarchyBindingServiceImpl;
import com.wind.funds.wallet.services.impl.AccountHierarchyServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerFactQueryService;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
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

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 外部资金事件消费应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ExternalFundsEventApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExternalFundsEventApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String BUSINESS_SCENE = "EXTERNAL_FUNDS_EVENT";

    private static final String BUSINESS_SN = "EXTERNAL_FUNDS_EVENT_001";

    private static final String TARGET_ACCOUNT_SN = "external_event_target_acc";

    private static final String CASH_MAPPING_ACCOUNT_SN = "external_event_cash_map";

    private static final String PREPAYMENT_ACCOUNT_SN = "external_event_prepay";

    private static final String EXTERNAL_SOURCE_ACCOUNT_SN = "external_funds_event_source";

    @Autowired
    private ExternalFundsEventApplicationService externalFundsEventApplicationService;

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：ACH、银行文件或渠道回调确认一笔外部入金，wallet 外部事件入口归一后委派标准充值内核。
     * 输入：ach_credit_confirmed、资金账户、外部事件流水、金额、币种和业务流水。
     * 输出：返回内部充值交易号，目标资金账户 AVAILABLE 增加，并生成标准 TOPUP 交易、route 和账本事实。
     * 红线：外部事件入口负责把外部事件类型归一为交易渠道，不直接写交易事实或账本事实。
     */
    @Test
    void testConsumeConfirmedCreditEventShouldDelegateTopupKernel() {
        createCreditConsumeScenario();
        FundsSubjectBalanceDTO before = balance(targetAccountId());
        assertBucket(before, LedgerSubjectCode.AVAILABLE, 0L, CurrencyIsoCode.USD);

        String transactionSn = externalFundsEventApplicationService.consume(consumeRequest(), WindOperator.system());

        assertThat(transactionSn).isNotBlank();
        FundsSubjectBalanceDTO after = balance(targetAccountId());
        assertBucket(after, LedgerSubjectCode.AVAILABLE, 90L, CurrencyIsoCode.USD);
        assertThat(fundsTransactionStatus()).isEqualTo(FundsTransactionStatus.CLOSED.name());
        assertThat(fundsTransactionDetailStatuses())
                .hasSize(3)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED.name());
        assertThat(ledgerTransactionEvents()).containsExactly(FundsTransactionEventType.TOPUP.name());
        assertThat(ledgerEntrySubjects())
                .contains(TARGET_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
        assertThat(ledgerEntrySubjectCodes())
                .contains(LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCode.CASH.name(),
                        LedgerSubjectCode.PREPAYMENT.name());
        assertThat(postingPlanCount()).isEqualTo(2);
        assertThat(ledgerEntryCount()).isEqualTo(4);
        assertExternalEventRouteSnapshot();
    }

    /**
     * 场景：外部事件类型是扣款确认，但本切片只授权正向入金消费。
     * 输入：BANK_DEBIT_CONFIRMED、目标资金账户和完整业务流水。
     * 输出：服务层入口在交易内核前拒绝。
     * 红线：未授权的扣款、退票、撤销或差异处理不得伪装成充值入账。
     */
    @Test
    void testConsumeUnsupportedDebitEventShouldFailFastBeforeFundsFacts() {
        createCreditConsumeScenario();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        ConsumeExternalFundsEventRequest request = consumeRequest()
                .setExternalEventType("BANK_DEBIT_CONFIRMED");

        assertThatThrownBy(() -> externalFundsEventApplicationService.consume(request, WindOperator.system()))
                .hasMessageContaining("外部资金事件类型暂不支持真实消费");

        assertNoFundsOrLedgerFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部入金事件目标是信用账户，但当前只接入资金账户充值内核。
     * 输入：ACH_CREDIT_CONFIRMED、信用账户主体和完整业务流水。
     * 输出：服务层入口在交易内核前拒绝。
     * 红线：信用账户还款、调额或授信消耗不能被默认包装为资金账户充值。
     */
    @Test
    void testConsumeCreditAccountTargetShouldFailFastBeforeFundsFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        ConsumeExternalFundsEventRequest request = consumeRequest()
                .setTargetAccountId(FundsAccountId.immutable("external_event_credit_acc",
                        FundsSubjectType.CREDIT_ACCOUNT));

        assertThatThrownBy(() -> externalFundsEventApplicationService.consume(request, WindOperator.system()))
                .hasMessageContaining("外部资金入金事件目标账户必须是资金账户");

        assertNoFundsOrLedgerFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpExternalFundsEventTestData() {
        cleanupExternalFundsEventTestData();
    }

    @AfterEach
    void tearDownExternalFundsEventTestData() {
        cleanupExternalFundsEventTestData();
    }

    private void cleanupExternalFundsEventTestData() {
        jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn IN (
                    SELECT sn FROM t_ledger_transaction
                    WHERE business_scene = ? AND business_sn = ?
                )
                """, BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_frozen_order WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE business_scene = ? AND business_sn = ?",
                BUSINESS_SCENE, BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                TARGET_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                TARGET_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
    }

    private void createCreditConsumeScenario() {
        createTargetFundingAccount();
        createPlatformFundingAccount(CASH_MAPPING_ACCOUNT_SN, PlatformFundingAccountRole.CASH_MAPPING);
        createPlatformFundingAccount(PREPAYMENT_ACCOUNT_SN, PlatformFundingAccountRole.PREPAYMENT);
        createTestLedger(cashMappingAccountId(), LedgerSubjectCode.CASH, 10_000L);
        createTestLedger(prepaymentAccountId(), LedgerSubjectCode.PREPAYMENT, 0L);
    }

    private void createTargetFundingAccount() {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setSn(TARGET_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId("owner_external_event")
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.GLOBAL_ACCOUNT.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .setStatus(FundsAccountStatus.ACTIVE));
    }

    private void createPlatformFundingAccount(String accountSn, PlatformFundingAccountRole role) {
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountSn);
        account.setOwnerId("platform");
        account.setOwnerType(FundsAccountOwnerType.PLATFORM);
        account.setAccountType(role.name());
        account.setPlatform(Boolean.TRUE);
        account.setAccountRoleCode(role);
        account.setCurrency(CurrencyIsoCode.USD);
        account.setLedgerProfileCode(role.getLedgerProfileCode());
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("external funds event platform funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void createTestLedger(FundsAccountId accountId,
                                  LedgerSubjectCode subjectCode,
                                  long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setLedgerProfileCode("TEST")
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(subjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        if (initialBalance != 0L) {
            ledgerService.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                    .setId(ledgerId)
                    .setCreditAmountDelta(initialBalance > 0L ? initialBalance : null)
                    .setDebitAmountDelta(initialBalance < 0L ? -initialBalance : null));
        }
    }

    private ConsumeExternalFundsEventRequest consumeRequest() {
        return new ConsumeExternalFundsEventRequest()
                .setTenantId(TENANT_ID)
                .setExternalEventSn("bank_event_001")
                .setExternalEventType("ach_credit_confirmed")
                .setTargetAccountId(targetAccountId())
                .setAmount(90L)
                .setCurrency(CurrencyIsoCode.USD)
                .setOriginalTransactionSn("original_funds_tx_001")
                .setReconciliationDifferenceSn("recon_diff_001")
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setDescription("external funds event contract");
    }

    private FundsAccountId targetAccountId() {
        return FundsAccountId.immutable(TARGET_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private FundsAccountId cashMappingAccountId() {
        return FundsAccountId.immutable(CASH_MAPPING_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private FundsAccountId prepaymentAccountId() {
        return FundsAccountId.immutable(PREPAYMENT_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
        return balanceQueryService.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(accountId))
                .setCurrency(CurrencyIsoCode.USD));
    }

    private String fundsTransactionStatus() {
        return jdbcTemplate.queryForObject("""
                SELECT status FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private List<String> fundsTransactionDetailStatuses() {
        return jdbcTemplate.queryForList("""
                SELECT status FROM t_funds_transaction_detail
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private List<String> ledgerTransactionEvents() {
        return jdbcTemplate.queryForList("""
                SELECT event_type FROM t_ledger_transaction
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private List<String> ledgerEntrySubjects() {
        return jdbcTemplate.queryForList("""
                SELECT subject_id FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private List<String> ledgerEntrySubjectCodes() {
        return jdbcTemplate.queryForList("""
                SELECT ledger_subject_code FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private void assertExternalEventRouteSnapshot() {
        JSONObject routeSnapshot = JSON.parseObject(routeSnapshotJson());
        JSONObject externalAccountRef = routeSnapshot.getJSONObject("externalAccountRef");
        assertThat(externalAccountRef).isNotNull().isNotEmpty();
        assertThat(externalAccountRef.getString("externalAccountId")).isEqualTo(EXTERNAL_SOURCE_ACCOUNT_SN);
        assertThat(externalAccountRef.getString("externalAccountType"))
                .isEqualTo("EXTERNAL_BANK");
        assertThat(externalAccountRef.getString("providerCode")).isEqualTo("ACH_RAIL");
        assertThat(externalAccountRef.getString("channelCode")).isEqualTo("WIRE_TRANSFER");
        assertThat(externalAccountRef.getJSONObject("contextVariables")
                .getString("externalTransactionId")).isEqualTo("bank_event_001");
    }

    private String routeSnapshotJson() {
        return jdbcTemplate.queryForObject("""
                SELECT route_snapshot FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private void assertNoFundsOrLedgerFacts() {
        assertThat(fundsTransactionCount()).isZero();
        assertThat(fundsTransactionDetailCount()).isZero();
        assertThat(ledgerTransactionCount()).isZero();
        assertThat(ledgerEntryCount()).isZero();
        assertThat(postingPlanCount()).isZero();
    }

    private Integer fundsTransactionCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private Integer fundsTransactionDetailCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_funds_transaction_detail
                WHERE business_scene = ? AND business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private Integer ledgerTransactionCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private Integer postingPlanCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private Integer ledgerEntryCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    @Configuration
    @Import({
            FundsDirectTransactionInstructionConverter.class,
            FundsBalanceControlInstructionConverter.class,
            FundsAuthorizationInstructionConverter.class,
            RouteParticipantFactory.class,
            RouteSubjectSupport.class,
            PlatformAccountRouteSupport.class,
            DefaultRouteReplayService.class,
            TransferFundsInstructionRouteResolver.class,
            BalanceControlFundsInstructionRouteResolver.class,
            AuthorizationFundsInstructionRouteResolver.class,
            CompositeRouteResolver.class,
            DefaultRouteSnapshotFactory.class,
            DefaultLedgerPostingAssembler.class,
            DefaultRoutedFundsInstructionOrchestrator.class,
            FundsTransactionCommandServiceImpl.class,
            LedgerServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class,
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultFundsInstructionLifecycleSaver.class,
            DefaultFundsFrozenOrderLifecycleSaver.class,
            DelegatingFundsInstructionLifecycleRecorder.class,
            DefaultFundsTransactionQueryService.class,
            DefaultLedgerFactQueryService.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            AccountHierarchyBindingServiceImpl.class,
            AccountHierarchyServiceImpl.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class,
            ExternalFundsEventApplicationServiceImpl.class
    })
    static class Config {
    }
}

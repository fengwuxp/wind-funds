package com.wind.funds.wallet.application.instrument;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingConcurrencyGuard;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
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
 * 支付工具预交易快照应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PaymentInstrumentPreTransactionSnapshotApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentInstrumentPreTransactionSnapshotApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "pre_tx_credit_account";

    private static final String PAYMENT_INSTRUMENT_SN = "pre_tx_payment_card";

    private static final String RECEIVE_INSTRUMENT_SN = "pre_tx_receive_card";

    private static final String PAYMENT_BINDING_SN = "pre_tx_payment_binding";

    private static final String FUNDING_RELATION_SN = "pre_tx_funding_rel";

    private static final String OWNER_ID = "pre_tx_owner";

    private static final String CHANNEL_CODE = "pre_tx_channel";

    private static final String BUSINESS_SCENE = "PRE_TRANSACTION_SNAPSHOT";

    private static final String BUSINESS_SN = "PRE_TX_SNAPSHOT_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private PaymentInstrumentPreTransactionSnapshotApplicationService snapshotApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：支付工具发起授权前生成完整只读快照。
     * 输入：支付工具绑定信用账户，资金责任也解析到同一信用账户，账户具备付款能力。
     * 输出：返回工具能力、资金责任、账户能力和最终目标账户快照。
     * 红线：预交易快照只读，不得创建资金交易、route、posting、LedgerEntry 或余额投影事实。
     */
    @Test
    void testResolvePreTransactionSnapshotShouldCombineAdmissionDecisionsWithoutFundsSideEffect() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentDirection.PAYMENT));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest(PAYMENT_INSTRUMENT_SN));
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                snapshotApplicationService.resolvePreTransactionSnapshot(snapshotRequest(PAYMENT_INSTRUMENT_SN)
                        .setAction(PaymentInstrumentAction.AUTHORIZE));

        assertThat(snapshot.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(snapshot.getBusinessScene()).isEqualTo(BUSINESS_SCENE);
        assertThat(snapshot.getBusinessSn()).isEqualTo(BUSINESS_SN);
        assertThat(snapshot.getAmount()).isEqualTo(60L);
        assertThat(snapshot.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(snapshot.getAction()).isEqualTo(PaymentInstrumentAction.AUTHORIZE);
        assertThat(snapshot.getReady()).isTrue();
        assertThat(snapshot.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(snapshot.getPaymentInstrumentCapability().getInstrumentSn()).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(snapshot.getPaymentInstrumentCapability().getBindingSn()).isEqualTo(PAYMENT_BINDING_SN);
        assertThat(snapshot.getPaymentInstrumentCapability().getBindingVersion()).isEqualTo(1);
        assertThat(snapshot.getFundingResponsibility().getRelationSn()).isEqualTo(FUNDING_RELATION_SN);
        assertThat(snapshot.getFundingResponsibility().getTargetSubjectType())
                .isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
        assertThat(snapshot.getFundingResponsibility().getTargetSubjectId()).isEqualTo(CREDIT_ACCOUNT_SN);
        assertThat(snapshot.getFundsAccountCapability().getCapabilities())
                .containsExactly(FundsAccountCapability.PAY);
        assertThat(snapshot.getFundsAccountCapability().getCanPay()).isTrue();
        assertThat(snapshot.getFundsAccountCapability().getCapabilitySource()).isEqualTo("LEDGER_PROFILE");
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具方向不支持当前预交易动作。
     * 输入：RECEIVE-only 工具发起授权预交易快照。
     * 输出：准入阶段失败。
     * 红线：预交易快照失败不得写交易、route、posting、LedgerEntry 或余额投影事实。
     */
    @Test
    void testResolvePreTransactionSnapshotShouldRejectInstrumentCapabilityWithoutFundsSideEffect() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(RECEIVE_INSTRUMENT_SN,
                PaymentInstrumentDirection.RECEIVE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> snapshotApplicationService.resolvePreTransactionSnapshot(
                snapshotRequest(RECEIVE_INSTRUMENT_SN).setAction(PaymentInstrumentAction.AUTHORIZE)))
                .hasMessageContaining("支付工具方向不支持当前动作");

        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpPaymentInstrumentPreTransactionSnapshotTestData() {
        cleanupPaymentInstrumentPreTransactionSnapshotTestData();
    }

    @AfterEach
    void tearDownPaymentInstrumentPreTransactionSnapshotTestData() {
        cleanupPaymentInstrumentPreTransactionSnapshotTestData();
    }

    private void cleanupPaymentInstrumentPreTransactionSnapshotTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn = ?", FUNDING_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
    }

    private ResolvePaymentInstrumentPreTransactionSnapshotRequest snapshotRequest(String instrumentSn) {
        return new ResolvePaymentInstrumentPreTransactionSnapshotRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(instrumentSn)
                .setAction(PaymentInstrumentAction.PAY)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(1)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN);
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
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentDirection direction) {
        return new CreatePaymentInstrumentRequest()
                .setSn(instrumentSn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setInstrumentDirection(direction)
                .setInstrumentNo("****8642")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_pre_tx_8642")
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest(String instrumentSn) {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(PAYMENT_BINDING_SN)
                .setRequestSn(PAYMENT_BINDING_SN + "_create")
                .setTenantId(TENANT_ID)
                .setInstrumentSn(instrumentSn)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setSubjectId(CREDIT_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setSn(FUNDING_RELATION_SN)
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(CREDIT_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setPriority(10)
                .setDefaultRelation(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private void assertNoTransactionFacts(String businessSn) {
        assertThat(countRows("t_funds_transaction", businessSn)).isZero();
        assertThat(countRows("t_funds_transaction_detail", businessSn)).isZero();
        assertThat(postingPlanCount(businessSn)).isZero();
        assertThat(countRows("t_ledger_transaction", businessSn)).isZero();
        assertThat(countRows("t_ledger_entry", businessSn)).isZero();
    }

    private Integer postingPlanCount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, businessSn);
    }

    private int countRows(String tableName, String businessSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, businessSn);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            PaymentInstrumentBindingConcurrencyGuard.class,
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class,
            FundingResponsibilityResolutionApplicationServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}

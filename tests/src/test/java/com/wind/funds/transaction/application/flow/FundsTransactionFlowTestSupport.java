package com.wind.funds.transaction.application.flow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.dal.entities.table.LedgerEntryNameRefs;
import com.wind.funds.ledger.dal.entities.table.LedgerPostingPlanNameRefs;
import com.wind.funds.ledger.dal.entities.table.LedgerTransactionNameRefs;
import com.wind.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.wind.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.wind.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.wind.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.wind.funds.route.CompositeRouteResolver;
import com.wind.funds.route.DefaultRouteReplayService;
import com.wind.funds.route.DefaultRouteSnapshotFactory;
import com.wind.funds.route.TransferFundsInstructionRouteResolver;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.support.FundsBalanceAssertionSupport;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.application.impl.FundsBenefitContributionTransactionServiceImpl;
import com.wind.funds.transaction.application.impl.DefaultFundsBalanceAdjustmentAuditApplicationService;
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.projection.impl.DefaultFundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.wind.funds.transaction.dal.entities.table.FundsTransactionDetailNameRefs;
import com.wind.funds.transaction.dal.entities.table.FundsTransactionNameRefs;
import com.wind.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyBindingRequest;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.service.AccountHierarchyService;
import com.wind.funds.wallet.service.SpendControlScopeService;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.services.impl.AccountHierarchyBindingServiceImpl;
import com.wind.funds.wallet.services.impl.AccountHierarchyServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlScopeServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerQueryService;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.transaction.enums.DefaultFeeType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 资金交易流程测试基座。
 *
 * <p>服务层流程测试使用真实账户查询、平台账户解析、交易转换、路由解析、编排器、记账翻译、
 * lifecycle saver、posting service、mapper 和 H2 表结构；账户依赖由测试 setup 显式准备，
 * 确保断言落在真实服务路径和持久化事实上。</p>
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class FundsTransactionFlowTestSupport extends AbstractFundsServiceTest {

    private static final LocalDateTime ACTIVE_TIME = LocalDateTime.of(2026, 5, 18, 0, 0);

    private static final int MAX_LEDGER_BUCKET_SIZE = 50;

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = "SPEND_CONTROL_SCOPE";

    private static final List<String> FLOW_TEST_TABLES = List.of(
            "t_ledger_entry",
            "t_ledger_posting_plan",
            "t_ledger_transaction",
            "t_funds_frozen_order",
            "t_funds_transaction_detail",
            "t_funds_transaction",
            "t_account_hierarchy_binding",
            "t_funding_account",
            "t_credit_account",
            "t_spend_control_scope",
            "t_ledger");

    @Autowired
    protected FundsDirectTransactionService directTransactionService;

    @Autowired
    protected FundsAuthorizationTransactionService authorizationTransactionService;

    @Autowired
    protected FundsBalanceControlService balanceControlService;

    @Autowired
    protected FundsTransactionQueryService fundsTransactionQueryService;

    @Autowired
    protected LedgerService ledgerService;

    @Autowired
    protected CreditAccountService creditAccountService;

    @Autowired
    protected AccountHierarchyService accountHierarchyService;

    @Autowired
    protected SpendControlScopeService spendControlScopeService;

    @Autowired
    private LedgerTransactionMapper ledgerTransactionMapper;

    @Autowired
    private LedgerPostingPlanMapper ledgerPostingPlanMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @Autowired
    private FundsTransactionMapper fundsTransactionMapper;

    @Autowired
    private FundsTransactionDetailMapper fundsTransactionDetailMapper;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupFlowTestData();
        seedFundingAccounts();
        seedLedgers();
    }

    private void cleanupFlowTestData() {
        FLOW_TEST_TABLES.forEach(table -> jdbcTemplate.update("DELETE FROM " + table));
    }

    private void seedLedgers() {
        FundsAccountId user = fundingAccount("funding_user");
        ensureLedger(user, LedgerSubjectCode.AVAILABLE, 0L);
        ensureLedger(user, LedgerSubjectCode.FROZEN, 0L);
        ensureLedger(user, LedgerSubjectCode.AUTHORIZATION, 0L);
        ensureLedger(cashMappingAccount(), LedgerSubjectCode.CASH, 10_000L);
        ensureLedger(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L);
        ensureLedger(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L);
        ensureLedger(feeAccount(), LedgerSubjectCode.FEE, 0L);
    }

    private void seedFundingAccounts() {
        ensureFundingAccount(fundingAccount("funding_user"));
        for (PlatformFundingAccountRole role : PlatformFundingAccountRole.values()) {
            ensurePlatformFundingAccount(role);
        }
    }

    protected void ensureFundingAccount(FundsAccountId accountId) {
        if (!FundsSubjectType.FUNDING_ACCOUNT.name().equals(accountId.type())
                || fundingAccountExists(accountId.id())) {
            return;
        }
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountId.id());
        account.setOwnerId("owner_" + accountId.id());
        account.setOwnerType(FundsAccountOwnerType.USER);
        account.setAccountType(accountId.type());
        account.setPlatform(Boolean.FALSE);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("flow test funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void ensurePlatformFundingAccount(PlatformFundingAccountRole role) {
        FundsAccountId accountId = platformAccount(role);
        if (fundingAccountExists(accountId.id())) {
            return;
        }
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountId.id());
        account.setOwnerId("platform");
        account.setOwnerType(FundsAccountOwnerType.PLATFORM);
        account.setAccountType(accountId.type());
        account.setPlatform(Boolean.TRUE);
        account.setAccountRoleCode(role);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(role.getLedgerProfileCode());
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("flow test platform funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private boolean fundingAccountExists(String accountSn) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.sn.eq(accountSn));
        return fundingAccountMapper.selectCountByQuery(wrapper) > 0;
    }

    private boolean spendControlScopeExists(String spendControlScopeSn) {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM t_spend_control_scope
                        WHERE tenant_id = ?
                          AND sn = ?
                        """,
                Long.class,
                TENANT_ID,
                spendControlScopeSn);
        return count != null && count > 0;
    }

    private void ensureLedger(FundsAccountId accountId,
                              LedgerSubjectCode ledgerSubjectCode,
                              long initialBalance) {
        ensureFundingAccount(accountId);
        Optional<LedgerDTO> existing = findLedger(accountId, ledgerSubjectCode);
        if (existing.isPresent()) {
            return;
        }
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setLedgerProfileCode("TEST")
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
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

    protected void ensureLedger(FundsAccountId accountId, LedgerSubjectCode ledgerSubjectCode) {
        ensureLedger(accountId, ledgerSubjectCode, 0L);
    }

    protected void allowNegativeLedger(FundsAccountId accountId, LedgerSubjectCode ledgerSubjectCode) {
        int updated = jdbcTemplate.update("""
                        UPDATE t_ledger
                        SET is_allow_negative = 1
                        WHERE tenant_id = ?
                          AND subject_id = ?
                          AND subject_type = ?
                          AND ledger_subject_code = ?
                          AND currency = ?
                          AND period_type = ?
                          AND period_id = ?
                        """,
                TENANT_ID,
                accountId.id(),
                accountId.type(),
                ledgerSubjectCode.name(),
                CURRENCY.name(),
                AccountBalancePeriodType.LIFETIME.name(),
                AccountBalancePeriodType.LIFETIME.name());
        assertThat(updated)
                .as("ledger allowNegative enabled for accountId %s and subject %s", accountId, ledgerSubjectCode)
                .isOne();
    }

    protected void ensureCreditAccount(FundsAccountId accountId) {
        assertThat(accountId.type()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT.name());
        if (!findLedgers(accountId).isEmpty()) {
            return;
        }
        creditAccountService.createCreditAccount(new CreateCreditAccountRequest()
                .setSn(accountId.id())
                .setTenantId(TENANT_ID)
                .setOwnerId("owner_" + accountId.id())
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CURRENCY));
    }

    protected void ensureSpendControlScope(FundsAccountId accountId) {
        assertThat(accountId.type()).isEqualTo(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE);
        if (spendControlScopeExists(accountId.id())) {
            return;
        }
        spendControlScopeService.createSpendControlScope(new CreateSpendControlScopeRequest()
                .setSn(accountId.id())
                .setTenantId(TENANT_ID)
                .setOwnerId("owner_scope")
                .setOwnerType(FundsAccountOwnerType.USER)
                .setScopeType(SpendRuleScopeType.SPEND_CONTROL_SCOPE.name())
                .setCurrency(CURRENCY));
    }

    protected void ensureSpendControlScopeWithoutLedgers(FundsAccountId accountId) {
        ensureSpendControlScope(accountId);
        assertThat(findLedgers(accountId)).isEmpty();
    }

    protected void bindAccountHierarchy(FundsAccountId accountId,
                                        FundsAccountId parentAccountId,
                                        String businessSn) {
        accountHierarchyService.createAccountHierarchyBinding(new CreateAccountHierarchyBindingRequest()
                .setSn("AH_" + businessSn)
                .setTenantId(TENANT_ID)
                .setAccountId(accountId)
                .setParentAccountId(parentAccountId)
                .setCurrency(CURRENCY)
                .setOperatorId("flow-test"));
    }

    protected void adjustBalance(FundsAccountId accountId, long amount, boolean increase, String businessSn) {
        balanceControlService.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setIncrease(increase)
                .setBusinessScene("BALANCE_ADJUST")
                .setBusinessSn(businessSn)
                .setAdjustReason("flow test balance adjust")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn)
                .setDescription("balance adjust"), WindOperator.system());
    }

    protected void topup(FundsAccountId accountId, long amount, String businessSn) {
        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(accountId)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn(businessSn + "_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("TOPUP")
                .setBusinessSn(businessSn)
                .setDescription("topup"), WindOperator.system());
    }

    protected void transfer(FundsAccountId payerAccountId,
                            FundsAccountId payeeAccountId,
                            long amount,
                            String businessSn) {
        directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payerAccountId)
                .setPayeeAccountId(payeeAccountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn(businessSn)
                .setDescription("transfer"), WindOperator.system());
    }

    protected String pay(FundsAccountId accountId,
                         FundsAccountId payeeId,
                         LedgerSubjectCode payeeLedgerCode,
                         long amount,
                         String businessSn) {
        return directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(accountId)
                .setPayeeId(payeeId)
                .setPayeeLedgerCode(payeeLedgerCode)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("PAY")
                .setBusinessSn(businessSn)
                .setDescription("pay"), WindOperator.system());
    }

    protected void payWithFixedFee(FundsAccountId accountId,
                                   FundsAccountId payeeId,
                                   LedgerSubjectCode payeeLedgerCode,
                                   long amount,
                                   long feeAmount,
                                   String businessSn) {
        directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(accountId)
                .setPayeeId(payeeId)
                .setPayeeLedgerCode(payeeLedgerCode)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setFeeSpec(FeeSpec.builder()
                        .feeType(DefaultFeeType.FEE.getCode())
                        .fixedFee(Math.toIntExact(feeAmount))
                        .build())
                .setBusinessScene("PAY")
                .setBusinessSn(businessSn)
                .setDescription("pay with fee"), WindOperator.system());
    }

    protected void refund(FundsAccountId accountId,
                          FundsAccountId payerId,
                          LedgerSubjectCode payerLedgerCode,
                          long amount,
                          String businessSn) {
        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(accountId)
                .setPayerId(payerId)
                .setPayerLedgerCode(payerLedgerCode)
                .setAmount(amount(amount))
                .setBusinessScene("REFUND")
                .setBusinessSn(businessSn)
                .setDescription("refund"), WindOperator.system());
    }

    protected String refundFee(FundsAccountId accountId,
                               long amount,
                               String feeSourceTransactionSn,
                               String businessSn) {
        return directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setFeeSourceTransactionSn(feeSourceTransactionSn)
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("fee refund"), WindOperator.system());
    }

    protected void fee(FundsAccountId accountId, long amount, String businessSn) {
        directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn(businessSn)
                .setDescription("fee"), WindOperator.system());
    }

    protected void withdraw(FundsAccountId accountId, long amount, String referenceFreezeSn, String businessSn) {
        directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(referenceFreezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn(businessSn)
                .setDescription("withdraw"), WindOperator.system());
    }

    protected void withdrawWithFixedFee(FundsAccountId accountId,
                                        long amount,
                                        long feeAmount,
                                        String referenceFreezeSn,
                                        String businessSn) {
        directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(referenceFreezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setFeeSpec(FeeSpec.builder()
                        .feeType(DefaultFeeType.FEE.getCode())
                        .fixedFee(Math.toIntExact(feeAmount))
                        .build())
                .setBusinessScene("WITHDRAW")
                .setBusinessSn(businessSn)
                .setDescription("withdraw with fee"), WindOperator.system());
    }

    protected String freeze(FundsAccountId accountId, long amount, String businessSn) {
        return balanceControlService.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setBusinessScene("FREEZE")
                .setBusinessSn(businessSn)
                .setDescription("freeze"), WindOperator.system());
    }

    protected String authorize(FundsAccountId accountId,
                               long amount,
                               boolean approved,
                               String businessSn) {
        return authorizationTransactionService.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setApproved(approved)
                .setBusinessScene("AUTHORIZATION")
                .setBusinessSn(businessSn)
                .setDescription("authorization"), WindOperator.system());
    }

    protected String declineAuthorization(FundsAccountId accountId,
                                          long amount,
                                          String declineReason,
                                          String businessSn) {
        return authorizationTransactionService.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setApproved(false)
                .setDeclineReason(declineReason)
                .setBusinessScene("AUTHORIZATION")
                .setBusinessSn(businessSn)
                .setDescription("authorization declined"), WindOperator.system());
    }

    protected String settleAuthorization(FundsAccountId accountId,
                                         long amount,
                                         String authorizationTransactionSn,
                                         String businessSn) {
        return authorizationTransactionService.settle(new FundsAuthorizationTransactionSettleRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(amount)))
                .setAuthorizationTransactionSn(authorizationTransactionSn)
                .setBusinessScene("AUTHORIZATION_SETTLE")
                .setBusinessSn(businessSn)
                .setDescription("authorization settle"), WindOperator.system());
    }

    protected String refundSettledAuthorization(FundsAccountId accountId,
                                                long amount,
                                                String authorizationTransactionSn,
                                                String businessSn) {
        return authorizationTransactionService.settleRefund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setAuthorizationTransactionSn(authorizationTransactionSn)
                .setBusinessScene("AUTHORIZATION_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("authorization refund"), WindOperator.system());
    }

    protected String refundWithoutAuthorization(FundsAccountId accountId,
                                                long amount,
                                                String businessSn) {
        return authorizationTransactionService.settleRefund(noAuthRefundRequest(accountId, amount,
                businessSn), WindOperator.system());
    }

    protected FundsAuthorizationTransactionRefundRequest noAuthRefundRequest(FundsAccountId accountId,
                                                                             long amount,
                                                                             String businessSn) {
        return new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setExternalReferenceSn("processor_capture_202606030001")
                .setRefundReason("external capture refunded without internal authorization")
                .setBusinessScene("AUTHORIZATION_NO_AUTH_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("authorization no-auth refund");
    }

    protected String reverseAuthorization(FundsAccountId accountId,
                                          long amount,
                                          String authorizationTransactionSn,
                                          String businessSn) {
        return authorizationTransactionService.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setAuthorizationTransactionSn(authorizationTransactionSn)
                .setBusinessScene("AUTHORIZATION_REVERSAL")
                .setBusinessSn(businessSn)
                .setDescription("authorization reversal"), WindOperator.system());
    }

    protected void unfreeze(FundsAccountId accountId, long amount, String referenceFreezeSn, String businessSn) {
        balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(accountId)
                .setAmount(amount(amount))
                .setReferenceFreezeSn(referenceFreezeSn)
                .setBusinessScene("UNFREEZE")
                .setBusinessSn(businessSn)
                .setDescription("unfreeze"), WindOperator.system());
    }

    protected List<FundsSubjectBalanceDTO> balances(FundsAccountId... accountIds) {
        return List.of(accountIds).stream()
                .map(this::balance)
                .toList();
    }

    protected FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
        Map<LedgerSubjectCode, LedgerBalanceBucket> buckets = balanceBuckets(accountId);
        return new FundsSubjectBalanceDTO()
                .setId(1L)
                .setTenantId(TENANT_ID)
                .setSubjectRef(accountId)
                .setCurrency(CURRENCY)
                .setInitialized(!buckets.isEmpty())
                .setBalanceBuckets(buckets);
    }

    protected void assertPostedTransactions(int expectedSize) {
        List<LedgerTransaction> transactions = ledgerTransactions();
        List<String> transactionSns = transactions.stream()
                .map(LedgerTransaction::getSn)
                .toList();
        assertThat(postingPlans())
                .as("ledger posting plans must belong to posted ledger transactions")
                .extracting(LedgerPostingPlan::getLedgerTransactionSn)
                .containsOnlyElementsOf(transactionSns);
        assertThat(entries())
                .as("ledger entries must belong to posted ledger transactions")
                .extracting(LedgerEntry::getLedgerTransactionSn)
                .containsOnlyElementsOf(transactionSns);
        assertThat(transactions).hasSize(expectedSize);
        transactions.forEach(this::assertValidPostedTransaction);
    }

    protected void assertNoLedgerFactsForFundsTransaction(String fundsTransactionSn) {
        assertThat(ledgerTransactionsByFundsTransactionSn(fundsTransactionSn))
                .as("ledger transactions for funds transaction %s", fundsTransactionSn)
                .isEmpty();
        assertThat(postingPlansByFundsTransactionSn(fundsTransactionSn))
                .as("posting plans for funds transaction %s", fundsTransactionSn)
                .isEmpty();
        assertThat(entriesByFundsTransactionSn(fundsTransactionSn))
                .as("ledger entries for funds transaction %s", fundsTransactionSn)
                .isEmpty();
    }

    protected LedgerFactSnapshot ledgerFactSnapshot() {
        return FundsBalanceAssertionSupport.ledgerFactSnapshot(jdbcTemplate);
    }

    protected void clearFundsTransactionRouteSnapshot(String transactionSn) {
        int updated = jdbcTemplate.update("""
                UPDATE t_funds_transaction
                SET route_snapshot = NULL
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, transactionSn);
        assertThat(updated)
                .as("funds transaction route snapshot cleared for transactionSn %s", transactionSn)
                .isOne();
    }

    protected void clearLedgerFactsForFundsTransaction(String transactionSn) {
        int deletedEntries = jdbcTemplate.update("""
                DELETE FROM t_ledger_entry
                WHERE tenant_id = ? AND funds_transaction_sn = ?
                """, TENANT_ID, transactionSn);
        int deletedPlans = jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE tenant_id = ? AND funds_transaction_sn = ?
                """, TENANT_ID, transactionSn);
        int deletedTransactions = jdbcTemplate.update("""
                DELETE FROM t_ledger_transaction
                WHERE tenant_id = ? AND funds_transaction_sn = ?
                """, TENANT_ID, transactionSn);
        assertThat(deletedEntries)
                .as("ledger entries cleared for funds transaction %s", transactionSn)
                .isPositive();
        assertThat(deletedPlans)
                .as("posting plans cleared for funds transaction %s", transactionSn)
                .isPositive();
        assertThat(deletedTransactions)
                .as("ledger transactions cleared for funds transaction %s", transactionSn)
                .isPositive();
    }

    protected void enrichFundsTransactionRouteSnapshot(String transactionSn,
                                                       Map<String, Object> values) {
        String routeSnapshotJson = jdbcTemplate.queryForObject("""
                SELECT route_snapshot
                FROM t_funds_transaction
                WHERE tenant_id = ? AND sn = ?
                """, String.class, TENANT_ID, transactionSn);
        assertThat(routeSnapshotJson)
                .as("funds transaction route snapshot must exist before enrichment for transactionSn %s",
                        transactionSn)
                .isNotBlank();
        JSONObject routeSnapshot = JSON.parseObject(routeSnapshotJson);
        values.forEach(routeSnapshot::put);
        int updated = jdbcTemplate.update("""
                UPDATE t_funds_transaction
                SET route_snapshot = ?
                WHERE tenant_id = ? AND sn = ?
                """, JSON.toJSONString(routeSnapshot), TENANT_ID, transactionSn);
        assertThat(updated)
                .as("funds transaction route snapshot enriched for transactionSn %s", transactionSn)
                .isOne();
    }

    protected void assertLedgerTransactionFactsUnchanged(LedgerFactSnapshot expected) {
        FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged(jdbcTemplate, expected);
    }

    protected void assertFailedFundsTransactionWithoutLedgerFacts(String businessSn) {
        List<FundsTransaction> fundsTransactions = fundsTransactionsByBusinessSn(businessSn);
        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(businessSn);
        assertThat(fundsTransactions)
                .as("failed funds transactions for businessSn %s", businessSn)
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.FAILED);
                    assertReadableRouteSnapshot(transaction.getSn(), businessSn);
                    assertNoLedgerFactsForFundsTransaction(transaction.getSn());
                    assertThat(details)
                            .as("failed funds transaction details must belong to transaction for businessSn %s",
                                    businessSn)
                            .extracting(FundsTransactionDetail::getTransactionSn)
                            .containsOnly(transaction.getSn());
                });
        assertThat(details)
                .as("funds transaction details for businessSn %s", businessSn)
                .isNotEmpty()
                .allSatisfy(detail -> {
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.FAILED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                    assertThat(detail.getErrorCode()).isNotBlank();
                    assertThat(detail.getErrorMessage()).isNotBlank();
                });
        assertThat(ledgerTransactionsForBusinessSn(businessSn))
                .as("ledger transactions for businessSn %s", businessSn)
                .isEmpty();
        assertThat(entriesByBusinessSn(businessSn))
                .as("ledger entries for businessSn %s", businessSn)
                .isEmpty();
    }

    protected void assertNoFundsOrLedgerFactsForBusinessSn(String businessSn) {
        assertNoPersistedTransactionFactsForBusinessSn(businessSn);
    }

    protected void assertNoPersistedTransactionFactsForBusinessSn(String businessSn) {
        assertThat(fundsTransactionsByBusinessSn(businessSn))
                .as("funds transactions for businessSn %s", businessSn)
                .isEmpty();
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction details for businessSn %s", businessSn)
                .isEmpty();
        assertThat(ledgerTransactionsForBusinessSn(businessSn))
                .as("ledger transactions for businessSn %s", businessSn)
                .isEmpty();
        assertThat(entriesByBusinessSn(businessSn))
                .as("ledger entries for businessSn %s", businessSn)
                .isEmpty();
    }

    protected void assertSingleFundsAndLedgerFactsForBusinessSn(String businessSn, int expectedDetails,
                                                                int expectedPostingPlans, int expectedEntries) {
        assertFundsAndLedgerFactsForBusinessSn(businessSn, 1, expectedDetails, expectedPostingPlans, expectedEntries);
    }

    protected void assertFundsAndLedgerFactsForBusinessSn(String businessSn, int expectedTransactions,
                                                          int expectedDetails, int expectedPostingPlans,
                                                          int expectedEntries) {
        List<FundsTransaction> fundsTransactions = fundsTransactionsByBusinessSn(businessSn);
        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(businessSn);
        List<LedgerTransaction> ledgerTransactions = ledgerTransactionsForBusinessSn(businessSn);
        assertThat(fundsTransactions)
                .as("funds transactions for businessSn %s", businessSn)
                .hasSize(expectedTransactions);
        if (!fundsTransactions.isEmpty()) {
            assertThat(fundsTransactions)
                    .as("successful funds transactions must have stable status and route snapshot for businessSn %s",
                            businessSn)
                    .allSatisfy(transaction -> {
                        assertThat(transaction.getStatus())
                                .isIn(FundsTransactionStatus.OPEN, FundsTransactionStatus.CLOSED);
                        assertReadableRouteSnapshot(transaction.getSn(), businessSn);
                    });
        }
        assertThat(details)
                .as("funds transaction details for businessSn %s", businessSn)
                .hasSize(expectedDetails);
        assertThat(ledgerTransactions)
                .as("ledger transactions for businessSn %s", businessSn)
                .singleElement()
                .satisfies(ledgerTransaction -> {
                    List<LedgerPostingPlan> postingPlans = postingPlansOf(ledgerTransaction);
                    List<LedgerEntry> entries = entriesByBusinessSn(businessSn);
                    List<String> postingPlanSns = postingPlans.stream()
                            .map(LedgerPostingPlan::getSn)
                            .toList();
                    assertThat(ledgerTransaction.getStatus()).isEqualTo(LedgerTransactionStatus.POSTED);
                    if (!fundsTransactions.isEmpty()) {
                        assertThat(ledgerTransaction.getFundsTransactionSn())
                                .as("ledger transaction must point to funds transaction for businessSn %s",
                                        businessSn)
                                .isIn(fundsTransactions.stream().map(FundsTransaction::getSn).toList());
                    }
                    assertThat(details)
                            .as("funds transaction details must point to ledger transaction for businessSn %s",
                                    businessSn)
                            .allSatisfy(detail -> {
                                assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.SUCCEEDED);
                                assertThat(detail.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
                            });
                    assertThat(postingPlans)
                            .as("posting plans for businessSn %s", businessSn)
                            .hasSize(expectedPostingPlans);
                    assertThat(postingPlans)
                            .as("posting plans must point to funds transaction for businessSn %s", businessSn)
                            .allSatisfy(plan -> {
                                assertThat(plan.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
                                assertThat(plan.getFundsTransactionSn())
                                        .isEqualTo(ledgerTransaction.getFundsTransactionSn());
                            });
                    assertThat(entries)
                            .as("ledger entries for businessSn %s", businessSn)
                            .hasSize(expectedEntries);
                    assertThat(entries)
                            .as("ledger entries must point to ledger transaction and posting plans for businessSn %s",
                                    businessSn)
                            .allSatisfy(entry -> {
                                assertThat(entry.getLedgerTransactionSn()).isEqualTo(ledgerTransaction.getSn());
                                assertThat(entry.getFundsTransactionSn())
                                        .isEqualTo(ledgerTransaction.getFundsTransactionSn());
                                assertThat(entry.getPostingPlanSn()).isIn(postingPlanSns);
                            });
                });
        if (!fundsTransactions.isEmpty()) {
            assertThat(details)
                    .as("funds transaction details must belong to funds transactions for businessSn %s", businessSn)
                    .extracting(FundsTransactionDetail::getTransactionSn)
                    .containsOnlyElementsOf(fundsTransactions.stream().map(FundsTransaction::getSn).toList());
        }
    }

    protected void assertSingleFundsAndLedgerFactsForBusinessSn(String businessSn, int expectedDetails,
                                                                int expectedEntries) {
        assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, expectedDetails, expectedEntries / 2,
                expectedEntries);
    }

    protected void assertLedgerFactsFollowRouteSnapshot(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<LedgerPostingPlan> postingPlans = postingPlansOf(ledgerTransaction);
        List<LedgerEntry> entries = entriesOf(ledgerTransaction);

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot must explain ledger facts for businessSn %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(businessSn);
                    assertThat(postingPlans)
                            .as("posting plan route legs for businessSn %s", businessSn)
                            .extracting(LedgerPostingPlan::getRouteLegId)
                            .containsExactlyInAnyOrderElementsOf(routeSnapshot.getLegs().stream()
                                    .map(RouteLegSpec::getLegId)
                                    .toList());
                    postingPlans.forEach(plan -> {
                        RouteLegSpec routeLeg = routeLegById(routeSnapshot.getLegs(), plan.getRouteLegId());
                        List<LedgerEntry> planEntries = entries.stream()
                                .filter(entry -> plan.getSn().equals(entry.getPostingPlanSn()))
                                .toList();

                        assertThat(plan.getAmount()).isEqualTo(routeLeg.getAmount().getAmount());
                        assertThat(plan.getCurrency()).isEqualTo(routeLeg.getAmount().getCurrency());
                        assertThat(plan.getBalanceEffectType()).isEqualTo(routeLeg.getBalanceEffectType().name());
                        assertThat(plan.getPhaseCode()).isEqualTo(routeLeg.getPhaseCode().name());
                        assertThat(planEntries.stream()
                                .map(RouteNodeLedgerEntryKey::from)
                                .toList())
                                .as("ledger entries must follow route leg nodes and sides for businessSn %s",
                                        businessSn)
                                .containsExactlyInAnyOrder(
                                        RouteNodeLedgerEntryKey.from(routeLeg.getSourceNode(), EntrySide.DEBIT),
                                        RouteNodeLedgerEntryKey.from(routeLeg.getTargetNode(), EntrySide.CREDIT));
                    });
                });
    }

    private void assertReadableRouteSnapshot(String transactionSn, String businessSn) {
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transactionSn))
                .as("funds transaction route snapshot must be readable for businessSn %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertRouteSnapshotIdentity(routeSnapshot, businessSn));
    }

    private void assertRouteSnapshotIdentity(RouteSnapshotSpec routeSnapshot, String businessSn) {
        assertThat(routeSnapshot.getBusinessSn()).isEqualTo(businessSn);
        assertThat(routeSnapshot.getSnapshotId()).isNotBlank();
        assertThat(routeSnapshot.getSnapshotSchemaVersion()).isNotBlank();
    }

    protected List<LedgerTransaction> ledgerTransactions() {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .orderBy(ref.id.asc());
        return ledgerTransactionMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerPostingPlan> postingPlans() {
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .orderBy(ref.id.asc());
        return ledgerPostingPlanMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerEntry> entries() {
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .orderBy(ref.id.asc());
        return ledgerEntryMapper.selectListByQuery(wrapper);
    }

    protected FundsTransactionDTO fundsTransaction(String transactionSn) {
        return fundsTransactionQueryService.queryFundsTransaction(transactionSn)
                .orElseThrow(() -> new AssertionError("funds transaction not found: " + transactionSn));
    }

    protected List<FundsTransactionDetailDTO> fundsTransactionDetails(String transactionSn) {
        return fundsTransactionQueryService.queryFundsTransactionDetails(transactionSn);
    }

    protected List<FundsTransaction> fundsTransactionsByBusinessSn(String businessSn) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn))
                .orderBy(ref.id.asc());
        return fundsTransactionMapper.selectListByQuery(wrapper);
    }

    protected List<FundsTransactionDetail> fundsTransactionDetailsByBusinessSn(String businessSn) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn))
                .orderBy(ref.id.asc());
        return fundsTransactionDetailMapper.selectListByQuery(wrapper);
    }

    protected LedgerTransaction ledgerTransactionByBusinessSn(String businessSn) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn));
        LedgerTransaction result = ledgerTransactionMapper.selectOneByQuery(wrapper);
        assertThat(result).as("ledger transaction for businessSn %s", businessSn).isNotNull();
        return result;
    }

    protected List<LedgerTransaction> ledgerTransactionsForBusinessSn(String businessSn) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn))
                .orderBy(ref.id.asc());
        return ledgerTransactionMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerTransaction> ledgerTransactionsByFundsTransactionSn(String fundsTransactionSn) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.fundsTransactionSn.eq(fundsTransactionSn))
                .orderBy(ref.id.asc());
        return ledgerTransactionMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerPostingPlan> postingPlansOf(LedgerTransaction transaction) {
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.ledgerTransactionSn.eq(transaction.getSn()))
                .orderBy(ref.id.asc());
        return ledgerPostingPlanMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerPostingPlan> postingPlansByFundsTransactionSn(String fundsTransactionSn) {
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.fundsTransactionSn.eq(fundsTransactionSn))
                .orderBy(ref.id.asc());
        return ledgerPostingPlanMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerEntry> entriesOf(LedgerTransaction transaction) {
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.ledgerTransactionSn.eq(transaction.getSn()))
                .orderBy(ref.id.asc());
        return ledgerEntryMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerEntry> entriesByFundsTransactionSn(String fundsTransactionSn) {
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.fundsTransactionSn.eq(fundsTransactionSn))
                .orderBy(ref.id.asc());
        return ledgerEntryMapper.selectListByQuery(wrapper);
    }

    protected List<LedgerEntry> entriesByBusinessSn(String businessSn) {
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn))
                .orderBy(ref.id.asc());
        return ledgerEntryMapper.selectListByQuery(wrapper);
    }

    protected FundsFrozenOrder frozenOrderByBusinessSn(String businessSn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn));
        FundsFrozenOrder result = fundsFrozenOrderMapper.selectOneByQuery(wrapper);
        assertThat(result).as("frozen order for businessSn %s", businessSn).isNotNull();
        return result;
    }

    protected boolean frozenOrderExistsByBusinessSn(String businessSn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.businessSn.eq(businessSn));
        return fundsFrozenOrderMapper.selectCountByQuery(wrapper) > 0;
    }

    protected static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    protected static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT.name());
    }

    protected static FundsAccountId spendControlScope(String accountId) {
        return FundsAccountId.immutable(accountId, SPEND_CONTROL_SCOPE_ACCOUNT_TYPE);
    }

    protected static FundsAccountId cashMappingAccount() {
        return platformAccount(PlatformFundingAccountRole.CASH_MAPPING);
    }

    protected static FundsAccountId prepaymentAccount() {
        return platformAccount(PlatformFundingAccountRole.PREPAYMENT);
    }

    protected static FundsAccountId settlementAccount() {
        return platformAccount(PlatformFundingAccountRole.SETTLEMENT);
    }

    protected static FundsAccountId feeAccount() {
        return platformAccount(PlatformFundingAccountRole.FEE);
    }

    private static FundsAccountId platformAccount(PlatformFundingAccountRole role) {
        return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                FundsSubjectType.FUNDING_ACCOUNT.name());
    }

    protected Optional<LedgerDTO> findLedger(FundsAccountId accountId, LedgerSubjectCode subjectCode) {
        return findLedgers(accountId).stream()
                .filter(ledger -> ledger.getLedgerSubjectCode() == subjectCode)
                .findFirst();
    }

    private List<LedgerDTO> findLedgers(FundsAccountId accountId) {
        LedgerQuery query = new LedgerQuery()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setCurrency(CURRENCY)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name());
        return ledgerService.queryLedgers(query, DefaultPageQueryOptions.defaults(MAX_LEDGER_BUCKET_SIZE))
                .getRecords()
                .stream()
                .toList();
    }

    private Map<LedgerSubjectCode, LedgerBalanceBucket> balanceBuckets(FundsAccountId accountId) {
        Map<LedgerSubjectCode, LedgerBalanceBucket> result = new EnumMap<>(LedgerSubjectCode.class);
        findLedgers(accountId).forEach(ledger -> result.put(ledger.getLedgerSubjectCode(),
                LedgerBalanceBucket.builder()
                        .accountCode(ledger.getLedgerSubjectCode())
                        .balance(Money.immutable(ledger.getNormalBalance(), CURRENCY))
                        .periodType(ledger.getPeriodType())
                        .periodId(ledger.getPeriodId())
                        .activeTime(ACTIVE_TIME)
                        .build()));
        return result;
    }

    private void assertValidPostedTransaction(LedgerTransaction transaction) {
        assertThat(transaction.getFundsTransactionSn()).isNotBlank();
        assertThat(transaction.getEventType()).isNotBlank();
        assertThat(transaction.getInstructionType()).isNotBlank();
        assertThat(transaction.getTransactionType()).isNotBlank();
        assertThat(transaction.getBusinessScene()).isNotBlank();
        assertThat(transaction.getBusinessSn()).isNotBlank();
        assertThat(transaction.getStatus()).isEqualTo(LedgerTransactionStatus.POSTED);
        assertThat(transaction.getAmount()).isPositive();
        assertThat(transaction.getOriginalAmount()).isPositive();
        assertThat(transaction.getBalanced()).isTrue();
        assertThat(transaction.getDebitAmount()).isEqualTo(transaction.getCreditAmount());
        assertThat(transaction.getSha256()).isNotBlank();
        List<LedgerPostingPlan> postingPlans = postingPlansOf(transaction);
        List<LedgerEntry> entries = entriesOf(transaction);
        assertThat(postingPlans)
                .isNotEmpty()
                .allSatisfy(plan -> assertValidPostingPlan(transaction, plan, entries));
        assertThat(entries)
                .isNotEmpty()
                .allSatisfy(entry -> assertValidEntry(transaction, entry));
        assertThat(sumEntries(entries, EntrySide.DEBIT)).as("transaction debit entries")
                .isEqualTo(transaction.getDebitAmount());
        assertThat(sumEntries(entries, EntrySide.CREDIT)).as("transaction credit entries")
                .isEqualTo(transaction.getCreditAmount());
        assertThat(entries)
                .extracting(LedgerEntry::getPostingPlanSn)
                .containsOnlyElementsOf(postingPlans.stream().map(LedgerPostingPlan::getSn).toList());
    }

    private void assertValidPostingPlan(LedgerTransaction transaction,
                                        LedgerPostingPlan postingPlan,
                                        List<LedgerEntry> entries) {
        List<LedgerEntry> planEntries = entries.stream()
                .filter(entry -> postingPlan.getSn().equals(entry.getPostingPlanSn()))
                .toList();
        assertThat(postingPlan.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
        assertThat(postingPlan.getFundsTransactionSn()).isEqualTo(transaction.getFundsTransactionSn());
        assertThat(postingPlan.getRouteLegId()).isNotBlank();
        assertThat(postingPlan.getCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(postingPlan.getAmount()).isPositive();
        assertKnownEnumName(LedgerPostingIntentType.class, postingPlan.getIntent(), "posting plan intent");
        assertKnownEnumName(LedgerPostingScope.class, postingPlan.getPostingScope(), "posting plan scope");
        assertKnownEnumName(LedgerBalanceEffectType.class, postingPlan.getBalanceEffectType(),
                "posting plan balance effect");
        assertKnownEnumName(LedgerPhaseCode.class, postingPlan.getPhaseCode(), "posting plan phase");
        assertThat(postingPlan.getBalanced()).isTrue();
        assertThat(postingPlan.getDebitAmount()).isEqualTo(postingPlan.getCreditAmount());
        assertThat(postingPlan.getSha256()).isNotBlank();
        assertThat(planEntries).as("posting entries for plan %s", postingPlan.getSn()).isNotEmpty();
        assertThat(sumEntries(planEntries, EntrySide.DEBIT)).as("posting plan debit entries")
                .isEqualTo(postingPlan.getDebitAmount());
        assertThat(sumEntries(planEntries, EntrySide.CREDIT)).as("posting plan credit entries")
                .isEqualTo(postingPlan.getCreditAmount());
    }

    private void assertValidEntry(LedgerTransaction transaction, LedgerEntry entry) {
        assertThat(entry.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
        assertThat(entry.getPostingPlanSn()).isNotBlank();
        assertThat(entry.getFundsTransactionSn()).isEqualTo(transaction.getFundsTransactionSn());
        assertThat(entry.getLedgerId()).isNotNull();
        assertThat(entry.getSubjectId()).isNotBlank();
        assertKnownEnumName(FundsSubjectType.class, entry.getSubjectType(), "ledger entry subject type");
        assertThat(entry.getLedgerSubjectCode()).isNotNull();
        assertThat(entry.getLedgerSubjectCategory()).isNotNull();
        assertThat(entry.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
        assertThat(entry.getCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(entry.getAmount()).isNotNull();
        assertThat(entry.getAmount()).isPositive();
        assertThat(entry.getOriginalAmount()).isPositive();
        assertThat(entry.getOriginalCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(entry.getExchangeRate()).isPositive();
        assertThat(entry.getEntrySide()).isIn(EntrySide.DEBIT, EntrySide.CREDIT);
        assertKnownEnumName(LedgerBalanceConstraintType.class, entry.getBalanceConstraintType(),
                "ledger entry balance constraint");
        assertKnownEnumName(LedgerPostingIntentType.class, entry.getIntent(), "ledger entry intent");
        assertKnownEnumName(LedgerPostingScope.class, entry.getPostingScope(), "ledger entry scope");
        assertKnownEnumName(LedgerBalanceEffectType.class, entry.getBalanceEffectType(),
                "ledger entry balance effect");
        assertKnownEnumName(LedgerPhaseCode.class, entry.getPhaseCode(), "ledger entry phase");
        assertThat(entry.getSha256()).isNotBlank();
    }

    private static <E extends Enum<E>> void assertKnownEnumName(Class<E> enumType, String value, String description) {
        assertThat(value).as(description).isNotBlank();
        assertThatCode(() -> Enum.valueOf(enumType, value))
                .as(description)
                .doesNotThrowAnyException();
    }

    private static long sumEntries(List<LedgerEntry> entries, EntrySide side) {
        return entries.stream()
                .filter(entry -> side == entry.getEntrySide())
                .mapToLong(LedgerEntry::getAmount)
                .sum();
    }

    private static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    private RouteLegSpec routeLegById(List<RouteLegSpec> routeLegs, String routeLegId) {
        return routeLegs.stream()
                .filter(routeLeg -> routeLeg.getLegId().equals(routeLegId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("route leg not found: " + routeLegId));
    }

    private record RouteNodeLedgerEntryKey(String subjectId,
                                           String subjectType,
                                           LedgerSubjectCode ledgerSubjectCode,
                                           EntrySide entrySide) {

        private static RouteNodeLedgerEntryKey from(RouteNodeSpec node, EntrySide entrySide) {
            return new RouteNodeLedgerEntryKey(node.getSubjectRef().getSubjectId(),
                    node.getSubjectRef().getSubjectType().name(), node.getLedgerSubjectCode(), entrySide);
        }

        private static RouteNodeLedgerEntryKey from(LedgerEntry entry) {
            return new RouteNodeLedgerEntryKey(entry.getSubjectId(), entry.getSubjectType(),
                    entry.getLedgerSubjectCode(), entry.getEntrySide());
        }
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
            FundsBenefitContributionTransactionServiceImpl.class,
            DefaultFundsBalanceAdjustmentAuditApplicationService.class,
            DefaultFundsTransactionProjectionExplainApplicationService.class,
            FundsTransactionCommandServiceImpl.class,
            LedgerServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class,
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultLedgerQueryService.class,
            DefaultFundsInstructionLifecycleSaver.class,
            DefaultFundsFrozenOrderLifecycleSaver.class,
            DelegatingFundsInstructionLifecycleRecorder.class,
            DefaultFundsTransactionQueryService.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            AccountHierarchyBindingServiceImpl.class,
            AccountHierarchyServiceImpl.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendControlScopeServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {
    }
}

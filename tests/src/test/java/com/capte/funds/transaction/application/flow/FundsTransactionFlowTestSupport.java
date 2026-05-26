package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.entities.table.LedgerEntryNameRefs;
import com.capte.funds.ledger.dal.entities.table.LedgerPostingPlanNameRefs;
import com.capte.funds.ledger.dal.entities.table.LedgerTransactionNameRefs;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.capte.funds.transaction.application.FundsAuthorizationTransactionService;
import com.capte.funds.transaction.application.FundsBalanceControlService;
import com.capte.funds.transaction.application.FundsDirectTransactionService;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.ledger.DefaultLedgerPostingAssembler;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.capte.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.capte.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.request.CreateBudgetGroupRequest;
import com.capte.funds.wallet.model.request.CreateCreditAccountRequest;
import com.capte.funds.wallet.service.BudgetGroupService;
import com.capte.funds.wallet.service.CreditAccountService;
import com.capte.funds.wallet.services.impl.BudgetGroupServiceImpl;
import com.capte.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.capte.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.capte.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.capte.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.capte.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.CreditFundsAccountType;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
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

    private static final List<String> FLOW_TEST_TABLES = List.of(
            "t_ledger_entry",
            "t_ledger_posting_plan",
            "t_ledger_transaction",
            "t_funds_frozen_order",
            "t_funds_transaction_detail",
            "t_funds_transaction",
            "t_funding_account",
            "t_credit_account",
            "t_budget_group",
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
    protected BudgetGroupService budgetGroupService;

    @Autowired
    private LedgerTransactionMapper ledgerTransactionMapper;

    @Autowired
    private LedgerPostingPlanMapper ledgerPostingPlanMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private FundsFrozenOrderMapper fundsFrozenOrderMapper;

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

    protected void ensureBudgetGroup(FundsAccountId accountId) {
        assertThat(accountId.type()).isEqualTo(FundsSubjectType.BUDGET_GROUP.name());
        if (!findLedgers(accountId).isEmpty()) {
            return;
        }
        budgetGroupService.createBudgetGroup(new CreateBudgetGroupRequest()
                .setSn(accountId.id())
                .setTenantId(TENANT_ID)
                .setOwnerId("owner_" + accountId.id())
                .setOwnerType(FundsAccountOwnerType.USER)
                .setBudgetType(DefaultFundsAccountType.BUDGET_GROUP.name())
                .setCurrency(CURRENCY));
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
        assertThat(transactions).hasSize(expectedSize);
        transactions.forEach(this::assertValidPostedTransaction);
    }

    protected List<LedgerTransaction> ledgerTransactions() {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .orderBy(ref.id.asc());
        return ledgerTransactionMapper.selectListByQuery(wrapper);
    }

    protected FundsTransactionDTO fundsTransaction(String transactionSn) {
        return fundsTransactionQueryService.queryFundsTransaction(transactionSn)
                .orElseThrow(() -> new AssertionError("funds transaction not found: " + transactionSn));
    }

    protected List<FundsTransactionDetailDTO> fundsTransactionDetails(String transactionSn) {
        return fundsTransactionQueryService.queryFundsTransactionDetails(transactionSn);
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

    protected List<LedgerPostingPlan> postingPlansOf(LedgerTransaction transaction) {
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TENANT_ID))
                .and(ref.ledgerTransactionSn.eq(transaction.getSn()))
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

    protected static FundsAccountId budgetGroup(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.BUDGET_GROUP.name());
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

    private Optional<LedgerDTO> findLedger(FundsAccountId accountId, LedgerSubjectCode subjectCode) {
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
        assertThat(transaction.getBalanced()).isTrue();
        assertThat(transaction.getDebitAmount()).isEqualTo(transaction.getCreditAmount());
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
        assertThat(postingPlan.getCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(postingPlan.getBalanced()).isTrue();
        assertThat(postingPlan.getDebitAmount()).isEqualTo(postingPlan.getCreditAmount());
        assertThat(planEntries).as("posting entries for plan %s", postingPlan.getSn()).isNotEmpty();
        assertThat(sumEntries(planEntries, EntrySide.DEBIT)).as("posting plan debit entries")
                .isEqualTo(postingPlan.getDebitAmount());
        assertThat(sumEntries(planEntries, EntrySide.CREDIT)).as("posting plan credit entries")
                .isEqualTo(postingPlan.getCreditAmount());
    }

    private void assertValidEntry(LedgerTransaction transaction, LedgerEntry entry) {
        assertThat(entry.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
        assertThat(entry.getFundsTransactionSn()).isEqualTo(transaction.getFundsTransactionSn());
        assertThat(entry.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
        assertThat(entry.getCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(entry.getAmount()).isNotNull();
        assertThat(entry.getEntrySide()).isIn(EntrySide.DEBIT, EntrySide.CREDIT);
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
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            CreditAccountServiceImpl.class,
            BudgetGroupServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {
    }
}

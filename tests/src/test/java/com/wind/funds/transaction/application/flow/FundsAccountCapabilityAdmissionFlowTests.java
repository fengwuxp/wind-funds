package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.application.FundsClearingTransactionService;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * canonical 资金命令账户能力准入 H2 流程测试。
 */
class FundsAccountCapabilityAdmissionFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FundsClearingTransactionService clearingTransactionService;

    @Autowired
    private FundsSettlementTransactionService settlementTransactionService;

    @Test
    void testTopupShouldRequireReceiveCapability() {
        FundsAccountId accountId = fundingAccount("cap_topup");
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        setCapabilities(accountId, "PAY", "WITHDRAW");

        assertThatThrownBy(() -> topup(accountId, 100L, "CAPABILITY_TOPUP_REJECTED"))
                .hasMessageContaining("RECEIVE")
                .hasMessageContaining(accountId.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_TOPUP_REJECTED");
        String transactionSn = fundsTransactionsByBusinessSn("CAPABILITY_TOPUP_REJECTED").getFirst().getSn();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, transactionSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getLegs())
                        .as("routeful capability rejection keeps the resolved, unexecuted route")
                        .isNotEmpty());
        assertBucket(balance(accountId), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    @Test
    void testTransferShouldRequirePayerPayCapability() {
        FundsAccountId payer = fundingAccount("cap_transfer_payer");
        FundsAccountId payee = fundingAccount("cap_transfer_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "CAPABILITY_TRANSFER_PAYER_TOPUP");
        setCapabilities(payer, "RECEIVE", "WITHDRAW");

        assertThatThrownBy(() -> transfer(payer, payee, 50L, "CAPABILITY_TRANSFER_PAYER_REJECTED"))
                .hasMessageContaining("PAY")
                .hasMessageContaining(payer.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_TRANSFER_PAYER_REJECTED");
        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    @Test
    void testTransferShouldRequirePayeeReceiveCapability() {
        FundsAccountId payer = fundingAccount("cap_recv_payer");
        FundsAccountId payee = fundingAccount("cap_recv_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "CAPABILITY_TRANSFER_PAYEE_TOPUP");
        setCapabilities(payee, "PAY", "WITHDRAW");

        assertThatThrownBy(() -> transfer(payer, payee, 50L, "CAPABILITY_TRANSFER_PAYEE_REJECTED"))
                .hasMessageContaining("RECEIVE")
                .hasMessageContaining(payee.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_TRANSFER_PAYEE_REJECTED");
        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    @Test
    void testPayShouldRequireBothAccountCapabilities() {
        FundsAccountId payer = fundingAccount("cap_pay_payer");
        FundsAccountId payee = fundingAccount("cap_pay_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureFundingAccount(payee, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(payee, LedgerSubjectCode.CLEARING);
        topup(payer, 100L, "CAPABILITY_PAY_TOPUP");
        setCapabilities(payee, "PAY", "WITHDRAW");

        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.CLEARING,
                50L, "CAPABILITY_PAY_REJECTED"))
                .hasMessageContaining("RECEIVE")
                .hasMessageContaining(payee.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_PAY_REJECTED");
        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.CLEARING, 0L, CURRENCY);
    }

    @Test
    void testBusinessRefundShouldRequireBothAccountCapabilities() {
        FundsAccountId payer = fundingAccount("cap_refund_direct_payer");
        FundsAccountId target = fundingAccount("cap_refund_direct_target");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(target, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "CAPABILITY_DIRECT_REFUND_TOPUP");
        setCapabilities(payer, "RECEIVE", "WITHDRAW");

        assertThatThrownBy(() -> directTransactionService.refund(
                businessRefundRequest(payer, target, "CAPABILITY_DIRECT_REFUND_PAYER_REJECTED"),
                WindOperatorFactory.system()))
                .hasMessageContaining("PAY")
                .hasMessageContaining(payer.id());
        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_DIRECT_REFUND_PAYER_REJECTED");

        setCapabilities(payer, "PAY");
        setCapabilities(target, "PAY", "WITHDRAW");
        assertThatThrownBy(() -> directTransactionService.refund(
                businessRefundRequest(payer, target, "CAPABILITY_DIRECT_REFUND_TARGET_REJECTED"),
                WindOperatorFactory.system()))
                .hasMessageContaining("RECEIVE")
                .hasMessageContaining(target.id());
        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_DIRECT_REFUND_TARGET_REJECTED");
        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(target), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    @Test
    void testStandaloneFeeShouldRequirePayCapability() {
        FundsAccountId accountId = fundingAccount("cap_fee_payer");
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        topup(accountId, 100L, "CAPABILITY_FEE_TOPUP");
        setCapabilities(accountId, "RECEIVE", "WITHDRAW");

        assertThatThrownBy(() -> fee(accountId, 20L, "CAPABILITY_FEE_REJECTED"))
                .hasMessageContaining("PAY")
                .hasMessageContaining(accountId.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_FEE_REJECTED");
        assertBucket(balance(accountId), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
    }

    @Test
    void testWithdrawShouldRequireWithdrawCapability() {
        FundsAccountId accountId = fundingAccount("cap_withdraw");
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 100L, "CAPABILITY_WITHDRAW_TOPUP");
        String freezeOrderSn = freeze(accountId, 60L, "CAPABILITY_WITHDRAW_FREEZE");
        setCapabilities(accountId, "RECEIVE", "PAY");

        assertThatThrownBy(() -> withdraw(accountId, 60L, freezeOrderSn, "CAPABILITY_WITHDRAW_REJECTED"))
                .hasMessageContaining("WITHDRAW")
                .hasMessageContaining(accountId.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_WITHDRAW_REJECTED");
        assertBucket(balance(accountId), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(accountId), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
    }

    @Test
    void testDeclinedAuthorizationShouldStillRequirePayCapabilityForAllFundingSubjects() {
        FundsAccountId cardAccount = fundingAccount("cap_auth_card");
        FundsAccountId linkedFundingAccount = fundingAccount("cap_auth_funding");
        ensureFundingAccount(cardAccount);
        ensureFundingAccount(linkedFundingAccount);
        setCapabilities(cardAccount, "RECEIVE", "WITHDRAW");

        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(cardAccount)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(50L, CURRENCY)))
                        .setApproved(false)
                        .setDeclineReason("issuer declined")
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("CAPABILITY_AUTHORIZATION_ACCOUNT_REJECTED"),
                WindOperatorFactory.system()))
                .hasMessageContaining("PAY")
                .hasMessageContaining(cardAccount.id());
        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_AUTHORIZATION_ACCOUNT_REJECTED");

        setCapabilities(cardAccount, "PAY");
        setCapabilities(linkedFundingAccount, "RECEIVE", "WITHDRAW");

        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(cardAccount)
                        .setLinkedFundingAccountId(linkedFundingAccount)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(50L, CURRENCY)))
                        .setApproved(false)
                        .setDeclineReason("issuer declined")
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("CAPABILITY_AUTHORIZATION_REJECTED"),
                WindOperatorFactory.system()))
                .hasMessageContaining("PAY")
                .hasMessageContaining(linkedFundingAccount.id());

        assertFailedFundsTransactionWithoutLedgerFacts("CAPABILITY_AUTHORIZATION_REJECTED");
    }

    @Test
    void testAuthorizationSuccessorsAndBalanceControlsShouldIgnoreCapabilityDrift() {
        FundsAccountId accountId = fundingAccount("cap_auth_successors");
        FundsAccountId adjustmentAccount = fundingAccount("platform_adjustment");
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        ensureLedger(accountId, LedgerSubjectCode.AUTHORIZATION);
        ensureLedger(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT);
        topup(accountId, 300L, "CAPABILITY_SUCCESSOR_TOPUP");
        String reversedAuthorizationSn = authorize(accountId, 40L, true,
                "CAPABILITY_SUCCESSOR_REVERSE_AUTHORIZE");
        String completedAuthorizationSn = authorize(accountId, 60L, true,
                "CAPABILITY_SUCCESSOR_COMPLETE_AUTHORIZE");
        setCapabilities(accountId);

        String reversalSn = reverseAuthorization(accountId, 40L, reversedAuthorizationSn,
                "CAPABILITY_SUCCESSOR_REVERSAL");
        String completionSn = completeAuthorization(accountId, 60L, completedAuthorizationSn,
                "CAPABILITY_SUCCESSOR_COMPLETE");
        String refundSn = refundCompletedAuthorization(accountId, 20L, completedAuthorizationSn,
                "CAPABILITY_SUCCESSOR_REFUND");
        String forceCompletionSn = authorizationTransactionService.complete(
                new FundsAuthorizationTransactionCompleteRequest()
                        .setAccountId(accountId)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                        .setCompletionMode(FundsAuthorizationTransactionCompleteRequest.COMPLETION_MODE_FORCE)
                        .setForceCompletionPolicyCode("B4_FORCE_COMPLETION_OPS")
                        .setForceCompletionLimitAmount(60L)
                        .setForceCompletionReason("external completion")
                        .setExternalOriginalFactRef("EXTERNAL_CAPABILITY_FORCE")
                        .setForceCompletionVoucherRef("VOUCHER_CAPABILITY_FORCE")
                        .setBusinessScene("AUTHORIZATION_FORCE_COMPLETION")
                        .setBusinessSn("CAPABILITY_FORCE_COMPLETION"),
                WindOperatorFactory.system());
        String noAuthRefundSn = refundWithoutAuthorization(accountId, 20L,
                "CAPABILITY_NO_AUTH_REFUND");
        String freezeOrderSn = freeze(accountId, 10L, "CAPABILITY_SUCCESSOR_FREEZE");
        unfreeze(accountId, 10L, freezeOrderSn, "CAPABILITY_SUCCESSOR_UNFREEZE");
        adjustBalance(accountId, 5L, false, "CAPABILITY_SUCCESSOR_ADJUST");

        assertThat(reversalSn).isNotBlank();
        assertThat(completionSn).isNotBlank();
        assertThat(refundSn).isNotBlank();
        assertThat(forceCompletionSn).isNotBlank();
        assertThat(noAuthRefundSn).isNotBlank();
        assertBucket(balance(accountId), LedgerSubjectCode.AVAILABLE, 245L, CURRENCY);
        assertBucket(balance(accountId), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(accountId), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY);
    }

    @Test
    void testCompletedReplayShouldNotRecheckChangedCapabilities() {
        FundsAccountId accountId = fundingAccount("cap_replay");
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        FundsTransactionTopupRequest request = topupRequest(accountId, "CAPABILITY_TOPUP_REPLAY");
        String transactionSn = directTransactionService.topup(request, WindOperatorFactory.system());
        setCapabilities(accountId, "PAY", "WITHDRAW");

        String replayTransactionSn = directTransactionService.topup(request, WindOperatorFactory.system());

        assertThat(replayTransactionSn).isEqualTo(transactionSn);
        assertThat(fundsTransactionsByBusinessSn("CAPABILITY_TOPUP_REPLAY")).hasSize(1);
        assertBucket(balance(accountId), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
    }

    @Test
    void testOriginalTransactionRefundAndBalanceControlShouldIgnoreCurrentCapabilities() {
        FundsAccountId payer = fundingAccount("cap_refund_payer");
        FundsAccountId payee = fundingAccount("cap_refund_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payer, LedgerSubjectCode.FROZEN);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "CAPABILITY_REFUND_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.AVAILABLE,
                80L, "CAPABILITY_REFUND_PAY");
        setCapabilities(payer);
        setCapabilities(payee);

        String refundTransactionSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("CAPABILITY_ORIGINAL_REFUND"), WindOperatorFactory.system());
        String freezeOrderSn = freeze(payer, 10L, "CAPABILITY_CONTROL_FREEZE");

        assertThat(refundTransactionSn).isNotBlank();
        assertThat(freezeOrderSn).isNotBlank();
        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 10L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
    }

    @Test
    void testClearingAndSettlementShouldIgnoreCurrentCapabilities() {
        FundsAccountId payer = fundingAccount("cap_settlement_payer");
        FundsAccountId merchant = fundingAccount("cap_settlement_merchant");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureFundingAccount(merchant, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(merchant, LedgerSubjectCode.CLEARING);
        ensureLedger(merchant, LedgerSubjectCode.AVAILABLE);
        ensureLedger(merchant, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "CAPABILITY_SETTLEMENT_TOPUP");
        String payTransactionSn = pay(payer, merchant, LedgerSubjectCode.CLEARING,
                100L, "CAPABILITY_SETTLEMENT_PAY");
        setCapabilities(merchant);

        String clearingTransactionSn = clearingTransactionService.confirm(new FundsClearingConfirmRequest()
                .setAccountId(merchant)
                .setAmount(Money.immutable(60L, CURRENCY))
                .setClearingBatchSn("CAPABILITY_CLEARING_CONFIRM")
                .setSourceTransactionSns(List.of(payTransactionSn)), WindOperatorFactory.system());
        String settlementTransactionSn = settlementTransactionService.lock(new FundsSettlementLockRequest()
                .setAccountId(merchant)
                .setAmount(Money.immutable(40L, CURRENCY))
                .setSettlementOrderSn("CAPABILITY_SETTLEMENT_LOCK"), WindOperatorFactory.system());

        assertThat(clearingTransactionSn).isNotBlank();
        assertThat(settlementTransactionSn).isNotBlank();
        assertBucket(balance(merchant), LedgerSubjectCode.CLEARING, 40L, CURRENCY);
        assertBucket(balance(merchant), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(merchant), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
    }

    private FundsTransactionTopupRequest topupRequest(FundsAccountId accountId, String businessSn) {
        return new FundsTransactionTopupRequest()
                .setAccountId(accountId)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn(businessSn + "_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(100L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn(businessSn)
                .setDescription("capability replay topup");
    }

    private FundsTransactionRefundRequest businessRefundRequest(FundsAccountId payer,
                                                                 FundsAccountId target,
                                                                 String businessSn) {
        return new FundsTransactionRefundRequest()
                .setAccountId(target)
                .setPayerId(payer)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn(businessSn)
                .setDescription("business-confirmed refund");
    }

    private void setCapabilities(FundsAccountId accountId, String... capabilities) {
        String tableName = FundsSubjectType.CREDIT_ACCOUNT.name().equals(accountId.type())
                ? "t_credit_account" : "t_funding_account";
        String capabilityValues = capabilities.length == 0
                ? "[]" : "[\"" + String.join("\",\"", capabilities) + "\"]";
        String contextVariables = "{\"fundsAccountCapabilities\":" + capabilityValues + "}";
        int updated = jdbcTemplate.update("UPDATE " + tableName
                        + " SET context_variables = ? WHERE tenant_id = ? AND sn = ?",
                contextVariables, TENANT_ID, accountId.id());
        assertThat(updated).as("account capabilities updated for %s", accountId).isOne();
    }

}

package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.application.ExternalFundsEventApplicationService;
import com.wind.funds.transaction.application.external.impl.ExternalFundsEventApplicationServiceImpl;
import com.wind.funds.transaction.model.request.ConsumeExternalFundsEventRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.jackson.WindJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import tools.jackson.databind.JsonNode;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 全球账户使用 ACH 外部轨道的收付款业务流程测试。
 *
 * <p>测试只消费上层已经归一的外部资金事实和终态出款决定，复用标准 topup、freeze 和 withdraw 内核；
 * 不解释 ACH 协议、return code、NOC、reversal 或银行状态机。</p>
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        GlobalAccountAchBusinessFlowTests.Config.class
})
class GlobalAccountAchBusinessFlowTests extends FundsTransactionFlowTestSupport {

    private static final String GLOBAL_ACCOUNT_SN = "global_account_ach_001";

    private static final String EXTERNAL_SOURCE_CODE = "BANK_A:GLOBAL_ACCOUNT_ACH";

    private static final String CREDIT_FACT_SN = "ACH_CREDIT_FACT_001";

    private static final String CREDIT_BUSINESS_SN = "GLOBAL_ACH_CREDIT_CONFIRMED";

    private static final String CREDIT_REPLAY_BUSINESS_SN = "GLOBAL_ACH_CREDIT_REPLAY";

    private static final String FREEZE_BUSINESS_SN = "GLOBAL_ACH_PAYOUT_FREEZE";

    private static final String WITHDRAW_BUSINESS_SN = "GLOBAL_ACH_PAYOUT_SUCCEEDED";

    @Autowired
    private ExternalFundsEventApplicationService externalFundsEventApplicationService;

    @Autowired
    private FundingAccountService fundingAccountService;

    /**
     * 场景：全球账户先收到 ACH confirmed credit，再经原冻结流水完成终态出款。
     * 输入：accepted/confirmed/replay 入金事件、出款冻结、终态成功回单和 return 事件。
     * 输出：accepted 和 return 无资金副作用；confirmed 只入账一次；出款只消费一次原冻结金额。
     * 红线：资金底座不把外部受理当到账，不把 return 当普通退款，也不让外部银行账户成为账务主体。
     */
    @Test
    void testConfirmedCreditThenTerminalWithdrawalShouldReuseSharedFundsKernel() {
        FundsAccountId globalAccount = createGlobalAccount();
        BalanceSnapshot before = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeAccepted = ledgerFactSnapshot();

        assertThatThrownBy(() -> externalFundsEventApplicationService.consume(
                externalEvent("ACH_EVENT_ACCEPTED_001", "ACH_ACCEPTED_FACT_001", "ACH_CREDIT_ACCEPTED",
                        "GLOBAL_ACH_CREDIT_ACCEPTED", globalAccount), WindOperatorFactory.system()))
                .hasMessageContaining("外部入金事件未确认到账不得入账");
        assertNoFundsOrLedgerFactsForBusinessSn("GLOBAL_ACH_CREDIT_ACCEPTED");
        assertLedgerTransactionFactsUnchanged(beforeAccepted);
        assertOnlyBalanceDeltas(before, snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount())),
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String creditTransactionSn = externalFundsEventApplicationService.consume(
                externalEvent("ACH_EVENT_CONFIRMED_001", CREDIT_FACT_SN, "ACH_CREDIT_CONFIRMED",
                        CREDIT_BUSINESS_SN, globalAccount), WindOperatorFactory.system());
        BalanceSnapshot afterCredit = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterCredit,
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 90L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 90L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(CREDIT_BUSINESS_SN, 3, 2, 4);
        assertLedgerFactsFollowRouteSnapshot(CREDIT_BUSINESS_SN);

        String replayTransactionSn = externalFundsEventApplicationService.consume(
                externalEvent("ACH_EVENT_CONFIRMED_REPLAY_001", CREDIT_FACT_SN, "ACH_CREDIT_CONFIRMED",
                        CREDIT_REPLAY_BUSINESS_SN, globalAccount), WindOperatorFactory.system());
        assertThat(replayTransactionSn).isEqualTo(creditTransactionSn);
        assertNoFundsOrLedgerFactsForBusinessSn(CREDIT_REPLAY_BUSINESS_SN);
        assertOnlyBalanceDeltas(afterCredit,
                snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount())),
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(globalAccount, 60L, FREEZE_BUSINESS_SN);
        BalanceSnapshot afterFreeze = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterCredit, afterFreeze,
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertFundsAndLedgerFactsForBusinessSn(FREEZE_BUSINESS_SN, 0, 0, 1, 2);

        LedgerFactSnapshot beforePayoutAccepted = ledgerFactSnapshot();
        assertThatThrownBy(() -> externalFundsEventApplicationService.consume(
                externalEvent("ACH_PAYOUT_ACCEPTED_001", "ACH_DEBIT_FACT_001", "ACH_DEBIT_ACCEPTED",
                        "GLOBAL_ACH_PAYOUT_ACCEPTED", globalAccount), WindOperatorFactory.system()))
                .hasMessageContaining("外部资金事件类型暂不支持真实消费");
        assertNoFundsOrLedgerFactsForBusinessSn("GLOBAL_ACH_PAYOUT_ACCEPTED");
        assertLedgerTransactionFactsUnchanged(beforePayoutAccepted);
        assertOnlyBalanceDeltas(afterFreeze,
                snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount())),
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(globalAccount, 60L, freezeSn, WITHDRAW_BUSINESS_SN);
        BalanceSnapshot afterWithdraw = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(WITHDRAW_BUSINESS_SN, 3, 2, 4);
        assertLedgerFactsFollowRouteSnapshot(WITHDRAW_BUSINESS_SN);

        withdraw(globalAccount, 60L, freezeSn, WITHDRAW_BUSINESS_SN);
        assertOnlyBalanceDeltas(afterWithdraw,
                snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount())),
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn(WITHDRAW_BUSINESS_SN, 3, 2, 4);

        LedgerFactSnapshot beforeReturn = ledgerFactSnapshot();
        assertThatThrownBy(() -> externalFundsEventApplicationService.consume(
                externalEvent("ACH_RETURN_EVENT_001", "ACH_RETURN_FACT_001", "ACH_RETURNED",
                        "GLOBAL_ACH_RETURNED", globalAccount)
                        .setOriginalTransactionSn(fundsTransactionsByBusinessSn(WITHDRAW_BUSINESS_SN).getFirst().getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("外部资金事件类型暂不支持真实消费");
        assertNoFundsOrLedgerFactsForBusinessSn("GLOBAL_ACH_RETURNED");
        assertLedgerTransactionFactsUnchanged(beforeReturn);
        assertBucket(balance(globalAccount), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(globalAccount), LedgerSubjectCode.FROZEN, 0L, CURRENCY);

        JsonNode externalAccountRef = WindJson.parseObject(
                fundsTransaction(creditTransactionSn).getRouteSnapshot(), JsonNode.class)
                .path("externalAccountRef");
        assertThat(externalAccountRef.path("externalAccountType").asString()).isEqualTo("EXTERNAL_BANK");
        assertThat(externalAccountRef.path("channelCode").asString()).isEqualTo("ACH");
        assertThat(entriesByBusinessSn(CREDIT_BUSINESS_SN).stream().map(LedgerEntry::getSubjectId))
                .doesNotContain("external_funds_event_source");
    }

    private FundsAccountId createGlobalAccount() {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setSn(GLOBAL_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId("merchant_global_account_owner")
                .setOwnerType(FundsAccountOwnerType.MERCHANT)
                .setAccountType(FundingAccountType.GLOBAL_ACCOUNT.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .setState(FundsAccountState.ACTIVE)
                .setDescription("global account ACH business flow"));
        FundsAccountId result = fundingAccount(GLOBAL_ACCOUNT_SN);
        ensureLedger(result, LedgerSubjectCode.AVAILABLE);
        ensureLedger(result, LedgerSubjectCode.FROZEN);
        return result;
    }

    private ConsumeExternalFundsEventRequest externalEvent(String eventSn,
                                                           String fundsFactSn,
                                                           String eventType,
                                                           String businessSn,
                                                           FundsAccountId targetAccountId) {
        return new ConsumeExternalFundsEventRequest()
                .setTenantId(TENANT_ID)
                .setExternalEventSn(eventSn)
                .setExternalSourceCode(EXTERNAL_SOURCE_CODE)
                .setExternalFundsFactSn(fundsFactSn)
                .setExternalEventType(eventType)
                .setTargetAccountId(targetAccountId)
                .setAmount(90L)
                .setCurrency(CURRENCY)
                .setBusinessScene("GLOBAL_ACCOUNT_ACH")
                .setBusinessSn(businessSn)
                .setDescription("global account ACH external event");
    }

    @Configuration
    @Import(ExternalFundsEventApplicationServiceImpl.class)
    static class Config {
    }
}

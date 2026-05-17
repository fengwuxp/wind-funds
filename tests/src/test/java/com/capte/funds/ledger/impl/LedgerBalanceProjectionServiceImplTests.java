package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.integration.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerBalanceProjectionServiceImplTests extends LedgerBalanceProjectionServiceImplTestSupport {

    /**
     * 场景：资产类账目按借方正常余额方向更新余额投影。
     * 输入：资金账户 CASH 账本正常余额方向为 DEBIT，分录为 DEBIT 300。
     * 输出：账本余额更新请求和余额变更事件。
     * 预期：debit delta 增加 300，事件能说明变更前后余额。
     * 红线：余额投影不得把资产类账目误按贷方余额方向处理。
     */
    @Test
    void testProjectShouldUseDebitNormalBalanceSideForAssetSubjects() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(
                100L,
                LedgerSubjectCode.CASH,
                LedgerSubjectCategory.ASSET,
                EntrySide.DEBIT
        ));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.CASH, 100L),
                ledgerService
        );
        List<Object> events = captureSpringEvents();

        service.project(List.of(entry(
                accountId,
                100L,
                LedgerSubjectCode.CASH,
                LedgerSubjectCategory.ASSET,
                EntrySide.DEBIT,
                300L
        )));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getId()).isEqualTo(100L);
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getCreditAmountDelta()).isEqualTo(0L);
        assertThat(updateRequest.getMinimumNormalBalance()).isZero();
        assertThat(events).hasSize(1);
        LedgerBalanceChangedEvent event = (LedgerBalanceChangedEvent) events.getFirst();
        assertThat(event.getSubjectId()).isEqualTo(accountId.id());
        assertThat(event.getSubjectType()).isEqualTo(accountId.type());
        assertThat(event.getLedgerId()).isEqualTo(100L);
        assertThat(event.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.CASH);
        assertThat(event.getBeforeBalance()).isEqualTo(1_000L);
        assertThat(event.getBalance()).isEqualTo(1_300L);
        assertThat(event.getBalanceDelta()).isEqualTo(300L);
        assertThat(event.getLedgerTransactionSn()).isEqualTo("ledger_txn_001");
        assertThat(event.getBusinessScene()).isEqualTo("TEST");
        assertThat(event.getBusinessSn()).isEqualTo("biz_00000001");
    }

    /**
     * 场景：控制类账目按账本正常余额方向更新余额投影。
     * 输入：信用账户 AVAILABLE 账本正常余额方向为 CREDIT，分录为 CREDIT 300。
     * 输出：账本余额更新请求和余额变更事件。
     * 预期：credit delta 增加 300，事件能说明变更前后余额。
     * 红线：余额投影不得按科目类别猜测借贷方向，必须服从账本 normalBalanceSide。
     */
    @Test
    void testProjectShouldUseLedgerNormalBalanceSideForControlSubjects() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );
        List<Object> events = captureSpringEvents();

        service.project(List.of(entry(accountId, 99L, EntrySide.CREDIT, 300L)));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getId()).isEqualTo(99L);
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(0L);
        assertThat(updateRequest.getCreditAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getMinimumNormalBalance()).isZero();
        assertThat(events).hasSize(1);
        LedgerBalanceChangedEvent event = (LedgerBalanceChangedEvent) events.getFirst();
        assertThat(event.getSubjectId()).isEqualTo(accountId.id());
        assertThat(event.getSubjectType()).isEqualTo(accountId.type());
        assertThat(event.getLedgerId()).isEqualTo(99L);
        assertThat(event.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        assertThat(event.getBeforeBalance()).isEqualTo(1_000L);
        assertThat(event.getBalance()).isEqualTo(1_300L);
        assertThat(event.getBalanceDelta()).isEqualTo(300L);
    }
}

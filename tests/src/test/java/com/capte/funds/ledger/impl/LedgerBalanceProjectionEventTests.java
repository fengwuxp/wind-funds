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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerBalanceProjectionEventTests extends LedgerBalanceProjectionServiceImplTestSupport {

    /**
     * 场景：同一账本余额投影由多条分录共同形成。
     * 输入：CASH 借方 300、贷方 100，两条分录分别带业务上下文和分录追溯字段。
     * 输出：两条余额变更观察事件。
     * 预期：事件按分录顺序记录变更前余额、变更后余额、变更额和来源分录信息。
     * 红线：余额变更日志口子不得只给聚合余额，导致业务侧无法追溯到具体 LedgerEntry。
     */
    @Test
    void testProjectShouldPublishBalanceChangedEventForEachLedgerEntry() throws Exception {
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

        service.project(List.of(
                entry(accountId, 100L, LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.DEBIT, 300L)
                        .setContextVariables(Map.of("ledgerEntrySn", "LGE_DEBIT_001", "routeLegId", "LEG_DEBIT"))
                        .setSha256("entry-digest-debit"),
                entry(accountId, 100L, LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.CREDIT, 100L)
                        .setContextVariables(Map.of("ledgerEntrySn", "LGE_CREDIT_001", "routeLegId", "LEG_CREDIT"))
                        .setSha256("entry-digest-credit")
        ));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getCreditAmountDelta()).isEqualTo(100L);
        assertThat(events).hasSize(2);
        LedgerBalanceChangedEvent firstEvent = (LedgerBalanceChangedEvent) events.get(0);
        assertThat(firstEvent.getBeforeBalance()).isEqualTo(1_000L);
        assertThat(firstEvent.getBalance()).isEqualTo(1_300L);
        assertThat(firstEvent.getBalanceDelta()).isEqualTo(300L);
        assertThat(firstEvent.getLedgerEntrySn()).isEqualTo("LGE_DEBIT_001");
        assertThat(firstEvent.getLedgerEntryDigest()).isEqualTo("entry-digest-debit");
        assertThat(firstEvent.getContextVariables()).containsEntry("routeLegId", "LEG_DEBIT");
        LedgerBalanceChangedEvent secondEvent = (LedgerBalanceChangedEvent) events.get(1);
        assertThat(secondEvent.getBeforeBalance()).isEqualTo(1_300L);
        assertThat(secondEvent.getBalance()).isEqualTo(1_200L);
        assertThat(secondEvent.getBalanceDelta()).isEqualTo(-100L);
        assertThat(secondEvent.getLedgerEntrySn()).isEqualTo("LGE_CREDIT_001");
        assertThat(secondEvent.getLedgerEntryDigest()).isEqualTo("entry-digest-credit");
        assertThat(secondEvent.getContextVariables()).containsEntry("routeLegId", "LEG_CREDIT");
    }

    /**
     * 场景：业务余额变更观察事件发布失败。
     * 输入：余额投影成功，但 Spring 事件发布器抛出运行时异常。
     * 输出：余额更新请求仍然提交到 LedgerService。
     * 预期：project 调用不抛出事件异常，余额投影结果不被观察日志失败回滚。
     * 红线：余额变更日志不是账本事实源，日志失败不得让已校验的账本投影失败。
     */
    @Test
    void testProjectShouldNotRollbackBalanceProjectionWhenBalanceChangedEventFails() throws Exception {
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
        rejectSpringEvents();

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
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getCreditAmountDelta()).isZero();
    }
}

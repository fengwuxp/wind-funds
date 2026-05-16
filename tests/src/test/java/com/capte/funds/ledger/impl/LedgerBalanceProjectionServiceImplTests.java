package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.ImmutableFundsAccount;
import com.capte.funds.wallet.ImmutableFundsBalanceView;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerBalanceProjectionServiceImplTests {

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

    /**
     * 场景：信用账户在 profile 和分录均允许时可受控透支。
     * 输入：账本 allowNegative=true，分录约束为 ALLOW_NEGATIVE。
     * 输出：余额更新请求。
     * 预期：不设置 minimumNormalBalance，让底层更新允许负余额。
     * 红线：负 AVAILABLE 必须由 profile 与本次分录约束共同授权，不能静默产生。
     */
    @Test
    void testProjectShouldAllowNegativeWhenProfileAndEntryAllowIt() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );
        captureSpringEvents();

        service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 1_200L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.ALLOW_NEGATIVE)));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(1_200L);
        assertThat(updateRequest.getMinimumNormalBalance()).isNull();
    }

    /**
     * 场景：分录要求允许负余额，但账本 profile 不允许。
     * 输入：资金账户 AVAILABLE 账本 allowNegative=false，分录约束为 ALLOW_NEGATIVE。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：不生成余额更新请求，错误信息能定位 ledgerId 和账目。
     * 红线：分录级 ALLOW_NEGATIVE 不得突破账本 profile 的不可负余额配置。
     */
    @Test
    void testProjectShouldRejectAllowNegativeWhenLedgerProfileDisallowsNegative() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, false)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 1_200L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.ALLOW_NEGATIVE))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本 profile 不允许负余额")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：profile 允许负余额，但本次分录要求不得为负。
     * 输入：账本 allowNegative=true，分录约束为 MUST_NOT_BE_NEGATIVE。
     * 输出：余额更新请求。
     * 预期：仍设置 minimumNormalBalance=0，保留本次交易红线。
     * 红线：profile 放开负余额不代表每笔交易都可以透支。
     */
    @Test
    void testProjectShouldKeepMustNotBeNegativeConstraintWhenProfileAllowsNegative() throws Exception {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );
        captureSpringEvents();

        service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 300L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE)));

        assertThat(ledgerService.updateRequests).hasSize(1);
        UpdateLedgerBalanceRequest updateRequest = ledgerService.updateRequests.getFirst();
        assertThat(updateRequest.getDebitAmountDelta()).isEqualTo(300L);
        assertThat(updateRequest.getMinimumNormalBalance()).isZero();
    }

    /**
     * 场景：本次交易要求不得为负，但当前余额已经为负。
     * 输入：信用账户 AVAILABLE 当前余额 -100，分录约束为 MUST_NOT_BE_NEGATIVE。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：错误信息包含 beforeBalance，且不生成余额更新请求。
     * 红线：不得在余额已经突破红线时继续追加受限交易。
     */
    @Test
    void testProjectShouldRejectMustNotBeNegativeWhenCurrentBalanceAlreadyNegative() {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L, -100L),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 300L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额不允许为负")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining("beforeBalance = -100");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：本次投影会把余额打穿到负数。
     * 输入：信用账户 AVAILABLE 当前余额 100，本次借方扣减 300。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：错误信息包含 beforeBalance 和 afterBalance，且不生成余额更新请求。
     * 红线：MUST_NOT_BE_NEGATIVE 约束下不得产生负余额投影。
     */
    @Test
    void testProjectShouldRejectMustNotBeNegativeWhenProjectionWouldBreakBalanceFloor() {
        FundsAccountId accountId = FundsAccountId.immutable("credit_001", FundsSubjectType.CREDIT_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.CREDIT, true)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type()));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L, 100L),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 300L)
                .setBalanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额不足")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining("beforeBalance = 100")
                .hasMessageContaining("afterBalance = -200");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：一次余额投影请求混入多个资金账户分录。
     * 输入：funding_001 和 funding_002 的分录被放在同一个 project 调用。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：单次投影只处理同一资金账户，避免账户维度锁定和审计混乱。
     * 红线：不得把多个资金主体的余额变化混在一个账户投影事务中。
     */
    @Test
    void testProjectShouldRejectEntriesFromDifferentFundsAccounts() {
        FundsAccountId firstAccountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        FundsAccountId secondAccountId = FundsAccountId.immutable("funding_002", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.DEBIT));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(firstAccountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(
                entry(firstAccountId, 99L, EntrySide.DEBIT, 100L),
                entry(secondAccountId, 99L, EntrySide.CREDIT, 100L)
        )))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额投影只允许处理同一资金账户的分录");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录主体与账本主体不一致。
     * 输入：分录主体为 funding_001，但 ledgerId 指向 funding_002 的账本。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：不生成余额更新请求，错误信息说明主体不一致。
     * 红线：主体 A 的分录不得更新主体 B 的账本余额。
     */
    @Test
    void testProjectShouldRejectEntryWhenLedgerBelongsToAnotherSubjectBeforeUpdate() {
        FundsAccountId entryAccountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO anotherSubjectLedger = ledger(99L, EntrySide.DEBIT)
                .setSubjectId("funding_002")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(anotherSubjectLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(entryAccountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(entryAccountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体与账本主体不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录科目与账本科目不一致。
     * 输入：分录科目为 AVAILABLE，但 ledgerId 指向 CASH 账本。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：分录科目和账本科目必须一致，且不生成余额更新请求。
     * 红线：不得把一个余额桶的资金变化更新到另一个账目。
     */
    @Test
    void testProjectShouldRejectEntryWhenLedgerSubjectCodeDoesNotMatchBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO cashLedger = ledger(99L, LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.DEBIT);
        RecordingLedgerService ledgerService = new RecordingLedgerService(cashLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录科目与账本科目不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录币种与账本币种不一致。
     * 输入：分录币种为 USD，但 ledgerId 指向 EUR 账本。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：分录币种和账本币种必须一致，且不生成余额更新请求。
     * 红线：不得在余额投影阶段隐式完成换汇或跨币种记账。
     */
    @Test
    void testProjectShouldRejectEntryWhenLedgerCurrencyDoesNotMatchBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO eurLedger = ledger(99L, EntrySide.DEBIT)
                .setCurrency(CurrencyIsoCode.EUR);
        RecordingLedgerService ledgerService = new RecordingLedgerService(eurLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录币种与账本币种不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录 ledgerId 指向不存在的账本。
     * 输入：分录绑定 ledgerId=99，但 LedgerService 返回 null。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：错误信息能定位 ledgerId，且不生成余额更新请求。
     * 红线：写流程缺账本必须失败，不得自动建账或用空账本继续投影。
     */
    @Test
    void testProjectShouldRejectMissingLedgerBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(null);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本不存在")
                .hasMessageContaining("ledgerId = 99");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：资金账户余额视图缺少分录对应的余额桶。
     * 输入：分录科目为 FROZEN，但当前余额视图只初始化 AVAILABLE。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：错误信息能定位主体、账目和 ledgerId，且不生成余额更新请求。
     * 红线：写流程缺余额桶必须失败，不得把未初始化余额当 0 静默投影。
     */
    @Test
    void testProjectShouldRejectMissingBalanceBucketBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO frozenLedger = ledger(99L, LedgerSubjectCode.FROZEN, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT);
        RecordingLedgerService ledgerService = new RecordingLedgerService(frozenLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 100L),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(
                accountId,
                99L,
                LedgerSubjectCode.FROZEN,
                LedgerSubjectCategory.CONTROL,
                EntrySide.CREDIT,
                100L
        ))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金账户余额桶未初始化")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining(LedgerSubjectCode.FROZEN.name());
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    private static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId) {
        return newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 99L);
    }

    private static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId,
                                                                        LedgerSubjectCode ledgerSubjectCode,
                                                                        Long ledgerId) {
        return newFundsAccountQueryService(accountId, ledgerSubjectCode, ledgerId, 1_000L);
    }

    private static FundsAccountQueryService newFundsAccountQueryService(FundsAccountId accountId,
                                                                        LedgerSubjectCode ledgerSubjectCode,
                                                                        Long ledgerId,
                                                                        long balance) {
        return new FundsAccountQueryService() {

            @Override
            public @NonNull FundsAccount getAccount(@NonNull FundsAccountId ignored) {
                return ImmutableFundsAccount.builder()
                        .id(1L)
                        .tenantId(1L)
                        .accountId(accountId)
                        .owner(FundsAccountOwner.of("user_001", FundsAccountOwnerType.USER))
                        .status(FundsAccountStatus.ACTIVE)
                        .currency(CurrencyIsoCode.USD)
                        .accountLedgerIds(Map.of(ledgerSubjectCode, ledgerId))
                        .version(1)
                        .build();
            }

            @Override
            public @NonNull FundsAccountBalanceView getBalance(@NonNull FundsAccountId ignored) {
                LedgerBalanceBucket bucket = LedgerBalanceBucket.builder()
                        .accountCode(ledgerSubjectCode)
                        .balance(Money.immutable(balance, CurrencyIsoCode.USD))
                        .periodType(AccountBalancePeriodType.LIFETIME)
                        .periodId(AccountBalancePeriodType.LIFETIME.name())
                        .activeTime(LocalDateTime.of(2026, 5, 7, 12, 0))
                        .build();
                return ImmutableFundsBalanceView.builder()
                        .id(1L)
                        .tenantId(1L)
                        .accountId(accountId)
                        .currency(CurrencyIsoCode.USD)
                        .balanceBuckets(Map.of(ledgerSubjectCode, bucket))
                        .build();
            }

            @Override
            public boolean supports(@NonNull FundsAccountId ignored) {
                return true;
            }
        };
    }

    private static List<Object> captureSpringEvents() throws Exception {
        List<Object> events = new ArrayList<>();
        setSpringEventPublisher(events::add);
        return events;
    }

    private static void rejectSpringEvents() throws Exception {
        setSpringEventPublisher(event -> {
            throw new IllegalStateException("balance event sink failed");
        });
    }

    private static void setSpringEventPublisher(ApplicationEventPublisher publisher) throws Exception {
        Method method = SpringEventPublishUtils.class.getDeclaredMethod(
                "setApplicationEventPublisher",
                ApplicationEventPublisher.class
        );
        method.setAccessible(true);
        method.invoke(null, publisher);
    }

    private static FundsTransactionTestSupport.MutableLedgerEntrySpec entry(FundsAccountId accountId,
                                                                            Long ledgerId,
                                                                            EntrySide entrySide,
                                                                            long amount) {
        return entry(accountId, ledgerId, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, entrySide, amount);
    }

    private static FundsTransactionTestSupport.MutableLedgerEntrySpec entry(FundsAccountId accountId,
                                                                            Long ledgerId,
                                                                            LedgerSubjectCode ledgerSubjectCode,
                                                                            LedgerSubjectCategory ledgerSubjectCategory,
                                                                            EntrySide entrySide,
                                                                            long amount) {
        LocalDateTime transactionTime = LocalDateTime.of(2026, 5, 7, 12, 0);
        return FundsTransactionTestSupport.ledgerEntrySpec(
                accountId.id(),
                accountId.type(),
                ledgerSubjectCode,
                ledgerSubjectCategory,
                entrySide,
                "ledger_txn_001",
                "TEST",
                "biz_00000001",
                amount,
                CurrencyIsoCode.USD,
                transactionTime
        ).setLedgerId(ledgerId)
                .setSha256("");
    }

    private static LedgerDTO ledger(Long id, EntrySide normalBalanceSide) {
        return ledger(id, normalBalanceSide, false);
    }

    private static LedgerDTO ledger(Long id, EntrySide normalBalanceSide, boolean allowNegative) {
        return ledger(id, LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.CONTROL, normalBalanceSide, allowNegative);
    }

    private static LedgerDTO ledger(Long id,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    LedgerSubjectCategory ledgerSubjectCategory,
                                    EntrySide normalBalanceSide) {
        return ledger(id, ledgerSubjectCode, ledgerSubjectCategory, normalBalanceSide, false);
    }

    private static LedgerDTO ledger(Long id,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    LedgerSubjectCategory ledgerSubjectCategory,
                                    EntrySide normalBalanceSide,
                                    boolean allowNegative) {
        return new LedgerDTO()
                .setId(id)
                .setGmtCreate(LocalDateTime.of(2026, 5, 7, 12, 0))
                .setGmtModified(LocalDateTime.of(2026, 5, 7, 12, 0))
                .setSubjectId("funding_001")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(ledgerSubjectCategory)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setDebitAmount(0L)
                .setCreditAmount(1_000L)
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name());
    }

    private static class RecordingLedgerService implements LedgerService {

        private final LedgerDTO ledger;

        private final List<UpdateLedgerBalanceRequest> updateRequests = new ArrayList<>();

        private RecordingLedgerService(LedgerDTO ledger) {
            this.ledger = ledger;
        }

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException("createLedger");
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            updateRequests.add(request);
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException("deleteLedgerByIds");
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            return ledger;
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            throw new UnsupportedOperationException("getLedgerByIds");
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(
                @NonNull LedgerQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("queryLedgers");
        }
    }
}

package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerBalanceProjectionNegativeConstraintTests extends LedgerBalanceProjectionServiceImplTestSupport {

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
}

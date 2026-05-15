package com.capte.funds.ledger;

import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.support.LedgerNormalBalanceUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerNormalBalanceUtilsTests {

    /**
     * 场景：借方正常余额账户计算当前正常余额。
     * 输入：normalSide=DEBIT，debit=1200，credit=200。
     * 输出：normalBalance=1000。
     * 预期：借方正常余额按 debit-credit 计算。
     * 红线：不得把借方正常余额按贷方口径反向计算。
     */
    @Test
    void testComputeNormalBalanceShouldUseDebitMinusCreditForDebitNormalAccount() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                EntrySide.DEBIT,
                1_200L,
                200L
        );

        assertThat(normalBalance).isEqualTo(1_000L);
    }

    /**
     * 场景：贷方正常余额账户计算当前正常余额。
     * 输入：normalSide=CREDIT，debit=200，credit=1200。
     * 输出：normalBalance=1000。
     * 预期：贷方正常余额按 credit-debit 计算。
     * 红线：不得把贷方正常余额按借方口径反向计算。
     */
    @Test
    void testComputeNormalBalanceShouldUseCreditMinusDebitForCreditNormalAccount() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                EntrySide.CREDIT,
                200L,
                1_200L
        );

        assertThat(normalBalance).isEqualTo(1_000L);
    }

    /**
     * 场景：账本缺少正常余额方向。
     * 输入：normalSide=null，debit=100，credit=50。
     * 输出：normalBalance=null。
     * 预期：工具不猜测余额方向。
     * 红线：缺少正常余额方向时不得默认为借方或贷方，避免余额口径被静默污染。
     */
    @Test
    void testComputeNormalBalanceShouldReturnNullWhenNormalSideUndefined() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                null,
                100L,
                50L
        );

        assertThat(normalBalance).isNull();
    }

    /**
     * 场景：额度类科目使用借方正常余额口径。
     * 输入：normalSide=DEBIT，debit=1000，credit=0。
     * 输出：normalBalance=1000。
     * 预期：LIMIT 等额度控制科目可按借方余额表示可用额度。
     * 红线：额度控制余额不得被贷方口径误算为负数或 0。
     */
    @Test
    void testComputeNormalBalanceShouldUseDebitNormalForLimitSubject() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                EntrySide.DEBIT,
                1_000L,
                0L
        );

        assertThat(normalBalance).isEqualTo(1_000L);
    }
}

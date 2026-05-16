package com.capte.funds.wallet;

import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.capte.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ControlAccountLedgerRulesTests {

    private final DefaultLedgerProfileServiceImpl service = new DefaultLedgerProfileServiceImpl();

    /**
     * 场景：信用账户只表达额度总量、可用额度和授权占用。
     * 输入：CREDIT_BASIC profile。
     * 输出：LIMIT、AVAILABLE、AUTHORIZATION 三个控制账目。
     * 预期：全部账目为 CONTROL，不含现金、冻结、结算、费用或调整挂账等真实资金账目。
     * 红线：信用账户不是真实资金池，不得通过资金类余额桶表达已消费或待结算。
     */
    @Test
    void testCreditAccountProfileShouldOnlyExposeControlBuckets() {
        assertControlProfile(
                LedgerProfileCode.CREDIT_BASIC,
                Set.of(
                        LedgerSubjectCode.LIMIT,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.AUTHORIZATION
                )
        );
    }

    /**
     * 场景：预算组只表达预算总量、可用预算和预算授权占用。
     * 输入：BUDGET_BASIC profile。
     * 输出：LIMIT、AVAILABLE、AUTHORIZATION 三个控制账目。
     * 预期：全部账目为 CONTROL，不含真实资金、冻结、结算或报表派生账目。
     * 红线：预算已使用金额应由交易生命周期、交易视图和报表计算，不得新增账务 CONSUMED 桶。
     */
    @Test
    void testBudgetGroupProfileShouldOnlyExposeControlBuckets() {
        assertControlProfile(
                LedgerProfileCode.BUDGET_BASIC,
                Set.of(
                        LedgerSubjectCode.LIMIT,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.AUTHORIZATION
                )
        );
    }

    /**
     * 场景：第一阶段不新增已消费账务桶。
     * 输入：LedgerSubjectCode 枚举全集。
     * 输出：枚举名集合。
     * 预期：不存在 CONSUMED。
     * 红线：已消费金额不是余额桶事实，不能和 LIMIT、AVAILABLE、AUTHORIZATION 同层建模。
     */
    @Test
    void testLedgerSubjectCodeShouldNotContainConsumedBucket() {
        Set<String> subjectCodes = Arrays.stream(LedgerSubjectCode.values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(subjectCodes).doesNotContain("CONSUMED");
    }

    private void assertControlProfile(LedgerProfileCode profileCode, Set<LedgerSubjectCode> expectedSubjects) {
        assertThat(service.getProfile(profileCode).getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .containsExactlyInAnyOrderElementsOf(expectedSubjects);
        assertThat(service.getProfile(profileCode).getItems())
                .allSatisfy(item -> assertThat(item.getLedgerSubjectCategory())
                        .isEqualTo(LedgerSubjectCategory.CONTROL));
        assertThat(service.getProfile(profileCode).getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .doesNotContain(
                        LedgerSubjectCode.CASH,
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.CLEARING,
                        LedgerSubjectCode.SETTLEMENT,
                        LedgerSubjectCode.FEE,
                        LedgerSubjectCode.ADJUSTMENT,
                        LedgerSubjectCode.IN_TRANSIT,
                        LedgerSubjectCode.RISK_RESERVE,
                        LedgerSubjectCode.DEFERRED_REVENUE,
                        LedgerSubjectCode.SUSPENSE
                );
    }
}

package com.wind.funds.wallet.services.impl;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.spec.ledger.LedgerProfileItemSpec;
import com.wind.funds.spec.ledger.LedgerProfileSpec;
import com.wind.funds.spec.ledger.NegativeAvailablePolicySpec;
import com.wind.funds.wallet.service.LedgerProfileService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LedgerProfile 核心契约测试。
 */
class LedgerProfileContractTests {

    private final LedgerProfileService ledgerProfileService = new DefaultLedgerProfileServiceImpl();

    /**
     * 场景：钱包侧静态 profile 被账本初始化、DSL 和后续多级账户能力共同消费。
     * 输入：FUNDING_BASIC profile。
     * 输出：返回对象可作为 core LedgerProfileSpec 消费，并保留 profile code/version/subject/items。
     * 红线：profile DTO 不能游离于 core DSL 契约之外，否则交易和账本初始化会各说各话。
     */
    @Test
    void testProfileDtoShouldBeConsumableAsCoreLedgerProfileSpec() {
        LedgerProfileSpec profile = ledgerProfileService.getProfile(LedgerProfileCode.FUNDING_BASIC);

        assertThat(profile.getProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC);
        assertThat(profile.getProfileName()).isEqualTo("普通资金账户");
        assertThat(profile.getProfileVersion()).isEqualTo(1);
        assertThat(profile.getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(profile.getItems())
                .extracting(LedgerProfileItemSpec::getLedgerSubjectCode)
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION);
    }

    /**
     * 场景：profile 允许 AVAILABLE 负余额，用于处理受控异常余额、风控和后续治理。
     * 输入：FUNDING_BASIC profile 的 AVAILABLE / FROZEN item。
     * 输出：允许负余额的 AVAILABLE 必须携带治理策略；不允许负余额的 FROZEN 不携带该策略。
     * 红线：allowNegative=true 不能只是布尔开关，必须能追溯来源事实、审批/风控、限额、账龄和治理路径。
     */
    @Test
    void testAllowNegativeProfileItemShouldExposeGovernancePolicy() {
        LedgerProfileSpec profile = ledgerProfileService.getProfile(LedgerProfileCode.FUNDING_BASIC);
        LedgerProfileItemSpec available = requiredItem(profile, LedgerSubjectCode.AVAILABLE);
        LedgerProfileItemSpec frozen = requiredItem(profile, LedgerSubjectCode.FROZEN);

        NegativeAvailablePolicySpec policy = available.getNegativeAvailablePolicy();

        assertThat(available.getAllowNegative()).isTrue();
        assertThat(policy).isNotNull();
        assertThat(policy.getPolicyCode()).isEqualTo("FUNDING_AVAILABLE_CONTROLLED_NEGATIVE");
        assertThat(policy.getPolicyVersion()).isEqualTo(1);
        assertThat(policy.getRequireSourceFact()).isTrue();
        assertThat(policy.getRequireApprovalOrRiskRule()).isTrue();
        assertThat(policy.getRequireSingleLimit()).isTrue();
        assertThat(policy.getRequireCumulativeLimit()).isTrue();
        assertThat(policy.getRequireAgingTracking()).isTrue();
        assertThat(policy.getRecheckFutureTransaction()).isTrue();
        assertThat(policy.getGovernancePath()).contains("风控", "对账");
        assertThat(frozen.getAllowNegative()).isFalse();
        assertThat(frozen.getNegativeAvailablePolicy()).isNull();
    }

    private LedgerProfileItemSpec requiredItem(LedgerProfileSpec profile, LedgerSubjectCode subjectCode) {
        return profile.getItems().stream()
                .filter(item -> item.getLedgerSubjectCode() == subjectCode)
                .findFirst()
                .orElseThrow();
    }
}

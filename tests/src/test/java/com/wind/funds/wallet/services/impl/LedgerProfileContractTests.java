package com.wind.funds.wallet.services.impl;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.ledger.spec.LedgerProfileItemSpec;
import com.wind.funds.ledger.spec.LedgerProfileSpec;
import com.wind.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.wind.funds.wallet.service.LedgerProfileService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

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
     * 场景：profile 只声明账目是否具备受控负余额能力，运行时治理由余额控制请求、路由和账本 posting 共同完成。
     * 输入：FUNDING_BASIC profile 的 AVAILABLE / FROZEN item。
     * 输出：允许负余额的 AVAILABLE 只暴露静态能力闸门；profile 契约和 DTO 不暴露运行时治理策略对象。
     * 红线：来源事实、审批/风控、限额、账龄和治理路径不能沉淀在静态账目画像中，避免和交易路由准入形成双重事实源。
     */
    @Test
    void testAllowNegativeProfileItemShouldOnlyExposeStaticCapabilityGuard() {
        LedgerProfileSpec profile = ledgerProfileService.getProfile(LedgerProfileCode.FUNDING_BASIC);
        LedgerProfileItemSpec available = requiredItem(profile, LedgerSubjectCode.AVAILABLE);
        LedgerProfileItemSpec frozen = requiredItem(profile, LedgerSubjectCode.FROZEN);

        assertThat(available.getAllowNegative()).isTrue();
        assertThat(frozen.getAllowNegative()).isFalse();
        assertThat(publicMethodNames(LedgerProfileItemSpec.class))
                .doesNotContain("getNegativeAvailablePolicy");
        assertThat(publicMethodNames(LedgerProfileItemDTO.class))
                .doesNotContain("getNegativeAvailablePolicy", "setNegativeAvailablePolicy");
    }

    private LedgerProfileItemSpec requiredItem(LedgerProfileSpec profile, LedgerSubjectCode subjectCode) {
        return profile.getItems().stream()
                .filter(item -> item.getLedgerSubjectCode() == subjectCode)
                .findFirst()
                .orElseThrow();
    }

    private static Iterable<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .map(Method::getName)
                .toList();
    }
}

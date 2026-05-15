package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.wallet.service.LedgerProfileService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.SettlementPolicySpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSubjectLedgerInitializerTests {

    /**
     * 场景：按账本 profile 初始化主体必需账本。
     * 输入：信用账户主体、CREDIT_BASIC profile，LIMIT/AVAILABLE 为必需账本，FROZEN 为非必需账本。
     * 输出：只创建必需账本，并返回必需科目到账本 ID 的映射。
     * 预期：创建请求继承主体、币种、profile、周期和结算策略信息。
     * 红线：初始化不得为非必需科目建账，也不得改变主体类型或账本 profile 语义。
     */
    @Test
    void testInitializeRequiredLedgersShouldCreateOnlyRequiredLedgers() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.CREDIT_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setItems(List.of(
                        item(LedgerSubjectCode.LIMIT, EntrySide.DEBIT, false, true),
                        item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true),
                        item(LedgerSubjectCode.FROZEN, EntrySide.CREDIT, false, false)
                ));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                FundsAccountServiceTestSupport.ledgerServiceWithCreateRecorder(requests)
        );

        Map<LedgerSubjectCode, Long> result = initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)
        );

        assertThat(result).containsEntry(LedgerSubjectCode.LIMIT, 101L)
                .containsEntry(LedgerSubjectCode.AVAILABLE, 102L)
                .doesNotContainKey(LedgerSubjectCode.FROZEN);
        assertThat(requests)
                .extracting(CreateLedgerRequest::getLedgerSubjectCode)
                .containsExactly(LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE);

        CreateLedgerRequest first = requests.getFirst();
        assertThat(first.getTenantId()).isEqualTo(1L);
        assertThat(first.getSubjectId()).isEqualTo("credit_001");
        assertThat(first.getSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT.name());
        assertThat(first.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(first.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC.name());
        assertThat(first.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(first.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(first.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        assertThat(first.getSettlementPolicy()).isEqualTo(SettlementPolicySpec.RT.getRaw());
        assertThat(first.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
    }

    /**
     * 场景：账本 profile 与请求主体类型不一致。
     * 输入：FUNDING_BASIC profile 绑定 FUNDING_ACCOUNT，请求却声明 CREDIT_ACCOUNT。
     * 输出：初始化失败且不创建任何账本。
     * 预期：错误信息带出 profileCode 和 subjectType，便于定位配置问题。
     * 红线：不得用不匹配的 profile 给主体建账，避免账本主体边界被污染。
     */
    @Test
    void testInitializeRequiredLedgersShouldRejectProfileSubjectTypeMismatch() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.FUNDING_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setItems(List.of(item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true)));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                FundsAccountServiceTestSupport.ledgerServiceWithCreateRecorder(requests)
        );

        assertThatThrownBy(() -> initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LedgerProfile 主体类型不匹配")
                .hasMessageContaining("profileCode = FUNDING_BASIC")
                .hasMessageContaining("subjectType = CREDIT_ACCOUNT");
        assertThat(requests).isEmpty();
    }

    private static LedgerProfileItemDTO item(LedgerSubjectCode code,
                                             EntrySide normalBalanceSide,
                                             boolean allowNegative,
                                             boolean required) {
        return new LedgerProfileItemDTO()
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(LedgerSubjectCategory.CONTROL)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setRequired(required)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setSettlementPolicy(SettlementPolicySpec.RT.getRaw())
                .setCutOffTime(LocalTime.MIDNIGHT);
    }

    private static LedgerProfileService ledgerProfileService(LedgerProfileDTO profile) {
        return new LedgerProfileService() {
            @Override
            public LedgerProfileDTO getProfile(LedgerProfileCode profileCode) {
                assertThat(profileCode).isEqualTo(profile.getCode());
                return profile;
            }

            @Override
            public LedgerProfileItemDTO getRequiredItem(LedgerProfileCode profileCode,
                                                        LedgerSubjectCode subjectCode) {
                throw new UnsupportedOperationException("getRequiredItem");
            }
        };
    }
}

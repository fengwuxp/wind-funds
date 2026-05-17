package com.capte.funds.wallet.services.impl;

import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSubjectLedgerInitializerValidationTests extends DefaultSubjectLedgerInitializerTestSupport {

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
                ledgerService(requests, List.of())
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

    /**
     * 场景：唯一账本桶已存在，但既有账本配置与当前 profile 不一致。
     * 输入：CREDIT_BASIC 期望 AVAILABLE 允许受控负余额，既有 AVAILABLE 账本却记录 allowNegative=false。
     * 输出：初始化失败且不创建新账本。
     * 预期：错误信息带出 subjectId 和 ledgerSubjectCode，便于排查 profile 漂移。
     * 红线：幂等复用不得掩盖账本 profile、余额方向或负余额策略漂移。
     */
    @Test
    void testInitializeRequiredLedgersShouldRejectExistingLedgerProfileMismatch() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.CREDIT_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setItems(List.of(item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true)));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                ledgerService(requests, List.of(ledger(902L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, false)))
        );

        assertThatThrownBy(() -> initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已存在账本与初始化 profile 不一致")
                .hasMessageContaining("subjectId = credit_001")
                .hasMessageContaining("ledgerSubjectCode = AVAILABLE");
        assertThat(requests).isEmpty();
    }
}

package com.capte.funds.wallet.services.impl;

import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.SettlementPolicySpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSubjectLedgerInitializerTests extends DefaultSubjectLedgerInitializerTestSupport {

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
                ledgerService(requests, List.of())
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
     * 场景：主体账本初始化请求被重复提交。
     * 输入：信用账户主体已经存在 LIMIT/AVAILABLE required ledgers，再次按 CREDIT_BASIC 初始化。
     * 输出：返回既有账本 ID，不发起新的 createLedger。
     * 预期：初始化遵循 ledger 唯一桶幂等语义，重复调用不制造重复账本。
     * 红线：开户重试不得把同一主体、币种、周期和账目重复建账，也不得依赖交易路径补账。
     */
    @Test
    void testInitializeRequiredLedgersShouldReuseExistingRequiredLedgers() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.CREDIT_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setItems(List.of(
                        item(LedgerSubjectCode.LIMIT, EntrySide.DEBIT, false, true),
                        item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true)
                ));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                ledgerService(requests, List.of(
                        ledger(901L, LedgerSubjectCode.LIMIT, EntrySide.DEBIT, false),
                        ledger(902L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true)
                ))
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

        assertThat(result).containsEntry(LedgerSubjectCode.LIMIT, 901L)
                .containsEntry(LedgerSubjectCode.AVAILABLE, 902L)
                .hasSize(2);
        assertThat(requests).isEmpty();
    }

}

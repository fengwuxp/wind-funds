package com.wind.funds.transaction.support;

import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.support.WindOperatorTestFixture;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 外部资金事实摘要兼容契约测试。
 */
class ExternalFundsFactDigestSupportTests {

    private static final FundsAccountId ACCOUNT_ID = FundsAccountId.immutable(
            "FUNDING-001", FundsSubjectType.FUNDING_ACCOUNT.name());

    private static final Money AMOUNT = Money.immutable(100L, CurrencyIsoCode.USD);

    private static final BigDecimal EXCHANGE_RATE = BigDecimal.ONE;

    @Test
    void testMatchesShouldPreserveNullSemanticsAndAcceptOnlyKnownDigestVersions() {
        String canonical = ExternalFundsFactDigestSupport.canonicalDigest(
                ACCOUNT_ID, AMOUNT, AMOUNT, EXCHANGE_RATE);
        String legacy = FundsStableHashSupport.sha256Json(legacyFacts());

        assertThat(ExternalFundsFactDigestSupport.matches(null, instruction(null))).isTrue();
        assertThat(ExternalFundsFactDigestSupport.matches(legacy, instruction(canonical))).isTrue();
        assertThat(ExternalFundsFactDigestSupport.matches(
                FundsStableHashSupport.sha256("unknown"), instruction(canonical))).isFalse();
        assertThat(ExternalFundsFactDigestSupport.matches(legacy,
                instruction(FundsStableHashSupport.sha256("unknown")))).isFalse();
    }

    private FundsInstructionSpec instruction(@Nullable String digest) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TOPUP)
                .amount(AMOUNT)
                .originalAmount(AMOUNT)
                .exchangeRate(EXCHANGE_RATE)
                .accountId(ACCOUNT_ID)
                .externalSourceCode(digest == null ? null : "BANK")
                .externalFundsFactSn(digest == null ? null : "BANK-001")
                .externalFundsEffectType(digest == null ? null : FundsEffectType.DIRECT)
                .externalFundsFactDigest(digest)
                .businessScene("TOPUP")
                .businessSn("TOPUP-001")
                .eventTime(LocalDateTime.of(2026, 8, 4, 12, 0))
                .operator(WindOperatorTestFixture.system())
                .contextVariables(Map.of())
                .build();
    }

    private Map<String, Object> legacyFacts() {
        Map<String, Object> values = new TreeMap<>();
        values.put("targetAccountId", ACCOUNT_ID.id());
        values.put("targetAccountType", ACCOUNT_ID.type());
        values.put("amount", AMOUNT.getAmount());
        values.put("currency", AMOUNT.getCurrency().name());
        values.put("originalAmount", AMOUNT.getAmount());
        values.put("originalCurrency", AMOUNT.getCurrency().name());
        values.put("exchangeRate", EXCHANGE_RATE);
        return values;
    }
}

package com.wind.funds.transaction.support;

import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 外部资金事实摘要兼容支撑。
 *
 * @author wuxp
 * @since 2026-08-04
 */
public final class ExternalFundsFactDigestSupport {

    private static final String DIGEST_DOMAIN = "transaction.external-funds-fact";

    private ExternalFundsFactDigestSupport() {
    }

    public static @NonNull String canonicalDigest(@NonNull FundsAccountId targetAccountId,
                                                  @NonNull Money amount,
                                                  @NonNull Money originalAmount,
                                                  @NonNull BigDecimal exchangeRate) {
        return FundsStableHashSupport.sha256CanonicalJson(
                DIGEST_DOMAIN, digestFacts(targetAccountId, amount, originalAmount, exchangeRate));
    }

    public static boolean matches(@Nullable String storedDigest, @NonNull FundsInstructionSpec instruction) {
        String requestDigest = instruction.getExternalFundsFactDigest();
        if (Objects.equals(storedDigest, requestDigest)) {
            return true;
        }
        FundsAccountId targetAccountId = instruction.getAccountId();
        if (storedDigest == null || requestDigest == null || targetAccountId == null) {
            return false;
        }
        Map<String, Object> facts = digestFacts(
                targetAccountId,
                instruction.getAmount(),
                instruction.getOriginalAmount(),
                instruction.getExchangeRate());
        String canonicalDigest = FundsStableHashSupport.sha256CanonicalJson(DIGEST_DOMAIN, facts);
        String legacyDigest = FundsStableHashSupport.sha256Json(facts);
        return isKnownDigest(storedDigest, canonicalDigest, legacyDigest)
                && isKnownDigest(requestDigest, canonicalDigest, legacyDigest);
    }

    private static boolean isKnownDigest(String digest, String canonicalDigest, String legacyDigest) {
        return canonicalDigest.equals(digest) || legacyDigest.equals(digest);
    }

    private static Map<String, Object> digestFacts(FundsAccountId targetAccountId,
                                                   Money amount,
                                                   Money originalAmount,
                                                   BigDecimal exchangeRate) {
        Map<String, Object> values = new TreeMap<>();
        values.put("targetAccountId", targetAccountId.id());
        values.put("targetAccountType", targetAccountId.type());
        values.put("amount", amount.getAmount());
        values.put("currency", amount.getCurrency().name());
        values.put("originalAmount", originalAmount.getAmount());
        values.put("originalCurrency", originalAmount.getCurrency().name());
        values.put("exchangeRate", exchangeRate);
        return values;
    }
}

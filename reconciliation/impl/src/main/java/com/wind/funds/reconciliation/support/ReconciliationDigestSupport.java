package com.wind.funds.reconciliation.support;

import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.enums.ReconciliationSourceType;
import com.wind.funds.transaction.support.FundsStableHashSupport;

import java.util.Map;
import java.util.TreeMap;

/**
 * 对账批次和来源快照稳定摘要支持。
 */
public final class ReconciliationDigestSupport {

    private ReconciliationDigestSupport() {
    }

    public static String sourceDigest(ReconciliationSourceRole sourceRole,
                                      ReconciliationSourceType sourceType,
                                      Map<String, String> sourceContentDigests) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("sourceRole", sourceRole);
        facts.put("sourceType", sourceType);
        facts.put("sourceContentDigests", new TreeMap<>(sourceContentDigests));
        return FundsStableHashSupport.sha256Json(facts);
    }
}

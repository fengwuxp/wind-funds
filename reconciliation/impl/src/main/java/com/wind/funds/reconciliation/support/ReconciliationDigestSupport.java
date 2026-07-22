package com.wind.funds.reconciliation.support;

import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.enums.ReconciliationSourceType;
import com.wind.funds.transaction.support.FundsStableHashSupport;

import java.util.List;
import java.util.TreeMap;

/**
 * 对账批次和来源快照稳定摘要支持。
 */
public final class ReconciliationDigestSupport {

    private ReconciliationDigestSupport() {
    }

    public static String sourceItemDigest(ReconciliationSourceRole sourceRole,
                                          ReconciliationSourceType sourceType,
                                          String sourceItemRef) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("sourceRole", sourceRole);
        facts.put("sourceType", sourceType);
        facts.put("sourceItemRef", sourceItemRef);
        return FundsStableHashSupport.sha256Json(facts);
    }

    public static String sourceDigest(ReconciliationSourceRole sourceRole,
                                      ReconciliationSourceType sourceType,
                                      List<String> itemDigests) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("sourceRole", sourceRole);
        facts.put("sourceType", sourceType);
        facts.put("itemDigests", itemDigests.stream().sorted().toList());
        return FundsStableHashSupport.sha256Json(facts);
    }
}

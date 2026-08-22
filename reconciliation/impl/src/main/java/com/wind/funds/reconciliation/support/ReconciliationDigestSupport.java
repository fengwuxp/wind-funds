package com.wind.funds.reconciliation.support;

import com.wind.funds.transaction.support.FundsStableHashSupport;

import java.util.Map;
import java.util.TreeMap;

/**
 * 对账批次和来源快照稳定摘要支持。
 */
public final class ReconciliationDigestSupport {

    private ReconciliationDigestSupport() {
    }

    public static String digest(Map<String, ?> facts) {
        return FundsStableHashSupport.sha256Json(new TreeMap<>(facts));
    }
}

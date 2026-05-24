package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易投影面向使用者的只读解释摘要。
 *
 * <p>职责：把交易事实、路径快照和账务引用整理成用户账单、商户账单和运营时间线可消费的解释载荷。</p>
 *
 * <p>边界：该对象只表达派生视图输入，不参与交易、账本或余额事实写入。</p>
 */
@Builder
public record FundsTransactionProjectionExplanation(@NonNull String businessScene,
                                                    @NonNull String businessSn,
                                                    @NonNull String fundsTransactionSn,
                                                    @NonNull String routeSnapshotId,
                                                    @NonNull String routeCode,
                                                    @Nullable String ledgerTransactionSn,
                                                    @NonNull String factStatus,
                                                    @NonNull String displayStatus,
                                                    @NonNull String operationStatus,
                                                    @NonNull String statusMeaning,
                                                    @NonNull String amountSource,
                                                    @NonNull String failureReason,
                                                    @NonNull String unavailableReason,
                                                    @NonNull String nextAction,
                                                    @NonNull List<String> evidenceRefs,
                                                    @NonNull String externalRuleVerificationStatus) {

    public FundsTransactionProjectionExplanation {
        evidenceRefs = List.copyOf(evidenceRefs);
    }

    /**
     * 生成投影重放和运营时间线可复用的解释载荷。
     *
     * @return 不可变解释载荷
     */
    public @NonNull Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("businessScene", businessScene);
        payload.put("businessSn", businessSn);
        payload.put("fundsTransactionSn", fundsTransactionSn);
        payload.put("routeSnapshotId", routeSnapshotId);
        payload.put("routeCode", routeCode);
        if (StringUtils.hasText(ledgerTransactionSn)) {
            payload.put("ledgerTransactionSn", ledgerTransactionSn);
        }
        payload.put("factStatus", factStatus);
        payload.put("displayStatus", displayStatus);
        payload.put("operationStatus", operationStatus);
        payload.put("statusMeaning", statusMeaning);
        payload.put("amountSource", amountSource);
        payload.put("failureReason", failureReason);
        payload.put("unavailableReason", unavailableReason);
        payload.put("nextAction", nextAction);
        payload.put("evidenceRefs", evidenceRefs);
        payload.put("externalRuleVerificationStatus", externalRuleVerificationStatus);
        return Map.copyOf(payload);
    }
}

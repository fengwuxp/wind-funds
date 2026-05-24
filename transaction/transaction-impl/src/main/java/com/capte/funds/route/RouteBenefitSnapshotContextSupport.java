package com.capte.funds.route;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Route 层权益快照摘要支持。
 */
final class RouteBenefitSnapshotContextSupport {

    private static final String MISSING_BENEFIT_SNAPSHOT_MESSAGE =
            "RouteSnapshot 回放缺少原权益快照摘要";

    private RouteBenefitSnapshotContextSupport() {
    }

    static Map<String, Object> mergeBenefitSnapshotSummary(FundsInstructionSpec instruction) {
        Map<String, Object> result = new LinkedHashMap<>(instruction.getContextVariables());
        FundsBenefitSnapshotSpec benefitSnapshot = instruction.getBenefitSnapshot();
        if (benefitSnapshot == null) {
            return Map.copyOf(result);
        }
        result.put(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID,
                benefitSnapshot.getBenefitSnapshotId());
        result.put(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST,
                benefitSnapshot.getStableDigest());
        return Map.copyOf(result);
    }

    static void assertOriginalBenefitSnapshotPresent(FundsInstructionSpec instruction,
                                                     RouteSnapshotSpec routeSnapshot) {
        FundsBenefitSnapshotSpec benefitSnapshot = instruction.getBenefitSnapshot();
        if (benefitSnapshot == null) {
            return;
        }
        Map<String, Object> originalContext = routeSnapshot.getContextVariables();
        Object originalSnapshotId = originalContext.get(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID);
        Object originalSnapshotDigest = originalContext.get(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST);
        AssertUtils.isTrue(originalSnapshotId instanceof String snapshotId && StringUtils.hasText(snapshotId)
                        && originalSnapshotDigest instanceof String stableDigest && StringUtils.hasText(stableDigest),
                MISSING_BENEFIT_SNAPSHOT_MESSAGE + "，referenceSn = {}，benefitSnapshotId = {}",
                instruction.getReference().getReferenceSn(), benefitSnapshot.getBenefitSnapshotId());
    }

    static Map<String, Object> mergeOriginalBenefitSnapshotSummary(Map<String, Object> contextVariables,
                                                                   RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> result = new LinkedHashMap<>(contextVariables);
        copyIfPresent(result, routeSnapshot.getContextVariables(), FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID);
        copyIfPresent(result, routeSnapshot.getContextVariables(),
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST);
        return Map.copyOf(result);
    }

    private static void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }
}

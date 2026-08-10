package com.wind.funds.route;

import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
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

    static void assertOriginalBenefitSnapshotPresent(FundsInstructionSpec instruction,
                                                     RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> originalContext = routeSnapshot.getContextVariables();
        Object originalSnapshotId = originalContext.get(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID);
        Object originalSnapshotDigest = originalContext.get(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST);
        boolean hasOriginalSnapshotId = originalSnapshotId instanceof String text && StringUtils.hasText(text);
        boolean hasOriginalSnapshotDigest = originalSnapshotDigest instanceof String text && StringUtils.hasText(text);
        if (!hasOriginalSnapshotId && !hasOriginalSnapshotDigest) {
            return;
        }
        AssertUtils.isTrue(hasOriginalSnapshotId && hasOriginalSnapshotDigest,
                MISSING_BENEFIT_SNAPSHOT_MESSAGE + "，referenceSn = {}",
                instruction.getReference().getReferenceSn());
    }

    static Map<String, Object> originalBenefitSnapshotSummary(RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> result = new LinkedHashMap<>();
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

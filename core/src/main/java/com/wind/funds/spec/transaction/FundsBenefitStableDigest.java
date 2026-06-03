package com.wind.funds.spec.transaction;

import com.wind.funds.route.ref.SubjectRef;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.StringJoiner;

/**
 * 权益快照稳定摘要生成器。
 */
final class FundsBenefitStableDigest {

    private static final String NULL_VALUE = "<null>";

    private FundsBenefitStableDigest() {
    }

    static String compute(FundsBenefitSnapshotSpec snapshot) {
        String payload = payload(snapshot);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", ex);
        }
    }

    private static String payload(FundsBenefitSnapshotSpec snapshot) {
        StringJoiner joiner = new StringJoiner("|");
        append(joiner, "benefitSnapshotId", snapshot.getBenefitSnapshotId());
        append(joiner, "benefitSchemaVersion", snapshot.getBenefitSchemaVersion());
        append(joiner, "benefitGroupSn", snapshot.getBenefitGroupSn());
        append(joiner, "orderSn", snapshot.getOrderSn());
        append(joiner, "pricingSnapshotSn", snapshot.getPricingSnapshotSn());
        appendMoney(joiner, "orderAmount", snapshot.getOrderAmount());
        appendMoney(joiner, "userPayAmount", snapshot.getUserPayAmount());
        appendMoney(joiner, "merchantReceivableAmount", snapshot.getMerchantReceivableAmount());
        appendRefundPolicy(joiner, "refundPolicy", snapshot.getRefundPolicy());

        List<FundsBenefitComponentSpec> components = new ArrayList<>(snapshot.getComponents());
        components.sort(Comparator
                .comparingInt(FundsBenefitComponentSpec::getSequence)
                .thenComparing(FundsBenefitComponentSpec::getComponentSn));
        append(joiner, "components.count", components.size());
        for (FundsBenefitComponentSpec component : components) {
            appendComponent(joiner, component);
        }
        return joiner.toString();
    }

    private static void appendComponent(StringJoiner joiner, FundsBenefitComponentSpec component) {
        String prefix = "component." + component.getSequence() + "." + component.getComponentSn();
        append(joiner, prefix + ".componentSn", component.getComponentSn());
        append(joiner, prefix + ".sequence", component.getSequence());
        append(joiner, prefix + ".benefitType", component.getBenefitType().name());
        append(joiner, prefix + ".componentType", component.getComponentType().name());
        append(joiner, prefix + ".closureRole", component.getClosureRole().name());
        appendMoney(joiner, prefix + ".amount", component.getAmount());
        append(joiner, prefix + ".ledgerEffect", component.getLedgerEffect().name());
        append(joiner, prefix + ".fundingNature", component.getFundingNature().name());
        appendSubjectRef(joiner, prefix + ".bearerSubjectRef", component.getBearerSubjectRef());
        appendSubjectRef(joiner, prefix + ".beneficiarySubjectRef", component.getBeneficiarySubjectRef());
        appendSubjectRef(joiner, prefix + ".fundingSubjectRef", component.getFundingSubjectRef());
        append(joiner, prefix + ".fundingAccountRole", component.getFundingAccountRole());
        appendReference(joiner, prefix + ".benefitReference", component.getBenefitReference());
        appendRefundPolicy(joiner, prefix + ".refundPolicy", component.getRefundPolicy());
    }

    private static void appendReference(StringJoiner joiner,
                                        String prefix,
                                        FundsBenefitReferenceSpec reference) {
        append(joiner, prefix + ".campaignId", reference.getCampaignId());
        append(joiner, prefix + ".couponId", reference.getCouponId());
        append(joiner, prefix + ".voucherId", reference.getVoucherId());
        append(joiner, prefix + ".benefitInstanceId", reference.getBenefitInstanceId());
        append(joiner, prefix + ".holdId", reference.getHoldId());
        append(joiner, prefix + ".writeOffId", reference.getWriteOffId());
        append(joiner, prefix + ".releaseId", reference.getReleaseId());
        append(joiner, prefix + ".ruleVersion", reference.getRuleVersion());
        append(joiner, prefix + ".externalDecisionId", reference.getExternalDecisionId());
    }

    private static void appendRefundPolicy(StringJoiner joiner,
                                           String prefix,
                                           @Nullable FundsBenefitRefundPolicySpec policy) {
        if (policy == null) {
            append(joiner, prefix, null);
            return;
        }
        append(joiner, prefix + ".partialRefundStrategy", policy.getPartialRefundStrategy().name());
        List<String> dispositions = policy.getDispositions().stream()
                .map(Enum::name)
                .sorted()
                .toList();
        append(joiner, prefix + ".dispositions", String.join(",", dispositions));
        appendMoney(joiner, prefix + ".refundableAmount", policy.getRefundableAmount());
        appendMoney(joiner, prefix + ".nonRefundableAmount", policy.getNonRefundableAmount());
        append(joiner, prefix + ".refundRuleVersion", policy.getRefundRuleVersion());
        append(joiner, prefix + ".refundPolicyCode", policy.getRefundPolicyCode());
        append(joiner, prefix + ".refundDecisionId", policy.getRefundDecisionId());
        append(joiner, prefix + ".decisionSource", policy.getDecisionSource());
        appendLocalDateTime(joiner, prefix + ".decisionTime", policy.getDecisionTime());
    }

    private static void appendSubjectRef(StringJoiner joiner,
                                         String prefix,
                                         @Nullable SubjectRef subjectRef) {
        if (subjectRef == null) {
            append(joiner, prefix, null);
            return;
        }
        append(joiner, prefix + ".tenantId", subjectRef.getTenantId());
        append(joiner, prefix + ".subjectType", subjectRef.getSubjectType().name());
        append(joiner, prefix + ".subjectId", subjectRef.getSubjectId());
        append(joiner, prefix + ".currency", subjectRef.getCurrency());
        append(joiner, prefix + ".ledgerProfileCode", subjectRef.getLedgerProfileCode());
    }

    private static void appendMoney(StringJoiner joiner, String prefix, @Nullable Money money) {
        if (money == null) {
            append(joiner, prefix, null);
            return;
        }
        append(joiner, prefix + ".currency", money.getCurrency().name());
        append(joiner, prefix + ".amount", money.getAmount());
    }

    private static void appendLocalDateTime(StringJoiner joiner,
                                            String key,
                                            @Nullable LocalDateTime value) {
        append(joiner, key, value == null ? null : value.toString());
    }

    private static void append(StringJoiner joiner, String key, @Nullable Object value) {
        joiner.add(escape(key) + "=" + escape(value == null ? NULL_VALUE : String.valueOf(value)));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("=", "\\=");
    }
}

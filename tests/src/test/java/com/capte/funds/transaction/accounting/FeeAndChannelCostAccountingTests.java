package com.capte.funds.transaction.accounting;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FeeAndChannelCostAccountingTests {

    private static final Path PLATFORM_FEE_DSL_CASE =
            Path.of("core/src/test/resources/dsl/transaction-layer/direct-wallet-payment-with-fee.json");

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> PLATFORM_FEE_ACCOUNTING_MARKERS = List.of(
            "PlatformFundingAccountRole.FEE",
            "LedgerSubjectCode.FEE",
            "RouteParticipantRole.FEE_RECEIVER",
            "FundsRouteCodes.FEE_STANDARD",
            "LedgerPostingScope.FEE",
            "LedgerPostingIntentType.FEE"
    );

    /**
     * 场景：商户订单收款同时收取平台手续费。
     * 输入：`direct-wallet-payment-with-fee` DSL 用例。
     * 输出：本金 route leg、平台手续费 route leg、posting plan 和费率版本上下文。
     * 预期：商户实收本金、平台手续费和费率版本分别可解释。
     * 红线：平台手续费不得混入用户本金、商户 CLEARING 或普通结算 posting scope。
     */
    @Test
    void testPlatformFeeDslShouldSeparatePrincipalFeeAndRuleVersion() throws IOException {
        JSONObject dslCase = JSON.parseObject(Files.readString(projectRoot().resolve(PLATFORM_FEE_DSL_CASE)));

        JSONObject contextVariables = dslCase.getJSONObject("instruction").getJSONObject("contextVariables");
        assertThat(contextVariables.getString("feeRuleCode")).isEqualTo("MERCHANT_STANDARD_001");
        assertThat(contextVariables.getString("feeRuleVersion")).isEqualTo("2026-05");

        JSONObject payLeg = findByKey(dslCase.getJSONObject("expectedRoute").getJSONArray("legs"), "legId", "PAY");
        JSONObject feeLeg = findByKey(dslCase.getJSONObject("expectedRoute").getJSONArray("legs"), "legId", "FEE");
        assertThat(minorValue(payLeg)).isEqualTo(10_000);
        assertThat(minorValue(feeLeg)).isEqualTo(150);
        assertThat(payLeg.getString("phaseCode")).isEqualTo(LedgerPhaseCode.SETTLEMENT.name());
        assertThat(feeLeg.getString("phaseCode")).isEqualTo(LedgerPhaseCode.FEE.name());
        assertThat(payLeg.getJSONObject("targetNode").getString("ledgerSubjectCode"))
                .isEqualTo(LedgerSubjectCode.CLEARING.name());
        assertThat(feeLeg.getJSONObject("targetNode").getString("ledgerSubjectCode"))
                .isEqualTo(LedgerSubjectCode.FEE.name());

        JSONObject feeParticipant = findByKey(dslCase.getJSONObject("expectedRoute")
                .getJSONArray("participants"), "participantRole", RouteParticipantRole.FEE_RECEIVER.name());
        assertThat(feeParticipant.getJSONObject("subjectRef").getString("subjectId"))
                .isEqualTo(dslCase.getJSONObject("expectedRoute")
                        .getJSONObject("platformAccounts")
                        .getJSONObject("feeFundingAccount")
                        .getString("subjectId"));

        JSONObject payPlan = findByKey(dslCase.getJSONObject("expectedPosting").getJSONArray("postingPlans"),
                "routeLegId", "PAY");
        JSONObject feePlan = findByKey(dslCase.getJSONObject("expectedPosting").getJSONArray("postingPlans"),
                "routeLegId", "FEE");
        assertThat(payPlan.getString("intent")).isEqualTo(LedgerPostingIntentType.TRANSFER.name());
        assertThat(payPlan.getString("postingScope")).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        assertThat(feePlan.getString("intent")).isEqualTo(LedgerPostingIntentType.FEE.name());
        assertThat(feePlan.getString("postingScope")).isEqualTo(LedgerPostingScope.FEE.name());
        assertThat(ledgerSubjectCodes(payPlan)).doesNotContain(LedgerSubjectCode.FEE.name());
        assertThat(ledgerSubjectCodes(feePlan)).doesNotContain(LedgerSubjectCode.CLEARING.name());
    }

    /**
     * 场景：通道文件包含网络手续费、通道成本或卡组织成本。
     * 输入：生产源码中未来可能出现的 `DefaultFeeType.NETWORK_FEE` 处理路径。
     * 输出：把通道成本直接接入平台手续费账户、平台费用 route 或平台费用 posting 的违规列表。
     * 预期：通道成本必须通过独立成本来源和差异处理口径解释。
     * 红线：不得把通道成本静默归集为平台手续费收入。
     */
    @Test
    void testChannelCostShouldNotBeRoutedAsPlatformFeeRevenue() throws IOException {
        assertThat(DefaultFeeType.NETWORK_FEE).isNotEqualTo(DefaultFeeType.FEE);
        assertThat(DefaultFeeType.NETWORK_FEE.getDesc()).contains("网络");
        assertThat(DefaultFeeType.FEE.getDesc()).doesNotContain("网络");

        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findNetworkFeePlatformFeeAccountingReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("channel or network cost must not be silently accounted as platform fee revenue")
                .isEmpty();
    }

    private static List<String> findNetworkFeePlatformFeeAccountingReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isDefaultFeeTypeDeclaration(sourceFile)) {
                continue;
            }
            String source = Files.readString(sourceFile);
            if (source.contains("DefaultFeeType.NETWORK_FEE") && containsAny(source, PLATFORM_FEE_ACCOUNTING_MARKERS)) {
                violations.add(sourceFile + ": NETWORK_FEE must not use platform fee accounting markers");
            }
        }
        return violations;
    }

    private static JSONObject findByKey(JSONArray values, String key, String expectedValue) {
        return values.stream()
                .map(JSONObject.class::cast)
                .filter(value -> Objects.equals(value.getString(key), expectedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(key + " not found: " + expectedValue));
    }

    private static int minorValue(JSONObject leg) {
        return leg.getJSONObject("amount").getIntValue("minorValue");
    }

    private static List<String> ledgerSubjectCodes(JSONObject postingPlan) {
        return postingPlan.getJSONArray("entries")
                .stream()
                .map(JSONObject.class::cast)
                .map(entry -> entry.getString("ledgerSubjectCode"))
                .toList();
    }

    private static List<Path> listJavaSources(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isDefaultFeeTypeDeclaration(Path sourceFile) {
        return sourceFile.endsWith(Path.of("transaction/enums/DefaultFeeType.java"));
    }

    private static boolean containsAny(String source, List<String> markers) {
        return markers.stream().anyMatch(source::contains);
    }

    private static Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(PLATFORM_FEE_DSL_CASE))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("project root not found");
    }
}

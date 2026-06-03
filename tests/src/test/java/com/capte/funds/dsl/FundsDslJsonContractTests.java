package com.capte.funds.dsl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.funds.util.FundsDslJsonContractVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DSL JSON 样例契约测试。
 */
class FundsDslJsonContractTests {

    /**
     * 场景：DSL 文档样例作为交易、路由、账务和投影测试的入口。
     * 预期：样例可被机器校验，金额表达、枚举字段和基础结构不漂移。
     * 红线：JSON 样例不能只停留在文档层，不能出现无法映射到 core 契约的字段。
     */
    @Test
    void testTransactionLayerJsonSamplesShouldRemainMachineVerifiable() throws IOException {
        List<Path> samples = jsonSamples(transactionLayerDslDir());
        samples.addAll(jsonSamples(benefitContractCaseDir()));

        assertThat(samples).isNotEmpty();
        for (Path sample : samples) {
            JSONObject document = JSON.parseObject(Files.readString(sample));

            FundsDslJsonContractVerifier.verifyTransactionLayerCase(document);
            assertThat(document.getString("caseId")).as(sample.getFileName().toString()).isNotBlank();
            assertThat(document).as(sample.getFileName().toString()).containsKey("instruction");
        }
    }

    /**
     * 场景：样例作者误把金额写成主单位 value。
     * 预期：JSON 契约校验显式失败。
     * 红线：测试样例不能把小数主单位绕过金额最小单位边界。
     */
    @Test
    void testJsonContractVerifierShouldRejectMajorUnitMoneyShape() {
        Map<String, Object> document = Map.of(
                "caseId", "DSL-INVALID-MONEY-001",
                "instruction", Map.of(
                        "instructionType", "DIRECT_TRANSACTION",
                        "eventType", "PAY",
                        "transactionType", "PAY",
                        "amount", Map.of("currency", "USD", "value", "1.23")));

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.amount")
                .hasMessageContaining("money.amount is required");
    }

    /**
     * 场景：样例作者把生命周期事件误写入交易类型。
     * 预期：JSON 契约校验显式失败。
     * 红线：transactionType 不承载 AUTHORIZE、SETTLE、REVERSAL 等生命周期事件。
     */
    @Test
    void testJsonContractVerifierShouldRejectEventAsTransactionType() {
        Map<String, Object> document = Map.of(
                "caseId", "DSL-INVALID-TRANSACTION-TYPE-001",
                "instruction", Map.of(
                        "instructionType", "AUTHORIZATION_TRANSACTION",
                        "eventType", "AUTHORIZE",
                        "transactionType", "AUTHORIZE",
                        "amount", Map.of("currency", "USD", "amount", 100)));

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.transactionType")
                .hasMessageContaining("DefaultFundsTransactionType");
    }

    /**
     * 场景：样例作者把权益快照核心金额藏入 contextVariables。
     * 预期：JSON 契约校验显式失败。
     * 红线：权益金额闭合、规则版本和退款处置必须是一等字段。
     */
    @Test
    void testJsonContractVerifierShouldRejectBenefitCoreFieldsInContextVariables() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-CONTEXT-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_invalid_context_001",
                      "benefitGroupSn": "bg_invalid_context_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 8000 },
                      "components": [{
                        "componentSn": "bc_invalid_context_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "closureRole": "ORDER_DISCOUNT_CLOSURE",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "bearerSubjectRef": { "subjectType": "MERCHANT", "subjectId": "merchant_001" },
                        "benefitReference": { "couponId": "coupon_001", "ruleVersion": "rule_v1" },
                        "contextVariables": { "ruleVersion": "rule_v1" }
                      }],
                      "contextVariables": {}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables")
                .hasMessageContaining("core benefit field");
    }

    /**
     * 场景：样例作者把权益金额写成不闭合。
     * 预期：JSON 契约校验显式失败。
     * 红线：权益累计超额或缺口不能进入 route/posting 后再补解释。
     */
    @Test
    void testJsonContractVerifierShouldRejectUnclosedBenefitSnapshotAmount() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-AMOUNT-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_invalid_amount_001",
                      "benefitGroupSn": "bg_invalid_amount_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 9000 },
                      "components": [{
                        "componentSn": "bc_invalid_amount_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "closureRole": "ORDER_DISCOUNT_CLOSURE",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "bearerSubjectRef": { "subjectType": "MERCHANT", "subjectId": "merchant_001" },
                        "benefitReference": { "couponId": "coupon_001", "ruleVersion": "rule_v1" },
                        "contextVariables": {}
                      }],
                      "contextVariables": {}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
    }

    /**
     * 场景：样例作者没有声明权益金额闭合角色。
     * 预期：JSON 契约校验显式失败。
     * 红线：资金底座不能按组件类型猜测闭合公式。
     */
    @Test
    void testJsonContractVerifierShouldRejectMissingBenefitClosureRole() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-MISSING-CLOSURE-ROLE-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_missing_closure_role_001",
                      "benefitGroupSn": "bg_missing_closure_role_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 8000 },
                      "components": [{
                        "componentSn": "bc_missing_closure_role_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "bearerSubjectRef": { "subjectType": "MERCHANT", "subjectId": "merchant_001" },
                        "benefitReference": { "couponId": "coupon_001", "ruleVersion": "rule_v1" },
                        "contextVariables": {}
                      }],
                      "contextVariables": {}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.benefitSnapshot.components.closureRole");
    }

    /**
     * 场景：样例作者把商户应收影响组件混入订单正向抵扣闭合。
     * 预期：JSON 契约校验显式失败。
     * 红线：只有 ORDER_DISCOUNT_CLOSURE 组件能参与订单金额闭合。
     */
    @Test
    void testJsonContractVerifierShouldRejectMixedBenefitClosureRole() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-CLOSURE-ROLE-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_invalid_closure_role_001",
                      "benefitGroupSn": "bg_invalid_closure_role_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 8000 },
                      "components": [{
                        "componentSn": "bc_invalid_closure_role_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "closureRole": "MERCHANT_RECEIVABLE_EFFECT",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "bearerSubjectRef": { "subjectType": "MERCHANT", "subjectId": "merchant_001" },
                        "benefitReference": { "couponId": "coupon_001", "ruleVersion": "rule_v1" },
                        "contextVariables": {}
                      }],
                      "contextVariables": {}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
    }

    /**
     * 场景：样例作者把当前营销规则输入放进权益引用上下文。
     * 预期：JSON 契约校验显式失败。
     * 红线：资金底座不能根据当前活动规则、券包或最优券选择重算优惠。
     */
    @Test
    void testJsonContractVerifierShouldRejectCurrentMarketingRuleInputs() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-RECALC-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_invalid_recalc_001",
                      "benefitGroupSn": "bg_invalid_recalc_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 8000 },
                      "components": [{
                        "componentSn": "bc_invalid_recalc_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "closureRole": "ORDER_DISCOUNT_CLOSURE",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "bearerSubjectRef": { "subjectType": "MERCHANT", "subjectId": "merchant_001" },
                        "benefitReference": {
                          "couponId": "coupon_001",
                          "ruleVersion": "rule_v1",
                          "contextVariables": { "currentMarketingRule": "latest_rule" }
                        },
                        "contextVariables": {}
                      }],
                      "contextVariables": {}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentMarketingRule");
    }

    /**
     * 场景：样例作者把当前营销规则输入藏在权益引用上下文的子对象中。
     * 预期：JSON 契约校验递归识别核心字段并显式失败。
     * 红线：文档样例不能通过嵌套 contextVariables 绕过资金底座不重算优惠的事实边界。
     */
    @Test
    void testJsonContractVerifierShouldRejectNestedCurrentMarketingRuleInputs() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-NESTED-RECALC-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_invalid_nested_recalc_001",
                      "benefitGroupSn": "bg_invalid_nested_recalc_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 8000 },
                      "components": [{
                        "componentSn": "bc_invalid_nested_recalc_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "closureRole": "ORDER_DISCOUNT_CLOSURE",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "bearerSubjectRef": { "subjectType": "MERCHANT", "subjectId": "merchant_001" },
                        "benefitReference": {
                          "couponId": "coupon_001",
                          "ruleVersion": "rule_v1",
                          "contextVariables": {
                            "decisionTrace": { "currentMarketingRule": "latest_rule" }
                          }
                        },
                        "contextVariables": {}
                      }],
                      "contextVariables": {}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentMarketingRule");
    }

    private Path transactionLayerDslDir() {
        return workspaceRoot().resolve("core/src/test/resources/dsl/transaction-layer");
    }

    private Path benefitContractCaseDir() {
        return workspaceRoot().resolve("tests/src/test/resources/dsl-contract-cases");
    }

    private List<Path> jsonSamples(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> sampleStream = Files.list(dir)) {
            return new ArrayList<>(sampleStream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList());
        }
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(multiModuleDir)) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        if ("tests".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
    }
}

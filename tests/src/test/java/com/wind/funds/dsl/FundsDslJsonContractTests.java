package com.wind.funds.dsl;

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
     * 场景：样例作者声明未被 DSL 基线允许的 fixtureLevel。
     * 预期：JSON 契约校验显式失败。
     * 红线：fixtureLevel 决定交付结论等级，不能用未知值绕过盘点和验证门禁。
     */
    @Test
    void testJsonContractVerifierShouldRejectUnknownFixtureLevel() {
        Map<String, Object> document = Map.of(
                "caseId", "DSL-INVALID-FIXTURE-LEVEL-001",
                "fixtureLevel", "DEMO_ONLY",
                "instruction", Map.of(
                        "instructionType", "DIRECT_TRANSACTION",
                        "eventType", "PAY",
                        "transactionType", "PAY",
                        "amount", Map.of("currency", "USD", "amount", 100),
                        "originalAmount", Map.of("currency", "USD", "amount", 100)));

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixtureLevel")
                .hasMessageContaining("DOC_ONLY")
                .hasMessageContaining("GOVERNANCE_FLOW");
    }

    /**
     * 场景：CONTRACT_ONLY 夹具缺少执行化盘点字段。
     * 预期：JSON 契约校验显式失败。
     * 红线：contract-only 只能证明结构契约，必须标明目标测试、核心断言和未完成范围。
     */
    @Test
    void testJsonContractVerifierShouldRejectContractOnlyFixtureWithoutExecutionInventory() {
        Map<String, Object> document = Map.of(
                "caseId", "DSL-INVALID-CONTRACT-INVENTORY-001",
                "fixtureLevel", "CONTRACT_ONLY",
                "scenarioCode", "DIRECT_PAY_WITH_CONTRACT_ONLY_FIXTURE",
                "acceptanceIds", List.of("AC-CONTRACT-001"),
                "tddIds", List.of("TDD-CONTRACT-001"),
                "systemDesignRefs", List.of("02-交易路由钱包账目与投影系分设计#契约承载"),
                "instruction", Map.of(
                        "instructionType", "DIRECT_TRANSACTION",
                        "eventType", "PAY",
                        "transactionType", "PAY",
                        "amount", Map.of("currency", "USD", "amount", 100),
                        "originalAmount", Map.of("currency", "USD", "amount", 100)),
                "validation", Map.of(
                        "mustPass", List.of("字段结构可解析"),
                        "mustFail", List.of("声明资金流已完成")));

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetTestClass");
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
     * 场景：账务分录样例缺少稳定可追踪的主体 ID。
     * 预期：JSON 契约校验显式失败。
     * 红线：LedgerEntry 不能只靠主体类型和账目代码落账，否则余额投影和回放无法定位真实主体。
     */
    @Test
    void testJsonContractVerifierShouldRejectPostingEntryWithoutSubjectId() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-SUBJECT-ID-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "USD",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedPosting.postingPlans.entries.subjectId");
    }

    /**
     * 场景：账务分录样例缺少顶层币种。
     * 预期：JSON 契约校验显式失败。
     * 红线：LedgerEntry 的余额 bucket 口径必须显式包含 currency，不能只从金额对象间接推断。
     */
    @Test
    void testJsonContractVerifierShouldRejectPostingEntryWithoutCurrency() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-CURRENCY-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectId": "fa_user_10001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedPosting.postingPlans.entries.currency");
    }

    /**
     * 场景：账务分录样例的余额 bucket 币种和金额币种不一致。
     * 预期：JSON 契约校验显式失败。
     * 红线：同一 LedgerEntry 不能表达为 USD 金额却落入 EUR 余额 bucket。
     */
    @Test
    void testJsonContractVerifierShouldRejectPostingEntryCurrencyMismatch() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-CURRENCY-MISMATCH-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectId": "fa_user_10001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "EUR",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry currency must match amount currency");
    }

    /**
     * 场景：账务分录样例声明非生命周期账本周期，却把 periodId 写成 LIFETIME。
     * 预期：JSON 契约校验显式失败。
     * 红线：非 LIFETIME 周期必须由账户策略、业务请求或路由规则显式确定，不能复用生命周期占位值。
     */
    @Test
    void testJsonContractVerifierShouldRejectPostingEntryInvalidPeriodId() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-PERIOD-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectId": "fa_user_10001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "USD",
                        "periodType": "MONTHLY",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodId must not be LIFETIME");
    }

    /**
     * 场景：账务期望样例没有任何账务计划。
     * 预期：JSON 契约校验显式失败。
     * 红线：expectedPosting 只要出现，就必须明确可验证的 PostingPlan。
     */
    @Test
    void testJsonContractVerifierShouldRejectExpectedPostingWithoutPostingPlans() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-EMPTY-PLANS-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": []
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedPosting.postingPlans must not be empty");
    }

    /**
     * 场景：账务计划样例没有任何分录。
     * 预期：JSON 契约校验显式失败。
     * 红线：PostingPlan 不能用空 entries 绕过借贷平衡和余额影响断言。
     */
    @Test
    void testJsonContractVerifierShouldRejectPostingPlanWithoutEntries() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-EMPTY-ENTRIES-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": []
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedPosting.postingPlans.entries must not be empty");
    }

    /**
     * 场景：账务计划样例中同币种借贷金额不相等。
     * 预期：JSON 契约校验显式失败。
     * 红线：DSL 样例不能让不平衡 PostingPlan 进入交易、账本和投影测试入口。
     */
    @Test
    void testJsonContractVerifierShouldRejectUnbalancedPostingPlan() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-UNBALANCED-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectId": "fa_user_10001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "USD",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }, {
                        "subjectId": "fa_merchant_20001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "CLEARING",
                        "currency": "USD",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "CREDIT",
                        "amount": { "currency": "USD", "amount": 80 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posting plan must be balanced");
    }

    /**
     * 场景：账务计划样例中借贷金额数值相等但币种不同。
     * 预期：JSON 契约校验显式失败。
     * 红线：PostingPlan 必须按同币种独立平衡，不能用数值相等掩盖跨币种错账。
     */
    @Test
    void testJsonContractVerifierShouldRejectCrossCurrencyPostingPlan() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-CROSS-CURRENCY-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectId": "fa_user_10001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "USD",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }, {
                        "subjectId": "fa_merchant_20001_eur",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "CLEARING",
                        "currency": "EUR",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "CREDIT",
                        "amount": { "currency": "EUR", "amount": 100 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posting plan currency mismatch");
    }

    /**
     * 场景：账务计划样例中 USD 和 EUR 各自借贷数值都平衡，但混在同一个 PostingPlan。
     * 预期：JSON 契约校验显式失败。
     * 红线：每个 PostingPlan 只能表达一个币种的独立平衡，不允许用多币种集合掩盖账务阶段边界。
     */
    @Test
    void testJsonContractVerifierShouldRejectMultiCurrencyPostingPlan() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-POSTING-MULTI-CURRENCY-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedPosting": {
                    "postingPlans": [{
                      "intent": "TRANSFER",
                      "postingScope": "BETWEEN_SUBJECTS",
                      "balanceEffectType": "CONSUME",
                      "phaseCode": "SETTLEMENT",
                      "entries": [{
                        "subjectId": "fa_user_10001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "USD",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }, {
                        "subjectId": "fa_merchant_20001_usd",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "CLEARING",
                        "currency": "USD",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "CREDIT",
                        "amount": { "currency": "USD", "amount": 100 }
                      }, {
                        "subjectId": "fa_user_10001_eur",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "AVAILABLE",
                        "currency": "EUR",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "DEBIT",
                        "amount": { "currency": "EUR", "amount": 80 }
                      }, {
                        "subjectId": "fa_merchant_20001_eur",
                        "subjectType": "FUNDING_ACCOUNT",
                        "ledgerSubjectCode": "CLEARING",
                        "currency": "EUR",
                        "periodType": "LIFETIME",
                        "periodId": "LIFETIME",
                        "entrySide": "CREDIT",
                        "amount": { "currency": "EUR", "amount": 80 }
                      }]
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posting plan must use one currency");
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

    /**
     * 场景：VCC 共享卡 route JSON 样例声明资金来源决策和账户层级快照。
     * 预期：JSON 契约校验可识别信用子账户、父资金账户和层级版本。
     * 红线：账户层级快照不能只停留在 Java 值对象，文档样例也必须机器可校验。
     */
    @Test
    void testJsonContractVerifierShouldAcceptVccRouteAccountHierarchySnapshot() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-VCC-ROUTE-HIERARCHY-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routeCode": "VCC_SHARED_CARD_AUTH",
                    "routeVersion": "v1",
                    "snapshotSchemaVersion": "route.snapshot.v1",
                    "routingDecision": {
                      "policyCode": "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT",
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_001",
                          "currency": "USD",
                          "ledgerProfileCode": "CREDIT_BASIC"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_001",
                            "currency": "USD",
                            "ledgerProfileCode": "CREDIT_BASIC"
                          },
                          "parentAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_001",
                            "currency": "USD",
                            "ledgerProfileCode": "FUNDING_BASIC"
                          },
                          "rootAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_001",
                            "currency": "USD",
                            "ledgerProfileCode": "FUNDING_BASIC"
                          },
                          "hierarchyVersion": "card-binding-v1",
                          "contextVariables": { "accountPurpose": "VCC_SHARED_CARD" }
                        }
                      }]
                    }
                  }
                }
                """);

        FundsDslJsonContractVerifier.verifyTransactionLayerCase(document);
    }

    /**
     * 场景：VCC route JSON 样例里的资金来源决策缺少 priority。
     * 预期：JSON 契约校验显式失败。
     * 红线：资金来源决策的排序优先级是路由重放和责任归因的稳定字段，不能只在 Java 构造器里校验。
     */
    @Test
    void testJsonContractVerifierShouldRejectFundingAllocationMissingPriority() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ALLOCATION-MISSING-PRIORITY-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_missing_priority_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_missing_priority_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "reason": "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT"
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundingAllocations.priority is required");
    }

    /**
     * 场景：VCC route JSON 样例里的资金来源决策缺少 reason。
     * 预期：JSON 契约校验显式失败。
     * 红线：资金来源决策原因是回放审计和问题排查的稳定字段，不能只在 Java 构造器里校验。
     */
    @Test
    void testJsonContractVerifierShouldRejectFundingAllocationMissingReason() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ALLOCATION-MISSING-REASON-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_missing_reason_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_missing_reason_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundingAllocations.reason is required");
    }

    /**
     * 场景：VCC route JSON 样例里多个资金来源决策使用相同 priority。
     * 预期：JSON 契约校验显式失败。
     * 红线：资金来源决策优先级必须稳定唯一，避免路由重放和资金责任解析出现非确定性排序。
     */
    @Test
    void testJsonContractVerifierShouldRejectFundingAllocationDuplicatePriority() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ALLOCATION-DUPLICATE-PRIORITY-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_duplicate_priority_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_duplicate_priority_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 60 },
                        "priority": 10,
                        "reason": "VCC_SHARED_CARD_PRIMARY_ALLOCATION"
                      }, {
                        "allocationId": "alloc_vcc_duplicate_priority_002",
                        "subjectRef": {
                          "subjectType": "FUNDING_ACCOUNT",
                          "subjectId": "fa_vcc_duplicate_priority_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AVAILABLE",
                        "amount": { "currency": "USD", "amount": 40 },
                        "priority": 10,
                        "reason": "VCC_SHARED_CARD_BACKUP_ALLOCATION"
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundingAllocations.priority must be unique");
    }

    /**
     * 场景：VCC route JSON 样例中信用子账户 leg 消耗 100，但资金责任只分配 80。
     * 预期：JSON 契约校验显式失败。
     * 红线：核心资金 / 信用账户的资金责任分配金额必须和 route leg 消耗金额闭合，避免 route snapshot 进入后续账务时资金责任短缺。
     */
    @Test
    void testJsonContractVerifierShouldRejectUnclosedCoreAccountFundingAllocationAmount() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ROUTE-ALLOCATION-CLOSURE-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_closure_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_closure_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 80 },
                        "priority": 10,
                        "reason": "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT"
                      }]
                    },
                    "legs": [{
                      "legType": "HOLD",
                      "sourceNode": {
                        "nodeType": "SUBJECT",
                        "nodeRole": "SOURCE",
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_closure_001",
                        "ledgerSubjectCode": "AVAILABLE"
                      },
                      "targetNode": {
                        "nodeType": "SUBJECT",
                        "nodeRole": "TARGET",
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_closure_001",
                        "ledgerSubjectCode": "AUTHORIZATION"
                      },
                      "amount": { "currency": "USD", "amount": 100 },
                      "balanceEffectType": "HOLD",
                      "phaseCode": "AUTHORIZATION",
                      "replayPolicy": "PARTIAL_ALLOWED"
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core account allocation amount must equal consume route amount");
    }

    /**
     * 场景：VCC route JSON 样例把支付工具节点写成账本 route leg 来源节点。
     * 预期：JSON 契约校验显式失败。
     * 红线：支付工具只能作为路由输入和快照引用，不能穿透为账本可记账节点。
     */
    @Test
    void testJsonContractVerifierShouldRejectPaymentInstrumentRouteLegNode() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ROUTE-PAYMENT-INSTRUMENT-NODE-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_card_node_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_node_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT"
                      }]
                    },
                    "legs": [{
                      "legType": "HOLD",
                      "sourceNode": {
                        "nodeType": "PAYMENT_INSTRUMENT",
                        "nodeRole": "SOURCE",
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_card_node_001",
                        "ledgerSubjectCode": "AVAILABLE"
                      },
                      "targetNode": {
                        "nodeType": "SUBJECT",
                        "nodeRole": "TARGET",
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_card_node_001",
                        "ledgerSubjectCode": "AUTHORIZATION"
                      },
                      "amount": { "currency": "USD", "amount": 100 },
                      "balanceEffectType": "HOLD",
                      "phaseCode": "AUTHORIZATION",
                      "replayPolicy": "PARTIAL_ALLOWED"
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RouteLeg sourceNode must be ledger-postable");
    }

    /**
     * 场景：VCC route participant 只声明预算组类型，缺少稳定主体标识。
     * 预期：JSON 契约校验显式失败。
     * 红线：预算组可作为迁移期控制参与方快照，但不能缺失可追溯主体 ID。
     */
    @Test
    void testJsonContractVerifierShouldRejectBudgetGroupRouteParticipantWithoutSubjectId() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ROUTE-BUDGET-PARTICIPANT-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "participants": [{
                      "participantRole": "BUDGET_CONTROLLER",
                      "subjectRef": {
                        "subjectType": "BUDGET_GROUP"
                      }
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRoute.participants.subjectRef.subjectId");
    }

    /**
     * 场景：route participant 只声明主体类型，缺少稳定主体标识。
     * 预期：JSON 契约校验显式失败。
     * 红线：route participant 会进入 route snapshot 和回放解释链路，不能缺失可追溯主体 ID。
     */
    @Test
    void testJsonContractVerifierShouldRejectRouteParticipantWithoutSubjectId() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ROUTE-PARTICIPANT-SUBJECT-ID-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "participants": [{
                      "participantRole": "AUTH_HOLDER",
                      "subjectRef": {
                        "subjectType": "CREDIT_ACCOUNT"
                      }
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRoute.participants.subjectRef.subjectId");
    }

    /**
     * 场景：route participant JSON 样例把外部账户原文字段放进上下文。
     * 预期：JSON 契约校验显式失败。
     * 红线：route participant 会进入 route snapshot 和归档重放链路，不能绕过对象 DSL 的敏感字段保护。
     */
    @Test
    void testJsonContractVerifierShouldRejectRouteParticipantSensitiveContextVariables() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-ROUTE-PARTICIPANT-SENSITIVE-CONTEXT-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "participants": [{
                      "participantRole": "AUTH_HOLDER",
                      "subjectRef": {
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_sensitive_context_001"
                      },
                      "contextVariables": {
                        "externalAccount": { "bankAccountNo": "123456789012" }
                      }
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRoute.participants.contextVariables")
                .hasMessageContaining("sensitive fields");
    }

    /**
     * 场景：route participant JSON 样例把权益退款处置藏进上下文。
     * 预期：JSON 契约校验显式失败。
     * 红线：route participant context 只能承载稳定摘要，不能替代权益快照或资金责任一等字段。
     */
    @Test
    void testJsonContractVerifierShouldRejectRouteParticipantCoreBenefitContextVariables() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-ROUTE-PARTICIPANT-BENEFIT-CONTEXT-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "participants": [{
                      "participantRole": "AUTH_HOLDER",
                      "subjectRef": {
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_benefit_context_001"
                      },
                      "contextVariables": {
                        "benefitPayload": { "refundDisposition": "REFUND_TO_PLATFORM" }
                      }
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRoute.participants.contextVariables")
                .hasMessageContaining("core benefit field: refundDisposition");
    }

    /**
     * 场景：JSON 样例把预算组写成账户层级中的实际账户。
     * 预期：JSON 契约校验显式失败。
     * 红线：预算组是控制范围，不是资金或信用账户，不能进入账户层级作为落账主体。
     */
    @Test
    void testJsonContractVerifierShouldRejectBudgetGroupAsHierarchyAccountRef() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-BUDGET-HIERARCHY-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_budget_001",
                        "subjectRef": {
                          "subjectType": "BUDGET_GROUP",
                          "subjectId": "bg_vcc_001"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "INVALID_BUDGET_GROUP_HIERARCHY_ACCOUNT",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "BUDGET_GROUP",
                            "subjectId": "bg_vcc_001"
                          },
                          "hierarchyVersion": "budget-binding-v1"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountHierarchySnapshot.accountRef.subjectType")
                .hasMessageContaining("FUNDING_ACCOUNT or CREDIT_ACCOUNT");
    }

    /**
     * 场景：JSON 样例里的资金来源决策主体和账户层级快照主体不一致。
     * 预期：JSON 契约校验显式失败。
     * 红线：回放、账单和责任归因不能出现 allocation 指向一个账户、层级快照指向另一个账户。
     */
    @Test
    void testJsonContractVerifierShouldRejectMismatchedHierarchyAccountRef() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-HIERARCHY-MISMATCH-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_mismatch_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_001"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "INVALID_MISMATCHED_HIERARCHY_ACCOUNT",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_002"
                          },
                          "hierarchyVersion": "card-binding-v1"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountRef must match funding allocation subjectRef");
    }

    /**
     * 场景：JSON 样例里的资金来源决策主体和账户层级快照主体 ID 一致，但币种不同。
     * 预期：JSON 契约校验显式失败。
     * 红线：同一个资金责任主体不能在 route snapshot 中被记录成不同币种，否则回放和余额投影会跨币种归因。
     */
    @Test
    void testJsonContractVerifierShouldRejectHierarchyAccountRefCurrencyMismatch() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-HIERARCHY-CURRENCY-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_currency_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_currency_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "INVALID_HIERARCHY_ACCOUNT_CURRENCY",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_currency_001",
                            "currency": "EUR"
                          },
                          "parentAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_currency_001",
                            "currency": "EUR"
                          },
                          "rootAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_currency_001",
                            "currency": "EUR"
                          },
                          "hierarchyVersion": "card-binding-currency-mismatch"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountRef currency must match funding allocation subjectRef currency");
    }

    /**
     * 场景：JSON 样例里的资金来源决策主体币种和 allocation 金额币种不一致。
     * 预期：JSON 契约校验显式失败。
     * 红线：资金来源决策不能表达为 EUR 主体承担 USD 金额，否则账务计划和回放投影会跨币种归因。
     */
    @Test
    void testJsonContractVerifierShouldRejectFundingAllocationSubjectAmountCurrencyMismatch() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-ALLOCATION-AMOUNT-CURRENCY-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_amount_currency_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_amount_currency_001",
                          "currency": "EUR"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "INVALID_ALLOCATION_AMOUNT_CURRENCY",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_amount_currency_001",
                            "currency": "EUR"
                          },
                          "parentAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_amount_currency_001",
                            "currency": "EUR"
                          },
                          "rootAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_amount_currency_001",
                            "currency": "EUR"
                          },
                          "hierarchyVersion": "card-binding-amount-currency-mismatch"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation amount currency must match subjectRef currency");
    }

    /**
     * 场景：JSON 样例把 VCC 卡绑定信用子账户同时写成 parent/root。
     * 预期：JSON 契约校验显式失败。
     * 红线：父账户和根账户不能指回实际落账账户本身，否则多级账户汇总和回放归因会出现自循环。
     */
    @Test
    void testJsonContractVerifierShouldRejectSelfReferencedHierarchyRelation() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-HIERARCHY-SELF-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_self_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_self_001",
                          "currency": "USD"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "INVALID_SELF_REFERENCED_HIERARCHY",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_self_001",
                            "currency": "USD"
                          },
                          "parentAccountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_self_001",
                            "currency": "USD"
                          },
                          "rootAccountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_self_001",
                            "currency": "USD"
                          },
                          "hierarchyVersion": "card-binding-self"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parentAccountRef must not reference accountRef itself");
    }

    /**
     * 场景：JSON 样例中的 VCC 子账户没有租户，父账户和根账户分别写入不同租户。
     * 预期：JSON 契约校验显式失败。
     * 红线：父账户和根账户必须属于同一责任边界，不能因子账户字段缺省而绕过父根冲突。
     */
    @Test
    void testJsonContractVerifierShouldRejectInconsistentParentAndRootRelation() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-VCC-HIERARCHY-ROOT-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "routingDecision": {
                      "fundingAllocations": [{
                        "allocationId": "alloc_vcc_root_001",
                        "subjectRef": {
                          "subjectType": "CREDIT_ACCOUNT",
                          "subjectId": "ca_vcc_card_root_001"
                        },
                        "ledgerSubjectCode": "AUTHORIZATION",
                        "amount": { "currency": "USD", "amount": 100 },
                        "priority": 10,
                        "reason": "INVALID_PARENT_ROOT_RELATION",
                        "accountHierarchySnapshot": {
                          "accountRef": {
                            "subjectType": "CREDIT_ACCOUNT",
                            "subjectId": "ca_vcc_card_root_001"
                          },
                          "parentAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_parent_root_001",
                            "tenantId": 1,
                            "currency": "USD"
                          },
                          "rootAccountRef": {
                            "subjectType": "FUNDING_ACCOUNT",
                            "subjectId": "fa_vcc_root_root_001",
                            "tenantId": 2,
                            "currency": "USD"
                          },
                          "hierarchyVersion": "card-binding-root-mismatch"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rootAccountRef.tenantId must match parentAccountRef.tenantId");
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

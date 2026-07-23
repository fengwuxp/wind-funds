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
import java.util.LinkedHashMap;
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
     * 场景：CONTRACT_ONLY 夹具携带 route、posting 或 replay 资金流断言字段。
     * 预期：JSON 契约校验显式失败，错误指向 fixtureLevel 越权。
     * 红线：contract-only 只能证明字段结构、枚举和 validation，不能夹带资金流完成证据。
     */
    @Test
    void testJsonContractVerifierShouldRejectContractOnlyFixtureWithFundsFlowAssertions() {
        for (String assertionField : List.of("expectedRoute", "expectedPosting", "replayRequest")) {
            Map<String, Object> document = new LinkedHashMap<>(contractOnlyFixtureInventory());
            document.put(assertionField, Map.of("placeholder", true));

            assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                    .as(assertionField)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CONTRACT_ONLY")
                    .hasMessageContaining(assertionField);
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
     * 红线：transactionType 不承载 AUTHORIZE、COMPLETE、REVERSAL 等生命周期事件。
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
     * 场景：旧权益快照 DSL 字段仍出现在资金指令样例中。
     * 预期：JSON 契约校验显式失败。
     * 红线：权益让利资金事实必须走交易应用服务，不再作为 core instruction 的 benefitSnapshot 字段承载。
     */
    @Test
    void testJsonContractVerifierShouldRejectLegacyBenefitSnapshotField() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-LEGACY-BENEFIT-SNAPSHOT-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "benefitSnapshot": {
                      "benefitSnapshotId": "bs_legacy_001",
                      "benefitGroupSn": "bg_legacy_001",
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "userPayAmount": { "currency": "USD", "amount": 8000 },
                      "merchantReceivableAmount": { "currency": "USD", "amount": 8000 },
                      "components": [{
                        "componentSn": "bc_legacy_001",
                        "benefitType": "MERCHANT_COUPON",
                        "componentType": "MERCHANT_DISCOUNT",
                        "closureRole": "ORDER_DISCOUNT_CLOSURE",
                        "amount": { "currency": "USD", "amount": 2000 },
                        "ledgerEffect": "NO_LEDGER",
                        "fundingNature": "MERCHANT_BORNE",
                        "costBearerSubjectRef": {
                          "subjectType": "FUNDING_ACCOUNT",
                          "subjectId": "fa_merchant_marketing_usd"
                        },
                        "benefitReceiverSubjectRef": {
                          "subjectType": "FUNDING_ACCOUNT",
                          "subjectId": "fa_user_benefit_usd"
                        },
                        "benefitReference": {
                          "couponId": "coupon_legacy_001",
                          "ruleVersion": "merchant_rule_v3"
                        }
                      }]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.benefitSnapshot")
                .hasMessageContaining("legacy benefit snapshot DSL");
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
     * 场景：样例作者把权益资金事实核心金额藏入指令 contextVariables。
     * 预期：JSON 契约校验显式失败。
     * 红线：权益让利事实必须走权益资金交易入口，不得用通用上下文伪装 DSL 字段。
     */
    @Test
    void testJsonContractVerifierShouldRejectBenefitCoreFieldsInInstructionContextVariables() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-CONTEXT-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "contextVariables": {
                      "orderAmount": { "currency": "USD", "amount": 10000 },
                      "fundingNature": "MERCHANT_BORNE"
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.contextVariables")
                .hasMessageContaining("core benefit field:");
    }

    /**
     * 场景：样例作者把当前营销规则输入藏在指令上下文的子对象中。
     * 预期：JSON 契约校验递归识别核心字段并显式失败。
     * 红线：资金底座不能根据当前活动规则、券包或最优券选择重算优惠。
     */
    @Test
    void testJsonContractVerifierShouldRejectNestedBenefitRecalculationInputs() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BENEFIT-NESTED-RECALC-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "contextVariables": {
                      "decisionTrace": {
                        "currentMarketingRule": "latest_rule"
                      }
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.contextVariables")
                .hasMessageContaining("currentMarketingRule");
    }

    /**
     * 场景：历史 route 或投影只保留原权益摘要。
     * 预期：JSON 契约允许摘要字段通过。
     * 红线：允许摘要追溯不等于恢复完整旧权益快照 DSL。
     */
    @Test
    void testJsonContractVerifierShouldAllowHistoricalBenefitSummaryContextVariables() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-BENEFIT-SUMMARY-CONTEXT-001",
                  "instruction": {
                    "instructionType": "DIRECT_TRANSACTION",
                    "eventType": "PAY",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 8000 },
                    "originalAmount": { "currency": "USD", "amount": 8000 },
                    "contextVariables": {
                      "benefitSnapshotId": "BS-HISTORICAL-SUMMARY-001",
                      "stableDigest": "sha256:historical-benefit-digest"
                    }
                  }
                }
                """);

        FundsDslJsonContractVerifier.verifyTransactionLayerCase(document);
    }

    /**
     * 场景：VCC 共享卡 route JSON 样例在信用账户 participant 上声明账户层级快照。
     * 预期：JSON 契约校验可识别关系号、信用子账户和直接父资金账户。
     * 红线：层级快照只描述参与账户当时的直接父关系，不得重复表达资金分配。
     */
    @Test
    void testJsonContractVerifierShouldAcceptParticipantAccountHierarchySnapshot() {
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
                    "participants": [{
                      "participantRole": "PAYER",
                      "subjectRef": {
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_card_001",
                        "tenantId": 1,
                        "currency": "USD"
                      },
                      "accountHierarchySnapshot": {
                        "relationSn": "AHR-VCC-001",
                        "parentAccountRef": {
                          "subjectType": "FUNDING_ACCOUNT",
                          "subjectId": "fa_vcc_parent_001",
                          "tenantId": 1,
                          "currency": "USD"
                        }
                      }
                    }]
                  }
                }
                """);

        FundsDslJsonContractVerifier.verifyTransactionLayerCase(document);
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
     * 场景：JSON 样例把支出控制范围写成账本 route leg 节点主体类型。
     * 预期：JSON 契约校验显式失败。
     * 红线：支出控制范围只能作为控制范围和控制投影视图，不能成为账本 route node。
     */
    @Test
    void testJsonContractVerifierShouldRejectSpendControlScopeRouteLegNode() {
        JSONObject document = JSON.parseObject("""
                {
                  "caseId": "DSL-INVALID-BUDGET-GROUP-ROUTE-NODE-001",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "legs": [{
                      "legType": "HOLD",
                      "sourceNode": {
                        "nodeRole": "SOURCE",
                        "subjectType": "SPEND_CONTROL_SCOPE",
                        "subjectId": "bg_invalid_route_node",
                        "ledgerSubjectCode": "AVAILABLE"
                      },
                      "targetNode": {
                        "nodeRole": "TARGET",
                        "subjectType": "SPEND_CONTROL_SCOPE",
                        "subjectId": "bg_invalid_route_node",
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
                .hasMessageContaining("expectedRoute.legs.sourceNode.subjectType")
                .hasMessageContaining("FundsSubjectType");
    }

    /**
     * 场景：VCC route participant 把支出控制范围声明为主体类型。
     * 预期：JSON 契约校验显式失败。
     * 红线：支出控制范围只能作为支出控制范围，不能成为 route participant 资金主体。
     */
    @Test
    void testJsonContractVerifierShouldRejectSpendControlScopeRouteParticipantSubjectType() {
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
                        "subjectType": "SPEND_CONTROL_SCOPE"
                      }
                    }]
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRoute.participants.subjectRef.subjectType")
                .hasMessageContaining("FundsSubjectType");
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
     * 场景：participant 层级快照缺少关系号。
     * 预期：JSON 契约校验显式失败。
     */
    @Test
    void testJsonContractVerifierShouldRejectHierarchyWithoutRelationSn() {
        JSONObject document = hierarchyParticipantDocument("""
                {
                  "parentAccountRef": {
                    "subjectType": "FUNDING_ACCOUNT",
                    "subjectId": "fa_vcc_parent_001",
                    "tenantId": 1,
                    "currency": "USD"
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountHierarchySnapshot.relationSn is required");
    }

    /**
     * 场景：支出控制范围被误写成 participant 的父账户。
     * 预期：JSON 契约校验显式失败。
     */
    @Test
    void testJsonContractVerifierShouldRejectSpendControlScopeAsHierarchyParent() {
        JSONObject document = hierarchyParticipantDocument("""
                {
                  "relationSn": "AHR-VCC-INVALID-001",
                  "parentAccountRef": {
                    "subjectType": "SPEND_CONTROL_SCOPE",
                    "subjectId": "scope_vcc_001",
                    "tenantId": 1,
                    "currency": "USD"
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parentAccountRef.subjectType must be FUNDING_ACCOUNT or CREDIT_ACCOUNT");
    }

    /**
     * 场景：participant 与父账户币种不一致。
     * 预期：JSON 契约校验显式失败。
     */
    @Test
    void testJsonContractVerifierShouldRejectHierarchyParentCurrencyMismatch() {
        JSONObject document = hierarchyParticipantDocument("""
                {
                  "relationSn": "AHR-VCC-INVALID-002",
                  "parentAccountRef": {
                    "subjectType": "FUNDING_ACCOUNT",
                    "subjectId": "fa_vcc_parent_001",
                    "tenantId": 1,
                    "currency": "EUR"
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parentAccountRef.currency must match account currency");
    }

    /**
     * 场景：participant 的直接父账户指回自身。
     * 预期：JSON 契约校验显式失败。
     */
    @Test
    void testJsonContractVerifierShouldRejectSelfReferencedHierarchyParent() {
        JSONObject document = hierarchyParticipantDocument("""
                {
                  "relationSn": "AHR-VCC-INVALID-003",
                  "parentAccountRef": {
                    "subjectType": "CREDIT_ACCOUNT",
                    "subjectId": "ca_vcc_card_001",
                    "tenantId": 1,
                    "currency": "USD"
                  }
                }
                """);

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parentAccountRef must not reference accountRef itself");
    }

    private JSONObject hierarchyParticipantDocument(String hierarchySnapshot) {
        return JSON.parseObject("""
                {
                  "caseId": "DSL-VCC-ROUTE-HIERARCHY-VALIDATION",
                  "instruction": {
                    "instructionType": "AUTHORIZATION_TRANSACTION",
                    "eventType": "AUTHORIZE",
                    "transactionType": "PAY",
                    "amount": { "currency": "USD", "amount": 100 },
                    "originalAmount": { "currency": "USD", "amount": 100 }
                  },
                  "expectedRoute": {
                    "participants": [{
                      "participantRole": "PAYER",
                      "subjectRef": {
                        "subjectType": "CREDIT_ACCOUNT",
                        "subjectId": "ca_vcc_card_001",
                        "tenantId": 1,
                        "currency": "USD"
                      },
                      "accountHierarchySnapshot": %s
                    }]
                  }
                }
                """.formatted(hierarchySnapshot));
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

    private Map<String, Object> contractOnlyFixtureInventory() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("caseId", "DSL-INVALID-CONTRACT-FUNDS-FLOW-001");
        document.put("fixtureLevel", "CONTRACT_ONLY");
        document.put("scenarioCode", "DIRECT_PAY_WITH_CONTRACT_ONLY_FUNDS_FLOW");
        document.put("acceptanceIds", List.of("AC-CONTRACT-002"));
        document.put("tddIds", List.of("TDD-CONTRACT-002"));
        document.put("systemDesignRefs", List.of("02-交易路由钱包账目与投影系分设计#契约承载"));
        document.put("targetTestClass", "FundsDslJsonContractTests");
        document.put("coreAssertions", List.of("字段结构可解析"));
        document.put("notDone", List.of("不证明 route、posting、ledger entry、余额投影或 replay 已完成"));
        document.put("instruction", Map.of(
                "instructionType", "DIRECT_TRANSACTION",
                "eventType", "PAY",
                "transactionType", "PAY",
                "amount", Map.of("currency", "USD", "amount", 100),
                "originalAmount", Map.of("currency", "USD", "amount", 100)));
        document.put("validation", Map.of(
                "mustPass", List.of("字段结构可解析"),
                "mustFail", List.of("声明资金流已完成")));
        return document;
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

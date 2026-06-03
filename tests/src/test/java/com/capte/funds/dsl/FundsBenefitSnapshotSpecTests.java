package com.capte.funds.dsl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.model.transaction.ImmutableFundsBenefitComponentSpec;
import com.wind.funds.model.transaction.ImmutableFundsBenefitReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsBenefitRefundPolicySpec;
import com.wind.funds.model.transaction.ImmutableFundsBenefitSnapshotSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.spec.transaction.FundsBenefitComponentSpec;
import com.wind.funds.spec.transaction.FundsBenefitReferenceSpec;
import com.wind.funds.spec.transaction.FundsBenefitRefundPolicySpec;
import com.wind.funds.spec.transaction.FundsBenefitSnapshotSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.funds.transaction.enums.FundsBenefitType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.util.FundsBenefitSnapshotJsonSupport;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 权益快照 DSL 契约测试。
 */
class FundsBenefitSnapshotSpecTests {

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;
    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 5, 21, 10, 0);

    /**
     * 场景：既有资金指令不携带权益快照。
     * 预期：benefitSnapshot 为空，amount、originalAmount 和 exchangeRate 语义不变。
     * 红线：不能为了权益支持破坏无权益交易、授权或退款的目标态稳定性。
     */
    @Test
    void testInstructionWithoutBenefitSnapshotShouldRemainCompatible() {
        FundsInstructionSpec instruction = instruction(null, money(100L));

        assertThat(instruction.getBenefitSnapshot()).isNull();
        assertThat(instruction.getAmount()).isEqualTo(money(100L));
        assertThat(instruction.getOriginalAmount()).isEqualTo(money(100L));
        assertThat(instruction.getExchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    /**
     * 场景：业务侧已经完成商户券决策，把权益快照带入资金指令。
     * 预期：快照、组件、引用、退款策略和上下文都有一等字段表达。
     * 红线：权益核心语义不能只藏在 contextVariables。
     */
    @Test
    void testMinimalBenefitSnapshotShouldExposeStableFields() {
        FundsBenefitSnapshotSpec snapshot = merchantDiscountSnapshot(8000L, 2000L);
        FundsInstructionSpec instruction = instruction(snapshot, money(8000L));

        assertThat(instruction.getBenefitSnapshot()).isSameAs(snapshot);
        assertThat(snapshot.getBenefitSnapshotId()).isEqualTo("BS-MERCHANT-001");
        assertThat(snapshot.getBenefitSchemaVersion()).isEqualTo("1.0");
        assertThat(snapshot.getOrderAmount()).isEqualTo(money(10000L));
        assertThat(snapshot.getUserPayAmount()).isEqualTo(money(8000L));
        assertThat(snapshot.getMerchantReceivableAmount()).isEqualTo(money(8000L));
        assertThat(snapshot.getComponents()).hasSize(1);
        assertThat(snapshot.getComponents().getFirst().getBenefitReference().getRuleVersion())
                .isEqualTo("merchant-rule-v3");
        assertThat(snapshot.getComponents().getFirst().getRefundPolicy().getDispositions())
                .containsExactly(FundsBenefitRefundDisposition.NO_REFUND,
                        FundsBenefitRefundDisposition.REDUCE_MERCHANT_RECEIVABLE);
        assertThat(snapshot.getContextVariables()).isEmpty();
        assertThat(snapshot.getComponents().getFirst().getContextVariables()).isEmpty();
    }

    /**
     * 场景：订单金额、用户实付和权益组件进入同一快照。
     * 预期：userPayAmount + ORDER_DISCOUNT_CLOSURE components.amount = orderAmount 才能构造成功。
     * 红线：权益累计超额或缺口不能进入 route/posting 后再被解释。
     */
    @Test
    void testBenefitSnapshotShouldRequireAmountClosure() {
        FundsBenefitSnapshotSpec snapshot = merchantDiscountSnapshot(8000L, 2000L);

        assertThat(snapshot.getOrderAmount()).isEqualTo(money(10000L));
        assertThatThrownBy(() -> benefitSnapshot("BS-MISMATCH-001",
                money(10000L),
                money(9000L),
                List.of(merchantDiscountComponent("BC-MISMATCH-001", 2000L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
    }

    /**
     * 场景：组件表示商户应收影响或展示对账，不属于订单正向抵扣闭合。
     * 预期：该组件不得被计入 userPayAmount + 抵扣金额 = orderAmount。
     * 红线：闭合角色混用会导致平台补贴、退款处置或商户应收被误算成订单优惠。
     */
    @Test
    void testOnlyOrderDiscountClosureComponentsShouldCloseOrderAmount() {
        assertThatThrownBy(() -> benefitSnapshot("BS-CLOSURE-ROLE-001",
                money(10000L),
                money(8000L),
                List.of(merchantReceivableEffectComponent("BC-CLOSURE-ROLE-001", 2000L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
    }

    /**
     * 场景：权益组件没有声明金额闭合角色。
     * 预期：DSL 入口拒绝构造。
     * 红线：资金底座不能猜测组件参与哪一种闭合公式。
     */
    @Test
    void testBenefitComponentShouldRequireClosureRole() {
        assertThatThrownBy(() -> ImmutableFundsBenefitComponentSpec.builder()
                .componentSn("BC-NO-CLOSURE-001")
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .amount(money(2000L))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(subjectRef("MERCHANT-001"))
                .benefitReference(benefitReference())
                .contextVariables(Map.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsBenefit.closureRole must not be null");
    }

    /**
     * 场景：同一权益快照内出现两个相同组件号。
     * 预期：DSL 入口拒绝重复组件。
     * 红线：退款、对账或问题定位不能依赖不唯一的 componentSn。
     */
    @Test
    void testBenefitSnapshotShouldRequireUniqueComponentSn() {
        FundsBenefitComponentSpec component = merchantDiscountComponent("BC-DUP-001", 1000L);

        assertThatThrownBy(() -> benefitSnapshot("BS-DUP-001",
                money(10000L),
                money(8000L),
                List.of(component, component)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsBenefit.componentSn must be unique");
    }

    /**
     * 场景：券覆盖全额，用户侧实付为 0。
     * 预期：权益快照可表达零实付，但主资金指令的正金额规则仍不被放宽。
     * 红线：不能把零金额用户付款静默塞进当前主链路。
     */
    @Test
    void testZeroUserPayShouldBeExplicitBenefitBoundary() {
        FundsBenefitSnapshotSpec snapshot = benefitSnapshot("BS-ZERO-PAY-001",
                money(10000L),
                Money.immutable(0L, CURRENCY),
                List.of(platformSubsidyComponent("BC-ZERO-PAY-001", 10000L)));

        assertThat(snapshot.getUserPayAmount()).isEqualTo(Money.immutable(0L, CURRENCY));
        assertThatThrownBy(() -> instruction(snapshot, Money.immutable(0L, CURRENCY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.amount must be positive");
    }

    /**
     * 场景：退款时用户侧不退券，但资金侧需要冲回补贴。
     * 预期：退款策略同时表达用户侧和资金侧处置，并保留业务决策来源。
     * 红线：资金底座不能自行判断券能不能退。
     */
    @Test
    void testRefundPolicyShouldCarryBusinessDecisionReference() {
        FundsBenefitRefundPolicySpec refundPolicy = ImmutableFundsBenefitRefundPolicySpec.builder()
                .partialRefundStrategy(FundsBenefitPartialRefundStrategy.PROPORTIONAL)
                .dispositions(List.of(FundsBenefitRefundDisposition.NO_REFUND,
                        FundsBenefitRefundDisposition.REVERSE_SUBSIDY))
                .refundRuleVersion("platform-refund-v5")
                .refundPolicyCode("NO_COUPON_RETURN_REVERSE_SUBSIDY")
                .refundDecisionId("refund-decision-001")
                .decisionSource("ORDER_REFUND_ENGINE")
                .contextVariables(Map.of())
                .build();

        assertThat(refundPolicy.getDispositions())
                .containsExactly(FundsBenefitRefundDisposition.NO_REFUND,
                        FundsBenefitRefundDisposition.REVERSE_SUBSIDY);
        assertThat(refundPolicy.getRefundDecisionId()).isEqualTo("refund-decision-001");
        assertThat(refundPolicy.getDecisionSource()).isEqualTo("ORDER_REFUND_ENGINE");
    }

    /**
     * 场景：同一业务流水下，权益快照核心字段发生变化。
     * 预期：稳定摘要随快照 ID、组件金额、闭合角色、退款处置或规则版本变化。
     * 红线：后续幂等摘要不能忽略权益快照差异并复用原交易结果。
     */
    @Test
    void testStableDigestShouldChangeWhenBenefitSnapshotCoreFieldsChange() {
        FundsBenefitSnapshotSpec baseSnapshot = merchantDiscountSnapshot(8000L, 2000L);

        assertThat(benefitSnapshot("BS-MERCHANT-002",
                money(10000L),
                money(8000L),
                List.of(merchantDiscountComponent("BC-MERCHANT-001", 2000L))).getStableDigest())
                .isNotEqualTo(baseSnapshot.getStableDigest());
        assertThat(benefitSnapshot("BS-MERCHANT-001",
                money(10000L),
                money(7000L),
                List.of(merchantDiscountComponent("BC-MERCHANT-001", 3000L))).getStableDigest())
                .isNotEqualTo(baseSnapshot.getStableDigest());
        assertThat(benefitSnapshot("BS-MERCHANT-001",
                money(10000L),
                money(8000L),
                List.of(merchantDiscountComponentWithRole("BC-MERCHANT-001",
                        2000L,
                        FundsBenefitAmountClosureRole.VIEW_RECONCILIATION_ONLY),
                        merchantDiscountComponent("BC-MERCHANT-002", 2000L))).getStableDigest())
                .isNotEqualTo(baseSnapshot.getStableDigest());
        assertThat(benefitSnapshot("BS-MERCHANT-001",
                money(10000L),
                money(8000L),
                List.of(merchantDiscountComponentWithRefundPolicy("BC-MERCHANT-001",
                        2000L,
                        merchantRefundPolicyWithDispositions(
                                "merchant-refund-v3",
                                FundsBenefitRefundDisposition.REISSUE)))).getStableDigest())
                .isNotEqualTo(baseSnapshot.getStableDigest());
        assertThat(benefitSnapshot("BS-MERCHANT-001",
                money(10000L),
                money(8000L),
                List.of(merchantDiscountComponentWithReference("BC-MERCHANT-001",
                        2000L,
                        benefitReference("merchant-rule-v4")))).getStableDigest())
                .isNotEqualTo(baseSnapshot.getStableDigest());
    }

    /**
     * 场景：权益快照只存在于请求对象，还没有进入 route snapshot、交易事实或等价不可变存储。
     * 预期：契约层只能给出稳定摘要，不能宣称生产链路已经可回放。
     * 红线：RED-058 作为生产 Done 门禁，不在 B1-10 被越权关闭。
     */
    @Test
    void testRequestBenefitSnapshotShouldOnlyProvideContractDigestBoundary() {
        FundsBenefitSnapshotSpec snapshot = merchantDiscountSnapshot(8000L, 2000L);

        assertThat(snapshot.getStableDigest()).startsWith("sha256:");
        assertThat(snapshot.getStableDigest()).hasSize("sha256:".length() + 64);
    }

    /**
     * 场景：Money JSON 契约已统一为 amount/currency，权益稳定摘要也必须使用同一字段口径。
     * 预期：稳定摘要样本值按 amount 标签生成，而不是沿用已废弃的旧字段标签。
     * 红线：幂等、重放、补录和审计摘要不能继续携带已废弃的 Money 字段语义。
     */
    @Test
    void testStableDigestShouldUseAmountFieldNameForMoneyContract() {
        FundsBenefitSnapshotSpec snapshot = merchantDiscountSnapshot(8000L, 2000L);

        assertThat(snapshot.getStableDigest())
                .isEqualTo("sha256:92ffcd34287906bf304a3e88a94368b19d32a9e75270cb31b371b60555ae345c");
    }

    /**
     * 场景：实现者试图把权益核心金额、规则版本或退款处置放入 contextVariables。
     * 预期：模型构造阶段显式失败。
     * 红线：contextVariables 只能承载非关键扩展信息。
     */
    @Test
    void testBenefitCoreFieldsShouldNotBeHiddenInContextVariables() {
        assertThatThrownBy(() -> ImmutableFundsBenefitSnapshotSpec.builder()
                .benefitSnapshotId("BS-CONTEXT-001")
                .benefitGroupSn("BG-CONTEXT-001")
                .orderAmount(money(10000L))
                .userPayAmount(money(8000L))
                .components(List.of(merchantDiscountComponent("BC-CONTEXT-001", 2000L)))
                .contextVariables(Map.of("orderAmount", money(10000L)))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain core benefit field");
    }

    /**
     * 场景：实现者把当前营销规则、券包或实时优惠计算信息塞入权益引用上下文。
     * 预期：模型构造阶段显式失败。
     * 红线：资金底座只消费原始已决策快照，不调用当前营销规则重算。
     */
    @Test
    void testBenefitReferenceShouldRejectCurrentMarketingRuleInputs() {
        assertThatThrownBy(() -> ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId("campaign-001")
                .couponId("coupon-001")
                .writeOffId("write-off-001")
                .ruleVersion("rule-v1")
                .contextVariables(Map.of("currentMarketingRule", "discount-now"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain core benefit field");
    }

    /**
     * 场景：调用方把当前营销规则藏入嵌套权益上下文。
     * 预期：模型构造阶段递归识别并失败。
     * 红线：实时权益规则不能通过嵌套 contextVariables 绕过资金 DSL 一等字段边界。
     */
    @Test
    void testBenefitContextShouldRejectNestedCurrentMarketingRuleInputs() {
        assertThatThrownBy(() -> ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId("campaign-nested-001")
                .couponId("coupon-nested-001")
                .writeOffId("write-off-nested-001")
                .ruleVersion("rule-v1")
                .contextVariables(Map.of("decisionTrace",
                        Map.of("currentMarketingRule", "discount-now")))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain core benefit field: currentMarketingRule");
    }

    /**
     * 场景：调用方把实时券包信息藏入权益上下文数组。
     * 预期：模型构造阶段递归识别并失败。
     * 红线：集合或数组不能成为权益核心规则字段的旁路。
     */
    @Test
    void testBenefitContextShouldRejectReservedFieldsInsideArrayValues() {
        assertThatThrownBy(() -> ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId("campaign-array-001")
                .couponId("coupon-array-001")
                .writeOffId("write-off-array-001")
                .ruleVersion("rule-v1")
                .contextVariables(Map.of("decisionTraces",
                        new Object[] {Map.of("userCouponBag", "coupon-bag-now")}))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain core benefit field: userCouponBag");
    }

    /**
     * 场景：调用方在权益快照构造后继续改写原始嵌套上下文。
     * 预期：已构造的权益快照保持稳定，不被追加的实时营销规则污染。
     * 红线：权益 DSL 快照不能因浅拷贝把当前规则、券包或重算结果带入资金事实。
     */
    @Test
    void testBenefitSnapshotShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> pricingTrace = new HashMap<>();
        pricingTrace.put("decisionId", "pricing-decision-immutable-001");
        FundsBenefitSnapshotSpec snapshot = ImmutableFundsBenefitSnapshotSpec.builder()
                .benefitSnapshotId("BS-IMMUTABLE-001")
                .benefitGroupSn("BG-IMMUTABLE-001")
                .orderAmount(money(10000L))
                .userPayAmount(money(8000L))
                .components(List.of(merchantDiscountComponent("BC-IMMUTABLE-001", 2000L)))
                .contextVariables(Map.of("pricingTrace", pricingTrace))
                .build();

        pricingTrace.put("currentMarketingRule", "discount-now");

        Object traceValue = snapshot.getContextVariables().get("pricingTrace");
        assertThat(traceValue).isInstanceOf(Map.class);
        Map<?, ?> trace = (Map<?, ?>) traceValue;
        assertThat(trace.get("decisionId")).isEqualTo("pricing-decision-immutable-001");
        assertThat(trace.containsKey("currentMarketingRule")).isFalse();
    }

    /**
     * 场景：调用方在权益引用构造后继续改写原始嵌套上下文。
     * 预期：已构造的权益引用保持稳定，不被追加的实时营销规则污染。
     * 红线：权益引用不能因浅拷贝把当前规则、券包或重算结果带入资金事实。
     */
    @Test
    void testBenefitReferenceShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> decisionTrace = new HashMap<>();
        decisionTrace.put("decisionId", "benefit-reference-immutable-001");
        FundsBenefitReferenceSpec reference = ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId("campaign-immutable-001")
                .couponId("coupon-immutable-001")
                .writeOffId("write-off-immutable-001")
                .ruleVersion("rule-v1")
                .contextVariables(Map.of("decisionTrace", decisionTrace))
                .build();

        decisionTrace.put("currentMarketingRule", "discount-now");

        Object traceValue = reference.getContextVariables().get("decisionTrace");
        assertThat(traceValue).isInstanceOf(Map.class);
        Map<?, ?> trace = (Map<?, ?>) traceValue;
        assertThat(trace.get("decisionId")).isEqualTo("benefit-reference-immutable-001");
        assertThat(trace.containsKey("currentMarketingRule")).isFalse();
    }

    /**
     * 场景：调用方在权益组件构造后继续改写原始嵌套上下文。
     * 预期：已构造的权益组件保持稳定，不被追加的券包或重算结果污染。
     * 红线：权益组件上下文不能成为核心优惠计算输入的旁路。
     */
    @Test
    void testBenefitComponentShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> componentTrace = new HashMap<>();
        componentTrace.put("decisionId", "benefit-component-immutable-001");
        FundsBenefitComponentSpec component = ImmutableFundsBenefitComponentSpec.builder()
                .componentSn("BC-IMMUTABLE-CONTEXT-001")
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .closureRole(FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE)
                .amount(money(2000L))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(subjectRef("MERCHANT-001"))
                .beneficiarySubjectRef(subjectRef("USER-001"))
                .benefitReference(benefitReference())
                .refundPolicy(merchantRefundPolicy())
                .contextVariables(Map.of("componentTrace", componentTrace))
                .build();

        componentTrace.put("userCouponBag", "coupon-bag-now");

        Object traceValue = component.getContextVariables().get("componentTrace");
        assertThat(traceValue).isInstanceOf(Map.class);
        Map<?, ?> trace = (Map<?, ?>) traceValue;
        assertThat(trace.get("decisionId")).isEqualTo("benefit-component-immutable-001");
        assertThat(trace.containsKey("userCouponBag")).isFalse();
    }

    /**
     * 场景：调用方在退款策略构造后继续改写原始嵌套上下文。
     * 预期：已构造的退款策略保持稳定，不被追加的实时退款处置污染。
     * 红线：退款策略上下文不能成为退款规则和处置结果的旁路。
     */
    @Test
    void testBenefitRefundPolicyShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> refundTrace = new HashMap<>();
        refundTrace.put("decisionId", "benefit-refund-immutable-001");
        FundsBenefitRefundPolicySpec refundPolicy = ImmutableFundsBenefitRefundPolicySpec.builder()
                .partialRefundStrategy(FundsBenefitPartialRefundStrategy.PROPORTIONAL)
                .dispositions(List.of(FundsBenefitRefundDisposition.NO_REFUND,
                        FundsBenefitRefundDisposition.REDUCE_MERCHANT_RECEIVABLE))
                .refundRuleVersion("merchant-refund-v5")
                .refundDecisionId("refund-decision-immutable-001")
                .decisionSource("ORDER_REFUND_ENGINE")
                .contextVariables(Map.of("refundTrace", refundTrace))
                .build();

        refundTrace.put("refundDisposition", "REISSUE");

        Object traceValue = refundPolicy.getContextVariables().get("refundTrace");
        assertThat(traceValue).isInstanceOf(Map.class);
        Map<?, ?> trace = (Map<?, ?>) traceValue;
        assertThat(trace.get("decisionId")).isEqualTo("benefit-refund-immutable-001");
        assertThat(trace.containsKey("refundDisposition")).isFalse();
    }

    /**
     * 场景：外部 JSON 解析器把原始权益快照对象结构交给资金 DSL 显式装配。
     * 预期：装配结果保持不可变模型、嵌套引用和退款策略语义，且稳定摘要可用。
     * 红线：测试不能用 fixture 替生产代码隐式转换 Money；原始 JSON money object 必须穿过生产入口。
     */
    @Test
    void testBenefitSnapshotJsonSupportShouldBuildImmutableSnapshotThroughBuilder() {
        JSONObject values = JSON.parseObject("""
                {
                  "benefitSnapshotId": "BS-JSON-001",
                  "benefitSchemaVersion": "1.0",
                  "benefitGroupSn": "BG-JSON-001",
                  "orderSn": "ORDER-JSON-001",
                  "pricingSnapshotSn": "PRICE-JSON-001",
                  "orderAmount": { "currency": "USD", "amount": 10000 },
                  "userPayAmount": { "currency": "USD", "amount": 8000 },
                  "merchantReceivableAmount": { "currency": "USD", "amount": 8000 },
                  "components": [{
                    "componentSn": "BC-JSON-001",
                    "sequence": 1,
                    "benefitType": "MERCHANT_COUPON",
                    "componentType": "MERCHANT_DISCOUNT",
                    "closureRole": "ORDER_DISCOUNT_CLOSURE",
                    "amount": { "currency": "USD", "amount": 2000 },
                    "ledgerEffect": "NO_LEDGER",
                    "fundingNature": "MERCHANT_BORNE",
                    "bearerSubjectRef": {
                      "tenantId": 1001,
                      "subjectId": "FA-MERCHANT-JSON-001",
                      "subjectType": "FUNDING_ACCOUNT",
                      "subjectName": "Merchant JSON",
                      "currency": "USD",
                      "ledgerProfileCode": "MERCHANT_PAYABLE"
                    },
                    "benefitReference": {
                      "couponId": "COUPON-JSON-001",
                      "writeOffId": "WRITE-OFF-JSON-001",
                      "ruleVersion": "merchant-rule-json-v1",
                      "contextVariables": {}
                    },
                    "refundPolicy": {
                      "partialRefundStrategy": "PROPORTIONAL",
                      "dispositions": ["NO_REFUND", "REDUCE_MERCHANT_RECEIVABLE"],
                      "refundRuleVersion": "merchant-refund-json-v1",
                      "refundDecisionId": "REFUND-DECISION-JSON-001",
                      "decisionSource": "ORDER_REFUND_ENGINE",
                      "decisionTime": "2026-05-21T10:00:00",
                      "contextVariables": {}
                    },
                    "description": "merchant coupon",
                    "contextVariables": {}
                  }],
                  "refundPolicy": {
                    "partialRefundStrategy": "PROPORTIONAL",
                    "dispositions": ["NO_REFUND", "REDUCE_MERCHANT_RECEIVABLE"],
                    "refundRuleVersion": "snapshot-refund-json-v1",
                    "refundDecisionId": "SNAPSHOT-REFUND-JSON-001",
                    "decisionSource": "ORDER_REFUND_ENGINE",
                    "contextVariables": {}
                  },
                  "decisionSource": "ORDER_PRICING",
                  "decisionTraceId": "TRACE-JSON-001",
                  "contextVariables": {}
                }
                """);

        FundsBenefitSnapshotSpec snapshot = FundsBenefitSnapshotJsonSupport.parseSnapshot(values);

        assertThat(snapshot).isInstanceOf(ImmutableFundsBenefitSnapshotSpec.class);
        assertThat(snapshot.getBenefitSnapshotId()).isEqualTo("BS-JSON-001");
        assertThat(snapshot.getOrderAmount()).isEqualTo(money(10000L));
        assertThat(snapshot.getUserPayAmount()).isEqualTo(money(8000L));
        assertThat(snapshot.getComponents()).hasSize(1);
        assertThat(snapshot.getComponents().getFirst()).isInstanceOf(ImmutableFundsBenefitComponentSpec.class);
        assertThat(snapshot.getComponents().getFirst().getBearerSubjectRef().getSubjectType())
                .isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(snapshot.getComponents().getFirst().getBearerSubjectRef().getSubjectId())
                .isEqualTo("FA-MERCHANT-JSON-001");
        assertThat(snapshot.getComponents().getFirst().getBenefitReference().getRuleVersion())
                .isEqualTo("merchant-rule-json-v1");
        assertThat(snapshot.getComponents().getFirst().getRefundPolicy().getDispositions())
                .containsExactly(FundsBenefitRefundDisposition.NO_REFUND,
                        FundsBenefitRefundDisposition.REDUCE_MERCHANT_RECEIVABLE);
        assertThat(snapshot.getRefundPolicy().getRefundDecisionId()).isEqualTo("SNAPSHOT-REFUND-JSON-001");
        assertThat(snapshot.getStableDigest()).startsWith("sha256:");
    }

    /**
     * 场景：外部 JSON 表达全额优惠，用户侧实付为 0。
     * 预期：显式装配允许权益快照中的零实付，同时组件金额仍按正金额校验。
     * 红线：Map 装配路径不能比不可变 Builder 路径更严格。
     */
    @Test
    void testBenefitSnapshotJsonSupportShouldAllowZeroUserPayAmount() {
        JSONObject values = JSON.parseObject("""
                {
                  "benefitSnapshotId": "BS-JSON-ZERO-PAY-001",
                  "benefitGroupSn": "BG-JSON-ZERO-PAY-001",
                  "orderAmount": { "currency": "USD", "amount": 10000 },
                  "userPayAmount": { "currency": "USD", "amount": 0 },
                  "components": [{
                    "componentSn": "BC-JSON-ZERO-PAY-001",
                    "benefitType": "PLATFORM_COUPON",
                    "componentType": "PLATFORM_SUBSIDY",
                    "closureRole": "ORDER_DISCOUNT_CLOSURE",
                    "amount": { "currency": "USD", "amount": 10000 },
                    "ledgerEffect": "POSTING_REQUIRED",
                    "fundingNature": "PLATFORM_OWN_FUNDS",
                    "fundingAccountRole": "PLATFORM_SUBSIDY_COST",
                    "benefitReference": { "couponId": "COUPON-JSON-ZERO-PAY-001", "ruleVersion": "rule-v1" },
                    "contextVariables": {}
                  }],
                  "contextVariables": {}
                }
                """);

        FundsBenefitSnapshotSpec snapshot = FundsBenefitSnapshotJsonSupport.parseSnapshot(values);

        assertThat(snapshot.getUserPayAmount()).isEqualTo(Money.immutable(0L, CURRENCY));
        assertThat(snapshot.getComponents().getFirst().getAmount()).isEqualTo(money(10000L));
    }

    /**
     * 场景：外部 JSON 把允许为 0 的用户实付金额误写成主单位 value 形态。
     * 预期：显式装配入口必须拒绝缺少 amount 的 Money 对象，而不是把 long 默认值当作 0。
     * 红线：JSON.to 绑定不能把错误金额字段静默转换成零金额并绕过权益金额闭合。
     */
    @Test
    void testBenefitSnapshotJsonSupportShouldRejectMajorUnitMoneyShapeForZeroAllowedAmount() {
        JSONObject values = JSON.parseObject("""
                {
                  "benefitSnapshotId": "BS-JSON-MAJOR-UNIT-001",
                  "benefitGroupSn": "BG-JSON-MAJOR-UNIT-001",
                  "orderAmount": { "currency": "USD", "amount": 10000 },
                  "userPayAmount": { "currency": "USD", "value": "0.00" },
                  "components": [{
                    "componentSn": "BC-JSON-MAJOR-UNIT-001",
                    "benefitType": "PLATFORM_COUPON",
                    "componentType": "PLATFORM_SUBSIDY",
                    "closureRole": "ORDER_DISCOUNT_CLOSURE",
                    "amount": { "currency": "USD", "amount": 10000 },
                    "ledgerEffect": "POSTING_REQUIRED",
                    "fundingNature": "PLATFORM_OWN_FUNDS",
                    "fundingAccountRole": "PLATFORM_SUBSIDY_COST",
                    "benefitReference": { "couponId": "COUPON-JSON-MAJOR-UNIT-001", "ruleVersion": "rule-v1" },
                    "contextVariables": {}
                  }],
                  "contextVariables": {}
                }
                """);

        assertThatThrownBy(() -> FundsBenefitSnapshotJsonSupport.parseSnapshot(values))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userPayAmount.amount is required");
    }

    /**
     * 场景：外部 JSON 的权益金额不闭合。
     * 预期：显式装配最终仍由不可变 Builder 拒绝。
     * 红线：反序列化路径不能绕过资金 DSL 的金额闭合不变量。
     */
    @Test
    void testBenefitSnapshotJsonSupportShouldRejectUnclosedAmountThroughBuilder() {
        JSONObject values = JSON.parseObject("""
                {
                  "benefitSnapshotId": "BS-JSON-MISMATCH-001",
                  "benefitGroupSn": "BG-JSON-MISMATCH-001",
                  "orderAmount": { "currency": "USD", "amount": 10000 },
                  "userPayAmount": { "currency": "USD", "amount": 9000 },
                  "components": [{
                    "componentSn": "BC-JSON-MISMATCH-001",
                    "benefitType": "MERCHANT_COUPON",
                    "componentType": "MERCHANT_DISCOUNT",
                    "closureRole": "ORDER_DISCOUNT_CLOSURE",
                    "amount": { "currency": "USD", "amount": 2000 },
                    "ledgerEffect": "NO_LEDGER",
                    "fundingNature": "MERCHANT_BORNE",
                    "bearerSubjectRef": {
                      "subjectId": "FA-MERCHANT-JSON-001",
                      "subjectType": "FUNDING_ACCOUNT"
                    },
                    "benefitReference": { "couponId": "COUPON-JSON-001", "ruleVersion": "rule-v1" },
                    "contextVariables": {}
                  }],
                  "contextVariables": {}
                }
                """);

        assertThatThrownBy(() -> FundsBenefitSnapshotJsonSupport.parseSnapshot(values))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDER_DISCOUNT_CLOSURE components.amount = orderAmount");
    }

    /**
     * 场景：调用方把原始 JSON 对象直接交给权益快照 Map 装配入口。
     * 预期：装配入口按 Money 自身 JSON 绑定语义转换金额对象，并继续执行金额闭合不变量。
     * 红线：测试不得用 fixture 预处理掩盖生产入口的 Money 绑定边界。
     */
    @Test
    void testBenefitSnapshotJsonSupportShouldBindRawJsonMoneyObject() {
        JSONObject values = JSON.parseObject("""
                {
                  "benefitSnapshotId": "BS-JSON-RAW-MONEY-001",
                  "benefitGroupSn": "BG-JSON-RAW-MONEY-001",
                  "orderAmount": { "currency": "USD", "amount": 10000 },
                  "userPayAmount": { "currency": "USD", "amount": 8000 },
                  "components": [{
                    "componentSn": "BC-JSON-RAW-MONEY-001",
                    "benefitType": "MERCHANT_COUPON",
                    "componentType": "MERCHANT_DISCOUNT",
                    "closureRole": "ORDER_DISCOUNT_CLOSURE",
                    "amount": { "currency": "USD", "amount": 2000 },
                    "ledgerEffect": "NO_LEDGER",
                    "fundingNature": "MERCHANT_BORNE",
                    "bearerSubjectRef": {
                      "subjectId": "FA-MERCHANT-JSON-001",
                      "subjectType": "FUNDING_ACCOUNT"
                    },
                    "benefitReference": { "couponId": "COUPON-JSON-001", "ruleVersion": "rule-v1" },
                    "contextVariables": {}
                  }],
                  "contextVariables": {}
                }
                """);

        FundsBenefitSnapshotSpec snapshot = FundsBenefitSnapshotJsonSupport.parseSnapshot(values);

        assertThat(snapshot.getOrderAmount()).isEqualTo(money(10000L));
        assertThat(snapshot.getUserPayAmount()).isEqualTo(money(8000L));
        assertThat(snapshot.getComponents().getFirst().getAmount()).isEqualTo(money(2000L));
    }

    private FundsBenefitSnapshotSpec merchantDiscountSnapshot(long userPayAmount, long componentAmount) {
        return benefitSnapshot("BS-MERCHANT-001",
                money(10000L),
                money(userPayAmount),
                List.of(merchantDiscountComponent("BC-MERCHANT-001", componentAmount)));
    }

    private FundsBenefitSnapshotSpec benefitSnapshot(String benefitSnapshotId,
                                                     Money orderAmount,
                                                     Money userPayAmount,
                                                     List<FundsBenefitComponentSpec> components) {
        return ImmutableFundsBenefitSnapshotSpec.builder()
                .benefitSnapshotId(benefitSnapshotId)
                .benefitGroupSn("BG-ORDER-001")
                .orderSn("ORDER-001")
                .pricingSnapshotSn("PRICE-001")
                .orderAmount(orderAmount)
                .userPayAmount(userPayAmount)
                .merchantReceivableAmount(userPayAmount)
                .components(components)
                .decisionSource("ORDER_PRICING")
                .decisionTraceId("TRACE-ORDER-001")
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitComponentSpec merchantDiscountComponent(String componentSn, long amount) {
        return merchantDiscountComponentWithRefundPolicy(componentSn, amount, merchantRefundPolicy());
    }

    private FundsBenefitComponentSpec merchantDiscountComponentWithReference(String componentSn,
                                                                            long amount,
                                                                            FundsBenefitReferenceSpec reference) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn(componentSn)
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .closureRole(FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE)
                .amount(money(amount))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(subjectRef("MERCHANT-001"))
                .beneficiarySubjectRef(subjectRef("USER-001"))
                .benefitReference(reference)
                .refundPolicy(merchantRefundPolicy())
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitComponentSpec merchantDiscountComponentWithRefundPolicy(String componentSn,
                                                                               long amount,
                                                                               FundsBenefitRefundPolicySpec refundPolicy) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn(componentSn)
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .closureRole(FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE)
                .amount(money(amount))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(subjectRef("MERCHANT-001"))
                .beneficiarySubjectRef(subjectRef("USER-001"))
                .benefitReference(benefitReference())
                .refundPolicy(refundPolicy)
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitComponentSpec merchantDiscountComponentWithRole(String componentSn,
                                                                       long amount,
                                                                       FundsBenefitAmountClosureRole closureRole) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn(componentSn)
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .closureRole(closureRole)
                .amount(money(amount))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(subjectRef("MERCHANT-001"))
                .beneficiarySubjectRef(subjectRef("USER-001"))
                .benefitReference(benefitReference())
                .refundPolicy(merchantRefundPolicy())
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitComponentSpec platformSubsidyComponent(String componentSn, long amount) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn(componentSn)
                .sequence(1)
                .benefitType(FundsBenefitType.PLATFORM_COUPON)
                .componentType(FundsBenefitComponentType.PLATFORM_SUBSIDY)
                .closureRole(FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE)
                .amount(money(amount))
                .ledgerEffect(FundsBenefitLedgerEffect.POSTING_REQUIRED)
                .fundingNature(FundsBenefitFundingNature.PLATFORM_OWN_FUNDS)
                .fundingAccountRole("PLATFORM_SUBSIDY_COST")
                .bearerSubjectRef(subjectRef("PLATFORM-001"))
                .beneficiarySubjectRef(subjectRef("MERCHANT-001"))
                .benefitReference(benefitReference())
                .refundPolicy(platformRefundPolicy())
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitComponentSpec merchantReceivableEffectComponent(String componentSn, long amount) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn(componentSn)
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .closureRole(FundsBenefitAmountClosureRole.MERCHANT_RECEIVABLE_EFFECT)
                .amount(money(amount))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(subjectRef("MERCHANT-001"))
                .beneficiarySubjectRef(subjectRef("USER-001"))
                .benefitReference(benefitReference())
                .refundPolicy(merchantRefundPolicy())
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitReferenceSpec benefitReference() {
        return benefitReference("merchant-rule-v3");
    }

    private FundsBenefitReferenceSpec benefitReference(String ruleVersion) {
        return ImmutableFundsBenefitReferenceSpec.builder()
                .campaignId("campaign-001")
                .couponId("coupon-001")
                .writeOffId("write-off-001")
                .ruleVersion(ruleVersion)
                .externalDecisionId("pricing-decision-001")
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitRefundPolicySpec merchantRefundPolicy() {
        return merchantRefundPolicyWithDispositions("merchant-refund-v3",
                FundsBenefitRefundDisposition.NO_REFUND,
                FundsBenefitRefundDisposition.REDUCE_MERCHANT_RECEIVABLE);
    }

    private FundsBenefitRefundPolicySpec merchantRefundPolicyWithDispositions(String ruleVersion,
                                                                             FundsBenefitRefundDisposition... dispositions) {
        return ImmutableFundsBenefitRefundPolicySpec.builder()
                .partialRefundStrategy(FundsBenefitPartialRefundStrategy.ITEM_LINE_BASED)
                .dispositions(List.of(dispositions))
                .refundRuleVersion(ruleVersion)
                .refundPolicyCode("MERCHANT_COUPON_NO_RETURN")
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitRefundPolicySpec platformRefundPolicy() {
        return ImmutableFundsBenefitRefundPolicySpec.builder()
                .partialRefundStrategy(FundsBenefitPartialRefundStrategy.PROPORTIONAL)
                .dispositions(List.of(FundsBenefitRefundDisposition.NO_REFUND,
                        FundsBenefitRefundDisposition.REVERSE_SUBSIDY))
                .refundRuleVersion("platform-refund-v5")
                .refundDecisionId("refund-decision-platform-001")
                .decisionSource("ORDER_REFUND_ENGINE")
                .contextVariables(Map.of())
                .build();
    }

    private FundsInstructionSpec instruction(FundsBenefitSnapshotSpec snapshot, Money amount) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount)
                .originalAmount(amount)
                .exchangeRate(BigDecimal.ONE)
                .benefitSnapshot(snapshot)
                .businessScene("BENEFIT_DSL")
                .businessSn("BIZ-BENEFIT-001")
                .eventTime(EVENT_TIME)
                .operator(operator())
                .contextVariables(Map.of())
                .build();
    }

    private FundsOperationActorSpec operator() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(1L)
                .operatorType("SYSTEM")
                .operatorName("Codex")
                .appName("wind-funds-tests")
                .contextVariables(Map.of())
                .build();
    }

    private SubjectRef subjectRef(String subjectId) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .currency(CURRENCY.name())
                .build();
    }

    private Money money(long amount) {
        return Money.immutable(amount, CURRENCY);
    }

}

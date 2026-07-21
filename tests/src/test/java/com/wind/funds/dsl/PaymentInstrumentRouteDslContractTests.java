package com.wind.funds.dsl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableAccountHierarchyFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.model.route.ImmutableRouteLegSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PaymentInstrument Route DSL 契约测试。
 */
class PaymentInstrumentRouteDslContractTests {

    /**
     * 场景：支付工具付款已完成路由解析。
     * 预期：工具和外部账户只能作为快照引用，route leg 的账务节点只能是内部可记账主体。
     * 红线：支付工具 ID、外部账户或通道 token 不得被写成 LedgerEntry 主体。
     */
    @Test
    void testPaymentInstrumentAndExternalAccountShouldNotBecomeLedgerRouteNodes() {
        SubjectRef payer = fundingAccount("FA-PAYER-001");
        SubjectRef payee = fundingAccount("FA-PAYEE-001");
        RouteLegSpec leg = routeLeg(payer, payee);
        PaymentInstrumentRefSpec instrumentRef = paymentInstrumentRef("PI-001", "**** 4242");
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-001", "acct_****_0422");

        assertThat(leg.getSourceNode().getNodeType()).isEqualTo(RouteNodeType.SUBJECT);
        assertThat(leg.getTargetNode().getNodeType()).isEqualTo(RouteNodeType.SUBJECT);
        assertThat(leg.getSourceNode().getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(leg.getTargetNode().getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(instrumentRef.getInstrumentId()).isEqualTo("PI-001");
        assertThat(externalAccountRef.getExternalAccountId()).isEqualTo("EA-001");
    }

    /**
     * 场景：支付工具、绑定关系和资金来源共同决定 route。
     * 预期：RoutingDecision 必须保留命中规则、资金来源、优先级和选择原因。
     * 红线：缺资金来源或选择原因的 route snapshot 不能解释后续回放和审计。
     */
    @Test
    void testRoutingDecisionShouldRecordFundingAllocationPriorityAndReason() {
        FundingAllocationDecisionSpec allocation = fundingAllocation("ALLOC-001",
                fundingAccount("FA-PAYER-001"),
                LedgerSubjectCode.AVAILABLE,
                10,
                "DEFAULT_PAYMENT_INSTRUMENT");

        RoutingDecisionSpec decision = ImmutableRoutingDecisionSpec.builder()
                .policyCode("PAYMENT_INSTRUMENT_ROUTE")
                .matchedRules(List.of("INSTRUMENT_ACTIVE", "DIRECTION_PAY", "UNIQUE_FUNDING_SOURCE"))
                .selectedProcessor("CARD_PROCESSOR")
                .fundingAllocations(List.of(allocation))
                .decisionReason("ACTIVE_CARD_WITH_DEFAULT_FUNDING_ACCOUNT")
                .contextVariables(Map.of("bindingVersion", 3))
                .build();

        assertThat(decision.getPolicyCode()).isEqualTo("PAYMENT_INSTRUMENT_ROUTE");
        assertThat(decision.getMatchedRules()).containsExactly("INSTRUMENT_ACTIVE", "DIRECTION_PAY", "UNIQUE_FUNDING_SOURCE");
        assertThat(decision.getDecisionReason()).isEqualTo("ACTIVE_CARD_WITH_DEFAULT_FUNDING_ACCOUNT");
        assertThat(decision.getFundingAllocations()).singleElement().satisfies(item -> {
            assertThat(item.getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
            assertThat(item.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(item.getPriority()).isEqualTo(10);
            assertThat(item.getReason()).isEqualTo("DEFAULT_PAYMENT_INSTRUMENT");
        });
    }

    /**
     * 场景：授权组合场景使用资金账户、共享卡 + 资金账户、共享卡 + 支出控制范围 + 资金账户三种模型。
     * 预期：RoutingDecision 能分别表达真实资金来源、工具快照和预算额度控制维度。
     * 红线：共享卡不得替代真实资金账户；支出控制范围不得成为唯一真实资金来源。
     */
    @Test
    void testRoutingDecisionShouldCoverRequiredFundingSourceModels() {
        RoutingDecisionSpec fundingAccountOnly = routingDecision("FUNDING_ACCOUNT_ONLY",
                List.of(fundingAllocation("ALLOC-FA",
                        fundingAccount("FA-AUTH-001"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "REAL_FUNDING_ACCOUNT")));
        PaymentInstrumentRefSpec sharedCard = paymentInstrumentRef("PI-SHARED-001",
                "**** 1888",
                Map.of("bindingRole", "SHARED_CARD", "bindingVersion", 5));
        RoutingDecisionSpec sharedCardFundingAccount = routingDecision("SHARED_CARD_FUNDING_ACCOUNT",
                List.of(fundingAllocation("ALLOC-SHARED-FA",
                        fundingAccount("FA-AUTH-002"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "SHARED_CARD_REAL_FUNDING_ACCOUNT")));
        assertThat(fundingAccountOnly.getFundingAllocations())
                .extracting(item -> item.getSubjectRef().getSubjectType())
                .containsExactly(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(sharedCard.getBindingSnapshot()).containsEntry("bindingRole", "SHARED_CARD");
        assertThat(sharedCardFundingAccount.getFundingAllocations())
                .extracting(item -> item.getSubjectRef().getSubjectType())
                .containsExactly(FundsSubjectType.FUNDING_ACCOUNT);
    }

    /**
     * 场景：VCC 共享卡授权时，支付工具先解析到信用子账户。
     * 预期：资金来源决策保留卡绑定快照，并固化账户层级快照，实际落账主体仍是信用账户。
     * 红线：VCC 卡不得成为账本主体；回放、账单和责任归因必须依赖当次账户层级快照。
     */
    @Test
    void testVccSharedCardShouldResolveToCreditSubAccountWithHierarchySnapshot() {
        PaymentInstrumentRefSpec sharedCard = paymentInstrumentRef("PI-VCC-SHARED-001",
                "**** 1888",
                Map.of("bindingRole", "VCC_SHARED_CARD", "bindingVersion", 7));
        SubjectRef parentAccount = fundingAccount("FA-VCC-PARENT-001");
        SubjectRef cardCreditAccount = creditAccount("CA-VCC-CARD-001");
        AccountHierarchySnapshotSpec hierarchySnapshot = accountHierarchySnapshot(cardCreditAccount,
                parentAccount,
                Map.of("cardBindingVersion", 7, "accountPurpose", "VCC_SHARED_CARD"));
        FundingAllocationDecisionSpec allocation = fundingAllocation("ALLOC-VCC-SHARED",
                cardCreditAccount,
                LedgerSubjectCode.AUTHORIZATION,
                10,
                "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT",
                hierarchySnapshot);

        assertThat(sharedCard.getBindingSnapshot()).containsEntry("bindingRole", "VCC_SHARED_CARD");
        assertThat(allocation.getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
        assertThat(allocation.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AUTHORIZATION);
        assertThat(allocation.getAccountHierarchySnapshot()).isSameAs(hierarchySnapshot);
        assertThat(hierarchySnapshot.getAccountRef()).isSameAs(cardCreditAccount);
        assertThat(hierarchySnapshot.getParentAccountRef()).isSameAs(parentAccount);
        assertThat(hierarchySnapshot.getContextVariables()).containsEntry("cardBindingVersion", 7);
    }

    /**
     * 场景：VCC 共享卡授权生成 route snapshot 后进入归档、重放或交易投影链路。
     * 预期：快照 JSON 保留支付工具引用、资金来源决策和账户层级快照。
     * 红线：route snapshot 不得把 VCC 卡当账本主体，也不得泄露完整卡号等敏感值。
     */
    @Test
    void testVccSharedCardRouteSnapshotJsonShouldCarryAccountHierarchySnapshot() {
        PaymentInstrumentRefSpec sharedCard = paymentInstrumentRef("PI-VCC-SHARED-002",
                "**** 2999",
                Map.of("bindingRole", "VCC_SHARED_CARD", "bindingVersion", 9));
        SubjectRef parentAccount = fundingAccount("FA-VCC-PARENT-004");
        SubjectRef cardCreditAccount = creditAccount("CA-VCC-CARD-004");
        AccountHierarchySnapshotSpec hierarchySnapshot = accountHierarchySnapshot(cardCreditAccount,
                parentAccount,
                Map.of("cardBindingVersion", 9, "accountPurpose", "VCC_SHARED_CARD"));
        FundingAllocationDecisionSpec allocation = fundingAllocation("ALLOC-VCC-SHARED-JSON",
                cardCreditAccount,
                LedgerSubjectCode.AUTHORIZATION,
                10,
                "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT",
                hierarchySnapshot);
        RouteSnapshotSpec snapshot = ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("RS-VCC-SHARED-JSON-001")
                .snapshotSchemaVersion("route-snapshot-v1")
                .routeCode("VCC_SHARED_CARD_AUTH")
                .routeVersion("v1")
                .businessScene("VCC_AUTHORIZATION")
                .businessSn("AUTH-VCC-SHARED-JSON-001")
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTHORIZE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of())
                .legs(List.of(routeLeg(cardCreditAccount, fundingAccount("FA-MERCHANT-JSON-001"))))
                .routingDecision(routingDecision("VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT", List.of(allocation)))
                .paymentInstrumentRef(sharedCard)
                .resolvedAt(LocalDateTime.of(2026, 6, 12, 10, 0))
                .contextVariables(Map.of("fixtureLevel", "CONTRACT_ONLY"))
                .build();

        JSONObject document = JSON.parseObject(JSON.toJSONString(snapshot));
        JSONObject serializedAllocation = document.getJSONObject("routingDecision")
                .getJSONArray("fundingAllocations")
                .getJSONObject(0);
        JSONObject serializedHierarchy = serializedAllocation.getJSONObject("accountHierarchySnapshot");

        assertThat(document.getJSONObject("paymentInstrumentRef").getString("instrumentId"))
                .isEqualTo("PI-VCC-SHARED-002");
        assertThat(serializedAllocation.getJSONObject("subjectRef").getString("subjectType"))
                .isEqualTo(FundsSubjectType.CREDIT_ACCOUNT.name());
        assertThat(serializedHierarchy.getJSONObject("accountRef").getString("subjectId"))
                .isEqualTo("CA-VCC-CARD-004");
        assertThat(serializedHierarchy.getJSONObject("parentAccountRef").getString("subjectId"))
                .isEqualTo("FA-VCC-PARENT-004");
        assertThat(serializedHierarchy).doesNotContainKey("hierarchyVersion");
        assertThat(serializedHierarchy.getJSONObject("contextVariables").getString("accountPurpose"))
                .isEqualTo("VCC_SHARED_CARD");
        assertThat(document.toJSONString()).doesNotContain("4242424242424242");
    }

    /**
     * 场景：业务侧把实际落账账户也填成 parent。
     * 预期：账户层级快照构造期拒绝自引用。
     * 红线：父账户必须表达直接上级约束，不能指回子账户本身，否则父子汇总和防双算失真。
     */
    @Test
    void testAccountHierarchySnapshotShouldRejectSelfParentAccountRef() {
        SubjectRef cardCreditAccount = creditAccount("CA-VCC-CARD-SELF-001");

        assertThatThrownBy(() -> accountHierarchySnapshot(cardCreditAccount,
                cardCreditAccount,
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent account must not reference account itself");
    }

    /**
     * 场景：资金来源决策挂载了不匹配的账户层级快照。
     * 预期：FundingAllocation 构造期拒绝。
     * 红线：路由回放不能出现 funding allocation 指向一个账户、层级快照指向另一个账户。
     */
    @Test
    void testFundingAllocationShouldRejectMismatchedAccountHierarchySnapshot() {
        AccountHierarchySnapshotSpec hierarchySnapshot = accountHierarchySnapshot(creditAccount("CA-VCC-CARD-002"),
                fundingAccount("FA-VCC-PARENT-003"),
                Map.of());

        assertThatThrownBy(() -> fundingAllocation("ALLOC-VCC-MISMATCH",
                creditAccount("CA-VCC-CARD-003"),
                LedgerSubjectCode.AUTHORIZATION,
                10,
                "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT",
                hierarchySnapshot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountHierarchySnapshot accountRef must match funding allocation subjectRef");
    }

    /**
     * 场景：资金来源决策主体和账户层级快照主体 ID 一致，但币种不同。
     * 预期：FundingAllocation 构造期拒绝。
     * 红线：同一个资金责任主体不能在 route snapshot 中被记录成不同币种，否则回放和余额投影会跨币种归因。
     */
    @Test
    void testFundingAllocationShouldRejectAccountHierarchySnapshotCurrencyMismatch() {
        SubjectRef allocationSubject = accountSubject("CA-VCC-CARD-CURRENCY-001",
                FundsSubjectType.CREDIT_ACCOUNT,
                1L,
                CurrencyIsoCode.USD.name());
        SubjectRef snapshotAccount = accountSubject("CA-VCC-CARD-CURRENCY-001",
                FundsSubjectType.CREDIT_ACCOUNT,
                1L,
                CurrencyIsoCode.EUR.name());
        AccountHierarchySnapshotSpec hierarchySnapshot = accountHierarchySnapshot(snapshotAccount,
                accountSubject("FA-VCC-PARENT-CURRENCY-001",
                        FundsSubjectType.FUNDING_ACCOUNT,
                        1L,
                        CurrencyIsoCode.EUR.name()),
                Map.of());

        assertThatThrownBy(() -> fundingAllocation("ALLOC-VCC-CURRENCY-MISMATCH",
                allocationSubject,
                LedgerSubjectCode.AUTHORIZATION,
                10,
                "VCC_SHARED_CARD_CREDIT_SUB_ACCOUNT",
                hierarchySnapshot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountHierarchySnapshot accountRef currency must match funding allocation subjectRef currency");
    }

    /**
     * 场景：资金来源决策主体币种和 allocation 金额币种不一致。
     * 预期：FundingAllocation 构造期拒绝。
     * 红线：资金来源决策是“某个资金主体承担某笔金额”，主体币种和金额币种不能分叉。
     */
    @Test
    void testFundingAllocationShouldRejectSubjectAmountCurrencyMismatch() {
        SubjectRef eurFundingAccount = accountSubject("FA-VCC-FUNDING-CURRENCY-001",
                FundsSubjectType.FUNDING_ACCOUNT,
                1L,
                CurrencyIsoCode.EUR.name());

        assertThatThrownBy(() -> fundingAllocation("ALLOC-VCC-AMOUNT-CURRENCY-MISMATCH",
                eurFundingAccount,
                LedgerSubjectCode.AVAILABLE,
                Money.immutable(100L, CurrencyIsoCode.USD),
                10,
                "SUBJECT_AMOUNT_CURRENCY_MISMATCH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation amount currency must match subjectRef currency");
    }

    /**
     * 场景：VCC 子账户 allocation 的主体和账户层级快照均为 EUR，但 allocation 金额为 USD。
     * 预期：FundingAllocation 构造期拒绝。
     * 红线：带层级快照的资金责任不能只校验主体快照一致，还必须校验金额币种和主体币种一致。
     */
    @Test
    void testAccountHierarchyFundingAllocationShouldRejectSubjectAmountCurrencyMismatch() {
        SubjectRef eurCardAccount = accountSubject("CA-VCC-CARD-AMOUNT-CURRENCY-001",
                FundsSubjectType.CREDIT_ACCOUNT,
                1L,
                CurrencyIsoCode.EUR.name());
        SubjectRef eurParentAccount = accountSubject("FA-VCC-PARENT-AMOUNT-CURRENCY-001",
                FundsSubjectType.FUNDING_ACCOUNT,
                1L,
                CurrencyIsoCode.EUR.name());
        AccountHierarchySnapshotSpec hierarchySnapshot = accountHierarchySnapshot(eurCardAccount,
                eurParentAccount,
                Map.of());

        assertThatThrownBy(() -> ImmutableAccountHierarchyFundingAllocationDecisionSpec.builder()
                .allocationId("ALLOC-VCC-HIERARCHY-AMOUNT-CURRENCY-MISMATCH")
                .subjectRef(eurCardAccount)
                .ledgerSubjectCode(LedgerSubjectCode.AUTHORIZATION)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .accountHierarchySnapshot(hierarchySnapshot)
                .priority(10)
                .reason("HIERARCHY_SUBJECT_AMOUNT_CURRENCY_MISMATCH")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation amount currency must match subjectRef currency");
    }

    /**
     * 场景：支付工具命中多条资金来源规则但没有确定资金来源。
     * 预期：RoutingDecision 构造期拒绝缺失资金来源。
     * 红线：缺资金来源仍生成 route 会让后续回放和审计无法解释。
     */
    @Test
    void testRoutingDecisionShouldRejectMissingFundingAllocation() {
        assertThatThrownBy(() -> routingDecision("MISSING_FUNDING_SOURCE", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundingAllocations must not be empty");
    }

    /**
     * 场景：支付工具路由决策上下文被调用方塞入通道密钥或支付工具原文。
     * 预期：RoutingDecision 构造期立即拒绝。
     * 红线：路由决策会进入 route snapshot、归档重放和审计链路，不能保存 PAN、CVV、密钥或外部账户原文。
     */
    @Test
    void testRoutingDecisionContextVariablesShouldRejectSensitiveValues() {
        assertThatThrownBy(() -> routingDecision("SENSITIVE_ROUTING_CONTEXT",
                List.of(fundingAllocation("ALLOC-SENSITIVE-CONTEXT",
                        fundingAccount("FA-SENSITIVE-CONTEXT"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "REAL_FUNDING_ACCOUNT")),
                Map.of("processorPayload", Map.of("secretKey", "secret-value"))))
                .hasMessageContaining("routingDecision.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：调用方在 RoutingDecision 构造后继续改写原始嵌套上下文。
     * 预期：已构造的路由决策保持稳定，不被追加的支付工具原文污染。
     * 红线：路由决策不能因浅拷贝绕过敏感字段校验并污染后续 route snapshot。
     */
    @Test
    void testRoutingDecisionShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:route-decision-001");
        RoutingDecisionSpec decision = routingDecision("IMMUTABLE_ROUTING_CONTEXT",
                List.of(fundingAllocation("ALLOC-IMMUTABLE-CONTEXT",
                        fundingAccount("FA-IMMUTABLE-CONTEXT"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "REAL_FUNDING_ACCOUNT")),
                Map.of("processorPayload", processorPayload));

        processorPayload.put("pan", "4242424242424242");

        Object payloadValue = decision.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:route-decision-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：支付工具资金来源存在多个候选，但优先级缺失或冲突。
     * 预期：FundingAllocation 必须有确定优先级，RoutingDecision 不允许重复优先级。
     * 红线：多来源命中不得随机选路。
     */
    @Test
    void testRoutingDecisionShouldRejectMissingOrDuplicateFundingPriority() {
        assertThatThrownBy(() -> fundingAllocation("ALLOC-NO-PRIORITY",
                fundingAccount("FA-NO-PRIORITY"),
                LedgerSubjectCode.AVAILABLE,
                null,
                "REAL_FUNDING_ACCOUNT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation priority is required");

        assertThatThrownBy(() -> routingDecision("DUPLICATE_PRIORITY",
                List.of(fundingAllocation("ALLOC-FA-01",
                                fundingAccount("FA-DUP-001"),
                                LedgerSubjectCode.AVAILABLE,
                                10,
                                "REAL_FUNDING_ACCOUNT"),
                        fundingAllocation("ALLOC-FA-02",
                                fundingAccount("FA-DUP-002"),
                                LedgerSubjectCode.AVAILABLE,
                                10,
                                "REAL_FUNDING_ACCOUNT"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation priority must be unique");
    }

    /**
     * 场景：资金来源决策只有主体和金额，没有选择原因。
     * 预期：FundingAllocation 构造期拒绝缺失原因。
     * 红线：缺少选择原因的资金来源不能支撑争议、对账和回放解释。
     */
    @Test
    void testFundingAllocationShouldRejectMissingReason() {
        assertThatThrownBy(() -> fundingAllocation("ALLOC-NO-REASON",
                fundingAccount("FA-NO-REASON"),
                LedgerSubjectCode.AVAILABLE,
                10,
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation reason is required");
    }

    /**
     * 场景：支付工具换绑后发起退款、撤销、退费或拒付回放。
     * 预期：原 route leg 明确声明按原路径回放，不能按当前绑定重新选路。
     * 红线：工具换绑、默认资金来源变化后，逆向资金路径不得漂移。
     */
    @Test
    void testPaymentInstrumentReplayShouldDeclareOriginalRouteReplayPolicy() {
        RouteLegSpec originalLeg = routeLeg(fundingAccount("FA-PAYER-001"), fundingAccount("FA-PAYEE-001"));
        RouteLegSpec replayLeg = ImmutableRouteLegSpec.builder()
                .legId("REFUND-PAY")
                .sequence(1)
                .legType(RouteLegType.RESTORE)
                .sourceNode(originalLeg.getTargetNode())
                .targetNode(originalLeg.getSourceNode())
                .amount(originalLeg.getAmount())
                .balanceEffectType(LedgerBalanceEffectType.RESTORE)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .replayRefLegId(originalLeg.getLegId())
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();

        assertThat(replayLeg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.FULL_ONLY);
        assertThat(replayLeg.getReplayRefLegId()).isEqualTo("PAY");
        assertThat(replayLeg.getSourceNode()).isSameAs(originalLeg.getTargetNode());
        assertThat(replayLeg.getTargetNode()).isSameAs(originalLeg.getSourceNode());
    }

    /**
     * 场景：业务侧错误地把完整卡号放入支付工具快照。
     * 预期：构造期拒绝明显敏感原文。
     * 红线：完整 PAN、CVV、密钥、token secret 不得进入普通快照、日志、导出、报表或测试数据。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldRejectRawPanLikeNumber() {
        assertThatThrownBy(() -> paymentInstrumentRef("PI-RAW", "4242424242424242"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instrumentNo must be masked or token reference");
    }

    /**
     * 场景：业务侧错误地把 CVV 或 token secret 放入绑定快照。
     * 预期：构造期拒绝敏感字段名。
     * 红线：绑定快照会进入 route snapshot、日志、导出和报表，不能承载支付工具敏感原文。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldRejectSensitiveBindingSnapshotFields() {
        assertThatThrownBy(() -> paymentInstrumentRef("PI-CVV", "**** 4242", Map.of("cvv", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-CARD-NO",
                "**** 4242",
                Map.of("cardNumber", "4242424242424242")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-TOKEN-SECRET",
                "tok_card_001",
                Map.of("token_secret", "secret-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-NESTED-SECRET",
                "tok_card_002",
                Map.<String, Object>of("processorPayload", Map.of("secretKey", "secret-value"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-NESTED-PAN",
                "tok_card_003",
                Map.<String, Object>of("processorPayload", Map.of("pan", "4242424242424242"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-NESTED-PAN-VALUE",
                "tok_card_004",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "4242424242424242"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
    }

    /**
     * 场景：通道回传字段名可能携带大小写、空格、短横线或下划线。
     * 预期：字段名归一化后仍按敏感字段阻断。
     * 红线：不能因字段命名风格差异让完整卡号、token secret 或密钥进入绑定快照。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldRejectNormalizedSensitiveBindingSnapshotFields() {
        assertThatThrownBy(() -> paymentInstrumentRef("PI-CARD-NO-STYLE",
                "**** 4242",
                Map.of("card-no", "4242424242424242")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-PRIMARY-ACCOUNT-STYLE",
                "**** 4242",
                Map.of("PRIMARY ACCOUNT NUMBER", "4242424242424242")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-TOKEN-SECRET-STYLE",
                "tok_card_006",
                Map.of("Token Secret", "secret-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
    }

    /**
     * 场景：业务侧错误地把外部银行账户原文放入 route external account 快照。
     * 预期：构造期拒绝外部账户原始账号和上下文敏感字段。
     * 红线：外部账户、VA、卡或通道 token 的敏感原文不得进入普通快照、日志、导出或报表。
     */
    @Test
    void testExternalAccountSnapshotShouldRejectSensitiveAccountValues() {
        assertThatThrownBy(() -> externalAccountRef("EA-RAW", "1234567890123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalAccountNo must be masked or token reference");
        assertThatThrownBy(() -> externalAccountRef("EA-RAW-IBAN", "GB82WEST12345698765432"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalAccountNo must be masked or token reference");
        assertThatThrownBy(() -> externalAccountRef("EA-NESTED-RAW",
                "token:external-account-001",
                Map.<String, Object>of("processorPayload", Map.of("accountNumber", "1234567890123456"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
        assertThatThrownBy(() -> externalAccountRef("EA-NESTED-IBAN-VALUE",
                "token:external-account-002",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "GB82WEST12345698765432"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
    }

    /**
     * 场景：通道回传外部账户字段名时使用大小写、空格或短横线变体。
     * 预期：字段名归一化后仍按敏感外部账户字段阻断。
     * 红线：外部账户号和 routing number 不能因字段命名风格差异进入 route 快照。
     */
    @Test
    void testExternalAccountSnapshotShouldRejectNormalizedSensitiveContextFields() {
        assertThatThrownBy(() -> externalAccountRef("EA-ACCOUNT-NUMBER-STYLE",
                "token:external-account-006",
                Map.of("Account Number", "token:external-account-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
        assertThatThrownBy(() -> externalAccountRef("EA-ROUTING-NUMBER-STYLE",
                "token:external-account-007",
                Map.of("routing-number", "token:external-routing-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
    }

    /**
     * 场景：内部资金交易号以两个字母加数字开头，形态上接近 IBAN 前缀。
     * 预期：上下文允许保存内部交易号引用。
     * 红线：敏感值识别不能误杀内部资金交易号、账本交易号或幂等号，导致授权后续链路不可执行。
     */
    @Test
    void testExternalAccountContextShouldAllowInternalFundsTransactionSnLikeIbanPrefix() {
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-INTERNAL-REF",
                "token:external-account-003",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "FT2026052714000062")));

        assertThat(externalAccountRef.getContextVariables())
                .containsEntry("processorPayload", Map.of("networkReference", "FT2026052714000062"));
    }

    /**
     * 场景：解冻和提现链路把内部冻结单号放入 referenceFreezeSn，冻结单号可能形似有效 IBAN。
     * 预期：内部冻结单引用允许进入上下文，但相同值放在普通通道字段中仍按敏感 IBAN 阻断。
     * 红线：敏感值治理不能误杀内部资金生命周期引用，也不能放开普通字段中的真实 IBAN。
     */
    @Test
    void testExternalAccountContextShouldAllowInternalFreezeSnOnlyForReferenceField() {
        String freezeSn = "FO2026052716000030";
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-INTERNAL-FREEZE-REF",
                "token:external-account-004",
                Map.<String, Object>of("referenceFreezeSn", freezeSn));

        assertThat(externalAccountRef.getContextVariables()).containsEntry("referenceFreezeSn", freezeSn);
        assertThatThrownBy(() -> externalAccountRef("EA-INTERNAL-FREEZE-RAW",
                "token:external-account-005",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "GB82WEST12345698765432"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
    }

    /**
     * 场景：调用方在支付工具快照构造后继续改写原始嵌套绑定上下文。
     * 预期：已构造的支付工具快照保持稳定，不被追加的卡敏感字段污染。
     * 红线：bindingSnapshot 会进入 route snapshot、日志和报表，不能因浅拷贝绕过构造期敏感字段校验。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldDefensivelyCopyNestedBindingSnapshot() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "tok_card_005");
        PaymentInstrumentRefSpec instrumentRef = paymentInstrumentRef("PI-IMMUTABLE",
                "tok_card_005",
                Map.of("processorPayload", processorPayload));

        processorPayload.put("pan", "4242424242424242");

        Object payloadValue = instrumentRef.getBindingSnapshot().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("tok_card_005");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：调用方在外部账户快照构造后继续改写原始嵌套上下文。
     * 预期：已构造的外部账户快照保持稳定，不被追加的账户原文污染。
     * 红线：外部账户快照不能因浅拷贝让银行账号原文进入普通 route 上下文。
     */
    @Test
    void testExternalAccountSnapshotShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:external-account-004");
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-IMMUTABLE",
                "token:external-account-004",
                Map.of("processorPayload", processorPayload));

        processorPayload.put("accountNumber", "1234567890123456");

        Object payloadValue = externalAccountRef.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:external-account-004");
        assertThat(payload.containsKey("accountNumber")).isFalse();
    }

    private RouteLegSpec routeLeg(SubjectRef payer, SubjectRef payee) {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode(payer, RouteNodeRole.SOURCE))
                .targetNode(routeNode(payee, RouteNodeRole.TARGET))
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
    }

    private RoutingDecisionSpec routingDecision(String policyCode,
                                                List<FundingAllocationDecisionSpec> fundingAllocations) {
        return routingDecision(policyCode, fundingAllocations, Map.of("accountModel", policyCode));
    }

    private RoutingDecisionSpec routingDecision(String policyCode,
                                                List<FundingAllocationDecisionSpec> fundingAllocations,
                                                Map<String, Object> contextVariables) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode(policyCode)
                .matchedRules(List.of("INSTRUMENT_ACTIVE", "DIRECTION_PAY", policyCode))
                .selectedProcessor("CARD_PROCESSOR")
                .fundingAllocations(fundingAllocations)
                .decisionReason(policyCode + "_DECISION")
                .contextVariables(contextVariables)
                .build();
    }

    private FundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                            SubjectRef subjectRef,
                                                            LedgerSubjectCode ledgerSubjectCode,
                                                            Integer priority,
                                                            String reason) {
        return fundingAllocation(allocationId,
                subjectRef,
                ledgerSubjectCode,
                Money.immutable(100L, CurrencyIsoCode.USD),
                priority,
                reason,
                null);
    }

    private FundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                            SubjectRef subjectRef,
                                                            LedgerSubjectCode ledgerSubjectCode,
                                                            Money amount,
                                                            Integer priority,
                                                            String reason) {
        return fundingAllocation(allocationId, subjectRef, ledgerSubjectCode, amount, priority, reason, null);
    }

    private FundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                            SubjectRef subjectRef,
                                                            LedgerSubjectCode ledgerSubjectCode,
                                                            Integer priority,
                                                            String reason,
                                                            AccountHierarchySnapshotSpec accountHierarchySnapshot) {
        return fundingAllocation(allocationId,
                subjectRef,
                ledgerSubjectCode,
                Money.immutable(100L, CurrencyIsoCode.USD),
                priority,
                reason,
                accountHierarchySnapshot);
    }

    private FundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                            SubjectRef subjectRef,
                                                            LedgerSubjectCode ledgerSubjectCode,
                                                            Money amount,
                                                            Integer priority,
                                                            String reason,
                                                            AccountHierarchySnapshotSpec accountHierarchySnapshot) {
        if (accountHierarchySnapshot != null) {
            return ImmutableAccountHierarchyFundingAllocationDecisionSpec.builder()
                    .allocationId(allocationId)
                    .subjectRef(subjectRef)
                    .ledgerSubjectCode(ledgerSubjectCode)
                    .amount(amount)
                    .accountHierarchySnapshot(accountHierarchySnapshot)
                    .priority(priority)
                    .reason(reason)
                    .build();
        }
        return ImmutableFundingAllocationDecisionSpec.builder()
                .allocationId(allocationId)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .amount(amount)
                .priority(priority)
                .reason(reason)
                .build();
    }

    private RouteNodeSpec routeNode(SubjectRef subjectRef, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private SubjectRef fundingAccount(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private SubjectRef creditAccount(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private SubjectRef subjectRef(String subjectId, FundsSubjectType subjectType) {
        return accountSubject(subjectId, subjectType, 1L, CurrencyIsoCode.USD.name());
    }

    private SubjectRef accountSubject(String subjectId,
                                      FundsSubjectType subjectType,
                                      Long tenantId,
                                      String currency) {
        return ImmutableSubjectRef.builder()
                .tenantId(tenantId)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(currency)
                .ledgerProfileCode("DEFAULT")
                .build();
    }

    private AccountHierarchySnapshotSpec accountHierarchySnapshot(SubjectRef accountRef,
                                                                  SubjectRef parentAccountRef,
                                                                  Map<String, Object> contextVariables) {
        return ImmutableAccountHierarchySnapshotSpec.builder()
                .accountRef(accountRef)
                .parentAccountRef(parentAccountRef)
                .contextVariables(contextVariables)
                .build();
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(String instrumentId, String instrumentNo) {
        return paymentInstrumentRef(instrumentId,
                instrumentNo,
                Map.of("bindingId", "BINDING-001", "bindingVersion", 3));
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(String instrumentId,
                                                          String instrumentNo,
                                                          Map<String, Object> bindingSnapshot) {
        return ImmutablePaymentInstrumentRefSpec.builder()
                .tenantId(1L)
                .instrumentId(instrumentId)
                .instrumentType("CARD")
                .instrumentNo(instrumentNo)
                .ownerId("USER-001")
                .ownerType("USER")
                .currency(CurrencyIsoCode.USD.name())
                .status("ACTIVE")
                .bindingSnapshot(bindingSnapshot)
                .build();
    }

    private ExternalAccountRefSpec externalAccountRef(String externalAccountId, String externalAccountNo) {
        return externalAccountRef(externalAccountId,
                externalAccountNo,
                Map.of("externalAccountVersion", 2));
    }

    private ExternalAccountRefSpec externalAccountRef(String externalAccountId,
                                                      String externalAccountNo,
                                                      Map<String, Object> contextVariables) {
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(externalAccountId)
                .externalAccountType("BANK_ACCOUNT")
                .externalAccountNo(externalAccountNo)
                .providerCode("BANK")
                .channelCode("ACH")
                .currency(CurrencyIsoCode.USD.name())
                .countryCode("US")
                .contextVariables(contextVariables)
                .build();
    }
}

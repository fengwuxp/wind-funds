package com.wind.funds.transaction.services.impl;

import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.model.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableRouteSnapshotSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RouteSnapshot JSON 摘要契约测试。
 */
class RouteSnapshotJsonSupportTests {

    /**
     * 场景：含权益交易的 RouteSnapshot 被保存为交易事实 JSON，随后用于退款或回放查询。
     * 输入：RouteSnapshot context 携带原权益快照 ID 和稳定摘要，且 route leg 有确定 sequence。
     * 输出：JSON 往返后的 RouteSnapshot。
     * 预期：权益摘要和 leg sequence 都不丢失。
     * 红线：原路径快照不能在持久化摘要层丢失权益回放依据或 route leg 顺序事实。
     */
    @Test
    void testRouteSnapshotJsonShouldKeepBenefitSummaryAndLegSequence() {
        RouteSnapshotSpec snapshot = routeSnapshot(Map.of(
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-JSON-001",
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST, "sha256:original-benefit-digest"));

        String json = RouteSnapshotJsonSupport.toRouteSnapshotJson(snapshot);
        JsonNode document = WindJson.parseObject(json, JsonNode.class);
        RouteSnapshotSpec parsed = RouteSnapshotJsonSupport.parseRouteSnapshot(json,
                LocalDateTime.of(2026, 5, 24, 10, 0));

        assertThat(document.path("participants").path(0).path("subjectRef").has("description")).isFalse();
        assertThat(parsed.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-JSON-001")
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST,
                        "sha256:original-benefit-digest");
        assertThat(parsed.getLegs()).singleElement()
                .extracting(RouteLegSpec::getSequence)
                .isEqualTo(7);
    }

    /**
     * 场景：VCC 共享卡经支付工具路由后，实际资金责任落到子信用账户。
     * 输入：RouteSnapshot participant 携带账户层级快照。
     * 输出：JSON 往返后的 RouteSnapshot。
     * 预期：子账户和直接父账户不丢失。
     * 红线：交易事实快照必须能支撑共享卡按卡、按子账户、按主账户追溯，且不得保存完整卡号。
     */
    @Test
    void testRouteSnapshotJsonShouldKeepAccountHierarchySnapshotForVccSharedCard() {
        AccountHierarchySnapshotSpec hierarchySnapshot = ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn("AHR-VCC-CARD-001")
                .parentAccountRef(subject("VCC-CREDIT-MAIN-001", FundsSubjectType.CREDIT_ACCOUNT))
                .build();
        RouteSnapshotSpec snapshot = routeSnapshot(Map.of(), hierarchySnapshot);

        String json = RouteSnapshotJsonSupport.toRouteSnapshotJson(snapshot);
        RouteSnapshotSpec parsed = RouteSnapshotJsonSupport.parseRouteSnapshot(json,
                LocalDateTime.of(2026, 5, 24, 10, 0));

        assertThat(json)
                .contains("accountHierarchySnapshot")
                .doesNotContain("4111111111111111");
        JsonNode document = WindJson.parseObject(json, JsonNode.class);
        JsonNode serializedHierarchy = document.path("participants").path(0).path("accountHierarchySnapshot");
        assertThat(serializedHierarchy.path("parentAccountRef").has("description")).isFalse();
        assertThat(parsed.getParticipants()).singleElement()
                .satisfies(participant -> {
                    assertThat(participant.getSubjectRef().getSubjectType())
                            .isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(participant.getSubjectRef().getSubjectId()).isEqualTo("VCC-CREDIT-SUB-001");
                    AccountHierarchySnapshotSpec parsedHierarchy = participant.getAccountHierarchySnapshot();
                    assertThat(parsedHierarchy).isNotNull();
                    assertThat(parsedHierarchy.getRelationSn()).isEqualTo("AHR-VCC-CARD-001");
                    assertThat(parsedHierarchy.getParentAccountRef()).isNotNull();
                    assertThat(parsedHierarchy.getParentAccountRef().getSubjectId())
                            .isEqualTo("VCC-CREDIT-MAIN-001");
                });
    }

    /**
     * 场景：公共 RouteResolver 返回没有经过资金域不可变模型构造器的自定义快照组件。
     * 输入：支付工具原始 PAN、外部账户原始账号或路由决策敏感上下文。
     * 输出：RouteSnapshot JSON 持久化请求。
     * 预期：共享持久化边界拒绝写入。
     * 红线：公共接口实现不能绕过不可变模型构造器的敏感数据校验。
     */
    @Test
    void testRouteSnapshotJsonShouldRejectSensitiveValuesFromCustomRouteComponents() {
        PaymentInstrumentRefSpec unsafeInstrumentNo = paymentInstrument("4111111111111112", Map.of());
        PaymentInstrumentRefSpec unsafeInstrumentBinding = paymentInstrument("****1111",
                Map.of("tokenSecret", "raw-secret"));
        ExternalAccountRefSpec unsafeExternalAccountNo = externalAccount("1234567890123456", Map.of());
        ExternalAccountRefSpec unsafeExternalAccountContext = externalAccount("****3456",
                Map.of("routingNumber", "123456789"));
        RoutingDecisionSpec unsafeRoutingDecision = new RoutingDecisionSpec() {

            @Override
            public Map<String, Object> getContextVariables() {
                return Map.of("accountNumber", "12345678");
            }
        };

        assertThatThrownBy(() -> RouteSnapshotJsonSupport.toRouteSnapshotJson(
                routeSnapshot(null, unsafeInstrumentNo, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RouteSnapshotJsonSupport.toRouteSnapshotJson(
                routeSnapshot(null, unsafeInstrumentBinding, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RouteSnapshotJsonSupport.toRouteSnapshotJson(
                routeSnapshot(null, null, unsafeExternalAccountNo)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RouteSnapshotJsonSupport.toRouteSnapshotJson(
                routeSnapshot(null, null, unsafeExternalAccountContext)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RouteSnapshotJsonSupport.toRouteSnapshotJson(
                routeSnapshot(unsafeRoutingDecision, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRouteSnapshotJsonShouldAllowMaskedCustomReferences() {
        RouteSnapshotSpec snapshot = routeSnapshot(null,
                paymentInstrument("****1111", Map.of("instrumentToken", "tok-card-001")),
                externalAccount("****3456", Map.of("accountToken", "tok-account-001")));

        String json = RouteSnapshotJsonSupport.toRouteSnapshotJson(snapshot);
        RouteSnapshotSpec parsed = RouteSnapshotJsonSupport.parseRouteSnapshot(json,
                LocalDateTime.of(2026, 5, 24, 10, 0));

        assertThat(parsed.getPaymentInstrumentRef().getInstrumentNo()).isEqualTo("****1111");
        assertThat(parsed.getExternalAccountRef().getExternalAccountNo()).isEqualTo("****3456");
    }

    private PaymentInstrumentRefSpec paymentInstrument(String instrumentNo,
                                                       Map<String, Object> bindingSnapshot) {
        return new PaymentInstrumentRefSpec() {

            @Override
            public String getInstrumentId() {
                return "PI-CUSTOM-001";
            }

            @Override
            public String getInstrumentType() {
                return "CARD";
            }

            @Override
            public String getInstrumentNo() {
                return instrumentNo;
            }

            @Override
            public String getOwnerId() {
                return "OWNER-001";
            }

            @Override
            public String getOwnerType() {
                return "USER";
            }

            @Override
            public Map<String, Object> getBindingSnapshot() {
                return bindingSnapshot;
            }
        };
    }

    private ExternalAccountRefSpec externalAccount(String externalAccountNo,
                                                   Map<String, Object> contextVariables) {
        return new ExternalAccountRefSpec() {

            @Override
            public String getExternalAccountId() {
                return "EA-CUSTOM-001";
            }

            @Override
            public String getExternalAccountType() {
                return "BANK_ACCOUNT";
            }

            @Override
            public String getExternalAccountNo() {
                return externalAccountNo;
            }

            @Override
            public Map<String, Object> getContextVariables() {
                return contextVariables;
            }
        };
    }

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables) {
        return routeSnapshot(contextVariables, null);
    }

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables,
                                            AccountHierarchySnapshotSpec hierarchySnapshot) {
        return routeSnapshot(contextVariables, hierarchySnapshot, null, null, null);
    }

    private RouteSnapshotSpec routeSnapshot(RoutingDecisionSpec routingDecision,
                                            PaymentInstrumentRefSpec paymentInstrumentRef,
                                            ExternalAccountRefSpec externalAccountRef) {
        return routeSnapshot(Map.of(), null, routingDecision, paymentInstrumentRef, externalAccountRef);
    }

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables,
                                            AccountHierarchySnapshotSpec hierarchySnapshot,
                                            RoutingDecisionSpec routingDecision,
                                            PaymentInstrumentRefSpec paymentInstrumentRef,
                                            ExternalAccountRefSpec externalAccountRef) {
        ImmutableSubjectRef sourceSubjectRef = hierarchySnapshot == null
                ? subject("PAYER-001")
                : subject("VCC-CREDIT-SUB-001", FundsSubjectType.CREDIT_ACCOUNT);
        var builder = ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("ROUTE-SNAPSHOT-BEN-001")
                .snapshotSchemaVersion(FundsRouteCodes.CURRENT_ROUTE_SNAPSHOT_SCHEMA_VERSION)
                .routeCode(FundsRouteCodes.DIRECT_PAY_STANDARD)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene("ORDER_PAY")
                .businessSn("ORDER_PAY_BEN_JSON_001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(ImmutableRouteParticipantSpec.builder()
                        .participantRole(RouteParticipantRole.PAYER)
                        .subjectRef(sourceSubjectRef)
                        .ledgerProfileCode("FUNDING_BASIC")
                        .currency(CurrencyIsoCode.USD.name())
                        .amount(Money.immutable(900L, CurrencyIsoCode.USD))
                        .accountHierarchySnapshot(hierarchySnapshot)
                        .contextVariables(Map.of())
                        .build()))
                .legs(List.of(routeLeg(sourceSubjectRef)))
                .resolvedAt(LocalDateTime.of(2026, 5, 24, 10, 0))
                .contextVariables(contextVariables);
        if (routingDecision != null) {
            builder.routingDecision(routingDecision);
        }
        if (paymentInstrumentRef != null) {
            builder.paymentInstrumentRef(paymentInstrumentRef);
        }
        if (externalAccountRef != null) {
            builder.externalAccountRef(externalAccountRef);
        }
        return builder.build();
    }

    private RouteLegSpec routeLeg(ImmutableSubjectRef sourceSubjectRef) {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(7)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(routeNode(sourceSubjectRef, LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE))
                .targetNode(routeNode(subject("PAYEE-001"), LedgerSubjectCode.SETTLEMENT, RouteNodeRole.TARGET))
                .amount(Money.immutable(900L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteNodeSpec routeNode(ImmutableSubjectRef subjectRef,
                                             LedgerSubjectCode ledgerSubjectCode,
                                             RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .nodeRole(nodeRole)
                .build();
    }

    private ImmutableSubjectRef subject(String subjectId) {
        return subject(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private ImmutableSubjectRef subject(String subjectId, FundsSubjectType subjectType) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(CurrencyIsoCode.USD.name())
                .build();
    }
}

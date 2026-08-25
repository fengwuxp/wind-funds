package com.wind.funds.route;

import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.support.WindOperatorTestFixture;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import com.wind.funds.wallet.service.AccountHierarchyRelationService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 路由账户层级快照追加器测试。
 */
class RouteAccountHierarchySnapshotAppenderTests {

    private final AccountHierarchyRelationService relationService = mock(AccountHierarchyRelationService.class);

    private final RouteAccountHierarchySnapshotAppender appender =
            new RouteAccountHierarchySnapshotAppender(relationService);

    @Test
    void testForwardRouteShouldAppendRelationSnapshotOncePerAccount() {
        SubjectRef child = account("child", FundsSubjectType.CREDIT_ACCOUNT);
        ResolvedRouteSpec route = route(List.of(participant(child), participant(child)));
        when(relationService.findAccountHierarchyRelation(1L, accountId(child)))
                .thenReturn(Optional.of(relation("AHR_FORWARD", "parent", FundsSubjectType.FUNDING_ACCOUNT)));

        ResolvedRouteSpec result = appender.append(forwardInstruction(), route);

        assertThat(result.getParticipants()).allSatisfy(participant -> {
            assertThat(participant.getAccountHierarchySnapshot()).isNotNull();
            assertThat(participant.getAccountHierarchySnapshot().getRelationSn()).isEqualTo("AHR_FORWARD");
            assertThat(participant.getAccountHierarchySnapshot().getParentAccountRef().getSubjectId())
                    .isEqualTo("parent");
        });
        assertThat(result.getLegs()).containsExactlyElementsOf(route.getLegs());
        verify(relationService, times(1)).findAccountHierarchyRelation(1L, accountId(child));
    }

    @Test
    void testForwardRouteWithoutRelationShouldKeepSnapshotEmpty() {
        SubjectRef child = account("child", FundsSubjectType.FUNDING_ACCOUNT);
        when(relationService.findAccountHierarchyRelation(1L, accountId(child)))
                .thenReturn(Optional.empty());

        ResolvedRouteSpec result = appender.append(forwardInstruction(), route(List.of(participant(child))));

        assertThat(result.getParticipants()).singleElement()
                .extracting(RouteParticipantSpec::getAccountHierarchySnapshot)
                .isNull();
    }

    @Test
    void testForwardRouteShouldPropagateRelationLookupFailure() {
        SubjectRef child = account("child", FundsSubjectType.FUNDING_ACCOUNT);
        when(relationService.findAccountHierarchyRelation(1L, accountId(child)))
                .thenThrow(new IllegalStateException("relation storage unavailable"));

        assertThatThrownBy(() -> appender.append(forwardInstruction(), route(List.of(participant(child)))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("relation storage unavailable");
    }

    @Test
    void testReplayRouteShouldNeverResolveCurrentRelation() {
        SubjectRef child = account("child", FundsSubjectType.FUNDING_ACCOUNT);
        ResolvedRouteSpec route = route(List.of(participant(child)));

        ResolvedRouteSpec result = appender.append(replayInstruction(), route);

        assertThat(result).isSameAs(route);
        assertThat(result.getParticipants()).singleElement()
                .extracting(RouteParticipantSpec::getAccountHierarchySnapshot)
                .isNull();
        verify(relationService, never()).findAccountHierarchyRelation(1L, accountId(child));
    }

    private FundsInstructionSpec forwardInstruction() {
        return instruction(FundsTransactionEventType.PAY, null);
    }

    private FundsInstructionSpec replayInstruction() {
        return instruction(FundsTransactionEventType.REFUND,
                ImmutableFundsInstructionReferenceSpec.builder()
                        .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                        .referenceSn("FT_ORIGINAL")
                        .build());
    }

    private FundsInstructionSpec instruction(FundsTransactionEventType eventType,
                                             ImmutableFundsInstructionReferenceSpec reference) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(eventType)
                .transactionType(eventType == FundsTransactionEventType.REFUND
                        ? DefaultFundsTransactionType.REFUND : DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                .businessScene(eventType.name())
                .businessSn("ROUTE_HIERARCHY_" + eventType.name())
                .reference(reference)
                .eventTime(LocalDateTime.of(2026, 7, 23, 10, 0))
                .operator(WindOperatorTestFixture.system())
                .contextVariables(Map.of())
                .build();
    }

    private ResolvedRouteSpec route(List<RouteParticipantSpec> participants) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(1L)
                .routeCode("DIRECT_ROUTE")
                .routeVersion("1")
                .businessScene("PAY")
                .businessSn("ROUTE_HIERARCHY")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(participants)
                .legs(List.of())
                .resolvedAt(LocalDateTime.of(2026, 7, 23, 10, 0))
                .contextVariables(Map.of())
                .build();
    }

    private RouteParticipantSpec participant(SubjectRef account) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(RouteParticipantRole.PAYER)
                .subjectRef(account)
                .currency(CurrencyIsoCode.USD)
                .contextVariables(Map.of())
                .build();
    }

    private SubjectRef account(String id, FundsSubjectType type) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(id)
                .subjectType(type)
                .currency(CurrencyIsoCode.USD)
                .build();
    }

    private FundsAccountId accountId(SubjectRef subjectRef) {
        return FundsAccountId.immutable(subjectRef.getSubjectId(), subjectRef.getSubjectType());
    }

    private AccountHierarchyRelationDTO relation(String sn, String parentId, FundsSubjectType parentType) {
        return new AccountHierarchyRelationDTO()
                .setSn(sn)
                .setTenantId(1L)
                .setAccountId("child")
                .setAccountType(FundsSubjectType.CREDIT_ACCOUNT)
                .setParentAccountId(parentId)
                .setParentAccountType(parentType)
                .setCurrency(CurrencyIsoCode.USD);
    }
}

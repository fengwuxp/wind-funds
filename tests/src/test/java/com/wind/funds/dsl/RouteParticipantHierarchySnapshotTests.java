package com.wind.funds.dsl;

import com.wind.funds.route.model.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 路由参与方账户层级快照契约测试。
 */
class RouteParticipantHierarchySnapshotTests {

    @Test
    void testParticipantShouldCarryDirectParentRelationSnapshot() {
        SubjectRef child = account("child", FundsSubjectType.CREDIT_ACCOUNT, CurrencyIsoCode.USD);
        SubjectRef parent = account("parent", FundsSubjectType.FUNDING_ACCOUNT, CurrencyIsoCode.USD);
        AccountHierarchySnapshotSpec hierarchy = ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn("AHR202607230001")
                .parentAccountRef(parent)
                .build();

        ImmutableRouteParticipantSpec participant = ImmutableRouteParticipantSpec.builder()
                .participantRole(RouteParticipantRole.PAYER)
                .subjectRef(child)
                .currency(CurrencyIsoCode.USD.name())
                .accountHierarchySnapshot(hierarchy)
                .contextVariables(Map.of())
                .build();

        assertThat(participant.getAccountHierarchySnapshot()).isSameAs(hierarchy);
        assertThat(hierarchy.getRelationSn()).isEqualTo("AHR202607230001");
        assertThat(hierarchy.getParentAccountRef()).isSameAs(parent);
    }

    @Test
    void testHierarchySnapshotShouldRejectBlankRelationSn() {
        assertThatThrownBy(() -> ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn(" ")
                .parentAccountRef(account("parent", FundsSubjectType.FUNDING_ACCOUNT, CurrencyIsoCode.USD))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relationSn");
    }

    @Test
    void testParticipantShouldRejectSelfOrCrossCurrencyParent() {
        SubjectRef child = account("child", FundsSubjectType.CREDIT_ACCOUNT, CurrencyIsoCode.USD);

        assertThatThrownBy(() -> participant(child, ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn("AHR_SELF")
                .parentAccountRef(child)
                .build()))
                .hasMessageContaining("不能等于参与账户");
        assertThatThrownBy(() -> participant(child, ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn("AHR_CURRENCY")
                .parentAccountRef(account("parent", FundsSubjectType.FUNDING_ACCOUNT, CurrencyIsoCode.EUR))
                .build()))
                .hasMessageContaining("币种必须一致");
    }

    private ImmutableRouteParticipantSpec participant(SubjectRef child,
                                                       AccountHierarchySnapshotSpec hierarchy) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(RouteParticipantRole.PAYER)
                .subjectRef(child)
                .currency(child.getCurrency())
                .accountHierarchySnapshot(hierarchy)
                .contextVariables(Map.of())
                .build();
    }

    private SubjectRef account(String id, FundsSubjectType type, CurrencyIsoCode currency) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(id)
                .subjectType(type)
                .currency(currency.name())
                .build();
    }
}

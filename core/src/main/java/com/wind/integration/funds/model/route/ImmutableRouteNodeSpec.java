package com.wind.integration.funds.model.route;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;

/**
 * 不可变 Route 节点实现。
 */
@Builder
@FieldNameConstants
public record ImmutableRouteNodeSpec(RouteNodeType nodeType,
                                     SubjectRef subjectRef,
                                     LedgerSubjectCode ledgerSubjectCode,
                                     RouteNodeRole nodeRole) implements RouteNodeSpec {

    @Override
    public @NonNull RouteNodeType getNodeType() {
        return nodeType;
    }

    @Override
    public @NonNull SubjectRef getSubjectRef() {
        return subjectRef;
    }

    @Override
    public @NonNull LedgerSubjectCode getLedgerSubjectCode() {
        return ledgerSubjectCode;
    }

    @Override
    public @NonNull RouteNodeRole getNodeRole() {
        return nodeRole;
    }
}

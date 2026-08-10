package com.wind.funds.route.model;

import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteNodeSpec;
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

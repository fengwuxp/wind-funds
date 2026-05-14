package com.wind.integration.funds.route.spec;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import org.jspecify.annotations.NonNull;

/**
 * Route 节点定义。
 */
public interface RouteNodeSpec {

    @NonNull
    RouteNodeType getNodeType();

    @NonNull
    SubjectRef getSubjectRef();

    @NonNull
    LedgerSubjectCode getLedgerSubjectCode();

    @NonNull
    RouteNodeRole getNodeRole();
}

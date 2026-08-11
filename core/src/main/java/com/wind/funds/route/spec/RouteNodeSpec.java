package com.wind.funds.route.spec;

import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.ref.SubjectRef;
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
    RouteNodeRole getNodeRole();
}

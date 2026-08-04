package com.wind.funds.support;

import com.wind.integration.operator.OperationActorType;
import com.wind.integration.operator.WindOperator;
import com.wind.security.core.WindSecurityAccessOperations;

/**
 * 无 Spring 上下文依赖的测试操作者夹具。
 */
public final class WindOperatorTestFixture {

    private static final WindOperator SYSTEM = WindOperator.builder()
            .operatorId(-1L)
            .operatorName(OperationActorType.SYSTEM.name())
            .appName("example")
            .actorType(OperationActorType.SYSTEM)
            .accessOperations(new WindSecurityAccessOperations() {
                @Override
                public boolean hasAnyAuthority(String... authorities) {
                    return false;
                }

                @Override
                public boolean hasAnyRole(String... roles) {
                    return false;
                }
            })
            .build();

    private WindOperatorTestFixture() {
    }

    public static WindOperator system() {
        return SYSTEM;
    }
}

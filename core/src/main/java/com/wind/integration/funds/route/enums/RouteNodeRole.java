package com.wind.integration.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路由节点角色。
 */
@AllArgsConstructor
@Getter
public enum RouteNodeRole implements DescriptiveEnum {

    SOURCE("来源节点"),

    TARGET("目标节点");

    private final String desc;
}

package com.wind.integration.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路径步骤回放策略。
 */
@AllArgsConstructor
@Getter
public enum RouteReplayPolicy implements DescriptiveEnum {

    FULL_ONLY("仅支持全量回放"),

    PARTIAL_ALLOWED("允许部分回放"),

    NON_REPLAYABLE("不可回放"),

    REPLAY_ONCE("仅允许成功回放一次");

    private final String desc;
}

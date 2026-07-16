package com.wind.funds.fx;

import org.jspecify.annotations.NonNull;

/**
 * 外汇金额换算服务。
 *
 * @author wuxp
 * @date 2026-07-15
 */
public interface FxAmountConversionService {

    /**
     * 按显式应用汇率或指定来源价格类型计算目标金额。
     *
     * @param request 金额换算请求
     * @return 金额换算结果及实际应用汇率
     */
    @NonNull
    FxAmountConversionResult calculate(@NonNull FxAmountConversionRequest request);
}

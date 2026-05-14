package com.wind.integration.funds.fx;

import org.jspecify.annotations.NonNull;

/**
 * 汇率服务
 *
 * @author wuxp
 * @date 2026-04-16 16:07
 **/
public interface FxService {

    /**
     * 换汇计算
     *
     * @param request 换汇请求
     * @return 换汇结果
     */
    @NonNull
    FxResult convert(@NonNull FxRequest request);
}

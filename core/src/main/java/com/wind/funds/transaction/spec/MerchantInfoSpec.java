package com.wind.funds.transaction.spec;

import lombok.Builder;
import lombok.Getter;

/**
 * 商户描述交易规范
 *
 * @author wuxp
 * @date 2026-04-17 13:48
 **/
@Builder
@Getter
public final class MerchantInfoSpec {

    // ========== 基础标识 ==========
    /**
     * 商户 ID（全局唯一）
     */
    private final String merchantId;

    /**
     * 商户名称
     */
    private final String merchantName;

    // ========== 行业与风控 ==========
    /**
     * MCC 码（Merchant Category Code）
     * 用于判断行业、费率、风控规则
     */
    private final String mccCode;
}

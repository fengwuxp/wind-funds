package com.capte.funds.transaction.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 商户信息请求。
 *
 * @author Codex
 * @date 2026-05-09
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class MerchantInfoRequest {

    @Schema(description = "商户 ID")
    private String merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "商户 MCC 编码")
    private String mccCode;
}

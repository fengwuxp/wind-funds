package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 创建账户层级关系请求。
 *
 * <p>只表达子账户与直接父账户的业务关系；关系号、币种和操作人由服务根据可信事实生成。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateAccountHierarchyRelationRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "子账户标识")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "直接父账户标识")
    @NotNull
    private FundsAccountId parentAccountId;
}

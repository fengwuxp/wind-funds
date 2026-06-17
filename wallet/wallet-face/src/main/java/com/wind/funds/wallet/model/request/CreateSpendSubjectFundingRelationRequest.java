package com.wind.funds.wallet.model.request;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 创建支出主体资金关系请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateSpendSubjectFundingRelationRequest {

    @Schema(description = "关系号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支出控制主体 ID")
    @NotBlank
    private String spendSubjectId;

    @Schema(description = "支出控制主体类型")
    @NotNull
    private FundsSubjectType spendSubjectType;

    @Schema(description = "兼容真实资金账户 ID，仅资金账户目标主体使用")
    private String fundingAccountId;

    @Schema(description = "资金责任目标主体类型")
    private FundsSubjectType targetSubjectType;

    @Schema(description = "资金责任目标主体 ID")
    private String targetSubjectId;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "关系类型")
    @NotNull
    private SpendSubjectFundingRelationType relationType;

    @Schema(description = "路由优先级")
    private Integer priority;

    @Schema(description = "是否默认关系")
    private Boolean defaultRelation;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}

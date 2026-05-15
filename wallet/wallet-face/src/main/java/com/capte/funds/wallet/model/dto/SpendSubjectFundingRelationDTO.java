package com.capte.funds.wallet.model.dto;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支出主体和真实资金账户关系 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendSubjectFundingRelationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1643737927933489069L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "关系号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "支出控制主体 ID")
    private String spendSubjectId;

    @Schema(description = "支出控制主体类型")
    private FundsSubjectType spendSubjectType;

    @Schema(description = "真实资金账户 ID")
    private String fundingAccountId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "关系类型")
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

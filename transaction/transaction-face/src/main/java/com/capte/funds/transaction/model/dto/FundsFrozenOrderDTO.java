package com.capte.funds.transaction.model.dto;

import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.funds.route.enums.FundsSubjectType;
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
 * 资金冻结订单 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsFrozenOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2709575196640617148L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "冻结单号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "被冻结主体 ID")
    private String subjectId;

    @Schema(description = "被冻结主体类型")
    private FundsSubjectType subjectType;

    @Schema(description = "冻结类型")
    private String freezeType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "外部业务号")
    private String businessSn;

    @Schema(description = "关联资金交易号")
    private String transactionSn;

    @Schema(description = "冻结动作明细号")
    private String freezeDetailSn;

    @Schema(description = "冻结账本交易号")
    private String freezeLedgerTransactionSn;

    @Schema(description = "原冻结金额")
    private Long amount;

    @Schema(description = "已释放金额")
    private Long releasedAmount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "冻结单状态")
    private FundsFrozenOrderStatus status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "完全释放时间")
    private LocalDateTime releaseTime;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;

    @Schema(description = "乐观锁版本")
    private Integer version;
}

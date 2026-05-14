package com.capte.funds.transaction.model.dto;

import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
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
import java.util.Map;

/**
 * 信用账户 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreditAccountDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3482792313801711717L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "信用账户号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "归属主体 ID")
    private String ownerId;

    @Schema(description = "归属主体类型")
    private FundsAccountOwnerType ownerType;

    @Schema(description = "信用账户类型")
    private String accountType;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "账本周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "ledger profile 编码")
    private LedgerProfileCode ledgerProfileCode;

    @Schema(description = "profile 版本")
    private Integer ledgerProfileVersion;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;

    @Schema(description = "乐观锁版本")
    private Integer version;

    @Schema(description = "账本科目到 ledger id 的映射")
    private Map<LedgerSubjectCode, Long> ledgerIds;
}

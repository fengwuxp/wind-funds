package com.capte.funds.wallet.model.dto;

import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * LedgerProfile 定义快照。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerProfileDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -9020154778373879922L;

    @Schema(description = "profile 编码")
    private LedgerProfileCode code;

    @Schema(description = "profile 版本")
    private Integer version;

    @Schema(description = "适用主体类型")
    private FundsSubjectType subjectType;

    @Schema(description = "科目明细")
    private List<LedgerProfileItemDTO> items;

}

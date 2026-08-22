package com.wind.funds.reconciliation.model.dto;

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
import java.util.List;

/**
 * 已确认清分结果的不可变明细快照。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingSplitResultSnapshotDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2938847331944243468L;

    private Long id;

    private String sn;

    private Long tenantId;

    private String splitBatchSn;

    private String splittableDetailSn;

    private String subjectType;

    private String subjectId;

    private CurrencyIsoCode currency;

    private String businessLine;

    private String splitPeriod;

    @Schema(description = "来源 CLEARING 入账金额，最小货币单位")
    private Long amount;

    private String fundsTransactionSn;

    private String fundsTransactionDetailSn;

    private String ledgerTransactionSn;

    private String postingPlanSn;

    private String ledgerEntrySn;

    private String routeSnapshotDigest;

    private String splitRuleCode;

    private String splitRuleVersion;

    private String gateEvidenceRef;

    private List<String> reconciliationEvidenceRefs;

    private String sourceDigest;

    private String snapshotDigest;

    private LocalDateTime createdTime;
}

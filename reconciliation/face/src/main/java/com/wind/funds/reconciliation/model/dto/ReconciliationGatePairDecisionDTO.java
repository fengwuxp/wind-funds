package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationGateBlockerCode;
import com.wind.funds.reconciliation.enums.ReconciliationRunOutcome;
import com.wind.funds.reconciliation.model.value.RequiredPairRef;
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
 * 一个必需对账对的当前血缘决策。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "对账门禁对账对决策")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationGatePairDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6203766091629919123L;

    private RequiredPairRef requiredPairRef;

    private String currentRunResultSn;

    private String currentBatchSn;

    private String currentLineageRef;

    private String resultDigest;

    private ReconciliationRunOutcome outcome;

    private List<ReconciliationGateBlockerCode> blockerCodes;

    private List<String> evidenceRefs;
}

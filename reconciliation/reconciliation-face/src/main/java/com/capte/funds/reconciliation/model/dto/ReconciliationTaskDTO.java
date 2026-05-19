package com.capte.funds.reconciliation.model.dto;

import com.capte.funds.reconciliation.enums.ReconciliationTaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账任务 DTO。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationTaskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2436826557233647476L;

    private String taskSn;

    private Long tenantId;

    private String taskType;

    private String ruleVersion;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private ReconciliationTaskStatus status;
}

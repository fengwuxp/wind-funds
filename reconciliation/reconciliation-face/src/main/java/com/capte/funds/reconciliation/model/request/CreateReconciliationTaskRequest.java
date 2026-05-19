package com.capte.funds.reconciliation.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建对账任务请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateReconciliationTaskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 8825668366714008670L;

    private String requestSn;

    private String requestDigest;

    private Long tenantId;

    private String taskType;

    private String ruleVersion;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private String operatorId;
}

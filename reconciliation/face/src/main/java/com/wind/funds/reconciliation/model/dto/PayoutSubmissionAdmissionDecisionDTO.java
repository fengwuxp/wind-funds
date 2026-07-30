package com.wind.funds.reconciliation.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class PayoutSubmissionAdmissionDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6534735922165169041L;

    private boolean passed;

    private String decisionDigest;

    private List<String> evidenceRefs;

    private LocalDateTime expiresAt;

    private String blockingReason;
}

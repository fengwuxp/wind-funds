package com.wind.funds.transaction.model.dto;

import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 一次资金动作在 Transaction 内已经记录的完整引用证据。
 *
 * <p>这里只声明 Transaction 自有引用，不声明 Ledger 或 Balance 已闭合。</p>
 *
 * @author wuxp
 * @since 2026-08-23
 */
@Value
public class FundsActionRecordedEvidenceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3036308880389999393L;

    @Schema(description = "资金动作事实")
    FundsActionFactDTO actionFact;

    @Schema(description = "完整匹配的参与方引用")
    List<RecordedSiblingRef> matchedSiblings;

    @Schema(description = "完整参与方共同记录的账本交易流水号")
    String recordedLedgerTransactionSn;

    @Schema(description = "已记录引用摘要")
    FundsActionFactDTO.SemanticDigest recordedReferenceDigest;

    public FundsActionRecordedEvidenceDTO(FundsActionFactDTO actionFact,
                                          List<RecordedSiblingRef> matchedSiblings,
                                          String recordedLedgerTransactionSn,
                                          FundsActionFactDTO.SemanticDigest recordedReferenceDigest) {
        this.actionFact = actionFact;
        this.matchedSiblings = List.copyOf(matchedSiblings);
        this.recordedLedgerTransactionSn = recordedLedgerTransactionSn;
        this.recordedReferenceDigest = recordedReferenceDigest;
    }

    /**
     * Transaction 已记录的动作参与方引用。
     */
    @Value
    public static class RecordedSiblingRef implements Serializable {

        @Serial
        private static final long serialVersionUID = -1752156791143366921L;

        @Schema(description = "资金交易明细流水号")
        String detailSn;

        @Schema(description = "参与方角色")
        RouteParticipantRole participantRole;

        @Schema(description = "主体 ID")
        String subjectId;

        @Schema(description = "主体类型")
        String subjectType;

        @Schema(description = "参与金额")
        Money money;

        @Schema(description = "该参与方记录的账本交易流水号")
        String recordedLedgerTransactionSn;
    }
}

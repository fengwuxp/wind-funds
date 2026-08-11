package com.wind.funds.transaction.model.dto;

import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.enums.FundsBalanceAdjustmentAuditCompleteness;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 余额调账审计查询结果。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsBalanceAdjustmentAuditDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -4625844318480070535L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "资金交易流水")
    private String fundsTransactionSn;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水")
    private String businessSn;

    @Schema(description = "资金交易类型")
    private DefaultFundsTransactionType transactionType;

    @Schema(description = "资金交易状态")
    private FundsTransactionState transactionState;

    @Schema(description = "交易金额，单位：最小币种单位")
    private Long amount;

    @Schema(description = "交易币种")
    private CurrencyIsoCode currency;

    @Schema(description = "审计链路完整性")
    private FundsBalanceAdjustmentAuditCompleteness auditCompleteness;

    @Schema(description = "是否存在持久化 RouteSnapshot")
    private boolean routeSnapshotPresent;

    @Schema(description = "是否存在账本交易和账本分录事实")
    private boolean ledgerFactsPresent;

    @Schema(description = "主账本交易流水")
    private String primaryLedgerTransactionSn;

    @Schema(description = "关联账本交易数量")
    private int ledgerTransactionCount;

    @Schema(description = "关联账本分录数量")
    private int ledgerEntryCount;

    @Schema(description = "关联账本交易流水列表")
    private List<String> ledgerTransactionSns = List.of();

    @Schema(description = "账本分录审计摘要列表")
    private List<LedgerEntryAuditDTO> ledgerEntries = List.of();

    @Schema(description = "过滤敏感字段后的审计上下文变量")
    private Map<String, Object> auditContextVariables = Map.of();

    public FundsBalanceAdjustmentAuditDTO setLedgerTransactionSns(List<String> ledgerTransactionSns) {
        this.ledgerTransactionSns = ledgerTransactionSns == null ? List.of() : List.copyOf(ledgerTransactionSns);
        return this;
    }

    public FundsBalanceAdjustmentAuditDTO setLedgerEntries(List<LedgerEntryAuditDTO> ledgerEntries) {
        this.ledgerEntries = ledgerEntries == null ? List.of() : List.copyOf(ledgerEntries);
        return this;
    }

    public FundsBalanceAdjustmentAuditDTO setAuditContextVariables(Map<String, Object> auditContextVariables) {
        this.auditContextVariables = auditContextVariables == null ? Map.of() : Map.copyOf(auditContextVariables);
        return this;
    }

    /**
     * 账本分录审计摘要。
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    @ToString
    @Accessors(chain = true)
    public static class LedgerEntryAuditDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 8706173561761586241L;

        @Schema(description = "账本分录流水")
        private String ledgerEntrySn;

        @Schema(description = "账本交易流水")
        private String ledgerTransactionSn;

        @Schema(description = "资金交易流水")
        private String fundsTransactionSn;

        @Schema(description = "业务场景")
        private String businessScene;

        @Schema(description = "业务流水")
        private String businessSn;

        @Schema(description = "账务主体 ID")
        private String subjectId;

        @Schema(description = "账务主体类型")
        private String subjectType;

        @Schema(description = "账目编码")
        private LedgerSubjectCode ledgerSubjectCode;

        @Schema(description = "账目类别")
        private LedgerSubjectCategory ledgerSubjectCategory;

        @Schema(description = "借贷方向")
        private EntrySide entryType;

        @Schema(description = "余额约束类型")
        private LedgerBalanceConstraintType balanceConstraintType;

        @Schema(description = "余额影响语义")
        private LedgerBalanceEffectType balanceEffectType;

        @Schema(description = "记账金额，单位：最小币种单位")
        private Long amount;

        @Schema(description = "记账币种")
        private String currency;
    }
}

package com.wind.funds.support;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.spec.ledger.LedgerEntrySpec;
import com.wind.transaction.core.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 账本余额投影测试夹具。
 */
public final class LedgerProjectionTestFixture {

    private LedgerProjectionTestFixture() {
    }

    public static LedgerEntrySpec balanceEntry(LedgerDTO ledger, EntrySide entrySide, long amount) {
        return new BalanceEntry(
                ledger.getSubjectId(),
                ledger.getSubjectType(),
                ledger.getLedgerSubjectCode(),
                ledger.getLedgerSubjectCategory(),
                ledger.getId(),
                ledger.getPeriodType(),
                ledger.getPeriodId(),
                entrySide,
                Money.immutable(amount, ledger.getCurrency()));
    }

    private record BalanceEntry(String subjectId,
                                String subjectType,
                                LedgerSubjectCode ledgerSubjectCode,
                                LedgerSubjectCategory ledgerSubjectCategory,
                                Long ledgerId,
                                AccountBalancePeriodType periodType,
                                String periodId,
                                EntrySide entryType,
                                Money amount) implements LedgerEntrySpec {

        @Override
        public String getSubjectId() {
            return subjectId;
        }

        @Override
        public String getSubjectType() {
            return subjectType;
        }

        @Override
        public LedgerSubjectCode getLedgerSubjectCode() {
            return ledgerSubjectCode;
        }

        @Override
        public LedgerSubjectCategory getLedgerSubjectCategory() {
            return ledgerSubjectCategory;
        }

        @Override
        public Long getLedgerId() {
            return ledgerId;
        }

        @Override
        public AccountBalancePeriodType getPeriodType() {
            return periodType;
        }

        @Override
        public String getPeriodId() {
            return periodId;
        }

        @Override
        public String getLedgerTransactionSn() {
            return "TEST-BALANCE-" + ledgerId;
        }

        @Override
        public EntrySide getEntryType() {
            return entryType;
        }

        @Override
        public LedgerPostingRole getPostingRole() {
            return LedgerPostingRole.DETAIL;
        }

        @Override
        public String getBusinessScene() {
            return "TEST_LEDGER_BALANCE_FIXTURE";
        }

        @Override
        public String getBusinessSn() {
            return "TEST-BALANCE-" + ledgerId;
        }

        @Override
        public Money getAmount() {
            return amount;
        }

        @Override
        public Money getOriginalAmount() {
            return amount;
        }

        @Override
        public BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return LocalDateTime.of(2026, 8, 3, 0, 0);
        }

        @Override
        public String getDescription() {
            return "ledger balance projection test fixture";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }
}

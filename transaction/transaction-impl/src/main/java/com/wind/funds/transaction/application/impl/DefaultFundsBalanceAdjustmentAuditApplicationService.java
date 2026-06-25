package com.wind.funds.transaction.application.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.application.LedgerFactQueryApplicationService;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.application.FundsBalanceAdjustmentAuditApplicationService;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.enums.FundsBalanceAdjustmentAuditCompleteness;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.model.dto.FundsBalanceAdjustmentAuditDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.query.FundsBalanceAdjustmentAuditQuery;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 默认余额调账审计查询应用服务。
 *
 * <p>该实现只读取资金交易事实、RouteSnapshot、账本交易和账本分录，不直接访问 Mapper，
 * 不补写或修复任何交易、账本、余额投影事实。</p>
 *
 * @author Codex
 * @date 2026-06-19
 */
@Service
@AllArgsConstructor
public class DefaultFundsBalanceAdjustmentAuditApplicationService
        implements FundsBalanceAdjustmentAuditApplicationService {

    private static final int AUDIT_QUERY_PAGE_SIZE = 50;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final LedgerFactQueryApplicationService ledgerFactQueryApplicationService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull Optional<FundsBalanceAdjustmentAuditDTO> findByBusinessSn(
            @NonNull FundsBalanceAdjustmentAuditQuery query) {
        assertBusinessSnQuery(query);
        Optional<FundsTransactionDTO> transaction = fundsTransactionQueryService.findFundsTransactionByBusiness(
                query.getTenantId(), query.getBusinessScene(), query.getBusinessSn());
        if (transaction.isEmpty()) {
            return Optional.empty();
        }
        return auditTransaction(query.getTenantId(), transaction.get());
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull Optional<FundsBalanceAdjustmentAuditDTO> findByTransactionSn(
            @NonNull FundsBalanceAdjustmentAuditQuery query) {
        assertTransactionSnQuery(query);
        Optional<FundsTransactionDTO> transaction = fundsTransactionQueryService.queryFundsTransaction(
                query.getFundsTransactionSn());
        if (transaction.isEmpty()) {
            return Optional.empty();
        }
        FundsTransactionDTO transactionDTO = transaction.get();
        if (transactionDTO.getTransactionType() != DefaultFundsTransactionType.ADJUSTMENT) {
            return Optional.empty();
        }
        return auditTransaction(query.getTenantId(), transactionDTO);
    }

    private Optional<FundsBalanceAdjustmentAuditDTO> auditTransaction(Long tenantId,
                                                                      FundsTransactionDTO transactionDTO) {
        if (transactionDTO.getTransactionType() != DefaultFundsTransactionType.ADJUSTMENT) {
            return Optional.empty();
        }
        assertTenantMatched(tenantId, transactionDTO.getTenantId());
        List<FundsTransactionDetailDTO> details = fundsTransactionQueryService.queryFundsTransactionDetails(
                transactionDTO.getSn());
        List<LedgerTransactionDTO> ledgerTransactions = queryLedgerTransactions(transactionDTO.getTenantId(),
                transactionDTO.getSn());
        List<LedgerEntryDTO> ledgerEntries = queryLedgerEntries(transactionDTO.getTenantId(), ledgerTransactions);
        Optional<RouteSnapshotSpec> routeSnapshot = fundsTransactionQueryService.findRouteSnapshotByTransactionSn(
                transactionDTO.getSn());
        FundsBalanceAdjustmentAuditCompleteness completeness = completeness(routeSnapshot.isPresent(),
                !ledgerTransactions.isEmpty(), !ledgerEntries.isEmpty());
        return Optional.of(new FundsBalanceAdjustmentAuditDTO()
                .setTenantId(transactionDTO.getTenantId())
                .setFundsTransactionSn(transactionDTO.getSn())
                .setBusinessScene(transactionDTO.getBusinessScene())
                .setBusinessSn(transactionDTO.getBusinessSn())
                .setTransactionType(transactionDTO.getTransactionType())
                .setTransactionStatus(transactionDTO.getStatus())
                .setAmount(transactionDTO.getAmount())
                .setCurrency(transactionDTO.getCurrency())
                .setAuditCompleteness(completeness)
                .setRouteSnapshotPresent(routeSnapshot.isPresent())
                .setLedgerFactsPresent(!ledgerTransactions.isEmpty() && !ledgerEntries.isEmpty())
                .setPrimaryLedgerTransactionSn(primaryLedgerTransactionSn(ledgerTransactions, details))
                .setLedgerTransactionCount(ledgerTransactions.size())
                .setLedgerEntryCount(ledgerEntries.size())
                .setLedgerTransactionSns(ledgerTransactions.stream()
                        .map(LedgerTransactionDTO::getSn)
                        .toList())
                .setLedgerEntries(ledgerEntries.stream()
                        .map(this::toEntryAudit)
                        .toList())
                .setAuditContextVariables(auditContext(transactionDTO, details, routeSnapshot)));
    }

    private void assertBusinessSnQuery(FundsBalanceAdjustmentAuditQuery query) {
        AssertUtils.notNull(query, "余额调账审计查询条件不能为空");
        assertTenant(query.getTenantId());
        AssertUtils.hasText(query.getBusinessScene(), "余额调账审计业务场景不能为空");
        AssertUtils.hasText(query.getBusinessSn(), "余额调账审计业务流水不能为空");
    }

    private void assertTransactionSnQuery(FundsBalanceAdjustmentAuditQuery query) {
        AssertUtils.notNull(query, "余额调账审计查询条件不能为空");
        assertTenant(query.getTenantId());
        AssertUtils.hasText(query.getFundsTransactionSn(), "余额调账审计资金交易流水不能为空");
    }

    private void assertTenant(Long tenantId) {
        AssertUtils.notNull(tenantId, "余额调账审计 tenantId 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), tenantId,
                "余额调账审计 tenantId 与当前租户不一致");
    }

    private void assertTenantMatched(Long queryTenantId, Long transactionTenantId) {
        AssertUtils.equals(queryTenantId, transactionTenantId,
                "余额调账审计交易租户不匹配");
    }

    private List<LedgerTransactionDTO> queryLedgerTransactions(Long tenantId, String transactionSn) {
        return ledgerFactQueryApplicationService.queryLedgerTransactions(new LedgerTransactionQuery()
                        .setTenantId(tenantId)
                        .setFundsTransactionSn(transactionSn),
                DefaultPageQueryOptions.defaults(AUDIT_QUERY_PAGE_SIZE))
                .getRecords();
    }

    private List<LedgerEntryDTO> queryLedgerEntries(Long tenantId, List<LedgerTransactionDTO> ledgerTransactions) {
        return ledgerTransactions.stream()
                .flatMap(ledgerTransaction -> ledgerFactQueryApplicationService.queryLedgerEntries(new LedgerEntryQuery()
                                        .setTenantId(tenantId)
                                        .setLedgerTransactionSn(ledgerTransaction.getSn()),
                                DefaultPageQueryOptions.defaults(AUDIT_QUERY_PAGE_SIZE))
                        .getRecords()
                        .stream())
                .toList();
    }

    private FundsBalanceAdjustmentAuditCompleteness completeness(boolean routeSnapshotPresent,
                                                                 boolean ledgerTransactionPresent,
                                                                 boolean ledgerEntryPresent) {
        if (!routeSnapshotPresent) {
            return FundsBalanceAdjustmentAuditCompleteness.INCOMPLETE_ROUTE;
        }
        if (!ledgerTransactionPresent || !ledgerEntryPresent) {
            return FundsBalanceAdjustmentAuditCompleteness.INCOMPLETE_LEDGER;
        }
        return FundsBalanceAdjustmentAuditCompleteness.COMPLETE;
    }

    private @Nullable String primaryLedgerTransactionSn(List<LedgerTransactionDTO> ledgerTransactions,
                                                        List<FundsTransactionDetailDTO> details) {
        Optional<String> detailLedgerTransactionSn = details.stream()
                .map(FundsTransactionDetailDTO::getLedgerTransactionSn)
                .filter(StringUtils::hasText)
                .findFirst();
        return detailLedgerTransactionSn.orElseGet(() -> ledgerTransactions.isEmpty()
                ? null : ledgerTransactions.getFirst().getSn());
    }

    private FundsBalanceAdjustmentAuditDTO.LedgerEntryAuditDTO toEntryAudit(LedgerEntryDTO entry) {
        return new FundsBalanceAdjustmentAuditDTO.LedgerEntryAuditDTO()
                .setLedgerEntrySn(entry.getSn())
                .setLedgerTransactionSn(entry.getLedgerTransactionSn())
                .setFundsTransactionSn(entry.getFundsTransactionSn())
                .setBusinessScene(entry.getBusinessScene())
                .setBusinessSn(entry.getBusinessSn())
                .setSubjectId(entry.getSubjectId())
                .setSubjectType(entry.getSubjectType())
                .setLedgerSubjectCode(entry.getLedgerSubjectCode())
                .setLedgerSubjectCategory(entry.getLedgerSubjectCategory())
                .setEntryType(entry.getEntryType())
                .setBalanceConstraintType(entry.getBalanceConstraintType())
                .setBalanceEffectType(entry.getBalanceEffectType())
                .setAmount(entry.getAmount() == null ? null : entry.getAmount().getAmount())
                .setCurrency(entry.getAmount() == null ? null : entry.getAmount().getCurrency().name());
    }

    private Map<String, Object> auditContext(FundsTransactionDTO transaction,
                                             List<FundsTransactionDetailDTO> details,
                                             Optional<RouteSnapshotSpec> routeSnapshot) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.putAll(safeContext(parseContext(transaction.getContextVariables())));
        details.stream()
                .map(FundsTransactionDetailDTO::getContextVariables)
                .map(this::parseContext)
                .map(this::safeContext)
                .forEach(result::putAll);
        routeSnapshot.map(RouteSnapshotSpec::getContextVariables)
                .map(this::safeContext)
                .ifPresent(result::putAll);
        return Map.copyOf(result);
    }

    private Map<String, Object> parseContext(@Nullable String contextVariables) {
        if (!StringUtils.hasText(contextVariables)) {
            return Map.of();
        }
        JSONObject values = JSON.parseObject(contextVariables);
        return values == null ? Map.of() : values;
    }

    private Map<String, Object> safeContext(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value != null && !sensitiveContextKey(key)) {
                result.put(key, value);
            }
        });
        return result;
    }

    private boolean sensitiveContextKey(String key) {
        if (!StringUtils.hasText(key)) {
            return true;
        }
        if (FundsInstructionContextKeys.EXTERNAL_ACCOUNT_REF.equals(key)) {
            return true;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("pan")
                || normalized.contains("cvv")
                || normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("rawpayload");
    }
}

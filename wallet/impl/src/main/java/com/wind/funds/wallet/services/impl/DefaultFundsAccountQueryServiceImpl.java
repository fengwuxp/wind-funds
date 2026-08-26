package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountOwner;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.ImmutableFundsAccount;
import com.wind.funds.wallet.ImmutableFundsBalanceView;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.query.CreditAccountQuery;
import com.wind.funds.wallet.model.query.FundingAccountQuery;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 v2 资金主体表和 ledger 投影的账户查询服务。
 *
 */
@Service
@AllArgsConstructor
public class DefaultFundsAccountQueryServiceImpl implements FundsAccountQueryService, FundsSubjectBalanceQueryService {

    private static final int MAX_LEDGER_BUCKET_SIZE = 50;

    private static final int MAX_ACCOUNT_MATCH_SIZE = 2;

    private final FundingAccountService fundingAccountService;

    private final CreditAccountService creditAccountService;

    private final LedgerService ledgerService;

    @Override
    public @NonNull FundsAccount getAccount(@NonNull Long tenantId, @NonNull FundsAccountId accountId) {
        ResolvedFundsSubject subject = findRequired(tenantId, accountId);
        FundsAccountCapabilitySourceResolver.AccountCapabilityResolution capabilityResolution =
                FundsAccountCapabilitySourceResolver.resolve(
                        subject.ledgerProfileCode(),
                        subject.contextVariables());
        return ImmutableFundsAccount.builder()
                .id(subject.id())
                .tenantId(subject.tenantId())
                .accountId(accountId)
                .owner(FundsAccountOwner.of(subject.ownerId(), subject.ownerType()))
                .state(subject.state())
                .currency(subject.currency())
                .capabilities(capabilityResolution.capabilities())
                .capabilitySource(capabilityResolution.source())
                .version(subject.version())
                .build();
    }

    @Override
    public @NonNull LedgerProfileCode getLedgerProfileCode(@NonNull Long tenantId,
                                                            @NonNull FundsAccountId accountId) {
        return findRequired(tenantId, accountId).ledgerProfileCode();
    }

    @Override
    public @NonNull FundsAccountBalanceView getBalance(@NonNull Long tenantId, @NonNull FundsAccountId accountId) {
        ResolvedFundsSubject subject = findRequired(tenantId, accountId);
        List<LedgerDTO> ledgers = loadLedgers(subject);
        return ImmutableFundsBalanceView.builder()
                .id(subject.id())
                .tenantId(subject.tenantId())
                .accountId(accountId)
                .currency(subject.currency())
                .ledgerProfileCode(subject.ledgerProfileCode())
                .balanceBuckets(toBalanceBuckets(ledgers, subject.currency()))
                .build();
    }

    @Override
    public boolean supports(@NonNull Long tenantId, @NonNull FundsAccountId accountId) {
        return findNullable(tenantId, accountId) != null;
    }

    @Override
    public @NonNull List<FundsSubjectBalanceDTO> queryCurrentBalances(@NonNull FundsSubjectBalanceQuery query) {
        validateQuery(query);
        return query.getSubjectRefs().stream()
                .map(subjectRef -> queryCurrentBalance(query, subjectRef, false))
                .toList();
    }

    @Override
    public @NonNull FundsSubjectBalanceDTO getRequiredCurrentBalance(@NonNull FundsSubjectBalanceQuery query) {
        validateQuery(query);
        AssertUtils.isTrue(query.getSubjectRefs().size() == 1,
                "资金主体必需余额查询 subjectRefs 只能包含一个主体");
        return queryCurrentBalance(query, query.getSubjectRefs().getFirst(), true);
    }

    private ResolvedFundsSubject findRequired(Long tenantId, FundsAccountId accountId) {
        ResolvedFundsSubject result = findNullable(tenantId, accountId);
        AssertUtils.notNull(result, "资金主体不存在，accountId = {}", accountId);
        return result;
    }

    @Nullable
    private ResolvedFundsSubject findNullable(Long tenantId, FundsAccountId accountId) {
        FundsSubjectType subjectType = parseSubjectType(accountId.type());
        if (subjectType != null) {
            return findBySubjectType(tenantId, subjectType, accountId.id());
        }
        FundingAccountDTO fundingAccount = findFundingAccount(tenantId, accountId);
        if (fundingAccount != null) {
            return ResolvedFundsSubject.from(fundingAccount);
        }
        CreditAccountDTO creditAccount = findCreditAccount(tenantId, accountId);
        if (creditAccount != null) {
            return ResolvedFundsSubject.from(creditAccount);
        }
        return null;
    }

    @Nullable
    private ResolvedFundsSubject findBySubjectType(Long tenantId,
                                                   FundsSubjectType subjectType,
                                                   String subjectId) {
        return switch (subjectType) {
            case FUNDING_ACCOUNT -> {
                FundingAccountDTO account = findFundingAccountBySn(tenantId, subjectId);
                yield account == null ? null : ResolvedFundsSubject.from(account);
            }
            case CREDIT_ACCOUNT -> {
                CreditAccountDTO account = findCreditAccountBySn(tenantId, subjectId);
                yield account == null ? null : ResolvedFundsSubject.from(account);
            }
        };
    }

    @Nullable
    private FundsSubjectType parseSubjectType(String subjectType) {
        for (FundsSubjectType candidate : FundsSubjectType.values()) {
            if (candidate.name().equals(subjectType)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private FundingAccountDTO findFundingAccountBySn(Long tenantId, String sn) {
        return querySingleFundingAccount(new FundingAccountQuery()
                .setTenantId(tenantId)
                .setSn(sn));
    }

    @Nullable
    private FundingAccountDTO findFundingAccount(Long tenantId, FundsAccountId accountId) {
        return querySingleFundingAccount(new FundingAccountQuery()
                .setTenantId(tenantId)
                .setSn(accountId.id())
                .setAccountType(accountId.type()));
    }

    @Nullable
    private CreditAccountDTO findCreditAccount(Long tenantId, FundsAccountId accountId) {
        return querySingleCreditAccount(new CreditAccountQuery()
                .setTenantId(tenantId)
                .setSn(accountId.id())
                .setAccountType(accountId.type()));
    }

    @Nullable
    private CreditAccountDTO findCreditAccountBySn(Long tenantId, String sn) {
        return querySingleCreditAccount(new CreditAccountQuery()
                .setTenantId(tenantId)
                .setSn(sn));
    }

    @Nullable
    private FundingAccountDTO querySingleFundingAccount(FundingAccountQuery query) {
        List<FundingAccountDTO> records = fundingAccountService
                .queryFundingAccounts(query, DefaultPageQueryOptions.result(MAX_ACCOUNT_MATCH_SIZE))
                .getRecords();
        return records.isEmpty() ? null : records.getFirst();
    }

    @Nullable
    private CreditAccountDTO querySingleCreditAccount(CreditAccountQuery query) {
        List<CreditAccountDTO> records = creditAccountService
                .queryCreditAccounts(query, DefaultPageQueryOptions.result(MAX_ACCOUNT_MATCH_SIZE))
                .getRecords();
        return records.isEmpty() ? null : records.getFirst();
    }

    private FundsSubjectBalanceDTO queryCurrentBalance(FundsSubjectBalanceQuery query,
                                                       FundsAccountId subjectRef,
                                                       boolean requireCompleteLedger) {
        ResolvedFundsSubject subject = findRequired(query.getTenantId(), subjectRef);
        AssertUtils.isTrue(Objects.equals(subject.tenantId(), query.getTenantId()),
                "资金主体租户不匹配，accountId = {}，tenantId = {}", subjectRef, query.getTenantId());
        AssertUtils.isTrue(subject.currency() == query.getCurrency(),
                "资金主体币种不匹配，accountId = {}，currency = {}", subjectRef, query.getCurrency());
        List<LedgerDTO> ledgers = loadLedgers(subject, query);
        if (requireCompleteLedger) {
            assertRequiredLedgersPresent(subjectRef, ledgers, query.getLedgerSubjectCodes());
        }
        List<LedgerDTO> selectedLedgers = filterLedgers(ledgers, query.getLedgerSubjectCodes());
        return new FundsSubjectBalanceDTO()
                .setId(subject.id())
                .setTenantId(subject.tenantId())
                .setSubjectRef(subjectRef)
                .setCurrency(subject.currency())
                .setInitialized(!ledgers.isEmpty())
                .setBalanceBuckets(toBalanceBuckets(selectedLedgers, subject.currency()));
    }

    private void validateQuery(FundsSubjectBalanceQuery query) {
        AssertUtils.notNull(query.getTenantId(), "资金主体余额查询 tenantId 不能为空");
        AssertUtils.notNull(query.getCurrency(), "资金主体余额查询 currency 不能为空");
        AssertUtils.notEmpty(query.getSubjectRefs(), "资金主体余额查询 subjectRefs 不能为空");
        AssertUtils.notNull(query.getPeriodType(), "资金主体余额查询 periodType 不能为空");
        if (query.getPeriodType() != AccountBalancePeriodType.LIFETIME) {
            AssertUtils.hasText(query.getPeriodId(), "资金主体余额查询 periodId 不能为空");
        }
        for (FundsAccountId subjectRef : query.getSubjectRefs()) {
            AssertUtils.notNull(subjectRef, "资金主体余额查询 subjectRefs 不能包含空值");
        }
        Set<String> subjectRefs = query.getSubjectRefs().stream()
                .map(this::subjectRefKey)
                .collect(Collectors.toCollection(HashSet::new));
        AssertUtils.isTrue(subjectRefs.size() == query.getSubjectRefs().size(),
                "资金主体余额查询 subjectRefs 不能重复");
    }

    private String subjectRefKey(FundsAccountId subjectRef) {
        return subjectRef.id() + "#" + subjectRef.type();
    }

    private void assertRequiredLedgersPresent(FundsAccountId subjectRef,
                                              List<LedgerDTO> ledgers,
                                              List<LedgerSubjectCode> requiredSubjectCodes) {
        AssertUtils.notEmpty(ledgers, "资金主体账本不存在，accountId = {}", subjectRef);
        if (requiredSubjectCodes == null || requiredSubjectCodes.isEmpty()) {
            return;
        }
        Set<LedgerSubjectCode> actual = ledgers.stream()
                .map(LedgerDTO::getLedgerSubjectCode)
                .collect(Collectors.toSet());
        AssertUtils.isTrue(actual.containsAll(requiredSubjectCodes),
                "资金主体账本不完整，accountId = {}，ledgerSubjectCodes = {}", subjectRef, requiredSubjectCodes);
    }

    private List<LedgerDTO> loadLedgers(ResolvedFundsSubject subject) {
        LedgerQuery query = baseLedgerQuery(subject);
        applyDefaultAccountPeriod(query, subject);
        return queryLedgers(query);
    }

    private List<LedgerDTO> loadLedgers(ResolvedFundsSubject subject, @Nullable FundsSubjectBalanceQuery balanceQuery) {
        LedgerQuery query = baseLedgerQuery(subject);
        if (balanceQuery != null) {
            query.setPeriodType(balanceQuery.getPeriodType());
            query.setPeriodId(resolvePeriodId(balanceQuery));
        }
        return queryLedgers(query);
    }

    private LedgerQuery baseLedgerQuery(ResolvedFundsSubject subject) {
        return new LedgerQuery()
                .setTenantId(subject.tenantId())
                .setSubjectId(subject.subjectId())
                .setSubjectType(subject.subjectType().name())
                .setCurrency(subject.currency());
    }

    private void applyDefaultAccountPeriod(LedgerQuery query, ResolvedFundsSubject subject) {
        query.setPeriodType(subject.periodType());
        if (subject.periodType() == AccountBalancePeriodType.LIFETIME) {
            query.setPeriodId(AccountBalancePeriodType.LIFETIME.name());
        } else {
            query.setPeriodId(subject.periodId());
        }
    }

    private List<LedgerDTO> queryLedgers(LedgerQuery query) {
        return ledgerService.queryLedgers(query, DefaultPageQueryOptions.result(MAX_LEDGER_BUCKET_SIZE))
                .getRecords()
                .stream()
                .toList();
    }

    @Nullable
    private String resolvePeriodId(FundsSubjectBalanceQuery balanceQuery) {
        if (StringUtils.hasText(balanceQuery.getPeriodId())) {
            return balanceQuery.getPeriodId();
        }
        return balanceQuery.getPeriodType() == AccountBalancePeriodType.LIFETIME
                ? AccountBalancePeriodType.LIFETIME.name() : null;
    }

    private boolean containsLedgerSubjectCode(@Nullable Collection<LedgerSubjectCode> ledgerSubjectCodes,
                                              LedgerSubjectCode ledgerSubjectCode) {
        return ledgerSubjectCodes == null || ledgerSubjectCodes.isEmpty() || ledgerSubjectCodes.contains(ledgerSubjectCode);
    }

    private List<LedgerDTO> filterLedgers(List<LedgerDTO> ledgers,
                                          @Nullable Collection<LedgerSubjectCode> ledgerSubjectCodes) {
        return ledgers.stream()
                .filter(ledger -> containsLedgerSubjectCode(ledgerSubjectCodes, ledger.getLedgerSubjectCode()))
                .toList();
    }

    private Map<LedgerSubjectCode, LedgerBalanceBucket> toBalanceBuckets(List<LedgerDTO> ledgers,
                                                                         CurrencyIsoCode currency) {
        Map<LedgerSubjectCode, LedgerBalanceBucket> result = new EnumMap<>(LedgerSubjectCode.class);
        for (LedgerDTO ledger : ledgers) {
            result.put(ledger.getLedgerSubjectCode(), LedgerBalanceBucket.builder()
                    .ledgerSubjectCode(ledger.getLedgerSubjectCode())
                    .balance(Money.immutable(balanceValue(ledger), currency))
                    .periodType(ledger.getPeriodType())
                    .periodId(ledger.getPeriodId())
                    .activeTime(activeTime(ledger))
                    .build());
        }
        return result;
    }

    private Long balanceValue(LedgerDTO ledger) {
        Long result = ledger.getNormalBalance();
        return result == null ? 0L : result;
    }

    private LocalDateTime activeTime(LedgerDTO ledger) {
        return ledger.getGmtCreate() == null ? LocalDateTime.now() : ledger.getGmtCreate();
    }

    private record ResolvedFundsSubject(
            Long id,
            Long tenantId,
            String subjectId,
            FundsSubjectType subjectType,
            String ownerId,
            FundsAccountOwnerType ownerType,
            FundsAccountState state,
            CurrencyIsoCode currency,
            LedgerProfileCode ledgerProfileCode,
            AccountBalancePeriodType periodType,
            String periodId,
            String contextVariables,
            Integer version
    ) {

        static ResolvedFundsSubject from(FundingAccountDTO account) {
            return new ResolvedFundsSubject(
                    account.getId(),
                    account.getTenantId(),
                    account.getSn(),
                    FundsSubjectType.FUNDING_ACCOUNT,
                    account.getOwnerId(),
                    account.getOwnerType(),
                    account.getState(),
                    account.getCurrency(),
                    account.getLedgerProfileCode(),
                    AccountBalancePeriodType.LIFETIME,
                    AccountBalancePeriodType.LIFETIME.name(),
                    account.getContextVariables(),
                    account.getVersion()
            );
        }

        static ResolvedFundsSubject from(CreditAccountDTO account) {
            return new ResolvedFundsSubject(
                    account.getId(),
                    account.getTenantId(),
                    account.getSn(),
                    FundsSubjectType.CREDIT_ACCOUNT,
                    account.getOwnerId(),
                    account.getOwnerType(),
                    account.getState(),
                    account.getCurrency(),
                    account.getLedgerProfileCode(),
                    account.getPeriodType(),
                    account.getPeriodId(),
                    account.getContextVariables(),
                    account.getVersion()
            );
        }

    }
}

package com.capte.funds.wallet.services.impl;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.ImmutableFundsAccount;
import com.capte.funds.wallet.ImmutableFundsBalanceView;
import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.entities.CreditAccount;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.entities.table.BudgetGroupNameRefs;
import com.capte.funds.wallet.dal.entities.table.CreditAccountNameRefs;
import com.capte.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.capte.funds.wallet.dal.mapper.BudgetGroupMapper;
import com.capte.funds.wallet.dal.mapper.CreditAccountMapper;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.route.enums.FundsSubjectType;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.capte.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountOwner;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
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
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class DefaultFundsAccountQueryServiceImpl implements FundsAccountQueryService, FundsSubjectBalanceQueryService {

    private static final int MAX_LEDGER_BUCKET_SIZE = 50;

    private final FundingAccountMapper fundingAccountMapper;

    private final CreditAccountMapper creditAccountMapper;

    private final BudgetGroupMapper budgetGroupMapper;

    private final LedgerService ledgerService;

    @Override
    public @NonNull FundsAccount getAccount(@NonNull FundsAccountId accountId) {
        ResolvedFundsSubject subject = findRequired(accountId);
        return ImmutableFundsAccount.builder()
                .id(subject.id())
                .tenantId(subject.tenantId())
                .accountId(accountId)
                .owner(FundsAccountOwner.of(subject.ownerId(), subject.ownerType()))
                .status(subject.status())
                .currency(subject.currency())
                .accountLedgerIds(loadLedgerIds(subject))
                .version(subject.version())
                .build();
    }

    @Override
    public @NonNull FundsAccountBalanceView getBalance(@NonNull FundsAccountId accountId) {
        ResolvedFundsSubject subject = findRequired(accountId);
        List<LedgerDTO> ledgers = loadLedgers(subject);
        return ImmutableFundsBalanceView.builder()
                .id(subject.id())
                .tenantId(subject.tenantId())
                .accountId(accountId)
                .currency(subject.currency())
                .balanceBuckets(toBalanceBuckets(ledgers, subject.currency()))
                .build();
    }

    @Override
    public boolean supports(@NonNull FundsAccountId accountId) {
        return findNullable(accountId) != null;
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

    private ResolvedFundsSubject findRequired(FundsAccountId accountId) {
        ResolvedFundsSubject result = findNullable(accountId);
        AssertUtils.notNull(result, "资金主体不存在，accountId = {}", accountId);
        return result;
    }

    @Nullable
    private ResolvedFundsSubject findNullable(FundsAccountId accountId) {
        FundsSubjectType subjectType = parseSubjectType(accountId.type());
        if (subjectType != null) {
            return findBySubjectType(subjectType, accountId.id());
        }
        FundingAccount fundingAccount = selectFundingAccount(accountId);
        if (fundingAccount != null) {
            return ResolvedFundsSubject.from(fundingAccount);
        }
        CreditAccount creditAccount = selectCreditAccount(accountId);
        if (creditAccount != null) {
            return ResolvedFundsSubject.from(creditAccount);
        }
        BudgetGroup budgetGroup = selectBudgetGroup(accountId);
        return budgetGroup == null ? null : ResolvedFundsSubject.from(budgetGroup);
    }

    @Nullable
    private ResolvedFundsSubject findBySubjectType(FundsSubjectType subjectType, String subjectId) {
        return switch (subjectType) {
            case FUNDING_ACCOUNT -> {
                FundingAccount account = selectFundingAccountBySn(subjectId);
                yield account == null ? null : ResolvedFundsSubject.from(account);
            }
            case CREDIT_ACCOUNT -> {
                CreditAccount account = selectCreditAccountBySn(subjectId);
                yield account == null ? null : ResolvedFundsSubject.from(account);
            }
            case BUDGET_GROUP -> {
                BudgetGroup account = selectBudgetGroupBySn(subjectId);
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
    private FundingAccount selectFundingAccountBySn(String sn) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(sn));
        return fundingAccountMapper.selectOneByQuery(wrapper);
    }

    @Nullable
    private FundingAccount selectFundingAccount(FundsAccountId accountId) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(accountId.id()))
                .and(ref.accountType.eq(accountId.type()));
        return fundingAccountMapper.selectOneByQuery(wrapper);
    }

    @Nullable
    private CreditAccount selectCreditAccount(FundsAccountId accountId) {
        CreditAccountNameRefs ref = CreditAccountNameRefs.creditAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(accountId.id()))
                .and(ref.accountType.eq(accountId.type()));
        return creditAccountMapper.selectOneByQuery(wrapper);
    }

    @Nullable
    private CreditAccount selectCreditAccountBySn(String sn) {
        CreditAccountNameRefs ref = CreditAccountNameRefs.creditAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(sn));
        return creditAccountMapper.selectOneByQuery(wrapper);
    }

    @Nullable
    private BudgetGroup selectBudgetGroup(FundsAccountId accountId) {
        BudgetGroupNameRefs ref = BudgetGroupNameRefs.budgetGroup;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(accountId.id()))
                .and(ref.budgetType.eq(accountId.type()));
        return budgetGroupMapper.selectOneByQuery(wrapper);
    }

    @Nullable
    private BudgetGroup selectBudgetGroupBySn(String sn) {
        BudgetGroupNameRefs ref = BudgetGroupNameRefs.budgetGroup;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(sn));
        return budgetGroupMapper.selectOneByQuery(wrapper);
    }

    private Map<LedgerSubjectCode, Long> loadLedgerIds(ResolvedFundsSubject subject) {
        return loadLedgers(subject).stream()
                .collect(Collectors.toMap(LedgerDTO::getLedgerSubjectCode, LedgerDTO::getId));
    }

    private FundsSubjectBalanceDTO queryCurrentBalance(FundsSubjectBalanceQuery query,
                                                       FundsAccountId subjectRef,
                                                       boolean requireCompleteLedger) {
        ResolvedFundsSubject subject = findRequired(subjectRef);
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
        return ledgerService.queryLedgers(query, DefaultPageQueryOptions.defaults(MAX_LEDGER_BUCKET_SIZE))
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
                    .accountCode(ledger.getLedgerSubjectCode())
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
            FundsAccountStatus status,
            CurrencyIsoCode currency,
            AccountBalancePeriodType periodType,
            String periodId,
            Integer version
    ) {

        static ResolvedFundsSubject from(FundingAccount account) {
            return new ResolvedFundsSubject(
                    account.getId(),
                    account.getTenantId(),
                    account.getSn(),
                    FundsSubjectType.FUNDING_ACCOUNT,
                    account.getOwnerId(),
                    account.getOwnerType(),
                    account.getStatus(),
                    account.getCurrency(),
                    AccountBalancePeriodType.LIFETIME,
                    AccountBalancePeriodType.LIFETIME.name(),
                    account.getVersion()
            );
        }

        static ResolvedFundsSubject from(CreditAccount account) {
            return new ResolvedFundsSubject(
                    account.getId(),
                    account.getTenantId(),
                    account.getSn(),
                    FundsSubjectType.CREDIT_ACCOUNT,
                    account.getOwnerId(),
                    account.getOwnerType(),
                    account.getStatus(),
                    account.getCurrency(),
                    account.getPeriodType(),
                    account.getPeriodId(),
                    account.getVersion()
            );
        }

        static ResolvedFundsSubject from(BudgetGroup account) {
            return new ResolvedFundsSubject(
                    account.getId(),
                    account.getTenantId(),
                    account.getSn(),
                    FundsSubjectType.BUDGET_GROUP,
                    account.getOwnerId(),
                    account.getOwnerType(),
                    account.getStatus(),
                    account.getCurrency(),
                    account.getPeriodType(),
                    account.getPeriodId(),
                    account.getVersion()
            );
        }
    }
}

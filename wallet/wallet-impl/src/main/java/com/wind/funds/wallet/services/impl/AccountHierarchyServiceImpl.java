package com.wind.funds.wallet.services.impl;

import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.model.route.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.route.AccountHierarchySnapshotResolver;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.dal.entities.AccountHierarchyBinding;
import com.wind.funds.wallet.dal.entities.CreditAccount;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.entities.table.AccountHierarchyBindingNameRefs;
import com.wind.funds.wallet.dal.entities.table.CreditAccountNameRefs;
import com.wind.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.wind.funds.wallet.dal.mapper.AccountHierarchyBindingMapper;
import com.wind.funds.wallet.dal.mapper.CreditAccountMapper;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyBindingRequest;
import com.wind.funds.wallet.service.AccountHierarchyService;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 账户层级服务实现。
 */
@Service
@AllArgsConstructor
public class AccountHierarchyServiceImpl implements AccountHierarchyService, AccountHierarchySnapshotResolver {

    private static final String DEFAULT_OPERATOR_ID = "SYSTEM";

    private final AccountHierarchyBindingMapper accountHierarchyBindingMapper;

    private final FundingAccountMapper fundingAccountMapper;

    private final CreditAccountMapper creditAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createAccountHierarchyBinding(@NonNull CreateAccountHierarchyBindingRequest request) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(request.getContextVariables());
        ResolvedAccount account = resolveRequiredAccount(request.getTenantId(), request.getAccountId());
        ResolvedAccount parentAccount = resolveRequiredAccount(request.getTenantId(), request.getParentAccountId());
        ResolvedAccount rootAccount = resolveRequiredAccount(request.getTenantId(), request.getRootAccountId());
        assertCompatibleBinding(request, account, parentAccount, rootAccount);
        AccountHierarchyBinding entity = toEntity(request, account, parentAccount, rootAccount);
        assertNoDuplicateActiveBinding(entity);
        accountHierarchyBindingMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建账户层级绑定失败");
        return entity.getId();
    }

    @Override
    public @NonNull Optional<AccountHierarchySnapshotSpec> findAccountHierarchySnapshot(
            @NonNull SubjectRef accountRef,
            @NonNull LocalDateTime effectiveAt) {
        return resolve(accountRef, effectiveAt);
    }

    @Override
    public @NonNull Optional<AccountHierarchySnapshotSpec> resolve(@NonNull SubjectRef accountRef,
                                                                   @NonNull LocalDateTime effectiveAt) {
        if (!isAccountSubject(accountRef.getSubjectType()) || accountRef.getTenantId() == null) {
            return Optional.empty();
        }
        AccountHierarchyBindingNameRefs ref = AccountHierarchyBindingNameRefs.accountHierarchyBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(accountRef.getTenantId()))
                .and(ref.accountId.eq(accountRef.getSubjectId()))
                .and(ref.accountType.eq(accountRef.getSubjectType()))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE))
                .orderBy(ref.id.desc());
        AccountHierarchyBinding binding = accountHierarchyBindingMapper.selectOneByQuery(wrapper);
        if (binding == null) {
            return Optional.empty();
        }
        return Optional.of(toSnapshot(accountRef, binding));
    }

    private AccountHierarchyBinding toEntity(CreateAccountHierarchyBindingRequest request,
                                             ResolvedAccount account,
                                             ResolvedAccount parentAccount,
                                             ResolvedAccount rootAccount) {
        AccountHierarchyBinding result = new AccountHierarchyBinding();
        result.setSn(request.getSn());
        result.setTenantId(request.getTenantId());
        result.setAccountId(account.accountId());
        result.setAccountType(account.accountType());
        result.setParentAccountId(parentAccount.accountId());
        result.setParentAccountType(parentAccount.accountType());
        result.setRootAccountId(rootAccount.accountId());
        result.setRootAccountType(rootAccount.accountType());
        result.setCurrency(request.getCurrency());
        result.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
        result.setVersion(1);
        result.setOperatorId(createOperatorId(request));
        result.setContextVariables(request.getContextVariables());
        return result;
    }

    private AccountHierarchySnapshotSpec toSnapshot(SubjectRef accountRef, AccountHierarchyBinding binding) {
        return ImmutableAccountHierarchySnapshotSpec.builder()
                .accountRef(accountRef)
                .parentAccountRef(subjectRef(binding.getTenantId(),
                        binding.getParentAccountId(),
                        binding.getParentAccountType(),
                        binding.getCurrency()))
                .rootAccountRef(subjectRef(binding.getTenantId(),
                        binding.getRootAccountId(),
                        binding.getRootAccountType(),
                        binding.getCurrency()))
                .contextVariables(parseContextVariables(binding.getContextVariables()))
                .build();
    }

    private SubjectRef subjectRef(Long tenantId,
                                  String accountId,
                                  FundsSubjectType accountType,
                                  CurrencyIsoCode currency) {
        return ImmutableSubjectRef.builder()
                .tenantId(tenantId)
                .subjectId(accountId)
                .subjectType(accountType)
                .currency(currency.name())
                .build();
    }

    private ResolvedAccount resolveRequiredAccount(Long tenantId, FundsAccountId accountId) {
        FundsSubjectType subjectType = parseAccountSubjectType(accountId.type());
        AssertUtils.notNull(subjectType, "账户层级只支持资金账户或信用账户，accountId = {}", accountId);
        ResolvedAccount result = switch (subjectType) {
            case FUNDING_ACCOUNT -> resolveFundingAccount(tenantId, accountId.id());
            case CREDIT_ACCOUNT -> resolveCreditAccount(tenantId, accountId.id());
            case BUDGET_GROUP -> null;
        };
        AssertUtils.notNull(result, "账户层级绑定账户不存在，accountId = {}", accountId);
        return result;
    }

    @Nullable
    private ResolvedAccount resolveFundingAccount(Long tenantId, String accountId) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(accountId));
        FundingAccount account = fundingAccountMapper.selectOneByQuery(wrapper);
        return account == null ? null : new ResolvedAccount(account.getSn(),
                FundsSubjectType.FUNDING_ACCOUNT,
                account.getCurrency(),
                account.getStatus());
    }

    @Nullable
    private ResolvedAccount resolveCreditAccount(Long tenantId, String accountId) {
        CreditAccountNameRefs ref = CreditAccountNameRefs.creditAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(accountId));
        CreditAccount account = creditAccountMapper.selectOneByQuery(wrapper);
        return account == null ? null : new ResolvedAccount(account.getSn(),
                FundsSubjectType.CREDIT_ACCOUNT,
                account.getCurrency(),
                account.getStatus());
    }

    private void assertCompatibleBinding(CreateAccountHierarchyBindingRequest request,
                                         ResolvedAccount account,
                                         ResolvedAccount parentAccount,
                                         ResolvedAccount rootAccount) {
        AssertUtils.isTrue(isActive(account) && isActive(parentAccount) && isActive(rootAccount),
                "账户层级绑定只支持 ACTIVE 账户");
        AssertUtils.isTrue(account.currency() == request.getCurrency()
                        && parentAccount.currency() == request.getCurrency()
                        && rootAccount.currency() == request.getCurrency(),
                "账户层级绑定账户币种必须一致");
        AssertUtils.isFalse(sameAccount(account, parentAccount), "父账户不能等于子账户");
        AssertUtils.isFalse(sameAccount(account, rootAccount), "根账户不能等于子账户");
        new ImmutableAccountHierarchySnapshotSpec(subjectRef(request.getTenantId(),
                account.accountId(),
                account.accountType(),
                account.currency()),
                subjectRef(request.getTenantId(),
                        parentAccount.accountId(),
                        parentAccount.accountType(),
                        parentAccount.currency()),
                subjectRef(request.getTenantId(),
                        rootAccount.accountId(),
                        rootAccount.accountType(),
                        rootAccount.currency()),
                parseContextVariables(request.getContextVariables()));
    }

    private void assertNoDuplicateActiveBinding(AccountHierarchyBinding binding) {
        if (binding.getStatus() != FundsAccountStatus.ACTIVE) {
            return;
        }
        AccountHierarchyBindingNameRefs ref = AccountHierarchyBindingNameRefs.accountHierarchyBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(binding.getTenantId()))
                .and(ref.accountId.eq(binding.getAccountId()))
                .and(ref.accountType.eq(binding.getAccountType()))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = !accountHierarchyBindingMapper.selectListByQuery(wrapper).isEmpty();
        AssertUtils.isFalse(duplicated,
                "账户层级绑定 ACTIVE 关系已存在，accountId = {}, accountType = {}",
                binding.getAccountId(),
                binding.getAccountType());
    }

    private Map<String, Object> parseContextVariables(@Nullable String contextVariables) {
        if (!StringUtils.hasText(contextVariables)) {
            return Map.of();
        }
        JSONObject values = JSONObject.parseObject(contextVariables);
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach(result::put);
        return result;
    }

    private FundsSubjectType parseAccountSubjectType(String accountType) {
        if (FundsSubjectType.FUNDING_ACCOUNT.name().equals(accountType)) {
            return FundsSubjectType.FUNDING_ACCOUNT;
        }
        if (FundsSubjectType.CREDIT_ACCOUNT.name().equals(accountType)) {
            return FundsSubjectType.CREDIT_ACCOUNT;
        }
        return null;
    }

    private boolean isAccountSubject(FundsSubjectType subjectType) {
        return subjectType == FundsSubjectType.FUNDING_ACCOUNT || subjectType == FundsSubjectType.CREDIT_ACCOUNT;
    }

    private boolean isActive(ResolvedAccount account) {
        return account.status() == FundsAccountStatus.ACTIVE;
    }

    private boolean sameAccount(ResolvedAccount left, ResolvedAccount right) {
        return left.accountType() == right.accountType() && left.accountId().equals(right.accountId());
    }

    private String createOperatorId(CreateAccountHierarchyBindingRequest request) {
        if (StringUtils.hasText(request.getOperatorId())) {
            return request.getOperatorId();
        }
        return DEFAULT_OPERATOR_ID;
    }

    private record ResolvedAccount(String accountId,
                                   FundsSubjectType accountType,
                                   CurrencyIsoCode currency,
                                   FundsAccountStatus status) {
    }
}

package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.dal.entities.AccountHierarchyRelation;
import com.wind.funds.wallet.dal.entities.table.AccountHierarchyRelationNameRefs;
import com.wind.funds.wallet.dal.mapper.AccountHierarchyRelationMapper;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.mapstruct.AccountHierarchyRelationConverter;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyRelationRequest;
import com.wind.funds.wallet.service.AccountHierarchyRelationService;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.integration.operator.WindOperator;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 账户层级关系服务实现。
 */
@Service
@AllArgsConstructor
public class AccountHierarchyRelationServiceImpl implements AccountHierarchyRelationService {

    private static final WindSequenceType ACCOUNT_HIERARCHY_RELATION_SEQUENCE_TYPE =
            WindSequenceType.immutable("ACCOUNT_HIERARCHY_RELATION", "AHR", 6);

    private final AccountHierarchyRelationMapper accountHierarchyRelationMapper;

    private final FundingAccountService fundingAccountService;

    private final CreditAccountService creditAccountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull AccountHierarchyRelationDTO createAccountHierarchyRelation(
            @NonNull CreateAccountHierarchyRelationRequest request,
            @NonNull WindOperator operator) {
        validateRequest(request);
        Optional<AccountHierarchyRelationDTO> existing = findAccountHierarchyRelation(
                request.getTenantId(), request.getAccountId());
        if (existing.isPresent()) {
            return reuseExistingRelation(existing.orElseThrow(), request.getParentAccountId());
        }

        ResolvedAccount account = resolveRequiredAccount(request.getTenantId(), request.getAccountId());
        ResolvedAccount parentAccount = resolveRequiredAccount(request.getTenantId(), request.getParentAccountId());
        validateRelation(account, parentAccount);
        validateNoCycle(request.getTenantId(), account, parentAccount);

        AccountHierarchyRelation entity = relation(request.getTenantId(), account, parentAccount, operator);
        try {
            accountHierarchyRelationMapper.insertSelective(entity);
        } catch (DuplicateKeyException ex) {
            return resolveConcurrentCreate(request, ex);
        }
        Optional<AccountHierarchyRelationDTO> created = findAccountHierarchyRelation(
                request.getTenantId(), request.getAccountId());
        AssertUtils.isTrue(created.isPresent(), "创建账户层级关系失败");
        return created.orElseThrow();
    }

    @Override
    public @NonNull Optional<AccountHierarchyRelationDTO> findAccountHierarchyRelation(
            @NonNull Long tenantId,
            @NonNull FundsAccountId accountId) {
        FundsSubjectType accountType = requireAccountSubjectType(accountId);
        AccountHierarchyRelationNameRefs ref = AccountHierarchyRelationNameRefs.accountHierarchyRelation;
        AccountHierarchyRelation result = accountHierarchyRelationMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.accountId.eq(accountId.id()))
                .and(ref.accountType.eq(accountType)));
        return Optional.ofNullable(result).map(this::toDTO);
    }

    private void validateRequest(CreateAccountHierarchyRelationRequest request) {
        AssertUtils.notNull(request.getTenantId(), "账户层级关系租户 ID 不能为空");
        AssertUtils.notNull(request.getAccountId(), "账户层级关系子账户不能为空");
        AssertUtils.notNull(request.getParentAccountId(), "账户层级关系父账户不能为空");
    }

    private ResolvedAccount resolveRequiredAccount(Long tenantId, FundsAccountId accountId) {
        FundsSubjectType subjectType = requireAccountSubjectType(accountId);
        return switch (subjectType) {
            case FUNDING_ACCOUNT -> resolveFundingAccount(tenantId, accountId.id());
            case CREDIT_ACCOUNT -> resolveCreditAccount(tenantId, accountId.id());
            default -> throw new IllegalArgumentException("账户层级关系只支持资金账户或信用账户");
        };
    }

    private ResolvedAccount resolveFundingAccount(Long tenantId, String accountId) {
        FundingAccountDTO account = fundingAccountService.getFundingAccount(tenantId, accountId);
        return new ResolvedAccount(account.getSn(), FundsSubjectType.FUNDING_ACCOUNT,
                account.getCurrency(), account.getState());
    }

    private ResolvedAccount resolveCreditAccount(Long tenantId, String accountId) {
        CreditAccountDTO account = creditAccountService.getCreditAccount(tenantId, accountId);
        return new ResolvedAccount(account.getSn(), FundsSubjectType.CREDIT_ACCOUNT,
                account.getCurrency(), account.getState());
    }

    private void validateRelation(ResolvedAccount account, ResolvedAccount parentAccount) {
        AssertUtils.isFalse(account.key().equals(parentAccount.key()), "父账户不能等于子账户");
        AssertUtils.isTrue(account.state() != FundsAccountState.CLOSED
                        && parentAccount.state() != FundsAccountState.CLOSED,
                "CLOSED 账户不能创建账户层级关系");
        AssertUtils.equals(account.currency(), parentAccount.currency(), "账户层级关系账户币种必须一致");
    }

    private void validateNoCycle(Long tenantId, ResolvedAccount account, ResolvedAccount parentAccount) {
        AccountKey current = parentAccount.key();
        Set<AccountKey> visited = new HashSet<>();
        while (true) {
            AssertUtils.isFalse(current.equals(account.key()), "账户层级关系不能形成环路");
            AssertUtils.isTrue(visited.add(current), "账户层级关系已存在环路，拒绝继续创建");
            Optional<AccountHierarchyRelationDTO> relation = findAccountHierarchyRelation(
                    tenantId, current.toFundsAccountId());
            if (relation.isEmpty()) {
                return;
            }
            AccountHierarchyRelationDTO value = relation.orElseThrow();
            current = new AccountKey(value.getParentAccountId(), value.getParentAccountType());
        }
    }

    private AccountHierarchyRelation relation(Long tenantId,
                                               ResolvedAccount account,
                                               ResolvedAccount parentAccount,
                                               WindOperator operator) {
        AccountHierarchyRelation result = new AccountHierarchyRelation();
        result.setSn(TemporalSequenceFactory.hourNext(ACCOUNT_HIERARCHY_RELATION_SEQUENCE_TYPE));
        result.setTenantId(tenantId);
        result.setAccountId(account.accountId());
        result.setAccountType(account.accountType());
        result.setParentAccountId(parentAccount.accountId());
        result.setParentAccountType(parentAccount.accountType());
        result.setCurrency(account.currency());
        result.setOperatorId(operator.getOperatorAsText());
        return result;
    }

    private AccountHierarchyRelationDTO resolveConcurrentCreate(CreateAccountHierarchyRelationRequest request,
                                                                DuplicateKeyException ex) {
        Optional<AccountHierarchyRelationDTO> existing = findAccountHierarchyRelation(
                request.getTenantId(), request.getAccountId());
        if (existing.isEmpty()) {
            throw ex;
        }
        return reuseExistingRelation(existing.orElseThrow(), request.getParentAccountId());
    }

    private AccountHierarchyRelationDTO reuseExistingRelation(AccountHierarchyRelationDTO existing,
                                                               FundsAccountId parentAccountId) {
        FundsSubjectType parentAccountType = requireAccountSubjectType(parentAccountId);
        AssertUtils.isTrue(existing.getParentAccountType() == parentAccountType
                        && existing.getParentAccountId().equals(parentAccountId.id()),
                "子账户已存在其他父账户关系，accountId = {}", existing.getAccountId());
        return existing;
    }

    private AccountHierarchyRelationDTO toDTO(AccountHierarchyRelation entity) {
        return AccountHierarchyRelationConverter.INSTANCE.convertToAccountHierarchyRelationDTO(entity);
    }

    private FundsSubjectType requireAccountSubjectType(FundsAccountId accountId) {
        FundsSubjectType result = parseAccountSubjectType(accountId.type());
        AssertUtils.notNull(result, "账户层级关系只支持资金账户或信用账户，accountId = {}", accountId);
        return result;
    }

    private FundsSubjectType parseAccountSubjectType(String type) {
        if (FundsSubjectType.FUNDING_ACCOUNT.name().equals(type)) {
            return FundsSubjectType.FUNDING_ACCOUNT;
        }
        if (FundsSubjectType.CREDIT_ACCOUNT.name().equals(type)) {
            return FundsSubjectType.CREDIT_ACCOUNT;
        }
        return null;
    }

    private record ResolvedAccount(String accountId,
                                   FundsSubjectType accountType,
                                   CurrencyIsoCode currency,
                                   FundsAccountState state) {

        private AccountKey key() {
            return new AccountKey(accountId, accountType);
        }
    }

    private record AccountKey(String accountId, FundsSubjectType accountType) {

        private FundsAccountId toFundsAccountId() {
            return FundsAccountId.immutable(accountId, accountType);
        }
    }
}

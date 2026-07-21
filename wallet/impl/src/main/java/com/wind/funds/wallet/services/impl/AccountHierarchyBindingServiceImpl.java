package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.dal.entities.AccountHierarchyBinding;
import com.wind.funds.wallet.dal.entities.table.AccountHierarchyBindingNameRefs;
import com.wind.funds.wallet.dal.mapper.AccountHierarchyBindingMapper;
import com.wind.funds.wallet.mapstruct.AccountHierarchyBindingConverter;
import com.wind.funds.wallet.model.dto.AccountHierarchyBindingDTO;
import com.wind.funds.wallet.service.AccountHierarchyBindingService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 账户层级绑定基础服务实现。
 *
 * @author Codex
 * @date 2026-06-24
 */
@Service
@AllArgsConstructor
public class AccountHierarchyBindingServiceImpl implements AccountHierarchyBindingService {

    private final AccountHierarchyBindingMapper accountHierarchyBindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createAccountHierarchyBinding(@NonNull AccountHierarchyBindingDTO binding) {
        AssertUtils.isFalse(existsCurrentAccountHierarchyBinding(binding),
                "账户层级绑定关系已存在，accountType = {}, accountId = {}",
                binding.getAccountType(),
                binding.getAccountId());
        AccountHierarchyBinding entity =
                AccountHierarchyBindingConverter.INSTANCE.convertToAccountHierarchyBinding(binding);
        try {
            accountHierarchyBindingMapper.insertSelective(entity);
        } catch (DuplicateKeyException ex) {
            assertNoDuplicateCurrentBinding(binding);
            throw ex;
        }
        AssertUtils.notNull(entity.getId(), "创建账户层级绑定失败");
        return entity.getId();
    }

    @Override
    public @NonNull Optional<AccountHierarchyBindingDTO> findCurrentAccountHierarchyBinding(
            @NonNull Long tenantId,
            @NonNull String accountId,
            @NonNull FundsSubjectType accountType) {
        AccountHierarchyBindingNameRefs ref = AccountHierarchyBindingNameRefs.accountHierarchyBinding;
        AccountHierarchyBinding result = accountHierarchyBindingMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.accountId.eq(accountId))
                .and(ref.accountType.eq(accountType))
                .orderBy(ref.id.desc()));
        return Optional.ofNullable(result)
                .map(AccountHierarchyBindingConverter.INSTANCE::convertToAccountHierarchyBindingDTO);
    }

    @Override
    public boolean existsCurrentAccountHierarchyBinding(@NonNull AccountHierarchyBindingDTO binding) {
        AccountHierarchyBindingNameRefs ref = AccountHierarchyBindingNameRefs.accountHierarchyBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(binding.getTenantId()))
                .and(ref.accountId.eq(binding.getAccountId()))
                .and(ref.accountType.eq(binding.getAccountType()));
        return !accountHierarchyBindingMapper.selectListByQuery(wrapper).isEmpty();
    }

    private void assertNoDuplicateCurrentBinding(AccountHierarchyBindingDTO binding) {
        AssertUtils.isFalse(existsCurrentAccountHierarchyBinding(binding),
                "账户层级绑定关系已存在，accountType = {}, accountId = {}",
                binding.getAccountType(),
                binding.getAccountId());
    }
}

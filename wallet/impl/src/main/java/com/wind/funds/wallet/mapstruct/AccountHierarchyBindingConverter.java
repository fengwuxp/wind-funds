package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.AccountHierarchyBinding;
import com.wind.funds.wallet.model.dto.AccountHierarchyBindingDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * AccountHierarchyBinding model converter.
 *
 * @author Codex
 * @date 2026-06-24
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountHierarchyBindingConverter {

    AccountHierarchyBindingConverter INSTANCE = Mappers.getMapper(AccountHierarchyBindingConverter.class);

    /**
     * AccountHierarchyBindingDTO convert to AccountHierarchyBinding.
     *
     * @param data 账户层级绑定 DTO
     * @return AccountHierarchyBinding 实例
     */
    AccountHierarchyBinding convertToAccountHierarchyBinding(AccountHierarchyBindingDTO data);

    /**
     * AccountHierarchyBinding convert to AccountHierarchyBindingDTO.
     *
     * @param data AccountHierarchyBinding 实例
     * @return AccountHierarchyBindingDTO 实例
     */
    AccountHierarchyBindingDTO convertToAccountHierarchyBindingDTO(AccountHierarchyBinding data);
}

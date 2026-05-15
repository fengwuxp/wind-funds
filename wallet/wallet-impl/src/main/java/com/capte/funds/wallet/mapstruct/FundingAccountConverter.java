package com.capte.funds.wallet.mapstruct;

import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.dto.FundingAccountDTO;
import com.capte.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * FundingAccount model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FundingAccountConverter {

    FundingAccountConverter INSTANCE = Mappers.getMapper(FundingAccountConverter.class);

    /**
     * CreateFundingAccountRequest convert to FundingAccount.
     *
     * @param request 创建请求
     * @return FundingAccount 实例
     */
    FundingAccount convertToFundingAccount(CreateFundingAccountRequest request);

    /**
     * FundingAccount convert to FundingAccountDTO.
     *
     * @param data FundingAccount 实例
     * @return FundingAccountDTO 实例
     */
    @Mapping(target = "ledgerIds", ignore = true)
    FundingAccountDTO convertToFundingAccountDTO(FundingAccount data);

    /**
     * Fill create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 资金账户实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateFundingAccountRequest request, @MappingTarget FundingAccount entity) {
        entity.setPlatform(Boolean.TRUE.equals(request.getPlatform()));
        entity.setLedgerProfileCode(resolveProfileCode(request));
        entity.setLedgerProfileVersion(1);
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }

    /**
     * Resolve ledger profile code by explicit request or platform account role.
     *
     * @param request 创建请求
     * @return ledger profile code
     */
    default LedgerProfileCode resolveProfileCode(CreateFundingAccountRequest request) {
        if (request.getLedgerProfileCode() != null) {
            return request.getLedgerProfileCode();
        }
        if (request.getAccountRoleCode() != null) {
            return request.getAccountRoleCode().getLedgerProfileCode();
        }
        if (request.getOwnerType() == FundsAccountOwnerType.MERCHANT) {
            return LedgerProfileCode.FUNDING_MERCHANT;
        }
        return LedgerProfileCode.FUNDING_BASIC;
    }
}

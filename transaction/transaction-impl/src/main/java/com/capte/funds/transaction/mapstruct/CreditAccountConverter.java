package com.capte.funds.transaction.mapstruct;

import com.capte.funds.transaction.dal.entities.CreditAccount;
import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.capte.funds.transaction.model.dto.CreditAccountDTO;
import com.capte.funds.transaction.model.request.CreateCreditAccountRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * CreditAccount model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CreditAccountConverter {

    CreditAccountConverter INSTANCE = Mappers.getMapper(CreditAccountConverter.class);

    /**
     * CreateCreditAccountRequest convert to CreditAccount.
     *
     * @param request 创建请求
     * @return CreditAccount 实例
     */
    CreditAccount convertToCreditAccount(CreateCreditAccountRequest request);

    /**
     * CreditAccount convert to CreditAccountDTO.
     *
     * @param data CreditAccount 实例
     * @return CreditAccountDTO 实例
     */
    @Mapping(target = "ledgerIds", ignore = true)
    CreditAccountDTO convertToCreditAccountDTO(CreditAccount data);

    /**
     * Fill create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 信用账户实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateCreditAccountRequest request, @MappingTarget CreditAccount entity) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        entity.setPeriodType(periodType);
        entity.setLedgerProfileCode(request.getLedgerProfileCode() == null
                ? LedgerProfileCode.CREDIT_BASIC : request.getLedgerProfileCode());
        entity.setLedgerProfileVersion(1);
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }
}

package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.CreditAccount;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * CreditAccount 模型转换器。
 *
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CreditAccountConverter {

    CreditAccountConverter INSTANCE = Mappers.getMapper(CreditAccountConverter.class);

    /**
     * 将 CreateCreditAccountRequest 转换为 CreditAccount。
     *
     * @param request 创建请求
     * @return CreditAccount 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "ledgerProfileVersion", ignore = true)
    @Mapping(target = "version", ignore = true)
    CreditAccount convertToCreditAccount(CreateCreditAccountRequest request);

    /**
     * 将 CreditAccount 转换为 CreditAccountDTO。
     *
     * @param data CreditAccount 实例
     * @return CreditAccountDTO 实例
     */
    CreditAccountDTO convertToCreditAccountDTO(CreditAccount data);

    /**
     * 在同名字段映射后补齐创建默认值。
     *
     * @param request 创建请求
     * @param entity 信用账户实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateCreditAccountRequest request, @MappingTarget CreditAccount entity) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        entity.setPeriodType(periodType);
        entity.setPeriodId(periodType == AccountBalancePeriodType.LIFETIME
                ? AccountBalancePeriodType.LIFETIME.name() : request.getPeriodId());
        entity.setLedgerProfileCode(request.getLedgerProfileCode() == null
                ? LedgerProfileCode.CREDIT_BASIC : request.getLedgerProfileCode());
        entity.setLedgerProfileVersion(1);
        entity.setState(request.getState() == null ? FundsAccountState.ACTIVE : request.getState());
    }
}

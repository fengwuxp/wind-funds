package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * FundingAccount 模型转换器。
 *
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FundingAccountConverter {

    FundingAccountConverter INSTANCE = Mappers.getMapper(FundingAccountConverter.class);

    /**
     * 将 CreateFundingAccountRequest 转换为 FundingAccount。
     *
     * @param request 创建请求
     * @return FundingAccount 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "ledgerProfileVersion", ignore = true)
    @Mapping(target = "version", ignore = true)
    FundingAccount convertToFundingAccount(CreateFundingAccountRequest request);

    /**
     * 将 FundingAccount 转换为 FundingAccountDTO。
     *
     * @param data FundingAccount 实例
     * @return FundingAccountDTO 实例
     */
    FundingAccountDTO convertToFundingAccountDTO(FundingAccount data);

    /**
     * 在同名字段映射后补齐创建默认值。
     *
     * @param request 创建请求
     * @param entity 资金账户实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateFundingAccountRequest request, @MappingTarget FundingAccount entity) {
        entity.setPlatform(Boolean.TRUE.equals(request.getPlatform()));
        entity.setLedgerProfileCode(resolveProfileCode(request));
        entity.setLedgerProfileVersion(1);
        entity.setState(request.getState() == null ? FundsAccountState.ACTIVE : request.getState());
    }

    /**
     * 根据显式请求或平台账户角色解析账本 Profile 编码。
     *
     * @param request 创建请求
     * @return 账本 Profile 编码
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

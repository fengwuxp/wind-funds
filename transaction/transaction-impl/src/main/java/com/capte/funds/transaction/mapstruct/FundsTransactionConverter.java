package com.capte.funds.transaction.mapstruct;

import com.alibaba.fastjson2.JSON;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * FundsTransaction model converter.
 */
@Mapper(imports = JSON.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FundsTransactionConverter {

    FundsTransactionConverter INSTANCE = Mappers.getMapper(FundsTransactionConverter.class);

    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "amount", expression = "java(instruction.getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(instruction.getAmount().getCurrency())")
    @Mapping(target = "routeSnapshot", ignore = true)
    @Mapping(target = "contextVariables", expression = "java(JSON.toJSONString(instruction.getContextVariables()))")
    FundsTransaction convertToFundsTransaction(FundsInstructionSpec instruction);

    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "amount", expression = "java(instruction.getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(instruction.getAmount().getCurrency())")
    @Mapping(target = "contextVariables", expression = "java(JSON.toJSONString(instruction.getContextVariables()))")
    FundsTransactionDetail convertToFundsTransactionDetail(FundsInstructionSpec instruction);

    FundsTransactionDTO convertToFundsTransactionDTO(FundsTransaction data);

    FundsTransactionDetailDTO convertToFundsTransactionDetailDTO(FundsTransactionDetail data);

    @AfterMapping
    default void fillInstructionDefaults(FundsInstructionSpec instruction,
                                         @MappingTarget FundsTransaction entity) {
        entity.setStatus(FundsTransactionStatus.PROCESSING);
        initSummaryAmounts(entity);
    }

    private void initSummaryAmounts(FundsTransaction entity) {
        entity.setAuthorizedAmount(0L);
        entity.setReversedAmount(0L);
        entity.setSettledAmount(0L);
        entity.setRefundedAmount(0L);
        entity.setDeclinedAmount(0L);
        entity.setFeeAmount(0L);
    }
}

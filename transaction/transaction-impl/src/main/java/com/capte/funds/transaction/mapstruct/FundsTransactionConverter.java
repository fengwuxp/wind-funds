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
 * 资金交易模型转换器。
 *
 * <p>职责：在资金指令、交易实体、交易明细实体和对外 DTO 之间做字段转换。</p>
 *
 * <p>边界：只做模型转换和确定性默认值填充，不承载交易状态机、路由解析、账本入账或余额计算。</p>
 */
@Mapper(imports = JSON.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FundsTransactionConverter {

    FundsTransactionConverter INSTANCE = Mappers.getMapper(FundsTransactionConverter.class);

    /**
     * 把资金指令转换为资金交易聚合实体。
     *
     * <p>转换时不生成交易流水号，不写入 route snapshot；交易状态和汇总金额由映射后默认值填充。</p>
     *
     * @param instruction 资金指令
     * @return 资金交易实体
     */
    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "amount", expression = "java(instruction.getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(instruction.getAmount().getCurrency())")
    @Mapping(target = "routeSnapshot", ignore = true)
    @Mapping(target = "contextVariables", expression = "java(JSON.toJSONString(instruction.getContextVariables()))")
    FundsTransaction convertToFundsTransaction(FundsInstructionSpec instruction);

    /**
     * 把资金指令转换为资金交易明细实体。
     *
     * <p>转换时不生成明细流水号；金额、币种和上下文变量来自资金指令。</p>
     *
     * @param instruction 资金指令
     * @return 资金交易明细实体
     */
    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "amount", expression = "java(instruction.getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(instruction.getAmount().getCurrency())")
    @Mapping(target = "contextVariables", expression = "java(JSON.toJSONString(instruction.getContextVariables()))")
    FundsTransactionDetail convertToFundsTransactionDetail(FundsInstructionSpec instruction);

    /**
     * 把资金交易实体转换为对外 DTO。
     *
     * @param data 资金交易实体
     * @return 资金交易 DTO
     */
    FundsTransactionDTO convertToFundsTransactionDTO(FundsTransaction data);

    /**
     * 把资金交易明细实体转换为对外 DTO。
     *
     * @param data 资金交易明细实体
     * @return 资金交易明细 DTO
     */
    FundsTransactionDetailDTO convertToFundsTransactionDetailDTO(FundsTransactionDetail data);

    /**
     * 填充资金指令转换后的交易默认值。
     *
     * @param instruction 资金指令
     * @param entity 资金交易实体
     */
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

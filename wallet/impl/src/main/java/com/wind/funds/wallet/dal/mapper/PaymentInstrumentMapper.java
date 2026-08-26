package com.wind.funds.wallet.dal.mapper;

import com.wind.funds.wallet.dal.entities.PaymentInstrument;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * PaymentInstrument 持久化 Mapper。
 *
 */
@Mapper
public interface PaymentInstrumentMapper extends BaseMapper<PaymentInstrument> {
}

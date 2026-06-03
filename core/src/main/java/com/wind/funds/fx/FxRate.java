package com.wind.funds.fx;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 汇率
 *
 * @author wuxp
 * @date 2026-04-16 16:24
 **/
@Builder
@Getter
public class FxRate {

    private final String rateId;

    private final BigDecimal mid;

    private final BigDecimal bid;

    private final BigDecimal ask;
}


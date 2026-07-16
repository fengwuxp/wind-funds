package com.wind.funds.fx;

import com.wind.common.exception.AssertUtils;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 汇率提供方返回的完整价格快照。
 *
 * <p>mid、bid、ask 均表示一单位源币可兑换的目标币数量。该对象不表达 provider、报价有效期、
 * 业务加点、费用或换汇执行结果。</p>
 *
 * @param snapshotId     Wind 归一化后的来源价格快照标识
 * @param sourceCurrency 源币种
 * @param targetCurrency 目标币种
 * @param mid            市场参考中间价
 * @param bid            汇率提供方买入源币的价格
 * @param ask            汇率提供方卖出源币的价格
 * @param observedAt     来源价格观测时间，不表示报价有效期
 * @author wuxp
 * @date 2026-07-15
 */
@Builder
public record FxRateSnapshot(@NonNull String snapshotId,
                             @NonNull CurrencyIsoCode sourceCurrency,
                             @NonNull CurrencyIsoCode targetCurrency,
                             @NonNull BigDecimal mid,
                             @NonNull BigDecimal bid,
                             @NonNull BigDecimal ask,
                             @NonNull Instant observedAt) {

    public FxRateSnapshot {
        AssertUtils.hasText(snapshotId, "汇率快照标识不能为空");
        AssertUtils.notNull(sourceCurrency, "汇率快照源币种不能为空");
        AssertUtils.notNull(targetCurrency, "汇率快照目标币种不能为空");
        AssertUtils.isTrue(sourceCurrency != CurrencyIsoCode.UNKNOWN, "汇率快照源币种不能为 UNKNOWN");
        AssertUtils.isTrue(targetCurrency != CurrencyIsoCode.UNKNOWN, "汇率快照目标币种不能为 UNKNOWN");
        AssertUtils.isTrue(sourceCurrency != targetCurrency, "汇率快照源币种与目标币种不能相同");
        AssertUtils.notNull(mid, "汇率快照中间价不能为空");
        AssertUtils.notNull(bid, "汇率快照买入价不能为空");
        AssertUtils.notNull(ask, "汇率快照卖出价不能为空");
        AssertUtils.isTrue(mid.compareTo(BigDecimal.ZERO) > 0, "汇率快照价格必须大于 0");
        AssertUtils.isTrue(bid.compareTo(BigDecimal.ZERO) > 0, "汇率快照价格必须大于 0");
        AssertUtils.isTrue(ask.compareTo(BigDecimal.ZERO) > 0, "汇率快照价格必须大于 0");
        AssertUtils.isTrue(bid.compareTo(mid) <= 0 && mid.compareTo(ask) <= 0,
                "汇率快照价格必须满足 bid <= mid <= ask");
        AssertUtils.notNull(observedAt, "汇率快照观测时间不能为空");
    }
}

package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
import org.jspecify.annotations.NonNull;

/**
 * 交易结果消费控制额度变动流水应用服务。
 *
 * <p>职责：在资金交易事实已经存在后，把交易成功、失败释放或退款补偿结果翻译为
 * Spend Rule 控制活动事实，并回链原控制活动和原资金交易流水。</p>
 *
 * <p>边界：本服务不创建或修改资金交易、route snapshot、账本交易、账目分录、余额投影、
 * 支付工具能力方向或交易 canonical 入参；退款和释放语义只作为控制活动补偿，不替代资金交易事实。</p>
 *
 * @author Codex
 * @date 2026-06-20
 */
public interface SpendControlTransactionConsumptionApplicationService {

    /**
     * 记录交易成功后的控制消耗活动。
     *
     * @param request 交易结果消费控制额度变动流水请求
     * @return 支出控制消耗活动
     */
    @NonNull SpendControlActivityDTO consume(@NonNull SpendControlTransactionConsumptionRequest request);

    /**
     * 记录交易失败、过期或被拒绝后的控制释放活动。
     *
     * @param request 交易结果释放控制额度变动流水请求
     * @return 支出控制释放活动
     */
    @NonNull SpendControlActivityDTO release(@NonNull SpendControlTransactionConsumptionRequest request);

    /**
     * 记录退款成功后的控制补偿活动。
     *
     * @param request 交易退款补偿控制额度变动流水请求
     * @return 支出控制退款补偿活动
     */
    @NonNull SpendControlActivityDTO refund(@NonNull SpendControlTransactionConsumptionRequest request);
}

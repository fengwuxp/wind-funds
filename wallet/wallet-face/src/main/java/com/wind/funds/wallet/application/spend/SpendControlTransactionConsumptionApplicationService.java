package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
import org.jspecify.annotations.NonNull;

/**
 * 交易结果消费控制额度变动流水应用服务。
 *
 * <p>职责：在资金交易事实已经存在后，把交易成功、失败释放或退款补偿结果翻译为
 * Spend Rule 控制额度变动事实，并回链原控制额度变动和原资金交易流水。</p>
 *
 * <p>资金退款和支付工具周期额度回补属于两个抽象层次：资金退款只处理资金事实；
 * 周期额度回补必须由业务侧确认支付工具、周期和金额后显式调用控制补偿入口。</p>
 *
 * <p>边界：本服务不创建或修改资金交易、route snapshot、账本交易、账目分录、余额投影、
 * 支付工具能力方向或交易 canonical 入参；退款和释放语义只作为控制额度变动补偿，不替代资金交易事实。</p>
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
    @NonNull SpendControlMovementDTO consume(@NonNull SpendControlTransactionConsumptionRequest request);

    /**
     * 记录交易失败、过期或被拒绝后的控制释放活动。
     *
     * @param request 交易结果释放控制额度变动流水请求
     * @return 支出控制释放活动
     */
    @NonNull SpendControlMovementDTO release(@NonNull SpendControlTransactionConsumptionRequest request);

    /**
     * 记录退款成功后的控制补偿活动。
     *
     * <p>本入口用于已能关联原控制额度变动的补偿链路；没有原控制事实时，不由资金退款入口自动猜测周期或金额，
     * 业务侧应先确认支付工具存在且状态有效，并显式给出周期、金额等控制补偿依据。</p>
     *
     * @param request 交易退款补偿控制额度变动流水请求
     * @return 支出控制退款补偿活动
     */
    @NonNull SpendControlMovementDTO refund(@NonNull SpendControlTransactionConsumptionRequest request);
}

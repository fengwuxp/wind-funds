package com.wind.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.model.request.ConsumeExternalFundsEventRequest;
import org.jspecify.annotations.NonNull;

/**
 * 外部资金事件消费应用服务。
 *
 * <p>职责：承接 ACH、银行文件、渠道回调或第三方钱包回调等外部资金事件，
 * 把已确认外部资金事实消费为标准资金交易。</p>
 *
 * <p>边界：本服务不把银行文件批次、支付工具或外部账户建模为账务主体；真正的资金事实仍必须委派到账户主体型交易内核和
 * ledger posting 链路生成。支付工具、绑定和账户能力准入仍属于 wallet application；本服务只负责外部事件到交易事实的消费入口。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface ExternalFundsEventApplicationService {

    /**
     * 消费外部资金事件。
     *
     * <p>当前仅支持已确认外部入金事件委派标准充值内核，目标账户必须是资金账户；扣款、退票、撤销、差异处理或事件消费者接入时，
     * 必须先解释事件方向、幂等、原交易引用、对账差异和目标账户主体，
     * 再委派直接交易、退款/撤销、调账或对账差异链路。</p>
     *
     * @param request  外部资金事件消费请求
     * @param operator 操作者
     * @return 内部资金事实引用
     */
    @NonNull String consume(@NonNull ConsumeExternalFundsEventRequest request, @NonNull WindOperator operator);
}

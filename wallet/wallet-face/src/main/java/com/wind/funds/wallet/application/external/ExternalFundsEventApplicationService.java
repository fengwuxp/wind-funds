package com.wind.funds.wallet.application.external;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.wallet.model.request.ConsumeExternalFundsEventRequest;
import org.jspecify.annotations.NonNull;

/**
 * 外部资金事件消费应用服务。
 *
 * <p>职责：承接 ACH、银行文件、渠道回调或第三方钱包回调等外部资金事件，把外部事件归一为资金域可消费的服务层入口。</p>
 *
 * <p>边界：本服务不把银行文件批次、支付工具或外部账户建模为账务主体；真正的资金事实仍必须委派到账户主体型交易内核和
 * ledger posting 链路生成。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface ExternalFundsEventApplicationService {

    /**
     * 消费外部资金事件。
     *
     * <p>当前只提供服务层契约和未接内核前置护栏。后续接入时必须先解释事件方向、幂等、原交易引用、对账差异和目标账户主体，
     * 再委派直接交易、退款/撤销、调账或对账差异链路。</p>
     *
     * @param request  外部资金事件消费请求
     * @param operator 操作者
     * @return 内部资金事实引用
     */
    @NonNull String consume(@NonNull ConsumeExternalFundsEventRequest request, @NonNull WindOperator operator);
}

package com.wind.funds.wallet.application.instrument;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具授权准入应用服务。
 *
 * <p>职责：面向 VCC、卡、外部钱包端点或通道 token 等支付工具入口，完成授权前的工具能力、
 * 绑定快照、资金责任和账户能力准入，再委派账户主体型授权交易内核。</p>
 *
 * <p>边界：本服务不直接写交易事实、路由事实、账本分录或余额投影；批准后只调用标准授权交易服务。
 * 交易内核仍以已解析的资金账户或信用账户主体为 canonical 入参，支付工具只作为外层业务入口。</p>
 *
 * @author Codex
 * @date 2026-06-18
 */
public interface AuthorizationAdmissionApplicationService {

    /**
     * 通过支付工具发起授权准入并委派授权交易内核。
     *
     * <p>准入顺序：支付工具动作能力与绑定快照、资金责任关系、账户主体能力与状态。
     * 任一准入失败时必须在进入交易内核前中止，不生成 route、posting、LedgerEntry 或交易事实。</p>
     *
     * @param request  支付工具授权请求
     * @param operator 操作者
     * @return 授权交易流水号
     */
    @NonNull String authorizeByPaymentInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                 @NonNull WindOperator operator);
}

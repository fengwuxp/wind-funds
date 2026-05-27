package com.capte.funds.dsl;

import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.core.WritableContextVariables;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易对外请求上下文契约测试。
 */
class FundsTransactionRequestContextVariablesContractTests {

    /**
     * 场景：调用方构造资金交易请求后，继续改写原始 WritableContextVariables 或其嵌套 Map。
     * 预期：请求对象持有设置时快照，不被外部可变对象污染。
     * 红线：交易请求不得因浅拷贝让 PAN、密钥或外部账户原文进入资金指令事实。
     */
    @Test
    void testTransactionRequestsShouldDefensivelyCopyNestedContextVariables() {
        assertDefensiveCopy(new FundsTransactionTopupRequest(),
                FundsTransactionTopupRequest::setContextVariables,
                FundsTransactionTopupRequest::getContextVariables);
        assertDefensiveCopy(new FundsTransactionPayRequest(),
                FundsTransactionPayRequest::setContextVariables,
                FundsTransactionPayRequest::getContextVariables);
        assertDefensiveCopy(new FundsTransactionTransferRequest(),
                FundsTransactionTransferRequest::setContextVariables,
                FundsTransactionTransferRequest::getContextVariables);
        assertDefensiveCopy(new FundsTransactionRefundRequest(),
                FundsTransactionRefundRequest::setContextVariables,
                FundsTransactionRefundRequest::getContextVariables);
        assertDefensiveCopy(new FundsTransactionWithdrawRequest(),
                FundsTransactionWithdrawRequest::setContextVariables,
                FundsTransactionWithdrawRequest::getContextVariables);
        assertDefensiveCopy(new FundsTransactionFeeRequest(),
                FundsTransactionFeeRequest::setContextVariables,
                FundsTransactionFeeRequest::getContextVariables);
        assertDefensiveCopy(new FundsTransactionFeeRefundRequest(),
                FundsTransactionFeeRefundRequest::setContextVariables,
                FundsTransactionFeeRefundRequest::getContextVariables);
        assertDefensiveCopy(new FundsAuthorizationTransactionAuthorizeRequest(),
                FundsAuthorizationTransactionAuthorizeRequest::setContextVariables,
                FundsAuthorizationTransactionAuthorizeRequest::getContextVariables);
        assertDefensiveCopy(new FundsAuthorizationTransactionSettleRequest(),
                FundsAuthorizationTransactionSettleRequest::setContextVariables,
                FundsAuthorizationTransactionSettleRequest::getContextVariables);
        assertDefensiveCopy(new FundsAuthorizationTransactionReversalRequest(),
                FundsAuthorizationTransactionReversalRequest::setContextVariables,
                FundsAuthorizationTransactionReversalRequest::getContextVariables);
        assertDefensiveCopy(new FundsAuthorizationTransactionRefundRequest(),
                FundsAuthorizationTransactionRefundRequest::setContextVariables,
                FundsAuthorizationTransactionRefundRequest::getContextVariables);
        assertDefensiveCopy(new FundsAuthorizationTransactionChargebackRequest(),
                FundsAuthorizationTransactionChargebackRequest::setContextVariables,
                FundsAuthorizationTransactionChargebackRequest::getContextVariables);
        assertDefensiveCopy(new FundsBalanceFreezeRequest(),
                FundsBalanceFreezeRequest::setContextVariables,
                FundsBalanceFreezeRequest::getContextVariables);
        assertDefensiveCopy(new FundsBalanceUnfreezeRequest(),
                FundsBalanceUnfreezeRequest::setContextVariables,
                FundsBalanceUnfreezeRequest::getContextVariables);
        assertDefensiveCopy(new FundsBalanceAdjustRequest(),
                FundsBalanceAdjustRequest::setContextVariables,
                FundsBalanceAdjustRequest::getContextVariables);
    }

    /**
     * 场景：调用方不传交易请求上下文。
     * 预期：请求对象保留 null，以维持服务层“无额外上下文”的语义。
     */
    @Test
    void testTransactionRequestsShouldKeepNullContextVariables() {
        assertNullContextPreserved(new FundsTransactionTopupRequest(),
                FundsTransactionTopupRequest::setContextVariables,
                FundsTransactionTopupRequest::getContextVariables);
        assertNullContextPreserved(new FundsTransactionPayRequest(),
                FundsTransactionPayRequest::setContextVariables,
                FundsTransactionPayRequest::getContextVariables);
        assertNullContextPreserved(new FundsTransactionTransferRequest(),
                FundsTransactionTransferRequest::setContextVariables,
                FundsTransactionTransferRequest::getContextVariables);
        assertNullContextPreserved(new FundsTransactionRefundRequest(),
                FundsTransactionRefundRequest::setContextVariables,
                FundsTransactionRefundRequest::getContextVariables);
        assertNullContextPreserved(new FundsTransactionWithdrawRequest(),
                FundsTransactionWithdrawRequest::setContextVariables,
                FundsTransactionWithdrawRequest::getContextVariables);
        assertNullContextPreserved(new FundsTransactionFeeRequest(),
                FundsTransactionFeeRequest::setContextVariables,
                FundsTransactionFeeRequest::getContextVariables);
        assertNullContextPreserved(new FundsTransactionFeeRefundRequest(),
                FundsTransactionFeeRefundRequest::setContextVariables,
                FundsTransactionFeeRefundRequest::getContextVariables);
        assertNullContextPreserved(new FundsAuthorizationTransactionAuthorizeRequest(),
                FundsAuthorizationTransactionAuthorizeRequest::setContextVariables,
                FundsAuthorizationTransactionAuthorizeRequest::getContextVariables);
        assertNullContextPreserved(new FundsAuthorizationTransactionSettleRequest(),
                FundsAuthorizationTransactionSettleRequest::setContextVariables,
                FundsAuthorizationTransactionSettleRequest::getContextVariables);
        assertNullContextPreserved(new FundsAuthorizationTransactionReversalRequest(),
                FundsAuthorizationTransactionReversalRequest::setContextVariables,
                FundsAuthorizationTransactionReversalRequest::getContextVariables);
        assertNullContextPreserved(new FundsAuthorizationTransactionRefundRequest(),
                FundsAuthorizationTransactionRefundRequest::setContextVariables,
                FundsAuthorizationTransactionRefundRequest::getContextVariables);
        assertNullContextPreserved(new FundsAuthorizationTransactionChargebackRequest(),
                FundsAuthorizationTransactionChargebackRequest::setContextVariables,
                FundsAuthorizationTransactionChargebackRequest::getContextVariables);
        assertNullContextPreserved(new FundsBalanceFreezeRequest(),
                FundsBalanceFreezeRequest::setContextVariables,
                FundsBalanceFreezeRequest::getContextVariables);
        assertNullContextPreserved(new FundsBalanceUnfreezeRequest(),
                FundsBalanceUnfreezeRequest::setContextVariables,
                FundsBalanceUnfreezeRequest::getContextVariables);
        assertNullContextPreserved(new FundsBalanceAdjustRequest(),
                FundsBalanceAdjustRequest::setContextVariables,
                FundsBalanceAdjustRequest::getContextVariables);
    }

    private static <T> void assertDefensiveCopy(T request,
                                                BiFunction<T, WritableContextVariables, T> setter,
                                                Function<T, WritableContextVariables> getter) {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:transaction-request-context-001");
        WritableContextVariables source = WritableContextVariables.of(
                Map.of("processorPayload", processorPayload));

        setter.apply(request, source);

        source.putVariable("pan", "PAN_AFTER_TRANSACTION_REQUEST_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_TRANSACTION_REQUEST_PAYLOAD_SHOULD_NOT_LEAK");

        WritableContextVariables stored = getter.apply(request);
        assertThat(stored).isNotSameAs(source);
        assertThat(stored.getContextVariables()).doesNotContainKey("pan");
        Object payloadValue = stored.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:transaction-request-context-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    private static <T> void assertNullContextPreserved(T request,
                                                       BiFunction<T, WritableContextVariables, T> setter,
                                                       Function<T, WritableContextVariables> getter) {
        setter.apply(request, null);

        assertThat(getter.apply(request)).isNull();
    }
}

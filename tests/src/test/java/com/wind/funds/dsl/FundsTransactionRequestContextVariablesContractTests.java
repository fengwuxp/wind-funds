package com.wind.funds.dsl;

import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.core.ReadonlyContextVariables;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易对外请求上下文契约测试。
 */
class FundsTransactionRequestContextVariablesContractTests {

    /**
     * 场景：调用方传入交易请求上下文。
     * 预期：请求对象以 ReadonlyContextVariables 承载当前阶段的上下文变量。
     */
    @Test
    void testTransactionRequestsShouldStoreReadonlyContextVariables() {
        assertReadonlyContextStored(new FundsTransactionTopupRequest(),
                FundsTransactionTopupRequest::setContextVariables,
                FundsTransactionTopupRequest::getContextVariables);
        assertReadonlyContextStored(new FundsTransactionPayRequest(),
                FundsTransactionPayRequest::setContextVariables,
                FundsTransactionPayRequest::getContextVariables);
        assertReadonlyContextStored(new FundsTransactionTransferRequest(),
                FundsTransactionTransferRequest::setContextVariables,
                FundsTransactionTransferRequest::getContextVariables);
        assertReadonlyContextStored(new FundsTransactionRefundRequest(),
                FundsTransactionRefundRequest::setContextVariables,
                FundsTransactionRefundRequest::getContextVariables);
        assertReadonlyContextStored(new FundsTransactionWithdrawRequest(),
                FundsTransactionWithdrawRequest::setContextVariables,
                FundsTransactionWithdrawRequest::getContextVariables);
        assertReadonlyContextStored(new FundsTransactionFeeRequest(),
                FundsTransactionFeeRequest::setContextVariables,
                FundsTransactionFeeRequest::getContextVariables);
        assertReadonlyContextStored(new FundsTransactionFeeRefundRequest(),
                FundsTransactionFeeRefundRequest::setContextVariables,
                FundsTransactionFeeRefundRequest::getContextVariables);
        assertReadonlyContextStored(new FundsAuthorizationTransactionAuthorizeRequest(),
                FundsAuthorizationTransactionAuthorizeRequest::setContextVariables,
                FundsAuthorizationTransactionAuthorizeRequest::getContextVariables);
        assertReadonlyContextStored(new FundsAuthorizationTransactionSettleRequest(),
                FundsAuthorizationTransactionSettleRequest::setContextVariables,
                FundsAuthorizationTransactionSettleRequest::getContextVariables);
        assertReadonlyContextStored(new FundsAuthorizationTransactionReversalRequest(),
                FundsAuthorizationTransactionReversalRequest::setContextVariables,
                FundsAuthorizationTransactionReversalRequest::getContextVariables);
        assertReadonlyContextStored(new FundsAuthorizationTransactionRefundRequest(),
                FundsAuthorizationTransactionRefundRequest::setContextVariables,
                FundsAuthorizationTransactionRefundRequest::getContextVariables);
        assertReadonlyContextStored(new FundsAuthorizationTransactionChargebackRequest(),
                FundsAuthorizationTransactionChargebackRequest::setContextVariables,
                FundsAuthorizationTransactionChargebackRequest::getContextVariables);
        assertReadonlyContextStored(new FundsBalanceFreezeRequest(),
                FundsBalanceFreezeRequest::setContextVariables,
                FundsBalanceFreezeRequest::getContextVariables);
        assertReadonlyContextStored(new FundsBalanceUnfreezeRequest(),
                FundsBalanceUnfreezeRequest::setContextVariables,
                FundsBalanceUnfreezeRequest::getContextVariables);
        assertReadonlyContextStored(new FundsBalanceAdjustRequest(),
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

    private static <T> void assertReadonlyContextStored(T request,
                                                        BiFunction<T, ReadonlyContextVariables, T> setter,
                                                        Function<T, ReadonlyContextVariables> getter) {
        ReadonlyContextVariables source = ReadonlyContextVariables.of(Map.of(
                "networkReference", "token:transaction-request-context-001"));

        setter.apply(request, source);

        ReadonlyContextVariables stored = getter.apply(request);
        assertThat(stored).isNotNull();
        assertThat(stored.getContextVariables())
                .containsEntry("networkReference", "token:transaction-request-context-001");
    }

    private static <T> void assertNullContextPreserved(T request,
                                                       BiFunction<T, ReadonlyContextVariables, T> setter,
                                                       Function<T, ReadonlyContextVariables> getter) {
        setter.apply(request, null);

        assertThat(getter.apply(request)).isNull();
    }
}

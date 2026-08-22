package com.wind.funds.dsl;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
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
import com.wind.funds.transaction.model.request.MerchantInfoRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.support.FundsContextVariables;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易对外请求上下文契约测试。
 */
class FundsTransactionRequestContextVariablesContractTests {

    private static final List<Class<?>> TRANSACTION_REQUEST_PACKAGE_TYPES = List.of(
            CreateFundsFrozenOrderRequest.class,
            FundsAuthorizationTransactionAuthorizeRequest.class,
            FundsAuthorizationTransactionRefundRequest.class,
            FundsAuthorizationTransactionReversalRequest.class,
            FundsAuthorizationTransactionCompleteRequest.class,
            FundsBalanceAdjustRequest.class,
            FundsBalanceFreezeRequest.class,
            FundsBalanceUnfreezeRequest.class,
            FundsTransactionFeeRefundRequest.class,
            FundsTransactionFeeRequest.class,
            FundsTransactionPayRequest.class,
            FundsTransactionRefundRequest.class,
            FundsTransactionTopupRequest.class,
            FundsTransactionTransferRequest.class,
            FundsTransactionWithdrawRequest.class,
            MerchantInfoRequest.class,
            TransactionAmount.class);

    private static final List<RequestContextCase<?>> REQUEST_CONTEXT_CASES = List.of(
            new RequestContextCase<>(FundsTransactionTopupRequest.class,
                    FundsTransactionTopupRequest::new,
                    FundsTransactionTopupRequest::setContextVariables,
                    FundsTransactionTopupRequest::getContextVariables),
            new RequestContextCase<>(FundsTransactionPayRequest.class,
                    FundsTransactionPayRequest::new,
                    FundsTransactionPayRequest::setContextVariables,
                    FundsTransactionPayRequest::getContextVariables),
            new RequestContextCase<>(FundsTransactionTransferRequest.class,
                    FundsTransactionTransferRequest::new,
                    FundsTransactionTransferRequest::setContextVariables,
                    FundsTransactionTransferRequest::getContextVariables),
            new RequestContextCase<>(FundsTransactionRefundRequest.class,
                    FundsTransactionRefundRequest::new,
                    FundsTransactionRefundRequest::setContextVariables,
                    FundsTransactionRefundRequest::getContextVariables),
            new RequestContextCase<>(FundsTransactionWithdrawRequest.class,
                    FundsTransactionWithdrawRequest::new,
                    FundsTransactionWithdrawRequest::setContextVariables,
                    FundsTransactionWithdrawRequest::getContextVariables),
            new RequestContextCase<>(FundsTransactionFeeRequest.class,
                    FundsTransactionFeeRequest::new,
                    FundsTransactionFeeRequest::setContextVariables,
                    FundsTransactionFeeRequest::getContextVariables),
            new RequestContextCase<>(FundsTransactionFeeRefundRequest.class,
                    FundsTransactionFeeRefundRequest::new,
                    FundsTransactionFeeRefundRequest::setContextVariables,
                    FundsTransactionFeeRefundRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionAuthorizeRequest.class,
                    FundsAuthorizationTransactionAuthorizeRequest::new,
                    FundsAuthorizationTransactionAuthorizeRequest::setContextVariables,
                    FundsAuthorizationTransactionAuthorizeRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionCompleteRequest.class,
                    FundsAuthorizationTransactionCompleteRequest::new,
                    FundsAuthorizationTransactionCompleteRequest::setContextVariables,
                    FundsAuthorizationTransactionCompleteRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionReversalRequest.class,
                    FundsAuthorizationTransactionReversalRequest::new,
                    FundsAuthorizationTransactionReversalRequest::setContextVariables,
                    FundsAuthorizationTransactionReversalRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionRefundRequest.class,
                    FundsAuthorizationTransactionRefundRequest::new,
                    FundsAuthorizationTransactionRefundRequest::setContextVariables,
                    FundsAuthorizationTransactionRefundRequest::getContextVariables),
            new RequestContextCase<>(FundsBalanceFreezeRequest.class,
                    FundsBalanceFreezeRequest::new,
                    FundsBalanceFreezeRequest::setContextVariables,
                    FundsBalanceFreezeRequest::getContextVariables),
            new RequestContextCase<>(FundsBalanceUnfreezeRequest.class,
                    FundsBalanceUnfreezeRequest::new,
                    FundsBalanceUnfreezeRequest::setContextVariables,
                    FundsBalanceUnfreezeRequest::getContextVariables),
            new RequestContextCase<>(FundsBalanceAdjustRequest.class,
                    FundsBalanceAdjustRequest::new,
                    FundsBalanceAdjustRequest::setContextVariables,
                    FundsBalanceAdjustRequest::getContextVariables));

    /**
     * 场景：调用方传入交易请求上下文。
     * 预期：请求对象以 ReadonlyContextVariables 承载当前阶段的上下文变量。
     */
    @Test
    void testTransactionRequestsShouldStoreReadonlyContextVariables() {
        REQUEST_CONTEXT_CASES.forEach(FundsTransactionRequestContextVariablesContractTests::assertReadonlyContextStored);
    }

    /**
     * 场景：手续费退回需要沿原费用路径回放金额事实。
     * 预期：请求统一使用 TransactionAmount，不保留只能表达同币种金额的 amount 字段。
     */
    @Test
    void testFeeRefundShouldUseTransactionAmount() throws NoSuchFieldException {
        Field transactionAmount = FundsTransactionFeeRefundRequest.class.getDeclaredField("transactionAmount");

        assertThat(transactionAmount.getType()).isEqualTo(TransactionAmount.class);
        assertThat(Arrays.stream(FundsTransactionFeeRefundRequest.class.getDeclaredFields())
                .map(Field::getName))
                .doesNotContain("amount");
    }

    /**
     * 场景：调用方不传交易请求上下文。
     * 预期：请求对象保留 null，以维持服务层“无额外上下文”的语义。
     */
    @Test
    void testTransactionRequestsShouldKeepNullContextVariables() {
        REQUEST_CONTEXT_CASES.forEach(FundsTransactionRequestContextVariablesContractTests::assertNullContextPreserved);
    }

    /**
     * 场景：交易请求新增 ReadonlyContextVariables 上下文字段。
     * 预期：新增请求必须进入本测试矩阵，显式验证存储和 null 语义。
     */
    @Test
    void testAllTransactionRequestsWithContextVariablesShouldBeCovered() {
        assertThat(TRANSACTION_REQUEST_PACKAGE_TYPES)
                .as("transaction request package inventory must be explicit and unique")
                .doesNotHaveDuplicates();
        assertThat(coveredRequestTypes())
                .as("requests with ReadonlyContextVariables must be included in context contract matrix")
                .containsExactlyInAnyOrderElementsOf(transactionRequestTypesWithReadonlyContextVariables());
    }

    /**
     * 场景：授权退款请求契约回归校验。
     * 预期：refundMode 不再作为调用方入参暴露，只保留为内部资金指令上下文标签。
     */
    @Test
    void testAuthorizationRefundRequestShouldNotExposeRefundMode() {
        assertThat(Arrays.stream(FundsAuthorizationTransactionRefundRequest.class.getDeclaredFields())
                .map(Field::getName)
                .toList())
                .doesNotContain("refundMode");
        assertThat(Arrays.stream(FundsAuthorizationTransactionRefundRequest.class.getMethods())
                .map(Method::getName)
                .toList())
                .doesNotContain("getRefundMode", "setRefundMode", "isRefundMode");
    }

    /**
     * 场景：余额调账公共契约收缩未被真实 Consumer 证明的负余额输入。
     * 预期：Request 属性、transaction-face keys 和 core raw flag 均不存在。
     * 红线：不得保留 getter/setter、常量别名或 raw context 旁路。
     */
    @Test
    void testBalanceAdjustShouldNotExposeNegativeBalanceSurface() {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(Arrays.stream(FundsBalanceAdjustRequest.class.getDeclaredFields())
                        .map(Field::getName)
                        .toList())
                .doesNotContain(
                        "allowNegativeBalance",
                        "negativeAvailablePolicyCode",
                        "negativeAvailableRiskStatus",
                        "negativeAvailableSingleLimit",
                        "negativeAvailableCumulativeLimit",
                        "negativeAvailableAgingStartedAt");
        softly.assertThat(Arrays.stream(FundsBalanceAdjustRequest.class.getMethods())
                        .map(Method::getName)
                        .toList())
                .doesNotContain(
                        "getAllowNegativeBalance", "setAllowNegativeBalance",
                        "getNegativeAvailablePolicyCode", "setNegativeAvailablePolicyCode",
                        "getNegativeAvailableRiskStatus", "setNegativeAvailableRiskStatus",
                        "getNegativeAvailableSingleLimit", "setNegativeAvailableSingleLimit",
                        "getNegativeAvailableCumulativeLimit", "setNegativeAvailableCumulativeLimit",
                        "getNegativeAvailableAgingStartedAt", "setNegativeAvailableAgingStartedAt");
        softly.assertThat(Arrays.stream(FundsInstructionContextKeys.class.getDeclaredFields())
                        .map(Field::getName)
                        .toList())
                .doesNotContain(
                        "ALLOW_NEGATIVE_BALANCE",
                        "NEGATIVE_AVAILABLE_POLICY_CODE",
                        "NEGATIVE_AVAILABLE_RISK_STATUS",
                        "NEGATIVE_AVAILABLE_SINGLE_LIMIT",
                        "NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT",
                        "NEGATIVE_AVAILABLE_AGING_STARTED_AT");
        softly.assertThat(Arrays.stream(FundsContextVariables.class.getDeclaredFields())
                        .map(Field::getName)
                        .toList())
                .doesNotContain("ALLOW_NEGATIVE_BALANCE");
        softly.assertAll();
    }

    private static <T> void assertReadonlyContextStored(RequestContextCase<T> requestCase) {
        ReadonlyContextVariables source = ReadonlyContextVariables.of(Map.of(
                "networkReference", "token:transaction-request-context-001"));
        T request = requestCase.requestFactory().get();

        requestCase.setter().apply(request, source);

        ReadonlyContextVariables stored = requestCase.getter().apply(request);
        assertThat(stored).isNotNull();
        assertThat(stored.getContextVariables())
                .containsEntry("networkReference", "token:transaction-request-context-001");
    }

    private static <T> void assertNullContextPreserved(RequestContextCase<T> requestCase) {
        T request = requestCase.requestFactory().get();

        requestCase.setter().apply(request, null);

        assertThat(requestCase.getter().apply(request)).isNull();
    }

    private static Set<Class<?>> coveredRequestTypes() {
        Set<Class<?>> requestTypes = new LinkedHashSet<>();
        REQUEST_CONTEXT_CASES.stream()
                .map(RequestContextCase::requestType)
                .forEach(requestTypes::add);
        return requestTypes;
    }

    private static Set<Class<?>> transactionRequestTypesWithReadonlyContextVariables() {
        Set<Class<?>> requestTypes = new LinkedHashSet<>();
        for (Class<?> requestType : TRANSACTION_REQUEST_PACKAGE_TYPES) {
            if (hasReadonlyContextVariablesField(requestType)) {
                requestTypes.add(requestType);
            }
        }
        return requestTypes;
    }

    private static boolean hasReadonlyContextVariablesField(Class<?> requestType) {
        return Arrays.stream(requestType.getDeclaredFields())
                .anyMatch(field -> "contextVariables".equals(field.getName())
                        && ReadonlyContextVariables.class.equals(field.getType()));
    }

    private record RequestContextCase<T>(
            Class<T> requestType,
            Supplier<T> requestFactory,
            BiFunction<T, ReadonlyContextVariables, T> setter,
            Function<T, ReadonlyContextVariables> getter) {
    }
}

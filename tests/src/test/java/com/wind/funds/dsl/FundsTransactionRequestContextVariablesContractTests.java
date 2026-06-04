package com.wind.funds.dsl;

import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionExpireRequest;
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易对外请求上下文契约测试。
 */
class FundsTransactionRequestContextVariablesContractTests {

    private static final String TRANSACTION_REQUEST_PACKAGE = "com.wind.funds.transaction.model.request";

    private static final String TRANSACTION_REQUEST_PACKAGE_PATH =
            TRANSACTION_REQUEST_PACKAGE.replace('.', '/');

    private static final String CLASS_FILE_SUFFIX = ".class";

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
            new RequestContextCase<>(FundsAuthorizationTransactionSettleRequest.class,
                    FundsAuthorizationTransactionSettleRequest::new,
                    FundsAuthorizationTransactionSettleRequest::setContextVariables,
                    FundsAuthorizationTransactionSettleRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionReversalRequest.class,
                    FundsAuthorizationTransactionReversalRequest::new,
                    FundsAuthorizationTransactionReversalRequest::setContextVariables,
                    FundsAuthorizationTransactionReversalRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionExpireRequest.class,
                    FundsAuthorizationTransactionExpireRequest::new,
                    FundsAuthorizationTransactionExpireRequest::setContextVariables,
                    FundsAuthorizationTransactionExpireRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionRefundRequest.class,
                    FundsAuthorizationTransactionRefundRequest::new,
                    FundsAuthorizationTransactionRefundRequest::setContextVariables,
                    FundsAuthorizationTransactionRefundRequest::getContextVariables),
            new RequestContextCase<>(FundsAuthorizationTransactionChargebackRequest.class,
                    FundsAuthorizationTransactionChargebackRequest::new,
                    FundsAuthorizationTransactionChargebackRequest::setContextVariables,
                    FundsAuthorizationTransactionChargebackRequest::getContextVariables),
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
    void testAllTransactionRequestsWithContextVariablesShouldBeCovered()
            throws IOException, URISyntaxException, ClassNotFoundException {
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

    private static Set<String> coveredRequestTypes() {
        Set<String> requestTypes = new TreeSet<>();
        REQUEST_CONTEXT_CASES.stream()
                .map(requestCase -> requestCase.requestType().getSimpleName())
                .forEach(requestTypes::add);
        return requestTypes;
    }

    private static Set<String> transactionRequestTypesWithReadonlyContextVariables()
            throws IOException, URISyntaxException, ClassNotFoundException {
        Set<String> requestTypes = new TreeSet<>();
        ClassLoader classLoader = FundsTransactionRequestContextVariablesContractTests.class.getClassLoader();
        Enumeration<URL> packageResources = classLoader.getResources(TRANSACTION_REQUEST_PACKAGE_PATH);
        while (packageResources.hasMoreElements()) {
            URL packageResource = packageResources.nextElement();
            if (!"file".equals(packageResource.getProtocol())) {
                continue;
            }
            try (Stream<Path> compiledClasses = Files.list(Path.of(packageResource.toURI()))) {
                for (Path compiledClass : compiledClasses
                        .filter(FundsTransactionRequestContextVariablesContractTests::isTopLevelClassFile)
                        .toList()) {
                    Class<?> requestType = Class.forName(
                            TRANSACTION_REQUEST_PACKAGE + "." + compiledClassSimpleName(compiledClass),
                            false,
                            classLoader);
                    if (hasReadonlyContextVariablesField(requestType)) {
                        requestTypes.add(requestType.getSimpleName());
                    }
                }
            }
        }
        return requestTypes;
    }

    private static boolean isTopLevelClassFile(Path compiledClass) {
        String fileName = compiledClass.getFileName().toString();
        return fileName.endsWith(CLASS_FILE_SUFFIX) && !fileName.contains("$");
    }

    private static String compiledClassSimpleName(Path compiledClass) {
        String fileName = compiledClass.getFileName().toString();
        return fileName.substring(0, fileName.length() - CLASS_FILE_SUFFIX.length());
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

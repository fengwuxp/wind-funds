package com.wind.funds.route;

import com.wind.funds.ledger.LedgerBalanceProjectionService;
import com.wind.funds.ledger.LedgerTransactionPostingService;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.ledger.service.LedgerTransactionService;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.services.FundsFrozenOrderService;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由解析事实写入边界测试。
 */
class RouteResolverFactBoundaryTests {

    private static final String ROUTE_SOURCE_ROOT = "transaction/transaction-impl/src/main/java";
    private static final String ROUTE_PACKAGE_PATH = "com/wind/funds/route";
    private static final String ROUTE_PACKAGE_NAME = "com.wind.funds.route";

    private static final List<Class<?>> ROUTE_LAYER_TYPES = List.of(
            RouteResolver.class,
            CompositeRouteResolver.class,
            TransferFundsInstructionRouteResolver.class,
            AuthorizationFundsInstructionRouteResolver.class,
            BalanceControlFundsInstructionRouteResolver.class,
            DefaultRouteReplayService.class,
            DefaultRouteSnapshotFactory.class,
            RouteBenefitSnapshotContextSupport.class,
            RouteReplaySupport.class,
            PlatformAccountRouteSupport.class,
            RouteParticipantFactory.class,
            RouteSpecSupport.class,
            RouteSubjectSupport.class);

    private static final List<Class<?>> FACT_WRITE_TYPES = List.of(
            LedgerTransactionPostingService.class,
            LedgerBalanceProjectionService.class,
            LedgerService.class,
            LedgerTransactionService.class,
            LedgerServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class,
            FundsDirectTransactionService.class,
            FundsAuthorizationTransactionService.class,
            FundsTransactionCommandServiceImpl.class,
            FundsFrozenOrderService.class,
            DefaultFundsInstructionLifecycleSaver.class,
            DefaultFundsFrozenOrderLifecycleSaver.class,
            DefaultRoutedFundsInstructionOrchestrator.class);

    private static final List<String> FACT_WRITE_PACKAGE_PREFIXES = List.of(
            "com.wind.funds.ledger.dal.",
            "com.wind.funds.ledger.impl.",
            "com.wind.funds.ledger.service.",
            "com.wind.funds.transaction.application.",
            "com.wind.funds.transaction.dal.",
            "com.wind.funds.transaction.ledger.",
            "com.wind.funds.transaction.services.impl.");

    /**
     * 场景：路由解析器参与资金交易主链路选路和 route snapshot 回放。
     * 预期：RouteResolver 只解析路径、参与方、账目和 route leg，不声明交易、账本或投影写入依赖。
     * 红线：路由层不得通过注入写事实服务形成隐式副作用。
     */
    @Test
    void testRouteLayerShouldNotDeclareFactWriteDependencies() {
        List<String> violations = new ArrayList<>();
        for (Class<?> routeLayerType : ROUTE_LAYER_TYPES) {
            collectDeclaredTypeDependencies(routeLayerType, violations);
        }

        assertThat(violations)
                .as("RouteResolver layer must not declare transaction, ledger, or projection fact write dependencies")
                .isEmpty();
    }

    /**
     * 场景：新增 route resolver 或 route helper 后，边界测试必须自动暴露未纳入扫描的问题。
     * 预期：transaction-impl route 源码下的每个 Java 类型都被事实写入依赖扫描覆盖。
     * 红线：新增路由类型不能绕开 route 不写交易、账本或投影事实的架构守护。
     */
    @Test
    void testRouteBoundaryScanShouldCoverAllRouteImplementationTypes() throws IOException {
        List<String> scannedRouteTypes = ROUTE_LAYER_TYPES.stream()
                .map(Class::getName)
                .filter(typeName -> typeName.startsWith(ROUTE_PACKAGE_NAME))
                .sorted()
                .toList();

        assertThat(scannedRouteTypes)
                .as("route boundary scan must include every route implementation type")
                .containsAll(routeImplementationTypeNames());
    }

    private void collectDeclaredTypeDependencies(Class<?> routeLayerType, List<String> violations) {
        for (Field field : routeLayerType.getDeclaredFields()) {
            collectTypeDependency(routeLayerType, "field " + field.getName(), field.getGenericType(), violations);
        }
        for (Constructor<?> constructor : routeLayerType.getDeclaredConstructors()) {
            for (Type parameterType : constructor.getGenericParameterTypes()) {
                collectTypeDependency(routeLayerType, "constructor parameter", parameterType, violations);
            }
        }
        for (Method method : routeLayerType.getDeclaredMethods()) {
            collectTypeDependency(routeLayerType, "method " + method.getName() + " return",
                    method.getGenericReturnType(), violations);
            for (Type parameterType : method.getGenericParameterTypes()) {
                collectTypeDependency(routeLayerType, "method " + method.getName() + " parameter",
                        parameterType, violations);
            }
        }
    }

    private void collectTypeDependency(Class<?> owner, String dependencySite, Type dependencyType,
                                       List<String> violations) {
        if (dependencyType instanceof Class<?> dependencyClass) {
            collectClassDependency(owner, dependencySite, dependencyClass, violations);
            return;
        }
        if (dependencyType instanceof ParameterizedType parameterizedType) {
            collectTypeDependency(owner, dependencySite, parameterizedType.getRawType(), violations);
            for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                collectTypeDependency(owner, dependencySite, actualTypeArgument, violations);
            }
            return;
        }
        if (dependencyType instanceof GenericArrayType genericArrayType) {
            collectTypeDependency(owner, dependencySite, genericArrayType.getGenericComponentType(), violations);
            return;
        }
        if (dependencyType instanceof WildcardType wildcardType) {
            for (Type upperBound : wildcardType.getUpperBounds()) {
                collectTypeDependency(owner, dependencySite, upperBound, violations);
            }
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                collectTypeDependency(owner, dependencySite, lowerBound, violations);
            }
            return;
        }
        if (dependencyType instanceof TypeVariable<?> typeVariable) {
            for (Type bound : typeVariable.getBounds()) {
                collectTypeDependency(owner, dependencySite, bound, violations);
            }
        }
    }

    private void collectClassDependency(Class<?> owner, String dependencySite, Class<?> dependencyClass,
                                        List<String> violations) {
        Class<?> normalizedClass = dependencyClass.isArray() ? dependencyClass.componentType() : dependencyClass;
        if (isFactWriteDependency(normalizedClass)) {
            violations.add(owner.getSimpleName() + " " + dependencySite + " depends on "
                    + normalizedClass.getName());
        }
    }

    private boolean isFactWriteDependency(Class<?> dependencyClass) {
        if (FACT_WRITE_TYPES.contains(dependencyClass)) {
            return true;
        }
        String className = dependencyClass.getName();
        return FACT_WRITE_PACKAGE_PREFIXES.stream().anyMatch(className::startsWith);
    }

    private List<String> routeImplementationTypeNames() throws IOException {
        Path sourceRoot = workspaceRoot().resolve(ROUTE_SOURCE_ROOT);
        Path routePackageRoot = sourceRoot.resolve(ROUTE_PACKAGE_PATH);
        try (Stream<Path> files = Files.walk(routePackageRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(sourceRoot::relativize)
                    .map(RouteResolverFactBoundaryTests::toClassName)
                    .sorted()
                    .toList();
        }
    }

    private static String toClassName(Path sourcePath) {
        String className = sourcePath.toString().replace(File.separatorChar, '.');
        return className.substring(0, className.length() - ".java".length());
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (multiModuleDir != null && !multiModuleDir.isBlank()) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        if ("tests".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
    }
}

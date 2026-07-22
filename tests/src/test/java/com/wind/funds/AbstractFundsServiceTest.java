package com.wind.funds;

import com.wind.integration.core.context.TenantContextHolder;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.query.QueryColumnBehavior;
import com.mybatisflex.spring.FlexTransactionManager;
import com.mybatisflex.spring.boot.ConfigurationCustomizer;
import com.mybatisflex.spring.boot.v4.MybatisFlexAutoConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import com.wind.common.exception.AssertUtils;
import com.wind.common.locks.JdkLockFactory;
import com.wind.common.locks.LockFactory;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.common.spring.SpringApplicationContextUtils;
import com.wind.integration.infrastructure.locks.LockTemplate;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.mybatis.convert.LocaleTypeHandler;
import com.wind.mybatis.encrypt.AbstractEncryptBaseTypeHandler;
import com.wind.security.core.WindSecurityAccessOperations;
import com.wind.server.i18n.WindMessageSourceProperties;
import com.wind.tools.h2.H2FunctionInitializer;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.ApplicationDataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.sql.DataSource;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * 资金域服务层流程测试公共基座。
 *
 * <p>公共基座只承载测试运行基础设施：H2 schema、MyBatis Flex、事务、JdbcTemplate、
 * 缓存、锁、国际化、SQL 审计、租户上下文和 Spring 静态上下文。业务 Bean、外部端口替身、
 * 测试数据和业务断言应由具体测试基座声明。</p>
 */
@TestPropertySource(locations = {
        "classpath:application-h2.properties",
        "classpath:application-test.properties"
})
@EnableConfigurationProperties
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Transactional(rollbackFor = Exception.class)
public abstract class AbstractFundsServiceTest {

    private static final int QUERY_IN_MAX_SIZE = 5120;

    private static final String QUERY_IN_SIZE_ERROR_MESSAGE = "database query in op size range in >=1 && <5120";

    protected static final Long TENANT_ID = 1L;

    protected static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    @BeforeEach
    void setUpFundsServiceTestContext() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDownFundsServiceTestContext() {
        TenantContextHolder.clear();
    }

    @Configuration
    @Import({
            TestCoreInfrastructureConfig.class
    })
    public static class TestInfrastructureConfig {
    }

    @Configuration
    @Import({
            MybatisTestConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceInitializationAutoConfiguration.class,
            H2InitializationAutoConfiguration.class,
            MybatisFlexAutoConfiguration.class,
            WindOperatorFactory.class
    })
    public static class TestCoreInfrastructureConfig {

        private static final Logger SQL_LOG = LoggerFactory.getLogger("mybatis-flex-sql");

        private final ApplicationContext applicationContext;

        public TestCoreInfrastructureConfig(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @PostConstruct
        public void init() {
            TenantContextHolder.setTenantId(TENANT_ID);
            new SpringApplicationContextUtils().setApplicationContext(applicationContext);
            SpringApplicationContextUtils.markStarted();
            setTestApplicationEventPublisher(event -> {
            });
            AuditManager.setAuditEnable(true);
            AuditManager.setMessageCollector(auditMessage -> SQL_LOG.info("{}, {}ms",
                    auditMessage.getFullSql(), auditMessage.getElapsedTime()));
        }

        @PreDestroy
        public void stop() {
            TenantContextHolder.clear();
        }

        @Bean
        public LockFactory lockFactory() {
            return new JdkLockFactory();
        }

        @Bean
        public LockTemplate lockTemplate(LockFactory lockFactory) {
            return new LockTemplate(lockFactory);
        }

        @Bean
        public CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }

        @Bean
        public WindSecurityAccessOperations windSecurityAccessOperations() {
            return new WindSecurityAccessOperations() {
                @Override
                public boolean hasAnyAuthority(String... authorities) {
                    return false;
                }

                @Override
                public boolean hasAnyRole(String... roles) {
                    return false;
                }
            };
        }

        @Bean
        @Primary
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        public PlatformTransactionManager transactionManager(
                DataSource dataSource,
                @Value("${wind.funds.test.flex-transaction-manager-enabled:false}") boolean flexTransactionManagerEnabled) {
            if (flexTransactionManagerEnabled) {
                return new FlexTransactionManager();
            }
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public ApplicationDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(
                DataSource dataSource,
                SqlInitializationProperties properties) {
            return new ApplicationDataSourceScriptDatabaseInitializer(dataSource, properties);
        }

        @Bean
        public WindMessageSourceProperties windMessageSourceProperties() {
            WindMessageSourceProperties result = new WindMessageSourceProperties();
            result.setLocales(Set.of(Locale.CHINA));
            return result;
        }
    }

    @AutoConfiguration
    @EnableConfigurationProperties({
            DataSourceProperties.class,
            SqlInitializationProperties.class
    })
    public static class H2InitializationAutoConfiguration {

        private static final String H2_MEMORY_URL_PREFIX = "jdbc:h2:mem:";

        private static final String H2_KEEP_ALIVE_OPTION = "DB_CLOSE_DELAY=-1";

        @Bean
        public DataSource dataSource(DataSourceProperties properties,
                                     @Value("${spring.datasource.url}") String url) {
            properties.setType(HikariDataSource.class);
            String dataSourceUrl = keepAliveH2MemoryDatabase(url);
            properties.setUrl(dataSourceUrl);
            DataSource result = properties.initializeDataSourceBuilder().build();
            if (dataSourceUrl.startsWith(H2_MEMORY_URL_PREFIX)) {
                H2FunctionInitializer.initialize(result);
            }
            return result;
        }

        private static String keepAliveH2MemoryDatabase(String url) {
            if (!url.startsWith(H2_MEMORY_URL_PREFIX) || url.contains(H2_KEEP_ALIVE_OPTION)) {
                return url;
            }
            return url + ";" + H2_KEEP_ALIVE_OPTION;
        }
    }

    @MapperScan({
            "com.wind.funds.ledger.dal.mapper",
            "com.wind.funds.transaction.dal.mapper",
            "com.wind.funds.reconciliation.dal.mapper",
            "com.wind.funds.wallet.dal.mapper"
    })
    @EnableTransactionManagement
    @Configuration
    public static class MybatisTestConfiguration {

        static {
            AbstractEncryptBaseTypeHandler.setTextEncryptor(new TextEncryptor() {

                @Override
                public String encrypt(String text) {
                    return text;
                }

                @Override
                public String decrypt(String encryptedText) {
                    return encryptedText;
                }
            });
            QueryColumnBehavior.setIgnoreFunction(AbstractFundsServiceTest::shouldIgnoreQueryValue);
        }

        @Bean
        public ConfigurationCustomizer configurationCustomizer() {
            return configuration -> configuration.getTypeHandlerRegistry()
                    .register(Locale.class, LocaleTypeHandler.class);
        }
    }

    static boolean shouldIgnoreQueryValue(Object value) {
        if (ObjectUtils.isEmpty(value)) {
            return true;
        }
        if (value instanceof Collection<?> elements) {
            AssertUtils.isTrue(elements.size() < QUERY_IN_MAX_SIZE, QUERY_IN_SIZE_ERROR_MESSAGE);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            if (length == 1) {
                return shouldIgnoreQueryValue(Array.get(value, 0));
            }
            AssertUtils.isTrue(length < QUERY_IN_MAX_SIZE, QUERY_IN_SIZE_ERROR_MESSAGE);
            for (int index = 0; index < length; index++) {
                if (Array.get(value, index) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static void setTestApplicationEventPublisher(ApplicationEventPublisher publisher) {
        try {
            Method method = SpringEventPublishUtils.class.getDeclaredMethod("setApplicationEventPublisher",
                    ApplicationEventPublisher.class);
            method.setAccessible(true);
            method.invoke(null, publisher);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize test application event publisher", e);
        }
    }
}

package com.capte.funds;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.mybatisflex.spring.FlexSqlSessionFactoryBean;
import com.wind.common.spring.SpringApplicationContextUtils;
import com.wind.tools.h2.H2FunctionInitializer;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * 资金域服务层流程测试公共基座。
 *
 * <p>公共基座只承载测试运行基础设施：H2 schema、MyBatis Flex、事务、JdbcTemplate、租户上下文
 * 和 Spring 静态上下文。业务 Bean、外部端口替身、测试数据和业务断言应由具体测试基座声明。</p>
 */
@TestPropertySource(locations = {
        "classpath:application-h2.properties",
        "classpath:application-test.properties"
})
@Transactional(rollbackFor = Exception.class)
public abstract class AbstractFundsServiceTest {

    protected static final Long TENANT_ID = 1L;

    protected static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    @BeforeEach
    void setUpFundsServiceTestContext() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDownFundsServiceTestContext() {
        ThreadContextTenantIdHolder.remove();
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan({
            "com.capte.funds.ledger.dal.mapper",
            "com.capte.funds.transaction.dal.mapper",
            "com.capte.funds.wallet.dal.mapper"
    })
    public static class TestInfrastructureConfig implements ApplicationContextAware {

        private static final String H2_MEMORY_URL_PREFIX = "jdbc:h2:mem:";

        private static final String H2_KEEP_ALIVE_OPTION = "DB_CLOSE_DELAY=-1";

        @Override
        public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
            new SpringApplicationContextUtils().setApplicationContext(applicationContext);
            SpringApplicationContextUtils.markStarted();
            ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        }

        @Bean
        DataSource dataSource(@Value("${spring.datasource.url}") String url,
                              @Value("${spring.datasource.username}") String username,
                              @Value("${spring.datasource.password}") String password,
                              @Value("${spring.datasource.driver-class-name}") String driverClassName) {
            DriverManagerDataSource result = new DriverManagerDataSource(keepAliveH2MemoryDatabase(url),
                    username, password);
            result.setDriverClassName(driverClassName);
            H2FunctionInitializer.initialize(result);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("jdbc-schema.sql"));
            populator.setIgnoreFailedDrops(true);
            populator.execute(result);
            return result;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            FlexSqlSessionFactoryBean factoryBean = new FlexSqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            return factoryBean.getObject();
        }

        @Bean
        SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        private static String keepAliveH2MemoryDatabase(String url) {
            if (!url.startsWith(H2_MEMORY_URL_PREFIX) || url.contains(H2_KEEP_ALIVE_OPTION)) {
                return url;
            }
            return url + ";" + H2_KEEP_ALIVE_OPTION;
        }
    }
}

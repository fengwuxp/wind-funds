package com.wind.funds.ledger;

import com.wind.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.wind.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.wind.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.ledger.service.LedgerTransactionService;
import com.wind.funds.wallet.FundsAccountQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * 账本写入链在 JDK 事务代理下的最小 Spring 装配测试。
 *
 * @author wuxp
 * @since 2026-08-10
 */
class LedgerPostingJdkProxyContextTests {

    @Test
    void testLedgerPostingContextShouldStartWithJdkTransactionalProxy() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(Config.class);

            assertThatCode(context::refresh).doesNotThrowAnyException();
            assertThat(context.getBean(LedgerTransactionPostingService.class)).isNotNull();
            assertThat(AopUtils.isJdkDynamicProxy(context.getBean(LedgerTransactionService.class))).isTrue();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = false)
    @Import({
            LedgerTransactionServiceImpl.class,
            DefaultLedgerTransactionPostingServiceImpl.class
    })
    static class Config {

        @Bean
        LedgerTransactionMapper ledgerTransactionMapper() {
            return mock(LedgerTransactionMapper.class);
        }

        @Bean
        LedgerPostingPlanMapper ledgerPostingPlanMapper() {
            return mock(LedgerPostingPlanMapper.class);
        }

        @Bean
        LedgerEntryMapper ledgerEntryMapper() {
            return mock(LedgerEntryMapper.class);
        }

        @Bean
        LedgerService ledgerService() {
            return mock(LedgerService.class);
        }

        @Bean
        FundsAccountQueryService fundsAccountQueryService() {
            return mock(FundsAccountQueryService.class);
        }

        @Bean
        LedgerBalanceProjectionServiceImpl ledgerBalanceProjectionService() {
            return mock(LedgerBalanceProjectionServiceImpl.class);
        }

        @Bean
        DefaultLedgerPostingAssembler ledgerPostingAssembler() {
            return mock(DefaultLedgerPostingAssembler.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }
}

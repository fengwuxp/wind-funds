package com.capte.funds.ledger.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.capte.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.common.spring.SpringEventPublishUtils;
import com.wind.integration.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.integration.funds.ledger.LedgerBalanceProjectionService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 账本余额投影服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        LedgerBalanceProjectionServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerBalanceProjectionServiceImplTests extends AbstractFundsServiceTest {

    private static final String ACCOUNT_ID = "funding_user_balance_log";

    private static final String ACCOUNT_TYPE = FundsSubjectType.FUNDING_ACCOUNT.name();

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    @Autowired
    private LedgerBalanceProjectionService projectionService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long availableLedgerId;

    /**
     * 场景：余额投影已经完成账本余额更新，但余额变更事件发布器临时不可用。
     * 输入：H2 中存在资金账户和 AVAILABLE 账本，期初正常余额 100，一笔借方入账 25；事件发布器抛出运行时异常。
     * 输出：投影服务保留余额更新结果，观察事件失败不向调用方冒泡。
     * 预期：余额变更事件只是业务切入/观察口，不是余额事实源。
     * 红线：余额变更日志或事件失败不得回滚、阻断或篡改 Ledger 余额投影事实。
     */
    @Test
    void testProjectShouldKeepLedgerBalanceUpdateWhenBalanceChangedEventPublishFails() {
        AtomicInteger eventAttempts = new AtomicInteger();
        setApplicationEventPublisher(event -> {
            eventAttempts.incrementAndGet();
            throw new IllegalStateException("balance log sink unavailable");
        });
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        projectionService.project(List.of(ledgerEntry(25L)));

        LedgerDTO ledger = ledgerService.getLedgerById(availableLedgerId);
        assertThat(eventAttempts).hasValue(1);
        assertThat(ledger.getDebitAmount()).isEqualTo(125L);
        assertThat(ledger.getCreditAmount()).isZero();
        assertThat(ledger.getNormalBalance()).isEqualTo(125L);
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：余额投影成功后发布余额变更观察事件。
     * 输入：期初 AVAILABLE 正常余额 100，一笔借方入账 25。
     * 输出：事件包含主体、账本、账目、币种、余额前后值、变更额、账本交易、账本分录和业务引用。
     * 预期：余额日志从 ledger entry 和 balance projection 派生，可观察、可追溯。
     * 红线：余额变更观察事件不得丢失来源分录，也不得成为新的余额事实源。
     */
    @Test
    void testProjectShouldPublishBalanceChangedEventWithSourceEvidence() {
        List<LedgerBalanceChangedEvent> publishedEvents = new ArrayList<>();
        setApplicationEventPublisher(event -> publishedEvents.add((LedgerBalanceChangedEvent) event));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        projectionService.project(List.of(ledgerEntry(25L)));

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
        assertThat(publishedEvents).singleElement().satisfies(event -> {
            assertThat(event.getSubjectId()).isEqualTo(ACCOUNT_ID);
            assertThat(event.getSubjectType()).isEqualTo(ACCOUNT_TYPE);
            assertThat(event.getLedgerId()).isEqualTo(availableLedgerId);
            assertThat(event.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(event.getCurrency()).isEqualTo(CURRENCY);
            assertThat(event.getBeforeBalance()).isEqualTo(100L);
            assertThat(event.getBalance()).isEqualTo(125L);
            assertThat(event.getBalanceDelta()).isEqualTo(25L);
            assertThat(event.getLedgerTransactionSn()).isEqualTo("LT-BALANCE-LOG-001");
            assertThat(event.getLedgerEntrySn()).isEqualTo("LE-BALANCE-LOG-001");
            assertThat(event.getLedgerEntryDigest()).isEqualTo("sha256-balance-log-001");
            assertThat(event.getBusinessScene()).isEqualTo("BALANCE_LOG_BOUNDARY");
            assertThat(event.getBusinessSn()).isEqualTo("BALANCE_LOG_BOUNDARY_001");
            assertThat(event.getTransactionTime()).isEqualTo(LocalDateTime.of(2026, 5, 19, 12, 0));
            assertThat(event.getContextVariables()).containsEntry("ledgerEntrySn", "LE-BALANCE-LOG-001");
        });
    }

    /**
     * 场景：余额变更观察事件发布后，调用方继续改写原始分录的嵌套上下文。
     * 输入：分录上下文携带可追溯的 processor payload，事件发布后原始 payload 被追加敏感字段。
     * 输出：已发布事件中的上下文保持发布时快照，不包含后续追加的敏感字段。
     * 预期：余额变更事件是可追溯观察事实，发布后上下文不可被外部引用回写污染。
     * 红线：余额观察事件不得因浅拷贝让 PAN、密钥或外部账户原文进入后续监听、日志或报表。
     */
    @Test
    void testProjectShouldPublishBalanceChangedEventWithImmutableNestedContext() {
        List<LedgerBalanceChangedEvent> publishedEvents = new ArrayList<>();
        setApplicationEventPublisher(event -> publishedEvents.add((LedgerBalanceChangedEvent) event));
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:balance-event-001");
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        projectionService.project(List.of(ledgerEntry(25L,
                Map.of("processorPayload", processorPayload))));
        processorPayload.put("pan", "PAN_AFTER_BALANCE_EVENT_SHOULD_NOT_LEAK");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
        assertThat(publishedEvents).singleElement().satisfies(event -> {
            Object payloadValue = event.getContextVariables().get("processorPayload");
            assertThat(payloadValue).isInstanceOf(Map.class);
            Map<?, ?> payload = (Map<?, ?>) payloadValue;
            assertThat(payload.get("networkReference")).isEqualTo("token:balance-event-001");
            assertThat(payload.containsKey("pan")).isFalse();
        });
    }

    /**
     * 场景：同一资金账户的一批投影同时命中多个余额桶，其中后一个桶余额不足。
     * 输入：先给 FROZEN 桶加 10，再从 AVAILABLE 桶扣 200；AVAILABLE 期初只有 100。
     * 输出：请求被拒绝；AVAILABLE 和 FROZEN 均保持期初余额。
     * 预期：投影服务必须先完成整批余额约束校验，再写任一余额桶。
     * 红线：不能出现前一个余额桶已更新、后一个余额桶失败的半截投影。
     */
    @Test
    void testProjectShouldRejectWholeBatchWhenLaterLedgerWouldBeNegative() {
        Long frozenLedgerId = createFrozenLedger(0L);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> projectionService.project(List.of(
                ledgerEntry(frozenLedgerId, LedgerSubjectCode.FROZEN, EntrySide.CREDIT, 10L),
                ledgerEntry(availableLedgerId, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 200L))))
                .hasMessageContaining("账本余额不足");

        LedgerDTO availableLedger = ledgerService.getLedgerById(availableLedgerId);
        LedgerDTO frozenLedger = ledgerService.getLedgerById(frozenLedgerId);
        assertThat(availableLedger.getNormalBalance()).isEqualTo(100L);
        assertThat(frozenLedger.getNormalBalance()).isZero();
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpProjectionServiceTestData() {
        cleanupProjectionServiceTestData();
        seedFundingAccount();
        availableLedgerId = createAvailableLedger(100L);
    }

    @AfterEach
    void tearDownProjectionServiceTestData() {
        setApplicationEventPublisher(event -> {
        });
        cleanupProjectionServiceTestData();
    }

    private void cleanupProjectionServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn = ?", ACCOUNT_ID);
    }

    private void seedFundingAccount() {
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(ACCOUNT_ID);
        account.setOwnerId("owner_" + ACCOUNT_ID);
        account.setOwnerType(FundsAccountOwnerType.USER);
        account.setAccountType(ACCOUNT_TYPE);
        account.setPlatform(Boolean.FALSE);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("balance projection service flow test funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private Long createAvailableLedger(long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(ACCOUNT_ID)
                .setSubjectType(ACCOUNT_TYPE)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.ASSET)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        ledgerService.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                .setId(ledgerId)
                .setDebitAmountDelta(initialBalance)
                .setCreditAmountDelta(0L));
        return ledgerId;
    }

    private Long createFrozenLedger(long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(ACCOUNT_ID)
                .setSubjectType(ACCOUNT_TYPE)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.FROZEN)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        if (initialBalance != 0L) {
            ledgerService.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                    .setId(ledgerId)
                    .setDebitAmountDelta(initialBalance < 0L ? -initialBalance : null)
                    .setCreditAmountDelta(initialBalance > 0L ? initialBalance : null));
        }
        return ledgerId;
    }

    private LedgerEntrySpec ledgerEntry(long amount) {
        return ledgerEntry(availableLedgerId, LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT, amount);
    }

    private LedgerEntrySpec ledgerEntry(long amount, Map<String, Object> contextVariables) {
        return ledgerEntry(availableLedgerId, LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT, amount, contextVariables);
    }

    private LedgerEntrySpec ledgerEntry(Long ledgerId,
                                        LedgerSubjectCode ledgerSubjectCode,
                                        EntrySide entrySide,
                                        long amount) {
        return ledgerEntry(ledgerId,
                ledgerSubjectCode,
                entrySide,
                amount,
                Map.of("ledgerEntrySn", "LE-BALANCE-LOG-001"));
    }

    private LedgerEntrySpec ledgerEntry(Long ledgerId,
                                        LedgerSubjectCode ledgerSubjectCode,
                                        EntrySide entrySide,
                                        long amount,
                                        Map<String, Object> contextVariables) {
        return TestLedgerEntrySpec.builder()
                .subjectId(ACCOUNT_ID)
                .subjectType(ACCOUNT_TYPE)
                .ledgerSubjectCode(ledgerSubjectCode)
                .ledgerSubjectCategory(resolveLedgerSubjectCategory(ledgerSubjectCode))
                .ledgerId(ledgerId)
                .ledgerTransactionSn("LT-BALANCE-LOG-001")
                .entryType(entrySide)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .businessScene("BALANCE_LOG_BOUNDARY")
                .businessSn("BALANCE_LOG_BOUNDARY_001")
                .amount(Money.immutable(amount, CURRENCY))
                .originalAmount(Money.immutable(amount, CURRENCY))
                .exchangeRate(BigDecimal.ONE)
                .transactionTime(LocalDateTime.of(2026, 5, 19, 12, 0))
                .description("balance log boundary")
                .contextVariables(contextVariables)
                .sha256("sha256-balance-log-001")
                .balanceConstraintType(LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE)
                .build();
    }

    private LedgerSubjectCategory resolveLedgerSubjectCategory(LedgerSubjectCode ledgerSubjectCode) {
        return ledgerSubjectCode == LedgerSubjectCode.AVAILABLE
                ? LedgerSubjectCategory.ASSET
                : LedgerSubjectCategory.LIABILITY;
    }

    private static void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        try {
            Method method = SpringEventPublishUtils.class.getDeclaredMethod(
                    "setApplicationEventPublisher",
                    ApplicationEventPublisher.class);
            method.setAccessible(true);
            method.invoke(null, publisher);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置 Spring 事件发布器失败", ex);
        }
    }

    @Configuration
    @Import({
            DefaultFundsAccountQueryServiceImpl.class,
            LedgerServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class
    })
    static class Config {
    }

    @Getter
    private static final class TestLedgerEntrySpec implements LedgerEntrySpec {

        private final String subjectId;

        private final String subjectType;

        private final LedgerSubjectCode ledgerSubjectCode;

        private final LedgerSubjectCategory ledgerSubjectCategory;

        private final Long ledgerId;

        private final String ledgerTransactionSn;

        private final EntrySide entryType;

        private final LedgerPhaseCode phaseCode;

        private final LedgerBalanceConstraintType balanceConstraintType;

        private final String businessScene;

        private final String businessSn;

        private final Money amount;

        private final Money originalAmount;

        private final BigDecimal exchangeRate;

        private final LocalDateTime transactionTime;

        private final String description;

        private final Map<String, Object> contextVariables;

        private final String sha256;

        @Builder
        private TestLedgerEntrySpec(String subjectId,
                                    String subjectType,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    LedgerSubjectCategory ledgerSubjectCategory,
                                    Long ledgerId,
                                    String ledgerTransactionSn,
                                    EntrySide entryType,
                                    LedgerPhaseCode phaseCode,
                                    LedgerBalanceConstraintType balanceConstraintType,
                                    String businessScene,
                                    String businessSn,
                                    Money amount,
                                    Money originalAmount,
                                    BigDecimal exchangeRate,
                                    LocalDateTime transactionTime,
                                    String description,
                                    Map<String, Object> contextVariables,
                                    String sha256) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
            this.ledgerSubjectCode = ledgerSubjectCode;
            this.ledgerSubjectCategory = ledgerSubjectCategory;
            this.ledgerId = ledgerId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.entryType = entryType;
            this.phaseCode = phaseCode;
            this.balanceConstraintType = balanceConstraintType;
            this.businessScene = businessScene;
            this.businessSn = businessSn;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.transactionTime = transactionTime;
            this.description = description;
            this.contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
            this.sha256 = sha256;
        }
    }
}

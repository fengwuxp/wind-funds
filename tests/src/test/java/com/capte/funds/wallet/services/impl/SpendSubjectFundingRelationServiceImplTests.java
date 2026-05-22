package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.capte.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.capte.funds.wallet.model.request.CreateFundingAccountRequest;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.capte.funds.wallet.service.FundingAccountService;
import com.capte.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.FundingAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 支出主体资金来源关系服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendSubjectFundingRelationServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendSubjectFundingRelationServiceImplTests extends AbstractFundsServiceTest {

    private static final String RELATION_SN = "spend_funding_rel_service";

    private static final String DUPLICATE_DEFAULT_RELATION_SN = "spend_funding_rel_duplicate_default";

    private static final String PRIORITY_CONFLICT_RELATION_SN = "spend_funding_rel_priority_conflict";

    private static final String PRIORITY_ORDER_RELATION_SN = "spend_funding_rel_priority_order";

    private static final String FUNDING_ACCOUNT_SN = "funding_relation_target";

    private static final String SECOND_FUNDING_ACCOUNT_SN = "funding_relation_second_target";

    private static final String THIRD_FUNDING_ACCOUNT_SN = "funding_relation_third_target";

    private static final String SPEND_SUBJECT_ID = "credit_relation_subject";

    private static final String OWNER_ID = "owner_relation_service";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateSpendSubjectFundingRelationShouldNotPostLedgerOrChangeFundingAccountBalance() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        List<LedgerDTO> fundingLedgersBefore = loadFundingAccountLedgers();

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest());

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setFundingAccountId(FUNDING_ACCOUNT_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setDefaultRelation(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(relationId).isPositive();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst())
                .satisfies(relation -> {
                    assertThat(relation.getSn()).isEqualTo(RELATION_SN);
                    assertThat(relation.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(relation.getSpendSubjectId()).isEqualTo(SPEND_SUBJECT_ID);
                    assertThat(relation.getSpendSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(relation.getFundingAccountId()).isEqualTo(FUNDING_ACCOUNT_SN);
                    assertThat(relation.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(relation.getRelationType())
                            .isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE);
                    assertThat(relation.getPriority()).isEqualTo(20);
                    assertThat(relation.getDefaultRelation()).isTrue();
                    assertThat(relation.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
                });
        assertThat(loadFundingAccountLedgers())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(fundingLedgersBefore);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectMissingFundingAccountWithoutRelation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setFundingAccountId("missing_relation_target")))
                .hasMessageContaining("资金账户不存在");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectUnavailableFundingAccountWithoutRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setStatus(FundsAccountStatus.SUSPENDED));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()))
                .hasMessageContaining("资金账户不可作为资金来源");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectCurrencyMismatchWithoutRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setCurrency(CurrencyIsoCode.CNY)))
                .hasMessageContaining("资金账户币种与资金来源关系币种不一致");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支出主体已经存在一个可用默认资金来源后，再配置第二个默认资金来源。
     * 输入：同租户、同支出主体、同币种、同关系类型，两个 ACTIVE 默认关系。
     * 输出：第二个关系被拒绝，保持原有唯一默认资金来源。
     * 红线：默认资金来源不唯一时不得为后续 route 留下随机选路候选，也不得写账。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectDuplicateActiveDefaultRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(SECOND_FUNDING_ACCOUNT_SN));
        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setSn(DUPLICATE_DEFAULT_RELATION_SN)
                .setFundingAccountId(SECOND_FUNDING_ACCOUNT_SN)
                .setPriority(30)))
                .hasMessageContaining("默认资金来源关系不唯一");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", DUPLICATE_DEFAULT_RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支出主体已经存在一个可用资金来源优先级后，再配置同优先级资金来源。
     * 输入：同租户、同支出主体、同币种、同关系类型，两个 ACTIVE 非默认关系使用相同 priority。
     * 输出：第二个关系被拒绝，保持原有资金来源候选唯一可排序。
     * 红线：资金来源优先级冲突时不得为后续 route 留下随机选路候选，也不得写账。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectDuplicateActivePriorityRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(SECOND_FUNDING_ACCOUNT_SN));
        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setDefaultRelation(Boolean.FALSE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setSn(PRIORITY_CONFLICT_RELATION_SN)
                .setFundingAccountId(SECOND_FUNDING_ACCOUNT_SN)
                .setDefaultRelation(Boolean.FALSE)))
                .hasMessageContaining("资金来源关系优先级冲突");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isOne();
        assertThat(countRows("t_spend_subject_funding_rel", "sn", PRIORITY_CONFLICT_RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支出主体下存在多个 ACTIVE 非默认资金来源，后续 route 需要按确定顺序读取候选。
     * 输入：两个候选优先级分别为 20 和 10，创建顺序与优先级顺序相反。
     * 输出：查询结果按 priority 升序返回，再由稳定主键兜底。
     * 红线：资金来源候选查询不得依赖数据库自然顺序，避免后续 route 出现隐式随机选路。
     */
    @Test
    void testQuerySpendSubjectFundingRelationsShouldReturnActiveCandidatesByPriority() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(SECOND_FUNDING_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setDefaultRelation(Boolean.FALSE));
        fundingRelationService.createSpendSubjectFundingRelation(createPriorityOrderRelationRequest());

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records)
                .extracting(SpendSubjectFundingRelationDTO::getSn)
                .containsExactly(PRIORITY_ORDER_RELATION_SN, RELATION_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支出主体下存在过期、当前有效和未来生效的 ACTIVE 非默认资金来源。
     * 输入：三条关系都为 ACTIVE，但 validFrom/validTo 覆盖过去、当前和未来窗口。
     * 输出：查询当前 ACTIVE 资金来源候选时只返回当前有效记录。
     * 红线：已过期或未生效的资金来源关系不得进入 route 候选，避免后续交易随机或错误扣款。
     */
    @Test
    void testQuerySpendSubjectFundingRelationsShouldExcludeInactiveValidityWindowCandidates() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(SECOND_FUNDING_ACCOUNT_SN));
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(THIRD_FUNDING_ACCOUNT_SN));
        LocalDateTime now = LocalDateTime.now();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setDefaultRelation(Boolean.FALSE)
                .setValidFrom(now.minusDays(2))
                .setValidTo(now.minusDays(1)));
        fundingRelationService.createSpendSubjectFundingRelation(createPriorityOrderRelationRequest()
                .setValidFrom(now.minusMinutes(1))
                .setValidTo(now.plusDays(1)));
        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setSn(PRIORITY_CONFLICT_RELATION_SN)
                .setFundingAccountId(THIRD_FUNDING_ACCOUNT_SN)
                .setPriority(30)
                .setDefaultRelation(Boolean.FALSE)
                .setValidFrom(now.plusDays(1))
                .setValidTo(now.plusDays(2)));

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records)
                .extracting(SpendSubjectFundingRelationDTO::getSn)
                .containsExactly(PRIORITY_ORDER_RELATION_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendSubjectFundingRelationTestData() {
        cleanupSpendSubjectFundingRelationTestData();
    }

    @AfterEach
    void tearDownSpendSubjectFundingRelationTestData() {
        cleanupSpendSubjectFundingRelationTestData();
    }

    private void cleanupSpendSubjectFundingRelationTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn IN (?, ?, ?, ?)",
                RELATION_SN,
                DUPLICATE_DEFAULT_RELATION_SN,
                PRIORITY_CONFLICT_RELATION_SN,
                PRIORITY_ORDER_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                FUNDING_ACCOUNT_SN,
                SECOND_FUNDING_ACCOUNT_SN,
                THIRD_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                FUNDING_ACCOUNT_SN,
                SECOND_FUNDING_ACCOUNT_SN,
                THIRD_FUNDING_ACCOUNT_SN);
    }

    private CreateFundingAccountRequest createFundingAccountRequest() {
        return new CreateFundingAccountRequest()
                .setSn(FUNDING_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
    }

    private CreateSpendSubjectFundingRelationRequest createRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setSn(RELATION_SN)
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(SPEND_SUBJECT_ID)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setFundingAccountId(FUNDING_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setPriority(20)
                .setDefaultRelation(Boolean.TRUE);
    }

    private CreateSpendSubjectFundingRelationRequest createPriorityOrderRelationRequest() {
        return createRelationRequest()
                .setSn(PRIORITY_ORDER_RELATION_SN)
                .setFundingAccountId(SECOND_FUNDING_ACCOUNT_SN)
                .setPriority(10)
                .setDefaultRelation(Boolean.FALSE);
    }

    private List<LedgerDTO> loadFundingAccountLedgers() {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(FUNDING_ACCOUNT_SN)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private long countRows(String tableName, String columnName, Object value) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class, value);
        return result;
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class
    })
    static class Config {
    }
}

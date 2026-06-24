package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
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
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

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

    private static final String LONG_FUNDING_ACCOUNT_SN =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private static final String LONG_RELATION_SN = "spend_funding_rel_long_subject";

    private static final String SECOND_FUNDING_ACCOUNT_SN = "funding_relation_second_target";

    private static final String THIRD_FUNDING_ACCOUNT_SN = "funding_relation_third_target";

    private static final String CREDIT_TARGET_SN = "credit_relation_target_subject";

    private static final String SUSPENDED_CREDIT_TARGET_SN = "credit_rel_suspended_target";

    private static final String CREDIT_TARGET_RELATION_SN = "spend_credit_target_rel";

    private static final String SUSPENDED_CREDIT_TARGET_RELATION_SN = "spend_credit_target_suspended_rel";

    private static final String BUDGET_TARGET_RELATION_SN = "spend_budget_target_rel";

    private static final String SPEND_SUBJECT_ID = "credit_relation_subject";

    private static final String OWNER_ID = "owner_relation_service";

    private static final String UNQUOTED_SENSITIVE_CONTEXT_VARIABLES =
            "{processorPayload:{secretKey:\"secret-value\"";

    private static final String UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES =
            "{externalAccount:{bankAccountNo:\"123456789012\"";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private CreditAccountService creditAccountService;

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
                    assertThat(relation.getTargetSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
                    assertThat(relation.getTargetSubjectId()).isEqualTo(FUNDING_ACCOUNT_SN);
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
    void testCreateSpendSubjectFundingRelationShouldSupportSixtyFourCharSubjectRefs() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(LONG_FUNDING_ACCOUNT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setSn(LONG_RELATION_SN)
                .setSpendSubjectId(LONG_FUNDING_ACCOUNT_SN)
                .setFundingAccountId(LONG_FUNDING_ACCOUNT_SN)
                .setTargetSubjectId(LONG_FUNDING_ACCOUNT_SN));

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(LONG_RELATION_SN)
                        .setSpendSubjectId(LONG_FUNDING_ACCOUNT_SN)
                        .setFundingAccountId(LONG_FUNDING_ACCOUNT_SN)
                        .setTargetSubjectId(LONG_FUNDING_ACCOUNT_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        assertThat(relationId).isPositive();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getSpendSubjectId()).isEqualTo(LONG_FUNDING_ACCOUNT_SN);
        assertThat(records.getFirst().getFundingAccountId()).isEqualTo(LONG_FUNDING_ACCOUNT_SN);
        assertThat(records.getFirst().getTargetSubjectId()).isEqualTo(LONG_FUNDING_ACCOUNT_SN);
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
     * 场景：运营创建资金来源关系时配置了倒置或空的生效窗口。
     * 输入：validTo 早于或等于 validFrom 的 ACTIVE 资金来源关系。
     * 输出：创建被拒绝，不留下资金来源候选。
     * 红线：无效窗口不得进入 route 候选池，也不得写账或污染关系证据。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectInvalidValidityWindowWithoutRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setValidFrom(now)
                .setValidTo(now.minusSeconds(1))))
                .hasMessageContaining("资金来源关系生效时间必须早于失效时间");

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setValidFrom(now)
                .setValidTo(now)))
                .hasMessageContaining("资金来源关系生效时间必须早于失效时间");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建资金来源关系时把外部账户号或通道密钥放入扩展上下文。
     * 输入：contextVariables 含嵌套 bankAccountNo 字段，或嵌套 secretKey 字段。
     * 输出：创建被拒绝，不留下资金来源关系，也不改变资金账户账本或账务事实。
     * 红线：资金来源关系不得成为外部账户号、PAN、CVV 或 token secret 的旁路存储。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectSensitiveContextVariablesWithoutRelation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        List<LedgerDTO> fundingLedgersBefore = loadFundingAccountLedgers();

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setContextVariables("{\"externalAccount\":{\"bankAccountNo\":\"123456789012\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setContextVariables(UNQUOTED_SENSITIVE_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isZero();
        assertThat(loadFundingAccountLedgers())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(fundingLedgersBefore);
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
     * 场景：运营提前配置未来生效的同优先级资金来源，用于当前资金来源到期后的计划切换。
     * 输入：两个 ACTIVE 非默认关系使用相同 priority，但 validFrom/validTo 首尾相接，不存在重叠。
     * 输出：未来关系创建成功；当前 route 候选仍只返回当前有效关系。
     * 红线：优先级唯一性只限制同一时刻有效候选，不得阻断计划内资金来源切换，也不得写账。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldAllowNonOverlappingPriorityCandidate() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setSn(SECOND_FUNDING_ACCOUNT_SN));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime switchAt = now.plusHours(1);
        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setDefaultRelation(Boolean.FALSE)
                .setValidFrom(now.minusDays(1))
                .setValidTo(switchAt));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long futureRelationId = fundingRelationService.createSpendSubjectFundingRelation(createPriorityOrderRelationRequest()
                .setPriority(20)
                .setValidFrom(switchAt)
                .setValidTo(now.plusDays(1)));

        List<SpendSubjectFundingRelationDTO> currentRelations = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        SpendSubjectFundingRelationDTO futureRelation = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(PRIORITY_ORDER_RELATION_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();

        assertThat(futureRelationId).isPositive();
        assertThat(currentRelations)
                .extracting(SpendSubjectFundingRelationDTO::getSn)
                .containsExactly(RELATION_SN);
        assertThat(futureRelation.getPriority()).isEqualTo(20);
        assertThat(futureRelation.getValidFrom()).isAfter(now);
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
        LocalDateTime now = LocalDateTime.now().withNano(0);
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

    /**
     * 场景：资金来源关系仍为 ACTIVE，但目标资金账户已被风控暂停。
     * 输入：先创建可用资金账户和 ACTIVE 关系，再将资金账户状态改为 SUSPENDED。
     * 输出：查询当前 ACTIVE 资金来源候选时不返回该关系。
     * 红线：资金账户不可借记时不得进入 route 候选，也不得因为候选过滤写账或覆盖关系证据。
     */
    @Test
    void testQuerySpendSubjectFundingRelationsShouldExcludeUnavailableFundingAccountCandidates() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createRelationRequest()
                .setDefaultRelation(Boolean.FALSE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        jdbcTemplate.update("UPDATE t_funding_account SET status = ? WHERE sn = ?",
                FundsAccountStatus.SUSPENDED.name(),
                FUNDING_ACCOUNT_SN);

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records).isEmpty();
        assertThat(countRows("t_spend_subject_funding_rel", "sn", RELATION_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：VCC 共享卡或平台授信场景把最终资金责任配置到信用账户子账户。
     * 输入：支出主体资金责任关系声明 targetSubjectType = CREDIT_ACCOUNT，targetSubjectId = 信用账户号。
     * 输出：关系可创建、可按目标主体查询，兼容 fundingAccountId 不再是唯一写入事实。
     * 红线：配置资金责任目标主体不写账、不改余额，不把信用账户误退化为 fundingAccountId。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldSupportCreditAccountTargetSubject() {
        creditAccountService.createCreditAccount(createCreditAccountRequest(CREDIT_TARGET_SN, FundsAccountStatus.ACTIVE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long relationId = fundingRelationService.createSpendSubjectFundingRelation(createCreditTargetRelationRequest());

        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(TENANT_ID)
                        .setSpendSubjectId(SPEND_SUBJECT_ID)
                        .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setTargetSubjectId(CREDIT_TARGET_SN)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                        .setDefaultRelation(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(relationId).isPositive();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst())
                .satisfies(relation -> {
                    assertThat(relation.getSn()).isEqualTo(CREDIT_TARGET_RELATION_SN);
                    assertThat(relation.getTargetSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(relation.getTargetSubjectId()).isEqualTo(CREDIT_TARGET_SN);
                    assertThat(relation.getFundingAccountId()).isNull();
                    assertThat(relation.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(relation.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
                });
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：信用账户目标主体已被风控暂停。
     * 输入：ACTIVE 资金责任关系指向 SUSPENDED 信用账户。
     * 输出：创建被拒绝，不留下候选关系。
     * 红线：不可用信用账户不得进入资金责任候选池，也不得写账。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectUnavailableCreditAccountTargetWithoutRelation() {
        creditAccountService.createCreditAccount(createCreditAccountRequest(SUSPENDED_CREDIT_TARGET_SN,
                FundsAccountStatus.SUSPENDED));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(
                createCreditTargetRelationRequest()
                        .setSn(SUSPENDED_CREDIT_TARGET_RELATION_SN)
                        .setTargetSubjectId(SUSPENDED_CREDIT_TARGET_SN)))
                .hasMessageContaining("资金责任目标主体不可用");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", SUSPENDED_CREDIT_TARGET_RELATION_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营误把预算组配置为最终资金责任主体。
     * 输入：targetSubjectType = BUDGET_GROUP。
     * 输出：创建被拒绝。
     * 红线：预算组和 Spend Rule 只能做控制和解释，不能成为最终资金责任主体。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldRejectBudgetGroupTargetSubjectWithoutRelation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingRelationService.createSpendSubjectFundingRelation(createCreditTargetRelationRequest()
                .setSn(BUDGET_TARGET_RELATION_SN)
                .setTargetSubjectType(FundsSubjectType.BUDGET_GROUP)
                .setTargetSubjectId("budget_relation_target")))
                .hasMessageContaining("资金责任目标主体类型不支持");

        assertThat(countRows("t_spend_subject_funding_rel", "sn", BUDGET_TARGET_RELATION_SN)).isZero();
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
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn IN (?, ?, ?, ?, ?)",
                RELATION_SN,
                LONG_RELATION_SN,
                DUPLICATE_DEFAULT_RELATION_SN,
                PRIORITY_CONFLICT_RELATION_SN,
                PRIORITY_ORDER_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn IN (?, ?, ?)",
                CREDIT_TARGET_RELATION_SN,
                SUSPENDED_CREDIT_TARGET_RELATION_SN,
                BUDGET_TARGET_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?, ?)",
                FUNDING_ACCOUNT_SN,
                LONG_FUNDING_ACCOUNT_SN,
                SECOND_FUNDING_ACCOUNT_SN,
                THIRD_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                CREDIT_TARGET_SN,
                SUSPENDED_CREDIT_TARGET_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?, ?)",
                FUNDING_ACCOUNT_SN,
                LONG_FUNDING_ACCOUNT_SN,
                SECOND_FUNDING_ACCOUNT_SN,
                THIRD_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn IN (?, ?)",
                CREDIT_TARGET_SN,
                SUSPENDED_CREDIT_TARGET_SN);
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

    private CreateSpendSubjectFundingRelationRequest createCreditTargetRelationRequest() {
        return createRelationRequest()
                .setSn(CREDIT_TARGET_RELATION_SN)
                .setFundingAccountId(null)
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_TARGET_SN);
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

    private CreateCreditAccountRequest createCreditAccountRequest(String sn, FundsAccountStatus status) {
        return new CreateCreditAccountRequest()
                .setSn(sn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setStatus(status);
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
            CreditAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class
    })
    static class Config {
    }
}

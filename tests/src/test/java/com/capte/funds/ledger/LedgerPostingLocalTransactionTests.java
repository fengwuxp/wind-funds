package com.capte.funds.ledger;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.dto.LedgerEntryDTO;
import com.capte.funds.ledger.dto.LedgerTransactionCreateResult;
import com.capte.funds.ledger.dto.LedgerTransactionDTO;
import com.capte.funds.ledger.dto.LedgerTransactionPostResult;
import com.capte.funds.ledger.query.LedgerEntryQuery;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.query.LedgerTransactionQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.request.UpdateLedgerTransactionRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.ledger.service.LedgerTransactionService;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.LedgerBalanceProjectionService;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 账本入账本地事务契约测试。
 */
@SpringJUnitConfig(LedgerPostingLocalTransactionTests.Config.class)
class LedgerPostingLocalTransactionTests extends LedgerTransactionPostingTestSupport {

    private final LedgerTransactionPostingService postingService;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    LedgerPostingLocalTransactionTests(LedgerTransactionPostingService postingService,
                                       JdbcTemplate jdbcTemplate) {
        this.postingService = postingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from t_ledger_entry");
        jdbcTemplate.update("delete from t_ledger_posting_plan");
        jdbcTemplate.update("delete from t_ledger_transaction");
        jdbcTemplate.update("delete from t_ledger");
        jdbcTemplate.update("""
                insert into t_ledger (
                    id, tenant_id, subject_id, subject_type, ledger_profile_code, ledger_profile_version,
                    ledger_subject_code, ledger_subject_category, normal_balance_side, is_allow_negative,
                    currency, period_type, period_id, settlement_policy, cut_off_time,
                    debit_amount, credit_amount, status, version
                ) values (
                    1, 1, 'funding_001', 'FUNDING_ACCOUNT', 'FUNDING_BASIC', 1,
                    'AVAILABLE', 'ASSET', 'DEBIT', 0,
                    'USD', 'LIFETIME', 'LIFETIME', 'RT', '00:00:00',
                    0, 0, 'ACTIVE', 0
                )
                """);
    }

    /**
     * 场景：账本交易、posting plan 和分录已写入后，余额投影失败。
     * 输入：一笔合法 POSTED 账本交易，投影服务在 project 阶段抛出异常。
     * 输出：post 整体失败。
     * 预期：账本交易、记账计划、账本分录全部回滚。
     * 红线：不得留下半成功账本事实，余额投影失败必须回滚同一本地事务内的事实写入。
     */
    @Test
    void testPostShouldRollbackFactsWhenProjectionFailsAfterEntriesInserted() {
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> postingService.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("projection failed after ledger entries inserted");

        assertThat(count("t_ledger_transaction")).isZero();
        assertThat(count("t_ledger_posting_plan")).isZero();
        assertThat(count("t_ledger_entry")).isZero();
    }

    private Integer count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("ledger-posting-rollback;MODE=MySQL")
                    .addScript("classpath:jdbc-schema.sql")
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        LedgerService ledgerService(JdbcTemplate jdbcTemplate) {
            return new JdbcLedgerService(jdbcTemplate);
        }

        @Bean
        LedgerTransactionService ledgerTransactionService(JdbcTemplate jdbcTemplate) {
            return new JdbcLedgerTransactionService(jdbcTemplate);
        }

        @Bean
        LedgerBalanceProjectionService ledgerBalanceProjectionService() {
            return new FailingLedgerBalanceProjectionService();
        }

        @Bean
        LedgerTransactionPostingService postingService(LedgerTransactionService ledgerTransactionService,
                                                       LedgerService ledgerService,
                                                       LedgerBalanceProjectionService projectionService) {
            return new DefaultLedgerTransactionPostingServiceImpl(
                    ledgerTransactionService,
                    ledgerService,
                    List.of(projectionService)
            );
        }
    }

    private static final class JdbcLedgerService implements LedgerService {

        private final JdbcTemplate jdbcTemplate;

        private JdbcLedgerService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException("createLedger");
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            throw new UnsupportedOperationException("updateLedgerBalance");
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException("deleteLedgerByIds");
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            return getLedgerByIds(List.of(id)).getFirst();
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            return jdbcTemplate.query("""
                            select id, tenant_id, subject_id, subject_type, ledger_profile_code,
                                   ledger_profile_version, ledger_subject_code, ledger_subject_category,
                                   normal_balance_side, is_allow_negative, debit_amount, credit_amount,
                                   currency, period_type, period_id
                            from t_ledger
                            where id in (%s)
                            """.formatted(ids.stream()
                                    .map(String::valueOf)
                                    .reduce((left, right) -> left + "," + right)
                                    .orElse("-1")),
                    (rs, rowNum) -> new LedgerDTO()
                            .setId(rs.getLong("id"))
                            .setTenantId(rs.getLong("tenant_id"))
                            .setSubjectId(rs.getString("subject_id"))
                            .setSubjectType(rs.getString("subject_type"))
                            .setLedgerProfileCode(rs.getString("ledger_profile_code"))
                            .setLedgerProfileVersion(rs.getInt("ledger_profile_version"))
                            .setLedgerSubjectCode(LedgerSubjectCode.valueOf(rs.getString("ledger_subject_code")))
                            .setLedgerSubjectCategory(LedgerSubjectCategory.valueOf(
                                    rs.getString("ledger_subject_category")))
                            .setNormalBalanceSide(EntrySide.valueOf(rs.getString("normal_balance_side")))
                            .setAllowNegative(rs.getBoolean("is_allow_negative"))
                            .setDebitAmount(rs.getLong("debit_amount"))
                            .setCreditAmount(rs.getLong("credit_amount"))
                            .setCurrency(CurrencyIsoCode.valueOf(rs.getString("currency")))
                            .setPeriodType(AccountBalancePeriodType.valueOf(rs.getString("period_type")))
                            .setPeriodId(rs.getString("period_id")));
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                               @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("queryLedgers");
        }
    }

    private static final class JdbcLedgerTransactionService implements LedgerTransactionService {

        private final JdbcTemplate jdbcTemplate;

        private JdbcLedgerTransactionService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public @NonNull LedgerTransactionPostResult postLedgerTransaction(@NonNull LedgerTransactionSpec transaction) {
            Integer existing = jdbcTemplate.queryForObject(
                    "select count(*) from t_ledger_transaction where sn = ?",
                    Integer.class,
                    transaction.getSn()
            );
            if (existing != null && existing > 0) {
                return new LedgerTransactionPostResult().setLedgerTransactionId(1L).setNewlyPosted(false);
            }
            jdbcTemplate.update("""
                            insert into t_ledger_transaction (
                                sn, tenant_id, funds_transaction_sn, reference_ledger_transaction_sn,
                                instruction_type, event_type, transaction_type, business_scene, business_sn,
                                amount, currency, original_amount, original_currency, exchange_rate,
                                debit_amount, credit_amount, is_balanced, status, transaction_time,
                                description, context_variables, sha256
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    transaction.getSn(),
                    transaction.getTenantId(),
                    transaction.getFundsTransactionSn(),
                    transaction.getReferenceLedgerTransactionSn(),
                    enumName(transaction.getInstructionType()),
                    transaction.getEventType().name(),
                    enumName(transaction.getTransactionType()),
                    transaction.getBusinessScene(),
                    transaction.getBusinessSn(),
                    transaction.getAmount().getAmount(),
                    transaction.getCurrency().name(),
                    transaction.getOriginalAmount().getAmount(),
                    transaction.getOriginalAmount().getCurrency().name(),
                    transaction.getExchangeRate(),
                    transaction.getTotalDebitAmount().getAmount(),
                    transaction.getTotalCreditAmount().getAmount(),
                    transaction.isBalanced(),
                    transaction.getStatus().name(),
                    transaction.getTransactionTime(),
                    transaction.getDescription(),
                    "{}",
                    "transaction-sha256"
            );
            Long transactionId = jdbcTemplate.queryForObject(
                    "select id from t_ledger_transaction where sn = ?",
                    Long.class,
                    transaction.getSn()
            );
            for (LedgerPostingPlanSpec plan : transaction.getPostingPlans()) {
                insertPostingPlan(transaction, plan);
                for (LedgerPostingPhaseSpec phase : plan.getPostingPhases()) {
                    for (LedgerEntrySpec entry : phase.getEntries()) {
                        insertLedgerEntry(transaction, plan, phase, entry);
                    }
                }
            }
            return new LedgerTransactionPostResult()
                    .setLedgerTransactionId(transactionId)
                    .setNewlyPosted(true);
        }

        @Override
        public @NonNull LedgerTransactionCreateResult createLedgerTransaction(
                @NonNull LedgerTransactionSpec transaction) {
            LedgerTransactionPostResult result = postLedgerTransaction(transaction);
            return new LedgerTransactionCreateResult()
                    .setLedgerTransactionId(result.getLedgerTransactionId())
                    .setCreated(result.isNewlyPosted());
        }

        private void insertPostingPlan(LedgerTransactionSpec transaction, LedgerPostingPlanSpec plan) {
            String phaseCode = plan.getPostingPhases().getFirst().getPhaseCode().name();
            jdbcTemplate.update("""
                            insert into t_ledger_posting_plan (
                                sn, tenant_id, ledger_transaction_sn, funds_transaction_sn, route_leg_id,
                                intent, posting_scope, balance_effect_type, phase_code, amount, currency,
                                debit_amount, credit_amount, is_balanced, description, context_variables, sha256
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    plan.getPlanId(),
                    transaction.getTenantId(),
                    transaction.getSn(),
                    transaction.getFundsTransactionSn(),
                    plan.getRouteLegId(),
                    plan.getIntent().name(),
                    enumName(plan.getPostingScope()),
                    enumName(plan.getBalanceEffectType()),
                    phaseCode,
                    plan.getAmount().getAmount(),
                    plan.getAmount().getCurrency().name(),
                    plan.getTotalDebitAmount().getAmount(),
                    plan.getTotalCreditAmount().getAmount(),
                    plan.isBalanced(),
                    plan.getDescription(),
                    "{}",
                    "plan-sha256"
            );
        }

        private void insertLedgerEntry(LedgerTransactionSpec transaction,
                                       LedgerPostingPlanSpec plan,
                                       LedgerPostingPhaseSpec phase,
                                       LedgerEntrySpec entry) {
            jdbcTemplate.update("""
                            insert into t_ledger_entry (
                                sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn,
                                ledger_id, subject_id, subject_type, ledger_subject_code, ledger_subject_category,
                                entry_side, balance_constraint_type, intent, posting_scope, balance_effect_type,
                                phase_code, business_scene, business_sn, amount, currency, original_amount,
                                original_currency, exchange_rate, transaction_time, settlement_status,
                                reconcile_status, description, context_variables, sha256
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    "%s-%s".formatted(transaction.getSn(), entry.getEntrySide().name()),
                    transaction.getTenantId(),
                    transaction.getSn(),
                    plan.getPlanId(),
                    transaction.getFundsTransactionSn(),
                    entry.getLedgerId(),
                    entry.getSubjectId(),
                    entry.getSubjectType(),
                    entry.getLedgerSubjectCode().name(),
                    entry.getLedgerSubjectCategory().name(),
                    entry.getEntrySide().name(),
                    enumName(entry.getBalanceConstraintType()),
                    enumName(entry.getIntent()),
                    enumName(entry.getPostingScope()),
                    enumName(entry.getBalanceEffectType()),
                    phase.getPhaseCode().name(),
                    entry.getBusinessScene(),
                    entry.getBusinessSn(),
                    entry.getAmount().getAmount(),
                    entry.getCurrency().name(),
                    entry.getOriginalAmount().getAmount(),
                    entry.getOriginalAmount().getCurrency().name(),
                    entry.getExchangeRate(),
                    entry.getTransactionTime(),
                    "PENDING",
                    "PENDING",
                    entry.getDescription(),
                    "{}",
                    "entry-sha256"
            );
        }

        @Override
        public void updateLedgerTransaction(@NonNull UpdateLedgerTransactionRequest request) {
            throw new UnsupportedOperationException("updateLedgerTransaction");
        }

        @Override
        public void deleteLedgerTransactionByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException("deleteLedgerTransactionByIds");
        }

        @Override
        public @NonNull LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id) {
            throw new UnsupportedOperationException("getLedgerTransactionById");
        }

        @Override
        public @NonNull WindPagination<LedgerTransactionDTO> queryAccountLedgerTransactions(
                @NonNull LedgerTransactionQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("queryAccountLedgerTransactions");
        }

        @Override
        public @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id) {
            throw new UnsupportedOperationException("getLedgerEntryById");
        }

        @Override
        public @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(
                @NonNull LedgerEntryQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("queryLedgerEntries");
        }
    }

    private static final class FailingLedgerBalanceProjectionService implements LedgerBalanceProjectionService {

        @Override
        public void project(@NonNull List<LedgerEntrySpec> entries) {
            throw new BaseException("projection failed after ledger entries inserted");
        }

        @Override
        public boolean supports(@NonNull FundsAccountId accountId) {
            return true;
        }
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }
}

package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.entities.CreditAccount;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.mapper.BudgetGroupMapper;
import com.capte.funds.wallet.dal.mapper.CreditAccountMapper;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsAccountQueryServiceImplTests {

    @Test
    void getAccountAndBalanceShouldResolveFundingAccountFromLedger() {
        FundingAccount fundingAccount = fundingAccount();
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount,
                null,
                null,
                List.of(
                        ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L),
                        ledger(12L, LedgerSubjectCode.FROZEN, EntrySide.CREDIT, 0L, 400L)
                )
        );
        FundsAccountId accountId = FundsAccountId.immutable("funding_001",
                DefaultFundsAccountType.USER_WALLET.name());

        FundsAccount account = service.getAccount(accountId);
        FundsAccountBalanceView balance = service.getBalance(accountId);

        assertThat(account.getAccountId()).isEqualTo(accountId);
        assertThat(account.getTenantId()).isEqualTo(1L);
        assertThat(account.getOwner().ownerId()).isEqualTo("user_001");
        assertThat(account.getAccountLedgerIds())
                .containsEntry(LedgerSubjectCode.AVAILABLE, 11L)
                .containsEntry(LedgerSubjectCode.FROZEN, 12L);
        assertThat(balance.getAvailableBalance()).isEqualTo(Money.immutable(2_500L, CurrencyIsoCode.USD));
        assertThat(balance.getFrozenBalance()).isEqualTo(Money.immutable(400L, CurrencyIsoCode.USD));
        assertThat(balance.getPendingBalance()).isEqualTo(Money.immutable(0L, CurrencyIsoCode.USD));
    }

    @Test
    void supportsShouldFallbackToBudgetGroup() {
        BudgetGroup budgetGroup = budgetGroup();
        DefaultFundsAccountQueryServiceImpl service = newService(
                null,
                null,
                budgetGroup,
                List.of(ledger(21L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 800L))
        );
        FundsAccountId accountId = FundsAccountId.immutable("budget_001", "TEAM_BUDGET");

        FundsAccount account = service.getAccount(accountId);

        assertThat(service.supports(accountId)).isTrue();
        assertThat(account.getOwner().ownerType()).isEqualTo(FundsAccountOwnerType.MERCHANT);
        assertThat(account.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
    }

    @Test
    void getAccountShouldResolveFundingAccountByLedgerSubjectType() {
        FundingAccount fundingAccount = fundingAccount();
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount,
                null,
                null,
                List.of(ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L))
        );
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        FundsAccount account = service.getAccount(accountId);

        assertThat(service.supports(accountId)).isTrue();
        assertThat(account.getAccountId()).isEqualTo(accountId);
        assertThat(account.getAccountLedgerIds()).containsEntry(LedgerSubjectCode.AVAILABLE, 11L);
    }

    @Test
    void getAccountShouldRejectUnknownSubject() {
        DefaultFundsAccountQueryServiceImpl service = newService(null, null, null, List.of());
        FundsAccountId accountId = FundsAccountId.immutable("missing_001",
                DefaultFundsAccountType.USER_WALLET.name());

        assertThat(service.supports(accountId)).isFalse();
        assertThatThrownBy(() -> service.getAccount(accountId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void queryCurrentBalancesShouldKeepSubjectOrderAndFilterLedgerCodes() {
        FundingAccount fundingAccount = fundingAccount();
        BudgetGroup budgetGroup = budgetGroup();
        DefaultFundsAccountQueryServiceImpl service = newServiceWithFundingAccounts(
                List.of(fundingAccount, fundingAccount(9L, "funding_shadow", 1L, CurrencyIsoCode.USD)),
                null,
                budgetGroup,
                List.of(
                        ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT,
                                11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L),
                        ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT,
                                12L, LedgerSubjectCode.FROZEN, EntrySide.CREDIT, 0L, 400L),
                        ledger("funding_shadow", FundsSubjectType.FUNDING_ACCOUNT,
                                19L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 9_999L),
                        ledger("budget_001", FundsSubjectType.BUDGET_GROUP,
                                21L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 800L)
                )
        );
        FundsAccountId budgetRef = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP.name());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        List<FundsSubjectBalanceDTO> balances = service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(budgetRef, fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.AVAILABLE)));

        assertThat(balances).extracting(FundsSubjectBalanceDTO::getSubjectRef)
                .containsExactly(budgetRef, fundingRef);
        assertThat(balances.get(0).getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.AVAILABLE);
        assertThat(balances.get(0).getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE).balance())
                .isEqualTo(Money.immutable(800L, CurrencyIsoCode.USD));
        assertThat(balances.get(1).getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.AVAILABLE);
        assertThat(balances.get(1).getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE).balance())
                .isEqualTo(Money.immutable(2_500L, CurrencyIsoCode.USD));
    }

    @Test
    void queryCurrentBalancesShouldUseRequestedPeriodAsLedgerBucketKey() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(),
                null,
                null,
                List.of(
                        ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT,
                                11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L),
                        ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT,
                                12L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 900L)
                                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                                .setPeriodId("2026-05"),
                        ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT,
                                13L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 700L)
                                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                                .setPeriodId("2026-04")
                )
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        List<FundsSubjectBalanceDTO> balances = service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId("2026-05"));

        assertThat(balances).hasSize(1);
        assertThat(balances.getFirst().getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.AVAILABLE);
        assertThat(balances.getFirst().getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE).balance())
                .isEqualTo(Money.immutable(900L, CurrencyIsoCode.USD));
        assertThat(balances.getFirst().getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE).periodType())
                .isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(balances.getFirst().getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE).periodId())
                .isEqualTo("2026-05");
    }

    @Test
    void queryCurrentBalancesShouldRejectSubjectFromAnotherTenant() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(3L, "funding_other_tenant", 2L, CurrencyIsoCode.USD),
                null,
                null,
                List.of()
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_other_tenant",
                FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资金主体租户不匹配");
    }

    @Test
    void queryCurrentBalancesShouldRejectSubjectCurrencyMismatch() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(4L, "funding_eur", 1L, CurrencyIsoCode.EUR),
                null,
                null,
                List.of()
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_eur",
                FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资金主体币种不匹配");
    }

    @Test
    void queryCurrentBalancesShouldRejectDuplicateSubjectRefs() {
        DefaultFundsAccountQueryServiceImpl service = newService(fundingAccount(), null, null, List.of());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef, fundingRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subjectRefs 不能重复");
    }

    @Test
    void queryCurrentBalancesShouldReturnUninitializedBalanceWhenLedgerMissing() {
        DefaultFundsAccountQueryServiceImpl service = newService(fundingAccount(), null, null, List.of());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        List<FundsSubjectBalanceDTO> balances = service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(balances).hasSize(1);
        assertThat(balances.getFirst().getInitialized()).isFalse();
        assertThat(balances.getFirst().getBalanceBuckets()).isEmpty();
    }

    /**
     * 场景：查询主体余额时需要区分“账本已初始化但余额为 0”和“主体尚未建账”。
     * 输入：一个已有 AVAILABLE 账本且 normalBalance 为 0 的主体，以及一个无任何账本的主体。
     * 输出：FundsSubjectBalanceDTO#isInitialized 和 AVAILABLE 余额 bucket。
     * 预期：已有 0 余额账本返回 true；未建账主体返回 false。
     */
    @Test
    void queryCurrentBalancesShouldDistinguishInitializedZeroBalanceFromMissingLedger() {
        DefaultFundsAccountQueryServiceImpl initializedService = newService(
                fundingAccount(),
                null,
                null,
                List.of(ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 0L))
        );
        DefaultFundsAccountQueryServiceImpl missingLedgerService = newService(fundingAccount(), null, null, List.of());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        FundsSubjectBalanceQuery query = new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD);

        FundsSubjectBalanceDTO initializedBalance = initializedService.queryCurrentBalances(query).getFirst();
        FundsSubjectBalanceDTO missingLedgerBalance = missingLedgerService.queryCurrentBalances(query).getFirst();

        assertThat(initializedBalance.isInitialized()).isTrue();
        assertThat(initializedBalance.getBalanceBuckets().get(LedgerSubjectCode.AVAILABLE).balance())
                .isEqualTo(Money.immutable(0L, CurrencyIsoCode.USD));
        assertThat(missingLedgerBalance.isInitialized()).isFalse();
        assertThat(missingLedgerBalance.getBalanceBuckets()).isEmpty();
    }

    @Test
    void getRequiredCurrentBalanceShouldRejectMissingRequiredLedger() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(),
                null,
                null,
                List.of(ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L))
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        assertThatThrownBy(() -> service.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.FROZEN))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资金主体账本不完整");
    }

    @Test
    void getRequiredCurrentBalanceShouldUseQueryContractAndLedgerFilters() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(),
                null,
                null,
                List.of(
                        ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L),
                        ledger(12L, LedgerSubjectCode.FROZEN, EntrySide.CREDIT, 0L, 400L)
                )
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001",
                FundsSubjectType.FUNDING_ACCOUNT.name());

        FundsSubjectBalanceDTO balance = service.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.FROZEN)));

        assertThat(balance.getSubjectRef()).isEqualTo(fundingRef);
        assertThat(balance.getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.FROZEN);
        assertThat(balance.getBalanceBuckets().get(LedgerSubjectCode.FROZEN).balance())
                .isEqualTo(Money.immutable(400L, CurrencyIsoCode.USD));
    }

    @Test
    void getRequiredCurrentBalanceShouldRejectMultipleSubjectRefs() {
        DefaultFundsAccountQueryServiceImpl service = newService(fundingAccount(), null, budgetGroup(), List.of());
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001",
                FundsSubjectType.FUNDING_ACCOUNT.name());
        FundsAccountId budgetRef = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP.name());

        assertThatThrownBy(() -> service.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef, budgetRef))
                .setCurrency(CurrencyIsoCode.USD)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subjectRefs 只能包含一个主体");
    }

    private static DefaultFundsAccountQueryServiceImpl newService(FundingAccount fundingAccount,
                                                                  CreditAccount creditAccount,
                                                                  BudgetGroup budgetGroup,
                                                                  List<LedgerDTO> ledgers) {
        return newServiceWithSubjects(
                fundingAccount == null ? List.of() : List.of(fundingAccount),
                creditAccount == null ? List.of() : List.of(creditAccount),
                budgetGroup == null ? List.of() : List.of(budgetGroup),
                ledgers
        );
    }

    private static DefaultFundsAccountQueryServiceImpl newServiceWithFundingAccounts(List<FundingAccount> fundingAccounts,
                                                                                     CreditAccount creditAccount,
                                                                                     BudgetGroup budgetGroup,
                                                                                     List<LedgerDTO> ledgers) {
        return newServiceWithSubjects(
                fundingAccounts,
                creditAccount == null ? List.of() : List.of(creditAccount),
                budgetGroup == null ? List.of() : List.of(budgetGroup),
                ledgers
        );
    }

    private static DefaultFundsAccountQueryServiceImpl newServiceWithSubjects(List<FundingAccount> fundingAccounts,
                                                                              List<CreditAccount> creditAccounts,
                                                                              List<BudgetGroup> budgetGroups,
                                                                              List<LedgerDTO> ledgers) {
        return new DefaultFundsAccountQueryServiceImpl(
                fundingAccountMapper(fundingAccounts),
                creditAccountMapper(creditAccounts),
                budgetGroupMapper(budgetGroups),
                ledgerService(ledgers)
        );
    }

    private static FundingAccountMapper fundingAccountMapper(List<FundingAccount> accounts) {
        return FundsAccountServiceTestSupport.mapper(
                FundingAccountMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> accounts.stream()
                        .filter(account -> matchesQueryValue(query, "sn", account.getSn()))
                        .filter(account -> matchesQueryValue(query, "account_type", account.getAccountType()))
                        .findFirst()
                        .orElse(null)
        );
    }

    private static CreditAccountMapper creditAccountMapper(List<CreditAccount> accounts) {
        return FundsAccountServiceTestSupport.mapper(
                CreditAccountMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> accounts.stream()
                        .filter(account -> matchesQueryValue(query, "sn", account.getSn()))
                        .filter(account -> matchesQueryValue(query, "account_type", account.getAccountType()))
                        .findFirst()
                        .orElse(null)
        );
    }

    private static BudgetGroupMapper budgetGroupMapper(List<BudgetGroup> budgetGroups) {
        return FundsAccountServiceTestSupport.mapper(
                BudgetGroupMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> budgetGroups.stream()
                        .filter(group -> matchesQueryValue(query, "sn", group.getSn()))
                        .filter(group -> matchesQueryValue(query, "budget_type", group.getBudgetType()))
                        .findFirst()
                        .orElse(null)
        );
    }

    private static boolean matchesQueryValue(QueryWrapper query, String columnName, Object actual) {
        Map<String, Object> values = queryValues(query);
        return !values.containsKey(columnName) || Objects.equals(values.get(columnName), actual);
    }

    private static Map<String, Object> queryValues(QueryWrapper query) {
        Map<String, Object> result = new LinkedHashMap<>();
        QueryCondition condition = whereCondition(query);
        while (condition != null) {
            if (condition.checkEffective() && condition.getColumn() != null) {
                result.put(condition.getColumn().getName(), condition.getValue());
            }
            condition = nextCondition(condition);
        }
        return result;
    }

    private static QueryCondition whereCondition(QueryWrapper query) {
        try {
            Field field = query.getClass().getSuperclass().getDeclaredField("whereQueryCondition");
            field.setAccessible(true);
            return (QueryCondition) field.get(query);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取 QueryWrapper 查询条件失败", exception);
        }
    }

    private static QueryCondition nextCondition(QueryCondition condition) {
        try {
            Field field = QueryCondition.class.getDeclaredField("next");
            field.setAccessible(true);
            return (QueryCondition) field.get(condition);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取 QueryCondition 链路失败", exception);
        }
    }

    private static LedgerService ledgerService(List<LedgerDTO> ledgers) {
        return new LedgerService() {
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
                throw new UnsupportedOperationException("getLedgerById");
            }

            @Override
            public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
                throw new UnsupportedOperationException("getLedgerByIds");
            }

            @Override
            public @NonNull WindPagination<LedgerDTO> queryLedgers(
                    @NonNull LedgerQuery query,
                    @NonNull WindQuery<? extends QueryOrderField> options) {
                return pagination(ledgers.stream()
                        .filter(ledger -> matches(query.getTenantId(), ledger.getTenantId()))
                        .filter(ledger -> matches(query.getSubjectId(), ledger.getSubjectId()))
                        .filter(ledger -> matches(query.getSubjectType(), ledger.getSubjectType()))
                        .filter(ledger -> matches(query.getCurrency(), ledger.getCurrency()))
                        .filter(ledger -> matches(query.getPeriodType(), ledger.getPeriodType()))
                        .filter(ledger -> matches(query.getPeriodId(), ledger.getPeriodId()))
                        .toList());
            }
        };
    }

    private static boolean matches(Object expected, Object actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    @SuppressWarnings("unchecked")
    private static WindPagination<LedgerDTO> pagination(List<LedgerDTO> ledgers) {
        return (WindPagination<LedgerDTO>) Proxy.newProxyInstance(
                WindPagination.class.getClassLoader(),
                new Class<?>[]{WindPagination.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "WindPaginationProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if ("getRecords".equals(method.getName())) {
                        return ledgers;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static FundingAccount fundingAccount() {
        return fundingAccount(1L, "funding_001", 1L, CurrencyIsoCode.USD);
    }

    private static FundingAccount fundingAccount(Long id,
                                                 String sn,
                                                 Long tenantId,
                                                 CurrencyIsoCode currency) {
        FundingAccount result = new FundingAccount();
        result.setId(id);
        result.setTenantId(tenantId);
        result.setSn(sn);
        result.setOwnerId("user_001");
        result.setOwnerType(FundsAccountOwnerType.USER);
        result.setAccountType(DefaultFundsAccountType.USER_WALLET.name());
        result.setCurrency(currency);
        result.setStatus(FundsAccountStatus.ACTIVE);
        result.setVersion(1);
        return result;
    }

    private static BudgetGroup budgetGroup() {
        BudgetGroup result = new BudgetGroup();
        result.setId(2L);
        result.setTenantId(1L);
        result.setSn("budget_001");
        result.setOwnerId("merchant_001");
        result.setOwnerType(FundsAccountOwnerType.MERCHANT);
        result.setBudgetType("TEAM_BUDGET");
        result.setCurrency(CurrencyIsoCode.USD);
        result.setStatus(FundsAccountStatus.ACTIVE);
        result.setVersion(1);
        return result;
    }

    private static LedgerDTO ledger(Long id,
                                    LedgerSubjectCode code,
                                    EntrySide normalBalanceSide,
                                    Long debitAmount,
                                    Long creditAmount) {
        return ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT, id, code, normalBalanceSide,
                debitAmount, creditAmount);
    }

    private static LedgerDTO ledger(String subjectId,
                                    FundsSubjectType subjectType,
                                    Long id,
                                    LedgerSubjectCode code,
                                    EntrySide normalBalanceSide,
                                    Long debitAmount,
                                    Long creditAmount) {
        return new LedgerDTO()
                .setId(id)
                .setGmtCreate(LocalDateTime.of(2026, 5, 7, 12, 0))
                .setGmtModified(LocalDateTime.of(2026, 5, 7, 12, 0))
                .setTenantId(1L)
                .setSubjectId(subjectId)
                .setSubjectType(subjectType.name())
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(normalBalanceSide)
                .setDebitAmount(debitAmount)
                .setCreditAmount(creditAmount)
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name());
    }
}

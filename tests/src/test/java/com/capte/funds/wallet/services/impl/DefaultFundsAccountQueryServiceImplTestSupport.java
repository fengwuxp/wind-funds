package com.capte.funds.wallet.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.entities.CreditAccount;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.mapper.BudgetGroupMapper;
import com.capte.funds.wallet.dal.mapper.CreditAccountMapper;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class DefaultFundsAccountQueryServiceImplTestSupport {

    protected static DefaultFundsAccountQueryServiceImpl newService(FundingAccount fundingAccount,
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

    protected static DefaultFundsAccountQueryServiceImpl newServiceWithFundingAccounts(
            List<FundingAccount> fundingAccounts,
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

    protected static FundingAccount fundingAccount() {
        return fundingAccount(1L, "funding_001", 1L, CurrencyIsoCode.USD);
    }

    protected static FundingAccount fundingAccount(Long id,
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

    protected static BudgetGroup budgetGroup() {
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

    protected static LedgerDTO ledger(Long id,
                                      LedgerSubjectCode code,
                                      EntrySide normalBalanceSide,
                                      Long debitAmount,
                                      Long creditAmount) {
        return ledger("funding_001", FundsSubjectType.FUNDING_ACCOUNT, id, code, normalBalanceSide,
                debitAmount, creditAmount);
    }

    protected static LedgerDTO ledger(String subjectId,
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

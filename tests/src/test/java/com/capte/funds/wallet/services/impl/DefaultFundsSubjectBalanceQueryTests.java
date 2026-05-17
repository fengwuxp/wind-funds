package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsSubjectBalanceQueryTests extends DefaultFundsAccountQueryServiceImplTestSupport {

    /**
     * 场景：批量查询多个资金主体的当前余额。
     * 输入：预算组与 funding account，并只请求 AVAILABLE 余额桶。
     * 输出：按请求顺序返回两个 FundsSubjectBalanceDTO。
     * 预期：结果保持 subjectRefs 顺序，只返回请求的 ledgerSubjectCodes。
     * 红线：批量展示查询不得串主体、串账本桶或带出未请求的余额科目。
     */
    @Test
    void testQueryCurrentBalancesShouldKeepSubjectOrderAndFilterLedgerCodes() {
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

    /**
     * 场景：按指定周期查询资金主体余额。
     * 输入：同一 funding account 同时存在 lifetime 与多个 monthly AVAILABLE 账本。
     * 输出：2026-05 月度 AVAILABLE 余额桶。
     * 预期：余额来自请求周期，periodType 和 periodId 原样返回。
     * 红线：周期化余额查询不得混用 lifetime 或其他月份账本。
     */
    @Test
    void testQueryCurrentBalancesShouldUseRequestedPeriodAsLedgerBucketKey() {
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

    /**
     * 场景：资金主体存在但尚未初始化任何账本。
     * 输入：存在 funding account，但没有关联 ledger。
     * 输出：initialized=false，余额桶为空。
     * 预期：查询返回未初始化状态而非异常。
     * 红线：展示查询不得自动补建账本，也不得把缺失账本伪装成 0 余额。
     */
    @Test
    void testQueryCurrentBalancesShouldReturnUninitializedBalanceWhenLedgerMissing() {
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
     * 红线：不得把真实 0 余额和未初始化状态混为一谈。
     */
    @Test
    void testQueryCurrentBalancesShouldDistinguishInitializedZeroBalanceFromMissingLedger() {
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

    /**
     * 场景：展示查询只请求一个尚未初始化的余额桶，但该主体已有其他账本。
     * 输入：主体已有 AVAILABLE 账本，请求 FROZEN 余额桶。
     * 输出：FundsSubjectBalanceDTO#isInitialized 和余额桶集合。
     * 预期：主体仍标记为已初始化，缺失的展示 bucket 返回空集合。
     * 红线：展示查询不得把“局部 bucket 未建”误判为“主体未建账”，也不得自动补建账本。
     */
    @Test
    void testQueryCurrentBalancesShouldKeepInitializedWhenRequestedBucketMissing() {
        DefaultFundsAccountQueryServiceImpl service = newService(
                fundingAccount(),
                null,
                null,
                List.of(ledger(11L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 0L, 2_500L))
        );
        FundsAccountId fundingRef = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());

        FundsSubjectBalanceDTO balance = service.queryCurrentBalances(new FundsSubjectBalanceQuery()
                .setTenantId(1L)
                .setSubjectRefs(List.of(fundingRef))
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerSubjectCodes(List.of(LedgerSubjectCode.FROZEN))).getFirst();

        assertThat(balance.isInitialized()).isTrue();
        assertThat(balance.getBalanceBuckets()).isEmpty();
    }
}

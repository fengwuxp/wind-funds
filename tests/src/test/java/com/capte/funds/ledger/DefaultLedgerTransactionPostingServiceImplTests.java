package com.capte.funds.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingServiceImplTests extends LedgerTransactionPostingTestSupport {

    /**
     * 场景：已确认账本交易完成入账。
     * 输入：一笔 POSTED 转账交易，包含平衡的 AVAILABLE 借贷分录。
     * 输出：创建账本交易，并把分录交给余额投影服务。
     * 预期：账本交易可追溯，posting plan 平衡，余额投影收到完整分录。
     * 红线：不得绕过账本交易事实直接更新余额投影。
     */
    @Test
    void testPostShouldCreateLedgerTransactionAndProjectEntries() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        service.post(transaction);

        assertThat(transactionService.createdTransactions).containsExactly(transaction);
        assertThat(projectionService.projectedEntries).hasSize(1);
        assertThat(projectionService.projectedEntries.getFirst()).hasSize(2);
    }

    /**
     * 场景：重复入账请求命中账本交易幂等。
     * 输入：账本交易创建服务返回未新建，表示交易事实已存在。
     * 输出：入账服务跳过余额投影。
     * 预期：幂等重放不重复写余额投影，已存在交易只保留一次资金影响。
     * 红线：不得因为重复请求导致同一 ledgerTransactionSn 被重复投影。
     */
    @Test
    void testPostShouldSkipProjectionWhenLedgerTransactionAlreadyExists() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService(false);
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        service.post(transaction);

        assertThat(transactionService.createdTransactions).containsExactly(transaction);
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本入账找不到支持账户的余额投影服务。
     * 输入：posting plan 分录完整，但所有 LedgerBalanceProjectionService 均不支持该账户。
     * 输出：入账服务拒绝继续执行。
     * 预期：不创建账本交易，不产生无法落到余额桶的投影动作。
     * 红线：不得在余额投影职责缺失时只写账本交易事实，造成账实链路断裂。
     */
    @Test
    void testPostShouldFailWhenNoProjectionServiceSupportsAccount() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(false);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到支持的账本余额投影服务");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：同一账户被多个余额投影服务同时支持。
     * 输入：posting plan 分录完整，两个投影服务都声明支持。
     * 输出：入账服务拒绝继续执行。
     * 预期：投影服务路由必须唯一，避免同一分录被重复或分叉处理。
     * 红线：不得让余额投影服务选择存在歧义的账本交易落库。
     */
    @Test
    void testPostShouldFailWhenMultipleProjectionServicesSupportAccount() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService firstProjectionService = new RecordingProjectionService(true);
        RecordingProjectionService secondProjectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(firstProjectionService, secondProjectionService));
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额投影服务不唯一");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(firstProjectionService.projectedEntries).isEmpty();
        assertThat(secondProjectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：交易总借贷平衡，但单个账务计划不平衡。
     * 输入：两个 posting plan 相互抵消，交易整体平衡，但每个 plan 内部不平衡。
     * 输出：入账服务拒绝第一个不平衡 plan。
     * 预期：平衡校验必须落到每个 posting plan，而不是只看交易总额。
     * 红线：不得用跨计划抵消掩盖单个资金事件的账务不平衡。
     */
    @Test
    void testPostShouldRejectUnbalancedPostingPlanEvenWhenTransactionIsBalanced() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000002";
        LedgerPostingPlanSpec debitHeavyPlan = uncheckedPostingPlan(ledgerTransactionSn, "DEBIT_HEAVY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 100L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 1L)
        ));
        LedgerPostingPlanSpec creditHeavyPlan = uncheckedPostingPlan(ledgerTransactionSn, "CREDIT_HEAVY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 1L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 100L)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(debitHeavyPlan, creditHeavyPlan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(debitHeavyPlan.isBalanced()).isFalse();
        assertThat(creditHeavyPlan.isBalanced()).isFalse();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划不平衡")
                .hasMessageContaining("DEBIT_HEAVY");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：同一账务计划内出现多币种分录。
     * 输入：posting plan 借方为 USD，贷方为 EUR。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：单个 posting plan 的借贷分录必须使用同一币种。
     * 红线：不得在未定义换汇事件和汇率规则时把多币种分录记入同一账务计划。
     */
    @Test
    void testPostShouldRejectCurrencyMismatchInsidePostingPlanBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000007";
        LedgerPostingPlanSpec mixedCurrencyPlan = uncheckedPostingPlan(ledgerTransactionSn, "MIXED_CURRENCY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 100L, CurrencyIsoCode.USD),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 100L, CurrencyIsoCode.EUR)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(mixedCurrencyPlan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("记账计划币种不一致")
                .hasMessageContaining("MIXED_CURRENCY")
                .hasMessageContaining(CurrencyIsoCode.USD.name())
                .hasMessageContaining(CurrencyIsoCode.EUR.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易金额币种和实际记账分录币种不一致。
     * 输入：交易金额为 EUR，posting plan 内分录均为 USD。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：交易级金额币种必须和所有 posting plan 币种一致。
     * 红线：不得产生交易事实币种与账本事实币种不一致的入账记录。
     */
    @Test
    void testPostShouldRejectTransactionCurrencyMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000012";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan), CurrencyIsoCode.EUR);

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易币种与记账计划币种不一致")
                .hasMessageContaining("LE_000000000012")
                .hasMessageContaining(CurrencyIsoCode.EUR.name())
                .hasMessageContaining(CurrencyIsoCode.USD.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

}

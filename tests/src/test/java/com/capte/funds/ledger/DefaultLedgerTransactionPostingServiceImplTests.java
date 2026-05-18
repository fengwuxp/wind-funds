package com.capte.funds.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
     * 场景：外部支付工具或外部账户引用被错误写成账本分录主体。
     * 输入：银行卡、VA、VCC、PSP、外部账户等非内部资金主体 subjectType。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：外部引用只能作为 route/transaction 快照，不得成为 LedgerEntry.subjectType。
     * 红线：不得把外部工具引用创建为内部 ledger subject。
     */
    @ParameterizedTest
    @ValueSource(strings = {"BANK_CARD", "VIRTUAL_ACCOUNT", "VCC", "PSP_ACCOUNT", "EXTERNAL_ACCOUNT"})
    void testPostShouldRejectExternalRefsAsLedgerEntrySubject(String externalSubjectType) {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_EXTERNAL_REF_00000001";
        LedgerEntrySpec externalEntry = entry(EntrySide.DEBIT, ledgerTransactionSn)
                .setSubjectId("external_ref_001")
                .setSubjectType(externalSubjectType);
        LedgerEntrySpec fundingEntry = entry(EntrySide.CREDIT, ledgerTransactionSn);
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn,
                List.of(postingPlan(ledgerTransactionSn, List.of(externalEntry, fundingEntry))));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining(externalSubjectType);
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
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

}

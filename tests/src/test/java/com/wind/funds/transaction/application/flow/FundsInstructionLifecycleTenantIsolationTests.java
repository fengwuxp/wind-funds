package com.wind.funds.transaction.application.flow;

import com.wind.funds.route.RouteResolver;
import com.wind.funds.route.RouteSnapshotFactory;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * 资金指令生命周期写入租户隔离测试。
 */
class FundsInstructionLifecycleTenantIsolationTests extends FundsTransactionFlowTestSupport {

    private static final Long FOREIGN_TENANT_ID = TENANT_ID + 1;

    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 8, 26, 15, 0);

    @Autowired
    private DefaultFundsInstructionLifecycleSaver instructionLifecycleSaver;

    @Autowired
    private DefaultFundsFrozenOrderLifecycleSaver frozenOrderLifecycleSaver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RouteResolver routeResolver;

    @Autowired
    private RouteSnapshotFactory routeSnapshotFactory;

    /**
     * 场景：外租户 instruction 持有当前租户尚在处理中的交易 lifecycle result。
     * 输入：真实 saver beforePosting 创建的 PROCESSING transaction/detail。
     * 输出：markFailed 在首次读取时按当前 instruction tenant 拒绝。
     * 红线：不得修改外租户 transaction/detail 状态、错误信息、账本引用、Ledger 或 Balance。
     */
    @Test
    void testMarkFailedShouldRejectForeignTenantTransactionLifecycleResult() {
        FundsAccountId accountId = fundingAccount("funding_user");
        FundsInstructionSpec instruction = transactionInstruction(TENANT_ID, accountId);
        ResolvedRouteSpec resolvedRoute = routeResolver.resolve(instruction);
        RouteSnapshotSpec routeSnapshot = routeSnapshotFactory.createSnapshot(resolvedRoute);
        FundsInstructionLifecycleResult result = instructionLifecycleSaver.beforePosting(
                instruction, resolvedRoute, routeSnapshot);
        FundsTransaction transactionBefore = fundsTransactionsByBusinessSn(instruction.getBusinessSn()).getFirst();
        var detailsBefore = detailSnapshots(instruction.getBusinessSn());
        assertThat(detailsBefore)
                .isNotEmpty()
                .extracting(DetailSnapshot::state)
                .containsOnly(FundsTransactionDetailState.PROCESSING);
        Map<?, ?> balanceBefore = Map.copyOf(balance(accountId).getBalanceBuckets());
        List<String> ledgerTransactionsBefore = ledgerTransactionSns();
        List<String> postingPlansBefore = postingPlanSns();
        List<String> entriesBefore = entrySns();

        Throwable failure = catchThrowable(() -> instructionLifecycleSaver.markFailed(
                transactionInstruction(FOREIGN_TENANT_ID, accountId),
                result,
                new IllegalStateException("外租户失败结果不得写入")));

        FundsTransaction transactionAfter = fundsTransactionsByBusinessSn(instruction.getBusinessSn()).getFirst();
        var detailsAfter = detailSnapshots(instruction.getBusinessSn());
        assertSoftly(softly -> {
            softly.assertThat(failure).hasMessageContaining("不存在");
            softly.assertThat(transactionBefore.getState()).isEqualTo(FundsTransactionState.PROCESSING);
            softly.assertThat(transactionAfter.getState()).isEqualTo(transactionBefore.getState());
            softly.assertThat(detailsAfter).isEqualTo(detailsBefore);
            softly.assertThat(fundsTransactionsByBusinessSn(instruction.getBusinessSn())).hasSize(1);
            softly.assertThat(fundsTransactionDetailsByBusinessSn(instruction.getBusinessSn()))
                    .hasSize(detailsBefore.size());
            softly.assertThat(ledgerTransactionSns()).isEqualTo(ledgerTransactionsBefore);
            softly.assertThat(postingPlanSns()).isEqualTo(postingPlansBefore);
            softly.assertThat(entrySns()).isEqualTo(entriesBefore);
            softly.assertThat(balance(accountId).getBalanceBuckets()).isEqualTo(balanceBefore);
        });
    }

    /**
     * 场景：外租户 instruction 持有当前租户尚未入账的冻结单 lifecycle result。
     * 输入：真实 frozen saver beforePosting 创建的 CREATED frozen order。
     * 输出：markSucceeded 在首次读取时按当前 instruction tenant 拒绝。
     * 红线：不得修改外租户冻结单状态、释放累计、账本引用、Ledger 或 Balance。
     */
    @Test
    void testMarkSucceededShouldRejectForeignTenantFrozenOrderLifecycleResult() {
        FundsAccountId accountId = fundingAccount("funding_user");
        topup(accountId, 20L, "LIFECYCLE_TENANT_FREEZE_SETUP");
        FundsInstructionSpec instruction = freezeInstruction(TENANT_ID, accountId);
        ResolvedRouteSpec resolvedRoute = routeResolver.resolve(instruction);
        RouteSnapshotSpec routeSnapshot = routeSnapshotFactory.createSnapshot(resolvedRoute);
        FundsInstructionLifecycleResult result = frozenOrderLifecycleSaver.beforePosting(
                instruction, resolvedRoute, routeSnapshot);
        FundsFrozenOrder orderBefore = frozenOrderByBusinessSn(instruction.getBusinessSn());
        Map<?, ?> balanceBefore = Map.copyOf(balance(accountId).getBalanceBuckets());
        List<String> ledgerTransactionsBefore = ledgerTransactionSns();
        List<String> postingPlansBefore = postingPlanSns();
        List<String> entriesBefore = entrySns();

        Throwable failure = catchThrowable(() -> frozenOrderLifecycleSaver.markSucceeded(
                freezeInstruction(FOREIGN_TENANT_ID, accountId), result, "LE_FOREIGN_TENANT"));

        FundsFrozenOrder orderAfter = frozenOrderByBusinessSn(instruction.getBusinessSn());
        assertSoftly(softly -> {
            softly.assertThat(failure).hasMessageContaining("不存在");
            softly.assertThat(orderBefore.getState()).isEqualTo(FundsFrozenOrderState.CREATED);
            softly.assertThat(orderAfter.getState()).isEqualTo(orderBefore.getState());
            softly.assertThat(orderAfter.getReleasedAmount()).isEqualTo(orderBefore.getReleasedAmount());
            softly.assertThat(orderAfter.getFreezeLedgerTransactionSn())
                    .isEqualTo(orderBefore.getFreezeLedgerTransactionSn());
            softly.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_funds_frozen_order WHERE tenant_id = ?",
                    Integer.class,
                    TENANT_ID)).isEqualTo(1);
            softly.assertThat(ledgerTransactionSns()).isEqualTo(ledgerTransactionsBefore);
            softly.assertThat(postingPlanSns()).isEqualTo(postingPlansBefore);
            softly.assertThat(entrySns()).isEqualTo(entriesBefore);
            softly.assertThat(balance(accountId).getBalanceBuckets()).isEqualTo(balanceBefore);
        });
    }

    private FundsInstructionSpec transactionInstruction(Long tenantId, FundsAccountId accountId) {
        return instruction(tenantId,
                FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TOPUP,
                DefaultFundsTransactionType.TOPUP,
                "LIFECYCLE_TENANT_TRANSACTION",
                accountId);
    }

    private FundsInstructionSpec freezeInstruction(Long tenantId, FundsAccountId accountId) {
        return instruction(tenantId,
                FundsInstructionType.BALANCE_CONTROL,
                FundsTransactionEventType.FREEZE,
                DefaultFundsTransactionType.BALANCE_CONTROL,
                "LIFECYCLE_TENANT_FREEZE",
                accountId);
    }

    private FundsInstructionSpec instruction(Long tenantId,
                                             FundsInstructionType instructionType,
                                             FundsTransactionEventType eventType,
                                             DefaultFundsTransactionType transactionType,
                                             String businessSn,
                                             FundsAccountId accountId) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(tenantId)
                .instructionType(instructionType)
                .eventType(eventType)
                .transactionType(transactionType)
                .amount(Money.immutable(10L, CURRENCY))
                .accountId(accountId)
                .businessScene(eventType.name())
                .businessSn(businessSn)
                .eventTime(EVENT_TIME)
                .operator(WindOperatorFactory.system())
                .contextVariables(Map.of())
                .build();
    }

    private List<DetailSnapshot> detailSnapshots(String businessSn) {
        return fundsTransactionDetailsByBusinessSn(businessSn).stream()
                .map(detail -> new DetailSnapshot(
                        detail.getSn(),
                        detail.getState(),
                        detail.getErrorCode(),
                        detail.getErrorMessage(),
                        detail.getLedgerTransactionSn()))
                .toList();
    }

    private List<String> ledgerTransactionSns() {
        return ledgerTransactions().stream().map(transaction -> transaction.getSn()).toList();
    }

    private List<String> postingPlanSns() {
        return postingPlans().stream().map(plan -> plan.getSn()).toList();
    }

    private List<String> entrySns() {
        return entries().stream().map(entry -> entry.getSn()).toList();
    }

    private record DetailSnapshot(String sn,
                                  FundsTransactionDetailState state,
                                  String errorCode,
                                  String errorMessage,
                                  String ledgerTransactionSn) {
    }
}

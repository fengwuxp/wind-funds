package com.wind.funds.transaction;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.funds.transaction.projection.FundsTransactionProjectionPublishContext;
import com.wind.funds.transaction.projection.FundsTransactionProjectionPublisher;
import com.wind.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.ledger.LedgerTransactionPostingService;
import com.wind.funds.route.RouteResolver;
import com.wind.funds.route.RouteSnapshotFactory;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.spec.FundsInstructionReferenceSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 默认资金指令编排器。
 *
 * <p>职责：
 * <ul>
 *   <li>串联资金指令处理主链路：路由、快照、生命周期保存、账本组装、账本入账</li>
 *   <li>在同一本地事务内协调业务交易生命周期和账本写入结果</li>
 *   <li>对无账务影响或已完成的指令进行短路处理</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不自行解析资金路径，路径解析委托 RouteResolver</li>
 *   <li>不自行生成账本交易或操作账本表，账务翻译与写入委托 LedgerTransactionPostingService</li>
 *   <li>不承接交易投影重放，重放与生产修复由治理模块负责</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class DefaultRoutedFundsInstructionOrchestrator implements FundsInstructionOrchestrator<FundsInstructionSpec> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoutedFundsInstructionOrchestrator.class);

    private final RouteResolver routeResolver;

    private final RouteSnapshotFactory routeSnapshotFactory;

    private final LedgerTransactionPostingService ledgerTransactionPostingService;

    private final FundsInstructionLifecycleRecorder fundsInstructionLifecycleRecorder;

    private final FundsAccountQueryService fundsAccountQueryService;

    private final List<FundsTransactionProjectionPublisher> projectionPublishers;

    /**
     * 执行资金指令主流程。
     *
     * <p>能力范围：解析 Route、创建 RouteSnapshot、保存业务生命周期、组装账本交易并提交入账。
     * 返回值是本次生命周期事实流水号，不是账本交易流水号。</p>
     *
     * @param instruction 资金指令
     * @return 资金交易流水号
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public @NonNull String execute(@NonNull FundsInstructionSpec instruction) {
        ResolvedRouteSpec resolvedRoute = routeResolver.resolve(instruction);
        RouteSnapshotSpec routeSnapshot = routeSnapshotFactory.createSnapshot(resolvedRoute);
        FundsInstructionLifecycleResult lifecycleResult = fundsInstructionLifecycleRecorder.beforePosting(instruction,
                resolvedRoute, routeSnapshot);
        if (lifecycleResult.isCompleted()) {
            LOGGER.info("资金指令已完成，复用原生命周期事实，instructionType={}, eventType={}, transactionType={}, "
                            + "businessScene={}, businessSn={}, transactionSn={}",
                    instruction.getInstructionType(), instruction.getEventType(), instruction.getTransactionType(),
                    instruction.getBusinessScene(), instruction.getBusinessSn(), lifecycleResult.getTransactionSn());
            publishProjection(instruction, resolvedRoute, routeSnapshot, lifecycleResult);
            return lifecycleResult.getTransactionSn();
        }
        try {
            assertAccountCapabilities(instruction, resolvedRoute, lifecycleResult.getTransactionSn());
            if (resolvedRoute.getLegs().isEmpty()) {
                fundsInstructionLifecycleRecorder.markSucceeded(instruction, lifecycleResult, null);
                logAfterCommit(() -> LOGGER.info("资金指令无账务影响，已标记成功，instructionType={}, eventType={}, "
                                + "transactionType={}, businessScene={}, businessSn={}, transactionSn={}, amount={}, currency={}",
                        instruction.getInstructionType(), instruction.getEventType(), instruction.getTransactionType(),
                        instruction.getBusinessScene(), instruction.getBusinessSn(), lifecycleResult.getTransactionSn(),
                        instruction.getAmount().getAmount(), instruction.getAmount().getCurrency()));
                publishProjection(instruction, resolvedRoute, routeSnapshot,
                        completedLifecycleResult(lifecycleResult, null));
                return lifecycleResult.getTransactionSn();
            }
            String ledgerTransactionSn = ledgerTransactionPostingService.post(
                    instruction, lifecycleResult.getTransactionSn(), resolvedRoute);
            fundsInstructionLifecycleRecorder.markSucceeded(instruction, lifecycleResult, ledgerTransactionSn);
            logAfterCommit(() -> LOGGER.info("资金指令执行完成，instructionType={}, eventType={}, transactionType={}, businessScene={}, "
                            + "businessSn={}, transactionSn={}, ledgerTransactionSn={}, amount={}, currency={}",
                    instruction.getInstructionType(), instruction.getEventType(), instruction.getTransactionType(),
                    instruction.getBusinessScene(), instruction.getBusinessSn(), lifecycleResult.getTransactionSn(),
                    ledgerTransactionSn, instruction.getAmount().getAmount(), instruction.getAmount().getCurrency()));
            publishProjection(instruction, resolvedRoute, routeSnapshot,
                    completedLifecycleResult(lifecycleResult, ledgerTransactionSn));
            return lifecycleResult.getTransactionSn();
        } catch (RuntimeException | Error exception) {
            fundsInstructionLifecycleRecorder.markFailed(instruction, lifecycleResult, exception);
            LOGGER.warn("资金指令执行失败，已尝试记录失败生命周期事实，instructionType={}, eventType={}, transactionType={}, "
                            + "businessScene={}, businessSn={}, transactionSn={}",
                    instruction.getInstructionType(), instruction.getEventType(), instruction.getTransactionType(),
                    instruction.getBusinessScene(), instruction.getBusinessSn(), lifecycleResult.getTransactionSn(),
                    exception);
            throw exception;
        }
    }

    /**
     * 判断当前编排器是否支持指定资金指令类型。
     *
     * <p>能力范围：仅用于上层选择编排器，不执行任何业务逻辑。</p>
     *
     * @param specType 资金指令类型
     * @return true 表示支持 FundsInstructionSpec 及其子类型
     */
    @Override
    public boolean supports(@NonNull Class<FundsInstructionSpec> specType) {
        return FundsInstructionSpec.class.isAssignableFrom(specType);
    }

    private void assertAccountCapabilities(FundsInstructionSpec instruction,
                                           ResolvedRouteSpec resolvedRoute,
                                           String transactionSn) {
        Long tenantId = instruction.getTenantId();
        switch (resolvedRoute.getEventType()) {
            case TOPUP -> assertCanReceive(tenantId, instruction.getAccountId(), transactionSn);
            case TRANSFER -> {
                assertCanPay(tenantId, instruction.getPayerAccountId(), transactionSn);
                assertCanReceive(tenantId, instruction.getPayeeAccountId(), transactionSn);
            }
            case PAY -> {
                assertCanPay(tenantId, instruction.getAccountId(), transactionSn);
                assertCanReceive(tenantId, instruction.getPayeeId(), transactionSn);
            }
            case REFUND -> {
                if (!isOriginalTransactionReference(instruction.getReference())) {
                    assertCanPay(tenantId, instruction.getPayerId(), transactionSn);
                    assertCanReceive(tenantId, instruction.getAccountId(), transactionSn);
                }
            }
            case WITHDRAW -> assertCanWithdraw(tenantId, instruction.getAccountId(), transactionSn);
            case FEE_CHARGE -> assertCanPay(tenantId, instruction.getAccountId(), transactionSn);
            case AUTHORIZE -> {
                assertCanPay(tenantId, instruction.getAccountId(), transactionSn);
                FundsAccountId linkedFundingAccountId = instruction.getLinkedFundingAccountId();
                if (linkedFundingAccountId != null && !linkedFundingAccountId.equals(instruction.getAccountId())) {
                    assertCanPay(tenantId, linkedFundingAccountId, transactionSn);
                }
            }
            case FEE_REFUND, CLEARING_CONFIRM, SETTLEMENT_LOCK, SETTLEMENT_RELEASE,
                    PAYOUT_SUCCEEDED, PAYOUT_FAILED, REVERSAL, COMPLETE, AUTH_REFUND,
                    FREEZE, UNFREEZE, BALANCE_ADJUST, LIMIT_ADJUST -> {
                // 原交易逆向、余额控制和清结算阶段只执行既有事实，不重判当前账户能力。
            }
            default -> throw new LedgerPostingRejectedException(transactionSn,
                    "资金事件未配置账户能力准入规则，eventType = " + resolvedRoute.getEventType());
        }
    }

    private boolean isOriginalTransactionReference(FundsInstructionReferenceSpec reference) {
        return reference != null
                && reference.getReferenceType() == FundsInstructionReferenceType.ORIGINAL_TRANSACTION;
    }

    private void assertCanPay(Long tenantId, FundsAccountId accountId, String transactionSn) {
        FundsAccount account = requiredAccount(tenantId, accountId);
        if (!account.canPay()) {
            throw capabilityRejected(transactionSn, accountId, account, "PAY");
        }
    }

    private void assertCanReceive(Long tenantId, FundsAccountId accountId, String transactionSn) {
        FundsAccount account = requiredAccount(tenantId, accountId);
        if (!account.canReceive()) {
            throw capabilityRejected(transactionSn, accountId, account, "RECEIVE");
        }
    }

    private void assertCanWithdraw(Long tenantId, FundsAccountId accountId, String transactionSn) {
        FundsAccount account = requiredAccount(tenantId, accountId);
        if (!account.canWithdraw()) {
            throw capabilityRejected(transactionSn, accountId, account, "WITHDRAW");
        }
    }

    private LedgerPostingRejectedException capabilityRejected(String transactionSn,
                                                               FundsAccountId accountId,
                                                               FundsAccount account,
                                                               String capability) {
        return new LedgerPostingRejectedException(transactionSn,
                "资金账户不具备 %s 能力，accountId = %s, accountType = %s, capabilitySource = %s".formatted(
                        capability, accountId.id(), accountId.type(), account.getCapabilitySource()));
    }

    private FundsAccount requiredAccount(Long tenantId, FundsAccountId accountId) {
        AssertUtils.notNull(accountId, "资金账户能力校验缺少账户标识");
        return fundsAccountQueryService.getAccount(tenantId, accountId);
    }

    private FundsInstructionLifecycleResult completedLifecycleResult(FundsInstructionLifecycleResult source,
                                                                     String ledgerTransactionSn) {
        return new FundsInstructionLifecycleResult()
                .setTransactionSn(source.getTransactionSn())
                .setTransactionDetailSns(source.getTransactionDetailSns())
                .setLedgerTransactionSn(ledgerTransactionSn)
                .setCompleted(true);
    }

    private void publishProjection(FundsInstructionSpec instruction,
                                   ResolvedRouteSpec resolvedRoute,
                                   RouteSnapshotSpec routeSnapshot,
                                   FundsInstructionLifecycleResult lifecycleResult) {
        if (projectionPublishers.isEmpty()) {
            return;
        }
        FundsTransactionProjectionPublishContext context = FundsTransactionProjectionPublishContext.builder()
                .instruction(instruction)
                .resolvedRoute(resolvedRoute)
                .routeSnapshot(routeSnapshot)
                .lifecycleResult(lifecycleResult)
                .build();
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    publishProjectionImmediately(context);
                }
            });
            return;
        }
        publishProjectionImmediately(context);
    }

    private void logAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void publishProjectionImmediately(FundsTransactionProjectionPublishContext context) {
        for (FundsTransactionProjectionPublisher publisher : projectionPublishers) {
            try {
                publisher.publish(context);
            } catch (RuntimeException exception) {
                LOGGER.warn("交易投影发布失败，已保留交易和账务事实等待治理重放或补偿，businessScene={}, businessSn={}, "
                                + "transactionSn={}, publisher={}",
                        context.instruction().getBusinessScene(), context.instruction().getBusinessSn(),
                        context.lifecycleResult().getTransactionSn(), publisher.getClass().getName(), exception);
            }
        }
    }
}

package com.capte.funds.transaction;

import com.wind.integration.funds.model.route.ImmutableReplayRequestSpec;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.route.RouteReplayService;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.RouteSnapshotFactory;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.FundsInstructionOrchestrator;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
 *   <li>不自行生成账本交易，账务翻译委托 LedgerPostingAssembler</li>
 *   <li>不直接操作账本表，账本写入委托 LedgerTransactionPostingService</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class DefaultRoutedFundsInstructionOrchestrator implements FundsInstructionOrchestrator<FundsInstructionSpec> {

    private final RouteResolver routeResolver;

    private final RouteSnapshotFactory routeSnapshotFactory;

    private final RouteReplayService routeReplayService;

    private final LedgerPostingAssembler<ResolvedRouteSpec> postingAssembler;

    private final LedgerTransactionPostingService ledgerTransactionPostingService;

    private final FundsInstructionLifecycleSaver fundsInstructionLifecycleSaver;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    /**
     * 执行资金指令主流程。
     *
     * <p>能力范围：解析 Route、创建 RouteSnapshot、保存业务生命周期、组装账本交易并提交入账。
     * 返回值是资金交易流水号，不是账本交易流水号。</p>
     *
     * @param instruction 资金指令
     * @return 资金交易流水号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String execute(@NonNull FundsInstructionSpec instruction) {
        ResolvedRouteSpec resolvedRoute = resolveRoute(instruction);
        RouteSnapshotSpec routeSnapshot = routeSnapshotFactory.createSnapshot(resolvedRoute);
        FundsInstructionLifecycleResult lifecycleResult = fundsInstructionLifecycleSaver.beforePosting(
                instruction, resolvedRoute, routeSnapshot);
        if (lifecycleResult.isCompleted()) {
            return lifecycleResult.getTransactionSn();
        }
        if (resolvedRoute.getLegs().isEmpty()) {
            fundsInstructionLifecycleSaver.markSucceeded(lifecycleResult, null);
            return lifecycleResult.getTransactionSn();
        }
        LedgerTransactionSpec transaction = postingAssembler.assemble(instruction, lifecycleResult.getTransactionSn(),
                resolvedRoute);
        try {
            ledgerTransactionPostingService.post(transaction);
            fundsInstructionLifecycleSaver.markSucceeded(lifecycleResult, transaction.getSn());
            return lifecycleResult.getTransactionSn();
        } catch (Throwable throwable) {
            fundsInstructionLifecycleSaver.markFailed(lifecycleResult, throwable);
            throw throwable;
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

    private ResolvedRouteSpec resolveRoute(FundsInstructionSpec instruction) {
        if (shouldReplay(instruction)) {
            RouteSnapshotSpec routeSnapshot = requireReplaySnapshot(instruction);
            return routeReplayService.replay(routeSnapshot, ImmutableReplayRequestSpec.builder()
                    .replayType(resolveReplayType(instruction.getEventType()))
                    .eventType(instruction.getEventType())
                    .businessScene(instruction.getBusinessScene())
                    .businessSn(instruction.getBusinessSn())
                    .referenceBusinessSn(resolveReferenceBusinessSn(instruction.getReference()))
                    .referenceSnapshotId(routeSnapshot.getSnapshotId())
                    .amount(instruction.getAmount())
                    .originalAmount(instruction.getOriginalAmount())
                    .exchangeRate(instruction.getExchangeRate())
                    .eventTime(instruction.getEventTime())
                    .description(instruction.getDescription())
                    .operator(instruction.getOperator())
                    .contextVariables(instruction.getContextVariables())
                    .build());
        }
        return routeResolver.resolve(instruction);
    }

    private boolean shouldReplay(FundsInstructionSpec instruction) {
        if (!isReplayEvent(instruction.getEventType())) {
            return false;
        }
        FundsInstructionReferenceSpec reference = instruction.getReference();
        return reference != null
                && hasText(reference.getReferenceSn())
                && isRouteSnapshotReference(reference.getReferenceType());
    }

    private boolean isRouteSnapshotReference(FundsInstructionReferenceType referenceType) {
        return switch (referenceType) {
            case ORIGINAL_TRANSACTION, AUTHORIZATION, REFUND, FEE, FREEZE_ORDER -> true;
            case EXTERNAL_TRANSACTION -> false;
        };
    }

    private RouteSnapshotSpec requireReplaySnapshot(FundsInstructionSpec instruction) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        Optional<RouteSnapshotSpec> routeSnapshot = switch (reference.getReferenceType()) {
            case FREEZE_ORDER -> fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(reference.getReferenceSn());
            default -> fundsTransactionQueryService.findRouteSnapshotByTransactionSn(reference.getReferenceSn());
        };
        AssertUtils.isTrue(routeSnapshot.isPresent(), "RouteSnapshot 回放事件未找到原路径快照，referenceSn = {}",
                reference.getReferenceSn());
        RouteSnapshotSpec result = routeSnapshot.get();
        assertReplayOnceNotConsumed(instruction, reference, result);
        return result;
    }

    private void assertReplayOnceNotConsumed(FundsInstructionSpec instruction,
                                             FundsInstructionReferenceSpec reference,
                                             RouteSnapshotSpec routeSnapshot) {
        for (RouteLegSpec leg : routeSnapshot.getLegs()) {
            if (leg.getReplayPolicy() != RouteReplayPolicy.REPLAY_ONCE) {
                continue;
            }
            AssertUtils.isFalse(fundsTransactionQueryService.hasConsumedReplayLeg(
                            reference.getReferenceSn(), instruction.getEventType(), leg.getLegId()),
                    "RouteSnapshot leg 仅允许成功回放一次，referenceSn = {}，eventType = {}，legId = {}",
                    reference.getReferenceSn(), instruction.getEventType(), leg.getLegId());
        }
    }

    private boolean isReplayEvent(FundsTransactionEventType eventType) {
        return switch (eventType) {
            case REVERSAL, SETTLE, AUTH_REFUND, CHARGEBACK, REFUND, FEE_REFUND, UNFREEZE -> true;
            default -> false;
        };
    }

    private RouteReplayType resolveReplayType(FundsTransactionEventType eventType) {
        return switch (eventType) {
            case REVERSAL -> RouteReplayType.RELEASE_HOLD;
            case SETTLE -> RouteReplayType.AUTHORIZATION_SETTLEMENT;
            case AUTH_REFUND -> RouteReplayType.AUTHORIZATION_REFUND;
            case REFUND -> RouteReplayType.REFUND;
            case FEE_REFUND -> RouteReplayType.FEE_REFUND;
            case CHARGEBACK -> RouteReplayType.CHARGEBACK;
            case UNFREEZE -> RouteReplayType.UNFREEZE;
            default -> throw new IllegalArgumentException("unsupported replay eventType: " + eventType);
        };
    }

    private @Nullable String resolveReferenceBusinessSn(@Nullable FundsInstructionReferenceSpec reference) {
        return reference == null ? null : reference.getReferenceBusinessSn();
    }

    private boolean hasText(@Nullable String value) {
        return value != null && !value.isBlank();
    }
}

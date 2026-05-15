package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 委托式资金指令生命周期记录器。
 *
 * <p>职责：根据资金指令选择唯一生命周期事实记录器。</p>
 */
@Service
@Primary
@AllArgsConstructor
public class DelegatingFundsInstructionLifecycleRecorder implements FundsInstructionLifecycleSaver {

    private final List<FundsInstructionLifecycleRecorder> delegates;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return supportedDelegates(instruction).size() == 1;
    }

    @Override
    public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                  @NonNull ResolvedRouteSpec resolvedRoute,
                                                                  @NonNull RouteSnapshotSpec routeSnapshot) {
        return requireDelegate(instruction).beforePosting(instruction, resolvedRoute, routeSnapshot);
    }

    @Override
    public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                              @NonNull FundsInstructionLifecycleResult result,
                              @Nullable String ledgerTransactionSn) {
        requireDelegate(instruction).markSucceeded(instruction, result, ledgerTransactionSn);
    }

    @Override
    public void markFailed(@NonNull FundsInstructionSpec instruction,
                           @NonNull FundsInstructionLifecycleResult result,
                           @NonNull Throwable cause) {
        requireDelegate(instruction).markFailed(instruction, result, cause);
    }

    private FundsInstructionLifecycleRecorder requireDelegate(FundsInstructionSpec instruction) {
        List<FundsInstructionLifecycleRecorder> candidates = supportedDelegates(instruction);
        AssertUtils.isFalse(candidates.isEmpty(),
                "未找到支持的资金指令生命周期记录器，instructionType = {}，eventType = {}",
                instruction.getInstructionType(), instruction.getEventType());
        AssertUtils.isTrue(candidates.size() == 1,
                "资金指令生命周期记录器不唯一，instructionType = {}，eventType = {}",
                instruction.getInstructionType(), instruction.getEventType());
        return candidates.getFirst();
    }

    private List<FundsInstructionLifecycleRecorder> supportedDelegates(FundsInstructionSpec instruction) {
        return delegates.stream()
                .filter(delegate -> delegate != this)
                .filter(delegate -> delegate.supports(instruction))
                .toList();
    }
}

package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 退款路径账户状态准入。
 *
 * <p>退款可以进入激活、冻结或挂起账户，最终是否可写由目标 Ledger 的 NORMAL/CLOSING 准入继续控制；
 * 关闭账户不自动重开或重路由。</p>
 */
@Component
@AllArgsConstructor
public class RefundRouteAdmission {

    private final FundsAccountQueryService fundsAccountQueryService;

    void validate(@NonNull FundsInstructionSpec instruction, @NonNull ResolvedRouteSpec route) {
        if (!isRefundEvent(instruction.getEventType())) {
            return;
        }
        Map<String, SubjectRef> targets = new LinkedHashMap<>();
        route.getLegs().forEach(leg -> {
            SubjectRef subjectRef = leg.getTargetNode().getSubjectRef();
            targets.putIfAbsent(subjectRef.getSubjectType().name() + ":" + subjectRef.getSubjectId(), subjectRef);
        });
        targets.values().forEach(this::assertRefundTargetAvailable);
    }

    private boolean isRefundEvent(FundsTransactionEventType eventType) {
        return eventType == FundsTransactionEventType.REFUND
                || eventType == FundsTransactionEventType.AUTH_REFUND
                || eventType == FundsTransactionEventType.FEE_REFUND;
    }

    private void assertRefundTargetAvailable(SubjectRef subjectRef) {
        FundsAccountId accountId = FundsAccountId.immutable(
                subjectRef.getSubjectId(), subjectRef.getSubjectType().name());
        FundsAccount account = fundsAccountQueryService.getAccount(accountId);
        AssertUtils.isTrue(account.getState().canAcceptRefund(),
                "关闭账户不允许承接退款，accountId = {}，status = {}",
                accountId,
                account.getState());
    }
}

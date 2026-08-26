package com.wind.funds.transaction.application.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsSettlementInstructionConverter;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.wind.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.wind.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.model.dto.FundsSettlementReleaseResultDTO;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.transaction.model.request.FundsSettlementReleaseRequest;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.transaction.core.Money;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算锁定资金命令服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
@NullMarked
public class FundsSettlementTransactionServiceImpl implements FundsSettlementTransactionService {

    static final String SETTLEMENT_RELEASE_HOLD = "SETTLEMENT_RELEASE_HOLD";

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private final FundsSettlementInstructionConverter settlementInstructionConverter;

    private final FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    private final FundsAccountQueryService fundsAccountQueryService;

    private final FundsTransactionMapper fundsTransactionMapper;

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final FundsBalanceControlInstructionConverter balanceControlInstructionConverter;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public String lock(FundsSettlementLockRequest request, WindOperator operator) {
        validateRequest(request, operator);
        String transactionSn = fundsInstructionOrchestrator.execute(
                settlementInstructionConverter.convert(request, operator));
        log.info("结算资金锁定完成，等待事务提交，settlementOrderSn={}, transactionSn={}, accountType={}, "
                        + "accountId={}, amount={}, currency={}",
                request.getSettlementOrderSn(), transactionSn, request.getAccountId().type(), request.getAccountId().id(),
                request.getAmount().getAmount(), request.getAmount().getCurrency());
        return transactionSn;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundsSettlementReleaseResultDTO release(FundsSettlementReleaseRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "结算释放请求不能为空");
        AssertUtils.notNull(operator, "结算释放操作人不能为空");
        AssertUtils.hasText(request.getLockFundsTransactionSn(), "原结算锁定资金交易流水号不能为空");
        AssertUtils.hasText(request.getSettlementOrderSn(), "结算单流水号不能为空");

        FundsTransaction lockTransaction = fundsTransactionMapper.selectBySnForUpdate(
                TenantContextHolder.requireTenantId(), request.getLockFundsTransactionSn());
        AssertUtils.notNull(lockTransaction, "原结算锁定资金交易不存在，transactionSn = {}",
                request.getLockFundsTransactionSn());
        RouteSnapshotSpec lockRoute = requireLockRoute(lockTransaction, request.getSettlementOrderSn());
        RouteParticipantSpec participant = lockRoute.getParticipants().getFirst();
        FundsAccountId accountId = FundsAccountId.immutable(
                participant.getSubjectRef().getSubjectId(), participant.getSubjectRef().getSubjectType().name());
        Money amount = Money.immutable(lockTransaction.getAmount(), lockTransaction.getCurrency());

        FundsFrozenOrder completedReleaseHold = findReleaseHold(request.getSettlementOrderSn());
        if (completedReleaseHold != null) {
            String releaseTransactionSn = completedReleaseHold.getTransactionSn();
            AssertUtils.hasText(releaseTransactionSn,
                    "资金冻结单请求参数不一致，sn = {}", completedReleaseHold.getSn());
            assertSameReleaseHold(completedReleaseHold, accountId, amount, releaseTransactionSn,
                    completedReleaseHold.getSn());
            requireCompletedReleaseTransaction(releaseTransactionSn, lockTransaction, accountId, amount,
                    request.getSettlementOrderSn());
            log.info("结算资金释放幂等复用，settlementOrderSn={}, lockTransactionSn={}, releaseTransactionSn={}, "
                            + "freezeOrderSn={}, amount={}, currency={}",
                    request.getSettlementOrderSn(), request.getLockFundsTransactionSn(), releaseTransactionSn,
                    completedReleaseHold.getSn(), amount.getAmount(), amount.getCurrency());
            return releaseResult(releaseTransactionSn, completedReleaseHold.getSn());
        }

        validateReleaseAccount(accountId, amount);
        String releaseTransactionSn = fundsInstructionOrchestrator.execute(
                settlementInstructionConverter.convertRelease(accountId, amount, request.getSettlementOrderSn(),
                        request.getLockFundsTransactionSn(), operator));
        String freezeOrderSn = fundsInstructionOrchestrator.execute(
                balanceControlInstructionConverter.convertToReferencedFreezeInstruction(
                        new FundsBalanceFreezeRequest()
                                .setAccountId(accountId)
                                .setAmount(amount)
                                .setBusinessScene(SETTLEMENT_RELEASE_HOLD)
                                .setBusinessSn(request.getSettlementOrderSn() + ":HOLD")
                                .setDescription("settlement release hold"),
                        FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        releaseTransactionSn,
                        operator));
        FundsFrozenOrder releaseHold = findReleaseHold(request.getSettlementOrderSn());
        AssertUtils.notNull(releaseHold, "结算释放冻结单不存在，freezeOrderSn = {}", freezeOrderSn);
        assertSameReleaseHold(releaseHold, accountId, amount, releaseTransactionSn, freezeOrderSn);
        log.info("结算资金释放完成，等待事务提交，settlementOrderSn={}, lockTransactionSn={}, "
                        + "releaseTransactionSn={}, freezeOrderSn={}, amount={}, currency={}",
                request.getSettlementOrderSn(), request.getLockFundsTransactionSn(), releaseTransactionSn,
                freezeOrderSn, amount.getAmount(), amount.getCurrency());
        return releaseResult(releaseTransactionSn, freezeOrderSn);
    }

    private void requireCompletedReleaseTransaction(String releaseTransactionSn,
                                                    FundsTransaction lockTransaction,
                                                    FundsAccountId accountId,
                                                    Money amount,
                                                    String settlementOrderSn) {
        FundsTransaction releaseTransaction = fundsTransactionMapper.selectBySnForUpdate(
                TenantContextHolder.requireTenantId(), releaseTransactionSn);
        AssertUtils.notNull(releaseTransaction, "结算释放资金交易不存在，transactionSn = {}", releaseTransactionSn);
        AssertUtils.isTrue(releaseTransaction.getTransactionType() == DefaultFundsTransactionType.SETTLEMENT
                        && releaseTransaction.getState() == FundsTransactionState.CLOSED
                        && FundsTransactionEventType.SETTLEMENT_RELEASE.name().equals(releaseTransaction.getBusinessScene())
                        && (settlementOrderSn + ":RELEASE").equals(releaseTransaction.getBusinessSn())
                        && lockTransaction.getSn().equals(releaseTransaction.getReferenceTransactionSn())
                        && releaseTransaction.getAmount() == amount.getAmount()
                        && releaseTransaction.getCurrency() == amount.getCurrency(),
                "结算释放资金交易与原锁定交易不一致，transactionSn = {}", releaseTransactionSn);
        RouteSnapshotSpec route = fundsTransactionQueryService.findRouteSnapshotByTransactionSn(
                        TenantContextHolder.requireTenantId(), releaseTransactionSn)
                .orElseThrow(() -> new IllegalArgumentException(
                        "结算释放资金交易缺少 RouteSnapshot，transactionSn = " + releaseTransactionSn));
        AssertUtils.isTrue(route.getEventType() == FundsTransactionEventType.SETTLEMENT_RELEASE
                        && route.getTransactionType() == DefaultFundsTransactionType.SETTLEMENT
                        && route.getParticipants().size() == 1
                        && route.getParticipants().getFirst().getParticipantRole() == RouteParticipantRole.PAYER
                        && route.getParticipants().getFirst().getSubjectRef().getSubjectId().equals(accountId.id())
                        && route.getParticipants().getFirst().getSubjectRef().getSubjectType().name().equals(accountId.type())
                        && route.getLegs().size() == 1
                        && isAccountNode(route.getLegs().getFirst().getSourceNode().getSubjectRef(), accountId)
                        && isAccountNode(route.getLegs().getFirst().getTargetNode().getSubjectRef(), accountId),
                "结算释放资金交易 RouteSnapshot 与原锁定交易不一致，transactionSn = {}", releaseTransactionSn);
        Money participantAmount = route.getParticipants().getFirst().getAmount();
        AssertUtils.notNull(participantAmount, "结算释放 RouteSnapshot 缺少主体金额，transactionSn = {}", releaseTransactionSn);
        AssertUtils.isTrue(participantAmount.getAmount() == amount.getAmount()
                        && participantAmount.getCurrency() == amount.getCurrency(),
                "结算释放 RouteSnapshot 主体金额与原锁定交易不一致，transactionSn = {}", releaseTransactionSn);
    }

    private FundsSettlementReleaseResultDTO releaseResult(String releaseTransactionSn, String freezeOrderSn) {
        return new FundsSettlementReleaseResultDTO()
                .setReleaseFundsTransactionSn(releaseTransactionSn)
                .setReleaseFreezeOrderSn(freezeOrderSn);
    }

    private FundsFrozenOrder findReleaseHold(String settlementOrderSn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        return fundsFrozenOrderMapper.selectOneByQuery(QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(TenantContextHolder.requireTenantId()))
                .and(ref.freezeType.eq(SETTLEMENT_RELEASE_HOLD))
                .and(ref.businessScene.eq(SETTLEMENT_RELEASE_HOLD))
                .and(ref.businessSn.eq(settlementOrderSn + ":HOLD")));
    }

    private void assertSameReleaseHold(FundsFrozenOrder order,
                                       FundsAccountId accountId,
                                       Money amount,
                                       String releaseTransactionSn,
                                       String freezeOrderSn) {
        AssertUtils.isTrue(order.getState() == FundsFrozenOrderState.FROZEN
                        && order.getSn().equals(freezeOrderSn)
                        && releaseTransactionSn.equals(order.getTransactionSn())
                        && order.getSubjectId().equals(accountId.id())
                        && order.getSubjectType().name().equals(accountId.type())
                        && order.getAmount() == amount.getAmount()
                        && order.getCurrency() == amount.getCurrency(),
                "结算释放冻结单请求参数不一致，freezeOrderSn = {}", order.getSn());
    }

    private RouteSnapshotSpec requireLockRoute(FundsTransaction transaction, String settlementOrderSn) {
        AssertUtils.isTrue(transaction.getTransactionType() == DefaultFundsTransactionType.SETTLEMENT
                        && transaction.getState() == FundsTransactionState.CLOSED
                        && FundsTransactionEventType.SETTLEMENT_LOCK.name().equals(transaction.getBusinessScene())
                        && settlementOrderSn.equals(transaction.getBusinessSn()),
                "原资金交易不是当前结算单已完成的 SETTLEMENT_LOCK，transactionSn = {}",
                transaction.getSn());
        RouteSnapshotSpec route = fundsTransactionQueryService.findRouteSnapshotByTransactionSn(
                        transaction.getTenantId(), transaction.getSn())
                .orElseThrow(() -> new IllegalArgumentException(
                        "原结算锁定资金交易缺少 RouteSnapshot，transactionSn = " + transaction.getSn()));
        AssertUtils.isTrue(route.getEventType() == FundsTransactionEventType.SETTLEMENT_LOCK
                        && route.getTransactionType() == DefaultFundsTransactionType.SETTLEMENT,
                "原资金交易 RouteSnapshot 不是 SETTLEMENT_LOCK，transactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(route.getParticipants().size() == 1
                        && route.getParticipants().getFirst().getParticipantRole() == RouteParticipantRole.PAYER,
                "原结算锁定 RouteSnapshot 必须包含唯一付款主体，transactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(route.getLegs().size() == 1, "原结算锁定 RouteSnapshot 必须包含唯一资金路径，transactionSn = {}",
                transaction.getSn());
        RouteLegSpec leg = route.getLegs().getFirst();
        AssertUtils.isTrue(isAccountNode(leg.getSourceNode().getSubjectRef(), route.getParticipants().getFirst())
                        && isAccountNode(leg.getTargetNode().getSubjectRef(), route.getParticipants().getFirst()),
                "原结算锁定 RouteSnapshot 资金路径不合法，transactionSn = {}", transaction.getSn());
        Money participantAmount = route.getParticipants().getFirst().getAmount();
        AssertUtils.notNull(participantAmount, "原结算锁定 RouteSnapshot 缺少主体金额，transactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(participantAmount.getAmount() == transaction.getAmount()
                        && participantAmount.getCurrency() == transaction.getCurrency(),
                "原结算锁定 RouteSnapshot 主体金额与交易不一致，transactionSn = {}", transaction.getSn());
        return route;
    }

    private boolean isAccountNode(SubjectRef subjectRef, FundsAccountId accountId) {
        return subjectRef.getSubjectId().equals(accountId.id())
                && subjectRef.getSubjectType().name().equals(accountId.type());
    }

    private boolean isAccountNode(SubjectRef subjectRef, RouteParticipantSpec participant) {
        return subjectRef.getSubjectId().equals(participant.getSubjectRef().getSubjectId())
                && subjectRef.getSubjectType() == participant.getSubjectRef().getSubjectType();
    }

    private void validateReleaseAccount(FundsAccountId accountId, Money amount) {
        FundsAccount account = fundsAccountQueryService.getAccount(accountId);
        AssertUtils.isTrue(account.isAvailable(), "结算释放资金账户不可用，accountId = {}", accountId);
        AssertUtils.equals(account.getCurrency(), amount.getCurrency(),
                "结算释放金额币种必须与账户币种一致，accountId = {}", accountId);
    }

    private void validateRequest(FundsSettlementLockRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "结算锁定请求不能为空");
        AssertUtils.notNull(operator, "结算锁定操作人不能为空");
        AssertUtils.notNull(request.getAccountId(), "结算资金账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "结算资金账户不能是外部账户");
        AssertUtils.isFalse(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE.equals(request.getAccountId().type()),
                "支出控制范围不能作为结算资金账户");
        AssertUtils.notNull(request.getAmount(), "结算锁定金额不能为空");
        AssertUtils.isTrue(request.getAmount().getAmount() > 0, "结算锁定金额必须大于 0");
        AssertUtils.hasText(request.getSettlementOrderSn(), "结算单流水号不能为空");
        FundsAccount account = fundsAccountQueryService.getAccount(request.getAccountId());
        AssertUtils.isTrue(account.isAvailable(), "结算资金账户不可用，accountId = {}", request.getAccountId());
        AssertUtils.equals(account.getCurrency(), request.getAmount().getCurrency(),
                "结算锁定金额币种必须与账户币种一致，accountId = {}", request.getAccountId());
    }
}

package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.model.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import com.wind.funds.wallet.service.AccountHierarchyRelationService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将正向路由使用的当前账户层级关系固化到参与方快照。
 *
 * <p>回放路径必须沿用原 RouteSnapshot，不查询当前关系；本类也不创建父账户参与方或 RouteLeg。</p>
 */
@Component
@AllArgsConstructor
public class RouteAccountHierarchySnapshotAppender {

    private final AccountHierarchyRelationService accountHierarchyRelationService;

    @NonNull ResolvedRouteSpec append(@NonNull FundsInstructionSpec instruction,
                                      @NonNull ResolvedRouteSpec route) {
        if (RouteReplaySupport.isReplayInstruction(instruction)) {
            return route;
        }
        Map<AccountKey, AccountHierarchySnapshotSpec> snapshots = resolveSnapshots(route);
        List<RouteParticipantSpec> participants = route.getParticipants().stream()
                .map(participant -> copyParticipant(participant, snapshots.get(accountKey(participant.getSubjectRef()))))
                .toList();
        return copyRoute(route, participants);
    }

    private Map<AccountKey, AccountHierarchySnapshotSpec> resolveSnapshots(ResolvedRouteSpec route) {
        Map<AccountKey, AccountHierarchySnapshotSpec> result = new LinkedHashMap<>();
        for (RouteParticipantSpec participant : route.getParticipants()) {
            SubjectRef subjectRef = participant.getSubjectRef();
            AccountKey key = accountKey(subjectRef);
            if (result.containsKey(key)) {
                continue;
            }
            Long tenantId = subjectRef.getTenantId() == null ? route.getTenantId() : subjectRef.getTenantId();
            AssertUtils.notNull(tenantId, "账户层级快照查询租户 ID 不能为空，accountId = {}", subjectRef.getSubjectId());
            AccountHierarchySnapshotSpec snapshot = accountHierarchyRelationService
                    .findAccountHierarchyRelation(tenantId,
                            FundsAccountId.immutable(subjectRef.getSubjectId(), subjectRef.getSubjectType()))
                    .map(this::snapshot)
                    .orElse(null);
            result.put(key, snapshot);
        }
        return result;
    }

    private AccountHierarchySnapshotSpec snapshot(AccountHierarchyRelationDTO relation) {
        return ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn(relation.getSn())
                .parentAccountRef(ImmutableSubjectRef.builder()
                        .tenantId(relation.getTenantId())
                        .subjectId(relation.getParentAccountId())
                        .subjectType(relation.getParentAccountType())
                        .currency(relation.getCurrency().name())
                        .build())
                .build();
    }

    private RouteParticipantSpec copyParticipant(RouteParticipantSpec participant,
                                                 @Nullable AccountHierarchySnapshotSpec hierarchySnapshot) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(participant.getParticipantRole())
                .subjectRef(participant.getSubjectRef())
                .ledgerProfileCode(participant.getLedgerProfileCode())
                .currency(participant.getCurrency())
                .amount(participant.getAmount())
                .description(participant.getDescription())
                .accountHierarchySnapshot(hierarchySnapshot)
                .contextVariables(participant.getContextVariables())
                .build();
    }

    private ResolvedRouteSpec copyRoute(ResolvedRouteSpec route, List<RouteParticipantSpec> participants) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(route.getTenantId())
                .routeCode(route.getRouteCode())
                .routeVersion(route.getRouteVersion())
                .businessScene(route.getBusinessScene())
                .businessSn(route.getBusinessSn())
                .instructionType(route.getInstructionType())
                .eventType(route.getEventType())
                .transactionType(route.getTransactionType())
                .participants(participants)
                .legs(route.getLegs())
                .routingDecision(route.getRoutingDecision())
                .paymentInstrumentRef(route.getPaymentInstrumentRef())
                .externalAccountRef(route.getExternalAccountRef())
                .platformAccounts(route.getPlatformAccounts())
                .resolvedAt(route.getResolvedAt())
                .expiresAt(route.getExpiresAt())
                .description(route.getDescription())
                .contextVariables(route.getContextVariables())
                .build();
    }

    private AccountKey accountKey(SubjectRef subjectRef) {
        return new AccountKey(subjectRef.getTenantId(), subjectRef.getSubjectType(), subjectRef.getSubjectId());
    }

    private record AccountKey(@Nullable Long tenantId, FundsSubjectType accountType, String accountId) {
    }
}

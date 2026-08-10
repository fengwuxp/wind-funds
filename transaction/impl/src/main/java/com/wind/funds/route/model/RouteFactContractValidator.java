package com.wind.funds.route.model;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;

import java.time.LocalDateTime;

/**
 * Route fact contract checks shared by runtime routes and persisted snapshots.
 */
final class RouteFactContractValidator {

    private RouteFactContractValidator() {
    }

    static void validateResolvedRoute(String routeCode,
                                      String routeVersion,
                                      String businessScene,
                                      String businessSn,
                                      FundsInstructionType instructionType,
                                      FundsTransactionEventType eventType,
                                      DefaultFundsTransactionType transactionType,
                                      LocalDateTime resolvedAt) {
        validateRouteIdentity("resolvedRoute",
                routeCode,
                routeVersion,
                businessScene,
                businessSn,
                instructionType,
                eventType,
                transactionType,
                resolvedAt);
    }

    static void validateRouteSnapshot(String snapshotId,
                                      String snapshotSchemaVersion,
                                      String routeCode,
                                      String routeVersion,
                                      String businessScene,
                                      String businessSn,
                                      FundsInstructionType instructionType,
                                      FundsTransactionEventType eventType,
                                      DefaultFundsTransactionType transactionType,
                                      LocalDateTime resolvedAt) {
        AssertUtils.hasText(snapshotId, "routeSnapshot.snapshotId must not be blank");
        AssertUtils.hasText(snapshotSchemaVersion, "routeSnapshot.snapshotSchemaVersion must not be blank");
        validateRouteIdentity("routeSnapshot",
                routeCode,
                routeVersion,
                businessScene,
                businessSn,
                instructionType,
                eventType,
                transactionType,
                resolvedAt);
    }

    private static void validateRouteIdentity(String owner,
                                              String routeCode,
                                              String routeVersion,
                                              String businessScene,
                                              String businessSn,
                                              FundsInstructionType instructionType,
                                              FundsTransactionEventType eventType,
                                              DefaultFundsTransactionType transactionType,
                                              LocalDateTime resolvedAt) {
        AssertUtils.hasText(routeCode, owner + ".routeCode must not be blank");
        AssertUtils.hasText(routeVersion, owner + ".routeVersion must not be blank");
        AssertUtils.hasText(businessScene, owner + ".businessScene must not be blank");
        AssertUtils.hasText(businessSn, owner + ".businessSn must not be blank");
        AssertUtils.notNull(instructionType, owner + ".instructionType must not be null");
        AssertUtils.notNull(eventType, owner + ".eventType must not be null");
        AssertUtils.notNull(transactionType, owner + ".transactionType must not be null");
        AssertUtils.notNull(resolvedAt, owner + ".resolvedAt must not be null");
    }
}

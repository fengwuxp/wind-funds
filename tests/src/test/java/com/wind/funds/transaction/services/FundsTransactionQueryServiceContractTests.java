package com.wind.funds.transaction.services;

import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FundsTransactionQueryService} 公共契约测试。
 */
class FundsTransactionQueryServiceContractTests {

    /**
     * 默认实现不能静默忽略排除参数，避免未显式支持排除能力的实现误算 replay
     * 剩余额度。
     */
    @Test
    void testDefaultReplayConsumptionQueryRejectsExcludedBusinessEvent() {
        FundsTransactionQueryService queryService = new MinimalFundsTransactionQueryService();

        assertThatThrownBy(() -> queryService.sumConsumedReplayLegAmount("FT1",
                FundsTransactionEventType.REFUND,
                "LEG1",
                CurrencyIsoCode.USD,
                "SCENE",
                "SN1"))
                .hasMessageContaining("必须由实现类显式支持");
    }

    private static final class MinimalFundsTransactionQueryService implements FundsTransactionQueryService {

        @Override
        public Optional<FundsTransactionDTO> queryFundsTransaction(String transactionSn) {
            return Optional.empty();
        }

        @Override
        public Optional<FundsTransactionDTO> findFundsTransactionByBusiness(Long tenantId,
                                                                            String businessScene,
                                                                            String businessSn) {
            return Optional.empty();
        }

        @Override
        public Optional<FundsTransactionDTO> findFundsTransactionByExternalFundsFact(Long tenantId,
                                                                                      String externalSourceCode,
                                                                                      String externalFundsFactSn,
                                                                                      FundsEffectType effectType) {
            return Optional.empty();
        }

        @Override
        public List<FundsTransactionDetailDTO> queryFundsTransactionDetails(String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(String referenceTransactionSn,
                                            FundsTransactionEventType eventType,
                                            String replayRefLegId) {
            return false;
        }

        @Override
        public Money sumConsumedReplayLegAmount(String referenceTransactionSn,
                                                FundsTransactionEventType eventType,
                                                String replayRefLegId,
                                                CurrencyIsoCode currency) {
            return Money.immutable(0L, currency);
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(String transactionSn) {
            return Optional.empty();
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(String freezeOrderSn) {
            return Optional.empty();
        }
    }
}

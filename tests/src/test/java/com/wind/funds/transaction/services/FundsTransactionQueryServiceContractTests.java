package com.wind.funds.transaction.services;

import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.dto.FundsActionFactDTO;
import com.wind.funds.transaction.model.dto.FundsActionFactRef;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.query.FundsActionFactQuery;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FundsTransactionQueryService} 公共契约测试。
 */
class FundsTransactionQueryServiceContractTests {

    /**
     * 场景：公共资金事实查询按稳定流水读取持久化数据。
     * 预期：所有相关查询显式携带 tenantId，旧无租户签名不存在。
     */
    @Test
    void testPersistentFactQueriesShouldRequireTenantScope() {
        Set<String> tenantScopedMethods = Set.of(
                "findFundsTransactionBySn",
                "queryFundsTransactionDetails",
                "hasConsumedReplayLeg",
                "sumConsumedReplayLegAmount",
                "findRouteSnapshotByTransactionSn",
                "findRouteSnapshotByFreezeOrderSn");

        assertThat(FundsTransactionQueryService.class.getDeclaredMethods())
                .filteredOn(method -> tenantScopedMethods.contains(method.getName()))
                .allSatisfy(method -> assertThat(method.getParameterTypes())
                        .as(method.getName())
                        .startsWith(Long.class));
        assertThat(FundsTransactionQueryService.class.getDeclaredMethods())
                .extracting(Method::getName)
                .doesNotContain("queryFundsTransaction");
    }

    /**
     * 默认实现不能静默忽略排除参数，避免未显式支持排除能力的实现误算 replay
     * 剩余额度。
     */
    @Test
    void testDefaultReplayConsumptionQueryRejectsExcludedBusinessEvent() {
        FundsTransactionQueryService queryService = new MinimalFundsTransactionQueryService();

        assertThatThrownBy(() -> queryService.sumConsumedReplayLegAmount(1L, "FT1",
                FundsTransactionEventType.REFUND,
                "LEG1",
                CurrencyIsoCode.USD,
                "SCENE",
                "SN1"))
                .hasMessageContaining("必须由实现类显式支持");
    }

    private static final class MinimalFundsTransactionQueryService implements FundsTransactionQueryService {

        @Override
        public List<FundsActionFactDTO> queryFundsActionFacts(FundsActionFactQuery query) {
            return List.of();
        }

        @Override
        public Optional<FundsActionFactDTO> findFundsActionFact(FundsActionFactRef ref) {
            return Optional.empty();
        }

        @Override
        public Optional<FundsTransactionDTO> findFundsTransactionBySn(Long tenantId, String transactionSn) {
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
        public List<FundsTransactionDetailDTO> queryFundsTransactionDetails(Long tenantId, String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(Long tenantId,
                                            String referenceTransactionSn,
                                            FundsTransactionEventType eventType,
                                            String replayRefLegId) {
            return false;
        }

        @Override
        public Money sumConsumedReplayLegAmount(Long tenantId,
                                                String referenceTransactionSn,
                                                FundsTransactionEventType eventType,
                                                String replayRefLegId,
                                                CurrencyIsoCode currency) {
            return Money.immutable(0L, currency);
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(Long tenantId, String transactionSn) {
            return Optional.empty();
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(Long tenantId, String freezeOrderSn) {
            return Optional.empty();
        }
    }
}

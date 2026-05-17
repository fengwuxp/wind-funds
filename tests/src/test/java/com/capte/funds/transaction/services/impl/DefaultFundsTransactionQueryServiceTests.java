package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DefaultFundsTransactionQueryServiceTests extends DefaultFundsTransactionQueryServiceTestSupport {

    /**
     * 场景：replay、运营查询和后续展示投影都应复用已保存的交易事实。
     * 输入：一条主交易和两条主体视角明细。
     * 输出：主交易 DTO 与按 id 排序的 FundsTransactionDetailDTO 列表。
     * 预期：查询服务只读交易事实，完整返回交易类型、金额、参与方角色和资金效果。
     * 红线：查询服务不得修改交易事实、明细或账本事实。
     */
    @Test
    void testQueryFundsTransactionShouldReuseFactsForReplayAndProjection() {
        FundsTransactionDetail firstDetail = detail("FTD_001", RouteParticipantRole.AUTH_HOLDER);
        FundsTransactionDetail secondDetail = detail("FTD_002", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        secondDetail.setId(403L);
        secondDetail.setSubjectId("platform_revenue_001");
        secondDetail.setSubjectType("FUNDING_ACCOUNT");
        secondDetail.setFundsEffectType(FundsEffectType.DIRECT);
        DefaultFundsTransactionQueryService queryService = queryService(transaction(),
                List.of(firstDetail, secondDetail), null);

        assertThat(queryService.queryFundsTransaction("FT_001")).hasValueSatisfying(result -> {
            assertThat(result.getSn()).isEqualTo("FT_001");
            assertThat(result.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
            assertThat(result.getAmount()).isEqualTo(1_000L);
            assertThat(result.getAuthorizedAmount()).isZero();
        });
        assertThat(queryService.queryFundsTransactionDetails("FT_001"))
                .extracting("sn", "participantRole", "fundsEffectType")
                .containsExactly(
                        tuple("FTD_001", RouteParticipantRole.AUTH_HOLDER, FundsEffectType.HOLD),
                        tuple("FTD_002", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, FundsEffectType.DIRECT)
                );
    }

    /**
     * 场景：手续费退回 replay 已产生用户和平台两个主体视角明细。
     * 输入：两条明细属于同一业务动作、同一原 fee leg，context 记录 replay leg 金额 300。
     * 输出：已消费金额 300。
     * 预期：查询服务按业务动作和原 leg 去重汇总，不因多主体明细把金额双算为 600。
     * 红线：replay 累计上限必须以原 RouteLeg 消费事实为口径，而不是主体明细行数。
     */
    @Test
    void testSumConsumedReplayLegAmountShouldDeduplicateParticipantDetails() {
        FundsTransactionDetail payerDetail = replayDetail("FTD_REFUND_001", RouteParticipantRole.PAYER);
        FundsTransactionDetail feeReceiverDetail = replayDetail("FTD_REFUND_002", RouteParticipantRole.FEE_RECEIVER);
        feeReceiverDetail.setId(403L);
        DefaultFundsTransactionQueryService queryService = queryService(transaction(),
                List.of(payerDetail, feeReceiverDetail), null);

        Money amount = queryService.sumConsumedReplayLegAmount("FT_001", FundsTransactionEventType.FEE_REFUND,
                "FEE", CurrencyIsoCode.USD);

        assertThat(amount).isEqualTo(Money.immutable(300L, CurrencyIsoCode.USD));
        assertThat(queryService.hasConsumedReplayLeg("FT_001", FundsTransactionEventType.FEE_REFUND,
                "FEE")).isTrue();
    }

    private static FundsTransactionDetail replayDetail(String sn, RouteParticipantRole participantRole) {
        FundsTransactionDetail detail = detail(sn, participantRole);
        detail.setBusinessScene("FEE_REFUND");
        detail.setBusinessSn("FEE_REFUND_001");
        detail.setEventType(FundsTransactionEventType.FEE_REFUND);
        detail.setReferenceDetailSn("FT_001");
        detail.setLedgerTransactionSn("LT_FEE_REFUND_001");
        detail.setStatus(FundsTransactionDetailStatus.SUCCEEDED);
        detail.setAmount(300L);
        detail.setContextVariables("{\""
                + FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_IDS
                + "\":[\"FEE\"],\""
                + FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS
                + "\":{\"FEE\":300}}");
        return detail;
    }
}

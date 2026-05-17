package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
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
}

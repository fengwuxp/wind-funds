package com.capte.funds.transaction.contract;

import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.model.FundsTransactionParticipant;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionParticipantContractTests {

    /**
     * 场景：生命周期保存需要从路径参与方沉淀标准交易参与方结构。
     * 输入：反射读取 FundsTransactionParticipant 的公开 getter。
     * 输出：可用于创建主体视角明细的稳定字段集合。
     * 预期：主体、角色、金额、币种、资金效果和扩展上下文字段均存在。
     */
    @Test
    void testFundsTransactionParticipantShouldContainStableFields() {
        Set<String> methods = methods(FundsTransactionParticipant.class);

        assertThat(methods)
                .contains("getSubjectId",
                        "getSubjectType",
                        "getParticipantRole",
                        "getAmount",
                        "getCurrency",
                        "getFundsEffectType",
                        "getReferenceSubjectId",
                        "getContextVariables");
    }

    /**
     * 场景：交易明细 DTO 作为 replay、运营查询和后续展示投影的事实输入。
     * 输入：反射读取 FundsTransactionDetailDTO 的公开 getter。
     * 输出：明细号、主交易号、主体、参与方角色、资金效果、金额和状态字段集合。
     * 预期：稳定参与方字段齐全，后续视图语义不依赖临时 JSON 字段。
     */
    @Test
    void testFundsTransactionDetailDtoShouldContainStableParticipantFields() {
        Set<String> methods = methods(FundsTransactionDetailDTO.class);

        assertThat(methods)
                .contains("getSn",
                        "getTransactionSn",
                        "getSubjectId",
                        "getSubjectType",
                        "getParticipantRole",
                        "getFundsEffectType",
                        "getAmount",
                        "getCurrency",
                        "getStatus");
    }

    /**
     * 场景：同一主交易下的多主体明细需要支撑后续展示语义推导。
     * 输入：构造付款方消费视角的 FundsTransactionParticipant，并转换为 FundsTransactionDetailDTO。
     * 输出：明细中的 participantRole、fundsEffectType 和 amount。
     * 预期：展示语义可由参与方角色和资金效果稳定推导，不依赖交易投影提前落地。
     */
    @Test
    void testParticipantRoleAndFundsEffectShouldBeSufficientForViewSemantics() {
        FundsTransactionParticipant participant = new FundsTransactionParticipant()
                .setSubjectId("funding_001")
                .setSubjectType("FUNDING_ACCOUNT")
                .setParticipantRole(RouteParticipantRole.PAYER)
                .setFundsEffectType(FundsEffectType.CONSUME)
                .setAmount(1_000L)
                .setCurrency(CurrencyIsoCode.USD);
        FundsTransactionDetailDTO detail = new FundsTransactionDetailDTO()
                .setSn("FTD_001")
                .setTransactionSn("FT_001")
                .setSubjectId(participant.getSubjectId())
                .setSubjectType(participant.getSubjectType())
                .setParticipantRole(participant.getParticipantRole())
                .setFundsEffectType(participant.getFundsEffectType())
                .setAmount(participant.getAmount())
                .setCurrency(participant.getCurrency())
                .setStatus(FundsTransactionDetailStatus.PROCESSING);

        assertThat(detail.getParticipantRole()).isEqualTo(RouteParticipantRole.PAYER);
        assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.CONSUME);
        assertThat(detail.getAmount()).isEqualTo(1_000L);
    }

    private static Set<String> methods(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}

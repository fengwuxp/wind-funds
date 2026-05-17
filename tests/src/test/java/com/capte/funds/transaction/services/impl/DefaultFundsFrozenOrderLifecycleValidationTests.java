package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsFrozenOrderLifecycleValidationTests extends DefaultFundsFrozenOrderLifecycleSaverTestSupport {

    /**
     * 场景：解冻事件缺少冻结单引用。
     * 输入：UNFREEZE 指令没有 reference。
     * 输出：beforePosting 拒绝生成解冻记录。
     * 预期：错误信息明确提示“解冻事件必须引用冻结单”。
     * 红线：解冻必须沿原冻结单释放，不得凭业务流水或主体余额直接释放冻结金额。
     */
    @Test
    void testUnfreezeShouldRejectMissingFreezeOrderReference() {
        AtomicReference<FundsFrozenOrder> savedOrder = new AtomicReference<>();
        DefaultFundsFrozenOrderLifecycleSaver saver = new DefaultFundsFrozenOrderLifecycleSaver(
                mapper(savedOrder, queryCount -> null));

        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.UNFREEZE, null,
                "RISK_UNFREEZE", "UNFREEZE_0001", 30L);

        assertThatThrownBy(() -> saver.beforePosting(instruction, resolvedRoute(), routeSnapshot()))
                .hasMessageContaining("解冻事件必须引用冻结单");
    }
}

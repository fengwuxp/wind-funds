package com.capte.funds.transaction.services.impl;

import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.BalanceControlInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.detail;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.lifecycleSaver;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.transaction;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsInstructionLifecycleSaverTests {

    @Test
    void testLifecycleSaverShouldSupportFundsTransactionEventsOnly() {
        DefaultFundsInstructionLifecycleSaver saver = lifecycleSaver(transaction(), detail("FTD_001",
                com.wind.integration.funds.route.enums.RouteParticipantRole.AUTH_HOLDER), new AtomicReference<>());

        assertThat(saver.supports(new SimpleInstruction())).isTrue();
        assertThat(saver.supports(new BalanceControlInstruction(FundsTransactionEventType.FREEZE))).isFalse();
        assertThat(saver.supports(new BalanceControlInstruction(FundsTransactionEventType.UNFREEZE))).isFalse();
    }
}

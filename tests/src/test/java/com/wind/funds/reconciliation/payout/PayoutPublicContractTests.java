package com.wind.funds.reconciliation.payout;

import com.wind.funds.reconciliation.application.payout.PayoutOrderApplicationService;
import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutPublicContractTests {

    @Test
    void testPublicContractShouldExposeStablePayoutLifecycleOnly() {
        assertThat(PayoutOrderApplicationService.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("createOrder", "submitOrder", "handleReceipt", "getOrder");
        assertThat(PayoutOrderState.values())
                .extracting(Enum::name)
                .containsExactly("CREATED", "SUBMITTED", "ACCEPTED", "PROCESSING",
                        "SUCCEEDED", "FAILED", "RETURNED", "MISMATCHED");
        assertThat(SettlementDestination.values())
                .extracting(Enum::name)
                .contains("EXTERNAL_ENDPOINT");
    }
}

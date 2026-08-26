package com.wind.funds.transaction.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Benefit 场景 facade 退役契约测试。
 */
class FundsBenefitContributionTransactionServiceContractTests {

    private static final String DIRECT_TRANSACTION_SERVICE =
            "com.wind.funds.transaction.application.FundsDirectTransactionService";

    private static final List<String> RETIRED_BENEFIT_SURFACE = List.of(
            "com.wind.funds.transaction.application.FundsBenefitContributionTransactionService",
            "com.wind.funds.transaction.model.request.FundsBenefitContributionSettleRequest",
            "com.wind.funds.transaction.model.request.FundsBenefitContributionRefundRequest",
            "com.wind.funds.transaction.enums.FundsBenefitFundingNature",
            "com.wind.funds.transaction.application.impl.FundsBenefitContributionTransactionServiceImpl"
    );

    @Test
    void testLegacyBenefitFacadeShouldBeAbsent() {
        assertThat(isTypePresent(DIRECT_TRANSACTION_SERVICE))
                .as("generic direct transaction service must remain available")
                .isTrue();
        assertThat(RETIRED_BENEFIT_SURFACE.stream()
                .filter(FundsBenefitContributionTransactionServiceContractTests::isTypePresent)
                .toList())
                .as("Benefit facade, requests, enum and implementation must be retired")
                .isEmpty();
    }

    private static boolean isTypePresent(String typeName) {
        try {
            Class.forName(typeName, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}

package com.capte.funds.route;

import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

abstract class TransferFundsInstructionRouteResolverTestSupport {

    protected FundsDirectTransactionInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.transactionInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    protected static void assertLeg(RouteLegSpec leg,
                                    RouteLegType legType,
                                    LedgerSubjectCode sourceLedgerSubjectCode,
                                    LedgerSubjectCode targetLedgerSubjectCode,
                                    LedgerBalanceEffectType balanceEffectType,
                                    LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(sourceLedgerSubjectCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(targetLedgerSubjectCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(balanceEffectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    protected static void assertMustNotBeNegative(RouteLegSpec leg,
                                                  FundsAccountId accountId,
                                                  LedgerSubjectCode ledgerSubjectCode) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }
}

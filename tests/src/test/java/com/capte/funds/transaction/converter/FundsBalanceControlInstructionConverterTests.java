package com.capte.funds.transaction.converter;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsBalanceControlInstructionConverterTests {

    private FundsBalanceControlInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.balanceControlInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testConvertToFreezeInstructionShouldUseBalanceControlInstruction() {
        FundsInstructionSpec instruction = converter.convertToFreezeInstruction(new FundsBalanceFreezeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(FundsRouteTestSupport.amount(400L))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_0001")
                .setDescription("freeze"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FREEZE);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"));
        assertThat(instruction.getReference()).isNull();
    }

    @Test
    void testConvertToFreezeInstructionShouldRejectWrongCurrencyWithoutFx() {
        assertThatThrownBy(() -> converter.convertToFreezeInstruction(new FundsBalanceFreezeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(Money.immutable(400L, CurrencyIsoCode.EUR))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_WRONG_CURRENCY"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("amount currency must equal account currency");
    }

    @Test
    void testConvertToAdjustInstructionShouldUseIncreaseEventAndAccountContext() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.creditAccount("credit_001"), 5_000L, Boolean.TRUE, "LIMIT", "LIMIT_0001")
                .setReconciliationExceptionRef("REC_EX_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.LIMIT_ADJUST);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID, FundsRouteTestSupport.creditAccount("credit_001"))
                .containsEntry(FundsInstructionContextKeys.INCREASE, Boolean.TRUE)
                .containsEntry(FundsInstructionContextKeys.ADJUST_REASON, "adjust reason")
                .containsEntry(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF, "EVIDENCE_LIMIT_0001")
                .containsEntry(FundsInstructionContextKeys.APPROVAL_REF, "APPROVAL_LIMIT_0001")
                .containsEntry(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF, "REC_EX_0001");
    }

    @Test
    void testConvertToFundingAdjustInstructionShouldUseBalanceAdjustEvent() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.fundingAccount("funding_001"), 300L, Boolean.FALSE, "BALANCE",
                "BALANCE_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.BALANCE_ADJUST);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"))
                .containsEntry(FundsInstructionContextKeys.INCREASE, Boolean.FALSE);
    }

    /**
     * 场景：财务或运营发起余额调账但审计信息不完整。
     * 输入：分别缺少调账原因、调账凭证和审批引用。
     * 输出：转换器拒绝生成资金指令。
     * 预期：失败发生在交易编排前，不生成 route 或账本分录。
     * 红线：无审批、无凭证或无原因的资金调账不得成功。
     */
    @Test
    void testConvertToAdjustInstructionShouldRejectMissingAuditContext() {
        assertThatThrownBy(() -> converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.fundingAccount("funding_001"), 300L, Boolean.FALSE, "BALANCE",
                "BALANCE_0002").setAdjustReason(" "), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("余额调账缺少调账原因");
        assertThatThrownBy(() -> converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.fundingAccount("funding_001"), 300L, Boolean.FALSE, "BALANCE",
                "BALANCE_0003").setAdjustEvidenceRef(null), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("余额调账缺少调账凭证");
        assertThatThrownBy(() -> converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.fundingAccount("funding_001"), 300L, Boolean.FALSE, "BALANCE",
                "BALANCE_0004").setApprovalRef(" "), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("余额调账缺少审批引用");
    }

    @Test
    void testConvertToUnfreezeInstructionShouldCarryFreezeReference() {
        FundsInstructionSpec instruction = converter.convertToUnfreezeInstruction(new FundsBalanceUnfreezeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(FundsRouteTestSupport.amount(100L))
                .setReferenceFreezeSn("FREEZE_0001")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("UNFREEZE_0001"), WindOperator.system());

        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FREEZE_ORDER);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, "FREEZE_0001");
    }

    private static FundsBalanceAdjustRequest adjustRequest(FundsAccountId accountId,
                                                           long amount,
                                                           Boolean increase,
                                                           String businessScene,
                                                           String businessSn) {
        return new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(amount))
                .setIncrease(increase)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setAdjustReason("adjust reason")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn);
    }
}

package com.wind.funds.transaction.application.flow;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.application.FundsBenefitContributionTransactionService;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsBenefitContributionRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitContributionSettleRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 让利出资记账交易应用服务流程测试。
 *
 * <p>验证权益让利应用服务不直接写账务事实，而是委派标准直接交易链路生成 route snapshot、
 * 交易事实、posting plan、ledger entry 和余额影响。</p>
 */
class FundsBenefitContributionTransactionServiceFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsBenefitContributionTransactionService benefitContributionTransactionService;

    /**
     * 场景：平台营销资金账户向让利承接账目记录优惠金额，随后发生部分退款和业务取消退款。
     * 输入：让利方充值 100；让利出资结算 30；退款 10；取消退款 5。
     * 输出：三笔让利出资交易均返回资金交易流水，并通过标准 route、交易事实和账本分录入账。
     * 红线：本服务不得绕过直接交易链路写 LedgerEntry；逆向事件必须引用原让利出资记账交易回放。
     */
    @Test
    void testSettleAndRefundShouldPostThroughStandardTransactionLedgerChain() {
        FundsAccountId costBearer = fundingAccount("ben_cost");
        FundsAccountId receiver = fundingAccount("ben_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);

        topup(costBearer, 100L, "BENEFIT_CHAIN_TOPUP");
        var afterTopup = snapshot(balances(costBearer, receiver));

        String settleTransactionSn = benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 30L,
                "BENEFIT_SETTLE_001"), WindOperatorFactory.system());

        assertThat(settleTransactionSn).isNotBlank();
        var afterSettle = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterTopup, afterSettle,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_SETTLE_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_SETTLE_001");
        assertLedgerEventAndBuckets("BENEFIT_SETTLE_001", FundsTransactionEventType.PAY,
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
        assertBenefitContributionDescription(settleTransactionSn, "benefit contribution settle");

        String refundTransactionSn = benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(settleTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(10L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("partial order refund"), WindOperatorFactory.system());

        assertThat(refundTransactionSn).isNotBlank();
        var afterRefund = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterSettle, afterRefund,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 10L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, -10L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_REFUND_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_REFUND_001");
        assertLedgerEventAndBuckets("BENEFIT_REFUND_001", FundsTransactionEventType.REFUND,
                LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertBenefitContributionDescription(refundTransactionSn, "benefit contribution refund");

        String cancelRefundTransactionSn = benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(settleTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("BENEFIT_CANCEL")
                .setBusinessSn("BENEFIT_CANCEL_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("benefit cancellation correction"), WindOperatorFactory.system());

        assertThat(cancelRefundTransactionSn).isNotBlank();
        var afterCancelRefund = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterRefund, afterCancelRefund,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, -5L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_CANCEL_REFUND_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_CANCEL_REFUND_001");
        assertLedgerEventAndBuckets("BENEFIT_CANCEL_REFUND_001", FundsTransactionEventType.REFUND,
                LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);

        assertBucket(balance(costBearer), LedgerSubjectCode.AVAILABLE, 85L, CURRENCY);
        assertBucket(balance(receiver), LedgerSubjectCode.SETTLEMENT, 15L, CURRENCY);
        assertThat(fundsTransaction(settleTransactionSn).getRefundedAmount()).isEqualTo(15L);
    }

    /**
     * 场景：调用方试图把返利、储值或用户权益余额入账伪装成让利结算。
     * 输入：资金性质为 USER_BENEFIT_BALANCE。
     * 输出：服务 fail-fast，不生成资金交易、交易明细、账务交易或分录。
     * 红线：本服务只处理优惠、代金券、支付立减等让利结算，不处理返利、佣金、分润或用户余额入账。
     */
    @Test
    void testSettleWithUserBenefitBalanceShouldFailWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_rebate_cost");
        FundsAccountId receiver = fundingAccount("ben_rebate_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        var before = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_REBATE_001").setFundingNature(FundsBenefitFundingNature.USER_BENEFIT_BALANCE),
                WindOperatorFactory.system()))
                .hasMessageContaining("不支持返利、佣金、分润、储值负债释放或无资金转移解释事实");

        var after = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(before, after,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_REBATE_001");
    }

    /**
     * 场景：调用方未声明让利承接目标账目，或声明了方案外账目。
     * 输入：目标账目为空或 AVAILABLE。
     * 输出：服务 fail-fast，不生成资金或账务事实。
     * 红线：公共契约不得默认猜测 CLEARING/SETTLEMENT，也不得把补贴直接记入可用余额。
     */
    @Test
    void testSettleWithoutSupportedReceiverLedgerSubjectShouldFailWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_subject_cost");
        FundsAccountId receiver = fundingAccount("ben_subject_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.AVAILABLE);
        var before = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(
                settleRequest(costBearer, receiver, 20L, "BENEFIT_MISSING_RECEIVER_SUBJECT_001")
                        .setBenefitReceiverLedgerSubjectCode(null),
                WindOperatorFactory.system()))
                .hasMessageContaining("让利承接目标账目不能为空");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_MISSING_RECEIVER_SUBJECT_001");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(
                settleRequest(costBearer, receiver, 20L, "BENEFIT_UNSUPPORTED_RECEIVER_SUBJECT_001")
                        .setBenefitReceiverLedgerSubjectCode(LedgerSubjectCode.AVAILABLE),
                WindOperatorFactory.system()))
                .hasMessageContaining("仅支持 CLEARING 或 SETTLEMENT");

        assertOnlyBalanceDeltas(before, snapshot(balances(costBearer, receiver)),
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_UNSUPPORTED_RECEIVER_SUBJECT_001");
    }

    /**
     * 场景：商户承担优惠券或支付立减，资金底座记录商户让利的出资责任账目。
     * 输入：资金性质为 MERCHANT_BORNE。
     * 输出：生成一笔从商户让利责任账户到用户让利账目的标准资金交易和账务分录。
     * 红线：商户承担让利不能被误拦截成无账务展示优惠。
     */
    @Test
    void testSettleWithMerchantBorneBenefitShouldPostFundingResponsibility() {
        FundsAccountId merchantCostBearer = fundingAccount("ben_merchant_cost");
        FundsAccountId userBenefit = fundingAccount("ben_user_benefit");
        ensureLedger(merchantCostBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(userBenefit, LedgerSubjectCode.SETTLEMENT);

        topup(merchantCostBearer, 100L, "BENEFIT_MERCHANT_TOPUP");
        var afterTopup = snapshot(balances(merchantCostBearer, userBenefit));

        String transactionSn = benefitContributionTransactionService.settle(settleRequest(merchantCostBearer, userBenefit, 25L,
                "BENEFIT_MERCHANT_BORNE_001").setFundingNature(FundsBenefitFundingNature.MERCHANT_BORNE),
                WindOperatorFactory.system());

        assertThat(transactionSn).isNotBlank();
        var afterSettle = snapshot(balances(merchantCostBearer, userBenefit));
        assertOnlyBalanceDeltas(afterTopup, afterSettle,
                delta(merchantCostBearer, LedgerSubjectCode.AVAILABLE, -25L, CURRENCY),
                delta(userBenefit, LedgerSubjectCode.SETTLEMENT, 25L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_MERCHANT_BORNE_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_MERCHANT_BORNE_001");
        assertLedgerEventAndBuckets("BENEFIT_MERCHANT_BORNE_001", FundsTransactionEventType.PAY,
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
    }

    /**
     * 场景：优惠券模块使用同一让利出资业务流水重复提交，随后同流水变更金额再次提交。
     * 输入：平台出资 30，重复同摘要提交，再把同一业务流水金额改成 31。
     * 输出：同摘要重试返回原交易流水；摘要冲突失败；余额和账务事实保持首次结算后的状态。
     * 红线：优惠券核销重试不能重复入账，同一出资方流水也不能被不同金额、主体或资金性质复用。
     */
    @Test
    void testSettleSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId costBearer = fundingAccount("ben_idempotent_cost");
        FundsAccountId receiver = fundingAccount("ben_idempotent_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(receiver, LedgerSubjectCode.CLEARING);

        topup(costBearer, 100L, "BENEFIT_IDEMPOTENT_TOPUP");
        var afterTopup = snapshot(balances(costBearer, receiver));

        String businessSn = "BENEFIT_IDEMPOTENT_SETTLE_001";
        String transactionSn = benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 30L,
                businessSn), WindOperatorFactory.system());
        var afterSettle = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterTopup, afterSettle,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));
        assertBenefitSettleFacts(businessSn);
        LedgerFactSnapshot afterSettleFacts = ledgerFactSnapshot();

        String retryTransactionSn = benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 30L,
                businessSn), WindOperatorFactory.system());

        assertThat(retryTransactionSn).isEqualTo(transactionSn);
        var afterRetry = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterSettle, afterRetry,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterSettleFacts);

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 31L,
                businessSn), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        var afterConflict = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterRetry, afterConflict,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterSettleFacts);

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(
                settleRequest(costBearer, receiver, 30L, businessSn)
                        .setBenefitReceiverLedgerSubjectCode(LedgerSubjectCode.CLEARING),
                WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        assertOnlyBalanceDeltas(afterConflict, snapshot(balances(costBearer, receiver)),
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.CLEARING, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterSettleFacts);
    }

    /**
     * 场景：平台补足商户、平台直补用户、合作方出资共同出现在让利方案中。
     * 输入：平台向商户结算主体出资 40，平台向用户可记账补贴账户出资 15，
     * 合作方向订单让利归集主体出资 8。
     * 输出：每组成本承担主体和让利承接账务主体分别形成独立资金交易、账本分录和余额影响；
     * 平台补足商户的退款沿原交易从商户 CLEARING 回补平台承担账户。
     * 红线：让利承接方可以是商户、用户或订单归集账目，但必须是可记账主体，
     * 不能退化为营销用户或券来源。
     */
    @Test
    void testSettleScenarioMatrixShouldRecordConcreteContributionPairs() {
        FundsAccountId platformMarketing = fundingAccount("ben_matrix_platform");
        FundsAccountId partnerCostBearer = fundingAccount("ben_matrix_partner");
        FundsAccountId merchantClearing = fundingAccount("ben_matrix_merchant");
        FundsAccountId userSubsidy = fundingAccount("ben_matrix_user");
        FundsAccountId orderBenefitPool = fundingAccount("ben_matrix_order");
        ensureLedger(platformMarketing, LedgerSubjectCode.AVAILABLE);
        ensureLedger(partnerCostBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(merchantClearing, LedgerSubjectCode.CLEARING);
        ensureLedger(userSubsidy, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(orderBenefitPool, LedgerSubjectCode.SETTLEMENT);

        topup(platformMarketing, 100L, "BENEFIT_MATRIX_PLATFORM_TOPUP");
        topup(partnerCostBearer, 100L, "BENEFIT_MATRIX_PARTNER_TOPUP");
        var afterTopup = snapshot(balances(platformMarketing,
                partnerCostBearer,
                merchantClearing,
                userSubsidy,
                orderBenefitPool));

        String platformToMerchantSn = benefitContributionTransactionService.settle(
                settleRequest(platformMarketing, merchantClearing, 40L, "BENEFIT_MATRIX_PLATFORM_MERCHANT_001")
                        .setFundingNature(FundsBenefitFundingNature.PLATFORM_OWN_FUNDS)
                        .setBenefitReceiverLedgerSubjectCode(LedgerSubjectCode.CLEARING),
                WindOperatorFactory.system());
        String platformToUserSn = benefitContributionTransactionService.settle(
                settleRequest(platformMarketing, userSubsidy, 15L, "BENEFIT_MATRIX_PLATFORM_USER_001")
                        .setFundingNature(FundsBenefitFundingNature.PLATFORM_OWN_FUNDS),
                WindOperatorFactory.system());
        String partnerToOrderSn = benefitContributionTransactionService.settle(
                settleRequest(partnerCostBearer, orderBenefitPool, 8L, "BENEFIT_MATRIX_PARTNER_ORDER_001")
                        .setFundingNature(FundsBenefitFundingNature.PARTNER_FUNDED),
                WindOperatorFactory.system());

        assertThat(platformToMerchantSn).isNotBlank();
        assertThat(platformToUserSn).isNotBlank();
        assertThat(partnerToOrderSn).isNotBlank();
        var afterSettle = snapshot(balances(platformMarketing,
                partnerCostBearer,
                merchantClearing,
                userSubsidy,
                orderBenefitPool));
        assertOnlyBalanceDeltas(afterTopup, afterSettle,
                delta(platformMarketing, LedgerSubjectCode.AVAILABLE, -55L, CURRENCY),
                delta(partnerCostBearer, LedgerSubjectCode.AVAILABLE, -8L, CURRENCY),
                delta(merchantClearing, LedgerSubjectCode.CLEARING, 40L, CURRENCY),
                delta(userSubsidy, LedgerSubjectCode.SETTLEMENT, 15L, CURRENCY),
                delta(orderBenefitPool, LedgerSubjectCode.SETTLEMENT, 8L, CURRENCY));
        assertBenefitSettleFacts("BENEFIT_MATRIX_PLATFORM_MERCHANT_001", LedgerSubjectCode.CLEARING);
        assertBenefitSettleFacts("BENEFIT_MATRIX_PLATFORM_USER_001");
        assertBenefitSettleFacts("BENEFIT_MATRIX_PARTNER_ORDER_001");

        benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(platformToMerchantSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(10L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_MATRIX_PLATFORM_MERCHANT_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("partial platform subsidy refund"), WindOperatorFactory.system());

        var afterClearingRefund = snapshot(balances(platformMarketing,
                partnerCostBearer,
                merchantClearing,
                userSubsidy,
                orderBenefitPool));
        assertOnlyBalanceDeltas(afterSettle, afterClearingRefund,
                delta(platformMarketing, LedgerSubjectCode.AVAILABLE, 10L, CURRENCY),
                delta(merchantClearing, LedgerSubjectCode.CLEARING, -10L, CURRENCY));
        assertLedgerEventAndBuckets("BENEFIT_MATRIX_PLATFORM_MERCHANT_REFUND_001",
                FundsTransactionEventType.REFUND, LedgerSubjectCode.CLEARING, LedgerSubjectCode.AVAILABLE);
        assertThat(fundsTransaction(platformToMerchantSn).getRefundedAmount()).isEqualTo(10L);
    }

    /**
     * 场景：平台和商户共同承担同一订单优惠，后续分别按原出资事实退款。
     * 输入：平台出资 20，商户出资 10，退款时分别冲回 5 和 3。
     * 输出：每个出资方都有独立结算交易流水，退款按各自原让利出资记账交易回放。
     * 红线：多方出资不能合并成一笔丢失出资方的资金事实，也不能退款时按当前规则重算分摊。
     */
    @Test
    void testMultipleContributorsShouldSettleAndRefundByOriginalBenefitTransaction() {
        FundsAccountId platformCostBearer = fundingAccount("ben_multi_platform");
        FundsAccountId merchantCostBearer = fundingAccount("ben_multi_merchant");
        FundsAccountId receiver = fundingAccount("ben_multi_receiver");
        ensureLedger(platformCostBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(merchantCostBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);

        topup(platformCostBearer, 100L, "BENEFIT_MULTI_PLATFORM_TOPUP");
        topup(merchantCostBearer, 100L, "BENEFIT_MULTI_MERCHANT_TOPUP");
        var afterTopup = snapshot(balances(platformCostBearer, merchantCostBearer, receiver));

        String platformTransactionSn = benefitContributionTransactionService.settle(
                settleRequest(platformCostBearer, receiver, 20L, "BENEFIT_MULTI_PLATFORM_SETTLE_001")
                        .setFundingNature(FundsBenefitFundingNature.PLATFORM_OWN_FUNDS),
                WindOperatorFactory.system());
        String merchantTransactionSn = benefitContributionTransactionService.settle(
                settleRequest(merchantCostBearer, receiver, 10L, "BENEFIT_MULTI_MERCHANT_SETTLE_001")
                        .setFundingNature(FundsBenefitFundingNature.MERCHANT_BORNE),
                WindOperatorFactory.system());

        var afterSettle = snapshot(balances(platformCostBearer, merchantCostBearer, receiver));
        assertOnlyBalanceDeltas(afterTopup, afterSettle,
                delta(platformCostBearer, LedgerSubjectCode.AVAILABLE, -20L, CURRENCY),
                delta(merchantCostBearer, LedgerSubjectCode.AVAILABLE, -10L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_MULTI_PLATFORM_SETTLE_001", 2, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_MULTI_MERCHANT_SETTLE_001", 2, 2);

        benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(platformTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_MULTI_PLATFORM_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("partial order refund"), WindOperatorFactory.system());
        benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(merchantTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(3L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_MULTI_MERCHANT_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("partial order refund"), WindOperatorFactory.system());

        var afterRefund = snapshot(balances(platformCostBearer, merchantCostBearer, receiver));
        assertOnlyBalanceDeltas(afterSettle, afterRefund,
                delta(platformCostBearer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(merchantCostBearer, LedgerSubjectCode.AVAILABLE, 3L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, -8L, CURRENCY));
        assertThat(fundsTransaction(platformTransactionSn).getRefundedAmount()).isEqualTo(5L);
        assertThat(fundsTransaction(merchantTransactionSn).getRefundedAmount()).isEqualTo(3L);
    }

    /**
     * 场景：退款引用不存在的原让利出资交易。
     * 输入：referenceBenefitTransactionSn 指向不存在的交易流水。
     * 输出：服务 fail-fast，不补造历史出资事实，也不生成退款资金事实。
     * 红线：让利出资退款必须按原交易 RouteSnapshot 回放，缺原事实时不得重新路由或自行分摊。
     */
    @Test
    void testRefundWithMissingOriginalBenefitTransactionShouldFailWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_missing_cost");
        FundsAccountId receiver = fundingAccount("ben_missing_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        var before = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn("BENEFIT_TXN_MISSING_ORIGINAL_001")
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_MISSING_ORIGINAL_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("missing original benefit contribution"), WindOperatorFactory.system()))
                .hasMessageContaining("退款原交易不存在")
                .hasMessageContaining("BENEFIT_TXN_MISSING_ORIGINAL_001");

        var after = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(before, after,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_MISSING_ORIGINAL_REFUND_001");
    }

    /**
     * 场景：调用方试图把展示优惠、商户折扣或平台券不补足商户等解释事实伪装成入账结算。
     * 输入：资金性质为 NO_FUNDS_TRANSFER。
     * 输出：服务 fail-fast，不生成资金交易、交易明细、账务交易或分录。
     * 红线：无资金转移的优惠解释事实不能进入会真实入账的权益让利结算入口。
     */
    @Test
    void testSettleWithNoFundsTransferNatureShouldFailWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_no_transfer_cost");
        FundsAccountId receiver = fundingAccount("ben_no_transfer_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        var before = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_NO_TRANSFER_001").setFundingNature(FundsBenefitFundingNature.NO_FUNDS_TRANSFER),
                WindOperatorFactory.system()))
                .hasMessageContaining("无资金转移解释事实");

        var after = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(before, after,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_NO_TRANSFER_001");
    }

    /**
     * 场景：调用方试图把让利承接目标账目藏入嵌套上下文、列表或点号别名。
     * 输入：contextVariables 包含嵌套或列表中的 benefitReceiverLedgerSubjectCode，或点号分隔别名。
     * 输出：服务 fail-fast，不生成资金交易、交易明细、账务交易或分录。
     * 红线：contextVariables 只能放轻量关联信息，不能成为核心金额、分摊或规则事实旁路。
     */
    @Test
    void testSettleContextShouldRejectCoreBenefitFactsWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_context_cost");
        FundsAccountId receiver = fundingAccount("ben_context_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        var before = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_NESTED_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "payload", Map.of("benefitReceiverLedgerSubjectCode", "CLEARING")))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("benefitReceiverLedgerSubjectCode");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_DOTTED_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "payload.benefitReceiverLedgerSubjectCode", "CLEARING"))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("payload.benefitReceiverLedgerSubjectCode");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_SPLIT_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "benefitReceiver.LedgerSubjectCode", "CLEARING"))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("benefitReceiver.LedgerSubjectCode");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_LIST_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "payloads", List.of(Map.of("benefitReceiverLedgerSubjectCode", "CLEARING"))))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("benefitReceiverLedgerSubjectCode");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_SLASH_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "payload/benefitReceiverLedgerSubjectCode", "CLEARING"))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("payload/benefitReceiverLedgerSubjectCode");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_BRACKET_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "payload[benefitReceiverLedgerSubjectCode]", "CLEARING"))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("payload[benefitReceiverLedgerSubjectCode]");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_ARRAY_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "payloads", new Object[]{Map.of("benefitReceiverLedgerSubjectCode", "CLEARING")}))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("benefitReceiverLedgerSubjectCode");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_NESTED_SPLIT_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "rule", Map.of("id", "RULE_001")))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("id");

        assertThatThrownBy(() -> benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_CONTEXT_MULTI_LEVEL_SPLIT_CORE_FIELD_001")
                .setContextVariables(ReadonlyContextVariables.of(Map.of(
                        "benefit", Map.of("receiver", Map.of("ledger", Map.of(
                                "subject", Map.of("code", "CLEARING"))))))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("code");

        var after = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(before, after,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_NESTED_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_DOTTED_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_SPLIT_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_LIST_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_SLASH_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_BRACKET_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_ARRAY_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_NESTED_SPLIT_CORE_FIELD_001");
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_CONTEXT_MULTI_LEVEL_SPLIT_CORE_FIELD_001");
    }

    /**
     * 场景：调用方试图在退款上下文中携带当前规则来源。
     * 输入：contextVariables 包含 rule_id。
     * 输出：服务 fail-fast，不生成退款资金交易、交易明细、账务交易或分录。
     * 红线：退款只能引用原让利出资交易，不能通过上下文按当前营销规则重算历史权益。
     */
    @Test
    void testRefundContextShouldRejectCoreBenefitFactsWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_refund_context_cost");
        FundsAccountId receiver = fundingAccount("ben_refund_context_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        topup(costBearer, 100L, "BENEFIT_REFUND_CONTEXT_TOPUP");
        String settleTransactionSn = benefitContributionTransactionService.settle(settleRequest(costBearer, receiver, 20L,
                "BENEFIT_REFUND_CONTEXT_SETTLE_001"), WindOperatorFactory.system());
        var beforeRefund = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitContributionTransactionService.refund(new FundsBenefitContributionRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(settleTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_REFUND_CONTEXT_CORE_FIELD_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("partial order refund")
                .setContextVariables(ReadonlyContextVariables.of(Map.of("rule_id", "RULE_001"))),
                WindOperatorFactory.system()))
                .hasMessageContaining("扩展上下文不得承载核心金额、分摊或规则事实")
                .hasMessageContaining("rule_id");

        var afterRefund = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_REFUND_CONTEXT_CORE_FIELD_001");
    }

    private FundsBenefitContributionSettleRequest settleRequest(FundsAccountId costBearer,
                                                           FundsAccountId receiver,
                                                           long amount,
                                                           String businessSn) {
        return new FundsBenefitContributionSettleRequest()
                .setTenantId(TENANT_ID)
                .setBusinessScene("BENEFIT_SETTLE")
                .setBusinessSn(businessSn)
                .setOriginalOrderSn("ORDER_001")
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setCostBearerSubjectRef(subjectRef(costBearer))
                .setBenefitReceiverSubjectRef(subjectRef(receiver))
                .setBenefitReceiverLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setFundingNature(FundsBenefitFundingNature.PLATFORM_OWN_FUNDS);
    }

    private SubjectRef subjectRef(FundsAccountId accountId) {
        return ImmutableSubjectRef.builder()
                .tenantId(TENANT_ID)
                .subjectId(accountId.id())
                .subjectType(FundsSubjectType.valueOf(accountId.type()))
                .currency(CURRENCY.name())
                .build();
    }

    private void assertLedgerEventAndBuckets(String businessSn,
                                             FundsTransactionEventType eventType,
                                             LedgerSubjectCode sourceBucket,
                                             LedgerSubjectCode targetBucket) {
        LedgerTransaction transaction = ledgerTransactionByBusinessSn(businessSn);
        assertThat(transaction.getEventType()).isEqualTo(eventType.name());
        assertThat(entriesOf(transaction))
                .extracting(LedgerEntry::getLedgerSubjectCode)
                .containsExactlyInAnyOrder(sourceBucket, targetBucket);
    }

    private void assertBenefitSettleFacts(String businessSn) {
        assertBenefitSettleFacts(businessSn, LedgerSubjectCode.SETTLEMENT);
    }

    private void assertBenefitSettleFacts(String businessSn, LedgerSubjectCode receiverLedgerSubjectCode) {
        assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, 2, 2);
        assertLedgerFactsFollowRouteSnapshot(businessSn);
        assertLedgerEventAndBuckets(businessSn, FundsTransactionEventType.PAY,
                LedgerSubjectCode.AVAILABLE, receiverLedgerSubjectCode);
    }

    private void assertBenefitContributionDescription(String transactionSn, String expectedDescription) {
        var transaction = fundsTransaction(transactionSn);
        assertThat(transaction.getDescription()).isEqualTo(expectedDescription);
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transactionSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getDescription())
                        .isEqualTo(expectedDescription));
    }
}

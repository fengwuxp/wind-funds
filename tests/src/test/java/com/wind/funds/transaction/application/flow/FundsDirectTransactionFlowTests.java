package com.wind.funds.transaction.application.flow;

import com.wind.integration.operator.OperationActorType;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.core.WritableContextVariables;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerStatus;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.request.UpdateLedgerStatusRequest;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FeeSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertSubjectBalanceNotInitialized;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 直接交易业务流测试。
 */
class FundsDirectTransactionFlowTests extends FundsTransactionFlowTestSupport {

    private static final String CORE1B_BUSINESS_SN = "DIRECT_CORE1B_LEGACY_TOPUP";

    private static final Map<String, String> CORE1B_CANONICAL_DETAIL_DIGESTS = Map.of(
            "funding_user", "5ba7816caaf9364a8f41656cf3676c406cb359b195018c39f217a9bd8214f53a",
            "platform_cash_mapping", "aa08a32f91c032b3b906ad8cfcba93796f72b0ef3bdf948ff0c89ee8b6199e5e",
            "platform_prepayment", "b241e319e526e583fa68a85768337b9115dbb1a706633f31903d56a15e4da957");

    private static final Map<String, String> CORE1B_LEGACY_DETAIL_DIGESTS = Map.of(
            "funding_user", "868c7b3858bb95b4ce98345f737b547e2b9165272d58553f43d7d9386085fc91",
            "platform_cash_mapping", "d24d56e8f0eb2a3dfdfee0d76e481c3e574651bb4ad1851573b566c68998291f",
            "platform_prepayment", "ce86ddc28cd3e34e818f375cda7fcbfcede955187b7f1620c85d092f8af24f84");

    private static final Set<String> DIRECT_LEDGER_CONTEXT_KEYS = Set.of(
            "routeLegId", "replayRefLegId", "replayPolicy");

    private static final Set<String> DIRECT_REQUEST_CONTEXT_KEYS = Set.of(
            "channelCode",
            "externalTransactionId",
            "feeChargeSpec");

    @Autowired
    private JdbcTemplate core1bJdbcTemplate;

    /**
     * 场景：历史直接充值的三条参与方明细已保存 legacy 摘要，当前版本收到相同请求和金额冲突请求。
     * 输入：先由当前 writer 写入 canonical v1，再把 route 和 request_hash 替换为基线旧 writer 的固定事实。
     * 输出：同请求复用原资金交易，冲突金额被拒绝，route、资金/账务事实和逐桶余额均保持不变。
     * 红线：兼容读取不得重写历史摘要、重复入账或让冲突请求产生任何资金副作用。
     */
    @Test
    void testPersistedLegacyDetailDigestsShouldReplayAndRejectConflictWithoutChangingFacts() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTransactionSn = directTransactionService.topup(
                core1bTopupRequest(40L), WindOperatorFactory.system());

        BalanceSnapshot afterFirst = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirst,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(detailRequestHashes(CORE1B_BUSINESS_SN))
                .isEqualTo(CORE1B_CANONICAL_DETAIL_DIGESTS)
                .isNotEqualTo(CORE1B_LEGACY_DETAIL_DIGESTS);
        assertSingleFundsAndLedgerFactsForBusinessSn(CORE1B_BUSINESS_SN, 3, 2, 4);
        replaceRouteSnapshotWithLegacyAccountingFields();
        replaceDetailDigest("funding_user", RouteParticipantRole.PAYEE);
        replaceDetailDigest("platform_cash_mapping", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        replaceDetailDigest("platform_prepayment", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        assertThat(detailRequestHashes(CORE1B_BUSINESS_SN)).isEqualTo(CORE1B_LEGACY_DETAIL_DIGESTS);
        PersistedTopupFacts persistedLegacyFacts = persistedTopupFacts();
        LedgerFactSnapshot persistedLegacyLedgerFacts = ledgerFactSnapshot();
        RouteSnapshotSpec persistedLegacyRoute = routeSnapshot(CORE1B_BUSINESS_SN);

        String replayTransactionSn = directTransactionService.topup(
                core1bTopupRequest(40L), WindOperatorFactory.system());

        assertThat(replayTransactionSn).isEqualTo(firstTransactionSn);
        assertThat(detailRequestHashes(CORE1B_BUSINESS_SN)).isEqualTo(CORE1B_LEGACY_DETAIL_DIGESTS);
        assertThat(persistedTopupFacts()).isEqualTo(persistedLegacyFacts);
        assertDirectRouteSnapshotUnchanged(CORE1B_BUSINESS_SN, persistedLegacyRoute);
        assertLedgerFactsUnchanged(core1bJdbcTemplate, persistedLegacyLedgerFacts);
        assertThat(snapshot(balances(account, cashMappingAccount(), prepaymentAccount()))).isEqualTo(afterFirst);

        assertThatThrownBy(() -> directTransactionService.topup(
                core1bTopupRequest(41L), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        assertThat(detailRequestHashes(CORE1B_BUSINESS_SN)).isEqualTo(CORE1B_LEGACY_DETAIL_DIGESTS);
        assertThat(persistedTopupFacts()).isEqualTo(persistedLegacyFacts);
        assertDirectRouteSnapshotUnchanged(CORE1B_BUSINESS_SN, persistedLegacyRoute);
        assertLedgerFactsUnchanged(core1bJdbcTemplate, persistedLegacyLedgerFacts);
        assertThat(snapshot(balances(account, cashMappingAccount(), prepaymentAccount()))).isEqualTo(afterFirst);
        assertSingleFundsAndLedgerFactsForBusinessSn(CORE1B_BUSINESS_SN, 3, 2, 4);
    }

    /**
     * 场景：升级前明细保存了 legacy 摘要，但其中一条仍处于处理中。
     * 预期：同请求不能使用历史 Route 兼容回退继续记账。
     * 红线：只有已完成历史事实才允许跨版本摘要兼容。
     */
    @Test
    void testPersistedLegacyDetailDigestShouldRejectIncompleteDetail() {
        FundsAccountId account = fundingAccount("funding_user");
        directTransactionService.topup(core1bTopupRequest(40L), WindOperatorFactory.system());
        replaceRouteSnapshotWithLegacyAccountingFields();
        replaceDetailDigest("funding_user", RouteParticipantRole.PAYEE);
        replaceDetailDigest("platform_cash_mapping", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        replaceDetailDigest("platform_prepayment", RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT);
        assertThat(core1bJdbcTemplate.update("""
                        UPDATE t_funds_transaction_detail
                        SET status = 'PROCESSING'
                        WHERE tenant_id = ? AND business_sn = ? AND subject_id = ?
                        """, TENANT_ID, CORE1B_BUSINESS_SN, "funding_user")).isEqualTo(1);
        BalanceSnapshot balancesBeforeReplay = snapshot(balances(
                account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot ledgerBeforeReplay = ledgerFactSnapshot();
        PersistedTopupFacts fundsFactsBeforeReplay = persistedTopupFacts();

        assertThatThrownBy(() -> directTransactionService.topup(
                core1bTopupRequest(40L), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        assertThat(snapshot(balances(account, cashMappingAccount(), prepaymentAccount())))
                .isEqualTo(balancesBeforeReplay);
        assertThat(persistedTopupFacts()).isEqualTo(fundsFactsBeforeReplay);
        assertLedgerFactsUnchanged(core1bJdbcTemplate, ledgerBeforeReplay);
    }

    @Test
    void testConvertedTopupShouldPropagateOriginalAmountAndExchangeRateToLedgerFacts() {
        FundsAccountId account = fundingAccount("funding_user");
        String businessSn = "DIRECT_CONVERTED_TOPUP";
        Money amount = Money.immutable(325L, CurrencyIsoCode.USD);
        Money originalAmount = Money.immutable(1_000L, CurrencyIsoCode.KWD);
        BigDecimal exchangeRate = new BigDecimal("3.25");

        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_converted_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_CONVERTED_TOPUP_CHANNEL")
                .setTransactionAmount(TransactionAmount.converted(amount, originalAmount, exchangeRate))
                .setBusinessScene("TOPUP")
                .setBusinessSn(businessSn)
                .setDescription("converted topup"), WindOperatorFactory.create(
                        "tenant-api-client-001",
                        "Tenant API client",
                        "wind-funds-tests",
                        OperationActorType.TENANT_API_CLIENT));

        assertThat(routeSnapshot(businessSn).getLegs()).isNotEmpty().allSatisfy(leg -> {
            assertThat(leg.getAmount()).isEqualTo(amount);
            assertThat(leg.getOriginalAmount()).isEqualTo(originalAmount);
            assertThat(leg.getExchangeRate()).isEqualByComparingTo(exchangeRate);
        });
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        assertThat(ledgerTransaction.getAmount()).isEqualTo(amount.getAmount());
        assertThat(ledgerTransaction.getCurrency()).isEqualTo(amount.getCurrency());
        assertThat(ledgerTransaction.getOriginalAmount()).isEqualTo(originalAmount.getAmount());
        assertThat(ledgerTransaction.getOriginalCurrency()).isEqualTo(originalAmount.getCurrency());
        assertThat(ledgerTransaction.getExchangeRate()).isEqualByComparingTo(exchangeRate);
        assertThat(entriesOf(ledgerTransaction)).isNotEmpty().allSatisfy(entry -> {
            assertThat(entry.getAmount()).isEqualTo(amount.getAmount());
            assertThat(entry.getCurrency()).isEqualTo(amount.getCurrency());
            assertThat(entry.getOriginalAmount()).isEqualTo(originalAmount.getAmount());
            assertThat(entry.getOriginalCurrency()).isEqualTo(originalAmount.getCurrency());
            assertThat(entry.getExchangeRate()).isEqualByComparingTo(exchangeRate);
        });
        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, amount.getAmount(), amount.getCurrency());
    }

    /**
     * 场景：外部入金确认后，业务要求在同一笔充值中收取手续费。
     * 输入：充值 100，并携带固定手续费 5。
     * 输出：充值账户 AVAILABLE 净增加 95，平台 CASH 减少 100，平台 FEE 增加 5。
     * 预期：手续费从充值到账 FundingAccount 的 AVAILABLE 扣取，并与充值本金原子入账。
     * 红线：不得从外部账户、平台 PREPAYMENT 或其他未受益账户扣取充值手续费。
     */
    @Test
    void testTopupWithFeeChargeSpecShouldChargeCreditedFundingAccountAvailable() {
        FundsAccountId account = fundingAccount("topup_fee_user");
        ensureLedger(account, LedgerSubjectCode.AVAILABLE);
        BalanceSnapshot before = snapshot(balances(account, feeAccount(), cashMappingAccount(), prepaymentAccount()));

        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_topup_fee_source",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_FEE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(100L, CURRENCY)))
                .setFeeChargeSpec(FeeSpec.builder()
                        .feeType("TOPUP_PROCESSING_FEE")
                        .fixedFee(5)
                        .build())
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_FEE")
                .setDescription("topup with processing fee"), WindOperatorFactory.system());

        BalanceSnapshot after = snapshot(balances(account, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(account, LedgerSubjectCode.AVAILABLE, 95L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(postingPlansOf(ledgerTransactionByBusinessSn("DIRECT_TOPUP_FEE")).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactlyInAnyOrder(
                        LedgerPhaseCode.FUND_IN.name(),
                        LedgerPhaseCode.SETTLEMENT.name(),
                        LedgerPhaseCode.FEE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TOPUP_FEE", 4, 6);
    }

    /**
     * 场景：充值本金足以入账，但到账账户不足以承担本次新增手续费。
     * 输入：充值 100，同时收取固定手续费 105。
     * 输出：充值本金腿和费用腿整体失败，到账账户、平台 CASH/PREPAYMENT/FEE 均不变化。
     * 预期：主交易与费用腿共享本地事务，不能先完成充值再留下手续费欠款。
     * 红线：费用失败不得形成部分成功、负余额或孤立账务事实。
     */
    @Test
    void testTopupWithFeeChargeSpecExceedingCreditedAmountShouldRollbackAllLedgerEffects() {
        FundsAccountId account = fundingAccount("topup_fee_insufficient");
        ensureLedger(account, LedgerSubjectCode.AVAILABLE);
        BalanceSnapshot before = snapshot(balances(account, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_topup_fee_insufficient",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_FEE_INSUFFICIENT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(100L, CURRENCY)))
                .setFeeChargeSpec(FeeSpec.builder()
                        .feeType("TOPUP_PROCESSING_FEE")
                        .fixedFee(105)
                        .build())
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_FEE_INSUFFICIENT")
                .setDescription("topup with excessive processing fee"), WindOperatorFactory.system()))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot after = snapshot(balances(account, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_TOPUP_FEE_INSUFFICIENT");
    }

    /**
     * 场景：调用方试图通过自由上下文注入随交易手续费规则。
     * 输入：feeChargeSpec 一等字段为空，但 contextVariables 含同名保留键。
     * 输出：请求在指令转换前被拒绝，账户和账务事实均不变化。
     * 预期：手续费只能由显式请求字段触发，扩展上下文不能改变资金行为。
     * 红线：不得允许同名上下文绕过公共契约并产生隐式扣费。
     */
    @Test
    void testTopupWithFeeChargeSpecInContextVariablesShouldRejectWithoutSideEffects() {
        FundsAccountId account = fundingAccount("topup_fee_context");
        ensureLedger(account, LedgerSubjectCode.AVAILABLE);
        BalanceSnapshot before = snapshot(balances(account, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_topup_fee_context",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_FEE_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(100L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("feeChargeSpec", FeeSpec.builder()
                        .feeType("TOPUP_PROCESSING_FEE")
                        .fixedFee(5)
                        .build())))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_FEE_CONTEXT")
                .setDescription("topup with fee rule in context"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain reserved funds transaction fields");

        BalanceSnapshot after = snapshot(balances(account, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_FEE_CONTEXT");
    }

    /**
     * 场景：内部账户转账时，业务要求在同一笔转账中向付款方收取手续费。
     * 输入：付款方充值 100，向收款方转账 30，并携带固定手续费 5。
     * 输出：付款方 AVAILABLE 减少 35，收款方 AVAILABLE 增加 30，平台 FEE 增加 5。
     * 预期：手续费从 payerAccountId 对应 FundingAccount 的 AVAILABLE 扣取，并与转账本金原子入账。
     * 红线：不得从收款方、CreditAccount 额度或其他未明确资金责任账户扣费。
     */
    @Test
    void testTransferWithFeeChargeSpecShouldChargePayerFundingAccountAvailable() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("transfer_fee_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "DIRECT_TRANSFER_FEE_TOPUP");
        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount()));

        directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setFeeChargeSpec(FeeSpec.builder()
                        .feeType("TRANSFER_PROCESSING_FEE")
                        .fixedFee(5)
                        .build())
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_FEE")
                .setDescription("transfer with processing fee"), WindOperatorFactory.system());

        BalanceSnapshot after = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, -35L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY));
        assertThat(postingPlansOf(ledgerTransactionByBusinessSn("DIRECT_TRANSFER_FEE")).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerPhaseCode.TRANSFER.name(), LedgerPhaseCode.FEE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_FEE", 3, 4);
    }

    @Test
    void testPartialFxRefundShouldPropagateCurrentAmountFactsToLedger() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fx_refund_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 325L, "FX_PARTIAL_REFUND_TOPUP");
        String payTransactionSn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.converted(
                        Money.immutable(325L, CurrencyIsoCode.USD),
                        Money.immutable(1_000L, CurrencyIsoCode.KWD),
                        new BigDecimal("3.25")))
                .setBusinessScene("PAY")
                .setBusinessSn("FX_PARTIAL_REFUND_PAY"), WindOperatorFactory.system());

        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.converted(
                        Money.immutable(100L, CurrencyIsoCode.USD),
                        Money.immutable(308L, CurrencyIsoCode.KWD),
                        new BigDecimal("3.25")))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("FX_PARTIAL_REFUND"), WindOperatorFactory.system());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("FX_PARTIAL_REFUND");
        assertThat(refundTransaction.getAmount()).isEqualTo(100L);
        assertThat(refundTransaction.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(refundTransaction.getOriginalAmount()).isEqualTo(308L);
        assertThat(refundTransaction.getOriginalCurrency()).isEqualTo(CurrencyIsoCode.KWD);
        assertThat(refundTransaction.getExchangeRate()).isEqualByComparingTo("3.25");
        assertThat(entriesOf(refundTransaction)).allSatisfy(entry -> {
            assertThat(entry.getAmount()).isEqualTo(100L);
            assertThat(entry.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
            assertThat(entry.getOriginalAmount()).isEqualTo(308L);
            assertThat(entry.getOriginalCurrency()).isEqualTo(CurrencyIsoCode.KWD);
            assertThat(entry.getExchangeRate()).isEqualByComparingTo("3.25");
        });
        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 100L, CurrencyIsoCode.USD);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 225L, CurrencyIsoCode.USD);
    }

    @Test
    void testPartialFxRefundWithChangedRateShouldRejectWithoutFundsSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fx_refund_rate_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 325L, "FX_REFUND_RATE_TOPUP");
        String payTransactionSn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.converted(
                        Money.immutable(325L, CurrencyIsoCode.USD),
                        Money.immutable(1_000L, CurrencyIsoCode.KWD),
                        new BigDecimal("3.25")))
                .setBusinessScene("PAY")
                .setBusinessSn("FX_REFUND_RATE_PAY"), WindOperatorFactory.system());
        BalanceSnapshot beforeFailure = snapshot(balances(payer, payee));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.converted(
                        Money.immutable(100L, CurrencyIsoCode.USD),
                        Money.immutable(303L, CurrencyIsoCode.KWD),
                        new BigDecimal("3.30")))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("FX_REFUND_RATE_CHANGED"), WindOperatorFactory.system()))
                .hasMessageContaining("退款汇率必须与原支付快照汇率一致");

        BalanceSnapshot afterFailure = snapshot(balances(payer, payee));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CurrencyIsoCode.USD),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CurrencyIsoCode.USD));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("FX_REFUND_RATE_CHANGED");
    }

    /**
     * 场景：用户充值后向普通收款方付款，随后收款方发起部分退款。
     * 输入：充值 100、付款 70、部分退款 30。
     * 输出：付款方 AVAILABLE、收款方 SETTLEMENT、平台 CASH/PREPAYMENT 余额快照。
     * 预期：充值、付款、退款均生成可追溯账务事实，付款进入请求指定收款桶，部分退款只回补退款金额。
     * 红线：普通支付不得默认套用商户清算路径；普通退款不得影响平台现金和预收款口径。
     */
    @Test
    void testTopupPayThenPartialRefundShouldPostLedgerFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("ordinary_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));

        topup(payer, 100L, "DIRECT_PAY_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_PAY_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 30L, "DIRECT_PAY_REFUND_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.REFUND.name());

        LedgerTransaction payTransaction = ledgerTransactionByBusinessSn("DIRECT_PAY_REFUND_PAY");
        assertThat(entriesOf(payTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
        assertThat(postingPlansOf(payTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("DIRECT_PAY_REFUND_REFUND");
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());

        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_REFUND_PAY", 2, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_REFUND_REFUND", 2, 2);
    }

    /**
     * 场景：直接退款出资方余额不足。
     * 输入：付款方充值 100、向收款方付款 70，随后收款方尝试退款 80。
     * 输出：退款失败；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：退款出资方余额不足时记录 FAILED 资金交易事实，不生成账务事实。
     * 红线：退款失败不能留下 posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testRefundWithInsufficientPayerBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_low_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_INSUFFICIENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_INSUFFICIENT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 80L,
                "DIRECT_REFUND_INSUFFICIENT_REFUND"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_INSUFFICIENT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_INSUFFICIENT_PAY", 2, 2);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_REFUND_INSUFFICIENT_REFUND");
    }

    /**
     * 场景：业务关闭收款账户后，收款账本挂起但仍有待收口余额。
     * 输入：付款方充值 100、付款 70，收款方 SETTLEMENT 账本挂起后尝试普通收款和退款 30。
     * 输出：普通收款被拒绝，退款作为 closing posting 继续入账。
     * 红线：SUSPENDED 账本不能承接新交易，但必须允许退款/撤销/清算这类收口事实消减余额。
     */
    @Test
    void testSuspendedPayeeLedgerShouldRejectNormalPayAndAllowRefundClosingPosting() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("closing_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        topup(payer, 100L, "DIRECT_CLOSING_TOPUP");
        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_CLOSING_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        Long payeeSettlementLedgerId = findLedger(payee, LedgerSubjectCode.SETTLEMENT)
                .orElseThrow()
                .getId();
        ledgerService.updateLedgerStatus(new UpdateLedgerStatusRequest()
                .setId(payeeSettlementLedgerId)
                .setStatus(LedgerStatus.SUSPENDED));
        LedgerFactSnapshot afterSuspended = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_CLOSING_REJECTED_PAY"))
                .hasMessageContaining("账本状态不允许入账");
        assertLedgerTransactionFactsUnchanged(afterSuspended);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_CLOSING_REJECTED_PAY");

        refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 30L, "DIRECT_CLOSING_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertThat(ledgerService.getLedgerById(payeeSettlementLedgerId).getStatus())
                .isEqualTo(LedgerStatus.SUSPENDED);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_CLOSING_REFUND", 2, 2);
    }

    /**
     * 场景：原付款账户已关闭，但旧交易随后收到关联退款。
     * 输入：付款 70 后把原付款账户状态改为 CLOSED，再按原 RouteSnapshot 退款 30。
     * 输出：退款被拒绝，原付款方、原收款方和平台账户余额保持不变。
     * 预期：关闭账户不自动重开，也不由资金底座静默改路到其他账户。
     * 红线：账户关闭不能被退款 closing posting 绕过；合法退款应由上层指定新的有效承接账户。
     */
    @Test
    void testReferencedRefundToClosedAccountShouldRejectWithoutLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("closed_refund_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "DIRECT_CLOSED_REFUND_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_CLOSED_REFUND_PAY");
        updateAccountStatus(payer, FundsAccountStatus.CLOSED);
        BalanceSnapshot beforeRefund = snapshot(balances(payer, payee, feeAccount()));
        LedgerFactSnapshot beforeRefundFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_CLOSED_REFUND_REFUND")
                .setDescription("referenced refund to closed account"), WindOperatorFactory.system()))
                .hasMessageContaining("关闭账户不允许承接退款");

        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeRefundFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_CLOSED_REFUND_REFUND");
    }

    /**
     * 场景：冻结账户仍可承接既有退款，但本次退款同时要求从该账户收取新手续费。
     * 输入：付款完成后冻结原付款账户，再发起关联退款 30 并收取手续费 5。
     * 输出：整笔退款被拒绝，退款本金和手续费均不入账。
     * 预期：退款入账义务不等于允许账户出账；手续费扣款必须满足 ACTIVE 借记准入。
     * 红线：不得借退款流程绕过冻结或挂起账户的出账控制。
     */
    @Test
    void testReferencedRefundWithFeeChargeSpecToFrozenAccountShouldRejectWithoutSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_frozen_fee_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "DIRECT_REFUND_FROZEN_FEE_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_FROZEN_FEE_PAY");
        updateAccountStatus(payer, FundsAccountStatus.FROZEN);
        BalanceSnapshot beforeRefund = snapshot(balances(payer, payee, feeAccount()));
        LedgerFactSnapshot beforeRefundFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setFeeChargeSpec(FeeSpec.builder()
                        .feeType("REFUND_PROCESSING_FEE")
                        .fixedFee(5)
                        .build())
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_FROZEN_FEE_REFUND")
                .setDescription("referenced refund with fee to frozen account"), WindOperatorFactory.system()))
                .hasMessageContaining("账户状态不允许扣取随交易手续费");

        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeRefundFacts);
        assertThat(fundsTransaction(payTransactionSn).getRefundedAmount()).isZero();
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_FROZEN_FEE_REFUND");
    }

    /**
     * 场景：直接退款指定了不存在的原支付交易流水。
     * 输入：付款方充值 100、向收款方付款 70，随后按缺失的原交易流水退款 30。
     * 输出：退款在 RouteSnapshot 回放阶段失败；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：缺少原交易 route snapshot 时不生成资金交易事实和账务事实。
     * 红线：直接退款一旦声明按原交易回放，不得静默退回普通退款路由或按当前账户绑定重选路。
     */
    @Test
    void testRefundWithMissingReferenceTransactionShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_missing_ref_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_MISSING_REFERENCE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_MISSING_REFERENCE_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn("FUNDS_TRANSACTION_NOT_EXISTS")
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_MISSING_REFERENCE_REFUND")
                .setDescription("refund with missing original transaction"), WindOperatorFactory.system()))
                .hasMessageContaining("退款原交易不存在");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_REFERENCE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_REFERENCE_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_REFERENCE_REFUND");
    }

    /**
     * 场景：原支付交易存在，但其账本流水缺失或不唯一。
     * 输入：完成充值和付款后，测试边界将原支付账本流水调整为 0 条或 2 条，再发起关联退款。
     * 输出：退款在账本来源唯一性校验处失败，余额和既有账务事实保持不变。
     * 红线：退款不得猜测账本来源，也不得在来源不唯一时写入任何资金或账务事实。
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testReferencedRefundWithoutUniqueLedgerTransactionShouldRejectWithoutSideEffects(boolean duplicate) {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_source_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "DIRECT_REFUND_LEDGER_SOURCE_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_LEDGER_SOURCE_PAY");

        if (duplicate) {
            int inserted = core1bJdbcTemplate.update("""
                    INSERT INTO t_ledger_transaction (
                        sn, tenant_id, funds_transaction_sn, reference_ledger_transaction_sn,
                        instruction_type, event_type, transaction_type, business_scene, business_sn,
                        amount, currency, original_amount, original_currency, exchange_rate,
                        debit_amount, credit_amount, transaction_time, description, context_variables, sha256)
                    SELECT ?, tenant_id, funds_transaction_sn, reference_ledger_transaction_sn,
                        instruction_type, event_type, transaction_type, business_scene, business_sn,
                        amount, currency, original_amount, original_currency, exchange_rate,
                        debit_amount, credit_amount, transaction_time, description, context_variables, sha256
                    FROM t_ledger_transaction
                    WHERE tenant_id = ? AND funds_transaction_sn = ?
                    """, "LE_DUPLICATE_DIRECT_REFUND_SOURCE", TenantContextHolder.requireTenantId(),
                    payTransactionSn);
            assertThat(inserted).isOne();
        } else {
            clearLedgerFactsForFundsTransaction(payTransactionSn);
        }
        assertThat(ledgerTransactionsByFundsTransactionSn(payTransactionSn)).hasSize(duplicate ? 2 : 0);
        BalanceSnapshot beforeFailure = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        String refundBusinessSn = duplicate
                ? "DIRECT_REFUND_DUPLICATE_LEDGER_SOURCE"
                : "DIRECT_REFUND_MISSING_LEDGER_SOURCE";

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn(refundBusinessSn)
                .setDescription("refund without unique ledger source"), WindOperatorFactory.system()))
                .hasMessageContaining("原资金交易账本流水不存在或不唯一");

        assertThat(snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount())))
                .isEqualTo(beforeFailure);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertThat(fundsTransaction(payTransactionSn).getRefundedAmount()).isZero();
        assertNoPersistedTransactionFactsForBusinessSn(refundBusinessSn);
    }

    /**
     * 场景：直接退款指定的原支付交易存在，但原交易缺少 route snapshot。
     * 输入：付款方充值 100、向收款方付款 70，随后清空原支付交易 route snapshot 并按原交易退款 30。
     * 输出：退款在 RouteSnapshot 回放阶段失败；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：原交易快照缺失时不生成新的资金交易事实和账务事实，原支付交易累计退款金额不变化。
     * 红线：直接退款一旦声明按原交易回放，不得在快照缺失时静默退回普通退款路由或重选路。
     */
    @Test
    void testRefundWithMissingReferenceRouteSnapshotShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_snap_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_MISSING_ROUTE_SNAPSHOT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_MISSING_ROUTE_SNAPSHOT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_ROUTE_SNAPSHOT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_ROUTE_SNAPSHOT_PAY", 2, 2);

        clearFundsTransactionRouteSnapshot(payTransactionSn);
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(payTransactionSn))
                .as("original direct pay route snapshot must be absent before replay refund")
                .isEmpty();
        LedgerFactSnapshot afterCorruptedReferenceFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_MISSING_ROUTE_SNAPSHOT_REFUND")
                .setDescription("refund with missing original route snapshot"), WindOperatorFactory.system()))
                .hasMessageContaining("RouteSnapshot 回放事件未找到原路径快照");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterCorruptedReferenceFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_ROUTE_SNAPSHOT_REFUND");
        assertThat(fundsTransaction(payTransactionSn).getRefundedAmount()).isZero();
    }

    /**
     * 场景：关联退款同时传入原交易引用和新的退款到账账户。
     * 输入：原付款完成后，退款请求同时携带 referenceTransactionSn 与 accountId。
     * 输出：请求在指令转换阶段被拒绝，原交易余额、累计退款和账务事实均不变化。
     * 预期：关联退款只允许原 route snapshot 决定路径，不能混入第二套路由来源。
     * 红线：不得优先使用请求账户，也不得在原快照与请求账户之间静默择一。
     */
    @Test
    void testReferencedRefundWithExplicitAccountShouldRejectWithoutSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_conflict_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "DIRECT_REFUND_CONFLICT_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_CONFLICT_PAY");
        BalanceSnapshot beforeRefund = snapshot(balances(payer, payee));
        LedgerFactSnapshot beforeRefundFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_CONFLICT_REFUND")
                .setDescription("referenced refund with explicit account"), WindOperatorFactory.system()))
                .hasMessageContaining("关联退款不得重复传入退款到账账户");

        BalanceSnapshot afterRefund = snapshot(balances(payer, payee));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeRefundFacts);
        assertThat(fundsTransaction(payTransactionSn).getRefundedAmount()).isZero();
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_CONFLICT_REFUND");
    }

    /**
     * 场景：直接退款指定原支付交易流水。
     * 输入：付款方充值 100、向收款方付款 70，随后按原支付交易流水退款 30。
     * 输出：退款按原支付 RouteSnapshot 回放并生成独立退款交易事实。
     * 预期：退款交易引用原支付交易，RouteCode 为 DIRECT_REFUND_REPLAY，原支付交易累计 refundedAmount。
     * 红线：直接退款不能把退款事件写回原支付 businessSn，也不能因引用原交易而丢失独立退款审计事实。
     */
    @Test
    void testRefundWithReferenceTransactionShouldReplayOriginalRouteAndKeepIndependentRefundFact() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_reference_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_REFERENCE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_REFERENCE_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String refundTransactionSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_REFERENCE_REFUND")
                .setDescription("refund with original transaction reference"), WindOperatorFactory.system());
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_REFERENCE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_REFERENCE_PAY", 2, 2);
        assertReferenceRefundFacts("DIRECT_REFUND_REFERENCE_REFUND", refundTransactionSn, payTransactionSn, 30L);

        LedgerFactSnapshot afterFirstRefundFacts = ledgerFactSnapshot();
        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(50L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_REFERENCE_EXCEED_REFUND")
                .setDescription("refund exceeds original transaction remaining amount"), WindOperatorFactory.system()))
                .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRefund, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_REFERENCE_EXCEED_REFUND");
        assertThat(fundsTransaction(payTransactionSn).getRefundedAmount()).isEqualTo(30L);
    }

    /**
     * 场景：同一原支付在并发窗口内收到两笔超过累计可退金额的部分退款。
     * 输入：原支付 100，收款方另有足额待清算余额，两笔不同业务流水同时退款 60。
     * 输出：只有一笔退款成功，另一笔按原交易累计可退金额失败且不留下资金或账务事实。
     * 红线：不能依赖收款方余额不足偶然阻止并发超退，同一 referenceTransactionSn 必须串行裁决。
     */
    @Test
    void testConcurrentReferencedRefundsShouldAllowOnlyOneWinner() throws Exception {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId reservePayer = fundingAccount("refund_reserve_payer");
        FundsAccountId payee = fundingAccount("refund_race_payee");
        ensureLedger(reservePayer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "DIRECT_REFUND_CONCURRENT_TOPUP");
        topup(reservePayer, 100L, "DIRECT_REFUND_CONCURRENT_RESERVE_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 100L,
                "DIRECT_REFUND_CONCURRENT_PAY");
        pay(reservePayer, payee, LedgerSubjectCode.SETTLEMENT, 100L,
                "DIRECT_REFUND_CONCURRENT_RESERVE_PAY");
        BalanceSnapshot beforeRace = snapshot(balances(payer, reservePayer, payee));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<DirectRefundRaceOutcome> first = executor.submit(() -> raceReferencedRefund(ready, start,
                    payTransactionSn, "DIRECT_REFUND_CONCURRENT_FIRST"));
            Future<DirectRefundRaceOutcome> second = executor.submit(() -> raceReferencedRefund(ready, start,
                    payTransactionSn, "DIRECT_REFUND_CONCURRENT_SECOND"));
            assertThat(ready.await(5, TimeUnit.SECONDS)).as("direct refund race commands are ready").isTrue();
            start.countDown();

            List<DirectRefundRaceOutcome> outcomes = List.of(awaitDirectRefundOutcome(first),
                    awaitDirectRefundOutcome(second));
            List<DirectRefundRaceOutcome> successes = outcomes.stream()
                    .filter(DirectRefundRaceOutcome::succeeded)
                    .toList();
            List<DirectRefundRaceOutcome> failures = outcomes.stream()
                    .filter(outcome -> !outcome.succeeded())
                    .toList();
            assertThat(successes).as("direct refund race outcomes: %s", outcomes).hasSize(1);
            assertThat(failures).as("direct refund race outcomes: %s", outcomes).hasSize(1);
            assertThat(failures.getFirst().failure())
                    .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");

            BalanceSnapshot afterRace = snapshot(balances(payer, reservePayer, payee));
            assertOnlyBalanceDeltas(beforeRace, afterRace,
                    delta(payer, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                    delta(reservePayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                    delta(payee, LedgerSubjectCode.SETTLEMENT, -60L, CURRENCY));
            assertThat(fundsTransaction(payTransactionSn).getRefundedAmount()).isEqualTo(60L);
            assertReferenceRefundFacts(successes.getFirst().businessSn(), successes.getFirst().transactionSn(),
                    payTransactionSn, 60L);
            assertNoFundsOrLedgerFactsForBusinessSn(failures.getFirst().businessSn());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：关联原支付的退款已由上层确认同时收取退款处理费。
     * 输入：付款 70 后按原 RouteSnapshot 退款 30，并传入固定手续费 5；请求不重复传入退款路由字段。
     * 输出：原付款方 AVAILABLE 回补 30 后扣费 5，原收款方 SETTLEMENT 减少 30，平台 FEE 增加 5。
     * 预期：退款本金腿和新增手续费腿在同一账本交易中原子入账。
     * 红线：关联退款不得忽略收费规则，也不得按当前请求重新选择退款路径。
     */
    @Test
    void testReferencedRefundWithFeeChargeSpecShouldChargeUniqueFundingBeneficiaryAtomically() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_fee_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        topup(payer, 100L, "DIRECT_REFUND_FEE_TOPUP");
        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_FEE_PAY");
        BalanceSnapshot beforeRefund = snapshot(balances(payer, payee, feeAccount()));

        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setFeeChargeSpec(FeeSpec.builder()
                        .feeType("REFUND_PROCESSING_FEE")
                        .fixedFee(5)
                        .build())
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_FEE_REFUND")
                .setDescription("referenced refund with processing fee"), WindOperatorFactory.system());

        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 25L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY));
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("DIRECT_REFUND_FEE_REFUND");
        assertThat(fundsTransaction(ledgerTransaction.getFundsTransactionSn()).getFeeAmount()).isEqualTo(5L);
        assertThat(fundsTransaction(payTransactionSn).getFeeAmount()).isZero();
        assertThat(postingPlansOf(ledgerTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerPhaseCode.REFUND.name(), LedgerPhaseCode.FEE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_FEE_REFUND", 3, 4);
    }

    /**
     * 场景：业务确认型退款只回补 CreditAccount 控制额度，同时要求自动收取退款处理费。
     * 输入：退款目标为 CreditAccount，退款请求携带固定手续费规则。
     * 输出：请求被拒绝，信用额度、退款出资账目和平台费用账目均不变化。
     * 预期：随交易手续费只能从唯一真实资金受益 FundingAccount 的 AVAILABLE 扣取。
     * 红线：不得把 CreditAccount.AVAILABLE 信用额度转换为平台手续费收入。
     */
    @Test
    void testRefundWithFeeChargeSpecAndCreditOnlyBeneficiaryShouldRejectWithoutLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId refundPayer = fundingAccount("refund_credit_fee_payer");
        FundsAccountId creditAccount = creditAccount("refund_credit_fee_target");
        ensureLedger(refundPayer, LedgerSubjectCode.SETTLEMENT);
        ensureCreditAccount(creditAccount);
        topup(payer, 100L, "DIRECT_REFUND_CREDIT_FEE_TOPUP");
        pay(payer, refundPayer, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_CREDIT_FEE_PAY");
        BalanceSnapshot beforeRefund = snapshot(balances(refundPayer, creditAccount, feeAccount()));
        LedgerFactSnapshot beforeRefundFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(creditAccount)
                .setPayerId(refundPayer)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setFeeChargeSpec(FeeSpec.builder()
                        .feeType("REFUND_PROCESSING_FEE")
                        .fixedFee(5)
                        .build())
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_CREDIT_FEE_REFUND")
                .setDescription("credit-only refund with processing fee"), WindOperatorFactory.system()))
                .hasMessageContaining("随交易手续费扣款账户必须是唯一真实资金账户");

        BalanceSnapshot afterRefund = snapshot(balances(refundPayer, creditAccount, feeAccount()));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(refundPayer, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(creditAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeRefundFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_CREDIT_FEE_REFUND");
    }

    /**
     * 场景：原支付交易的 route snapshot 已固化旧支付工具和旧路由决策，后续业务侧发生换绑或规则变化。
     * 输入：付款方充值 100、向收款方付款 70；测试构造原支付快照含 CARD-OLD 和旧路由决策，退款请求携带当前规则上下文。
     * 输出：退款按原支付 RouteSnapshot 回放并生成独立退款交易事实。
     * 预期：退款交易保存的新 RouteSnapshot 继续保留 CARD-OLD、旧绑定版本和旧路由决策，不消费当前请求上下文重选路。
     * 红线：直接退款引用原交易时不得因当前卡绑定、当前资金责任或当前规则变化改写历史资金路径。
     */
    @Test
    void testRefundWithReferenceTransactionShouldReuseOriginalInstrumentAndRouteDecision() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_replay_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_REPLAY_BINDING_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String payTransactionSn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L,
                "DIRECT_REFUND_REPLAY_BINDING_PAY");
        enrichFundsTransactionRouteSnapshot(payTransactionSn, Map.of(
                "paymentInstrumentRef", paymentInstrumentSnapshot("CARD-OLD", "BINDING-OLD", "v1"),
                "routingDecision", routingDecisionSnapshot()));
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(payTransactionSn))
                .as("original direct pay route snapshot must carry historical attribution")
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getPaymentInstrumentRef().getInstrumentId()).isEqualTo("CARD-OLD");
                    assertThat(routeSnapshot.getPaymentInstrumentRef().getBindingSnapshot())
                            .containsEntry("bindingId", "BINDING-OLD")
                            .containsEntry("bindingVersion", "v1");
                    assertThat(routeSnapshot.getRoutingDecision().getPolicyCode())
                            .isEqualTo("HISTORICAL_ROUTE_DECISION");
                });
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String refundTransactionSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(payTransactionSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_REPLAY_BINDING_REFUND")
                .setContextVariables(WritableContextVariables.of(Map.of(
                        "businessContextVersion", "CURRENT-BINDING-RULE-V2")))
                .setDescription("refund with changed current binding context"), WindOperatorFactory.system());
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_REPLAY_BINDING_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_REPLAY_BINDING_PAY", 2, 2);
        assertReferenceRefundFacts("DIRECT_REFUND_REPLAY_BINDING_REFUND", refundTransactionSn, payTransactionSn,
                30L);
        assertReferencedRefundRouteSnapshotKeepsHistoricalAttribution(
                "DIRECT_REFUND_REPLAY_BINDING_REFUND");
    }

    /**
     * 场景：直接退款请求把敏感账户值放入扩展上下文。
     * 输入：付款方充值 100 并向收款方付款 70 后，退款 contextVariables 含嵌套 IBAN 值。
     * 输出：退款请求被拒绝；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：退款入口在构造指令前阻断敏感上下文，不生成资金交易事实和账务事实。
     * 红线：IBAN、完整账户号等敏感值不得通过退款上下文落库。
     */
    @Test
    void testRefundWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_ctx_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_SENSITIVE_CONTEXT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("refund with sensitive IBAN value"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：直接退款缺少退款到账账户。
     * 输入：付款方充值 100 并向收款方付款 70 后，退款请求不传 accountId。
     * 输出：退款请求被拒绝；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：直接退款必须明确退款到账账户，缺到账账户不能进入 route 和 ledger。
     * 红线：缺到账账户不能以底层账户查询异常或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testRefundWithoutAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_miss_acc_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_MISSING_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_MISSING_ACCOUNT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_MISSING_ACCOUNT")
                .setDescription("refund without account"), WindOperatorFactory.system()))
                .hasMessageContaining("直接退款到账账户不能为空");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_ACCOUNT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_ACCOUNT_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_ACCOUNT");
    }

    /**
     * 场景：业务确认型直接退款没有给出退款出资账目。
     * 输入：到账账户和出资账户均有效，但不传 payerLedgerSubjectCode。
     * 输出：请求在路由前被拒绝，不生成资金交易或账务事实。
     * 预期：无原交易快照可回放时，调用方必须完整确认退款资金来源。
     * 红线：不得默认使用 SETTLEMENT、AVAILABLE 或任何其他账目。
     */
    @Test
    void testBusinessConfirmedRefundWithoutPayerLedgerSubjectCodeShouldRejectWithoutSideEffects() {
        FundsAccountId beneficiary = fundingAccount("refund_missing_ledger_beneficiary");
        FundsAccountId payer = fundingAccount("refund_missing_ledger_payer");
        LedgerFactSnapshot before = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(beneficiary)
                .setPayerId(payer)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_MISSING_PAYER_LEDGER")
                .setDescription("business-confirmed refund without payer ledger"), WindOperatorFactory.system()))
                .hasMessageContaining("业务确认型直接退款出资账目不能为空");

        assertLedgerTransactionFactsUnchanged(before);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_PAYER_LEDGER");
    }

    /**
     * 场景：直接退款把外部账户作为退款到账账户。
     * 输入：付款方充值 100 并向收款方付款 70 后，退款到账账户为外部银行账户。
     * 输出：退款请求被拒绝；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：外部账户只能作为出入金引用或快照，不能成为退款到账 ledger subject。
     * 红线：外部账户不得生成退款 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testRefundToExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_ext_payee");
        FundsAccountId externalAccount = FundsAccountId.immutable("external_refund_account",
                DefaultFundsAccountType.EXTERNAL_BANK);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_EXTERNAL_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_EXTERNAL_ACCOUNT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(externalAccount)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_EXTERNAL_ACCOUNT")
                .setDescription("refund to external account"), WindOperatorFactory.system()))
                .hasMessageContaining("直接退款到账账户不能是外部账户");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_EXTERNAL_ACCOUNT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_EXTERNAL_ACCOUNT_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_EXTERNAL_ACCOUNT");
    }

    /**
     * 场景：直接退款把外部账户作为退款出资主体。
     * 输入：付款方充值 100 并向收款方付款 70 后，退款出资主体为外部银行账户。
     * 输出：退款请求被拒绝；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：外部账户只能作为出入金引用或快照，不能成为退款出资 ledger subject。
     * 红线：外部账户不得生成退款 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testRefundFromExternalPayerShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_ext_payer_payee");
        FundsAccountId externalPayer = FundsAccountId.immutable("external_refund_payer",
                DefaultFundsAccountType.EXTERNAL_BANK);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_EXTERNAL_PAYER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_EXTERNAL_PAYER_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(externalPayer)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_EXTERNAL_PAYER")
                .setDescription("refund from external payer"), WindOperatorFactory.system()))
                .hasMessageContaining("直接退款出资主体不能是外部账户");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_EXTERNAL_PAYER_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_EXTERNAL_PAYER_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_EXTERNAL_PAYER");
    }

    /**
     * 场景：直接退款缺少退款出资主体。
     * 输入：付款方充值 100 并向收款方付款 70 后，退款请求不传 payerId。
     * 输出：退款请求被拒绝；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：直接退款必须明确退款出资主体，缺主体不能进入 route 和 ledger。
     * 红线：缺出资主体不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testRefundWithoutPayerShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_miss_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_MISSING_PAYER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_MISSING_PAYER_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_MISSING_PAYER")
                .setDescription("refund without payer"), WindOperatorFactory.system()))
                .hasMessageContaining("直接退款出资主体不能为空");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_PAYER_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_PAYER_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_MISSING_PAYER");
    }

    /**
     * 场景：同一资金账户向自己发起系统内转账。
     * 输入：充值 100 后，付款方和收款方均为同一账户，转账 10。
     * 输出：请求被拒绝；余额、资金交易事实和账务事实保持充值后的状态。
     * 预期：系统内转账必须是跨主体价值转移，同主体转账不能生成 route、posting 或 ledger entry。
     * 红线：不能用一借一贷自循环掩盖无业务意义的资金事实。
     */
    @Test
    void testSameAccountTransferShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        topup(account, 100L, "DIRECT_SAME_ACCOUNT_TRANSFER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(account, account, 10L, "DIRECT_SAME_ACCOUNT_TRANSFER"))
                .hasMessageContaining("付款账户和收款账户不能一致");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_TRANSFER_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_TRANSFER");
    }

    /**
     * 场景：系统内转账缺少付款和收款账户。
     * 输入：转账请求未传 payerAccountId 和 payeeAccountId。
     * 输出：请求被拒绝；平台现金和预收款余额均不变化。
     * 预期：系统内转账必须先明确付款主体，不能把两个缺失主体误判成同账户转账。
     * 红线：缺主体不能生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTransferWithoutAccountsShouldRejectAndLeaveNoLedgerSideEffects() {
        BalanceSnapshot before = snapshot(balances(cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_MISSING_ACCOUNTS")
                .setDescription("transfer without accounts"), WindOperatorFactory.system()))
                .hasMessageContaining("系统内转账付款账户不能为空");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTransfer,
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_MISSING_ACCOUNTS");
    }

    /**
     * 场景：资金账户向自己发起直接付款。
     * 输入：充值 100 后，付款账户和收款主体均为同一账户，收款账目为 SETTLEMENT。
     * 输出：请求被拒绝；账户和平台账户余额保持充值后的状态，且不生成付款事实。
     * 预期：直接付款必须表达跨主体价值转移，同主体账目修正应走余额控制或调账能力。
     * 红线：不能用普通付款把同主体 AVAILABLE -> SETTLEMENT 伪装成跨主体支付。
     */
    @Test
    void testPayToSameAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        ensureLedger(account, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        topup(account, 100L, "DIRECT_SAME_ACCOUNT_PAY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(account, account, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_SAME_ACCOUNT_PAY"))
                .hasMessageContaining("付款账户和收款主体不能一致");

        BalanceSnapshot afterRejectedPay = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_PAY_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_PAY");
    }

    /**
     * 场景：资金账户向自己发起直接退款。
     * 输入：付款方充值 100 并向目标账户付款 40 后，目标账户把自己作为退款到账账户和退款出资主体。
     * 输出：请求被拒绝；付款方、目标账户和平台账户余额保持付款后的状态，且不生成退款事实。
     * 预期：直接退款必须表达跨主体逆向价值转移，同主体账目修正应走余额控制或调账能力。
     * 红线：不能用普通退款把同主体 SETTLEMENT -> AVAILABLE 伪装成跨主体退款。
     */
    @Test
    void testRefundToSameAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId account = fundingAccount("same_refund_account");
        ensureLedger(account, LedgerSubjectCode.AVAILABLE);
        ensureLedger(account, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, account, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_SAME_ACCOUNT_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, account, LedgerSubjectCode.SETTLEMENT, 40L, "DIRECT_SAME_ACCOUNT_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> refund(account, account, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_SAME_ACCOUNT_REFUND"))
                .hasMessageContaining("退款到账账户和退款出资主体不能一致");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, account, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_REFUND_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_REFUND");
    }

    /**
     * 场景：系统内转账把外部账户作为收款主体。
     * 输入：付款方充值 50 后，提交外部银行账户作为 payeeAccountId。
     * 输出：请求被拒绝；付款方和平台账户余额保持充值后的状态。
     * 预期：外部账户只能作为出入金引用或快照，不能成为系统内转账的 ledger subject。
     * 红线：外部账户不得生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTransferToExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId externalPayee = FundsAccountId.immutable("external_bank_transfer_payee",
                DefaultFundsAccountType.EXTERNAL_BANK);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_TRANSFER_EXTERNAL_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(externalPayee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_EXTERNAL_PAYEE")
                .setDescription("transfer to external payee"), WindOperatorFactory.system()))
                .hasMessageContaining("系统内转账收款账户不能是外部账户");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_EXTERNAL_PAYEE_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_EXTERNAL_PAYEE");
    }

    /**
     * 场景：系统内转账把外部账户作为付款主体。
     * 输入：外部银行账户作为 payerAccountId，向普通资金账户转账 10。
     * 输出：请求被拒绝；收款方和平台账户余额均不变化。
     * 预期：外部账户只能作为出入金引用或快照，不能成为系统内转账的 ledger subject。
     * 红线：外部账户不得生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTransferFromExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId externalPayer = FundsAccountId.immutable("external_bank_transfer_payer",
                DefaultFundsAccountType.EXTERNAL_BANK);
        FundsAccountId payee = fundingAccount("external_transfer_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot before = snapshot(balances(payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(externalPayer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_EXTERNAL_PAYER")
                .setDescription("transfer from external payer"), WindOperatorFactory.system()))
                .hasMessageContaining("系统内转账付款账户不能是外部账户");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTransfer,
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_EXTERNAL_PAYER");
    }

    /**
     * 场景：直接充值把外部账户作为入账主体。
     * 输入：外部银行账户作为 accountId，外部银行账户作为资金来源。
     * 输出：请求被拒绝；平台账户余额不变化。
     * 预期：充值入账主体必须是内部可记账主体，外部账户只能作为资金来源引用。
     * 红线：外部账户不得生成充值 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTopupToExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId externalAccount = FundsAccountId.immutable("external_bank_topup_account",
                DefaultFundsAccountType.EXTERNAL_BANK);
        BalanceSnapshot before = snapshot(balances(externalAccount, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(externalAccount)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_topup_source",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_EXTERNAL_ACCOUNT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_EXTERNAL_ACCOUNT")
                .setDescription("topup to external account"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值入账账户不能是外部账户");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(externalAccount, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_EXTERNAL_ACCOUNT");
    }

    /**
     * 场景：直接充值把支出控制范围作为入账主体。
     * 输入：支出控制范围作为 accountId，外部银行账户作为资金来源。
     * 输出：请求被拒绝；不生成支出控制流水或核心余额投影，平台账户余额不变化。
     * 预期：支出控制范围只能作为预算控制上下文，不得被充值交易包装成入金价值主体。
     * 红线：支出控制范围不得生成充值 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTopupToSpendControlScopeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId budget = spendControlScope("topup_bg");
        ensureSpendControlScopeWithoutLedgers(budget);
        BalanceSnapshot before = snapshot(balances(budget, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> topup(budget, 10L, "DIRECT_TOPUP_SPEND_CONTROL_SCOPE"))
                .hasMessageContaining("直接充值入账账户不能是支出控制范围");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(budget, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(budget, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertSubjectBalanceNotInitialized(balance(budget));
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoPersistedTransactionFactsForBusinessSn("DIRECT_TOPUP_SPEND_CONTROL_SCOPE");
    }

    /**
     * 场景：直接充值把内部资金账户作为资金来源。
     * 输入：普通资金账户作为 fundsSourceAccountId，向另一个普通资金账户充值 10。
     * 输出：请求被拒绝；用户账户、资金来源账户和平台账户余额均不变化。
     * 预期：充值资金来源只能是外部账户引用，内部资金账户不能伪装成入金来源。
     * 红线：内部资金账户不得作为充值 externalAccountRef 进入 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTopupFromInternalSourceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        FundsAccountId internalSource = fundingAccount("internal_topup_source");
        ensureLedger(internalSource, LedgerSubjectCode.AVAILABLE);
        ensureLedger(internalSource, LedgerSubjectCode.FROZEN);
        BalanceSnapshot before = snapshot(balances(account, internalSource, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(internalSource)
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_INTERNAL_SOURCE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_INTERNAL_SOURCE")
                .setDescription("topup from internal source"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值资金来源账户必须是外部账户");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, internalSource, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(internalSource, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(internalSource, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(internalSource), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(internalSource), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_INTERNAL_SOURCE");
    }

    /**
     * 场景：USD 资金账户发起 CNY 系统内转账。
     * 输入：付款方充值 50 USD，随后向收款方转账 10 CNY。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：系统内转账只接受付款方账户同币种金额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：系统内转账不得隐式换汇，不得留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testTransferWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("transfer_currency_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_TRANSFER_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CurrencyIsoCode.CNY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_CURRENCY")
                .setDescription("transfer with different currency"), WindOperatorFactory.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_CURRENCY_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_CURRENCY");
    }

    /**
     * 场景：USD 资金账户收到 CNY 充值请求。
     * 输入：用户资金账户为 USD，外部通道充值请求金额为 10 CNY。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：充值只接受目标账户同币种金额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：充值不得静默换汇，不得留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testTopupWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_topup_currency",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_CURRENCY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CurrencyIsoCode.CNY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_CURRENCY")
                .setDescription("topup with different currency"), WindOperatorFactory.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_CURRENCY");
    }

    /**
     * 场景：直接充值缺少入账账户。
     * 输入：充值请求未传 accountId。
     * 输出：请求被拒绝；平台现金和预收款余额均不变化。
     * 预期：直接充值必须明确入账账户，缺主体不能进入 route 和 ledger。
     * 红线：缺入账账户不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testTopupWithoutAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        BalanceSnapshot before = snapshot(balances(cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_missing_topup_account",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_MISSING_ACCOUNT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_MISSING_ACCOUNT")
                .setDescription("topup without account"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值入账账户不能为空");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_MISSING_ACCOUNT");
    }

    /**
     * 场景：直接充值同时缺少入账账户和资金来源账户。
     * 输入：充值请求未传 accountId 和 fundsSourceAccountId。
     * 输出：请求被拒绝；平台现金和预收款余额均不变化。
     * 预期：直接充值必须先明确入账主体，不能用资金来源校验掩盖缺入账账户。
     * 红线：缺入账主体不能生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTopupWithoutAccountAndFundsSourceShouldRejectAndLeaveNoLedgerSideEffects() {
        BalanceSnapshot before = snapshot(balances(cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_MISSING_ACCOUNT_AND_SOURCE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_MISSING_ACCOUNT_AND_SOURCE")
                .setDescription("topup without account and funds source"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值入账账户不能为空");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_MISSING_ACCOUNT_AND_SOURCE");
    }

    /**
     * 场景：直接充值缺少资金来源账户。
     * 输入：充值请求未传 fundsSourceAccountId。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：直接充值必须明确外部资金来源，缺来源不能进入 route 和 ledger。
     * 红线：缺外部资金来源不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testTopupWithoutFundsSourceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_MISSING_SOURCE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_MISSING_SOURCE")
                .setDescription("topup without funds source"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值资金来源账户不能为空");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_MISSING_SOURCE");
    }

    /**
     * 场景：直接充值缺少资金通道。
     * 输入：充值请求未传 channel。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：直接充值必须明确外部资金通道，缺通道不能进入 route 和 ledger。
     * 红线：缺资金通道不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testTopupWithoutChannelShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_missing_topup_channel",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannelTransactionSn("DIRECT_TOPUP_MISSING_CHANNEL_REF")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_MISSING_CHANNEL")
                .setDescription("topup without channel"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值资金通道不能为空");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_MISSING_CHANNEL");
    }

    /**
     * 场景：直接充值缺少通道交易流水。
     * 输入：充值请求未传 channelTransactionSn。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：直接充值必须明确外部通道交易流水，缺流水不能进入 route 和 ledger。
     * 红线：缺通道交易流水不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testTopupWithoutChannelTransactionSnShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_missing_topup_channel_sn",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_MISSING_CHANNEL_SN")
                .setDescription("topup without channel transaction sn"), WindOperatorFactory.system()))
                .hasMessageContaining("直接充值通道交易流水不能为空");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_MISSING_CHANNEL_SN");
    }

    /**
     * 场景：直接充值把完整外部账户号伪装成外部账户引用 ID。
     * 输入：充值资金来源 FundsAccountId.id 为 12 位银行账户号。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：外部账户引用快照构造期阻断敏感原文，不生成资金交易事实和账务事实。
     * 红线：外部账户引用字段不得保存完整银行账户号、IBAN 或其他敏感原文。
     */
    @Test
    void testTopupWithRawExternalAccountIdShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("123456789012",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_RAW_EXTERNAL_ACCOUNT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_RAW_EXTERNAL_ACCOUNT")
                .setDescription("topup with raw external account id"), WindOperatorFactory.system()))
                .hasMessageContaining("externalAccountNo must be masked or token reference");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_RAW_EXTERNAL_ACCOUNT");
    }

    /**
     * 场景：直接充值和系统内转账请求把敏感账户值放入扩展上下文。
     * 输入：充值 contextVariables 含嵌套 IBAN 值；有效充值后，转账 contextVariables 含嵌套 IBAN 值。
     * 输出：两次请求均被拒绝；账户和平台余额保持最近一次成功事实后的状态。
     * 预期：直接交易各入口在构造指令前统一阻断敏感上下文，不生成资金交易事实和账务事实。
     * 红线：IBAN、完整账户号等敏感值不得通过普通交易上下文落库。
     */
    @Test
    void testTopupAndTransferWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("ctx_transfer_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payer)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_sensitive_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_SENSITIVE_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(50L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("topup with sensitive IBAN value"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        topup(payer, 50L, "DIRECT_TRANSFER_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRejectedTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("transfer with sensitive IBAN value"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_SENSITIVE_CONTEXT_IBAN_VALUE");
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：付款方可用余额不足时发起系统内转账。
     * 输入：付款方未充值，向收款方转账 10。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额均不变化。
     * 预期：转账必须受付款方 AVAILABLE 余额约束，余额不足时记录 FAILED 资金交易事实。
     * 红线：转账余额不足不能留下 posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testTransferWithInsufficientBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("transfer_low_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(payer, payee, 10L, "DIRECT_TRANSFER_INSUFFICIENT"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_TRANSFER_INSUFFICIENT");
    }

    /**
     * 场景：系统内转账把支出控制范围作为付款主体。
     * 输入：支出控制范围向普通资金账户转账 10。
     * 输出：请求被拒绝；不生成支出控制流水或核心余额投影，收款方和平台账户余额保持请求前状态。
     * 预期：支出控制范围只能作为预算控制上下文，不得被转账交易包装成资金价值主体。
     * 红线：支出控制范围不得生成转账 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testTransferFromSpendControlScopeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId budget = spendControlScope("transfer_bg");
        FundsAccountId payee = fundingAccount("budget_transfer_payee");
        ensureSpendControlScopeWithoutLedgers(budget);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTransfer = snapshot(balances(budget, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeTransferFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(budget, payee, 10L, "DIRECT_TRANSFER_SPEND_CONTROL_SCOPE"))
                .hasMessageContaining("系统内转账付款账户不能是支出控制范围");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(budget, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTransfer, afterRejectedTransfer,
                delta(budget, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeTransferFacts);

        assertSubjectBalanceNotInitialized(balance(budget));
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_SPEND_CONTROL_SCOPE");
    }

    /**
     * 场景：系统内转账第一次余额不足生成 FAILED 事实后，付款方补足余额并用相同业务流水重试。
     * 输入：同一 `businessSn` 首次转账 10 失败，随后充值 20 后再次提交同一转账。
     * 输出：重试被拒绝；原 FAILED 资金交易事实不被改写成成功，补账后的余额和账务事实保持不变。
     * 预期：失败事实是稳定审计事实；调用方必须使用新的业务流水表达新的资金交易。
     * 红线：同业务流水重试不得污染原失败事实，不得补生成 route、posting、ledger entry 或余额变化。
     */
    @Test
    void testFailedTransferRetryAfterFundingShouldRejectAndKeepFailedFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("failed_retry_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(payer, payee, 10L, "DIRECT_TRANSFER_FAILED_RETRY"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterFirstFailure = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_TRANSFER_FAILED_RETRY");
        RouteSnapshotSpec firstFailedRouteSnapshot = routeSnapshot("DIRECT_TRANSFER_FAILED_RETRY");

        topup(payer, 20L, "DIRECT_TRANSFER_FAILED_RETRY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(payer, payee, 10L, "DIRECT_TRANSFER_FAILED_RETRY"))
                .hasMessageContaining("资金交易已失败");

        BalanceSnapshot afterRetry = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRetry,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_TRANSFER_FAILED_RETRY", firstFailedRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_980L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_TRANSFER_FAILED_RETRY");
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_FAILED_RETRY_TOPUP", 3, 4);
    }

    /**
     * 场景：付款方可用余额不足时发起直接付款。
     * 输入：付款方未充值，向普通收款方付款 10。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额均不变化。
     * 预期：余额不足失败必须记录 FAILED 资金交易事实，不生成 posted ledger transaction。
     * 红线：余额不足不能留下半截 posting、ledger entry 或余额投影。
     */
    @Test
    void testPayWithInsufficientBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("insufficient_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_INSUFFICIENT_PAY"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot after = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_INSUFFICIENT_PAY");
    }

    /**
     * 场景：USD 资金账户发起 CNY 直接付款。
     * 输入：付款方充值 50 USD，随后向普通收款方付款 10 CNY。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：直接交易只接受账户同币种金额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：直接付款不得隐式换汇，不得留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testPayWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("different_currency_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CurrencyIsoCode.CNY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_CURRENCY_PAY")
                .setDescription("pay with different currency"), WindOperatorFactory.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_CURRENCY_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_CURRENCY_PAY");
    }

    /**
     * 场景：直接付款请求把通道 token secret 放入扩展上下文。
     * 输入：付款方充值 50 后，付款请求的 contextVariables 含嵌套 secretKey 字段。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：请求扩展上下文不得进入资金交易事实、交易明细、route snapshot 或账务事实。
     * 红线：完整卡号、CVV、密钥和 token secret 不得通过普通交易上下文落库。
     */
    @Test
    void testPayWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("sensitive_context_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("secretKey", "secret-value"))))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT")
                .setDescription("pay with sensitive context"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("pay with sensitive IBAN value"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT");
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：直接付款缺少收款主体。
     * 输入：付款方充值 50 后，提交 payeeId 为空的直接付款。
     * 输出：请求被拒绝；付款方和平台账户余额保持充值后的状态。
     * 预期：直接付款必须先解析到最终可记账收款主体，缺主体不能进入 route 和 ledger。
     * 红线：缺主体不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testPayWithoutPayeeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_MISSING_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_MISSING_PAYEE")
                .setDescription("pay without payee"), WindOperatorFactory.system()))
                .hasMessageContaining("直接付款收款主体不能为空");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_PAYEE_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_PAYEE");
    }

    /**
     * 场景：直接付款收款主体存在，但目标收款账目未建账。
     * 输入：付款方充值 50，收款资金账户存在但没有 SETTLEMENT 账本。
     * 输出：付款失败；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：账务计划缺目标账本时标记 FAILED 资金交易事实，不自动建账、不展示交易成功。
     * 红线：缺账本不能留下 posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testPayWithoutPayeeLedgerShouldRejectAndRollbackTransactionFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("no_payee_ledger");
        ensureFundingAccount(payee);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_MISSING_LEDGER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_PAY_MISSING_LEDGER"))
                .hasMessageContaining("账本不存在或不唯一");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertThat(balance(payee).getBalanceBuckets()).doesNotContainKey(LedgerSubjectCode.SETTLEMENT);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_LEDGER_TOPUP", 3, 4);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_PAY_MISSING_LEDGER");
    }

    /**
     * 场景：直接付款把外部账户作为收款主体。
     * 输入：付款方充值 50 后，提交外部银行账户作为 payeeId。
     * 输出：请求被拒绝；付款方和平台账户余额保持充值后的状态。
     * 预期：外部账户只能作为出入金引用或快照，不能成为 ledger subject。
     * 红线：外部账户不得生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testPayToExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId externalPayee = FundsAccountId.immutable("external_bank_payee",
                DefaultFundsAccountType.EXTERNAL_BANK);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_EXTERNAL_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(externalPayee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_EXTERNAL_PAYEE")
                .setDescription("pay to external payee"), WindOperatorFactory.system()))
                .hasMessageContaining("直接付款收款主体不能是外部账户");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_EXTERNAL_PAYEE_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_EXTERNAL_PAYEE");
    }

    /**
     * 场景：直接付款把外部账户作为付款主体。
     * 输入：外部银行账户作为 accountId，向普通收款方付款 10。
     * 输出：请求被拒绝；收款方和平台账户余额均不变化。
     * 预期：外部账户只能作为出入金引用或快照，不能成为直接付款的 ledger subject。
     * 红线：外部账户不得生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testPayFromExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId externalPayer = FundsAccountId.immutable("external_bank_payer",
                DefaultFundsAccountType.EXTERNAL_BANK);
        FundsAccountId payee = fundingAccount("external_payer_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(externalPayer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_EXTERNAL_PAYER")
                .setDescription("pay from external payer"), WindOperatorFactory.system()))
                .hasMessageContaining("直接付款账户不能是外部账户");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedPay,
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_EXTERNAL_PAYER");
    }

    /**
     * 场景：支出控制范围被误作为直接付款主体。
     * 输入：提交支出控制范围向普通收款方付款 10。
     * 输出：付款请求被拒绝；不生成支出控制流水或核心余额投影，收款方和平台账户余额保持请求前状态。
     * 预期：支出控制范围只能作为预算控制上下文，不得被直接交易包装成资金价值主体。
     * 红线：支出控制范围不得生成直接付款 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testPayFromSpendControlScopeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId budget = spendControlScope("direct_pay_spend_control_scope");
        FundsAccountId payee = fundingAccount("budget_pay_payee");
        ensureSpendControlScopeWithoutLedgers(budget);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforePay = snapshot(balances(budget, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforePayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(budget, payee, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_PAY_SPEND_CONTROL_SCOPE"))
                .hasMessageContaining("直接付款账户不能是支出控制范围");

        BalanceSnapshot afterRejectedPay = snapshot(balances(budget, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforePay, afterRejectedPay,
                delta(budget, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforePayFacts);

        assertSubjectBalanceNotInitialized(balance(budget));
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_SPEND_CONTROL_SCOPE");
    }

    /**
     * 场景：直接充值使用相同业务流水重复通知，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_ONLY`，随后同流水同金额重试，再同流水改金额为 41。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次充值后的状态。
     * 预期：充值幂等必须由 `tenantId + businessScene + businessSn + requestHash` 共同保护。
     * 红线：同业务流水不同请求不得新增交易、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTopupSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_ONLY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY")
                .setDescription("idempotent topup"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_TOPUP_ONLY");

        String retryTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_ONLY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY")
                .setDescription("idempotent topup"), WindOperatorFactory.system());

        assertThat(retryTopupSn).isEqualTo(firstTopupSn);
        BalanceSnapshot afterRetryTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterRetryTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_ONLY", firstRouteSnapshot);
        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_ONLY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(41L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY")
                .setDescription("idempotent topup"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTopup, afterConflict,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_ONLY", firstRouteSnapshot);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY", 3, 4);
    }

    /**
     * 场景：同一业务键首次为直接充值，重试请求被错误改成系统内转账。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_EVENT`，随后用相同 businessScene/businessSn 发起转账 10。
     * 输出：第二次请求被摘要冲突拒绝；余额和账务事实保持第一次充值后的状态。
     * 预期：直接交易幂等必须保护事件类型和交易类型，不能把不同资金动作追加到同一交易聚合。
     * 红线：同业务键不同事件不得新增 detail、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectSameBusinessSnWithDifferentEventShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_event_payee");
        ensureFundingAccount(payee);
        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payer)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_event_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_EVENT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_EVENT")
                .setDescription("idempotent topup event"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_TOPUP_EVENT");

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_EVENT")
                .setDescription("idempotent event mismatch transfer"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_EVENT", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertThat(balance(payee).isInitialized())
                .as("rejected transfer should not initialize payee ledger")
                .isFalse();
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_EVENT", 3, 4);
    }

    /**
     * 场景：直接充值使用相同业务流水重复通知，第二次请求只更换 traceId。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_TRACE`，首请求 traceId 为 TRACE-1，重试 traceId 为 TRACE-2。
     * 输出：重试返回同一资金交易流水；余额和账务事实保持第一次充值后的状态。
     * 预期：traceId 只用于审计追踪，不参与资金请求摘要的业务一致性判断。
     * 红线：易变审计字段变化不得误判为幂等冲突，也不得重复生成 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTopupSameBusinessSnWithDifferentTraceIdShouldReuseOriginalTransaction() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_trace_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_TRACE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("traceId", "TRACE-1")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_TRACE")
                .setDescription("idempotent topup with trace"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_TOPUP_TRACE");

        String retryTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_trace_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_TRACE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("traceId", "TRACE-2")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_TRACE")
                .setDescription("idempotent topup with trace"), WindOperatorFactory.system());

        assertThat(retryTopupSn).isEqualTo(firstTopupSn);
        BalanceSnapshot afterRetryTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterRetryTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_TRACE", firstRouteSnapshot);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_TRACE", 3, 4);
    }

    /**
     * 场景：直接充值使用相同业务流水重复通知，第二次请求更换非易变业务上下文字段。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_CONTEXT`，首请求 context 为 RULE-A，重试同 context 后改为 RULE-B。
     * 输出：同 context 重试返回同一资金交易流水；业务上下文变化被摘要冲突拒绝。
     * 预期：非易变 contextVariables 字段必须参与资金请求摘要，不能像 traceId 一样被过滤。
     * 红线：同业务流水不同业务上下文不得静默复用原交易，也不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTopupSameBusinessSnWithDifferentBusinessContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT")
                .setDescription("idempotent topup with business context"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_TOPUP_CONTEXT");

        String retryTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT")
                .setDescription("idempotent topup with business context"), WindOperatorFactory.system());

        assertThat(retryTopupSn).isEqualTo(firstTopupSn);
        BalanceSnapshot afterRetryTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterRetryTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_CONTEXT", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-B")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT")
                .setDescription("idempotent topup with business context"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTopup, afterConflict,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_CONTEXT", firstRouteSnapshot);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT", 3, 4);
    }

    /**
     * 场景：直接付款使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100，付款 40 使用业务流水 `DIRECT_IDEMPOTENT_PAY`，随后同流水同金额重试，再同流水改金额为 41。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次付款后的状态。
     * 预期：`tenantId + businessScene + businessSn + requestHash` 共同保护直接交易幂等。
     * 红线：同业务流水不同请求不得新增交易、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectPaySameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("direct_idempotent_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstPaySn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY");
        BalanceSnapshot afterFirstPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstPayFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_PAY");

        String retryPaySn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY");

        assertThat(retryPaySn).isEqualTo(firstPaySn);
        BalanceSnapshot afterRetryPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstPay, afterRetryPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY", firstRouteSnapshot);
        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 41L,
                "DIRECT_IDEMPOTENT_PAY"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryPay, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP", 3, 4);
        assertThat(fundsTransactionDetails(firstPaySn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY", 2, 2);
    }

    /**
     * 场景：直接付款使用相同业务流水重复提交，第二次请求更换非易变业务上下文字段。
     * 输入：充值 100，付款 40 使用业务流水 `DIRECT_IDEMPOTENT_PAY_CONTEXT`，首请求 context 为 RULE-A，重试同 context 后改为 RULE-B。
     * 输出：同 context 重试返回同一资金交易流水；业务上下文变化被摘要冲突拒绝。
     * 预期：付款幂等摘要必须覆盖非易变 contextVariables 字段，不能只覆盖金额和参与主体。
     * 红线：同业务流水不同业务上下文不得静默复用原交易，也不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectPaySameBusinessSnWithDifferentBusinessContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_pay_ctx_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_PAY_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstPaySn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT")
                .setDescription("idempotent pay with business context"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstPayFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_PAY_CONTEXT");

        String retryPaySn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT")
                .setDescription("idempotent pay with business context"), WindOperatorFactory.system());

        assertThat(retryPaySn).isEqualTo(firstPaySn);
        BalanceSnapshot afterRetryPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstPay, afterRetryPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY_CONTEXT", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-B")))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT")
                .setDescription("idempotent pay with business context"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryPay, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY_CONTEXT", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstPaySn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT", 2, 2);
    }

    /**
     * 场景：直接付款使用相同业务流水重复提交，但第二次把付款方和收款方都换成新的主体。
     * 输入：两个付款方各充值 100，第一次付款方 A 向收款方 A 支付 40，随后同业务流水改为付款方 B 向收款方 B 支付 40。
     * 输出：第二次请求被幂等摘要拒绝；付款方 B、收款方 B 和既有交易事实均不变化。
     * 预期：同业务流水的幂等保护必须覆盖参与主体，不只覆盖金额。
     * 红线：同业务流水不同参与方不得新增 detail、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectPaySameBusinessSnWithDifferentParticipantsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId anotherPayer = fundingAccount("idem_payer2");
        FundsAccountId payee = fundingAccount("idem_payee1");
        FundsAccountId anotherPayee = fundingAccount("idem_payee2");
        ensureLedger(anotherPayer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(anotherPayer, LedgerSubjectCode.FROZEN);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(anotherPayee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeFirstTopup = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_PARTICIPANT_TOPUP");
        BalanceSnapshot afterFirstTopup = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFirstTopup, afterFirstTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        topup(anotherPayer, 100L, "DIRECT_IDEMPOTENT_PARTICIPANT_ANOTHER_TOPUP");
        BalanceSnapshot afterSecondTopup = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterSecondTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstPaySn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY_PARTICIPANT");
        BalanceSnapshot afterFirstPay = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterSecondTopup, afterFirstPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstPayFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_PAY_PARTICIPANT");

        assertThatThrownBy(() -> pay(anotherPayer, anotherPayee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY_PARTICIPANT"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstPay, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY_PARTICIPANT", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(anotherPayer), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(anotherPayee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PARTICIPANT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PARTICIPANT_ANOTHER_TOPUP", 3, 4);
        assertThat(fundsTransactionDetails(firstPaySn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY_PARTICIPANT", 2, 2);
    }

    /**
     * 场景：系统内转账使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、转账 40，随后同流水同金额重试，再同流水改金额为 41。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次转账后的状态。
     * 预期：系统内转账幂等必须由业务键和请求摘要共同保护。
     * 红线：同业务流水不同请求不得新增交易、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTransferSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_transfer_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_TRANSFER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstTransferSn = directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER")
                .setDescription("idempotent transfer"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTransferFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_TRANSFER");

        String retryTransferSn = directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER")
                .setDescription("idempotent transfer"), WindOperatorFactory.system());

        assertThat(retryTransferSn).isEqualTo(firstTransferSn);
        BalanceSnapshot afterRetryTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTransfer, afterRetryTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTransferFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TRANSFER", firstRouteSnapshot);
        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(41L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER")
                .setDescription("idempotent transfer"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTransfer, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTransferFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TRANSFER", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TRANSFER.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_TOPUP", 3, 4);
        assertThat(fundsTransactionDetails(firstTransferSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TRANSFER", 2, 2);
    }

    /**
     * 场景：系统内转账使用相同业务流水重复提交，第二次请求更换非易变业务上下文字段。
     * 输入：充值 100，转账 40 使用业务流水 `DIRECT_IDEMPOTENT_TRANSFER_CONTEXT`，首请求 context 为 RULE-A，重试同 context 后改为 RULE-B。
     * 输出：同 context 重试返回同一资金交易流水；业务上下文变化被摘要冲突拒绝。
     * 预期：转账幂等摘要必须覆盖非易变 contextVariables 字段，不能只覆盖金额和参与主体。
     * 红线：同业务流水不同业务上下文不得静默复用原交易，也不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTransferSameBusinessSnWithDifferentBusinessContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_transfer_ctx_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_TRANSFER_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstTransferSn = directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT")
                .setDescription("idempotent transfer with business context"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTransferFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT");

        String retryTransferSn = directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT")
                .setDescription("idempotent transfer with business context"), WindOperatorFactory.system());

        assertThat(retryTransferSn).isEqualTo(firstTransferSn);
        BalanceSnapshot afterRetryTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTransfer, afterRetryTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTransferFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-B")))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT")
                .setDescription("idempotent transfer with business context"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTransfer, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTransferFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstTransferSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_CONTEXT", 2, 2);
    }

    /**
     * 场景：直接退款使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、付款 70、退款 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次退款后的状态。
     * 预期：退款幂等必须同时保护退款到账账户、退款出资账户、出资账目、金额和 route replay 摘要。
     * 红线：同业务流水不同退款请求不得重复返还、超额扣减或污染账务事实。
     */
    @Test
    void testDirectRefundSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_refund_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_IDEMPOTENT_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstRefundSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND")
                .setDescription("idempotent refund"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFirstRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstRefundFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_REFUND");

        String retryRefundSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND")
                .setDescription("idempotent refund"), WindOperatorFactory.system());

        assertThat(retryRefundSn).isEqualTo(firstRefundSn);
        BalanceSnapshot afterRetryRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstRefund, afterRetryRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_REFUND", firstRouteSnapshot);
        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(31L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND")
                .setDescription("idempotent refund"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryRefund, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_REFUND", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.REFUND.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_PAY", 2, 2);
        assertThat(fundsTransactionDetails(firstRefundSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND", 2, 2);
    }

    /**
     * 场景：直接退款使用相同业务流水重复提交，第二次请求更换非易变业务上下文字段。
     * 输入：充值 100、付款 70、退款 30 使用业务流水 `DIRECT_IDEMPOTENT_REFUND_CONTEXT`，首请求 context 为 RULE-A，重试同 context 后改为 RULE-B。
     * 输出：同 context 重试返回同一资金交易流水；业务上下文变化被摘要冲突拒绝。
     * 预期：退款幂等摘要必须覆盖非易变 contextVariables 字段，不能只覆盖退款账户、出资账户、出资账目和金额。
     * 红线：同业务流水不同业务上下文不得静默复用原退款，也不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectRefundSameBusinessSnWithDifferentBusinessContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_refund_ctx_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_REFUND_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_IDEMPOTENT_REFUND_CONTEXT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstRefundSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND_CONTEXT")
                .setDescription("idempotent refund with business context"), WindOperatorFactory.system());
        BalanceSnapshot afterFirstRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFirstRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstRefundFacts = ledgerFactSnapshot();
        RouteSnapshotSpec firstRouteSnapshot = routeSnapshot("DIRECT_IDEMPOTENT_REFUND_CONTEXT");

        String retryRefundSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND_CONTEXT")
                .setDescription("idempotent refund with business context"), WindOperatorFactory.system());

        assertThat(retryRefundSn).isEqualTo(firstRefundSn);
        BalanceSnapshot afterRetryRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstRefund, afterRetryRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_REFUND_CONTEXT", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-B")))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND_CONTEXT")
                .setDescription("idempotent refund with business context"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryRefund, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_REFUND_CONTEXT", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstRefundSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_CONTEXT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_CONTEXT_PAY", 2, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_CONTEXT", 2, 2);
    }

    @Override
    protected void assertSingleFundsAndLedgerFactsForBusinessSn(String businessSn, int expectedDetails,
                                                                int expectedEntries) {
        super.assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, expectedDetails, expectedEntries);
        assertLedgerFactsFollowRouteSnapshot(businessSn);
        assertDirectPostingPlansUseRouteSnapshotLegs(businessSn);
        assertDirectRouteSnapshotCarriesMetadata(businessSn);
        assertDirectRouteSnapshotKeepsContextMinimal(businessSn);
        assertDirectTransactionKeepsContextMinimal(businessSn);
        assertDirectFactsShareBusinessScene(businessSn);
        assertDirectFactsShareTransactionIdentity(businessSn);
        assertDirectDetailsFollowRouteParticipants(businessSn);
        assertDirectDetailsKeepRequestFactsOutOfContext(businessSn);
        assertDirectEntriesFollowPostingPlans(businessSn);
        assertDirectLedgerTransactionKeepsContextMinimal(businessSn);
        assertDirectLedgerContextsKeepPostingEvidenceOnly(businessSn);
        assertDirectFactsCarryAuditTrail(businessSn);
        assertDirectBalancesMatchLedgerEntries();
    }

    @Override
    protected void assertFailedFundsTransactionWithoutLedgerFacts(String businessSn) {
        super.assertFailedFundsTransactionWithoutLedgerFacts(businessSn);
        assertFailedDirectFactsCarryIdentityAndAudit(businessSn);
        assertDirectRouteSnapshotCarriesMetadata(businessSn);
        assertDirectRouteSnapshotKeepsContextMinimal(businessSn);
        assertDirectTransactionKeepsContextMinimal(businessSn);
        assertDirectDetailsFollowRouteParticipants(businessSn);
        assertDirectDetailsKeepRequestFactsOutOfContext(businessSn);
        assertDirectBalancesMatchLedgerEntries();
    }

    @Override
    protected void assertNoFundsOrLedgerFactsForBusinessSn(String businessSn) {
        super.assertNoFundsOrLedgerFactsForBusinessSn(businessSn);
        assertDirectBalancesMatchLedgerEntries();
    }

    private void assertDirectRouteSnapshotCarriesMetadata(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("direct route snapshot metadata for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(routeSnapshot.getSnapshotId()).isEqualTo(businessSn + "_ROUTE");
                    assertThat(routeSnapshot.getSnapshotSchemaVersion())
                            .isEqualTo(FundsRouteCodes.CURRENT_ROUTE_SNAPSHOT_SCHEMA_VERSION);
                    assertThat(routeSnapshot.getRouteCode()).isEqualTo(expectedDirectRouteCode(transaction));
                    assertThat(routeSnapshot.getRouteVersion()).isEqualTo(FundsRouteCodes.CURRENT_ROUTE_VERSION);
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
                    assertThat(routeSnapshot.getResolvedAt()).isNotNull();
                });
    }

    private void assertDirectRouteSnapshotUnchanged(String businessSn, RouteSnapshotSpec expectedRouteSnapshot) {
        assertThat(routeSnapshot(businessSn))
                .as("direct route snapshot must not be rewritten for idempotent businessSn %s", businessSn)
                .isEqualTo(expectedRouteSnapshot);
        assertDirectRouteSnapshotCarriesMetadata(businessSn);
        assertDirectRouteSnapshotKeepsContextMinimal(businessSn);
    }

    private void assertDirectRouteSnapshotKeepsContextMinimal(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("direct route snapshot context for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getContextVariables())
                        .as("direct route snapshot must not carry request context variables for %s", businessSn)
                        .isEmpty());
    }

    private RouteSnapshotSpec routeSnapshot(String businessSn) {
        String transactionSn = fundsTransactionsByBusinessSn(businessSn).getFirst().getSn();
        return fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transactionSn)
                .orElseThrow(() -> new AssertionError("missing route snapshot for businessSn " + businessSn));
    }

    private void assertDirectTransactionKeepsContextMinimal(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        Map<String, Object> transactionContext = contextVariablesOf(transaction.getContextVariables());

        assertThat(transactionContext.keySet())
                .as("direct transaction context must not carry request context for %s", businessSn)
                .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);
    }

    private String expectedDirectRouteCode(FundsTransaction transaction) {
        if (transaction.getTransactionType() == DefaultFundsTransactionType.REFUND
                && transaction.getReferenceTransactionSn() != null) {
            return FundsRouteCodes.DIRECT_REFUND_REPLAY;
        }
        return switch (transaction.getTransactionType()) {
            case TOPUP -> FundsRouteCodes.TOPUP_STANDARD;
            case TRANSFER -> FundsRouteCodes.INTERNAL_TRANSFER_STANDARD;
            case PAY -> FundsRouteCodes.DIRECT_PAY_STANDARD;
            case REFUND -> FundsRouteCodes.DIRECT_REFUND_STANDARD;
            default -> throw new AssertionError("unsupported direct transaction type: "
                    + transaction.getTransactionType());
        };
    }

    private void assertReferenceRefundFacts(String businessSn,
                                            String refundTransactionSn,
                                            String payTransactionSn,
                                            long refundAmount) {
        assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, 2, 2);
        assertThat(refundTransactionSn).isNotEqualTo(payTransactionSn);
        assertThat(fundsTransaction(refundTransactionSn))
                .as("referenced direct refund transaction")
                .satisfies(transaction -> {
                    assertThat(transaction.getBusinessSn()).isEqualTo(businessSn);
                    assertThat(transaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
                    assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
                    assertThat(transaction.getReferenceTransactionSn()).isEqualTo(payTransactionSn);
                    assertThat(transaction.getRefundedAmount()).isEqualTo(refundAmount);
                });
        assertThat(fundsTransaction(payTransactionSn).getRefundedAmount())
                .as("original direct pay refunded amount")
                .isEqualTo(refundAmount);
        RouteSnapshotSpec payRouteSnapshot = fundsTransactionQueryService
                .findRouteSnapshotByTransactionSn(payTransactionSn)
                .orElseThrow();
        assertThat(payRouteSnapshot.getLegs()).singleElement();
        RouteSnapshotSpec refundRouteSnapshot = routeSnapshot(businessSn);
        assertThat(refundRouteSnapshot.getRouteCode()).isEqualTo(FundsRouteCodes.DIRECT_REFUND_REPLAY);
        assertThat(refundRouteSnapshot.getLegs()).singleElement().satisfies(refundLeg -> {
            String sourceLegId = payRouteSnapshot.getLegs().getFirst().getLegId();
            assertThat(refundLeg.getReplayRefLegId()).isEqualTo(sourceLegId);
            assertThat(fundsTransactionQueryService.sumConsumedReplayLegAmount(payTransactionSn,
                    FundsTransactionEventType.REFUND, sourceLegId, refundLeg.getAmount().getCurrency()).getAmount())
                    .isEqualTo(refundAmount);
        });
        LedgerTransaction refundLedgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        assertThat(ledgerTransactionsByFundsTransactionSn(payTransactionSn)).singleElement().satisfies(
                payLedgerTransaction -> {
                    assertThat(refundLedgerTransaction.getReferenceLedgerTransactionSn())
                            .isEqualTo(payLedgerTransaction.getSn());
                    assertThat(fundsTransactionDetailsByBusinessSn(businessSn)).allSatisfy(detail ->
                            assertThat(detail.getReferenceLedgerTransactionSn())
                                    .isEqualTo(payLedgerTransaction.getSn()));
                });
        assertThat(refundLedgerTransaction.getFundsTransactionSn()).isEqualTo(refundTransactionSn);
        assertThat(postingPlansOf(refundLedgerTransaction)).singleElement().satisfies(plan -> {
            assertThat(plan.getRouteLegId()).isEqualTo(refundRouteSnapshot.getLegs().getFirst().getLegId());
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.REFUND.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        });
        assertThat(entriesOf(refundLedgerTransaction)).allSatisfy(entry -> {
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.REFUND.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        });
        assertLedgerFactsFollowRouteSnapshot(businessSn);
    }

    private FundsTransactionTopupRequest core1bTopupRequest(long amount) {
        return new FundsTransactionTopupRequest()
                .setAccountId(fundingAccount("funding_user"))
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_core1b_legacy",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("DIRECT_CORE1B_LEGACY_TOPUP_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TOPUP")
                .setBusinessSn(CORE1B_BUSINESS_SN)
                .setDescription("core1b legacy topup");
    }

    private void replaceDetailDigest(String subjectId, RouteParticipantRole participantRole) {
        assertThat(core1bJdbcTemplate.update("""
                        UPDATE t_funds_transaction_detail
                        SET request_hash = ?
                        WHERE tenant_id = ?
                          AND business_sn = ?
                          AND subject_id = ?
                          AND participant_role = ?
                        """,
                CORE1B_LEGACY_DETAIL_DIGESTS.get(subjectId),
                TENANT_ID,
                CORE1B_BUSINESS_SN,
                subjectId,
                participantRole.name())).isEqualTo(1);
    }

    private void replaceRouteSnapshotWithLegacyAccountingFields() {
        String routeSnapshotJson = core1bJdbcTemplate.queryForObject("""
                SELECT route_snapshot
                FROM t_funds_transaction
                WHERE tenant_id = ? AND business_sn = ?
                """, String.class, TENANT_ID, CORE1B_BUSINESS_SN);
        ObjectNode routeSnapshot = WindJson.parseObject(routeSnapshotJson, ObjectNode.class);
        ArrayNode legs = (ArrayNode) routeSnapshot.get("legs");
        for (JsonNode value : legs) {
            ObjectNode leg = (ObjectNode) value;
            String legId = leg.get("legId").asText();
            ObjectNode sourceNode = (ObjectNode) leg.get("sourceNode");
            ObjectNode targetNode = (ObjectNode) leg.get("targetNode");
            if ("FUND_IN".equals(legId)) {
                sourceNode.put("ledgerSubjectCode", LedgerSubjectCode.CASH.name());
                targetNode.put("ledgerSubjectCode", LedgerSubjectCode.PREPAYMENT.name());
                leg.put("phaseCode", LedgerPhaseCode.FUND_IN.name());
            } else {
                sourceNode.put("ledgerSubjectCode", LedgerSubjectCode.PREPAYMENT.name());
                targetNode.put("ledgerSubjectCode", LedgerSubjectCode.AVAILABLE.name());
                leg.put("phaseCode", LedgerPhaseCode.SETTLEMENT.name());
            }
            leg.put("balanceEffectType", "INCREASE");
            leg.put("periodType", "LIFETIME");
            leg.put("periodId", "LIFETIME");
            leg.set("constraintOverrides", WindJson.parseObject("{}", ObjectNode.class));
        }
        assertThat(core1bJdbcTemplate.update("""
                        UPDATE t_funds_transaction
                        SET route_snapshot = ?
                        WHERE tenant_id = ? AND business_sn = ?
                        """,
                WindJson.toJsonString(routeSnapshot),
                TENANT_ID,
                CORE1B_BUSINESS_SN)).isEqualTo(1);
    }

    private Map<String, String> detailRequestHashes(String businessSn) {
        Map<String, String> result = new LinkedHashMap<>();
        core1bJdbcTemplate.queryForList("""
                        SELECT subject_id, request_hash
                        FROM t_funds_transaction_detail
                        WHERE tenant_id = ? AND business_sn = ?
                        ORDER BY id ASC
                        """, TENANT_ID, businessSn)
                .forEach(row -> result.put((String) row.get("SUBJECT_ID"), (String) row.get("REQUEST_HASH")));
        return Map.copyOf(result);
    }

    private PersistedTopupFacts persistedTopupFacts() {
        List<Map<String, Object>> transactions = core1bJdbcTemplate.queryForList("""
                SELECT *
                FROM t_funds_transaction
                WHERE tenant_id = ? AND business_sn = ?
                ORDER BY id ASC
                """, TENANT_ID, CORE1B_BUSINESS_SN);
        List<Map<String, Object>> details = core1bJdbcTemplate.queryForList("""
                SELECT *
                FROM t_funds_transaction_detail
                WHERE tenant_id = ? AND business_sn = ?
                ORDER BY id ASC
                """, TENANT_ID, CORE1B_BUSINESS_SN);
        String routeSnapshotJson = core1bJdbcTemplate.queryForObject("""
                SELECT route_snapshot
                FROM t_funds_transaction
                WHERE tenant_id = ? AND business_sn = ?
                """, String.class, TENANT_ID, CORE1B_BUSINESS_SN);
        return new PersistedTopupFacts(transactions, details, routeSnapshotJson);
    }

    private record PersistedTopupFacts(List<Map<String, Object>> transactions,
                                       List<Map<String, Object>> details,
                                       String routeSnapshotJson) {

        private PersistedTopupFacts {
            transactions = List.copyOf(transactions);
            details = List.copyOf(details);
        }
    }

    private Map<String, Object> paymentInstrumentSnapshot(String instrumentId,
                                                          String bindingId,
                                                          String bindingVersion) {
        Map<String, Object> bindingSnapshot = new LinkedHashMap<>();
        bindingSnapshot.put("bindingId", bindingId);
        bindingSnapshot.put("bindingVersion", bindingVersion);
        bindingSnapshot.put("bindingStatus", "ACTIVE");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("instrumentId", instrumentId);
        result.put("instrumentType", "CARD");
        result.put("instrumentNo", "**** 4242");
        result.put("ownerId", "funding_user");
        result.put("ownerType", "USER");
        result.put("tenantId", TENANT_ID);
        result.put("currency", CURRENCY.name());
        result.put("status", "ACTIVE");
        result.put("bindingSnapshot", bindingSnapshot);
        result.put("description", "historical payment instrument snapshot");
        return result;
    }

    private Map<String, Object> routingDecisionSnapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policyCode", "HISTORICAL_ROUTE_DECISION");
        result.put("matchedRules", List.of("RULE-OLD-BINDING"));
        result.put("selectedProcessor", "processor-old");
        result.put("selectedCashFundingAccount", null);
        result.put("selectedPlatformAccount", null);
        result.put("decisionReason", "historical route decision snapshot");
        result.put("contextVariables", Map.of("decisionVersion", "v1"));
        return result;
    }

    private void assertReferencedRefundRouteSnapshotKeepsHistoricalAttribution(String businessSn) {
        assertThat(routeSnapshot(businessSn))
                .as("referenced refund route snapshot must keep historical instrument and funding attribution")
                .satisfies(routeSnapshot -> {
                    assertThat(routeSnapshot.getPaymentInstrumentRef().getInstrumentId()).isEqualTo("CARD-OLD");
                    assertThat(routeSnapshot.getPaymentInstrumentRef().getBindingSnapshot())
                            .containsEntry("bindingId", "BINDING-OLD")
                            .containsEntry("bindingVersion", "v1");
                    assertThat(routeSnapshot.getRoutingDecision().getPolicyCode())
                            .isEqualTo("HISTORICAL_ROUTE_DECISION");
                    assertThat(routeSnapshot.getRoutingDecision().getContextVariables())
                            .containsEntry("decisionVersion", "v1");
                    assertThat(routeSnapshot.getContextVariables())
                            .doesNotContainEntry("businessContextVersion", "CURRENT-BINDING-RULE-V2");
                });
    }

    private void assertFailedDirectFactsCarryIdentityAndAudit(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(businessSn);

        assertThat(transaction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(transaction.getGmtCreate()).isNotNull();
        assertThat(transaction.getGmtModified()).isAfterOrEqualTo(transaction.getGmtCreate());
        assertThat(details)
                .as("failed direct details must carry identity and audit fields for %s", businessSn)
                .allSatisfy(detail -> {
                    assertThat(detail.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(detail.getTransactionSn()).isEqualTo(transaction.getSn());
                    assertThat(detail.getBusinessScene()).isEqualTo(transaction.getBusinessScene());
                    assertThat(detail.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
                    assertThat(detail.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    if (detail.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER) {
                        assertThat(detail.getAmount()).isEqualTo(transaction.getAmount());
                    }
                    assertThat(detail.getCurrency()).isEqualTo(transaction.getCurrency());
                    assertThat(detail.getGmtCreate()).isNotNull();
                    assertThat(detail.getGmtModified()).isAfterOrEqualTo(detail.getGmtCreate());
                    assertThat(detail.getRequestHash()).isNotBlank();
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("failed direct route snapshot identity must follow transaction for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(routeSnapshot.getBusinessScene()).isEqualTo(transaction.getBusinessScene());
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
                    assertThat(routeSnapshot.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(routeSnapshot.getResolvedAt()).isNotNull();
                    assertThat(details)
                            .as("failed direct details must share route event type for %s", businessSn)
                            .extracting(FundsTransactionDetail::getEventType)
                            .containsOnly(routeSnapshot.getEventType());
                });
    }

    private void assertDirectPostingPlansUseRouteSnapshotLegs(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<String> postingRouteLegIds = postingPlansOf(ledgerTransaction).stream()
                .map(LedgerPostingPlan::getRouteLegId)
                .toList();

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot for direct transaction %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(businessSn);
                    assertThat(routeSnapshot.getLegs())
                            .as("route snapshot legs for direct transaction %s", businessSn)
                            .isNotEmpty();
                    assertThat(postingRouteLegIds)
                            .as("posting routeLegId must come from route snapshot for direct transaction %s",
                                    businessSn)
                            .containsExactlyInAnyOrderElementsOf(routeSnapshot.getLegs().stream()
                                    .map(RouteLegSpec::getLegId)
                                    .toList());
                    postingPlansOf(ledgerTransaction).forEach(plan -> {
                        RouteLegSpec routeLeg = directRouteLegById(routeSnapshot.getLegs(), plan.getRouteLegId());
                        assertThat(plan.getAmount())
                                .as("posting amount must follow route leg for direct transaction %s", businessSn)
                                .isEqualTo(routeLeg.getAmount().getAmount());
                        assertThat(plan.getCurrency())
                                .as("posting currency must follow route leg for direct transaction %s", businessSn)
                                .isEqualTo(routeLeg.getAmount().getCurrency());
                    });
                });
    }

    private RouteLegSpec directRouteLegById(List<RouteLegSpec> routeLegs, String routeLegId) {
        return routeLegs.stream()
                .filter(routeLeg -> routeLeg.getLegId().equals(routeLegId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("route leg not found: " + routeLegId));
    }

    private void assertDirectDetailsFollowRouteParticipants(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(businessSn);

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot participants must explain funds transaction details for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertThat(details.stream()
                        .map(DirectRouteParticipantKey::from)
                        .toList())
                        .containsExactlyInAnyOrderElementsOf(routeSnapshot.getParticipants().stream()
                                .map(DirectRouteParticipantKey::from)
                                .toList()));
    }

    private void assertDirectDetailsKeepRequestFactsOutOfContext(String businessSn) {
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("direct detail contexts for %s", businessSn)
                .isNotEmpty()
                .allSatisfy(detail -> assertThat(contextVariablesOf(detail.getContextVariables()).keySet())
                        .as("direct detail context must not carry request context variables for %s", businessSn)
                        .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS));
    }

    private void assertDirectEntriesFollowPostingPlans(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<LedgerEntry> entries = entriesOf(ledgerTransaction);

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot for direct transaction %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> postingPlansOf(ledgerTransaction).forEach(plan -> {
                    RouteLegSpec routeLeg = directRouteLegById(routeSnapshot.getLegs(), plan.getRouteLegId());
                    List<LedgerEntry> planEntries = entries.stream()
                            .filter(entry -> plan.getSn().equals(entry.getPostingPlanSn()))
                            .toList();

                    assertThat(planEntries)
                            .as("ledger entries must follow posting plan for direct transaction %s", businessSn)
                            .hasSize(2);
                    assertThat(planEntries.stream()
                            .map(DirectRouteNodeKey::from)
                            .toList())
                            .as("ledger entries must follow route leg nodes and sides for direct transaction %s",
                                    businessSn)
                            .containsExactlyInAnyOrder(
                                    DirectRouteNodeKey.from(routeLeg.getSourceNode(), EntrySide.DEBIT),
                                    DirectRouteNodeKey.from(routeLeg.getTargetNode(), EntrySide.CREDIT));
                    assertThat(planEntries).allSatisfy(entry -> {
                        assertThat(entry.getIntent()).isEqualTo(plan.getIntent());
                        assertThat(entry.getPostingScope()).isEqualTo(plan.getPostingScope());
                        assertThat(entry.getBalanceEffectType()).isEqualTo(plan.getBalanceEffectType());
                        assertThat(entry.getPhaseCode()).isEqualTo(plan.getPhaseCode());
                        assertThat(entry.getAmount()).isEqualTo(plan.getAmount());
                        assertThat(entry.getCurrency()).isEqualTo(plan.getCurrency());
                        assertThat(entry.getOriginalAmount()).isEqualTo(routeLeg.getOriginalAmount().getAmount());
                        assertThat(entry.getOriginalCurrency()).isEqualTo(routeLeg.getOriginalAmount().getCurrency());
                        assertThat(entry.getExchangeRate()).isEqualByComparingTo(routeLeg.getExchangeRate());
                    });
                }));
    }

    private void assertDirectLedgerContextsKeepPostingEvidenceOnly(String businessSn) {
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<LedgerEntry> entries = entriesOf(ledgerTransaction);

        postingPlansOf(ledgerTransaction).forEach(plan -> {
            Map<String, Object> planContext = contextVariablesOf(plan.getContextVariables());
            assertThat(planContext)
                    .as("posting plan context must retain route evidence for direct transaction %s", businessSn)
                    .containsEntry("routeLegId", plan.getRouteLegId())
                    .containsKey("replayPolicy");
            assertThat(planContext.keySet())
                    .as("posting plan context must not carry request context for direct transaction %s", businessSn)
                    .isSubsetOf(DIRECT_LEDGER_CONTEXT_KEYS)
                    .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);

            List<LedgerEntry> planEntries = entries.stream()
                    .filter(entry -> plan.getSn().equals(entry.getPostingPlanSn()))
                    .toList();
            assertThat(planEntries)
                    .as("posting entries must exist for direct transaction %s", businessSn)
                    .isNotEmpty()
                    .allSatisfy(entry -> assertLedgerEntryContextKeepsPostingEvidenceOnly(
                            businessSn, plan, entry));
        });
    }

    private void assertDirectLedgerTransactionKeepsContextMinimal(String businessSn) {
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        Map<String, Object> transactionContext = contextVariablesOf(ledgerTransaction.getContextVariables());

        assertThat(transactionContext.keySet())
                .as("direct ledger transaction context must not carry request context for %s", businessSn)
                .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);
    }

    private void assertLedgerEntryContextKeepsPostingEvidenceOnly(String businessSn,
                                                                  LedgerPostingPlan plan,
                                                                  LedgerEntry entry) {
        Map<String, Object> entryContext = contextVariablesOf(entry.getContextVariables());
        assertThat(entryContext)
                .as("ledger entry context must retain route evidence for direct transaction %s", businessSn)
                .containsEntry("routeLegId", plan.getRouteLegId())
                .containsKey("replayPolicy");
        assertThat(entryContext.keySet())
                .as("ledger entry context must not carry request context for direct transaction %s", businessSn)
                .isSubsetOf(DIRECT_LEDGER_CONTEXT_KEYS)
                .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);
    }

    private Map<String, Object> contextVariablesOf(String contextVariables) {
        if (contextVariables == null || contextVariables.isBlank()) {
            return Map.of();
        }
        return WindJson.parseObject(contextVariables, new TypeReference<>() {
        });
    }

    private void assertDirectFactsCarryAuditTrail(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);

        assertThat(transaction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(transaction.getGmtCreate()).isNotNull();
        assertThat(transaction.getGmtModified()).isAfterOrEqualTo(transaction.getGmtCreate());
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction details must carry audit fields for %s", businessSn)
                .allSatisfy(detail -> {
                    assertThat(detail.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(detail.getGmtCreate()).isNotNull();
                    assertThat(detail.getGmtModified()).isAfterOrEqualTo(detail.getGmtCreate());
                    assertThat(detail.getRequestHash()).isNotBlank();
                });
        assertThat(ledgerTransaction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(ledgerTransaction.getGmtCreate()).isNotNull();
        assertThat(ledgerTransaction.getGmtModified()).isAfterOrEqualTo(ledgerTransaction.getGmtCreate());
        assertThat(ledgerTransaction.getTransactionTime()).isNotNull();
        assertThat(ledgerTransaction.getSha256()).isNotBlank();
        assertThat(postingPlansOf(ledgerTransaction))
                .as("posting plans must carry audit fields for direct transaction %s", businessSn)
                .allSatisfy(plan -> {
                    assertThat(plan.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(plan.getGmtCreate()).isNotNull();
                    assertThat(plan.getGmtModified()).isAfterOrEqualTo(plan.getGmtCreate());
                    assertThat(plan.getSha256()).isNotBlank();
                });
        assertThat(entriesOf(ledgerTransaction))
                .as("ledger entries must carry audit fields for direct transaction %s", businessSn)
                .allSatisfy(entry -> {
                    assertThat(entry.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(entry.getGmtCreate()).isNotNull();
                    assertThat(entry.getGmtModified()).isAfterOrEqualTo(entry.getGmtCreate());
                    assertThat(entry.getTransactionTime()).isEqualTo(ledgerTransaction.getTransactionTime());
                    assertThat(entry.getSha256()).isNotBlank();
                });
    }

    private void assertDirectBalancesMatchLedgerEntries() {
        Map<DirectBalanceKey, Long> deltas = new LinkedHashMap<>();
        entries().forEach(entry -> deltas.merge(DirectBalanceKey.from(entry), signedEntryAmount(entry), Long::sum));

        deltas.forEach((key, amountDelta) -> assertBucket(balance(key.accountId()), key.ledgerSubjectCode(),
                initialBalance(key) + amountDelta, key.currency()));
    }

    private long signedEntryAmount(LedgerEntry entry) {
        return entry.getEntrySide() == EntrySide.CREDIT ? entry.getAmount() : -entry.getAmount();
    }

    private long initialBalance(DirectBalanceKey key) {
        if (cashMappingAccount().id().equals(key.subjectId()) && key.ledgerSubjectCode() == LedgerSubjectCode.CASH) {
            return 10_000L;
        }
        return 0L;
    }

    private void assertDirectFactsShareBusinessScene(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);

        assertThat(ledgerTransaction.getBusinessScene())
                .as("ledger transaction businessScene must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getBusinessScene());
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction detail businessScene must follow funds transaction for %s", businessSn)
                .extracting(FundsTransactionDetail::getBusinessScene)
                .containsOnly(transaction.getBusinessScene());
        assertThat(entriesOf(ledgerTransaction))
                .as("ledger entry businessScene must follow funds transaction for %s", businessSn)
                .extracting(LedgerEntry::getBusinessScene)
                .containsOnly(transaction.getBusinessScene());
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot businessScene must follow funds transaction for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getBusinessScene())
                        .isEqualTo(transaction.getBusinessScene()));
    }

    private void assertDirectFactsShareTransactionIdentity(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);

        assertThat(ledgerTransaction.getFundsTransactionSn())
                .as("ledger transaction must point to funds transaction for %s", businessSn)
                .isEqualTo(transaction.getSn());
        assertThat(ledgerTransaction.getTransactionType())
                .as("ledger transaction type must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getTransactionType().name());
        assertThat(ledgerTransaction.getAmount())
                .as("ledger transaction amount must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getAmount());
        assertThat(ledgerTransaction.getCurrency())
                .as("ledger transaction currency must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getCurrency());
        assertThat(ledgerTransaction.getOriginalAmount())
                .as("direct ledger transaction original amount must equal amount for %s", businessSn)
                .isEqualTo(transaction.getAmount());
        assertThat(ledgerTransaction.getOriginalCurrency())
                .as("direct ledger transaction original currency must equal currency for %s", businessSn)
                .isEqualTo(transaction.getCurrency());
        assertThat(ledgerTransaction.getExchangeRate())
                .as("direct ledger transaction exchange rate must be one for %s", businessSn)
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction details must share transaction identity for %s", businessSn)
                .allSatisfy(detail -> {
                    assertThat(detail.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(detail.getEventType().name()).isEqualTo(ledgerTransaction.getEventType());
                    if (detail.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER) {
                        assertThat(detail.getAmount()).isEqualTo(transaction.getAmount());
                    }
                    assertThat(detail.getCurrency()).isEqualTo(transaction.getCurrency());
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot identity must follow funds transaction for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(routeSnapshot.getEventType().name()).isEqualTo(ledgerTransaction.getEventType());
                });
    }

    private DirectRefundRaceOutcome raceReferencedRefund(CountDownLatch ready,
                                                         CountDownLatch start,
                                                         String referenceTransactionSn,
                                                         String businessSn) {
        try {
            TenantContextHolder.setTenantId(TENANT_ID);
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).as("direct refund race start signal received").isTrue();
            String transactionSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                    .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                    .setReferenceTransactionSn(referenceTransactionSn)
                    .setBusinessScene("REFUND")
                    .setBusinessSn(businessSn)
                    .setDescription("concurrent referenced refund"), WindOperatorFactory.system());
            return DirectRefundRaceOutcome.success(businessSn, transactionSn);
        } catch (Throwable failure) {
            return DirectRefundRaceOutcome.failure(businessSn, failure);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static DirectRefundRaceOutcome awaitDirectRefundOutcome(Future<DirectRefundRaceOutcome> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(10, TimeUnit.SECONDS);
    }

    private record DirectRefundRaceOutcome(String businessSn,
                                           String transactionSn,
                                           Throwable failure) {

        private static DirectRefundRaceOutcome success(String businessSn, String transactionSn) {
            return new DirectRefundRaceOutcome(businessSn, transactionSn, null);
        }

        private static DirectRefundRaceOutcome failure(String businessSn, Throwable failure) {
            return new DirectRefundRaceOutcome(businessSn, null, failure);
        }

        private boolean succeeded() {
            return failure == null;
        }
    }

    private record DirectRouteParticipantKey(String subjectId,
                                             String subjectType,
                                             RouteParticipantRole participantRole,
                                             Long amount,
                                             CurrencyIsoCode currency) {

        private static DirectRouteParticipantKey from(RouteParticipantSpec participant) {
            Money amount = participant.getAmount();
            return new DirectRouteParticipantKey(participant.getSubjectRef().getSubjectId(),
                    participant.getSubjectRef().getSubjectType().name(), participant.getParticipantRole(),
                    amount == null ? null : amount.getAmount(), amount == null ? null : amount.getCurrency());
        }

        private static DirectRouteParticipantKey from(FundsTransactionDetail detail) {
            return new DirectRouteParticipantKey(detail.getSubjectId(), detail.getSubjectType(),
                    detail.getParticipantRole(), detail.getAmount(), detail.getCurrency());
        }
    }

    private record DirectRouteNodeKey(String subjectId,
                                      String subjectType,
                                      EntrySide entrySide) {

        private static DirectRouteNodeKey from(RouteNodeSpec node, EntrySide entrySide) {
            return new DirectRouteNodeKey(node.getSubjectRef().getSubjectId(),
                    node.getSubjectRef().getSubjectType().name(), entrySide);
        }

        private static DirectRouteNodeKey from(LedgerEntry entry) {
            return new DirectRouteNodeKey(entry.getSubjectId(), entry.getSubjectType(),
                    entry.getEntrySide());
        }
    }

    private record DirectBalanceKey(String subjectId,
                                    String subjectType,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    CurrencyIsoCode currency) {

        private static DirectBalanceKey from(LedgerEntry entry) {
            return new DirectBalanceKey(entry.getSubjectId(), entry.getSubjectType(), entry.getLedgerSubjectCode(),
                    entry.getCurrency());
        }

        private FundsAccountId accountId() {
            return FundsAccountId.immutable(subjectId, subjectType);
        }
    }
}

package com.wind.funds.transaction.application.flow;

import com.wind.common.exception.BaseException;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerState;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerStateRequest;
import com.wind.funds.transaction.model.dto.FundsActionFactDTO;
import com.wind.funds.transaction.model.dto.FundsActionFactRef;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.wind.funds.transaction.model.request.MerchantInfoRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.core.WritableContextVariables;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertSubjectBalanceNotInitialized;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static com.wind.funds.support.LedgerProjectionTestFixture.balanceEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 授权交易业务流测试。
 */
class FundsAuthorizationTransactionFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private JdbcTemplate authorizationJdbcTemplate;

    @Test
    void testAuthorizationSuccessorShouldRejectLedgerReferenceFromSuccessorFact() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "MIG05_AUTH_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "MIG05_AUTH_AUTHORIZE");
        completeAuthorization(user, 20L, authorizationSn, "MIG05_AUTH_FIRST_COMPLETE");
        assertSingleFundsAndLedgerFactsForBusinessSn("MIG05_AUTH_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("MIG05_AUTH_FIRST_COMPLETE", 0, 2, 1, 2);
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, authorizationSn))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.getLegs()).singleElement());
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);

        FundsTransactionDetail authorizationDetail = fundsTransactionDetailsByBusinessSn("MIG05_AUTH_AUTHORIZE")
                .getFirst();
        String successorLedgerTransactionSn = ledgerTransactionByBusinessSn("MIG05_AUTH_FIRST_COMPLETE").getSn();
        updateFundsTransactionDetailLedgerRef(authorizationDetail.getSn(), successorLedgerTransactionSn);
        BalanceSnapshot beforeRelease = snapshot(balances(user, settlementAccount()));
        LedgerFactSnapshot beforeReleaseFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> reverseAuthorization(user, 10L, authorizationSn,
                "MIG05_AUTH_INVALID_SUCCESSOR_REFERENCE"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("原记账计划不存在或不唯一");
        assertOnlyBalanceDeltas(beforeRelease, snapshot(balances(user, settlementAccount())),
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeReleaseFacts);
        assertThat(fundsTransactionsByBusinessSn("MIG05_AUTH_INVALID_SUCCESSOR_REFERENCE")).isEmpty();
        assertThat(fundsTransactionDetailsByBusinessSn("MIG05_AUTH_INVALID_SUCCESSOR_REFERENCE"))
                .isNotEmpty()
                .allSatisfy(detail -> {
                    assertThat(detail.getTransactionSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.FAILED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                    assertThat(detail.getErrorCode()).isNotBlank();
                    assertThat(detail.getErrorMessage()).contains("原记账计划不存在或不唯一");
                });
        assertThat(ledgerTransactionsForBusinessSn("MIG05_AUTH_INVALID_SUCCESSOR_REFERENCE")).isEmpty();
        assertThat(entriesByBusinessSn("MIG05_AUTH_INVALID_SUCCESSOR_REFERENCE")).isEmpty();
        assertThat(fundsTransactionsByBusinessSn("MIG05_AUTH_AUTHORIZE")).singleElement();
        assertThat(fundsTransactionDetailsByBusinessSn("MIG05_AUTH_AUTHORIZE"))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.SUCCEEDED);
                    assertThat(detail.getLedgerTransactionSn()).isEqualTo(successorLedgerTransactionSn);
                });
        assertThat(ledgerTransactionsForBusinessSn("MIG05_AUTH_AUTHORIZE")).singleElement();
        assertThat(entriesByBusinessSn("MIG05_AUTH_AUTHORIZE")).hasSize(2);
        assertFundsAndLedgerFactsForBusinessSn("MIG05_AUTH_FIRST_COMPLETE", 0, 2, 1, 2);
        assertThat(fundsTransaction(authorizationSn).getCompletedAmount()).isEqualTo(20L);
        assertThat(fundsTransaction(authorizationSn).getReversedAmount()).isZero();
    }

    /**
     * 场景：普通资金账户把可用资金转入授权占用，随后完成扣款。
     * 输入：充值 100、授权 60、完成 60。
     * 输出：授权期间 FUNDING_BASIC 总余额保持 100，完成后减少为 40。
     * 红线：该总余额口径不能把授权占用误写成清算或结算 pending。
     */
    @Test
    void testFundingBasicTotalBalanceShouldRemainStableWhileAuthorizationIsHeld() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "OWNED_FUNDS_BALANCE_TOPUP");

        String authorizationSn = authorize(user, 60L, true, "OWNED_FUNDS_BALANCE_AUTHORIZE");

        FundsAccountBalanceView afterAuthorize = fundsAccountQueryService.getBalance(TENANT_ID, user);
        assertThat(afterAuthorize.getAuthorizationBalance()).isEqualTo(Money.immutable(60L, CURRENCY));
        assertThat(afterAuthorize.getTotalBalance()).isEqualTo(Money.immutable(100L, CURRENCY));

        completeAuthorization(user, 60L, authorizationSn, "OWNED_FUNDS_BALANCE_COMPLETE");

        assertThat(fundsAccountQueryService.getBalance(TENANT_ID, user).getTotalBalance())
                .isEqualTo(Money.immutable(40L, CURRENCY));
    }

    /**
     * 场景：平台结算账户请求尚未定义口径的总余额。
     * 输入：FUNDING_PLATFORM / SETTLEMENT 账户。
     * 输出：拒绝不适用的汇总请求。
     * 红线：平台资产、负债、清算和结算责任不能套用 FUNDING_BASIC 汇总公式。
     */
    @Test
    void testTotalBalanceShouldRejectUndefinedProfile() {
        assertThatThrownBy(() -> fundsAccountQueryService.getBalance(
                TENANT_ID, settlementAccount()).getTotalBalance())
                .hasMessageContaining("口径尚未定义");
    }

    @Test
    void testPartialFxAuthorizationRefundShouldReuseSnapshotRate() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 325L, "FX_AUTH_REFUND_TOPUP");
        String authorizationSn = authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.converted(
                                Money.immutable(325L, CurrencyIsoCode.USD),
                                Money.immutable(1_000L, CurrencyIsoCode.KWD),
                                new BigDecimal("3.25")))
                        .setApproved(true)
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("FX_AUTH_REFUND_AUTHORIZE"), WindOperatorFactory.system());
        authorizationTransactionService.complete(new FundsAuthorizationTransactionCompleteRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.converted(
                        Money.immutable(325L, CurrencyIsoCode.USD),
                        Money.immutable(1_000L, CurrencyIsoCode.KWD),
                        new BigDecimal("3.25")))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene("AUTHORIZATION_COMPLETE")
                .setBusinessSn("FX_AUTH_REFUND_COMPLETE"), WindOperatorFactory.system());

        authorizationTransactionService.refund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.converted(
                        Money.immutable(100L, CurrencyIsoCode.USD),
                        Money.immutable(308L, CurrencyIsoCode.KWD),
                        new BigDecimal("3.25")))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene("AUTHORIZATION_REFUND")
                .setBusinessSn("FX_AUTH_REFUND_RETURN"), WindOperatorFactory.system());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("FX_AUTH_REFUND_RETURN");
        assertThat(refundTransaction.getAmount()).isEqualTo(100L);
        assertThat(refundTransaction.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(refundTransaction.getOriginalAmount()).isEqualTo(308L);
        assertThat(refundTransaction.getOriginalCurrency()).isEqualTo(CurrencyIsoCode.KWD);
        assertThat(refundTransaction.getExchangeRate()).isEqualByComparingTo("3.25");
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CurrencyIsoCode.USD);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CurrencyIsoCode.USD);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 225L, CurrencyIsoCode.USD);

        BalanceSnapshot beforeFailure = snapshot(balances(user, settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        assertThatThrownBy(() -> authorizationTransactionService.refund(
                new FundsAuthorizationTransactionRefundRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.converted(
                                Money.immutable(50L, CurrencyIsoCode.USD),
                                Money.immutable(152L, CurrencyIsoCode.KWD),
                                new BigDecimal("3.30")))
                        .setAuthorizationTransactionSn(authorizationSn)
                        .setBusinessScene("AUTHORIZATION_REFUND")
                        .setBusinessSn("FX_AUTH_REFUND_RATE_CHANGED"), WindOperatorFactory.system()))
                .hasMessageContaining("退款汇率必须与原支付快照汇率一致");

        BalanceSnapshot afterFailure = snapshot(balances(user, settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CurrencyIsoCode.USD),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CurrencyIsoCode.USD),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CurrencyIsoCode.USD));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("FX_AUTH_REFUND_RATE_CHANGED");
    }

    /**
     * 场景：风控或额度判断拒绝授权。
     * 输入：账户余额 100，授权请求 60，授权结果 approved=false。
     * 输出：记录授权拒绝交易事实和拒绝明细，余额不变，无账务路径。
     * 预期：拒绝不是授权创建，不生成 route leg、posting plan 或 LedgerEntry。
     * 红线：授权拒绝不得被当作完成后拒付，也不得写入 chargeback/declined 累计金额。
     */
    @Test
    void testAuthorizationDeclinedShouldRecordRejectedFactWithoutLedgerPosting() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_DECLINE_TOPUP");
        BalanceSnapshot beforeDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeDeclineFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(beforeTopup, beforeDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, false, "AUTH_DECLINE_AUTHORIZE");

        BalanceSnapshot afterDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeDecline, afterDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeDeclineFacts);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.REJECTED);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, authorizationSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getLegs()).isEmpty());

        assertThat(fundsTransactionDetails(authorizationSn))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                    assertThat(contextVariablesOf(detail.getContextVariables()))
                            .containsEntry(FundsInstructionContextKeys.APPROVED, false);
                });

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertNoLedgerFactsForFundsTransaction(authorizationSn);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DECLINE_TOPUP", 3, 4);
        assertThat(fundsTransactionsByBusinessSn("AUTH_DECLINE_AUTHORIZE"))
                .as("rejected funds transactions for businessSn AUTH_DECLINE_AUTHORIZE")
                .singleElement()
                .satisfies(rejectedTransaction -> {
                    assertThat(rejectedTransaction.getState()).isEqualTo(FundsTransactionState.REJECTED);
                    assertThat(rejectedTransaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
                    assertThat(rejectedTransaction.getAuthorizedAmount()).isZero();
                    assertThat(rejectedTransaction.getReversedAmount()).isZero();
                    assertThat(rejectedTransaction.getCompletedAmount()).isZero();
                    assertThat(rejectedTransaction.getRefundedAmount()).isZero();
                    assertThat(rejectedTransaction.getDeclinedAmount()).isZero();
                    assertNoLedgerFactsForFundsTransaction(rejectedTransaction.getSn());
                });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_DECLINE_AUTHORIZE"))
                .as("rejected funds transaction details for businessSn AUTH_DECLINE_AUTHORIZE")
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
                    assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.HOLD);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                });
    }

    @Test
    void testSuccessfulAuthorizationActionFactShouldRemainStableAcrossReplayAndConflict() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_ACTION_TOPUP");
        String authorizationSn = authorize(user, 60L, true, "AUTH_ACTION_AUTHORIZE");

        assertActionFactQueryBoundary("AUTHORIZATION", "AUTH_ACTION_AUTHORIZE");
        FundsActionFactDTO firstFact = assertAuthorizationActionFact(
                "AUTH_ACTION_AUTHORIZE", 60L, "succeeded", "proven-full");
        assertActionFactReferenceBoundary(firstFact, "AUTHORIZATION", "AUTH_ACTION_AUTHORIZE");

        assertThat(authorize(user, 60L, true, "AUTH_ACTION_AUTHORIZE")).isEqualTo(authorizationSn);
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_AUTHORIZE"))
                .containsExactly(firstFact);
        assertThatThrownBy(() -> authorize(user, 61L, true, "AUTH_ACTION_AUTHORIZE"))
                .hasMessageContaining("资金交易明细请求参数不一致");
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_AUTHORIZE"))
                .containsExactly(firstFact);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_ACTION_AUTHORIZE", 1, 2);
    }

    @Test
    void testRejectedAuthorizationShouldExposeProvenZeroActionFact() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_ACTION_REJECTED_TOPUP");
        BalanceSnapshot beforeReject = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeRejectFacts = ledgerFactSnapshot();

        String authorizationSn = authorize(user, 60L, false, "AUTH_ACTION_REJECTED");

        assertOnlyBalanceDeltas(beforeReject,
                snapshot(balances(user, cashMappingAccount(), settlementAccount())),
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeRejectFacts);
        assertNoLedgerFactsForFundsTransaction(authorizationSn);
        FundsActionFactDTO rejectedFact = assertAuthorizationActionFact(
                "AUTH_ACTION_REJECTED", 60L, "rejected", "proven-zero");
        assertThat(rejectedFact.getFundsEffect().getProvenMoney()).isNull();
        assertActionFactReferenceBoundary(rejectedFact, "AUTHORIZATION", "AUTH_ACTION_REJECTED");
    }

    @Test
    void testSharedAuthorizationShouldExposeOneActionFactAndFailClosedForRouteDrift() {
        FundsAccountId parentAccount = fundingAccount("auth_action_parent");
        FundsAccountId cardAccount = creditAccount("auth_action_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_ACTION_SHARED_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_ACTION_SHARED_LIMIT");
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_ACTION_SHARED");

        FundsActionFactDTO firstFact = assertAuthorizationActionFact(
                "AUTH_ACTION_SHARED", 60L, "succeeded", "proven-full");
        String routeSnapshot = fundsTransactionsByBusinessSn("AUTH_ACTION_SHARED")
                .getFirst().getRouteSnapshot();

        ObjectNode wrongHierarchy = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        for (JsonNode participant : (ArrayNode) wrongHierarchy.get("participants")) {
            if (cardAccount.id().equals(participant.path("subjectRef").path("subjectId").asText())) {
                ((ObjectNode) participant.path("accountHierarchySnapshot").path("parentAccountRef"))
                        .put("subjectId", "wrong_parent");
            }
        }
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(wrongHierarchy));
        assertNoActionFacts("AUTHORIZATION", "AUTH_ACTION_SHARED");
        assertThat(fundsTransactionQueryService.findFundsActionFact(firstFact.getIdentity())).isEmpty();
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_SHARED")).containsExactly(firstFact);

        ObjectNode missingParentCurrency = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        for (JsonNode participant : (ArrayNode) missingParentCurrency.get("participants")) {
            if (cardAccount.id().equals(participant.path("subjectRef").path("subjectId").asText())) {
                ((ObjectNode) participant.path("accountHierarchySnapshot").path("parentAccountRef"))
                        .put("currency", "");
            }
        }
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(missingParentCurrency));
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_SHARED");
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);

        ObjectNode missingFundingParticipantCurrency = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        for (JsonNode participant : (ArrayNode) missingFundingParticipantCurrency.get("participants")) {
            if (parentAccount.id().equals(participant.path("subjectRef").path("subjectId").asText())) {
                ((ObjectNode) participant).put("currency", "");
            }
        }
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(missingFundingParticipantCurrency));
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_SHARED");
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);

        ObjectNode wrongLeg = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ObjectNode firstLeg = (ObjectNode) ((ArrayNode) wrongLeg.get("legs")).get(0);
        String sourceSubjectId = firstLeg.path("sourceNode").path("subjectRef").path("subjectId").asText();
        String wrongTarget = sourceSubjectId.equals(cardAccount.id()) ? parentAccount.id() : cardAccount.id();
        ((ObjectNode) firstLeg.path("targetNode").path("subjectRef")).put("subjectId", wrongTarget);
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(wrongLeg));
        assertNoActionFacts("AUTHORIZATION", "AUTH_ACTION_SHARED");
        assertThat(fundsTransactionQueryService.findFundsActionFact(firstFact.getIdentity())).isEmpty();
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);

        FundsTransactionDetail parentDetail = fundsTransactionDetailsByBusinessSn("AUTH_ACTION_SHARED").stream()
                .filter(detail -> parentAccount.id().equals(detail.getSubjectId()))
                .findFirst()
                .orElseThrow();
        deleteFundsTransactionDetail(parentDetail.getSn());
        assertNoActionFacts("AUTHORIZATION", "AUTH_ACTION_SHARED");
        assertThat(fundsTransactionQueryService.findFundsActionFact(firstFact.getIdentity())).isEmpty();
    }

    @Test
    void testAuthorizationActionFactShouldRemainStableAcrossLifecycleProgress() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_ACTION_LIFECYCLE_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_ACTION_LIFECYCLE_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_ACTION_LIFECYCLE_AUTHORIZE", 100L, "succeeded", "proven-full");

        completeAuthorization(user, 40L, authorizationSn, "AUTH_ACTION_LIFECYCLE_COMPLETE_1");
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_LIFECYCLE_AUTHORIZE"))
                .containsExactly(authorizationFact);
        reverseAuthorization(user, 20L, authorizationSn, "AUTH_ACTION_LIFECYCLE_RELEASE");
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_LIFECYCLE_AUTHORIZE"))
                .containsExactly(authorizationFact);
        completeAuthorization(user, 40L, authorizationSn, "AUTH_ACTION_LIFECYCLE_COMPLETE_2");
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_LIFECYCLE_AUTHORIZE"))
                .containsExactly(authorizationFact);
        authorizationTransactionService.refund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(20L, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene("AUTHORIZATION_REFUND")
                .setBusinessSn("AUTH_ACTION_LIFECYCLE_REFUND"), WindOperatorFactory.system());
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_LIFECYCLE_AUTHORIZE"))
                .containsExactly(authorizationFact);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);

        FundsTransaction lifecycle = fundsTransactionsByBusinessSn("AUTH_ACTION_LIFECYCLE_AUTHORIZE").getFirst();
        updateFundsTransactionState(authorizationSn, FundsTransactionState.FAILED);
        assertAuthorizationActionFactAvailable(authorizationFact, "AUTH_ACTION_LIFECYCLE_AUTHORIZE");
        updateFundsTransactionState(authorizationSn, lifecycle.getState());

        updateFundsTransactionCompletedAmount(authorizationSn, lifecycle.getAuthorizedAmount() + 1L);
        assertAuthorizationActionFactAvailable(authorizationFact, "AUTH_ACTION_LIFECYCLE_AUTHORIZE");
        updateFundsTransactionCompletedAmount(authorizationSn, lifecycle.getCompletedAmount());

        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction SET reversed_amount = ? WHERE tenant_id = ? AND sn = ?
                """, lifecycle.getAuthorizedAmount() + 1L, TENANT_ID, authorizationSn)).isOne();
        assertAuthorizationActionFactAvailable(authorizationFact, "AUTH_ACTION_LIFECYCLE_AUTHORIZE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction SET reversed_amount = ? WHERE tenant_id = ? AND sn = ?
                """, lifecycle.getReversedAmount(), TENANT_ID, authorizationSn)).isOne();

        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction SET refunded_amount = ? WHERE tenant_id = ? AND sn = ?
                """, lifecycle.getAuthorizedAmount() + 1L, TENANT_ID, authorizationSn)).isOne();
        assertAuthorizationActionFactAvailable(authorizationFact, "AUTH_ACTION_LIFECYCLE_AUTHORIZE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction SET refunded_amount = ? WHERE tenant_id = ? AND sn = ?
                """, lifecycle.getRefundedAmount(), TENANT_ID, authorizationSn)).isOne();
    }

    @Test
    void testAuthorizationActionFactShouldFailClosedForDurableFactTamper() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_ACTION_TAMPER_TOPUP");
        String authorizationSn = authorize(user, 60L, true, "AUTH_ACTION_TAMPER");
        FundsActionFactDTO firstFact = assertAuthorizationActionFact(
                "AUTH_ACTION_TAMPER", 60L, "succeeded", "proven-full");
        FundsTransactionDetail detail = fundsTransactionDetailsByBusinessSn("AUTH_ACTION_TAMPER").getFirst();
        String ledgerTransactionSn = detail.getLedgerTransactionSn();
        String routeSnapshot = fundsTransactionsByBusinessSn("AUTH_ACTION_TAMPER").getFirst().getRouteSnapshot();

        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction SET authorized_amount = ? WHERE tenant_id = ? AND sn = ?
                """, 61L, TENANT_ID, authorizationSn)).isOne();
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction SET authorized_amount = ? WHERE tenant_id = ? AND sn = ?
                """, 60L, TENANT_ID, authorizationSn)).isOne();

        updateFundsTransactionDetailState(detail.getSn(), FundsTransactionDetailState.PROCESSING);
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");
        updateFundsTransactionDetailState(detail.getSn(), FundsTransactionDetailState.SUCCEEDED);

        updateFundsTransactionDetailLedgerRef(detail.getSn(), null);
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");
        updateFundsTransactionDetailLedgerRef(detail.getSn(), ledgerTransactionSn);

        ObjectNode missingLegs = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        missingLegs.putArray("legs");
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(missingLegs));
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");

        clearFundsTransactionRouteSnapshot(authorizationSn);
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);
        assertThat(actionFactsByBusiness("AUTHORIZATION", "AUTH_ACTION_TAMPER")).containsExactly(firstFact);

        ObjectNode missingParticipantSubject = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ((ObjectNode) ((ArrayNode) missingParticipantSubject.get("participants")).get(0)).remove("subjectRef");
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(missingParticipantSubject));
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);

        ObjectNode wrongFundingResponsibility = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ObjectNode participant = (ObjectNode) ((ArrayNode) wrongFundingResponsibility.get("participants")).get(0);
        participant.put("participantRole", RouteParticipantRole.AUTH_HOLDER.name());
        ((ObjectNode) participant.path("subjectRef")).put("subjectType", FundsSubjectType.CREDIT_ACCOUNT.name());
        for (JsonNode leg : (ArrayNode) wrongFundingResponsibility.get("legs")) {
            ((ObjectNode) leg.path("sourceNode").path("subjectRef"))
                    .put("subjectType", FundsSubjectType.CREDIT_ACCOUNT.name());
            ((ObjectNode) leg.path("targetNode").path("subjectRef"))
                    .put("subjectType", FundsSubjectType.CREDIT_ACCOUNT.name());
        }
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(wrongFundingResponsibility));
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail
                SET subject_type = ?, participant_role = ?
                WHERE tenant_id = ? AND sn = ?
                """, FundsSubjectType.CREDIT_ACCOUNT.name(), RouteParticipantRole.AUTH_HOLDER.name(),
                TENANT_ID, detail.getSn())).isOne();
        assertAuthorizationActionFactUnavailable(firstFact, "AUTH_ACTION_TAMPER");
    }

    /**
     * 场景：普通 Funding 授权完成后投影稳定 complete ActionFact。
     * 输入：充值 100、授权 100、完成 30，并执行同摘要重放和异金额冲突。
     * 输出：完成物理事实、原账本引用和余额先闭合，再查询唯一 complete ActionFact。
     * 红线：ActionFact 缺失不得被不完整的 detail、route、ledger 或余额事实掩盖。
     */
    @Test
    void testSuccessfulAuthorizationCompleteShouldExposeStableActionFact() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_COMPLETE_ACTION_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_COMPLETE_ACTION_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_COMPLETE_ACTION_AUTHORIZE", 100L, "succeeded", "proven-full");

        String completeSn = completeAuthorization(user, 30L, authorizationSn, "AUTH_COMPLETE_ACTION_CAPTURE");
        LedgerFactSnapshot completedFacts = ledgerFactSnapshot();
        BalanceSnapshot completedBalances = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertThat(fundsTransaction(authorizationSn).getCompletedAmount()).isEqualTo(30L);
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_ACTION_AUTHORIZE",
                "AUTH_COMPLETE_ACTION_CAPTURE", 2, 1, 2);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);

        assertThat(completeAuthorization(user, 30L, authorizationSn, "AUTH_COMPLETE_ACTION_CAPTURE"))
                .isEqualTo(completeSn);
        assertLedgerTransactionFactsUnchanged(completedFacts);
        assertOnlyBalanceDeltas(completedBalances,
                snapshot(balances(user, cashMappingAccount(), settlementAccount())),
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertThatThrownBy(() -> completeAuthorization(user, 31L, authorizationSn,
                "AUTH_COMPLETE_ACTION_CAPTURE"))
                .hasMessageContaining("资金交易明细请求参数不一致");
        assertThat(fundsTransaction(authorizationSn).getCompletedAmount()).isEqualTo(30L);

        FundsActionFactDTO completeFact = assertCompleteActionFact(
                authorizationSn, authorizationFact, "AUTH_COMPLETE_ACTION_CAPTURE", 30L, 1);
        assertActionFactReferenceBoundary(completeFact,
                "AUTHORIZATION_COMPLETE", "AUTH_COMPLETE_ACTION_CAPTURE");
    }

    /**
     * 场景：同一授权根经历两次部分完成、释放和退款后保留两条独立 complete ActionFact。
     * 输入：授权 100，依次完成 30、完成 50、释放 20、退款 20。
     * 输出：每个后继动作的 detail/posting/ledger 独立闭合，累计和最终余额可复算。
     * 红线：release/refund 不得覆盖或合并两条 complete 动作事实。
     */
    @Test
    void testPartialAuthorizationCompletesShouldRemainIndependentAcrossReleaseAndRefund() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_COMPLETE_LIFECYCLE_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_COMPLETE_LIFECYCLE_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_COMPLETE_LIFECYCLE_AUTHORIZE", 100L, "succeeded", "proven-full");

        completeAuthorization(user, 30L, authorizationSn, "AUTH_COMPLETE_LIFECYCLE_CAPTURE_1");
        completeAuthorization(user, 50L, authorizationSn, "AUTH_COMPLETE_LIFECYCLE_CAPTURE_2");
        FundsActionFactDTO firstCompleteBeforeReverse = assertCompleteActionFact(
                authorizationSn, authorizationFact, "AUTH_COMPLETE_LIFECYCLE_CAPTURE_1", 30L, 1);
        FundsActionFactDTO secondCompleteBeforeReverse = assertCompleteActionFact(
                authorizationSn, authorizationFact, "AUTH_COMPLETE_LIFECYCLE_CAPTURE_2", 50L, 1);
        reverseAuthorization(user, 20L, authorizationSn, "AUTH_COMPLETE_LIFECYCLE_RELEASE");
        refundCompletedAuthorization(user, 20L, authorizationSn, "AUTH_COMPLETE_LIFECYCLE_REFUND");

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getCompletedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(20L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(20L);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_LIFECYCLE_AUTHORIZE",
                "AUTH_COMPLETE_LIFECYCLE_CAPTURE_1", 2, 1, 2);
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_LIFECYCLE_AUTHORIZE",
                "AUTH_COMPLETE_LIFECYCLE_CAPTURE_2", 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_COMPLETE_LIFECYCLE_RELEASE", 0, 1, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_COMPLETE_LIFECYCLE_REFUND", 0, 2, 1, 2);

        FundsActionFactDTO firstComplete = assertCompleteActionFact(
                authorizationSn, authorizationFact, "AUTH_COMPLETE_LIFECYCLE_CAPTURE_1", 30L, 1);
        FundsActionFactDTO secondComplete = assertCompleteActionFact(
                authorizationSn, authorizationFact, "AUTH_COMPLETE_LIFECYCLE_CAPTURE_2", 50L, 1);
        assertThat(firstComplete).isEqualTo(firstCompleteBeforeReverse);
        assertThat(secondComplete).isEqualTo(secondCompleteBeforeReverse);
        assertThat(firstComplete.getIdentity()).isNotEqualTo(secondComplete.getIdentity());
    }

    /**
     * 场景：SHARED 授权完成同时消费 Credit 与父 Funding 责任。
     * 输入：Credit 与父 Funding 各授权占用 60，随后完成 60。
     * 输出：三条 detail、两组 posting 和四条 entry 闭合，但只形成一条金额 60 的 ActionFact。
     * 红线：多责任 participant 不得把 complete Money 累加成 120。
     */
    @Test
    void testSharedAuthorizationCompleteShouldExposeOneActionFactWithoutDoublingMoney() {
        FundsAccountId parentAccount = fundingAccount("aca_parent");
        FundsAccountId cardAccount = creditAccount("aca_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_COMPLETE_ACTION_SHARED_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_COMPLETE_ACTION_SHARED_LIMIT");
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_COMPLETE_ACTION_SHARED_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_COMPLETE_ACTION_SHARED_AUTHORIZE", 60L, "succeeded", "proven-full");

        completeAuthorization(cardAccount, 60L, authorizationSn, "AUTH_COMPLETE_ACTION_SHARED_CAPTURE");

        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_ACTION_SHARED_AUTHORIZE",
                "AUTH_COMPLETE_ACTION_SHARED_CAPTURE", 3, 2, 4);
        assertBucket(balance(cardAccount), LedgerSubjectCode.OUTSTANDING, 60L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);
        assertCompleteActionFact(authorizationSn, authorizationFact,
                "AUTH_COMPLETE_ACTION_SHARED_CAPTURE", 60L, 2);
    }

    /**
     * 场景：两个不同 complete identity 并发竞争同一授权剩余额度。
     * 输入：授权 80，并发完成 60 与 60。
     * 输出：仅一个 winner 形成完整资金/账务事实，loser 零新增，余额只反映 60。
     * 红线：并发失败方不得生成 detail、ledger、余额或 ActionFact。
     */
    @Test
    void testConcurrentAuthorizationCompletesShouldExposeOnlyWinnerActionFact() throws Exception {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_COMPLETE_ACTION_RACE_TOPUP");
        String authorizationSn = authorize(user, 80L, true, "AUTH_COMPLETE_ACTION_RACE_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_COMPLETE_ACTION_RACE_AUTHORIZE", 80L, "succeeded", "proven-full");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<RaceOutcome> first = executor.submit(() -> raceCommand(ready, start,
                    "AUTH_COMPLETE_ACTION_RACE_1", FundsTransactionEventType.COMPLETE,
                    () -> completeAuthorization(user, 60L, authorizationSn, "AUTH_COMPLETE_ACTION_RACE_1")));
            Future<RaceOutcome> second = executor.submit(() -> raceCommand(ready, start,
                    "AUTH_COMPLETE_ACTION_RACE_2", FundsTransactionEventType.COMPLETE,
                    () -> completeAuthorization(user, 60L, authorizationSn, "AUTH_COMPLETE_ACTION_RACE_2")));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<RaceOutcome> outcomes = List.of(awaitOutcome(first), awaitOutcome(second));
            List<RaceOutcome> successes = outcomes.stream().filter(RaceOutcome::succeeded).toList();
            List<RaceOutcome> failures = outcomes.stream().filter(outcome -> !outcome.succeeded()).toList();
            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            RaceOutcome winner = successes.getFirst();
            RaceOutcome loser = failures.getFirst();
            assertThat(fundsTransaction(authorizationSn).getCompletedAmount()).isEqualTo(60L);
            assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_ACTION_RACE_AUTHORIZE",
                    winner.businessSn(), 2, 1, 2);
            assertNoFundsOrLedgerFactsForBusinessSn(loser.businessSn());
            assertNoActionFacts("AUTHORIZATION_COMPLETE", loser.businessSn());
            assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
            assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 20L, CURRENCY);
            assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
            assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);
            assertCompleteActionFact(authorizationSn, authorizationFact, winner.businessSn(), 60L, 1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    /**
     * 场景：完整 complete 物理事实存在时查询稳定 ActionFact，并为后续篡改用例冻结基线。
     * 输入：充值 100、授权 100、完成 30。
     * 输出：detail、route provenance、ledger 和余额闭合后，查询唯一 complete ActionFact。
     * 红线：目标红灯只能来自 ActionFact 投影缺失，不能来自完成主链不完整。
     */
    @Test
    void testAuthorizationCompleteActionFactShouldFailClosedForDurableFactTamper() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_COMPLETE_ACTION_TAMPER_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_COMPLETE_ACTION_TAMPER_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_COMPLETE_ACTION_TAMPER_AUTHORIZE", 100L, "succeeded", "proven-full");
        completeAuthorization(user, 30L, authorizationSn, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_ACTION_TAMPER_AUTHORIZE",
                "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE", 2, 1, 2);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);
        FundsActionFactDTO completeFact = assertCompleteActionFact(
                authorizationSn, authorizationFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE", 30L, 1);
        FundsTransactionDetail detail = fundsTransactionDetailsByBusinessSn(
                "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE").getFirst();
        String requestHash = detail.getRequestHash();
        String ledgerRef = detail.getLedgerTransactionSn();
        String contextVariables = detail.getContextVariables();
        String routeSnapshot = fundsTransactionsByBusinessSn(
                "AUTH_COMPLETE_ACTION_TAMPER_AUTHORIZE").getFirst().getRouteSnapshot();

        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET request_hash = '' WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, detail.getSn())).isOne();
        assertThat(actionFactsByBusiness("AUTHORIZATION_COMPLETE", "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE"))
                .containsExactly(completeFact);
        assertThat(fundsTransactionQueryService.findFundsActionFact(completeFact.getIdentity()))
                .hasValue(completeFact);
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET request_hash = ? WHERE tenant_id = ? AND sn = ?
                """, "0".repeat(64), TENANT_ID, detail.getSn())).isOne();
        assertThat(actionFactsByBusiness("AUTHORIZATION_COMPLETE", "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE"))
                .containsExactly(completeFact);
        assertThat(fundsTransactionQueryService.findFundsActionFact(completeFact.getIdentity()))
                .hasValue(completeFact);
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET request_hash = ? WHERE tenant_id = ? AND sn = ?
                """, requestHash, TENANT_ID, detail.getSn())).isOne();

        ObjectNode forceContext = WindJson.parseObject(contextVariables, ObjectNode.class);
        forceContext.put(FundsInstructionContextKeys.COMPLETION_MODE,
                FundsAuthorizationTransactionCompleteRequest.COMPLETION_MODE_FORCE);
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, WindJson.toJsonString(forceContext), TENANT_ID, detail.getSn())).isOne();
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, contextVariables, TENANT_ID, detail.getSn())).isOne();

        ObjectNode fractionalContext = WindJson.parseObject(contextVariables, ObjectNode.class);
        ObjectNode replayAmounts = (ObjectNode) fractionalContext.get(
                FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS);
        replayAmounts.put(replayAmounts.propertyNames().iterator().next(), 30.5D);
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, WindJson.toJsonString(fractionalContext), TENANT_ID, detail.getSn())).isOne();
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, contextVariables, TENANT_ID, detail.getSn())).isOne();

        updateFundsTransactionDetailState(detail.getSn(), FundsTransactionDetailState.PROCESSING);
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        updateFundsTransactionDetailState(detail.getSn(), FundsTransactionDetailState.SUCCEEDED);

        updateFundsTransactionDetailLedgerRef(detail.getSn(), null);
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        updateFundsTransactionDetailLedgerRef(detail.getSn(), ledgerRef);

        updateFundsTransactionCompletedAmount(authorizationSn, 31L);
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        updateFundsTransactionCompletedAmount(authorizationSn, 30L);

        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, "{}", TENANT_ID, detail.getSn())).isOne();
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, contextVariables, TENANT_ID, detail.getSn())).isOne();

        ObjectNode missingLegs = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        missingLegs.putArray("legs");
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(missingLegs));
        assertCompleteActionFactUnavailable(completeFact, "AUTH_COMPLETE_ACTION_TAMPER_CAPTURE");
    }

    /**
     * 场景：complete 的耐久 detail、引用、状态或授权累计被篡改。
     * 输入：先形成完整完成事实，再依次篡改 sibling 状态、Ledger 引用、累计和 context。
     * 输出：业务列表和 identity 查询均 fail-closed，恢复原值后继续测试下一种篡改。
     * 红线：FAILED/PROCESSING 标签、缺引用或聚合不一致不得被投影为成功事实。
     */
    @Test
    void testAuthorizationCompleteActionFactShouldRejectDetailAndAggregateTamper() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_COMPLETE_DETAIL_TAMPER_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_COMPLETE_DETAIL_TAMPER_AUTHORIZE");
        completeAuthorization(user, 30L, authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_AUTHORIZE",
                "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE", 2, 1, 2);

        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(
                "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        FundsTransactionDetail firstDetail = details.getFirst();
        String ledgerRef = firstDetail.getLedgerTransactionSn();
        String contextVariables = firstDetail.getContextVariables();

        updateFundsTransactionDetailState(firstDetail.getSn(), FundsTransactionDetailState.PROCESSING);
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        updateFundsTransactionDetailState(firstDetail.getSn(), FundsTransactionDetailState.FAILED);
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        updateFundsTransactionDetailState(firstDetail.getSn(), FundsTransactionDetailState.SUCCEEDED);

        updateFundsTransactionDetailLedgerRef(firstDetail.getSn(), null);
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        updateFundsTransactionDetailLedgerRef(firstDetail.getSn(), ledgerRef);

        updateFundsTransactionCompletedAmount(authorizationSn, 31L);
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        updateFundsTransactionCompletedAmount(authorizationSn, 30L);

        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, "{}", TENANT_ID, firstDetail.getSn())).isOne();
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_DETAIL_TAMPER_CAPTURE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET context_variables = ? WHERE tenant_id = ? AND sn = ?
                """, contextVariables, TENANT_ID, firstDetail.getSn())).isOne();
    }

    /**
     * 场景：SHARED complete 的责任 sibling 缺失、重复或交换。
     * 输入：Credit、父 Funding 和 settlement 三条 detail 已成功形成。
     * 输出：任何责任 identity/role 映射不再无重无漏时，两种查询均返回空。
     * 红线：不得用仍存在的 sibling 或聚合金额补推缺失责任事实。
     */
    @Test
    void testSharedAuthorizationCompleteActionFactShouldRejectResponsibilitySiblingTamper() {
        FundsAccountId parentAccount = fundingAccount("acs_parent");
        FundsAccountId cardAccount = creditAccount("acs_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_COMPLETE_SIBLING_TAMPER_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_COMPLETE_SIBLING_TAMPER_LIMIT");
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_COMPLETE_SIBLING_TAMPER_AUTHORIZE");
        completeAuthorization(cardAccount, 60L, authorizationSn, "AUTH_COMPLETE_SIBLING_TAMPER_CAPTURE");
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_SIBLING_TAMPER_AUTHORIZE",
                "AUTH_COMPLETE_SIBLING_TAMPER_CAPTURE", 3, 2, 4);

        FundsTransactionDetail cardDetail = fundsTransactionDetailsByBusinessSn(
                "AUTH_COMPLETE_SIBLING_TAMPER_CAPTURE").stream()
                .filter(detail -> cardAccount.id().equals(detail.getSubjectId()))
                .findFirst()
                .orElseThrow();
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET subject_id = ? WHERE tenant_id = ? AND sn = ?
                """, parentAccount.id(), TENANT_ID, cardDetail.getSn())).isOne();
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_SIBLING_TAMPER_CAPTURE");
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail SET subject_id = ? WHERE tenant_id = ? AND sn = ?
                """, cardAccount.id(), TENANT_ID, cardDetail.getSn())).isOne();

        deleteFundsTransactionDetail(cardDetail.getSn());
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_SIBLING_TAMPER_CAPTURE");
    }

    /**
     * 场景：complete 的派生 settlement sibling 或原授权 replay leg 不完整。
     * 输入：普通授权完成事实已闭合，随后删除 capture target 或篡改原 HOLD leg。
     * 输出：缺失、重复、换向或改 identity/amount 时，两种 ActionFact 查询均返回空。
     * 红线：不得按当前余额、剩余 detail 或聚合累计重建 provenance。
     */
    @Test
    void testAuthorizationCompleteActionFactShouldRejectCaptureTargetAndReplayLegTamper() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_COMPLETE_ROUTE_TAMPER_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_COMPLETE_ROUTE_TAMPER_AUTHORIZE");
        completeAuthorization(user, 30L, authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE");
        assertAuthorizationCompletePhysicalFacts(authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_AUTHORIZE",
                "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE", 2, 1, 2);

        String routeSnapshot = fundsTransactionsByBusinessSn(
                "AUTH_COMPLETE_ROUTE_TAMPER_AUTHORIZE").getFirst().getRouteSnapshot();
        ObjectNode duplicateLeg = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ArrayNode duplicateLegs = (ArrayNode) duplicateLeg.get("legs");
        duplicateLegs.add(duplicateLegs.get(0).deepCopy());
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(duplicateLeg));
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE");

        ObjectNode exchangedLeg = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ObjectNode leg = (ObjectNode) ((ArrayNode) exchangedLeg.get("legs")).get(0);
        JsonNode sourceNode = leg.get("sourceNode").deepCopy();
        leg.set("sourceNode", leg.get("targetNode").deepCopy());
        leg.set("targetNode", sourceNode);
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(exchangedLeg));
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE");

        ObjectNode wrongLegIdentity = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ((ObjectNode) ((ArrayNode) wrongLegIdentity.get("legs")).get(0)).put("legId", "HOLD_TAMPERED");
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(wrongLegIdentity));
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE");

        ObjectNode wrongLegAmount = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        ObjectNode amount = (ObjectNode) ((ArrayNode) wrongLegAmount.get("legs")).get(0).get("amount");
        ObjectNode originalAmount = (ObjectNode) ((ArrayNode) wrongLegAmount.get("legs")).get(0)
                .get("originalAmount");
        amount.put("amount", 31L);
        originalAmount.put("amount", 31L);
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(wrongLegAmount));
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE");

        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);
        FundsTransactionDetail settlementDetail = fundsTransactionDetailsByBusinessSn(
                "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE").stream()
                .filter(detail -> settlementAccount().id().equals(detail.getSubjectId()))
                .findFirst()
                .orElseThrow();
        deleteFundsTransactionDetail(settlementDetail.getSn());
        assertCompleteActionFactUnavailable(authorizationSn, "AUTH_COMPLETE_ROUTE_TAMPER_CAPTURE");
    }

    /**
     * 场景：普通授权部分完成后释放一部分未完成额度。
     * 输入：authorize100 -> complete30 -> release20。
     * 输出：形成唯一 release20 ActionFact，并保留原授权引用和原 route provenance。
     * 红线：同步 REVERSAL 成功不能替代可按业务键和 identity 查询的 release 事实。
     */
    @Test
    void testSuccessfulAuthorizationReleaseShouldExposeStableActionFact() {
        FundsAccountId user = fundingAccount("funding_user");
        String authorizationBusinessSn = "AUTH_RELEASE_ACTION_AUTHORIZE";
        String completeBusinessSn = "AUTH_RELEASE_ACTION_COMPLETE";
        String releaseBusinessSn = "AUTH_RELEASE_ACTION_RELEASE";
        topup(user, 100L, "AUTH_RELEASE_ACTION_TOPUP");
        String authorizationSn = authorize(user, 100L, true, authorizationBusinessSn);
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                authorizationBusinessSn, 100L, "succeeded", "proven-full");
        completeAuthorization(user, 30L, authorizationSn, completeBusinessSn);
        assertCompleteActionFact(authorizationSn, authorizationFact, completeBusinessSn, 30L, 1);

        BalanceSnapshot beforeRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorization(user, 20L, authorizationSn, releaseBusinessSn);
        assertFundingReleaseBalanceDelta(beforeRelease, user, 20L);

        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 20L, 1, 1, 2);
        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getCompletedAmount()).isEqualTo(30L);
        assertThat(transaction.getReversedAmount()).isEqualTo(20L);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 50L, CURRENCY);
        assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 20L, 1);
    }

    /**
     * 场景：同一授权分两次释放未完成额度。
     * 输入：authorize100 -> release20 -> release30。
     * 输出：形成两条身份独立、Money 分别为 20/30 的 release ActionFact。
     * 红线：后继 release 不得改写或重复贡献前一条 release 事实。
     */
    @Test
    void testMultipleAuthorizationReleasesShouldExposeIndependentStableActionFacts() {
        FundsAccountId user = fundingAccount("funding_user");
        String authorizationBusinessSn = "AUTH_RELEASE_MULTIPLE_AUTHORIZE";
        topup(user, 100L, "AUTH_RELEASE_MULTIPLE_TOPUP");
        String authorizationSn = authorize(user, 100L, true, authorizationBusinessSn);
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                authorizationBusinessSn, 100L, "succeeded", "proven-full");

        BalanceSnapshot beforeFirstRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorization(user, 20L, authorizationSn, "AUTH_RELEASE_MULTIPLE_FIRST");
        BalanceSnapshot afterFirstRelease = assertFundingReleaseBalanceDelta(beforeFirstRelease, user, 20L);
        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_MULTIPLE_FIRST", 20L, 1, 1, 2);
        FundsActionFactDTO first = assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_MULTIPLE_FIRST", 20L, 1);

        reverseAuthorization(user, 30L, authorizationSn, "AUTH_RELEASE_MULTIPLE_SECOND");
        assertFundingReleaseBalanceDelta(afterFirstRelease, user, 30L);
        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_MULTIPLE_SECOND", 30L, 1, 1, 2);
        assertThat(fundsTransaction(authorizationSn).getReversedAmount()).isEqualTo(50L);
        FundsActionFactDTO second = assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_MULTIPLE_SECOND", 30L, 1);
        assertThat(first.getIdentity()).isNotEqualTo(second.getIdentity());
        assertReleaseActionFactAvailable(first, "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_MULTIPLE_FIRST");
    }

    /**
     * 场景：共享卡授权同时占用信用子账户和父资金账户后执行释放。
     * 输入：SHARED authorize60 -> release60。
     * 输出：两组责任和 replay provenance 完整，但公共 ActionFact Money 只计一次 60。
     * 红线：多 sibling 不得把 release 金额重复累计为 120。
     */
    @Test
    void testSharedAuthorizationReleaseShouldExposeOneActionFactWithoutDoublingMoney() {
        FundsAccountId parentAccount = fundingAccount("auth_rel_shared_parent");
        FundsAccountId cardAccount = creditAccount("auth_release_shared_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_RELEASE_SHARED_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_RELEASE_SHARED_LIMIT");
        String authorizationBusinessSn = "AUTH_RELEASE_SHARED_AUTHORIZE";
        String releaseBusinessSn = "AUTH_RELEASE_SHARED_RELEASE";
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L, authorizationBusinessSn);
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                authorizationBusinessSn, 60L, "succeeded", "proven-full");

        BalanceSnapshot beforeRelease = snapshot(balances(cardAccount, parentAccount));
        reverseAuthorization(cardAccount, 60L, authorizationSn, releaseBusinessSn);
        assertSharedReleaseBalanceDelta(beforeRelease, cardAccount, parentAccount, 60L);

        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 60L, 2, 2, 4);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        FundsActionFactDTO releaseFact = assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 60L, 2);
        assertThat(releaseFact.getMoney()).isEqualTo(Money.immutable(60L, CURRENCY));
    }

    /**
     * 场景：release 重放、分隔符碰撞和跨事实族业务键冲突。
     * 输入：合法 release、含冒号业务键，以及主表/detail、action family、authorization root 冲突。
     * 输出：合法身份稳定且无碰撞；歧义业务键的列表与 identity 查询共同 fail-closed。
     * 红线：不得按字符串分隔或主表优先顺序遮蔽另一条资金动作事实。
     */
    @Test
    void testAuthorizationReleaseActionFactShouldRemainStableAcrossReplayConflictAndLifecycleProgress() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 500L, "AUTH_RELEASE_IDENTITY_TOPUP");
        String authorizationSn = authorize(user, 100L, true, "AUTH_RELEASE_IDENTITY_AUTHORIZE");
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                "AUTH_RELEASE_IDENTITY_AUTHORIZE", 100L, "succeeded", "proven-full");
        BalanceSnapshot beforeRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorization(user, 20L, authorizationSn, "AUTH_RELEASE_IDENTITY_RELEASE");
        assertFundingReleaseBalanceDelta(beforeRelease, user, 20L);
        assertAuthorizationReleasePhysicalFacts(authorizationSn, "AUTH_RELEASE_IDENTITY_AUTHORIZE",
                "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_IDENTITY_RELEASE", 20L, 1, 1, 2);
        FundsActionFactDTO firstFact = assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_IDENTITY_RELEASE", 20L, 1);

        assertThat(reverseAuthorization(user, 20L, authorizationSn, "AUTH_RELEASE_IDENTITY_RELEASE"))
                .isEqualTo(authorizationSn);
        assertReleaseActionFactAvailable(firstFact, "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_IDENTITY_RELEASE");
        assertThatThrownBy(() -> reverseAuthorization(user, 21L, authorizationSn,
                "AUTH_RELEASE_IDENTITY_RELEASE"))
                .hasMessageContaining("资金交易明细请求参数不一致");
        assertReleaseActionFactAvailable(firstFact, "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_IDENTITY_RELEASE");

        BalanceSnapshot beforeComplete = snapshot(balances(user, settlementAccount()));
        completeAuthorization(user, 10L, authorizationSn, "AUTH_RELEASE_IDENTITY_COMPLETE");
        assertOnlyBalanceDeltas(beforeComplete, snapshot(balances(user, settlementAccount())),
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -10L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 10L, CURRENCY));
        assertCompleteActionFact(authorizationSn, authorizationFact,
                "AUTH_RELEASE_IDENTITY_COMPLETE", 10L, 1);
        assertReleaseActionFactAvailable(firstFact, "AUTHORIZATION_REVERSAL", "AUTH_RELEASE_IDENTITY_RELEASE");

        String delimiterAuthorizationOne = authorize(user, 30L, true, "AUTH_RELEASE_DELIMITER_AUTH_1");
        FundsActionFactDTO delimiterAuthorizationFactOne = assertAuthorizationActionFact(
                "AUTH_RELEASE_DELIMITER_AUTH_1", 30L, "succeeded", "proven-full");
        BalanceSnapshot beforeDelimiterReleaseOne = snapshot(balances(user, settlementAccount()));
        reverseAuthorizationWithBusinessKey(user, 5L, delimiterAuthorizationOne, "A", "B:C");
        assertFundingReleaseBalanceDelta(beforeDelimiterReleaseOne, user, 5L);
        assertAuthorizationReleasePhysicalFacts(delimiterAuthorizationOne, "AUTH_RELEASE_DELIMITER_AUTH_1",
                "A", "B:C", 5L, 1, 1, 2);
        String delimiterAuthorizationTwo = authorize(user, 30L, true, "AUTH_RELEASE_DELIMITER_AUTH_2");
        FundsActionFactDTO delimiterAuthorizationFactTwo = assertAuthorizationActionFact(
                "AUTH_RELEASE_DELIMITER_AUTH_2", 30L, "succeeded", "proven-full");
        BalanceSnapshot beforeDelimiterReleaseTwo = snapshot(balances(user, settlementAccount()));
        reverseAuthorizationWithBusinessKey(user, 5L, delimiterAuthorizationTwo, "A:B", "C");
        assertFundingReleaseBalanceDelta(beforeDelimiterReleaseTwo, user, 5L);
        assertAuthorizationReleasePhysicalFacts(delimiterAuthorizationTwo, "AUTH_RELEASE_DELIMITER_AUTH_2",
                "A:B", "C", 5L, 1, 1, 2);
        FundsActionFactDTO delimiterFactOne = assertReleaseActionFact(delimiterAuthorizationOne,
                delimiterAuthorizationFactOne, "A", "B:C", 5L, 1);
        FundsActionFactDTO delimiterFactTwo = assertReleaseActionFact(delimiterAuthorizationTwo,
                delimiterAuthorizationFactTwo, "A:B", "C", 5L, 1);
        assertThat(delimiterFactOne.getIdentity()).isNotEqualTo(delimiterFactTwo.getIdentity());

        String tableCollisionScene = "AUTHORIZATION_REVERSAL";
        String tableCollisionSn = "AUTH_RELEASE_TABLE_COLLISION";
        authorizeWithBusinessKey(user, 20L, tableCollisionScene, tableCollisionSn);
        String tableCollisionAuthorization = authorize(user, 20L, true,
                "AUTH_RELEASE_TABLE_COLLISION_PARENT");
        BalanceSnapshot beforeTableCollisionRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorizationWithBusinessKey(user, 5L, tableCollisionAuthorization,
                tableCollisionScene, tableCollisionSn);
        assertFundingReleaseBalanceDelta(beforeTableCollisionRelease, user, 5L);
        assertAuthorizationReleasePhysicalFacts(tableCollisionAuthorization,
                "AUTH_RELEASE_TABLE_COLLISION_PARENT", tableCollisionScene, tableCollisionSn, 5L, 1, 1, 2);
        assertReleaseActionFactUnavailable(tableCollisionAuthorization, tableCollisionScene, tableCollisionSn);

        String familyCollisionAuthorization = authorize(user, 20L, true,
                "AUTH_RELEASE_FAMILY_COLLISION_PARENT");
        String familyCollisionScene = "AUTH_RELEASE_FAMILY_COLLISION";
        String familyCollisionSn = "AUTH_RELEASE_FAMILY_COLLISION_KEY";
        BalanceSnapshot beforeFamilyCollisionComplete = snapshot(balances(user, settlementAccount()));
        completeAuthorizationWithBusinessKey(user, 5L, familyCollisionAuthorization,
                familyCollisionScene, familyCollisionSn);
        assertOnlyBalanceDeltas(beforeFamilyCollisionComplete, snapshot(balances(user, settlementAccount())),
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -5L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 5L, CURRENCY));
        BalanceSnapshot beforeFamilyCollisionRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorizationWithBusinessKey(user, 5L, familyCollisionAuthorization,
                familyCollisionScene, familyCollisionSn);
        assertFundingReleaseBalanceDelta(beforeFamilyCollisionRelease, user, 5L);
        assertAuthorizationReleasePhysicalFacts(familyCollisionAuthorization,
                "AUTH_RELEASE_FAMILY_COLLISION_PARENT", familyCollisionScene, familyCollisionSn, 5L, 1, 1, 2);
        assertReleaseActionFactUnavailable(familyCollisionAuthorization,
                familyCollisionScene, familyCollisionSn);
        assertThat(fundsTransactionQueryService.findFundsActionFact(new FundsActionFactRef(TENANT_ID,
                familyCollisionAuthorization + ":complete:" + familyCollisionScene + ":" + familyCollisionSn)))
                .isEmpty();

        String rootCollisionScene = "AUTH_RELEASE_ROOT_COLLISION";
        String rootCollisionSn = "AUTH_RELEASE_ROOT_COLLISION_KEY";
        String firstRoot = authorize(user, 20L, true, "AUTH_RELEASE_ROOT_COLLISION_PARENT_1");
        String secondRoot = authorize(user, 20L, true, "AUTH_RELEASE_ROOT_COLLISION_PARENT_2");
        BalanceSnapshot beforeFirstRootRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorizationWithBusinessKey(user, 5L, firstRoot, rootCollisionScene, rootCollisionSn);
        assertFundingReleaseBalanceDelta(beforeFirstRootRelease, user, 5L);
        assertAuthorizationReleasePhysicalFacts(firstRoot, "AUTH_RELEASE_ROOT_COLLISION_PARENT_1",
                rootCollisionScene, rootCollisionSn, 5L, 1, 1, 2);
        BalanceSnapshot beforeSecondRootRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorizationWithBusinessKey(user, 5L, secondRoot, rootCollisionScene, rootCollisionSn);
        assertFundingReleaseBalanceDelta(beforeSecondRootRelease, user, 5L);
        assertAuthorizationReleasePhysicalFacts(secondRoot, "AUTH_RELEASE_ROOT_COLLISION_PARENT_2",
                rootCollisionScene, rootCollisionSn, 5L, 1, 1, 2);
        assertReleaseActionFactUnavailable(firstRoot, rootCollisionScene, rootCollisionSn);
        assertReleaseActionFactUnavailable(secondRoot, rootCollisionScene, rootCollisionSn);
    }

    /**
     * 场景：合法 release 形成后篡改耐久 detail 事实。
     * 输入：依次篡改状态、Ledger 引用、原授权引用、context 和 sibling 完整性。
     * 输出：任一篡改都使列表与 identity 查询共同返回空。
     * 红线：FAILED/PROCESSING 标签或残缺 sibling 不得被猜成 proven-full/proven-zero。
     */
    @Test
    void testAuthorizationReleaseActionFactShouldFailClosedForDurableFactTamper() {
        FundsAccountId user = fundingAccount("funding_user");
        String authorizationBusinessSn = "AUTH_RELEASE_TAMPER_AUTHORIZE";
        String releaseBusinessSn = "AUTH_RELEASE_TAMPER_RELEASE";
        topup(user, 100L, "AUTH_RELEASE_TAMPER_TOPUP");
        String authorizationSn = authorize(user, 60L, true, authorizationBusinessSn);
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                authorizationBusinessSn, 60L, "succeeded", "proven-full");
        BalanceSnapshot beforeRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorization(user, 20L, authorizationSn, releaseBusinessSn);
        assertFundingReleaseBalanceDelta(beforeRelease, user, 20L);
        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 20L, 1, 1, 2);
        FundsActionFactDTO releaseFact = assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 20L, 1);

        FundsTransactionDetail detail = fundsTransactionDetailsByBusinessSn(releaseBusinessSn).getFirst();
        String ledgerTransactionSn = detail.getLedgerTransactionSn();
        String referenceDetailSn = detail.getReferenceDetailSn();
        String contextVariables = detail.getContextVariables();

        updateFundsTransactionDetailState(detail.getSn(), FundsTransactionDetailState.PROCESSING);
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionDetailState(detail.getSn(), FundsTransactionDetailState.SUCCEEDED);
        updateFundsTransactionDetailLedgerRef(detail.getSn(), null);
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionDetailLedgerRef(detail.getSn(), ledgerTransactionSn);
        updateFundsTransactionDetailReferenceDetailSn(detail.getSn(), "wrong_authorization");
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionDetailReferenceDetailSn(detail.getSn(), referenceDetailSn);
        updateFundsTransactionDetailContextVariables(detail.getSn(), "{}");
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionDetailContextVariables(detail.getSn(), contextVariables);
        deleteFundsTransactionDetail(detail.getSn());
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
    }

    /**
     * 场景：release 已成立后篡改原 route、complete/release sibling 和 root 累计。
     * 输入：authorize100 -> complete30 -> release20 后逐项修改承重事实。
     * 输出：complete 与 release 两类累计任一不闭合时 release 双查询返回空。
     * 红线：不得只信 root 数字或当前余额补推 release 事实。
     */
    @Test
    void testAuthorizationReleaseActionFactShouldRejectRouteAndCumulativeTamper() {
        FundsAccountId user = fundingAccount("funding_user");
        String authorizationBusinessSn = "AUTH_RELEASE_CUMULATIVE_AUTHORIZE";
        String completeBusinessSn = "AUTH_RELEASE_CUMULATIVE_COMPLETE";
        String releaseBusinessSn = "AUTH_RELEASE_CUMULATIVE_RELEASE";
        topup(user, 100L, "AUTH_RELEASE_CUMULATIVE_TOPUP");
        String authorizationSn = authorize(user, 100L, true, authorizationBusinessSn);
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                authorizationBusinessSn, 100L, "succeeded", "proven-full");
        completeAuthorization(user, 30L, authorizationSn, completeBusinessSn);
        assertCompleteActionFact(authorizationSn, authorizationFact, completeBusinessSn, 30L, 1);
        BalanceSnapshot beforeRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorization(user, 20L, authorizationSn, releaseBusinessSn);
        assertFundingReleaseBalanceDelta(beforeRelease, user, 20L);
        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 20L, 1, 1, 2);
        FundsActionFactDTO releaseFact = assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", releaseBusinessSn, 20L, 1);

        updateFundsTransactionCompletedAmount(authorizationSn, 29L);
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionCompletedAmount(authorizationSn, 30L);
        FundsTransactionDetail completeDetail = fundsTransactionDetailsByBusinessSn(completeBusinessSn).getFirst();
        updateFundsTransactionDetailState(completeDetail.getSn(), FundsTransactionDetailState.PROCESSING);
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionDetailState(completeDetail.getSn(), FundsTransactionDetailState.SUCCEEDED);
        updateFundsTransactionReversedAmount(authorizationSn, 21L);
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionReversedAmount(authorizationSn, 20L);

        String routeSnapshot = fundsTransactionsByBusinessSn(authorizationBusinessSn).getFirst().getRouteSnapshot();
        ObjectNode missingLegs = WindJson.parseObject(routeSnapshot, ObjectNode.class);
        missingLegs.putArray("legs");
        updateFundsTransactionRouteSnapshot(authorizationSn, WindJson.toJsonString(missingLegs));
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionRouteSnapshot(authorizationSn, routeSnapshot);

        FundsTransactionDetail releaseDetail = fundsTransactionDetailsByBusinessSn(releaseBusinessSn).getFirst();
        String releaseContext = releaseDetail.getContextVariables();
        updateFundsTransactionDetailContextVariables(releaseDetail.getSn(), "{}");
        assertReleaseActionFactUnavailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
        updateFundsTransactionDetailContextVariables(releaseDetail.getSn(), releaseContext);
        assertReleaseActionFactAvailable(releaseFact, "AUTHORIZATION_REVERSAL", releaseBusinessSn);
    }

    /**
     * 场景：同一授权并发完成与释放后继续释放剩余额度。
     * 输入：authorize100，并发 complete60/release60，随后 release20。
     * 输出：竞态仅一方成功，后续 release20 形成稳定 ActionFact，输家无动作事实。
     * 红线：并发失败方不得贡献 ActionFact，成功方与后续 release 不得突破授权上限。
     */
    @Test
    void testConcurrentAuthorizationCompleteAndReleaseShouldExposeOnlyWinnerActionFact() throws Exception {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_RELEASE_RACE_TOPUP");
        String authorizationBusinessSn = "AUTH_RELEASE_RACE_AUTHORIZE";
        String authorizationSn = authorize(user, 100L, true, authorizationBusinessSn);
        FundsActionFactDTO authorizationFact = assertAuthorizationActionFact(
                authorizationBusinessSn, 100L, "succeeded", "proven-full");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        RaceOutcome winner;
        RaceOutcome loser;
        try {
            Future<RaceOutcome> completeFuture = executor.submit(() -> raceCommand(ready, start,
                    "AUTH_RELEASE_RACE_COMPLETE", FundsTransactionEventType.COMPLETE,
                    () -> completeAuthorization(user, 60L, authorizationSn, "AUTH_RELEASE_RACE_COMPLETE")));
            Future<RaceOutcome> releaseFuture = executor.submit(() -> raceCommand(ready, start,
                    "AUTH_RELEASE_RACE_RELEASE", FundsTransactionEventType.REVERSAL,
                    () -> reverseAuthorization(user, 60L, authorizationSn, "AUTH_RELEASE_RACE_RELEASE")));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<RaceOutcome> outcomes = List.of(awaitOutcome(completeFuture), awaitOutcome(releaseFuture));
            assertThat(outcomes.stream().filter(RaceOutcome::succeeded).toList()).singleElement();
            assertThat(outcomes.stream().filter(outcome -> !outcome.succeeded()).toList()).singleElement();
            winner = outcomes.stream().filter(RaceOutcome::succeeded).findFirst().orElseThrow();
            loser = outcomes.stream().filter(outcome -> !outcome.succeeded()).findFirst().orElseThrow();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        String followUpReleaseSn = "AUTH_RELEASE_RACE_FOLLOW_UP";
        BalanceSnapshot beforeFollowUpRelease = snapshot(balances(user, settlementAccount()));
        reverseAuthorization(user, 20L, authorizationSn, followUpReleaseSn);
        assertFundingReleaseBalanceDelta(beforeFollowUpRelease, user, 20L);
        assertAuthorizationReleasePhysicalFacts(authorizationSn, authorizationBusinessSn,
                "AUTHORIZATION_REVERSAL", followUpReleaseSn, 20L, 1, 1, 2);
        assertReleaseActionFact(authorizationSn, authorizationFact,
                "AUTHORIZATION_REVERSAL", followUpReleaseSn, 20L, 1);

        if (winner.eventType() == FundsTransactionEventType.COMPLETE) {
            assertCompleteActionFact(authorizationSn, authorizationFact, winner.businessSn(), 60L, 1);
        } else {
            assertReleaseActionFact(authorizationSn, authorizationFact,
                    "AUTHORIZATION_REVERSAL", winner.businessSn(), 60L, 1);
        }
        String loserScene = loser.eventType() == FundsTransactionEventType.COMPLETE
                ? "AUTHORIZATION_COMPLETE" : "AUTHORIZATION_REVERSAL";
        assertNoActionFacts(loserScene, loser.businessSn());
        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getCompletedAmount() + transaction.getReversedAmount()).isEqualTo(80L);
    }

    /**
     * 场景：force/standalone complete 或不存在的业务身份查询 ActionFact。
     * 输入：force completion 60，以及不存在的 ordinary complete businessSn。
     * 输出：两种查询均返回空，不生成公共 complete ActionFact。
     * 红线：standalone/force、wrong tenant 或 malformed identity 不得混入普通授权完成投影。
     */
    @Test
    void testUnsupportedAuthorizationCompleteInputsShouldNotExposeActionFact() {
        FundsAccountId user = fundingAccount("funding_user");
        assertActionFactQueryBoundary("AUTHORIZATION_COMPLETE", "AUTH_COMPLETE_ACTION_ABSENT");
        topup(user, 100L, "AUTH_COMPLETE_ACTION_FORCE_TOPUP");

        forceCompletionAuthorization(user, 60L, "AUTH_COMPLETE_ACTION_FORCE");

        assertNoActionFacts("AUTHORIZATION_COMPLETE", "AUTH_COMPLETE_ACTION_FORCE");
        assertNoActionFacts("AUTHORIZATION_COMPLETE", "AUTH_COMPLETE_ACTION_ABSENT");
    }

    /**
     * 场景：授权拒绝使用相同业务流水重复提交，第二次请求摘要一致时复用原拒绝交易，拒绝原因变化时拒绝。
     * 输入：账户充值 100 后，授权拒绝 60，拒绝原因为 RISK_DECLINED；随后同流水同原因重试，再改为 LIMIT_DECLINED。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持首次拒绝后的状态。
     * 预期：授权拒绝幂等必须保护拒绝金额、授权主体和拒绝原因。
     * 红线：同业务流水不同拒绝原因不得静默复用原交易，也不得生成 route、posting、ledger entry 或污染拒绝事实。
     */
    @Test
    void testAuthorizationDeclineSameBusinessSnWithDifferentReasonShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_DECLINE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        String declinedSn = declineAuthorization(user, 60L, "RISK_DECLINED", "AUTH_IDEMPOTENT_DECLINE");
        BalanceSnapshot afterFirstDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        String retryDeclinedSn = declineAuthorization(user, 60L, "RISK_DECLINED", "AUTH_IDEMPOTENT_DECLINE");
        BalanceSnapshot afterRetryDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryDeclinedSn).isEqualTo(declinedSn);
        assertOnlyBalanceDeltas(afterFirstDecline, afterRetryDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertThatThrownBy(() -> declineAuthorization(user, 60L, "LIMIT_DECLINED", "AUTH_IDEMPOTENT_DECLINE"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryDecline, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(declinedSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.REJECTED);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, declinedSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getLegs()).isEmpty());

        assertThat(fundsTransactionDetails(declinedSn))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                    assertThat(contextVariablesOf(detail.getContextVariables()))
                            .containsEntry(FundsInstructionContextKeys.DECLINE_REASON, "RISK_DECLINED")
                            .doesNotContainValue("LIMIT_DECLINED");
                });
        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_DECLINE_TOPUP", 3, 4);
        assertThat(fundsTransactionsByBusinessSn("AUTH_IDEMPOTENT_DECLINE"))
                .singleElement()
                .satisfies(rejectedTransaction -> {
                    assertThat(rejectedTransaction.getSn()).isEqualTo(declinedSn);
                    assertThat(rejectedTransaction.getState()).isEqualTo(FundsTransactionState.REJECTED);
                    assertNoLedgerFactsForFundsTransaction(rejectedTransaction.getSn());
                });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_DECLINE"))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getTransactionSn()).isEqualTo(declinedSn);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                });
    }

    /**
     * 场景：授权请求把卡组织 CVV 原文字段放入扩展上下文。
     * 输入：账户充值 100 后，授权请求 contextVariables 含嵌套 cardSecurityCode 字段。
     * 输出：授权请求被拒绝，AVAILABLE/AUTHORIZATION 和平台账户余额保持充值后状态。
     * 预期：请求扩展上下文不得进入资金交易事实、route snapshot 或账务事实。
     * 红线：完整卡号、CVV、密钥和 token secret 不得通过普通授权上下文落库。
     */
    @Test
    void testAuthorizeWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                        .setApproved(true)
                        .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                                Map.of("cardSecurityCode", "123"))))
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("AUTH_SENSITIVE_CONTEXT_AUTHORIZE")
                        .setDescription("authorization with sensitive context"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                        .setApproved(true)
                        .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                                Map.of("networkReference", "4242424242424242"))))
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("AUTH_SENSITIVE_CONTEXT_PAN_VALUE")
                        .setDescription("authorization with sensitive PAN value"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                        .setApproved(true)
                        .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                                Map.of("networkReference", "GB82WEST12345698765432"))))
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("AUTH_SENSITIVE_CONTEXT_IBAN_VALUE")
                        .setDescription("authorization with sensitive IBAN value"), WindOperatorFactory.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_AUTHORIZE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_PAN_VALUE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：用户充值后发起资金账户授权，授权批准后全额撤销。
     * 输入：充值 100、授权批准 60、全额撤销 60。
     * 输出：用户 AVAILABLE/AUTHORIZATION 余额快照和账务事实。
     * 预期：授权批准只占用可用余额，撤销只释放授权占用。
     * 红线：授权撤销不得进入 SETTLEMENT，不得表达消费、扣划或完成后退款。
     */
    @Test
    void testFundingAuthorizationApproveThenFullReversalShouldReleaseAuthorizationBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FULL_REVERSAL_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_FULL_REVERSAL_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        reverseAuthorization(user, 60L, authorizationSn, "AUTH_FULL_REVERSAL_CANCEL");
        BalanceSnapshot afterReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isEqualTo(60L);
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REVERSAL_AUTHORIZE");
        LedgerTransaction reversalTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REVERSAL_CANCEL");
        assertThat(reversalTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(reversalTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(reversalTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REVERSAL.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_CANCEL", 0, 1, 1, 2);
    }

    /**
     * 场景：资金账户授权后账户进入关闭收口态。
     * 输入：授权占用 60 后，将 AUTHORIZATION 账本挂起；随后尝试新授权并完成原授权。
     * 输出：新授权被拒绝，原授权完成继续消费 AUTHORIZATION 并进入平台 SETTLEMENT。
     * 红线：SUSPENDED 账本只允许原链路清算收口，不得承接新的普通授权。
     */
    @Test
    void testSuspendedAuthorizationLedgerShouldRejectNewAuthorizeAndAllowCompletionClosingPosting() {
        FundsAccountId user = fundingAccount("funding_user");

        topup(user, 100L, "AUTH_CLOSING_COMPLETE_TOPUP");
        String authorizationSn = authorize(user, 60L, true, "AUTH_CLOSING_COMPLETE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        Long authorizationLedgerId = findLedger(user, LedgerSubjectCode.AUTHORIZATION)
                .orElseThrow()
                .getId();
        ledgerService.updateLedgerState(new UpdateLedgerStateRequest()
                .setId(authorizationLedgerId)
                .setState(LedgerState.SUSPENDED));
        LedgerFactSnapshot afterSuspended = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorize(user, 10L, true, "AUTH_CLOSING_COMPLETE_REJECTED_AUTHORIZE"))
                .hasMessageContaining("账本状态不允许入账");
        assertLedgerTransactionFactsUnchanged(afterSuspended);
        assertFailedFundsTransactionWithoutLedgerFacts("AUTH_CLOSING_COMPLETE_REJECTED_AUTHORIZE");

        completeAuthorization(user, 60L, authorizationSn, "AUTH_CLOSING_COMPLETE_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));
        assertThat(ledgerService.getLedgerById(authorizationLedgerId).getState())
                .isEqualTo(LedgerState.SUSPENDED);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_CLOSING_COMPLETE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：授权交易命中月度账本 bucket 后，全额撤销必须继承原授权路由周期。
     * 输入：资金账户只预置 AVAILABLE/AUTHORIZATION 的 MONTHLY/2026-06 账本，授权 60 后撤销 60。
     * 输出：授权和撤销都只改动 MONTHLY/2026-06 bucket，LedgerEntry 保留同一周期。
     * 预期：后继交易不得使用当前默认周期或 LIFETIME bucket 解释旧授权链路。
     * 红线：旧周期账本是授权事实的一部分，撤销、结算和退款不能静默串到账本新周期。
     */
    @Test
    void testAuthorizationSuccessorShouldInheritOriginalRoutePeriod() {
        FundsAccountId user = fundingAccount("funding_period_user");
        AccountBalancePeriodType periodType = AccountBalancePeriodType.MONTHLY;
        String periodId = "2026-06";
        ensurePeriodLedger(user, LedgerSubjectCode.AVAILABLE, periodType, periodId, 100L);
        ensurePeriodLedger(user, LedgerSubjectCode.AUTHORIZATION, periodType, periodId, 0L);

        assertPeriodLedgerBalance(user, LedgerSubjectCode.AVAILABLE, periodType, periodId, 100L);
        assertPeriodLedgerBalance(user, LedgerSubjectCode.AUTHORIZATION, periodType, periodId, 0L);

        String authorizationSn = authorizeWithLedgerPeriod(user, 60L, "AUTH_PERIOD_AUTHORIZE", periodType, periodId);

        assertPeriodLedgerBalance(user, LedgerSubjectCode.AVAILABLE, periodType, periodId, 40L);
        assertPeriodLedgerBalance(user, LedgerSubjectCode.AUTHORIZATION, periodType, periodId, 60L);
        assertLedgerEntriesPeriod("AUTH_PERIOD_AUTHORIZE", periodType, periodId);

        String reversalSn = reverseAuthorization(user, 60L, authorizationSn, "AUTH_PERIOD_REVERSAL");

        assertPeriodLedgerBalance(user, LedgerSubjectCode.AVAILABLE, periodType, periodId, 100L);
        assertPeriodLedgerBalance(user, LedgerSubjectCode.AUTHORIZATION, periodType, periodId, 0L);
        assertLedgerEntriesPeriod("AUTH_PERIOD_REVERSAL", periodType, periodId);
    }

    /**
     * 场景：支出控制范围被误作为授权交易账户。
     * 输入：提交支出控制范围授权批准 10。
     * 输出：授权请求被拒绝；不生成支出控制流水或核心余额投影，平台结算账户余额保持请求前状态。
     * 预期：支出控制范围只能作为预算控制上下文，不得被授权交易包装成资金价值主体。
     * 红线：支出控制范围不得生成授权 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testAuthorizeSpendControlScopeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId budget = spendControlScope("auth_spend_control_scope");
        ensureSpendControlScopeWithoutLedgers(budget);

        BalanceSnapshot beforeAuthorize = snapshot(balances(budget, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeAuthorizeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorize(budget, 10L, true, "AUTH_SPEND_CONTROL_SCOPE_AUTHORIZE"))
                .hasMessageContaining("授权交易账户不能是支出控制范围");

        BalanceSnapshot afterRejectedAuthorize = snapshot(balances(budget, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeAuthorize, afterRejectedAuthorize,
                delta(budget, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeAuthorizeFacts);

        assertSubjectBalanceNotInitialized(balance(budget));
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SPEND_CONTROL_SCOPE_AUTHORIZE");
    }

    /**
     * 场景：用户充值后授权批准，先部分撤销，再完成剩余授权金额。
     * 输入：充值 100、授权批准 80、部分撤销 30、剩余完成 50。
     * 输出：每一步 AVAILABLE/AUTHORIZATION/SETTLEMENT 余额变化和账务事实。
     * 预期：部分撤销释放授权占用，剩余完成只消费剩余授权占用。
     * 红线：完成剩余授权不得重新从 AVAILABLE 扣款，部分撤销后的累计处理金额不得超过原授权。
     */
    @Test
    void testFundingAuthorizationPartialReversalThenCompleteRemainingShouldCloseAuthorization() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_PARTIAL_REVERSAL_COMPLETE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_PARTIAL_REVERSAL_COMPLETE_AUTHORIZE");
        String authorizationLegId = "AUTHORIZATION_1";
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, authorizationSn))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.getLegs())
                        .singleElement()
                        .satisfies(leg -> assertThat(leg.getLegId()).isEqualTo(authorizationLegId)));
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        reverseAuthorization(user, 30L, authorizationSn, "AUTH_PARTIAL_REVERSAL_COMPLETE_CANCEL");
        BalanceSnapshot afterReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 50L, authorizationSn, "AUTH_PARTIAL_REVERSAL_COMPLETE_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterReversal, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(30L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name(),
                        FundsTransactionEventType.COMPLETE.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_COMPLETE_AUTHORIZE");
        LedgerTransaction reversalTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_COMPLETE_CANCEL");
        assertThat(reversalTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(reversalTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(reversalTransaction)).singleElement().satisfies(plan -> {
            assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.REVERSAL.name());
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.AUTHORIZATION_REVERSAL.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_HOLD.name());
            assertThat(plan.getRouteLegId()).isEqualTo("RELEASE_" + authorizationLegId);
        });
        assertThat(entriesOf(reversalTransaction)).allSatisfy(entry -> {
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.AUTHORIZATION_REVERSAL.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_HOLD.name());
        });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertThat(fundsTransactionQueryService.sumConsumedReplayLegAmount(TENANT_ID, authorizationSn,
                FundsTransactionEventType.REVERSAL, authorizationLegId, CURRENCY).getAmount()).isEqualTo(30L);

        LedgerTransaction completeTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_COMPLETE_CAPTURE");
        assertThat(completeTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(completeTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.SETTLEMENT);
        assertThat(postingPlansOf(completeTransaction)).singleElement().satisfies(plan -> {
            assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.COMPLETION.name());
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.AUTHORIZATION_COMPLETION.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_CONSUME.name());
            assertThat(plan.getRouteLegId()).isEqualTo("CONSUME_" + authorizationLegId);
        });
        assertThat(entriesOf(completeTransaction)).allSatisfy(entry -> {
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.AUTHORIZATION_COMPLETION.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_CONSUME.name());
        });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertThat(fundsTransactionQueryService.sumConsumedReplayLegAmount(TENANT_ID, authorizationSn,
                FundsTransactionEventType.COMPLETE, authorizationLegId, CURRENCY).getAmount()).isEqualTo(50L);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_CANCEL", 0, 1, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_COMPLETE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：授权金额按部分完成、部分释放、再次完成的事件顺序闭合。
     * 输入：充值 100、授权批准 100、结算 40、释放 20、再结算 40。
     * 输出：累计完成 80、累计释放 20、剩余授权 0，每个后继事件形成独立资金和账务事实。
     * 预期：部分处理期间保持 OPEN，最终由累计金额闭合为 CLOSED，不引入部分完成状态。
     * 红线：剩余授权必须等于授权金额减累计完成和累计释放，后继事件必须引用原授权账务事实。
     */
    @Test
    void testAuthorizationAmountProgressShouldCloseAfterCompletionReleaseAndCompletion() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_AMOUNT_PROGRESS_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 100L, true, "AUTH_AMOUNT_PROGRESS_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 40L, authorizationSn, "AUTH_AMOUNT_PROGRESS_FIRST_COMPLETE");
        BalanceSnapshot afterFirstComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY));
        FundsTransactionDTO afterFirstCompleteTransaction = fundsTransaction(authorizationSn);
        assertThat(afterFirstCompleteTransaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(afterFirstCompleteTransaction.getAuthorizedAmount()).isEqualTo(100L);
        assertThat(afterFirstCompleteTransaction.getCompletedAmount()).isEqualTo(40L);
        assertThat(afterFirstCompleteTransaction.getReversedAmount()).isZero();
        assertThat(afterFirstCompleteTransaction.getAuthorizedAmount()
                - afterFirstCompleteTransaction.getCompletedAmount()
                - afterFirstCompleteTransaction.getReversedAmount()).isEqualTo(60L);

        reverseAuthorization(user, 20L, authorizationSn, "AUTH_AMOUNT_PROGRESS_RELEASE");
        BalanceSnapshot afterRelease = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterFirstComplete, afterRelease,
                delta(user, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        FundsTransactionDTO afterReleaseTransaction = fundsTransaction(authorizationSn);
        assertThat(afterReleaseTransaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(afterReleaseTransaction.getAuthorizedAmount()).isEqualTo(100L);
        assertThat(afterReleaseTransaction.getCompletedAmount()).isEqualTo(40L);
        assertThat(afterReleaseTransaction.getReversedAmount()).isEqualTo(20L);
        assertThat(afterReleaseTransaction.getAuthorizedAmount()
                - afterReleaseTransaction.getCompletedAmount()
                - afterReleaseTransaction.getReversedAmount()).isEqualTo(40L);

        completeAuthorization(user, 40L, authorizationSn, "AUTH_AMOUNT_PROGRESS_SECOND_COMPLETE");
        BalanceSnapshot afterSecondComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRelease, afterSecondComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 80L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(100L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(20L);
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getAuthorizedAmount()
                - transaction.getCompletedAmount()
                - transaction.getReversedAmount()).isZero();

        assertPostedTransactions(5);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name(),
                        FundsTransactionEventType.REVERSAL.name(),
                        FundsTransactionEventType.COMPLETE.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_AMOUNT_PROGRESS_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_AMOUNT_PROGRESS_FIRST_COMPLETE")
                .getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(ledgerTransactionByBusinessSn("AUTH_AMOUNT_PROGRESS_RELEASE")
                .getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(ledgerTransactionByBusinessSn("AUTH_AMOUNT_PROGRESS_SECOND_COMPLETE")
                .getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_AMOUNT_PROGRESS_FIRST_COMPLETE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList()).containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_AMOUNT_PROGRESS_RELEASE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList()).containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_AMOUNT_PROGRESS_SECOND_COMPLETE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList()).containsOnly(authorizationSn);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_AMOUNT_PROGRESS_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_AMOUNT_PROGRESS_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_AMOUNT_PROGRESS_FIRST_COMPLETE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_AMOUNT_PROGRESS_RELEASE", 0, 1, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_AMOUNT_PROGRESS_SECOND_COMPLETE", 0, 2, 1, 2);
    }

    /**
     * 场景：用户充值后授权批准，部分撤销后再次发起超过剩余授权的撤销。
     * 输入：充值 100、授权批准 80、先撤销 30、再撤销 60。
     * 输出：第二次撤销失败，余额、交易累计和账务事实保持第一次撤销后的状态。
     * 预期：剩余授权只有 50 时，不允许再释放 60。
     * 红线：超额撤销不得透支 AUTHORIZATION，不得把失败请求记录成成功账务事实。
     */
    @Test
    void testAuthorizationReversalExceedingRemainingShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_REVERSAL_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_REVERSAL_EXCEED_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        reverseAuthorization(user, 30L, authorizationSn, "AUTH_REVERSAL_EXCEED_FIRST_CANCEL");
        BalanceSnapshot afterFirstReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstReversalFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> reverseAuthorization(user, 60L, authorizationSn,
                "AUTH_REVERSAL_EXCEED_SECOND_CANCEL"))
                .hasMessageContaining("资金交易剩余授权金额不足");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterFirstReversal, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(30L);
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_FIRST_CANCEL", 0, 1, 1, 2);
        assertLedgerTransactionFactsUnchanged(afterFirstReversalFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_SECOND_CANCEL");
    }

    /**
     * 场景：用户充值后发起资金账户授权，授权批准后全额完成。
     * 输入：充值 100、授权批准 60、全额完成 60。
     * 输出：用户 AVAILABLE/AUTHORIZATION、平台 CASH/SETTLEMENT 余额快照和账务事实。
     * 预期：授权批准只占用可用余额，完成只消费授权占用并进入平台结算桶。
     * 红线：授权批准不是消费；普通完成不得触碰 LIMIT，也不得重新从 AVAILABLE 扣款。
     */
    @Test
    void testFundingAuthorizationApproveThenFullCompleteShouldConsumeAuthorizationBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FULL_COMPLETE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_FULL_COMPLETE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 60L, authorizationSn, "AUTH_FULL_COMPLETE_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_COMPLETE_AUTHORIZE");
        assertThat(entriesOf(authorizationTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.AUTHORIZATION);
        assertThat(postingPlansOf(authorizationTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.AUTHORIZATION.name());

        LedgerTransaction completeTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_COMPLETE_CAPTURE");
        assertThat(completeTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        List<LedgerPostingPlan> completePostingPlans = postingPlansOf(completeTransaction);
        assertThat(entriesOf(completeTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.SETTLEMENT);
        assertThat(completePostingPlans)
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.getSn()).hasSizeLessThanOrEqualTo(64);
                    assertThat(plan.getRouteLegId()).isEqualTo("CONSUME_AUTHORIZATION_1");
                });
        assertThat(completePostingPlans.stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.COMPLETION.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_COMPLETE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_COMPLETE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_COMPLETE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_COMPLETE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_COMPLETE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：VCC 共享卡授权解析到信用子账户及其父资金账户。
     * 输入：信用子账户额度 100，父资金账户可用余额 100，授权批准 60。
     * 输出：信用子账户和父资金账户同时形成授权占用，信用子账户 participant 携带账户层级快照。
     * 预期：accountHierarchySnapshot 固化关系号和直接父账户，不改变 route leg。
     * 红线：共享卡授权不得只占用信用额度而跳过父资金账户。
     */
    @Test
    void testSharedCardAuthorizationShouldHoldCreditAndParentFundingAccountWithHierarchySnapshot() {
        FundsAccountId parentAccount = fundingAccount("vcc_parent_pool");
        FundsAccountId cardAccount = creditAccount("vcc_shared_card_credit");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);

        topup(parentAccount, 100L, "AUTH_SHARED_CARD_PARENT_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_SHARED_CARD_LIMIT");

        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_SHARED_CARD_AUTHORIZE");

        assertBucket(balance(cardAccount), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, authorizationSn))
                .as("authorization route snapshot should carry account hierarchy")
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getParticipants())
                            .filteredOn(participant -> cardAccount.id()
                                    .equals(participant.getSubjectRef().getSubjectId()))
                            .singleElement()
                            .satisfies(participant -> {
                                assertThat(participant.getSubjectRef().getSubjectType().name())
                                        .isEqualTo(cardAccount.type());
                                assertThat(participant.getAmount()).isEqualTo(Money.immutable(60L, CURRENCY));
                                assertThat(participant.getAccountHierarchySnapshot()).isNotNull();
                                assertThat(participant.getAccountHierarchySnapshot().getRelationSn()).isNotBlank();
                                assertThat(participant.getAccountHierarchySnapshot().getParentAccountRef())
                                        .isNotNull()
                                        .satisfies(parent -> assertThat(parent.getSubjectId())
                                                .isEqualTo(parentAccount.id()));
                            });
                    assertThat(routeSnapshot.getParticipants())
                            .filteredOn(participant -> parentAccount.id()
                                    .equals(participant.getSubjectRef().getSubjectId()))
                            .singleElement()
                            .satisfies(participant -> assertThat(participant.getAccountHierarchySnapshot()).isNull());
                });
        assertThat(entriesByBusinessSn("AUTH_SHARED_CARD_AUTHORIZE"))
                .extracting(LedgerEntry::getSubjectId)
                .contains(cardAccount.id())
                .contains(parentAccount.id());
        assertFundsAndLedgerFactsForBusinessSn("AUTH_SHARED_CARD_AUTHORIZE", 1, 2, 2, 4);
    }

    /**
     * 场景：VCC 共享卡授权后全额完成。
     * 输入：信用子账户和父资金账户各占用授权 60，随后完成 60。
     * 输出：信用子账户形成已用额度，两个主体的授权占用都归零，但只有父资金账户承担一次真实资金结算责任。
     * 红线：信用额度控制 leg 必须进入 OUTSTANDING，不得让平台 SETTLEMENT 重复增加。
     */
    @Test
    void testSharedCardAuthorizationCompletionShouldRecordCreditOutstandingAndSettleParentOnce() {
        FundsAccountId parentAccount = fundingAccount("vcc_complete_pool");
        FundsAccountId cardAccount = creditAccount("vcc_complete_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_SHARED_CARD_COMPLETION_PARENT_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_SHARED_CARD_COMPLETION_LIMIT");
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_SHARED_CARD_COMPLETION_AUTHORIZE");

        String completionSn = completeAuthorization(cardAccount, 60L, authorizationSn,
                "AUTH_SHARED_CARD_COMPLETION_COMPLETE");

        assertBucket(balance(cardAccount), LedgerSubjectCode.LIMIT, 100L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.OUTSTANDING, 60L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);
        assertThat(fundsTransaction(authorizationSn).getCompletedAmount()).isEqualTo(60L);
        assertThat(entriesByBusinessSn("AUTH_SHARED_CARD_COMPLETION_COMPLETE").stream()
                .filter(entry -> settlementAccount().id().equals(entry.getSubjectId()))
                .filter(entry -> entry.getLedgerSubjectCode() == LedgerSubjectCode.SETTLEMENT)
                .mapToLong(LedgerEntry::getAmount)
                .sum()).isEqualTo(60L);
        assertThat(entriesByBusinessSn("AUTH_SHARED_CARD_COMPLETION_COMPLETE").stream()
                .filter(entry -> cardAccount.id().equals(entry.getSubjectId()))
                .filter(entry -> entry.getLedgerSubjectCode() == LedgerSubjectCode.OUTSTANDING)
                .mapToLong(LedgerEntry::getAmount)
                .sum()).isEqualTo(60L);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_SHARED_CARD_COMPLETION_COMPLETE", 0, 3, 2, 4);
        LedgerFactSnapshot afterCompletionFacts = ledgerFactSnapshot();

        assertThat(completeAuthorization(cardAccount, 60L, authorizationSn,
                "AUTH_SHARED_CARD_COMPLETION_COMPLETE")).isEqualTo(completionSn);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(afterCompletionFacts);

        String refundSn = refundCompletedAuthorization(cardAccount, 60L, authorizationSn,
                "AUTH_SHARED_CARD_COMPLETION_REFUND");

        assertBucket(balance(cardAccount), LedgerSubjectCode.LIMIT, 100L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.OUTSTANDING, 0L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertThat(fundsTransaction(authorizationSn).getRefundedAmount()).isEqualTo(60L);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_SHARED_CARD_COMPLETION_REFUND", 0, 3, 2, 4);
        LedgerFactSnapshot afterRefundFacts = ledgerFactSnapshot();
        assertThat(refundCompletedAuthorization(cardAccount, 60L, authorizationSn,
                "AUTH_SHARED_CARD_COMPLETION_REFUND")).isEqualTo(refundSn);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(afterRefundFacts);
    }

    /**
     * 场景：VCC 共享卡授权后收到全额撤销。
     * 输入：授权时已占用信用子账户和父资金账户各 60，撤销请求只携带信用子账户和原授权流水。
     * 输出：撤销从原授权事实继承父资金账户并双释放。
     * 预期：信用子账户和父资金账户 AVAILABLE 均恢复 100，AUTHORIZATION 均归零。
     * 红线：撤销不得要求上层重复传父资金账户，也不得只释放信用子账户导致父账户授权占用残留。
     */
    @Test
    void testSharedCardAuthorizationReversalShouldInheritParentFundingAccount() {
        FundsAccountId parentAccount = fundingAccount("vcc_rev_pool");
        FundsAccountId cardAccount = creditAccount("vcc_rev_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_SHARED_CARD_REVERSAL_PARENT_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_SHARED_CARD_REVERSAL_LIMIT");
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_SHARED_CARD_REVERSAL_AUTHORIZE");

        reverseAuthorization(cardAccount, 60L, authorizationSn, "AUTH_SHARED_CARD_REVERSAL_CANCEL");

        assertBucket(balance(cardAccount), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(cardAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(parentAccount), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertThat(entriesByBusinessSn("AUTH_SHARED_CARD_REVERSAL_CANCEL"))
                .extracting(LedgerEntry::getSubjectId)
                .contains(cardAccount.id())
                .contains(parentAccount.id());
        assertFundsAndLedgerFactsForBusinessSn("AUTH_SHARED_CARD_REVERSAL_AUTHORIZE", 1, 2, 2, 4);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_SHARED_CARD_REVERSAL_CANCEL", 0, 2, 2, 4);
    }

    /**
     * 场景：VCC 共享卡授权后卡账户进入关闭收口态。
     * 输入：信用子账户和父资金账户授权占用各 60；子账户 AVAILABLE/AUTHORIZATION 账本挂起后撤销原授权。
     * 输出：新共享卡授权被拒绝，原授权撤销仍释放信用子账户和父资金账户。
     * 红线：共享卡关闭不能留下父资金账户授权占用，也不能让已挂起卡账本继续承接新消费。
     */
    @Test
    void testSuspendedSharedCardLedgerShouldRejectNewAuthorizeAndAllowReversalClosingPosting() {
        FundsAccountId parentAccount = fundingAccount("vcc_closing_pool");
        FundsAccountId cardAccount = creditAccount("vcc_closing_card");
        ensureFundingAccount(parentAccount);
        ensureLedger(parentAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(parentAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureCreditAccount(cardAccount);
        bindAccountHierarchy(cardAccount, parentAccount);
        topup(parentAccount, 100L, "AUTH_SHARED_CARD_CLOSING_PARENT_TOPUP");
        adjustBalance(cardAccount, 100L, true, "AUTH_SHARED_CARD_CLOSING_LIMIT");
        String authorizationSn = authorizeSharedCard(cardAccount, parentAccount, 60L,
                "AUTH_SHARED_CARD_CLOSING_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(cardAccount, parentAccount));
        Long cardAvailableLedgerId = findLedger(cardAccount, LedgerSubjectCode.AVAILABLE)
                .orElseThrow()
                .getId();
        Long cardAuthorizationLedgerId = findLedger(cardAccount, LedgerSubjectCode.AUTHORIZATION)
                .orElseThrow()
                .getId();
        ledgerService.updateLedgerState(new UpdateLedgerStateRequest()
                .setId(cardAvailableLedgerId)
                .setState(LedgerState.SUSPENDED));
        ledgerService.updateLedgerState(new UpdateLedgerStateRequest()
                .setId(cardAuthorizationLedgerId)
                .setState(LedgerState.SUSPENDED));
        LedgerFactSnapshot afterSuspended = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizeSharedCard(cardAccount, parentAccount, 10L,
                "AUTH_SHARED_CARD_CLOSING_REJECTED_AUTHORIZE"))
                .hasMessageContaining("账本状态不允许入账");
        assertLedgerTransactionFactsUnchanged(afterSuspended);
        assertFailedFundsTransactionWithoutLedgerFacts("AUTH_SHARED_CARD_CLOSING_REJECTED_AUTHORIZE");

        reverseAuthorization(cardAccount, 60L, authorizationSn, "AUTH_SHARED_CARD_CLOSING_CANCEL");
        BalanceSnapshot afterReversal = snapshot(balances(cardAccount, parentAccount));
        assertOnlyBalanceDeltas(afterAuthorize, afterReversal,
                delta(cardAccount, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(cardAccount, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(parentAccount, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(parentAccount, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY));
        assertThat(ledgerService.getLedgerById(cardAvailableLedgerId).getState())
                .isEqualTo(LedgerState.SUSPENDED);
        assertThat(ledgerService.getLedgerById(cardAuthorizationLedgerId).getState())
                .isEqualTo(LedgerState.SUSPENDED);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_SHARED_CARD_CLOSING_CANCEL", 0, 2, 2, 4);
    }

    /**
     * 场景：同一笔授权在并发窗口内同时收到完成和撤销。
     * 输入：充值 100、授权批准 60，两个线程同时发起完成 60 和撤销 60。
     * 输出：只有一个后继事件成功落账，输家业务流水不留下任何交易或账务事实。
     * 预期：授权金额闭合、状态合法，route/posting/ledger/projection/余额均不会重复或漏记。
     * 红线：失败方不得产生资金事实；授权后继并发不能突破 AUTHORIZATION 桶或重复写平台 SETTLEMENT。
     */
    @Test
    void testAuthorizationCompleteAndReversalRaceShouldAllowOnlyOneWinner() throws Exception {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_RACE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_RACE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<RaceOutcome> completeFuture = executor.submit(() -> raceCommand(ready, start,
                    "AUTH_RACE_CAPTURE", FundsTransactionEventType.COMPLETE,
                    () -> completeAuthorization(user, 60L, authorizationSn, "AUTH_RACE_CAPTURE")));
            Future<RaceOutcome> reversalFuture = executor.submit(() -> raceCommand(ready, start,
                    "AUTH_RACE_REVERSAL", FundsTransactionEventType.REVERSAL,
                    () -> reverseAuthorization(user, 60L, authorizationSn, "AUTH_RACE_REVERSAL")));

            assertThat(ready.await(5, TimeUnit.SECONDS))
                    .as("race commands are ready")
                    .isTrue();
            start.countDown();

            List<RaceOutcome> outcomes = List.of(awaitOutcome(completeFuture), awaitOutcome(reversalFuture));
            List<RaceOutcome> successes = outcomes.stream().filter(RaceOutcome::succeeded).toList();
            List<RaceOutcome> failures = outcomes.stream().filter(outcome -> !outcome.succeeded()).toList();
            assertThat(successes)
                    .as("race outcomes: %s", outcomes)
                    .hasSize(1);
            assertThat(failures)
                    .as("race outcomes: %s", outcomes)
                    .hasSize(1);

            RaceOutcome winner = successes.getFirst();
            RaceOutcome loser = failures.getFirst();
            assertThat(loser.failure())
                    .as("losing authorization event should be rejected and rolled back")
                    .isNotNull();
            assertThat(winner.eventType()).isIn(FundsTransactionEventType.COMPLETE, FundsTransactionEventType.REVERSAL);

            BalanceSnapshot afterRace = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
            FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
            assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
            assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
            assertThat(transaction.getRefundedAmount()).isZero();
            assertThat(transaction.getDeclinedAmount()).isZero();

            if (winner.eventType() == FundsTransactionEventType.COMPLETE) {
                assertOnlyBalanceDeltas(afterAuthorize, afterRace,
                        delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                        delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                        delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                        delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));
                assertThat(transaction.getCompletedAmount()).isEqualTo(60L);
                assertThat(transaction.getReversedAmount()).isZero();
                assertFundsAndLedgerFactsForBusinessSn(winner.businessSn(), 0, 2, 1, 2);
            } else {
                assertOnlyBalanceDeltas(afterAuthorize, afterRace,
                        delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                        delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                        delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                        delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
                assertThat(transaction.getCompletedAmount()).isZero();
                assertThat(transaction.getReversedAmount()).isEqualTo(60L);
                assertFundsAndLedgerFactsForBusinessSn(winner.businessSn(), 0, 1, 1, 2);
            }

            assertPostedTransactions(3);
            assertThat(ledgerTransactions().stream()
                    .map(LedgerTransaction::getEventType)
                    .toList())
                    .containsExactly(
                            FundsTransactionEventType.TOPUP.name(),
                            FundsTransactionEventType.AUTHORIZE.name(),
                            winner.eventType().name());
            assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_RACE_TOPUP", 3, 4);
            assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_RACE_AUTHORIZE", 1, 2);
            assertNoFundsOrLedgerFactsForBusinessSn(loser.businessSn());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .as("race executor stopped")
                    .isTrue();
        }
    }

    /**
     * 场景：外部已经完成扣款但本系统没有内部授权事实，运营按授权后继能力发起强制完成。
     * 输入：充值 100、强制完成 60、策略上限 60、外部原始事实引用和凭证引用齐备。
     * 输出：用户 AVAILABLE 直接扣减，平台 SETTLEMENT 增加，AUTHORIZATION 不变，账务事实保留强制完成审计上下文。
     * 预期：强制完成不伪造 authorizationTransactionSn，也不消费 AUTHORIZATION 桶。
     * 红线：没有内部授权事实时，普通完成路径不得被复用成“查不到授权”的失败，也不得绕过策略、原因和外部证据。
     */
    @Test
    void testForceCompletionWithoutAuthorizationShouldConsumeAvailableBalanceAndPreserveAuditContext() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FORCE_COMPLETION_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String forceCompletionSn = forceCompletionAuthorization(user, 60L, "AUTH_FORCE_COMPLETION_CAPTURE");

        BalanceSnapshot afterForceCompletion = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterForceCompletion,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(forceCompletionSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.COMPLETE.name());

        LedgerTransaction completeTransaction = ledgerTransactionByBusinessSn("AUTH_FORCE_COMPLETION_CAPTURE");
        assertThat(completeTransaction.getReferenceLedgerTransactionSn()).isNull();
        List<LedgerPostingPlan> completePostingPlans = postingPlansOf(completeTransaction);
        assertThat(entriesOf(completeTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
        assertThat(completePostingPlans)
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.getSn()).hasSizeLessThanOrEqualTo(64);
                    assertThat(plan.getRouteLegId()).isEqualTo("FORCE_COMPLETION_1");
                });
        assertThat(completePostingPlans.stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.COMPLETION.name());

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FORCE_COMPLETION_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnlyNulls();
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FORCE_COMPLETION_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnlyNulls();
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FORCE_COMPLETION_CAPTURE"))
                .allSatisfy(detail -> assertForceCompletionContext(detail.getContextVariables()));
        assertLedgerFactsFollowRouteSnapshot("AUTH_FORCE_COMPLETION_CAPTURE");
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_CAPTURE", 1, 2, 1, 2);
    }

    /**
     * 场景：强制完成请求缺少策略编码或金额超过策略上限。
     * 输入：充值 100，分别提交缺策略、超上限的强制完成请求。
     * 输出：请求在交易事实创建前被拒绝，余额、账务事实和交易事实不变化。
     * 预期：强制完成必须显式携带策略、原因、外部原始事实、凭证和金额上限。
     * 红线：缺少授权事实的完成不得降级成普通完成，不得在参数非法时留下 FAILED 资金交易或半成功账务。
     */
    @Test
    void testForceCompletionMissingPolicyOrExceedingLimitShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_FORCE_COMPLETION_REJECT_TOPUP");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_MISSING_POLICY")
                .setForceCompletionPolicyCode(null), WindOperatorFactory.system()))
                .hasMessageContaining("forceCompletionPolicyCode");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_UNKNOWN_POLICY")
                .setForceCompletionPolicyCode("UNKNOWN_FORCE_COMPLETION_POLICY"), WindOperatorFactory.system()))
                .hasMessageContaining("forceCompletionPolicyCode");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_EXCEED_LIMIT")
                .setForceCompletionLimitAmount(50L), WindOperatorFactory.system()))
                .hasMessageContaining("forceCompletionLimitAmount");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_LIMIT_MISMATCH")
                .setForceCompletionLimitAmount(99L), WindOperatorFactory.system()))
                .hasMessageContaining("forceCompletionLimitAmount");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_WITH_AUTH_SN")
                .setAuthorizationTransactionSn("FT_SHOULD_NOT_BE_ACCEPTED"), WindOperatorFactory.system()))
                .hasMessageContaining("authorizationTransactionSn");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_MISSING_REASON")
                .setForceCompletionReason("   "), WindOperatorFactory.system()))
                .hasMessageContaining("forceCompletionReason");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_MISSING_EXTERNAL_FACT")
                .setExternalOriginalFactRef("   "), WindOperatorFactory.system()))
                .hasMessageContaining("externalOriginalFactRef");
        assertThatThrownBy(() -> authorizationTransactionService.complete(forceCompletionRequest(user, 60L,
                "AUTH_FORCE_COMPLETION_MISSING_VOUCHER")
                .setForceCompletionVoucherRef("   "), WindOperatorFactory.system()))
                .hasMessageContaining("forceCompletionVoucherRef");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_REJECT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_MISSING_POLICY");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_UNKNOWN_POLICY");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_EXCEED_LIMIT");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_LIMIT_MISMATCH");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_WITH_AUTH_SN");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_MISSING_EXTERNAL_FACT");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_COMPLETION_MISSING_VOUCHER");
    }

    /**
     * 场景：用户充值后发起授权，授权全额完成后再全额退款。
     * 输入：充值 100、授权批准 60、全额完成 60、全额退款 60。
     * 输出：用户 AVAILABLE/AUTHORIZATION、平台 SETTLEMENT 余额逐步变化和账务事实。
     * 预期：完成后退款沿原完成路径回退，回补用户 AVAILABLE 并扣减平台 SETTLEMENT。
     * 红线：完成后退款不得重新释放 AUTHORIZATION，不得按当前绑定重新选路。
     */
    @Test
    void testFundingAuthorizationFullCompleteThenFullRefundShouldRestoreAvailableBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FULL_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_FULL_REFUND_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 60L, authorizationSn, "AUTH_FULL_REFUND_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        refundCompletedAuthorization(user, 60L, authorizationSn, "AUTH_FULL_REFUND_RETURN");
        BalanceSnapshot afterRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterComplete, afterRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -60L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(60L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name(),
                        FundsTransactionEventType.AUTH_REFUND.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REFUND_AUTHORIZE");
        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REFUND_RETURN");
        String authorizationLegId = "AUTHORIZATION_1";
        assertThat(refundTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction)).singleElement().satisfies(plan -> {
            assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.REFUND.name());
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.REFUND.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
            assertThat(plan.getRouteLegId()).isEqualTo("RESTORE_" + authorizationLegId);
        });
        assertThat(entriesOf(refundTransaction)).allSatisfy(entry -> {
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.REFUND.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertThat(fundsTransactionQueryService.sumConsumedReplayLegAmount(TENANT_ID, authorizationSn,
                FundsTransactionEventType.AUTH_REFUND, authorizationLegId, CURRENCY).getAmount()).isEqualTo(60L);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：外部引用可追溯，但本系统没有内部授权流水，运营发起无授权直接退款。
     * 输入：用户充值 100 后向平台结算户付款 70 形成可退结算余额；随后按 `NO_AUTH` 模式退款 40。
     * 输出：用户 AVAILABLE 恢复 40，平台 SETTLEMENT 扣减 40，退款资金事实保留外部引用和退款原因。
     * 预期：无授权退款不携带内部授权流水，不查询原授权账本交易，也不补造 AUTHORIZATION 占用。
     * 红线：无授权退款不得按普通授权链退款回放，不得把外部事实伪装成内部授权或原交易聚合。
     */
    @Test
    void testNoAuthRefundShouldCreateStandaloneRefundFactAndPreserveExternalOriginalFact() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_NO_AUTH_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L, "AUTH_NO_AUTH_REFUND_EXTERNAL_CAPTURE");
        BalanceSnapshot afterExternalCapture = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterExternalCapture,
                delta(user, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY));

        String noAuthRefundSn = refundWithoutAuthorization(user, 40L, "AUTH_NO_AUTH_REFUND_RETURN");
        BalanceSnapshot afterNoAuthRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterExternalCapture, afterNoAuthRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(noAuthRefundSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
        assertThat(transaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isEqualTo(40L);
        assertThat(transaction.getReferenceTransactionSn()).isNull();

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("AUTH_NO_AUTH_REFUND_RETURN");
        assertThat(refundTransaction.getReferenceLedgerTransactionSn()).isNull();
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_NO_AUTH_REFUND_RETURN"))
                .allSatisfy(detail -> {
                    assertThat(detail.getReferenceDetailSn()).isNull();
                    assertThat(detail.getReferenceLedgerTransactionSn()).isNull();
                    assertNoAuthRefundContext(detail.getContextVariables());
                });
        assertThat(fundsTransactionsByBusinessSn("AUTH_NO_AUTH_REFUND_RETURN"))
                .singleElement()
                .satisfies(refund -> assertNoAuthRefundRouteCode(refund.getSn()));
        assertLedgerFactsFollowRouteSnapshot("AUTH_NO_AUTH_REFUND_RETURN");
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_EXTERNAL_CAPTURE", 2, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_RETURN", 1, 2, 1, 2);
    }

    /**
     * 场景：调用方不再传入退款模式字段，且未携带内部原授权流水。
     * 输入：平台结算户已有外部原消费沉淀余额，提交无 authorizationTransactionSn、无退款模式入参的请求。
     * 输出：系统按无授权退款处理，仍补充 NO_AUTH 上下文标签并保留外部引用审计。
     * 预期：无授权退款判定以内部原授权流水是否为空为准，refundMode 只作为资金指令内部归类标签。
     * 红线：请求侧没有 refundMode 时不得回退成普通授权链退款，也不得查询内部原授权账本交易。
     */
    @Test
    void testNoAuthRefundShouldInferModeWhenAuthorizationTransactionSnIsBlank() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_NO_AUTH_REFUND_INFER_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "AUTH_NO_AUTH_REFUND_INFER_EXTERNAL_CAPTURE");
        BalanceSnapshot beforeRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        String refundSn = authorizationTransactionService.refund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_INFER_RETURN"), WindOperatorFactory.system());

        BalanceSnapshot afterRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        assertThat(fundsTransaction(refundSn).getReferenceTransactionSn()).isNull();
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_NO_AUTH_REFUND_INFER_RETURN"))
                .allSatisfy(detail -> assertNoAuthRefundContext(detail.getContextVariables()));
        assertThat(fundsTransactionsByBusinessSn("AUTH_NO_AUTH_REFUND_INFER_RETURN"))
                .singleElement()
                .satisfies(refund -> assertNoAuthRefundRouteCode(refund.getSn()));
        assertFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_INFER_RETURN", 1, 2, 1, 2);
    }

    /**
     * 场景：无授权退款缺少外部引用、原因，或请求携带不存在的内部授权流水。
     * 输入：平台结算户已有可退余额，分别提交非法 no-auth refund 请求和无效授权链退款请求。
     * 输出：请求在交易事实创建前失败，余额、账务事实和资金事实均不变化。
     * 预期：无内部授权流水的退款必须携带最小外部引用和原因；携带 `authorizationTransactionSn`
     * 时必须按授权链退款校验原授权事实。
     * 红线：无授权退款不得回退成普通授权链退款，不得查询原授权账本交易或留下半成功事实。
     */
    @Test
    void testNoAuthRefundMissingRequiredAuditFieldsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_NO_AUTH_REFUND_REJECT_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "AUTH_NO_AUTH_REFUND_REJECT_EXTERNAL_CAPTURE");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.refund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_MISSING_EXTERNAL_REFERENCE")
                .setExternalReferenceSn("   "), WindOperatorFactory.system()))
                .hasMessageContaining("externalReferenceSn");

        assertThatThrownBy(() -> authorizationTransactionService.refund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_MISSING_REASON").setRefundReason("   "), WindOperatorFactory.system()))
                .hasMessageContaining("refundReason");

        assertThatThrownBy(() -> authorizationTransactionService.refund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_WITH_AUTH_SN")
                .setAuthorizationTransactionSn("FT202606030000000001"), WindOperatorFactory.system()))
                .hasMessageContaining("授权交易不存在");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_REJECT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_REJECT_EXTERNAL_CAPTURE", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_MISSING_EXTERNAL_REFERENCE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_WITH_AUTH_SN");
    }

    /**
     * 场景：无授权退款请求错误携带争议/拒付字段。
     * 输入：无内部原授权流水的退款请求，同时携带 disputeMode、disputeReason、disputeVoucherRef 和 externalDisputeRef。
     * 输出：请求在交易事实创建前失败，余额、账务事实和资金事实均不变化。
     * 预期：NO_AUTH 退款与已完成授权后的争议退款互斥，不能静默丢弃争议审计字段。
     * 红线：无授权退款不得被带争议字段的请求伪装成争议退款，也不得在忽略争议字段后成功入账。
     */
    @Test
    void testNoAuthRefundWithDisputeFieldsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_EXTERNAL_CAPTURE");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.refund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_WITH_DISPUTE")
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_NO_AUTH")
                .setExternalDisputeRef("DISPUTE_CASE_NO_AUTH"), WindOperatorFactory.system()))
                .hasMessageContaining("dispute");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_EXTERNAL_CAPTURE", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_WITH_DISPUTE");
    }

    /**
     * 场景：已完成授权发生外部争议，业务侧通过授权链退款承接争议退回。
     * 输入：充值 100、授权 60、完成 60、争议类退款 40，并携带争议原因和凭证引用。
     * 输出：用户 AVAILABLE 恢复 40，平台 SETTLEMENT 释放 40，资金明细和账本交易保留争议审计上下文。
     * 预期：争议类退款仍走 `refund` 的 AUTH_REFUND 资金事实，可与普通退款通过业务场景和上下文区分。
     * 红线：争议类退款不得被压缩成授权拒绝，不得误写 CHARGEBACK 事件或 declinedAmount。
     */
    @Test
    void testAuthorizationDisputeRefundShouldUseRefundAndPreserveAuditContext() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_DISPUTE_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_DISPUTE_REFUND_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 60L, authorizationSn, "AUTH_DISPUTE_REFUND_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        authorizationTransactionService.refund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_202605290001")
                .setExternalDisputeRef("DISPUTE_CASE_202605290001")
                .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                .setBusinessSn("AUTH_DISPUTE_REFUND_RETURN")
                .setDescription("authorization dispute refund")
                .setContextVariables(WritableContextVariables.of(Map.of(
                        "caseOwner", "ops-team-a"))), WindOperatorFactory.system());
        BalanceSnapshot afterDisputeRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterComplete, afterDisputeRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(60L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(40L);
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name(),
                        FundsTransactionEventType.AUTH_REFUND.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_DISPUTE_REFUND_AUTHORIZE");
        LedgerTransaction disputeRefundTransaction = ledgerTransactionByBusinessSn("AUTH_DISPUTE_REFUND_RETURN");
        assertThat(disputeRefundTransaction.getBusinessScene()).isEqualTo("AUTHORIZATION_DISPUTE_REFUND");
        assertThat(disputeRefundTransaction.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND.name());
        assertThat(disputeRefundTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertDisputeRefundContext(disputeRefundTransaction.getContextVariables());
        assertThat(contextVariablesOf(disputeRefundTransaction.getContextVariables()))
                .containsEntry("caseOwner", "ops-team-a")
                .doesNotContainKey(FundsInstructionContextKeys.DECLINE_REASON);
        assertThat(entriesOf(disputeRefundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(disputeRefundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());
        assertThat(postingPlansOf(disputeRefundTransaction))
                .allSatisfy(plan -> {
                    assertDisputeRefundContext(plan.getContextVariables());
                    assertThat(contextVariablesOf(plan.getContextVariables()))
                            .containsEntry("caseOwner", "ops-team-a");
                });
        assertThat(entriesOf(disputeRefundTransaction))
                .allSatisfy(entry -> {
                    assertDisputeRefundContext(entry.getContextVariables());
                    assertThat(contextVariablesOf(entry.getContextVariables()))
                            .containsEntry("caseOwner", "ops-team-a");
                });

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_DISPUTE_REFUND_RETURN"))
                .allSatisfy(detail -> {
                    assertThat(detail.getBusinessScene()).isEqualTo("AUTHORIZATION_DISPUTE_REFUND");
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
                    assertThat(detail.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
                    assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.RETURN);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.SUCCEEDED);
                    assertThat(detail.getReferenceDetailSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
                    assertDisputeRefundContext(detail.getContextVariables());
                    assertThat(contextVariablesOf(detail.getContextVariables()))
                            .containsEntry("caseOwner", "ops-team-a")
                            .doesNotContainKey(FundsInstructionContextKeys.DECLINE_REASON);
                    assertThat(detail.getRequestHash()).isNotBlank();
                });
        BalanceSnapshot beforeIdempotencyConflict = snapshot(balances(user, cashMappingAccount(),
                settlementAccount()));
        LedgerFactSnapshot beforeIdempotencyConflictFacts = ledgerFactSnapshot();
        assertThatThrownBy(() -> authorizationTransactionService.refund(
                new FundsAuthorizationTransactionRefundRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                        .setAuthorizationTransactionSn(authorizationSn)
                        .setDisputeMode("CHARGEBACK")
                        .setDisputeReason("CARDHOLDER_DISPUTE")
                        .setDisputeVoucherRef("DISPUTE_EVIDENCE_202605290001")
                        .setExternalDisputeRef("DISPUTE_CASE_CHANGED")
                        .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                        .setBusinessSn("AUTH_DISPUTE_REFUND_RETURN")
                        .setDescription("authorization dispute refund")
                        .setContextVariables(WritableContextVariables.of(Map.of(
                                "caseOwner", "ops-team-a"))), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");
        BalanceSnapshot afterIdempotencyConflict = snapshot(balances(user, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(beforeIdempotencyConflict, afterIdempotencyConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeIdempotencyConflictFacts);

        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：争议类授权退款缺少模式、原因、凭证或外部争议引用。
     * 输入：已完成授权后分别提交 dispute 审计字段半填或空白的退款请求。
     * 输出：请求在交易事实创建前失败，余额、账务事实和资金事实均不变化。
     * 预期：争议类退款必须完整携带模式、原因、凭证和外部争议引用。
     * 红线：争议类退款不得半填审计字段后退化成普通退款。
     */
    @Test
    void testAuthorizationDisputeRefundMissingRequiredAuditFieldsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_DISPUTE_REFUND_REJECT_TOPUP");
        String authorizationSn = authorize(user, 60L, true, "AUTH_DISPUTE_REFUND_REJECT_AUTHORIZE");
        completeAuthorization(user, 60L, authorizationSn, "AUTH_DISPUTE_REFUND_REJECT_CAPTURE");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.refund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_MODE")
                .setDisputeMode("   "), WindOperatorFactory.system()))
                .hasMessageContaining("disputeMode");

        assertThatThrownBy(() -> authorizationTransactionService.refund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_REASON")
                .setDisputeReason("   "), WindOperatorFactory.system()))
                .hasMessageContaining("disputeReason");

        assertThatThrownBy(() -> authorizationTransactionService.refund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_VOUCHER")
                .setDisputeVoucherRef("   "), WindOperatorFactory.system()))
                .hasMessageContaining("disputeVoucherRef");

        assertThatThrownBy(() -> authorizationTransactionService.refund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_EXTERNAL_REF")
                .setExternalDisputeRef(null), WindOperatorFactory.system()))
                .hasMessageContaining("externalDisputeRef");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_REJECT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_REJECT_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_REJECT_CAPTURE", 0, 2, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_MODE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_VOUCHER");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_EXTERNAL_REF");
    }

    /**
     * 场景：授权后继事件引用不存在的原授权交易。
     * 输入：已存在账户但不存在原授权流水，分别提交撤销、完成和退款请求。
     * 输出：请求在生成资金事实前失败，余额、账务事实和业务流水均不变化。
     * 预期：授权后继事件必须先锁定原授权事实和原 route snapshot，不得根据当前账户重新选路。
     * 红线：缺原授权事实不得生成 route、posting、ledger transaction、LedgerEntry 或交易投影副作用。
     */
    @Test
    void testAuthorizationSuccessorsMissingOriginalFactShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        String missingAuthorizationSn = "MISSING_AUTHORIZATION_TXN";
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> reverseAuthorization(user, 30L, missingAuthorizationSn,
                "AUTH_REPLAY_MISSING_ORIGINAL_REVERSAL"))
                .hasMessageContaining("授权交易不存在");
        assertThatThrownBy(() -> completeAuthorization(user, 30L, missingAuthorizationSn,
                "AUTH_REPLAY_MISSING_ORIGINAL_COMPLETE"))
                .hasMessageContaining("授权交易不存在");
        assertThatThrownBy(() -> refundCompletedAuthorization(user, 30L, missingAuthorizationSn,
                "AUTH_REPLAY_MISSING_ORIGINAL_REFUND"))
                .hasMessageContaining("授权交易不存在");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REPLAY_MISSING_ORIGINAL_REVERSAL");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REPLAY_MISSING_ORIGINAL_COMPLETE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REPLAY_MISSING_ORIGINAL_REFUND");
    }

    /**
     * 场景：授权后继请求携带的账户不是原授权主主体。
     * 输入：账户 A 授权 100 并完成 40，账户 B 引用 A 的授权提交撤销、完成、退款和同业务号完成重试。
     * 输出：所有账户不一致请求都在生成资金事实前失败，原授权累计金额和余额不变。
     * 预期：后继动作沿原快照回放，但请求账户仍必须明确匹配原授权主主体。
     * 红线：不得静默忽略 accountId，也不得因同业务号重试而复用其他账户的成功结果。
     */
    @Test
    void testAuthorizationSuccessorsMismatchedAccountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId originalAccount = fundingAccount("auth_original_account");
        FundsAccountId mismatchedAccount = fundingAccount("auth_mismatched_account");
        ensureFundingAccount(originalAccount);
        ensureLedger(originalAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(originalAccount, LedgerSubjectCode.AUTHORIZATION);
        ensureFundingAccount(mismatchedAccount);
        ensureLedger(mismatchedAccount, LedgerSubjectCode.AVAILABLE);
        topup(originalAccount, 100L, "AUTH_ACCOUNT_MISMATCH_ORIGINAL_TOPUP");
        topup(mismatchedAccount, 100L, "AUTH_ACCOUNT_MISMATCH_OTHER_TOPUP");
        String authorizationSn = authorize(originalAccount, 100L, true,
                "AUTH_ACCOUNT_MISMATCH_AUTHORIZE");
        completeAuthorization(originalAccount, 40L, authorizationSn,
                "AUTH_ACCOUNT_MISMATCH_ORIGINAL_COMPLETE");
        BalanceSnapshot beforeFailure = snapshot(balances(originalAccount,
                mismatchedAccount,
                cashMappingAccount(),
                settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> reverseAuthorization(mismatchedAccount, 20L, authorizationSn,
                "AUTH_ACCOUNT_MISMATCH_REVERSAL"))
                .hasMessageContaining("授权引用主体与请求账户不一致");
        assertThatThrownBy(() -> completeAuthorization(mismatchedAccount, 20L, authorizationSn,
                "AUTH_ACCOUNT_MISMATCH_COMPLETE"))
                .hasMessageContaining("授权引用主体与请求账户不一致");
        assertThatThrownBy(() -> refundCompletedAuthorization(mismatchedAccount, 20L, authorizationSn,
                "AUTH_ACCOUNT_MISMATCH_REFUND"))
                .hasMessageContaining("授权引用主体与请求账户不一致");
        assertThatThrownBy(() -> completeAuthorization(mismatchedAccount, 40L, authorizationSn,
                "AUTH_ACCOUNT_MISMATCH_ORIGINAL_COMPLETE"))
                .hasMessageContaining("授权引用主体与请求账户不一致");

        BalanceSnapshot afterFailure = snapshot(balances(originalAccount,
                mismatchedAccount,
                cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(originalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(originalAccount, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(mismatchedAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(100L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(40L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_ACCOUNT_MISMATCH_REVERSAL");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_ACCOUNT_MISMATCH_COMPLETE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_ACCOUNT_MISMATCH_REFUND");
    }

    /**
     * 场景：用户授权 80 后只完成 50，平台结算户另有充足余额时尝试拒付 60。
     * 输入：A 充值并授权 80、完成 50；B 另完成 100 使平台 SETTLEMENT 余额充足；A 争议退款 60。
     * 输出：A 争议退款请求失败，A/B/平台余额、交易累计和账务事实保持失败前状态。
     * 预期：争议退款以本交易已完成可回退金额为上限，不以授权金额或平台总余额为上限。
     * 红线：失败争议退款不得借用其他交易沉淀在 SETTLEMENT 的余额，不得写入 AUTH_REFUND 账务事实。
     */
    @Test
    void testAuthorizationDisputeRefundExceedingCompletedAmountShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId reserveUser = fundingAccount("settlement_reserve_user");
        ensureLedger(reserveUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(reserveUser, LedgerSubjectCode.AUTHORIZATION);

        BalanceSnapshot beforeTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_DISPUTE_REFUND_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_DISPUTE_REFUND_EXCEED_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 50L, authorizationSn, "AUTH_DISPUTE_REFUND_EXCEED_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        topup(reserveUser, 100L, "AUTH_DISPUTE_REFUND_EXCEED_RESERVE_TOPUP");
        BalanceSnapshot afterReserveTopup = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterComplete, afterReserveTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String reserveAuthorizationSn = authorize(reserveUser, 100L, true,
                "AUTH_DISPUTE_REFUND_EXCEED_RESERVE_AUTHORIZE");
        BalanceSnapshot afterReserveAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveTopup, afterReserveAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(reserveUser, 100L, reserveAuthorizationSn,
                "AUTH_DISPUTE_REFUND_EXCEED_RESERVE_CAPTURE");

        BalanceSnapshot beforeFailure = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveAuthorize, beforeFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, -100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 100L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 30L, CURRENCY);
        assertBucket(balance(reserveUser), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(reserveUser), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_200L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 150L, CURRENCY);

        assertThatThrownBy(() -> authorizationTransactionService.refund(
                disputeRefundRequest(user, 60L, authorizationSn, "AUTH_DISPUTE_REFUND_EXCEED_RETURN"),
                WindOperatorFactory.system()))
                .hasMessageContaining("资金交易已完成可退金额不足");

        BalanceSnapshot afterFailure = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(6);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_CAPTURE", 0, 2, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_RESERVE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_RESERVE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_RESERVE_CAPTURE", 0, 2, 1, 2);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_EXCEED_RETURN");
    }

    /**
     * 场景：用户授权 80 后只完成 50，平台结算户另有充足余额时尝试退款 60。
     * 输入：A 充值并授权 80、完成 50；B 另完成 100 使平台 SETTLEMENT 余额充足；A 退款 60。
     * 输出：A 退款请求失败，A/B/平台余额、交易累计和账务事实保持失败前状态。
     * 预期：授权完成后退款以本交易已完成可回退金额为上限，不以授权金额或平台总余额为上限。
     * 红线：失败退款不得借用其他交易沉淀在 SETTLEMENT 的余额，不得写入 AUTH_REFUND 账务事实。
     */
    @Test
    void testAuthorizationRefundExceedingCompletedAmountShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId reserveUser = fundingAccount("settlement_reserve_user");
        ensureLedger(reserveUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(reserveUser, LedgerSubjectCode.AUTHORIZATION);

        BalanceSnapshot beforeTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_REFUND_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_REFUND_EXCEED_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 50L, authorizationSn, "AUTH_REFUND_EXCEED_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        topup(reserveUser, 100L, "AUTH_REFUND_EXCEED_RESERVE_TOPUP");
        BalanceSnapshot afterReserveTopup = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterComplete, afterReserveTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String reserveAuthorizationSn = authorize(reserveUser, 100L, true,
                "AUTH_REFUND_EXCEED_RESERVE_AUTHORIZE");
        BalanceSnapshot afterReserveAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveTopup, afterReserveAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(reserveUser, 100L, reserveAuthorizationSn, "AUTH_REFUND_EXCEED_RESERVE_CAPTURE");

        BalanceSnapshot beforeFailure = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveAuthorize, beforeFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, -100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 100L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 30L, CURRENCY);
        assertBucket(balance(reserveUser), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(reserveUser), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_200L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 150L, CURRENCY);

        assertThatThrownBy(() -> refundCompletedAuthorization(user, 60L, authorizationSn,
                "AUTH_REFUND_EXCEED_RETURN"))
                .hasMessageContaining("资金交易已完成可退金额不足");

        BalanceSnapshot afterFailure = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getCompletedAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(6);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_CAPTURE", 0, 2, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RESERVE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RESERVE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RESERVE_CAPTURE", 0, 2, 1, 2);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RETURN");
    }

    /**
     * 场景：授权批准使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 60，随后同流水同金额重试，再同流水改金额为 61。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次授权后的状态。
     * 预期：授权批准幂等必须由业务键和请求摘要共同保护。
     * 红线：同业务流水不同授权批准请求不得重复冻结、不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testAuthorizeSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_AUTHORIZE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_IDEMPOTENT_AUTHORIZE");
        BalanceSnapshot afterFirstAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstAuthorizeFacts = ledgerFactSnapshot();

        String retryAuthorizationSn = authorize(user, 60L, true, "AUTH_IDEMPOTENT_AUTHORIZE");
        BalanceSnapshot afterRetryAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryAuthorizationSn).isEqualTo(authorizationSn);
        assertOnlyBalanceDeltas(afterFirstAuthorize, afterRetryAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstAuthorizeFacts);
        assertThatThrownBy(() -> authorize(user, 61L, true, "AUTH_IDEMPOTENT_AUTHORIZE"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryAuthorize, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstAuthorizeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_AUTHORIZE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_AUTHORIZE", 1, 2);
    }

    @Test
    void testAuthorizeWithMerchantInfoShouldReplayOnlyTheSameCanonicalFacts() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_MERCHANT_INFO_TOPUP");
        MerchantInfoRequest merchantInfo = new MerchantInfoRequest()
                .setMerchantId("merchant_001")
                .setMerchantName("Example Store")
                .setMccCode("5411");
        FundsAuthorizationTransactionAuthorizeRequest request = new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setApproved(true)
                .setMerchantInfo(merchantInfo)
                .setBusinessScene("AUTHORIZATION")
                .setBusinessSn("AUTH_MERCHANT_INFO_AUTHORIZE");

        String authorizationSn = authorizationTransactionService.authorize(request, WindOperatorFactory.system());
        String replaySn = authorizationTransactionService.authorize(request, WindOperatorFactory.system());

        assertThat(replaySn).isEqualTo(authorizationSn);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_MERCHANT_INFO_AUTHORIZE"))
                .singleElement()
                .satisfies(detail -> assertThat(contextVariablesOf(detail.getContextVariables()))
                        .containsEntry(FundsInstructionContextKeys.MERCHANT_INFO, Map.of(
                                "merchantId", "merchant_001",
                                "merchantName", "Example Store",
                                "mccCode", "5411")));

        request.setMerchantInfo(new MerchantInfoRequest()
                .setMerchantId("merchant_001")
                .setMerchantName("Example Store")
                .setMccCode("5999"));
        assertThatThrownBy(() -> authorizationTransactionService.authorize(request, WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_MERCHANT_INFO_AUTHORIZE", 1, 2);
    }

    /**
     * 场景：授权批准使用相同业务流水重复提交，但第二次把授权账户换成新的主体。
     * 输入：两个账户各充值 100，第一次账户 A 授权 60，随后同业务流水改为账户 B 授权 60。
     * 输出：第二次请求被幂等摘要拒绝；账户 B 不冻结，授权聚合和账务事实保持第一次授权后的状态。
     * 预期：授权批准幂等必须覆盖授权主体，不只覆盖金额。
     * 红线：同业务流水不同授权主体不得新增 detail、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testAuthorizeSameBusinessSnWithDifferentAccountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId anotherUser = fundingAccount("auth_user2");
        ensureLedger(anotherUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(anotherUser, LedgerSubjectCode.AUTHORIZATION);

        BalanceSnapshot beforeTopup = snapshot(balances(user, anotherUser, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_ACCOUNT_TOPUP");
        BalanceSnapshot afterUserTopup = snapshot(balances(user, anotherUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterUserTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        topup(anotherUser, 100L, "AUTH_IDEMPOTENT_ACCOUNT_ANOTHER_TOPUP");
        BalanceSnapshot afterAnotherTopup = snapshot(balances(user, anotherUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterUserTopup, afterAnotherTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_IDEMPOTENT_ACCOUNT");
        BalanceSnapshot afterFirstAuthorize = snapshot(balances(user, anotherUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterAnotherTopup, afterFirstAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstAuthorizeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorize(anotherUser, 60L, true, "AUTH_IDEMPOTENT_ACCOUNT"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, anotherUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterFirstAuthorize, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstAuthorizeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_200L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_ACCOUNT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_ACCOUNT_ANOTHER_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_ACCOUNT", 1, 2);
    }

    /**
     * 场景：授权撤销使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、撤销 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次撤销后的状态。
     * 预期：授权撤销幂等必须保护原授权引用、撤销金额和原 route replay 摘要。
     * 红线：同业务流水不同撤销请求不得重复释放授权占用或污染授权累计金额。
     */
    @Test
    void testAuthorizationReversalSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_REVERSAL_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_REVERSAL_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String firstReversalSn = reverseAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REVERSAL_CANCEL");
        BalanceSnapshot afterFirstReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstReversalFacts = ledgerFactSnapshot();

        String retryReversalSn = reverseAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REVERSAL_CANCEL");
        BalanceSnapshot afterRetryReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryReversalSn).isEqualTo(firstReversalSn);
        assertOnlyBalanceDeltas(afterFirstReversal, afterRetryReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstReversalFacts);
        assertThatThrownBy(() -> reverseAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_REVERSAL_CANCEL"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryReversal, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstReversalFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(30L);
        assertThat(transaction.getCompletedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(2);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_REVERSAL_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REVERSAL_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REVERSAL_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL", 0, 1, 1, 2);
    }

    /**
     * 场景：授权完成使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、完成 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次完成后的状态。
     * 预期：授权完成幂等必须保护原授权引用、完成金额和原 route replay 摘要。
     * 红线：同业务流水不同完成请求不得重复扣减 AUTHORIZATION、不得重复增加 SETTLEMENT。
     */
    @Test
    void testAuthorizationCompleteSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_COMPLETE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_COMPLETE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String firstCompleteSn = completeAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_COMPLETE_CAPTURE");
        BalanceSnapshot afterFirstComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));
        LedgerFactSnapshot afterFirstCompleteFacts = ledgerFactSnapshot();

        String retryCompleteSn = completeAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_COMPLETE_CAPTURE");
        BalanceSnapshot afterRetryComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryCompleteSn).isEqualTo(firstCompleteSn);
        assertOnlyBalanceDeltas(afterFirstComplete, afterRetryComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstCompleteFacts);
        assertThatThrownBy(() -> completeAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_COMPLETE_CAPTURE"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryComplete, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstCompleteFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isEqualTo(30L);
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(3);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_COMPLETE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_COMPLETE_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_COMPLETE_CAPTURE")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_COMPLETE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_COMPLETE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_COMPLETE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_COMPLETE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：授权完成后退款使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、完成 50、退款 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次退款后的状态。
     * 预期：授权退款幂等必须保护原授权引用、退款金额和原完成路径回放摘要。
     * 红线：同业务流水不同退款请求不得重复回补 AVAILABLE、不得重复扣减 SETTLEMENT。
     */
    @Test
    void testAuthorizationRefundSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_REFUND_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(user, 50L, authorizationSn, "AUTH_IDEMPOTENT_REFUND_CAPTURE");
        BalanceSnapshot afterComplete = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        String firstRefundSn = refundCompletedAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REFUND_RETURN");
        BalanceSnapshot afterFirstRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterComplete, afterFirstRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY));
        LedgerFactSnapshot afterFirstRefundFacts = ledgerFactSnapshot();

        String retryRefundSn = refundCompletedAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REFUND_RETURN");
        BalanceSnapshot afterRetryRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryRefundSn).isEqualTo(firstRefundSn);
        assertOnlyBalanceDeltas(afterFirstRefund, afterRetryRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertThatThrownBy(() -> refundCompletedAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_REFUND_RETURN"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryRefund, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 30L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getState()).isEqualTo(FundsTransactionState.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getCompletedAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(30L);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.COMPLETE.name(),
                        FundsTransactionEventType.AUTH_REFUND.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(5);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_REFUND_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN", 0, 2, 1, 2);
    }

    private RaceOutcome raceCommand(CountDownLatch ready,
                                    CountDownLatch start,
                                    String businessSn,
                                    FundsTransactionEventType eventType,
                                    RaceCommand command) {
        try {
            TenantContextHolder.setTenantId(TENANT_ID);
            ready.countDown();
            awaitLatch(start);
            return RaceOutcome.success(businessSn, eventType, command.execute());
        } catch (Throwable failure) {
            return RaceOutcome.failure(businessSn, eventType, failure);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as("race start signal received")
                    .isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static RaceOutcome awaitOutcome(Future<RaceOutcome> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(10, TimeUnit.SECONDS);
    }

    private static void assertNoAuthRefundContext(String contextVariables) {
        Map<String, Object> context = contextVariablesOf(contextVariables);

        assertThat(context)
                .containsEntry(FundsInstructionContextKeys.REFUND_MODE,
                        FundsInstructionContextKeys.REFUND_MODE_NO_AUTH)
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_REFERENCE_SN,
                        "processor_capture_202606030001")
                .containsEntry(FundsInstructionContextKeys.REFUND_REASON,
                        "external capture refunded without internal authorization");
        assertThat(context.keySet())
                .doesNotContain(
                        "externalOriginalFactRef",
                        "externalOriginalFactType",
                        "refundVoucherRef",
                        "originalFactAmount",
                        "originalFactCurrency",
                        FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN);
    }

    private static void assertDisputeRefundContext(String contextVariables) {
        assertThat(contextVariablesOf(contextVariables))
                .containsEntry(FundsInstructionContextKeys.REFUND_MODE,
                        FundsInstructionContextKeys.REFUND_MODE_DISPUTE)
                .containsEntry(FundsInstructionContextKeys.DISPUTE_MODE, "CHARGEBACK")
                .containsEntry(FundsInstructionContextKeys.DISPUTE_REASON, "CARDHOLDER_DISPUTE")
                .containsEntry(FundsInstructionContextKeys.DISPUTE_VOUCHER_REF,
                        "DISPUTE_EVIDENCE_202605290001")
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF,
                        "DISPUTE_CASE_202605290001");
    }

    private static void assertForceCompletionContext(String contextVariables) {
        assertThat(contextVariablesOf(contextVariables))
                .containsEntry(FundsInstructionContextKeys.COMPLETION_MODE, "FORCE")
                .containsEntry(FundsInstructionContextKeys.FORCE_COMPLETION_POLICY_CODE,
                        "B4_FORCE_COMPLETION_OPS")
                .containsEntry(FundsInstructionContextKeys.EXTERNAL_ORIGINAL_FACT_REF,
                        "processor_settlement_202606020001")
                .containsEntry(FundsInstructionContextKeys.FORCE_COMPLETION_VOUCHER_REF,
                        "ops_voucher_202606020001");
    }

    private void assertNoAuthRefundRouteCode(String transactionSn) {
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, transactionSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getRouteCode())
                        .isEqualTo(FundsRouteCodes.AUTHORIZATION_NO_AUTH_REFUND_STANDARD)
                        .isNotEqualTo(FundsRouteCodes.AUTHORIZATION_REFUND_REPLAY));
    }

    private void ensurePeriodLedger(FundsAccountId accountId,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    AccountBalancePeriodType periodType,
                                    String periodId,
                                    long initialBalance) {
        ensureFundingAccount(accountId);
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(ledgerSubjectCode == LedgerSubjectCode.AVAILABLE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(periodType)
                .setPeriodId(periodId));
        if (initialBalance != 0L) {
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledgerService.getLedgerById(ledgerId),
                    initialBalance > 0L ? EntrySide.CREDIT : EntrySide.DEBIT,
                    Math.abs(initialBalance))), LedgerPostingAccessType.NORMAL);
        }
    }

    private String authorizeWithLedgerPeriod(FundsAccountId accountId,
                                             long amount,
                                             String businessSn,
                                             AccountBalancePeriodType periodType,
                                             String periodId) {
        return authorizationTransactionService.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setApproved(true)
                .setLedgerPeriodType(periodType)
                .setLedgerPeriodId(periodId)
                .setBusinessScene("AUTHORIZATION")
                .setBusinessSn(businessSn)
                .setDescription("authorization with ledger period"), WindOperatorFactory.system());
    }

    private void assertPeriodLedgerBalance(FundsAccountId accountId,
                                           LedgerSubjectCode ledgerSubjectCode,
                                           AccountBalancePeriodType periodType,
                                           String periodId,
                                           long normalBalance) {
        assertThat(periodLedger(accountId, ledgerSubjectCode, periodType, periodId).getNormalBalance())
                .isEqualTo(normalBalance);
    }

    private LedgerDTO periodLedger(FundsAccountId accountId,
                                   LedgerSubjectCode ledgerSubjectCode,
                                   AccountBalancePeriodType periodType,
                                   String periodId) {
        List<LedgerDTO> ledgers = ledgerService.queryLedgers(new LedgerQuery()
                                .setTenantId(TENANT_ID)
                                .setSubjectId(accountId.id())
                                .setSubjectType(accountId.type())
                                .setLedgerSubjectCode(ledgerSubjectCode)
                                .setCurrency(CURRENCY)
                                .setPeriodType(periodType)
                                .setPeriodId(periodId),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords()
                .stream()
                .toList();
        assertThat(ledgers)
                .as("period ledger for accountId %s subject %s period %s/%s",
                        accountId, ledgerSubjectCode, periodType, periodId)
                .singleElement();
        return ledgers.getFirst();
    }

    private void assertLedgerEntriesPeriod(String businessSn,
                                           AccountBalancePeriodType periodType,
                                           String periodId) {
        assertThat(entriesOf(ledgerTransactionByBusinessSn(businessSn)))
                .isNotEmpty()
                .allSatisfy(entry -> {
                    assertThat(entry.getPeriodType()).isEqualTo(periodType);
                    assertThat(entry.getPeriodId()).isEqualTo(periodId);
                });
    }

    private FundsActionFactDTO assertAuthorizationActionFact(String businessSn,
                                                             long amount,
                                                             String outcome,
                                                             String effectKind) {
        List<FundsActionFactDTO> actionFacts = actionFactsByBusiness("AUTHORIZATION", businessSn);
        assertThat(actionFacts).singleElement();
        FundsActionFactDTO actionFact = actionFacts.getFirst();
        Money expectedMoney = Money.immutable(amount, CURRENCY);
        assertThat(actionFact.getIdentity().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(actionFact.getIdentity().getIdentity()).isNotBlank();
        assertThat(actionFact.getIntentRef()).isNotBlank();
        assertThat(actionFact.getAttemptRef()).isNotBlank();
        assertThat(actionFact.getActionKind()).isEqualToIgnoringCase("authorize");
        assertThat(actionFact.getMoney()).isEqualTo(expectedMoney);
        assertThat(actionFact.getOutcome().getOwner()).isEqualTo("funds-transaction");
        assertThat(actionFact.getOutcome().getCode()).isEqualToIgnoringCase(outcome);
        assertThat(actionFact.getFundsEffect().getEffectKind()).isEqualToIgnoringCase(effectKind);
        if ("proven-zero".equalsIgnoreCase(effectKind)) {
            assertThat(actionFact.getFundsEffect().getProvenMoney()).isNull();
        } else {
            assertThat(actionFact.getFundsEffect().getProvenMoney()).isEqualTo(expectedMoney);
        }
        assertThat(actionFact.getSemanticDigest().getAlgorithm()).isEqualTo("SHA-256");
        assertThat(actionFact.getSemanticDigest().getValue()).matches("[0-9a-f]{64}");
        assertThat(actionFact.getSemanticDigest().getCoveredFieldsVersion())
                .isEqualTo("transaction.action.authorization.projection.v1");
        assertThat(actionFact.getOriginalFundsFactRefs()).isEmpty();
        assertThat(actionFact.getRouteProvenance()).singleElement().satisfies(provenance -> {
            assertThat(provenance.getOriginalFundsFactRef()).isNull();
            assertThat(provenance.getAllocatedMoney()).isEqualTo(expectedMoney);
            assertThat(provenance.getRouteSnapshotRef().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(provenance.getRouteSnapshotRef().getIdentity().getOwnerNamespace())
                    .isEqualTo("funds-route-snapshot");
            assertThat(provenance.getRouteSnapshotRef().getIdentity().getValue()).isNotBlank();
        });
        assertThat(fundsTransactionQueryService.findFundsActionFact(actionFact.getIdentity())).hasValue(actionFact);
        return actionFact;
    }

    private void assertAuthorizationCompletePhysicalFacts(String authorizationSn,
                                                          String authorizationBusinessSn,
                                                          String completeBusinessSn,
                                                          int expectedDetails,
                                                          int expectedPostingPlans,
                                                          int expectedEntries) {
        assertFundsAndLedgerFactsForBusinessSn(completeBusinessSn, 0, expectedDetails,
                expectedPostingPlans, expectedEntries);
        LedgerTransaction authorizationLedger = ledgerTransactionByBusinessSn(authorizationBusinessSn);
        LedgerTransaction completeLedger = ledgerTransactionByBusinessSn(completeBusinessSn);
        assertThat(completeLedger.getReferenceLedgerTransactionSn()).isEqualTo(authorizationLedger.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn(completeBusinessSn))
                .allSatisfy(detail -> {
                    assertThat(detail.getReferenceDetailSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getReferenceLedgerTransactionSn()).isEqualTo(authorizationLedger.getSn());
                });
        assertThat(postingPlansOf(completeLedger))
                .allSatisfy(plan -> assertThat(plan.getRouteLegId()).startsWith("CONSUME_"));
    }

    private FundsActionFactDTO assertCompleteActionFact(String authorizationSn,
                                                        FundsActionFactDTO authorizationFact,
                                                        String businessSn,
                                                        long amount,
                                                        int provenanceCount) {
        List<FundsActionFactDTO> actionFacts = actionFactsByBusiness("AUTHORIZATION_COMPLETE", businessSn);
        assertThat(actionFacts).singleElement();
        FundsActionFactDTO actionFact = actionFacts.getFirst();
        Money expectedMoney = Money.immutable(amount, CURRENCY);
        String expectedIdentity = authorizationSn + ":complete:AUTHORIZATION_COMPLETE:" + businessSn;
        assertThat(actionFact.getIdentity().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(actionFact.getIdentity().getIdentity()).isEqualTo(expectedIdentity);
        assertThat(actionFact.getIntentRef()).isEqualTo(authorizationSn);
        assertThat(actionFact.getAttemptRef())
                .isEqualTo(authorizationSn + ":AUTHORIZATION_COMPLETE:" + businessSn + ":COMPLETE");
        assertThat(actionFact.getActionKind()).isEqualTo("complete");
        assertThat(actionFact.getMoney()).isEqualTo(expectedMoney);
        assertThat(actionFact.getOutcome().getOwner()).isEqualTo("funds-transaction");
        assertThat(actionFact.getOutcome().getCode()).isEqualTo("succeeded");
        assertThat(actionFact.getFundsEffect().getEffectKind()).isEqualTo("proven-full");
        assertThat(actionFact.getFundsEffect().getProvenMoney()).isEqualTo(expectedMoney);
        assertThat(actionFact.getSemanticDigest().getAlgorithm()).isEqualTo("SHA-256");
        assertThat(actionFact.getSemanticDigest().getValue()).matches("[0-9a-f]{64}");
        assertThat(actionFact.getSemanticDigest().getCoveredFieldsVersion())
                .isEqualTo("transaction.action.complete.projection.v1");
        assertThat(actionFact.getOriginalFundsFactRefs()).singleElement().satisfies(originalRef -> {
            assertThat(originalRef.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(originalRef.getFactType()).isEqualTo("funds-action");
            assertThat(originalRef.getFactId()).isEqualTo(authorizationFact.getIdentity().getIdentity());
            assertThat(originalRef.getRelationRole()).isEqualTo("consumes-authorized-effect");
            assertThat(originalRef.getAllocatedMoney()).isEqualTo(expectedMoney);
        });
        assertThat(actionFact.getRouteProvenance()).hasSize(provenanceCount).allSatisfy(provenance -> {
            assertThat(provenance.getOriginalFundsFactRef()).isEqualTo(actionFact.getOriginalFundsFactRefs().getFirst());
            assertThat(provenance.getAllocatedMoney()).isEqualTo(expectedMoney);
            assertThat(provenance.getRouteSnapshotRef())
                    .isEqualTo(authorizationFact.getRouteProvenance().getFirst().getRouteSnapshotRef());
            assertThat(provenance.getProvenanceRole()).isEqualTo("replayed-original-route");
        });
        assertThat(fundsTransactionQueryService.findFundsActionFact(actionFact.getIdentity())).hasValue(actionFact);
        return actionFact;
    }

    private void assertAuthorizationReleasePhysicalFacts(String authorizationSn,
                                                         String authorizationBusinessSn,
                                                         String releaseBusinessScene,
                                                         String releaseBusinessSn,
                                                         long releaseAmount,
                                                         int expectedDetails,
                                                         int expectedPostingPlans,
                                                         int expectedEntries) {
        LedgerTransaction authorizationLedger = ledgerTransactionByBusinessSn(authorizationBusinessSn);
        List<LedgerTransaction> releaseLedgers = ledgerTransactionsForBusinessSn(releaseBusinessSn).stream()
                .filter(ledger -> authorizationSn.equals(ledger.getFundsTransactionSn()))
                .filter(ledger -> FundsTransactionEventType.REVERSAL.name().equals(ledger.getEventType()))
                .filter(ledger -> releaseBusinessScene.equals(ledger.getBusinessScene()))
                .toList();
        assertThat(releaseLedgers).singleElement();
        LedgerTransaction releaseLedger = releaseLedgers.getFirst();
        assertThat(releaseLedger.getAmount()).isEqualTo(releaseAmount);
        assertThat(releaseLedger.getCurrency()).isEqualTo(CURRENCY);
        assertThat(releaseLedger.getReferenceLedgerTransactionSn()).isEqualTo(authorizationLedger.getSn());
        var authorizationRoute = fundsTransactionQueryService.findRouteSnapshotByTransactionSn(
                        TENANT_ID, authorizationSn)
                .orElseThrow();
        List<FundsTransactionDetail> releaseDetails = fundsTransactionDetailsByBusinessSn(releaseBusinessSn).stream()
                .filter(detail -> authorizationSn.equals(detail.getTransactionSn()))
                .filter(detail -> releaseBusinessScene.equals(detail.getBusinessScene()))
                .filter(detail -> detail.getEventType() == FundsTransactionEventType.REVERSAL)
                .toList();
        assertThat(releaseDetails)
                .hasSize(expectedDetails)
                .allSatisfy(detail -> {
                    assertThat(detail.getTransactionSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getBusinessScene()).isEqualTo(releaseBusinessScene);
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.REVERSAL);
                    assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.RELEASE);
                    assertThat(detail.getState()).isEqualTo(FundsTransactionDetailState.SUCCEEDED);
                    assertThat(detail.getReferenceDetailSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getReferenceLedgerTransactionSn()).isEqualTo(authorizationLedger.getSn());
                    assertThat(detail.getLedgerTransactionSn()).isEqualTo(releaseLedger.getSn());
                    assertThat(detail.getAmount()).isEqualTo(releaseAmount);
                    assertThat(detail.getCurrency()).isEqualTo(CURRENCY);

                    var matchingAuthorizationLegs = authorizationRoute.getLegs().stream()
                            .filter(leg -> detail.getSubjectId()
                                    .equals(leg.getSourceNode().getSubjectRef().getSubjectId()))
                            .filter(leg -> detail.getSubjectType()
                                    .equals(leg.getSourceNode().getSubjectRef().getSubjectType().name()))
                            .toList();
                    assertThat(matchingAuthorizationLegs).singleElement();
                    var authorizationLeg = matchingAuthorizationLegs.getFirst();
                    assertThat(authorizationLeg.getTargetNode().getSubjectRef().getSubjectId())
                            .isEqualTo(detail.getSubjectId());
                    assertThat(authorizationLeg.getTargetNode().getSubjectRef().getSubjectType().name())
                            .isEqualTo(detail.getSubjectType());

                    Map<String, Object> context = contextVariablesOf(detail.getContextVariables());
                    Object replayLegIdsValue = context.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_IDS);
                    Object replayAmountsValue = context.get(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS);
                    assertThat(replayLegIdsValue).isInstanceOf(List.class);
                    assertThat(((List<?>) replayLegIdsValue).stream().map(Object::toString).toList())
                            .containsExactly(authorizationLeg.getLegId());
                    assertThat(replayAmountsValue).isInstanceOf(Map.class);
                    Map<?, ?> replayAmounts = (Map<?, ?>) replayAmountsValue;
                    assertThat(replayAmounts).hasSize(1);
                    assertThat(replayAmounts.containsKey(authorizationLeg.getLegId())).isTrue();
                    assertThat(replayAmounts.get(authorizationLeg.getLegId())).isInstanceOf(Number.class);
                    Number replayAmount = (Number) replayAmounts.get(authorizationLeg.getLegId());
                    assertThat(new BigDecimal(replayAmount.toString()))
                            .isEqualByComparingTo(BigDecimal.valueOf(releaseAmount));
                });
        List<LedgerPostingPlan> releasePlans = postingPlansOf(releaseLedger);
        assertThat(releasePlans)
                .hasSize(expectedPostingPlans)
                .extracting(LedgerPostingPlan::getRouteLegId)
                .containsExactlyInAnyOrderElementsOf(authorizationRoute.getLegs().stream()
                        .map(leg -> "RELEASE_" + leg.getLegId())
                        .toList());
        assertThat(releasePlans).allSatisfy(plan -> {
            assertThat(plan.getAmount()).isEqualTo(releaseAmount);
            assertThat(plan.getCurrency()).isEqualTo(CURRENCY);
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.AUTHORIZATION_REVERSAL.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_HOLD.name());
            assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.REVERSAL.name());
        });
        assertThat(entriesOf(releaseLedger))
                .hasSize(expectedEntries)
                .allSatisfy(entry -> assertThat(entry.getLedgerTransactionSn()).isEqualTo(releaseLedger.getSn()));
    }

    private BalanceSnapshot assertFundingReleaseBalanceDelta(BalanceSnapshot beforeRelease,
                                                              FundsAccountId accountId,
                                                              long releaseAmount) {
        BalanceSnapshot afterRelease = snapshot(balances(accountId, settlementAccount()));
        assertOnlyBalanceDeltas(beforeRelease, afterRelease,
                delta(accountId, LedgerSubjectCode.AVAILABLE, releaseAmount, CURRENCY),
                delta(accountId, LedgerSubjectCode.AUTHORIZATION, -releaseAmount, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        return afterRelease;
    }

    private void assertSharedReleaseBalanceDelta(BalanceSnapshot beforeRelease,
                                                 FundsAccountId cardAccount,
                                                 FundsAccountId parentAccount,
                                                 long releaseAmount) {
        assertOnlyBalanceDeltas(beforeRelease, snapshot(balances(cardAccount, parentAccount)),
                delta(cardAccount, LedgerSubjectCode.AVAILABLE, releaseAmount, CURRENCY),
                delta(cardAccount, LedgerSubjectCode.AUTHORIZATION, -releaseAmount, CURRENCY),
                delta(parentAccount, LedgerSubjectCode.AVAILABLE, releaseAmount, CURRENCY),
                delta(parentAccount, LedgerSubjectCode.AUTHORIZATION, -releaseAmount, CURRENCY));
    }

    private FundsActionFactDTO assertReleaseActionFact(String authorizationSn,
                                                       FundsActionFactDTO authorizationFact,
                                                       String businessScene,
                                                       String businessSn,
                                                       long amount,
                                                       int provenanceCount) {
        List<FundsActionFactDTO> actionFacts = actionFactsByBusiness(businessScene, businessSn);
        assertThat(actionFacts)
                .as("release action fact missing for %s/%s", businessScene, businessSn)
                .singleElement();
        FundsActionFactDTO actionFact = actionFacts.getFirst();
        Money expectedMoney = Money.immutable(amount, CURRENCY);
        assertThat(actionFact.getIdentity().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(actionFact.getIdentity().getIdentity())
                .isEqualTo(releaseIdentity(authorizationSn, businessScene, businessSn));
        assertThat(actionFact.getIntentRef())
                .isEqualTo(releaseIntentRef(authorizationSn, businessScene, businessSn));
        assertThat(actionFact.getAttemptRef())
                .isEqualTo(releaseAttemptRef(authorizationSn, businessScene, businessSn));
        assertThat(actionFact.getActionKind()).isEqualTo("release");
        assertThat(actionFact.getMoney()).isEqualTo(expectedMoney);
        assertThat(actionFact.getOutcome().getOwner()).isEqualTo("funds-transaction");
        assertThat(actionFact.getOutcome().getCode()).isEqualTo("succeeded");
        assertThat(actionFact.getFundsEffect().getEffectKind()).isEqualTo("proven-full");
        assertThat(actionFact.getFundsEffect().getProvenMoney()).isEqualTo(expectedMoney);
        assertThat(actionFact.getSemanticDigest().getAlgorithm()).isEqualTo("SHA-256");
        assertThat(actionFact.getSemanticDigest().getValue()).matches("[0-9a-f]{64}");
        assertThat(actionFact.getSemanticDigest().getCoveredFieldsVersion())
                .isEqualTo("transaction.action.release.projection.v1");
        assertThat(actionFact.getOriginalFundsFactRefs()).singleElement().satisfies(originalRef -> {
            assertThat(originalRef.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(originalRef.getFactType()).isEqualTo("funds-action");
            assertThat(originalRef.getFactId()).isEqualTo(authorizationFact.getIdentity().getIdentity());
            assertThat(originalRef.getRelationRole()).isEqualTo("releases-authorized-effect");
            assertThat(originalRef.getAllocatedMoney()).isEqualTo(expectedMoney);
        });
        assertThat(actionFact.getRouteProvenance()).hasSize(provenanceCount).allSatisfy(provenance -> {
            assertThat(provenance.getOriginalFundsFactRef())
                    .isEqualTo(actionFact.getOriginalFundsFactRefs().getFirst());
            assertThat(provenance.getAllocatedMoney()).isEqualTo(expectedMoney);
            assertThat(provenance.getRouteSnapshotRef())
                    .isEqualTo(authorizationFact.getRouteProvenance().getFirst().getRouteSnapshotRef());
            assertThat(provenance.getProvenanceRole()).isEqualTo("replayed-original-route");
        });
        assertThat(fundsTransactionQueryService.findFundsActionFact(actionFact.getIdentity())).hasValue(actionFact);
        return actionFact;
    }

    private void assertReleaseActionFactUnavailable(FundsActionFactDTO originalFact,
                                                    String businessScene,
                                                    String businessSn) {
        assertNoActionFacts(businessScene, businessSn);
        assertThat(fundsTransactionQueryService.findFundsActionFact(originalFact.getIdentity())).isEmpty();
    }

    private void assertReleaseActionFactUnavailable(String authorizationSn,
                                                    String businessScene,
                                                    String businessSn) {
        assertNoActionFacts(businessScene, businessSn);
        assertThat(fundsTransactionQueryService.findFundsActionFact(new FundsActionFactRef(
                TENANT_ID, releaseIdentity(authorizationSn, businessScene, businessSn)))).isEmpty();
    }

    private void assertReleaseActionFactAvailable(FundsActionFactDTO originalFact,
                                                  String businessScene,
                                                  String businessSn) {
        assertThat(actionFactsByBusiness(businessScene, businessSn)).containsExactly(originalFact);
        assertThat(fundsTransactionQueryService.findFundsActionFact(originalFact.getIdentity()))
                .hasValue(originalFact);
    }

    private static String releaseIdentity(String authorizationSn, String businessScene, String businessSn) {
        return "release:v1:" + encodedIdentityPart(authorizationSn) + ":"
                + encodedIdentityPart(businessScene) + ":" + encodedIdentityPart(businessSn);
    }

    private static String releaseIntentRef(String authorizationSn, String businessScene, String businessSn) {
        return "release-intent:v1:" + encodedIdentityPart(authorizationSn) + ":"
                + encodedIdentityPart(businessScene) + ":" + encodedIdentityPart(businessSn);
    }

    private static String releaseAttemptRef(String authorizationSn, String businessScene, String businessSn) {
        return "release-attempt:v1:" + encodedIdentityPart(authorizationSn) + ":"
                + encodedIdentityPart(businessScene) + ":" + encodedIdentityPart(businessSn) + ":REVERSAL";
    }

    private static String encodedIdentityPart(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String authorizeWithBusinessKey(FundsAccountId accountId,
                                            long amount,
                                            String businessScene,
                                            String businessSn) {
        return authorizationTransactionService.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setApproved(true)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setDescription("authorization collision seed"), WindOperatorFactory.system());
    }

    private String completeAuthorizationWithBusinessKey(FundsAccountId accountId,
                                                        long amount,
                                                        String authorizationSn,
                                                        String businessScene,
                                                        String businessSn) {
        return authorizationTransactionService.complete(new FundsAuthorizationTransactionCompleteRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setDescription("authorization complete collision seed"), WindOperatorFactory.system());
    }

    private String reverseAuthorizationWithBusinessKey(FundsAccountId accountId,
                                                       long amount,
                                                       String authorizationSn,
                                                       String businessScene,
                                                       String businessSn) {
        return authorizationTransactionService.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setDescription("authorization release collision seed"), WindOperatorFactory.system());
    }

    private void updateFundsTransactionReversedAmount(String transactionSn, long reversedAmount) {
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction
                SET reversed_amount = ?
                WHERE tenant_id = ? AND sn = ?
                """, reversedAmount, TENANT_ID, transactionSn)).isOne();
    }

    private void updateFundsTransactionDetailReferenceDetailSn(String detailSn, String referenceDetailSn) {
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail
                SET reference_detail_sn = ?
                WHERE tenant_id = ? AND sn = ?
                """, referenceDetailSn, TENANT_ID, detailSn)).isOne();
    }

    private void updateFundsTransactionDetailContextVariables(String detailSn, String contextVariables) {
        assertThat(authorizationJdbcTemplate.update("""
                UPDATE t_funds_transaction_detail
                SET context_variables = ?
                WHERE tenant_id = ? AND sn = ?
                """, contextVariables, TENANT_ID, detailSn)).isOne();
    }

    private void assertCompleteActionFactUnavailable(FundsActionFactDTO originalFact, String businessSn) {
        assertNoActionFacts("AUTHORIZATION_COMPLETE", businessSn);
        assertThat(fundsTransactionQueryService.findFundsActionFact(originalFact.getIdentity())).isEmpty();
    }

    private void assertCompleteActionFactUnavailable(String authorizationSn, String businessSn) {
        assertNoActionFacts("AUTHORIZATION_COMPLETE", businessSn);
        String identity = authorizationSn + ":complete:AUTHORIZATION_COMPLETE:" + businessSn;
        assertThat(fundsTransactionQueryService.findFundsActionFact(new FundsActionFactRef(TENANT_ID, identity)))
                .isEmpty();
    }

    private void assertAuthorizationActionFactUnavailable(FundsActionFactDTO originalFact, String businessSn) {
        assertNoActionFacts("AUTHORIZATION", businessSn);
        assertThat(fundsTransactionQueryService.findFundsActionFact(originalFact.getIdentity())).isEmpty();
    }

    private void assertAuthorizationActionFactAvailable(FundsActionFactDTO originalFact, String businessSn) {
        assertThat(actionFactsByBusiness("AUTHORIZATION", businessSn)).containsExactly(originalFact);
        assertThat(fundsTransactionQueryService.findFundsActionFact(originalFact.getIdentity()))
                .hasValue(originalFact);
    }

    private static Map<String, Object> contextVariablesOf(String contextVariables) {
        if (contextVariables == null || contextVariables.isBlank()) {
            return Map.of();
        }
        return WindJson.parseObject(contextVariables, new TypeReference<>() {
        });
    }

    @FunctionalInterface
    private interface RaceCommand {

        String execute();
    }

    private record RaceOutcome(String businessSn,
                               FundsTransactionEventType eventType,
                               String transactionSn,
                               Throwable failure) {

        private static RaceOutcome success(String businessSn,
                                           FundsTransactionEventType eventType,
                                           String transactionSn) {
            return new RaceOutcome(businessSn, eventType, transactionSn, null);
        }

        private static RaceOutcome failure(String businessSn,
                                           FundsTransactionEventType eventType,
                                           Throwable failure) {
            return new RaceOutcome(businessSn, eventType, null, failure);
        }

        private boolean succeeded() {
            return failure == null;
        }
    }

    private String forceCompletionAuthorization(FundsAccountId accountId, long amount, String businessSn) {
        return authorizationTransactionService.complete(forceCompletionRequest(accountId, amount, businessSn),
                WindOperatorFactory.system());
    }

    private String authorizeSharedCard(FundsAccountId cardAccount,
                                       FundsAccountId parentFundingAccount,
                                       long amount,
                                       String businessSn) {
        return authorizationTransactionService.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(cardAccount)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setApproved(true)
                .setLinkedFundingAccountId(parentFundingAccount)
                .setBusinessScene("AUTHORIZATION")
                .setBusinessSn(businessSn)
                .setDescription("shared card authorization"), WindOperatorFactory.system());
    }

    private FundsAuthorizationTransactionCompleteRequest forceCompletionRequest(FundsAccountId accountId,
                                                                          long amount,
                                                                          String businessSn) {
        return new FundsAuthorizationTransactionCompleteRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setCompletionMode("FORCE")
                .setForceCompletionPolicyCode("B4_FORCE_COMPLETION_OPS")
                .setForceCompletionLimitAmount(amount)
                .setForceCompletionReason("external settlement accepted without internal authorization")
                .setExternalOriginalFactRef("processor_settlement_202606020001")
                .setForceCompletionVoucherRef("ops_voucher_202606020001")
                .setBusinessScene("AUTHORIZATION_FORCE_COMPLETION")
                .setBusinessSn(businessSn)
                .setDescription("authorization force complete");
    }

    private FundsAuthorizationTransactionRefundRequest disputeRefundRequest(FundsAccountId accountId,
                                                                            long amount,
                                                                            String authorizationTransactionSn,
                                                                            String businessSn) {
        return new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationTransactionSn)
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_202605290001")
                .setExternalDisputeRef("DISPUTE_CASE_202605290001")
                .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("authorization dispute refund");
    }
}

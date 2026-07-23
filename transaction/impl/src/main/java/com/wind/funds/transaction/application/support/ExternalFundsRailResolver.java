package com.wind.funds.transaction.application.support;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 外部资金 rail 与交易渠道解析器。
 *
 * <p>将本次外部交易使用的 rail 或已确认外部事件类型解析为交易内核的稳定渠道大类。</p>
 */
public final class ExternalFundsRailResolver {

    private static final String ACH_CREDIT_CONFIRMED = "ACH_CREDIT_CONFIRMED";

    private static final String BANK_CREDIT_CONFIRMED = "BANK_CREDIT_CONFIRMED";

    private static final String BANK_RAIL = "BANK_RAIL";

    private static final Set<String> NON_FINAL_CREDIT_EVENT_SUFFIXES = Set.of(
            "_ACCEPTED",
            "_SUBMITTED",
            "_PROCESSING",
            "_MESSAGE_SENT",
            "_IN_TRANSIT");

    private static final Map<String, ExternalFundsRailDecision> RECEIVE_RAIL_DECISIONS = Map.ofEntries(
            Map.entry(FundsTransactionChannel.BANK_TRANSFER.name(), bankRail("BANK")),
            Map.entry(BANK_RAIL, bankRail("BANK")),
            Map.entry("BANK", bankRail("BANK")),
            Map.entry("WIRE_TRANSFER", bankRail("WIRE")),
            Map.entry("WIRE", bankRail("WIRE")),
            Map.entry("WIRE_RAIL", bankRail("WIRE")),
            Map.entry("LOCAL_BANK", bankRail("LOCAL_BANK")),
            Map.entry("ACH", bankRail("ACH")),
            Map.entry("ACH_RAIL", bankRail("ACH")),
            Map.entry(FundsTransactionChannel.DIGITAL_WALLET.name(),
                    new ExternalFundsRailDecision(FundsTransactionChannel.DIGITAL_WALLET.name(),
                            FundsTransactionChannel.DIGITAL_WALLET)),
            Map.entry("WALLET", new ExternalFundsRailDecision(FundsTransactionChannel.DIGITAL_WALLET.name(),
                    FundsTransactionChannel.DIGITAL_WALLET))
    );

    private static final Map<String, FundsTransactionChannel> INSTRUMENT_CHANNELS = Map.of(
            "VA", FundsTransactionChannel.BANK_TRANSFER,
            "VIRTUAL_ACCOUNT", FundsTransactionChannel.BANK_TRANSFER,
            "BANK_ACCOUNT", FundsTransactionChannel.BANK_TRANSFER,
            "EXTERNAL_WALLET", FundsTransactionChannel.DIGITAL_WALLET,
            "DIGITAL_WALLET", FundsTransactionChannel.DIGITAL_WALLET,
            "WALLET", FundsTransactionChannel.DIGITAL_WALLET
    );

    private static final Map<String, ExternalFundsRailDecision> CONFIRMED_CREDIT_RAIL_DECISIONS = Map.of(
            ACH_CREDIT_CONFIRMED,
            bankRail("ACH"),
            BANK_CREDIT_CONFIRMED,
            bankRail("BANK")
    );

    private ExternalFundsRailResolver() {
        throw new AssertionError();
    }

    public static @NonNull ExternalFundsRailDecision requireReceiveRailDecision(
            @NonNull String instrumentType,
            @NonNull String externalRailCode) {
        String normalizedRailCode = normalize(externalRailCode);
        ExternalFundsRailDecision result = RECEIVE_RAIL_DECISIONS.get(normalizedRailCode);
        AssertUtils.notNull(result,
                "收款外部 rail 编码不支持，externalRailCode = {}，支持的外部 rail = {}",
                externalRailCode,
                RECEIVE_RAIL_DECISIONS.keySet());
        FundsTransactionChannel instrumentChannel = INSTRUMENT_CHANNELS.get(normalize(instrumentType));
        AssertUtils.notNull(instrumentChannel, "支付工具类型不支持收款，instrumentType = {}", instrumentType);
        AssertUtils.isTrue(instrumentChannel == result.transactionChannel(),
                "支付工具类型与外部 rail 不匹配，instrumentType = {}，externalRailCode = {}",
                instrumentType,
                externalRailCode);
        return result;
    }

    public static @NonNull ExternalFundsRailDecision requireConfirmedCreditRailDecision(
            @NonNull String externalEventType) {
        String normalizedType = normalize(externalEventType);
        AssertUtils.isFalse(isNonFinalCreditEvent(normalizedType), "外部入金事件未确认到账不得入账");
        ExternalFundsRailDecision result = CONFIRMED_CREDIT_RAIL_DECISIONS.get(normalizedType);
        AssertUtils.notNull(result, "外部资金事件类型暂不支持真实消费");
        return result;
    }

    private static boolean isNonFinalCreditEvent(String externalEventType) {
        return externalEventType.contains("_CREDIT_")
                && NON_FINAL_CREDIT_EVENT_SUFFIXES.stream().anyMatch(externalEventType::endsWith);
    }

    private static String normalize(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static ExternalFundsRailDecision bankRail(String externalRailCode) {
        return new ExternalFundsRailDecision(externalRailCode, FundsTransactionChannel.BANK_TRANSFER);
    }

    /**
     * 本次外部交易实际 rail 与交易渠道大类的解析结果。
     */
    public record ExternalFundsRailDecision(
            String externalRailCode,
            FundsTransactionChannel transactionChannel
    ) {
    }
}

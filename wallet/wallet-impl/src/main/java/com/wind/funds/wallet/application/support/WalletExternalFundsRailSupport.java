package com.wind.funds.wallet.application.support;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Map;

/**
 * 钱包外部资金 rail 与交易渠道解析工具。
 *
 * <p>钱包 application 可以接收业务 rail、通道别名或外部事件类型；交易内核只接收稳定的
 * {@link FundsTransactionChannel}。</p>
 */
public final class WalletExternalFundsRailSupport {

    private static final String ACH_CREDIT_CONFIRMED = "ACH_CREDIT_CONFIRMED";

    private static final String BANK_CREDIT_CONFIRMED = "BANK_CREDIT_CONFIRMED";

    private static final String EXTERNAL_CREDIT_CONFIRMED = "EXTERNAL_CREDIT_CONFIRMED";

    private static final String BANK_RAIL = "BANK_RAIL";

    private static final Map<String, FundsTransactionChannel> CHANNEL_ALIASES = Map.ofEntries(
            Map.entry(FundsTransactionChannel.WIRE_TRANSFER.name(), FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry(BANK_RAIL, FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("BANK_TRANSFER", FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("BANK", FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("WIRE", FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("WIRE_RAIL", FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("LOCAL_BANK", FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("ACH", FundsTransactionChannel.WIRE_TRANSFER),
            Map.entry("ACH_RAIL", FundsTransactionChannel.WIRE_TRANSFER)
    );

    private static final Map<String, ExternalCreditRailDecision> CONFIRMED_CREDIT_RAIL_DECISIONS = Map.of(
            ACH_CREDIT_CONFIRMED,
            new ExternalCreditRailDecision("ACH_RAIL", FundsTransactionChannel.WIRE_TRANSFER),
            BANK_CREDIT_CONFIRMED,
            new ExternalCreditRailDecision(BANK_RAIL, FundsTransactionChannel.WIRE_TRANSFER),
            EXTERNAL_CREDIT_CONFIRMED,
            new ExternalCreditRailDecision("EXTERNAL_RAIL", FundsTransactionChannel.WIRE_TRANSFER)
    );

    private WalletExternalFundsRailSupport() {
        throw new AssertionError();
    }

    public static @NonNull FundsTransactionChannel resolveReceiveChannel(@NonNull String channelCode) {
        String normalizedCode = normalize(channelCode);
        FundsTransactionChannel result = CHANNEL_ALIASES.get(normalizedCode);
        AssertUtils.notNull(result,
                "收款渠道编码不支持，channelCode = {}，支持的收款渠道 = {}",
                channelCode,
                CHANNEL_ALIASES.keySet());
        return result;
    }

    public static @NonNull ExternalCreditRailDecision requireConfirmedCreditRailDecision(
            @NonNull String externalEventType) {
        ExternalCreditRailDecision result = CONFIRMED_CREDIT_RAIL_DECISIONS.get(normalize(externalEventType));
        AssertUtils.notNull(result, "外部资金事件类型暂不支持真实消费");
        return result;
    }

    private static String normalize(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    /**
     * 已确认外部入金事件映射出的 rail 与交易渠道。
     */
    public record ExternalCreditRailDecision(
            String externalRailCode,
            FundsTransactionChannel transactionChannel
    ) {
    }
}

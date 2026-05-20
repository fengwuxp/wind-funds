package com.wind.integration.funds.util;

import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * Parser for money values carried by DSL JSON contracts.
 */
public final class FundsDslMoneyParser {

    private static final String CURRENCY_FIELD = "currency";
    private static final String MINOR_VALUE_FIELD = "minorValue";
    private static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    private FundsDslMoneyParser() {
    }

    public static @NonNull Money parse(@NonNull Map<String, ?> values) {
        Object rawCurrency = values.get(CURRENCY_FIELD);
        if (!(rawCurrency instanceof String currency) || currency.isBlank()) {
            throw new IllegalArgumentException("money.currency is required");
        }
        if (!values.containsKey(MINOR_VALUE_FIELD)) {
            throw new IllegalArgumentException("money.minorValue is required");
        }
        long minorValue = parseMinorValue(values.get(MINOR_VALUE_FIELD));
        return Money.immutable(minorValue, CurrencyIsoCode.valueOf(currency));
    }

    private static long parseMinorValue(Object value) {
        BigInteger parsed = switch (value) {
            case Byte number -> BigInteger.valueOf(number.longValue());
            case Short number -> BigInteger.valueOf(number.longValue());
            case Integer number -> BigInteger.valueOf(number.longValue());
            case Long number -> BigInteger.valueOf(number);
            case BigInteger number -> number;
            case BigDecimal number -> parseIntegerDecimal(number);
            case String text when text.matches("-?\\d+") -> new BigInteger(text);
            default -> throw new IllegalArgumentException("money.minorValue must be integer");
        };
        if (parsed.compareTo(BigInteger.ONE) < 0) {
            throw new IllegalArgumentException("money.minorValue must be positive");
        }
        if (parsed.compareTo(LONG_MAX_VALUE) > 0) {
            throw new IllegalArgumentException("money.minorValue exceeds system limit");
        }
        return parsed.longValue();
    }

    private static BigInteger parseIntegerDecimal(BigDecimal value) {
        try {
            return value.toBigIntegerExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("money.minorValue must be integer", ex);
        }
    }
}

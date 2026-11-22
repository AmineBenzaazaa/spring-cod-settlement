package com.benzaazaa.codsettlement.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Money rules used across the codebase.
 *
 * <ul>
 *   <li>Money is always {@link BigDecimal}, never {@code double}: 0.1 has no exact binary
 *       representation, so floating point sums drift.</li>
 *   <li>Amounts have scale 2 (centimes) and are rounded {@link RoundingMode#HALF_UP}, the mode
 *       accountants expect.</li>
 *   <li>We round once, at the boundary (parsing input, producing a total), so sums of values
 *       that are already scale 2 never drift.</li>
 * </ul>
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    /** Matches the {@code numeric(12,2)} columns: up to 9,999,999,999.99. */
    public static final int MAX_PRECISION = 12;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    public static final String DEFAULT_CURRENCY = "MAD";

    private static final Pattern PLAIN_DECIMAL = Pattern.compile("[+-]?\\d+(\\.\\d+)?");

    private Money() {
    }

    /** Normalizes to scale 2 with HALF_UP rounding; {@code null} becomes zero. */
    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : value.setScale(SCALE, ROUNDING);
    }

    /** Sums the values (nulls count as zero) and returns a scale-2 total. */
    public static BigDecimal sum(Stream<BigDecimal> values) {
        return normalize(values.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public static BigDecimal parse(String raw) {
        return parse(raw, false);
    }

    /**
     * Strictly parses courier or user supplied text into a scale-2 amount.
     *
     * <p>More than two decimals is <em>rejected</em>, not rounded: a courier file saying
     * {@code 10.005} is a data problem we want to surface, not hide.
     */
    public static BigDecimal parse(String raw, boolean allowNegative) {
        String text = raw == null ? "" : raw.strip().replace(" ", "");
        if (text.isEmpty()) {
            throw new InvalidAmountException("amount is empty");
        }
        if (!PLAIN_DECIMAL.matcher(text).matches()) {
            throw new InvalidAmountException("'" + raw + "' is not a number");
        }
        BigDecimal value = new BigDecimal(text);
        if (value.scale() > SCALE) {
            throw new InvalidAmountException("'" + raw + "' has more than 2 decimal places");
        }
        if (value.signum() < 0 && !allowNegative) {
            throw new InvalidAmountException("'" + raw + "' must not be negative");
        }
        BigDecimal amount = normalize(value);
        if (amount.precision() > MAX_PRECISION) {
            throw new InvalidAmountException("'" + raw + "' is too large");
        }
        return amount;
    }
}

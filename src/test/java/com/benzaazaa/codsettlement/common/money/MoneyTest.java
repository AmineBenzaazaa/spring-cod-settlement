package com.benzaazaa.codsettlement.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0.00",
            "10, 10.00",
            "10.5, 10.50",
            "' 1 234.56 ', 1234.56",
            "+3.10, 3.10",
            "9999999999.99, 9999999999.99"
    })
    void parsesValidAmountsToScaleTwo(String raw, String expected) {
        BigDecimal value = Money.parse(raw);
        assertThat(value).isEqualTo(new BigDecimal(expected));
        assertThat(value.scale()).isEqualTo(2);
    }

    @Test
    void rejectsMoreThanTwoDecimalsInsteadOfRounding() {
        assertThatThrownBy(() -> Money.parse("10.005"))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("more than 2 decimal places");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1e3", "NaN", "Infinity", "1,50", "12.", ".5"})
    void rejectsAnythingThatIsNotAPlainDecimal(String raw) {
        assertThatThrownBy(() -> Money.parse(raw)).isInstanceOf(InvalidAmountException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsEmptyInput(String raw) {
        assertThatThrownBy(() -> Money.parse(raw)).hasMessage("amount is empty");
    }

    @Test
    void rejectsNegativeUnlessAllowed() {
        assertThatThrownBy(() -> Money.parse("-1.00")).hasMessageContaining("must not be negative");
        assertThat(Money.parse("-1.00", true)).isEqualTo(new BigDecimal("-1.00"));
    }

    @Test
    void rejectsAmountsThatDoNotFitTheColumn() {
        assertThatThrownBy(() -> Money.parse("12345678901.00")).hasMessageContaining("too large");
    }

    @ParameterizedTest
    @CsvSource({"2.345, 2.35", "2.344, 2.34", "-2.345, -2.35", "0.005, 0.01"})
    void normalizeRoundsHalfUp(String raw, String expected) {
        assertThat(Money.normalize(new BigDecimal(raw))).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void normalizeTreatsNullAsZero() {
        assertThat(Money.normalize(null)).isEqualTo(Money.ZERO);
        assertThat(Money.ZERO.scale()).isEqualTo(2);
    }

    @Test
    void sumHasNoDriftWhereDoublesDo() {
        double asDouble = 0;
        for (int i = 0; i < 10_000; i++) {
            asDouble += 0.10;
        }
        BigDecimal exact = Money.sum(Collections.nCopies(10_000, new BigDecimal("0.10")).stream());

        assertThat(asDouble).isNotEqualTo(1000.0); // 1000.0000000001588
        assertThat(exact).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void sumIgnoresNullsAndReturnsScaleTwo() {
        assertThat(Money.sum(Stream.of(new BigDecimal("1.5"), null, new BigDecimal("2")))).isEqualTo(new BigDecimal("3.50"));
        assertThat(Money.sum(Stream.empty())).isEqualTo(Money.ZERO);
    }
}

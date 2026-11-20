package com.benzaazaa.codsettlement.logistics.domain;

import static com.benzaazaa.codsettlement.support.TestData.courier;
import static com.benzaazaa.codsettlement.support.TestData.merchant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.benzaazaa.codsettlement.common.error.BusinessRuleException;
import com.benzaazaa.codsettlement.support.TestData;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The order lifecycle lives in the entity, so it is tested without Spring or a database. */
class OrderTest {

    private static final Instant AT = Instant.parse("2026-10-05T12:00:00Z");

    private Order newOrder() {
        return TestData.order(1, merchant(1), courier(1), " spx-1 ", "349.9");
    }

    @Test
    void newOrderIsPendingWithNormalizedTrackingAndMoney() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTrackingNumber()).isEqualTo("SPX-1");
        assertThat(order.getAmountDue()).isEqualTo(new BigDecimal("349.90"));
        assertThat(order.getCurrency()).isEqualTo("MAD");
    }

    @ParameterizedTest
    @CsvSource({"DELIVERED, DELIVERED", "NO_ANSWER, SHIPPED", "RESCHEDULED, SHIPPED", "REFUSED, RETURNED", "RETURNED, RETURNED"})
    void attemptOutcomeDrivesTheStatus(AttemptOutcome outcome, OrderStatus expected) {
        Order order = newOrder();

        DeliveryAttempt attempt = order.recordAttempt(outcome, AT, "note");

        assertThat(order.getStatus()).isEqualTo(expected);
        assertThat(order.getAttempts()).containsExactly(attempt);
        assertThat(order.getDeliveredAt()).isEqualTo(expected == OrderStatus.DELIVERED ? AT : null);
    }

    @Test
    void noAttemptAfterAFinalStatus() {
        Order order = newOrder();
        order.recordAttempt(AttemptOutcome.DELIVERED, AT, null);

        assertThatThrownBy(() -> order.recordAttempt(AttemptOutcome.NO_ANSWER, AT, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order is already DELIVERED.");
    }

    @Test
    void pendingAndShippedOrdersCanBeCancelledButNotDeliveredOnes() {
        Order pending = newOrder();
        pending.cancel();
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order delivered = newOrder();
        delivered.recordAttempt(AttemptOutcome.DELIVERED, AT, null);
        assertThatThrownBy(delivered::cancel).hasMessage("Cannot cancel a DELIVERED order.");
    }

    @Test
    void finalOrdersCannotBeEditedAndOnlyPendingOrdersAreDeletable() {
        Order order = newOrder();
        assertThat(order.isDeletable()).isTrue();
        order.recordAttempt(AttemptOutcome.REFUSED, AT, null);

        assertThat(order.isDeletable()).isFalse();
        assertThatThrownBy(() -> order.updateDetails(courier(1), "X", "n", null, null, BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class);
    }
}

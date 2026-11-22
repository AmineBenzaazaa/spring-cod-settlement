package com.benzaazaa.codsettlement.support;

import com.benzaazaa.codsettlement.account.domain.AppUser;
import com.benzaazaa.codsettlement.account.domain.Merchant;
import com.benzaazaa.codsettlement.account.domain.Role;
import com.benzaazaa.codsettlement.logistics.domain.AttemptOutcome;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.domain.Dispute;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builders for entities in unit tests. Entities have no id setter (the database assigns
 * ids), so tests set it by reflection, exactly like Hibernate does.
 */
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private TestData() {
    }

    public static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    public static AuthenticatedUser merchantUser(long merchantId) {
        return new AuthenticatedUser(100 + merchantId, "merchant" + merchantId, Role.MERCHANT, merchantId);
    }

    public static AuthenticatedUser staff() {
        return new AuthenticatedUser(1L, "ops", Role.STAFF, null);
    }

    public static Merchant merchant(long id) {
        return withId(new Merchant("Merchant " + id, "merchant-" + id, null), id);
    }

    public static Courier courier(long id) {
        return withId(new Courier("Courier " + id, "courier-" + id, null, true), id);
    }

    public static AppUser appUser(long id, Merchant merchant) {
        AppUser user = merchant == null ? AppUser.staff("user" + id, "{hash}") : AppUser.merchantUser("user" + id, "{hash}", merchant);
        return withId(user, id);
    }

    public static Order order(long id, Merchant merchant, Courier courier, String tracking, String amountDue) {
        return withId(new Order(merchant, courier, tracking, "Customer " + id, null, "Rabat", money(amountDue)), id);
    }

    public static Order delivered(long id, Merchant merchant, Courier courier, String tracking, String amountDue) {
        Order order = order(id, merchant, courier, tracking, amountDue);
        order.recordAttempt(AttemptOutcome.DELIVERED, Instant.parse("2026-10-10T10:00:00Z"), null);
        return order;
    }

    public static RemittanceStatement statement(long id, Merchant merchant, Courier courier) {
        return withId(new RemittanceStatement(merchant, courier, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                "remittance.csv", "a".repeat(64), null), id);
    }

    public static RemittanceLine line(long id, RemittanceStatement statement, int row, String tracking,
                                      String collected, String fee) {
        return withId(new RemittanceLine(statement, row, tracking, money(collected), money(fee)), id);
    }

    public static ReconciliationResult result(long id, RemittanceStatement statement, ReconciliationOutcome outcome,
                                              String tracking, String expected, String collected) {
        BigDecimal e = money(expected);
        BigDecimal c = money(collected);
        return withId(new ReconciliationResult(statement, outcome, null, null, tracking, e, c, c.subtract(e)), id);
    }

    public static Dispute dispute(long id, ReconciliationResult result, String claimed) {
        return withId(new Dispute(result, result.getStatement().getMerchant(), "reason", money(claimed), null), id);
    }
}

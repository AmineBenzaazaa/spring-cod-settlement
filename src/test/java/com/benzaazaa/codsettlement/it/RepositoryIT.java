package com.benzaazaa.codsettlement.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.benzaazaa.codsettlement.account.domain.AppUser;
import com.benzaazaa.codsettlement.account.domain.Merchant;
import com.benzaazaa.codsettlement.account.repository.AppUserRepository;
import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.config.ClockConfig;
import com.benzaazaa.codsettlement.config.JpaConfig;
import com.benzaazaa.codsettlement.logistics.domain.AttemptOutcome;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.logistics.domain.OrderStatus;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.logistics.repository.OrderRepository;
import com.benzaazaa.codsettlement.logistics.repository.OrderSpecifications;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderFilter;
import com.benzaazaa.codsettlement.security.TenantSpecifications;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.repository.CountAndSum;
import com.benzaazaa.codsettlement.settlement.repository.MoneyTotals;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceLineRepository;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.repository.SettlementOrderRepository;
import com.benzaazaa.codsettlement.support.PostgresTestBase;
import com.benzaazaa.codsettlement.support.TestData;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

/**
 * Repository slice against real PostgreSQL. {@code @DataJpaTest} wraps each test in a
 * transaction that is rolled back, so tests do not see each other's rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keep the Testcontainers DataSource
@Import({JpaConfig.class, ClockConfig.class})
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class RepositoryIT extends PostgresTestBase {

    @Autowired EntityManager em;
    @Autowired MerchantRepository merchants;
    @Autowired CourierRepository couriers;
    @Autowired AppUserRepository users;
    @Autowired OrderRepository orders;
    @Autowired RemittanceStatementRepository statements;
    @Autowired RemittanceLineRepository lines;
    @Autowired SettlementOrderRepository settlementOrders;

    private Merchant atlas;
    private Merchant medina;
    private Courier speedex;

    @BeforeEach
    void setUp() {
        atlas = merchants.save(new Merchant("Atlas Home", "atlas-home", null));
        medina = merchants.save(new Merchant("Medina Style", "medina-style", null));
        speedex = couriers.save(new Courier("Speedex", "speedex", null, true));
    }

    private Order order(Merchant merchant, String tracking, String amount, AttemptOutcome outcome, Instant at) {
        Order order = new Order(merchant, speedex, tracking, "Sara Tazi", "0600000000", "Rabat", new BigDecimal(amount));
        if (outcome != null) {
            order.recordAttempt(outcome, at, null);
        }
        return orders.save(order);
    }

    @Test
    void auditingFillsTimestampsAndVersionIncrementsOnUpdate() {
        Order order = order(atlas, "T1", "10.00", null, null);
        em.flush();
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
        long version = order.getVersion();

        order.cancel();
        orders.saveAndFlush(order);

        assertThat(order.getVersion()).isEqualTo(version + 1);
    }

    @Test
    void trackingNumberIsUniquePerCourierAtTheDatabaseLevel() {
        order(atlas, "DUP-1", "10.00", null, null);

        assertThatThrownBy(() -> orders.saveAndFlush(
                new Order(medina, speedex, "dup-1", "Other", null, null, BigDecimal.ONE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void merchantRoleWithoutMerchantIsRejectedByACheckConstraint() {
        users.saveAndFlush(AppUser.staff("ops", "{hash}"));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO app_user (username, password_hash, role, created_at) VALUES ('bad', 'x', 'MERCHANT', now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void specificationsFilterSearchAndScopeByTenant() {
        order(atlas, "SPX-1", "100.00", AttemptOutcome.DELIVERED, TestData.NOW);
        order(atlas, "SPX-2", "500.00", AttemptOutcome.DELIVERED, TestData.NOW);
        order(atlas, "SPX-3", "900.00", AttemptOutcome.REFUSED, TestData.NOW);
        order(medina, "SPX-4", "600.00", AttemptOutcome.DELIVERED, TestData.NOW);

        OrderFilter filter = new OrderFilter(List.of(OrderStatus.DELIVERED), null, null, "rabat",
                new BigDecimal("300"), null, null, null, "spx");
        Specification<Order> spec = Specification.allOf(
                TenantSpecifications.visibleTo(TestData.merchantUser(atlas.getId()), "merchant"),
                OrderSpecifications.matching(filter));

        assertThat(orders.findAll(spec)).extracting(Order::getTrackingNumber).containsExactly("SPX-2");
        assertThat(orders.findAll(OrderSpecifications.matching(filter))).hasSize(2); // without tenant scope
    }

    @Test
    void likeWildcardsInSearchAreEscaped() {
        order(atlas, "A%B", "1.00", null, null);
        order(atlas, "AXB", "1.00", null, null);

        OrderFilter filter = new OrderFilter(null, null, null, null, null, null, null, null, "a%b");

        assertThat(orders.findAll(OrderSpecifications.matching(filter))).extracting(Order::getTrackingNumber)
                .containsExactly("A%B");
    }

    @Test
    void pagingWithAttemptsAvoidsNPlusOneQueries() {
        for (int i = 0; i < 12; i++) {
            order(atlas, "N" + i, "10.00", AttemptOutcome.NO_ANSWER, TestData.NOW);
        }
        em.flush();
        em.clear();
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        Page<Order> page = orders.findAll(
                TenantSpecifications.visibleTo(TestData.staff()), PageRequest.of(0, 10, Sort.by("trackingNumber")));
        page.forEach(o -> {
            o.getCourier().getName();  // fetched by @EntityGraph
            o.getAttempts().size();    // batch-loaded by @BatchSize
        });

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(12);
        // select page + count + one batched IN (...) for attempts. Without the fixes: 1 + 10 + 10.
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void statementHashIsUniquePerMerchantOnly() {
        statements.saveAndFlush(new RemittanceStatement(atlas, speedex, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), "a.csv", "f".repeat(64), null));
        statements.saveAndFlush(new RemittanceStatement(medina, speedex, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), "a.csv", "f".repeat(64), null));
        assertThat(statements.findByMerchantIdAndFileSha256(medina.getId(), "f".repeat(64))).isPresent();

        // Last statement on purpose: after a failed flush the persistence context is unusable,
        // which is why the upload service re-reads the winner in a fresh transaction.
        assertThatThrownBy(() -> statements.saveAndFlush(new RemittanceStatement(atlas, speedex,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "b.csv", "f".repeat(64), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deliveredInPeriodRespectsBoundariesAndOrdersSettledElsewhere() {
        Order inPeriod = order(atlas, "IN", "10.00", AttemptOutcome.DELIVERED, Instant.parse("2026-10-10T10:00:00Z"));
        order(atlas, "BEFORE", "10.00", AttemptOutcome.DELIVERED, Instant.parse("2026-09-30T22:59:59Z"));
        order(atlas, "AFTER", "10.00", AttemptOutcome.DELIVERED, Instant.parse("2026-10-31T23:00:00Z"));
        Order settled = order(atlas, "SETTLED", "10.00", AttemptOutcome.DELIVERED, Instant.parse("2026-10-11T10:00:00Z"));
        order(medina, "OTHER-MERCHANT", "10.00", AttemptOutcome.DELIVERED, Instant.parse("2026-10-11T10:00:00Z"));

        RemittanceStatement september = statements.save(new RemittanceStatement(atlas, speedex,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15), "old.csv", "1".repeat(64), null));
        RemittanceLine line = new RemittanceLine(september, 1, "SETTLED", new BigDecimal("10.00"), BigDecimal.ZERO);
        line.linkToOrder(settled);
        lines.save(line);
        RemittanceStatement october = statements.save(new RemittanceStatement(atlas, speedex,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "oct.csv", "2".repeat(64), null));

        List<Order> result = settlementOrders.findDeliveredInPeriodNotSettledElsewhere(october.getId(), atlas.getId(),
                speedex.getId(), Instant.parse("2026-09-30T23:00:00Z"), Instant.parse("2026-10-31T23:00:00Z"));

        assertThat(result).containsExactly(inPeriod);
    }

    @Test
    void balanceAggregatesOnlyCountReconciledStatements() {
        Order delivered = order(atlas, "D1", "300.00", AttemptOutcome.DELIVERED, TestData.NOW);
        order(atlas, "D2", "200.00", AttemptOutcome.DELIVERED, TestData.NOW);
        order(medina, "D3", "999.00", AttemptOutcome.DELIVERED, TestData.NOW);
        RemittanceStatement reconciled = statements.save(new RemittanceStatement(atlas, speedex,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "a.csv", "3".repeat(64), null));
        reconciled.markReconciled(TestData.NOW, 1);
        RemittanceLine line = new RemittanceLine(reconciled, 1, "D1", new BigDecimal("280.00"), new BigDecimal("25.00"));
        line.linkToOrder(delivered);
        lines.save(line);
        RemittanceStatement pending = statements.save(new RemittanceStatement(atlas, speedex,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "b.csv", "4".repeat(64), null));
        RemittanceLine pendingLine = new RemittanceLine(pending, 1, "D1", new BigDecimal("1000.00"), BigDecimal.ZERO);
        pendingLine.linkToOrder(delivered);
        lines.save(pendingLine);

        CountAndSum atlasDelivered = settlementOrders.deliveredTotals(speedex.getId(), atlas.getId());
        CountAndSum allDelivered = settlementOrders.deliveredTotals(speedex.getId());
        MoneyTotals reported = lines.reportedTotals(speedex.getId(), atlas.getId());

        assertThat(atlasDelivered.count()).isEqualTo(2);
        assertThat(atlasDelivered.sum()).isEqualByComparingTo("500.00");
        assertThat(allDelivered.sum()).isEqualByComparingTo("1499.00");
        assertThat(reported.first()).isEqualByComparingTo("280.00");
        assertThat(reported.second()).isEqualByComparingTo("25.00");
        assertThat(lines.reportedTotals(speedex.getId(), medina.getId()).first()).isNull(); // SUM over no rows
    }
}

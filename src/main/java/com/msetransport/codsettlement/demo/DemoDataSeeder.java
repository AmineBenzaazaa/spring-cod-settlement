package com.benzaazaa.codsettlement.demo;

import com.benzaazaa.codsettlement.account.domain.AppUser;
import com.benzaazaa.codsettlement.account.domain.Merchant;
import com.benzaazaa.codsettlement.account.repository.AppUserRepository;
import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.logistics.domain.AttemptOutcome;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.logistics.domain.OrderStatus;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.logistics.repository.OrderRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic demo data, only with {@code SPRING_PROFILES_ACTIVE=demo}.
 *
 * <p>Safe to run on every start: reference data is upserted and orders are only created for
 * merchants that have none. The random generator is seeded, so the orders always match
 * {@code fixtures/sample_remittance.csv}. Set {@code app.demo.sample-csv-path} to regenerate
 * that file.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "demo-pass-123";
    static final Instant PERIOD_START = Instant.parse("2026-10-01T09:00:00Z");
    static final int ORDERS_PER_MERCHANT = 30;

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String[] CITIES = {"Casablanca", "Rabat", "Marrakech", "Tangier", "Fes", "Agadir", "Oujda"};
    private static final String[] FIRST_NAMES = {"Youssef", "Salma", "Omar", "Khadija", "Hamza", "Imane", "Mehdi", "Sara"};
    private static final String[] LAST_NAMES = {"Alaoui", "Bennani", "El Idrissi", "Tazi", "Chraibi", "Berrada", "Fassi"};

    private record MerchantSeed(String name, String slug, String username) { }

    private record CourierSeed(String name, String code, String trackingPrefix) { }

    private static final List<MerchantSeed> MERCHANTS = List.of(
            new MerchantSeed("Atlas Home", "atlas-home", "atlas"),
            new MerchantSeed("Medina Style", "medina-style", "medina"));
    private static final List<CourierSeed> COURIERS = List.of(
            new CourierSeed("Speedex Colis", "speedex", "SPX"),
            new CourierSeed("Rapido Express", "rapido", "RPD"));

    private final MerchantRepository merchants;
    private final CourierRepository couriers;
    private final AppUserRepository users;
    private final OrderRepository orders;
    private final PasswordEncoder passwordEncoder;
    private final String sampleCsvPath;

    public DemoDataSeeder(MerchantRepository merchants, CourierRepository couriers, AppUserRepository users,
                          OrderRepository orders, PasswordEncoder passwordEncoder,
                          @Value("${app.demo.sample-csv-path:}") String sampleCsvPath) {
        this.merchants = merchants;
        this.couriers = couriers;
        this.users = users;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
        this.sampleCsvPath = sampleCsvPath;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Merchant> merchantList = MERCHANTS.stream().map(this::upsertMerchant).toList();
        List<Courier> courierList = COURIERS.stream().map(this::upsertCourier).toList();

        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        upsertUser("ops", hash, null);
        for (int m = 0; m < MERCHANTS.size(); m++) {
            upsertUser(MERCHANTS.get(m).username(), hash, merchantList.get(m));
        }

        for (int m = 0; m < merchantList.size(); m++) {
            Merchant merchant = merchantList.get(m);
            if (orders.existsByMerchantId(merchant.getId())) {
                continue;
            }
            Random rng = new Random(42L + m); // one seeded generator per merchant: stable data
            for (int i = 0; i < ORDERS_PER_MERCHANT; i++) {
                int c = i % courierList.size();
                String tracking = "%s-%d%05d".formatted(COURIERS.get(c).trackingPrefix(), m + 1, i + 1);
                createOrder(rng, merchant, courierList.get(c), tracking, i);
            }
        }

        if (sampleCsvPath != null && !sampleCsvPath.isBlank()) {
            int rows = writeSampleCsv(Path.of(sampleCsvPath), merchantList.getFirst(), courierList.getFirst());
            log.info("Wrote {} ({} rows)", sampleCsvPath, rows);
        }
        log.info("Demo data ready. Users: ops (STAFF), atlas, medina (MERCHANT); password: {}", DEMO_PASSWORD);
        log.info("Upload fixtures/sample_remittance.csv as 'atlas' with courierId={} and period 2026-10-01..2026-10-31",
                courierList.getFirst().getId());
    }

    private Merchant upsertMerchant(MerchantSeed seed) {
        Merchant merchant = merchants.findBySlug(seed.slug())
                .orElseGet(() -> merchants.save(new Merchant(seed.name(), seed.slug(), null)));
        merchant.updateDetails(seed.name(), "finance@" + seed.slug() + ".example");
        return merchant;
    }

    private Courier upsertCourier(CourierSeed seed) {
        return couriers.findByCode(seed.code())
                .orElseGet(() -> couriers.save(new Courier(seed.name(), seed.code(), null, true)));
    }

    private void upsertUser(String username, String passwordHash, Merchant merchant) {
        users.findByUsername(username).ifPresentOrElse(
                user -> user.changePassword(passwordHash),
                () -> users.save(merchant == null
                        ? AppUser.staff(username, passwordHash)
                        : AppUser.merchantUser(username, passwordHash, merchant)));
    }

    private void createOrder(Random rng, Merchant merchant, Courier courier, String tracking, int i) {
        // Mostly delivered, with a realistic sprinkle of returns and parcels still in transit.
        int roll = rng.nextInt(100);
        AttemptOutcome outcome = roll < 75 ? AttemptOutcome.DELIVERED
                : roll < 90 ? AttemptOutcome.REFUSED
                : AttemptOutcome.NO_ANSWER;
        BigDecimal amount = BigDecimal.valueOf(9_900 + 50L * rng.nextInt(1_600)).movePointLeft(2); // 99.00 .. 898.50
        Order order = new Order(merchant, courier, tracking,
                FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rng.nextInt(LAST_NAMES.length)],
                "+2126" + (10_000_000 + rng.nextInt(90_000_000)),
                CITIES[rng.nextInt(CITIES.length)],
                amount);
        Instant shippedAt = PERIOD_START.plus(Duration.ofDays(i % 28)).plus(Duration.ofHours(rng.nextInt(9)));
        order.recordAttempt(outcome, shippedAt.plus(Duration.ofDays(1)), null);
        orders.save(order);
    }

    /** A realistic courier file: mostly correct, plus one of each kind of discrepancy. */
    private int writeSampleCsv(Path path, Merchant merchant, Courier courier) {
        Specification<Order> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("merchant").get("id"), merchant.getId()),
                cb.equal(root.get("courier").get("id"), courier.getId()),
                root.get("status").in(OrderStatus.DELIVERED, OrderStatus.RETURNED));
        BigDecimal fee = new BigDecimal("25.00");
        List<String> rows = new ArrayList<>();
        rows.add("tracking_number,amount_collected,fee");
        int deliveredSeen = 0;
        for (Order order : orders.findAll(spec, Sort.by("trackingNumber"))) {
            if (order.getStatus() == OrderStatus.RETURNED) {
                rows.add(order.getTrackingNumber() + ",0.00,10.00");
                continue;
            }
            deliveredSeen++;
            if (deliveredSeen == 1) {
                continue; // left out -> MISSING_FROM_STATEMENT
            }
            BigDecimal collected = order.getAmountDue();
            if (deliveredSeen == 2) {
                collected = collected.subtract(new BigDecimal("50.00")); // short-paid -> AMOUNT_MISMATCH
            }
            rows.add(order.getTrackingNumber() + "," + collected.toPlainString() + "," + fee.toPlainString());
        }
        rows.add("SPX-UNKNOWN01,199.00," + fee.toPlainString()); // -> UNKNOWN_TRACKING
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, rows, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return rows.size() - 1;
    }
}

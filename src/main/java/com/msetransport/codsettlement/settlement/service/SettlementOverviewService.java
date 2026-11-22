package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import com.benzaazaa.codsettlement.common.money.Money;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.repository.CountAndSum;
import com.benzaazaa.codsettlement.settlement.repository.DisputeRepository;
import com.benzaazaa.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.CourierBalanceResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.OverviewResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.OverviewResponse.CourierPosition;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model for the web dashboard: KPI totals plus the per-courier balance table.
 *
 * <p>Per-courier numbers reuse {@link CourierBalanceService}, so the dashboard and
 * {@code GET /api/couriers/{id}/balance} can never disagree. That costs two aggregate
 * queries per courier; couriers are reference data (a handful per country), so this is
 * cheaper to maintain than a second, grouped version of the same formulas.
 */
@Service
@Transactional(readOnly = true)
public class SettlementOverviewService {

    private final CourierRepository couriers;
    private final MerchantRepository merchants;
    private final CourierBalanceService balances;
    private final DisputeRepository disputes;
    private final ReconciliationResultRepository results;

    public SettlementOverviewService(CourierRepository couriers, MerchantRepository merchants,
                                     CourierBalanceService balances, DisputeRepository disputes,
                                     ReconciliationResultRepository results) {
        this.couriers = couriers;
        this.merchants = merchants;
        this.balances = balances;
        this.disputes = disputes;
        this.results = results;
    }

    /**
     * @param requestedMerchantId staff only: restrict to one merchant ({@code null} = all merchants).
     *                            Merchant users are always scoped to their own merchant.
     */
    public OverviewResponse overview(AuthenticatedUser user, Long requestedMerchantId) {
        Long merchantId = user.isStaff() ? requestedMerchantId : user.merchantId();
        if (user.isStaff() && merchantId != null && !merchants.existsById(merchantId)) {
            throw new InvalidRequestException("merchantId", "Unknown merchant.");
        }

        List<CourierPosition> positions = couriers.findAll(Sort.by("name")).stream()
                .map(courier -> position(courier, balances.balance(user, courier.getId(), merchantId)))
                .toList();

        CountAndSum open = merchantId == null ? disputes.openTotals() : disputes.openTotals(merchantId);
        CountAndSum discrepancy = merchantId == null ? results.discrepancyTotals() : results.discrepancyTotals(merchantId);

        BigDecimal expected = sum(positions, CourierPosition::expectedCollections);
        BigDecimal reported = sum(positions, CourierPosition::reportedCollections);
        BigDecimal fees = sum(positions, CourierPosition::fees);
        return new OverviewResponse(merchantId, Money.DEFAULT_CURRENCY,
                expected, reported, fees,
                Money.normalize(reported.subtract(fees)),
                Money.normalize(expected.subtract(reported)),
                count(open), Money.normalize(open == null ? null : open.sum()),
                count(discrepancy), Money.normalize(discrepancy == null ? null : discrepancy.sum()),
                positions);
    }

    private static CourierPosition position(Courier courier, CourierBalanceResponse b) {
        return new CourierPosition(courier.getId(), courier.getName(), courier.getCode(), courier.isActive(),
                b.deliveredOrders(), b.expectedCollections(), b.reportedCollections(), b.fees(), b.netRemitted(),
                b.balanceOwed());
    }

    private static BigDecimal sum(List<CourierPosition> rows, Function<CourierPosition, BigDecimal> field) {
        return Money.normalize(rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static long count(CountAndSum totals) {
        return totals == null || totals.count() == null ? 0 : totals.count();
    }
}

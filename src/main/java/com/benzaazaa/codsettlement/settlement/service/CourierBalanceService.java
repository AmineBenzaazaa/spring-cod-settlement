package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.common.money.Money;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.repository.CountAndSum;
import com.benzaazaa.codsettlement.settlement.repository.MoneyTotals;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceLineRepository;
import com.benzaazaa.codsettlement.settlement.repository.SettlementOrderRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.CourierBalanceResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * How much cash a courier still owes. A positive balance is cash collected at the door
 * (or that should have been) but not yet accounted for: short payments plus delivered
 * parcels missing from statements. Computed with SQL aggregates, never by loading rows.
 */
@Service
@Transactional(readOnly = true)
public class CourierBalanceService {

    private final CourierRepository couriers;
    private final MerchantRepository merchants;
    private final SettlementOrderRepository orders;
    private final RemittanceLineRepository lines;

    public CourierBalanceService(CourierRepository couriers, MerchantRepository merchants,
                                 SettlementOrderRepository orders, RemittanceLineRepository lines) {
        this.couriers = couriers;
        this.merchants = merchants;
        this.orders = orders;
        this.lines = lines;
    }

    /**
     * @param requestedMerchantId staff only: restrict to one merchant ({@code null} = all merchants).
     *                            Merchant users are always scoped to their own merchant.
     */
    public CourierBalanceResponse balance(AuthenticatedUser user, long courierId, Long requestedMerchantId) {
        if (!couriers.existsById(courierId)) {
            throw new ResourceNotFoundException("Courier", courierId);
        }
        Long merchantId = user.isStaff() ? requestedMerchantId : user.merchantId();
        if (user.isStaff() && merchantId != null && !merchants.existsById(merchantId)) {
            throw new InvalidRequestException("merchantId", "Unknown merchant.");
        }

        CountAndSum delivered = merchantId == null
                ? orders.deliveredTotals(courierId)
                : orders.deliveredTotals(courierId, merchantId);
        MoneyTotals reported = merchantId == null
                ? lines.reportedTotals(courierId)
                : lines.reportedTotals(courierId, merchantId);

        BigDecimal expected = Money.normalize(delivered == null ? null : delivered.sum());
        BigDecimal collected = Money.normalize(reported == null ? null : reported.first());
        BigDecimal fees = Money.normalize(reported == null ? null : reported.second());
        long deliveredCount = delivered == null || delivered.count() == null ? 0 : delivered.count();

        return new CourierBalanceResponse(courierId, merchantId, Money.DEFAULT_CURRENCY, deliveredCount,
                expected, collected, fees,
                Money.normalize(collected.subtract(fees)),
                Money.normalize(expected.subtract(collected)));
    }
}

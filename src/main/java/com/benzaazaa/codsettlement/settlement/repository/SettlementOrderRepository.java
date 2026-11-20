package com.benzaazaa.codsettlement.settlement.repository;

import com.benzaazaa.codsettlement.logistics.domain.Order;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Settlement-specific read queries over orders. Extends the bare {@link Repository} marker,
 * so it exposes only these methods (no save/delete): the settlement module reads orders,
 * it never writes them.
 */
public interface SettlementOrderRepository extends Repository<Order, Long> {

    /**
     * Orders whose tracking number appears on the statement. A subquery instead of
     * {@code IN (:trackingNumbers)}: a 50,000-line statement would exceed PostgreSQL's
     * 32,767 bind-parameter limit.
     */
    @Query("""
            select o from Order o
            where o.merchant.id = :merchantId
              and o.courier.id = :courierId
              and o.trackingNumber in (
                  select l.trackingNumber from RemittanceLine l where l.statement.id = :statementId)""")
    List<Order> findOrdersOnStatement(@Param("statementId") Long statementId,
                                      @Param("merchantId") Long merchantId,
                                      @Param("courierId") Long courierId);

    /** Delivered in [from, to) and not already settled on another statement (overlapping periods). */
    @Query("""
            select o from Order o
            where o.merchant.id = :merchantId
              and o.courier.id = :courierId
              and o.status = com.benzaazaa.codsettlement.logistics.domain.OrderStatus.DELIVERED
              and o.deliveredAt >= :from and o.deliveredAt < :to
              and not exists (
                  select 1 from RemittanceLine l
                  where l.order = o and l.statement.id <> :statementId)""")
    List<Order> findDeliveredInPeriodNotSettledElsewhere(@Param("statementId") Long statementId,
                                                         @Param("merchantId") Long merchantId,
                                                         @Param("courierId") Long courierId,
                                                         @Param("from") Instant from,
                                                         @Param("to") Instant to);
}

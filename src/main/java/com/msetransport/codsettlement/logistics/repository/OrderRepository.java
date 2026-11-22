package com.msetransport.codsettlement.logistics.repository;

import com.msetransport.codsettlement.logistics.domain.Order;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /** Same as the inherited method, but fetches the courier in the same query (no N+1 on courierName). */
    @Override
    @EntityGraph(attributePaths = "courier")
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "courier")
    Optional<Order> findOne(Specification<Order> spec);

    /** {@code SELECT ... FOR UPDATE}: two concurrent courier callbacks cannot both "deliver" the parcel. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCourierIdAndTrackingNumber(Long courierId, String trackingNumber);

    boolean existsByCourierIdAndTrackingNumberAndIdNot(Long courierId, String trackingNumber, Long id);

    boolean existsByCourierId(Long courierId);

    boolean existsByMerchantId(Long merchantId);
}

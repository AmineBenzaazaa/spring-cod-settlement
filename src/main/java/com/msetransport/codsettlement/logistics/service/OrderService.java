package com.msetransport.codsettlement.logistics.service;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.account.repository.MerchantRepository;
import com.msetransport.codsettlement.common.error.BusinessRuleException;
import com.msetransport.codsettlement.common.error.ConflictException;
import com.msetransport.codsettlement.common.error.InvalidRequestException;
import com.msetransport.codsettlement.common.error.ResourceNotFoundException;
import com.msetransport.codsettlement.common.web.PageResponse;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.logistics.domain.Order;
import com.msetransport.codsettlement.logistics.domain.TrackingNumbers;
import com.msetransport.codsettlement.logistics.mapper.LogisticsMapper;
import com.msetransport.codsettlement.logistics.repository.CourierRepository;
import com.msetransport.codsettlement.logistics.repository.OrderRepository;
import com.msetransport.codsettlement.logistics.repository.OrderSpecifications;
import com.msetransport.codsettlement.logistics.web.dto.DeliveryAttemptRequest;
import com.msetransport.codsettlement.logistics.web.dto.OrderCreateRequest;
import com.msetransport.codsettlement.logistics.web.dto.OrderFilter;
import com.msetransport.codsettlement.logistics.web.dto.OrderResponse;
import com.msetransport.codsettlement.logistics.web.dto.OrderUpdateRequest;
import com.msetransport.codsettlement.security.AuthenticatedUser;
import com.msetransport.codsettlement.security.TenantSpecifications;
import java.time.Clock;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order use cases. Every method takes the caller and scopes data to the caller's merchant,
 * so tenant isolation does not depend on the controller remembering to check.
 *
 * <p>Methods return DTOs built inside the transaction: with {@code open-in-view=false},
 * touching a lazy association after the method returns would throw
 * {@code LazyInitializationException}.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orders;
    private final CourierRepository couriers;
    private final MerchantRepository merchants;
    private final LogisticsMapper mapper;
    private final Clock clock;

    public OrderService(OrderRepository orders, CourierRepository couriers, MerchantRepository merchants,
                        LogisticsMapper mapper, Clock clock) {
        this.orders = orders;
        this.couriers = couriers;
        this.merchants = merchants;
        this.mapper = mapper;
        this.clock = clock;
    }

    public PageResponse<OrderResponse> search(AuthenticatedUser user, OrderFilter filter, Pageable pageable) {
        Specification<Order> spec = Specification.allOf(
                TenantSpecifications.visibleTo(user, "merchant"),
                OrderSpecifications.matching(filter));
        return PageResponse.of(orders.findAll(spec, pageable).map(mapper::toResponse));
    }

    public OrderResponse get(AuthenticatedUser user, long id) {
        return mapper.toResponse(findVisible(user, id));
    }

    @Transactional
    public OrderResponse create(AuthenticatedUser user, OrderCreateRequest request) {
        Merchant merchant = resolveMerchantForWrite(user, request.merchantId());
        Courier courier = activeCourier(request.courierId());
        String tracking = TrackingNumbers.normalize(request.trackingNumber());
        if (orders.existsByCourierIdAndTrackingNumber(courier.getId(), tracking)) {
            throw duplicateTracking(tracking, courier);
        }
        Order order = new Order(merchant, courier, tracking, request.customerName(), request.customerPhone(),
                request.city(), request.amountDue());
        return mapper.toResponse(orders.save(order));
    }

    @Transactional
    public OrderResponse update(AuthenticatedUser user, long id, OrderUpdateRequest request) {
        Order order = findVisible(user, id);
        Courier courier = Objects.equals(order.getCourier().getId(), request.courierId())
                ? order.getCourier()
                : activeCourier(request.courierId());
        String tracking = TrackingNumbers.normalize(request.trackingNumber());
        if (orders.existsByCourierIdAndTrackingNumberAndIdNot(courier.getId(), tracking, order.getId())) {
            throw duplicateTracking(tracking, courier);
        }
        order.updateDetails(courier, tracking, request.customerName(), request.customerPhone(), request.city(),
                request.amountDue());
        // Flush now so @LastModifiedDate and @Version are up to date in the response.
        return mapper.toResponse(orders.saveAndFlush(order));
    }

    @Transactional
    public void delete(AuthenticatedUser user, long id) {
        Order order = findVisible(user, id);
        if (!order.isDeletable()) {
            throw new BusinessRuleException("Only pending orders can be deleted; cancel it instead.");
        }
        orders.delete(order);
    }

    @Transactional
    public OrderResponse recordAttempt(AuthenticatedUser user, long id, DeliveryAttemptRequest request) {
        // Lock the row first, then check the tenant on the locked row.
        Order order = orders.findByIdForUpdate(id)
                .filter(o -> user.isStaff() || o.getMerchant().getId().equals(user.merchantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        order.recordAttempt(request.outcome(),
                request.attemptedAt() != null ? request.attemptedAt() : clock.instant(),
                request.note());
        return mapper.toResponse(orders.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse cancel(AuthenticatedUser user, long id) {
        Order order = findVisible(user, id);
        order.cancel();
        return mapper.toResponse(orders.saveAndFlush(order));
    }

    private Order findVisible(AuthenticatedUser user, long id) {
        return orders.findOne(Specification.allOf(
                        OrderSpecifications.hasId(id),
                        TenantSpecifications.visibleTo(user, "merchant")))
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private Merchant resolveMerchantForWrite(AuthenticatedUser user, Long requestedMerchantId) {
        if (!user.isStaff()) {
            // Never trust a client-supplied merchant: tenants only write to themselves.
            return merchants.getReferenceById(user.merchantId());
        }
        if (requestedMerchantId == null) {
            throw new InvalidRequestException("merchantId", "Staff must specify the merchant.");
        }
        return merchants.findById(requestedMerchantId)
                .orElseThrow(() -> new InvalidRequestException("merchantId", "Unknown merchant."));
    }

    private Courier activeCourier(Long courierId) {
        return couriers.findByIdAndActiveTrue(courierId)
                .orElseThrow(() -> new InvalidRequestException("courierId", "Unknown or inactive courier."));
    }

    private static ConflictException duplicateTracking(String tracking, Courier courier) {
        return new ConflictException(
                "Tracking number " + tracking + " already exists for courier " + courier.getCode() + ".");
    }
}

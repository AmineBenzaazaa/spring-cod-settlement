package com.benzaazaa.codsettlement.logistics.web;

import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.common.web.SortGuard;
import com.benzaazaa.codsettlement.logistics.service.OrderService;
import com.benzaazaa.codsettlement.logistics.web.dto.DeliveryAttemptRequest;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderCreateRequest;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderFilter;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderResponse;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderUpdateRequest;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP adapter: validate input, call the service, choose the status code. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    static final Set<String> SORTABLE = Set.of("createdAt", "amountDue", "deliveredAt", "status", "trackingNumber");

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                            OrderFilter filter,
                                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {
        return orderService.search(user, filter, SortGuard.allow(pageable, SORTABLE));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return orderService.get(user, id);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse created = orderService.create(user, request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                @Valid @RequestBody OrderUpdateRequest request) {
        return orderService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        orderService.delete(user, id);
    }

    /** Records a delivery attempt; moves the order to SHIPPED, DELIVERED or RETURNED. */
    @PostMapping("/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse recordAttempt(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                       @Valid @RequestBody DeliveryAttemptRequest request) {
        return orderService.recordAttempt(user, id, request);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return orderService.cancel(user, id);
    }
}

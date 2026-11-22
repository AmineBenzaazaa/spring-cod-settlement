package com.msetransport.codsettlement.logistics.service;

import com.msetransport.codsettlement.common.error.ConflictException;
import com.msetransport.codsettlement.common.error.ResourceNotFoundException;
import com.msetransport.codsettlement.common.web.PageResponse;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.logistics.mapper.LogisticsMapper;
import com.msetransport.codsettlement.logistics.repository.CourierRepository;
import com.msetransport.codsettlement.logistics.repository.OrderRepository;
import com.msetransport.codsettlement.logistics.web.dto.CourierRequest;
import com.msetransport.codsettlement.logistics.web.dto.CourierResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Couriers are shared reference data. Write access (staff only) is enforced by the URL rules. */
@Service
@Transactional(readOnly = true)
public class CourierService {

    private final CourierRepository couriers;
    private final OrderRepository orders;
    private final LogisticsMapper mapper;

    public CourierService(CourierRepository couriers, OrderRepository orders, LogisticsMapper mapper) {
        this.couriers = couriers;
        this.orders = orders;
        this.mapper = mapper;
    }

    public PageResponse<CourierResponse> list(Boolean active, String q, Pageable pageable) {
        List<Specification<Courier>> specs = new ArrayList<>();
        if (active != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)));
        }
        return PageResponse.of(couriers.findAll(Specification.allOf(specs), pageable).map(mapper::toResponse));
    }

    public CourierResponse get(long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CourierResponse create(CourierRequest request) {
        if (couriers.existsByNameIgnoreCase(request.name().strip()) || couriers.existsByCode(request.code())) {
            throw new ConflictException("A courier with this name or code already exists.");
        }
        Courier courier = new Courier(request.name().strip(), request.code(), request.contactPhone(),
                request.activeOrDefault());
        return mapper.toResponse(couriers.save(courier));
    }

    @Transactional
    public CourierResponse update(long id, CourierRequest request) {
        Courier courier = find(id);
        if (couriers.existsByNameIgnoreCaseAndIdNot(request.name().strip(), id)
                || couriers.existsByCodeAndIdNot(request.code(), id)) {
            throw new ConflictException("A courier with this name or code already exists.");
        }
        courier.update(request.name().strip(), request.code(), request.contactPhone(), request.activeOrDefault());
        return mapper.toResponse(courier);
    }

    @Transactional
    public void delete(long id) {
        Courier courier = find(id);
        if (orders.existsByCourierId(id)) {
            throw new ConflictException("Courier has orders; deactivate it instead.");
        }
        couriers.delete(courier);
    }

    private Courier find(long id) {
        return couriers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Courier", id));
    }
}

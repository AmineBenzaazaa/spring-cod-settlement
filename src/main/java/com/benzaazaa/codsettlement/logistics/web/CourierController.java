package com.benzaazaa.codsettlement.logistics.web;

import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.common.web.SortGuard;
import com.benzaazaa.codsettlement.logistics.service.CourierService;
import com.benzaazaa.codsettlement.logistics.web.dto.CourierRequest;
import com.benzaazaa.codsettlement.logistics.web.dto.CourierResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Read: any authenticated user. Write: STAFF only (see the URL rules in SecurityConfig). */
@RestController
@RequestMapping("/api/couriers")
public class CourierController {

    private static final Set<String> SORTABLE = Set.of("name", "code", "createdAt");

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @GetMapping
    public PageResponse<CourierResponse> list(@RequestParam(required = false) Boolean active,
                                              @RequestParam(required = false) String q,
                                              @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return courierService.list(active, q, SortGuard.allow(pageable, SORTABLE));
    }

    @GetMapping("/{id}")
    public CourierResponse get(@PathVariable long id) {
        return courierService.get(id);
    }

    @PostMapping
    public ResponseEntity<CourierResponse> create(@Valid @RequestBody CourierRequest request) {
        CourierResponse created = courierService.create(request);
        return ResponseEntity.created(URI.create("/api/couriers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public CourierResponse update(@PathVariable long id, @Valid @RequestBody CourierRequest request) {
        return courierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        courierService.delete(id);
    }
}

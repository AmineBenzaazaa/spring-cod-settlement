package com.benzaazaa.codsettlement.settlement.web;

import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.common.web.SortGuard;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.service.DisputeService;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeCreateRequest;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeFilter;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResolveRequest;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final Set<String> SORTABLE = Set.of("createdAt", "claimedAmount", "status");

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    public PageResponse<DisputeResponse> list(@AuthenticationPrincipal AuthenticatedUser user, DisputeFilter filter,
                                              @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                              Pageable pageable) {
        return disputeService.list(user, filter, SortGuard.allow(pageable, SORTABLE));
    }

    /** Download the (filtered) disputes as CSV. Declared before /{id} only for readability; paths do not clash. */
    @GetMapping("/export")
    public ResponseEntity<String> export(@AuthenticationPrincipal AuthenticatedUser user, DisputeFilter filter) {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("disputes.csv").build().toString())
                .body(disputeService.exportCsv(user, filter));
    }

    @GetMapping("/{id}")
    public DisputeResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return disputeService.get(user, id);
    }

    @PostMapping
    public ResponseEntity<DisputeResponse> open(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody DisputeCreateRequest request) {
        DisputeResponse created = disputeService.open(user, request);
        return ResponseEntity.created(URI.create("/api/disputes/" + created.id())).body(created);
    }

    /** Staff closes a dispute as RESOLVED (courier pays) or REJECTED. */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('STAFF')")
    public DisputeResponse resolve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                   @Valid @RequestBody DisputeResolveRequest request) {
        return disputeService.resolve(user, id, request);
    }
}

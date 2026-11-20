package com.benzaazaa.codsettlement.settlement.web;

import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.common.web.SortGuard;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import com.benzaazaa.codsettlement.settlement.service.StatementIngestionService;
import com.benzaazaa.codsettlement.settlement.service.StatementService;
import com.benzaazaa.codsettlement.settlement.web.dto.ReconciliationReportResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementFilter;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementUploadForm;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementUploadResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Remittance statements: created by uploading a courier CSV, immutable afterwards. */
@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private static final Set<String> SORTABLE = Set.of("uploadedAt", "periodStart", "periodEnd");

    private final StatementIngestionService ingestionService;
    private final StatementService statementService;

    public StatementController(StatementIngestionService ingestionService, StatementService statementService) {
        this.ingestionService = ingestionService;
        this.statementService = statementService;
    }

    /**
     * 202 Accepted: stored and queued, reconciled in the background (poll GET /{id}).
     * 200 OK with {@code duplicate=true}: this exact file was already uploaded.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatementUploadResponse> upload(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @RequestPart("file") MultipartFile file,
                                                          @Valid @ModelAttribute StatementUploadForm form)
            throws IOException {
        StatementUploadResponse result = ingestionService.upload(user, form, file.getOriginalFilename(), file.getBytes());
        if (result.duplicate()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.accepted()
                .location(URI.create("/api/statements/" + result.statement().id()))
                .body(result);
    }

    @GetMapping
    public PageResponse<StatementResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                StatementFilter filter,
                                                @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
                                                Pageable pageable) {
        return statementService.list(user, filter, SortGuard.allow(pageable, SORTABLE));
    }

    @GetMapping("/{id}")
    public StatementResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return statementService.get(user, id);
    }

    /** Reconciliation report; {@code ?outcome=AMOUNT_MISMATCH} narrows the results. */
    @GetMapping("/{id}/reconciliation")
    public ReconciliationReportResponse reconciliation(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @PathVariable long id,
                                                       @RequestParam(required = false) ReconciliationOutcome outcome) {
        return statementService.report(user, id, outcome);
    }

    /** Method security: only staff may re-run processing. */
    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<StatementResponse> reprocess(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @PathVariable long id) {
        return ResponseEntity.accepted().body(statementService.reprocess(user, id));
    }
}

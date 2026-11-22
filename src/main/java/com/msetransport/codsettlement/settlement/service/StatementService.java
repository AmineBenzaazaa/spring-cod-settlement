package com.msetransport.codsettlement.settlement.service;

import com.msetransport.codsettlement.common.error.ConflictException;
import com.msetransport.codsettlement.common.error.ResourceNotFoundException;
import com.msetransport.codsettlement.common.web.PageResponse;
import com.msetransport.codsettlement.security.AuthenticatedUser;
import com.msetransport.codsettlement.security.TenantSpecifications;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import com.msetransport.codsettlement.settlement.domain.ReconciliationResult;
import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import com.msetransport.codsettlement.settlement.domain.StatementStatus;
import com.msetransport.codsettlement.settlement.mapper.SettlementMapper;
import com.msetransport.codsettlement.settlement.repository.DisputeRepository;
import com.msetransport.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.msetransport.codsettlement.settlement.web.dto.ReconciliationReportResponse;
import com.msetransport.codsettlement.settlement.web.dto.StatementFilter;
import com.msetransport.codsettlement.settlement.web.dto.StatementResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of statements, always scoped to the caller's merchant. */
@Service
@Transactional(readOnly = true)
public class StatementService {

    private final RemittanceStatementRepository statements;
    private final ReconciliationResultRepository results;
    private final DisputeRepository disputes;
    private final ReconciliationService reconciliation;
    private final SettlementMapper mapper;
    private final ApplicationEventPublisher events;

    public StatementService(RemittanceStatementRepository statements, ReconciliationResultRepository results,
                            DisputeRepository disputes, ReconciliationService reconciliation, SettlementMapper mapper,
                            ApplicationEventPublisher events) {
        this.statements = statements;
        this.results = results;
        this.disputes = disputes;
        this.reconciliation = reconciliation;
        this.mapper = mapper;
        this.events = events;
    }

    public PageResponse<StatementResponse> list(AuthenticatedUser user, StatementFilter filter, Pageable pageable) {
        List<Specification<RemittanceStatement>> specs = new ArrayList<>();
        specs.add(TenantSpecifications.visibleTo(user, "merchant"));
        if (filter.courierId() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("courier").get("id"), filter.courierId()));
        }
        if (filter.merchantId() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("merchant").get("id"), filter.merchantId()));
        }
        if (filter.status() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.periodFrom() != null) {
            specs.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("periodStart"), filter.periodFrom()));
        }
        if (filter.periodTo() != null) {
            specs.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("periodEnd"), filter.periodTo()));
        }
        return PageResponse.of(statements.findAll(Specification.allOf(specs), pageable).map(mapper::toResponse));
    }

    public StatementResponse get(AuthenticatedUser user, long id) {
        return mapper.toResponse(findVisible(user, id));
    }

    /** Statement + summary + results, optionally only one outcome. */
    public ReconciliationReportResponse report(AuthenticatedUser user, long id, ReconciliationOutcome outcome) {
        RemittanceStatement statement = findVisible(user, id);
        List<ReconciliationResult> rows = outcome == null
                ? results.findForReport(id)
                : results.findForReport(id, outcome);
        Set<Long> disputed = disputes.findResultIdsByStatementId(id); // one query, not one per row
        return new ReconciliationReportResponse(
                mapper.toResponse(statement),
                statement.getStatus() == StatementStatus.RECONCILED ? reconciliation.summaryFor(id) : null,
                rows.stream().map(r -> mapper.toResponse(r, disputed.contains(r.getId()))).toList());
    }

    /** Staff re-runs processing (e.g. after fixing orders, or a FAILED run). Async, like an upload. */
    @Transactional
    public StatementResponse reprocess(AuthenticatedUser user, long id) {
        RemittanceStatement statement = findVisible(user, id);
        reconciliation.ensureNotDisputed(id);
        if (statement.getStatus() == StatementStatus.PROCESSING) {
            throw new ConflictException("Statement is already being processed.");
        }
        statement.requeue();
        events.publishEvent(new StatementUploadedEvent(id));
        return mapper.toResponse(statement);
    }

    RemittanceStatement findVisible(AuthenticatedUser user, long id) {
        Specification<RemittanceStatement> byId = (root, q, cb) -> cb.equal(root.get("id"), id);
        return statements.findOne(byId.and(TenantSpecifications.visibleTo(user, "merchant")))
                .orElseThrow(() -> new ResourceNotFoundException("Statement", id));
    }
}

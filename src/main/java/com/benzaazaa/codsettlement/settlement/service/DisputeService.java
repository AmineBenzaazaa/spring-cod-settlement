package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.account.repository.AppUserRepository;
import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.security.TenantSpecifications;
import com.benzaazaa.codsettlement.settlement.domain.Dispute;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import com.benzaazaa.codsettlement.settlement.mapper.SettlementMapper;
import com.benzaazaa.codsettlement.settlement.repository.DisputeRepository;
import com.benzaazaa.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeCreateRequest;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeFilter;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResolveRequest;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DisputeService {

    private final DisputeRepository disputes;
    private final ReconciliationResultRepository results;
    private final AppUserRepository users;
    private final SettlementMapper mapper;
    private final DisputeCsvWriter csvWriter;
    private final Clock clock;

    public DisputeService(DisputeRepository disputes, ReconciliationResultRepository results, AppUserRepository users,
                          SettlementMapper mapper, DisputeCsvWriter csvWriter, Clock clock) {
        this.disputes = disputes;
        this.results = results;
        this.users = users;
        this.mapper = mapper;
        this.csvWriter = csvWriter;
        this.clock = clock;
    }

    public PageResponse<DisputeResponse> list(AuthenticatedUser user, DisputeFilter filter, Pageable pageable) {
        return PageResponse.of(disputes.findAll(spec(user, filter), pageable).map(mapper::toResponse));
    }

    public DisputeResponse get(AuthenticatedUser user, long id) {
        return mapper.toResponse(findVisible(user, id));
    }

    @Transactional
    public DisputeResponse open(AuthenticatedUser user, DisputeCreateRequest request) {
        ReconciliationResult result = results.findWithStatement(request.resultId())
                .filter(r -> user.isStaff() || r.getStatement().getMerchant().getId().equals(user.merchantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation result", request.resultId()));
        if (disputes.existsByResultId(result.getId())) {
            throw new ConflictException("This result is already disputed.");
        }
        BigDecimal claimed = request.claimedAmount() != null
                ? request.claimedAmount()
                : result.getDifference().abs(); // default claim = the full discrepancy
        Dispute dispute = new Dispute(result, result.getStatement().getMerchant(), request.reason().strip(), claimed,
                users.getReferenceById(user.id()));
        return mapper.toResponse(disputes.save(dispute));
    }

    /** Staff closes a dispute. {@code @Version} turns a concurrent double-close into a 409. */
    @Transactional
    public DisputeResponse resolve(AuthenticatedUser user, long id, DisputeResolveRequest request) {
        Dispute dispute = findVisible(user, id);
        dispute.close(request.decision(), request.resolutionNote(), clock.instant());
        return mapper.toResponse(disputes.saveAndFlush(dispute));
    }

    public String exportCsv(AuthenticatedUser user, DisputeFilter filter) {
        return csvWriter.write(disputes.findAll(spec(user, filter), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private Dispute findVisible(AuthenticatedUser user, long id) {
        Specification<Dispute> byId = (root, q, cb) -> cb.equal(root.get("id"), id);
        return disputes.findOne(byId.and(TenantSpecifications.visibleTo(user, "merchant")))
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
    }

    private static Specification<Dispute> spec(AuthenticatedUser user, DisputeFilter filter) {
        List<Specification<Dispute>> specs = new ArrayList<>();
        specs.add(TenantSpecifications.visibleTo(user, "merchant"));
        if (filter.status() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.merchantId() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("merchant").get("id"), filter.merchantId()));
        }
        if (filter.statementId() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("result").get("statement").get("id"), filter.statementId()));
        }
        if (filter.courierId() != null) {
            specs.add((root, q, cb) ->
                    cb.equal(root.get("result").get("statement").get("courier").get("id"), filter.courierId()));
        }
        return Specification.allOf(specs);
    }
}

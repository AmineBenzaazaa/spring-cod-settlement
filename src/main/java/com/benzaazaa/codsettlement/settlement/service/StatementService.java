package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.security.TenantSpecifications;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.mapper.SettlementMapper;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementFilter;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of statements, always scoped to the caller's merchant. */
@Service
@Transactional(readOnly = true)
public class StatementService {

    private final RemittanceStatementRepository statements;
    private final SettlementMapper mapper;

    public StatementService(RemittanceStatementRepository statements, SettlementMapper mapper) {
        this.statements = statements;
        this.mapper = mapper;
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

    RemittanceStatement findVisible(AuthenticatedUser user, long id) {
        Specification<RemittanceStatement> byId = (root, q, cb) -> cb.equal(root.get("id"), id);
        return statements.findOne(byId.and(TenantSpecifications.visibleTo(user, "merchant")))
                .orElseThrow(() -> new ResourceNotFoundException("Statement", id));
    }
}

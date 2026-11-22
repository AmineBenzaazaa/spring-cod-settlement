package com.msetransport.codsettlement.settlement.web.dto;

import com.msetransport.codsettlement.settlement.domain.DisputeStatus;

public record DisputeFilter(DisputeStatus status, Long courierId, Long statementId, Long merchantId) {

    public static DisputeFilter empty() {
        return new DisputeFilter(null, null, null, null);
    }
}

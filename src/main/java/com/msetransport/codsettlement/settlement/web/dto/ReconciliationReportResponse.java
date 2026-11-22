package com.msetransport.codsettlement.settlement.web.dto;

import com.msetransport.codsettlement.settlement.service.ReconciliationSummary;
import java.util.List;

/** @param summary {@code null} until the statement is RECONCILED */
public record ReconciliationReportResponse(StatementResponse statement, ReconciliationSummary summary,
                                           List<ReconciliationResultResponse> results) {
}

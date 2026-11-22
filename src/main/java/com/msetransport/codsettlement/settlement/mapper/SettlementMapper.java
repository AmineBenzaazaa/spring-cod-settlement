package com.msetransport.codsettlement.settlement.mapper;

import com.msetransport.codsettlement.settlement.domain.Dispute;
import com.msetransport.codsettlement.settlement.domain.ReconciliationResult;
import com.msetransport.codsettlement.settlement.domain.RemittanceLine;
import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import com.msetransport.codsettlement.settlement.web.dto.DisputeResponse;
import com.msetransport.codsettlement.settlement.web.dto.ReconciliationResultResponse;
import com.msetransport.codsettlement.settlement.web.dto.RemittanceLineResponse;
import com.msetransport.codsettlement.settlement.web.dto.StatementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SettlementMapper {

    @Mapping(target = "merchantId", source = "merchant.id")
    @Mapping(target = "courierId", source = "courier.id")
    StatementResponse toResponse(RemittanceStatement statement);

    RemittanceLineResponse toResponse(RemittanceLine line);

    /** {@code hasDispute} is a second source parameter, mapped by name. */
    @Mapping(target = "orderId", source = "result.order.id")
    ReconciliationResultResponse toResponse(ReconciliationResult result, boolean hasDispute);

    @Mapping(target = "resultId", source = "result.id")
    @Mapping(target = "statementId", source = "result.statement.id")
    @Mapping(target = "trackingNumber", source = "result.trackingNumber")
    @Mapping(target = "outcome", source = "result.outcome")
    @Mapping(target = "merchantId", source = "merchant.id")
    DisputeResponse toResponse(Dispute dispute);
}

package com.benzaazaa.codsettlement.settlement.mapper;

import com.benzaazaa.codsettlement.settlement.domain.Dispute;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.ReconciliationResultResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.RemittanceLineResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementResponse;
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

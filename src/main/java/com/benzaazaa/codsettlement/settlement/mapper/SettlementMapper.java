package com.benzaazaa.codsettlement.settlement.mapper;

import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
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

    @Mapping(target = "orderId", source = "order.id")
    ReconciliationResultResponse toResponse(ReconciliationResult result);
}

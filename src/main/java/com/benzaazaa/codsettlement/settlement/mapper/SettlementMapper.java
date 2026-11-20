package com.benzaazaa.codsettlement.settlement.mapper;

import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SettlementMapper {

    @Mapping(target = "merchantId", source = "merchant.id")
    @Mapping(target = "courierId", source = "courier.id")
    StatementResponse toResponse(RemittanceStatement statement);
}

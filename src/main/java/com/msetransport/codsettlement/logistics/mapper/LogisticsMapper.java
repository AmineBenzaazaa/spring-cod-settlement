package com.msetransport.codsettlement.logistics.mapper;

import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.logistics.domain.DeliveryAttempt;
import com.msetransport.codsettlement.logistics.domain.Order;
import com.msetransport.codsettlement.logistics.web.dto.CourierResponse;
import com.msetransport.codsettlement.logistics.web.dto.DeliveryAttemptResponse;
import com.msetransport.codsettlement.logistics.web.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity to DTO mapping, generated at compile time by MapStruct (see
 * {@code target/generated-sources/annotations/.../LogisticsMapperImpl.java}). No reflection
 * at runtime, and an unmapped DTO field fails the build.
 */
@Mapper
public interface LogisticsMapper {

    CourierResponse toResponse(Courier courier);

    @Mapping(target = "merchantId", source = "merchant.id")
    @Mapping(target = "courierId", source = "courier.id")
    @Mapping(target = "courierName", source = "courier.name")
    OrderResponse toResponse(Order order);

    DeliveryAttemptResponse toResponse(DeliveryAttempt attempt);
}

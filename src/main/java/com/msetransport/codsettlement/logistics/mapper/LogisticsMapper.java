package com.benzaazaa.codsettlement.logistics.mapper;

import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.domain.DeliveryAttempt;
import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.logistics.web.dto.CourierResponse;
import com.benzaazaa.codsettlement.logistics.web.dto.DeliveryAttemptResponse;
import com.benzaazaa.codsettlement.logistics.web.dto.OrderResponse;
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

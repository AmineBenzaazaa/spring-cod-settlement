package com.benzaazaa.codsettlement.logistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.mapper.LogisticsMapperImpl;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.logistics.repository.OrderRepository;
import com.benzaazaa.codsettlement.logistics.web.dto.CourierRequest;
import com.benzaazaa.codsettlement.support.TestData;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock CourierRepository couriers;
    @Mock OrderRepository orders;

    private CourierService service;

    @BeforeEach
    void setUp() {
        service = new CourierService(couriers, orders, new LogisticsMapperImpl());
    }

    @Test
    void createDefaultsToActive() {
        when(couriers.save(any())).thenAnswer(inv -> TestData.withId(inv.getArgument(0, Courier.class), 3L));

        var response = service.create(new CourierRequest(" Amana ", "amana", null, null));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Amana");
        assertThat(response.active()).isTrue();
    }

    @Test
    void duplicateNameOrCodeIsAConflict() {
        when(couriers.existsByNameIgnoreCase("Amana")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CourierRequest("Amana", "amana", null, true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCanDeactivate() {
        when(couriers.findById(3L)).thenReturn(Optional.of(TestData.courier(3)));

        var response = service.update(3L, new CourierRequest("Courier 3", "courier-3", "0522", false));

        assertThat(response.active()).isFalse();
        assertThat(response.contactPhone()).isEqualTo("0522");
    }

    @Test
    void courierWithOrdersCannotBeDeleted() {
        when(couriers.findById(3L)).thenReturn(Optional.of(TestData.courier(3)));
        when(orders.existsByCourierId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(3L)).hasMessage("Courier has orders; deactivate it instead.");
        verify(couriers, never()).delete(any(Courier.class));
    }

    @Test
    void unknownCourierIsNotFound() {
        when(couriers.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}

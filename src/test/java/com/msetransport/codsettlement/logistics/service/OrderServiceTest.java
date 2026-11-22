package com.msetransport.codsettlement.logistics.service;

import static com.msetransport.codsettlement.support.TestData.courier;
import static com.msetransport.codsettlement.support.TestData.merchant;
import static com.msetransport.codsettlement.support.TestData.merchantUser;
import static com.msetransport.codsettlement.support.TestData.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msetransport.codsettlement.account.repository.MerchantRepository;
import com.msetransport.codsettlement.common.error.BusinessRuleException;
import com.msetransport.codsettlement.common.error.ConflictException;
import com.msetransport.codsettlement.common.error.InvalidRequestException;
import com.msetransport.codsettlement.common.error.ResourceNotFoundException;
import com.msetransport.codsettlement.logistics.domain.AttemptOutcome;
import com.msetransport.codsettlement.logistics.domain.Order;
import com.msetransport.codsettlement.logistics.domain.OrderStatus;
import com.msetransport.codsettlement.logistics.mapper.LogisticsMapperImpl;
import com.msetransport.codsettlement.logistics.repository.CourierRepository;
import com.msetransport.codsettlement.logistics.repository.OrderRepository;
import com.msetransport.codsettlement.logistics.web.dto.DeliveryAttemptRequest;
import com.msetransport.codsettlement.logistics.web.dto.OrderCreateRequest;
import com.msetransport.codsettlement.logistics.web.dto.OrderFilter;
import com.msetransport.codsettlement.logistics.web.dto.OrderResponse;
import com.msetransport.codsettlement.logistics.web.dto.OrderUpdateRequest;
import com.msetransport.codsettlement.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orders;
    @Mock CourierRepository couriers;
    @Mock MerchantRepository merchants;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, couriers, merchants, new LogisticsMapperImpl(),
                Clock.fixed(TestData.NOW, ZoneOffset.UTC));
    }

    private static OrderCreateRequest create(Long merchantId) {
        return new OrderCreateRequest(merchantId, 7L, " spx-9 ", "Sara Tazi", null, "Rabat", new BigDecimal("349.90"));
    }

    private void saveReturnsArgument() {
        when(orders.save(any())).thenAnswer(inv -> TestData.withId(inv.getArgument(0, Order.class), 5L));
    }

    @Test
    void merchantUserAlwaysCreatesForTheirOwnMerchant() {
        when(merchants.getReferenceById(1L)).thenReturn(merchant(1));
        when(couriers.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(courier(7)));
        saveReturnsArgument();

        OrderResponse response = service.create(merchantUser(1), create(2L)); // body says merchant 2

        assertThat(response.merchantId()).isEqualTo(1L);
        assertThat(response.trackingNumber()).isEqualTo("SPX-9");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        verify(merchants, never()).findById(any());
    }

    @Test
    void staffMustChooseAnExistingMerchant() {
        assertThatThrownBy(() -> service.create(staff(), create(null))).hasMessage("Staff must specify the merchant.");

        when(merchants.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(staff(), create(3L))).hasMessage("Unknown merchant.");
    }

    @Test
    void staffCreatesForTheRequestedMerchant() {
        when(merchants.findById(2L)).thenReturn(Optional.of(merchant(2)));
        when(couriers.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(courier(7)));
        saveReturnsArgument();

        assertThat(service.create(staff(), create(2L)).merchantId()).isEqualTo(2L);
    }

    @Test
    void inactiveCourierIsRejected() {
        when(merchants.getReferenceById(1L)).thenReturn(merchant(1));
        when(couriers.findByIdAndActiveTrue(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(merchantUser(1), create(null)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("courierId");
    }

    @Test
    void trackingNumberMustBeUniquePerCourier() {
        when(merchants.getReferenceById(1L)).thenReturn(merchant(1));
        when(couriers.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(courier(7)));
        when(orders.existsByCourierIdAndTrackingNumber(7L, "SPX-9")).thenReturn(true);

        assertThatThrownBy(() -> service.create(merchantUser(1), create(null))).isInstanceOf(ConflictException.class);
        verify(orders, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchIsAlwaysCombinedWithTheTenantScope() {
        Order order = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        Pageable page = PageRequest.of(0, 20);
        when(orders.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order), page, 1));

        var result = service.search(merchantUser(1), OrderFilter.empty(), page);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().getFirst().courierName()).isEqualTo("Courier 7");
    }

    @Test
    void recordAttemptDefaultsToNowAndLocksTheRow() {
        Order order = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        when(orders.findByIdForUpdate(5L)).thenReturn(Optional.of(order));
        when(orders.saveAndFlush(order)).thenReturn(order);

        OrderResponse response = service.recordAttempt(merchantUser(1), 5L,
                new DeliveryAttemptRequest(AttemptOutcome.DELIVERED, null, null));

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(response.deliveredAt()).isEqualTo(TestData.NOW);
        assertThat(response.attempts()).hasSize(1);
    }

    @Test
    void recordAttemptOnAnotherMerchantsOrderIsNotFound() {
        when(orders.findByIdForUpdate(5L)).thenReturn(Optional.of(TestData.order(5, merchant(2), courier(7), "T1", "10.00")));

        assertThatThrownBy(() -> service.recordAttempt(merchantUser(1), 5L,
                new DeliveryAttemptRequest(AttemptOutcome.DELIVERED, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyPendingOrdersCanBeDeleted() {
        Order shipped = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        shipped.recordAttempt(AttemptOutcome.NO_ANSWER, TestData.NOW, null);
        when(orders.findOne(any(Specification.class))).thenReturn(Optional.of(shipped));

        assertThatThrownBy(() -> service.delete(merchantUser(1), 5L)).isInstanceOf(BusinessRuleException.class);
        verify(orders, never()).delete(any(Order.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pendingOrderIsDeleted() {
        Order pending = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        when(orders.findOne(any(Specification.class))).thenReturn(Optional.of(pending));

        service.delete(merchantUser(1), 5L);

        verify(orders).delete(pending);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancelMovesAPendingOrderToCancelled() {
        Order pending = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        when(orders.findOne(any(Specification.class))).thenReturn(Optional.of(pending));
        when(orders.saveAndFlush(pending)).thenReturn(pending);

        assertThat(service.cancel(merchantUser(1), 5L).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateChecksTrackingUniquenessExcludingItself() {
        Order order = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        when(orders.findOne(any(Specification.class))).thenReturn(Optional.of(order));
        when(orders.existsByCourierIdAndTrackingNumberAndIdNot(7L, "T2", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(merchantUser(1), 5L,
                new OrderUpdateRequest(7L, "t2", "Name", null, null, BigDecimal.TEN)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateCanMoveTheOrderToAnotherActiveCourier() {
        Order order = TestData.order(5, merchant(1), courier(7), "T1", "10.00");
        when(orders.findOne(any(Specification.class))).thenReturn(Optional.of(order));
        when(couriers.findByIdAndActiveTrue(8L)).thenReturn(Optional.of(courier(8)));
        when(orders.saveAndFlush(order)).thenReturn(order);
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);

        OrderResponse response = service.update(merchantUser(1), 5L,
                new OrderUpdateRequest(8L, "t1", "New Name", "0600", "Fes", new BigDecimal("12.5")));

        verify(orders).saveAndFlush(saved.capture());
        assertThat(response.courierId()).isEqualTo(8L);
        assertThat(response.amountDue()).isEqualTo(new BigDecimal("12.50"));
        assertThat(response.customerName()).isEqualTo("New Name");
    }
}

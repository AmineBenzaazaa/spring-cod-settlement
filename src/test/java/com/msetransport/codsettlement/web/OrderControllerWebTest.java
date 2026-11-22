package com.msetransport.codsettlement.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msetransport.codsettlement.common.error.BusinessRuleException;
import com.msetransport.codsettlement.common.error.ConflictException;
import com.msetransport.codsettlement.common.error.ResourceNotFoundException;
import com.msetransport.codsettlement.common.web.PageResponse;
import com.msetransport.codsettlement.logistics.domain.OrderStatus;
import com.msetransport.codsettlement.logistics.service.OrderService;
import com.msetransport.codsettlement.logistics.web.OrderController;
import com.msetransport.codsettlement.logistics.web.dto.OrderFilter;
import com.msetransport.codsettlement.logistics.web.dto.OrderResponse;
import com.msetransport.codsettlement.support.TestData;
import com.msetransport.codsettlement.support.WebSliceTestConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(OrderController.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class OrderControllerWebTest extends WebSliceTest {

    private static final OrderResponse ORDER = new OrderResponse(5L, 1L, 7L, "Speedex", "SPX-1", "Sara Tazi", null,
            "Rabat", new BigDecimal("349.90"), "MAD", OrderStatus.PENDING, null, List.of(), TestData.NOW, TestData.NOW);

    @MockitoBean
    OrderService orderService;

    @Test
    void listBindsFiltersAndPagingFromTheQueryString() throws Exception {
        when(orderService.search(any(), any(), any())).thenReturn(new PageResponse<>(List.of(ORDER), 0, 5, 1, 1));

        mvc.perform(get("/api/orders")
                        .param("status", "DELIVERED", "RETURNED")
                        .param("minAmount", "300")
                        .param("q", "tazi")
                        .param("size", "5")
                        .param("sort", "amountDue,desc")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amountDue").value("349.90")) // money is a string
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<OrderFilter> filter = ArgumentCaptor.forClass(OrderFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).search(eq(TestData.merchantUser(1)), filter.capture(), pageable.capture());
        assertThat(filter.getValue().status()).containsExactly(OrderStatus.DELIVERED, OrderStatus.RETURNED);
        assertThat(filter.getValue().minAmount()).isEqualByComparingTo("300");
        assertThat(filter.getValue().q()).isEqualTo("tazi");
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("amountDue").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void sortingByAnUnlistedPropertyIsRejected() throws Exception {
        mvc.perform(get("/api/orders").param("sort", "customerPhone").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort[0]").exists());
        verifyNoInteractions(orderService);
    }

    @Test
    void unknownEnumValueIsA400() throws Exception {
        mvc.perform(get("/api/orders").param("status", "LOST").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(orderService.create(any(), any())).thenReturn(ORDER);

        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courierId":7,"trackingNumber":"SPX-1","customerName":"Sara Tazi","amountDue":"349.90"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/orders/5"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReportsEveryInvalidField() throws Exception {
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"","customerName":"Sara","amountDue":-1.005}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courierId").exists())
                .andExpect(jsonPath("$.errors.trackingNumber").exists())
                .andExpect(jsonPath("$.errors.amountDue.length()").value(2)); // negative AND 3 decimals
        verifyNoInteractions(orderService);
    }

    @Test
    void domainExceptionsMapToStatusCodes() throws Exception {
        when(orderService.get(any(), eq(1L))).thenThrow(new ResourceNotFoundException("Order", 1L));
        when(orderService.cancel(any(), eq(2L))).thenThrow(new BusinessRuleException("Cannot cancel a DELIVERED order."));
        when(orderService.create(any(), any())).thenThrow(new ConflictException("Tracking number exists"));

        mvc.perform(get("/api/orders/1").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://settle.msetransport.com/problems/not-found"));
        mvc.perform(post("/api/orders/2/cancel").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Cannot cancel a DELIVERED order."));
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courierId\":7,\"trackingNumber\":\"X\",\"customerName\":\"S\",\"amountDue\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204AndAttemptReturns201() throws Exception {
        mvc.perform(delete("/api/orders/5").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isNoContent());
        verify(orderService).delete(TestData.merchantUser(1), 5L);

        when(orderService.recordAttempt(any(), eq(5L), any())).thenReturn(ORDER);
        mvc.perform(post("/api/orders/5/attempts").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"DELIVERED\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unexpectedErrorsDoNotLeakDetails() throws Exception {
        doThrow(new IllegalStateException("SELECT * FROM secret_table failed")).when(orderService).delete(any(), eq(9L));

        mvc.perform(delete("/api/orders/9").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side."));
    }

    @Test
    void ordersRequireAuthentication() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }
}

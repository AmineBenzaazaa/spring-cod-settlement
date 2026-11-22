package com.benzaazaa.codsettlement.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benzaazaa.codsettlement.common.web.PageResponse;
import com.benzaazaa.codsettlement.logistics.service.CourierService;
import com.benzaazaa.codsettlement.logistics.web.CourierController;
import com.benzaazaa.codsettlement.logistics.web.dto.CourierResponse;
import com.benzaazaa.codsettlement.settlement.service.CourierBalanceService;
import com.benzaazaa.codsettlement.settlement.web.CourierBalanceController;
import com.benzaazaa.codsettlement.settlement.web.dto.CourierBalanceResponse;
import com.benzaazaa.codsettlement.support.TestData;
import com.benzaazaa.codsettlement.support.WebSliceTestConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest({CourierController.class, CourierBalanceController.class})
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class CourierControllerWebTest extends WebSliceTest {

    private static final CourierResponse COURIER = new CourierResponse(7L, "Speedex", "speedex", null, true, TestData.NOW);
    private static final String BODY = "{\"name\":\"Amana\",\"code\":\"amana\"}";

    @MockitoBean CourierService courierService;
    @MockitoBean CourierBalanceService balanceService;

    @Test
    void merchantsCanReadCouriers() throws Exception {
        when(courierService.list(any(), any(), any())).thenReturn(new PageResponse<>(List.of(COURIER), 0, 20, 1, 1));

        mvc.perform(get("/api/couriers").param("active", "true").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("speedex"));
    }

    @Test
    void merchantsCannotWriteCouriers() throws Exception {
        mvc.perform(post("/api/couriers").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/couriers/7").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(courierService);
    }

    @Test
    void staffCreatesCouriersWithValidatedCodes() throws Exception {
        when(courierService.create(any())).thenReturn(COURIER);

        mvc.perform(post("/api/couriers").header(HttpHeaders.AUTHORIZATION, staffToken())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/couriers").header(HttpHeaders.AUTHORIZATION, staffToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"code\":\"Not A Slug\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists());
    }

    @Test
    void balanceIsReadableByMerchants() throws Exception {
        when(balanceService.balance(eq(TestData.merchantUser(1)), eq(7L), eq(null))).thenReturn(new CourierBalanceResponse(
                7L, 1L, "MAD", 8, new BigDecimal("4588.00"), new BigDecimal("4057.50"), new BigDecimal("225.00"),
                new BigDecimal("3832.50"), new BigDecimal("530.50")));

        mvc.perform(get("/api/couriers/7/balance").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceOwed").value("530.50"))
                .andExpect(jsonPath("$.deliveredOrders").value(8));
    }

    @Test
    void anonymousCallersAreRejected() throws Exception {
        mvc.perform(get("/api/couriers")).andExpect(status().isUnauthorized());
    }
}

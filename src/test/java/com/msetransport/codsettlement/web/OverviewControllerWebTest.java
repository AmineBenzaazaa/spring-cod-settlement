package com.benzaazaa.codsettlement.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benzaazaa.codsettlement.settlement.service.SettlementOverviewService;
import com.benzaazaa.codsettlement.settlement.web.OverviewController;
import com.benzaazaa.codsettlement.settlement.web.dto.OverviewResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.OverviewResponse.CourierPosition;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.benzaazaa.codsettlement.support.WebSliceTestConfig;

@WebMvcTest(OverviewController.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class OverviewControllerWebTest extends WebSliceTest {

    private static final OverviewResponse OVERVIEW = new OverviewResponse(1L, "MAD",
            new BigDecimal("4588.00"), new BigDecimal("4057.50"), new BigDecimal("225.00"),
            new BigDecimal("3832.50"), new BigDecimal("530.50"), 1, new BigDecimal("50.00"), 3,
            new BigDecimal("1009.50"),
            List.of(new CourierPosition(1L, "Speedex Colis", "speedex", true, 8, new BigDecimal("4588.00"),
                    new BigDecimal("4057.50"), new BigDecimal("225.00"), new BigDecimal("3832.50"),
                    new BigDecimal("530.50"))));

    @MockitoBean
    SettlementOverviewService overviewService;

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
        verifyNoInteractions(overviewService);
    }

    @Test
    void returnsMoneyAsStrings() throws Exception {
        when(overviewService.overview(any(), eq(null))).thenReturn(OVERVIEW);

        mvc.perform(get("/api/overview").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceOwed").value("530.50"))
                .andExpect(jsonPath("$.discrepancyAmount").value("1009.50"))
                .andExpect(jsonPath("$.openDisputes").value(1))
                .andExpect(jsonPath("$.couriers[0].name").value("Speedex Colis"))
                .andExpect(jsonPath("$.couriers[0].netRemitted").value("3832.50"));
    }

    @Test
    void passesTheStaffMerchantFilterThrough() throws Exception {
        when(overviewService.overview(argThat(u -> u.isStaff()), eq(2L))).thenReturn(OVERVIEW);

        mvc.perform(get("/api/overview").param("merchantId", "2").header(HttpHeaders.AUTHORIZATION, staffToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("MAD"));
    }
}

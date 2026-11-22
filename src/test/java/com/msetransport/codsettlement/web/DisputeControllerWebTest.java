package com.benzaazaa.codsettlement.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benzaazaa.codsettlement.settlement.domain.DisputeStatus;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import com.benzaazaa.codsettlement.settlement.service.DisputeService;
import com.benzaazaa.codsettlement.settlement.web.DisputeController;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResponse;
import com.benzaazaa.codsettlement.support.TestData;
import com.benzaazaa.codsettlement.support.WebSliceTestConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DisputeController.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class DisputeControllerWebTest extends WebSliceTest {

    private static final DisputeResponse DISPUTE = new DisputeResponse(9L, 3L, 10L, "T2",
            ReconciliationOutcome.AMOUNT_MISMATCH, 1L, "short", new BigDecimal("50.00"), DisputeStatus.OPEN, null,
            TestData.NOW, null);

    @MockitoBean
    DisputeService disputeService;

    @Test
    void openReturns201() throws Exception {
        when(disputeService.open(any(), any())).thenReturn(DISPUTE);

        mvc.perform(post("/api/disputes").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resultId\":3,\"reason\":\"short\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/disputes/9"))
                .andExpect(jsonPath("$.claimedAmount").value("50.00"));
    }

    @Test
    void openRequiresAReasonAndANonNegativeClaim() throws Exception {
        mvc.perform(post("/api/disputes").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultId\":3,\"reason\":\" \",\"claimedAmount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists())
                .andExpect(jsonPath("$.errors.claimedAmount").exists());
        verifyNoInteractions(disputeService);
    }

    @Test
    void onlyStaffCanResolve() throws Exception {
        String body = "{\"decision\":\"RESOLVED\",\"resolutionNote\":\"paid\"}";
        mvc.perform(post("/api/disputes/9/resolve").header(HttpHeaders.AUTHORIZATION, merchantToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));

        when(disputeService.resolve(any(), eq(9L), any())).thenReturn(DISPUTE);
        mvc.perform(post("/api/disputes/9/resolve").header(HttpHeaders.AUTHORIZATION, staffToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void concurrentResolutionIsA409() throws Exception {
        when(disputeService.resolve(any(), eq(9L), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Dispute", 9L));

        mvc.perform(post("/api/disputes/9/resolve").header(HttpHeaders.AUTHORIZATION, staffToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent update"));
    }

    @Test
    void exportIsADownloadableCsv() throws Exception {
        when(disputeService.exportCsv(any(), any())).thenReturn("dispute_id\n9\n");

        mvc.perform(get("/api/disputes/export").param("status", "OPEN").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"disputes.csv\""))
                .andExpect(content().string("dispute_id\n9\n"));
    }
}

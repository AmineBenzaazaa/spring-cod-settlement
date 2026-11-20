package com.benzaazaa.codsettlement.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import com.benzaazaa.codsettlement.settlement.domain.StatementStatus;
import com.benzaazaa.codsettlement.settlement.service.CsvValidationException;
import com.benzaazaa.codsettlement.settlement.service.StatementIngestionService;
import com.benzaazaa.codsettlement.settlement.service.StatementService;
import com.benzaazaa.codsettlement.settlement.web.StatementController;
import com.benzaazaa.codsettlement.settlement.web.dto.ReconciliationReportResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementUploadForm;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementUploadResponse;
import com.benzaazaa.codsettlement.support.TestData;
import com.benzaazaa.codsettlement.support.WebSliceTestConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(StatementController.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class StatementControllerWebTest extends WebSliceTest {

    private static final byte[] CSV = "tracking_number,amount_collected,fee\nT1,1.00,0\n".getBytes(StandardCharsets.UTF_8);
    private static final StatementResponse STATEMENT = new StatementResponse(42L, 1L, 7L, LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31), "oct.csv", "a".repeat(64), "MAD", StatementStatus.UPLOADED, null, 0,
            TestData.NOW, null);

    @MockitoBean StatementIngestionService ingestionService;
    @MockitoBean StatementService statementService;

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "oct.csv", "text/csv", CSV);
    }

    @Test
    void uploadIsAcceptedAndQueued() throws Exception {
        when(ingestionService.upload(any(), any(), any(), any())).thenReturn(StatementUploadResponse.created(STATEMENT));

        mvc.perform(multipart("/api/statements").file(file())
                        .param("courierId", "7").param("periodStart", "2026-10-01").param("periodEnd", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/statements/42"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.statement.status").value("UPLOADED"));

        ArgumentCaptor<StatementUploadForm> form = ArgumentCaptor.forClass(StatementUploadForm.class);
        verify(ingestionService).upload(eq(TestData.merchantUser(1)), form.capture(), eq("oct.csv"), eq(CSV));
        assertThat(form.getValue().periodEnd()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void duplicateUploadIs200() throws Exception {
        when(ingestionService.upload(any(), any(), any(), any())).thenReturn(StatementUploadResponse.duplicateOf(STATEMENT));

        mvc.perform(multipart("/api/statements").file(file())
                        .param("courierId", "7").param("periodStart", "2026-10-01").param("periodEnd", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void periodEndBeforeStartIsRejectedByTheCustomConstraint() throws Exception {
        mvc.perform(multipart("/api/statements").file(file())
                        .param("courierId", "7").param("periodStart", "2026-10-31").param("periodEnd", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.periodEnd[0]").value("must be on or after periodStart"));
        verifyNoInteractions(ingestionService);
    }

    @Test
    void missingFileIsA400() throws Exception {
        mvc.perform(multipart("/api/statements")
                        .param("courierId", "7").param("periodStart", "2026-10-01").param("periodEnd", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csvRowErrorsAreAllReturned() throws Exception {
        when(ingestionService.upload(any(), any(), any(), any()))
                .thenThrow(new CsvValidationException(List.of("Row 1: tracking_number is empty", "Row 2: fee: 'x' is not a number")));

        mvc.perform(multipart("/api/statements").file(file())
                        .param("courierId", "7").param("periodStart", "2026-10-01").param("periodEnd", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.file.length()").value(2))
                .andExpect(jsonPath("$.detail").value("The request contains invalid data."));
    }

    @Test
    void reportPassesTheOutcomeFilter() throws Exception {
        when(statementService.report(any(), eq(42L), eq(ReconciliationOutcome.AMOUNT_MISMATCH)))
                .thenReturn(new ReconciliationReportResponse(STATEMENT, null, List.of()));

        mvc.perform(get("/api/statements/42/reconciliation").param("outcome", "AMOUNT_MISMATCH")
                        .header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isEmpty());
    }

    @Test
    void onlyStaffCanReprocess() throws Exception {
        mvc.perform(post("/api/statements/42/reprocess").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(statementService);

        when(statementService.reprocess(any(), eq(42L))).thenReturn(STATEMENT);
        mvc.perform(post("/api/statements/42/reprocess").header(HttpHeaders.AUTHORIZATION, staffToken()))
                .andExpect(status().isAccepted());
    }
}

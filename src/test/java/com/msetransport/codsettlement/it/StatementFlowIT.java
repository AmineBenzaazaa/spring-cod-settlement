package com.msetransport.codsettlement.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.logistics.domain.AttemptOutcome;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.settlement.domain.StatementStatus;
import com.msetransport.codsettlement.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** Upload -> async reconciliation after commit -> report -> balance -> dispute -> export, end to end. */
class StatementFlowIT extends AbstractIntegrationTest {

    private static final Instant OCT_10 = Instant.parse("2026-10-10T10:00:00Z");
    private static final String CSV = """
            tracking_number,amount_collected,fee
            SPX-1,300.00,25.00
            SPX-2,150.00,25.00
            SPX-3,0.00,10.00
            SPX-404,99.00,25.00
            """;

    private Merchant atlas;
    private Courier speedex;
    private String atlasToken;

    @BeforeEach
    void setUp() throws Exception {
        atlas = merchant("atlas");
        speedex = courier("speedex");
        merchantUser("atlas", atlas);
        staffUser("ops");
        order(atlas, speedex, "SPX-1", "300.00", AttemptOutcome.DELIVERED, OCT_10);  // matched
        order(atlas, speedex, "SPX-2", "200.00", AttemptOutcome.DELIVERED, OCT_10);  // short by 50
        order(atlas, speedex, "SPX-3", "120.00", AttemptOutcome.REFUSED, OCT_10);    // returned, 0 expected
        order(atlas, speedex, "SPX-4", "80.00", AttemptOutcome.DELIVERED, OCT_10);   // missing from file
        atlasToken = login("atlas");
    }

    private ResultActions upload(String token, String csv, Long merchantId) throws Exception {
        var request = multipart("/api/statements")
                .file(new MockMultipartFile("file", "october.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .param("courierId", speedex.getId().toString())
                .param("periodStart", "2026-10-01")
                .param("periodEnd", "2026-10-31")
                .header(HttpHeaders.AUTHORIZATION, token);
        if (merchantId != null) {
            request.param("merchantId", merchantId.toString());
        }
        return mvc.perform(request);
    }

    @Test
    void uploadIsReconciledInTheBackgroundAndCanBeDisputedAndExported() throws Exception {
        JsonNode accepted = body(upload(atlasToken, CSV, null).andExpect(status().isAccepted()));
        long id = accepted.at("/statement/id").asLong();

        awaitStatus(id, StatementStatus.RECONCILED);

        JsonNode report = body(mvc.perform(get("/api/statements/{id}/reconciliation", id)
                .header(HttpHeaders.AUTHORIZATION, atlasToken)).andExpect(status().isOk()));
        assertThat(report.at("/statement/lineCount").asInt()).isEqualTo(4);
        assertThat(report.at("/summary/counts/MATCHED").asInt()).isEqualTo(2);
        assertThat(report.at("/summary/counts/AMOUNT_MISMATCH").asInt()).isEqualTo(1);
        assertThat(report.at("/summary/counts/UNKNOWN_TRACKING").asInt()).isEqualTo(1);
        assertThat(report.at("/summary/counts/MISSING_FROM_STATEMENT").asInt()).isEqualTo(1);
        assertThat(report.at("/summary/collectedTotal").asText()).isEqualTo("549.00");
        assertThat(report.at("/summary/netRemitted").asText()).isEqualTo("464.00");

        // Balance: expected 580 (300+200+80), reported 450 on our orders (unknown line excluded).
        mvc.perform(get("/api/couriers/{id}/balance", speedex.getId()).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedCollections").value("580.00"))
                .andExpect(jsonPath("$.reportedCollections").value("450.00"))
                .andExpect(jsonPath("$.fees").value("60.00"))
                .andExpect(jsonPath("$.balanceOwed").value("130.00"));

        JsonNode mismatch = body(mvc.perform(get("/api/statements/{id}/reconciliation", id)
                .param("outcome", "AMOUNT_MISMATCH").header(HttpHeaders.AUTHORIZATION, atlasToken)));
        long resultId = mismatch.at("/results/0/id").asLong();

        mvc.perform(post("/api/disputes").header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultId\":" + resultId + ",\"reason\":\"Courier short-paid 50 MAD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimedAmount").value("50.00"));
        mvc.perform(post("/api/disputes").header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultId\":" + resultId + ",\"reason\":\"again\"}"))
                .andExpect(status().isConflict());

        // Results under dispute are evidence: staff can no longer re-run the statement.
        mvc.perform(post("/api/statements/{id}/reprocess", id).header(HttpHeaders.AUTHORIZATION, login("ops")))
                .andExpect(status().isConflict());

        String csv = mvc.perform(get("/api/disputes/export").param("status", "OPEN")
                        .header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv.lines()).hasSize(2);
        assertThat(csv).contains("SPX-2,AMOUNT_MISMATCH,200.00,150.00,50.00,OPEN");
    }

    @Test
    void overviewSumsBalancesOpenDisputesAndDiscrepanciesPerTenant() throws Exception {
        Merchant medina = merchant("medina");
        merchantUser("medina", medina);
        long id = body(upload(atlasToken, CSV, null).andExpect(status().isAccepted())).at("/statement/id").asLong();
        awaitStatus(id, StatementStatus.RECONCILED);
        long resultId = body(mvc.perform(get("/api/statements/{id}/reconciliation", id)
                .param("outcome", "AMOUNT_MISMATCH").header(HttpHeaders.AUTHORIZATION, atlasToken)))
                .at("/results/0/id").asLong();
        mvc.perform(post("/api/disputes").header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultId\":" + resultId + ",\"reason\":\"short\"}"))
                .andExpect(status().isCreated());

        // Discrepancies: |150 - 200| + |99 - 0| + |0 - 80| = 229.00 over three results.
        mvc.perform(get("/api/overview").header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(atlas.getId()))
                .andExpect(jsonPath("$.expectedCollections").value("580.00"))
                .andExpect(jsonPath("$.reportedCollections").value("450.00"))
                .andExpect(jsonPath("$.fees").value("60.00"))
                .andExpect(jsonPath("$.netRemitted").value("390.00"))
                .andExpect(jsonPath("$.balanceOwed").value("130.00"))
                .andExpect(jsonPath("$.openDisputes").value(1))
                .andExpect(jsonPath("$.openDisputedAmount").value("50.00"))
                .andExpect(jsonPath("$.discrepancies").value(3))
                .andExpect(jsonPath("$.discrepancyAmount").value("229.00"))
                .andExpect(jsonPath("$.couriers[0].code").value("speedex"))
                .andExpect(jsonPath("$.couriers[0].balanceOwed").value("130.00"));

        // Another tenant sees none of it; staff sees everything.
        mvc.perform(get("/api/overview").header(HttpHeaders.AUTHORIZATION, login("medina")))
                .andExpect(jsonPath("$.expectedCollections").value("0.00"))
                .andExpect(jsonPath("$.openDisputes").value(0))
                .andExpect(jsonPath("$.discrepancyAmount").value("0.00"));
        mvc.perform(get("/api/overview").header(HttpHeaders.AUTHORIZATION, login("ops")))
                .andExpect(jsonPath("$.merchantId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.openDisputes").value(1))
                .andExpect(jsonPath("$.discrepancyAmount").value("229.00"));
        mvc.perform(get("/api/overview").param("merchantId", medina.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, login("ops")))
                .andExpect(jsonPath("$.discrepancies").value(0));
    }

    @Test
    void reUploadingTheSameFileIsANoOp() throws Exception {
        long first = body(upload(atlasToken, CSV, null).andExpect(status().isAccepted())).at("/statement/id").asLong();
        awaitStatus(first, StatementStatus.RECONCILED);

        upload(atlasToken, CSV, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.statement.id").value(first));
        assertThat(statements.count()).isEqualTo(1);
    }

    @Test
    void theSameFileForAnotherMerchantIsIndependent() throws Exception {
        Merchant medina = merchant("medina");
        long first = body(upload(atlasToken, CSV, null).andExpect(status().isAccepted())).at("/statement/id").asLong();
        awaitStatus(first, StatementStatus.RECONCILED);

        long second = body(upload(login("ops"), CSV, medina.getId()).andExpect(status().isAccepted()))
                .at("/statement/id").asLong();
        awaitStatus(second, StatementStatus.RECONCILED);

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void invalidFilesAreRejectedSynchronouslyWithEveryRowError() throws Exception {
        upload(atlasToken, "tracking_number,amount_collected,fee\n,1.00,0\nSPX-1,abc,0\n", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.file.length()").value(2));
        assertThat(statements.count()).isZero();
    }

    @Test
    void staffCanReprocessAfterOrdersChange() throws Exception {
        long id = body(upload(atlasToken, CSV, null)).at("/statement/id").asLong();
        awaitStatus(id, StatementStatus.RECONCILED);
        order(atlas, speedex, "SPX-404", "99.00", AttemptOutcome.DELIVERED, OCT_10); // the "unknown" parcel appears

        mvc.perform(post("/api/statements/{id}/reprocess", id).header(HttpHeaders.AUTHORIZATION, login("ops")))
                .andExpect(status().isAccepted());
        awaitStatus(id, StatementStatus.RECONCILED);

        mvc.perform(get("/api/statements/{id}/reconciliation", id).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(jsonPath("$.summary.counts.UNKNOWN_TRACKING").value(0))
                .andExpect(jsonPath("$.summary.counts.MATCHED").value(3));
    }
}

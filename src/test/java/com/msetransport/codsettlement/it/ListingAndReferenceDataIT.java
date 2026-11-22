package com.msetransport.codsettlement.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.logistics.domain.AttemptOutcome;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.settlement.domain.StatementStatus;
import com.msetransport.codsettlement.support.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** Courier CRUD by staff, and filtered listings of statements and disputes. */
class ListingAndReferenceDataIT extends AbstractIntegrationTest {

    private Merchant atlas;
    private Courier speedex;
    private String ops;
    private String atlasToken;

    @BeforeEach
    void setUp() throws Exception {
        atlas = merchant("atlas");
        speedex = courier("speedex");
        merchantUser("atlas", atlas);
        staffUser("ops");
        ops = login("ops");
        atlasToken = login("atlas");
    }

    @Test
    void staffManagesCouriers() throws Exception {
        String created = mvc.perform(post("/api/couriers").header(HttpHeaders.AUTHORIZATION, ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Amana\",\"code\":\"amana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long amanaId = json.readTree(created).get("id").asLong();

        mvc.perform(post("/api/couriers").header(HttpHeaders.AUTHORIZATION, ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"AMANA\",\"code\":\"other\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/couriers/{id}", amanaId).header(HttpHeaders.AUTHORIZATION, ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Amana Colis\",\"code\":\"amana\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/couriers").param("active", "false").header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Amana Colis"));
        mvc.perform(get("/api/couriers").param("q", "SPEED").header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(jsonPath("$.content[0].code").value("speedex"));
        mvc.perform(get("/api/couriers/{id}", speedex.getId()).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isOk());

        order(atlas, speedex, "SPX-1", "10.00", null, null);
        mvc.perform(delete("/api/couriers/{id}", speedex.getId()).header(HttpHeaders.AUTHORIZATION, ops))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/couriers/{id}", amanaId).header(HttpHeaders.AUTHORIZATION, ops))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/couriers/{id}", amanaId).header(HttpHeaders.AUTHORIZATION, ops))
                .andExpect(status().isNotFound());
    }

    @Test
    void statementsAndDisputesCanBeFilteredAndResolved() throws Exception {
        order(atlas, speedex, "SPX-1", "100.00", AttemptOutcome.DELIVERED, Instant.parse("2026-10-05T10:00:00Z"));
        String csv = "tracking_number,amount_collected,fee\nSPX-1,90.00,10.00\n";
        String upload = mvc.perform(multipart("/api/statements")
                        .file(new MockMultipartFile("file", "oct.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .param("courierId", speedex.getId().toString())
                        .param("periodStart", "2026-10-01").param("periodEnd", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        long statementId = json.readTree(upload).at("/statement/id").asLong();
        awaitStatus(statementId, StatementStatus.RECONCILED);

        mvc.perform(get("/api/statements").header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .param("courierId", speedex.getId().toString()).param("status", "RECONCILED")
                        .param("periodFrom", "2026-10-01").param("periodTo", "2026-10-31")
                        .param("sort", "periodStart,asc"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lineCount").value(1));
        mvc.perform(get("/api/statements").header(HttpHeaders.AUTHORIZATION, ops)
                        .param("merchantId", atlas.getId().toString()).param("status", "FAILED"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/statements/{id}", statementId).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(jsonPath("$.status").value("RECONCILED"));

        String report = mvc.perform(get("/api/statements/{id}/reconciliation", statementId)
                        .header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andReturn().getResponse().getContentAsString();
        long resultId = json.readTree(report).at("/results/0/id").asLong();
        String dispute = mvc.perform(post("/api/disputes").header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultId\":" + resultId + ",\"reason\":\"short\",\"claimedAmount\":\"10.00\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long disputeId = json.readTree(dispute).get("id").asLong();

        mvc.perform(get("/api/disputes").header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .param("status", "OPEN").param("statementId", String.valueOf(statementId))
                        .param("courierId", speedex.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].trackingNumber").value("SPX-1"));
        mvc.perform(get("/api/statements/{id}/reconciliation", statementId).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(jsonPath("$.results[0].hasDispute").value(true));

        mvc.perform(post("/api/disputes/{id}/resolve", disputeId).header(HttpHeaders.AUTHORIZATION, ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RESOLVED\",\"resolutionNote\":\"Courier pays 10 MAD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(get("/api/disputes/{id}", disputeId).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(jsonPath("$.resolutionNote").value("Courier pays 10 MAD"));
        mvc.perform(get("/api/disputes").header(HttpHeaders.AUTHORIZATION, ops)
                        .param("merchantId", atlas.getId().toString()).param("status", "OPEN"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}

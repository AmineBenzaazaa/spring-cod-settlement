package com.benzaazaa.codsettlement.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benzaazaa.codsettlement.demo.DemoDataSeeder;
import com.benzaazaa.codsettlement.settlement.domain.StatementStatus;
import com.benzaazaa.codsettlement.support.AbstractIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** The demo profile and the committed fixture CSV stay in sync: this is the flow the README shows. */
@ActiveProfiles({"test", "demo"})
@TestPropertySource(properties = "app.demo.sample-csv-path=target/regenerated_sample_remittance.csv")
class DemoSeederIT extends AbstractIntegrationTest {

    @Autowired DemoDataSeeder seeder;

    @Test
    void seedIsIdempotentAndTheSampleCsvReconcilesWithOneOfEachDiscrepancy() throws Exception {
        seeder.run(); // tables were truncated before this test
        seeder.run();
        assertThat(merchants.count()).isEqualTo(2);
        assertThat(users.count()).isEqualTo(3);
        assertThat(orders.count()).isEqualTo(2L * 30);
        // The generator is seeded, so the committed fixture must equal a fresh regeneration.
        assertThat(Files.readString(Path.of("target/regenerated_sample_remittance.csv")))
                .isEqualTo(Files.readString(Path.of("fixtures/sample_remittance.csv")));

        String token = "Bearer " + json.readTree(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"atlas\",\"password\":\"" + DemoDataSeeder.DEMO_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
        long courierId = couriers.findByCode("speedex").orElseThrow().getId();

        String body = mvc.perform(multipart("/api/statements")
                        .file(new MockMultipartFile("file", "sample_remittance.csv", "text/csv",
                                Files.readAllBytes(Path.of("fixtures/sample_remittance.csv"))))
                        .param("courierId", String.valueOf(courierId))
                        .param("periodStart", "2026-10-01").param("periodEnd", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).at("/statement/id").asLong();
        awaitStatus(id, StatementStatus.RECONCILED);

        mvc.perform(get("/api/statements/{id}/reconciliation", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.summary.counts.MATCHED").value(11))
                .andExpect(jsonPath("$.summary.counts.AMOUNT_MISMATCH").value(1))
                .andExpect(jsonPath("$.summary.counts.UNKNOWN_TRACKING").value(1))
                .andExpect(jsonPath("$.summary.counts.MISSING_FROM_STATEMENT").value(1));
    }
}

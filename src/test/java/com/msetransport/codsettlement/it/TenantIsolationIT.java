package com.benzaazaa.codsettlement.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benzaazaa.codsettlement.account.domain.Merchant;
import com.benzaazaa.codsettlement.logistics.domain.AttemptOutcome;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.domain.Order;
import com.benzaazaa.codsettlement.support.AbstractIntegrationTest;
import com.benzaazaa.codsettlement.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** A merchant must never see or touch another merchant's data; foreign IDs behave like missing ones. */
class TenantIsolationIT extends AbstractIntegrationTest {

    private Merchant atlas;
    private Merchant medina;
    private Courier speedex;
    private Order atlasOrder;
    private Order medinaOrder;
    private String medinaToken;

    @BeforeEach
    void setUp() throws Exception {
        atlas = merchant("atlas");
        medina = merchant("medina");
        speedex = courier("speedex");
        merchantUser("atlas", atlas);
        merchantUser("medina", medina);
        staffUser("ops");
        atlasOrder = order(atlas, speedex, "SPX-1", "100.00", null, null);
        medinaOrder = order(medina, speedex, "SPX-2", "200.00", AttemptOutcome.DELIVERED, TestData.NOW);
        medinaToken = login("medina");
    }

    @Test
    void listsOnlyContainTheCallersOwnRows() throws Exception {
        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, medinaToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].trackingNumber").value("SPX-2"));
        // Asking for another merchant explicitly does not widen the scope.
        mvc.perform(get("/api/orders").param("merchantId", atlas.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, medinaToken))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void foreignObjectsAreNotFoundForReadsAndWrites() throws Exception {
        long id = atlasOrder.getId();
        mvc.perform(get("/api/orders/{id}", id).header(HttpHeaders.AUTHORIZATION, medinaToken))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/orders/{id}", id).header(HttpHeaders.AUTHORIZATION, medinaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courierId\":" + speedex.getId() + ",\"trackingNumber\":\"X\",\"customerName\":\"X\",\"amountDue\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/{id}/attempts", id).header(HttpHeaders.AUTHORIZATION, medinaToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"DELIVERED\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/orders/{id}", id).header(HttpHeaders.AUTHORIZATION, medinaToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void merchantFromTheBodyIsIgnoredOnCreate() throws Exception {
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, medinaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantId\":" + atlas.getId() + ",\"courierId\":" + speedex.getId()
                                + ",\"trackingNumber\":\"spx-9\",\"customerName\":\"Omar\",\"amountDue\":\"49.90\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(medina.getId()))
                .andExpect(jsonPath("$.trackingNumber").value("SPX-9"));
    }

    @Test
    void balanceIsScopedToTheCallersMerchant() throws Exception {
        mvc.perform(get("/api/couriers/{id}/balance", speedex.getId())
                        .param("merchantId", atlas.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, medinaToken))
                .andExpect(jsonPath("$.merchantId").value(medina.getId()))
                .andExpect(jsonPath("$.expectedCollections").value("200.00"));
    }

    @Test
    void staffSeesEveryMerchantAndMustChooseOneOnCreate() throws Exception {
        String ops = login("ops");
        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, ops))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/orders/{id}", medinaOrder.getId()).header(HttpHeaders.AUTHORIZATION, ops))
                .andExpect(status().isOk());
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courierId\":" + speedex.getId() + ",\"trackingNumber\":\"X1\",\"customerName\":\"X\",\"amountDue\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.merchantId[0]").value("Staff must specify the merchant."));
    }

    @Test
    void lifecycleRulesHoldOverHttp() throws Exception {
        String atlasToken = login("atlas");
        long id = atlasOrder.getId();
        mvc.perform(post("/api/orders/{id}/attempts", id).header(HttpHeaders.AUTHORIZATION, atlasToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"DELIVERED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.attempts.length()").value(1));
        mvc.perform(post("/api/orders/{id}/cancel", id).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/api/orders/{id}", id).header(HttpHeaders.AUTHORIZATION, atlasToken))
                .andExpect(status().isUnprocessableEntity());
    }
}

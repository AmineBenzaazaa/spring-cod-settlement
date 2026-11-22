package com.msetransport.codsettlement.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msetransport.codsettlement.account.domain.AppUser;
import com.msetransport.codsettlement.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthAndPlatformIT extends AbstractIntegrationTest {

    @Test
    void loginRefreshAndMeAgainstTheDatabase() throws Exception {
        merchantUser("atlas", merchant("atlas"));

        JsonNode tokens = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"atlas\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()));
        JsonNode refreshed = body(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isOk()));

        mvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("atlas"))
                .andExpect(jsonPath("$.role").value("MERCHANT"));
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        merchantUser("atlas", merchant("atlas"));

        for (String body : new String[] {
                "{\"username\":\"atlas\",\"password\":\"wrong\"}",
                "{\"username\":\"nobody\",\"password\":\"wrong\"}"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid username or password."));
        }
    }

    @Test
    void disabledUserCannotLogInOrRefresh() throws Exception {
        AppUser user = merchantUser("atlas", merchant("atlas"));
        JsonNode tokens = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"atlas\",\"password\":\"" + PASSWORD + "\"}")));
        user.disable();
        users.save(user);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"atlas\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthAndApiDocsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());

        JsonNode docs = body(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()));
        assertThat(docs.at("/paths").has("/api/statements/{id}/reconciliation")).isTrue();
        assertThat(docs.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(docs.at("/components/schemas/OrderResponse/properties/amountDue/type").asText()).isEqualTo("string");
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        String token = login(staffUser("ops").getUsername());
        mvc.perform(get("/actuator/env").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNotFound());
    }
}

package com.msetransport.codsettlement.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msetransport.codsettlement.account.domain.AppUser;
import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.account.repository.AppUserRepository;
import com.msetransport.codsettlement.account.repository.MerchantRepository;
import com.msetransport.codsettlement.logistics.domain.AttemptOutcome;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.logistics.domain.Order;
import com.msetransport.codsettlement.logistics.repository.CourierRepository;
import com.msetransport.codsettlement.logistics.repository.OrderRepository;
import com.msetransport.codsettlement.settlement.domain.StatementStatus;
import com.msetransport.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Full application (real security, real async executor, real PostgreSQL), driven over HTTP
 * through MockMvc. Each test starts from empty tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest extends PostgresTestBase {

    protected static final String PASSWORD = "integration-pass";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected MerchantRepository merchants;
    @Autowired protected CourierRepository couriers;
    @Autowired protected AppUserRepository users;
    @Autowired protected OrderRepository orders;
    @Autowired protected RemittanceStatementRepository statements;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected Merchant merchant(String slug) {
        return merchants.save(new Merchant(slug, slug, null));
    }

    protected Courier courier(String code) {
        return couriers.save(new Courier(code, code, null, true));
    }

    protected AppUser merchantUser(String username, Merchant merchant) {
        return users.save(AppUser.merchantUser(username, passwordEncoder.encode(PASSWORD), merchant));
    }

    protected AppUser staffUser(String username) {
        return users.save(AppUser.staff(username, passwordEncoder.encode(PASSWORD)));
    }

    protected Order order(Merchant merchant, Courier courier, String tracking, String amount, AttemptOutcome outcome,
                          Instant at) {
        Order order = new Order(merchant, courier, tracking, "Customer " + tracking, null, "Casablanca",
                new BigDecimal(amount));
        if (outcome != null) {
            order.recordAttempt(outcome, at, null);
        }
        return orders.save(order);
    }

    /** Logs in through the real endpoint and returns an {@code Authorization} header value. */
    protected String login(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Statements are processed on a background thread after commit: poll until done. */
    protected void awaitStatus(long statementId, StatementStatus expected) {
        Awaitility.await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(100))
                .until(() -> statements.findById(statementId).map(s -> s.getStatus() == expected).orElse(false));
    }
}

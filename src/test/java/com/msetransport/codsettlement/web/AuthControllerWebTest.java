package com.msetransport.codsettlement.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msetransport.codsettlement.auth.service.AuthService;
import com.msetransport.codsettlement.auth.web.AuthController;
import com.msetransport.codsettlement.auth.web.dto.TokenResponse;
import com.msetransport.codsettlement.support.TestData;
import com.msetransport.codsettlement.support.WebSliceTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class AuthControllerWebTest extends WebSliceTest {

    @MockitoBean
    AuthService authService;

    @Test
    void loginIsPublicAndReturnsTokens() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponse.bearer("access", "refresh", 900));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"atlas\",\"password\":\"demo-pass-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void loginValidationErrorsAreProblemDetails() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void wrongCredentialsGive401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid username or password."));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"atlas\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void malformedJsonIsA400ProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void meWithoutTokenIs401WithBearerChallenge() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.detail").value("Authentication is required."));
    }

    @Test
    void meReturnsThePrincipalFromTheToken() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, merchantToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MERCHANT"))
                .andExpect(jsonPath("$.merchantId").value(1));
    }

    @Test
    void garbageOrRefreshTokensAreRejectedByTheFilter() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Token is invalid"));

        String refresh = jwtService.issueRefreshToken(TestData.merchantUser(1));
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong token type"));
    }
}

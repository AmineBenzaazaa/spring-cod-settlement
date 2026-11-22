package com.benzaazaa.codsettlement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 401/403 raised inside the security filter chain never reach {@code @RestControllerAdvice}
 * (they happen before the DispatcherServlet), so we write the same ProblemDetail JSON here.
 */
@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String TYPE_PREFIX = "https://cod-settlement.benzaazaa.com/problems/";

    private final ObjectMapper objectMapper;

    public ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String detail = ex instanceof InvalidTokenException ? ex.getMessage() : "Authentication is required.";
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, request, HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", detail);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "forbidden", "Forbidden",
                "You are not allowed to perform this action.");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
                       String type, String title, String detail) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_PREFIX + type));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), pd);
    }
}

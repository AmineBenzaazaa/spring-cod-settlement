package com.benzaazaa.codsettlement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>}, verifies it and puts an authenticated
 * {@link AuthenticatedUser} into the SecurityContext. Runs once per request, before
 * Spring Security's authorization checks.
 *
 * <p>No header: the request continues anonymously and the authorization rules decide
 * (public endpoints pass, protected ones get 401). Bad header: 401 immediately, with the
 * reason (expired / invalid) in a ProblemDetail body.
 *
 * <p>Not a {@code @Component} on purpose: Spring Boot would also register any Filter bean
 * in the servlet container, so it would run twice. It is created in {@link SecurityConfig}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, AuthenticationEntryPoint entryPoint) {
        this.jwtService = jwtService;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            AuthenticatedUser user = jwtService.parseAccessToken(header.substring(BEARER_PREFIX.length()).trim());
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (InvalidTokenException ex) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, ex);
            return;
        }
        chain.doFilter(request, response);
    }
}

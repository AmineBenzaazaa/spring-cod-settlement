package com.msetransport.codsettlement.common.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 7807 {@code application/problem+json} responses.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} gives ProblemDetail bodies for all the
 * standard Spring MVC exceptions (bad JSON, wrong method, type mismatch, 413 upload too
 * large...). The handlers below cover our own exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://settle.msetransport.com/problems/";

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        String detail = ex.getMessages().size() == 1 ? ex.getMessages().getFirst() : "The request contains invalid data.";
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", detail);
        pd.setProperty("errors", Map.of(ex.getField(), ex.getMessages()));
        return pd;
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "business-rule", "Business rule violated", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "concurrent-update", "Concurrent update",
                "The resource was modified by someone else. Reload it and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // The database is the last line of defence (unique / FK / check constraints).
        // Do not echo the SQL error: it can reveal schema details.
        log.warn("Constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "constraint-violation", "Conflict",
                "The request conflicts with existing data.");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleBadSort(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request",
                "Unknown sort property '" + ex.getPropertyName() + "'.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        // Thrown by @PreAuthorize inside the MVC layer; without this handler the catch-all
        // below would turn it into a 500.
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden",
                "You are not allowed to perform this action.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error",
                "Something went wrong on our side.");
    }

    /** Bean Validation failures: report every field error at once, grouped by field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fe.getField(), k -> new java.util.ArrayList<>()).add(fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                errors.computeIfAbsent(ge.getObjectName(), k -> new java.util.ArrayList<>()).add(ge.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
                "One or more fields are invalid.");
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd && pd.getType().equals(URI.create("about:blank"))) {
            pd.setType(URI.create(TYPE_PREFIX + "http-" + statusCode.value()));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_PREFIX + type));
        pd.setTitle(title);
        return pd;
    }
}

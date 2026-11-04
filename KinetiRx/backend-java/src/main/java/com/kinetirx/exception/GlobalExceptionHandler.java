package com.kinetirx.exception;

import com.kinetirx.common.ApiErrorBody;
import com.kinetirx.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Turns every failure into the standard envelope, so no stack trace or internal
 * detail reaches the client. Internal errors are logged server-side only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorBody> api(ApiException ex, HttpServletRequest req) {
        return respond(req, ex.status(), ex.type(), ex.getMessage(), ex.errors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorBody> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiErrorBody.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiErrorBody.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return respond(req, 400, "validation_error", "Invalid request payload", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorBody> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return respond(req, 400, "validation_error", "Invalid request payload", null);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiErrorBody> notFoundRoute(Exception ex, HttpServletRequest req) {
        return respond(req, 404, "not_found", "Route not found", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorBody> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return respond(req, 413, "payload_too_large", "Uploaded file is too large", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorBody> denied(AccessDeniedException ex, HttpServletRequest req) {
        return respond(req, 403, "forbidden", "You do not have permission to access this resource", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorBody> unexpected(Exception ex, HttpServletRequest req) {
        log.error("request_id={} unhandled error", req.getAttribute(RequestIdFilter.ATTRIBUTE), ex);
        return respond(req, 500, "internal_error", "Internal server error", null);
    }

    private ResponseEntity<ApiErrorBody> respond(HttpServletRequest req, int status, String type, String title,
                                                 List<ApiErrorBody.FieldError> errors) {
        Object rid = req.getAttribute(RequestIdFilter.ATTRIBUTE);
        return ResponseEntity.status(HttpStatus.valueOf(status))
                .body(new ApiErrorBody(type, title, status, errors, rid == null ? null : rid.toString()));
    }
}

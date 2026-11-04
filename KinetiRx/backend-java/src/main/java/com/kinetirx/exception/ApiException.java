package com.kinetirx.exception;

import com.kinetirx.common.ApiErrorBody;

import java.util.List;

/** A business or request error with an HTTP status and an envelope type. */
public class ApiException extends RuntimeException {

    private final int status;
    private final String type;
    private final List<ApiErrorBody.FieldError> errors;

    public ApiException(int status, String type, String message) {
        this(status, type, message, null);
    }

    public ApiException(int status, String type, String message, List<ApiErrorBody.FieldError> errors) {
        super(message);
        this.status = status;
        this.type = type;
        this.errors = errors;
    }

    public int status() { return status; }
    public String type() { return type; }
    public List<ApiErrorBody.FieldError> errors() { return errors; }

    public static ApiException badRequest(String title) {
        return new ApiException(400, "validation_error", title);
    }

    public static ApiException unauthorized(String title) {
        return new ApiException(401, "unauthorized", title);
    }

    public static ApiException forbidden(String title) {
        return new ApiException(403, "forbidden", title);
    }

    public static ApiException notFound(String title) {
        return new ApiException(404, "not_found", title);
    }

    public static ApiException conflict(String title) {
        return new ApiException(409, "conflict", title);
    }

    public static ApiException internal(String title) {
        return new ApiException(500, "internal_error", title);
    }
}

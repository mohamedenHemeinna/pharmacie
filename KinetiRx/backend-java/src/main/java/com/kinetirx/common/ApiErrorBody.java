package com.kinetirx.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The error envelope every non-2xx response uses. Field names match the Go
 * httpx.ErrorBody exactly, since the frontend's ApiError parses them.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorBody(String type, String title, int status, List<FieldError> errors, String request_id) {

    public record FieldError(String field, String message) {}
}

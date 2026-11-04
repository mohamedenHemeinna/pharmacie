package com.kinetirx.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Writes the standard error envelope directly, for errors raised outside controllers (filters, security). */
@Component
public class ApiErrorWriter {

    private final ObjectMapper mapper;

    public ApiErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      int status, String type, String title) throws IOException {
        write(request, response, status, type, title, null);
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      int status, String type, String title, List<ApiErrorBody.FieldError> errors) throws IOException {
        Object rid = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        ApiErrorBody body = new ApiErrorBody(type, title, status, errors,
                rid == null ? null : rid.toString());
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(mapper.writeValueAsBytes(body));
    }
}

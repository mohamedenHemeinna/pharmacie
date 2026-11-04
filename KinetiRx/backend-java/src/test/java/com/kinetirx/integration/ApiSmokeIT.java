package com.kinetirx.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiSmokeIT extends IntegrationTestBase {

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void protectedRouteRequiresToken() throws Exception {
        mvc.perform(get("/api/medicines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("unauthorized"))
                .andExpect(jsonPath("$.title").value("Missing or malformed Authorization header"));
    }

    @Test
    void errorEnvelopeCarriesRequestId() throws Exception {
        MvcResult r = mvc.perform(get("/api/medicines").header("X-Request-Id", "req-123"))
                .andExpect(status().isUnauthorized()).andReturn();
        org.junit.jupiter.api.Assertions.assertEquals("req-123", r.getResponse().getHeader("X-Request-Id"));
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getContentAsString().contains("\"request_id\":\"req-123\""));
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'"));
    }

    @Test
    void unknownRouteWithoutTokenIsUnauthorizedNotFound() throws Exception {
        // Deliberate difference from Go: the Java security chain runs before routing, so an
        // anonymous request to an unknown path gets 401 (Go answered 404). Authenticated requests get 404.
        mvc.perform(get("/api/no-such-route")).andExpect(status().isUnauthorized());
    }
}

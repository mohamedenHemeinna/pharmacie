package com.kinetirx.common;

import com.kinetirx.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void blocksAfterLimitWithin429() {
        RateLimiter limiter = new RateLimiter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        for (int i = 0; i < 3; i++) limiter.check("login", req, 3, Duration.ofMinutes(1));
        ApiException ex = assertThrows(ApiException.class, () -> limiter.check("login", req, 3, Duration.ofMinutes(1)));
        assertEquals(429, ex.status());
        assertEquals("rate_limited", ex.type());
    }

    @Test
    void limitIsPerRouteAndPerClient() {
        RateLimiter limiter = new RateLimiter();
        MockHttpServletRequest a = new MockHttpServletRequest();
        a.setRemoteAddr("10.0.0.1");
        MockHttpServletRequest b = new MockHttpServletRequest();
        b.setRemoteAddr("10.0.0.2");
        limiter.check("login", a, 1, Duration.ofMinutes(1));
        assertDoesNotThrow(() -> limiter.check("login", b, 1, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> limiter.check("setup", a, 1, Duration.ofMinutes(1)));
    }

    @Test
    void windowResets() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.9");
        limiter.check("r", req, 1, Duration.ofMillis(100));
        assertThrows(ApiException.class, () -> limiter.check("r", req, 1, Duration.ofMillis(100)));
        Thread.sleep(150);
        assertDoesNotThrow(() -> limiter.check("r", req, 1, Duration.ofMillis(100)));
    }

    @Test
    void clientIpPrefersForwardedHeaderLikeGin() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("172.18.0.2");
        req.addHeader("X-Forwarded-For", "203.0.113.7, 172.18.0.2");
        assertEquals("203.0.113.7", ClientIp.of(req));
        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("198.51.100.4");
        assertEquals("198.51.100.4", ClientIp.of(direct));
    }
}

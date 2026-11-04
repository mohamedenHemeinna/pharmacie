package com.kinetirx.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * One log line per request, in the same shape as the Go StructuredLogger. Bodies and headers are
 * never logged, so passwords and tokens cannot leak into logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            Duration latency = Duration.ofNanos(System.nanoTime() - start);
            log.info("request_id={} method={} path={} status={} latency={}ms client_ip={}",
                    request.getAttribute(RequestIdFilter.ATTRIBUTE), request.getMethod(),
                    request.getRequestURI(), response.getStatus(), latency.toMillis(), ClientIp.of(request));
        }
    }
}

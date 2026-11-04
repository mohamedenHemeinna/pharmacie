package com.kinetirx.security;

import com.kinetirx.common.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads "Authorization: Bearer <jwt>" and populates the security context.
 *
 * Authorities mirror the Go RequirePermission rule: ROLE_ADMIN for role "admin", and
 * PERM_<tab> for each granted tab. Admins pass every tab check, as they did in Go.
 *
 * Routes that accept a token in the query string (/api/events/stream) are handled by
 * their own controller, so this filter is skipped for them.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final ApiErrorWriter errors;

    public JwtAuthenticationFilter(JwtService jwt, ApiErrorWriter errors) {
        this.jwt = jwt;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/events/stream");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);   // unauthenticated: the entry point decides for protected routes
            return;
        }
        var claims = jwt.verify(header.substring("Bearer ".length()));
        if (claims.isEmpty()) {
            errors.write(request, response, 401, "unauthorized", "Invalid or expired access token");
            return;
        }
        JwtService.Claims c = claims.get();
        EmployeePrincipal principal = new EmployeePrincipal(c.employeeId(), c.name(), c.role(), c.permissions());
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (principal.isAdmin()) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (String p : c.permissions()) authorities.add(new SimpleGrantedAuthority("PERM_" + p));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
        chain.doFilter(request, response);
    }

}

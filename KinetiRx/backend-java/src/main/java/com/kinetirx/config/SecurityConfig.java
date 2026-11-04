package com.kinetirx.config;

import com.kinetirx.common.ApiErrorWriter;
import com.kinetirx.security.JwtAuthenticationFilter;
import com.kinetirx.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Route authorization. Each rule below mirrors one RegisterRoutes group in the Go router.
 * Order matters: the first matching rule wins, so specific rules come before generic ones.
 *
 * Admins pass every tab rule (ROLE_ADMIN is granted alongside every PERM_*), as in Go.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ROLE_ADMIN";

    @Bean
    PasswordEncoder passwordEncoder() {
        // Cost 12, same as the Go service. $2a$ hashes written by Go verify unchanged.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, ApiErrorWriter errors) throws Exception {
        http
            .csrf(c -> c.disable())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    String header = req.getHeader("Authorization");
                    boolean missing = header == null || !header.startsWith("Bearer ");
                    errors.write(req, res, 401, "unauthorized",
                            missing ? "Missing or malformed Authorization header"
                                    : "Invalid or expired access token");
                })
                .accessDeniedHandler((req, res, ex) ->
                        errors.write(req, res, 403, "forbidden", "You do not have permission to access this resource")))
            .addFilterBefore(new JwtAuthenticationFilter(jwt, errors), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                // Public
                .requestMatchers("/api/health", "/api/auth/login", "/api/auth/setup-status", "/api/auth/setup",
                        "/api/events/stream").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Any authenticated employee
                .requestMatchers("/api/auth/me", "/api/auth/password").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/invoice-config").authenticated()
                .requestMatchers("/api/ai/ask").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/security/master-pin").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/security/master-pin/verify").authenticated()

                // Admin only
                .requestMatchers("/api/ai-config", "/api/ai-config/**").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/security/master-pin").hasAuthority(ADMIN)
                .requestMatchers("/api/backup", "/api/backup/**").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/employees").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasAuthority(ADMIN)

                // Tab permissions (admins also match through ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/employees", "/api/employees/**").hasAnyAuthority(ADMIN, "PERM_employee-mgmt")
                .requestMatchers(HttpMethod.PUT, "/api/invoice-config").hasAnyAuthority(ADMIN, "PERM_invoice-settings")
                .requestMatchers(HttpMethod.POST, "/api/sales").hasAnyAuthority(ADMIN, "PERM_pos")
                .requestMatchers(HttpMethod.GET, "/api/sales", "/api/sales/**").hasAnyAuthority(ADMIN, "PERM_daily-sales")
                .requestMatchers("/api/medicines", "/api/medicines/**", "/api/medicine-groups", "/api/medicine-groups/**",
                        "/api/distributors", "/api/distributors/**").hasAnyAuthority(ADMIN, "PERM_inventory")
                .requestMatchers("/api/patients", "/api/patients/**").hasAnyAuthority(ADMIN, "PERM_patients")
                .requestMatchers("/api/due-khata", "/api/due-khata/**").hasAnyAuthority(ADMIN, "PERM_due-khata")
                .requestMatchers("/api/expenses", "/api/expenses/**").hasAnyAuthority(ADMIN, "PERM_expenses")
                .requestMatchers("/api/needed-meds", "/api/needed-meds/**").hasAnyAuthority(ADMIN, "PERM_medicine-orders")
                .requestMatchers("/api/opd-visits", "/api/opd-visits/**").hasAnyAuthority(ADMIN, "PERM_opd")
                .requestMatchers("/api/marketing-campaigns", "/api/marketing-campaigns/**",
                        "/api/worksheet-tasks", "/api/worksheet-tasks/**").hasAnyAuthority(ADMIN, "PERM_business-dev")
                .requestMatchers("/api/daily-register", "/api/daily-register/**").hasAnyAuthority(ADMIN, "PERM_daily-calc")
                .requestMatchers("/api/ocr/parse-bill").hasAnyAuthority(ADMIN, "PERM_inward-ocr")

                .anyRequest().authenticated());
        return http.build();
    }
}

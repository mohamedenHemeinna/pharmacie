package com.kinetirx.config;

import com.kinetirx.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * First-boot admin seeding. Runs only when the employees table is empty, same as the Go startup.
 * Without KINETIRX_ADMIN_PASSWORD, the operator creates the admin in the UI (POST /api/auth/setup).
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService auth;
    private final KinetiRxProperties props;

    public AdminBootstrap(AuthService auth, KinetiRxProperties props) {
        this.auth = auth;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = props.adminBootstrapPassword();
        if (password == null || password.isEmpty()) {
            log.info("startup: no KINETIRX_ADMIN_PASSWORD set — the first admin account is created from the app's setup screen if no employees exist");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("KINETIRX_ADMIN_PASSWORD must be at least 8 characters long");
        }
        if (auth.seedAdminIfEmpty(password)) {
            log.info("startup: seeded initial admin employee 'Master Admin' (id=EMP-ADMIN-1)");
        }
    }
}

package com.kinetirx.service;

import com.kinetirx.dto.AuthDtos.CurrentUser;
import com.kinetirx.dto.AuthDtos.LoginResponse;
import com.kinetirx.dto.AuthDtos.SetupStatus;
import com.kinetirx.entity.Employee;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.EmployeeRepository;
import com.kinetirx.security.JwtService;
import com.kinetirx.common.TimeFormat;
import com.kinetirx.validation.Tabs;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Authentication rules. Mirrors handlers/auth.go and handlers/setup.go. */
@Service
public class AuthService {

    static final String ADMIN_ID = "EMP-ADMIN-1";
    static final String ADMIN_DESIG = "Director & Admin";

    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthService(EmployeeRepository employees, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String identifier, String password) {
        if (identifier == null || identifier.isBlank() || password == null || password.isEmpty()) {
            throw ApiException.badRequest("identifier and password are required");
        }
        // Same 401 for an unknown identifier and a wrong password: no account enumeration.
        Employee e = employees.findByIdOrName(identifier)
                .orElseThrow(() -> new ApiException(401, "unauthorized", "Invalid credentials"));
        if (!verify(e.getPasswordHash(), password)) {
            throw new ApiException(401, "unauthorized", "Invalid credentials");
        }
        return issue(e, true);
    }

    @Transactional(readOnly = true)
    public CurrentUser me(String employeeId) {
        Employee e = employees.findById(employeeId)
                .orElseThrow(() -> new ApiException(401, "unauthorized", "Employee account no longer exists"));
        return toUser(e);
    }

    @Transactional
    public void changePassword(String employeeId, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw ApiException.badRequest("currentPassword and newPassword are required");
        }
        if (newPassword.length() < 8) {
            throw ApiException.badRequest("New password must be at least 8 characters long");
        }
        Employee e = employees.findById(employeeId)
                .orElseThrow(() -> new ApiException(401, "unauthorized", "Employee account no longer exists"));
        if (!verify(e.getPasswordHash(), currentPassword)) {
            throw new ApiException(401, "unauthorized", "Current password is incorrect");
        }
        e.setPasswordHash(passwordEncoder.encode(newPassword));
        e.setMustChangePassword(false);
        e.setUpdatedAt(Instant.now());
        employees.save(e);
    }

    @Transactional(readOnly = true)
    public SetupStatus setupStatus() {
        return new SetupStatus(employees.count() == 0);
    }

    @Transactional
    public LoginResponse setup(String name, String password) {
        if (name == null || name.isBlank() || password == null || password.isEmpty()) {
            throw ApiException.badRequest("name and password are required");
        }
        if (password.length() < 8) {
            throw ApiException.badRequest("password must be at least 8 characters long");
        }
        if (employees.count() > 0) {
            throw ApiException.conflict("Setup has already been completed — log in instead");
        }
        Employee admin = new Employee();
        admin.setId(ADMIN_ID);
        admin.setName(name);
        admin.setDesig(ADMIN_DESIG);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole("admin");
        admin.setPermissions(Tabs.ALL.toArray(String[]::new));
        try {
            employees.saveAndFlush(admin);
        } catch (DataIntegrityViolationException ex) {
            // Two setup calls raced past the count check; the primary key rejects the loser.
            throw ApiException.internal("Failed to create admin account");
        }
        return issue(admin, false);
    }

    /** Creates the first admin from KINETIRX_ADMIN_PASSWORD when the table is empty. */
    @Transactional
    public boolean seedAdminIfEmpty(String plaintextPassword) {
        if (employees.count() > 0) return false;
        Employee admin = new Employee();
        admin.setId(ADMIN_ID);
        admin.setName("Master Admin");
        admin.setDesig(ADMIN_DESIG);
        admin.setPasswordHash(passwordEncoder.encode(plaintextPassword));
        admin.setRole("admin");
        admin.setPermissions(Tabs.ALL.toArray(String[]::new));
        employees.save(admin);
        return true;
    }

    private LoginResponse issue(Employee e, boolean includeMustChange) {
        List<String> perms = Arrays.asList(e.getPermissions());
        JwtService.IssuedToken token = jwt.issue(e.getId(), e.getName(), e.getRole(), perms);
        return new LoginResponse(
                token.token(),
                TimeFormat.rfc3339Seconds(token.expiresAt()),
                new CurrentUser(e.getId(), e.getName(), e.getDesig(), e.getRole(), perms,
                        includeMustChange && e.isMustChangePassword()));
    }

    private CurrentUser toUser(Employee e) {
        return new CurrentUser(e.getId(), e.getName(), e.getDesig(), e.getRole(),
                Arrays.asList(e.getPermissions()), e.isMustChangePassword());
    }

    private boolean verify(String hash, String plaintext) {
        return hash != null && !hash.isEmpty() && passwordEncoder.matches(plaintext, hash);
    }
}

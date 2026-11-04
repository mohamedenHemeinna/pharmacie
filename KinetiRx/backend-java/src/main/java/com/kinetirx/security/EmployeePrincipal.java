package com.kinetirx.security;

import java.util.List;

/** The authenticated employee, taken from a verified JWT. */
public record EmployeePrincipal(String employeeId, String name, String role, List<String> permissions) {

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}

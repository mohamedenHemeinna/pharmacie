package com.kinetirx.service;

import com.kinetirx.common.EventBus;
import com.kinetirx.common.JsonCodec;
import com.kinetirx.dto.StaffDtos;
import com.kinetirx.entity.Employee;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.DailyRegisterRepository;
import com.kinetirx.repository.EmployeeRepository;
import com.kinetirx.repository.InvoiceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The last-admin and self-deletion guards that protect against locking everyone out. */
class StaffGuardsTest {

    private EmployeeRepository employees;
    private StaffService service;

    @BeforeEach
    void setUp() {
        employees = mock(EmployeeRepository.class);
        service = new StaffService(employees, mock(DailyRegisterRepository.class), mock(InvoiceConfigRepository.class),
                new BCryptPasswordEncoder(4), new JsonCodec(new com.fasterxml.jackson.databind.ObjectMapper()),
                new EventBus());
    }

    private Employee employee(String id, String role) {
        Employee e = new Employee();
        e.setId(id);
        e.setName(id);
        e.setRole(role);
        e.setPermissions(new String[0]);
        return e;
    }

    private static StaffDtos.EmployeeInput input(String name, String role, List<String> perms) {
        return new StaffDtos.EmployeeInput(null, name, "", null, null, role, null, perms);
    }

    @Test
    void cannotDemoteTheOnlyAdmin() {
        when(employees.findById("EMP-ADMIN-1")).thenReturn(Optional.of(employee("EMP-ADMIN-1", "admin")));
        when(employees.countOtherAdmins("EMP-ADMIN-1")).thenReturn(0L);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateEmployee("EMP-ADMIN-1", input("Admin", "staff", List.of())));
        assertEquals(409, ex.status());
    }

    @Test
    void demotionAllowedWhenAnotherAdminExists() {
        when(employees.findById("EMP-ADMIN-1")).thenReturn(Optional.of(employee("EMP-ADMIN-1", "admin")));
        when(employees.countOtherAdmins("EMP-ADMIN-1")).thenReturn(1L);
        when(employees.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("staff", service.updateEmployee("EMP-ADMIN-1", input("Admin", "staff", List.of())).role());
    }

    @Test
    void cannotDeleteYourself() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.deleteEmployee("EMP-ADMIN-1", "EMP-ADMIN-1"));
        assertEquals(409, ex.status());
        verify(employees, never()).deleteById(any());
    }

    @Test
    void cannotDeleteLastAdmin() {
        when(employees.findById("EMP-2")).thenReturn(Optional.of(employee("EMP-2", "admin")));
        when(employees.countOtherAdmins("EMP-2")).thenReturn(0L);
        assertThrows(ApiException.class, () -> service.deleteEmployee("EMP-2", "EMP-ADMIN-1"));
        verify(employees, never()).deleteById(any());
    }

    @Test
    void rejectsUnknownPermissionNames() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createEmployee(new StaffDtos.EmployeeInput(null, "Ann", "", "longenough1", null,
                        "staff", null, List.of("does-not-exist"))));
        assertEquals(400, ex.status());
        assertTrue(ex.getMessage().contains("unknown permission"));
    }

    @Test
    void passwordMustBeAtLeastEightCharacters() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createEmployee(new StaffDtos.EmployeeInput(null, "Ann", "", "short", null,
                        "staff", null, List.of())));
        assertEquals(400, ex.status());
    }
}

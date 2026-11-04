package com.kinetirx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kinetirx.common.EventBus;
import com.kinetirx.common.JsonCodec;
import com.kinetirx.dto.StaffDtos;
import com.kinetirx.entity.DailyRegister;
import com.kinetirx.entity.Employee;
import com.kinetirx.entity.InvoiceConfig;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.DailyRegisterRepository;
import com.kinetirx.repository.EmployeeRepository;
import com.kinetirx.repository.InvoiceConfigRepository;
import com.kinetirx.validation.Tabs;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Employees, daily register, and invoice config. Mirrors handlers/employees.go, daily_register.go, invoice_config.go. */
@Service
public class StaffService {

    /** Go's zero time, returned for a not-yet-configured invoice (kept so the wire value matches). */
    private static final Instant GO_ZERO_TIME = Instant.parse("0001-01-01T00:00:00Z");
    private static final TypeReference<Map<String, Integer>> DENOMINATIONS = new TypeReference<>() {};

    private final EmployeeRepository employees;
    private final DailyRegisterRepository registers;
    private final InvoiceConfigRepository invoices;
    private final PasswordEncoder encoder;
    private final JsonCodec json;
    private final EventBus events;

    public StaffService(EmployeeRepository employees, DailyRegisterRepository registers,
                        InvoiceConfigRepository invoices, PasswordEncoder encoder, JsonCodec json, EventBus events) {
        this.employees = employees;
        this.registers = registers;
        this.invoices = invoices;
        this.encoder = encoder;
        this.json = json;
        this.events = events;
    }

    // ---- employees -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<StaffDtos.Employee> listEmployees() {
        return employees.findAll(Sort.by("name").ascending()).stream().map(StaffService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StaffDtos.Employee getEmployee(String id) {
        return employees.findById(id).map(StaffService::toDto)
                .orElseThrow(() -> ApiException.notFound("Employee not found"));
    }

    @Transactional
    public StaffDtos.Employee createEmployee(StaffDtos.EmployeeInput in) {
        requireText(in.name(), "Invalid employee payload: name is required");
        if (in.password() == null || in.password().isEmpty()) {
            throw ApiException.badRequest("password is required to create an employee");
        }
        if (in.password().length() < 8) {
            throw ApiException.badRequest("password must be at least 8 characters long");
        }
        String role = in.role() == null || in.role().isEmpty() ? "staff" : in.role();
        List<String> perms = in.permissions() == null ? List.of() : in.permissions();
        validatePermissions(perms);

        Employee e = new Employee();
        e.setId(in.id() == null || in.id().isBlank() ? UUID.randomUUID().toString() : in.id());
        e.setName(in.name());
        e.setDesig(nz(in.desig()));
        e.setPasswordHash(encoder.encode(in.password()));
        e.setPhone(in.phone());
        e.setRole(role);
        e.setPinHash(in.pin() == null || in.pin().isEmpty() ? null : encoder.encode(in.pin()));
        e.setPermissions(perms.toArray(String[]::new));
        e.setMustChangePassword(true);
        try {
            return toDto(employees.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create employee (id may already exist)");
        }
    }

    @Transactional
    public StaffDtos.Employee updateEmployee(String id, StaffDtos.EmployeeInput in) {
        requireText(in.name(), "Invalid employee payload: name is required");
        String role = in.role() == null || in.role().isEmpty() ? "staff" : in.role();
        List<String> perms = in.permissions() == null ? List.of() : in.permissions();
        validatePermissions(perms);
        if (in.password() != null && !in.password().isEmpty() && in.password().length() < 8) {
            throw ApiException.badRequest("password must be at least 8 characters long");
        }

        Employee e = employees.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
        // Refuse to demote the last remaining admin, so nobody can lock everyone out.
        if (!"admin".equals(role) && "admin".equals(e.getRole()) && employees.countOtherAdmins(id) == 0) {
            throw ApiException.conflict("Cannot demote the last remaining administrator account");
        }

        if (in.password() != null && !in.password().isEmpty()) {
            // An admin-set password is temporary until the employee picks their own.
            e.setPasswordHash(encoder.encode(in.password()));
            e.setMustChangePassword(true);
        }
        if (in.pin() != null && !in.pin().isEmpty()) {
            e.setPinHash(encoder.encode(in.pin()));
        }
        e.setName(nz(in.name()));
        e.setDesig(nz(in.desig()));
        e.setPhone(in.phone());
        e.setRole(role);
        e.setPermissions(perms.toArray(String[]::new));
        try {
            return toDto(employees.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update employee");
        }
    }

    @Transactional
    public void deleteEmployee(String id, String callerId) {
        if (id.equals(callerId)) {
            throw ApiException.conflict("You cannot delete your own account while logged in as it");
        }
        Employee e = employees.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
        if ("admin".equals(e.getRole()) && employees.countOtherAdmins(id) == 0) {
            throw ApiException.conflict("Cannot delete the last remaining administrator account");
        }
        employees.deleteById(id);
    }

    // ---- daily register --------------------------------------------------

    @Transactional(readOnly = true)
    public StaffDtos.DailyRegister getRegister() {
        return registers.findById(Boolean.TRUE).map(this::toDto).orElseGet(() ->
                new StaffDtos.DailyRegister(null, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0,
                        Map.of(), 0, 0, false, GO_ZERO_TIME));
    }

    @Transactional
    public StaffDtos.DailyRegister putRegister(StaffDtos.DailyRegisterInput in) {
        DailyRegister r = registers.findById(Boolean.TRUE).orElseGet(DailyRegister::new);
        r.setDate(in.date());
        r.setPrevBd(bd(in.prevBD()));
        r.setTodaySell(bd(in.todaySell()));
        r.setPhonePe(bd(in.phonePe()));
        r.setExpenses(bd(in.expenses()));
        r.setBankShift(bd(in.bankShift()));
        r.setLocked(in.isLocked());
        r.setOpeningCash(bd(in.openingCash()));
        r.setTotalSales(bd(in.totalSales()));
        r.setCashSales(bd(in.cashSales()));
        r.setUpiSales(bd(in.upiSales()));
        r.setCardSales(bd(in.cardSales()));
        r.setTotalExpenses(bd(in.totalExpenses()));
        r.setDenominations(json.write(in.denominations() == null ? Map.of() : in.denominations()));
        r.setClosingPhysicalCash(bd(in.closingPhysicalCash()));
        r.setCashDifference(bd(in.cashDifference()));
        r.setDrawerClosed(in.isDrawerClosed());
        StaffDtos.DailyRegister out = toDto(registers.saveAndFlush(r));
        events.publish("dailyRegister");
        return out;
    }

    // ---- invoice config --------------------------------------------------

    @Transactional(readOnly = true)
    public StaffDtos.InvoiceConfig getInvoiceConfig() {
        return invoices.findById(Boolean.TRUE).map(StaffService::toDto).orElseGet(() ->
                new StaffDtos.InvoiceConfig("", null, null, "", "", "", "", "", "", null, null, null, true,
                        null, null, null, null, null, null, GO_ZERO_TIME));
    }

    @Transactional
    public StaffDtos.InvoiceConfig putInvoiceConfig(StaffDtos.InvoiceConfigInput in) {
        if (in.name() == null || in.name().isBlank()) {
            throw ApiException.badRequest("Invalid invoice config payload: name is required");
        }
        InvoiceConfig c = invoices.findById(Boolean.TRUE).orElseGet(InvoiceConfig::new);
        c.setName(in.name());
        c.setStoreName(in.storeName());
        c.setSubtitle(in.subtitle());
        c.setDl(nz(in.dl()));
        c.setGst(nz(in.gst()));
        c.setPhone(nz(in.phone()));
        c.setWaGroup(nz(in.waGroup()));
        c.setAddr(nz(in.addr()));
        c.setTerms(nz(in.terms()));
        c.setLogoUrl(in.logoUrl());
        c.setRetentionMonths(in.retentionMonths());
        c.setRetentionPolicyNotice(in.retentionPolicyNotice());
        c.setAutoPurgeOldInvoices(in.autoPurgeOldInvoices());
        c.setLastPurgeDate(in.lastPurgeDate());
        c.setDirector(in.director());
        c.setPharmacist(in.pharmacist());
        c.setCurrency(in.currency());
        c.setPrinterType(in.printerType());
        c.setHeaderTheme(in.headerTheme());
        try {
            return toDto(invoices.saveAndFlush(c));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to save invoice config");
        }
    }

    // ---- mapping and validation -----------------------------------------

    static StaffDtos.Employee toDto(Employee e) {
        return new StaffDtos.Employee(e.getId(), e.getName(), e.getDesig(), e.getPhone(), e.getRole(),
                Arrays.asList(e.getPermissions()), e.isMustChangePassword(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private StaffDtos.DailyRegister toDto(DailyRegister r) {
        return new StaffDtos.DailyRegister(r.getDate(), num(r.getPrevBd()), num(r.getTodaySell()),
                num(r.getPhonePe()), num(r.getExpenses()), num(r.getBankShift()), r.isLocked(),
                num(r.getOpeningCash()), num(r.getTotalSales()), num(r.getCashSales()), num(r.getUpiSales()),
                num(r.getCardSales()), num(r.getTotalExpenses()),
                json.read(r.getDenominations(), DENOMINATIONS, Map.of()),
                num(r.getClosingPhysicalCash()), num(r.getCashDifference()), r.isDrawerClosed(), r.getUpdatedAt());
    }

    static StaffDtos.InvoiceConfig toDto(InvoiceConfig c) {
        return new StaffDtos.InvoiceConfig(c.getName(), c.getStoreName(), c.getSubtitle(), c.getDl(), c.getGst(),
                c.getPhone(), c.getWaGroup(), c.getAddr(), c.getTerms(), c.getLogoUrl(), c.getRetentionMonths(),
                c.getRetentionPolicyNotice(), c.isAutoPurgeOldInvoices(), c.getLastPurgeDate(), c.getDirector(),
                c.getPharmacist(), c.getCurrency(), c.getPrinterType(), c.getHeaderTheme(), c.getUpdatedAt());
    }

    private static void validatePermissions(List<String> perms) {
        for (String p : perms) {
            if (!Tabs.ALL.contains(p)) throw ApiException.badRequest("unknown permission: " + p);
        }
    }

    private static void requireText(String value, String title) {
        if (value == null || value.isBlank()) throw ApiException.badRequest(title);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static double num(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}

package com.kinetirx.service;

import com.kinetirx.common.EventBus;
import com.kinetirx.dto.OperationsDtos;
import com.kinetirx.dto.OperationsDtos.*;
import com.kinetirx.entity.Expense;
import com.kinetirx.entity.MarketingCampaign;
import com.kinetirx.entity.NeededMed;
import com.kinetirx.entity.OpdVisit;
import com.kinetirx.entity.WorksheetTask;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Expenses, medicine orders, OPD visits, marketing campaigns, and worksheet tasks.
 * Mirrors the matching Go handlers, including their status validation and defaults.
 */
@Service
public class OperationsService {

    private static final Set<String> NEEDED_STATUSES =
            Set.of("Distributor Ordered", "Processing", "Pending", "Delivered", "Cancelled");
    private static final Set<String> CAMPAIGN_STATUSES =
            Set.of("7-Day Alert Active", "Upcoming", "Planned", "Completed");
    private static final Set<String> TASK_STATUSES =
            Set.of("Pending", "In Progress", "Planned", "Completed");

    private final ExpenseRepository expenses;
    private final NeededMedRepository neededMeds;
    private final OpdRepository opd;
    private final CampaignRepository campaigns;
    private final TaskRepository tasks;
    private final EventBus events;

    public OperationsService(ExpenseRepository expenses, NeededMedRepository neededMeds, OpdRepository opd,
                             CampaignRepository campaigns, TaskRepository tasks, EventBus events) {
        this.expenses = expenses;
        this.neededMeds = neededMeds;
        this.opd = opd;
        this.campaigns = campaigns;
        this.tasks = tasks;
        this.events = events;
    }

    // ---- expenses --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OperationsDtos.Expense> listExpenses(String date) {
        return expenses.search(date == null ? "" : date).stream().map(OperationsService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OperationsDtos.Expense getExpense(String id) {
        return expenses.findById(id).map(OperationsService::toDto)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));
    }

    @Transactional
    public OperationsDtos.Expense createExpense(ExpenseInput in) {
        requireText(in.date(), "Invalid expense payload: date is required");
        Expense e = new Expense();
        e.setId(idOrNew(in.id()));
        applyExpense(e, in);
        try {
            return toDto(expenses.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create expense (id may already exist)");
        }
    }

    @Transactional
    public OperationsDtos.Expense updateExpense(String id, ExpenseInput in) {
        Expense e = expenses.findById(id)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));
        applyExpense(e, in);
        try {
            return toDto(expenses.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update expense");
        }
    }

    @Transactional
    public void deleteExpense(String id) {
        if (!expenses.existsById(id)) throw ApiException.notFound("Expense not found");
        expenses.deleteById(id);
    }

    // ---- medicine orders -------------------------------------------------

    @Transactional(readOnly = true)
    public List<OperationsDtos.NeededMed> listNeededMeds() {
        return neededMeds.findAllByOrderByCreatedAtDesc().stream().map(OperationsService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OperationsDtos.NeededMed getNeededMed(String id) {
        return neededMeds.findById(id).map(OperationsService::toDto)
                .orElseThrow(() -> ApiException.notFound("Needed medicine order not found"));
    }

    @Transactional
    public OperationsDtos.NeededMed createNeededMed(NeededMedInput in) {
        requireText(in.med(), "Invalid needed medicine order payload: med is required");
        String status = in.status() == null || in.status().isEmpty() ? "Pending" : in.status();
        if (!NEEDED_STATUSES.contains(status)) {
            throw ApiException.badRequest("status must be one of: Distributor Ordered, Processing, Pending, Delivered, Cancelled");
        }
        NeededMed n = new NeededMed();
        n.setId(idOrNew(in.id()));
        n.setPatientId(in.patientId());
        n.setMed(in.med());
        n.setName(nz(in.name()));
        n.setPhone(nz(in.phone()));
        n.setDist(nz(in.dist()));
        n.setTime(nz(in.time()));
        n.setQty(qty(in.qty()));
        n.setStatus(status);
        try {
            OperationsDtos.NeededMed out = toDto(neededMeds.saveAndFlush(n));
            events.publish("neededMeds");
            return out;
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create needed medicine order (id may already exist)");
        }
    }

    @Transactional
    public OperationsDtos.NeededMed updateNeededMed(String id, NeededMedInput in) {
        if (in.status() != null && !in.status().isEmpty() && !NEEDED_STATUSES.contains(in.status())) {
            throw ApiException.badRequest("status must be one of: Distributor Ordered, Processing, Pending, Delivered, Cancelled");
        }
        NeededMed n = neededMeds.findById(id)
                .orElseThrow(() -> ApiException.notFound("Needed medicine order not found"));
        n.setPatientId(in.patientId());
        n.setMed(nz(in.med()));
        n.setName(nz(in.name()));
        n.setPhone(nz(in.phone()));
        n.setDist(nz(in.dist()));
        n.setTime(nz(in.time()));
        n.setQty(qty(in.qty()));
        n.setStatus(nz(in.status()));
        try {
            OperationsDtos.NeededMed out = toDto(neededMeds.saveAndFlush(n));
            events.publish("neededMeds");
            return out;
        } catch (DataIntegrityViolationException ex) {
            // An empty status violates the database CHECK constraint, which Go also surfaced as a 500.
            throw ApiException.internal("Failed to update needed medicine order");
        }
    }

    @Transactional
    public void deleteNeededMed(String id) {
        if (!neededMeds.existsById(id)) throw ApiException.notFound("Needed medicine order not found");
        neededMeds.deleteById(id);
        events.publish("neededMeds");
    }

    // ---- OPD visits ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OperationsDtos.OpdVisit> listOpd() {
        return opd.listOrdered().stream().map(OperationsService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OperationsDtos.OpdVisit getOpd(String id) {
        return opd.findById(id).map(OperationsService::toDto)
                .orElseThrow(() -> ApiException.notFound("OPD visit not found"));
    }

    @Transactional
    public OperationsDtos.OpdVisit createOpd(OpdInput in) {
        requireText(in.name(), "Invalid OPD visit payload: name is required");
        OpdVisit o = new OpdVisit();
        o.setId(idOrNew(in.id()));
        applyOpd(o, in);
        try {
            return toDto(opd.saveAndFlush(o));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create OPD visit (id may already exist)");
        }
    }

    @Transactional
    public OperationsDtos.OpdVisit updateOpd(String id, OpdInput in) {
        OpdVisit o = opd.findById(id).orElseThrow(() -> ApiException.notFound("OPD visit not found"));
        applyOpd(o, in);
        try {
            return toDto(opd.saveAndFlush(o));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update OPD visit");
        }
    }

    @Transactional
    public void deleteOpd(String id) {
        if (!opd.existsById(id)) throw ApiException.notFound("OPD visit not found");
        opd.deleteById(id);
    }

    // ---- marketing campaigns --------------------------------------------

    @Transactional(readOnly = true)
    public List<Campaign> listCampaigns() {
        return campaigns.listOrdered().stream().map(OperationsService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Campaign getCampaign(String id) {
        return campaigns.findById(id).map(OperationsService::toDto)
                .orElseThrow(() -> ApiException.notFound("Marketing campaign not found"));
    }

    @Transactional
    public Campaign createCampaign(CampaignInput in) {
        requireText(in.action(), "Invalid marketing campaign payload: action is required");
        String status = in.status() == null || in.status().isEmpty() ? "Planned" : in.status();
        if (!CAMPAIGN_STATUSES.contains(status)) {
            throw ApiException.badRequest("status must be one of: 7-Day Alert Active, Upcoming, Planned, Completed");
        }
        MarketingCampaign c = new MarketingCampaign();
        c.setId(idOrNew(in.id()));
        c.setDoc(nz(in.doc()));
        c.setDate(nz(in.date()));
        c.setAction(in.action());
        c.setStatus(status);
        try {
            return toDto(campaigns.saveAndFlush(c));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create marketing campaign (id may already exist)");
        }
    }

    @Transactional
    public Campaign updateCampaign(String id, CampaignInput in) {
        if (in.status() != null && !in.status().isEmpty() && !CAMPAIGN_STATUSES.contains(in.status())) {
            throw ApiException.badRequest("status must be one of: 7-Day Alert Active, Upcoming, Planned, Completed");
        }
        MarketingCampaign c = campaigns.findById(id)
                .orElseThrow(() -> ApiException.notFound("Marketing campaign not found"));
        c.setDoc(nz(in.doc()));
        c.setDate(nz(in.date()));
        c.setAction(nz(in.action()));
        c.setStatus(nz(in.status()));
        try {
            return toDto(campaigns.saveAndFlush(c));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update marketing campaign");
        }
    }

    @Transactional
    public void deleteCampaign(String id) {
        if (!campaigns.existsById(id)) throw ApiException.notFound("Marketing campaign not found");
        campaigns.deleteById(id);
    }

    // ---- worksheet tasks -------------------------------------------------

    @Transactional(readOnly = true)
    public List<Task> listTasks() {
        return tasks.listOrdered().stream().map(OperationsService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Task getTask(String id) {
        return tasks.findById(id).map(OperationsService::toDto)
                .orElseThrow(() -> ApiException.notFound("Worksheet task not found"));
    }

    @Transactional
    public Task createTask(TaskInput in) {
        requireText(in.desc(), "Invalid worksheet task payload: desc is required");
        String status = in.status() == null || in.status().isEmpty() ? "Pending" : in.status();
        if (!TASK_STATUSES.contains(status)) {
            throw ApiException.badRequest("status must be one of: Pending, In Progress, Planned, Completed");
        }
        WorksheetTask t = new WorksheetTask();
        t.setId(idOrNew(in.id()));
        t.setCat(nz(in.cat()));
        t.setDescription(in.desc());
        t.setDate(nz(in.date()));
        t.setStatus(status);
        try {
            return toDto(tasks.saveAndFlush(t));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create worksheet task (id may already exist)");
        }
    }

    @Transactional
    public Task updateTask(String id, TaskInput in) {
        if (in.status() != null && !in.status().isEmpty() && !TASK_STATUSES.contains(in.status())) {
            throw ApiException.badRequest("status must be one of: Pending, In Progress, Planned, Completed");
        }
        WorksheetTask t = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Worksheet task not found"));
        t.setCat(nz(in.cat()));
        t.setDescription(nz(in.desc()));
        t.setDate(nz(in.date()));
        t.setStatus(nz(in.status()));
        try {
            return toDto(tasks.saveAndFlush(t));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update worksheet task");
        }
    }

    @Transactional
    public void deleteTask(String id) {
        if (!tasks.existsById(id)) throw ApiException.notFound("Worksheet task not found");
        tasks.deleteById(id);
    }

    // ---- mapping helpers -------------------------------------------------

    private static OperationsDtos.Expense toDto(Expense e) {
        return new OperationsDtos.Expense(e.getId(), e.getDate(), e.getCat(), e.getDescription(), num(e.getAmt()),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static void applyExpense(Expense e, ExpenseInput in) {
        e.setDate(in.date());
        e.setCat(nz(in.cat()));
        e.setDescription(nz(in.desc()));
        e.setAmt(bd(in.amt()));
    }

    private static OperationsDtos.NeededMed toDto(NeededMed n) {
        return new OperationsDtos.NeededMed(n.getId(), n.getPatientId(), n.getMed(), n.getName(), n.getPhone(), n.getDist(),
                n.getTime(), num(n.getQty()), n.getStatus(), n.getCreatedAt(), n.getUpdatedAt());
    }

    private static OperationsDtos.OpdVisit toDto(OpdVisit o) {
        return new OperationsDtos.OpdVisit(o.getId(), o.getPatientId(), o.getName(), o.getPhone(), o.getAgeSex(), o.getDoc(),
                o.getVdate(), o.getRvdate(), o.getBtest(), o.getReminder(), o.getCreatedAt(), o.getUpdatedAt());
    }

    private static void applyOpd(OpdVisit o, OpdInput in) {
        o.setPatientId(in.patientId());
        o.setName(in.name());
        o.setPhone(nz(in.phone()));
        o.setAgeSex(nz(in.ageSex()));
        o.setDoc(nz(in.doc()));
        o.setVdate(nz(in.vdate()));
        o.setRvdate(nz(in.rvdate()));
        o.setBtest(nz(in.btest()));
        o.setReminder(nz(in.reminder()));
    }

    private static Campaign toDto(MarketingCampaign c) {
        return new Campaign(c.getId(), c.getDoc(), c.getDate(), c.getAction(), c.getStatus(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private static Task toDto(WorksheetTask t) {
        return new Task(t.getId(), t.getCat(), t.getDescription(), t.getDate(), t.getStatus(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    private static String idOrNew(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
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

    private static BigDecimal qty(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }
}

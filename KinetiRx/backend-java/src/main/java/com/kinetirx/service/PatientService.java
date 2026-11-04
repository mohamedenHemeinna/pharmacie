package com.kinetirx.service;

import com.kinetirx.common.EventBus;
import com.kinetirx.dto.PatientDtos;
import com.kinetirx.entity.Patient;
import com.kinetirx.entity.PatientDue;
import com.kinetirx.exception.ApiException;
import com.kinetirx.mapper.PatientMapper;
import com.kinetirx.repository.PatientDueRepository;
import com.kinetirx.repository.PatientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Patients and due-khata (patient credit ledger). Mirrors handlers/patients.go and patients_due.go. */
@Service
public class PatientService {

    private final PatientRepository patients;
    private final PatientDueRepository dues;
    private final PatientMapper mapper;
    private final EventBus events;

    public PatientService(PatientRepository patients, PatientDueRepository dues, PatientMapper mapper, EventBus events) {
        this.patients = patients;
        this.dues = dues;
        this.mapper = mapper;
        this.events = events;
    }

    // ---- patients --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PatientDtos.Patient> list() {
        return patients.findAllByOrderByNameAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PatientDtos.Patient get(String id) {
        return patients.findById(id).map(mapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Patient not found"));
    }

    /** Reserves the next sequential ID from patient_id_seq; never computed client-side. */
    @Transactional
    public PatientDtos.NextId nextId() {
        return new PatientDtos.NextId(String.valueOf(patients.nextId()));
    }

    @Transactional
    public PatientDtos.Patient create(PatientDtos.PatientInput in) {
        requireName(in.name(), "Invalid patient payload: name is required");
        Patient p = new Patient();
        p.setId(in.id() == null || in.id().isBlank() ? UUID.randomUUID().toString() : in.id());
        mapper.apply(p, in);
        try {
            return mapper.toDto(patients.saveAndFlush(p));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create patient (id may already exist)");
        }
    }

    @Transactional
    public PatientDtos.Patient update(String id, PatientDtos.PatientInput in) {
        requireName(in.name(), "Invalid patient payload: name is required");
        Patient p = patients.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found"));
        mapper.apply(p, in);
        try {
            return mapper.toDto(patients.saveAndFlush(p));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update patient");
        }
    }

    @Transactional
    public void delete(String id) {
        if (!patients.existsById(id)) throw ApiException.notFound("Patient not found");
        patients.deleteById(id);
    }

    // ---- due-khata ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PatientDtos.Due> listDues() {
        return dues.listOrdered().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PatientDtos.Due getDue(String id) {
        return dues.findById(id).map(mapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Due-khata entry not found"));
    }

    @Transactional
    public PatientDtos.Due createDue(PatientDtos.DueInput in) {
        requireName(in.name(), "Invalid due-khata payload: name is required");
        PatientDue d = new PatientDue();
        d.setId(in.id() == null || in.id().isBlank() ? UUID.randomUUID().toString() : in.id());
        mapper.apply(d, in);
        try {
            PatientDtos.Due out = mapper.toDto(dues.saveAndFlush(d));
            events.publish("dueKhata");
            return out;
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create due-khata entry (id may already exist)");
        }
    }

    @Transactional
    public PatientDtos.Due updateDue(String id, PatientDtos.DueInput in) {
        requireName(in.name(), "Invalid due-khata payload: name is required");
        PatientDue d = dues.findById(id).orElseThrow(() -> ApiException.notFound("Due-khata entry not found"));
        mapper.apply(d, in);
        try {
            PatientDtos.Due out = mapper.toDto(dues.saveAndFlush(d));
            events.publish("dueKhata");
            return out;
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.internal("Failed to update due-khata entry");
        }
    }

    @Transactional
    public void deleteDue(String id) {
        if (!dues.existsById(id)) throw ApiException.notFound("Due-khata entry not found");
        dues.deleteById(id);
        events.publish("dueKhata");
    }

    private static void requireName(String name, String title) {
        if (name == null || name.isBlank()) throw ApiException.badRequest(title);
    }
}

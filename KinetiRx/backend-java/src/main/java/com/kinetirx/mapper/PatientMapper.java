package com.kinetirx.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kinetirx.common.JsonCodec;
import com.kinetirx.dto.PatientDtos;
import com.kinetirx.entity.Patient;
import com.kinetirx.entity.PatientDue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    private static final TypeReference<List<PatientDtos.PurchaseHistoryEntry>> HISTORY = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};

    private final JsonCodec json;

    public PatientMapper(JsonCodec json) {
        this.json = json;
    }

    public PatientDtos.Patient toDto(Patient p) {
        return new PatientDtos.Patient(
                p.getId(), p.getName(), p.getPhone(), p.getAge(), p.getGender(), p.getAgeGender(),
                p.getAddr(), p.getAddress(), p.getDoc(), p.getDoctor(), p.getReason(),
                num(p.getTotalDue()), optNum(p.getDueAmount()), p.getLastDate(), p.getLastVisitDate(),
                p.getTotalVisits(),
                json.read(p.getPurchaseHistory(), HISTORY, List.of()),
                List.of(p.getBloodTests()),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    public void apply(Patient p, PatientDtos.PatientInput in) {
        p.setName(in.name());
        p.setPhone(nz(in.phone()));
        p.setAge(in.age());
        p.setGender(in.gender());
        p.setAgeGender(in.ageGender());
        p.setAddr(in.addr());
        p.setAddress(in.address());
        p.setDoc(in.doc());
        p.setDoctor(in.doctor());
        p.setReason(in.reason());
        p.setTotalDue(bd(in.totalDue()));
        p.setDueAmount(in.dueAmount() == null ? null : bd(in.dueAmount()));
        p.setLastDate(in.lastDate());
        p.setLastVisitDate(in.lastVisitDate());
        p.setTotalVisits(in.totalVisits());
        p.setPurchaseHistory(json.write(in.purchaseHistory() == null ? List.of() : in.purchaseHistory()));
        List<String> tests = in.bloodTests() == null ? List.of() : in.bloodTests();
        p.setBloodTests(tests.toArray(String[]::new));
    }

    public PatientDtos.Due toDto(PatientDue d) {
        return new PatientDtos.Due(d.getId(), d.getPatientId(), d.getName(), d.getPhone(), d.getAddr(),
                d.getDoc(), d.getReason(), num(d.getDue()), d.getLastDate(), d.getCreatedAt(), d.getUpdatedAt());
    }

    public void apply(PatientDue d, PatientDtos.DueInput in) {
        d.setPatientId(in.patientId());
        d.setName(in.name());
        d.setPhone(nz(in.phone()));
        d.setAddr(nz(in.addr()));
        d.setDoc(nz(in.doc()));
        d.setReason(nz(in.reason()));
        d.setDue(bd(in.due()));
        d.setLastDate(nz(in.lastDate()));
    }

    static double num(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }

    /** Nullable numeric: null stays null (Go omitempty), never invented as zero. */
    static Double optNum(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }
}

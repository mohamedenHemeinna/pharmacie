package com.kinetirx.repository;

import com.kinetirx.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    List<Patient> findAllByOrderByNameAsc();

    /** Atomically reserves the next patient number (see migration V9). */
    @Query(value = "SELECT nextval('patient_id_seq')", nativeQuery = true)
    long nextId();
}

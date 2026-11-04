package com.kinetirx.repository;

import com.kinetirx.entity.PatientDue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientDueRepository extends JpaRepository<PatientDue, String> {

    /** Same order as the Go query: ORDER BY last_date DESC NULLS LAST, name ASC. */
    @Query(value = "SELECT * FROM patients_due ORDER BY last_date DESC NULLS LAST, name ASC", nativeQuery = true)
    List<PatientDue> listOrdered();
}

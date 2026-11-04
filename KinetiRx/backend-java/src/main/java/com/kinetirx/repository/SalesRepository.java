package com.kinetirx.repository;

import com.kinetirx.entity.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesRepository extends JpaRepository<SalesRecord, String> {

    /** Optional filters use the same '' sentinel as the Go query. */
    @Query(value = "SELECT * FROM sales_history WHERE (:date = '' OR date = :date) "
            + "AND (:patientId = '' OR patient_id = :patientId) ORDER BY created_at DESC", nativeQuery = true)
    List<SalesRecord> search(@Param("date") String date, @Param("patientId") String patientId);
}

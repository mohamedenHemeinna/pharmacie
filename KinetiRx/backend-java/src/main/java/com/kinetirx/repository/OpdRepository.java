package com.kinetirx.repository;

import com.kinetirx.entity.OpdVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpdRepository extends JpaRepository<OpdVisit, String> {
    @Query(value = "SELECT * FROM opd_visits ORDER BY vdate DESC, created_at DESC", nativeQuery = true)
    List<OpdVisit> listOrdered();
}

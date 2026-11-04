package com.kinetirx.repository;

import com.kinetirx.entity.NeededMed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NeededMedRepository extends JpaRepository<NeededMed, String> {
    List<NeededMed> findAllByOrderByCreatedAtDesc();
}

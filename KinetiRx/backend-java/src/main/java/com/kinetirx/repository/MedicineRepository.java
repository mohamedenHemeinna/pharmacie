package com.kinetirx.repository;

import com.kinetirx.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, String> {

    /** Same order as the Go list query: ORDER BY name ASC. */
    List<Medicine> findAllByOrderByNameAsc();
}

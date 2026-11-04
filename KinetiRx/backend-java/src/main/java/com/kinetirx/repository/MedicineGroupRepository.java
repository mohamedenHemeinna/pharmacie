package com.kinetirx.repository;

import com.kinetirx.entity.MedicineGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineGroupRepository extends JpaRepository<MedicineGroup, String> {
    List<MedicineGroup> findAllByOrderByNameAsc();
}

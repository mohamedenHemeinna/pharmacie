package com.kinetirx.repository;

import com.kinetirx.entity.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, String> {
    List<Distributor> findAllByOrderByNameAsc();
}

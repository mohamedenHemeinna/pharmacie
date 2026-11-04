package com.kinetirx.repository;

import com.kinetirx.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /** Identifier matches the id or the name, case-insensitively. Same rule as the Go login query. */
    @Query(value = "SELECT * FROM employees WHERE id = :identifier OR lower(name) = lower(:identifier) LIMIT 1",
            nativeQuery = true)
    Optional<Employee> findByIdOrName(@Param("identifier") String identifier);

    @Query(value = "SELECT count(*) FROM employees WHERE role = 'admin' AND id <> :id", nativeQuery = true)
    long countOtherAdmins(@Param("id") String id);
}

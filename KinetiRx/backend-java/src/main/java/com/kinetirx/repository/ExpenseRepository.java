package com.kinetirx.repository;

import com.kinetirx.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {

    /** Same filter and order as the Go query. */
    @Query(value = "SELECT * FROM expenses WHERE (:date = '' OR date = :date) ORDER BY date DESC, created_at DESC",
            nativeQuery = true)
    List<Expense> search(@Param("date") String date);
}

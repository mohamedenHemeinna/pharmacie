package com.kinetirx.repository;

import com.kinetirx.entity.WorksheetTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<WorksheetTask, String> {
    @Query(value = "SELECT * FROM worksheet_tasks ORDER BY date DESC, created_at DESC", nativeQuery = true)
    List<WorksheetTask> listOrdered();
}

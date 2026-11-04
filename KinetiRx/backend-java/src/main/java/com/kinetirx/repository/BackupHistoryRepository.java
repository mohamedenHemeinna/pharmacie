package com.kinetirx.repository;

import com.kinetirx.entity.BackupHistory;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistory, String> {

    List<BackupHistory> findAllByOrderByStartedAtDesc(Limit limit);

    /** Verified backups beyond the newest N, oldest last, for retention. */
    @Query(value = "SELECT * FROM backup_history WHERE status = 'verified' ORDER BY completed_at DESC OFFSET :skip",
            nativeQuery = true)
    List<BackupHistory> verifiedBeyond(int skip);

    Optional<BackupHistory> findFirstByStatusOrderByCompletedAtDesc(String status);
}

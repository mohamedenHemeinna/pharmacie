package com.kinetirx.repository;

import com.kinetirx.entity.BackupConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupConfigRepository extends JpaRepository<BackupConfig, Boolean> {}

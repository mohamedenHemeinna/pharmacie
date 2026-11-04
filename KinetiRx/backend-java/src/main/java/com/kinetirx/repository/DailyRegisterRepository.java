package com.kinetirx.repository;

import com.kinetirx.entity.DailyRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyRegisterRepository extends JpaRepository<DailyRegister, Boolean> {}

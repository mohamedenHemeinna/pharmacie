package com.kinetirx.repository;

import com.kinetirx.entity.AiProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, Boolean> {}

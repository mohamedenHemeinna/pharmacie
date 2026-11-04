package com.kinetirx.repository;

import com.kinetirx.entity.InvoiceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceConfigRepository extends JpaRepository<InvoiceConfig, Boolean> {}

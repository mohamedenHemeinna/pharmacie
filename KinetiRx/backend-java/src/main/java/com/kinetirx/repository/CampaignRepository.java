package com.kinetirx.repository;

import com.kinetirx.entity.MarketingCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<MarketingCampaign, String> {
    @Query(value = "SELECT * FROM marketing_campaigns ORDER BY date DESC, created_at DESC", nativeQuery = true)
    List<MarketingCampaign> listOrdered();
}

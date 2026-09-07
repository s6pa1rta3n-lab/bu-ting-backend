package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.RewardPayoutHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for RewardPayoutHistory entity. */
public interface RewardPayoutHistoryRepository extends JpaRepository<RewardPayoutHistory, UUID> {

  List<RewardPayoutHistory> findByPayoutIdOrderByCreatedAtDesc(UUID payoutId);
}

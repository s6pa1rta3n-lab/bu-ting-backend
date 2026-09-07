package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardPayout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data JPA repository for RewardPayout entity. */
public interface RewardPayoutRepository
    extends JpaRepository<RewardPayout, UUID>, JpaSpecificationExecutor<RewardPayout> {

  Optional<RewardPayout> findByParticipationIdAndRewardReason(
      UUID participationId, GrantReason rewardReason);

  boolean existsByParticipationIdAndRewardReason(UUID participationId, GrantReason rewardReason);

  List<RewardPayout> findByRoundId(UUID roundId);

  List<RewardPayout> findByEventId(UUID eventId);
}

package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardGrant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RewardGrantRepository
    extends JpaRepository<RewardGrant, UUID>, JpaSpecificationExecutor<RewardGrant> {

  boolean existsByParticipationIdAndGrantReasonAndReward_Id(
      UUID participationId, GrantReason grantReason, UUID rewardId);

  List<RewardGrant> findByUserIdOrderByGrantedAtDesc(UUID userId);

  List<RewardGrant> findByParticipationIdInAndRevokedAtIsNull(
      java.util.Collection<UUID> participationIds);

  List<RewardGrant> findByParticipationIdAndRevokedAtIsNull(UUID participationId);

  @org.springframework.data.jpa.repository.Query(
      "SELECT count(g) FROM RewardGrant g WHERE g.reward.id = :rewardId "
          + "AND g.grantedAt >= :startOfMonth AND g.grantedAt < :startOfNextMonth "
          + "AND g.revokedAt IS NULL")
  long countActiveGrantsInMonth(
      @org.springframework.data.repository.query.Param("rewardId") UUID rewardId,
      @org.springframework.data.repository.query.Param("startOfMonth")
          java.time.OffsetDateTime startOfMonth,
      @org.springframework.data.repository.query.Param("startOfNextMonth")
          java.time.OffsetDateTime startOfNextMonth);
}

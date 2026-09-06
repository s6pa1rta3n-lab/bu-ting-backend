package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardGrant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 보상 지급 기록 저장소 인터페이스. */
public interface RewardGrantRepository extends JpaRepository<RewardGrant, UUID> {

  /**
   * 참여 식별자, 지급 사유, 보상 식별자로 중복 지급 여부를 확인한다.
   *
   * @param participationId 구역 이벤트 참여 식별자
   * @param grantReason 지급 사유
   * @param rewardId 보상 식별자
   * @return 중복 지급 여부
   */
  boolean existsByParticipationIdAndGrantReasonAndReward_Id(
      UUID participationId, GrantReason grantReason, UUID rewardId);

  /**
   * 유저의 모든 보상 지급 기록을 최근 순으로 조회한다.
   *
   * @param userId 유저 식별자
   * @return 보상 지급 기록 목록
   */
  List<RewardGrant> findByUserIdOrderByGrantedAtDesc(UUID userId);
}

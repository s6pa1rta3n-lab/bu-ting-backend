package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.UserBadge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저 배지 저장소 인터페이스. */
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

  /**
   * 유저가 특정 보상 배지를 이미 보유하고 있는지 확인한다.
   *
   * @param userId 유저 식별자
   * @param rewardId 보상 식별자
   * @return 보유 여부
   */
  boolean existsByUserIdAndReward_Id(UUID userId, UUID rewardId);

  /**
   * 유저의 모든 보유 배지를 최근 획득 순으로 조회한다.
   *
   * @param userId 유저 식별자
   * @return 보유 배지 목록
   */
  List<UserBadge> findByUserIdOrderByEarnedAtDesc(UUID userId);
}

package com.butingbe.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저 보유 배지 엔티티. */
@Entity
@Table(
    name = "user_badge",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_badge_user_reward",
          columnNames = {"user_id", "reward_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_badge_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reward_id", nullable = false)
  private RewardCatalog reward;

  @Column(name = "grant_id", nullable = false)
  private UUID grantId;

  @Column(name = "earned_at", nullable = false)
  private OffsetDateTime earnedAt;

  @Builder
  private UserBadge(UUID userId, RewardCatalog reward, UUID grantId) {
    this.userId = userId;
    this.reward = reward;
    this.grantId = grantId;
    this.earnedAt = OffsetDateTime.now();
  }
}

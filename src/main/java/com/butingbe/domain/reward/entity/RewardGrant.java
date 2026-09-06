package com.butingbe.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 보상 지급 기록 엔티티. */
@Entity
@Table(
    name = "reward_grant",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_reward_grant_participation_reason_reward",
          columnNames = {"participation_id", "grant_reason", "reward_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardGrant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "grant_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reward_id", nullable = false)
  private RewardCatalog reward;

  @Column(name = "participation_id")
  private UUID participationId;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "round_id")
  private UUID roundId;

  @Enumerated(EnumType.STRING)
  @Column(name = "grant_reason", nullable = false, length = 30)
  private GrantReason grantReason;

  @Column(name = "granted_at", nullable = false)
  private OffsetDateTime grantedAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @Builder
  private RewardGrant(
      UUID userId,
      RewardCatalog reward,
      UUID participationId,
      UUID eventId,
      UUID roundId,
      GrantReason grantReason,
      OffsetDateTime grantedAt) {
    this.userId = userId;
    this.reward = reward;
    this.participationId = participationId;
    this.eventId = eventId;
    this.roundId = roundId;
    this.grantReason = grantReason;
    this.grantedAt = grantedAt;
  }

  /**
   * 지급된 보상을 회수 처리한다.
   *
   * @param at 회수 처리 시각
   */
  public void revoke(OffsetDateTime at) {
    if (revokedAt == null) {
      revokedAt = at;
    }
  }
}

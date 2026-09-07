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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Step-by-step history log for a reward payout lifecycle event. */
@Entity
@Table(name = "reward_payout_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardPayoutHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "history_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payout_id", nullable = false)
  private RewardPayout payout;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 30)
  private RewardPayoutStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 30)
  private RewardPayoutStatus toStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private RewardPayoutAction action;

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(columnDefinition = "text")
  private String note;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Builder
  public RewardPayoutHistory(
      RewardPayout payout,
      RewardPayoutStatus fromStatus,
      RewardPayoutStatus toStatus,
      RewardPayoutAction action,
      UUID actorId,
      String note,
      OffsetDateTime createdAt) {
    this.payout = payout;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.action = action;
    this.actorId = actorId;
    this.note = note;
    this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
  }
}

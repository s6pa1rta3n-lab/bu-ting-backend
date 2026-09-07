package com.butingbe.domain.reward.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing an independent reward payout process for BASE or TOP_LIKE rewards. */
@Entity
@Table(
    name = "reward_payout",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_reward_payout_participation_reason",
          columnNames = {"participation_id", "reward_reason"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardPayout extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "payout_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "round_id")
  private UUID roundId;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "participation_id")
  private UUID participationId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "reward_reason", nullable = false, length = 30)
  private GrantReason rewardReason;

  @Column(name = "rank_n")
  private Integer rankN;

  @Column(name = "like_count_at_close")
  private Integer likeCountAtClose;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RewardPayoutStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "hold_status", nullable = false, length = 30)
  private RewardPayoutHoldStatus holdStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reward_id")
  private RewardCatalog rewardCatalog;

  @Column(name = "points")
  private Integer points;

  @Column(name = "badge_code", length = 100)
  private String badgeCode;

  @Column(name = "prize_name", length = 255)
  private String prizeName;

  @Column(name = "scheduled_at")
  private OffsetDateTime scheduledAt;

  @Column(name = "confirmed_at")
  private OffsetDateTime confirmedAt;

  @Column(name = "confirmed_by")
  private UUID confirmedBy;

  @Column(name = "mailed_at")
  private OffsetDateTime mailedAt;

  @Column(name = "information_collected_at")
  private OffsetDateTime informationCollectedAt;

  @Column(name = "sent_at")
  private OffsetDateTime sentAt;

  @Column(name = "paid_at")
  private OffsetDateTime paidAt;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(columnDefinition = "text")
  private String note;

  @Column(length = 255)
  private String reference;

  @Column(nullable = false)
  private Long revision;

  @OneToMany(mappedBy = "payout", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt DESC")
  private List<RewardPayoutHistory> histories = new ArrayList<>();

  @Builder
  public RewardPayout(
      UUID roundId,
      UUID eventId,
      UUID participationId,
      UUID userId,
      GrantReason rewardReason,
      Integer rankN,
      Integer likeCountAtClose,
      RewardPayoutStatus status,
      RewardPayoutHoldStatus holdStatus,
      RewardCatalog rewardCatalog,
      Integer points,
      String badgeCode,
      String prizeName,
      OffsetDateTime scheduledAt,
      String note,
      Long revision) {
    this.roundId = roundId;
    this.eventId = eventId;
    this.participationId = participationId;
    this.userId = userId;
    this.rewardReason = rewardReason;
    this.rankN = rankN;
    this.likeCountAtClose = likeCountAtClose;
    this.status = status;
    this.holdStatus = holdStatus != null ? holdStatus : RewardPayoutHoldStatus.NONE;
    this.rewardCatalog = rewardCatalog;
    this.points = points;
    this.badgeCode = badgeCode;
    this.prizeName = prizeName;
    this.scheduledAt = scheduledAt;
    this.note = note;
    this.revision = revision != null ? revision : 0L;
    this.retryCount = 0;
  }

  /** Adds a history entry to this payout. */
  public void addHistory(
      RewardPayoutAction action,
      RewardPayoutStatus from,
      RewardPayoutStatus to,
      UUID actorId,
      String historyNote) {
    RewardPayoutHistory history =
        RewardPayoutHistory.builder()
            .payout(this)
            .fromStatus(from)
            .toStatus(to)
            .action(action)
            .actorId(actorId)
            .note(historyNote)
            .createdAt(OffsetDateTime.now())
            .build();
    this.histories.add(history);
  }

  /** Increments the optimistic concurrency revision number. */
  public void incrementRevision() {
    this.revision = (this.revision == null ? 0L : this.revision) + 1L;
  }

  /** Confirms the payout. */
  public void confirm(UUID actorId) {
    this.confirmedAt = OffsetDateTime.now();
    this.confirmedBy = actorId;
    this.status = RewardPayoutStatus.CONFIRMED;
  }

  /** Marks the payout as paid upon successful ledger settlement. */
  public void markPaid(OffsetDateTime timestamp) {
    this.status = RewardPayoutStatus.PAID;
    this.paidAt = timestamp != null ? timestamp : OffsetDateTime.now();
    this.failureCode = null;
  }

  /** Marks the payout as failed. */
  public void markFailed(String code) {
    this.status = RewardPayoutStatus.FAILED;
    this.failureCode = code;
  }

  /** Marks notification mail as sent for TOP_LIKE prizes. */
  public void markMailSent(OffsetDateTime mailedTimestamp, String mailNote) {
    this.status = RewardPayoutStatus.MAIL_SENT;
    this.mailedAt = mailedTimestamp != null ? mailedTimestamp : OffsetDateTime.now();
    if (mailNote != null && !mailNote.isBlank()) {
      this.note = mailNote;
    }
  }

  /** Marks winner shipping information as collected. */
  public void markInfoCollected(OffsetDateTime collectedTimestamp, String collectionNote) {
    this.status = RewardPayoutStatus.INFO_COLLECTED;
    this.informationCollectedAt =
        collectedTimestamp != null ? collectedTimestamp : OffsetDateTime.now();
    if (collectionNote != null && !collectionNote.isBlank()) {
      this.note = collectionNote;
    }
  }

  /** Marks physical or coupon prize as officially sent. */
  public void markSent(
      OffsetDateTime sentTimestamp, String deliveryReference, String deliveryNote) {
    this.status = RewardPayoutStatus.SENT;
    this.sentAt = sentTimestamp != null ? sentTimestamp : OffsetDateTime.now();
    if (deliveryReference != null && !deliveryReference.isBlank()) {
      this.reference = deliveryReference;
    }
    if (deliveryNote != null && !deliveryNote.isBlank()) {
      this.note = deliveryNote;
    }
  }

  /** Updates payout schedule date. */
  public void schedule(OffsetDateTime targetSchedule) {
    this.scheduledAt = targetSchedule;
  }

  /** Puts payout on hold due to report. */
  public void hold(String reason) {
    this.holdStatus = RewardPayoutHoldStatus.HELD_REPORT;
    if (reason != null && !reason.isBlank()) {
      this.note = reason;
    }
  }

  /** Releases hold status back to NONE. */
  public void releaseHold(String reason) {
    this.holdStatus = RewardPayoutHoldStatus.NONE;
    if (reason != null && !reason.isBlank()) {
      this.note = reason;
    }
  }

  /** Updates reward configuration for unconfirmed payouts. */
  public void updateRewardConfig(
      RewardCatalog newCatalog,
      Integer newPoints,
      String newBadgeCode,
      String newPrizeName,
      OffsetDateTime newScheduledAt,
      String newNote) {
    if (newCatalog != null) {
      this.rewardCatalog = newCatalog;
    }
    if (newPoints != null) {
      this.points = newPoints;
    }
    if (newBadgeCode != null) {
      this.badgeCode = newBadgeCode;
    }
    if (newPrizeName != null) {
      this.prizeName = newPrizeName;
    }
    if (newScheduledAt != null) {
      this.scheduledAt = newScheduledAt;
    }
    if (newNote != null) {
      this.note = newNote;
    }
    if (this.status == RewardPayoutStatus.PENDING_ASSIGN
        && (this.rewardCatalog != null
            || this.prizeName != null
            || this.points != null
            || this.badgeCode != null)) {
      this.status = RewardPayoutStatus.PENDING_CONFIRM;
    }
  }

  /** Records a retry execution attempt. */
  public void incrementRetryCount() {
    this.retryCount += 1;
  }
}

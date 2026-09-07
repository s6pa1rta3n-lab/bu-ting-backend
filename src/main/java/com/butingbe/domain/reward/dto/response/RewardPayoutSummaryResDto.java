package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.entity.RewardPayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Summary DTO for reward payout list items. */
public record RewardPayoutSummaryResDto(
    UUID payoutId,
    UUID roundId,
    UUID eventId,
    UUID participationId,
    UUID userId,
    String rewardReason,
    Integer rankN,
    Integer likeCountAtClose,
    RewardPayoutStatus status,
    RewardPayoutHoldStatus holdStatus,
    UUID rewardCatalogId,
    Integer points,
    String badgeCode,
    String prizeName,
    OffsetDateTime scheduledAt,
    OffsetDateTime confirmedAt,
    OffsetDateTime paidAt,
    OffsetDateTime mailedAt,
    OffsetDateTime informationCollectedAt,
    OffsetDateTime sentAt,
    String failureCode,
    int retryCount,
    String note,
    String reference,
    Long revision,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static RewardPayoutSummaryResDto from(RewardPayout payout) {
    return new RewardPayoutSummaryResDto(
        payout.getId(),
        payout.getRoundId(),
        payout.getEventId(),
        payout.getParticipationId(),
        payout.getUserId(),
        payout.getRewardReason() != null ? payout.getRewardReason().name() : null,
        payout.getRankN(),
        payout.getLikeCountAtClose(),
        payout.getStatus(),
        payout.getHoldStatus(),
        payout.getRewardCatalog() != null ? payout.getRewardCatalog().getId() : null,
        payout.getPoints(),
        payout.getBadgeCode(),
        payout.getPrizeName(),
        payout.getScheduledAt(),
        payout.getConfirmedAt(),
        payout.getPaidAt(),
        payout.getMailedAt(),
        payout.getInformationCollectedAt(),
        payout.getSentAt(),
        payout.getFailureCode(),
        payout.getRetryCount(),
        payout.getNote(),
        payout.getReference(),
        payout.getRevision(),
        payout.getCreatedAt(),
        payout.getUpdatedAt());
  }
}

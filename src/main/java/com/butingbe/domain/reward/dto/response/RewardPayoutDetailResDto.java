package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.entity.RewardPayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Detailed view DTO of a reward payout including full snapshot and history timeline. */
public record RewardPayoutDetailResDto(
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
    RewardPayoutSnapshotDto snapshot,
    OffsetDateTime scheduledAt,
    OffsetDateTime confirmedAt,
    UUID confirmedBy,
    OffsetDateTime mailedAt,
    OffsetDateTime informationCollectedAt,
    OffsetDateTime sentAt,
    OffsetDateTime paidAt,
    String failureCode,
    int retryCount,
    String note,
    String reference,
    Long revision,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<RewardPayoutHistoryResDto> histories) {
  public static RewardPayoutDetailResDto from(RewardPayout payout) {
    RewardCatalog catalog = payout.getRewardCatalog();
    RewardPayoutSnapshotDto snapshotDto =
        new RewardPayoutSnapshotDto(
            catalog != null ? catalog.getId() : null,
            catalog != null ? catalog.getCode() : null,
            catalog != null ? catalog.getName() : null,
            catalog != null && catalog.getRewardType() != null
                ? catalog.getRewardType().name()
                : null,
            payout.getPoints(),
            payout.getBadgeCode(),
            payout.getPrizeName());

    List<RewardPayoutHistoryResDto> historyList =
        payout.getHistories() == null
            ? List.of()
            : payout.getHistories().stream().map(RewardPayoutHistoryResDto::from).toList();

    return new RewardPayoutDetailResDto(
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
        snapshotDto,
        payout.getScheduledAt(),
        payout.getConfirmedAt(),
        payout.getConfirmedBy(),
        payout.getMailedAt(),
        payout.getInformationCollectedAt(),
        payout.getSentAt(),
        payout.getPaidAt(),
        payout.getFailureCode(),
        payout.getRetryCount(),
        payout.getNote(),
        payout.getReference(),
        payout.getRevision(),
        payout.getCreatedAt(),
        payout.getUpdatedAt(),
        historyList);
  }
}

package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.reward.entity.RewardPayoutAction;
import com.butingbe.domain.reward.entity.RewardPayoutHistory;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DTO representing an individual audit history entry for a reward payout. */
public record RewardPayoutHistoryResDto(
    UUID historyId,
    RewardPayoutStatus fromStatus,
    RewardPayoutStatus toStatus,
    RewardPayoutAction action,
    UUID actorId,
    String note,
    OffsetDateTime createdAt) {
  public static RewardPayoutHistoryResDto from(RewardPayoutHistory history) {
    return new RewardPayoutHistoryResDto(
        history.getId(),
        history.getFromStatus(),
        history.getToStatus(),
        history.getAction(),
        history.getActorId(),
        history.getNote(),
        history.getCreatedAt());
  }
}

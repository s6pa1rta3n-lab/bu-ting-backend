package com.butingbe.domain.reward.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Request DTO for partially updating an unconfirmed reward payout item. */
public record AdminRewardPayoutPatchReqDto(
    UUID rewardCatalogId,
    String rewardCode,
    String rewardName,
    Integer points,
    String badgeCode,
    OffsetDateTime scheduledAt,
    String note,
    Long expectedRevision) {}

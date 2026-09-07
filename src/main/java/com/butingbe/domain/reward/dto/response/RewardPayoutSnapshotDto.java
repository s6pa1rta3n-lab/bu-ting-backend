package com.butingbe.domain.reward.dto.response;

import java.util.UUID;

/** Snapshot of reward configuration and catalog metadata associated with a payout. */
public record RewardPayoutSnapshotDto(
    UUID rewardCatalogId,
    String code,
    String name,
    String rewardType,
    Integer points,
    String badgeCode,
    String prizeName) {}

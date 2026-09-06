package com.butingbe.domain.reward.dto.response;

import java.util.UUID;

/** TOP_LIKE 정산 개별 항목 결과. */
public record TopLikeSettlementItemResDto(
    UUID eventId,
    UUID participationId,
    UUID userId,
    int rank,
    long likeCount,
    String rewardCode,
    SettlementItemStatus status,
    UUID grantId,
    UUID couponId) {}

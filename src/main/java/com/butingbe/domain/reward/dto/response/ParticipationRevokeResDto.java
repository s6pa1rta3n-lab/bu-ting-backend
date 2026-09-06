package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import java.util.UUID;

/** 참여 회수 결과. */
public record ParticipationRevokeResDto(
    UUID participationId,
    ParticipationStatus status,
    int revokedGrantsCount,
    int revokedPointsAmount,
    int revokedCouponsCount) {}

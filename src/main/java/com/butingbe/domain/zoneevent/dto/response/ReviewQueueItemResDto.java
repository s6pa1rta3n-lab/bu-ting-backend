package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Review queue item DTO for operators reviewing flagged or degraded participations. */
public record ReviewQueueItemResDto(
    UUID participationId,
    UUID eventId,
    String eventTitle,
    String zoneId,
    UUID userId,
    String userNickname,
    String mediaUrl,
    String content,
    Double targetLatitude,
    Double targetLongitude,
    Double submitLatitude,
    Double submitLongitude,
    Integer distanceM,
    OffsetDateTime capturedAt,
    OffsetDateTime submittedAt,
    ParticipationStatus status,
    boolean isHidden,
    long openReportCount,
    List<String> flaggedReasons) {}

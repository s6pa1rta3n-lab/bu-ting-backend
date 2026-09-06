package com.butingbe.domain.zoneevent.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Single album entry response representing a public successful event participation. */
public record AlbumItemResDto(
    UUID participationId,
    UUID eventId,
    String eventTitle,
    String zoneId,
    UUID userId,
    String authorNickname,
    String authorProfileImageUrl,
    EquippedTitleResDto authorEquippedTitle,
    String mediaUrl,
    String content,
    long likeCount,
    long commentCount,
    OffsetDateTime completedAt,
    boolean likedByMe,
    boolean isMine) {}

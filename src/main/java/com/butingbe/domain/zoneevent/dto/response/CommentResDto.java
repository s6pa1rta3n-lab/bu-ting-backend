package com.butingbe.domain.zoneevent.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO representing an event participation comment. */
public record CommentResDto(
    UUID commentId,
    UUID participationId,
    UUID userId,
    String authorNickname,
    String authorProfileImageUrl,
    EquippedTitleResDto authorEquippedTitle,
    String content,
    LocalDateTime createdAt,
    boolean isMine) {}

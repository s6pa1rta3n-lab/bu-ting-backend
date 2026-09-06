package com.butingbe.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** Request DTO for administrative push notification dispatch. */
public record AdminPushSendReqDto(
    String topic,
    UUID targetUserId,
    String targetZoneId,
    @NotBlank String title,
    @NotBlank String body) {}

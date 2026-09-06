package com.butingbe.domain.notification.dto.request;

/** Request DTO for updating user notification preference toggles. */
public record NotificationSettingReqDto(
    Boolean pushEnabled, Boolean zoneEventEnabled, Boolean settlementEnabled) {}
